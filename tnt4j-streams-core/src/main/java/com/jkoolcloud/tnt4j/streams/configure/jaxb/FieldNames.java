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

package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for FieldNames.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="FieldNames">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ApplName"/>
 *     &lt;enumeration value="ServerName"/>
 *     &lt;enumeration value="ServerIp"/>
 *     &lt;enumeration value="EventName"/>
 *     &lt;enumeration value="EventType"/>
 *     &lt;enumeration value="StartTime"/>
 *     &lt;enumeration value="EndTime"/>
 *     &lt;enumeration value="ElapsedTime"/>
 *     &lt;enumeration value="ProcessId"/>
 *     &lt;enumeration value="ThreadId"/>
 *     &lt;enumeration value="CompCode"/>
 *     &lt;enumeration value="ReasonCode"/>
 *     &lt;enumeration value="Exception"/>
 *     &lt;enumeration value="Severity"/>
 *     &lt;enumeration value="Location"/>
 *     &lt;enumeration value="Correlator"/>
 *     &lt;enumeration value="Tag"/>
 *     &lt;enumeration value="UserName"/>
 *     &lt;enumeration value="ResourceName"/>
 *     &lt;enumeration value="Message"/>
 *     &lt;enumeration value="TrackingId"/>
 *     &lt;enumeration value="MsgLength"/>
 *     &lt;enumeration value="MsgMimeType"/>
 *     &lt;enumeration value="MsgEncoding"/>
 *     &lt;enumeration value="MsgCharSet"/>
 *     &lt;enumeration value="MessageAge"/>
 *     &lt;enumeration value="Category"/>
 *     &lt;enumeration value="ParentId"/>
 *     &lt;enumeration value="Guid"/>
 *     &lt;enumeration value="TTL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "FieldNames")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public enum FieldNames {

	/**
	 * 
	 * Name of application associated with the activity.
	 * 
	 * 
	 */
	@XmlEnumValue("ApplName")
	APPL_NAME("ApplName"),

	/**
	 * 
	 * Host name of server where activity occurred.
	 * 
	 * 
	 */
	@XmlEnumValue("ServerName")
	SERVER_NAME("ServerName"),

	/**
	 * 
	 * IP Address of server where activity occurred.
	 * 
	 * 
	 */
	@XmlEnumValue("ServerIp")
	SERVER_IP("ServerIp"),

	/**
	 * 
	 * String identifying activity/operation/event/method.
	 * 
	 * 
	 */
	@XmlEnumValue("EventName")
	EVENT_NAME("EventName"),

	/**
	 * 
	 * Type of activity - Value must match values in OpType enumeration.
	 * 
	 * 
	 */
	@XmlEnumValue("EventType")
	EVENT_TYPE("EventType"),

	/**
	 * 
	 * Start time of the activity as either a date/time or a timestamp.
	 * 
	 * 
	 */
	@XmlEnumValue("StartTime")
	START_TIME("StartTime"),

	/**
	 * 
	 * End time of the activity as either a date/time or a timestamp.
	 * 
	 * 
	 */
	@XmlEnumValue("EndTime")
	END_TIME("EndTime"),

	/**
	 * 
	 * Elapsed time of the activity in the specified units - default: Microseconds.
	 * 
	 * 
	 */
	@XmlEnumValue("ElapsedTime")
	ELAPSED_TIME("ElapsedTime"),

	/**
	 * 
	 * Identifier of process where activity entity has occurred.
	 * 
	 * 
	 */
	@XmlEnumValue("ProcessId")
	PROCESS_ID("ProcessId"),

	/**
	 * 
	 * Identifier of thread where activity entity has occurred.
	 * 
	 * 
	 */
	@XmlEnumValue("ThreadId")
	THREAD_ID("ThreadId"),

	/**
	 * 
	 * Indicates completion status of the activity - Value must match values in OpCompCode enumeration.
	 * 
	 * 
	 */
	@XmlEnumValue("CompCode")
	COMP_CODE("CompCode"),

	/**
	 * 
	 * Numeric reason/error code associated with the activity.
	 * 
	 * 
	 */
	@XmlEnumValue("ReasonCode")
	REASON_CODE("ReasonCode"),

	/**
	 * 
	 * Error/exception message associated with the activity.
	 * 
	 * 
	 */
	@XmlEnumValue("Exception")
	EXCEPTION("Exception"),

	/**
	 * 
	 * Indicates severity of the activity - Value can either be a label in OperationSeverity enumeration or a numeric
	 * value.
	 * 
	 * 
	 */
	@XmlEnumValue("Severity")
	SEVERITY("Severity"),

	/**
	 * 
	 * String defining location activity occurred at - e.g. GPS location, source file line, etc.
	 * 
	 * 
	 */
	@XmlEnumValue("Location")
	LOCATION("Location"),

	/**
	 * 
	 * Identifier used to correlate/relate activity entries to group them into logical entities.
	 * 
	 * 
	 */
	@XmlEnumValue("Correlator")
	CORRELATOR("Correlator"),

	/**
	 * 
	 * User-defined label to associate with the activity, generally for locating activity.
	 * 
	 * 
	 */
	@XmlEnumValue("Tag")
	TAG("Tag"),

	/**
	 * 
	 * Name of user associated with the activity.
	 * 
	 * 
	 */
	@XmlEnumValue("UserName")
	USER_NAME("UserName"),

	/**
	 * 
	 * Name of resource associated with the activity.
	 * 
	 * 
	 */
	@XmlEnumValue("ResourceName")
	RESOURCE_NAME("ResourceName"),

	/**
	 * 
	 * Data to associate with the activity entity.
	 * 
	 * 
	 */
	@XmlEnumValue("Message")
	MESSAGE("Message"),

	/**
	 * 
	 * Identifier used to uniquely identify the data associated with this activity.
	 * 
	 * 
	 */
	@XmlEnumValue("TrackingId")
	TRACKING_ID("TrackingId"),

	/**
	 * 
	 * Length of activity entity message data.
	 * 
	 * 
	 */
	@XmlEnumValue("MsgLength")
	MSG_LENGTH("MsgLength"),

	/**
	 * 
	 * MIME type of activity entity message data.
	 * 
	 * 
	 */
	@XmlEnumValue("MsgMimeType")
	MSG_MIME_TYPE("MsgMimeType"),

	/**
	 * 
	 * Encoding of activity entity message data.
	 * 
	 * 
	 */
	@XmlEnumValue("MsgEncoding")
	MSG_ENCODING("MsgEncoding"),

	/**
	 * 
	 * CharSet of activity entity message data.
	 * 
	 * 
	 */
	@XmlEnumValue("MsgCharSet")
	MSG_CHAR_SET("MsgCharSet"),

	/**
	 *
	 * Age (in microseconds) of activity entity message.
	 *
	 *
	 */
	@XmlEnumValue("MessageAge")
	MESSAGE_AGE("MessageAge"),

	/**
	 * 
	 * Activity entity category name.
	 * 
	 * 
	 */
	@XmlEnumValue("Category")
	CATEGORY("Category"),

	/**
	 * 
	 * Identifier used to uniquely identify parent activity associated with this activity.
	 * 
	 * 
	 */
	@XmlEnumValue("ParentId")
	PARENT_ID("ParentId"),

	/**
	 *
	 * Identifier used to globally identify the data associated with this activity.
	 *
	 *
	 */
	@XmlEnumValue("Guid")
	GUID("Guid"),

	/**
	 *
	 * Activity entity time-to-live attribute.
	 *
	 *
	 */
	@XmlEnumValue("TTL")
	TTL("TTL");

	private final String value;

	FieldNames(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static FieldNames fromValue(String v) {
		for (FieldNames c : FieldNames.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
