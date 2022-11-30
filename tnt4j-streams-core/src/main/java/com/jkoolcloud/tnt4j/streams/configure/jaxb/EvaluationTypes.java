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
 * Java class for EvaluationTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="EvaluationTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="is"/&gt;
 *     &lt;enumeration value="contains"/&gt;
 *     &lt;enumeration value="wildcard"/&gt;
 *     &lt;enumeration value="regex"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "EvaluationTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
public enum EvaluationTypes {

	/**
	 * 
	 * Value evaluation by direct comparison for equality.
	 * 
	 * 
	 */
	@XmlEnumValue("is")
	IS("is"),

	/**
	 * 
	 * Value evaluation by checking if value contents has defined subset value.
	 * 
	 * 
	 */
	@XmlEnumValue("contains")
	CONTAINS("contains"),

	/**
	 * 
	 * Value evaluation by matching wildcard pattern.
	 * 
	 * 
	 */
	@XmlEnumValue("wildcard")
	WILDCARD("wildcard"),

	/**
	 * 
	 * Value evaluation by matching regular expression.
	 * 
	 * 
	 */
	@XmlEnumValue("regex")
	REGEX("regex");

	private final String value;

	EvaluationTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static EvaluationTypes fromValue(String v) {
		for (EvaluationTypes c : EvaluationTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
