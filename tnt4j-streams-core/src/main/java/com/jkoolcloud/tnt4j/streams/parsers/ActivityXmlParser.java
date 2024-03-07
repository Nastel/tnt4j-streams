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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an XML string, with the value for each
 * field being retrieved from a particular XML element or attribute.
 * <p>
 * This parser supports reading the activity data from several types of input sources, and supports input streams
 * containing multiple XML documents. If there are multiple XML documents, each document must start with
 * {@code "<?xml ...>"}, and be separated by a new line.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>Namespace - additional XML namespace mappings. (Optional)</li>
 * <li>NamespaceAware - indicates that parser has to provide support for XML namespaces. Default value - {@code true}.
 * (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#ParserProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#SystemProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#EnvVariable}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityXmlParser extends GenericActivityParser<Node> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityXmlParser.class);

	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String UNITS_ATTR = "units"; // NON-NLS
	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String FORMAT_ATTR = "format"; // NON-NLS
	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String LOCALE_ATTR = "locale"; // NON-NLS

	/**
	 * Contains the XML namespace mappings.
	 */
	protected NamespaceMap namespaces = null;

	private XPath xPath;
	private static final Map<String, XPathExpression> expCache = new HashMap<>();
	private DocumentBuilder builder;

	private final Lock xPathLock = new ReentrantLock();
	private final Lock builderLock = new ReentrantLock();

	/**
	 * Property indicating that parser shall be namespace aware.
	 */
	protected boolean namespaceAware = true;

	/**
	 * Constructs a new activity XML string parser.
	 */
	public ActivityXmlParser() {
		super(ActivityFieldDataType.AsInput);
	}

	/**
	 * Initiates DOM document builder and XPath compiler.
	 *
	 * @param uNamespaces
	 *            custom namespace prefix and URI mappings
	 * 
	 * @throws ParserConfigurationException
	 *             if any errors configuring the parser
	 */
	protected synchronized void initXmlParser(Map<String, String> uNamespaces) throws ParserConfigurationException {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(namespaceAware);
		domFactory.setValidating(false);

		builder = domFactory.newDocumentBuilder();
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) {
				return new InputSource(new StringReader(""));
			}
		});

		xPath = StreamsXMLUtils.getStreamsXPath();

		if (namespaces == null) {
			if (xPath.getNamespaceContext() instanceof NamespaceMap) {
				namespaces = (NamespaceMap) xPath.getNamespaceContext();
			} else {
				namespaces = new NamespaceMap();
				xPath.setNamespaceContext(namespaces);
			}
		}

		namespaces.setPrefixUriMapping(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
		namespaces.setPrefixUriMapping("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI); // NON-NLS

		namespaces.addPrefixUriMappings(uNamespaces);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		Map<String, String> uNamespaces = new HashMap<>();

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();
				if (ParserProperties.PROP_NAMESPACE.equalsIgnoreCase(name)) {
					if (StringUtils.isNotEmpty(value)) {
						String[] nSpaces = StreamsConstants.getMultiProperties(value);
						for (String nSpace : nSpaces) {
							String[] nsFields = nSpace.split("="); // NON-NLS
							uNamespaces.put(nsFields[0], nsFields[1]);
							logger().log(OpLevel.DEBUG,
									StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
									"ActivityXmlParser.adding.mapping", name, nSpace);
						}
					}
				} else if (ParserProperties.PROP_NAMESPACE_AWARE.equalsIgnoreCase(name)) {
					if (StringUtils.isNotEmpty(value)) {
						namespaceAware = Utils.toBoolean(value);
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.setting", name, value);
					}
				}
			}
		}

		try {
			initXmlParser(uNamespaces);
		} catch (ParserConfigurationException exc) {
			throw new RuntimeException(exc);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_NAMESPACE_AWARE.equalsIgnoreCase(name)) {
			return namespaceAware;
		}

		return super.getProperty(name);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.w3c.dom.Node}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return data instanceof Node || super.isDataClassSupportedByParser(data);
	}

	@Override
	public boolean canHaveDelimitedLocators() {
		return false;
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		Node xmlDoc;
		String xmlString = null;
		try {
			if (data instanceof Node) {
				xmlDoc = (Node) data;

				// make sure document has all namespace related data, since we can't assure if document producer took
				// care on this
				if (namespaceAware) {
					Document tDoc = xmlDoc.getOwnerDocument();
					// Element docElem = tDoc == null ? null : tDoc.getDocumentElement();
					if (tDoc != null && StringUtils.isEmpty(tDoc.getNamespaceURI())) {
						xmlDoc = nodeToDoc(xmlDoc);
					}
				}
			} else {
				xmlString = getNextActivityString(data);
				if (StringUtils.isEmpty(xmlString)) {
					return null;
				}
				xmlDoc = parseXmlDoc(IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityXmlParser.xmlDocument.parse.error", xmlString), 0);
			pe.initCause(e);

			throw pe;
		}

		StreamsXMLUtils.resolveDocumentNamespaces(xmlDoc, namespaces, true);

		if (xmlString == null) {
			try {
				xmlString = Utils.documentToString(xmlDoc);
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityXmlParser.xmlDocument.toString.error", exc);
			}
		}

		ActivityContext cData = new ActivityContext(stream, data, xmlDoc).setParser(this);
		cData.setMessage(xmlString);
		cData.setRawData(xmlString);

		return cData;
	}

	@Override
	protected void parseFields(ActivityContext cData) throws Exception {
		String[] savedFormats = null;
		String[] savedUnits = null;
		String[] savedLocales = null;
		// apply fields for parser
		Object[] values;
		for (ActivityField aField : fieldList) {
			values = null;
			cData.setField(aField);
			List<ActivityFieldLocator> locators = aField.getLocators();
			if (locators != null) {
				// need to save format and units specification from config
				// in case individual entry in activity data overrides it
				if (ArrayUtils.getLength(savedFormats) < locators.size()) {
					savedFormats = new String[locators.size()];
					savedUnits = new String[locators.size()];
					savedLocales = new String[locators.size()];
				}

				values = parseLocatorValues(locators, cData);
				for (int li = 0; li < locators.size(); li++) {
					ActivityFieldLocator loc = locators.get(li);
					savedFormats[li] = loc.getFormat();
					savedUnits[li] = loc.getUnits();
					savedLocales[li] = loc.getLocale();
				}
			}

			Object val = Utils.simplifyValue(values);

			// if (val != null && aField.isEmptyAsNull() && Utils.isEmptyContent(val, true)) {
			// logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
			// "ActivityParser.field.empty.as.null", aField, toString(val));
			// val = null;
			// }

			applyFieldValue(aField, val, cData);
			if (locators != null && savedFormats != null) {
				for (int li = 0; li < locators.size(); li++) {
					ActivityFieldLocator loc = locators.get(li);
					loc.setFormat(savedFormats[li], savedLocales[li]);
					loc.setUnits(savedUnits[li]);
				}
			}
		}
	}

	/**
	 * Gets field raw data value resolved by locator and formats it according locator definition.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object XML DOM document
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value or applying locator format properties to specified
	 *             value
	 *
	 * @see #getTextOnDemand(org.w3c.dom.Node, com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext,
	 *      java.util.concurrent.atomic.AtomicBoolean)
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();

		if (ActivityField.isDynamicAttr(locStr)) {
			ActivityInfo ai = cData.getActivity();
			locStr = StreamsCache.fillInKeyPattern(locStr, ai, getName());
		}

		if (StringUtils.isNotEmpty(locStr)) {
			try {
				if (locStr.startsWith(StreamsConstants.PARENT_REFERENCE_PREFIX)) {
					XPathExpression expr = getXPathExpr(
							locStr.substring(StreamsConstants.PARENT_REFERENCE_PREFIX.length()));
					val = resolveValueOverXPath(cData.getParentContext(), expr);
				} else {
					XPathExpression expr = getXPathExpr(locStr);
					Node nodeDocument = cData.getData();

					if (nodeDocument != null) { // try expression relative to node
						val = resolveValueOverXPath(nodeDocument, expr);
					}
					if (val == null) { // otherwise try on context document
						Object rawData = cData.getRawData();
						Node xmlDoc = rawData instanceof Node ? ((Node) rawData).getOwnerDocument() : null;
						if (xmlDoc != null) {
							val = resolveValueOverXPath(xmlDoc, expr);
						}
					}
				}

				if (!locator.hasTransformationsOfPhase(ValueTransformation.Phase.RAW)) {
					if (val instanceof Node) {
						val = getTextOnDemand((Node) val, locator, cData, formattingNeeded);
					} else if (Utils.isObjCollection(val)) {
						Object[] nodes = Utils.makeArray(val, Object.class);
						for (int i = 0; i < nodes.length; i++) {
							if (nodes[i] instanceof Node) {
								nodes[i] = getTextOnDemand((Node) nodes[i], locator, cData, formattingNeeded);
							}
						}
						val = Utils.makeArray(nodes);
					}
				}
			} catch (XPathExpressionException exc) {
				ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityXMLParser.xPath.exception"), 0);
				pe.initCause(exc);
				throw pe;
			}
		}
		return val;
	}

	private Node parseXmlDoc(InputStream ins) throws SAXException, IOException {
		builderLock.lock();
		try {
			return builder.parse(ins);
		} finally {
			builderLock.unlock();
		}
	}

	private Node nodeToDoc(Node node) throws SAXException, IOException {
		builderLock.lock();
		try {
			Document newDocument = builder.newDocument();
			Node importedNode = newDocument.importNode(node, true);
			newDocument.appendChild(importedNode);

			return newDocument;
		} finally {
			builderLock.unlock();
		}
	}

	private XPathExpression getXPathExpr(String locStr) throws XPathExpressionException {
		xPathLock.lock();
		try {
			XPathExpression exp = expCache.get(locStr);
			if (exp == null) {
				exp = xPath.compile(locStr);
				expCache.put(locStr, exp);
			}

			return exp;
		} finally {
			xPathLock.unlock();
		}
	}

	/**
	 * Retrieves provided {@code node} XML text, if field bound stacked parser does not support {@link Node} type data.
	 * When stacked parser supports {@link Node} type data, parameters defined {@code node} instance is returned.
	 *
	 * @param node
	 *            XML document node to retrieve text on demand
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object XML DOM document
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return resolved textual value formatted based on the locator's formatting properties, or parameter {@code node}
	 *         defined {@link Node} if stacked parser supports that type of data
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 *
	 * @see #getTextContent(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator, org.w3c.dom.Node,
	 *      java.util.concurrent.atomic.AtomicBoolean)
	 */
	protected Object getTextOnDemand(Node node, ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		if (!isDataSupportedByStackedParser(cData.getField(), node)) {
			return getTextContent(locator, node, formattingNeeded);
		}

		return node;
	}

	private static Object resolveValueOverXPath(Node xmlDoc, XPathExpression expr) throws XPathExpressionException {
		Object val = null;
		NodeList nodes = null;
		try {
			nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
		} catch (XPathException exc) {
			val = expr.evaluate(xmlDoc);
		}

		int length = nodes == null ? 0 : nodes.getLength();

		if (length > 0) {
			List<Object> valuesList = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				Node node = nodes.item(i);
				valuesList.add(node);
			}

			val = Utils.simplifyValue(valuesList);
		}
		return val;
	}

	@SuppressWarnings("unchecked")
	private static Object resolveValueOverXPath(ActivityParserContext pcData, XPathExpression expr)
			throws XPathExpressionException {
		Object val = null;
		ActivityContext pc = (ActivityContext) pcData;
		Node xmlDoc = pc == null ? null : pc.getData();
		if (xmlDoc != null) {
			val = resolveValueOverXPath(xmlDoc, expr);

			if (val == null) {
				val = resolveValueOverXPath(pc.getParentContext(), expr);
			}
		}

		return val;
	}

	/**
	 * Resolves DOM node contained textual data and formats it using provided locator.
	 *
	 * @param locator
	 *            locator instance to alter using XML attributes contained data type, format and units used to format
	 *            resolved value
	 * @param node
	 *            DOM node to collect textual data
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return resolved textual value formatted based on the locator's formatting properties
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * 
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected static Object getTextContent(ActivityFieldLocator locator, Node node, AtomicBoolean formattingNeeded)
			throws ParseException {
		String strValue = node.getTextContent();
		Node attrsNode = node;

		if (node instanceof Attr) {
			Attr attr = (Attr) node;

			attrsNode = attr.getOwnerElement();
		}

		// Get list of attributes and their values for current element
		NamedNodeMap attrsMap = attrsNode == null ? null : attrsNode.getAttributes();
		if (attrsMap != null && attrsMap.getLength() > 0) {
			ActivityFieldLocator locCopy = locator.clone();

			Node attr = getFormattingAttr(attrsMap, DATA_TYPE_ATTR);
			String attrVal = attr == null ? null : attr.getTextContent();
			if (StringUtils.isNotEmpty(attrVal)) {
				locCopy.setDataType(ActivityFieldDataType.valueOf(attrVal));
			}

			attr = getFormattingAttr(attrsMap, FORMAT_ATTR);
			attrVal = attr == null ? null : attr.getTextContent();
			if (StringUtils.isNotEmpty(attrVal)) {
				attr = attrsMap.getNamedItem(LOCALE_ATTR);
				String attrLVal = attr == null ? null : attr.getTextContent();

				locCopy.setFormat(attrVal, StringUtils.isEmpty(attrLVal) ? locator.getLocale() : attrLVal);
			}

			attr = getFormattingAttr(attrsMap, UNITS_ATTR);
			attrVal = attr == null ? null : attr.getTextContent();
			if (StringUtils.isNotEmpty(attrVal)) {
				locCopy.setUnits(attrVal);
			}

			Object fValue = locCopy.formatValue(strValue.trim());
			formattingNeeded.set(false);

			return fValue;
		}

		return strValue.trim();
	}

	private static Node getFormattingAttr(NamedNodeMap attrsMap, String attrName) {
		if (attrsMap != null) {
			int attrsCount = attrsMap.getLength();

			for (int i = 0; i < attrsCount; i++) {
				Node attr = attrsMap.item(i);
				if (attr.getNodeName().equalsIgnoreCase(attrName)) {
					return attr;
				}
			}
		}

		return null;
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "XML", "TEXT" }; // NON-NLS

	/**
	 * Returns types of RAW activity data entries.
	 *
	 * @return types of RAW activity data entries - {@code "XML"} and {@code "TEXT"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
