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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigData;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Utility class dedicated to load TNT4J-Streams configuration using SAX-based parser.
 *
 * @version $Revision: 2 $
 */
public final class StreamsConfigSAXParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsConfigSAXParser.class);

	private static final String SAX_CFG_PROPERTIES = "sax.properties"; // NON-NLS
	private static final String HANDLER_PROP_KEY = "tnt4j.streams.config.sax.handler"; // NON-NLS
	private static final String HANDLER_EXT_PROP_KEY = "tnt4j.streams.config.sax.handler.ext."; // NON-NLS

	/**
	 * Path of streams configuration file if it can be resolved from input stream, {@code null} - otherwise.
	 */
	static String cfgFilePath;

	private StreamsConfigSAXParser() {
	}

	/**
	 * Reads the configuration and invokes the (SAX-based) parser to parse the configuration file contents.
	 *
	 * @param config
	 *            input stream to get configuration data from
	 * @param validateXSD
	 *            flag indicating whether to validate configuration XML against XSD schema
	 * @param validateExp
	 *            flag indicating whether to validate configuration contained script expressions
	 * @return streams configuration data or {@code null} if configuration is erroneous (fails XML-XSD validation)
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public static StreamsConfigData parse(InputStream config, boolean validateXSD, boolean validateExp)
			throws ParserConfigurationException, SAXException, IOException {
		cfgFilePath = Utils.resolveInputFilePath(config);

		if (validateXSD) {
			config = config.markSupported() ? config : new ByteArrayInputStream(IOUtils.toByteArray(config));

			Map<OpLevel, List<SAXParseException>> validationErrors = validate(config);

			if (MapUtils.isNotEmpty(validationErrors)) {
				for (Map.Entry<OpLevel, List<SAXParseException>> vee : validationErrors.entrySet()) {
					for (SAXParseException ve : vee.getValue()) {
						LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"StreamsConfigSAXParser.xml.validation.error", ve.getLineNumber(), ve.getColumnNumber(),
								vee.getKey(), ve.getLocalizedMessage());
					}
				}

				return null;
			}
		}

		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		ConfigParserHandler hndlr = getConfigHandler();
		hndlr.setValidateScriptExpressions(validateExp);

		parser.parse(config, hndlr);

		return hndlr.getStreamsConfigData();
	}

	private static ConfigParserHandler getConfigHandler() throws IOException {
		Properties p = Utils.loadPropertiesResources(SAX_CFG_PROPERTIES);

		ConfigParserHandler hndlr = null;
		try {
			String handlerClassName = null;

			// check for handler extensions
			for (String pName : p.stringPropertyNames()) {
				if (pName.startsWith(HANDLER_EXT_PROP_KEY)) {
					handlerClassName = p.getProperty(pName);
					break; // TODO: review when more modules will extend base tnt-data-source configuration
				}
			}

			// if no extensions available, get core handler
			if (StringUtils.isEmpty(handlerClassName)) {
				handlerClassName = p.getProperty(HANDLER_PROP_KEY, ConfigParserHandler.class.getName());
			}

			hndlr = (ConfigParserHandler) Utils.createInstance(handlerClassName);
		} catch (Exception exc) {
		}

		return hndlr == null ? new ConfigParserHandler() : hndlr;
	}

	/**
	 * Validates configuration XML against XML defined XSD schema.
	 *
	 * @param config
	 *            {@link InputStream} to get configuration data from
	 * @return map of found validation errors
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public static Map<OpLevel, List<SAXParseException>> validate(InputStream config) throws SAXException, IOException {
		Map<OpLevel, List<SAXParseException>> validationErrors = new EnumMap<>(OpLevel.class);
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema();
			Validator validator = schema.newValidator();
			validator.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					handleValidationError(OpLevel.WARNING, exception);
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					handleValidationError(OpLevel.ERROR, exception);
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					handleValidationError(OpLevel.FATAL, exception);
				}

				private void handleValidationError(OpLevel level, SAXParseException exception) {
					List<SAXParseException> lErrorsList = validationErrors.computeIfAbsent(level,
							k -> new ArrayList<>());

					lErrorsList.add(exception);
				}
			});
			validator.validate(new StreamSource(config));
		} finally {
			if (config.markSupported()) {
				config.reset();
			}
		}

		return validationErrors;
	}
}
