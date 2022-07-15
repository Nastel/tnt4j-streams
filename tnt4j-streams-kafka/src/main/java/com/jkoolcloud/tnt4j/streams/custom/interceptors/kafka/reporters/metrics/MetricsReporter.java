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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.metrics;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.InterceptionsManager;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.TNTKafkaPInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.utils.*;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;

/**
 * Producer/Consumer interceptors intercepted data metrics aggregator and reporter sending collected metrics data as
 * JSON over configured TNT4J {@link Tracker}.
 * <p>
 * Reported metrics:
 * <ul>
 * <li>Kafka consumer JMX metrics</li>
 * <li>Kafka producer JMX metrics</li>
 * <li>JVM JMX metrics</li>
 * <li>This reporter accumulated additional Kafka metrics</li>
 * </ul>
 * <p>
 * Metrics data is reported in scheduled intervals and on interceptions shutdown.
 *
 * @version $Revision: 2 $
 */
public class MetricsReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MetricsReporter.class);

	/**
	 * Default metrics reporting period - {@value} seconds.
	 */
	public static final int DEFAULT_REPORTING_PERIOD_SEC = 30;
	private static final String TOPIC_UNRELATED = "Topic unrelated"; // NON-NLS
	private static final String OBJ_NAME_ENTRY_KEY = "ObjectName"; // NON-NLS

	private java.util.Timer metricsReportingTimer;

	private final Tracker tracker;

	private Map<String, TopicMetrics> topicsMetrics = new HashMap<>();

	private boolean useObjectNameProperties = true;

	/**
	 * Base class defining generic metrics data for Kafka topic.
	 */
	protected abstract static class TopicMetrics {
		/**
		 * The topic name.
		 */
		String topic;
		/**
		 * The partition index.
		 */
		Integer partition;
		/**
		 * The collected metrics registry.
		 */
		MetricRegistry mRegistry = new MetricRegistry();
		/**
		 * Last producer sent message timestamp.
		 */
		long lastSend;
		/**
		 * The metrics correlator, tying all metrics packages of one particular sampling.
		 */
		String correlator;
		/**
		 * String client identifier.
		 */
		String clientId;
		/**
		 * Intercepted client call name.
		 */
		String callName;

		/**
		 * Constructs a new TopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 * @param clientId
		 *            client identifier
		 */
		TopicMetrics(String topic, Integer partition, String clientId) {
			this(topic, partition, clientId, null);
		}

		/**
		 * Constructs a new TopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 * @param clientId
		 *            client identifier
		 * @param callName
		 *            intercepted client call name
		 */
		TopicMetrics(String topic, Integer partition, String clientId, String callName) {
			this.topic = topic;
			this.partition = partition;
			this.clientId = clientId;
			this.callName = callName;
		}

		/**
		 * Resets metrics before next collection iteration (after send).
		 */
		void reset() {
			getOffset().reset();
		}

		abstract Offset getOffset();
	}

	private static class ConsumerTopicMetrics extends TopicMetrics {
		private final Meter consumeM = mRegistry.meter("consumeMeter"); // NON-NLS
		private final Counter consumeC = mRegistry.counter("consumeIterationsCounter"); // NON-NLS
		private final Counter consumeMessagesC = mRegistry.counter("consumeMessageCounter"); // NON-NLS
		private final Counter commitC = mRegistry.counter("commitCounter"); // NON-NLS
		private final Histogram keySize = mRegistry.histogram("keySize"); // NON-NLS
		private final Histogram valueSize = mRegistry.histogram("valueSize"); // NON-NLS
		private final com.codahale.metrics.Timer latency = mRegistry.timer("messageLatency");// NON-NLS

		/**
		 * Topic consumer offset data.
		 */
		COffset offset;

		/**
		 * Constructs a new ConsumerTopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 * @param clientId
		 *            client identifier
		 * @param callName
		 *            intercepted client call name
		 */
		ConsumerTopicMetrics(String topic, Integer partition, String clientId, String callName) {
			super(topic, partition, clientId, callName);
			offset = new COffset();
		}

		@Override
		COffset getOffset() {
			return offset;
		}
	}

	private static class ProducerTopicMetrics extends TopicMetrics {
		private final Histogram jitter = mRegistry.histogram("messageProducerJitter"); // NON-NLS
		private final Meter sendM = mRegistry.meter("sendMeter"); // NON-NLS
		private final Meter ackM = mRegistry.meter("ackMeter"); // NON-NLS
		private final Counter sendC = mRegistry.counter("sendCounter"); // NON-NLS
		private final Counter ackC = mRegistry.counter("ackCounter"); // NON-NLS
		private final Meter errorMeter = mRegistry.meter("errorCounter"); // NON-NLS

		/**
		 * Topic producer offset data.
		 */
		Offset offset;

		/**
		 * Constructs a new ProducerTopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 * @param clientId
		 *            client identifier
		 * @param callName
		 *            intercepted client call name
		 */
		ProducerTopicMetrics(String topic, Integer partition, String clientId, String callName) {
			super(topic, partition, clientId, callName);
			offset = new Offset();
		}

		@Override
		Offset getOffset() {
			return offset;
		}
	}

	/**
	 * Constructs a new MetricsReporter.
	 */
	public MetricsReporter() {
		this(DEFAULT_REPORTING_PERIOD_SEC, null);
	}

	/**
	 * Constructs a new MetricsReporter.
	 *
	 * @param reportingPeriod
	 *            the metrics reporting period in seconds
	 * @param reportingDelay
	 *            delay (in seconds) before first metrics reporting is invoked
	 */
	public MetricsReporter(int reportingPeriod, Integer reportingDelay) {
		tracker = initTracker();
		TimerTask mrt = new TimerTask() {
			@Override
			public void run() {
				reportMetrics(topicsMetrics);
			}
		};
		long period = TimeUnit.SECONDS.toMillis(reportingPeriod);
		long delay = reportingDelay == null ? period : TimeUnit.SECONDS.toMillis(reportingDelay);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MetricsReporter.schedule.reporting", delay, period);
		metricsReportingTimer = new java.util.Timer("KafkaInterceptorMetricsReporterTimer", true); // NON-NLS
		metricsReportingTimer.scheduleAtFixedRate(mrt, delay, period);
	}

	@Override
	public void send(TNTKafkaPInterceptor interceptor, ProducerRecord<Object, Object> producerRecord,
			ClusterResource clusterResource) {
		String topic = producerRecord.topic();
		String clientId = MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG);
		ProducerTopicMetrics topicMetrics = getProducerTopicMetrics(topic, producerRecord.partition(), clientId,
				"send"); // NON-NLS
		long now = System.currentTimeMillis();
		long jitter = now - topicMetrics.lastSend;
		topicMetrics.jitter.update(jitter);
		topicMetrics.lastSend = now;
		topicMetrics.sendM.mark();
		topicMetrics.sendC.inc();

		topicMetrics.offset.update(-1,
				producerRecord.timestamp() == null ? System.currentTimeMillis() : producerRecord.timestamp());
	}

	private ConsumerTopicMetrics getConsumerTopicMetrics(String topic, Integer partition, String clientId,
			String callName) {
		if (topic == null) {
			topic = TOPIC_UNRELATED;
		}
		String key = getMetricsKey(topic, partition, ConsumerTopicMetrics.class, callName);
		ConsumerTopicMetrics topicMetrics = (ConsumerTopicMetrics) topicsMetrics.get(key);
		if (topicMetrics == null) {
			topicMetrics = new ConsumerTopicMetrics(topic, partition, clientId, callName);
			topicsMetrics.put(key, topicMetrics);
		}

		return topicMetrics;
	}

	private ProducerTopicMetrics getProducerTopicMetrics(String topic, Integer partition, String clientId,
			String callName) {
		if (topic == null) {
			topic = TOPIC_UNRELATED;
		}
		String key = getMetricsKey(topic, partition, ProducerTopicMetrics.class, callName);
		ProducerTopicMetrics topicMetrics = (ProducerTopicMetrics) topicsMetrics.get(key);
		if (topicMetrics == null) {
			topicMetrics = new ProducerTopicMetrics(topic, partition, clientId, callName);
			topicsMetrics.put(key, topicMetrics);
		}

		return topicMetrics;
	}

	private static String getMetricsKey(String topic, Integer partition, Class<? extends TopicMetrics> metricsClass,
			String op) {
		return topic + ":" + partition + ":" + metricsClass.getSimpleName() + ":" + op; // NON-NLS
	}

	@Override
	public void acknowledge(TNTKafkaPInterceptor interceptor, RecordMetadata recordMetadata, Exception e,
			ClusterResource clusterResource) {
		String clientId = MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG);
		ProducerTopicMetrics topicMetrics = getProducerTopicMetrics(recordMetadata.topic(), recordMetadata.partition(),
				clientId, "acknowledge"); // NON-NLS
		if (e != null) {
			topicMetrics.errorMeter.mark();
		}
		topicMetrics.ackM.mark();
		topicMetrics.ackC.inc();

		if (recordMetadata.offset() != -1L) {
			topicMetrics.offset.update(recordMetadata.offset(), recordMetadata.timestamp());
		}
	}

	@Override
	public void consume(TNTKafkaCInterceptor interceptor, ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		String clientId = MapUtils.getString(interceptor.getConfig(), ConsumerConfig.CLIENT_ID_CONFIG);
		for (ConsumerRecord<?, ?> record : consumerRecords) {
			ConsumerTopicMetrics topicMetrics = getConsumerTopicMetrics(record.topic(), record.partition(), clientId,
					"consume"); // NON-NLS
			long duration = Duration.duration(record.timestamp());
			topicMetrics.latency.update(duration, TimeUnit.MILLISECONDS);
			topicMetrics.keySize.update(record.serializedKeySize());
			topicMetrics.valueSize.update(record.serializedValueSize());
			topicMetrics.consumeMessagesC.inc();
			topicMetrics.consumeM.mark();
			topicMetrics.consumeC.inc();

			topicMetrics.offset.update(record.offset(), record.timestamp(),
					record.timestampType() == null ? null : record.timestampType().toString(),
					System.currentTimeMillis());
		}
	}

	@Override
	public void commit(TNTKafkaCInterceptor interceptor, Map<TopicPartition, OffsetAndMetadata> tpomMap,
			ClusterResource clusterResource) {
		String clientId = MapUtils.getString(interceptor.getConfig(), ConsumerConfig.CLIENT_ID_CONFIG);
		for (Map.Entry<TopicPartition, OffsetAndMetadata> tpom : tpomMap.entrySet()) {
			TopicPartition partition = tpom.getKey();
			ConsumerTopicMetrics topicMetrics = getConsumerTopicMetrics(partition.topic(), partition.partition(),
					clientId, "commit"); // NON-NLS
			topicMetrics.commitC.inc();

			topicMetrics.offset.update(tpom.getValue().offset(), -1, null, System.currentTimeMillis());
		}
	}

	@Override
	public void shutdown() {
		metricsReportingTimer.cancel();
		reportMetrics(topicsMetrics);

		synchronized (tracker) {
			Utils.close(tracker);
		}
	}

	/**
	 * Reports (logs over the tracker) Kafka topic metrics provided by metrics registry map.
	 *
	 * @param mRegistry
	 *            metrics registry map
	 */
	protected void reportMetrics(Map<String, TopicMetrics> mRegistry) {
		synchronized (tracker) {
			String metricsCorrelator = tracker.newUUID();

			try {
				TrackingActivity metricsJMX = collectJVMMetricsJMX(tracker);
				metricsJMX.setCorrelator(metricsCorrelator);
				prepareAndSend(metricsJMX);

				if (InterceptionsManager.getInstance().isProducerIntercepted()) {
					metricsJMX = collectKafkaProducerMetricsJMX(tracker);
					metricsJMX.setCorrelator(metricsCorrelator);
					prepareAndSend(metricsJMX);
				}
				if (InterceptionsManager.getInstance().isConsumerIntercepted()) {
					metricsJMX = collectKafkaConsumerMetricsJMX(tracker);
					metricsJMX.setCorrelator(metricsCorrelator);
					prepareAndSend(metricsJMX);
				}
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.WARNING,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MetricsReporter.clients.jmx.fail", exc);
			}

			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
				mapper.registerModule(new MetricsRegistryModule());
				for (Map.Entry<String, TopicMetrics> metrics : mRegistry.entrySet()) {
					TopicMetrics topicMetrics = metrics.getValue();
					topicMetrics.correlator = metricsCorrelator;
					String msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(topicMetrics);
					topicMetrics.reset();
					tracker.log(OpLevel.INFO, msg);
				}
			} catch (JsonProcessingException exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MetricsReporter.report.metrics.fail", exc);
			}
		}
	}

	private void prepareAndSend(TrackingActivity activity) throws JsonProcessingException {
		activity.setSeverity(OpLevel.INFO);
		tracker.tnt(activity);
	}

	private static Tracker initTracker() {
		TrackerConfig trackerConfig = DefaultConfigFactory.getInstance().getConfig(MetricsReporter.class,
				SourceType.APPL, (String) null);
		String cfgSource = trackerConfig == null ? null : trackerConfig.getProperty("source"); // NON-NLS
		if (!MetricsReporter.class.getPackage().getName().equals(cfgSource)) {
			URL defaultCfg = InterceptionsManager.getDefaultTrackerConfiguration();
			trackerConfig = DefaultConfigFactory.getInstance().getConfig(MetricsReporter.class, SourceType.APPL,
					defaultCfg.toExternalForm());
		}

		Tracker tracker = TrackingLogger.getInstance(trackerConfig.build());

		return tracker;
	}

	/**
	 * Collects Kafka consumer domain {@code 'kafka.consumer'} JMX attributes values.
	 *
	 * @param tracker
	 *            tracker instance to use
	 * @return activity containing snapshots of Kafka consumer JMX attributes values
	 *
	 * @see #collectMetricsJMX(String, javax.management.MBeanServer, com.jkoolcloud.tnt4j.core.Activity)
	 */
	private TrackingActivity collectKafkaConsumerMetricsJMX(Tracker tracker) {
		TrackingActivity activity = tracker.newActivity(OpLevel.INFO, "Kafka consumer JMX metrics"); // NON-NLS

		String objName = "kafka.consumer:*"; // NON-NLS
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX(objName, mBeanServer, activity);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.scope.jmx.fail", objName, exc);
		}

		return activity;
	}

	/**
	 * Collects Kafka producer domain {@code 'kafka.producer'} JMX attributes values.
	 *
	 * @param tracker
	 *            tracker instance to use
	 * @return activity containing snapshots of Kafka producer JMX attributes values
	 *
	 * @see #collectMetricsJMX(String, javax.management.MBeanServer, com.jkoolcloud.tnt4j.core.Activity)
	 */
	public TrackingActivity collectKafkaProducerMetricsJMX(Tracker tracker) {
		TrackingActivity activity = tracker.newActivity(OpLevel.INFO, "Kafka producer JMX metrics"); // NON-NLS

		String objName = "kafka.producer:*"; // NON-NLS
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX(objName, mBeanServer, activity);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.scope.jmx.fail", objName, exc);
		}
		return activity;
	}

	/**
	 * Collects JVM domain {@code 'java.lang'} JMX attributes values.
	 *
	 * @param tracker
	 *            tracker instance to use
	 * @return activity containing snapshots of JVM JMX attributes values
	 *
	 * @see #collectMetricsJMX(String, javax.management.MBeanServer, com.jkoolcloud.tnt4j.core.Activity)
	 */
	public TrackingActivity collectJVMMetricsJMX(Tracker tracker) {
		TrackingActivity activity = tracker.newActivity(OpLevel.INFO, "JVM JMX metrics"); // NON-NLS

		String objName = "java.lang:*"; // NON-NLS
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX(objName, mBeanServer, activity);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.scope.jmx.fail", objName, exc);
		}
		return activity;
	}

	/**
	 * Collects JMX attributes of MBeans defined by {@code objNameStr}.
	 *
	 * @param objNameStr
	 *            MBeans object name pattern to query
	 * @param mBeanServer
	 *            MBean server instance to use
	 * @param activity
	 *            activity instance to put JMX metrics snapshots
	 * @throws Exception
	 *             if JMX attributes collecting fails
	 */
	public void collectMetricsJMX(String objNameStr, MBeanServer mBeanServer, Activity activity) throws Exception {
		ObjectName oName = new ObjectName(objNameStr);
		Set<ObjectName> metricsBeans = mBeanServer.queryNames(oName, null);

		for (ObjectName mBeanName : metricsBeans) {
			try {
				PropertySnapshot snapshot = new PropertySnapshot(mBeanName.getCanonicalName());
				MBeanInfo metricsBean = mBeanServer.getMBeanInfo(mBeanName);
				MBeanAttributeInfo[] pMetricsAttrs = metricsBean.getAttributes();
				for (MBeanAttributeInfo pMetricsAttr : pMetricsAttrs) {
					try {
						String attrName = pMetricsAttr.getName();
						Object attrValue = mBeanServer.getAttribute(mBeanName, attrName);
						processAttrValue(snapshot, new PropertyNameBuilder(pMetricsAttr.getName()), attrValue);
					} catch (Exception exc) {
						if (exc.getCause() instanceof UnsupportedOperationException) {
							LOGGER.log(OpLevel.WARNING,
									StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
									"MetricsReporter.bean.attr.unsupported", mBeanName, pMetricsAttr.getName(),
									exc.getCause().getMessage());
						} else {
							Utils.logThrowable(LOGGER, OpLevel.WARNING,
									StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
									"MetricsReporter.bean.attr.fail", mBeanName, pMetricsAttr.getName(), exc);
						}
					}
				}

				if (getSnapshotPropIgnoreCase(snapshot, OBJ_NAME_ENTRY_KEY) == null) {
					snapshot.add(OBJ_NAME_ENTRY_KEY, mBeanName.getCanonicalName());
				}
				if (useObjectNameProperties) {
					snapshot.add("domain", mBeanName.getDomain()); // NON-NLS
					Map<String, String> objNameProps = mBeanName.getKeyPropertyList();
					for (Map.Entry<String, String> objNameProp : objNameProps.entrySet()) {
						String propKey = objNameProp.getKey();
						Object mv = snapshot.get(propKey);
						snapshot.add(propKey + (mv == null ? "" : "_"), objNameProp.getValue()); // NON-NLS
					}
				}
				activity.addSnapshot(snapshot);
			} catch (Exception exc) {
				if (exc.getCause() instanceof InstanceNotFoundException) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MetricsReporter.bean.not.found", mBeanName);
				} else {
					Utils.logThrowable(LOGGER, OpLevel.WARNING,
							StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MetricsReporter.bean.info.fail", mBeanName, exc);
				}
			}
		}
	}

	/**
	 * Adds JMX attribute to properties snapshot.
	 * <p>
	 * In case attribute value is {@link javax.management.openmbean.CompositeData}, it will be split into separate
	 * properties by building property names individually for every composite data element.
	 *
	 * @param snapshot
	 *            properties snapshot instance
	 * @param propName
	 *            property name builder
	 * @param value
	 *            property value
	 */
	protected static void processAttrValue(PropertySnapshot snapshot, PropertyNameBuilder propName, Object value) {
		if (value instanceof CompositeData) {
			CompositeData cdata = (CompositeData) value;
			Set<String> keys = cdata.getCompositeType().keySet();
			boolean isKVSet = keys.contains("key") && keys.contains("value"); // NON-NLS
			for (String key : keys) {
				Object cVal = cdata.get(key);
				if (isKVSet && "key".equals(key)) { // NON-NLS
					propName.append(Utils.toString(cVal));
				} else if (isKVSet && "value".equals(key)) { // NON-NLS
					processAttrValue(snapshot, propName, cVal);
				} else {
					processAttrValue(snapshot, propName.append(key), cVal);
				}
			}
			propName.popLevel();
		} else if (value instanceof TabularData) {
			TabularData tData = (TabularData) value;
			Collection<?> values = tData.values();
			int row = 0;
			for (Object tVal : values) {
				processAttrValue(snapshot, tVal instanceof CompositeData ? propName : propName.append(padNumber(++row)),
						tVal);
			}
			propName.popLevel();
		} else {
			snapshot.add(propName.propString(), value);
		}
	}

	private static String padNumber(int idx) {
		return idx < 10 ? "0" + idx : String.valueOf(idx);
	}

	/**
	 * Retrieves snapshot property matching provided {@code key} cases insensitively.
	 *
	 * @param snap
	 *            properties snapshot instance
	 * @param key
	 *            property key string
	 * @return snapshot property matching defined key string case insensitive
	 */
	public static Property getSnapshotPropIgnoreCase(Snapshot snap, String key) {
		if (snap != null) {
			for (Property property : snap.getProperties()) {
				if (property.getKey().equalsIgnoreCase(key)) {
					return property;
				}
			}
		}

		return null;
	}

	/**
	 * Defines metrics "jackson-databind" serializer.
	 */
	protected static class MetricsRegistrySerializer extends StdSerializer<TopicMetrics> {
		private static final long serialVersionUID = -4078902193304449876L;

		/**
		 * Constructs a new MetricsRegistrySerializer.
		 */
		MetricsRegistrySerializer() {
			super(TopicMetrics.class);
		}

		@Override
		public void serialize(TopicMetrics topicMetrics, JsonGenerator json, SerializerProvider provider)
				throws IOException {
			json.writeStartObject();
			json.writeObjectField("Topic", topicMetrics.topic); // NON-NLS
			json.writeObjectField("Partition", topicMetrics.partition); // NON-NLS
			json.writeObjectField("Type", topicMetrics.getClass().getSimpleName()); // NON-NLS
			json.writeObjectField("Metrics", topicMetrics.mRegistry); // NON-NLS
			json.writeObjectField("Correlator", topicMetrics.correlator); // NON-NLS
			json.writeObjectField("Offset", topicMetrics.getOffset().values()); // NON-NLS
			json.writeObjectField("ClientId", topicMetrics.clientId); // NON-NLS
			if (StringUtils.isNotEmpty(topicMetrics.callName)) {
				json.writeObjectField("CallName", topicMetrics.callName); // NON-NLS
			}

			json.writeEndObject();
		}
	}

	/**
	 * Defines metrics "jackson-databind" serializer module.
	 */
	protected static class MetricsRegistryModule extends Module {
		private static final Version VERSION = new Version(1, 2, 0, "", "com.jkoolcloud.tnt4j.streams", // NON-NLS
				"tnt4j-streams-kafka"); // NON-NLS

		@Override
		public String getModuleName() {
			return "TopicMetrics"; // NON-NLS
		}

		@Override
		public Version version() {
			return VERSION;
		}

		@Override
		public void setupModule(SetupContext setupContext) {
			SimpleSerializers simpleSerializers = new SimpleSerializers();
			simpleSerializers.addSerializer(new MetricsRegistrySerializer());

			setupContext.addSerializers(simpleSerializers);
		}
	}

	/**
	 * Defines producer topic offset data.
	 */
	protected static class Offset {
		/**
		 * Topic offset values map.
		 */
		final Map<String, Object> values = new HashMap<>(2);

		/**
		 * Constructs a new Offset.
		 */
		Offset() {
			reset();
		}

		/**
		 * Updates topic offset values.
		 *
		 * @param offset
		 *            topic offset index
		 * @param p_timestamp
		 *            message production timestamp
		 */
		void update(long offset, long p_timestamp) {
			synchronized (values) {
				values.put("offset", offset); // NON-NLS
				values.put("p_timestamp", p_timestamp); // NON-NLS
			}
		}

		/**
		 * Returns topic offset values map.
		 * 
		 * @return topic offset values map
		 */
		Map<?, ?> values() {
			synchronized (values) {
				return values;
			}
		}

		/**
		 * Resets topic offset values.
		 */
		void reset() {
			update(-1L, -1L);
		}
	}

	/**
	 * Defines consumer topic offset data.
	 */
	protected static class COffset extends Offset {
		/**
		 * Constructs a new COffset.
		 */
		COffset() {
			reset();
		}

		/**
		 * Updates topic offset values.
		 *
		 * @param offset
		 *            topic offset index
		 * @param p_timestamp
		 *            message production timestamp
		 * @param c_timestamp
		 *            message consumption timestamp
		 */
		void update(long offset, long p_timestamp, String tType, long c_timestamp) {
			synchronized (values) {
				values.put("offset", offset); // NON-NLS
				values.put("p_timestamp", p_timestamp); // NON-NLS
				values.put("p_timestamp_type", tType); // NON-NLS
				values.put("c_timestamp", c_timestamp); // NON-NLS
			}
		}

		@Override
		void reset() {
			update(-1L, -1L, null, -1L);
		}
	}

	/**
	 * Metric property name builder.
	 */
	protected static class PropertyNameBuilder {
		private StringBuilder sb;
		private Deque<Integer> marks;
		private String delimiter;

		/**
		 * Constructs a new PropertyNameBuilder. Default delimiter is {@code "\"}.
		 *
		 * @param initName
		 *            initial property name string
		 */
		public PropertyNameBuilder(String initName) {
			this(initName, "\\"); // NON-NLS
		}

		/**
		 * Constructs a new PropertyNameBuilder.
		 *
		 * @param initName
		 *            initial property name string
		 * @param delimiter
		 *            property tokens delimiter
		 */
		public PropertyNameBuilder(String initName, String delimiter) {
			sb = new StringBuilder(checkNull(initName));
			marks = new ArrayDeque<>(5);
			this.delimiter = delimiter;
		}

		/**
		 * Resets property name builder setting new initial property name value.
		 * <p>
		 * Internal {@link StringBuilder} is reset to {@code 0} position and marks stack is cleared.
		 *
		 * @param initName
		 *            initial property name string
		 */
		public void reset(String initName) {
			sb.setLength(0);
			sb.append(checkNull(initName));
			marks.clear();
		}

		private static String checkNull(String initName) {
			return initName == null ? "null" : initName;
		}

		/**
		 * Appends provided string to current property name in internal {@link StringBuilder}. Constructor defined
		 * {@code delimiter} is added before string to tokenize property name.
		 * <p>
		 * Before appending internal {@link StringBuilder}, current builder length is pushed to marks stack for later
		 * use.
		 *
		 * @param str
		 *            string to append to property name
		 * @return instance of this property name builder
		 */
		public PropertyNameBuilder append(String str) {
			marks.push(sb.length());
			sb.append(delimiter).append(checkNull(str));
			return this;
		}

		/**
		 * Resets internal {@link StringBuilder} to marked position of previous token. If marks stack is empty - nothing
		 * happens, leaving initial property name string in string builder.
		 */
		public void popLevel() {
			if (!marks.isEmpty()) {
				sb.setLength(marks.pop());
			}
		}

		/**
		 * Returns internal {@link StringBuilder} contained string and resets string builder to marked position of
		 * previous token.
		 *
		 * @return complete property name string
		 *
		 * @see #popLevel()
		 */
		public String propString() {
			String str = sb.toString();
			popLevel();

			return str;
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}
}
