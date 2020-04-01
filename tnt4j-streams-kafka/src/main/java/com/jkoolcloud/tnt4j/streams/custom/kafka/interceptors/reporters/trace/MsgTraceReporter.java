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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
import org.apache.kafka.common.serialization.StringDeserializer;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.build.POJOStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.sax.ConfigParserHandler;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptionsManager;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener;
import com.jkoolcloud.tnt4j.streams.inputs.StreamStatus;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStreamStatistics;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.uuid.DefaultUUIDFactory;

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
 * @version $Revision: 1 $
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

	/**
	 * Constant defining tracing configuration dedicated topic name.
	 */
	public static final String TNT_TRACE_CONFIG_TOPIC = "TNT_TRACE_CONFIG_TOPIC"; // NON-NLS
	/**
	 * Constant defining tracing configuration dedicated topic polling interval in seconds.
	 */
	public static final int POOL_TIME_SECONDS = 3;
	/**
	 * Constant defining interceptor message tracing configuration properties prefix.
	 */
	public static final String TRACER_PROPERTY_PREFIX = "messages.tracer.kafka."; // NON-NLS
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

	private KafkaObjTraceStream<ActivityInfo> stream;
	private final Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig = new HashMap<>();
	private Timer pollTimer;
	private Set<String> traceOptions;

	private static KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer;

	/**
	 * Constructs a new MsgTraceReporter.
	 * 
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param traceOpts
	 *            messages tracing options set
	 */
	public MsgTraceReporter(Properties interceptorProperties, Set<String> traceOpts) {
		this(new KafkaObjTraceStream<>(), interceptorProperties, true, traceOpts);
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
	MsgTraceReporter(KafkaObjTraceStream<ActivityInfo> stream, Properties interceptorProperties,
			boolean enableCfgPolling, String traceOpts) {
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
	MsgTraceReporter(KafkaObjTraceStream<ActivityInfo> stream, Properties interceptorProperties,
			boolean enableCfgPolling, Set<String> traceOpts) {
		this.stream = stream;
		this.traceOptions = traceOpts;

		String streamName = interceptorProperties.getProperty("messages.tracer.stream.name");
		if (StringUtils.isEmpty(streamName)) {
			streamName = interceptorProperties.getProperty("messages.tracer.kafka.client.id");
		}

		stream.setName(StringUtils.isEmpty(streamName) ? "kafka-x-ray-trace-stream_" + System.currentTimeMillis() // NON-NLS
				: streamName); // NON-NLS

		String parserCfg = Utils.getString("messages.tracer.stream.parser", interceptorProperties,
				DEFAULT_PARSER_CONFIG_FILE + PARSER_DELIM + DEFAULT_PARSER_NAME);
		mainParser = getParser(parserCfg);
		stream.addParser(mainParser);

		Object x = new Object();
		InputStreamListener y = new InputStreamListener() {
			@Override
			public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
			}

			@Override
			public void onSuccess(TNTInputStream<?, ?> stream) {
			}

			@Override
			public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
			}

			@Override
			public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
				if (status.ordinal() >= StreamStatus.STARTED.ordinal()) {
					try {
						synchronized (x) {
							x.notify();
						}
					} catch (Throwable t) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
								"MsgTraceReporter.stream.start.notify.interrupted", stream.getName(), t);
					}
				}
			}

			@Override
			public void onFinish(TNTInputStream<?, ?> stream, TNTInputStreamStatistics stats) {
			}

			@Override
			public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
			}
		};
		stream.addStreamListener(y);
		StreamsAgent.runFromAPI(new POJOStreamsBuilder().addStream(stream));
		try {
			synchronized (x) {
				x.wait();
			}
		} catch (Throwable t) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.stream.start.wait.interrupted", stream.getName(), t);
		}
		stream.removeStreamListener(y);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.stream.started", stream.getName());

		if (enableCfgPolling) {
			TimerTask mrt = new TimerTask() {
				@Override
				public void run() {
					Map<String, Map<String, ?>> consumersCfg = InterceptionsManager.getInstance()
							.getInterceptorsConfig(TNTKafkaCInterceptor.class);
					Map<String, ?> cConfig = MapUtils.isEmpty(consumersCfg) ? null
							: consumersCfg.entrySet().iterator().next().getValue();
					pollConfigQueue(cConfig, interceptorProperties, traceConfig);
				}
			};
			traceConfig.put(TraceCommandDeserializer.MASTER_CONFIG, new TraceCommandDeserializer.TopicTraceCommand());
			long period = TimeUnit.SECONDS.toMillis(POOL_TIME_SECONDS);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.schedule.commands.polling", TNT_TRACE_CONFIG_TOPIC, period, period);
			pollTimer = new Timer("KafkaInterceptorTracesPollTimer", true); // NON-NLS
			pollTimer.scheduleAtFixedRate(mrt, period, period);
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
	 * @param config
	 *            Kafka consumer interceptor configuration properties map
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param traceConfig
	 *            complete message tracing configuration properties
	 */
	protected static void pollConfigQueue(Map<String, ?> config, Properties interceptorProperties,
			Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig) {
		Properties props = new Properties();
		if (config != null) {
			props.putAll(config);
		}
		if (interceptorProperties != null) {
			props.putAll(extractKafkaProperties(interceptorProperties));
		}
		if (!props.isEmpty()) {
			props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-x-ray-message-trace-reporter-config-listener"); // NON-NLS
			props.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
			KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer = getKafkaConsumer(props);
			while (true) {
				ConsumerRecords<String, TraceCommandDeserializer.TopicTraceCommand> records = consumer
						.poll(Duration.ofMillis(100));
				if (records.count() > 0) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.polled.commands", records.count(), records.iterator().next());
					for (ConsumerRecord<String, TraceCommandDeserializer.TopicTraceCommand> record : records) {
						if (record.value() != null) {
							traceConfig.put(record.value().topic, record.value());
						}
					}
					break;
				}
			}
		}
	}

	private static KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> getKafkaConsumer(
			Properties props) {
		if (consumer != null) {
			return consumer;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.creating.command.consumer", props);
		consumer = new KafkaConsumer<>(props, new StringDeserializer(), new TraceCommandDeserializer());
		TopicPartition topic = new TopicPartition(MsgTraceReporter.TNT_TRACE_CONFIG_TOPIC, 0);
		consumer.assign(Collections.singletonList(topic));
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
		Properties props = new Properties();
		for (String key : interceptorProperties.stringPropertyNames()) {
			if (key.startsWith(TRACER_PROPERTY_PREFIX)) {
				props.put(key.substring(TRACER_PROPERTY_PREFIX.length()), interceptorProperties.getProperty(key));
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
		if (!isOpTraceEnabled(opName) || TNT_TRACE_CONFIG_TOPIC.equals(topic)) {
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
	public void send(TNTKafkaPInterceptor interceptor, ProducerRecord<Object, Object> producerRecord) {
		if (producerRecord == null) {
			return;
		}
		if (shouldSendTrace(producerRecord.topic(), true, SEND)) {
			try {
				KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(producerRecord,
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

	/**
	 * Appends resource related fields to provided activity info instance {@code ai}.
	 *
	 * @param ai
	 *            activity info instance
	 * @param topic
	 *            topic name
	 * @param appName
	 *            application name
	 *
	 * @throws ParseException
	 *             if fails to set activity info value
	 */
	protected static void appendResourceFields(ActivityInfo ai, String topic, String appName) throws ParseException {
		ai.setFieldValue(new ActivityField(StreamFieldType.ResourceName.name()), "QUEUE=" + topic); // NON-NLS
		ai.setFieldValue(new ActivityField(StreamFieldType.ApplName.name()), appName);
	}

	@Override
	public void consume(TNTKafkaCInterceptor interceptor, ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		if (consumerRecords == null) {
			return;
		}
		String tid = null;
		ActivityInfo ai = null;
		try {
			ai = new ActivityInfo(true);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.ACTIVITY);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Consumer_Consume"); // NON-NLS
			ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
					DefaultUUIDFactory.getInstance().newUUID());
			stream.addInputToBuffer(ai);

			tid = ai.getTrackingId();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.consume.failed", exc);
		}
		for (ConsumerRecord<Object, Object> cr : consumerRecords) {
			if (cr == null) {
				continue;
			}
			if (shouldSendTrace(cr.topic(), true, CONSUME)) {
				try {
					KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(cr,
							MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));
					kafkaTraceData.setParentId(tid);
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
	public void commit(TNTKafkaCInterceptor interceptor, Map<TopicPartition, OffsetAndMetadata> map) {
		if (map == null || map.isEmpty()) {
			return;
		}
		String tid = null;
		ActivityInfo ai;
		try {
			ai = new ActivityInfo(true);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.ACTIVITY);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Consumer_Commit"); // NON-NLS
			ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
					DefaultUUIDFactory.getInstance().newUUID());
			stream.addInputToBuffer(ai);

			tid = ai.getTrackingId();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.commit.failed", exc);
		}
		for (Map.Entry<TopicPartition, OffsetAndMetadata> me : map.entrySet()) {
			if (me == null) {
				continue;
			}
			if (shouldSendTrace(me.getKey().topic(), false, COMMIT)) {
				try {
					KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(me.getKey(), me.getValue(),
							MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));
					kafkaTraceData.setParentId(tid);
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
		if (stream != null) {
			stream.markEnded();
		}

		if (pollTimer != null) {
			pollTimer.cancel();
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
