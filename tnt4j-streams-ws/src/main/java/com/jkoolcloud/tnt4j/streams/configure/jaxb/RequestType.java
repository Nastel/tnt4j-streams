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
 * Defines scenario step request (or command/query/etc.) data.
 * 
 * 
 * <p>
 * Java class for RequestType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="req-param" type="{}ReqParameterType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="parser-ref" type="{}ParserReference" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequestType", propOrder = { "value", "req-param" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
public class RequestType {

	@XmlValue
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String value;
	@XmlElement(name = "req-param", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<ReqParameterType> reqParam;
	@XmlAttribute(name = "parser-ref")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String parserRef;

	public RequestType() {
	}

	public RequestType(String value) {
		this.value = value;
	}

	public RequestType(String value, String parserRef) {
		this.value = value;
		this.parserRef = parserRef;
	}

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the value of the req-param property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the req-param property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getReqParam().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link ReqParameterType }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	public List<ReqParameterType> getReqParam() {
		if (reqParam == null) {
			reqParam = new ArrayList<ReqParameterType>();
		}
		return this.reqParam;
	}

	public void addReqParam(ReqParameterType reqParam) {
		getReqParam().add(reqParam);
	}

	public void addReqParam(String id, String value) {
		getReqParam().add(new ReqParameterType(id, value));
	}

	public void addReqParam(String id, String value, String type) {
		getReqParam().add(new ReqParameterType(id, value, type));
	}

	/**
	 * Gets the value of the parserRef property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getParserRef() {
		return parserRef;
	}

	/**
	 * Sets the value of the parserRef property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-07-20T10:29:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setParserRef(String value) {
		this.parserRef = value;
	}

}
