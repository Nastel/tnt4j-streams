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
 * Java class for ValueResolutionPhases.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="ValueResolutionPhases"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="raw"/&gt;
 *     &lt;enumeration value="formatted"/&gt;
 *     &lt;enumeration value="aggregated"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ValueResolutionPhases")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
public enum ValueResolutionPhases {

	/**
	 * 
	 * When RAW data value is resolved.
	 * 
	 * 
	 */
	@XmlEnumValue("raw")
	RAW("raw"),

	/**
	 * 
	 * When RAW data value gets formatted.
	 * 
	 * 
	 */
	@XmlEnumValue("formatted")
	FORMATTED("formatted"),

	/**
	 * 
	 * When activity data value gets aggregated into activity entity.
	 * 
	 * 
	 */
	@XmlEnumValue("aggregated")
	AGGREGATED("aggregated");

	private final String value;

	ValueResolutionPhases(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ValueResolutionPhases fromValue(String v) {
		for (ValueResolutionPhases c : ValueResolutionPhases.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
