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

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Transforms field/locator value by applying transformation bean/script/expression defined rules.
 * 
 * 
 * <p>
 * Java class for FieldTransform complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldTransform"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="beanRef" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="lang" type="{}ScriptLangs" default="javascript" /&gt;
 *       &lt;attribute name="phase" type="{}ValueResolutionPhases" default="formatted" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldTransform", propOrder = { "tScript" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public class FieldTransform {

	@XmlValue
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String tScript;
	@XmlAttribute(name = "name")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "beanRef")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String beanRef;
	@XmlAttribute(name = "lang")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected ScriptLangs lang;
	@XmlAttribute(name = "phase")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	protected ValueResolutionPhases phase;

	public FieldTransform() {

	}

	public FieldTransform(String name, String beanRef) {
		this.name = name;
		this.beanRef = beanRef;
	}

	public FieldTransform(String name, String lang, String tScript) {
		this.name = name;
		this.lang = ScriptLangs.fromValue(lang);
		this.tScript = tScript;
	}

	public FieldTransform(String name, String lang, String tScript, String phase) {
		this.name = name;
		this.lang = ScriptLangs.fromValue(lang);
		this.tScript = tScript;
		this.phase = ValueResolutionPhases.fromValue(phase);
	}

	/**
	 * Gets the value of the tScript property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getTSctipt() {
		return tScript;
	}

	/**
	 * Sets the value of the tScript property.
	 *
	 * @param tScript
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setTSctipt(String tScript) {
		this.tScript = tScript;
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
	 * Gets the value of the beanRef property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public String getBeanRef() {
		return beanRef;
	}

	/**
	 * Sets the value of the beanRef property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setBeanRef(String value) {
		this.beanRef = value;
	}

	/**
	 * Gets the value of the lang property.
	 * 
	 * @return possible object is {@link ScriptLangs }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public ScriptLangs getLang() {
		if (lang == null) {
			return ScriptLangs.JAVASCRIPT;
		} else {
			return lang;
		}
	}

	/**
	 * Sets the value of the lang property.
	 * 
	 * @param value
	 *            allowed object is {@link ScriptLangs }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public void setLang(ScriptLangs value) {
		this.lang = value;
	}

	/**
	 * Gets the value of the phase property.
	 *
	 * @return possible object is {@link ValueResolutionPhases }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public ValueResolutionPhases getPhase() {
		if (phase == null) {
			return ValueResolutionPhases.FORMATTED;
		} else {
			return phase;
		}
	}

	/**
	 * Sets the value of the phase property.
	 *
	 * @param value
	 *            allowed object is {@link ValueResolutionPhases }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-15T02:53:24+03:00", comments = "JAXB RI v2.2.4-2")
	public void setPhase(ValueResolutionPhases value) {
		this.phase = value;
	}

}
