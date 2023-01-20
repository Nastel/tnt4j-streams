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

import java.math.BigInteger;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Defines Cron scheduler parameters.
 * 
 * 
 * <p>
 * Java class for ScheduleCron complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScheduleCron"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attGroup ref="{}SchedulerAttributeGroup"/&gt;
 *       &lt;attribute name="expression" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScheduleCron")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class ScheduleCron {

	@XmlAttribute(name = "expression", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected String expression;
	@XmlAttribute(name = "startDelay")
	@XmlSchemaType(name = "nonNegativeInteger")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected BigInteger startDelay;
	@XmlAttribute(name = "startDelayUnits")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	protected UnitsTypes startDelayUnits;

	public ScheduleCron() {

	}

	public ScheduleCron(String expression) {
		this.expression = expression;
	}

	public ScheduleCron(String expression, Integer startDelay, UnitsTypes delayUnits) {
		this.expression = expression;
		this.startDelay = startDelay == null ? null : BigInteger.valueOf(startDelay);
		this.startDelayUnits = delayUnits;
	}

	/**
	 * Gets the value of the expression property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public String getExpression() {
		return expression;
	}

	/**
	 * Sets the value of the expression property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setExpression(String value) {
		this.expression = value;
	}

	/**
	 * Gets the value of the startDelay property.
	 *
	 * @return possible object is {@link BigInteger }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public BigInteger getStartDelay() {
		return startDelay;
	}

	/**
	 * Sets the value of the startDelay property.
	 *
	 * @param value
	 *            allowed object is {@link BigInteger }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setStartDelay(BigInteger value) {
		this.startDelay = value;
	}

	/**
	 * Gets the value of the startDelayUnits property.
	 *
	 * @return possible object is {@link UnitsTypes }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public UnitsTypes getStartDelayUnits() {
		if (startDelayUnits == null) {
			return UnitsTypes.SECONDS;
		} else {
			return startDelayUnits;
		}
	}

	/**
	 * Sets the value of the startDelayUnits property.
	 *
	 * @param value
	 *            allowed object is {@link UnitsTypes }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2019-05-28T11:13:40+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
	public void setStartDelayUnits(UnitsTypes value) {
		this.startDelayUnits = value;
	}

}
