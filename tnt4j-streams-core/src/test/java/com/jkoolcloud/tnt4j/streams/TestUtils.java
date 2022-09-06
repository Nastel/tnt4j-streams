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

package com.jkoolcloud.tnt4j.streams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import com.jkoolcloud.tnt4j.format.DefaultFormatter;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.*;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * @author akausinis
 * @version 1.0
 */
public final class TestUtils {

	public static void testPropertyList(TNTInputStream<?, ?> stream,
			Collection<Map.Entry<String, String>> propertiesToTest) {
		for (Map.Entry<String, String> property : propertiesToTest) {
			String name = property.getKey();
			Object result = stream.getProperty(name);
			assertNotNull("Property " + name + " is null", result); // NON-NLS
			assertEquals("Property not set as expected", property.getValue(), String.valueOf(result));
		}
	}

	public static class SimpleTestStream extends AbstractBufferedStream<Object> {
		private static final EventSink LOGGER = LoggerUtils.getLoggerSink(TestUtils.class);

		@Override
		protected long getActivityItemByteSize(Object activityItem) {
			return 0;
		}

		@Override
		protected boolean isInputEnded() {
			return false;
		}

		@Override
		protected EventSink logger() {
			return LOGGER;
		}
	}

	public static void configureConsoleLogger() {
		configureConsoleLogger("../config/log4j2.xml");
	}

	public static void configureConsoleLogger(String logPath) {
		File log4jConfig = new File(logPath);
		if (log4jConfig.exists()) {
			System.setProperty("log4j2.configurationFile", "file:./../config/log4j2.xml");
			return;
		}

		EventSinkFactory delegate = DefaultEventSinkFactory.getInstance();
		SinkLogEventListener logToConsoleEvenSinkListener = new SinkLogEventListener() {
			private final DefaultFormatter formatter = new DefaultFormatter();

			@Override
			public void sinkLogEvent(SinkLogEvent ev) {
				System.out.println(formatter.format(ev.getSinkObject(), ev.getArguments()));
			}
		};
		DefaultEventSinkFactory.setDefaultEventSinkFactory(new EventSinkFactory() {
			@Override
			public EventSink getEventSink(String name) {
				return configure(delegate.getEventSink(name));
			}

			@Override
			public EventSink getEventSink(String name, Properties props) {
				return configure(delegate.getEventSink(name, props));
			}

			@Override
			public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
				return configure(delegate.getEventSink(name, props, frmt));
			}

			@Override
			public long getTTL() {
				return delegate.getTTL();
			}

			@Override
			public void setTTL(long ttl) {
				delegate.setTTL(ttl);
			}

			private EventSink configure(EventSink eventSink) {
				eventSink.filterOnLog(false);
				eventSink.addSinkLogEventListener(logToConsoleEvenSinkListener);

				return eventSink;
			}
		});
	}
}
