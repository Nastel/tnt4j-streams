package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.google.common.collect.Ordering;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJavaObjectParser;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.TimestampFormatter;

public class MsgTraceReporterTest {

	public static final String SEND = "Kafka_Producer_Send";
	public static final int PARTITION = 0;
	public static final String TOPIC = "TestTopic";
	public static final String KEY = "Key";
	public static final String MESSAGE = "TestMessage";
	public static final long TIMESTAMP = System.currentTimeMillis();
	public static final long OFFSET = 123;
	public static final long CHECKSUM = -1L;
	public TestActivityInfoConsumer test;
	private Consumer<String, String> consumer;

	@Ignore
	@Test
	public void pollConfigQueue() throws Exception {
		Properties config = new Properties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
		config.put(ConsumerConfig.GROUP_ID_CONFIG, "Test");
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
		config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");

		KafkaMsgTraceStream<ActivityInfo> stream = buildStream();
		MsgTraceReporter reporter = getMsgTraceReporter(stream);

		for (int i = 0; i < 10; i++) {
			HashMap<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig = new HashMap<>();
			reporter.pollConfigQueue(config, MsgTraceReporter.TRACE_CONFIG_TOPIC, traceConfig);
			System.out.println("Control records for " + traceConfig.size());
			TimeUnit.SECONDS.sleep(3);
		}

		reporter.shutdown();
	}

	@Ignore
	@Test
	public void connectToKafkaQueueAndGetTheMessages() {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "testGroup");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "30000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		consumer = new KafkaConsumer<>(props);
		try {
			TopicPartition topicPartition = new TopicPartition(MsgTraceReporter.TRACE_CONFIG_TOPIC, 0);
			consumer.assign(Arrays.asList(topicPartition));

			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
				if (records.count() > 0) {
					System.out.println("Polled " + records.count() + "messages");
				}

				for (ConsumerRecord<String, String> record : records) {
					System.out
							.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value())
							.println();
				}
			}
		} catch (WakeupException exc) {
		} finally {
			consumer.close();
		}
	}

	@Test
	@Ignore("Stop messages consumer")
	public void stopConsuming() {
		if (consumer != null) {
			consumer.wakeup();
		}
	}

	@Test
	public void testKafkaProperties() throws Exception {
		Properties props = null;
		Properties kafkaProperties = new Properties();
		kafkaProperties
				.load(new FileInputStream("..\\tnt4j-streams-kafka\\config\\intercept\\interceptorsC.properties"));

		props = MsgTraceReporter.extractKafkaProperties(kafkaProperties);

		for (String name : props.stringPropertyNames()) {
			System.out.println(name + "\n");
			Set<String> keys = new TreeSet<>(Ordering.usingToString());
			keys.addAll(ConsumerConfig.configNames());

			assertTrue(keys.contains(name));
		}

		System.out.println(props);
	}

	@Test
	public void testSend() throws Exception {
		KafkaMsgTraceStream<ActivityInfo> stream = buildStream();
		MsgTraceReporter reporter = getMsgTraceReporter(stream);

		ProducerRecord producerRecord = getProducerRecord();

		reporter.send(mock(TNTKafkaPInterceptor.class), producerRecord, null);

		test = new TestActivityInfoConsumer() {
			@Override
			public void test(ActivityInfo ai) {
				testSendFields(producerRecord, ai);
			}
		};

		reporter.shutdown();
	}

	@Test
	public void testAck() throws Exception {
		KafkaMsgTraceStream<ActivityInfo> stream = buildStream();
		MsgTraceReporter reporter = getMsgTraceReporter(stream);

		RecordMetadata recordMetadata = new RecordMetadata(getTopicPartition(), OFFSET, OFFSET, TIMESTAMP, 123L,
				KEY.length(), MESSAGE.length());
		Exception e = new Exception("AAA");
		ClusterResource clusterResource = new ClusterResource("CLUSTERID");

		reporter.acknowledge(mock(TNTKafkaPInterceptor.class), recordMetadata, e, clusterResource);

		ActivityInfo activityInfo = stream.getNextItem();

		// assertEquals(activityInfo.getFieldValue((StreamFieldType.TrackingId.name())), MsgTraceReporter
		// .calcSignature(recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));

		test = new TestActivityInfoConsumer() {
			@Override
			public void test(ActivityInfo activityInfo) {
				assertEquals(activityInfo.getFieldValue(StreamFieldType.Exception.name()), e.getMessage());
				assertEquals(activityInfo.getFieldValue("ClusterId"), clusterResource.clusterId()); // NON-NLS
				testRecordMetadataFields(recordMetadata, activityInfo);
			}
		};

		reporter.shutdown();
	}

	private void testSendFields(ProducerRecord producerRecord, ActivityInfo activityInfo) {
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventType.name()), OpType.EVENT);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventName.name()), "Kafka_Producer_Send"); // NON-NLS

		assertEquals(activityInfo.getFieldValue("Partition"), producerRecord.partition()); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Topic"), producerRecord.topic()); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Key"), producerRecord.key()); // NON-NLS
		assertEquals(activityInfo.getFieldValue(StreamFieldType.Message.name()), producerRecord.value());
		assertEquals(activityInfo.getFieldValue(StreamFieldType.StartTime.name()), producerRecord.timestamp());
		assertEquals(activityInfo.getFieldValue(StreamFieldType.ApplName.name()), null);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.ResourceName.name()),
				"QUEUE=" + producerRecord.topic());
	}

	private ProducerRecord getProducerRecord() {
		return new ProducerRecord<String, String>(TOPIC, 0, null, "VALUE"); // NON-NLS
	}

	private KafkaMsgTraceStream<ActivityInfo> buildStream() throws Exception {
		KafkaMsgTraceStream<ActivityInfo> stream = new KafkaMsgTraceStream<ActivityInfo>() {
			{
				setOutput(new JKCloudActivityOutput() {
					@Override
					public void logItem(ActivityInfo ai) throws Exception {
						test.test(ai);
					}
				});
				initialize();
			}
		};
		return stream;
	}

	private MsgTraceReporter getMsgTraceReporter(KafkaMsgTraceStream<ActivityInfo> stream) throws Exception {
		// System.setProperty("tnt4j.config", "../config/tnt4j.properties");

		MsgTraceReporter reporter = new MsgTraceReporter(stream, new Properties(), false, "all"); // NON-NLS

		return reporter;
	}

	private void testRecordMetadataFields(RecordMetadata recordMetadata, ActivityInfo activityInfo) {
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventType.name()), OpType.SEND);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventName.name()), "Kafka_Producer_Acknowledge"); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Offset"), recordMetadata.offset()); // NON-NLS
		assertEquals(activityInfo.getFieldValue(StreamFieldType.StartTime.name()), new UsecTimestamp(TIMESTAMP * 1000));
		assertEquals(activityInfo.getFieldValue("Topic"), recordMetadata.topic()); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Partition"), recordMetadata.partition()); // NON-NLS
		assertEquals(activityInfo.getFieldValue((StreamFieldType.MsgLength.name())), MESSAGE.length() + KEY.length());
		assertEquals(activityInfo.getFieldValue(StreamFieldType.ApplName.name()), null);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.ResourceName.name()), "QUEUE=" + TOPIC); // NON-NLS
	}

	private TopicPartition getTopicPartition() {
		return new TopicPartition(TOPIC, PARTITION);
	}

	@Test
	public void testCommit() throws Exception {
		KafkaMsgTraceStream<ActivityInfo> stream = buildStream();
		MsgTraceReporter reporter = getMsgTraceReporter(stream);

		HashMap<TopicPartition, OffsetAndMetadata> map = new HashMap<>();
		map.put(getTopicPartition(), new OffsetAndMetadata(OFFSET));
		Map.Entry<TopicPartition, OffsetAndMetadata> me = map.entrySet().iterator().next();
		reporter.commit(mock(TNTKafkaCInterceptor.class), map, null);

		ActivityInfo activityInfo = stream.getNextItem();

		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventType.name()), OpType.ACTIVITY);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventName.name()), "Kafka_Consumer_Commit"); // NON-NLS
		assertNotNull(activityInfo.getFieldValue(StreamFieldType.TrackingId.name()));

		test = new TestActivityInfoConsumer() {
			@Override
			public void test(ActivityInfo activityInfo) {
				assertNotNull(activityInfo.getFieldValue(StreamFieldType.ParentId.name()));

				assertEquals(activityInfo.getFieldValue(StreamFieldType.EventType.name()), OpType.EVENT);
				assertEquals(activityInfo.getFieldValue(StreamFieldType.EventName.name()),
						"Kafka_Consumer_Commit_Entry"); // NON-NLS
				assertEquals(activityInfo.getFieldValue("Partition"), me.getKey().partition()); // NON-NLS
				assertEquals(activityInfo.getFieldValue("Topic"), me.getKey().topic()); // NON-NLS
				assertEquals(activityInfo.getFieldValue("Offset"), me.getValue().offset()); // NON-NLS
				assertEquals(activityInfo.getFieldValue("Metadata"), me.getValue().metadata()); // NON-NLS
				assertEquals(activityInfo.getFieldValue(StreamFieldType.ApplName.name()), null);
				assertEquals(activityInfo.getFieldValue(StreamFieldType.ResourceName.name()), "QUEUE=" + TOPIC);
			}
		};

		reporter.shutdown();
	}

	@Test
	public void testConsume() throws Exception {
		KafkaMsgTraceStream<ActivityInfo> stream = buildStream();
		MsgTraceReporter reporter = getMsgTraceReporter(stream);

		ConsumerRecords<Object, Object> consumerRecords = getConsumerRecords();

		reporter.consume(mock(TNTKafkaCInterceptor.class), consumerRecords, null);

		ActivityInfo activityInfo = stream.getNextItem();

		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventType.name()), OpType.ACTIVITY);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventName.name()), "Kafka_Consumer_Consume"); // NON-NLS
		assertNotNull(activityInfo.getFieldValue(StreamFieldType.TrackingId.name()));

		test = new TestActivityInfoConsumer() {
			@Override
			public void test(ActivityInfo activityInfo) {
				assertNotNull(activityInfo.getFieldValue(StreamFieldType.ParentId.name()));
				assertEquals(activityInfo.getFieldValue(StreamFieldType.StartTime.name()),
						TimestampFormatter.getTimestamp(new UsecTimestamp((Number) TimeUnit.MILLISECONDS
								.toMicros(consumerRecords.iterator().next().timestamp()))));
				assertEquals(activityInfo.getFieldValue(StreamFieldType.MsgLength.name()),
						KEY.length() + MESSAGE.length());
				testConsumeFields(consumerRecords, activityInfo);
			}
		};

		reporter.shutdown();
	}

	private void testConsumeFields(ConsumerRecords<Object, Object> consumerRecords, ActivityInfo activityInfo) {

		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventType.name()), OpType.RECEIVE);
		assertEquals(activityInfo.getFieldValue(StreamFieldType.EventName.name()), "Kafka_Consumer_Consume_Record"); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Topic"), consumerRecords.iterator().next().topic()); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Partition"), consumerRecords.iterator().next().partition()); // NON-NLS
		assertEquals(activityInfo.getFieldValue("Offset"), consumerRecords.iterator().next().offset()); // NON-NLS
		// assertEquals(activityInfo.getFieldValue("TimestampType"), consumerRecords.iterator().next().timestampType());
		// // NON-NLS
		assertEquals(activityInfo.getFieldValue("Key"), consumerRecords.iterator().next().key()); // NON-NLS
		assertEquals(activityInfo.getFieldValue(StreamFieldType.Message.name()),
				consumerRecords.iterator().next().value());
		assertEquals(activityInfo.getFieldValue("Checksum"), consumerRecords.iterator().next().checksum()); // NON-NLS

	}

	private ConsumerRecords<Object, Object> getConsumerRecords() {
		ConsumerRecord<Object, Object> cr = new ConsumerRecord<>(TOPIC, PARTITION, OFFSET, TIMESTAMP,
				TimestampType.CREATE_TIME, CHECKSUM, KEY.length(), MESSAGE.length(), KEY, MESSAGE);
		Map<TopicPartition, List<ConsumerRecord<Object, Object>>> map = Collections.singletonMap(getTopicPartition(),
				Collections.singletonList(cr));

		return new ConsumerRecords<>(map);
	}

	@Test
	public void testParseWithJavaObjectParser() throws ParseException {
		ConsumerRecords<Object, Object> consumerRecords = getConsumerRecords();
		ActivityJavaObjectParser objectParser = new ActivityJavaObjectParser();
		ActivityField topic = new ActivityField("Topic");
		topic.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "topic"));
		objectParser.addField(topic);

		ActivityInfo ai = objectParser.parse(new TestUtils.SimpleTestStream(), consumerRecords.iterator().next());

		assertEquals(TOPIC, ai.getFieldValue("Topic"));

	}

	@Test
	public void testConsumeConfig() throws Exception {
		ConsumerRecords<Object, Object> consumerRecords = getConsumerRecords();
		ActivityParser consumerRecordParser = getActivityParser("ConsumerRecordParser");
		ConsumerRecord<Object, Object> record = consumerRecords.iterator().next();
		if (consumerRecordParser.isDataClassSupported(record)) {
			ActivityInfo activityInfo = consumerRecordParser.parse(new TestUtils.SimpleTestStream(), record);
			testConsumeFields(consumerRecords, activityInfo);

		} else {
			fail("Data Class not supported");
		}
	}

	@Test
	public void testSendConfig() throws Exception {
		ProducerRecord producerRecord = getProducerRecord();
		ActivityParser producerActivityParser = getActivityParser("ProducerRecordParser");
		if (producerActivityParser.isDataClassSupported(producerRecord)) {
			ActivityInfo activityInfo = producerActivityParser.parse(new TestUtils.SimpleTestStream(), producerRecord);
			testSendFields(producerRecord, activityInfo);
		} else {
			fail("Data Class nor supported");
		}
	}

	@Test
	public void testRecordMetadata() throws Exception {
		RecordMetadata rm = new RecordMetadata(getTopicPartition(), OFFSET, OFFSET, TIMESTAMP, CHECKSUM, KEY.length(),
				MESSAGE.length());
		ActivityParser producerActivityParser = getActivityParser("RecordMetadataParser");
		if (producerActivityParser.isDataClassSupported(rm)) {
			ActivityInfo activityInfo = producerActivityParser.parse(new TestUtils.SimpleTestStream(), rm);
			testRecordMetadataFields(rm, activityInfo);
		} else {
			fail("Data Class nor supported");
		}
	}

	@Test
	public void testOffsetMetadata() throws Exception {
		OffsetAndMetadata om = new OffsetAndMetadata(OFFSET, "");
		ActivityParser producerActivityParser = getActivityParser("OffsetAndMetadataParser");
		if (producerActivityParser.isDataClassSupported(om)) {
			ActivityInfo activityInfo = producerActivityParser.parse(new TestUtils.SimpleTestStream(), om);
			assertEquals(OFFSET, activityInfo.getFieldValue("Offset"));
			assertEquals("", activityInfo.getFieldValue("Metadata"));
		} else {
			fail("Data Class nor supported");
		}
	}

	@Test
	public void topicPartition() throws Exception {
		TopicPartition tp = new TopicPartition(TOPIC, 0);
		ActivityParser producerActivityParser = getActivityParser("TopicPartitionParser");
		if (producerActivityParser.isDataClassSupported(tp)) {
			ActivityInfo activityInfo = producerActivityParser.parse(new TestUtils.SimpleTestStream(), tp);
			assertEquals(TOPIC, activityInfo.getFieldValue("Topic"));
			assertEquals(PARTITION, activityInfo.getFieldValue("Partition"));
		} else {
			fail("Data Class nor supported");
		}
	}

	private ActivityParser getActivityParser(String parserName)
			throws ParserConfigurationException, SAXException, IOException {

		return MsgTraceReporter.getParser("#" + parserName); // NON-NLS
	}

	public interface TestActivityInfoConsumer {
		void test(ActivityInfo ai);
	}
}