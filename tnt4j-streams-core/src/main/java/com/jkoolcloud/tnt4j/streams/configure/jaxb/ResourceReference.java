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
import javax.xml.bind.annotation.*;

/**
 * Defines a external resource reference to be used by this streams data source configuration.
 * 
 * 
 * <p>
 * Java class for ResourceReference complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ResourceReference"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="type" use="required" type="{}ResourceReferenceType" /&gt;
 *       &lt;attribute name="uri" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *       &lt;attribute name="separator" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResourceReference")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public class ResourceReference {

	@XmlAttribute(name = "id", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String id;
	@XmlAttribute(name = "type", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected ResourceReferenceType type;
	@XmlAttribute(name = "uri", required = true)
	@XmlSchemaType(name = "anyURI")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String uri;
	@XmlAttribute(name = "separator")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String separator;

	public ResourceReference() {
	}

	public ResourceReference(String id, String type, String uri) {
		this(id, ResourceReferenceType.fromValue(type), uri);
	}

	public ResourceReference(String id, ResourceReferenceType type, String uri) {
		this.id = id;
		this.type = type;
		this.uri = uri;
	}

	public ResourceReference(String id, ResourceReferenceType type, String uri, String separator) {
		this(id, type, uri);
		this.separator = separator;
	}

	/**
	 * Gets the value of the id property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setId(String value) {
		this.id = value;
	}

	/**
	 * Gets the value of the type property.
	 * 
	 * @return possible object is {@link ResourceReferenceType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public ResourceReferenceType getType() {
		return type;
	}

	/**
	 * Sets the value of the type property.
	 * 
	 * @param value
	 *            allowed object is {@link ResourceReferenceType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setType(ResourceReferenceType value) {
		this.type = value;
	}

	/**
	 * Gets the value of the uri property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the value of the uri property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setUri(String value) {
		this.uri = value;
	}

	/**
	 * Gets the value of the separator property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getSeparator() {
		return separator;
	}

	/**
	 * Sets the value of the separator property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setSeparator(String value) {
		this.separator = value;
	}

}
