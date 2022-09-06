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

package com.jkoolcloud.tnt4j.streams.inputs.feeds;

import java.io.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * This class encapsulates {@link BufferedReader}, wrapping the specified {@link Reader} or {@link InputStream} (via
 * {@link InputStreamReader}), and adding the ability to detect if the underlying object has been closed or reading
 * input was erroneous.
 *
 * @version $Revision: 1 $
 *
 * @see BufferedReader
 * @see InputStreamReader
 * @see com.jkoolcloud.tnt4j.streams.inputs.CharacterStream
 * @see com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream
 */
public class ReaderFeed extends AbstractFeed<BufferedReader> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ReaderFeed.class);

	/**
	 * Constructs a new ReaderFeed, buffering the specified {@link Reader}.
	 *
	 * @param in
	 *            reader to buffer and read
	 * @see BufferedReader#BufferedReader(Reader)
	 */
	public ReaderFeed(Reader in) {
		setInput(new FeedR(in));
	}

	/**
	 * Constructs a new ReaderFeed, buffering the specified {@link Reader}, using an internal buffer with the given
	 * size.
	 *
	 * @param in
	 *            reader to buffer and read
	 * @param size
	 *            buffer size
	 * @see BufferedReader#BufferedReader(Reader, int)
	 */
	public ReaderFeed(Reader in, int size) {
		setInput(new FeedR(in, size));
	}

	/**
	 * Constructs a new ReaderFeed, buffering the specified {@link InputStream}.
	 *
	 * @param in
	 *            input stream to buffer and read
	 * @see BufferedReader#BufferedReader(Reader)
	 */
	public ReaderFeed(InputStream in) {
		this(new InputStreamReader(in));
	}

	/**
	 * Constructs a new ReaderFeed, buffering the specified {@link InputStream}, using an internal buffer with the given
	 * size.
	 *
	 * @param in
	 *            input stream to buffer and read
	 * @param size
	 *            buffer size
	 * @see BufferedReader#BufferedReader(Reader, int)
	 */
	public ReaderFeed(InputStream in, int size) {
		this(new InputStreamReader(in), size);
	}

	private class FeedR extends BufferedReader {
		private FeedR(Reader in) {
			super(in);
		}

		private FeedR(Reader in, int sz) {
			super(in, sz);
		}

		@Override
		public String readLine() throws IOException {
			if (isClosed()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFeed.read.closed");
				return null;
			}

			try {
				String line = super.readLine();

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFeed.read.line", line);

				if (line == null) {
					close();
				} else {
					notifyBytesRead(line.getBytes().length);
				}

				return line;
			} catch (EOFException exc) {
				throw exc;
			} catch (IOException ioe) {
				setError(true);
				throw ioe;
			}
		}

		@Override
		public int read(char cbuf[], int off, int len) throws IOException {
			if (isClosed()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFeed.read.closed");
				return -1;
			}

			try {
				int total = super.read(cbuf, off, len);

				String line = total == -1 ? "EOF" : new String(cbuf, off, total); // NON-NLS

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFeed.read.line", line);

				if (total == -1) {
					close();
				} else {
					notifyBytesRead(total);
				}

				return total;
			} catch (EOFException exc) {
				throw exc;
			} catch (IOException ioe) {
				setError(true);
				throw ioe;
			}
		}

		@Override
		public void close() throws IOException {
			closeFeed();

			super.close();
		}
	}
}
