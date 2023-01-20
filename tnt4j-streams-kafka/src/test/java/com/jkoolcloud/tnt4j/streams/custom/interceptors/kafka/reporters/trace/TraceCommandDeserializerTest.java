/*
 * Copyright 2014-2023 JKOOL, LLC.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class TraceCommandDeserializerTest {

	// String[] testCases = { "trace on topic TNT4JStreams", "trace off topic TNT4JStreams",
	// "trace until 2017-11-10 12:54 topic TNT4JStreams",
	// "trace between 2017-11-10 12:54 2017-11-10 12:54 topic TNT4JStreams" };

	private Consumer<String, String> consumer;

	@Test
	public void interpretTest() throws Exception {
		TraceCommandDeserializer.TopicTraceCommand command = TraceCommandDeserializer.interpret("trace 100 messages");
		System.out.println(command);
		assertTrue(command.match("ANY", true));
		assertTrue(command.match("ANY_OTHER", true));
		for (int i = 0; i <= 98; i++) {
			command.match("TNT4JStreams", true);
		}
		assertFalse(command.match("TNT4JStreams", true));

		command = TraceCommandDeserializer.interpret("trace on");
		System.out.println(command);
		assertTrue(command.match("ANY", true));
		assertTrue(command.match("ANY_OTHER", true));
		command = TraceCommandDeserializer.interpret("trace off");
		System.out.println(command);
		assertFalse(command.match("ANY", true));
		assertFalse(command.match("ANY_OTHER", true));
		command = TraceCommandDeserializer.interpret("trace until 2017-11-10 12:54");
		System.out.println(command);
		assertFalse(command.match("ANY", true));
		assertFalse(command.match("ANY_OTHER", true));
		command = TraceCommandDeserializer.interpret("trace until 2018-11-10 12:54");
		System.out.println(command);
		assertTrue(command.match("ANY", true));
		assertTrue(command.match("ANY_OTHER", true));
		command = TraceCommandDeserializer.interpret("trace between 2017-11-10 12:54 2018-11-10 12:54 ");
		System.out.println(command);
		assertTrue(command.match("ANY", true));
		assertTrue(command.match("ANY_OTHER", true));
		command = TraceCommandDeserializer.interpret("trace 100 messages topic TNT4JStreams");
		System.out.println(command);
		assertFalse(command.match("ANY", true));
		assertFalse(command.match("ANY_OTHER", true));
		assertTrue(command.match("TNT4JStreams", true));
		for (int i = 0; i <= 99; i++) {
			command.match("TNT4JStreams", true);
		}
		assertFalse(command.match("TNT4JStreams", true));
	}

	@Test
	@Ignore("In order to test this the environment has to be set-up 'a-priori'")
	public void kafkaReadTopicTest() {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "testGroup");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "30000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("client.id", "kafka-x-ray-test-consumer");

		consumer = new KafkaConsumer<>(props);
		try {
			consumer.subscribe(Collections.singletonList(MsgTraceReporter.TRACE_CONFIG_TOPIC));

			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
				for (ConsumerRecord<String, String> record : records) {
					System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(),
							record.value());
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

}