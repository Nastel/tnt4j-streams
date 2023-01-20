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

package com.jkoolcloud.tnt4j.streams.sample.custom;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.TNTParseableInputStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Sample custom stream.
 *
 * @version $Revision: 1 $
 */
public class SampleStream extends TNTParseableInputStream<String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SampleStream.class);

	private String fileName;
	private File activityFile;
	private LineNumberReader lineReader;
	private int lineNumber;

	private SampleStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Get value of specified property.
	 *
	 * @param name
	 *            name of property whose value is to be retrieved
	 *
	 * @return value for property, or {@code null} if property does not exist
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		return super.getProperty(name);
	}

	/**
	 * Set property for this activity stream.
	 *
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 */
	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			fileName = value;
		}
	}

	/**
	 * Initialize the stream.
	 *
	 * @throws Exception
	 *             indicates that stream is not configured properly and cannot continue.
	 */
	@Override
	public void initialize() throws Exception {
		super.initialize();
		if (fileName == null) {
			throw new IllegalStateException("SampleStream: File name not defined"); // NON-NLS
		}
		logger().log(OpLevel.DEBUG, "Opening file: {0}", fileName); // NON-NLS
		activityFile = new File(fileName);
		lineReader = new LineNumberReader(new FileReader(activityFile));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string containing the contents of the next line in the file.
	 */
	@Override
	public String getNextItem() throws Exception {
		if (lineReader == null) {
			throw new IllegalStateException("SampleStream: File is not opened for reading"); // NON-NLS
		}
		String line = lineReader.readLine();
		lineNumber = lineReader.getLineNumber();
		return line;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the file last read.
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	/**
	 * Closes file being read.
	 */
	@Override
	protected void cleanup() {
		Utils.close(lineReader);

		lineReader = null;
		activityFile = null;

		super.cleanup();
	}
}
