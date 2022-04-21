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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.data.ActivityData;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is a token-separated string of fields, where
 * each field is represented by a name/value pair and the name is used to map each field into its corresponding activity
 * field. The field-separator and the name/value separator can both be customized.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.streams.parsers.AbstractActivityMapParser}):
 * <ul>
 * <li>FieldDelim - fields separator. Default value - {@value DEFAULT_DELIM}. (Optional)</li>
 * <li>ValueDelim - value delimiter. Default value - {@code "="}. (Optional)</li>
 * <li>Pattern - pattern used to determine which types of activity data string this parser supports. When {@code null},
 * all strings are assumed to match the format supported by this parser. (Optional)</li>
 * <li>StripQuotes - whether surrounding double quotes should be stripped off. Default value - {@code true}.
 * (Optional)</li>
 * <li>EntryPattern - pattern used to to split data into name/value pairs. It should define two RegEx groups named
 * {@code "key"} and {@code "value"} used to map data contained values to name/value pair. NOTE: this parameter takes
 * preference on {@code "FieldDelim"} and {@code "ValueDelim"} properties. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class ActivityNameValueParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityNameValueParser.class);

	private static final Pattern SURROUNDING_QUOTES_PATTERN = Pattern.compile("^\"|\"$"); // NON-NLS

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) - Default:
	 * {@value com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#DEFAULT_DELIM}
	 */
	protected StringMatcher fieldDelim = StringMatcherFactory.INSTANCE.charSetMatcher(DEFAULT_DELIM);

	/**
	 * Contains the name/value separator (set by {@code ValueDelim} property) - Default: "="
	 */
	protected String valueDelim = "="; // NON-NLS

	/**
	 * Contains the pattern used to determine which types of activity data string this parser supports (set by
	 * {@code Pattern} property). When {@code null}, all strings are assumed to match the format supported by this
	 * parser.
	 */
	protected Pattern pattern = null;

	/**
	 * Indicates whether surrounding double quotes should be stripped from extracted data values (set by
	 * {@code StripQuotes} property) - default: {@code true}
	 */
	protected boolean stripQuotes = true;

	/**
	 * Contains the pattern used to to split data into name/value pairs. It should define two RegEx groups named
	 * {@code "key"} and {@code "value"} used to map data contained values to name/value pair. NOTE: this parameter
	 * takes preference on {@link #fieldDelim} and {@link #valueDelim} parameters.
	 */
	protected Pattern entryPattern = null;

	/**
	 * String tokenizer instance used to tokenize input string into name/value pairs. It uses {@link #fieldDelim} as
	 * delimiter for name/value pairs.
	 */
	protected StringTokenizer strTokenizer = null;

	/**
	 * Constructs a new ActivityNameValueParser.
	 */
	public ActivityNameValueParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (entryPattern == null) {
			strTokenizer = stripQuotes
					? new StringTokenizer("", fieldDelim, StringMatcherFactory.INSTANCE.doubleQuoteMatcher())
					: new StringTokenizer("", fieldDelim);
			strTokenizer.setIgnoreEmptyTokens(false);
		}
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			fieldDelim = StringUtils.isEmpty(value) ? null : StringMatcherFactory.INSTANCE.charSetMatcher(value);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_VAL_DELIM.equalsIgnoreCase(name)) {
			valueDelim = value;
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
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_ENTRY_PATTERN.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				entryPattern = Pattern.compile(value);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			return fieldDelim;
		}
		if (ParserProperties.PROP_VAL_DELIM.equalsIgnoreCase(name)) {
			return valueDelim;
		}
		if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
			return pattern;
		}
		if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
			return stripQuotes;
		}
		if (ParserProperties.PROP_ENTRY_PATTERN.equalsIgnoreCase(name)) {
			return entryPattern;
		}

		return super.getProperty(name);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link String}</li>
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
		return isDataClassSupportedByDefault(data);
	}

	@Override
	protected ActivityInfo parse(TNTInputStream<?, ?> stream, ActivityData<Object> data, ActivityParserContext cData)
			throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityNameValueParser.no.field.delimiter"));
		}
		if (valueDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityNameValueParser.no.value.delimiter"));
		}

		return super.parse(stream, data, cData);
	}

	/**
	 * Makes map of name/value pairs, resolved from provided string.
	 *
	 * @param data
	 *            activity object data object
	 *
	 * @return activity object data map
	 */
	@Override
	protected Map<String, String> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

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

		return strTokenizer != null ? delimit(dataStr) : regex(dataStr);
	}

	private Map<String, String> delimit(String dataStr) {
		strTokenizer.reset(dataStr);
		String[] fields = strTokenizer.getTokenArray();
		if (ArrayUtils.isEmpty(fields)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.no.fields");
			return null;
		}
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.split", fields.length);
		Map<String, String> nameValues = new HashMap<>(fields.length);
		for (String field : fields) {
			if (StringUtils.isNotEmpty(field)) {
				String[] nv = field.split(Pattern.quote(valueDelim));
				if (ArrayUtils.isNotEmpty(nv)) {
					String key = nv[0];
					String value = nv.length > 1 ? nv[1].trim() : "";
					if (stripQuotes) {
						key = SURROUNDING_QUOTES_PATTERN.matcher(key).replaceAll("");
						value = SURROUNDING_QUOTES_PATTERN.matcher(value).replaceAll("");
					}

					nameValues.put(key, value);
				}
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityNameValueParser.found.delim", field);
			}
		}

		return nameValues;
	}

	private Map<String, String> regex(String dataStr) {
		Matcher matcher = entryPattern.matcher(dataStr);
		Map<String, String> nameValues = new HashMap<>();
		while (matcher.find()) {
			String key = matcher.group("key"); // NON-NLS
			String value = matcher.group("value"); // NON-NLS
			if (!StringUtils.isAllEmpty(key, value)) {
				nameValues.put(key, value == null ? "" : value.trim());
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityNameValueParser.found.regex", key, value);
			}
		}

		return nameValues;
	}
}
