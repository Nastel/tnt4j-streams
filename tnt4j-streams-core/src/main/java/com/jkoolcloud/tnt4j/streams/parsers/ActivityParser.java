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

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTParseableInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.data.ActivityData;
import com.jkoolcloud.tnt4j.streams.parsers.data.CommonActivityData;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class that all activity parsers must extend. It provides some base functionality useful for all activity
 * parsers.
 *
 * @version $Revision: 1 $
 */
public abstract class ActivityParser implements NamedObject {
	/**
	 * Name of activity parser
	 */
	private String name;

	private ActivityFieldDataType defaultDataType;
	private boolean defaultEmptyAsNull = true;

	/**
	 * Constructs a new ActivityParser.
	 */
	protected ActivityParser() {
	}

	/**
	 * Returns logger used by this parser.
	 *
	 * @return parser logger
	 */
	protected abstract EventSink logger();

	/**
	 * Set configuration properties for the parser.
	 * <p>
	 * This method is called during the parsing of the configuration when all specified properties in the configuration
	 * have been loaded. In general, parsers should ignore properties that they do not recognize, since they may be
	 * valid for a subclass of the parser. If extending an existing parser subclass, the method from the base class
	 * should be called so that it can process any properties it requires.
	 *
	 * @param props
	 *            configuration properties to set
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	/**
	 * Sets configuration property for this activity parser.
	 *
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 *
	 * @see #setProperties(java.util.Collection)
	 */
	public abstract void setProperty(String name, String value);

	/**
	 * Get value of specified property. If subclasses override {@link #setProperties(java.util.Collection)}, they should
	 * generally override this method as well to return the value of custom properties, and invoke the base class method
	 * to handle any built-in properties.
	 * 
	 * @param name
	 *            name of property whose value is to be retrieved
	 * @return value for property, or {@code null} if property does not exist
	 *
	 * @see #setProperties(java.util.Collection)
	 */
	public abstract Object getProperty(String name);

	/**
	 * Add an activity field definition to the set of fields supported by this parser.
	 *
	 * @param field
	 *            activity field to add
	 */
	public abstract void addField(ActivityField field);

	/**
	 * Verifies field references and arranges parser fields to ensure right values resolution order.
	 */
	public abstract void organizeFields();

	/**
	 * Parse the specified raw activity data, converting each field in raw data to its corresponding value for passing
	 * to jKoolCloud.
	 *
	 * @param stream
	 *            parent stream
	 * @param data
	 *            raw activity data to parse
	 * @return converted activity info, or {@code null} if raw activity data does not match format for this parser
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error occurs parsing raw data string
	 * @see #isDataClassSupported(Object)
	 * @see GenericActivityParser#parsePreparedItem(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	@SuppressWarnings("unchecked")
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		ActivityData<Object> pData;

		if (data instanceof ActivityData) {
			pData = (ActivityData<Object>) data;
		} else {
			pData = new CommonActivityData<>(data);
		}

		return parse(stream, pData, null);
	}

	/**
	 * Parse the specified raw activity data, converting each field in raw data to its corresponding value for passing
	 * to jKoolCloud.
	 *
	 * @param stream
	 *            parent stream
	 * @param data
	 *            raw activity data to parse
	 * @param cData
	 *            parent parser context data package
	 * @return converted activity info, or {@code null} if raw activity data does not match format for this parser
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error occurs parsing raw data string
	 * @see #isDataClassSupported(Object)
	 * @see GenericActivityParser#parsePreparedItem(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected abstract ActivityInfo parse(TNTInputStream<?, ?> stream, ActivityData<Object> data,
			ActivityParserContext cData) throws IllegalStateException, ParseException;

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	public abstract boolean isDataClassSupported(Object data);

	/**
	 * Sets the value for the field in the specified activity.
	 *
	 * @param ai
	 *            activity object whose field is to be set
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 * @throws ParseException
	 *             if an error occurs while parsing provided activity data {@code value}
	 */
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		ai.applyField(field, value);
	}

	/**
	 * Sets the value for the field in the specified activity entity.
	 * <p>
	 * If field has stacked parser defined, then field value is parsed into separate activity using stacked parser. If
	 * field can be parsed by stacked parser, produced activity can be merged or added as a child into specified
	 * (parent) activity depending on stacked parser reference 'aggregation' attribute value.
	 *
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 * @param cData
	 *            parsing context data package
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error occurs while parsing provided activity data {@code value}
	 * @see #parse(TNTInputStream, Object)
	 */
	protected void applyFieldValue(ActivityField field, Object value, ActivityParserContext cData)
			throws IllegalStateException, ParseException {

		ActivityInfo ai = cData.getActivity();
		applyFieldValue(ai, field, value);

		if (value != null && CollectionUtils.isNotEmpty(field.getStackedParsers())) {
			boolean applied = false;
			for (ActivityField.FieldParserReference parserRef : field.getStackedParsers()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.stacked.parser.applying", name, field, parserRef);
				try {
					Object valueToParse = parserRef.getApplyOn() == ParserApplyType.Activity
							? ai.getFieldValue(field.getFieldTypeName()) : value;
					logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.stacked.parser.input.value.type", name, field, parserRef,
							valueToParse == null ? null : valueToParse.getClass().getName());
					applied |= applyStackedParser(field, parserRef, valueToParse, cData);

					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.stacked.parser.applied", name, field, parserRef, applied);

					if (applied && !parserRef.isContinuous()) {
						break;
					}
				} catch (Exception exc) {
					Utils.logThrowable(logger(), OpLevel.WARNING,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.stacked.parser.failed", name, field, parserRef, exc);
				}
			}

			if (!applied) {
				logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.stacked.parsers.missed", name, field);
			}
		}
	}

	/**
	 * Applies stacked parser to parse provided activity data {@code value}.
	 *
	 * @param field
	 *            activity field providing data
	 * @param parserRef
	 *            stacked parser reference
	 * @param value
	 *            data value to be parsed by stacked parser
	 * @param cData
	 *            parsing context data package
	 * @return {@code true} if referenced stacked parser was applied, {@code false} - otherwise
	 * @throws ParseException
	 *             if an error occurs while parsing provided activity data {@code value}
	 */
	protected boolean applyStackedParser(ActivityField field, ActivityField.FieldParserReference parserRef,
			Object value, ActivityParserContext cData) throws ParseException {
		TNTInputStream<?, ?> stream = cData.getStream();
		ActivityInfo ai = cData.getActivity();

		boolean dataMatch = parserRef.getParser().isDataClassSupported(value);
		Boolean tagsMatch = null;
		Boolean expMatch = null;

		boolean parserMatch = dataMatch;

		if (parserMatch) {
			tagsMatch = tagsMatch(stream, value, parserRef);
			parserMatch = BooleanUtils.toBooleanDefaultIfNull(tagsMatch, true);

			if (parserMatch) {
				expMatch = parserRef.matchExp(this, value, (Map<String, ?>) cData);
				parserMatch = BooleanUtils.toBooleanDefaultIfNull(expMatch, true);
			}
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.stacked.parser.match", parserRef, dataMatch, tagsMatch == null ? "----" : tagsMatch, // NON-NLS
				expMatch == null ? "----" : expMatch); // NON-NLS

		if (parserMatch) {
			cData.setParserRef(parserRef);
			ActivityData<Object> pData = new CommonActivityData<>(value);
			ActivityInfo sai = parserRef.getParser().parse(stream, pData, cData);

			if (sai != null) {
				if (parserRef.getAggregationType() == AggregationType.Merge) {
					ai.mergeAll(sai);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if activity data provided tags matches stacked parser reference bound tags.
	 * <p>
	 * If data tags or parser reference bound tags are {@code null} or empty, then {@code true} is returned meaning no
	 * tags matching shall be applicable by any of both parties: data or parser.
	 *
	 * @param stream
	 *            parent stream used to resolve tags from activity data
	 * @param value
	 *            data value to be parsed by stacked parser
	 * @param parserRef
	 *            stacked parser reference
	 * @return {@code null} if any of parser or data tag arrays are {@code null} or empty, {@code true} if
	 *         {@code dataTags} or parser reference bound tags matches any element of both arrays, {@code false} -
	 *         otherwise
	 *
	 * @see com.jkoolcloud.tnt4j.streams.reference.ParserReference#matchTags(String[])
	 */
	protected static Boolean tagsMatch(TNTInputStream<?, ?> stream, Object value,
			ActivityField.FieldParserReference parserRef) {
		String[] dataTags = null;
		if (stream instanceof TNTParseableInputStream) {
			dataTags = ((TNTParseableInputStream<?>) stream).getDataTags(value);
		}

		return parserRef.matchTags(dataTags);
	}

	/**
	 * Checks if activity {@code data} package is supported by {@code field} referenced stacked parsers.
	 *
	 * @param field
	 *            activity field to get stacked parsers
	 * @param data
	 *            activity data package
	 * @param <T>
	 *            type of activity data package
	 * @return {@code true} if field has stacked parsers defined and activity data is supported by any of those parsers,
	 *         {@code false} - otherwise.
	 *
	 * @see #isDataClassSupported(Object)
	 */
	protected static <T> boolean isDataSupportedByStackedParser(ActivityField field, T data) {
		Collection<ActivityField.FieldParserReference> stackedParsers = field.getStackedParsers();

		if (stackedParsers != null) {
			for (ActivityField.FieldParserReference pRef : stackedParsers) {
				if (pRef.getParser().isDataClassSupported(data)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns name of activity parser
	 *
	 * @return name string of activity parser
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets name for activity parser
	 *
	 * @param name
	 *            name string value
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns whether this parser supports delimited locators in parser fields configuration. This allows user to
	 * define multiple locations for a field using locators delimiter
	 * {@link com.jkoolcloud.tnt4j.streams.configure.sax.ConfigParserHandler#LOC_DELIM} field value.
	 * <p>
	 * But if locators are some complex expressions like XPath functions, it may be better to deny this feature for a
	 * parser to correctly load locator expression.
	 *
	 * @return flag indicating if parser supports delimited locators
	 */
	public boolean canHaveDelimitedLocators() {
		return true;
	}

	/**
	 * Adds reference to specified entity object being used by this parser.
	 *
	 * @param refObject
	 *            entity object to reference
	 * @throws IllegalStateException
	 *             if referenced object can't be linked to parser
	 */
	public abstract void addReference(Object refObject) throws IllegalStateException;

	/**
	 * Returns types of RAW activity data entries.
	 *
	 * @return types of RAW activity data entries
	 */
	protected abstract String[] getActivityDataType();

	/**
	 * Returns default data type to be used by parser bound fields.
	 *
	 * @return default data type to be used by parser bound fields
	 */
	public ActivityFieldDataType getDefaultDataType() {
		return defaultDataType;
	}

	/**
	 * Sets default data type to be used by parser bound fields/locators.
	 * 
	 * @param defaultDataType
	 *            default data type to be used by parser bound fields/locators
	 */
	public void setDefaultDataType(ActivityFieldDataType defaultDataType) {
		this.defaultDataType = defaultDataType;
	}

	/**
	 * Returns default "empty as null" attribute value for all fields/locators of this parser.
	 * 
	 * @return default attribute flag value
	 */
	public boolean isDefaultEmptyAsNull() {
		return defaultEmptyAsNull;
	}

	/**
	 * Sets default "empty as null" attribute value for all fields/locators of this parser.
	 *
	 * @param defaultEmptyAsNull
	 *            default attribute flag value
	 */
	public void setDefaultEmptyAsNull(boolean defaultEmptyAsNull) {
		this.defaultEmptyAsNull = defaultEmptyAsNull;
	}

	/**
	 * Base interface of activity data parsing context, providing all related data and references used by parsers to
	 * resolve activity field values.
	 */
	protected interface ActivityParserContext {
		/**
		 * Returns instance of stream providing activity data.
		 *
		 * @return stream providing activity data
		 */
		TNTInputStream<?, ?> getStream();

		/**
		 * Returns instance of parser currently parsing activity data.
		 * 
		 * @return parser currently parsing activity data
		 */
		ActivityParser getParser();

		/**
		 * Returns resolved activity entity data.
		 *
		 * @return resolved activity entity data
		 */
		ActivityInfo getActivity();

		/**
		 * Sets currently used stacked parser reference.
		 *
		 * @param pRef
		 *            currently used stacked parser reference
		 * 
		 * @return this context instance
		 */
		ActivityParserContext setParserRef(ActivityField.FieldParserReference pRef);

		/**
		 * Gets currently used stacked parser reference.
		 * 
		 * @return currently used stacked parser reference
		 */
		ActivityField.FieldParserReference getParserRef();

		/**
		 * Sets activity entity metadata map.
		 * 
		 * @param metaMap
		 *            activity entity metadata map
		 * 
		 * @return this context instance
		 */
		ActivityParserContext setMetadata(Map<String, ?> metaMap);

		/**
		 * Returns activity entity metadata map.
		 * 
		 * @return activity entity metadata map
		 */
		Map<String, ?> getMetadata();
	}
}
