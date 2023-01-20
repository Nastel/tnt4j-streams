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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Defines the set of item fields supported by jKool Database Access API.
 * <p>
 * Fields should be specified using the defined label instead of the enumeration name.
 *
 * @version $Revision: 1 $
 */
public enum StreamFieldType {
	/**
	 * Name of application associated with the activity.
	 */
	ApplName(String.class),

	/**
	 * Host name of server to associate with activity.
	 */
	ServerName(String.class),

	/**
	 * IP Address of server to associate with activity.
	 */
	ServerIp(String.class),

	/**
	 * Name to assign to activity entry. Examples are operation, method, API call, event, etc.
	 */
	EventName(String.class),

	/**
	 * Type of activity - value must match values in {@link com.jkoolcloud.tnt4j.core.OpType} enumeration.
	 */
	EventType(Enum.class),

	/**
	 * Status of activity - value must match values in {@link com.jkoolcloud.tnt4j.core.ActivityStatus} enumeration.
	 */
	EventStatus(Enum.class),

	/**
	 * Time action associated with activity started.
	 */
	StartTime(UsecTimestamp.class),

	/**
	 * Time action associated with activity ended.
	 */
	EndTime(UsecTimestamp.class),

	/**
	 * Elapsed time of the activity in microseconds.
	 */
	ElapsedTime(Long.class),

	/**
	 * Identifier of process where activity event has occurred.
	 */
	ProcessId(Integer.class),

	/**
	 * Identifier of thread where activity event has occurred.
	 */
	ThreadId(Integer.class),

	/**
	 * Indicates completion status of the activity - value must match values in
	 * {@link com.jkoolcloud.tnt4j.core.OpCompCode} enumeration.
	 */
	CompCode(Enum.class),

	/**
	 * Numeric reason/error code associated with the activity.
	 */
	ReasonCode(Integer.class),

	/**
	 * Error/exception message associated with the activity.
	 */
	Exception(String.class),

	/**
	 * Indicates completion status of the activity - value can either be label from
	 * {@link com.jkoolcloud.tnt4j.core.OpLevel} enumeration or a numeric value.
	 */
	Severity(Enum.class),

	/**
	 * Location that activity occurred at.
	 */
	Location(String.class),

	/**
	 * Identifier used to correlate/relate activity entries to group them into logical entities.
	 */
	Correlator(String[].class),

	/**
	 * User-defined label to associate with the activity, generally for locating activity.
	 */
	Tag(String[].class),

	/**
	 * Name of user associated with the activity.
	 */
	UserName(String.class),

	/**
	 * Name of resource associated with the activity.
	 */
	ResourceName(String.class),

	/**
	 * User data to associate with the activity.
	 */
	Message(String.class),

	/**
	 * Identifier used to uniquely identify the data associated with this activity.
	 */
	TrackingId(String.class),

	/**
	 * Length of activity event message data.
	 */
	MsgLength(Integer.class),

	/**
	 * MIME type of activity event message data.
	 */
	MsgMimeType(String.class),

	/**
	 * Encoding of activity event message data.
	 */
	MsgEncoding(String.class),

	/**
	 * CharSet of activity event message data.
	 */
	MsgCharSet(String.class),

	/**
	 * Age of activity event message.
	 */
	MessageAge(Long.class),

	/**
	 * Activity event category name.
	 */
	Category(String.class),

	/**
	 * Identifier used to uniquely identify parent activity associated with this activity.
	 */
	ParentId(String.class),

	/**
	 * Identifier used to globally identify the data associated with this activity.
	 */
	Guid(String.class),

	/**
	 * Stream activity entity time-to-live attribute.
	 */
	TTL(Long.class);

	private final Class<?> dataType;

	private StreamFieldType(Class<?> type) {
		this.dataType = type;
	}

	/**
	 * Gets the data type that this field's values are represented in.
	 *
	 * @return field data type
	 */
	public Class<?> getDataType() {
		return dataType;
	}

	/**
	 * For fields that are {@link Enum}s, gets the enumeration class defining the set of possible values for the field.
	 *
	 * @return enumeration class for field, or {@code null} if this field is not an enumeration
	 * @throws IllegalArgumentException
	 *             if ordinal is not a valid enumeration value
	 */
	public Class<? extends Enum<?>> getEnumerationClass() {
		if (dataType != Enum.class) {
			return null;
		}

		switch (this) {
		case Severity:
			return OpLevel.class;
		case EventType:
			return OpType.class;
		case CompCode:
			return OpCompCode.class;
		case EventStatus:
			return ActivityStatus.class;

		default:
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamFieldType.not.defined", this));
		}
	}

	/**
	 * Checks if field type is {@link Enum}.
	 *
	 * @return {@code true} if field is of enum type, {@code false} - otherwise.
	 */
	boolean isEnumField() {
		return dataType == Enum.class;
	}

	/**
	 * For fields that are {@link Enum}s, gets the numeric value for the enumeration constant with the specified name.
	 *
	 * @param enumLabel
	 *            name of enumeration constant
	 * @return ordinal value for enumeration with specified name, or {@code -1} if this field is not an enumeration
	 * @throws IllegalArgumentException
	 *             if enumLabel is not a valid enumeration label
	 */
	public int getEnumValue(String enumLabel) {
		if (dataType != Enum.class) {
			return -1;
		}

		switch (this) {
		case Severity:
			return OpLevel.valueOf(enumLabel).ordinal();
		case EventType:
			return OpType.valueOf(enumLabel).ordinal();
		case CompCode:
			return OpCompCode.valueOf(enumLabel).ordinal();
		case EventStatus:
			return ActivityStatus.valueOf(enumLabel).ordinal();

		default:
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamFieldType.not.defined", this));
		}
	}

	/**
	 * For fields that are {@link Enum}s, gets the enumeration with the specified name.
	 *
	 * @param enumLabel
	 *            name of enumeration constant
	 * @return enumeration constant, or {@code null} if this field is not an enumeration
	 * @throws IllegalArgumentException
	 *             if ordinal is not a valid enumeration value
	 */
	public Enum<?> getEnum(String enumLabel) {
		if (dataType != Enum.class) {
			return null;
		}

		switch (this) {
		case Severity:
			return OpLevel.valueOf(enumLabel.toUpperCase());
		case EventType:
			return OpType.valueOf(enumLabel.toUpperCase());
		case CompCode:
			return OpCompCode.valueOf(enumLabel.toUpperCase());
		case EventStatus:
			return ActivityStatus.valueOf(enumLabel.toUpperCase());

		default:
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamFieldType.not.defined", this));
		}
	}

	/**
	 * For fields that are {@link Enum}s, gets the name of the enumeration constant with the specified ordinal value.
	 *
	 * @param value
	 *            value for enumeration
	 * @return enumLabel name of enumeration constant, or {@code null} if this field is not an enumeration
	 * @throws IllegalArgumentException
	 *             if ordinal is not a valid enumeration value
	 */
	public String getEnumLabel(int value) {
		if (dataType != Enum.class) {
			return null;
		}

		switch (this) {
		case Severity:
			return OpLevel.valueOf(value).name();
		case EventType:
			return OpType.valueOf(value).name();
		case CompCode:
			return OpCompCode.valueOf(value).name();
		case EventStatus:
			return ActivityStatus.valueOf(value).name();

		default:
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamFieldType.not.defined", this));
		}
	}

	/**
	 * For fields that are {@link Enum}s, gets the enumeration with the specified ordinal value.
	 *
	 * @param value
	 *            value for enumeration
	 * @return enumeration constant, or {@code null} if this field is not an enumeration
	 * @throws IllegalArgumentException
	 *             if ordinal is not a valid enumeration value
	 */
	public Enum<?> getEnum(int value) {
		if (dataType != Enum.class) {
			return null;
		}

		switch (this) {
		case Severity:
			return OpLevel.valueOf(value);
		case EventType:
			return OpType.valueOf(value);
		case CompCode:
			return OpCompCode.valueOf(value);
		case EventStatus:
			return ActivityStatus.valueOf(value);

		default:
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamFieldType.not.defined", this));
		}
	}

	/**
	 * Gets the field enumeration object based on the enumeration's ordinal value.
	 *
	 * @param ordinal
	 *            enumeration ordinal value
	 * @return field type enumeration object
	 * @throws IndexOutOfBoundsException
	 *             if ordinal value is outside the range of enumeration ordinal values
	 */
	public static StreamFieldType getType(int ordinal) {
		StreamFieldType[] enums = StreamFieldType.values();
		if (ordinal < 0 || ordinal >= enums.length) {
			throw new IndexOutOfBoundsException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "StreamFieldType.invalid.ordinal", ordinal,
					(enums.length - 1), StreamFieldType.class.getSimpleName()));
		}
		return enums[ordinal];
	}

	private static final Map<String, Object> CACHE = new HashMap<>();
	private static final Object NULL_VALUE = new Object();

	/**
	 * Gets the field enumeration object based on enumeration object name value ignoring case.
	 *
	 * @param name
	 *            name of field type
	 * @return field type enumeration object, or {@code null} if name is empty or does not match any enumeration object
	 */
	static StreamFieldType _valueOfIgnoreCase(String name) {
		if (StringUtils.isNotEmpty(name)) {
			Object sft = CACHE.get(name);

			if (sft == null) {
				StreamFieldType[] enumConstants = StreamFieldType.values();
				for (StreamFieldType enumConstant : enumConstants) {
					if (enumConstant.name().equalsIgnoreCase(name)) {
						CACHE.put(name, enumConstant);
						return enumConstant;
					}
				}

				CACHE.put(name, NULL_VALUE);
			}

			if (sft instanceof StreamFieldType) {
				return (StreamFieldType) sft;
			}
		}

		return null;
	}
}
