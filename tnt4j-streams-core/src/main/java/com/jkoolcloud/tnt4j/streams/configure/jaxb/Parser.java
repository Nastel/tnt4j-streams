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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 *
 * Defines a parser to extract activity elements from a string identifying an activity.
 *
 *
 * <p>
 * Java class for Parser complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Parser">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="property" type="{}ParserProperty" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="reference" type="{}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="field" type="{}Field" maxOccurs="unbounded"/>
 *         &lt;element name="embedded-activity" type="{}EmbeddedActivity" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="filter" type="{}Filter" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attGroup ref="{}EntityAttributeGroup"/>
 *       &lt;attribute name="tags" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="manualFieldsOrder" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="default-data-type" type="{}DataTypes" default="String" />
 *       &lt;attribute name="default-emptyAsNull" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true"  />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Parser", propOrder = { "property", "reference", "field", "embeddedActivity", "filter" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public class Parser {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<ParserProperty> property;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Reference> reference;
	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Field> field;
	@XmlElement(name = "embedded-activity")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<EmbeddedActivity> embeddedActivity;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected Filter filter;
	@XmlAttribute(name = "tags")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String tags;
	@XmlAttribute(name = "manualFieldsOrder")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	protected Boolean manualFieldsOrder;
	@XmlAttribute(name = "default-data-type")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-01-22T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected DataTypes defaultDataType;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "default-emptyAsNull")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	protected Boolean defaultEmptyAsNull = true;
	@XmlAttribute(name = "class", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String clazz;

	public Parser() {
	}

	public Parser(String name, String clazz) {
		this(name, clazz, null);
	}

	public Parser(String name, String clazz, String tags) {
		this.name = name;
		this.clazz = clazz;
		this.tags = tags;
	}

	/**
	 * Gets the value of the property property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the property property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getProperty().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link ParserProperty }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<ParserProperty> getProperty() {
		if (property == null) {
			property = new ArrayList<ParserProperty>();
		}
		return this.property;
	}

	public void addProperty(ParserProperty p) {
		getProperty().add(p);
	}

	public void addProperty(String name, String value) {
		getProperty().add(new ParserProperty(name, value));
	}

	/**
	 * Gets the value of the reference property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the reference property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getReference().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Reference }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Reference> getReference() {
		if (reference == null) {
			reference = new ArrayList<Reference>();
		}
		return this.reference;
	}

	public void addReference(Reference r) {
		getReference().add(r);
	}

	public void addReference(String name) {
		getReference().add(new Reference(name));
	}

	/**
	 * Gets the value of the field property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the field property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getField().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Field }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Field> getField() {
		if (field == null) {
			field = new ArrayList<Field>();
		}
		return this.field;
	}

	public void addField(Field f) {
		getField().add(f);
	}

	/**
	 * Gets the value of the embeddedActivity property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the embeddedActivity property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getEmbeddedActivity().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link EmbeddedActivity }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<EmbeddedActivity> getEmbeddedActivity() {
		if (embeddedActivity == null) {
			embeddedActivity = new ArrayList<EmbeddedActivity>();
		}
		return this.embeddedActivity;
	}

	public void addEmbeddedActivity(EmbeddedActivity ea) {
		getEmbeddedActivity().add(ea);
	}

	public void addEmbeddedActivity(String name, String locator) {
		getEmbeddedActivity().add(new EmbeddedActivity(name, locator));
	}

	public void addEmbeddedActivity(String name, String locator, String locatorType, DataTypes dataType,
			String charset) {
		getEmbeddedActivity().add(new EmbeddedActivity(name, locator, locatorType, dataType, charset));
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
	 * Gets the value of the tags property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getTags() {
		return tags;
	}

	/**
	 * Sets the value of the tags property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setTags(String value) {
		this.tags = value;
	}

	/**
	 * Gets the value of the manualFieldsOrder property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public boolean isManualFieldsOrder() {
		if (manualFieldsOrder == null) {
			return false;
		} else {
			return manualFieldsOrder;
		}
	}

	/**
	 * Sets the value of the manualFieldsOrder property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public void setManualFieldsOrder(Boolean value) {
		this.manualFieldsOrder = value;
	}

	/**
	 * Gets the value of the defaultDatatype property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-01-22T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public DataTypes getDefaultDataType() {
		return defaultDataType;
	}

	/**
	 * Sets the value of the defaultDatatype property.
	 *
	 * @param value
	 *            allowed object is {@link DataTypes }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-01-22T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public void setDefaultDataType(DataTypes value) {
		this.defaultDataType = value;
	}

	/**
	 * Gets the value of the defaultEmptyAsNull property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public boolean isDefaultEmptyAsNull() {
		if (defaultEmptyAsNull == null) {
			return false;
		} else {
			return defaultEmptyAsNull;
		}
	}

	/**
	 * Sets the value of the defaultEmptyAsNull property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public void setDefaultEmptyAsNull(Boolean value) {
		this.defaultEmptyAsNull = value;
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
	 * Gets the value of the clazz property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getClazz() {
		return clazz;
	}

	/**
	 * Sets the value of the clazz property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setClazz(String value) {
		this.clazz = value;
	}

}
