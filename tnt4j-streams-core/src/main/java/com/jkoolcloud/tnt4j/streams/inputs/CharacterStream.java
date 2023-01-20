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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.*;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.feeds.Feed;
import com.jkoolcloud.tnt4j.streams.inputs.feeds.ReaderFeed;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements a character arrays based activity stream, where raw input source is {@link Reader} or {@link InputStream}
 * (via {@link InputStreamReader}). This class wraps the raw {@link Reader} with a {@link ReaderFeed}.
 * <p>
 * This activity stream requires parsers that can support {@link Reader}s as the source for activity data.
 * <p>
 * This activity stream supports configuration properties from {@link FeedInputStream} (and higher hierarchy streams).
 * 
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.inputs.feeds.ReaderFeed
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class CharacterStream extends FeedInputStream<Reader, BufferedReader> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(CharacterStream.class);

	/**
	 * Constructs an empty CharacterStream. Requires configuration settings to set input stream source.
	 */
	public CharacterStream() {
		super();
	}

	/**
	 * Constructs a new CharacterStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public CharacterStream(InputStream stream) {
		this();
		setInputStream(stream);
	}

	/**
	 * Constructs a new CharacterStream to obtain activity data from the specified {@link Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public CharacterStream(Reader reader) {
		this();
		setInputSource(reader);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Sets {@link InputStream} from which activity data should be read. Wraps provided input stream into
	 * {@link InputStreamReader}.
	 *
	 * @param stream
	 *            input stream to read data from
	 *
	 * @see #setInputSource(Closeable)
	 */
	@Override
	public void setInputStream(InputStream stream) {
		setInputSource(new InputStreamReader(stream));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * {@link ReaderFeed} type activity data feed is created.
	 */
	@Override
	protected Feed<BufferedReader> createFeedInput(Reader feedSource) {
		return new ReaderFeed(feedSource);
	}
}
