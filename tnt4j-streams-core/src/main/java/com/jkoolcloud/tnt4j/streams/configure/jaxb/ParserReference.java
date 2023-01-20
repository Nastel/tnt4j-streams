
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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Associates field with parser reference.
 *
 *
 * <p>
 * Java class for ParserReference complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ParserReference"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{}Reference"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="matchExp" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParserReference", propOrder = { "matchExp" })
@XmlSeeAlso({ FieldParserReference.class })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-12-05T10:53:01+02:00", comments = "JAXB RI v2.2.4-2")
public class ParserReference extends Reference {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-12-05T10:53:01+02:00", comments = "JAXB RI v2.2.4-2")
	protected List<String> matchExp;

	public ParserReference() {
		super();
	}

	public ParserReference(String name) {
		super(name);
	}

	/**
	 * Gets the value of the matchExp property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the matchExp property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getMatchExp().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link String }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-12-05T10:53:01+02:00", comments = "JAXB RI v2.2.4-2")
	public List<String> getMatchExp() {
		if (matchExp == null) {
			matchExp = new ArrayList<String>();
		}
		return this.matchExp;
	}

	public void addMatchExp(String matchExp) {
		getMatchExp().add(matchExp);
	}

}
