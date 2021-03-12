/*
 * Copyright 2014-2018 JKOOL, LLC.
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
import java.nio.charset.Charset;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a Java {@link InputStream} or {@link Reader} carried activity stream, where each line of input is assumed
 * to represent a single activity or event which should be recorded. {@link InputStream} or {@link Reader} is defined
 * over configuration element "reference".
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>InputCloseable - flag indicating if stream has to close input when stream is closing. Default value -
 * {@code true}. (Optional)</li>
 * <li>Charset - charset name used to decode file(s) contained data. Charset name must comply Java specification
 * (resolvable by {@link java.nio.charset.Charset#forName(String)} to be handled properly. Default value - one returned
 * by {@link java.nio.charset.Charset#defaultCharset()}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JavaInputStream extends TNTParseableInputStream<String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JavaInputStream.class);

	private Reader rawReader;
	private LineNumberReader inputReader;
	private Charset charset = Charset.defaultCharset();

	private int lineNumber = 0;

	/**
	 * Flag indicating if stream has to close input when stream is closing.
	 */
	protected boolean inputCloseable = true;

	/**
	 * Constructs a new JavaInputStream.
	 */
	public JavaInputStream() {
		super();
	}

	/**
	 * Constructs a new JavaInputStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public JavaInputStream(InputStream stream) {
		this(stream, Charset.defaultCharset());
	}

	/**
	 * Constructs a new JavaInputStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 * @param charsetName
	 *            input charset name
	 * 
	 * @throws java.io.UnsupportedEncodingException
	 *             if the named charset is not supported
	 */
	public JavaInputStream(InputStream stream, String charsetName) throws UnsupportedEncodingException {
		this(new InputStreamReader(stream, charsetName));
		this.charset = Charset.forName(charsetName);
	}

	/**
	 * Constructs a new JavaInputStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 * @param charset
	 *            input charset
	 */
	public JavaInputStream(InputStream stream, Charset charset) {
		this(new InputStreamReader(stream, charset));
		this.charset = charset;
	}

	/**
	 * Constructs a new JavaInputStream to obtain activity data from the specified {@link Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public JavaInputStream(Reader reader) {
		this();
		setReader(reader);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_INPUT_CLOSEABLE.equalsIgnoreCase(name)) {
			inputCloseable = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_CHARSET.equalsIgnoreCase(name)) {
			charset = Charset.forName(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_INPUT_CLOSEABLE.equalsIgnoreCase(name)) {
			return inputCloseable;
		}
		if (StreamProperties.PROP_CHARSET.equalsIgnoreCase(name)) {
			return charset.name();
		}

		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Adds reference to {@link InputStream} to read lines.
	 */
	@Override
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof InputStream) {
			setReader(new InputStreamReader((InputStream) refObject, charset));
		} else if (refObject instanceof Reader) {
			setReader((Reader) refObject);
		}

		super.addReference(refObject);
	}

	private void setReader(Reader reader) {
		this.rawReader = reader;
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (rawReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.no.stream.reader"));
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		inputReader = new LineNumberReader(
				rawReader instanceof BufferedReader ? rawReader : new BufferedReader(rawReader));
	}

	@Override
	public String getNextItem() throws Exception {
		if (inputReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"JavaInputStream.input.not.opened"));
		}

		String line = Utils.getNonEmptyLine(inputReader);
		lineNumber = inputReader.getLineNumber();

		if (line != null) {
			addStreamedBytesCount(line.getBytes().length);
		}

		return line;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the input stream last read.
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	@Override
	protected void cleanup() {
		if (inputCloseable) {
			Utils.close(inputReader);
		}
		inputReader = null;
		rawReader = null;

		super.cleanup();
	}
}
