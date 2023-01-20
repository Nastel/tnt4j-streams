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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * com.jkoolcloud.tnt4j.streams.configure.jaxb package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

	private final static QName _OriginalStreamParserRef_QNAME = new QName("", "parser-ref");
	private final static QName _OriginalStreamTnt4JProperties_QNAME = new QName("", "tnt4j-properties");
	private final static QName _OriginalStreamProperty_QNAME = new QName("", "property");
	private final static QName _OriginalStreamReference_QNAME = new QName("", "reference");
	private final static QName _FieldLocatorFieldTransform_QNAME = new QName("", "field-transform");
	private final static QName _FieldLocatorFieldMap_QNAME = new QName("", "field-map");
	private final static QName _FieldLocatorFieldMapRef_QNAME = new QName("", "field-map-ref");
	private final static QName _FieldLocatorFilter_QNAME = new QName("", "filter");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
	 * com.jkoolcloud.tnt4j.streams.configure.jaxb
	 * 
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link Filter }
	 * 
	 */
	public Filter createFilter() {
		return new Filter();
	}

	/**
	 * Create an instance of {@link OriginalStream }
	 * 
	 */
	public OriginalStream createOriginalStream() {
		return new OriginalStream();
	}

	/**
	 * Create an instance of {@link TntDataSource }
	 * 
	 */
	public TntDataSource createTntDataSource() {
		return new TntDataSource();
	}

	/**
	 * Create an instance of {@link ResourceReference }
	 * 
	 */
	public ResourceReference createResourceReference() {
		return new ResourceReference();
	}

	/**
	 * Create an instance of {@link JavaObject }
	 * 
	 */
	public JavaObject createJavaObject() {
		return new JavaObject();
	}

	/**
	 * Create an instance of {@link Parser }
	 *
	 */
	public Parser createParser() {
		return new Parser();
	}

	/**
	 * Create an instance of {@link Stream }
	 * 
	 */
	public Stream createStream() {
		return new Stream();
	}

	/**
	 * Create an instance of {@link Field }
	 *
	 */
	public Field createField() {
		return new Field();
	}

	/**
	 * Create an instance of {@link ParserProperty }
	 * 
	 */
	public ParserProperty createParserProperty() {
		return new ParserProperty();
	}

	/**
	 * Create an instance of {@link ScheduleCron }
	 * 
	 */
	public ScheduleCron createScheduleCron() {
		return new ScheduleCron();
	}

	/**
	 * Create an instance of {@link Property }
	 * 
	 */
	public Property createProperty() {
		return new Property();
	}

	/**
	 * Create an instance of {@link ScheduleSimple }
	 * 
	 */
	public ScheduleSimple createScheduleSimple() {
		return new ScheduleSimple();
	}

	/**
	 * Create an instance of {@link Parameter }
	 *
	 */
	public Parameter createParameter() {
		return new Parameter();
	}

	/**
	 * Create an instance of {@link CacheEntry }
	 *
	 */
	public CacheEntry createCacheEntry() {
		return new CacheEntry();
	}

	/**
	 * Create an instance of {@link FieldMap }
	 * 
	 */
	public FieldMap createFieldMap() {
		return new FieldMap();
	}

	/**
	 * Create an instance of {@link Scenario }
	 * 
	 */
	public Scenario createScenario() {
		return new Scenario();
	}

	/**
	 * Create an instance of {@link CacheProperty }
	 * 
	 */
	public CacheProperty createCacheProperty() {
		return new CacheProperty();
	}

	/**
	 * Create an instance of {@link FieldTransform }
	 * 
	 */
	public FieldTransform createFieldTransform() {
		return new FieldTransform();
	}

	/**
	 * Create an instance of {@link Cache }
	 *
	 */
	public Cache createCache() {
		return new Cache();
	}

	/**
	 * Create an instance of {@link FieldParserReference }
	 * 
	 */
	public FieldParserReference createFieldParserReference() {
		return new FieldParserReference();
	}

	/**
	 * Create an instance of {@link EmbeddedActivity }
	 *
	 */
	public EmbeddedActivity createEmbeddedActivity() {
		return new EmbeddedActivity();
	}

	/**
	 * Create an instance of {@link ScenarioStep }
	 * 
	 */
	public ScenarioStep createScenarioStep() {
		return new ScenarioStep();
	}

	/**
	 * Create an instance of {@link FieldMapRef }
	 * 
	 */
	public FieldMapRef createFieldMapRef() {
		return new FieldMapRef();
	}

	/**
	 * Create an instance of {@link Reference }
	 * 
	 */
	public Reference createReference() {
		return new Reference();
	}

	/**
	 * Create an instance of {@link FieldLocator }
	 * 
	 */
	public FieldLocator createFieldLocator() {
		return new FieldLocator();
	}

	/**
	 * Create an instance of {@link ParserReference }
	 *
	 */
	public ParserReference createParserReference() {
		return new ParserReference();
	}

	/**
	 * Create an instance of {@link RequestType }
	 * 
	 */
	public RequestType createRequestType() {
		return new RequestType();
	}

	/**
	 * Create an instance of {@link ReqParameterType }
	 *
	 */
	public ReqParameterType createReqParameterType() {
		return new ReqParameterType();
	}

	/**
	 * Create an instance of {@link StreamProperty }
	 * 
	 */
	public StreamProperty createStreamProperty() {
		return new StreamProperty();
	}

	/**
	 * Create an instance of {@link Filter.Value }
	 * 
	 */
	public Filter.Value createFilterValue() {
		return new Filter.Value();
	}

	/**
	 * Create an instance of {@link Filter.Expression }
	 * 
	 */
	public Filter.Expression createFilterExpression() {
		return new Filter.Expression();
	}

	/**
	 * Create an instance of {@link OriginalStream.Tnt4JProperties }
	 * 
	 */
	public OriginalStream.Tnt4JProperties createOriginalStreamTnt4JProperties() {
		return new OriginalStream.Tnt4JProperties();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Reference }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "parser-ref", scope = OriginalStream.class)
	public JAXBElement<Reference> createOriginalStreamParserRef(Reference value) {
		return new JAXBElement<> (_OriginalStreamParserRef_QNAME, Reference.class, OriginalStream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link OriginalStream.Tnt4JProperties }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "tnt4j-properties", scope = OriginalStream.class)
	public JAXBElement<OriginalStream.Tnt4JProperties> createOriginalStreamTnt4JProperties(
			OriginalStream.Tnt4JProperties value) {
		return new JAXBElement<> (_OriginalStreamTnt4JProperties_QNAME, OriginalStream.Tnt4JProperties.class, OriginalStream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link StreamProperty }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "property", scope = OriginalStream.class)
	public JAXBElement<StreamProperty> createOriginalStreamProperty(StreamProperty value) {
		return new JAXBElement<> (_OriginalStreamProperty_QNAME, StreamProperty.class, OriginalStream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Reference }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "reference", scope = OriginalStream.class)
	public JAXBElement<Reference> createOriginalStreamReference(Reference value) {
		return new JAXBElement<> (_OriginalStreamReference_QNAME, Reference.class, OriginalStream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldTransform }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-transform", scope = FieldLocator.class)
	public JAXBElement<FieldTransform> createFieldLocatorFieldTransform(FieldTransform value) {
		return new JAXBElement<> (_FieldLocatorFieldTransform_QNAME, FieldTransform.class, FieldLocator.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldMap }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-map", scope = FieldLocator.class)
	public JAXBElement<FieldMap> createFieldLocatorFieldMap(FieldMap value) {
		return new JAXBElement<> (_FieldLocatorFieldMap_QNAME, FieldMap.class, FieldLocator.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldMapRef }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-map-ref", scope = FieldLocator.class)
	public JAXBElement<FieldMapRef> createFieldLocatorFieldMapRef(FieldMapRef value) {
		return new JAXBElement<> (_FieldLocatorFieldMapRef_QNAME, FieldMapRef.class, FieldLocator.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Filter }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "filter", scope = FieldLocator.class)
	public JAXBElement<Filter> createFieldLocatorFilter(Filter value) {
		return new JAXBElement<> (_FieldLocatorFilter_QNAME, Filter.class, FieldLocator.class, value);
	}

}
