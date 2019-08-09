/*
 * Copyright 2014-2019 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.configure.build;

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.inputs.PipedStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Build streams and streaming context from provided configuration sources: files, resource, strings, readers, input
 * stream, etc.
 * 
 * @version $Revision: 1 $
 */
public class CfgStreamsBuilder extends POJOStreamsBuilder {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(CfgStreamsBuilder.class);

	private Reader cfgReader;
	private String[] names;

	/**
	 * Sets streams configuration to be loaded from defined file path.
	 * 
	 * @param cfgFileName
	 *            streams configuration file path
	 * @return instance of this streams builder
	 */
	public CfgStreamsBuilder setConfig(String cfgFileName) {
		return setConfig(StringUtils.isEmpty(cfgFileName) ? null : new File(cfgFileName));
	}

	/**
	 * Sets streams configuration to be loaded from defined file.
	 * 
	 * @param cfgFile
	 *            streams configuration file
	 * @return instance of this streams builder
	 */
	public CfgStreamsBuilder setConfig(File cfgFile) {
		try {
			return setConfig(cfgFile == null ? null : new FileReader(cfgFile));
		} catch (FileNotFoundException e) {
			LOGGER.log(OpLevel.ERROR, Utils.getExceptionMessages(e));
			return this;
		}
	}

	/**
	 * Sets streams configuration to be loaded from defined input stream.
	 * 
	 * @param is
	 *            stream configuration input stream
	 * @return instance of this streams builder
	 */
	public CfgStreamsBuilder setConfig(InputStream is) {
		return setConfig(is == null ? null : new InputStreamReader(is));
	}

	/**
	 * Sets streams configuration to be loaded from defined reader.
	 * 
	 * @param cfgReader
	 *            stream configuration reader
	 * @return instance of this streams builder
	 */
	public CfgStreamsBuilder setConfig(Reader cfgReader) {
		this.cfgReader = cfgReader;

		return this;
	}

	/**
	 * Sets set of stream names to be run.
	 * 
	 * @param names
	 *            set of stream names to run
	 * @return instance of this streams builder
	 */
	public CfgStreamsBuilder setNames(String... names) {
		this.names = names;

		return this;
	}

	/**
	 * Loads stream configuration from provided input source.
	 * 
	 * @param osPipeInput
	 *            flag indicating whether OS pipe input shall be used as stream input
	 * @param haltOnUnparsed
	 *            flag indicating whether to stop streaming if parser fails to parse input data
	 * @throws Exception
	 *             if configuration is malformed or streams can't be initiated
	 */
	public void loadConfig(boolean osPipeInput, boolean haltOnUnparsed) throws Exception {
		StreamsConfigLoader cfg = cfgReader == null ? new StreamsConfigLoader() : new StreamsConfigLoader(cfgReader);

		if (cfg != null) {
			if (cfg.isErroneous()) {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.erroneous.configuration"));
			}

			Collection<TNTInputStream<?, ?>> streams;
			if (osPipeInput) {
				streams = initPiping(cfg, haltOnUnparsed);
			} else {
				streams = cfg.getStreams();

				if (names != null) {
					Iterator<TNTInputStream<?, ?>> sIt = streams.iterator();
					while (sIt.hasNext()) {
						if (!ArrayUtils.contains(names, sIt.next().getName())) {
							sIt.remove();
						}
					}
				}
			}

			addStreams(streams);
		}
	}

	/**
	 * Creates and initiates OS piped input streams for provided parsers configuration.
	 *
	 * @param cfg
	 *            piped data parsers configuration file path
	 * @param haltOnUnparsed
	 *            flag indicating whether to stop streaming if parser fails to parse input data
	 * @return collection of created OS piped input streams
	 * @throws Exception
	 *             if parsers configuration is malformed, or OS piped input streams initiation failed
	 */
	protected static Collection<TNTInputStream<?, ?>> initPiping(StreamsConfigLoader cfg, boolean haltOnUnparsed)
			throws Exception {
		Map<String, String> props = new HashMap<>(1);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(haltOnUnparsed));

		PipedStream pipeStream = new PipedStream();
		pipeStream.setName("DefaultSystemPipeStream"); // NON-NLS
		pipeStream.setProperties(props.entrySet());

		Collection<ActivityParser> parsers = cfg.getParsers();
		if (CollectionUtils.isEmpty(parsers)) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsAgent.no.piped.activity.parsers"));
		}
		pipeStream.addParsers(parsers);

		Collection<TNTInputStream<?, ?>> streams = new ArrayList<>(1);
		streams.add(pipeStream);

		return streams;
	}
}
