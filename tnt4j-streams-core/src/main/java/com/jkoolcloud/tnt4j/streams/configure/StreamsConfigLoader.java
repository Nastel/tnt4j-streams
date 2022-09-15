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

package com.jkoolcloud.tnt4j.streams.configure;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.jkoolcloud.tnt4j.streams.configure.sax.StreamsConfigSAXParser;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class will load the specified stream configuration.
 *
 * @version $Revision: 2 $
 */
public class StreamsConfigLoader {

	/**
	 * Name of default streams configuration file referring system property name ({@value})
	 */
	public static final String STREAMS_CONFIG_KEY = "tnt4j.streams.config"; // NON-NLS
	/**
	 * Name of default configuration file name ({@value})
	 */
	public static final String DFLT_CFG_FILE_NAME = "tnt-data-source.xml"; // NON-NLS

	private static final String DFLT_CONFIG_PATH = "./../config"; // NON-NLS
	private static final String DFLT_CONFIG_PATH2 = "./config"; // NON-NLS

	private static final String DFLT_CONFIG_FILE_PATH = DFLT_CONFIG_PATH + File.separator + DFLT_CFG_FILE_NAME;
	private static final String DFLT_CONFIG_FILE_PATH2 = DFLT_CONFIG_PATH2 + File.separator + DFLT_CFG_FILE_NAME;

	private StreamsConfigData streamsCfgData;
	private boolean erroneous = false;

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the default configuration file
	 * ({@value #DFLT_CFG_FILE_NAME}), which is assumed to be in the classpath.
	 *
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader() throws SAXException, ParserConfigurationException, IOException {
		InputStream config;
		String cfgPath = System.getProperty(STREAMS_CONFIG_KEY);
		if (StringUtils.isNotEmpty(cfgPath)) {
			config = openCfgFile(cfgPath);

			if (config == null) {
				throw new FileNotFoundException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsConfig.file.not.found", cfgPath));
			}
		} else {
			config = openCfgFile(DFLT_CONFIG_FILE_PATH);
			if (config == null) {
				config = openCfgFile(DFLT_CONFIG_FILE_PATH2);
			}

			if (config == null) {
				throw new FileNotFoundException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"StreamsConfig.files.not.found", DFLT_CONFIG_FILE_PATH, DFLT_CONFIG_FILE_PATH2));
			}
		}

		load(config);
	}

	private static InputStream openCfgFile(String path) {
		try {
			return Files.newInputStream(Paths.get(path));
		} catch (IOException e) {
		}
		return Utils.getResourceAsStream(path);
	}

	/**
	 * Returns configuration file defined by one of default paths: {@code "./../config"} or {@code "./config"}.
	 *
	 * @return existing configuration file, or {@code null} if no configuration file found using default paths
	 */
	public static File getDefaultFile() {
		return getDefaultFile(DFLT_CONFIG_FILE_PATH, DFLT_CONFIG_FILE_PATH2);
	}

	private static File getDefaultFile(String... paths) {
		if (paths != null) {
			for (String path : paths) {
				File f = new File(path);
				if (f.exists()) {
					return f;
				}
			}
		}

		return null;
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader for the file with the specified file name.
	 *
	 * @param configFileName
	 *            name of configuration file
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader(String configFileName) throws SAXException, ParserConfigurationException, IOException {
		load(Files.newInputStream(Paths.get(configFileName)));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader for the specified {@link java.io.File}.
	 *
	 * @param configFile
	 *            configuration file
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader(File configFile) throws SAXException, ParserConfigurationException, IOException {
		load(Files.newInputStream(configFile.toPath()));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the specified {@link java.io.Reader} to obtain the
	 * configuration data.
	 *
	 * @param configReader
	 *            reader to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public StreamsConfigLoader(Reader configReader) throws SAXException, ParserConfigurationException, IOException {
		load(new ReaderInputStream(configReader, StandardCharsets.UTF_8));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the specified {@link java.io.InputStream} to obtain
	 * the configuration data.
	 * 
	 * @param is
	 *            input stream to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public StreamsConfigLoader(InputStream is) throws SAXException, ParserConfigurationException, IOException {
		load(is);
	}

	/**
	 * Loads the configuration XML using SAX parser.
	 * <p>
	 * Configuration XML validation against XSD schema is performed if system property
	 * {@code "com.jkoolcloud.tnt4j.streams.validate.config.xsd"} is set to {@code true}.
	 * <p>
	 * Default configuration defined transformations, filters and matchers script expressions validation can be turned
	 * off by setting system property {@code "com.jkoolcloud.tnt4j.streams.validate.config.exp"} to {@code false}.
	 *
	 * @param config
	 *            input stream to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 * @see StreamsConfigSAXParser#parse(java.io.InputStream, boolean, boolean)
	 */
	protected void load(InputStream config) throws SAXException, ParserConfigurationException, IOException {
		String pValue = System.getProperty("com.jkoolcloud.tnt4j.streams.validate.config.xsd"); // NON-NLS
		if (pValue == null) {
			pValue = System.getProperty("com.jkoolcloud.tnt4j.streams.validate.config"); // NON-NLS
		}

		boolean validateXSD = Boolean.parseBoolean(pValue);
		boolean validateExp = Utils.getBoolean("com.jkoolcloud.tnt4j.streams.validate.config.exp", // NON-NLS
				System.getProperties(), true);
		try {
			streamsCfgData = StreamsConfigSAXParser.parse(config, validateXSD, validateExp);
			erroneous = streamsCfgData == null;
		} finally {
			Utils.close(config);
		}
	}

	/**
	 * Returns the stream with the specified name.
	 *
	 * @param streamName
	 *            name of stream, as specified in configuration file
	 * @return stream with specified name, or {@code null} if no such stream
	 */
	public TNTInputStream<?, ?> getStream(String streamName) {
		return streamsCfgData == null ? null : streamsCfgData.getStream(streamName);
	}

	/**
	 * Returns the set of streams found in the configuration.
	 *
	 * @return set of streams found
	 */
	public Collection<TNTInputStream<?, ?>> getStreams() {
		return streamsCfgData == null ? null : streamsCfgData.getStreams();
	}

	/**
	 * Returns the set of parsers found in the configuration.
	 *
	 * @return set of parsers found
	 */
	public Collection<ActivityParser> getParsers() {
		return streamsCfgData == null ? null : streamsCfgData.getParsers();
	}

	/**
	 * Returns the parser with the specified name.
	 *
	 * @param parserName
	 *            name of parser, as specified in configuration file
	 * @return parser with specified name, or {@code null} if no such parser
	 */
	public ActivityParser getParser(String parserName) {
		return streamsCfgData == null ? null : streamsCfgData.getParser(parserName);
	}

	/**
	 * Returns flag indicating if streams configuration has XML-XSD validation errors.
	 *
	 * @return {@code true} if streams configuration is erroneous, {@code false} - otherwise
	 */
	public boolean isErroneous() {
		return erroneous;
	}

	/**
	 * Returns the map of data source properties found in the configuration.
	 * 
	 * @return map of data source properties
	 */
	public Map<String, String> getDataSourceProperties() {
		return streamsCfgData == null ? null : streamsCfgData.getDataSourceProperties();
	}

	/**
	 * Configures static APIs to be used by TNT4J-Streams.
	 */
	public static void configureStatics() {
		configureJsonPath();
	}

	private static void configureJsonPath() {
		Configuration.setDefaults(new Configuration.Defaults() {
			private final JsonProvider jsonProvider = new JacksonJsonProvider();
			private final MappingProvider mappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}
}
