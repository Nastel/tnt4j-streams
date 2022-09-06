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

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.feeds.Feed;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.reference.ParserReference;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for feeds based activity stream, where activity data is read from the specified raw input source (e.g.,
 * {@link InputStream} or {@link Reader}). This class wraps the raw input source with a {@link Feed}. RAW input source
 * also can be {@link File} descriptor or {@link ServerSocket} accepted {@link Socket} connection.
 * <p>
 * In case input source is {@link ServerSocket} connection, stream allows only one {@link Socket} connection at a time.
 * After accepting connection from {@link ServerSocket}, server socket gets closed and no more connections can be
 * accepted. But there is stream property 'RestartOnInputClose' allowing to restart (reset) stream and open new server
 * socket instance if already streamed connection socket gets closed.
 * <p>
 * This activity stream requires parsers that can support type of activity data feed input as the source for activity
 * data.
 * <p>
 * NOTE: there can be only one parser referenced with this kind of stream! Because next item returned by this stream is
 * raw input source and parseable value is retrieved inside parser, there is no way to rewind reader position if first
 * parser fails to parse RAW activity data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>FileName - the system-dependent file name or file name pattern defined using wildcard characters '*' and '?'.
 * (Required - just one of: 'FileName' or 'Port')</li>
 * <li>Port - port number to accept character stream over TCP/IP. (Required - just one of: 'FileName' or 'Port')</li>
 * <li>RestartOnInputClose - flag indicating to restart stream if input socked gets closed. (Optional)</li>
 * </ul>
 *
 * @param <R>
 *            type of raw input source
 * @param <T>
 *            type of activity data feed input
 *
 * @version $Revision: 2 $
 *
 * @see BytesInputStream
 * @see CharacterStream
 * @see ActivityParser#isDataClassSupported(Object)
 */
public abstract class FeedInputStream<R extends Closeable, T> extends TNTParseableInputStream<T> {
	/**
	 * Stream attribute defining file name.
	 */
	protected String fileName = null;
	private Integer socketPort = null;

	private FeedInput feedInput;

	/**
	 * RAW input source (e.g., {@link java.io.Reader} or {@link InputStream}) from which activity data is read.
	 */
	private R rawInputSource = null;

	/**
	 * Activity data feed that wraps {@link #rawInputSource}
	 */
	private Feed<T> dataFeed = null;

	/**
	 * Indicates whether stream should restart listening for incoming data if RAW data socket gets closed (set by
	 * {@code RestartOnInputClose} property) - default: {@code false}
	 */
	private boolean restartOnInputClose = false;

	/**
	 * Constructs an empty FeedInputStream. Requires configuration settings to set input stream source.
	 */
	public FeedInputStream() {
		super();
	}

	/**
	 * Constructs a new FeedInputStream to obtain activity data from the specified raw input source.
	 *
	 * @param source
	 *            raw input source to read data from
	 */
	public FeedInputStream(R source) {
		this();
		setInputSource(source);
	}

	/**
	 * Sets {@link InputStream} from which activity data should be read.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public abstract void setInputStream(InputStream stream);

	/**
	 * Sets raw input source from which activity data should be read.
	 *
	 * @param source
	 *            input source to read data from
	 */
	public void setInputSource(R source) {
		rawInputSource = source;
	}

	/**
	 * Creates activity data feed instance from provided raw input source
	 *
	 * @param feedSource
	 *            raw input source object
	 * @return instance of activity data feed
	 */
	protected abstract Feed<T> createFeedInput(R feedSource);

	@Override
	public void addParser(ParserReference parserRef) throws IllegalStateException {
		if (!parsersSet.isEmpty()) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.cannot.have.multiple.parsers"));
		}

		super.addParser(parserRef);
	}

	@Override
	public void addParsers(Iterable<ActivityParser> parsers) throws IllegalArgumentException {
		if (!parsersSet.isEmpty()) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.cannot.have.multiple.parsers"));
		}

		Iterator<ActivityParser> pi = parsers.iterator();
		while (parsersSet.isEmpty()) {
			addParser(pi.next());
		}

		if (pi.hasNext()) {
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FeedInputStream.skipping.remaining.parsers");
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		if (StreamProperties.PROP_RESTART_ON_CLOSE.equalsIgnoreCase(name)) {
			return restartOnInputClose;
		}
		return super.getProperty(name);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			if (socketPort != null) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
						StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
			}
			fileName = value;
		} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(fileName)) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
						StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
			}
			socketPort = Integer.valueOf(value);
		} else if (StreamProperties.PROP_RESTART_ON_CLOSE.equalsIgnoreCase(name)) {
			restartOnInputClose = Utils.toBoolean(value);
		}
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(fileName) && socketPort == null) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined.one.of", StreamProperties.PROP_FILENAME,
					StreamProperties.PROP_PORT));
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		feedInput = socketPort == null ? new FileInput(fileName, getFileSystem()) : new SocketInput(socketPort);
	}

	/**
	 * Returns {@link java.nio.file.FileSystem} instance used by this stream.
	 *
	 * @return the file system instance to be used
	 *
	 * @throws Exception
	 *             if file system can't be initialized, or file system configuration is malformed
	 */
	protected FileSystem getFileSystem() throws Exception {
		return FileSystems.getDefault();
	}

	/**
	 * Sets up the input data stream to prepare it for reading feeds.
	 *
	 * @throws IOException
	 *             if an I/O error preparing the stream
	 */
	protected void startDataStream() throws IOException {
		if (rawInputSource == null) {
			setInputStream(feedInput.getInputStream());
		}
		if (rawInputSource == null) {
			throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.no.stream.reader"));
		}
		dataFeed = createFeedInput(rawInputSource);
		dataFeed.addFeedListener(new StreamFeedsListener());
	}

	/**
	 * Closes opened data feed.
	 */
	@Override
	protected void stopInternals() {
		Utils.close(dataFeed);

		super.stopInternals();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method does not actually return the next item, but the activity data feed from which the next item should be
	 * read. This is useful for parsers that accept {@link InputStream}s or {@link java.io.Reader}s that are using
	 * underlying classes to process the data from an input stream. The parser needs to handle all I/O, along with any
	 * associated errors.
	 */
	@Override
	public T getNextItem() throws Exception {
		while (true) {
			if (dataFeed == null) {
				try {
					startDataStream();
				} catch (IOException exc) {
					Utils.logThrowable(logger(), OpLevel.WARNING,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FeedInputStream.input.start.failed", exc);
					return null;
				}
			}
			if (dataFeed.isClosed() || dataFeed.hasError()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"FeedInputStream.reader.terminated");
				if (feedInput.canContinue()) {
					resetDataStream();
					continue;
				}

				return null;
			}
			logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FeedInputStream.stream.still.open");
			return dataFeed.getInput();
		}
	}

	private void resetDataStream() {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"FeedInputStream.resetting.stream", feedInput);

		cleanupStreamInternals();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"FeedInputStream.stream.reset", getName());
	}

	@Override
	protected void cleanup() {
		cleanupStreamInternals();

		if (feedInput != null) {
			feedInput.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected void cleanupStreamInternals() {
		Utils.close(rawInputSource);
		Utils.close(dataFeed);

		rawInputSource = null;
		dataFeed = null;

		if (feedInput != null) {
			feedInput.cleanup();
		}
	}

	private class StreamFeedsListener implements Feed.FeedListener {
		@Override
		public void bytesReadFromInput(int bCount) {
			addStreamedBytesCount(bCount);
		}
	}

	/**
	 * Interface defining feed input.
	 */
	protected interface FeedInput {
		/**
		 * Opens input stream from available input resource.
		 *
		 * @return opened input stream to feed data from
		 * @throws IOException
		 *             if fails to open input stream to reed feeds data
		 */
		InputStream getInputStream() throws IOException;

		/**
		 * Checks there is more input resources available or input can be restored.
		 *
		 * @return {@code true} if there is more input resources available or input can be restored, {@code false} -
		 *         otherwise.
		 */
		boolean canContinue();

		/**
		 * Cleans or closes opened input resources, but does not destroy this feed input.
		 */
		void cleanup();

		/**
		 * Shuts down this feed input by destroying it.
		 */
		void shutdown();
	}

	private class SocketInput implements FeedInput {
		private ServerSocket srvSocket = null;
		private Socket socket = null;

		private int socketPort;

		private SocketInput(int socketPort) {
			this.socketPort = socketPort;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			srvSocket = new ServerSocket(socketPort);
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FeedInputStream.waiting.for.connection", socketPort);

			socket = srvSocket.accept();
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FeedInputStream.accepted.connection", socket);
			// only accept one connection, close down server socket
			Utils.close(srvSocket);
			srvSocket = null;

			return socket.getInputStream();
		}

		@Override
		public boolean canContinue() {
			return restartOnInputClose;
		}

		@Override
		public void cleanup() {
			Utils.close(socket);
			Utils.close(srvSocket);

			socket = null;
			srvSocket = null;
		}

		@Override
		public void shutdown() {

		}

		@Override
		public String toString() {
			return socket == null ? String.valueOf(socketPort) : socket.toString();
		}
	}

	private class FileInput implements FeedInput {
		private Iterator<Path> files;
		private Path file;
		private boolean halt = false;

		private String fileName;
		private FileSystem fs;

		private FileInput(String fileName, FileSystem fs) throws IOException {
			this.fileName = fileName;
			this.fs = fs;

			files = Arrays.asList(Utils.listFilesByName(fileName, fs)).iterator();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			while (!halt) {
				if (files.hasNext()) {
					file = files.next();
					if (Files.exists(file)) {
						try {
							InputStream fis = Files.newInputStream(file);
							logger().log(OpLevel.INFO,
									StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
									"FeedInputStream.opening.file", file);
							return fis;
						} finally {
						}
					}
					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FeedInputStream.file.not.found", file);
				} else {
					throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FeedInputStream.no.more.files"));
				}
			}

			return null;
		}

		@Override
		public boolean canContinue() {
			return files != null && files.hasNext();
		}

		@Override
		public void cleanup() {
			file = null;
		}

		@Override
		public void shutdown() {
			halt = true;

			try {
				Utils.close(fs);
			} catch (UnsupportedOperationException exc) {
			}
		}

		@Override
		public String toString() {
			return file == null ? fileName : file.toString();
		}
	}
}
