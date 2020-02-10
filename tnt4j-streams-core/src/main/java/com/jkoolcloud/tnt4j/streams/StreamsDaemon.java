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

package com.jkoolcloud.tnt4j.streams;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.build.CfgStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Main class for jKool LLC TNT4J-Streams to run as system service.
 *
 * @version $Revision: 1 $
 */
public class StreamsDaemon implements Daemon {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsDaemon.class);

	private static final String START_COMMAND = "start"; // NON-NLS

	private static final StreamsDaemon instance = new StreamsDaemon();
	private DaemonContext daemonContext;

	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
		if (this.daemonContext != null) {
			throw new DaemonInitException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsDaemon.already.initialized"));
		}

		this.daemonContext = daemonContext;
		String[] arguments = daemonContext.getArguments();
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.init"),
				(Object[]) arguments);

		if (!Utils.isEmpty(arguments)) {
			LOGGER.log(OpLevel.INFO,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.processing.args"),
					(Object[]) arguments);

			boolean validArgs = StreamsAgent.processArgs(arguments);
			if (!validArgs) {
				throw new DaemonInitException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsDaemon.invalid.args"));
			}
		} else {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.no.args"));
		}
	}

	@Override
	public void start() throws Exception {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.starting"));
		StreamsAgent.loadConfigAndRun(new CfgStreamsBuilder().setConfig(StreamsAgent.getCfgFileName()));
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.streams.started"));
		while (StreamsAgent.isStreamsRunning()) {
			synchronized (instance) {
				try {
					instance.wait(TimeUnit.SECONDS.toMillis(30)); // wait 30 sec. and check if stopped
				} catch (InterruptedException ie) {
				}
			}
		}
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.start.completed"));
	}

	@Override
	public void stop() throws Exception {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.stopping"));
		stopStreams();
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.stop.completed"));
	}

	@Override
	public void destroy() {
		daemonContext = null;
	}

	private void stopStreams() {
		notifyInstance();
		StreamsAgent.stopStreams();
	}

	private static void notifyInstance() {
		synchronized (instance) {
			instance.notifyAll();
		}
	}

	/**
	 * Main entry point for running as a system service.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams daemon command line arguments">
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;command_name</td>
	 *            <td>(mandatory) service handling command name: {@code start} or {@code stop}</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;stream command-line arguments</td>
	 *            <td>(optional) list of streams configuration supported command-line arguments</td>
	 *            </tr>
	 *            </table>
	 *
	 * @see com.jkoolcloud.tnt4j.streams.StreamsAgent#main(String...)
	 */
	public static void main(String... args) {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.start.main"),
				StreamsAgent.pkgVersion(), StreamsAgent.runEnv());
		try {
			String cmd = START_COMMAND;
			if (args.length > 0) {
				LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsDaemon.processing.args"), Arrays.toString(args));
				StreamsAgent.processArgs(Arrays.copyOfRange(args, 1, args.length));
				cmd = args[0];
			}

			if (START_COMMAND.equalsIgnoreCase(cmd)) {
				instance.start();
			} else {
				instance.stop();
			}
		} catch (Exception e) {
			LOGGER.log(OpLevel.FATAL,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsDaemon.fatal.error"), e);
		}
	}
}
