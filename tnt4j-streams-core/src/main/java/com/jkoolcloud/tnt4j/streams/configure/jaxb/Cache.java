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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Defines a stream cache entries patterns.
 * 
 * 
 * <p>
 * Java class for Cache complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Cache"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="property" type="{}CacheProperty" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="entry" type="{}CacheEntry" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Cache", propOrder = { "property", "entry" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
public class Cache {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<CacheProperty> property;
	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<CacheEntry> entry;

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
	 * Objects of the following type(s) are allowed in the list {@link CacheProperty }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	public List<CacheProperty> getProperty() {
		if (property == null) {
			property = new ArrayList<CacheProperty>();
		}
		return this.property;
	}

	public void addProperty(CacheProperty cp) {
		getProperty().add(cp);
	}

	public void addProperty(String key, String value) {
		getProperty().add(new CacheProperty(key, value));
	}

	/**
	 * Gets the value of the entry property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the entry property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getEntry().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link CacheEntry }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public List<CacheEntry> getEntry() {
		if (entry == null) {
			entry = new ArrayList<CacheEntry>();
		}
		return this.entry;
	}

	public void addEntry(CacheEntry ce) {
		getEntry().add(ce);
	}

	public void addEntry(String id, String key, String value) {
		getEntry().add(new CacheEntry(id, key, value));
	}

	public void addEntry(String id, String key, String value, String defaultValue) {
		getEntry().add(new CacheEntry(id, key, value, defaultValue));
	}
}
