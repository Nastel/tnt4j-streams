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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.streams.transform.FuncFindRegex;
import com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName;
import com.jkoolcloud.tnt4j.streams.transform.FuncGetObjectName;
import com.jkoolcloud.tnt4j.streams.transform.FuncReplaceRegex;

/**
 * General XML utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class StreamsXMLUtils {
	private static final String STREAMS_NS = "ts"; // NON-NLS
	private static final String STREAMS_NS_URI = "http://github.com/Nastel/tnt4j-streams"; // NON-NLS

	private static NamespaceMap tsContext;
	private static StreamsFunctionResolver tsFunctionResolver;

	static {
		tsContext = new NamespaceMap();
		tsFunctionResolver = new StreamsFunctionResolver();

		tsContext.setPrefixUriMapping(STREAMS_NS, STREAMS_NS_URI);
	}

	private StreamsXMLUtils() {
	}

	/**
	 * Adds XPath function to custom functions registry.
	 * <p>
	 * To call these functions from script code use 'ts:' prefix: e.g., 'ts:getFileName()'
	 *
	 * @param functionName
	 *            name of custom function to be used
	 * @param function
	 *            custom XPath function to add to registry
	 * @return previously registered function for defined function name, or {@code null} if function was not previously
	 *         registered.
	 */
	public static XPathFunction registerCustomFunction(String functionName, XPathFunction function) {
		return StreamsFunctionResolver.fMap.put(functionName, function);
	}

	/**
	 * Initiates new instance of {@link XPath} and decorates it with TNT4J-Streams namespace context 'ts:' and custom
	 * functions resolver.
	 *
	 * @return new instance of an {@link XPath}
	 *
	 * @see com.jkoolcloud.tnt4j.streams.utils.NamespaceMap
	 * @see com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName
	 * @see com.jkoolcloud.tnt4j.streams.transform.FuncGetObjectName
	 * @see com.jkoolcloud.tnt4j.streams.transform.FuncReplaceRegex
	 * @see com.jkoolcloud.tnt4j.streams.transform.FuncFindRegex
	 */
	public static XPath getStreamsXPath() {
		XPath xPath = XPathFactory.newInstance().newXPath();

		xPath.setNamespaceContext(tsContext);
		xPath.setXPathFunctionResolver(tsFunctionResolver);

		return xPath;
	}

	private static class StreamsFunctionResolver implements XPathFunctionResolver {
		private static Map<String, XPathFunction> fMap = new HashMap<>(2);

		static {
			fMap.put(FuncGetFileName.FUNCTION_NAME, new FuncGetFileName());
			fMap.put(FuncGetObjectName.FUNCTION_NAME, new FuncGetObjectName());
			fMap.put(FuncReplaceRegex.FUNCTION_NAME, new FuncReplaceRegex());
			fMap.put(FuncFindRegex.FUNCTION_NAME, new FuncFindRegex());
		}

		@Override
		public XPathFunction resolveFunction(QName functionName, int arity) {
			if (functionName.getNamespaceURI().equalsIgnoreCase(STREAMS_NS_URI)) {
				return fMap.get(functionName.getLocalPart());
			}

			return null;
		}
	}

	/**
	 * Parses <tt>is</tt> defined XML contents using <tt>dh</tt> SAX parser handler.
	 *
	 * 
	 * @param is
	 *            input stream to read XML contents
	 * @param dh
	 *            SAX parser handler instance to use for parsing
	 * @throws ParserConfigurationException
	 *             if a parser cannot be created which satisfies the requested configuration
	 * @throws SAXException
	 *             if any SAX errors occur during parser initialization or XML parsing
	 * @throws IOException
	 *             if any I/O errors occur
	 */
	public static void parse(InputStream is, DefaultHandler dh)
			throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		parser.parse(is, dh);
	}

	/**
	 * Resolves XML Document DOM node defined namespace mappings and puts them to <tt>namespaces</tt> map.
	 *
	 * @param xmlDoc
	 *            xml document DOM node to use
	 * @param namespaces
	 *            namespaces map to put resolved mappings to
	 * @param topOnly
	 *            flag indicating to resolve only top level namespace definitions
	 */
	public static void resolveDocumentNamespaces(Node xmlDoc, NamespaceMap namespaces, boolean topOnly) {
		if (xmlDoc instanceof Document) {
			xmlDoc = ((Document) xmlDoc).getDocumentElement();
		}

		NamedNodeMap attrs = xmlDoc == null ? null : xmlDoc.getAttributes();
		if (attrs == null) {
			return;
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			if (attr.getNamespaceURI() != null && attr.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
				String ns = attr.getLocalName();
				if (ns.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
					ns = XMLConstants.DEFAULT_NS_PREFIX;
				}

				namespaces.setPrefixUriMapping(ns, attr.getNodeValue());
			}
		}

		if (!topOnly) {
			NodeList children = xmlDoc.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					resolveDocumentNamespaces(child, namespaces, false);
				}
			}
		}
	}
}
