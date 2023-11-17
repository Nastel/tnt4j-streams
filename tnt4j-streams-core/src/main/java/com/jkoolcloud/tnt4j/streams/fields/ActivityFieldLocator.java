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

package com.jkoolcloud.tnt4j.streams.fields;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Represents the locator rules for a specific activity data item field, defining how to locate a particular raw
 * activity data item field for its corresponding activity item value, as well any transformations and filters that are
 * necessary.
 *
 * @version $Revision: 2 $
 */
public class ActivityFieldLocator extends AbstractFieldEntity implements Cloneable {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityFieldLocator.class);

	private String type = null;
	private String locator = null;
	private ActivityFieldDataType dataType = ActivityFieldDataType.String;
	private int radix = 10;
	private String units = null;
	private String format = null;
	private String locale = null;
	private String timeZone = null;
	private Object cfgValue = null;
	private String id = null;
	private String charset = null;

	private ActivityFieldLocatorType builtInType = null;
	private ActivityFieldFormatType builtInFormat = null;
	private TimeUnit builtInUnits = null;
	private Map<Object, Object> valueMap = null;
	private Object mapCatchAll = null;

	private NumericFormatter numberParser = null;
	private TimestampFormatter timeParser = null;

	/**
	 * Constructs a new activity field locator for either a built-in type or a custom type.
	 *
	 * @param type
	 *            type of locator - can be one of predefined values from {@link ActivityFieldLocatorType} or a custom
	 *            type
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this value depends on locator type
	 * @throws IllegalArgumentException
	 *             if locator type is a numeric value and is not a positive number
	 */
	public ActivityFieldLocator(String type, String locator) {
		this(type, locator, null);
	}

	/**
	 * Constructs a new activity field locator for a built-in type.
	 *
	 * @param type
	 *            type of locator
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this value depends on locator type
	 * @throws IllegalArgumentException
	 *             if locator type is a numeric value and is not a positive number
	 */
	public ActivityFieldLocator(ActivityFieldLocatorType type, String locator) {
		this(type, locator, null);
	}

	/**
	 * Constructs a new activity field locator for either a built-in type or a custom type.
	 *
	 * @param type
	 *            type of locator - can be one of predefined values from {@link ActivityFieldLocatorType} or a custom
	 *            type
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this value depends on locator type
	 * @param dataType
	 *            the data type for raw data field
	 * @throws IllegalArgumentException
	 *             if locator type is a numeric value and is not a positive number
	 */
	public ActivityFieldLocator(String type, String locator, ActivityFieldDataType dataType) {
		this.type = type;
		this.locator = locator;
		try {
			builtInType = ActivityFieldLocatorType.valueOf(this.type);
		} catch (Exception e) {
		}
		setDataType(dataType);

		validateLocator();
	}

	/**
	 * Constructs a new activity field locator for a built-in type.
	 *
	 * @param type
	 *            type of locator
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this value depends on locator type
	 * @param dataType
	 *            the data type for raw data field
	 * @throws IllegalArgumentException
	 *             if locator type is a numeric value and is not a positive number
	 */
	public ActivityFieldLocator(ActivityFieldLocatorType type, String locator, ActivityFieldDataType dataType) {
		this.type = type.name();
		this.locator = locator;
		this.builtInType = type;
		setDataType(dataType);

		validateLocator();
	}

	/**
	 * Constructs a new activity field locator that simply uses the specified value as the value for this locator.
	 *
	 * @param value
	 *            constant value for locator
	 */
	public ActivityFieldLocator(Object value) {
		this.cfgValue = value;
	}

	/**
	 * Constructs a new "hidden" activity field locator that is used to format final field value made from multiple
	 * locators values.
	 */
	ActivityFieldLocator() {
	}

	private void validateLocator() {
		if (builtInType == null) {
			builtInType = StringUtils.isNumeric(locator) ? ActivityFieldLocatorType.Index
					: ActivityFieldLocatorType.Label;

			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityFieldLocator.setting.default.locator.type", locator, builtInType);
		} else {
			if (builtInType.getDataType() == Integer.class) {
				try {
					int loc = Integer.parseInt(locator);
					if (loc < 0) {
						throw new IllegalArgumentException(
								StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
										"ActivityFieldLocator.numeric.locator.positive"));
					}
				} catch (NumberFormatException exc) {
					throw new IllegalArgumentException(
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"ActivityFieldLocator.invalid.numeric.locator", locator));
				}
			} else if (builtInType.getDataType() == IntRange.class) {
				try {
					IntRange.getRange(locator, true);
				} catch (Exception exc) {
					throw new IllegalArgumentException(
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"ActivityFieldLocator.invalid.range.locator", locator));
				}
			}
		}
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Gets the type of this locator that indicates how to interpret the locator to find the value in the raw activity
	 * data. This value can be one of the predefined types, or it can be a custom type.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field locator is always a specific type.
	 *
	 * @return the label representing the type of locator
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the enumeration value for this locator if it implements one of the built-in locator types.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field locator is always a specific type.
	 *
	 * @return the builtInType built-in locator type, or {@code null} if this locator is a custom one.
	 */
	public ActivityFieldLocatorType getBuiltInType() {
		return builtInType;
	}

	/**
	 * Checks if this locator type is equal to anny of provided locator <tt>types</tt>.
	 *
	 * @param types
	 *            locator types to check against
	 * @return {@code true} if any of provided locator types are equal to type of this locator
	 */
	public boolean isOfType(ActivityFieldLocatorType... types) {
		return Utils.isOneOf(builtInType, types);
	}

	/**
	 * Gets the locator to find the value of this field in the raw activity data. This is generally a numeric position
	 * or a string label.
	 *
	 * @return the locator for data value
	 */
	public String getLocator() {
		return locator;
	}

	/**
	 * Checks if fields <tt>locator key</tt> and <tt>constant value</tt> are empty.
	 *
	 * @return {@code true} if <tt>locator key</tt> and <tt>constant value</tt> are empty
	 */
	public boolean isEmpty() {
		return StringUtils.isEmpty(locator) && (cfgValue == null || StringUtils.isEmpty(Utils.toString(cfgValue)));
	}

	/**
	 * Get the radix that raw data field values are interpreted in. Only relevant for numeric fields and will be ignored
	 * by those fields to which it does not apply.
	 *
	 * @return radix for field values
	 */
	public int getRadix() {
		return radix;
	}

	/**
	 * Set the radix used to interpret the raw data field values. Only relevant for numeric fields and will be ignored
	 * by those fields to which it does not apply.
	 *
	 * @param radix
	 *            radix of field values
	 */
	public void setRadix(int radix) {
		this.radix = radix;
	}

	/**
	 * Gets the data type indicating how to treat the raw data field value.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field value is always a specific data type.
	 *
	 * @return the data type for raw data field
	 */
	public ActivityFieldDataType getDataType() {
		return dataType;
	}

	/**
	 * Sets the data type indicating how to treat the raw data field value.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field value is always a specific data type.
	 *
	 * @param dataType
	 *            the data type for raw data field
	 */
	public void setDataType(ActivityFieldDataType dataType) {
		if (dataType == null) {
			if (builtInType == ActivityFieldLocatorType.Activity || builtInType == ActivityFieldLocatorType.Cache) {
				this.dataType = ActivityFieldDataType.AsInput;
			} else {
				this.dataType = ActivityFieldDataType.String;
			}
		} else {
			this.dataType = dataType;
		}
	}

	/**
	 * Gets the units represented by the raw data field value. This value can be one of the predefined units, or it can
	 * be a custom unit type.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields.
	 *
	 * @return the units the raw data value represents
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * Gets the built-in time units enumerator matching defined locator units.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the built-in time units enumerator, or {@code null} if this units specification is a custom one.
	 */
	public TimeUnit getBuiltInUnits() {
		return builtInUnits;
	}

	/**
	 * Gets the built-in time units enumerator matching defined locator units.
	 * <p>
	 * If locator has no built-in units defined, then parameter defined {@code defaultUnits} is returned.
	 * 
	 * @param defaultUnits
	 *            default time units
	 * @return the locator built-in time units enumerator, or default units if locator has has no built-in units defined
	 */
	public TimeUnit getBuiltInUnits(TimeUnit defaultUnits) {
		return builtInUnits == null ? defaultUnits : builtInUnits;
	}

	/**
	 * Returns locator instance defined time units.
	 * 
	 * @param loc
	 *            locator instance to get time units
	 * @param defaultUnits
	 *            default time units
	 * @return locator defined time units, or default units if locator is {@code null} or has has no units defined
	 */
	public static TimeUnit getLocatorUnits(ActivityFieldLocator loc, TimeUnit defaultUnits) {
		return loc == null ? defaultUnits : loc.getBuiltInUnits(defaultUnits);
	}

	/**
	 * Sets the units represented by the raw data field value. This value can be one of the predefined units, or it can
	 * be a custom unit type.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @param units
	 *            the units the raw data value represents
	 */
	public void setUnits(String units) {
		this.units = units;
		this.builtInUnits = null;

		try {
			if (StringUtils.isNotEmpty(units)) {
				builtInUnits = TimeUnit.valueOf(units.toUpperCase());
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Gets the format string defining how to interpret the raw data field value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the format string for interpreting raw data value
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Gets locale representation string used by formatter.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the locale representation string used by formatter.
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * Gets the enumeration value for this locator's format if it implements one of the built-in format types.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the builtInFormat built-in format type, or {@code null} if this format is either a format string, or a
	 *         custom one.
	 */
	public ActivityFieldFormatType getBuiltInFormat() {
		return builtInFormat;
	}

	/**
	 * Sets the format string defining how to interpret the raw data field value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 * <p>
	 * Format for numeric values also can be one of number types enumerators: {@code "integer"},
	 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
	 *
	 * @param format
	 *            the format string for interpreting raw data value
	 * @param locale
	 *            locale for formatter to use
	 */
	public void setFormat(String format, String locale) {
		this.format = format;
		this.locale = locale;

		this.builtInFormat = null;
		try {
			builtInFormat = ActivityFieldFormatType.valueOf(this.format);
		} catch (Exception e) {
		}
		this.numberParser = null;
		this.timeParser = null;
	}

	/**
	 * Gets the time zone ID that the date/time string is assumed to be in when parsed.
	 *
	 * @return time zone ID
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * Sets the time zone ID that the date/time string is assumed to represent.
	 *
	 * @param timeZone
	 *            the timeZone to set
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Gets the charset name for binary data to be used when converting to string.
	 *
	 * @return charset name
	 */
	public String getCharset() {
		return charset;
	}

	/**
	 * Sets the charset name for binary data to be used when converting to string.
	 *
	 * @param charset
	 *            the charset name to set
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * Gets field locator identifier.
	 *
	 * @return field locator identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets field locator identifier.
	 *
	 * @param id
	 *            field locator identifier
	 */
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return id;
	}

	/**
	 * Adds a mapping to translate a raw data value to the corresponding converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @param target
	 *            value to translate raw value to
	 * @return instance of this locator object
	 */
	public ActivityFieldLocator addValueMap(String source, String target) {
		return addValueMap(source, target, null);
	}

	/**
	 * Adds a mapping to translate a raw data value to the corresponding converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @param target
	 *            value to translate raw value to
	 * @param mapType
	 *            type of values mapping
	 * @return instance of this locator object
	 */
	public ActivityFieldLocator addValueMap(String source, String target, ActivityFieldMappingType mapType) {
		if (StringUtils.isEmpty(source)) {
			mapCatchAll = target;
		} else {
			if (valueMap == null) {
				valueMap = new ValueMap<>();
			}
			try {
				if (mapType == null) {
					mapType = ActivityFieldMappingType.Value;
				}

				switch (mapType) {
				case Range:
					valueMap.put(DoubleRange.getRange(source), target);
					break;
				case Calc:
					valueMap.put(getCalcKey(source), target);
					break;
				default:
					valueMap.put(source, target);
				}
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityFieldLocator.mapping.add.error", source, target, mapType);
			}
		}

		return this;
	}

	/**
	 * Translates the specified raw data value to its corresponding converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @return converted value
	 */
	protected Object getMappedValue(Object source) {
		if (valueMap == null && mapCatchAll == null) {
			return source;
		}
		Object target = null;
		if (source == null) {
			target = mapCatchAll;
		} else {
			String srcString = Utils.toString(source);
			if (valueMap != null) {
				target = valueMap.get(srcString);
			}
			if (target == null) {
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityFieldLocator.mapped.default", type);
				target = mapCatchAll == null ? source : mapCatchAll;
			}
		}
		LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityFieldLocator.mapped.result", source, target, type);
		return target;
	}

	/**
	 * Formats the specified value based on the locator's formatting properties.
	 *
	 * @param value
	 *            value to format
	 * @return value formatted based on locator definition
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 */
	public Object formatValue(Object value) throws ParseException {
		if (cfgValue != null) {
			return cfgValue;
		}

		if (value != null) {
			switch (dataType) {
			case Generic:
				value = getPredictedValue(value);
				break;
			case String:
				value = formatStringValue(value);
				break;
			case Number:
				value = formatNumericValue(value);
				break;
			case Binary:
				value = formatBinaryValue(value);
				break;
			case DateTime:
			case Timestamp:
				value = formatDateValue(value);
				break;
			case AsInput:
			default:
				break;
			}
		}

		value = getMappedValue(value);

		return value;
	}

	private Object getPredictedValue(Object value) {
		// nothing to predict of
		if (value == null) {
			return null;
		}

		// is it a number
		String prevPattern = format;
		try {
			if (StringUtils.isEmpty(format)) {
				setFormat(NumericFormatter.FormatterContext.ANY, locale);
			}
			Object pValue = formatNumericValue(value);
			dataType = ActivityFieldDataType.Number;
			return pValue;
		} catch (ParseException exc) {
			setFormat(prevPattern, locale);
		}

		// is it a datetime/timestamp
		try {
			Object pValue = formatDateValue(value);
			dataType = value instanceof Number || NumberUtils.isDigits(Utils.toString(value))
					? ActivityFieldDataType.Timestamp : ActivityFieldDataType.DateTime;
			return pValue;
		} catch (ParseException exc) {
		}

		// is it a binary
		if (value instanceof byte[]) {
			dataType = ActivityFieldDataType.Binary;
			return value;
		}

		// is it a string
		if (value instanceof CharSequence) {
			String pValue = formatStringValue(value);
			dataType = ActivityFieldDataType.String;
			return pValue;
		}

		// leave it as is eventually
		dataType = ActivityFieldDataType.AsInput;
		return value;
	}

	/**
	 * Formats field value as {@link String} based on the definition of the field locator attributes: {@code format},
	 * {@code charset}, {@code locale}, {@code timezone}.
	 * <p>
	 * If raw field value is of type {@code byte[]}, formatting is done using {@code format} attribute.
	 * {@link ActivityFieldFormatType} formats processed as:
	 * <ul>
	 * <li>{@link ActivityFieldFormatType#base64Binary} - {@link Utils#base64EncodeStr(byte[])}</li>
	 * <li>{@link ActivityFieldFormatType#hexBinary} - {@link Utils#encodeHex(byte[])}</li>
	 * <li>{@link ActivityFieldFormatType#string} - {@link Utils#getString(byte[], String)}</li>
	 * <li>{@link ActivityFieldFormatType#bytes} - {@link Utils#toHexString(byte[])}</li>
	 * </ul>
	 * <p>
	 * If raw field value is of type {@link com.jkoolcloud.tnt4j.core.UsecTimestamp}, formatting is done using
	 * {@code format} and {@code timezone} attributes.
	 * <p>
	 * If raw field value is of type {@link Date}, formatting is done using {@code format}, {@code locale} and
	 * {@code timezone} attributes.
	 * <p>
	 * If raw field value is of type {@link Number}, formatting is done using {@code format} and {@code locale}
	 * attributes.
	 * <p>
	 * If raw field value is of type {@link String}, formatting is done using {@code format} and {@code charset}
	 * attributes. {@link ActivityFieldFormatType} formats processed as:
	 * <ul>
	 * <li>{@link ActivityFieldFormatType#base64Binary} - {@link Utils#base64Decode(String, String)}</li>
	 * <li>performs simple cast to {@link String} in all other cases</li>
	 * </ul>
	 * <p>
	 * In all other cases raw field value conversion to string is performed using {@link Utils#toString(Object)} method.
	 *
	 * @param value
	 *            raw field value
	 * @return formatted field value as {@link String} based on locator defined format, or raw value if no matching
	 *         format is defined or value is not {@code byte[]}.
	 *
	 * @see ActivityFieldFormatType
	 * @see com.jkoolcloud.tnt4j.core.UsecTimestamp#toString(String, String)
	 * @see com.jkoolcloud.tnt4j.streams.utils.TimestampFormatter#format(String, Object, String, String)
	 * @see com.jkoolcloud.tnt4j.streams.utils.NumericFormatter#toString(String, Object, String)
	 * @see Utils#toString(Object)
	 */
	protected String formatStringValue(Object value) {
		if (value instanceof byte[]) {
			if (builtInFormat == ActivityFieldFormatType.base64Binary) {
				return Utils.base64EncodeStr((byte[]) value);
			} else if (builtInFormat == ActivityFieldFormatType.hexBinary) {
				return Utils.encodeHex((byte[]) value);
			} else if (builtInFormat == ActivityFieldFormatType.bytes) {
				return Utils.toHexString((byte[]) value);
			} else { // if (builtInFormat == ActivityFieldFormatType.string) {
				return Utils.getString((byte[]) value, charset);
			}
		} else if (value instanceof UsecTimestamp) {
			return ((UsecTimestamp) value).toString(format, timeZone);
		} else if (value instanceof Date) {
			return TimestampFormatter.format(format, value, locale, timeZone);
		} else if (value instanceof Number) {
			return NumericFormatter.toString(format, value, locale);
		} else if (value instanceof String) {
			if (builtInFormat == ActivityFieldFormatType.base64Binary) {
				return Utils.base64Decode((String) value, charset);
			} else {
				return (String) value;
			}
		} else {
			return Utils.toString(value);
		}
	}

	/**
	 * Formats field value as {@code byte[]} based on the definition of the field locator format.
	 * <p>
	 * Locator defined formats processed as:
	 * <ul>
	 * <li>{@link ActivityFieldFormatType#base64Binary} - {@link Utils#base64Decode(String)}</li>
	 * <li>{@link ActivityFieldFormatType#hexBinary} - {@link Utils#decodeHex(String)}</li>
	 * <li>{@link ActivityFieldFormatType#string} - {@link String#getBytes()}</li>
	 * </ul>
	 *
	 * @param value
	 *            raw field value
	 * @return formatted field value as {@code byte[]} based on locator defined format, or raw value if no matching
	 *         format is defined.
	 *
	 * @see ActivityFieldFormatType
	 */
	protected Object formatBinaryValue(Object value) {
		if (value instanceof String) {
			if (builtInFormat == ActivityFieldFormatType.base64Binary) {
				value = Utils.base64Decode(String.valueOf(value));
			} else if (builtInFormat == ActivityFieldFormatType.hexBinary) {
				value = Utils.decodeHex(String.valueOf(value));
			} else { // if (builtInFormat == ActivityFieldFormatType.string) {
				value = String.valueOf(value).getBytes();
			}
		}

		return value;
	}

	/**
	 * Formats the value for the specified numeric field based on the definition of the field locator.
	 *
	 * @param value
	 *            raw field value
	 * @return formatted field value as {@link Number}
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 */
	protected Number formatNumericValue(Object value) throws ParseException {
		if (numberParser == null) {
			numberParser = new NumericFormatter(format, locale);
		}

		Object val = value;

		if (value instanceof String) {
			val = value.toString().trim();

			// TODO: make empty value handling configurable: null, exception, default
			if (StringUtils.isEmpty(val.toString())) {
				return null;
			}
		}

		return numberParser.parse(val);
	}

	/**
	 * Formats the value for the specified date/time field based on the definition of the field locator.
	 *
	 * @param value
	 *            raw field value
	 * @return formatted field value as {@link UsecTimestamp}
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 */
	protected UsecTimestamp formatDateValue(Object value) throws ParseException {
		UsecTimestamp timestamp = TimestampFormatter.getTimestamp(value);
		if (timestamp != null) {
			return timestamp;
		}
		if (timeParser == null) {
			timeParser = dataType == ActivityFieldDataType.Timestamp || dataType == ActivityFieldDataType.Number
					? new TimestampFormatter(getBuiltInUnits(TimeUnit.MILLISECONDS))
					: new TimestampFormatter(format, timeZone, locale);
		}
		return timeParser.parse(value);
	}

	/**
	 * Returns string representing field locator by type and locator key.
	 *
	 * @return a string representing field locator.
	 */
	@Override
	public String toString() {
		return String.format("%s::%s", type, locator); // NON-NLS
	}

	/**
	 * Makes clone copy of activity field locator.
	 *
	 * @return clone copy of activity field locator
	 */
	@Override
	public ActivityFieldLocator clone() {
		try {
			ActivityFieldLocator cafl = (ActivityFieldLocator) super.clone();
			cafl.type = type;
			cafl.locator = locator;
			cafl.dataType = dataType;
			cafl.radix = radix;
			cafl.units = units;
			cafl.format = format;
			cafl.locale = locale;
			cafl.timeZone = timeZone;
			cafl.cfgValue = cfgValue;
			cafl.requiredVal = requiredVal;
			cafl.id = id;
			cafl.charset = charset;

			cafl.builtInType = builtInType;
			cafl.builtInFormat = builtInFormat;
			cafl.builtInUnits = builtInUnits;
			cafl.valueMap = valueMap;
			cafl.mapCatchAll = mapCatchAll;
			cafl.transformations = transformations;
			cafl.filter = filter;

			return cafl;
		} catch (CloneNotSupportedException exc) {
		}

		return null;
	}

	private static Calc getCalcKey(String source) throws IllegalArgumentException {
		return new Calc(ActivityFieldMappingCalc.valueOf(source.toUpperCase()));
	}

	private static class Calc {
		private ActivityFieldMappingCalc function;

		private Calc(ActivityFieldMappingCalc functionName) {
			this.function = functionName;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Number) {
				return match(((Number) obj).doubleValue());
			}

			if (obj instanceof String) {
				try {
					return match(Double.parseDouble((String) obj));
				} catch (NumberFormatException exc) {
				}
			}

			return super.equals(obj);
		}

		private boolean match(Double num) {
			switch (function) {
			case ODD:
				return num % 2 != 0;
			case EVEN:
				return num % 2 == 0;
			default:
				return false;
			}
		}

		@Override
		public String toString() {
			return String.valueOf(function);
		}
	}

	private class ValueMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 2566002253449435488L;

		private ValueMap() {
			super();
		}

		private ValueMap(int ic) {
			super(ic);
		}

		@Override
		public V get(Object key) {
			V e = super.get(key);

			if (e == null) {
				e = getCompared(key);
			}

			return e;
		}

		private V getCompared(Object key) {
			Iterator<Map.Entry<K, V>> i = entrySet().iterator();
			if (key == null) {
				while (i.hasNext()) {
					Map.Entry<K, V> e = i.next();
					if (e.getKey() == null) {
						return e.getValue();
					}
				}
			} else {
				while (i.hasNext()) {
					Map.Entry<K, V> e = i.next();
					if (e.getKey() != null && e.getKey().equals(key)) {
						return e.getValue();
					}
				}
			}
			return null;
		}
	}
}
