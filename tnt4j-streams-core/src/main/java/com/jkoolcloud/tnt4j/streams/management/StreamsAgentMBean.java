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

import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements MBean operations and attributes for {@link com.jkoolcloud.tnt4j.streams.StreamsAgent} management.
 *
 * @version $Revision: 1 $
 */
public class StreamsAgentMBean extends StandardMBean implements AgentMBean {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsAgentMBean.class);

	private static final AtomicInteger AGENT_ID = new AtomicInteger();

	private ObjectName mbObjName;

	/**
	 * Constructs a new StreamsAgentMBean.
	 */
	public StreamsAgentMBean() {
		super(AgentMBean.class, true);

		try {
			Hashtable<String, String> props = new Hashtable<>(2);
			props.put("type", "Agent"); // NON-NLS
			props.put("name", "management"); // NON-NLS
			props.put("agentId", String.valueOf(AGENT_ID.getAndIncrement())); // NON-NLS
			mbObjName = new ObjectName(StreamsAgent.class.getPackage().getName(), props);
		} catch (MalformedObjectNameException e) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgentMBean.object.name.failure", e);
		}
	}

	/**
	 * Returns object name for this MBean.
	 *
	 * @return object name for this MBean
	 */
	public ObjectName getName() {
		return mbObjName;
	}

	@Override
	public void pauseAll() {
		StreamsAgent.pauseStreams();
	}

	@Override
	public void pause(String names) {
		StreamsAgent.pauseStreams(splitNames(names));
	}

	@Override
	public void stopAll() {
		StreamsAgent.stopStreams();

	}

	@Override
	public void stop(String names) {
		StreamsAgent.stopStreams(splitNames(names));
	}

	@Override
	public void restartAll() {
		StreamsAgent.restartStreams();
	}

	@Override
	public void restart(String names) {
		StreamsAgent.restartStreams(splitNames(names));
	}

	@Override
	public String[] getRunningStreamNames() {
		Collection<String> x = StreamsAgent.getRunningStreamNames();
		return x.toArray(new String[0]);
	}

	@Override
	public boolean isRunning() {
		return StreamsAgent.isStreamsRunning();
	}

	private static String[] splitNames(String names) {
		String[] na = names.split(Utils.TAG_DELIM);

		for (int i = 0; i < na.length; i++) {
			na[i] = na[i].trim();
		}

		return na;
	}
}
