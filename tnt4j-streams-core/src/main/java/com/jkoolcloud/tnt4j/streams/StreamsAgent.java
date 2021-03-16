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

import java.io.Reader;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.configure.build.CfgStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.build.StreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigManager;
import com.jkoolcloud.tnt4j.streams.inputs.*;
import com.jkoolcloud.tnt4j.streams.management.MBeansManager;
import com.jkoolcloud.tnt4j.streams.utils.Duration;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Main class for jKool LLC TNT4J-Streams standalone application.
 *
 * @version $Revision: 3 $
 *
 * @see com.jkoolcloud.tnt4j.streams.configure.build.StreamsBuilder
 */
public final class StreamsAgent {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsAgent.class);

	static {
		StreamsConfigLoader.configureStatics();
	}

	private static final String PARAM_STREAM_CFG = "-f:"; // NON-NLS
	private static final String PARAM_ZOOKEEPER_CFG = "-z:"; // NON-NLS
	private static final String PARAM_ZOOKEEPER_STREAM_ID = "-sid:"; // NON-NLS
	private static final String PARAM_OS_PIPE_INPUT = "-p"; // NON-NLS
	private static final String PARAM_SKIP_UNPARSED = "-s"; // NON-NLS
	private static final String PARAM_HELP1 = "-h"; // NON-NLS
	private static final String PARAM_HELP2 = "-?"; // NON-NLS

	private static String cfgFileName = null;
	private static String zookeeperCfgFile = null;
	private static String zookeeperStreamId = null;
	private static boolean osPipeInput = false;
	private static boolean haltOnUnparsed = true;

	private static ThreadGroup streamThreads;
	private static boolean restarting = true;

	private static final Map<String, String> dataSourceProperties = new HashMap<>(5);

	private StreamsAgent() {
	}

	/**
	 * Main entry point for running as a standalone application.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams agent command line arguments">
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-f:&lt;cfg_file_name&gt;</td>
	 *            <td>(optional) Load TNT4J Streams data source configuration from &lt;cfg_file_name&gt;</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-p</td>
	 *            <td>(optional) Flag indicating to use OS piping as stream input</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;&nbsp;&nbsp;-s</td>
	 *            <td>(optional) Skip unparsed activity data entries and continue streaming</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-z:&lt;cfg_file_name&gt;</td>
	 *            <td>(optional) Load TNT4J-Streams ZooKeeper configuration from &lt;cfg_file_name&gt;</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;&nbsp;&nbsp;-sid:</td>
	 *            <td>(optional) Stream identifier to use form ZooKeeper configuration</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-h | -?</td>
	 *            <td>(optional) Print usage</td>
	 *            </tr>
	 *            </table>
	 */
	public static void main(String... args) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.start.main", pkgVersion(), runEnv());
		boolean argsValid = processArgs(args);
		if (argsValid) {
			boolean loadedZKConfig = loadZKConfig(zookeeperCfgFile, zookeeperStreamId);

			if (!loadedZKConfig) {
				loadConfigAndRun(new CfgStreamsBuilder().setConfig(cfgFileName));
				// DefaultTNTStreamListener dsl = new DefaultTNTStreamListener(LOGGER);
				// loadConfigAndRun(new
				// CfgStreamsBuilder().setConfig(cfgFileName).setStreamListener(dsl).setTaskListener(dsl));
			}

			if (streamThreads != null) {
				boolean complete = false;
				while (!complete) {
					try {
						synchronized (streamThreads) {
							streamThreads.wait();
							if (restarting) {
								complete = true;
							}
						}
					} catch (InterruptedException exc) {
					}
				}
			}
		}
	}

	/**
	 * Returns jKool LLC TNT4J-Streams runtime environment properties string.
	 *
	 * @return runtime environment properties string
	 */
	static String runEnv() {
		String[] envProps = new String[] { // set of interesting runtime environment properties
				"java.version", "java.vendor", "java.vm.name", "java.vm.version", // JVM props
				"os.name", "os.version" // OS props
		};

		StringBuilder sb = new StringBuilder();
		sb.append("\n"); // NON-NLS
		sb.append("------------------------------------------------------------------------\n"); // NON-NLS
		for (String property : envProps) {
			sb.append(String.format("%20s: %s", // NON-NLS
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, property),
					System.getProperty(property)));
			sb.append("\n"); // NON-NLS
		}
		sb.append("------------------------------------------------------------------------\n"); // NON-NLS

		return sb.toString();
	}

	/**
	 * Returns jKool LLC TNT4J-Streams package version.
	 *
	 * @return the version of the implementation, {@code null} is returned if it is not known
	 */
	static String pkgVersion() {
		Package sPkg = StreamsAgent.class.getPackage();
		return sPkg.getImplementationVersion();
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param builder
	 *            streams builder to build streams context
	 *
	 * @see #run(com.jkoolcloud.tnt4j.streams.configure.build.StreamsBuilder)
	 */
	public static void runFromAPI(StreamsBuilder builder) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.start.api", pkgVersion(), runEnv());
		if (builder instanceof CfgStreamsBuilder) {
			loadConfigAndRun((CfgStreamsBuilder) builder);
		} else {
			run(builder);
		}
	}

	/**
	 * Loads stream configuration from provided file path and runs configuration defined streams.
	 *
	 * @param builder
	 *            streams builder to build streams context
	 *
	 * @see #run(com.jkoolcloud.tnt4j.streams.configure.build.StreamsBuilder)
	 */
	protected static void loadConfigAndRun(CfgStreamsBuilder builder) {
		try {
			builder.loadConfig(osPipeInput, haltOnUnparsed);
		} catch (SAXException | IllegalStateException e) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.cfg.error", Utils.getExceptionMessages(e));
		} catch (Throwable e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.start.failed", e);
		}

		run(builder);
	}

	/**
	 * Loads streams data source configuration from ZooKeeper node.
	 *
	 * @param zookeeperCfgFile
	 *            ZooKeeper configuration file path
	 * @param zookeeperStreamId
	 *            ZooKeeper stream identifier
	 * @return {@code true} if stream configuration was loaded from ZooKeeper node, {@code false} - otherwise
	 */
	protected static boolean loadZKConfig(String zookeeperCfgFile, String zookeeperStreamId) {
		Properties zooProps = ZKConfigManager.readStreamsZKConfig(zookeeperCfgFile);

		if (MapUtils.isNotEmpty(zooProps)) {
			try {
				ZKConfigManager.openConnection(zooProps);
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						ZKConfigManager.close();
					}
				}, "ZKConfigManagerShutdownHookThread"));

				String path = zooProps.getProperty(ZKConfigManager.PROP_CONF_PATH_STREAM);
				if (StringUtils.isEmpty(path) && StringUtils.isNotEmpty(zookeeperStreamId)) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsAgent.streams.registry.sid", zookeeperStreamId);
					path = ZKConfigManager.getZKNodePath(zooProps, zookeeperStreamId);
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsAgent.streams.registry.sid.zk.path", zookeeperStreamId, path);

					if (StringUtils.isEmpty(path)) {
						throw new IllegalArgumentException(
								StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
										"StreamsAgent.invalid.stream.id", zookeeperStreamId));
					}

					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsAgent.streams.registry.sid.setup", zookeeperStreamId);
					ZKConfigManager.setupZKNodeData(zooProps, zookeeperStreamId);
					ZKConfigManager.setupZKNodeData(zooProps, ZKConfigManager.PROP_CONF_LOGGER);
					ZKConfigManager.setupZKNodeData(zooProps, ZKConfigManager.PROP_CONF_TNT4J);
					// ZKConfigManager.setupZKNodeData(zooProps, ZKConfigManager.PROP_CONF_TNT4J_KAFKA);
				}

				path = zooProps.getProperty(ZKConfigManager.PROP_CONF_PATH_LOGGER);

				if (StringUtils.isNotEmpty(path)) {
					LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsAgent.zk.cfg.monitor.logger", path);

					ZKConfigManager.handleZKStoredConfiguration(path, new ZKConfigManager.ZKConfigChangeListener() {
						@Override
						public void applyConfigurationData(byte[] data) {
							LoggerUtils.setLoggerConfig(data, LOGGER);
						}
					});
				}

				path = StringUtils.isEmpty(zookeeperStreamId)
						? zooProps.getProperty(ZKConfigManager.PROP_CONF_PATH_STREAM)
						: ZKConfigManager.getZKNodePath(zooProps, zookeeperStreamId);

				if (path != null) {
					LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsAgent.zk.cfg.monitor.streams", path);

					ZKConfigManager.handleZKStoredConfiguration(path, new ZKConfigManager.ZKConfigChangeListener() {
						@Override
						public void applyConfigurationData(byte[] data) {
							restartStreams(Utils.bytesReader(data));
						}
					});
					return true;
				}
			} catch (Throwable exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "StreamsAgent.zk.cfg.failed",
						exc);
			}
		}

		return false;
	}

	/**
	 * Checks if there are active stream threads running on runner JVM.
	 *
	 * @return {@code true} if streams threads group is not {@code null} and has active treads running, {@code false} -
	 *         otherwise
	 */
	public static boolean isStreamsRunning() {
		return streamThreads != null && streamThreads.activeCount() > 0;
	}

	/**
	 * Stops all running streams within default streams thread group.
	 */
	public static void stopStreams() {
		stopStreams(streamThreads);
	}

	/**
	 * Stops running streams identified by {@code streamNames} defined names within default streams thread group.
	 *
	 * @param streamNames
	 *            names of the stream to stop
	 */
	public static void stopStreams(String... streamNames) {
		stopStreams(streamThreads, streamNames);
	}

	/**
	 * Stops running streams identified by {@code streamNames} defined names within provided {@code streamThreads}
	 * group. When {@code streamNames} is {@code null} - all group streams are stopped.
	 *
	 * @param streamThreads
	 *            thread group running all streams threads
	 * @param streamNames
	 *            names of the streams to stop
	 */
	public static void stopStreams(ThreadGroup streamThreads, String... streamNames) {
		if (streamThreads != null) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.stopping.streams", streamThreads.getName(), Arrays.toString(streamNames));
			Thread[] atl = new Thread[streamThreads.activeCount()];
			streamThreads.enumerate(atl, false);

			List<StreamThread> stl = new ArrayList<>(atl.length);

			for (Thread t : atl) {
				if (t instanceof StreamThread) {
					StreamThread st = (StreamThread) t;
					if (ArrayUtils.isEmpty(streamNames) || ArrayUtils.contains(streamNames, st.getTarget().getName())) {
						stl.add(st);
					}
				}
			}

			if (stl.isEmpty()) {
				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsAgent.streams.stop.empty");
			} else {
				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsAgent.streams.stop.start", stl.size());
				CountDownLatch streamsCompletionSignal = new CountDownLatch(stl.size());
				Duration sd = Duration.arm();

				for (StreamThread st : stl) {
					st.addCompletionLatch(streamsCompletionSignal);

					st.getTarget().stop();
				}

				try {
					streamsCompletionSignal.await();
				} catch (InterruptedException exc) {
				}

				// StreamsCache.cleanup();

				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsAgent.streams.stop.complete", sd.duration());
			}
		}
	}

	/**
	 * Restarts all streams within default streams thread group.
	 */
	public static void restartStreams() {
		restartStreams(null);
	}

	/**
	 * Restart set of streams within default streams thread group.
	 *
	 * @param streamNames
	 *            set of stream names to restart
	 */
	public static void restartStreams(String... streamNames) {
		restartStreams(null, streamNames);
	}

	/**
	 * Restart set of streams within default streams thread group.
	 *
	 * @param reader
	 *            streams configuration data reader
	 * @param streamNames
	 *            set of stream names to restart
	 */
	public static void restartStreams(Reader reader, String... streamNames) {
		if (isStreamsRunning()) {
			restarting = false;
			stopStreams(streamNames);
			restarting = true;
		}

		try {
			loadConfigAndRun(new CfgStreamsBuilder().setConfig(reader).setNames(streamNames));
		} catch (Throwable exc) {
			synchronized (streamThreads) {
				streamThreads.notifyAll();
			}
		}
	}

	/**
	 * Pauses all streams within default streams thread group.
	 */
	public static void pauseStreams() {
		pauseStreams(streamThreads);
	}

	/**
	 * Pauses set of streams within default streams thread group.
	 *
	 * @param streamNames
	 *            set of stream names to pause
	 */
	public static void pauseStreams(String... streamNames) {
		pauseStreams(streamThreads, streamNames);
	}

	/**
	 * Pauses set of streams within provided streams thread group.
	 *
	 * @param streamThreads
	 *            streams thread group
	 * @param streamNames
	 *            set of stream names to pause
	 */
	public static void pauseStreams(ThreadGroup streamThreads, String... streamNames) {
		// TODO:
		LOGGER.log(OpLevel.INFO, "PAUSING TBD!");
	}

	/**
	 * Performs actions on agent process completion.
	 */
	public static void complete() {
		new StreamStatisticsReporter(TNTInputStreamStatistics.getMetrics(), null).report(LOGGER);
		TNTInputStreamStatistics.clear();
		DefaultEventSinkFactory.shutdownAll();
		MBeansManager.unregisterMBeans();
	}

	/**
	 * Builds streams instances to be run, adds listeners and runs streams on separate threads.
	 *
	 * @param builder
	 *            streams builder to build streams context
	 */
	protected static void run(StreamsBuilder builder) {
		dataSourceProperties.putAll(builder.getDataSourceProperties());
		Collection<TNTInputStream<?, ?>> streams = builder.getStreams();

		if (CollectionUtils.isEmpty(streams)) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsAgent.no.activity.streams"));
		}

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable exc) {
				if (exc instanceof ThreadDeath) {
					return;
				}

				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsAgent.uncaught.exception", t, exc);
			}
		});

		if (streamThreads == null) {
			streamThreads = new StreamThreadGroup(StreamsAgent.class.getName() + "Threads"); // NON-NLS
		}

		MBeansManager.registerMBeans();

		InputStreamListener streamListener = builder.getStreamListener();
		StreamTasksListener streamTasksListener = builder.getTasksListener();

		StreamThread ft;
		for (TNTInputStream<?, ?> stream : streams) {
			if (streamListener != null) {
				stream.addStreamListener(streamListener);
			}

			if (streamTasksListener != null) {
				stream.addStreamTasksListener(streamTasksListener);
			}

			stream.output().setProperty(OutputProperties.PROP_TNT4J_CONFIG_ZK_NODE,
					ZKConfigManager.getZKCfgProperty(ZKConfigManager.PROP_CONF_PATH_TNT4J));
			stream.output().setProperty(OutputProperties.PROP_TNT4J_CONFIG_ZK_NODE,
					ZKConfigManager.getZKCfgProperty(ZKConfigManager.PROP_CONF_PATH_TNT4J));
			// TODO: tnt4j-kafka settings
			// ZKConfigManager.getZKCfgProperty(ZKConfigManager.PROP_CONF_PATH_TNT4J_KAFKA);

			ft = new StreamThread(streamThreads, stream,
					String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
			ft.start();
		}
	}

	/**
	 * Process and interprets command-line arguments.
	 *
	 * @param args
	 *            command-line arguments
	 * @return {@code true} if command-line arguments were valid to interpret, {@code false} - otherwise
	 */
	static boolean processArgs(String... args) {
		for (String arg : args) {
			if (StringUtils.isEmpty(arg)) {
				continue;
			}
			if (arg.startsWith(PARAM_STREAM_CFG)) {
				if (StringUtils.isNotEmpty(cfgFileName)) {
					logOutput(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args"));
					printUsage();
					return false;
				}

				cfgFileName = arg.substring(PARAM_STREAM_CFG.length());
				if (StringUtils.isEmpty(cfgFileName)) {
					logOutput(OpLevel.ERROR, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, PARAM_STREAM_CFG.length())));
					printUsage();
					return false;
				}
			} else if (arg.startsWith(PARAM_ZOOKEEPER_CFG)) {
				if (StringUtils.isNotEmpty(zookeeperCfgFile)) {
					logOutput(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args2"));
					printUsage();
					return false;
				}

				zookeeperCfgFile = arg.substring(PARAM_ZOOKEEPER_CFG.length());
				if (StringUtils.isEmpty(zookeeperCfgFile)) {
					logOutput(OpLevel.ERROR, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, PARAM_ZOOKEEPER_CFG.length())));
					printUsage();
					return false;
				}
			} else if (arg.startsWith(PARAM_ZOOKEEPER_STREAM_ID)) {
				if (StringUtils.isNotEmpty(zookeeperStreamId)) {
					logOutput(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args3"));
					printUsage();
					return false;
				}
				zookeeperStreamId = arg.substring(PARAM_ZOOKEEPER_STREAM_ID.length());
				if (StringUtils.isEmpty(zookeeperStreamId)) {
					logOutput(OpLevel.ERROR,
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"StreamsAgent.missing.argument.value",
									arg.substring(0, PARAM_ZOOKEEPER_STREAM_ID.length())));
					printUsage();
					return false;
				}
			} else if (arg.startsWith(PARAM_OS_PIPE_INPUT)) {
				osPipeInput = true;
			} else if (PARAM_SKIP_UNPARSED.equals(arg)) {
				haltOnUnparsed = false;
			} else if (PARAM_HELP1.equals(arg) || PARAM_HELP2.equals(arg)) {
				printUsage();
				return false;
			} else {
				logOutput(OpLevel.ERROR, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.invalid.argument", arg));
				printUsage();
				return false;
			}
		}

		return true;
	}

	/**
	 * Prints short standalone application usage manual.
	 */
	protected static void printUsage() {
		logOutput(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.help"));
	}

	private static void logOutput(OpLevel lvl, String msg) {
		LOGGER.log(lvl, msg);
		if (lvl.ordinal() >= OpLevel.ERROR.ordinal()) {
			System.err.println(msg);
		} else {
			System.out.println(msg);
		}
	}

	/**
	 * Returns streams configuration file name/path.
	 *
	 * @return streams configuration file name
	 */
	static String getCfgFileName() {
		return cfgFileName;
	}

	/**
	 * Returns list of running stream names.
	 *
	 * @return list of running stream names
	 */
	public static Collection<String> getRunningStreamNames() {
		List<String> runningStreamNames = new ArrayList<>();

		if (streamThreads != null) {
			Thread[] atl = new Thread[streamThreads.activeCount()];
			streamThreads.enumerate(atl, false);

			for (Thread t : atl) {
				if (t instanceof StreamThread) {
					StreamThread st = (StreamThread) t;
					runningStreamNames.add(st.getTarget().getName());
				}
			}
		}

		return runningStreamNames;
	}
}
