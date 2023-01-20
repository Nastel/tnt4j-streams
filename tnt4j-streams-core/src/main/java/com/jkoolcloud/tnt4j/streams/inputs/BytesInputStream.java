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

import java.io.BufferedInputStream;
import java.io.InputStream;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.feeds.Feed;
import com.jkoolcloud.tnt4j.streams.inputs.feeds.StreamFeed;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements a byte arrays based activity stream, where raw input source is {@link InputStream}. This class wraps the
 * raw {@link InputStream} with a {@link StreamFeed}.
 * <p>
 * This activity stream requires parsers that can support {@link InputStream}s as the source for activity data.
 * <p>
 * This activity stream supports configuration properties from {@link FeedInputStream} (and higher hierarchy streams).
 *
 * @version $Revision: 1 $
 *
 * @see StreamFeed
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class BytesInputStream extends FeedInputStream<InputStream, BufferedInputStream> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(BytesInputStream.class);

	/**
	 * Constructs an empty BytesInputStream. Requires configuration settings to set input stream source.
	 */
	public BytesInputStream() {
		super();
	}

	/**
	 * Constructs a new BytesInputStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public BytesInputStream(InputStream stream) {
		this();
		setInputSource(stream);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Sets {@link InputStream} from which activity data should be read.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	@Override
	public void setInputStream(InputStream stream) {
		setInputSource(stream);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * {@link StreamFeed} type activity data feed is created.
	 */
	@Override
	protected Feed<BufferedInputStream> createFeedInput(InputStream feedSource) {
		return new StreamFeed(feedSource);
	}
}
