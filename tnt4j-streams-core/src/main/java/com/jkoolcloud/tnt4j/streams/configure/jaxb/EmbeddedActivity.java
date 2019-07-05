/*
 * Copyright 2014-2018 JKOOL, LLC.
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
 * Defines activity field containing embedded activity entities data parsed by referenced parser.
 * 
 * 
 * <p>
 * Java class for EmbeddedActivity complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EmbeddedActivity">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="field-locator" type="{}FieldLocator" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="field-transform" type="{}FieldTransform" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="parser-ref" type="{}FieldParserReference" maxOccurs="unbounded"/>
 *         &lt;element name="filter" type="{}Filter" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attGroup ref="{}EmbeddedActivityLocatorAttributes"/>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EmbeddedActivity", propOrder = { "fieldLocator", "fieldTransform", "parserRef", "filter" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public class EmbeddedActivity {

	@XmlElement(name = "field-locator")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldLocator> fieldLocator;
	@XmlElement(name = "field-transform")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldTransform> fieldTransform;
	@XmlElement(name = "parser-ref", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<FieldParserReference> parserRef;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected Filter filter;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "locator", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String locator;
	@XmlAttribute(name = "locator-type", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String locatorType;
	@XmlAttribute(name = "required")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String required;
	@XmlAttribute(name = "id")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String id;

	public EmbeddedActivity() {
	}

	public EmbeddedActivity(String name, String locator) {
		this.name = name;
		this.locator = locator;
	}

	public EmbeddedActivity(String name, String locator, String locatorType) {
		this.name = name;
		this.locator = locator;
		this.locatorType = locatorType;
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldTransform> getFieldTransform() {
		if (fieldTransform == null) {
			fieldTransform = new ArrayList<FieldTransform>();
		}
		return this.fieldTransform;
	}

	public void addFieldTransform(FieldTransform ft) {
		getFieldTransform().add(ft);
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<FieldParserReference> getParserRef() {
		if (parserRef == null) {
			parserRef = new ArrayList<FieldParserReference>();
		}
		return this.parserRef;
	}

	public void addParserRef(FieldParserReference fpr) {
		getParserRef().add(fpr);
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setFilter(Filter value) {
		this.filter = value;
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
	 * Gets the value of the locator property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setLocator(String value) {
		this.locator = value;
	}

	/**
	 * Gets the value of the locatorType property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setLocatorType(String value) {
		this.locatorType = value;
	}

	/**
	 * Gets the value of the required property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setRequired(String value) {
		this.required = value;
	}

	/**
	 * Gets the value of the id property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setId(String value) {
		this.id = value;
	}

}
