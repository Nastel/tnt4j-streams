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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class encapsulates {@link BufferedInputStream}, wrapping the specified {@link InputStream}, and adding the
 * ability to detect if the underlying object has been closed or reading input was erroneous.
 *
 * @version $Revision: 1 $
 *
 * @see BufferedInputStream
 */
public class StreamFeed extends AbstractFeed<BufferedInputStream> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamFeed.class);

	/**
	 * Constructs a new StreamFeed, buffering the specified {@link InputStream}.
	 *
	 * @param in
	 *            input stream to buffer and read
	 * @see BufferedInputStream#BufferedInputStream(InputStream)
	 */
	public StreamFeed(InputStream in) {
		setInput(new FeedS(in));
	}

	/**
	 * Constructs a new StreamFeed, buffering the specified {@link InputStream}, using an internal buffer with the given
	 * size.
	 *
	 * @param in
	 *            input stream to buffer and read
	 * @param size
	 *            buffer size
	 * @see BufferedInputStream#BufferedInputStream(InputStream, int)
	 */
	public StreamFeed(InputStream in, int size) {
		setInput(new FeedS(in, size));
	}

	private class FeedS extends BufferedInputStream {
		private FeedS(InputStream in) {
			super(in);
		}

		private FeedS(InputStream in, int size) {
			super(in, size);
		}

		@Override
		public synchronized int read() throws IOException {
			if (isClosed()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFeed.read.closed");
				return -1;
			}

			try {
				int b = super.read();

				LoggerUtils.log(LOGGER, OpLevel.DEBUG,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "AbstractFeed.read.bytes",
						() -> Utils.args(b == -1 ? "EOF" : String.format("%02X ", b))); // NON-NLS

				if (b == -1) {
					close();
				} else {
					notifyBytesRead(1);
				}

				return b;
			} catch (EOFException exc) {
				throw exc;
			} catch (IOException ioe) {
				setError(true);
				throw ioe;
			}
		}

		@Override
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			if (isClosed()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFeed.read.closed");
				return -1;
			}

			try {
				int total = super.read(b, off, len);

				LoggerUtils.log(LOGGER, OpLevel.DEBUG,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "AbstractFeed.read.bytes",
						() -> Utils.args(total == -1 ? "EOF" : Utils.toHexDump(b, off, total)));

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

		@Override
		public boolean markSupported() {
			return false;
		}
	}
}
