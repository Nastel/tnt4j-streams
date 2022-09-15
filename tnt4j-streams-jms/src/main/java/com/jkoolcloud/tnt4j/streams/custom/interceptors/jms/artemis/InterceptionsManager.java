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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.configure.build.CfgStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.build.POJOStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.build.StreamsBuilder;
import com.jkoolcloud.tnt4j.streams.inputs.InputStreamEventsAdapter;
import com.jkoolcloud.tnt4j.streams.inputs.StreamStatus;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * TNT4J-Streams Artemis JMS interceptions manager. It loads configuration from files located next to System property
 * {@value TrackerConfigStore#TNT4J_PROPERTIES_KEY} referenced properties file, or as default
 * {@code "tnt4j/tnt4j.properties"} within Artemis configuration directory - usually {@code "<BROKER_DIR>/etc"}.
 * <p>
 * There are those types of interceptors:
 * <ul>
 * <li>{@link AmqpInterceptor} - to intercept Amqp packets.</li>
 * <li>{@link MQTTInterceptor} - to intercept MQTT packets.</li>
 * <li>{@link OpenWireInterceptor} - to intercept OpenWire packets.</li>
 * <li>{@link PacketInterceptor} - to intercept core Artemis packets.</li>
 * <li>{@link StompInterceptor} - to intercept Stomp packets.</li>
 * </ul>
 * <p>
 *
 * @version $Revision: 1 $
 *
 * @see AbstractArtemisInterceptor
 * @see JMSInterceptorStream
 */
public class InterceptionsManager {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(InterceptionsManager.class);

	private static final String DEFAULT_CFG_PATH = "tnt4j/tnt4j.properties"; // NON-NLS
	private static final String DEFAULT_ARTEMIS_CFG_DIR = "etc"; // NON-NLS

	private static final String CFG_SCOPE_ANY = "any"; // NON-NLS

	private static final String[] CFG_SCOPES = new String[] { //
			CFG_SCOPE_ANY //
			, AmqpInterceptor.SCOPE //
			, MQTTInterceptor.SCOPE //
			, OpenWireInterceptor.SCOPE //
			, PacketInterceptor.SCOPE //
			, StompInterceptor.SCOPE //
	};

	private static final String ALL_CLASSES_REGEX = ".*"; // NON-NLS

	private final Set<AbstractArtemisInterceptor<?>> references = new LinkedHashSet<>(5);
	private final Set<JMSInterceptorStream> jmsInterceptorStreams = new HashSet<>(3);

	private final Map<String, ScopeConfiguration> interceptorsConfig = new HashMap<>();

	private static InterceptionsManager instance;

	private InterceptionsManager() {
	}

	private void initialize() {
		String tnt4jPropsPath = System.getProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY);
		if (tnt4jPropsPath == null) {
			String artemisBrokerConfigPath = getArtemisConfigDir();
			tnt4jPropsPath = artemisBrokerConfigPath + DEFAULT_CFG_PATH;
			System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, tnt4jPropsPath);
		}

		LOGGER.log(OpLevel.INFO, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
				"InterceptionsManager.init.tnt4j.config"), tnt4jPropsPath);
		LOGGER.log(OpLevel.INFO, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
				"InterceptionsManager.init.tnt4j.config.continue"));

		File cfgPath = null;
		File tnt4jPropsFile = new File(tnt4jPropsPath);
		if (tnt4jPropsFile.exists()) {
			cfgPath = tnt4jPropsFile.getParentFile();
		}

		if (cfgPath != null) {
			// LOG4J
			File log4jCfg = new File(cfgPath, "log4j2.xml");
			if (log4jCfg.exists()) {
				try (InputStream is = Files.newInputStream(log4jCfg.toPath())) {
					LoggerUtils.loadLog4j2Config(is);
				} catch (NoClassDefFoundError exc) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
							"InterceptionsManager.init.log4j.no.class"));
				} catch (Exception exc) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
							"InterceptionsManager.init.fail.log4j"), exc);
				}
			}

			// Interceptor configuration
			try {
				Properties interceptorProps = Utils.loadPropertiesFile(cfgPath + "/interceptors.properties"); // NON-NLS

				if (interceptorProps.isEmpty()) {
					LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
							"InterceptionsManager.init.using.defaults");
				} else {
					LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
							"InterceptionsManager.init.using.props", interceptorProps);
				}

				for (String scope : CFG_SCOPES) {
					Properties sCfgProps = extractScopeProperties(interceptorProps, scope + "."); // NON-NLS
					if (sCfgProps == null || sCfgProps.isEmpty()) {
						continue;
					}

					ScopeConfiguration sCfg = new ScopeConfiguration();
					interceptorsConfig.put(scope, sCfg);

					String prop = sCfgProps.getProperty("intercepted.data.map.layout"); // NON-NLS
					if (StringUtils.isNotEmpty(prop)) {
						sCfg.interceptedDataMapLayout = MapConstructionStrategy.valueOf(prop.toUpperCase());
					}

					prop = sCfgProps.getProperty("intercept.packet.include.pattern"); // NON-NLS
					if (StringUtils.isNotEmpty(prop)) {
						sCfg.includePacketPattern = Pattern.compile(prop);
					}

					prop = sCfgProps.getProperty("intercept.packet.exclude.pattern"); // NON-NLS
					if (StringUtils.isNotEmpty(prop)) {
						sCfg.excludePacketPattern = Pattern.compile(prop);
					}
				}
			} catch (Throwable exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
						"InterceptionsManager.init.fail.interceptor"), exc);
			}

			// TNT4J-Streams data source
			File dataSourceCfg = new File(cfgPath, "tnt-data-source.xml");
			if (dataSourceCfg.exists()) {
				try (InputStream fis = Files.newInputStream(dataSourceCfg.toPath())) {
					StreamsBuilder streamsBuilder = new CfgStreamsBuilder().setConfig(fis);
					StreamsConfigLoader cfgLoader = ((CfgStreamsBuilder) streamsBuilder).loadConfig(false, true);

					Collection<TNTInputStream<?, ?>> streams = streamsBuilder.getStreams();
					if (CollectionUtils.isNotEmpty(streams)) {
						for (TNTInputStream<?, ?> stream : streams) {
							if (stream instanceof JMSInterceptorStream) {
								jmsInterceptorStreams.add((JMSInterceptorStream) stream);
							}
						}
					} else {
						JMSInterceptorStream jmsInterceptorStream = new JMSInterceptorStream(
								"ArtemisInterceptorStream"); // NON-NLS
						jmsInterceptorStream.addParsers(cfgLoader.getParsers());
						jmsInterceptorStreams.add(jmsInterceptorStream);

						streamsBuilder = new POJOStreamsBuilder().addStream(jmsInterceptorStream);
					}

					CountDownLatch streamStartSignal = new CountDownLatch(jmsInterceptorStreams.size());
					InputStreamEventsAdapter startupListener = new InputStreamEventsAdapter() {
						@Override
						public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
							if (status.ordinal() >= StreamStatus.STARTED.ordinal()) {
								streamStartSignal.countDown();
							}
						}
					};
					for (JMSInterceptorStream stream : jmsInterceptorStreams) {
						stream.addStreamListener(startupListener);
					}
					StreamsAgent.runFromAPI(streamsBuilder);
					try {
						streamStartSignal.await();
					} catch (Throwable t) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
								"InterceptionsManager.stream.start.wait.interrupted", jmsInterceptorStreams.size(), t);
					}
					for (JMSInterceptorStream stream : jmsInterceptorStreams) {
						stream.removeStreamListener(startupListener);
					}
				} catch (Throwable exc) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
							"InterceptionsManager.init.fail.streams"), exc);
				}
			}
		}
	}

	private String getArtemisConfigDir() {
		String cfgPath = System.getProperty("artemis.instance.etc"); // apache-artemis-2.19.1\bin\mybroker0\etc
		if (StringUtils.isEmpty(cfgPath)) {
			cfgPath = System.getProperty("artemis.instance"); // apache-artemis-2.19.1\bin\mybroker0
			if (StringUtils.isNotEmpty(cfgPath)) {
				cfgPath += File.separator + DEFAULT_ARTEMIS_CFG_DIR;
			}
		}
		if (StringUtils.isEmpty(cfgPath)) {
			cfgPath = System.getProperty("data.dir"); // apache-artemis-2.19.1\bin\mybroker0\data
			if (StringUtils.isNotEmpty(cfgPath)) {
				File path = new File(cfgPath);
				cfgPath += path.getParent() + File.separator + DEFAULT_ARTEMIS_CFG_DIR;
			}
		}
		if (StringUtils.isEmpty(cfgPath)) {
			cfgPath = System.getProperty("user.dir"); // apache-artemis-2.19.1\bin\mybroker0\bin
			if (StringUtils.isNotEmpty(cfgPath)) {
				File path = new File(cfgPath);
				cfgPath += path.getParent() + File.separator + DEFAULT_ARTEMIS_CFG_DIR;
			}
		}
		// if (StringUtils.isEmpty(cfgPath)) {
		// cfgPath = System.getProperty("artemis.home"); // apache-artemis-2.19.1
		// }
		if (StringUtils.isEmpty(cfgPath)) {
			cfgPath = "." + File.separator + ".." + File.separator + DEFAULT_ARTEMIS_CFG_DIR; // NON-NLS
		}

		return cfgPath + File.separator;
	}

	/**
	 * Collects properties relevant to defined scope.
	 *
	 * @param interceptorProperties
	 *            complete set of interceptors configuration properties
	 * @param scopePrefix
	 *            configuration scope prefix
	 * @return scope relevant configuration properties
	 */
	protected static Properties extractScopeProperties(Properties interceptorProperties, String scopePrefix) {
		Properties props = new Properties();
		for (String key : interceptorProperties.stringPropertyNames()) {
			if (key.startsWith(scopePrefix)) {
				props.put(key.substring(scopePrefix.length()), interceptorProperties.getProperty(key));
			}
		}
		return props;
	}

	/**
	 * Gets instance of interceptions manager.
	 *
	 * @return instance of interceptions manager
	 */
	public static synchronized InterceptionsManager getInstance() {
		if (instance == null) {
			instance = new InterceptionsManager();
			instance.initialize();
		}

		return instance;
	}

	/**
	 * Returns configured interceptor produced map layout for defined scope.
	 *
	 * @param scope
	 *            intercepted packet scope (protocol)
	 * @return interceptor produced map layout
	 */
	public MapConstructionStrategy getInterceptedDataMapLayout(String scope) {
		ScopeConfiguration sCfg = getScopeConfig(scope);

		return sCfg.interceptedDataMapLayout;
	}

	/**
	 * Checks if intercepted packet shall be intercepted by inspecting if scope include/exclude configuration matches
	 * packet class name.
	 *
	 * @param scope
	 *            intercepted packet scope (protocol)
	 * @param cls
	 *            intercepted packet class
	 * @return {@code true} if packet class name matches scope include pattern and does not match exclude pattern,
	 *         {@code false} - otherwise
	 *
	 * @see #shallIntercept(String, String)
	 */
	public boolean shallIntercept(String scope, Class<?> cls) {
		return shallIntercept(scope, cls.getName());
	}

	/**
	 * Checks if intercepted packet shall be intercepted by inspecting if scope include/exclude configuration matches
	 * packet class name.
	 *
	 * @param scope
	 *            intercepted packet scope (protocol)
	 * @param className
	 *            intercepted packet class name
	 * @return {@code true} if packet class name matches scope include pattern and does not match exclude pattern,
	 *         {@code false} - otherwise
	 */
	public boolean shallIntercept(String scope, String className) {
		ScopeConfiguration sCfg = getScopeConfig(scope);

		return sCfg.includePacketPattern.matcher(className).find()
				&& (sCfg.excludePacketPattern == null || !sCfg.excludePacketPattern.matcher(className).find());
	}

	private ScopeConfiguration getScopeConfig(String scope) {
		ScopeConfiguration sCfg = interceptorsConfig.get(scope);
		if (sCfg == null) {
			sCfg = interceptorsConfig.get(CFG_SCOPE_ANY);
		}

		return sCfg;
	}

	/**
	 * Passes intercepted objects properties map to bound streams for further parsing.
	 *
	 * @param packetMap
	 *            intercepted objects properties values map
	 */
	public void intercept(Map<String, Object> packetMap) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
				"InterceptionsManager.passing.map.to.streams"), Utils.toString(packetMap));
		for (JMSInterceptorStream jmsInterceptorStream : jmsInterceptorStreams) {
			jmsInterceptorStream.addInputToBuffer(packetMap);
		}
	}

	/**
	 * Binds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to bind
	 */
	public void bindReference(AbstractArtemisInterceptor<?> ref) {
		references.add(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.bind.reference", ref, references.size());
	}

	/**
	 * Unbinds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to unbind
	 */
	public void unbindReference(AbstractArtemisInterceptor<?> ref) {
		references.remove(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.unbind.reference", ref, references.size());

		finalizeReferences();
	}

	private void finalizeReferences() {
		if (references.isEmpty()) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.shutdown.streams", jmsInterceptorStreams.size());

			for (JMSInterceptorStream jmsInterceptorStream : jmsInterceptorStreams) {
				jmsInterceptorStream.markEnded();
			}

			jmsInterceptorStreams.clear();
		}
	}

	/**
	 * Cleans and unbinds all references bound to this manager.
	 */
	public void shutdown() {
		int refCount = references.size();
		references.clear();

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.unbind.all.reference", refCount);

		finalizeReferences();
	}

	private static class ScopeConfiguration {
		private MapConstructionStrategy interceptedDataMapLayout = MapConstructionStrategy.FLAT;

		private Pattern includePacketPattern = Pattern.compile(ALL_CLASSES_REGEX);
		private Pattern excludePacketPattern = null;
	}

	/**
	 * The enumeration defining map construction strategies.
	 */
	public enum MapConstructionStrategy {
		/**
		 * Map shall be single level (flat) where entry key is made by appending complex type property names path.
		 */
		FLAT,
		/**
		 * Map shall be constructed by making dedicated value map for complex type properties.
		 */
		DEEP
	}
}
