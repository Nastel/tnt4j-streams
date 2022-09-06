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
 * Java class for ResourceReferenceType.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="ResourceReferenceType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ValuesMap"/>
 *     &lt;enumeration value="Parser"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ResourceReferenceType")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public enum ResourceReferenceType {

	/**
	 * 
	 * Resource defines values mappings.
	 * 
	 * 
	 */
	@XmlEnumValue("ValuesMap")
	VALUES_MAP("ValuesMap"),

	/**
	 *
	 * Resource defines parser configuration.
	 *
	 * 
	 */
	@XmlEnumValue("Parser")
	PARSER("Parser");

	private final String value;

	ResourceReferenceType(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ResourceReferenceType fromValue(String v) {
		for (ResourceReferenceType c : ResourceReferenceType.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
