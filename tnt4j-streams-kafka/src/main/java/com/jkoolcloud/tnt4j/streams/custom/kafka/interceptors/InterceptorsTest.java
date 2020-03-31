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

import java.io.FileReader;
import java.net.URL;
import java.time.Duration;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * TNT4J-Streams Kafka interceptors test which can be run as standalone application.
 *
 * @version $Revision: 1 $
 */
public class InterceptorsTest {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(InterceptorsTest.class);

	private static String[] eventsPayload = {
			"# Licensed to the Apache Software Foundation (ASF) under one or more          ", // NON-NLS
			"# contributor license agreements.  See the NOTICE file distributed with       ", // NON-NLS
			"# this work for additional information regarding copyright ownership.         ", // NON-NLS
			"# The ASF licenses this file to You under the Apache License, Version 2.0     ", // NON-NLS
			"# (the \"License\"); you may not use this file except in compliance with      ", // NON-NLS
			"# the License.  You may obtain a copy of the License at                       ", // NON-NLS
			"#                                                                             ", // NON-NLS
			"#    http://www.apache.org/licenses/LICENSE-2.0                               ", // NON-NLS
			"#                                                                             ", "" }; // NON-NLS
	private static int eventsToProduce = eventsPayload.length - 1;

	private static String topicName = "tnt4j_streams_kafka_intercept_test_page_visits"; // NON-NLS

	/**
	 * The entry point of standalone application.
	 *
	 * @param args
	 *            test application arguments
	 */
	public static void main(String... args) {
		try {
			interceptionsTest();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptorsTest.interceptionsTest failed to complete: {0}", exc); // NON-NLS
		}
	}

	/**
	 * Runs interceptions test scenario.
	 *
	 * @throws Exception
	 *             if exception occurs while running interceptions test
	 */
	public static void interceptionsTest() throws Exception {
		String tnt4jCfgPath = System.getProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY);
		if (StringUtils.isEmpty(tnt4jCfgPath)) {
			URL defaultCfg = InterceptionsManager.getDefaultTrackerConfiguration();
			System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, defaultCfg.toExternalForm());
		}

		Consumer<String, String> consumer = initConsumer();

		Thread pt = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					produce();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}, "InterceptProducerThread");

		Thread ct = new Thread(new Runnable() {
			@Override
			public void run() {
				consume(consumer);
			}
		}, "InterceptConsumerThread");
		ct.start();

		pt.start();
		pt.join();
		consumer.wakeup();
		ct.join();
	}

	/**
	 * Consumes Kafka topic messages.
	 *
	 * @throws Exception
	 *             if exception occurs while initializing producer or sending messages
	 */
	public static void produce() throws Exception {
		Producer<String, String> producer = initProducer();
		produce(producer, topicName, eventsToProduce);
	}

	/**
	 * Consumes Kafka topic contained messages.
	 *
	 * @throws Exception
	 *             if exception occurs while initializing consumer or consuming messages
	 */
	public static void consume() throws Exception {
		Consumer<String, String> consumer = initConsumer();
		consume(consumer);
	}

	private static Producer<String, String> initProducer() throws Exception {
		Properties props = new Properties();
		props.load(new FileReader(System.getProperty("producer.config")));// NON-NLS

		eventsToProduce = Utils.getInt("events.count", props, 10);
		props.remove("events.count");
		topicName = props.getProperty("test.app.topic.name", topicName); // NON-NLS
		props.remove("test.app.topic.name");

		Producer<String, String> producer = new KafkaProducer<>(props);

		return producer;
	}

	private static void produce(Producer<String, String> producer, String topic, int eventCount) {
		Random rnd = new Random();

		for (int ei = 0; ei < eventCount; ei++) {
			long runtime = System.currentTimeMillis();
			String ip = "192.168.2." + rnd.nextInt(255); // NON-NLS
			String msg = runtime + ",www.example.com," + ip + "," + (ei + 1) + eventsPayload[ei / eventsPayload.length];// NON-NLS
			ProducerRecord<String, String> data = new ProducerRecord<>(topic, ip, msg);
			producer.send(data);
			LOGGER.log(OpLevel.INFO, "Producing Kafka message: seqNumber={2} msg={1}", producer.hashCode(), // NON-NLS
					data, ei);
			if (ei % 5 == 0) {
				try {
					Thread.sleep((long) (1000 + 2000 * Math.random()));
				} catch (InterruptedException exc) {
				}
			}
		}

		producer.close();
	}

	private static Consumer<String, String> initConsumer() throws Exception {
		Properties props = new Properties();
		props.load(new FileReader(System.getProperty("consumer.config"))); // NON-NLS
		topicName = props.getProperty("test.app.topic.name", topicName); // NON-NLS
		props.remove("test.app.topic.name");

		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList(topicName));

		return consumer;
	}

	private static void consume(Consumer<String, String> consumer) {
		boolean halt = false;
		int ei = 0;
		Map<String, Object> data = new HashMap<>(3);
		while (!halt) {
			try {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
				for (ConsumerRecord<String, String> record : records) {
					data.clear();
					data.put("partition", record.partition()); // NON-NLS
					data.put("offset", record.offset()); // NON-NLS
					data.put("value", record.value()); // NON-NLS
					LOGGER.log(OpLevel.INFO, "Consuming Kafka message: seqNumber={2} msg={1}", consumer.hashCode(), // NON-NLS
							data, ei++);

					try {
						Thread.sleep((long) (200 * Math.random()));
					} catch (InterruptedException exc) {
					}
				}
			} catch (WakeupException exc) {
				halt = true;
			}
		}

		consumer.close();
	}
}
