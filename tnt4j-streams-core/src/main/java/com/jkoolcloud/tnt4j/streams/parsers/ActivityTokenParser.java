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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.data.ActivityData;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is a token-separated string of fields, with
 * the value for each field being retrieved from a specific 1-based numeric token position. The field-separator can be
 * customized.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>FieldDelim - fields separator. Default value - {@value DEFAULT_DELIM}. (Optional)</li>
 * <li>Pattern - pattern used to determine which types of activity data string this parser supports. When {@code null},
 * all strings are assumed to match the format supported by this parser. (Optional)</li>
 * <li>StripQuotes - whether surrounding double quotes should be stripped off. Default value - {@code true}.
 * (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label} - only for locator
 * {@value #LOC_FOR_COMPLETE_ACTIVITY_DATA}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#ParserProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#SystemProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#EnvVariable}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityTokenParser extends GenericActivityParser<String[]> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityTokenParser.class);

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) - default:
	 * {@value com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#DEFAULT_DELIM}
	 */
	protected StringMatcher fieldDelim = StringMatcherFactory.INSTANCE.charSetMatcher(DEFAULT_DELIM);

	/**
	 * Indicates whether surrounding double quotes should be stripped from extracted data values (set by
	 * {@code StripQuotes} property) - default: {@code true}
	 */
	protected boolean stripQuotes = true;

	/**
	 * Contains the pattern used to determine which types of activity data string this parser supports (set by
	 * {@code Pattern} property). When {@code null}, all strings are assumed to match the format supported by this
	 * parser.
	 */
	protected Pattern pattern = null;

	/**
	 * String tokenizer instance used to tokenize input string into name/value pairs. It uses {@link #fieldDelim} as
	 * delimiter for name/value pairs.
	 */
	protected StringTokenizer strTokenizer = null;

	/**
	 * Constructs a new ActivityTokenParser.
	 */
	public ActivityTokenParser() {
		super(ActivityFieldDataType.String);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		strTokenizer = stripQuotes
				? new StringTokenizer("", fieldDelim, StringMatcherFactory.INSTANCE.doubleQuoteMatcher())
				: new StringTokenizer("", fieldDelim);
		strTokenizer.setIgnoreEmptyTokens(false);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			fieldDelim = StringUtils.isEmpty(value) ? null : StringMatcherFactory.INSTANCE.charSetMatcher(value);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				pattern = Pattern.compile(value);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
			stripQuotes = Utils.toBoolean(value);
			logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			return fieldDelim;
		}
		if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
			return pattern;
		}
		if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
			return stripQuotes;
		}

		return super.getProperty(name);
	}

	@Override
	protected ActivityInfo parse(TNTInputStream<?, ?> stream, ActivityData<Object> data, ActivityParserContext cData)
			throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityTokenParser.no.field.delimiter"));
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
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.splitting.string", dataStr);
		if (pattern != null) {
			Matcher matcher = pattern.matcher(dataStr);
			if (matcher == null || !matcher.matches()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.input.not.match", getName(), pattern.pattern());
				return null;
			}
		}

		strTokenizer.reset(dataStr);
		String[] fields = strTokenizer.getTokenArray();
		if (ArrayUtils.isEmpty(fields)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.no.fields");
			return null;
		}
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.split", fields.length);

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

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
