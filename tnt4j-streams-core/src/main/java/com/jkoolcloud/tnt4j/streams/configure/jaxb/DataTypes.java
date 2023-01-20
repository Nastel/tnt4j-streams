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
 * Java class for DataTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="DataTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="String"/&gt;
 *     &lt;enumeration value="Binary"/&gt;
 *     &lt;enumeration value="Number"/&gt;
 *     &lt;enumeration value="DateTime"/&gt;
 *     &lt;enumeration value="Timestamp"/&gt;
 *     &lt;enumeration value="Generic"/&gt;
 *     &lt;enumeration value="AsInput"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "DataTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-02-22T04:46:33+02:00", comments = "JAXB RI v2.2.4-2")
public enum DataTypes {

	/**
	 * 
	 * Field value is a character string.
	 * 
	 * 
	 */
	@XmlEnumValue("String")
	STRING("String"),

	/**
	 * 
	 * Field value is a generic sequence of bytes.
	 * 
	 * 
	 */
	@XmlEnumValue("Binary")
	BINARY("Binary"),

	/**
	 * 
	 * Field value is a numeric value.
	 * 
	 * 
	 */
	@XmlEnumValue("Number")
	NUMBER("Number"),

	/**
	 * 
	 * Field value is a date, time, or date/time expression with a specific format.
	 * 
	 * 
	 */
	@XmlEnumValue("DateTime")
	DATE_TIME("DateTime"),

	/**
	 * 
	 * Field value is a numeric value representing a date/time in the specified resolution.
	 * 
	 * 
	 */
	@XmlEnumValue("Timestamp")
	TIMESTAMP("Timestamp"),

	/**
	 *
	 * Field value is generic and streams should try to make real value out of it: boolean, number, timestamp, date,
	 * string.
	 *
	 *
	 */
	@XmlEnumValue("Generic")
	GENERIC("Generic"),

	/**
	 *
	 * Field value has same type as it is received from input without changes.
	 *
	 *
	 */
	@XmlEnumValue("AsInput")
	AS_INPUT("AsInput");

	private final String value;

	DataTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static DataTypes fromValue(String v) {
		for (DataTypes c : DataTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
