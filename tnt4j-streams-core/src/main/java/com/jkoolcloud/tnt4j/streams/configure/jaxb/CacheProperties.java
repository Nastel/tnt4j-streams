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
 * <p>
 * Java class for CacheProperties.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="CacheProperties">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="MaxSize"/>
 *     &lt;enumeration value="ExpireDuration"/>
 *     &lt;enumeration value="Persisted"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CacheProperties")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
public enum CacheProperties {

	/**
	 * 
	 * Cache maximum capacity value.
	 * 
	 * 
	 */
	@XmlEnumValue("MaxSize")
	MAX_SIZE("MaxSize"),

	/**
	 * 
	 * Cache entry expiration duration in minutes.
	 * 
	 * 
	 */
	@XmlEnumValue("ExpireDuration")
	EXPIRE_DURATION("ExpireDuration"),

	/**
	 * 
	 * Flag indicating cache contents has to be persisted to file on close and loaded on initialization.
	 * 
	 * 
	 */
	@XmlEnumValue("Persisted")
	PERSISTED("Persisted");

	private final String value;

	CacheProperties(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static CacheProperties fromValue(String v) {
		for (CacheProperties c : CacheProperties.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
