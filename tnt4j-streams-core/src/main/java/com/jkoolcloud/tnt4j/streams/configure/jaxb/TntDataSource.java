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
 * Defines TNT4J-Streams data source configuration.
 * 
 * 
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="resource-ref" type="{}ResourceReference" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="java-object" type="{}JavaObject" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="parser" type="{}Parser" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="cache" type="{}Cache" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="stream" type="{}Stream" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "resourceRef", "javaObject", "parser", "cache", "stream" })
@XmlRootElement(name = "tnt-data-source")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public class TntDataSource {

	@XmlElement(name = "resource-ref")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<ResourceReference> resourceRef;
	@XmlElement(name = "java-object")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<JavaObject> javaObject;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Parser> parser;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Cache> cache;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Stream> stream;

	/**
	 * Gets the value of the resourceRef property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the resourceRef property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getResourceRef().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link ResourceReference }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<ResourceReference> getResourceRef() {
		if (resourceRef == null) {
			resourceRef = new ArrayList<ResourceReference>();
		}
		return this.resourceRef;
	}

	public void addResourceRef(ResourceReference r) {
		getResourceRef().add(r);
	}

	public void addResourceRef(String id, String type, String uri) {
		getResourceRef().add(new ResourceReference(id, type, uri));
	}

	public void addResourceRef(String id, ResourceReferenceType type, String uri) {
		getResourceRef().add(new ResourceReference(id, type, uri));
	}

	/**
	 * Gets the value of the javaObject property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the javaObject property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getJavaObject().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JavaObject }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<JavaObject> getJavaObject() {
		if (javaObject == null) {
			javaObject = new ArrayList<JavaObject>();
		}
		return this.javaObject;
	}

	public void addJavaObject(JavaObject j) {
		getJavaObject().add(j);
	}

	/**
	 * Gets the value of the parser property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the parser property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getParser().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Parser }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Parser> getParser() {
		if (parser == null) {
			parser = new ArrayList<Parser>();
		}
		return this.parser;
	}

	public void addParser(Parser p) {
		getParser().add(p);
	}

	/**
	 * Gets the value of the cache property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the cache property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getCache().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Cache }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-09-20T01:17:34+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Cache> getCache() {
		if (cache == null) {
			cache = new ArrayList<Cache>();
		}
		return this.cache;
	}

	public void addCache(Cache c) {
		getCache().add(c);
	}

	/**
	 * Gets the value of the stream property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the stream property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getStream().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Stream }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Stream> getStream() {
		if (stream == null) {
			stream = new ArrayList<Stream>();
		}
		return this.stream;
	}

	public void addStream(Stream s) {
		getStream().add(s);
	}
}
