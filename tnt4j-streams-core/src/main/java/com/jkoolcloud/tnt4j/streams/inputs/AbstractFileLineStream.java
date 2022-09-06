/*
 * Copyright 2014-2022 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.state.AbstractFileStreamStateHandler;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.parsers.data.CommonActivityData;
import com.jkoolcloud.tnt4j.streams.utils.Duration;
import com.jkoolcloud.tnt4j.streams.utils.IntRange;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for files lines activity stream, where each line of the file is assumed to represent a single activity or
 * event which should be recorded. Stream also can read changes from defined files every "FileReadDelay" property
 * defined seconds (default is 15sec.).
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>FileName - the system-dependent file name or file name pattern defined using wildcard characters '*' and '?'.
 * (Required)</li>
 * <li>FilePolling - flag {@code true}/{@code false} indicating whether files should be polled for changes or not. If
 * not, then files are read from oldest to newest sequentially one single time. Default value - {@code false}.
 * (Optional)</li>
 * <li>FileReadDelay - delay in seconds between file reading iterations. Actual only if 'FilePolling' property is set to
 * {@code true}. Default value - {@code 15sec}. (Optional)</li>
 * <li>RestoreState - flag {@code true}/{@code false} indicating whether files read state should be stored and restored
 * on stream restart. Note, if 'StartFromLatest' is set to {@code false} - read state storing stays turned on, but
 * previous stored read state is reset (no need to delete state file manually). Default value - {@code false}.
 * (Optional)</li>
 * <li>StartFromLatest - flag {@code true}/{@code false} indicating that streaming should be performed from latest file
 * entry line. If {@code false} - then all lines from available files are streamed on startup. Actual only if
 * 'FilePolling' or 'RestoreState' properties are set to {@code true}. Default value - {@code true}. (Optional)</li>
 * <li>RangeToStream - defines streamed data lines index range. Default value - {@code 1:}. (Optional)</li>
 * <li>ActivityDelim - defines activities data delimiter used by stream. Value can be: {@code "EOL"} - end of line,
 * {@code "EOF"} - end of file/stream, or any user defined symbol or string. Default value - '{@code EOL}'.
 * (Optional)</li>
 * <li>KeepLineSeparators - flag indicating whether to return line separators at the end of read line. Default value -
 * {@code false}. (Optional)</li>
 * <li>TruncatedFilePolicy - defines truncated file (when size of the file decreases while it is streamed) access
 * policy. Value can be: {@code "START_FROM_BEGINNING"} - read file from beginning, {@code "CONTINUE_FROM_LAST"} -
 * continue reading file from last line (skipping all available lines). Default value - '{@code START_FROM_BEGINNING}'.
 * (Optional)</li>
 * <li>Charset - charset name used to decode file(s) contained data. Charset name must comply Java specification (be
 * resolvable by {@link java.nio.charset.Charset#forName(String)} to be handled properly. {@code "guess"} value
 * indicates that stream (except HDFS) shall guess charset using some set of first bytes from file. Default value - one
 * returned by {@link java.nio.charset.Charset#defaultCharset()}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 3 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractFileLineStream<T> extends AbstractBufferedStream<AbstractFileLineStream.Line> {
	private static final long DEFAULT_DELAY_PERIOD = TimeUnit.SECONDS.toMillis(15);

	/**
	 * Stream attribute defining file name or file name pattern.
	 */
	protected String fileName = null;
	/**
	 * Charset name of streamed files. {@code "guess"} value indicates to guess charset using some set of first bytes
	 * from file.
	 */
	protected String charsetName = Utils.UTF8;

	/**
	 * Stream attribute defining if streaming should be performed from file position found on stream initialization. If
	 * {@code false} - then streaming is performed from beginning of the file.
	 */
	protected boolean startFromLatestActivity = true;

	private long fileWatcherDelay = DEFAULT_DELAY_PERIOD;

	private FileWatcher fileWatcher;
	private boolean pollingOn = false;

	/**
	 * File read state storing-restoring manager.
	 */
	protected AbstractFileStreamStateHandler<T> stateHandler;

	/**
	 * Stream attribute defining whether file read state should be stored and restored on stream restart.
	 */
	protected boolean storeState = false;

	private String rangeValue = "1:"; // NON-NLS
	private IntRange lineRange = null;

	/**
	 * Streamed activity delimiter: {@code "EOL"} (end of line ) or {@code "EOF"} (end of file).
	 */
	protected String activityDelimiter = ActivityDelim.EOL.name();
	/**
	 * Defines truncated file (when size of the file decreases while it is streamed) access policy. Value can be:
	 * {@code "START_FROM_BEGINNING"} - read file from beginning, {@code "CONTINUE_FROM_LAST"} - continue reading file
	 * from last line (skipping all available lines).
	 */
	protected String truncatedFilePolicy = FileAccessPolicy.START_FROM_BEGINNING.name();
	/**
	 * Flag indicating to keep line separators within streamed data.
	 */
	protected boolean keepLineSeparators = false;

	/**
	 * Constructs a new AbstractFileLineStream.
	 */
	protected AbstractFileLineStream() {
		super(1);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			fileName = value;
		} else if (StreamProperties.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
			startFromLatestActivity = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_FILE_READ_DELAY.equalsIgnoreCase(name)) {
			fileWatcherDelay = TimeUnit.SECONDS.toMillis(Long.parseLong(value));
		} else if (StreamProperties.PROP_FILE_POLLING.equalsIgnoreCase(name)) {
			pollingOn = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_RESTORE_STATE.equalsIgnoreCase(name)) {
			storeState = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_RANGE_TO_STREAM.equalsIgnoreCase(name)) {
			rangeValue = value;
		} else if (StreamProperties.PROP_ACTIVITY_DELIM.equalsIgnoreCase(name)) {
			activityDelimiter = value;
		} else if (StreamProperties.PROP_KEEP_LINE_SEPARATORS.equalsIgnoreCase(name)) {
			keepLineSeparators = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_TRUNCATED_FILE_POLICY.equalsIgnoreCase(name)) {
			truncatedFilePolicy = value;
		} else if (StreamProperties.PROP_CHARSET.equalsIgnoreCase(name)) {
			charsetName = value;
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (StreamProperties.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
			return startFromLatestActivity;
		}
		if (StreamProperties.PROP_FILE_READ_DELAY.equalsIgnoreCase(name)) {
			return fileWatcherDelay;
		}
		if (StreamProperties.PROP_FILE_POLLING.equalsIgnoreCase(name)) {
			return pollingOn;
		}
		if (StreamProperties.PROP_RESTORE_STATE.equalsIgnoreCase(name)) {
			return storeState;
		}
		if (StreamProperties.PROP_RANGE_TO_STREAM.equalsIgnoreCase(name)) {
			return rangeValue;
		}
		if (StreamProperties.PROP_ACTIVITY_DELIM.equalsIgnoreCase(name)) {
			return activityDelimiter;
		}
		if (StreamProperties.PROP_KEEP_LINE_SEPARATORS.equalsIgnoreCase(name)) {
			return keepLineSeparators;
		}
		if (StreamProperties.PROP_TRUNCATED_FILE_POLICY.equalsIgnoreCase(name)) {
			return truncatedFilePolicy;
		}
		if (StreamProperties.PROP_CHARSET.equalsIgnoreCase(name)) {
			return charsetName;
		}
		return super.getProperty(name);
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		lineRange = IntRange.getRange(rangeValue);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #initializeFs()
	 */
	@Override
	public void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(fileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_FILENAME));
		}

		if (!pollingOn && !storeState) {
			startFromLatestActivity = false;
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"FileLineStream.initializing.stream", fileName);

		initializeFs();

		fileWatcher = createFileWatcher();
		fileWatcher.initialize(charsetName);
	}

	/**
	 * Initializes stream used file system environment.
	 * <p>
	 * This method is called by default {@link #initialize()} method to perform any necessary file system initialization
	 * before the stream starts reading files, including verifying that all required properties are set. If subclasses
	 * override this method to perform any custom file system initialization, they must also call the base class method.
	 * <p>
	 * Default implementation does nothing, since default file system does not require any additional initialization.
	 * 
	 * @throws Exception
	 *             indicates that stream is not configured properly and cannot continue
	 *
	 * @see #initialize()
	 */
	protected void initializeFs() throws Exception {
	}

	@Override
	protected void start() throws Exception {
		super.start();

		fileWatcher.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns total lines count in all streamed files.
	 */
	@Override
	public int getTotalActivities() {
		return fileWatcher == null ? super.getTotalActivities() : fileWatcher.totalLinesCount;
	}

	@Override
	public long getTotalBytes() {
		return fileWatcher == null ? super.getTotalBytes() : fileWatcher.totalBytesCount;
	}

	@Override
	protected void cleanup() {
		if (fileWatcher != null) {
			fileWatcher.shutdown();
		}

		super.cleanup();
	}

	@Override
	public Line getNextItem() throws Exception {
		Line nextItem = super.getNextItem();
		if (stateHandler != null) {
			stateHandler.saveState(nextItem, getName());
		}

		return nextItem;
	}

	/**
	 * Constructs a new file watcher instance specific for this stream.
	 *
	 * @return file watcher instance
	 *
	 * @throws java.lang.Exception
	 *             if file watcher can't be initialized or initialization fails
	 */
	protected abstract FileWatcher createFileWatcher() throws Exception;

	@Override
	protected boolean isInputEnded() {
		return fileWatcher.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(Line activityItem) {
		return activityItem == null || activityItem.getData() == null ? 0 : activityItem.getData().getBytes().length;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the file last read.
	 */
	@Override
	public int getActivityPosition() {
		return fileWatcher == null ? 0 : fileWatcher.lineNumber;
	}

	/**
	 * Base class containing common file watcher features.
	 */
	protected abstract class FileWatcher extends InputProcessor {

		protected T fileToRead = null;
		protected Charset fileCharset = Charset.defaultCharset();

		protected T[] availableFiles;

		/**
		 * File monitor attribute storing line number marker of streamed file.
		 */
		protected int lineNumber = -1;

		/**
		 * File monitor attribute storing modification time of streamed file.
		 */
		protected long lastModifTime = -1;
		/**
		 * File monitor attribute storing timestamp for last reading of new file lines.
		 */
		protected long lastReadTime = -1;

		/**
		 * Total bytes count available to stream.
		 */
		protected int totalBytesCount = 0;
		/**
		 * Total lines count available to stream.
		 */
		protected int totalLinesCount = 0;

		/**
		 * Constructs a new FileWatcher.
		 *
		 * @param name
		 *            the name of file watcher thread
		 */
		FileWatcher(String name) {
			super(name);
		}

		@Override
		protected void initialize(Object... params) throws Exception {
			String pCharsetName = (String) params[0];

			if (StringUtils.isNotEmpty(pCharsetName)) {
				if (pCharsetName.equalsIgnoreCase("guess")) { // NON-NLS
					fileCharset = null;
				} else {
					fileCharset = Charset.forName(pCharsetName);
				}
			}
		}

		/**
		 * Checks if stored file read state is available and should be loaded.
		 *
		 * @return flag indicating whether stored file read state should be loaded
		 */
		protected boolean isStoredStateAvailable() {
			return startFromLatestActivity && stateHandler != null && stateHandler.isStreamedFileAvailable();
		}

		/**
		 * Performs continuous file monitoring until stream thread is halted or monitoring is interrupted. File
		 * monitoring is performed with {@link #fileWatcherDelay} defined delays between iterations.
		 */
		@Override
		public void run() {
			while (!isStopping()) {
				readFileChanges();

				if (isReadingLatestFile() && !isStopping()) {
					if (!pollingOn) {
						shutdown();
					} else {
						logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FileLineStream.waiting", TimeUnit.MILLISECONDS.toSeconds(fileWatcherDelay));
						StreamThread.sleep(fileWatcherDelay);
					}
				}
			}
		}

		private boolean isReadingLatestFile() {
			return fileToRead == null || ArrayUtils.isEmpty(availableFiles) ? true
					: fileToRead.equals(Utils.lastOf(availableFiles));
		}

		/**
		 * Performs file changes reading.
		 */
		protected abstract void readFileChanges();

		/**
		 * Reads new file lines and adds them to changed lines buffer.
		 *
		 * @param lnr
		 *            line number reader
		 * @throws IOException
		 *             if exception occurs when reading file line
		 */
		protected void readNewFileLines(LineNumberReader lnr) throws IOException {
			String line;
			StringBuilder sb = new StringBuilder(256);
			while ((line = lnr.readLine()) != null && !isInputEnded()) {
				lastReadTime = System.currentTimeMillis();
				lineNumber = lnr.getLineNumber();
				if (StringUtils.isNotEmpty(line) && IntRange.inRange(lineRange, lineNumber)) {
					addActivityDataLine(line, sb, lineNumber);
				} else {
					skipFilteredActivities();
				}
			}

			if (sb.length() > 0) {
				addLineToBuffer(sb, lineNumber);
			}
		}

		private void addActivityDataLine(String line, StringBuilder sb, int lineNumber) {
			sb.append(line);
			if (keepLineSeparators) {
				sb.append('\n');
			}

			if (lineHasActivityDelim(line)) {
				addLineToBuffer(sb, lineNumber);
			}
		}

		private boolean lineHasActivityDelim(String line) {
			if (activityDelimiter.equals(ActivityDelim.EOL.name())) {
				return true;
			} else if (activityDelimiter.equals(ActivityDelim.EOF.name())) {
				return false;
			} else {
				return line.endsWith(activityDelimiter);
			}
		}

		private void addLineToBuffer(StringBuilder sb, int lineNumber) {
			addInputToBuffer(new Line(sb.toString(), lineNumber));
			sb.setLength(0);
		}

		/**
		 * Sets currently read file.
		 *
		 * @param file
		 *            file to read
		 * 
		 * @throws java.io.IOException
		 *             if an I/O error occurs
		 */
		protected void setFileToRead(T file) throws IOException {
			this.fileToRead = file;

			if (stateHandler != null) {
				stateHandler.setStreamedFile(file);
			}
		}

		/**
		 * Persists file access state.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		@Override
		void closeInternals() throws Exception {
			if (stateHandler != null && fileToRead != null) {
				stateHandler.writeState(fileToRead instanceof File ? ((File) fileToRead).getParentFile() : null,
						AbstractFileLineStream.this.getName());
			}
		}

		/**
		 * Returns time period in seconds from last activity provided <tt>timestamp</tt> value.
		 *
		 * @param timestamp
		 *            timestamp value to calculate period
		 * @return time period in seconds to be logged
		 */
		protected Object getPeriodInSeconds(long timestamp) {
			return timestamp < 0 ? "--" : Duration.duration(timestamp, TimeUnit.SECONDS); // NON-NLS
		}
	}

	/**
	 * File line data package defining line text string and line number in file.
	 */
	public static class Line extends CommonActivityData<String> {
		private int lineNr;

		/**
		 * Creates a new Line.
		 *
		 * @param text
		 *            line text string
		 * @param lineNumber
		 *            line number in file
		 */
		public Line(String text, int lineNumber) {
			super(text);
			this.lineNr = lineNumber;
		}

		/**
		 * Returns line number in file.
		 *
		 * @return line number in file
		 */
		public int getLineNumber() {
			return lineNr;
		}

		@Override
		public String toString() {
			return getData();
		}
	}

	/**
	 * List built-in types of activity data delimiters within RAW data.
	 */
	protected enum ActivityDelim {
		/**
		 * Activity data delimiter is end-of-line.
		 */
		EOL,

		/**
		 * Activity data delimiter is end-of-file.
		 */
		EOF,
	}

	/**
	 * Lists predefined special case file(s) access policies.
	 */
	protected enum FileAccessPolicy {
		/**
		 * Stream shall read file(s) from beginning.
		 */
		START_FROM_BEGINNING,

		/**
		 * Stream shall continue reading file(s) from last line (skipping all available lines).
		 */
		CONTINUE_FROM_LAST,
	}
}
