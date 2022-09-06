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

package com.jkoolcloud.tnt4j.streams.sample.custom;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser;
import com.jkoolcloud.tnt4j.streams.parsers.data.ActivityData;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Sample custom parser.
 *
 * @version $Revision: 1 $
 */
public class SampleParser extends GenericActivityParser<String[]> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SampleParser.class);

	/**
	 * Defines field separator.
	 */
	protected String fieldDelim = DEFAULT_DELIM;

	/**
	 * Constructs an SampleParser.
	 */
	public SampleParser() {
		super(ActivityFieldDataType.String);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Sets custom property for this parser.
	 *
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 */
	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		logger().log(OpLevel.DEBUG, "Setting {0} to ''{1}''", name, value); // NON-NLS
		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			fieldDelim = value;
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			return fieldDelim;
		}

		return super.getProperty(name);
	}

	@Override
	protected ActivityInfo parse(TNTInputStream<?, ?> stream, ActivityData<Object> data, ActivityParserContext cData)
			throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException("SampleParser: field delimiter not specified or empty"); // NON-NLS
		}

		return super.parse(stream, data, cData);
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		// Get next string to parse
		String dataStr = getNextActivityString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		logger().log(OpLevel.DEBUG, "Input string to split: {0}", dataStr); // NON-NLS
		String[] fields = dataStr.split(fieldDelim);
		if (ArrayUtils.isEmpty(fields)) {
			logger().log(OpLevel.DEBUG, "Did not find any fields in input string"); // NON-NLS
			return null;
		}
		logger().log(OpLevel.DEBUG, "Split input into {0} fields", fields.length); // NON-NLS

		ActivityContext cData = new ActivityContext(stream, data, fields);
		// cData.setMessage(getRawDataAsMessage(fields));

		return cData;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object data fields array
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();
		String[] fields = cData.getData();

		if (StringUtils.isNotEmpty(locStr)) {
			int loc = Integer.parseInt(locStr);

			if (loc > 0 && loc <= fields.length) {
				val = fields[loc - 1].trim();
			}
		}

		return val;
	}
}
