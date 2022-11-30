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
 * Java class for UnitsTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="UnitsTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Days"/&gt;
 *     &lt;enumeration value="Hours"/&gt;
 *     &lt;enumeration value="Minutes"/&gt;
 *     &lt;enumeration value="Seconds"/&gt;
 *     &lt;enumeration value="Milliseconds"/&gt;
 *     &lt;enumeration value="Microseconds"/&gt;
 *     &lt;enumeration value="Nanoseconds"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "UnitsTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-19T11:37:17+03:00", comments = "JAXB RI v2.2.4-2")
public enum UnitsTypes {

	@XmlEnumValue("Days")
	DAYS("Days"), @XmlEnumValue("Hours")
	HOURS("Hours"), @XmlEnumValue("Minutes")
	MINUTES("Minutes"), @XmlEnumValue("Seconds")
	SECONDS("Seconds"), @XmlEnumValue("Milliseconds")
	MILLISECONDS("Milliseconds"), @XmlEnumValue("Microseconds")
	MICROSECONDS("Microseconds"), @XmlEnumValue("Nanoseconds")
	NANOSECONDS("Nanoseconds");

	private final String value;

	UnitsTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static UnitsTypes fromValue(String v) {
		for (UnitsTypes c : UnitsTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
