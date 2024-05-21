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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Java class for Scenario complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Scenario"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="property" type="{}Property" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="step" type="{}ScenarioStep" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attGroup ref="{}ScenarioEntityAttributes"/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Scenario", propOrder = { "property", "step" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class Scenario {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-12-05T10:53:23+02:00", comments = "JAXB RI v2.2.4-2")
	protected List<Property> property;
	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected List<ScenarioStep> step;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String name;
	@XmlAttribute(name = "url")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String url;
	@XmlAttribute(name = "method")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String method;
	@XmlAttribute(name = "username")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String username;
	@XmlAttribute(name = "password")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String password;

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
	 * Objects of the following type(s) are allowed in the list {@link Property }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-12-05T10:53:23+02:00", comments = "JAXB RI v2.2.4-2")
	public List<Property> getProperty() {
		if (property == null) {
			property = new ArrayList<Property>();
		}
		return this.property;
	}

	public void addProperty(Property prop) {
		getProperty().add(prop);
	}

	public void addProperty(String name, String value) {
		getProperty().add(new Property(name, value));
	}

	/**
	 * Gets the value of the step property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the step property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getStep().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link ScenarioStep }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public List<ScenarioStep> getStep() {
		if (step == null) {
			step = new ArrayList<ScenarioStep>();
		}
		return this.step;
	}

	public void addStep(ScenarioStep s) {
		getStep().add(s);
	}

	public void addStep(String expression, String request, String name, String url, String method) {
		getStep().add(new ScenarioStep(expression, request, name, url, method));
	}

	public void addStep(int interval, UnitsTypes units, Integer repeatCount, String request, String name, String url,
			String method) {
		getStep().add(new ScenarioStep(interval, units, repeatCount, request, name, url, method));
	}

	public void addStep(int interval, UnitsTypes units, Integer repeatCount, String request, String name, String url,
			String method, String username, String password) {
		getStep().add(new ScenarioStep(interval, units, repeatCount, request, name, url, method, username, password));
	}

	public void addStep(int interval, UnitsTypes units, Integer repeatCount, String request, String parserRef,
			String name, String url, String method, String username, String password) {
		getStep().add(new ScenarioStep(interval, units, repeatCount, request, parserRef, name, url, method, username,
				password));
	}

	public void addStep(int interval, UnitsTypes units, Integer repeatCount, RequestType request, String name,
			String url, String method, String username, String password) {
		getStep().add(new ScenarioStep(interval, units, repeatCount, request, name, url, method, username, password));
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the url property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the value of the url property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setUrl(String value) {
		this.url = value;
	}

	/**
	 * Gets the value of the method property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getMethod() {
		return method;
	}

	/**
	 * Sets the value of the method property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setMethod(String value) {
		this.method = value;
	}

	/**
	 * Gets the value of the username property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the value of the username property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setUsername(String value) {
		this.username = value;
	}

	/**
	 * Gets the value of the password property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the value of the password property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setPassword(String value) {
		this.password = value;
	}

	public void setUserCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

}
