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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;

/**
 * Defines a field locator configuration entity.
 * 
 * 
 * <p>
 * Java class for FieldLocator complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldLocator"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded"&gt;
 *         &lt;element name="field-map" type="{}FieldMap" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="field-map-ref" type="{}FieldMapRef" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="field-transform" type="{}FieldTransform" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="filter" type="{}Filter" minOccurs="0"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attGroup ref="{}FieldLocatorAttributes"/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldLocator", propOrder = { "content" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public class FieldLocator {

	@XmlElementRefs({ @XmlElementRef(name = "field-map-ref", type = JAXBElement.class, required = false),
			@XmlElementRef(name = "field-map", type = JAXBElement.class, required = false),
			@XmlElementRef(name = "field-transform", type = JAXBElement.class, required = false),
			@XmlElementRef(name = "filter", type = JAXBElement.class, required = false) })
	@XmlMixed
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Serializable> content;
	@XmlAttribute(name = "locator")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String locator;
	@XmlAttribute(name = "locator-type")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String locatorType;
	@XmlAttribute(name = "datatype")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected DataTypes datatype;
	@XmlAttribute(name = "radix")
	@XmlSchemaType(name = "nonNegativeInteger")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected BigInteger radix;
	@XmlAttribute(name = "units")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String units;
	@XmlAttribute(name = "timezone")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String timezone;
	@XmlAttribute(name = "format")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String format;
	@XmlAttribute(name = "locale")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String locale;
	@XmlAttribute(name = "value")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String value;
	@XmlAttribute(name = "required")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String required;
	@XmlAttribute(name = "id")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String id;
	@XmlAttribute(name = "cacheKey")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String cacheKey;
	@XmlAttribute(name = "charset")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String charset;
	@XmlAttribute(name = "emptyAsNull")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected Boolean emptyAsNull = true;

	public FieldLocator() {
	}

	public FieldLocator(String locator, String locatorType, DataTypes datatype, BigInteger radix, String units,
			String timezone, String format, String locale, String value, String charset) {
		this.locator = locator;
		this.locatorType = locatorType;
		this.datatype = datatype;
		this.radix = radix;
		this.units = units;
		this.timezone = timezone;
		this.format = format;
		this.locale = locale;
		this.value = value;
		this.charset = charset;
	}

	/**
	 * 
	 * Defines a field locator configuration entity. Gets the value of the content property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the content property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getContent().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JAXBElement }{@code <}{@link FieldMap }{@code >}
	 * {@link JAXBElement }{@code <}{@link FieldTransform }{@code >} {@link String } {@link JAXBElement
	 * }{@code <}{@link Filter }{@code >}
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Serializable> getContent() {
		if (content == null) {
			content = new ArrayList<Serializable>();
		}
		return this.content;
	}

	public void addContent(Serializable content) {
		getContent().add(content);
	}

	/**
	 * Gets the value of the locator property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getLocator() {
		return locator;
	}

	/**
	 * Sets the value of the locator property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setLocator(String value) {
		this.locator = value;
	}

	/**
	 * Gets the value of the locatorType property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getLocatorType() {
		return locatorType;
	}

	/**
	 * Sets the value of the locatorType property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setLocatorType(String value) {
		this.locatorType = value;
	}

	/**
	 * Gets the value of the datatype property.
	 * 
	 * @return possible object is {@link DataTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public DataTypes getDatatype() {
		if (datatype == null) {
			return DataTypes.STRING;
		} else {
			return datatype;
		}
	}

	/**
	 * Sets the value of the datatype property.
	 * 
	 * @param value
	 *            allowed object is {@link DataTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setDatatype(DataTypes value) {
		this.datatype = value;
	}

	/**
	 * Gets the value of the radix property.
	 * 
	 * @return possible object is {@link BigInteger }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public BigInteger getRadix() {
		return radix;
	}

	/**
	 * Sets the value of the radix property.
	 * 
	 * @param value
	 *            allowed object is {@link BigInteger }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setRadix(BigInteger value) {
		this.radix = value;
	}

	/**
	 * Gets the value of the units property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getUnits() {
		return units;
	}

	/**
	 * Sets the value of the units property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setUnits(String value) {
		this.units = value;
	}

	/**
	 * Gets the value of the timezone property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getTimezone() {
		return timezone;
	}

	/**
	 * Sets the value of the timezone property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setTimezone(String value) {
		this.timezone = value;
	}

	/**
	 * Gets the value of the charset property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getCharset() {
		return charset;
	}

	/**
	 * Sets the value of the charset property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setCharset(String value) {
		this.charset = value;
	}

	/**
	 * Gets the value of the format property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setFormat(String value) {
		this.format = value;
	}

	/**
	 * Gets the value of the locale property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getLocale() {
		return locale;
	}

	/**
	 * Sets the value of the locale property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setLocale(String value) {
		this.locale = value;
	}

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the value of the required property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getRequired() {
		return required;
	}

	/**
	 * Sets the value of the required property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setRequired(String value) {
		this.required = value;
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
	 * Gets the value of the cacheKey property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getCacheKey() {
		return cacheKey;
	}

	/**
	 * Sets the value of the cacheKey property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setCacheKey(String value) {
		this.cacheKey = value;
	}

	/**
	 * Gets the value of the emptyAsNull property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public Boolean isEmptyAsNull() {
		return emptyAsNull;
	}

	/**
	 * Sets the value of the emptyAsNull property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setEmptyAsNull(Boolean value) {
		this.emptyAsNull = value;
	}

}
