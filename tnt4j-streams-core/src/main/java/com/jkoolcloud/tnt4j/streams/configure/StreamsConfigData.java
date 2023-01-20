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

package com.jkoolcloud.tnt4j.streams.configure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;

/**
 * This class contains streams and parsers objects loaded from configuration.
 *
 * @version $Revision: 1 $
 */
public class StreamsConfigData {

	private Map<String, ActivityParser> parsers = null;
	private Map<String, TNTInputStream<?, ?>> streams = null;
	private Map<String, String> dsProperties = new HashMap<>(5);

	public StreamsConfigData() {
	}

	/**
	 * Returns the collection of streams found in the configuration.
	 *
	 * @return collection of configuration defined streams or {@code null} if there is no streams defined
	 */
	public Collection<TNTInputStream<?, ?>> getStreams() {
		return streams == null ? null : streams.values();
	}

	/**
	 * Returns the collection of parsers found in the configuration.
	 *
	 * @return collection of configuration defined parsers or {@code null} if there is no parsers defined
	 */
	public Collection<ActivityParser> getParsers() {
		return parsers == null ? null : parsers.values();
	}

	/**
	 * Returns the stream with the specified name.
	 *
	 * @param streamName
	 *            name of stream, as specified in configuration file
	 * @return stream with specified name, or {@code null} if there is no such stream
	 */
	public TNTInputStream<?, ?> getStream(String streamName) {
		return streams == null ? null : streams.get(streamName);
	}

	/**
	 * Returns the parser with the specified name.
	 *
	 * @param parserName
	 *            name of parser, as specified in configuration file
	 * @return parser with specified name, or {@code null} if there is no such parser
	 */
	public ActivityParser getParser(String parserName) {
		return parsers == null ? null : parsers.get(parserName);
	}

	/**
	 * Checks if configuration has any streams defined.
	 *
	 * @return {@code true} if streams collection contains at least one stream instance, {@code false} - otherwise
	 */
	public boolean isStreamsAvailable() {
		return MapUtils.isNotEmpty(streams);
	}

	/**
	 * Checks if configuration has any parsers defined.
	 *
	 * @return {@code true} if parsers collection contains at least one parser instance, {@code false} - otherwise
	 */
	public boolean isParsersAvailable() {
		return MapUtils.isNotEmpty(parsers);
	}

	/**
	 * Adds stream instance to streams collection.
	 *
	 * @param stream
	 *            stream instance to add
	 */
	public void addStream(TNTInputStream<?, ?> stream) {
		if (streams == null) {
			streams = new HashMap<>();
		}

		streams.put(stream.getName(), stream);
	}

	/**
	 * Adds activity parser instance to parsers collection.
	 *
	 * @param parser
	 *            activity parser instance to add
	 */
	public void addParser(ActivityParser parser) {
		if (parsers == null) {
			parsers = new HashMap<>();
		}

		parsers.put(parser.getName(), parser);
	}

	/**
	 * Adds data source property.
	 * 
	 * @param key
	 *            property key/name
	 * @param value
	 *            property value
	 */
	public void addDataSourceProperty(String key, String value) {
		dsProperties.put(key, value);
	}

	/**
	 * Returns data source properties map.
	 * 
	 * @return data source properties map
	 */
	public Map<String, String> getDataSourceProperties() {
		return dsProperties;
	}

	/**
	 * Returns data source property value.
	 * 
	 * @param pKey
	 *            property key/name
	 * @return property value, or {@code null} if there is no such property
	 */
	public String getDataSourceProperty(String pKey) {
		return dsProperties.get(pKey);
	}

	/**
	 * Checks if configuration has any data source properties defined.
	 * 
	 * @return {@code true} if configuration contains at least one data source property, {@code false} - otherwise
	 */
	public boolean isDataSourcePropertiesAvailable() {
		return MapUtils.isNotEmpty(dsProperties);
	}

}
