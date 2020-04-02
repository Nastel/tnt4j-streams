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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics.MetricsReporter;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.MsgTraceReporter;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * TNT4J-Streams Kafka interceptions manager. It loads interceptions configuration from {@code "interceptors.config"}
 * referenced properties file (default name is {@code "interceptors.properties"} and initiates interceptions reporters
 * to be used.
 * <p>
 * There are those types of reporters:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics.MetricsReporter} - to aggregate
 * and report Kafka metrics collected over interceptions and JMX to dedicated Kafka topic.</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.MsgTraceReporter} - to trace
 * intercepted messages and send message related events to jKoolCloud.</li>
 * </ul>
 * <p>
 * Interceptions configuration properties:
 * <ul>
 * <li>{@code metrics.report.period} - metrics reporting period in seconds. Default value - {@code 30} sec.</li>
 * <li>{@code metrics.report.delay} - delay (in seconds) before first metrics reporting is invoked. If not defined, it
 * is equal to {@code metrics.report.period}.</li>
 * <li>{@code trace.kafka.messages} - flag indicating whether to trace intercepted messages. Default value -
 * {@code true}.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics.MetricsReporter
 * @see com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.MsgTraceReporter
 */
public class InterceptionsManager {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(InterceptionsManager.class);

	private static final String DEFAULT_INTERCEPTORS_PROP_FILE = "interceptors.properties"; // NON-NLS
	private static final String DEFAULT_TRACKER_CFG_FILE = "tnt4j_kafka_metrics_default.properties"; // NON-NLS

	private final Set<TNTKafkaInterceptor> references = new HashSet<>(5);
	private final Collection<InterceptionsReporter> reporters = new ArrayList<>(2);

	private static InterceptionsManager instance;

	private String interceptorsPropFile = DEFAULT_INTERCEPTORS_PROP_FILE;
	private Properties interceptorProps = new Properties();

	private InterceptionsManager() {
	}

	private void loadProperties() {
		interceptorsPropFile = getInterceptorsConfigFile();

		try (FileInputStream fis = new FileInputStream(interceptorsPropFile)) {
			interceptorProps.load(fis);
		} catch (IOException exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.cfg.load.failed", interceptorProps, exc);
		}
	}

	public static String getInterceptorsConfigFile() {
		return System.getProperty("interceptors.config", DEFAULT_INTERCEPTORS_PROP_FILE); // NON-NLS
	}

	private void initialize() {
		loadProperties();

		if (interceptorProps.isEmpty()) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.init.using.defaults");
		} else {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.init.using.props", interceptorProps);
		}

		int reportingPeriod = Utils.getInt("metrics.report.period", interceptorProps,
				MetricsReporter.DEFAULT_REPORTING_PERIOD_SEC);
		int reportingDelay = Utils.getInt("metrics.report.delay", interceptorProps, reportingPeriod);
		if (reportingPeriod > 0) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.metrics.reporter.starting");
			addReporter(new MetricsReporter(reportingPeriod, reportingDelay));
		} else {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.metrics.reporter.skipping", "metrics.report.period", reportingPeriod);
		}
		String traceMessages = Utils.getString("messages.tracer.trace", interceptorProps, MsgTraceReporter.NONE);
		Set<String> traceOpts = MsgTraceReporter.getTraceOptsSet(traceMessages);
		if (MsgTraceReporter.isTraceEnabled(traceOpts)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.msg.tracer.starting");
			addReporter(new MsgTraceReporter(interceptorProps, traceOpts));
		} else {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.msg.tracer.skipping", "messages.tracer.trace", traceMessages);
		}
	}

	/**
	 * Returns default Kafka metrics tracker configuration file path.
	 * 
	 * @return default Kafka metrics tracker configuration file path
	 */
	public static URL getDefaultTrackerConfiguration() {
		return Utils.getResource(DEFAULT_TRACKER_CFG_FILE);
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
	 * Binds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to bind
	 */
	public void bindReference(TNTKafkaInterceptor ref) {
		references.add(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.bind.reference", ref, references.size());
	}

	/**
	 * Unbinds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to unbind
	 */
	public void unbindReference(TNTKafkaInterceptor ref) {
		references.remove(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.unbind.reference", ref, references.size());

		if (references.isEmpty()) {
			for (InterceptionsReporter rep : reporters) {
				rep.shutdown();
			}
		}
	}

	/**
	 * Checks if Kafka producer is intercepted.
	 *
	 * @return {@code true} if interceptors references has any instance if
	 *         {@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor} class, {@code false}
	 *         - otherwise
	 */
	public boolean isProducerIntercepted() {
		return isClientIntercepted(TNTKafkaPInterceptor.class);
	}

	/**
	 * Checks if Kafka consumer is intercepted.
	 *
	 * @return {@code true} if interceptors references has any instance if
	 *         {@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor} class, {@code false}
	 *         - otherwise
	 */
	public boolean isConsumerIntercepted() {
		return isClientIntercepted(TNTKafkaCInterceptor.class);
	}

	private boolean isClientIntercepted(Class<? extends TNTKafkaInterceptor> refClass) {
		for (TNTKafkaInterceptor ref : references) {
			if (refClass.isInstance(ref)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds interceptions reporter to reporters list.
	 *
	 * @param reporter
	 *            reporter instance to add
	 */
	public void addReporter(InterceptionsReporter reporter) {
		reporters.add(reporter);
	}

	/**
	 * Notifies reporters that Kafka producer has sent message.
	 *
	 * @param interceptor
	 *            interceptor instance invoking send
	 * @param producerRecord
	 *            producer record to be sent
	 * @param clusterResource
	 *            cluster resource metadata records where sent to
	 *
	 * @return producer record to be sent
	 */
	public ProducerRecord<Object, Object> send(TNTKafkaPInterceptor interceptor,
			ProducerRecord<Object, Object> producerRecord, ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.send", producerRecord);

		for (InterceptionsReporter rep : reporters) {
			rep.send(interceptor, producerRecord, clusterResource);
		}

		return producerRecord;
	}

	/**
	 * Notifies reporters that Kafka producer has acknowledge sent message.
	 *
	 * @param interceptor
	 *            interceptor instance invoking acknowledge
	 * @param recordMetadata
	 *            sent message record metadata
	 * @param e
	 *            occurred exception
	 * @param clusterResource
	 *            cluster resource metadata records where sent to
	 */
	public void acknowledge(TNTKafkaPInterceptor interceptor, RecordMetadata recordMetadata, Exception e,
			ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.acknowledge", recordMetadata, e); // NOTE: exception logging

		for (InterceptionsReporter rep : reporters) {
			rep.acknowledge(interceptor, recordMetadata, e, clusterResource);
		}
	}

	/**
	 * Notifies reporters that Kafka consumer has consumed messages.
	 *
	 * @param interceptor
	 *            interceptor instance invoking commit
	 * @param consumerRecords
	 *            consumed records collection
	 * @param clusterResource
	 *            cluster resource metadata records where consumed from
	 *
	 * @return consumed records collection
	 */
	public ConsumerRecords<Object, Object> consume(TNTKafkaCInterceptor interceptor,
			ConsumerRecords<Object, Object> consumerRecords, ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.consume", consumerRecords);

		for (InterceptionsReporter rep : reporters) {
			rep.consume(interceptor, consumerRecords, clusterResource);
		}

		return consumerRecords;
	}

	/**
	 * Notifies reporters that Kafka consumer has committed messages.
	 *
	 * @param interceptor
	 *            interceptor instance invoking commit
	 * @param map
	 *            committed records topics and messages map
	 * @param clusterResource
	 *            cluster resource metadata records where consumed from
	 */
	public void commit(TNTKafkaCInterceptor interceptor, Map<TopicPartition, OffsetAndMetadata> map,
			ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.commit", map);

		for (InterceptionsReporter rep : reporters) {
			rep.commit(interceptor, map, clusterResource);
		}
	}

	/**
	 * Collects configurations of all referenced interceptors configurations. Configuration of interceptor is bound over
	 * {@link org.apache.kafka.clients.producer.ProducerInterceptor#configure(java.util.Map)} or
	 * {@link org.apache.kafka.clients.consumer.ConsumerInterceptor#configure(java.util.Map)} methods.
	 * <p>
	 * Key of produced map is {@code "client.id"} from interceptor configuration, value - configuration map of
	 * interceptor.
	 * <p>
	 * Does same as calling {@code getInterceptorsConfig(null)}.
	 *
	 * @return map of referenced interceptors configurations
	 *
	 * @see #getInterceptorsConfig(Class)
	 */
	public Map<String, Map<String, ?>> getInterceptorsConfig() {
		return getInterceptorsConfig(null);
	}

	/**
	 * Collects configurations of all class {@code iClass} matching referenced interceptors configurations.
	 * Configuration of interceptor is bound over
	 * {@link org.apache.kafka.clients.producer.ProducerInterceptor#configure(java.util.Map)} or
	 * {@link org.apache.kafka.clients.consumer.ConsumerInterceptor#configure(java.util.Map)} methods.
	 * <p>
	 * Key of produced map is {@code "client.id"} from interceptor configuration, value - configuration map of
	 * interceptor.
	 *
	 * @param iClass
	 *            interceptor type class to match, or {@code null} to iterate over all referenced interceptors
	 * @return map of class matching referenced interceptors configurations
	 *
	 * @see TNTKafkaCInterceptor#getConfig()
	 */
	public Map<String, Map<String, ?>> getInterceptorsConfig(Class<? extends TNTKafkaInterceptor> iClass) {
		Map<String, Map<String, ?>> interceptorsCfg = new HashMap<>();
		for (TNTKafkaInterceptor ref : references) {
			if (iClass == null || ref.getClass().isAssignableFrom(iClass)) {
				Map<String, ?> refCfg = ref.getConfig();
				String clientId = (String) refCfg.get(ConsumerConfig.CLIENT_ID_CONFIG);
				interceptorsCfg.put(clientId, refCfg);
			}
		}

		return interceptorsCfg;
	}
}
