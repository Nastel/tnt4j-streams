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
package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for FieldLocatorTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="FieldLocatorTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="StreamProp"/&gt;
 *     &lt;enumeration value="Index"/&gt;
 *     &lt;enumeration value="Label"/&gt;
 *     &lt;enumeration value="REMatchId"/&gt;
 *     &lt;enumeration value="Range"/&gt;
 *     &lt;enumeration value="Cache"/&gt;
 *     &lt;enumeration value="Activity"/&gt;
 *     &lt;enumeration value="Expression"/&gt;
 *     &lt;enumeration value="ParserProp"/&gt;
 *     &lt;enumeration value="SystemProp"/&gt;
 *     &lt;enumeration value="EnvVariable"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "FieldLocatorTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public enum FieldLocatorTypes {

	/**
	 * 
	 * Locator value is the value for the specified stream property.
	 * 
	 * 
	 */
	@XmlEnumValue("StreamProp")
	STREAM_PROP("StreamProp"),

	/**
	 * 
	 * Locator value is the value at the specified index/offset/position. It also can be index of RegEx group.
	 * 
	 * 
	 */
	@XmlEnumValue("Index")
	INDEX("Index"),

	/**
	 * 
	 * Locator value is the value for the specified label/expression (e.g. name/value pairs like label=value, XPath
	 * expression, etc.). It also can be name of RegEx group.
	 * 
	 * 
	 */
	@XmlEnumValue("Label")
	LABEL("Label"),

	/**
	 * 
	 * Locator value is the value for the specified regular expression match group identifier: sequence position or
	 * name. Deprecated - use "Label" instead.
	 * 
	 * 
	 */
	@XmlEnumValue("REMatchId")
	@Deprecated
	RE_MATCH_ID("REMatchId"),

	/**
	 *
	 * Locator value is the range within enclosing object: r.g. characters range within string.
	 *
	 *
	 */
	@XmlEnumValue("Range")
	RANGE("Range"),

	/**
	 * 
	 * Locator value is the value for stream stored cache key.
	 * 
	 * 
	 */
	@XmlEnumValue("Cache")
	CACHE("Cache"),

	/**
	 * 
	 * Locator value is the value for streamed activity entity field name.
	 * 
	 * 
	 */
	@XmlEnumValue("Activity")
	ACTIVITY("Activity"),

	/**
	 * 
	 * Locator value is the expression applicable for parser context, e.g. regex.
	 * 
	 * 
	 */
	@XmlEnumValue("Expression")
	EXPRESSION("Expression"),

	/**
	 * 
	 * Locator value is the value for the specified parser property.
	 * 
	 * 
	 */
	@XmlEnumValue("ParserProp")
	PARSER_PROP("ParserProp"),

	/**
	 * 
	 * Locator value is the value for the specified Java System property.
	 * 
	 * 
	 */
	@XmlEnumValue("SystemProp")
	SYSTEM_PROP("SystemProp"),

	/**
	 * 
	 * Locator value is the value for the specified OS environment variable.
	 * 
	 * 
	 */
	@XmlEnumValue("EnvVariable")
	ENV_VARIABLE("EnvVariable");

	private final String value;

	FieldLocatorTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static FieldLocatorTypes fromValue(String v) {
		for (FieldLocatorTypes c : FieldLocatorTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
