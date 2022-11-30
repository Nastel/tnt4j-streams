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
import javax.xml.bind.annotation.*;

/**
 * Defines a cache configuration entity property - set of supported properties is entity-specific.
 * 
 * 
 * <p>
 * Java class for CacheProperty complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CacheProperty"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="name" use="required" type="{}CachePropertiesAll" /&gt;
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CacheProperty", propOrder = { "pValue" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
public class CacheProperty {

	@XmlValue
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	protected String pValue;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "value")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	protected String value;

	public CacheProperty() {
	}

	public CacheProperty(String name, String value) {
		this.name = name;
		if (value != null && (value.length() > 50)) {
			this.pValue = value;
		} else {
			this.value = value;
		}
	}

	/**
	 * Gets the value of the pValue property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	public String getPValue() {
		return pValue;
	}

	/**
	 * Sets the value of the pValue property.
	 * 
	 * @param pValue
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	public void setPValue(String pValue) {
		this.pValue = pValue;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	public void setValue(String value) {
		this.value = value;
	}

}
