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
 * Filters activity data to be included/excluded from streaming by evaluating value or script/expression defined rules.
 * 
 * 
 * <p>
 * Java class for Filter complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Filter"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded"&gt;
 *         &lt;element name="value" maxOccurs="unbounded" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                 &lt;attribute name="handle" use="required" type="{}HandleTypes" /&gt;
 *                 &lt;attribute name="evaluation" type="{}EvaluationTypes" default="is" /&gt;
 *                 &lt;attribute name="format" type="{}DataFormatsAll" /&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="expression" maxOccurs="unbounded" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                 &lt;attribute name="handle" use="required" type="{}HandleTypes" /&gt;
 *                 &lt;attribute name="lang" use="required" type="{}ScriptLangs" /&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Filter", propOrder = { "value", "expression" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public class Filter {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Filter.Value> value;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Filter.Expression> expression;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;

	/**
	 * Gets the value of the value property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the value property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getValue().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Filter.Value }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Filter.Value> getValue() {
		if (value == null) {
			value = new ArrayList<Filter.Value>();
		}
		return this.value;
	}

	public void addValue(Filter.Value value) {
		getValue().add(value);
	}

	public void addValue(String value, HandleTypes handle) {
		getValue().add(new Filter.Value(value, handle));
	}

	public void addValue(String value, HandleTypes handle, String format) {
		getValue().add(new Filter.Value(value, handle, format));
	}

	public void addValue(String value, HandleTypes handle, EvaluationTypes evaluation) {
		getValue().add(new Filter.Value(value, handle, evaluation));
	}

	public void addValue(String value, HandleTypes handle, EvaluationTypes evaluation, String format) {
		getValue().add(new Filter.Value(value, handle, evaluation, format));
	}

	/**
	 * Gets the value of the expression property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the expression property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getExpression().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Filter.Expression }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Filter.Expression> getExpression() {
		if (expression == null) {
			expression = new ArrayList<Filter.Expression>();
		}
		return this.expression;
	}

	public void addExpression(Filter.Expression expression) {
		getExpression().add(expression);
	}

	public void addExpression(String value, HandleTypes handle) {
		getExpression().add(new Filter.Expression(value, handle));
	}

	public void addExpression(String value, HandleTypes handle, ScriptLangs lang) {
		getExpression().add(new Filter.Expression(value, handle, lang));
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
	 * <p>
	 * Java class for anonymous complex type.
	 * 
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 * 
	 * <pre>
	 * &lt;complexType&gt;
	 *   &lt;simpleContent&gt;
	 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
	 *       &lt;attribute name="handle" use="required" type="{}HandleTypes" /&gt;
	 *       &lt;attribute name="lang" use="required" type="{}ScriptLangs" /&gt;
	 *     &lt;/extension&gt;
	 *   &lt;/simpleContent&gt;
	 * &lt;/complexType&gt;
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = { "value" })
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public static class Expression {

		@XmlValue
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected String value;
		@XmlAttribute(name = "handle", required = true)
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected HandleTypes handle;
		@XmlAttribute(name = "lang", required = true)
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected ScriptLangs lang;

		public Expression() {
		}

		public Expression(String value, HandleTypes handle) {
			this.value = value;
			this.handle = handle;
		}

		public Expression(String value, HandleTypes handle, ScriptLangs lang) {
			this.value = value;
			this.handle = handle;
			this.lang = lang;
		}

		/**
		 * Gets the value of the value property.
		 * 
		 * @return possible object is {@link String }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public void setValue(String value) {
			this.value = value;
		}

		/**
		 * Gets the value of the handle property.
		 * 
		 * @return possible object is {@link HandleTypes }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public HandleTypes getHandle() {
			return handle;
		}

		/**
		 * Sets the value of the handle property.
		 * 
		 * @param value
		 *            allowed object is {@link HandleTypes }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public void setHandle(HandleTypes value) {
			this.handle = value;
		}

		/**
		 * Gets the value of the lang property.
		 * 
		 * @return possible object is {@link ScriptLangs }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public ScriptLangs getLang() {
			return lang;
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

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 * 
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 * 
	 * <pre>
	 * &lt;complexType&gt;
	 *   &lt;simpleContent&gt;
	 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
	 *       &lt;attribute name="handle" use="required" type="{}HandleTypes" /&gt;
	 *       &lt;attribute name="evaluation" type="{}EvaluationTypes" default="is" /&gt;
	 *       &lt;attribute name="format" type="{}DataFormatsAll" /&gt;
	 *     &lt;/extension&gt;
	 *   &lt;/simpleContent&gt;
	 * &lt;/complexType&gt;
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = { "value" })
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public static class Value {

		@XmlValue
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected String value;
		@XmlAttribute(name = "handle", required = true)
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected HandleTypes handle;
		@XmlAttribute(name = "evaluation")
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected EvaluationTypes evaluation;
		@XmlAttribute(name = "format")
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		protected String format;

		public Value() {
		}

		public Value(String value, HandleTypes handle) {
			this.value = value;
			this.handle = handle;
		}

		public Value(String value, HandleTypes handle, String format) {
			this.value = value;
			this.handle = handle;
			this.format = format;
		}

		public Value(String value, HandleTypes handle, EvaluationTypes evaluation) {
			this.value = value;
			this.handle = handle;
			this.evaluation = evaluation;
		}

		public Value(String value, HandleTypes handle, EvaluationTypes evaluation, String format) {
			this.value = value;
			this.handle = handle;
			this.evaluation = evaluation;
			this.format = format;
		}

		/**
		 * Gets the value of the value property.
		 * 
		 * @return possible object is {@link String }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public void setValue(String value) {
			this.value = value;
		}

		/**
		 * Gets the value of the handle property.
		 * 
		 * @return possible object is {@link HandleTypes }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public HandleTypes getHandle() {
			return handle;
		}

		/**
		 * Sets the value of the handle property.
		 * 
		 * @param value
		 *            allowed object is {@link HandleTypes }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public void setHandle(HandleTypes value) {
			this.handle = value;
		}

		/**
		 * Gets the value of the evaluation property.
		 * 
		 * @return possible object is {@link EvaluationTypes }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public EvaluationTypes getEvaluation() {
			if (evaluation == null) {
				return EvaluationTypes.IS;
			} else {
				return evaluation;
			}
		}

		/**
		 * Sets the value of the evaluation property.
		 * 
		 * @param value
		 *            allowed object is {@link EvaluationTypes }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public void setEvaluation(EvaluationTypes value) {
			this.evaluation = value;
		}

		/**
		 * Gets the value of the format property.
		 * 
		 * @return possible object is {@link String }
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
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
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
		public void setFormat(String value) {
			this.format = value;
		}

	}

}
