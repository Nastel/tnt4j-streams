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
 * Java class for Stream complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Stream">
 *   &lt;complexContent>
 *     &lt;extension base="{}Stream">
 *       &lt;redefine>
 *         &lt;complexType name="Stream">
 *           &lt;complexContent>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *               &lt;choice maxOccurs="unbounded">
 *                 &lt;element name="property" type="{}StreamProperty" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="parser-ref" type="{}ParserReference" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="reference" type="{}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="tnt4j-properties" minOccurs="0">
 *                   &lt;complexType>
 *                     &lt;complexContent>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                         &lt;sequence>
 *                           &lt;element name="property" type="{}Property" maxOccurs="unbounded"/>
 *                         &lt;/sequence>
 *                       &lt;/restriction>
 *                     &lt;/complexContent>
 *                   &lt;/complexType>
 *                 &lt;/element>
 *               &lt;/choice>
 *               &lt;attGroup ref="{}EntityAttributeGroup"/>
 *             &lt;/restriction>
 *           &lt;/complexContent>
 *         &lt;/complexType>
 *       &lt;/redefine>
 *       &lt;sequence>
 *         &lt;element name="scenario" type="{}Scenario" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Stream", propOrder = { "scenario" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
public class Stream extends OriginalStream {

	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Scenario> scenario;

	/**
	 * Gets the value of the scenario property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the scenario property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getScenario().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Scenario }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Scenario> getScenario() {
		if (scenario == null) {
			scenario = new ArrayList<Scenario>();
		}
		return this.scenario;
	}

	public void addRequest(Scenario scr) {
		getScenario().add(scr);
	}

}
