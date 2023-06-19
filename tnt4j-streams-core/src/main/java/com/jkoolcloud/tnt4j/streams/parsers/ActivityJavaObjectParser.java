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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an plain java {@link Object} (POJO) data
 * structure, where each field or non-args method is represented by declared class field/method. Name of POJO
 * field/method is used to map its value into corresponding activity field.
 * <p>
 * If field is (or method returns) complex object, sub-fields can be accessed using
 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} as naming hierarchy separator: e.g.,
 * 'header.author.name'.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>SupportedClass - defines class/interface name of parser supported objects. Parser can have multiple definitions
 * of this property. It is useful when just some specific set of objects has to be handled by this parser instead of all
 * passed objects. (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#ParserProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#SystemProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#EnvVariable}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityJavaObjectParser extends GenericActivityParser<Object> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityJavaObjectParser.class);

	private Set<Class<?>> supportedClasses = new HashSet<>();

	/**
	 * Constructs a new ActivityJavaObjectParser.
	 */
	public ActivityJavaObjectParser() {
		super(ActivityFieldDataType.AsInput);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (ParserProperties.PROP_SUPPORTED_CLASS.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				String[] sClasses = StreamsConstants.getMultiProperties(value);
				for (String sClass : sClasses) {
					try {
						supportedClasses.add(Class.forName(sClass));
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityJavaObjectParser.adding.supported.class", sClass);
					} catch (Throwable e) {
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityJavaObjectParser.resolve.class.failed", sClass, e.getLocalizedMessage());
					}
				}
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_SUPPORTED_CLASS.equalsIgnoreCase(name)) {
			return supportedClasses;
		}

		return super.getProperty(name);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.Object} - default case when no custom set of supported classes defined</li>
	 * <li>Set of custom classes defined over parser property
	 * {@value com.jkoolcloud.tnt4j.streams.configure.ParserProperties#PROP_SUPPORTED_CLASS}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		if (data == null || CollectionUtils.isEmpty(supportedClasses)) {
			return data instanceof Object;
		} else {
			return isDataClassSupported(data.getClass());
		}
	}

	/**
	 * Checks if set of custom supported classes has class or interface assignable from data object class.
	 * 
	 * @param objCls
	 *            data object class
	 * @return {@code true} if set of custom supported classes has class or interface assignable from data object class,
	 *         {@code false} - otherwise
	 */
	protected boolean isDataClassSupported(Class<?> objCls) {
		for (Class<?> sClass : supportedClasses) {
			if (sClass.isAssignableFrom(objCls)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected String toString(Object data) {
		return data == null ? "null" : ToStringBuilder.reflectionToString(data, ToStringStyle.MULTI_LINE_STYLE);
	}

	/**
	 * Gets field or non-arg method raw data value resolved by locator.
	 *
	 * @param locator
	 *            POJO field or non-arg method name
	 * @param cData
	 *            activity data carrier object
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 * 
	 * @see Utils#getNodePath(String, String)
	 * @see Utils#getFieldValue(String[], Object, int)
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = (String[]) getPreparedLocator(locStr,
				k -> Utils.getNodePath(k, StreamsConstants.DEFAULT_PATH_DELIM));
		try {
			val = Utils.getFieldValue(path, cData.getData(), 0);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityJavaObjectParser.resolve.locator.value.failed", exc);
		}

		return val;
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "OBJECT" }; // NON-NLS

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - {@code "OBJECT"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.REMatchId, ActivityFieldLocatorType.Range);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
