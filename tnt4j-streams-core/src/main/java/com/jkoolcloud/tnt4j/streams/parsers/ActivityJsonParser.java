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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an JSON format string. JSON parsing is
 * performed using {@link JsonPath} API. Activity fields locator values are treated as JsonPath expressions.
 * <p>
 * See <a href="https://github.com/jayway/JsonPath">JsonPath API</a> for more details.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>ReadLines - indicates that complete JSON data package is single line. Default value - '{@code true}'. (Optional,
 * deprecated - use 'ActivityDelim' instead)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class ActivityJsonParser extends GenericActivityParser<DocumentContext> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityJsonParser.class);

	private static final String JSON_PATH_ROOT = "$";// NON-NLS
	private static final String JSON_PATH_SEPARATOR = StreamsConstants.DEFAULT_PATH_DELIM;

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public ActivityJsonParser() {
		super(ActivityFieldDataType.AsInput);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (ParserProperties.PROP_READ_LINES.equalsIgnoreCase(name)) {
			activityDelim = Utils.toBoolean(value) ? ActivityDelim.EOL.name() : ActivityDelim.EOF.name();

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_READ_LINES.equalsIgnoreCase(name)) {
			return activityDelim;
		}

		return super.getProperty(name);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link com.jayway.jsonpath.DocumentContext}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return DocumentContext.class.isInstance(data) || super.isDataClassSupportedByParser(data);
	}

	@Override
	public boolean canHaveDelimitedLocators() {
		return false;
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		DocumentContext jsonDoc;
		String jsonString = null;
		try {
			if (data instanceof DocumentContext) {
				jsonDoc = (DocumentContext) data;
			} else if (data instanceof InputStream) {
				jsonDoc = JsonPath.parse((InputStream) data);
			} else {
				jsonString = getNextActivityString(data);
				if (StringUtils.isEmpty(jsonString)) {
					return null;
				}
				jsonDoc = JsonPath.parse(jsonString);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityJsonParser.jsonDocument.parse.error"), 0);
			pe.initCause(e);

			throw pe;
		}

		if (jsonString == null) {
			jsonString = jsonDoc.jsonString();
		}

		ActivityContext cData = new ActivityContext(stream, data, jsonDoc);
		cData.setMessage(jsonString);

		return cData;
	}

	/**
	 * Reads RAW activity data JSON package string from {@link BufferedReader}.
	 *
	 * @param rdr
	 *            reader to use for reading
	 * @return non empty RAW activity data JSON package string, or {@code null} if the end of the stream has been
	 *         reached
	 */
	@Override
	protected String readNextActivity(BufferedReader rdr) {
		StringBuilder jsonStringBuilder = new StringBuilder(1024);
		String line;

		nextLock.lock();
		try {
			try {
				while ((line = rdr.readLine()) != null) {
					jsonStringBuilder.append(line);
					if (ActivityDelim.EOL.name().equals(activityDelim)) {
						break;
					}
				}
			} catch (EOFException eof) {
				Utils.logThrowable(logger(), OpLevel.DEBUG,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ActivityParser.data.end",
						getActivityDataType()[0], eof);
			} catch (IOException ioe) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.error.reading", getActivityDataType()[0], ioe);
			}
		} finally {
			nextLock.unlock();
		}

		return jsonStringBuilder.toString();
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "JSON", "TEXT" }; // NON-NLS

	/**
	 * Returns types of RAW activity data entries.
	 *
	 * @return types of RAW activity data entries - {@code "JSON"} and {@code "TEXT"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	/**
	 * Gets field raw data value resolved by locator and formats it according locator definition.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            {@link JsonPath} document context to read
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value or applying locator format properties to specified
	 *             value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();

		if (StringUtils.isNotEmpty(locStr)) {
			if (!locStr.startsWith(JSON_PATH_ROOT)) {
				locStr = JSON_PATH_ROOT + JSON_PATH_SEPARATOR + locStr;
			}

			Object jsonValue;
			try {
				jsonValue = cData.getData().read(locStr);
			} catch (JsonPathException exc) {
				jsonValue = null;
			}

			if (jsonValue != null) {
				if (jsonValue instanceof List) {
					List<Object> jsonValuesList = (List<Object>) jsonValue;
					List<Object> valuesList = new ArrayList<>(jsonValuesList.size());
					for (Object jsonValues : jsonValuesList) {
						valuesList.add(locator.formatValue(jsonValues));
					}
					val = valuesList;
				} else {
					val = locator.formatValue(jsonValue);
				}
				formattingNeeded.set(false);
			}
		}

		return val;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
