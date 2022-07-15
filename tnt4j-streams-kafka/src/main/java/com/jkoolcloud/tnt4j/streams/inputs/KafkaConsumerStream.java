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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.KafkaStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements a Kafka topics transmitted activity stream, where each message body is assumed to represent a single
 * activity or event which should be recorded. Topic to listen is defined using "Topic" property in stream
 * configuration.
 * <p>
 * This activity stream requires parsers that can support {@link ConsumerRecord} data like
 * {@link com.jkoolcloud.tnt4j.streams.parsers.KafkaConsumerRecordParser}.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>Topic - defines set of topic names (delimited using '|' character) to listen. (Required - just one of: 'Topic' or
 * 'TopicPattern')</li>
 * <li>TopicPattern - defines topic name RegEx pattern. (Required - just one of: 'Topic' or 'TopicPattern')</li>
 * <li>Offset - defines list of topic offsets (delimited using '|' character) to start consuming messages. Single value
 * applies to all topics. Default value - {@code -1 (from latest)}. (Optional)</li>
 * <li>FileName - Kafka Consumer configuration file ({@code "consumer.properties"}) path. (Optional)</li>
 * <li>List of Kafka Consumer configuration properties. See
 * <a href="https://kafka.apache.org/documentation/#consumerconfigs">Kafka Consumer configuration reference</a>.</li>
 * </ul>
 * <p>
 * NOTE: those file defined Kafka consumer properties gets merged with ones defined in stream configuration - user
 * defined properties. So you can take some basic consumer configuration form file and customize it using stream
 * configuration defined properties.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see com.jkoolcloud.tnt4j.streams.parsers.KafkaConsumerRecordParser
 */
public class KafkaConsumerStream extends AbstractBufferedStream<ConsumerRecord<?, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(KafkaConsumerStream.class);

	/**
	 * Kafka consumer user (over stream configuration) defined configuration scope mapping key.
	 */
	protected static final String PROP_SCOPE_USER = "user"; // NON-NLS
	/**
	 * Kafka consumer properties file defined configuration scope mapping key.
	 */
	protected static final String PROP_SCOPE_CONSUMER = "consumer"; // NON-NLS

	private String topicName;
	private Set<String> topicNames;
	private Pattern topicPattern;
	private String offset;
	private List<Integer> offsets;
	private String cfgFileName;

	private Map<String, Properties> userKafkaProps = new HashMap<>(3);

	private KafkaDataReceiver kafkaDataReceiver;

	/**
	 * Constructs a new KafkaConsumerStream.
	 */
	public KafkaConsumerStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			topicName = value;

			if (StringUtils.isNotEmpty(value)) {
				String[] topics = Utils.splitValue(topicName);
				topicNames = new LinkedHashSet<>(topics.length);

				for (String topic : topics) {
					String tTopic = topic.trim();
					if (!tTopic.isEmpty()) {
						topicNames.add(tTopic);
					}
				}
			}
		} else if (KafkaStreamProperties.PROP_TOPIC_PATTERN.equalsIgnoreCase(name)) {
			topicName = value;

			if (StringUtils.isNotEmpty(value)) {
				topicPattern = Pattern.compile(value);
			}
		} else if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			cfgFileName = value;
		} else if (KafkaStreamProperties.PROP_OFFSET.equalsIgnoreCase(name)) {
			offset = value;

			if (StringUtils.isNotEmpty(value)) {
				String[] offsetArray = Utils.splitValue(offset);
				offsets = new ArrayList<>(offsetArray.length);

				for (String offst : offsetArray) {
					String tOffst = offst.trim();
					offsets.add(tOffst.isEmpty() ? -1 : Integer.parseInt(tOffst));
				}
			}
		} else if (!StreamsConstants.isStreamCfgProperty(name, KafkaStreamProperties.class)) {
			addUserKafkaProperty(name, decPassword(value));
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (KafkaStreamProperties.PROP_TOPIC_PATTERN.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (KafkaStreamProperties.PROP_OFFSET.equalsIgnoreCase(name)) {
			return offset;
		}
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return cfgFileName;
		}

		Object prop = super.getProperty(name);
		if (prop == null) {
			prop = getUserKafkaProperty(name);
		}

		return prop;
	}

	/**
	 * Adds Kafka configuration property to user defined (from stream configuration) properties map.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @param pValue
	 *            property value
	 * @return the previous value of the specified property in user's Kafka configuration property list, or {@code null}
	 *         if it did not have one
	 */
	protected Object addUserKafkaProperty(String pName, String pValue) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		String[] pParts = tokenizePropertyName(pName);

		Properties sProps = userKafkaProps.get(pParts[0]);
		if (sProps == null) {
			sProps = new Properties();
			userKafkaProps.put(pParts[0], sProps);
		}

		return sProps.setProperty(pParts[1], pValue);
	}

	/**
	 * Gets user defined (from stream configuration) Kafka consumer configuration property value.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @return property value, or {@code null} if property is not set
	 */
	protected String getUserKafkaProperty(String pName) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		// String[] pParts = tokenizePropertyName(pName);
		Properties sProperties = userKafkaProps.get(PROP_SCOPE_USER);

		return sProperties == null ? null : sProperties.getProperty(pName);
	}

	/**
	 * Splits fully qualified property name to property scope and name.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @return string array containing property scope and name
	 */
	protected static String[] tokenizePropertyName(String pName) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		int sIdx = pName.indexOf(':');
		String[] pParts = new String[2];

		if (sIdx >= 0) {
			pParts[0] = pName.substring(0, sIdx);
			pParts[1] = pName.substring(sIdx + 1);
		} else {
			pParts[1] = pName;
		}

		if (StringUtils.isEmpty(pParts[0])) {
			pParts[0] = PROP_SCOPE_USER;
		}

		return pParts;
	}

	/**
	 * Returns scope defined properties set.
	 *
	 * @param scope
	 *            properties scope key
	 * @return scope defined properties
	 */
	protected Properties getScopeProps(String scope) {
		Properties allScopeProperties = new Properties();

		Properties sProperties = userKafkaProps.get(scope);
		if (sProperties != null) {
			allScopeProperties.putAll(sProperties);
		}

		if (!PROP_SCOPE_USER.equals(scope)) {
			sProperties = userKafkaProps.get(PROP_SCOPE_USER);
			if (sProperties != null) {
				allScopeProperties.putAll(sProperties);
			}
		}

		return allScopeProperties;
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(topicName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined.one.of", StreamProperties.PROP_TOPIC_NAME,
					KafkaStreamProperties.PROP_TOPIC_PATTERN));
		}

		if (CollectionUtils.isNotEmpty(topicNames)) {
			if (topicPattern != null) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
						StreamProperties.PROP_TOPIC_NAME, KafkaStreamProperties.PROP_TOPIC_PATTERN));
			}
		}

		if (StringUtils.isNotEmpty(cfgFileName)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"KafkaConsumerStream.consumer.cfgFile.load", cfgFileName);
			try {
				Properties fCfgProps = Utils.loadPropertiesFile(cfgFileName);
				userKafkaProps.put(PROP_SCOPE_CONSUMER, fCfgProps);
			} catch (IOException exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"KafkaConsumerStream.consumer.cfgFile.load.failed", cfgFileName, exc);
			}
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"KafkaConsumerStream.consumer.starting");

		kafkaDataReceiver = new KafkaDataReceiver();
		kafkaDataReceiver.initialize(getScopeProps(PROP_SCOPE_CONSUMER), topicNames, topicPattern, offsets);
	}

	@Override
	protected void start() throws Exception {
		super.start();

		kafkaDataReceiver.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected long getActivityItemByteSize(ConsumerRecord<?, ?> activityItem) {
		return Math.max(activityItem.serializedKeySize(), 0) + Math.max(activityItem.serializedValueSize(), 0);
	}

	@Override
	public boolean isInputEnded() {
		return kafkaDataReceiver.isInputEnded();
	}

	@Override
	protected void cleanup() {
		if (kafkaDataReceiver != null) {
			kafkaDataReceiver.shutdown();
		}

		userKafkaProps.clear();

		super.cleanup();
	}

	private class KafkaDataReceiver extends InputProcessor {

		private Consumer<?, ?> consumer;
		private Set<String> topics;
		private Pattern topicNamePattern;
		private List<Integer> topicOffsets;
		private boolean autoCommit = true;

		private final Object closeLock = new Object();

		private KafkaDataReceiver() {
			super("KafkaConsumerStream.KafkaDataReceiver"); // NON-NLS
		}

		/**
		 * Input data receiver initialization - Kafka consumer configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize data receiver and configure Kafka consumer
		 */
		@Override
		@SuppressWarnings("unchecked")
		protected void initialize(Object... params) throws Exception {
			Properties cProperties = (Properties) params[0];
			topics = (Set<String>) params[1];
			topicNamePattern = (Pattern) params[2];
			topicOffsets = (List<Integer>) params[3];

			autoCommit = Utils.getBoolean(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, cProperties, true);
			consumer = new KafkaConsumer<>(cProperties);
		}

		/**
		 * Starts Kafka consumer client to receive incoming data. Shuts down this data receiver if exception occurs.
		 */
		@Override
		public void run() {
			if (consumer != null) {
				try {
					if (topicNamePattern != null) {
						consumer.subscribe(topicNamePattern);
						topics = consumer.subscription();
					} else {
						consumer.subscribe(topics);
					}

					if (CollectionUtils.isNotEmpty(topicOffsets)) {
						if (topicOffsets.size() > 1 && topicOffsets.size() != topics.size()) {
							throw new IllegalStateException(StreamsResources.getStringFormatted(
									StreamsResources.RESOURCE_BUNDLE_NAME, "KafkaConsumerStream.offsets.mismatch",
									topicOffsets.size(), topics.size()));
						}

						int idx = 0;
						for (String topic : topics) {
							int tOffset = topicOffsets.size() == 1 ? topicOffsets.get(idx) : topicOffsets.get(idx++);
							if (tOffset >= 0) {
								consumer.seek(new TopicPartition(topic, 0), tOffset);
							}
						}
					}

					while (!isHalted()) {
						ConsumerRecords<?, ?> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
						if (autoCommit) {
							addRecordsToBuffer(records);
						} else {
							for (TopicPartition partition : records.partitions()) {
								List<? extends ConsumerRecord<?, ?>> partitionRecords = records.records(partition);
								addRecordsToBuffer(partitionRecords);
								long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
								consumer.commitSync(
										Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
								logger().log(OpLevel.DEBUG,
										StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
										"KafkaConsumerStream.committing.offset", partition, lastOffset);
							}
						}
					}
				} catch (WakeupException exc) {
				} finally {
					consumer.close();
					synchronized (closeLock) {
						closeLock.notifyAll();
					}
				}
			}
		}

		/**
		 * Adds consumer records from provided {@code records} collection to stream input buffer.
		 *
		 * @param records
		 *            records collection to add to stream input buffer
		 *
		 * @see #addInputToBuffer(Object)
		 */
		protected void addRecordsToBuffer(Iterable<? extends ConsumerRecord<?, ?>> records) {
			for (ConsumerRecord<?, ?> record : records) {
				String msgData = Utils.toString(record.value());
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"KafkaConsumerStream.next.message", msgData);

				addInputToBuffer(record);
			}
		}

		/**
		 * Closes Kafka consume.
		 */
		@Override
		void closeInternals() {
			if (consumer != null) {
				consumer.wakeup();
				synchronized (closeLock) {
					try {
						closeLock.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}
}
