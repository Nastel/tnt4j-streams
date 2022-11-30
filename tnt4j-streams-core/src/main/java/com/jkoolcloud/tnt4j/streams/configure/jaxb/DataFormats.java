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
 * Java class for DataFormats.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="DataFormats"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="base64Binary"/&gt;
 *     &lt;enumeration value="hexBinary"/&gt;
 *     &lt;enumeration value="string"/&gt;
 *     &lt;enumeration value="[DateTime-specification]"/&gt;
 *     &lt;enumeration value="[Decimal-specification]"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "DataFormats")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-02-22T04:46:33+02:00", comments = "JAXB RI v2.2.4-2")
public enum DataFormats {

	/**
	 * 
	 * Data is binary, in base64 encoding
	 * 
	 * 
	 */
	@XmlEnumValue("base64Binary")
	BASE_64_BINARY("base64Binary"),

	/**
	 * 
	 * Data is binary, represented as a hex string
	 * 
	 * 
	 */
	@XmlEnumValue("hexBinary")
	HEX_BINARY("hexBinary"),

	/**
	 * 
	 * Data is a string of characters
	 * 
	 * 
	 */
	@XmlEnumValue("string")
	STRING("string"),

	/**
	 * 
	 * Data is a date/time expression in the specified format (see java.text.SimpleDateFormat)
	 * 
	 * 
	 */
	@XmlEnumValue("[DateTime-specification]")
	DATE_TIME_SPECIFICATION("[DateTime-specification]"),

	/**
	 * 
	 * Data is a numeric expression in the specified format (see java.text.DecimalFormat)
	 * 
	 * 
	 */
	@XmlEnumValue("[Decimal-specification]")
	DECIMAL_SPECIFICATION("[Decimal-specification]");

	private final String value;

	DataFormats(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static DataFormats fromValue(String v) {
		for (DataFormats c : DataFormats.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
