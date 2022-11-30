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
 * Java class for ParserProperties.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="ParserProperties"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Pattern"/&gt;
 *     &lt;enumeration value="FieldDelim"/&gt;
 *     &lt;enumeration value="ValueDelim"/&gt;
 *     &lt;enumeration value="StripQuotes"/&gt;
 *     &lt;enumeration value="RequireDefault"/&gt;
 *     &lt;enumeration value="StripHeaders"/&gt;
 *     &lt;enumeration value="Namespace"/&gt;
 *     &lt;enumeration value="ReadLines"/&gt;
 *     &lt;enumeration value="LocPathDelim"/&gt;
 *     &lt;enumeration value="UseActivityDataAsMessageForUnset"/&gt;
 *     &lt;enumeration value="EntryPattern"/&gt;
 *     &lt;enumeration value="ActivityDelim"/&gt;
 *     &lt;enumeration value="NamespaceAware"/&gt;
 *     &lt;enumeration value="MatchStrategy"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ParserProperties")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
public enum ParserProperties {

	/**
	 * 
	 * Property value is a regular expression pattern.
	 * 
	 * 
	 */
	@XmlEnumValue("Pattern")
	PATTERN("Pattern"),

	/**
	 * 
	 * Property value represents the delimiter between fields in raw activity data.
	 * 
	 * 
	 */
	@XmlEnumValue("FieldDelim")
	FIELD_DELIM("FieldDelim"),

	/**
	 * 
	 * Property value represents the delimiter between the label and value for a field in raw activity data.
	 * 
	 * 
	 */
	@XmlEnumValue("ValueDelim")
	VALUE_DELIM("ValueDelim"),

	/**
	 * 
	 * Property value is "true" to remove surrounding double quotes from data value, or "false" to keep them.
	 * 
	 * 
	 */
	@XmlEnumValue("StripQuotes")
	STRIP_QUOTES("StripQuotes"),

	/**
	 * 
	 * Property indicates that all parser fields/locators by default requires to resolve non-null value.
	 * 
	 * 
	 */
	@XmlEnumValue("RequireDefault")
	REQUIRE_DEFAULT("RequireDefault"),

	/**
	 * 
	 * Property identifies whether stream should strip RAW activity data (e.g., WMQ message) headers.
	 * 
	 * 
	 */
	@XmlEnumValue("StripHeaders")
	STRIP_HEADERS("StripHeaders"),

	/**
	 * 
	 * Property to define additional XML namespace mappings.
	 * 
	 * 
	 */
	@XmlEnumValue("Namespace")
	NAMESPACE("Namespace"),

	/**
	 * 
	 * Property indicates that complete activity RAW data (e.g., JSON) package is single line. Deprecated - use
	 * "ActivityDelim" instead.
	 * 
	 * 
	 */
	@XmlEnumValue("ReadLines")
	READ_LINES("ReadLines"),

	/**
	 *
	 * Property to defile locator path delimiter for a nested structures (e.g., maps).
	 *
	 *
	 */
	@XmlEnumValue("LocPathDelim")
	LOC_PATH_DELIM("LocPathDelim"),

	/**
	 *
	 * Property indicates that activity RAW data shall be put into 'Message' field of activity entity if no custom
	 * mapping for that field is defined.
	 *
	 *
	 */
	@XmlEnumValue("UseActivityDataAsMessageForUnset")
	USE_ACTIVITY_DATA_AS_MESSAGE_FOR_UNSET("UseActivityDataAsMessageForUnset"),

	/**
	 *
	 * Property defines pattern used to to split data into name/value pairs. It should define two RegEx groups named
	 * "key" and "value" used to map data contained values to name/value pair. NOTE: this parameter takes preference on
	 * "FieldDelim" and "ValueDelim" properties.
	 *
	 *
	 */
	@XmlEnumValue("EntryPattern")
	ENTRY_PATTERN("EntryPattern"),

	/**
	 *
	 * Property indicates the delimiter of activity RAW data entries. Can be "EOL" (end-of-line), or "EOF"
	 * (end-of-file/stream).
	 *
	 *
	 */
	@XmlEnumValue("ActivityDelim")
	ACTIVITY_DELIM("ActivityDelim"),

	/**
	 *
	 * Property indicates that parser has to provide support for XML namespaces.
	 *
	 *
	 */
	@XmlEnumValue("NamespaceAware")
	NAMESPACE_AWARE("NamespaceAware"),

	/**
	 *
	 * Property defines strategy used to verify if RegEx 'pattern' created 'Matcher' matches input data string. Can be
	 * "MATCH" (pattern should match complete input string), or "FIND" (pattern has to match subsequence within input
	 * string).
	 *
	 *
	 */
	@XmlEnumValue("MatchStrategy")
	MATCH_STRATEGY("MatchStrategy");

	private final String value;

	ParserProperties(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ParserProperties fromValue(String v) {
		for (ParserProperties c : ParserProperties.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
