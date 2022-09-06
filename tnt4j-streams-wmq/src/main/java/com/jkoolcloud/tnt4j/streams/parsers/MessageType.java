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

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Provides list of valid Message Types.
 *
 * @version $Revision: 1 $
 */
public enum MessageType {
	/**
	 * Unknown message type.
	 */
	UNKNOWN(0),

	/**
	 * Request message type.
	 */
	REQUEST(1),

	/**
	 * Reply message type.
	 */
	REPLY(2),

	/**
	 * Report message type.
	 */
	REPORT(4),

	/**
	 * Datagram message type.
	 */
	DATAGRAM(8),

	/**
	 * Acknowledgement message type.
	 */
	ACKNOWLEDGEMENT(12);

	MessageType(int enumValue) {
		this.enumValue = enumValue;
	}

	/**
	 * Gets numeric value of message type representation.
	 *
	 * @return message type member value
	 */
	public int value() {
		return enumValue;
	}

	/**
	 * Converts the specified value to a member of the enumeration.
	 *
	 * @param value
	 *            enumeration value to convert
	 * @return enumeration member
	 * @throws IllegalArgumentException
	 *             if there is no member of the enumeration with the specified value
	 */
	public static MessageType valueOf(int value) {
		if (value == UNKNOWN.value()) {
			return UNKNOWN;
		}
		if (value == REQUEST.value()) {
			return REQUEST;
		}
		if (value == REPLY.value()) {
			return REPLY;
		}
		if (value == REPORT.value()) {
			return REPORT;
		}
		if (value == DATAGRAM.value()) {
			return DATAGRAM;
		}
		if (value == ACKNOWLEDGEMENT.value()) {
			return ACKNOWLEDGEMENT;
		}
		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"MessageType.illegal.num.value", value, MessageType.class.getSimpleName()));
	}

	private final int enumValue;

	/**
	 * Converts the specified object to a member of the enumeration.
	 *
	 * @param value
	 *            object to convert
	 * @return enumeration member
	 * @throws IllegalArgumentException
	 *             if value is {@code null} or object cannot be matched to a member of the enumeration
	 */
	public static MessageType valueOf(Object value) {
		if (value == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "MessageType.object.null"));
		}
		if (value instanceof Number) {
			return valueOf(((Number) value).intValue());
		}
		if (value instanceof String) {
			return valueOf(value.toString());
		}
		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"MessageType.illegal.obj.value", value.getClass().getName(), MessageType.class.getSimpleName()));
	}
}
