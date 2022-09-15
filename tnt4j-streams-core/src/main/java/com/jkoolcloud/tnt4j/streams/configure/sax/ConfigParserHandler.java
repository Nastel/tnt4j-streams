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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.config.Configurable;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigData;
import com.jkoolcloud.tnt4j.streams.configure.jaxb.ResourceReferenceType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldMappingType;
import com.jkoolcloud.tnt4j.streams.filters.AbstractExpressionFilter;
import com.jkoolcloud.tnt4j.streams.filters.DefaultValueFilter;
import com.jkoolcloud.tnt4j.streams.filters.StreamFiltersGroup;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.outputs.TNTStreamOutput;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser;
import com.jkoolcloud.tnt4j.streams.reference.MatchingParserReference;
import com.jkoolcloud.tnt4j.streams.transform.AbstractScriptTransformation;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements the SAX DefaultHandler for parsing TNT4J-Streams configuration.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader
 * @see com.jkoolcloud.tnt4j.streams.configure.sax.StreamsConfigSAXParser
 */
public class ConfigParserHandler extends DefaultHandler {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ConfigParserHandler.class);

	/**
	 * Constant for default location delimiter in configuration definition.
	 */
	public static final String LOC_DELIM = "|"; // NON-NLS

	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String CONFIG_ROOT_ELMT = "tnt-data-source"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String PARSER_ELMT = "parser"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String STREAM_ELMT = "stream"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String PROPERTY_ELMT = "property"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_ELMT = "field"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_MAP_ELMT = "field-map"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_MAP_REF_ELMT = "field-map-ref"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_LOC_ELMT = "field-locator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_TRANSFORM_ELMT = "field-transform"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String PARSER_REF_ELMT = "parser-ref"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String TNT4J_PROPERTIES_ELMT = "tnt4j-properties"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FILTER_ELMT = "filter"; // NON-NLS
	// /**
	// * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	// */
	// private static final String RULE_ELMT = "rule"; // NON-NLS
	// /**
	// * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	// */
	// private static final String STEP_ELMT = "step"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String VALUE_ELMT = "value"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String EXPRESSION_ELMT = "expression"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String REF_ELMT = "reference"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String JAVA_OBJ_ELMT = "java-object"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String PARAM_ELMT = "param"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String EMBEDDED_ACTIVITY_ELMT = "embedded-activity"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String CACHE_ELMT = "cache"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String CACHE_ENTRY_ELMT = "entry"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String CACHE_KEY_ELMT = "key"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String RESOURCE_REF_ELMT = "resource-ref"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String CACHE_DEFAULT_VALUE_ELMT = "default"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String MATCH_EXP_ELMT = "matchExp"; // NON-NLS

	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String NAME_ATTR = "name"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String CLASS_ATTR = "class"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String VALUE_ATTR = "value"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LOC_TYPE_ATTR = "locator-type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LOCATOR_ATTR = "locator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String VALUE_TYPE_ATTR = "value-type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String SPLIT_ATTR = "split"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String SEPARATOR_ATTR = "separator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String RADIX_ATTR = "radix"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String UNITS_ATTR = "units"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String FORMAT_ATTR = "format"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LOCALE_ATTR = "locale"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TIMEZONE_ATTR = "timezone"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String CHARSET_ATTR = "charset"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String EMPTY_AS_NULL = "emptyAsNull"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String SOURCE_ATTR = "source";
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TARGET_ATTR = "target"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String TYPE_ATTR = "type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TAGS_ATTR = "tags"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String REQUIRED_ATTR = "required"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String TRANSPARENT_ATTR = "transparent"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String ID_ATTR = "id"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String BEAN_REF_ATTR = "beanRef"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LANG_ATTR = "lang"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String HANDLE_ATTR = "handle"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String EVALUATION_ATTR = "evaluation"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String AGGREGATION_ATTR = "aggregation"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String APPLY_ON_ATTR = "applyOn"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String FORMATTING_PATTERN_ATTR = "formattingPattern"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String URI_ATTR = "uri"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String RESOURCE_ATTR = "resource"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String AUTO_SORT_ATTR = "manualFieldsOrder"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String PHASE_ATTR = "phase"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String TRANSIENT_ATTR = "transient"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String DEFAULT_TYPE_ATTR = "default-data-type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String DEFAULT_EMPTY_AS_NULL = "default-emptyAsNull"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration entity {@value}.
	 */
	private static final String CDATA = "<![CDATA[]]>"; // NON-NLS

	/**
	 * Currently configured TNT input stream.
	 */
	protected TNTInputStream<?, ?> currStream = null;
	protected Map<String, Map<String, String>> currProperties = null;
	private ActivityParser currParser = null;
	private ActivityFieldData currField = null;
	@SuppressWarnings("rawtypes")
	private StreamFiltersGroup currFilter = null;

	private StreamsConfigData streamsConfigData = null;
	private Map<String, Object> javaObjectsMap = null;

	private Map<String, Map<String, ?>> resourcesMap;

	/**
	 * Configuration parsing locator.
	 */
	protected Locator currParseLocation = null;

	private JavaObjectData javaObjectData = null;
	protected Property currProperty = null;
	private FieldLocatorData currLocatorData = null;
	private FieldTransformData currTransform = null;
	private FilterValueData currFilterValue = null;
	private FilterExpressionData currFilterExpression = null;
	private CacheEntryData currCacheEntry = null;
	private ParserRefData currParserRef = null;

	/**
	 * Buffers stack to put current configuration element (token) characters data value.
	 */
	protected Stack<StringBuilder> elementDataStack;

	private boolean include = false;
	private Stack<String> path;

	private boolean validateScriptExpressions = true;

	/**
	 * Constructs a new ConfigurationParserHandler.
	 */
	public ConfigParserHandler() {
	}

	/**
	 * Sets flag indicating whether to check script expressions.
	 * 
	 * @param validateScriptExpressions
	 *            flag indicating whether to validate configuration contained script expressions
	 */
	public void setValidateScriptExpressions(boolean validateScriptExpressions) {
		this.validateScriptExpressions = validateScriptExpressions;
	}

	/**
	 * Returns streams and parsers data loaded from configuration.
	 *
	 * @return un-marshaled streams and parsers data
	 */
	public StreamsConfigData getStreamsConfigData() {
		return streamsConfigData;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		currParseLocation = locator;
	}

	@Override
	public void startDocument() throws SAXException {
		if (include) {
			return;
		}

		currStream = null;
		currProperties = new HashMap<>();
		currParser = null;
		currField = null;
		currFilter = null;
		currLocatorData = null;
		streamsConfigData = new StreamsConfigData();
		javaObjectsMap = new HashMap<>();

		path = new Stack<>();
		elementDataStack = new Stack<>();
	}

	@Override
	public void endDocument() throws SAXException {
		if (include) {
			return;
		}

		javaObjectsMap.clear();
		if (resourcesMap != null) {
			resourcesMap.clear();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		path.push(qName);
		startCfgElement(qName, attributes);
	}

	/**
	 * Receives notification of the start of an stream configuration element.
	 * 
	 * @param qName
	 *            the qualified element name
	 * @param attributes
	 *            the element attributes list
	 * @throws SAXException
	 *             if malformed configuration found
	 */
	protected void startCfgElement(String qName, Attributes attributes) throws SAXException {
		if (CONFIG_ROOT_ELMT.equals(qName)) {
			if (streamsConfigData.isStreamsAvailable()) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.multiple.elements", qName), currParseLocation);
			}
		} else if (PROPERTY_ELMT.equals(qName)) {
			processProperty(attributes);
		} else if (FIELD_ELMT.equals(qName)) {
			processField(attributes);
		} else if (FIELD_LOC_ELMT.equals(qName)) {
			processFieldLocator(attributes);
		} else if (FIELD_MAP_ELMT.equals(qName)) {
			processFieldMap(attributes);
		} else if (PARSER_REF_ELMT.equals(qName)) {
			processParserRef(attributes);
		} else if (PARSER_ELMT.equals(qName)) {
			processParser(attributes);
		} else if (STREAM_ELMT.equals(qName)) {
			processStream(attributes);
		} else if (FILTER_ELMT.equals(qName)) {
			processFilter(attributes);
		} else if (VALUE_ELMT.equals(qName)) {
			processValue(attributes);
		} else if (EXPRESSION_ELMT.equals(qName)) {
			processFilterExpression(attributes);
		} else if (TNT4J_PROPERTIES_ELMT.equals(qName)) {
			processTNT4JProperties(attributes);
		} else if (REF_ELMT.equals(qName)) {
			processReference(attributes);
		} else if (JAVA_OBJ_ELMT.equals(qName)) {
			processJavaObject(attributes);
		} else if (PARAM_ELMT.equals(qName)) {
			processParam(attributes);
		} else if (FIELD_TRANSFORM_ELMT.equals(qName)) {
			processFieldTransform(attributes);
		} else if (EMBEDDED_ACTIVITY_ELMT.equals(qName)) {
			processEmbeddedActivity(attributes);
		} else if (CACHE_ELMT.equals(qName)) {
			processCache(attributes);
		} else if (CACHE_ENTRY_ELMT.equals(qName)) {
			processCacheEntry(attributes);
		} else if (CACHE_KEY_ELMT.equals(qName)) {
			processKey(attributes);
		} else if (FIELD_MAP_REF_ELMT.equals(qName)) {
			processFieldMapReference(attributes);
		} else if (RESOURCE_REF_ELMT.equals(qName)) {
			processResourceReference(attributes);
		} else if (CACHE_DEFAULT_VALUE_ELMT.equals(qName)) {
			processDefault(attributes);
		} else if (MATCH_EXP_ELMT.equals(qName)) {
			processMatchExpression(attributes);
		} else {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.unknown.tag", qName), currParseLocation);
		}
	}

	/**
	 * Processes a {@code <cache>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processCache(Attributes attrs) throws SAXException {
		// for (int i = 0; i < attrs.getLength(); i++) {
		// String attName = attrs.getQName(i);
		// String attValue = attrs.getValue(i);
		// }
	}

	/**
	 * Processes a {@code <key>} element under {@code <entry>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processCacheEntry(Attributes attrs) throws SAXException {
		if (currCacheEntry != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", CACHE_ENTRY_ELMT), currParseLocation);
		}

		if (!StringUtils.equalsIgnoreCase(getParentElmt(CACHE_ENTRY_ELMT), CACHE_ELMT)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", CACHE_ENTRY_ELMT, CACHE_ELMT),
					currParseLocation);
		}

		if (currCacheEntry == null) {
			currCacheEntry = new CacheEntryData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (ID_ATTR.equals(attName)) {
				currCacheEntry.id = attValue;
			} else if (TRANSIENT_ATTR.equals(attName)) {
				currCacheEntry.transientEntry = Utils.toBoolean(attValue);
			} else {
				unknownAttribute(CACHE_ENTRY_ELMT, attName);
			}
		}

		notEmpty(currCacheEntry.id, CACHE_ENTRY_ELMT, ID_ATTR);
	}

	/**
	 * Processes a {@code <key>} element under {@code <entry>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processKey(Attributes attrs) throws SAXException {
		if (currCacheEntry == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", CACHE_KEY_ELMT, CACHE_ENTRY_ELMT),
					currParseLocation);
		}

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <default>} element under {@code <entry>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processDefault(Attributes attrs) throws SAXException {
		if (currCacheEntry == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", CACHE_DEFAULT_VALUE_ELMT, CACHE_ENTRY_ELMT),
					currParseLocation);
		}

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <parser>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processParser(Attributes attrs) throws SAXException {
		if (currParser != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", PARSER_ELMT), currParseLocation);
		}
		String name = null;
		String className = null;
		boolean autoArrange = true;
		ActivityFieldDataType defaultDataType = null;
		boolean defaultEmptyAsNull = true;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (CLASS_ATTR.equals(attName)) {
				className = attValue;
			} else if (AUTO_SORT_ATTR.equals(attName)) {
				autoArrange = !Utils.toBoolean(attValue);
			} else if (DEFAULT_TYPE_ATTR.equals(attName)) {
				defaultDataType = ActivityFieldDataType.valueOf(attValue);
			} else if (DEFAULT_EMPTY_AS_NULL.equals(attName)) {
				defaultEmptyAsNull = Utils.toBoolean(attValue);
			} else {
				unknownAttribute(PARSER_ELMT, attName);
			}
		}

		notEmpty(name, PARSER_ELMT, NAME_ATTR);
		notEmpty(className, PARSER_ELMT, CLASS_ATTR);
		if (streamsConfigData.getParser(name) != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.duplicate.parser.definition", name), currParseLocation);
		}
		try {
			Object newParser = Utils.createInstance(className);
			if (!(newParser instanceof ActivityParser)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ConfigParserHandler.not.implement.interface",
						PARSER_ELMT, CLASS_ATTR, className, ActivityParser.class.getName(), getLocationInfo()));
			}
			currParser = (ActivityParser) newParser;
		} catch (Exception exc) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.failed.to.load", PARSER_ELMT, CLASS_ATTR, className, getLocationInfo()), exc);
		}
		if (currParser != null) {
			currParser.setName(name);
			currParser.setProperty(ParserProperties.PROP_AUTO_ARRANGE_FIELDS, String.valueOf(autoArrange));
			if (defaultDataType != null) {
				currParser.setDefaultDataType(defaultDataType);
			}
			currParser.setDefaultEmptyAsNull(defaultEmptyAsNull);
			streamsConfigData.addParser(currParser);
		}
	}

	/**
	 * Processes a {@code <field>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processField(Attributes attrs) throws SAXException {
		if (currField != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_ELMT), currParseLocation);
		}
		if (currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", FIELD_ELMT, PARSER_ELMT),
					currParseLocation);
		}

		String field = null;
		ActivityFieldDataType dataType = null;
		String locatorType = null;
		String locator = null;
		String valueType = null;
		String separator = null;
		String pattern = null;
		String units = null;
		String format = null;
		String locale = null;
		String timeZone = null;
		String value = null;
		int radix = 10;
		String reqVal = "";
		Boolean transparent = null;
		boolean split = false;
		String id = null;
		String charset = null;
		Boolean emptyAsNull = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				field = attValue;
			} else if (DATA_TYPE_ATTR.equals(attName)) {
				dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				locator = attValue;
			} else if (VALUE_TYPE_ATTR.equals(attName)) {
				valueType = attValue;
			} else if (SEPARATOR_ATTR.equals(attName)) {
				separator = attValue;
			} else if (FORMATTING_PATTERN_ATTR.equals(attName)) {
				pattern = attValue;
			} else if (RADIX_ATTR.equals(attName)) {
				radix = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				units = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				format = attValue;
			} else if (LOCALE_ATTR.equals(attName)) {
				locale = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				timeZone = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			} else if (REQUIRED_ATTR.equals(attName)) {
				reqVal = attValue;
			} else if (TRANSPARENT_ATTR.equals(attName)) {
				transparent = Utils.toBoolean(attValue);
			} else if (SPLIT_ATTR.equals(attName)) {
				split = Utils.toBoolean(attValue);
			} else if (ID_ATTR.equals(attName)) {
				id = attValue;
			} else if (CHARSET_ATTR.equals(attName)) {
				charset = attValue;
			} else if (EMPTY_AS_NULL.equals(attName)) {
				emptyAsNull = Utils.toBoolean(attValue);
			} else {
				unknownAttribute(FIELD_ELMT, attName);
			}
		}
		// make sure required fields are present
		notEmpty(field, FIELD_ELMT, NAME_ATTR);

		if (separator != null && pattern != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_ELMT, SEPARATOR_ATTR, FORMATTING_PATTERN_ATTR),
					currParseLocation);
		}
		if (locator != null && value != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}

		if (dataType == null) {
			dataType = currParser.getDefaultDataType();
		}
		if (emptyAsNull == null) {
			emptyAsNull = currParser.isDefaultEmptyAsNull();
		}

		ActivityFieldLocator afl;
		ActivityField af = new ActivityField(field);
		currField = new ActivityFieldData(af);

		if (value != null) {
			currField.hasLocValAttr = true;
			afl = new ActivityFieldLocator(value);
			afl.setRadix(radix);
			afl.setRequired(reqVal);
			if (dataType != null) {
				afl.setDataType(dataType);
			}
			if (StringUtils.isNotEmpty(units)) {
				afl.setUnits(units);
			}
			if (StringUtils.isNotEmpty(format)) {
				afl.setFormat(format, locale);
			}
			if (StringUtils.isNotEmpty(timeZone)) {
				afl.setTimeZone(timeZone);
			}
			if (StringUtils.isNotEmpty(id)) {
				afl.setId(id);
			}
			if (StringUtils.isNotEmpty(charset)) {
				afl.setCharset(charset);
			}
			afl.setEmptyAsNull(emptyAsNull);
			currField.addLocator(afl);
		} else if (StringUtils.isNotEmpty(locator)) {
			currField.hasLocValAttr = true;
			String[] locators = currParser.canHaveDelimitedLocators() ? locator.split(Pattern.quote(LOC_DELIM))
					: new String[] { locator };
			for (String loc : locators) {
				if (StringUtils.isNotEmpty(loc)) {
					afl = new ActivityFieldLocator(locatorType, loc);
					afl.setRadix(radix);
					afl.setRequired(reqVal);
					if (dataType != null) {
						afl.setDataType(dataType);
					}
					if (StringUtils.isNotEmpty(units)) {
						afl.setUnits(units);
					}
					if (StringUtils.isNotEmpty(format)) {
						afl.setFormat(format, locale);
					}
					if (StringUtils.isNotEmpty(timeZone)) {
						afl.setTimeZone(timeZone);
					}
					if (StringUtils.isNotEmpty(id)) {
						afl.setId(id);
					}
					if (StringUtils.isNotEmpty(charset)) {
						afl.setCharset(charset);
					}
					afl.setEmptyAsNull(emptyAsNull);
					currField.addLocator(afl);
				}
			}
		} else if (StringUtils.isEmpty(locator)) {
			af.setGroupLocator(radix, reqVal, dataType, units, format, locale, timeZone, charset);
		}

		if (separator != null) {
			af.setSeparator(separator);
		}
		if (StringUtils.isNotEmpty(pattern)) {
			af.setFormattingPattern(pattern);
		}
		if (StringUtils.isNotEmpty(valueType)) {
			af.setValueType(valueType);
		}
		if (transparent != null) {
			af.setTransparent(transparent);
		}
		af.setRequired(reqVal);
		af.setSplitCollection(split);
		af.setEmptyAsNull(emptyAsNull);
	}

	/**
	 * Checks if attribute resolved {@link String} value is not empty.
	 *
	 * @param attrValue
	 *            attribute resolved value
	 * @param elemName
	 *            element name
	 * @param attrName
	 *            attribute name
	 * @throws SAXParseException
	 *             if attribute resolved {@link String} value is {@code null} or {@code ""}
	 */
	protected void notEmpty(String attrValue, String elemName, String attrName) throws SAXParseException {
		if (StringUtils.isEmpty(attrValue)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", elemName, attrName), currParseLocation);
		}
	}

	/**
	 * Checks if element resolved {@link String} value is not empty.
	 *
	 * @param attrValue
	 *            attribute resolved value
	 * @param elemName
	 *            element name
	 * @throws SAXParseException
	 *             if attribute resolved {@link String} value is {@code null} or {@code ""}
	 */
	protected void notEmpty(String attrValue, String elemName) throws SAXParseException {
		if (StringUtils.isEmpty(attrValue)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.element", elemName), currParseLocation);
		}
	}

	/**
	 * Checks if attribute resolved value is not {@code null}.
	 *
	 * @param attrValue
	 *            attribute resolved value
	 * @param elemName
	 *            element name
	 * @param attrName
	 *            attribute name
	 *
	 * @throws SAXParseException
	 *             if attribute resolved value is {@code null}
	 */
	protected void notNull(Object attrValue, String elemName, String attrName) throws SAXParseException {
		if (attrValue == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", elemName, attrName), currParseLocation);
		}
	}

	/**
	 * Processes a {@code <embedded-activity>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processEmbeddedActivity(Attributes attrs) throws SAXException {
		if (currField != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration5", EMBEDDED_ACTIVITY_ELMT, FIELD_ELMT),
					currParseLocation);
		}
		if (currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", EMBEDDED_ACTIVITY_ELMT, PARSER_ELMT),
					currParseLocation);
		}

		String field = null;
		String locatorType = null;
		String locator = null;
		String reqVal = "";
		boolean split = true;
		String id = null;
		ActivityFieldDataType dataType = null;
		String charset = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				field = attValue;
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				locator = attValue;
			} else if (REQUIRED_ATTR.equals(attName)) {
				reqVal = attValue;
			} else if (ID_ATTR.equals(attName)) {
				id = attValue;
			} else if (DATA_TYPE_ATTR.equals(attName)) {
				dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (CHARSET_ATTR.equals(attName)) {
				charset = attValue;
			} else {
				unknownAttribute(EMBEDDED_ACTIVITY_ELMT, attName);
			}
		}
		// make sure required fields are present
		notEmpty(field, EMBEDDED_ACTIVITY_ELMT, NAME_ATTR);
		notEmpty(locator, EMBEDDED_ACTIVITY_ELMT, LOCATOR_ATTR);
		notEmpty(locatorType, EMBEDDED_ACTIVITY_ELMT, LOC_TYPE_ATTR);

		ActivityFieldLocator afl;
		ActivityField af = new ActivityField(field);
		currField = new ActivityFieldData(af);

		if (dataType == null) {
			dataType = currParser.getDefaultDataType() == null ? ActivityFieldDataType.AsInput
					: currParser.getDefaultDataType();
		}

		String[] locators = currParser.canHaveDelimitedLocators() ? locator.split(Pattern.quote(LOC_DELIM))
				: new String[] { locator };
		for (String loc : locators) {
			if (StringUtils.isNotEmpty(loc)) {
				afl = new ActivityFieldLocator(locatorType, loc);
				afl.setRequired(reqVal);
				if (StringUtils.isNotEmpty(id)) {
					afl.setId(id);
				}
				if (dataType != null) {
					afl.setDataType(dataType);
				}
				if (StringUtils.isNotEmpty(charset)) {
					afl.setCharset(charset);
				}
				currField.addLocator(afl);
			}
		}

		af.setRequired(reqVal);
		af.setTransparent(true);
		af.setSplitCollection(split);
	}

	/**
	 * Processes a {@code <field-locator>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFieldLocator(Attributes attrs) throws SAXException {
		if (currLocatorData != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_LOC_ELMT), currParseLocation);
		}
		if (currField == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", FIELD_LOC_ELMT,
					Utils.arrayToString(FIELD_ELMT, EMBEDDED_ACTIVITY_ELMT)), currParseLocation);
		}
		if (!currField.field.hasDynamicAttrs() && currField.hasLocValAttr) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.has.both", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR, FIELD_LOC_ELMT,
					getLocationInfo()));
		}
		// if (currField.hasMapElmt) {
		// throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
		// "ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
		// getLocationInfo()));
		// }

		currLocatorData = new FieldLocatorData();

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (DATA_TYPE_ATTR.equals(attName)) {
				currLocatorData.dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				currLocatorData.locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				currLocatorData.locator = attValue;
			} else if (RADIX_ATTR.equals(attName)) {
				currLocatorData.radix = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				currLocatorData.units = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				currLocatorData.format = attValue;
			} else if (LOCALE_ATTR.equals(attName)) {
				currLocatorData.locale = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				currLocatorData.timeZone = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				currLocatorData.value = attValue;
			} else if (REQUIRED_ATTR.equals(attName)) {
				currLocatorData.reqVal = attValue;
			} else if (ID_ATTR.equals(attName)) {
				currLocatorData.id = attValue;
			} else if (CHARSET_ATTR.equals(attName)) {
				currLocatorData.charset = attValue;
			} else if (EMPTY_AS_NULL.equals(attName)) {
				currLocatorData.emptyAsNull = Utils.toBoolean(attValue);
			} else {
				unknownAttribute(FIELD_LOC_ELMT, attName);
			}
		}

		if (currLocatorData.dataType == null) {
			currLocatorData.dataType = currParser.getDefaultDataType();
		}
		if (currLocatorData.emptyAsNull == null) {
			currLocatorData.emptyAsNull = currParser.isDefaultEmptyAsNull();
		}

		// make sure any fields that are required based on other fields are specified
		if (ActivityFieldDataType.DateTime == currLocatorData.dataType) {
			if (currLocatorData.format == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, FORMAT_ATTR,
						currLocatorData.dataType), currParseLocation);
			}
			// if (locale == null)
			// {
			//
			// }
		} else if (ActivityFieldDataType.Timestamp == currLocatorData.dataType) {
			if (currLocatorData.units == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, UNITS_ATTR, currLocatorData.dataType),
						currParseLocation);
			}
		}
		// currField.hasLocElmt = true;

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <field-map>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFieldMap(Attributes attrs) throws SAXException {
		if (currField == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", FIELD_MAP_ELMT,
					Utils.arrayToString(FIELD_ELMT, FIELD_LOC_ELMT)), currParseLocation);
		}
		// if (currField.hasLocElmt && currLocatorData == null) {
		// throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
		// "ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
		// getLocationInfo()));
		// }
		if (CollectionUtils.isEmpty(currField.getLocators()) && currLocatorData == null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.no.binding", FIELD_MAP_ELMT, FIELD_LOC_ELMT, getLocationInfo()));
		}
		String source = null;
		String target = null;
		String type = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (SOURCE_ATTR.equals(attName)) {
				source = attValue;
			} else if (TARGET_ATTR.equals(attName)) {
				target = attValue;
			} else if (TYPE_ATTR.equals(attName)) {
				type = attValue;
			} else {
				unknownAttribute(FIELD_MAP_ELMT, attName);
			}
		}
		notNull(source, FIELD_MAP_ELMT, SOURCE_ATTR);
		notNull(target, FIELD_MAP_ELMT, TARGET_ATTR);

		if (currLocatorData != null) {
			currLocatorData.valueMapItems.add(new FieldLocatorData.ValueMapData(source, target,
					StringUtils.isEmpty(type) ? null : ActivityFieldMappingType.valueOf(type)));
		} else {
			// currField.hasMapElmt = true;
			if (currField.getLocators() != null) {
				for (ActivityFieldLocator loc : currField.getLocators()) {
					loc.addValueMap(source, target,
							StringUtils.isEmpty(type) ? null : ActivityFieldMappingType.valueOf(type));
				}
			}
		}
	}

	/**
	 * Processes a {@code <field-map-ref>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	@SuppressWarnings("unchecked")
	private void processFieldMapReference(Attributes attrs) throws SAXException {
		if (currField == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", FIELD_MAP_REF_ELMT,
					Utils.arrayToString(FIELD_ELMT, FIELD_LOC_ELMT)), currParseLocation);
		}

		if (CollectionUtils.isEmpty(currField.getLocators()) && currLocatorData == null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.no.binding", FIELD_MAP_REF_ELMT, FIELD_LOC_ELMT, getLocationInfo()));
		}

		String reference = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (RESOURCE_ATTR.equals(attName)) {
				reference = attValue;
			} else {
				unknownAttribute(FIELD_MAP_REF_ELMT, attName);
			}
		}

		notEmpty(reference, FIELD_MAP_REF_ELMT, RESOURCE_ATTR);

		Object val = Utils.getMapValueByPath(reference, resourcesMap);

		if (val instanceof Map) {
			AttributesImpl mappingAttrs = new AttributesImpl();
			for (Map.Entry<String, ?> entry : ((Map<String, ?>) val).entrySet()) {
				mappingAttrs.clear();
				mappingAttrs.addAttribute(null, null, SOURCE_ATTR, null, entry.getKey());
				mappingAttrs.addAttribute(null, null, TARGET_ATTR, null, String.valueOf(entry.getValue()));

				processFieldMap(mappingAttrs);
			}
		}
	}

	/**
	 * Processes a {@code <resource-ref>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processResourceReference(Attributes attrs) throws SAXException {
		String id = null;
		String type = null;
		String uri = null;
		String delim = null;

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (ID_ATTR.equals(attName)) {
				id = attValue;
			} else if (TYPE_ATTR.equals(attName)) {
				type = attValue;
			} else if (URI_ATTR.equals(attName)) {
				uri = attValue;
			} else if (SEPARATOR_ATTR.equals(attName)) {
				delim = attValue;
			} else {
				unknownAttribute(RESOURCE_REF_ELMT, attName);
			}
		}

		notEmpty(id, RESOURCE_REF_ELMT, ID_ATTR);
		notEmpty(type, RESOURCE_REF_ELMT, TYPE_ATTR);
		notEmpty(uri, RESOURCE_REF_ELMT, URI_ATTR);

		if (type.equals(ResourceReferenceType.VALUES_MAP.value())) {
			try (InputStream is = getResourceInputStream(id, uri)) {
				if (resourcesMap == null) {
					resourcesMap = new HashMap<>(5);
				}

				if (uri.toLowerCase().endsWith(".json")) { // NON-NLS
					resourcesMap.put(id, Utils.fromJsonToMap(is, false));
				} else if (uri.toLowerCase().endsWith(".csv")) { // NON-NLS
					resourcesMap.put(id, fromPropsToMap(is, StringUtils.isEmpty(delim) ? "," : delim)); // NON-NLS
				} else if (uri.toLowerCase().endsWith(".properties")) { // NON-NLS
					resourcesMap.put(id, fromPropsToMap(is, StringUtils.isEmpty(delim) ? "=" : delim)); // NON-NLS
				} else {
					throw new SAXParseException(
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"ConfigParserHandler.invalidResource", id, uri),
							currParseLocation);
				}
			} catch (Exception exc) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.resource.load.error", id, uri), exc);
			}
		} else if (type.equals(ResourceReferenceType.PARSER.value())) {
			try (InputStream is = getResourceInputStream(id, uri)) {
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				SAXParser parser = parserFactory.newSAXParser();
				include = true;
				parser.parse(is, this);
			} catch (Exception exc) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.resource.load.error", id, uri), exc);
			} finally {
				include = false;
			}
		} else {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.invalidRefType", id, type), currParseLocation);
		}
	}

	private static InputStream getResourceInputStream(String id, String uri) throws IOException {
		try {
			URL url = new URL(uri);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ConfigParserHandler.resource.ref.load", id, url);
			return url.openStream();
		} catch (MalformedURLException exc) {
			// try use uri as JVM work dir. relative path
			File file = new File(uri);
			if (file.exists()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ConfigParserHandler.resource.ref.load", id, file.getAbsolutePath());
				return Files.newInputStream(file.toPath());
			} else {
				// try use uri as streams cfg. file relative path
				if (!StringUtils.isEmpty(StreamsConfigSAXParser.cfgFilePath)) {
					File base = new File(StreamsConfigSAXParser.cfgFilePath);
					file = Paths.get(base.getParent(), uri).toFile();
					if (file.exists()) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ConfigParserHandler.resource.ref.load", id, file.getAbsolutePath());
						return Files.newInputStream(file.toPath());
					}
				}
			}

			throw new IOException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.resource.file.not.found", uri, file.getAbsolutePath()));
		}
	}

	private static Map<String, ?> fromPropsToMap(InputStream is, String delim) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		Map<String, String> propsMap = new HashMap<>(10);

		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(delim); // StringUtils.split (line, delim);

			String key = tokens.length > 0 ? tokens[0].trim() : "";
			String value = tokens.length > 1 ? tokens[1].trim() : "";

			propsMap.put(key, value);
		}

		return propsMap;
	}

	/**
	 * Processes a {@code <field-transform>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFieldTransform(Attributes attrs) throws SAXException {
		if (currTransform != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_TRANSFORM_ELMT), currParseLocation);
		}

		if (currField == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", FIELD_TRANSFORM_ELMT,
							Utils.arrayToString(FIELD_ELMT, EMBEDDED_ACTIVITY_ELMT, FIELD_LOC_ELMT)),
					currParseLocation);
		}

		if (currTransform == null) {
			currTransform = new FieldTransformData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				currTransform.name = attValue;
			} else if (BEAN_REF_ATTR.equals(attName)) {
				currTransform.beanRef = attValue;
			} else if (LANG_ATTR.equals(attName)) {
				currTransform.scriptLang = attValue;
			} else if (PHASE_ATTR.equals(attName)) {
				currTransform.phase = attValue;
			} else {
				unknownAttribute(FIELD_TRANSFORM_ELMT, attName);
			}
		}

		if (StringUtils.isEmpty(currTransform.name)) {
			currTransform.name = currField.field.getFieldTypeName() + "Transform";// NON-NLS
		}

		if (StringUtils.isNoneEmpty(currTransform.beanRef, currTransform.scriptLang)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR, LANG_ATTR),
					currParseLocation);
		}

		handleFieldLocatorCDATA();

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <stream>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processStream(Attributes attrs) throws SAXException {
		if (currStream != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", STREAM_ELMT), currParseLocation);
		}
		String name = null;
		String className = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (CLASS_ATTR.equals(attName)) {
				className = attValue;
			} else {
				unknownAttribute(STREAM_ELMT, attName);
			}
		}
		notEmpty(name, STREAM_ELMT, NAME_ATTR);
		notEmpty(className, STREAM_ELMT, CLASS_ATTR);
		if (streamsConfigData.getStream(name) != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.duplicate", STREAM_ELMT, name), currParseLocation);
		}
		try {
			Object newStream = Utils.createInstance(className);
			if (!(newStream instanceof TNTInputStream)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ConfigParserHandler.not.extend.class", STREAM_ELMT,
						CLASS_ATTR, className, TNTInputStream.class.getName(), getLocationInfo()));
			}
			currStream = (TNTInputStream<?, ?>) newStream;
		} catch (Exception exc) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.failed.to.load", STREAM_ELMT, CLASS_ATTR, className, getLocationInfo()), exc);
		}

		currStream.setName(name);
		streamsConfigData.addStream(currStream);

		// currStream.ensureOutputSet();
	}

	/**
	 * Processes a {@code <property>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processProperty(Attributes attrs) throws SAXException {
		checkPropertyState();

		currProperty = new Property();

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				currProperty.name = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				currProperty.value = attValue;
			} else {
				unknownAttribute(PROPERTY_ELMT, attName);
			}
		}

		notEmpty(currProperty.name, PROPERTY_ELMT, NAME_ATTR);

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Checks whether required parent configuration definition elements for {@code <property>} element are initialized.
	 *
	 * @throws SAXException
	 *             if no required parent elements found in configuration
	 */
	protected void checkPropertyState() throws SAXException {
		if (!StringUtils.equalsAnyIgnoreCase(getParentElmt(PROPERTY_ELMT), CONFIG_ROOT_ELMT, STREAM_ELMT, PARSER_ELMT,
				JAVA_OBJ_ELMT, CACHE_ELMT, TNT4J_PROPERTIES_ELMT)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", PROPERTY_ELMT, Utils.arrayToString(CONFIG_ROOT_ELMT,
							STREAM_ELMT, PARSER_ELMT, JAVA_OBJ_ELMT, CACHE_ELMT, TNT4J_PROPERTIES_ELMT)),
					currParseLocation);
		}
	}

	/**
	 * Returns parent path element for current element.
	 * 
	 * @param chElement
	 *            child element name
	 * @return parent element name
	 * @throws SAXParseException
	 *             if path stack is less than 2 elements depth
	 */
	protected String getParentElmt(String chElement) throws SAXParseException {
		if (path.size() < 2) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration3", chElement), currParseLocation);
		}
		return path.get(path.size() - 2);
	}

	/**
	 * Processes a {@code <parser-ref>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs element
	 */
	private void processParserRef(Attributes attrs) throws SAXException {
		if (currField == null && currStream == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", PARSER_REF_ELMT,
					Utils.arrayToString(FIELD_ELMT, EMBEDDED_ACTIVITY_ELMT, STREAM_ELMT)), currParseLocation);
		}

		if (currParserRef != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", PARSER_REF_ELMT), currParseLocation);
		}

		String parserName = null;
		String tags = null;
		String aggregationType = null;
		String applyOn = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				parserName = attValue;
			} else if (TAGS_ATTR.equals(attName)) {
				tags = attValue;
			} else if (AGGREGATION_ATTR.equals(attName)) {
				aggregationType = attValue;
			} else if (APPLY_ON_ATTR.equals(attName)) {
				applyOn = attValue;
			} else {
				unknownAttribute(PARSER_REF_ELMT, attName);
			}
		}

		notEmpty(parserName, PARSER_REF_ELMT, NAME_ATTR);

		ActivityParser parser = streamsConfigData.getParser(parserName);
		if (parser == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.undefined.reference", PARSER_REF_ELMT, parserName), currParseLocation);
		}

		currParserRef = new ParserRefData(parser, aggregationType, applyOn, tags);
	}

	/**
	 * Processes a {@code <reference>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processReference(Attributes attrs) throws SAXException {
		if (currStream == null && currParser == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", REF_ELMT,
					Utils.arrayToString(STREAM_ELMT, PARSER_ELMT)), currParseLocation);
		}

		if (currStream != null) {
			processStreamReference(attrs);
		}

		if (currParser != null) {
			processParserReference(attrs);
		}
	}

	/**
	 * Process parser {@code <reference>} element, e.g., pre-parser.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXParseException
	 *             if error occurs parsing element
	 */
	private void processParserReference(Attributes attrs) throws SAXParseException {
		String refObjName = getRefObjectNameFromAttr(attrs);
		notEmpty(refObjName, REF_ELMT, NAME_ATTR);
		Object refObject = findReference(refObjName);
		try {
			currParser.addReference(refObject);
		} catch (IllegalStateException exc) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.could.not.add.stream.reference", currStream.getName(), refObjName),
					currParseLocation, exc);
		}

	}

	/**
	 * Process stream {@code <reference>} element, e.g., parser, output, etc.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXParseException
	 *             if error occurs parsing element
	 */
	private void processStreamReference(Attributes attrs) throws SAXParseException {
		String refObjName = getRefObjectNameFromAttr(attrs);
		notEmpty(refObjName, REF_ELMT, NAME_ATTR);
		Object refObject = findReference(refObjName);
		if (refObject == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.undefined.reference", REF_ELMT, refObjName), currParseLocation);
		}

		try {
			currStream.addReference(refObject);
		} catch (IllegalStateException exc) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.could.not.add.stream.reference", currStream.getName(), refObjName),
					currParseLocation, exc);
		}
	}

	private String getRefObjectNameFromAttr(Attributes attrs) throws SAXParseException {
		String refObjName = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				refObjName = attValue;
			} else {
				unknownAttribute(REF_ELMT, attName);
			}
		}
		return refObjName;
	}

	/**
	 * Finds referenced object instance by reference name.
	 * <p>
	 * Reference name usually is object name returned by {@code getName()} method and defined in stream configuration
	 * {@code <tnt-data-source>} over XML {@code <stream>}, {@code <parser>} and {@code <java-object>} tags attribute
	 * {@code name="ObjName"} .
	 *
	 * @param refName
	 *            reference name
	 * @return referenced object instance, or {@code null} if reference not found
	 */
	protected Object findReference(String refName) {
		Object refObject = streamsConfigData == null ? null : streamsConfigData.getParser(refName);
		if (refObject == null) {
			refObject = streamsConfigData == null ? null : streamsConfigData.getStream(refName);
		}
		if (refObject == null) {
			refObject = javaObjectsMap == null ? null : javaObjectsMap.get(refName);
		}

		return refObject;
	}

	/**
	 * Processes a {@code <java-object>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processJavaObject(Attributes attrs) throws SAXException {
		if (javaObjectData == null) {
			javaObjectData = new JavaObjectData();
		}

		String name = null;
		String className = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (CLASS_ATTR.equals(attName)) {
				className = attValue;
			} else {
				unknownAttribute(JAVA_OBJ_ELMT, attName);
			}
		}

		notEmpty(name, JAVA_OBJ_ELMT, NAME_ATTR);
		notEmpty(className, JAVA_OBJ_ELMT, CLASS_ATTR);

		javaObjectData.name = name;
		javaObjectData.className = className;
	}

	/**
	 * Processes a {@code <param>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processParam(Attributes attrs) throws SAXException {
		if (javaObjectData == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", PARAM_ELMT, JAVA_OBJ_ELMT),
					currParseLocation);
		}

		String name = null;
		String value = null;
		String type = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			} else if (TYPE_ATTR.equals(attName)) {
				type = attValue;
			} else {
				unknownAttribute(PARAM_ELMT, attName);
			}
		}

		notEmpty(name, PARAM_ELMT, NAME_ATTR);
		notEmpty(type, PARAM_ELMT, TYPE_ATTR);

		try {
			Object obj;
			if (StringUtils.isEmpty(value)) {
				obj = Utils.createInstance(type);
			} else {
				obj = javaObjectsMap.get(value);
				if (obj == null) {
					obj = Utils.createInstance(type, new Object[] { value }, String.class);
				}
			}

			if (obj instanceof NamedObject) {
				((NamedObject) obj).setName(name);
			}
			javaObjectData.addArg(obj);
			javaObjectData.addType(type);
		} catch (Exception exc) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.could.not.init.obj.param", javaObjectData.name, name),
					currParseLocation, exc);
		}
	}

	/**
	 * Processes a {@code <filter>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFilter(Attributes attrs) throws SAXException {
		if (currFilter != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FILTER_ELMT), currParseLocation);
		}

		if (currLocatorData == null && currField == null && currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", FILTER_ELMT,
							Utils.arrayToString(FIELD_LOC_ELMT, FIELD_ELMT, EMBEDDED_ACTIVITY_ELMT, PARSER_ELMT)),
					currParseLocation);
		}

		String name = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else {
				unknownAttribute(FILTER_ELMT, attName);
			}
		}

		// make sure required fields are present
		notEmpty(name, FILTER_ELMT, NAME_ATTR);

		if (currLocatorData != null || currField != null) {
			currFilter = new StreamFiltersGroup<>(name);
		} else if (currParser != null) {
			currFilter = new StreamFiltersGroup<>(name);
		}

		handleFieldLocatorCDATA();
	}

	/**
	 * Processes a {@code <value>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processValue(Attributes attrs) throws SAXException {
		if (path.contains(CACHE_ELMT)) {
			processCacheValue(attrs);
		} else {
			processFilterValue(attrs);
		}
	}

	/**
	 * Processes a {@code <value>} element under {@code <filter>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFilterValue(Attributes attrs) throws SAXException {
		if (currFilterValue != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", VALUE_ELMT), currParseLocation);
		}

		if (currFilter == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", VALUE_ELMT, FILTER_ELMT),
					currParseLocation);
		}

		if (currFilterValue == null) {
			currFilterValue = new FilterValueData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (HANDLE_ATTR.equals(attName)) {
				currFilterValue.handle = attValue;
			} else if (EVALUATION_ATTR.equals(attName)) {
				currFilterValue.evaluation = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				currFilterValue.format = attValue;
			} else {
				unknownAttribute(VALUE_ELMT, attName);
			}
		}

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <value>} element under {@code <cache>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processCacheValue(Attributes attrs) throws SAXException {
		if (currCacheEntry == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", VALUE_ELMT, CACHE_ENTRY_ELMT),
					currParseLocation);
		}

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <expression>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFilterExpression(Attributes attrs) throws SAXException {
		if (currFilterExpression != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", EXPRESSION_ELMT), currParseLocation);
		}

		if (currFilter == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", EXPRESSION_ELMT, FILTER_ELMT),
					currParseLocation);
		}

		if (currFilterExpression == null) {
			currFilterExpression = new FilterExpressionData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (HANDLE_ATTR.equals(attName)) {
				currFilterExpression.handle = attValue;
			} else if (LANG_ATTR.equals(attName)) {
				currFilterExpression.lang = attValue;
			} else {
				unknownAttribute(EXPRESSION_ELMT, attName);
			}
		}

		elementDataStack.push(new StringBuilder());
	}

	/**
	 * Processes a {@code <matchExp>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	protected void processMatchExpression(Attributes attrs) throws SAXException {
		if (!StringUtils.equalsAnyIgnoreCase(getParentElmt(MATCH_EXP_ELMT), PARSER_REF_ELMT)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", MATCH_EXP_ELMT, PARSER_REF_ELMT),
					currParseLocation);
		}

		elementDataStack.push(new StringBuilder());
	}

	private void processTNT4JProperties(Attributes attrs) throws SAXException {
		if (currStream == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", TNT4J_PROPERTIES_ELMT, STREAM_ELMT),
					currParseLocation);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String cdata = new String(ch, start, length);

		if (CollectionUtils.isNotEmpty(elementDataStack)) {
			elementDataStack.peek().append(cdata);
		}
	}

	/**
	 * Returns string buffer contained string for current configuration element (token).
	 *
	 * @return configuration element (token) data string value, or {@code null} if no element data
	 */
	protected String getElementData() {
		return CollectionUtils.isEmpty(elementDataStack) ? null : elementDataStack.pop().toString().trim();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			endCfgElement(qName);
		} finally {
			if (CollectionUtils.isNotEmpty(path)) {
				path.pop();
			}
		}
	}

	/**
	 * Receives notification of the end of an stream configuration element.
	 * 
	 * @param qName
	 *            the qualified element name
	 * @throws SAXException
	 *             if malformed configuration found
	 */
	protected void endCfgElement(String qName) throws SAXException {
		try {
			if (STREAM_ELMT.equals(qName)) {
				currStream.setProperties(applyVariableProperties(currProperties.remove(qName)));
				currStream = null;
			} else if (PARSER_ELMT.equals(qName)) {
				currParser.setProperties(applyVariableProperties(currProperties.remove(qName)));
				currParser.organizeFields();
				currParser = null;
			} else if (StringUtils.equalsAny(qName, FIELD_ELMT, EMBEDDED_ACTIVITY_ELMT)) {
				if (currField != null) {
					handleActivityField(currField, qName);
					currParser.addField(currField.field);
					currField = null;
				}
			} else if (FIELD_LOC_ELMT.equals(qName)) {
				if (currLocatorData != null) {
					handleFieldLocator(currLocatorData);

					currLocatorData = null;
				}
			} else if (TNT4J_PROPERTIES_ELMT.equals(qName)) {
			} else if (JAVA_OBJ_ELMT.equals(qName)) {
				if (javaObjectData != null) {
					handleJavaObject(javaObjectData);
					javaObjectData = null;
				}
			} else if (PROPERTY_ELMT.equals(qName)) {
				if (currProperty != null) {
					handleProperty(currProperty);

					currProperty = null;
				}
			} else if (FIELD_TRANSFORM_ELMT.equals(qName)) {
				if (currTransform != null) {
					handleFieldTransform(currTransform);

					currTransform = null;
				}
			} else if (FILTER_ELMT.equals(qName)) {
				if (currFilter != null) {
					handleFilter(currFilter);

					currFilter = null;
				}
			} else if (VALUE_ELMT.equals(qName)) {
				if (currFilterValue != null) {
					handleFilterValue(currFilterValue);

					currFilterValue = null;
				} else if (currCacheEntry != null) {
					handleValue(currCacheEntry);
				}
			} else if (EXPRESSION_ELMT.equals(qName)) {
				if (currFilterExpression != null) {
					handleFilterExpression(currFilterExpression);

					currFilterExpression = null;
				}
			} else if (CACHE_ELMT.equals(qName)) {
				StreamsCache.setProperties(applyVariableProperties(currProperties.remove(qName)));
			} else if (CACHE_ENTRY_ELMT.equals(qName)) {
				if (currCacheEntry != null) {
					handleCacheEntry(currCacheEntry);

					currCacheEntry = null;
				}
			} else if (CACHE_KEY_ELMT.equals(qName)) {
				if (currCacheEntry != null) {
					handleKey(currCacheEntry);
				}
			} else if (CACHE_DEFAULT_VALUE_ELMT.equals(qName)) {
				if (currCacheEntry != null) {
					handleDefault(currCacheEntry);
				}
			} else if (MATCH_EXP_ELMT.equals(qName)) {
				if (currParserRef != null) {
					handleMatchExp(currParserRef);
				}
			} else if (PARSER_REF_ELMT.equals(qName)) {
				if (currParserRef != null) {
					handleParserRef(currParserRef);

					currParserRef = null;
				}
			}
		} catch (SAXException exc) {
			throw exc;
		} catch (Exception e) {
			throw new SAXException(e.getLocalizedMessage() + getLocationInfo(), e);
		}
	}

	/**
	 * Makes {@link java.util.Collection} of properties contained in provided {@code propsMap}. Also resolves and
	 * fills-in values for dynamically defined properties values having format {@code ${env.prop.name}}.
	 * <p>
	 * Sequence of dynamic properties values resolution (stops on first non-null value):
	 * <ul>
	 * <li>Java System properties</li>
	 * <li>OS environment variables (case insensitive)</li>
	 * <li>TNT4J properties</li>
	 * </ul>
	 *
	 * @param propsMap
	 *            configuration defined properties map
	 * @return set of properties (filled-in if dynamic), or {@code null} if {@code propsMap} is {@code null}
	 */
	protected static Collection<Map.Entry<String, String>> applyVariableProperties(Map<String, String> propsMap) {
		if (propsMap == null) {
			return null;
		}

		Set<Map.Entry<String, String>> props = propsMap.entrySet();
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String value = prop.getValue();

				ArrayList<String> variables = new ArrayList<>();
				Utils.resolveCfgVariables(variables, value);

				for (String variable : variables) {
					String variableName = variable.substring(2, variable.length() - 1);

					// Try java properties first
					String variableProperty = System.getProperty(variableName);

					// then do env variables lookup
					if (variableProperty == null) {
						variableProperty = System.getenv().get(variableName);
					}

					// then do env variables lookup ignore case
					if (variableProperty == null) {
						ignorecaseloop: for (String property : System.getenv().keySet()) {
							if (property.equalsIgnoreCase(variableName)) {
								variableProperty = System.getenv(property);
								break ignorecaseloop;
							}
						}
					}

					// then tnt4j properties lookup
					if (variableProperty == null) {
						TrackerConfig trCfg = DefaultConfigFactory.getInstance().getConfig(ConfigParserHandler.class);
						variableProperty = trCfg.getProperty(variableName);
					}

					if (variableProperty != null) {
						prop.setValue(value.replace(variable, variableProperty));
					}
				}
			}
		}

		return props;
	}

	private void handleActivityField(ActivityFieldData aFieldData, String qName) throws SAXException {
		if (CollectionUtils.isEmpty(aFieldData.getLocators())) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.must.have", qName, LOCATOR_ATTR, VALUE_ATTR, FIELD_LOC_ELMT,
					getLocationInfo(), aFieldData.field.getFieldTypeName()));
		}

		List<String> dynamicLocators = new ArrayList<>();
		Utils.resolveCfgVariables(dynamicLocators, aFieldData.field.getFieldTypeName(), aFieldData.field.getValueType(),
				aFieldData.field.getSeparator(), aFieldData.field.getFormattingPattern());

		for (String dLoc : dynamicLocators) {
			if (!aFieldData.field.hasDynamicLocator(dLoc)) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.ref.missing", qName, FIELD_LOC_ELMT, dLoc, getLocationInfo()));
			}
		}

		if (aFieldData.transformations != null) {
			boolean transformationsAdded = false;
			if (aFieldData.getLocators().size() == 1 && aFieldData.transformations.size() == 1) {
				ValueTransformation<Object, Object> aft = aFieldData.transformations.get(0);

				if (isLocalTransformation(aft)) {
					aFieldData.getLocators().get(0).addTransformation(aft);
					transformationsAdded = true;
				}
			}

			if (!transformationsAdded) {
				for (ValueTransformation<Object, Object> aft : aFieldData.transformations) {
					aFieldData.field.addTransformation(aft);
				}
			}
		}
	}

	private boolean isLocalTransformation(ValueTransformation<?, ?> vt) {
		return vt.getPhase() == ValueTransformation.Phase.RAW //
				|| vt.getPhase() == ValueTransformation.Phase.FORMATTED //
				|| (vt.getPhase() == null);
	}

	private void handleJavaObject(JavaObjectData javaObjectData) throws Exception {
		if (javaObjectData == null) {
			return;
		}

		Object obj = Utils.createInstance(javaObjectData.className, javaObjectData.getArgs(),
				javaObjectData.getTypes());

		javaObjectsMap.put(javaObjectData.name, obj);

		if (obj instanceof NamedObject) {
			((NamedObject) obj).setName(javaObjectData.name);
		}

		Map<String, String> objProps = currProperties.remove(JAVA_OBJ_ELMT);

		if (obj instanceof TNTStreamOutput) {
			TNTStreamOutput<?> out = (TNTStreamOutput<?>) obj;

			if (MapUtils.isNotEmpty(objProps)) {
				out.setProperties(applyVariableProperties(objProps));
			}
		} else if (obj instanceof Configurable) {
			Configurable cfgObj = (Configurable) obj;

			if (MapUtils.isNotEmpty(objProps)) {
				String beanRefObj = objProps.get(BEAN_REF_ATTR);
				if (StringUtils.isNotEmpty(beanRefObj)) {
					Map<String, Object> props = new HashMap<>(objProps.size());
					props.putAll(objProps);
					Object beanObj = resolveTransformationBean(beanRefObj);
					props.put(BEAN_REF_ATTR, beanObj);
					cfgObj.setConfiguration(props);
				} else {
					cfgObj.setConfiguration(objProps);
				}
			}
		}
	}

	private void handleProperty(Property currProperty) throws SAXException {
		String eDataVal = getElementData();
		if (eDataVal != null) {
			if (currProperty.value != null && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", PROPERTY_ELMT, VALUE_ATTR, getLocationInfo()));
			} else if (currProperty.value == null) {
				currProperty.value = eDataVal;
			}
		}

		notNull(currProperty.value, PROPERTY_ELMT, VALUE_ATTR);

		String pParent = getParentElmt(PROPERTY_ELMT);
		if (StringUtils.equalsIgnoreCase(pParent, TNT4J_PROPERTIES_ELMT)) {
			Map.Entry<String, String> p = new AbstractMap.SimpleEntry<>(currProperty.name, currProperty.value);
			currStream.output().setProperty(OutputProperties.PROP_TNT4J_PROPERTY, p);
		} else if (StringUtils.equalsIgnoreCase(pParent, CONFIG_ROOT_ELMT)) {
			streamsConfigData.addDataSourceProperty(currProperty.name, currProperty.value);
		} else {
			Map<String, String> eProps = currProperties.get(pParent);
			if (eProps == null) {
				eProps = new HashMap<>();
				currProperties.put(pParent, eProps);
			}
			String cpv = eProps.get(currProperty.name);
			if (cpv != null && !isPropValueAlreadyAdded(cpv, currProperty.value)) {
				currProperty.value = cpv + StreamsConstants.MULTI_PROPS_DELIMITER + currProperty.value;
			}
			eProps.put(currProperty.name, currProperty.value);
		}
	}

	private static boolean isPropValueAlreadyAdded(String cpv, String propValue) {
		if (cpv != null) {
			String[] pva = StreamsConstants.getMultiProperties(cpv);

			for (String pvt : pva) {
				if (pvt.equals(propValue)) {
					return true;
				}
			}
		}

		return false;
	}

	private void handleFieldLocator(FieldLocatorData currLocatorData) throws SAXException {
		handleFieldLocatorCDATA();

		if (currLocatorData.locator != null && currLocatorData.locator.isEmpty()) {
			currLocatorData.locator = null;
		}

		// make sure common required fields are present
		if (currLocatorData.locator == null && currLocatorData.value == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.must.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}
		if (currLocatorData.locator != null && currLocatorData.value != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}

		ActivityFieldLocator afl = currLocatorData.value != null ? new ActivityFieldLocator(currLocatorData.value)
				: new ActivityFieldLocator(currLocatorData.locatorType, currLocatorData.locator);
		afl.setRadix(currLocatorData.radix);
		afl.setRequired(currLocatorData.reqVal);
		afl.setId(currLocatorData.id);
		if (currLocatorData.format != null) {
			afl.setFormat(currLocatorData.format, currLocatorData.locale);
		}
		if (currLocatorData.dataType != null) {
			afl.setDataType(currLocatorData.dataType);
		}
		if (currLocatorData.units != null) {
			afl.setUnits(currLocatorData.units);
		}
		if (currLocatorData.timeZone != null) {
			afl.setTimeZone(currLocatorData.timeZone);
		}

		if (CollectionUtils.isNotEmpty(currLocatorData.valueMapItems)) {
			for (FieldLocatorData.ValueMapData vmd : currLocatorData.valueMapItems) {
				afl.addValueMap(vmd.source, vmd.target, vmd.mapTyp);
			}
		}

		if (CollectionUtils.isNotEmpty(currLocatorData.valueTransforms)) {
			for (ValueTransformation<Object, Object> vt : currLocatorData.valueTransforms) {
				afl.addTransformation(vt);
			}
		}

		if (currLocatorData.filter != null) {
			afl.setFilter(currLocatorData.filter);
		}
		if (currLocatorData.charset != null) {
			afl.setCharset(currLocatorData.charset);
		}
		afl.setEmptyAsNull(currLocatorData.emptyAsNull == null ? true : currLocatorData.emptyAsNull);

		currField.addLocator(afl);
	}

	private void handleFieldLocatorCDATA() throws SAXException {
		if (currLocatorData == null) {
			return;
		}

		String eDataVal = getElementData();

		if (StringUtils.isNotEmpty(eDataVal)) {
			if (StringUtils.isNotEmpty(currLocatorData.locator) && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", FIELD_LOC_ELMT, LOCATOR_ATTR, getLocationInfo()));
			}

			currLocatorData.locator = eDataVal;
		}
	}

	private void handleFieldTransform(FieldTransformData currTransformData) throws SAXException {
		String eDataVal = getElementData();
		checkScriptExpression(eDataVal);

		if (eDataVal != null) {
			if (StringUtils.isNotEmpty(currTransformData.beanRef) && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR,
						getLocationInfo()));
			} else if (currTransformData.scriptCode == null) {
				currTransformData.scriptCode = eDataVal;
			}
		}

		if (StringUtils.isEmpty(currTransformData.beanRef) && StringUtils.isEmpty(currTransformData.scriptCode)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.must.contain", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR, CDATA),
					currParseLocation);
		}

		ValueTransformation<Object, Object> transform;

		if (StringUtils.isNotEmpty(currTransformData.beanRef)) {
			transform = resolveTransformationBean(currTransformData.beanRef);
		} else {
			transform = AbstractScriptTransformation.createScriptTransformation(currTransformData.name,
					currTransformData.scriptLang, currTransformData.scriptCode, currTransformData.phase);
		}

		if (transform.getPhase() == null) {
			transform.setPhase(ValueTransformation.Phase.FORMATTED);
		}

		if (currLocatorData != null) {
			currLocatorData.valueTransforms.add(transform);
		} else {
			currField.addTransformation(transform);
		}
	}

	/**
	 * Checks if provided script expressions string is valid.
	 * 
	 * @param expString
	 *            expression string to check
	 * 
	 * @throws SAXException
	 *             if script expression is invalid
	 */
	protected void checkScriptExpression(String expString) throws SAXException {
		if (!validateScriptExpressions) {
			return;
		}

		if (StringUtils.isEmpty(expString)) {
			return;
		}

		boolean valid = StreamsScriptingUtils.isScriptExpressionValid(expString);
		if (!valid) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.invalid.expression.token", expString,
							StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR, "${FIELD_NAME}"), // NON-NLS
					currParseLocation);
		}
	}

	@SuppressWarnings("unchecked")
	private ValueTransformation<Object, Object> resolveTransformationBean(String beanRef) throws SAXException {
		Object tObj = javaObjectsMap.get(beanRef);

		if (tObj == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.undefined.reference", FIELD_TRANSFORM_ELMT, beanRef),
					currParseLocation);
		}

		if (tObj instanceof ValueTransformation) {
			return (ValueTransformation<Object, Object>) tObj;
		} else {
			throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ConfigParserHandler.not.extend.class", FIELD_TRANSFORM_ELMT,
					BEAN_REF_ATTR, tObj.getClass().getName(), ValueTransformation.class.getName(), getLocationInfo()));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleFilter(StreamFiltersGroup currFilter) throws SAXException {
		if (currLocatorData != null) {
			currLocatorData.filter = currFilter;
		} else if (currField != null) {
			currField.field.setFilter(currFilter);
		} else if (currParser != null) {
			((GenericActivityParser<?>) currParser).setActivityFilter(currFilter);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleFilterValue(FilterValueData fvData) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			fvData.value = eDataVal;
		}

		currFilter.addFilter(new DefaultValueFilter(fvData.handle, fvData.evaluation, fvData.format, fvData.value));
	}

	@SuppressWarnings("unchecked")
	private void handleFilterExpression(FilterExpressionData feData) throws SAXException {
		String eDataVal = getElementData();
		checkScriptExpression(eDataVal);

		if (eDataVal != null) {
			feData.expression = eDataVal;
		}

		currFilter.addFilter(
				AbstractExpressionFilter.createExpressionFilter(feData.handle, feData.lang, feData.expression));
	}

	private void handleCacheEntry(CacheEntryData currCacheEntry) throws SAXException {
		StreamsCache.addEntry(currCacheEntry.id, currCacheEntry.key, currCacheEntry.value, currCacheEntry.defaultValue,
				currCacheEntry.transientEntry);
	}

	private void handleKey(CacheEntryData currCacheEntry) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			currCacheEntry.key = eDataVal;
		}

		notEmpty(currCacheEntry.key, CACHE_KEY_ELMT);
	}

	private void handleDefault(CacheEntryData currCacheEntry) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			currCacheEntry.defaultValue = eDataVal;
		}

		notEmpty(currCacheEntry.defaultValue, CACHE_KEY_ELMT);
	}

	private void handleValue(CacheEntryData currCacheEntry) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			currCacheEntry.value = eDataVal;
		}

		notEmpty(currCacheEntry.value, VALUE_ELMT);
	}

	private void handleMatchExp(ParserRefData parserRefData) throws SAXException {
		String eDataVal = getElementData();
		checkScriptExpression(eDataVal);

		if (StringUtils.isNotEmpty(eDataVal)) {
			parserRefData.addMatcherExp(eDataVal);
		}
	}

	protected void handleParserRef(ParserRefData parserRefData) throws SAXException {
		MatchingParserReference apr = new MatchingParserReference(parserRefData.parser);
		apr.setTags(parserRefData.tags);
		apr.setMatchExpressions(parserRefData.matchExps);

		if (currField != null) {
			currField.field.addStackedParser(apr, parserRefData.aggregation, parserRefData.applyOn);
		} else {
			try {
				currStream.addReference(apr);
			} catch (IllegalStateException exc) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.could.not.add.stream.parser", currStream.getName(),
						parserRefData.parser.getName()), currParseLocation, exc);
			}
		}
	}

	/**
	 * Gets a string representing the current line in the file being parsed. Used for error messages.
	 *
	 * @return string representing current line number being parsed
	 */
	protected String getLocationInfo() {
		String locInfo = "";
		if (currParseLocation != null) {
			locInfo = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.at.line", currParseLocation.getLineNumber());
		}
		return locInfo;
	}

	protected void unknownAttribute(String element, String attribute) throws SAXParseException {
		// LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
		// "ConfigParserHandler.unknown.attr", tag, attribute, getLocationInfo());
		throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"ConfigParserHandler.unknown.attr", element, attribute), currParseLocation);
	}

	private static class Property {
		private String name;
		private String value;

		private Property() {
			reset();
		}

		private void reset() {
			name = null;
			value = null;
		}
	}

	private static class FieldLocatorData {
		ActivityFieldDataType dataType;
		String locatorType;
		String locator;
		String units;
		String format;
		String locale;
		String timeZone;
		String value;
		int radix;
		String reqVal;
		String id;
		String charset;
		Boolean emptyAsNull = null;

		List<ValueMapData> valueMapItems;
		List<ValueTransformation<Object, Object>> valueTransforms;
		StreamFiltersGroup<Object> filter;

		private FieldLocatorData() {
			reset();
		}

		private void reset() {
			dataType = null;
			locatorType = null;
			locator = null;
			units = null;
			format = null;
			locale = null;
			timeZone = null;
			value = null;
			radix = 10;
			reqVal = ""; /* string to allow override */
			id = null;
			charset = null;
			emptyAsNull = null;

			if (valueMapItems == null) {
				valueMapItems = new ArrayList<>();
			} else {
				valueMapItems.clear();
			}

			if (valueTransforms == null) {
				valueTransforms = new ArrayList<>();
			} else {
				valueTransforms.clear();
			}

			filter = null;
		}

		private static class ValueMapData {
			String source;
			String target;
			ActivityFieldMappingType mapTyp;

			private ValueMapData(String source, String target, ActivityFieldMappingType mapTyp) {
				this.source = source;
				this.target = target;
				this.mapTyp = mapTyp;
			}
		}
	}

	private static class JavaObjectData {
		private String name;
		private String className;
		private List<Object> args;
		private List<Class<?>> types;

		private void addArg(Object arg) {
			if (args == null) {
				args = new ArrayList<>();
			}

			args.add(arg);
		}

		private void addType(String typeClass) throws ClassNotFoundException {
			addType(Class.forName(typeClass));
		}

		private void addType(Class<?> typeClass) {
			if (types == null) {
				types = new ArrayList<>();
			}

			types.add(typeClass);
		}

		private Object[] getArgs() {
			return args == null ? new Object[0] : args.toArray();
		}

		private Class<?>[] getTypes() {
			Class<?>[] typesArray = new Class[types == null ? 0 : types.size()];
			if (types != null) {
				typesArray = types.toArray(typesArray);
			}

			return typesArray;
		}

		@SuppressWarnings("unused")
		private void reset() {
			name = "";
			className = "";
			if (args != null) {
				args.clear();
			}
			if (types != null) {
				types.clear();
			}
		}
	}

	private static class FieldTransformData {
		String name;
		String beanRef;
		String scriptLang;
		String scriptCode;
		String phase;
	}

	private static class FilterValueData {
		String handle;
		String evaluation;
		String format;
		String value;
	}

	private static class FilterExpressionData {
		String handle;
		String lang;
		String expression;
	}

	private static class CacheEntryData {
		String id;
		String key;
		String value;
		String defaultValue;
		boolean transientEntry;
	}

	protected static class ParserRefData {
		ActivityParser parser;
		String aggregation;
		String applyOn;
		String tags;
		List<String> matchExps;

		ParserRefData(ActivityParser parser, String aggregation, String applyOn, String tags) {
			this.parser = parser;
			this.aggregation = aggregation;
			this.applyOn = applyOn;
			this.tags = tags;
		}

		void addMatcherExp(String expression) {
			if (matchExps == null) {
				matchExps = new ArrayList<>();
			}

			matchExps.add(expression);
		}
	}

	private static class ActivityFieldData {
		ActivityField field;
		List<ValueTransformation<Object, Object>> transformations;

		private boolean hasLocValAttr = false;
		// private boolean hasLocElmt = false;
		// private boolean hasMapElmt = false;

		ActivityFieldData(ActivityField field) {
			this.field = field;
		}

		private void addLocator(ActivityFieldLocator afl) {
			field.addLocator(afl);
		}

		private List<ActivityFieldLocator> getLocators() {
			return field.getLocators();
		}

		private void addTransformation(ValueTransformation<Object, Object> aft) {
			if (transformations == null) {
				transformations = new ArrayList<>();
			}

			transformations.add(aft);
		}

		@SuppressWarnings("unused")
		private void reset() {
			field = null;
			transformations.clear();

			hasLocValAttr = false;
			// hasLocElmt = false;
			// hasMapElmt = false;
		}
	}
}
