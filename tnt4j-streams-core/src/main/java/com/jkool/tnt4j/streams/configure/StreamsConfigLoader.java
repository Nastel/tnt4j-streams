/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.configure;

import java.io.*;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.jkool.tnt4j.streams.configure.sax.StreamsConfigSAXParser;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;

/**
 * This class will load the specified stream configuration.
 *
 * @version $Revision: 1 $
 */
public class StreamsConfigLoader {

	/**
	 * Name of default configuration file name ({@value})
	 */
	public static final String DFLT_CFG_FILE_NAME = "tnt-data-source.xml"; // NON-NLS

	private static final String DFLT_CONFIG_FILE_PATH = "./../config" + File.separator + DFLT_CFG_FILE_NAME; // NON-NLS

	private StreamsConfigData streamsCfgData;

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the default
	 * configuration file ({@value #DFLT_CFG_FILE_NAME}), which is assumed to be
	 * in the classpath.
	 *
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader() throws SAXException, ParserConfigurationException, IOException {
		InputStream config = null;
		try {
			config = new FileInputStream(DFLT_CONFIG_FILE_PATH);
		} catch (FileNotFoundException e) {
		}
		// if could not locate file on file system, try classpath
		if (config == null) {
			config = Thread.currentThread().getContextClassLoader().getResourceAsStream(DFLT_CONFIG_FILE_PATH);
		}
		if (config == null) {
			throw new FileNotFoundException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"StreamsConfig.file.not.found", DFLT_CONFIG_FILE_PATH));
		}

		load(new InputStreamReader(config));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader for the file with the
	 * specified file name.
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
		load(new FileReader(configFileName));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader for the specified
	 * file.
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
		load(new FileReader(configFile));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the specified
	 * Reader to obtain the configuration data.
	 *
	 * @param configReader
	 *            Reader to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public StreamsConfigLoader(Reader configReader) throws SAXException, ParserConfigurationException, IOException {
		load(configReader);
	}

	/**
	 * Loads the configuration using SAX parser.
	 *
	 * @param config
	 *            Reader to get configuration data from.
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 * @see StreamsConfigSAXParser#parse(Reader)
	 */
	protected void load(Reader config) throws SAXException, ParserConfigurationException, IOException {
		try {
			streamsCfgData = StreamsConfigSAXParser.parse(config);
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
	public TNTInputStream getStream(String streamName) {
		return streamsCfgData == null ? null : streamsCfgData.getStream(streamName);
	}

	/**
	 * Returns the set of streams found in the configuration.
	 *
	 * @return set of streams found
	 */
	public Collection<TNTInputStream> getStreams() {
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
}
