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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.codahale.metrics.*;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jkoolcloud.tnt4j.config.TrackerConfigStore;

/**
 * @author akausinis
 * @version 1.0
 */
public class InterceptionsManagerTest {

	static {
		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, "./config/intercept/tnt4j_kafka.properties");
		System.setProperty("producer.config", "./config/intercept/producer.properties");
		System.setProperty("consumer.config", "./config/intercept/consumer.properties");
		System.setProperty("interceptors.config", "./config/intercept/interceptorsC.properties");
		System.setProperty("log4j.configuration", "file:../config/log4j.properties");
	}

	@Test
	public void interceptionsTest() throws Exception {
		InterceptorsTest.interceptionsTest();
	}

	@Test
	public void kafkaProducerTest() throws Exception {
		InterceptorsTest.produce();
	}

	@Test
	public void kafkaConsumerTest() throws Exception {
		InterceptorsTest.consume();
	}

	@Test
	public void testMetrics() throws JsonProcessingException {
		MetricRegistry mRegistry = new MetricRegistry();
		Timer messageLatency = mRegistry.timer("messageLatency");
		Counter inputCounter = mRegistry.counter("inputCounter");
		Histogram messageSize = mRegistry.histogram("MessageSize");
		Meter messageSize2 = mRegistry.meter("MessageSize2");

		inputCounter.inc();
		inputCounter.inc();
		inputCounter.inc();
		inputCounter.inc();

		System.out.println(inputCounter.getCount());

		messageLatency.update(5, TimeUnit.MILLISECONDS);
		messageLatency.update(6, TimeUnit.MILLISECONDS);
		messageLatency.update(6, TimeUnit.MILLISECONDS);
		messageLatency.update(6, TimeUnit.MILLISECONDS);
		messageLatency.update(6, TimeUnit.MILLISECONDS);
		messageLatency.update(7, TimeUnit.MILLISECONDS);

		messageSize.update(10);
		messageSize.update(20);
		messageSize.update(30);

		messageSize2.mark(10);
		messageSize2.mark(20);
		messageSize2.mark(30);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new MetricsModule(TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS, false));
		System.out.println(mapper.writeValueAsString(mRegistry));
	}
}
