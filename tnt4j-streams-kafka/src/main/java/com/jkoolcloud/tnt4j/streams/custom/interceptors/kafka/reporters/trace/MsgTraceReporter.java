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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.build.POJOStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.sax.ConfigParserHandler;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.InterceptionsManager;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.TNTKafkaPInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.InputStreamEventsAdapter;
import com.jkoolcloud.tnt4j.streams.inputs.InterceptorStream;
import com.jkoolcloud.tnt4j.streams.inputs.StreamStatus;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Producer/Consumer interceptors intercepted messages reporter sending jKoolCloud events containing intercepted message
 * payload data, metadata and context data.
 * <p>
 * jKool Event types sent on consumer/producer interceptions:
 * <ul>
 * <li>send - 1 {@link com.jkoolcloud.tnt4j.core.OpType#SEND} type event.</li>
 * <li>acknowledge - 1 {@link com.jkoolcloud.tnt4j.core.OpType#EVENT} type event.</li>
 * <li>consume - n {@link com.jkoolcloud.tnt4j.core.OpType#RECEIVE} type events.</li>
 * <li>commit - n {@link com.jkoolcloud.tnt4j.core.OpType#EVENT} type events.</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class MsgTraceReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MsgTraceReporter.class);

	static final String SEND = "send"; // NON-NLS
	static final String ACK = "ack"; // NON-NLS
	static final String CONSUME = "consume"; // NON-NLS
	static final String COMMIT = "commit"; // NON-NLS
	/**
	 * Trace option enabling all interceptions.
	 */
	public static final String ALL = "all"; // NON-NLS
	/**
	 * Trace option disabling all interceptions.
	 */
	public static final String NONE = "none"; // NON-NLS

	private static final String TRACER_PROPERTY_PREFIX = "messages.tracer."; // NON-NLS

	/**
	 * Constant defining tracing configuration dedicated topic name.
	 */
	public static final String TRACE_CONFIG_TOPIC = "tnt4j-trace-config-topic"; // NON-NLS
	/**
	 * Constant defining message trace reporter stream configuration properties prefix.
	 */
	public static final String STREAM_PROPERTY_PREFIX = TRACER_PROPERTY_PREFIX + "stream."; // NON-NLS
	/**
	 * Constant defining message tracing reporter configuration topic consumer configuration properties prefix.
	 */
	public static final String CFG_CONSUMER_PROPERTY_PREFIX = TRACER_PROPERTY_PREFIX + "kafka."; // NON-NLS
	/**
	 * Constant defining interceptor event parsers configuration file name.
	 */
	public static final String DEFAULT_PARSER_CONFIG_FILE = "tnt-data-source_kafka_msg_trace.xml"; // NON-NLS
	/**
	 * Constant defining default interceptor event parser name.
	 */
	public static final String DEFAULT_PARSER_NAME = "KafkaTraceParser"; // NON-NLS
	private static final String PARSER_DELIM = "#"; // NON-NLS

	private ActivityParser mainParser;

	private InterceptorStream<ActivityInfo> stream;
	private final Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig = new HashMap<>();
	private String cfgTopic = TRACE_CONFIG_TOPIC;
	private Set<String> traceOptions;

	private KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer;
	private final Object closeLock = new Object();

	/**
	 * Constructs a new MsgTraceReporter.
	 * 
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param traceOpts
	 *            messages tracing options set
	 */
	public MsgTraceReporter(Properties interceptorProperties, Set<String> traceOpts) {
		this(new InterceptorStream<>("KafkaMsgTraceStream"), interceptorProperties, true, traceOpts); // NON-NLS
	}

	/**
	 * Constructs a new MsgTraceReporter.
	 *
	 * @param stream
	 *            trace stream instance
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param enableCfgPolling
	 *            flag indicating whether to enable tracing configuration pooling from dedicated Kafka topic
	 * @param traceOpts
	 *            messages tracing options string
	 */
	MsgTraceReporter(InterceptorStream<ActivityInfo> stream, Properties interceptorProperties, boolean enableCfgPolling,
			String traceOpts) {
		this(stream, interceptorProperties, enableCfgPolling, getTraceOptsSet(traceOpts));
	}

	/**
	 * Constructs a new MsgTraceReporter.
	 *
	 * @param stream
	 *            trace stream instance
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param enableCfgPolling
	 *            flag indicating whether to enable tracing configuration pooling from dedicated Kafka topic
	 * @param traceOpts
	 *            messages tracing options set
	 */
	MsgTraceReporter(InterceptorStream<ActivityInfo> stream, Properties interceptorProperties, boolean enableCfgPolling,
			Set<String> traceOpts) {
		this.stream = stream;
		this.traceOptions = traceOpts;

		String streamName = interceptorProperties.getProperty(STREAM_PROPERTY_PREFIX + "name"); // NON-NLS
		// ---------
		// * make startup faster: intercepted stuff shall be enqueued and processed by stream when it starts.
		// Interceptions startup is slow taking delay before processing (and even failure if no connection to jkool),
		// better have delay on shutdown...
		// -----------
		stream.setName((StringUtils.isEmpty(streamName) ? stream.getName() : streamName) + "_" + Utils.getVMPID()); // NON-NLS
		Properties streamProps = extractScopeProperties(interceptorProperties, STREAM_PROPERTY_PREFIX);
		for (String propName : streamProps.stringPropertyNames()) {
			if (StreamsConstants.isStreamCfgProperty(propName, StreamProperties.class)) {
				stream.setProperty(propName, streamProps.getProperty(propName));
			}
		}

		String parserCfg = Utils.getString(STREAM_PROPERTY_PREFIX + "parser", interceptorProperties, // NON-NLS
				DEFAULT_PARSER_CONFIG_FILE + PARSER_DELIM + DEFAULT_PARSER_NAME);
		mainParser = getParser(parserCfg);
		stream.addParser(mainParser);

		CountDownLatch streamStartSignal = new CountDownLatch(1);
		InputStreamEventsAdapter startupListener = new InputStreamEventsAdapter() {
			@Override
			public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
				if (status.ordinal() >= StreamStatus.STARTED.ordinal()) {
					streamStartSignal.countDown();
				}
			}
		};
		stream.addStreamListener(startupListener);
		StreamsAgent.runFromAPI(new POJOStreamsBuilder().addStream(stream));
		try {
			streamStartSignal.await();
		} catch (Throwable t) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.stream.start.wait.interrupted", stream.getName(), t);
		}
		stream.removeStreamListener(startupListener);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.stream.started", stream.getName());

		if (enableCfgPolling) {
			String cfgTopicName = interceptorProperties.getProperty(TRACER_PROPERTY_PREFIX + "cfg.topic"); // NON-NLS
			if (StringUtils.isNotEmpty(cfgTopicName)) {
				cfgTopic = cfgTopicName;
			}
			traceConfig.put(TraceCommandDeserializer.MASTER_CONFIG, new TraceCommandDeserializer.TopicTraceCommand());

			Properties consumerConfig = extractKafkaProperties(interceptorProperties);
			Thread traceConfigPollThread = new Thread(new Runnable() {
				@Override
				public void run() {
					pollConfigQueue(consumerConfig, cfgTopic, traceConfig);
				}
			}, stream.getName() + "_TraceConfigPollThread");
			traceConfigPollThread.start();
		}
	}

	/**
	 * Loads parser name having provided {@code name} from interceptor events parsers configuration file.
	 * <p>
	 * Parser name is defined using {@code "parsers_cfg_file_path#parserName"} pattern. To define only parser name from
	 * file under default parsers configuration file path - use {@code "#parserName"} pattern.
	 * <p>
	 * When pattern token {@code "parsers_cfg_file_path"} is omitted - {@value DEFAULT_PARSER_CONFIG_FILE} is used.
	 * <p>
	 * When pattern token {@code "parserName"} is omitted - {@value DEFAULT_PARSER_NAME} is used.
	 *
	 * @param name
	 *            configuration file path and parser name
	 *
	 * @return parser instance having provided name
	 */
	protected static ActivityParser getParser(String name) {
		InputStream is = null;
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigParserHandler hndlr = new ConfigParserHandler();

			String[] nameTokens;
			if (name.contains(PARSER_DELIM)) {
				nameTokens = name.split(PARSER_DELIM);
				if (nameTokens.length == 1) {
					nameTokens = new String[] { DEFAULT_PARSER_CONFIG_FILE, name };
				}
			} else {
				nameTokens = new String[] { name, DEFAULT_PARSER_NAME };
			}

			File configFile = new File(nameTokens[0]);
			if (configFile.exists()) {
				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.loading.parsers.config", nameTokens[1], nameTokens[0]);
				is = new FileInputStream(nameTokens[0]);
			} else {
				String interceptorsPropFile = InterceptionsManager.getInterceptorsConfigFile();
				configFile = new File(interceptorsPropFile);
				String cfgFilePath = configFile.getParent() + File.separator + nameTokens[0];
				configFile = new File(cfgFilePath);
				if (configFile.exists()) {
					LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.loading.parsers.config", nameTokens[1], cfgFilePath);
					is = new FileInputStream(cfgFilePath);
				}
			}

			if (is == null) {
				configFile = null;
				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.loading.parsers.config", nameTokens[1], nameTokens[0]);
				is = Utils.getResourceAsStream(nameTokens[0]);
			}

			if (is == null) {
				throw new FileNotFoundException(
						StreamsResources.getStringFormatted(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
								"MsgTraceReporter.parsers.config.not.found", nameTokens[0]));
			}

			parser.parse(is, hndlr);

			ActivityParser mainParser = hndlr.getStreamsConfigData().getParser(nameTokens[1]);

			if (mainParser == null) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "MsgTraceReporter.parser.not.found", nameTokens[1],
						configFile == null ? nameTokens[0] : configFile.getCanonicalFile()));
			}

			return mainParser;
		} catch (Exception e) {
			throw new RuntimeException(StreamsResources.getStringFormatted(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
					"MsgTraceReporter.loading.parsers.config.failed", e.getMessage(), e.getCause()), e);
		} finally {
			Utils.close(is);
		}
	}

	/**
	 * Merges Kafka consumer, messages interceptor file and topic provided properties and puts them all into complete
	 * tracing configuration map {@code traceConfig}.
	 *
	 * @param consumerConfig
	 *            trace configuration topic consumer configuration properties
	 * @param cfgTopicName
	 *            trace configuration topic name
	 * @param traceConfig
	 *            complete message tracing configuration properties
	 */
	protected void pollConfigQueue(Properties consumerConfig, String cfgTopicName,
			Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig) {
		if (consumerConfig.isEmpty() || StringUtils.isEmpty(cfgTopicName)) {
			return;
		}

		String clintId = consumerConfig.getProperty(ConsumerConfig.CLIENT_ID_CONFIG);
		if (StringUtils.isEmpty(clintId)) {
			clintId = "kafka-x-ray-trace-config-listener"; // NON-NLS
		}
		consumerConfig.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, clintId + "_" + Utils.getVMPID()); // NON-NLS
		Utils.setPropertyIfAbsent(consumerConfig, ConsumerConfig.GROUP_ID_CONFIG, "kafka-x-ray-trace-config-consumers"); // NON-NLS

		consumerConfig.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"); // NON-NLS
		consumerConfig.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);

		consumer = createKafkaConsumer(consumerConfig, cfgTopicName);
		boolean consume = true;
		while (consume) {
			try {
				ConsumerRecords<String, TraceCommandDeserializer.TopicTraceCommand> records = consumer
						.poll(Duration.ofMillis(500));
				if (records.count() > 0) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.polled.commands", records.count(), records.iterator().next());
					for (ConsumerRecord<String, TraceCommandDeserializer.TopicTraceCommand> record : records) {
						if (record.value() != null) {
							traceConfig.put(record.value().topic, record.value());
						}
					}
				}
			} catch (WakeupException exc) {
				consume = false;
			}
		}

		consumer.close();
		synchronized (closeLock) {
			closeLock.notifyAll();
		}
	}

	private static KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> createKafkaConsumer(
			Properties props, String cfgTopic) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.creating.command.consumer", cfgTopic, props);
		KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer = new KafkaConsumer<>(props,
				new StringDeserializer(), new TraceCommandDeserializer());
		TopicPartition topicPartition = new TopicPartition(cfgTopic, 0);
		consumer.assign(Collections.singletonList(topicPartition));

		return consumer;
	}

	/**
	 * Parses provided message trace options string {@code traceOptsStr} to {@link java.util.Set} of tracing options: id
	 * trace enabled/disabled, set of intercepted producer/ consumer methods.
	 * 
	 * @param traceOptsStr
	 *            messages trace options string
	 * @return set of messages tracing options (intercepted methods)
	 */
	public static Set<String> getTraceOptsSet(String traceOptsStr) {
		String[] opts = traceOptsStr.split(","); // NON-NLS
		Set<String> optsSet = new HashSet<>(opts.length);

		for (String opt : opts) {
			String o = opt.trim().toLowerCase();
			if ("false".equals(o)) { // NON-NLS
				o = NONE;
			} else if ("true".equals(o)) { // NON-NLS
				o = ALL;
			}
			optsSet.add(o);
		}

		return optsSet;
	}

	/**
	 * Checks if messages tracing options enables tracing.
	 * 
	 * @param traceOptions
	 *            set of messages tracing options
	 * @return {@code true} if set does not contain {@code "none"}, {@code false} - otherwise
	 */
	public static boolean isTraceEnabled(Set<String> traceOptions) {
		boolean disabled = traceOptions.contains(NONE); // NON-NLS

		return !disabled;
	}

	/**
	 * Extracts message tracing specific configuration properties from interceptor configuration.
	 *
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 *
	 * @return interceptor message tracing configuration properties
	 */
	protected static Properties extractKafkaProperties(Properties interceptorProperties) {
		return extractScopeProperties(interceptorProperties, CFG_CONSUMER_PROPERTY_PREFIX);
	}

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
	 * Checks tracing configuration whether message lifecycle event shall be traced by interceptor.
	 *
	 * @param topic
	 *            topic name event received from
	 * @param count
	 *            events counts
	 * @param opName
	 *            intercepted operation name
	 *
	 * @return {@code true} if message should be traced, {@code false} - otherwise
	 */
	protected boolean shouldSendTrace(String topic, boolean count, String opName) {
		if (!isOpTraceEnabled(opName) || cfgTopic.equals(topic)) {
			return false;
		}

		TraceCommandDeserializer.TopicTraceCommand topicTraceConfig = traceConfig.get(topic);
		if (topicTraceConfig == null) {
			topicTraceConfig = traceConfig.get(TraceCommandDeserializer.MASTER_CONFIG);
		}

		boolean send = (topic != null && topicTraceConfig != null) && topicTraceConfig.match(topic, count);
		StackTraceElement callMethodTrace = Utils.getStackFrame(2);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.should.trace", callMethodTrace.getMethodName(), topic, count, topicTraceConfig, send);

		return send;
	}

	private boolean isOpTraceEnabled(String opName) {
		return traceOptions == null || traceOptions.contains(opName) || traceOptions.contains(ALL);
	}

	@Override
	public void send(TNTKafkaPInterceptor interceptor, ProducerRecord<Object, Object> producerRecord,
			ClusterResource clusterResource) {
		if (producerRecord == null) {
			return;
		}
		if (shouldSendTrace(producerRecord.topic(), true, SEND)) {
			try {
				KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(producerRecord, clusterResource,
						MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));

				stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.send.failed", exc);
			}
		}
	}

	@Override
	public void acknowledge(TNTKafkaPInterceptor interceptor, RecordMetadata recordMetadata, Exception e,
			ClusterResource clusterResource) {
		if (recordMetadata == null) {
			return;
		}
		if (shouldSendTrace(recordMetadata.topic(), false, ACK)) {
			try {
				KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(recordMetadata, e, clusterResource,
						MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));
				stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.acknowledge.failed", exc);
				exc.printStackTrace();
			}
		}
	}

	@Override
	public void consume(TNTKafkaCInterceptor interceptor, ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		if (consumerRecords == null) {
			return;
		}
		for (ConsumerRecord<Object, Object> cr : consumerRecords) {
			if (cr == null) {
				continue;
			}
			if (shouldSendTrace(cr.topic(), true, CONSUME)) {
				try {
					KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(cr, clusterResource,
							MapUtils.getString(interceptor.getConfig(), ConsumerConfig.CLIENT_ID_CONFIG));
					stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
				} catch (Exception exc) {
					Utils.logThrowable(LOGGER, OpLevel.ERROR,
							StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.consume.failed", exc);
				}
			}
		}
	}

	@Override
	public void commit(TNTKafkaCInterceptor interceptor, Map<TopicPartition, OffsetAndMetadata> map,
			ClusterResource clusterResource) {
		if (map == null || map.isEmpty()) {
			return;
		}
		for (Map.Entry<TopicPartition, OffsetAndMetadata> me : map.entrySet()) {
			if (me == null) {
				continue;
			}
			if (shouldSendTrace(me.getKey().topic(), false, COMMIT)) {
				try {
					KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(me.getKey(), me.getValue(),
							clusterResource,
							MapUtils.getString(interceptor.getConfig(), ConsumerConfig.CLIENT_ID_CONFIG));
					stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
				} catch (Exception exc) {
					Utils.logThrowable(LOGGER, OpLevel.ERROR,
							StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.commit.failed", exc);
				}
			}
		}
	}

	@Override
	public void shutdown() {
		if (consumer != null) {
			consumer.wakeup();
			synchronized (closeLock) {
				try {
					closeLock.wait();
				} catch (InterruptedException exc) {
				}
			}
		}

		if (stream != null) {
			stream.markEnded();
		}
	}

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();

	/**
	 * Generates a new unique message event signature.
	 *
	 * @param elements
	 *            elements array to calculate signature
	 *
	 * @return unique message event signature
	 */
	public static String calcSignature(Object... elements) {
		synchronized (MSG_DIGEST) {
			return calcSignature(MSG_DIGEST, elements);
		}
	}

	/**
	 * Generates a new unique message event signature.
	 *
	 * @param _msgDigest
	 *            message type
	 * @param elements
	 *            elements array to calculate signature
	 *
	 * @return unique message event signature
	 */
	protected static String calcSignature(MessageDigest _msgDigest, Object... elements) {
		_msgDigest.reset();

		if (elements != null) {
			for (Object element : elements) {
				if (element == null) {
					continue;
				}

				if (element instanceof byte[]) {
					_msgDigest.update((byte[]) element);
				} else if (element instanceof String) {
					_msgDigest.update(((String) element).trim().getBytes());
				} else if (element instanceof Number) {
					if (element instanceof Integer
							|| (element.getClass().isPrimitive() && element.getClass() == Integer.TYPE)) {
						_msgDigest.update(ByteBuffer.allocate(4).putInt(((Number) element).intValue()).array());
					} else if (element instanceof Long
							|| (element.getClass().isPrimitive() && element.getClass() == Long.TYPE)) {
						_msgDigest.update(ByteBuffer.allocate(8).putLong(((Number) element).longValue()).array());
					} else if (element instanceof Double
							|| (element.getClass().isPrimitive() && element.getClass() == Double.TYPE)) {
						_msgDigest.update(ByteBuffer.allocate(8).putDouble(((Number) element).doubleValue()).array());
					} else if (element instanceof Float
							|| (element.getClass().isPrimitive() && element.getClass() == Float.TYPE)) {
						_msgDigest.update(ByteBuffer.allocate(4).putFloat(((Number) element).floatValue()).array());
					} else if (element instanceof Short
							|| (element.getClass().isPrimitive() && element.getClass() == Short.TYPE)) {
						_msgDigest.update(ByteBuffer.allocate(2).putShort(((Number) element).shortValue()).array());
					} else if (element instanceof Byte
							|| (element.getClass().isPrimitive() && element.getClass() == Byte.TYPE)) {
						_msgDigest.update(ByteBuffer.allocate(1).put(((Number) element).byteValue()).array());
					}
				} else if (element instanceof Character
						|| (element.getClass().isPrimitive() && element.getClass() == Character.TYPE)) {
					_msgDigest.update(ByteBuffer.allocate(2).putChar((Character) element).array());
				} else if (element instanceof Boolean
						|| (element.getClass().isPrimitive() && element.getClass() == Boolean.TYPE)) {
					_msgDigest.update(ByteBuffer.allocate(1).put(((Boolean) element) ? (byte) 1 : (byte) 0).array());
				} else if (element.getClass().isEnum()) {
					_msgDigest.update(((Enum<?>) element).name().getBytes());
				} else {
					String elemStr = Utils.toString(element);
					_msgDigest.update(elemStr.trim().getBytes());
				}
			}
		}

		return Utils.base64EncodeStr(_msgDigest.digest());
	}

}
