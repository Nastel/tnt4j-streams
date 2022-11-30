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

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Associates field with stacked parser reference.
 * 
 * 
 * <p>
 * Java class for FieldParserReference complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldParserReference"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{}ParserReference"&gt;
 *       &lt;attribute name="aggregation" type="{}AggregationTypes" default="Merge" /&gt;
 *       &lt;attribute name="applyOn" type="{}ParserApplyTypes" default="Field" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldParserReference")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-12-06T04:18:24+02:00", comments = "JAXB RI v2.2.4-2")
public class FieldParserReference extends ParserReference {

	@XmlAttribute(name = "aggregation")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	protected AggregationTypes aggregation;
	@XmlAttribute(name = "applyOn")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	protected ParserApplyTypes applyOn;

	public FieldParserReference() {
		super();
	}

	public FieldParserReference(String name, AggregationTypes aType) {
		this(name, aType, null);
	}

	public FieldParserReference(String name, AggregationTypes aType, ParserApplyTypes applyOn) {
		super(name);
		this.aggregation = aType;
		this.applyOn = applyOn;
	}

	/**
	 * Gets the value of the aggregation property.
	 * 
	 * @return possible object is {@link AggregationTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	public AggregationTypes getAggregation() {
		if (aggregation == null) {
			return AggregationTypes.MERGE;
		} else {
			return aggregation;
		}
	}

	/**
	 * Sets the value of the aggregation property.
	 * 
	 * @param value
	 *            allowed object is {@link AggregationTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	public void setAggregation(AggregationTypes value) {
		this.aggregation = value;
	}

	/**
	 * Gets the value of the applyOn property.
	 *
	 * @return possible object is {@link ParserApplyTypes }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	public ParserApplyTypes getApplyOn() {
		if (applyOn == null) {
			return ParserApplyTypes.FIELD;
		} else {
			return applyOn;
		}
	}

	/**
	 * Sets the value of the applyOn property.
	 *
	 * @param value
	 *            allowed object is {@link ParserApplyTypes }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	public void setApplyOn(ParserApplyTypes value) {
		this.applyOn = value;
	}

}
