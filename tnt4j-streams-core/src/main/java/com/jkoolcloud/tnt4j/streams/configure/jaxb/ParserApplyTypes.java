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
 * Java class for ParserApplyTypes.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 *
 * <pre>
 * &lt;simpleType name="ParserApplyTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Field"/&gt;
 *     &lt;enumeration value="Activity"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 *
 */
@XmlType(name = "ParserApplyTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
public enum ParserApplyTypes {

	/**
	 *
	 * Apply stacked parser on field resolved value.
	 *
	 *
	 */
	@XmlEnumValue("Field")
	FIELD("Field"),

	/**
	 *
	 * Apply stacked parser on activity entity stored value.
	 *
	 *
	 */
	@XmlEnumValue("Activity")
	@Deprecated
	ACTIVITY("Activity");

	private final String value;

	ParserApplyTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ParserApplyTypes fromValue(String v) {
		for (ParserApplyTypes c : ParserApplyTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}
