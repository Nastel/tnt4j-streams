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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Translates an item from an activity RAW data to an activity entity field.
 * 
 * 
 * <p>
 * Java class for Field complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Field">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="field-locator" type="{}FieldLocator" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="field-map" type="{}FieldMap" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="field-map-ref" type="{}FieldMapRef" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="field-transform" type="{}FieldTransform" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="parser-ref" type="{}FieldParserReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="filter" type="{}Filter" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attGroup ref="{}FieldLocatorAttributes"/>
 *       &lt;attribute name="name" use="required" type="{}FieldNamesAll" />
 *       &lt;attribute name="separator" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="formattingPattern" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="transparent" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="value-type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="split" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Field", propOrder = { "fieldLocator", "fieldMap", "fieldMapRef", "fieldTransform", "parserRef",
		"filter" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public class Field {

	@XmlElement(name = "field-locator")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldLocator> fieldLocator;
	@XmlElement(name = "field-map")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldMap> fieldMap;
	@XmlElement(name = "field-map-ref")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldMapRef> fieldMapRef;
	@XmlElement(name = "field-transform")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldTransform> fieldTransform;
	@XmlElement(name = "parser-ref")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldParserReference> parserRef;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected Filter filter;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "separator")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String separator;
	@XmlAttribute(name = "formattingPattern")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String formattingPattern;
	@XmlAttribute(name = "transparent")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected Boolean transparent;
	@XmlAttribute(name = "value-type")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected String valueType;
	@XmlAttribute(name = "split")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected Boolean split;
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

	public Field() {
	}

	public Field(String name, String locator) {
		this.name = name;
		this.locator = locator;
	}

	public Field(String name, String locator, String value) {
		this.name = name;
		this.locator = locator;
		this.value = value;
	}

	public Field(String name, String locator, String format, String locale) {
		this.name = name;
		this.locator = locator;
		this.format = format;
		this.locale = locale;
	}

	public Field(String name, String locator, String locatorType, DataTypes datatype, String format, String units,
			String timezone, String locale, BigInteger radix, String value, String separator, String charset) {
		this.name = name;
		this.locator = locator;
		this.locatorType = locatorType;
		this.datatype = datatype;
		this.format = format;
		this.units = units;
		this.timezone = timezone;
		this.locale = locale;
		this.radix = radix;
		this.value = value;
		this.separator = separator;
		this.charset = charset;
	}

	/**
	 * Gets the value of the fieldLocator property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the fieldLocator property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getFieldLocator().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link FieldLocator }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldLocator> getFieldLocator() {
		if (fieldLocator == null) {
			fieldLocator = new ArrayList<FieldLocator>();
		}
		return this.fieldLocator;
	}

	public void addFieldLocator(FieldLocator fl) {
		getFieldLocator().add(fl);
	}

	public void addFieldLocator(String locator, String locatorType, DataTypes datatype, BigInteger radix, String units,
			String timezone, String format, String locale, String value, String charset) {
		getFieldLocator().add(new FieldLocator(locator, locatorType, datatype, radix, units, timezone, format, locale,
				value, charset));
	}

	/**
	 * Gets the value of the fieldMap property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the fieldMap property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getFieldMap().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link FieldMap }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldMap> getFieldMap() {
		if (fieldMap == null) {
			fieldMap = new ArrayList<FieldMap>();
		}
		return this.fieldMap;
	}

	public void addFieldMap(FieldMap fm) {
		getFieldMap().add(fm);
	}

	public void addFieldMap(String source, String target) {
		getFieldMap().add(new FieldMap(source, target));
	}

	public void addFieldMap(String source, String target, FieldMapTypes type) {
		getFieldMap().add(new FieldMap(source, target, type));
	}

	/**
	 * Gets the value of the fieldMapRef property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the fieldMapRef property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getFieldMapRef().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link FieldMapRef }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldMapRef> getFieldMapRef() {
		if (fieldMapRef == null) {
			fieldMapRef = new ArrayList<FieldMapRef>();
		}
		return this.fieldMapRef;
	}

	public void addFieldMapRef(FieldMapRef fmr) {
		getFieldMapRef().add(fmr);
	}

	public void addFieldMapRef(String resource) {
		getFieldMapRef().add(new FieldMapRef(resource));
	}

	/**
	 * Gets the value of the fieldTransform property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the fieldTransform property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getFieldTransform().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link FieldTransform }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldTransform> getFieldTransform() {
		if (fieldTransform == null) {
			fieldTransform = new ArrayList<FieldTransform>();
		}
		return this.fieldTransform;
	}

	public void addFieldTransform() {
		getFieldTransform().add(new FieldTransform());
	}

	public void addFieldTransform(String name, String beanRef) {
		getFieldTransform().add(new FieldTransform(name, beanRef));
	}

	public void addFieldTransform(String name, String lang, String tScript) {
		getFieldTransform().add(new FieldTransform(name, lang, tScript));
	}

	public void addFieldTransform(String name, String lang, String tScript, String phase) {
		getFieldTransform().add(new FieldTransform(name, lang, tScript, phase));
	}

	/**
	 * Gets the value of the parserRef property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the parserRef property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getParserRef().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link FieldParserReference }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldParserReference> getParserRef() {
		if (parserRef == null) {
			parserRef = new ArrayList<FieldParserReference>();
		}
		return this.parserRef;
	}

	public void addParserRef(FieldParserReference pr) {
		getParserRef().add(pr);
	}

	public void addParserRef(String name, AggregationTypes aType) {
		getParserRef().add(new FieldParserReference(name, aType));
	}

	/**
	 * Gets the value of the filter property.
	 * 
	 * @return possible object is {@link Filter }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public Filter getFilter() {
		return filter;
	}

	/**
	 * Sets the value of the filter property.
	 * 
	 * @param value
	 *            allowed object is {@link Filter }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setFilter(Filter value) {
		this.filter = value;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
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

	/**
	 * Gets the value of the formattingPattern property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getFormattingPattern() {
		return formattingPattern;
	}

	/**
	 * Sets the value of the formattingPattern property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setFormattingPattern(String value) {
		this.formattingPattern = value;
	}

	/**
	 * Gets the value of the transparent property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public Boolean isTransparent() {
		return transparent;
	}

	/**
	 * Sets the value of the transparent property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setTransparent(Boolean value) {
		this.transparent = value;
	}

	/**
	 * Gets the value of the valueType property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public String getValueType() {
		return valueType;
	}

	/**
	 * Sets the value of the valueType property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setValueType(String value) {
		this.valueType = value;
	}

	/**
	 * Gets the value of the split property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public Boolean isSplit() {
		return split;
	}

	/**
	 * Sets the value of the split property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public void setSplit(Boolean value) {
		this.split = value;
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
