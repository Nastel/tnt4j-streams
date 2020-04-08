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

import java.text.ParseException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.IntRange;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements an activity data parser that assumes each activity data item is a string (0-based characters array), where
 * each field is represented as range of substring characters. Range values can be:
 * <ul>
 * <li>{@code ":x"} - from the beginning of the string to character at the index {@code x} (exclusive)</li>
 * <li>{@code "x:y"} - from character at the index {@code x} (inclusive) to character at the index {@code y}
 * (exclusive)</li>
 * <li>{@code "x:"} - from character at the index {@code x} (inclusive) to the end of the string</li>
 * </ul>
 * <p>
 * This activity parser supports configuration properties from {@link GenericActivityParser} (and higher hierarchy
 * parsers).
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label} - only for locator
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * {@value #LOC_FOR_COMPLETE_ACTIVITY_DATA}</li>
 * </ul>
 * <p>
 * Here are some examples of how this parser can be used:
 * <p>
 * <blockquote>
 * 
 * <pre>
 *  <parser name="StrRangesParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityStringParser">
 *      <field name="EventType" value="EVENT"/>
 *      <field name="EventName" value="SOCGEN_Msg_Data"/>
 *
 * 		<field name= "TransID" field-locator locator="0:14" locator-type="Range"/>
 * 		<field name= "TransType" field-locator locator="20:36" locator-type="Range"/>
 * 		<field name= "TransValue"field-locator locator="70:90" locator-type="Range"/>
 * 		<field name= "UserData" field-locator locator="123:146" locator-type="Range"/>
 * 	</parser>
 * </pre>
 * 
 * </blockquote>
 * <p>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.utils.IntRange
 */
public class ActivityStringParser extends GenericActivityParser<String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityStringParser.class);

	/**
	 * Constructs a new ActivityStringParser.
	 */
	public ActivityStringParser() {
		super(ActivityFieldDataType.String);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
	}

	@Override
	public Object getProperty(String name) {
		return super.getProperty(name);
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		String dataStr = getNextActivityString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}

		ActivityContext cData = new ActivityContext(stream, data, dataStr);
		// cData.setMessage(dataStr);

		return cData;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity data carrier object
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return substring value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		try {
			IntRange range = IntRange.getRange(locStr, true);

			val = StringUtils.substring(cData.getData(), range.getFrom(), range.getTo());
		} catch (Exception exc) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityStringParser.range.exception"), 0);
			pe.initCause(exc);

			throw pe;
		}

		return val;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
