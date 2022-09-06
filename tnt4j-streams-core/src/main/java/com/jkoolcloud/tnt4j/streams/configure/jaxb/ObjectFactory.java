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

	private final static QName _StreamParserRef_QNAME = new QName("", "parser-ref");
	private final static QName _StreamTnt4JProperties_QNAME = new QName("", "tnt4j-properties");
	private final static QName _StreamProperty_QNAME = new QName("", "property");
	private final static QName _StreamReference_QNAME = new QName("", "reference");
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
	 * Create an instance of {@link Stream }
	 * 
	 */
	public Stream createStream() {
		return new Stream();
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
	 * Create an instance of {@link Cache }
	 *
	 */
	public Cache createCache() {
		return new Cache();
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
	 * Create an instance of {@link Property }
	 * 
	 */
	public Property createProperty() {
		return new Property();
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
	 * Create an instance of {@link Stream.Tnt4JProperties }
	 * 
	 */
	public Stream.Tnt4JProperties createStreamTnt4JProperties() {
		return new Stream.Tnt4JProperties();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Reference }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "parser-ref", scope = Stream.class)
	public JAXBElement<Reference> createStreamParserRef(Reference value) {
		return new JAXBElement<Reference>(_StreamParserRef_QNAME, Reference.class, Stream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Stream.Tnt4JProperties }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "tnt4j-properties", scope = Stream.class)
	public JAXBElement<Stream.Tnt4JProperties> createStreamTnt4JProperties(Stream.Tnt4JProperties value) {
		return new JAXBElement<Stream.Tnt4JProperties>(_StreamTnt4JProperties_QNAME, Stream.Tnt4JProperties.class,
				Stream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link StreamProperty }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "property", scope = Stream.class)
	public JAXBElement<StreamProperty> createStreamProperty(StreamProperty value) {
		return new JAXBElement<StreamProperty>(_StreamProperty_QNAME, StreamProperty.class, Stream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Reference }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "reference", scope = Stream.class)
	public JAXBElement<Reference> createStreamReference(Reference value) {
		return new JAXBElement<Reference>(_StreamReference_QNAME, Reference.class, Stream.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldTransform }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-transform", scope = FieldLocator.class)
	public JAXBElement<FieldTransform> createFieldLocatorFieldTransform(FieldTransform value) {
		return new JAXBElement<FieldTransform>(_FieldLocatorFieldTransform_QNAME, FieldTransform.class,
				FieldLocator.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldMap }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-map", scope = FieldLocator.class)
	public JAXBElement<FieldMap> createFieldLocatorFieldMap(FieldMap value) {
		return new JAXBElement<FieldMap>(_FieldLocatorFieldMap_QNAME, FieldMap.class, FieldLocator.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldMapRef }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-map-ref", scope = FieldLocator.class)
	public JAXBElement<FieldMapRef> createFieldLocatorFieldMapRef(FieldMapRef value) {
		return new JAXBElement<FieldMapRef>(_FieldLocatorFieldMapRef_QNAME, FieldMapRef.class, FieldLocator.class,
				value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link Filter }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "filter", scope = FieldLocator.class)
	public JAXBElement<Filter> createFieldLocatorFilter(Filter value) {
		return new JAXBElement<Filter>(_FieldLocatorFilter_QNAME, Filter.class, FieldLocator.class, value);
	}

}
