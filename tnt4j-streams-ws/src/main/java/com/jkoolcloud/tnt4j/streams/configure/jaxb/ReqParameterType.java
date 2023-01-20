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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Defines scenario step request (or command/query/etc.) parameter.
 *
 *
 * <p>
 * Java class for ReqParameterType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ReqParameterType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="format" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReqParameterType")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class ReqParameterType {

	@XmlAttribute(name = "id")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String id;
	@XmlAttribute(name = "value", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String value;
	@XmlAttribute(name = "type")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String type;
	@XmlAttribute(name = "format")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String format;

	public ReqParameterType() {

	}

	public ReqParameterType(String id, String value) {
		this.id = id;
		this.value = value;
	}

	public ReqParameterType(String id, String value, String type) {
		this.id = id;
		this.value = value;
		this.type = type;
	}

	/**
	 * Gets the value of the id property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getId() {
		return id;
	}

	/**
	 * Sets the value of the id property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setId(String value) {
		this.id = value;
	}

	/**
	 * Gets the value of the value property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the value of the type property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setType(String value) {
		this.type = value;
	}

	/**
	 * Gets the value of the format property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getFormat() {
		return format;
	}

	/**
	 * Sets the value of the format property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setFormat(String value) {
		this.format = value;
	}

}