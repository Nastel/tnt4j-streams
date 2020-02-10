/*
 * Copyright 2014-2020 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.management;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Management MBeans initialization and registration manager for the jKool LLC TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public class MBeansManager {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MBeansManager.class);

	private static final StreamsAgentMBean STREAMS_AGENT_MBEAN = new StreamsAgentMBean();

	/**
	 * Registers all streams management MBeans.
	 */
	public static void registerMBeans() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			server.registerMBean(STREAMS_AGENT_MBEAN, STREAMS_AGENT_MBEAN.getName());
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "MBeansManager.register.fail",
					exc);
		}
	}

	/**
	 * Unregisters all streams management MBeans.
	 */
	public static void unregisterMBeans() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			server.unregisterMBean(STREAMS_AGENT_MBEAN.getName());
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "MBeansManager.unregister.fail",
					exc);
		}
	}
}
