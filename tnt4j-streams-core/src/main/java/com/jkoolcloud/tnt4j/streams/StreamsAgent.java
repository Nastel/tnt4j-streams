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

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigManager;
import com.jkoolcloud.tnt4j.streams.inputs.*;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.Duration;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Main class for jKool LLC TNT4J-Streams standalone application.
 *
 * @version $Revision: 2 $
 */
public final class StreamsAgent {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsAgent.class);

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
				"StreamsAgent.start.main", pkgVersion(), System.getProperty("java.version"));
		boolean argsValid = processArgs(args);
		if (argsValid) {
			boolean loadedZKConfig = loadZKConfig(zookeeperCfgFile, zookeeperStreamId);

			if (!loadedZKConfig) {
				loadConfigAndRun(cfgFileName);
				// DefaultTNTStreamListener dsl = new DefaultTNTStreamListener (LOGGER);
				// loadConfigAndRun(cfgFileName, dsl, dsl);
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
	 * <p>
	 * Requires streams data source configuration to be referenced over system property
	 * {@value com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader#STREAMS_CONFIG_KEY}.
	 */
	public static void runFromAPI() {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.start.api", pkgVersion(), System.getProperty("java.version"));
		loadConfigAndRun((Reader) null);
	}

	/**
	 * Main entry point for running as a API integration.
	 * <p>
	 * Requires streams data source configuration to be referenced over system property
	 * {@value com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader#STREAMS_CONFIG_KEY}.
	 *
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	public static void runFromAPI(InputStreamListener streamListener, StreamTasksListener streamTasksListener) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.start.api", pkgVersion(), System.getProperty("java.version"));
		loadConfigAndRun((Reader) null, streamListener, streamTasksListener);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFileName
	 *            stream data source configuration file name
	 */
	public static void runFromAPI(String cfgFileName) {
		runFromAPI(cfgFileName, null, null);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFileName
	 *            stream data source configuration file name
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	public static void runFromAPI(String cfgFileName, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.start.api", pkgVersion(), System.getProperty("java.version"));
		loadConfigAndRun(cfgFileName, streamListener, streamTasksListener);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFile
	 *            stream data source configuration file
	 */
	public static void runFromAPI(File cfgFile) {
		runFromAPI(cfgFile, null, null);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFile
	 *            stream data source configuration file
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	public static void runFromAPI(File cfgFile, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.start.api", pkgVersion(), System.getProperty("java.version"));
		loadConfigAndRun(cfgFile, streamListener, streamTasksListener);
	}

	/**
	 * Main entry point for running as a API integration without using stream data source configuration XML file.
	 *
	 * @param streams
	 *            streams to run
	 */
	public static void runFromAPI(TNTInputStream<?, ?>... streams) {
		runFromAPI(null, null, streams);
	}

	/**
	 * Main entry point for running as a API integration without using stream data source configuration XML file.
	 *
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 * @param streams
	 *            streams to run
	 */
	public static void runFromAPI(InputStreamListener streamListener, StreamTasksListener streamTasksListener,
			TNTInputStream<?, ?>... streams) {
		run(Arrays.asList(streams), streamListener, streamTasksListener);
	}

	protected static void loadConfigAndRun(String cfgFileName) {
		loadConfigAndRun(cfgFileName, null, null);
	}

	protected static void loadConfigAndRun(String cfgFileName, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		loadConfigAndRun(StringUtils.isEmpty(cfgFileName) ? null : new File(cfgFileName), streamListener,
				streamTasksListener);
	}

	protected static void loadConfigAndRun(File cfgFile) {
		loadConfigAndRun(cfgFile, null, null);
	}

	protected static void loadConfigAndRun(File cfgFile, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsAgent.loading.config.file", cfgFile == null ? StreamsConfigLoader.getDefaultFile() : cfgFile);
		try {
			loadConfigAndRun(cfgFile == null ? null : new FileReader(cfgFile), streamListener, streamTasksListener);
		} catch (FileNotFoundException e) {
			LOGGER.log(OpLevel.ERROR, Utils.getExceptionMessages(e));
		}
	}

	protected static void loadConfigAndRun(InputStream is) {
		loadConfigAndRun(is, null, null);
	}

	protected static void loadConfigAndRun(InputStream is, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		loadConfigAndRun(new InputStreamReader(is), streamListener, streamTasksListener);
	}

	protected static void loadConfigAndRun(Reader reader) {
		loadConfigAndRun(reader, null, null);
	}

	protected static void loadConfigAndRun(Reader reader, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		try {
			initAndRun(reader == null ? new StreamsConfigLoader() : new StreamsConfigLoader(reader), streamListener,
					streamTasksListener);
		} catch (SAXException | IllegalStateException e) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.cfg.error", Utils.getExceptionMessages(e));
		} catch (Exception e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.start.failed", e);
		}
	}

	/**
	 * Configure streams and parsers, and run each stream in its own thread.
	 *
	 * @param cfg
	 *            stream data source configuration
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	protected static void initAndRun(StreamsConfigLoader cfg, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) throws Exception {
		if (cfg == null) {
			return;
		}

		if (cfg.isErroneous()) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsAgent.erroneous.configuration"));
		}

		Collection<TNTInputStream<?, ?>> streams;
		if (osPipeInput) {
			streams = initPiping(cfg);
		} else {
			streams = cfg.getStreams();
		}
		if (CollectionUtils.isEmpty(streams)) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsAgent.no.activity.streams"));
		}

		run(streams, streamListener, streamTasksListener);
	}

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
				}));

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
							if (isStreamsRunning()) {
								restarting = false;
								stopStreams();
								restarting = true;
							}

							try {
								loadConfigAndRun(Utils.bytesReader(data));
							} catch (Exception exc) {
								synchronized (streamThreads) {
									streamThreads.notifyAll();
								}
							}
						}
					});
					return true;
				}
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "StreamsAgent.zk.cfg.failed",
						exc);
			}
		}

		return false;
	}

	protected static boolean isStreamsRunning() {
		return streamThreads != null && streamThreads.activeCount() > 0;
	}

	/**
	 * Stops all running streams within default streams thread group.
	 */
	public static void stopStreams() {
		stopStreams(streamThreads);
	}

	/**
	 * Stops all running streams within provided <tt>streamThreads</tt> group.
	 *
	 * @param streamThreads
	 *            thread group running all streams threads
	 */
	public static void stopStreams(ThreadGroup streamThreads) {
		if (streamThreads != null) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.stopping.streams", streamThreads.getName());
			Thread[] atl = new Thread[streamThreads.activeCount()];
			streamThreads.enumerate(atl, false);

			List<StreamThread> stl = new ArrayList<>(atl.length);

			for (Thread t : atl) {
				if (t instanceof StreamThread) {
					stl.add((StreamThread) t);
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

		DefaultEventSinkFactory.shutdownAll();
	}

	/**
	 * Adds listeners to provided streams and runs streams on separate threads.
	 *
	 * @param streams
	 *            streams to run
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	protected static void run(Collection<TNTInputStream<?, ?>> streams, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsAgent.uncaught.exception", t, exc);
			}
		});

		if (streamThreads == null) {
			streamThreads = new ThreadGroup(StreamsAgent.class.getName() + "Threads"); // NON-NLS
		}

		StreamThread ft;
		// final CountDownLatch streamsCompletionSignal = new CountDownLatch(streams.size());
		for (TNTInputStream<?, ?> stream : streams) {
			if (streamListener != null) {
				stream.addStreamListener(streamListener);
			}

			if (streamTasksListener != null) {
				stream.addStreamTasksListener(streamTasksListener);
			}

			stream.output().setProperty(OutputProperties.PROP_TNT4J_CONFIG_ZK_NODE,
					ZKConfigManager.getZKCfgProperty(ZKConfigManager.PROP_CONF_PATH_TNT4J));
			// TODO: tnt4j-kafka settings
			// ZKConfigManager.getZKCfgProperty(ZKConfigManager.PROP_CONF_PATH_TNT4J_KAFKA);

			ft = new StreamThread(streamThreads, stream,
					String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
			// ft.addCompletionLatch(streamsCompletionSignal);
			ft.start();
		}

		// try {
		// streamsCompletionSignal.await();
		// } catch (InterruptedException exc) {
		// }
	}

	protected static Collection<TNTInputStream<?, ?>> initPiping(StreamsConfigLoader cfg) throws Exception {
		Collection<TNTInputStream<?, ?>> streams = new ArrayList<>(1);

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

		streams.add(pipeStream);

		return streams;
	}

	/**
	 * Process and interprets command-line arguments.
	 *
	 * @param args
	 *            command-line arguments
	 * @return {@code true} if command-line arguments where valid to interpret, {@code false} - otherwise
	 */
	static boolean processArgs(String... args) {
		for (String arg : args) {
			if (StringUtils.isEmpty(arg)) {
				continue;
			}
			if (arg.startsWith(PARAM_STREAM_CFG)) {
				if (StringUtils.isNotEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args"));
					printUsage();
					return false;
				}

				cfgFileName = arg.substring(PARAM_STREAM_CFG.length());
				if (StringUtils.isEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, PARAM_STREAM_CFG.length())));
					printUsage();
					return false;
				}
			} else if (arg.startsWith(PARAM_ZOOKEEPER_CFG)) {
				if (StringUtils.isNotEmpty(zookeeperCfgFile)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args2"));
					printUsage();
					return false;
				}

				zookeeperCfgFile = arg.substring(PARAM_ZOOKEEPER_CFG.length());
				if (StringUtils.isEmpty(zookeeperCfgFile)) {
					System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, PARAM_ZOOKEEPER_CFG.length())));
					printUsage();
					return false;
				}
			} else if (arg.startsWith(PARAM_ZOOKEEPER_STREAM_ID)) {
				if (StringUtils.isNotEmpty(zookeeperStreamId)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args3"));
					printUsage();
					return false;
				}
				zookeeperStreamId = arg.substring(PARAM_ZOOKEEPER_STREAM_ID.length());
				if (StringUtils.isEmpty(zookeeperStreamId)) {
					System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
		System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.help"));
	}

	/**
	 * Returns streams configuration file name/path.
	 *
	 * @return streams configuration file name
	 */
	static String getCfgFileName() {
		return cfgFileName;
	}
}
