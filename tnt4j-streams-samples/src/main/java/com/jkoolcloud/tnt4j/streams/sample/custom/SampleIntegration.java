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

package com.jkoolcloud.tnt4j.streams.sample.custom;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.inputs.StreamThread;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Sample integration of TNT4J-Streams into an application.
 *
 * @version $Revision: 1 $
 */
public final class SampleIntegration {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SampleIntegration.class);

	/**
	 * Configure streams and parsers, and run each stream in its own thread.
	 *
	 * @param cfgFileName
	 *            configuration file name
	 */
	public static void loadConfigAndRun(String cfgFileName) {
		try {
			StreamsConfigLoader cfg = StringUtils.isEmpty(cfgFileName) ? new StreamsConfigLoader()
					: new StreamsConfigLoader(cfgFileName);

			if (cfg.isErroneous()) {
				throw new IllegalStateException(
						"Erroneous TNT4J-Streams configuration found! Interrupting initialization..."); // NON-NLS
			}

			Collection<TNTInputStream<?, ?>> streams = cfg.getStreams();
			if (streams == null || streams.isEmpty()) {
				throw new IllegalStateException("No Activity Streams found in configuration"); // NON-NLS
			}

			ThreadGroup streamThreads = new ThreadGroup("Streams"); // NON-NLS
			StreamThread ft;
			for (TNTInputStream<?, ?> stream : streams) {
				ft = new StreamThread(streamThreads, stream,
						String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
				ft.start();
			}
		} catch (SAXException | IllegalStateException e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, "Stream configuration error: {0}", Utils.getExceptionMessages(e)); // NON-NLS
		} catch (Exception e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, "Failed to start streams: {0}", e); // NON-NLS
		}
	}

	/**
	 * The following can be used if using the default configuration file with a single stream.
	 *
	 * @param cfgFileName
	 *            configuration file name
	 */
	public static void simpleConfigAndRun(String cfgFileName) {
		try {
			StreamsConfigLoader cfg = new StreamsConfigLoader();
			TNTInputStream<?, ?> stream = cfg.getStream("StreamName"); // NON-NLS
			StreamThread ft = new StreamThread(stream);
			ft.start();
		} catch (SAXException | IllegalStateException e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, "Stream configuration error: {0}", Utils.getExceptionMessages(e)); // NON-NLS
		} catch (Exception e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, "Failed to start stream: {0}", e); // NON-NLS
		}
	}
}
