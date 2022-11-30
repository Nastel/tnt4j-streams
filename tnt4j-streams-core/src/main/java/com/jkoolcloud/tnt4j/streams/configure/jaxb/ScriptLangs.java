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
 * Java class for ScriptLangs.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="ScriptLangs"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="javascript"/&gt;
 *     &lt;enumeration value="groovy"/&gt;
 *     &lt;enumeration value="xpath"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ScriptLangs")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public enum ScriptLangs {

	/**
	 * 
	 * JavaScript language used to script.
	 * 
	 * 
	 */
	@XmlEnumValue("javascript")
	JAVASCRIPT("javascript"),

	/**
	 * 
	 * Groovy language used to script.
	 * 
	 * 
	 */
	@XmlEnumValue("groovy")
	GROOVY("groovy"),

	/**
	 * 
	 * XPath syntax and functions used to script.
	 * 
	 * 
	 */
	@XmlEnumValue("xpath")
	XPATH("xpath");

	private final String value;

	ScriptLangs(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ScriptLangs fromValue(String v) {
		for (ScriptLangs c : ScriptLangs.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
