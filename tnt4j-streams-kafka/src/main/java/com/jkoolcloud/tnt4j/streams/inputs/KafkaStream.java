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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.KafkaStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.*;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;

/**
 * Implements a Kafka topics transmitted activity stream, where each message body is assumed to represent a single
 * activity or event which should be recorded. Topic to listen is defined using "Topic" property in stream
 * configuration.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On message reception message data is packed
 * into {@link Map} filling these entries:
 * <ul>
 * <li>ActivityTopic - topic name message with activity data was received.</li>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from message.</li>
 * <li>ActivityTransport - activity transport definition: 'Kafka'.</li>
 * <li>Partition - partition from which this record is received.</li>
 * <li>Offset - position of this record in the corresponding Kafka partition.</li>
 * <li>Key - message key (or null if no key is specified).</li>
 * <li>Timestamp - timestamp of this record.</li>
 * <li>TimestampType - timestamp type of this record.</li>
 * <li>SerializedKeySize - size of the serialized, uncompressed key in bytes.</li>
 * <li>SerializedValueSize - size of the serialized, uncompressed value in bytes.</li>
 * <li>Epoch - leader epoch for the record if available.</li>
 * <li>Checksum - checksum (CRC32) of the record.</li>
 * <li>Headers - map message headers contained values</li>
 * </ul>
 * <p>
 * NOTE: if {@link org.apache.kafka.clients.consumer.ConsumerRecord} is preferred to be used as activity RAW data
 * packages, use {@link KafkaConsumerStream} instead.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>Topic - topic name to listen. (Required)</li>
 * <li>StartServer - flag indicating if stream has to start Kafka server on startup. Default value - {@code false}.
 * (Optional)</li>
 * <li>StartZooKeeper - flag indicating if stream has to start ZooKeeper server on startup. Default value -
 * {@code false}. (Optional)</li>
 * <li>List of properties used by Kafka API, e.g., zookeeper.connect, group.id. @see
 * <a href="https://kafka.apache.org/documentation/#consumerconfigs">for more details on Kafka consumer configuration
 * properties</a>. @see <a href="https://kafka.apache.org/documentation/#configuration">Kafka configuration
 * reference</a></li>.
 * </ul>
 * <p>
 * Default ZooKeeper and Kafka server configuration properties are loaded from configuration files referenced by Java
 * System properties:
 * <ul>
 * <li>tnt4j.zookeeper.config - defines path of ZooKeeper server configuration properties file. Sample:
 * {@code -Dtnt4j.zookeeper.config=tnt4j-streams-kafka/config/zookeeper.properties}</li>
 * <li>tnt4j.kafka.srv.config - defines path of Kafka server configuration properties file. Sample:
 * {@code -Dtnt4j.kafka.srv.config=tnt4j-streams-kafka/config/kafka-server.properties}</li>
 * </ul>
 * <p>
 * NOTE: those file defined Kafka server properties gets merged with ones defined in stream configuration - user defined
 * properties.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see ActivityMapParser
 * @see kafka.server.KafkaServer
 * @see KafkaConsumerStream
 */
public class KafkaStream extends TNTParseableInputStream<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(KafkaStream.class);

	/**
	 * Kafka server/consumer properties scope mapping key.
	 */
	protected static final String PROP_SCOPE_COMMON = "common"; // NON-NLS
	/**
	 * Kafka server properties scope mapping key.
	 */
	protected static final String PROP_SCOPE_SERVER = "server"; // NON-NLS
	/**
	 * Kafka consumer properties scope mapping key.
	 */
	protected static final String PROP_SCOPE_CONSUMER = "consumer"; // NON-NLS

	/**
	 * System property key for ZooKeeper server configuration properties file path.
	 */
	protected static final String ZK_PROP_FILE_KEY = "tnt4j.zookeeper.config"; // NON-NLS
	/**
	 * System property key for Kafka server configuration properties file path.
	 */
	protected static final String KS_PROP_FILE_KEY = "tnt4j.kafka.srv.config"; // NON-NLS

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private Consumer<?, ?> consumer;
	private String topicName;
	boolean autoCommit = true;

	private ServerCnxnFactory zkCnxnFactory;
	private FileTxnSnapLog zLog;
	private boolean startZooKeeper = false;
	private KafkaServerStartable server;
	private boolean startServer = false;

	private List<ConsumerRecord<?, ?>> messageBuffer = new ArrayList<>();

	private Map<String, Properties> userKafkaProps = new HashMap<>(3);

	/**
	 * Constructs a new KafkaStream.
	 */
	public KafkaStream() {
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
		} else if (StreamProperties.PROP_START_SERVER.equalsIgnoreCase(name)) {
			startServer = Utils.toBoolean(value);
		} else if (KafkaStreamProperties.PROP_START_ZOOKEEPER.equalsIgnoreCase(name)) {
			startZooKeeper = Utils.toBoolean(value);
		} else if (!StreamsConstants.isStreamCfgProperty(KafkaStreamProperties.class, name)) {
			addUserKafkaProperty(name, decPassword(value));
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}

		if (StreamProperties.PROP_START_SERVER.equalsIgnoreCase(name)) {
			return startServer;
		}

		if (KafkaStreamProperties.PROP_START_ZOOKEEPER.equalsIgnoreCase(name)) {
			return startZooKeeper;
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
	 * Gets user defined (from stream configuration) Kafka server configuration property value.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @return property value, or {@code null} if property is not set
	 */
	protected String getUserKafkaProperty(String pName) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		String[] pParts = tokenizePropertyName(pName);
		Properties sProperties = userKafkaProps.get(pParts[0]);

		return sProperties == null ? null : sProperties.getProperty(pParts[1]);
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
			pParts[0] = PROP_SCOPE_COMMON;
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

		if (!PROP_SCOPE_COMMON.equals(scope)) {
			sProperties = userKafkaProps.get(PROP_SCOPE_COMMON);
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
					"TNTInputStream.property.undefined", StreamProperties.PROP_TOPIC_NAME));
		}

	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (startServer) {
			if (startZooKeeper) {
				startZooKeeper();
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"KafkaStream.server.starting");

			Properties srvProp = getServerProperties(getScopeProps(PROP_SCOPE_SERVER));
			server = new KafkaServerStartable(new KafkaConfig(srvProp));
			server.startup();

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"KafkaStream.server.started");
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"KafkaStream.consumer.starting");

		Properties cProperties = getScopeProps(PROP_SCOPE_CONSUMER);
		consumer = new KafkaConsumer<>(cProperties);
		autoCommit = Utils.getBoolean(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, cProperties, true);
	}

	/**
	 * Starts ZooKeeper server instance.
	 *
	 * @throws Exception
	 *             if an error occurred wile starting ZooKeeper server
	 */
	protected void startZooKeeper() throws Exception {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"KafkaStream.zookeeper.server.starting");

		// ZooKeeperServerMain.main();

		ServerConfig sc = new ServerConfig();
		sc.parse(System.getProperty(ZK_PROP_FILE_KEY));

		ZooKeeperServer zkServer = new ZooKeeperServer();
		zLog = new FileTxnSnapLog(sc.getDataLogDir(), sc.getDataDir());
		zkServer.setTxnLogFactory(zLog);
		zkServer.setTickTime(sc.getTickTime());
		zkServer.setMinSessionTimeout(sc.getMinSessionTimeout());
		zkServer.setMaxSessionTimeout(sc.getMaxSessionTimeout());
		zkCnxnFactory = ServerCnxnFactory.createFactory(sc.getClientPortAddress(), sc.getMaxClientCnxns());
		zkCnxnFactory.startup(zkServer);

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"KafkaStream.zookeeper.server.started");
	}

	/**
	 * Loads Kafka server configuration properties.
	 *
	 * @param userDefinedProps
	 *            properties set to append
	 * @return properties set appended with loaded Kafka server configuration properties
	 *
	 * @throws IOException
	 *             if an error occurred when reading properties file
	 */
	protected static Properties getServerProperties(Properties userDefinedProps) throws IOException {
		putIfAbsent(userDefinedProps, "zookeeper.connect", "localhost:2181/tnt4j_kafka"); // NON-NLS

		Properties fProps = Utils.loadPropertiesFor(KS_PROP_FILE_KEY);

		for (Map.Entry<?, ?> pe : fProps.entrySet()) {
			putIfAbsent(userDefinedProps, String.valueOf(pe.getKey()), pe.getValue());
		}

		return userDefinedProps;
	}

	/**
	 * Updates provided properties set by setting property value if properties has no such property yet set or property
	 * value is empty.
	 *
	 * @param props
	 *            properties to update
	 * @param key
	 *            property name
	 * @param value
	 *            property value to set
	 * @return flag indicating whether property was updated - {@code true}, {@code false} if not
	 */
	protected static boolean putIfAbsent(Properties props, String key, Object value) {
		if (StringUtils.isEmpty(props.getProperty(key))) {
			props.put(key, String.valueOf(value));

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a map structured content of next raw activity data item received over Kafka consumer.
	 * Returned {@link Map} contains:
	 * <ul>
	 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TOPIC_KEY}</li>
	 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#ACTIVITY_DATA_KEY}</li>
	 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TRANSPORT_KEY}</li>
	 * </ul>
	 */
	@Override
	public Map<String, ?> getNextItem() throws Exception {
		while (!closed.get() && !isHalted()) {
			try {
				if (messageBuffer.isEmpty()) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"KafkaStream.empty.messages.buffer");
					Map<String, Integer> topicCountMap = new HashMap<>();
					topicCountMap.put(topicName, 1);

					ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(Long.MAX_VALUE));

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
									"KafkaStream.committing.offset", partition, lastOffset);
						}
					}
				}

				if (!messageBuffer.isEmpty()) {
					ConsumerRecord<?, ?> msg = messageBuffer.remove(0);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"KafkaStream.next.message", Utils.toString(msg.value()));

					Map<String, Object> msgDataMap = new HashMap<>();
					msgDataMap.put(StreamsConstants.TOPIC_KEY, msg.topic());
					msgDataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, msg.value());
					msgDataMap.put(StreamsConstants.TRANSPORT_KEY, KafkaStreamConstants.TRANSPORT_KAFKA);
					msgDataMap.put("Partition", msg.partition()); // NON-NLS
					msgDataMap.put("Offset", msg.offset()); // NON-NLS
					msgDataMap.put("Timestamp", msg.timestamp()); // NON-NLS
					msgDataMap.put("TimestampType", msg.timestampType()); // NON-NLS
					msgDataMap.put("SerializedKeySize", msg.serializedKeySize()); // NON-NLS
					msgDataMap.put("SerializedValueSize", msg.serializedValueSize()); // NON-NLS
					msgDataMap.put("Key", msg.key()); // NON-NLS
					// msgDataMap.put("Epoch", msg.leaderEpoch().orElse(null)); // NON-NLS
					msgDataMap.put("Checksum", msg.checksum()); // NON-NLS

					Iterable<Header> headers = msg.headers();
					if (!IterableUtils.isEmpty(headers)) {
						Map<String, byte[]> hMap = new HashMap<>();
						for (Header header : headers) {
							hMap.put(header.key(), header.value());
						}

						msgDataMap.put("Headers", hMap); // NON-NLS
					}

					addStreamedBytesCount(
							Math.max(msg.serializedKeySize(), 0) + Math.max(msg.serializedValueSize(), 0));

					return msgDataMap;
				}
			} catch (Exception e) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"KafkaStream.retrieving.messages.timeout");
			}
		}
		logger().log(OpLevel.INFO, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"KafkaStream.stopping");
		return null;
	}

	/**
	 * Adds consumer records from provided {@code records} collection to stream input buffer.
	 *
	 * @param records
	 *            records collection to add to stream input buffer
	 */
	protected void addRecordsToBuffer(Iterable<? extends ConsumerRecord<?, ?>> records) {
		for (ConsumerRecord<?, ?> record : records) {
			String msgData = Utils.toString(record.value());
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"KafkaStream.next.message", msgData);

			messageBuffer.add(record);
		}
	}

	@Override
	protected void cleanup() {
		if (server != null) {
			server.shutdown();
			server.awaitShutdown();
		}

		if (zLog != null) {
			try {
				zLog.close();
			} catch (IOException exc) {
			}
		}

		if (zkCnxnFactory != null) {
			zkCnxnFactory.shutdown();
		}

		closed.set(true);
		if (consumer != null) {
			consumer.wakeup();
			consumer.unsubscribe();
			consumer.close();
		}

		userKafkaProps.clear();

		super.cleanup();
	}
}
