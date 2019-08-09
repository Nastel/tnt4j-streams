/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.plugins.flume;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;

import com.google.gson.Gson;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.configure.build.CfgStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.utils.FlumeConstants;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Extends Apache Flume {@link AbstractSink} to deliver output events to jKoolCloud.
 *
 * @version $Revision: 1 $
 */
public class TNT4JStreamsEventSink extends AbstractSink implements Configurable {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(TNT4JStreamsEventSink.class);

	/**
	 * Constant for name of built-in sink @value} property.
	 */
	public static final String PROP_HOST = "hostname"; // NON-NLS

	/**
	 * Constant for name of built-in sink {@value} property.
	 */
	public static final String PROP_PORT = "port"; // NON-NLS

	/**
	 * Constant for name of built-in sink {@value} property.
	 */
	public static final String PROP_STREAM_CONFIG = "streamConfig"; // NON-NLS

	private static final String DEFAULT_HOST = "localhost"; // NON-NLS
	private static final int DEFAULT_PORT = 9595;
	private static final String DEFAULT_CONFIG_FILE_NAME = StreamsConfigLoader.DFLT_CFG_FILE_NAME;

	private String hostname;
	private String streamConfig;
	private int port;
	private Socket socket;
	private PrintWriter out;

	/**
	 *
	 * @return status of event delivery
	 *
	 * @throws EventDeliveryException
	 *             when event can't be delivered
	 */
	@Override
	public Status process() throws EventDeliveryException {
		Status result = Status.READY;
		Channel channel = getChannel();
		Transaction transaction = null;
		Event event;

		try {

			transaction = channel.getTransaction();
			transaction.begin();
			event = channel.take();

			if (event != null) {
				StreamsEvent eventE = new StreamsEvent(event, getName(), channel.getName());
				Gson gson = new Gson();
				String jsonEvent = gson.toJson(eventE);

				if (out == null || socket == null || socket.isClosed()) {
					openSocket();
				}

				out.println(jsonEvent);

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
						"TNT4JStreamsEventSink.sending.json", hostname, port, jsonEvent);
			}
			transaction.commit();
		} catch (Exception ex) {
			String errorMsg = StreamsResources.getString(FlumeConstants.RESOURCE_BUNDLE_NAME,
					"TNT4JStreamsEventSink.failed.to.publish");
			Utils.logThrowable(LOGGER, OpLevel.ERROR, errorMsg, ex);
			result = Status.BACKOFF;
			if (transaction != null) {
				try {
					transaction.rollback();
				} catch (Exception exc) {
					Utils.logThrowable(LOGGER, OpLevel.ERROR,
							StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
							"TNT4JStreamsEventSink.transaction.rollback.failed", exc);
				}
			}
			throw new EventDeliveryException(errorMsg, ex);
		} finally {
			if (transaction != null) {
				transaction.close();
			}
		}

		return result;
	}

	private void openSocket() throws IOException {
		Utils.close(out);
		Utils.close(socket);

		socket = new Socket(hostname, port);
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	/**
	 * Starts Apache Flume output events sink to jKoolCloud.
	 */
	@Override
	public synchronized void start() {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
				"TNT4JStreamsEventSink.plugin.starting");
		if (StringUtils.isEmpty(hostname) || hostname.equals(DEFAULT_HOST)) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
					"TNT4JStreamsEventSink.streams.starting");
			StreamsAgent.runFromAPI(new CfgStreamsBuilder().setConfig(streamConfig));
		}
		try {
			openSocket();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
					"TNT4JStreamsEventSink.failed.open.socket", exc);
		}

		super.start();
	}

	/**
	 * Stops Apache Flume output events sinking.
	 */
	@Override
	public synchronized void stop() {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
				"TNT4JStreamsEventSink.plugin.stopping");
		Utils.close(out);
		Utils.close(socket);

		super.stop();
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
				"TNT4JStreamsEventSink.plugin.stopped");
	}

	/**
	 * Configures Apache Flume jKoolCloud event sink (plugin).
	 *
	 * @param context
	 *            Apache Flume configuration context
	 */
	@Override
	public void configure(Context context) {
		hostname = context.getString(PROP_HOST);
		String portStr = context.getString(PROP_PORT);
		streamConfig = context.getString(PROP_STREAM_CONFIG);

		if (hostname == null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
					"TNT4JStreamsEventSink.no.hostname.configured", DEFAULT_HOST);
			hostname = DEFAULT_HOST;
		}

		if (portStr == null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
					"TNT4JStreamsEventSink.no.port.configured", DEFAULT_PORT);
			port = DEFAULT_PORT;
		} else {
			try {
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
						"TNT4JStreamsEventSink.parse.port.error", portStr);
				port = DEFAULT_PORT;
			}
		}

		if (streamConfig == null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(FlumeConstants.RESOURCE_BUNDLE_NAME),
					"TNT4JStreamsEventSink.no.tnt4j.config", DEFAULT_CONFIG_FILE_NAME);
			streamConfig = DEFAULT_CONFIG_FILE_NAME;
		}
	}
}
