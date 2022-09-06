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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Defines a java object constructor parameter: argument value and type.
 * 
 * 
 * <p>
 * Java class for Parameter complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Parameter">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Parameter")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public class Parameter {

	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "value")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String value;
	@XmlAttribute(name = "type", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String type;

	public Parameter() {

	}

	public Parameter(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public Parameter(String name, String value, String type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getName() {
		return name;
	}

	/**
	 * Sets the value of the name property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getValue() {
		return value;
	}

	/**
	 * Sets the value of the value property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the value of the type property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getType() {
		return type;
	}

	/**
	 * Sets the value of the type property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setType(String value) {
		this.type = value;
	}

}
