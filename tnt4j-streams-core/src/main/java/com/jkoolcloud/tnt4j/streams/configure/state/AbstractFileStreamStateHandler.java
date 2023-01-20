/*
 * Copyright 2014-2023 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.configure.state;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractFileLineStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * The class manages stream resuming from last file read line. It manages streamed files access state persisting, and
 * loading of persisted state.
 *
 * @param <T>
 *            the type of streamed file descriptor
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractFileStreamStateHandler<T> {
	private static final String FILE_ACCESS_STATE_SUFFIX = ".xml"; // NON-NLS
	private static final String FILE_ACCESS_STATE_FILENAME = ".TNT4JStreamed"; // NON-NLS
	private static final int BYTES_TO_COUNT_CRC = 256;
	private static final int LINE_SHIFT_TOLERANCE = 50;

	/**
	 * Enum for Error handling when line CRC is mismatched record saved, two options - halt the stream or begin from
	 * found file first line, default - halt;
	 */
	public enum LinePolicy {
		/**
		 * Policy to halt if CRC mismatch.
		 */
		HALT_IF_CRC_MISMATCH,
		/**
		 * Policy to read file from beginning.
		 */
		READ_FROM_BEGINNING
	}

	private LinePolicy linePolicy = LinePolicy.READ_FROM_BEGINNING;

	/**
	 * Streamed file descriptor.
	 */
	protected T file;

	private AbstractFileLineStream.Line prevLine;

	private FileAccessState fileAccessState;

	/**
	 * Constructs a new AbstractFileStreamStateHandler.
	 */
	protected AbstractFileStreamStateHandler() {
	}

	/**
	 * Constructs a new AbstractFileStreamStateHandler. Performs search of persisted streaming state and loads it if
	 * such is available.
	 *
	 * @param activityFiles
	 *            files, passed by stream to search in
	 * @param streamName
	 *            stream name
	 */
	protected AbstractFileStreamStateHandler(T[] activityFiles, String streamName) {
		initialize(activityFiles, streamName);
	}

	/**
	 * Returns logger used by this state handler.
	 *
	 * @return state handler logger
	 */
	protected abstract EventSink logger();

	/**
	 * Initiates streamed files access state handler. Load already persisted stream accessed file state if such is
	 * available.
	 *
	 * @param activityFiles
	 *            streamed files set
	 * @param streamName
	 *            stream name
	 *
	 * @throws IllegalArgumentException
	 *             if streamed files set is empty
	 * @throws IllegalStateException
	 *             if persisted line could not be found in streamed files
	 */
	void initialize(T[] activityFiles, String streamName) {
		if (ArrayUtils.isEmpty(activityFiles)) {
			throw new IllegalArgumentException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FileStreamStateHandler.illegal.argument.file"));
		}
		try {
			fileAccessState = loadState(getParent(activityFiles), streamName);
			if (fileAccessState != null) {
				file = findStreamingFile(fileAccessState, activityFiles);
				if (file != null) {
					fileAccessState.currentLineNumber = checkLine(file, fileAccessState);
					if (linePolicy == LinePolicy.HALT_IF_CRC_MISMATCH
							&& Utils.isZero(fileAccessState.currentLineNumber)) {
						throw new IllegalStateException(StreamsResources.getString(
								StreamsResources.RESOURCE_BUNDLE_NAME, "FileStreamStateHandler.location.not.found"));
					}
				}
			}
		} catch (IOException e) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileStreamStateHandler.file.error.load", e);
		} catch (JAXBException e) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileStreamStateHandler.file.not.parsed", e);
		}

		if (fileAccessState == null) {
			fileAccessState = new FileAccessState();
		}
	}

	abstract String getParent(T[] activityFiles);

	/**
	 * Finds the file to stream matching file header CRC persisted in files access state.
	 *
	 * @param fileAccessState
	 *            persisted files access state
	 * @param streamFiles
	 *            descriptors array of files to search
	 *
	 * @return found file that matches CRC
	 *
	 * @throws IOException
	 *             if file can't be opened.
	 */
	T findStreamingFile(FileAccessState fileAccessState, T[] streamFiles) throws IOException {
		for (T file : streamFiles) {
			Long fileCRC = getFileCrc(file);
			if (fileCRC != null && fileCRC.equals(fileAccessState.currentFileCrc)) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Check if file has persisted state defined line and returns corresponding line number in file.
	 *
	 * @param file
	 *            file to find line matching CRC
	 * @param fileAccessState
	 *            persisted streamed files access state
	 *
	 * @return line number matching CRC, or {@code 0} if line not found
	 *
	 * @throws IOException
	 *             if I/O exception occurs
	 */
	int checkLine(T file, FileAccessState fileAccessState) throws IOException {
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(openFile(file));
			// skip lines until reaching line with number: persisted line number
			// - LINE_SHIFT_TOLERANCE
			int lastAccessedLine = getLastReadLineNumber();

			int startCompareLineIndex = lastAccessedLine - LINE_SHIFT_TOLERANCE;
			int endCompareLineIndex = lastAccessedLine + LINE_SHIFT_TOLERANCE;
			String line;
			int li = 0;
			while (((line = reader.readLine()) != null) && (li <= endCompareLineIndex)) {
				li = reader.getLineNumber();

				if (li >= startCompareLineIndex) {
					if (checkCrc(line, fileAccessState.currentLineCrc)) {
						return li;
					}
				}
			}
		} finally {
			Utils.close(reader);
		}

		return 0;
	}

	/**
	 * Check the line CRC.
	 *
	 * @param line
	 *            line to check
	 * @param crc
	 *            CRC to match
	 *
	 * @return {@code true} if matched
	 */
	private static boolean checkCrc(String line, Long crc) {
		if (line == null || crc == null) {
			return false;
		}

		Checksum crcLine = new CRC32();
		byte[] bytes4Line = line.getBytes();
		crcLine.update(bytes4Line, 0, bytes4Line.length);
		long lineCRC = crcLine.getValue();
		return lineCRC == crc;
	}

	/**
	 * Loads persisted streamed files access state from streamed files directory or system temp directory.
	 *
	 * @param path
	 *            to search in
	 * @param streamName
	 *            stream name
	 *
	 * @return persisted streamed files access state
	 *
	 * @throws IOException
	 *             if file open errors occur
	 * @throws JAXBException
	 *             if state unmarshaller fails
	 */
	FileAccessState loadState(String path, String streamName) throws IOException, JAXBException {
		return loadStateFromTemp(streamName);
	}

	/**
	 * Loads XML persisted streamed files access state from system temp directory.
	 *
	 * @param streamName
	 *            stream name
	 *
	 * @return loaded streamed files access state
	 *
	 * @throws IOException
	 *             if persisted state file can't be loaded
	 * @throws JAXBException
	 *             if state unmarshaling fails
	 */
	private static FileAccessState loadStateFromTemp(String streamName) throws IOException, JAXBException {
		File tempFile = File.createTempFile("CHECK_PATH", null); // NON-NLS
		String path = tempFile.getParent();
		tempFile.delete();

		return loadStateFile(path, streamName);

	}

	/**
	 * Loads XML persisted streamed files access state from directory defined by path. If state files does not exists,
	 * then returns {@code null}.
	 *
	 * @param path
	 *            persisted state file location path
	 * @param streamName
	 *            stream name
	 *
	 * @return loaded streamed files access state, or {@code null} if file does not exists.
	 *
	 * @throws JAXBException
	 *             if state unmarshaling fails
	 */
	static FileAccessState loadStateFile(String path, String streamName) throws JAXBException {
		File stateFile = new File(path + File.separator + getFileName(streamName));

		return isFileAvailable(stateFile) ? unmarshal(stateFile) : null;
	}

	private static boolean isFileAvailable(File f) {
		return f != null && f.isFile() && f.length() > 0;
	}

	/**
	 * Loads XML persisted streamed files access state.
	 *
	 * @param stateFile
	 *            XML file of persisted streamed files access state
	 *
	 * @return loaded streamed files access state
	 *
	 * @throws JAXBException
	 *             if state unmarshaling fails
	 */
	private static FileAccessState unmarshal(File stateFile) throws JAXBException {
		JAXBContext jaxb = JAXBContext.newInstance(FileAccessState.class);
		Unmarshaller unmarshaller = jaxb.createUnmarshaller();
		return (FileAccessState) unmarshaller.unmarshal(stateFile);
	}

	/**
	 * Calculates file header CRC.
	 *
	 * @param file
	 *            file to calculate header CRC
	 *
	 * @return file crc value
	 *
	 * @throws IOException
	 *             if file fails to open
	 */
	Long getFileCrc(T file) throws IOException {
		Reader in = null;
		try {
			in = openFile(file);
			return getInputCrc(in);
		} finally {
			Utils.close(in);
		}
	}

	/**
	 * Creates a new {@link Reader} object for given file to read from.
	 *
	 * @param file
	 *            file to open for reading
	 *
	 * @return reader to read file contents
	 *
	 * @throws IOException
	 *             if file fails to open
	 */
	abstract Reader openFile(T file) throws IOException;

	/**
	 * Calculates CRC value for bytes read from provided input stream.
	 *
	 * @param in
	 *            reader to read bytes for CRC calculation
	 *
	 * @return calculated CRC value
	 *
	 * @throws IOException
	 *             if input can't be read.
	 */
	protected static Long getInputCrc(Reader in) throws IOException {
		char[] buff = new char[BYTES_TO_COUNT_CRC];
		Checksum crc = new CRC32();
		int readLen = in.read(buff);

		if (readLen > 0) {
			String str = new String(buff, 0, readLen);
			byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
			crc.update(bytes, 0, bytes.length);
		}

		return crc.getValue();
	}

	/**
	 * Checks whether streamed file is available is available.
	 *
	 * @return {@code true} if file is available, {@code false} - otherwise
	 */
	public abstract boolean isStreamedFileAvailable();

	/**
	 * Gets the file to be streamed.
	 *
	 * @return descriptor of file to be streamed
	 */
	public T getFile() {
		return isStreamedFileAvailable() ? file : null;
	}

	/**
	 * Gets the line number to be streamed.
	 *
	 * @return line number to be streamed
	 */
	public int getLineNumber() {
		return isStreamedFileAvailable() ? getLastReadLineNumber() : 0;
	}

	private int getLastReadLineNumber() {
		return fileAccessState == null || fileAccessState.currentLineNumber == null ? 0
				: fileAccessState.currentLineNumber;
	}

	/**
	 * Gets the file last read timestamp.
	 *
	 * @return file last read timestamp
	 */
	public long getReadTime() {
		return isStreamedFileAvailable() ? getLastReadTime() : -1;
	}

	private long getLastReadTime() {
		return fileAccessState == null || fileAccessState.lastReadTime == null ? -1 : fileAccessState.lastReadTime;
	}

	/**
	 * Persists files streaming state in specified directory or system temp directory.
	 *
	 * @param fileDir
	 *            directory to save file
	 * @param streamName
	 *            stream name
	 */
	public void writeState(File fileDir, String streamName) {
		try {
			writeState(fileAccessState, fileDir, streamName);
		} catch (JAXBException exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileStreamStateHandler.file.error.save", exc);
		}
	}

	/**
	 * Persists files streaming state in specified directory or system temp directory.
	 *
	 * @param fileAccessState
	 *            streamed files access state
	 * @param fileDir
	 *            directory to save file
	 * @param streamName
	 *            stream name
	 *
	 * @return file containing persisted state
	 *
	 * @throws JAXBException
	 *             if parsing fails
	 */
	static File writeState(FileAccessState fileAccessState, File fileDir, String streamName) throws JAXBException {
		if (fileAccessState == null) {
			return null;
		}

		JAXBContext jaxb = JAXBContext.newInstance(FileAccessState.class);
		Marshaller marshaller = jaxb.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		File fasFile = null;
		String fileName = getFileName(streamName);
		if (fileDir != null) {
			fasFile = new File(fileDir, fileName);
		}

		if (fileDir == null || !fasFile.canWrite()) {
			fasFile = new File(System.getProperty("java.io.tmpdir"), fileName);
		}
		marshaller.marshal(fileAccessState, fasFile);

		return fasFile;
	}

	/**
	 * Returns file name string for persisted streamed files access state.
	 *
	 * @param streamName
	 *            stream name
	 *
	 * @return persisted streamed files access state file name
	 */
	static String getFileName(String streamName) {
		return (StringUtils.isEmpty(streamName) ? "UnnamedStream" : streamName) + FILE_ACCESS_STATE_FILENAME // NON-NLS
				+ FILE_ACCESS_STATE_SUFFIX;
	}

	/**
	 * Save current file access state. Actually takes the current streamed file line, and calculates CRC of that line.
	 *
	 * @param line
	 *            line currently streamed
	 * @param streamName
	 *            stream name
	 */
	public void saveState(AbstractFileLineStream.Line line, String streamName) {
		AbstractFileLineStream.Line procLine = prevLine;
		prevLine = line;
		if (procLine == null) {
			return;
		}

		String lineStr = procLine.getData();
		int lineNr = procLine.getLineNumber();

		try {
			fileAccessState.currentLineNumber = lineNr;
			fileAccessState.lastReadTime = System.currentTimeMillis();

			CRC32 crc = new CRC32();
			byte[] bytes4Line = lineStr.getBytes(StandardCharsets.UTF_8);
			crc.update(bytes4Line, 0, bytes4Line.length);
			fileAccessState.currentLineCrc = crc.getValue();
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileStreamStateHandler.file.error", exc);
		}
	}

	/**
	 * Gets streamed files access state.
	 *
	 * @return streamed files access state
	 */
	public FileAccessState getFileAccessState() {
		return fileAccessState;
	}

	/**
	 * Sets the currently streamed file.
	 *
	 * @param file
	 *            currently streamed file
	 */
	public void setStreamedFile(T file) {
		this.file = file;
		try {
			fileAccessState.currentFileCrc = getFileCrc(file);
		} catch (IOException exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileStreamStateHandler.file.error", exc);
		}
	}

}
