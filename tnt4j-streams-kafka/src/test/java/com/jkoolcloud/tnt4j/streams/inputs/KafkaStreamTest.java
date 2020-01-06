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

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.Duration;
import java.util.*;

import org.I0Itec.zkclient.exception.ZkTimeoutException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Ignore;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class KafkaStreamTest {
	private static final String DEFAULT_TEST_TOPIC = "TNT4JKafkaTestTopic"; // NON-NLS

	KafkaStream input;

	@Test
	public void testProperties() {
		input = new KafkaStream();

		Map<String, String> props = new HashMap<>(3);
		props.put(StreamProperties.PROP_TOPIC_NAME, DEFAULT_TEST_TOPIC);
		props.put("zookeeper.connect", "127.0.0.1:2181");// NON-NLS
		props.put("group.id", "13"); // NON-NLS
		input.setProperties(props.entrySet());
		testPropertyList(input, props.entrySet());
	}

	@Test(expected = ZkTimeoutException.class)
	public void testInitialize() throws Exception {
		testProperties();
		input.startStream();
	}

	@Test
	public void testRB() {
		String keyModule = "KafkaStream.empty.messages.buffer";
		String keyCore = "ActivityField.field.type.name.empty";
		String brbStr;

		String rbs1 = StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Kafka resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Kafka resource bundle entry found in core", keyModule, rbs1);
		brbStr = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, brbStr);
		rbs1 = StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in kafka", brbStr, rbs1);
	}

	@Ignore("Used to run when testing server")
	@Test
	public void produceMessages() throws InterruptedException {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // NON-NLS
		props.put("client.id", "TestProducer"); // NON-NLS
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer"); // NON-NLS
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer"); // NON-NLS
		KafkaProducer<String, String> producer = new KafkaProducer<>(props);

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i <= 150; i++) {
					System.out.println("Sending message: " + i); // NON-NLS
					String messageStr = "0:0:0:0:0:0:0:1 - - [09/Sep/2016:15:18:34 +0300] \"GET /SimpleJSF/Index.xhtml " // NON-NLS
							+ i + " HTTP/1.1\" 200 6561"; // NON-NLS
					long startTime = System.currentTimeMillis();
					producer.send(new ProducerRecord<>(DEFAULT_TEST_TOPIC, i + "_" + startTime, messageStr));
				}
			}
		});
		thread.start();
		thread.join();
		producer.close();
	}

	@Ignore("Used to run when testing server")
	@Test
	public void consumeMessages() {
		Properties props = new Properties();

		props.put("zookeeper.connect", "localhost:2181"); // NON-NLS
		props.put("group.id", "13"); // NON-NLS
		props.put("zookeeper.session.timeout.ms", "4000"); // NON-NLS
		props.put("zookeeper.sync.time.ms", "200"); // NON-NLS
		props.put("auto.commit.interval.ms", "1000"); // NON-NLS
		props.put("consumer.timeout.ms", "1000"); // NON-NLS

		Collection<String> topics = new ArrayList<>();
		topics.add(DEFAULT_TEST_TOPIC);

		KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(topics);

		ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(Duration.ofSeconds(1));
		for (ConsumerRecord<byte[], byte[]> cRec : consumerRecords) {
			System.out.println(new String(cRec.value()));
		}

		System.err.println();
		consumer.wakeup();
		consumer.unsubscribe();
		consumer.close();
	}
}
