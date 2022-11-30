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
 * Java class for AggregationTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="AggregationTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Merge"/&gt;
 *     &lt;enumeration value="Join"/&gt;
 *     &lt;enumeration value="Relate"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AggregationTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
public enum AggregationTypes {

	/**
	 * 
	 * Merge stacked parser resolved activity entity data to parent activity entity.
	 * 
	 * 
	 */
	@XmlEnumValue("Merge")
	MERGE("Merge"),

	/**
	 * 
	 * Add stacked parser resolved activity entity data to parent activity entity as child. Deprecated, use "Relate"
	 * instead.
	 * 
	 * 
	 */
	@XmlEnumValue("Join")
	@Deprecated
	JOIN("Join"),

	/**
	 *
	 * Add stacked parser resolved activity entity data to parent activity entity as child.
	 *
	 *
	 */
	@XmlEnumValue("Relate")
	RELATE("Relate"),

	/**
	 *
	 * Add stacked parser resolved activity entity data to root activity entity as child.
	 *
	 *
	 */
	@XmlEnumValue("Relate_Flat")
	RELATE_FLAT("Relate_Flat");

	private final String value;

	AggregationTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static AggregationTypes fromValue(String v) {
		for (AggregationTypes c : AggregationTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
