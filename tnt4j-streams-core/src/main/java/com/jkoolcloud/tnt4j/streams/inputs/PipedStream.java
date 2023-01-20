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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements piped activity stream, where each piped RAW data line is assumed to represent a single activity or event
 * which should be recorded. This class wraps the RAW {@link InputStream} or {@link Reader} with a
 * {@link BufferedReader}. Default RAW input stream is {@link System#in}.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * NOTE: this stream just pipe input from RAW input source (e.g., System.in) and does not open it. This way when closing
 * this stream, RAW {@link InputStream} or {@link Reader} is not closed and left to be closed by opener.
 * <p>
 * This activity stream supports configuration properties from {@link JavaInputStream} (and higher hierarchy streams).
 * 
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class PipedStream extends JavaInputStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(PipedStream.class);

	/**
	 * Constructs an empty PipedStream. Default input stream is {@link System#in}.
	 */
	public PipedStream() {
		this(System.in);
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public PipedStream(InputStream stream) {
		super(stream);
		inputCloseable = false;
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 * @param charsetName
	 *            input charset name
	 * 
	 * @throws java.io.UnsupportedEncodingException
	 *             if the named charset is not supported
	 */
	public PipedStream(InputStream stream, String charsetName) throws UnsupportedEncodingException {
		super(stream, charsetName);
		inputCloseable = false;
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 * @param charset
	 *            input charset
	 */
	public PipedStream(InputStream stream, Charset charset) {
		super(stream, charset);
		inputCloseable = false;
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified {@link Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public PipedStream(Reader reader) {
		super(reader);
		inputCloseable = false;
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}
}
