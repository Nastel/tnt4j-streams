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
package com.jkoolcloud.tnt4j.streams.inputs;

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.MqttStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class MqttStreamTest {
	MqttStream input;

	@Test
	public void testProperties() {
		input = new MqttStream();
		input.setName("MQTTTestStream"); // NON-NLS
		Map<String, String> props = new HashMap<>(7);
		props.put(StreamProperties.PROP_SERVER_URI, "tcp://localhost:1883"); // NON-NLS
		props.put(StreamProperties.PROP_USERNAME, ""); // NON-NLS
		props.put(StreamProperties.PROP_PASSWORD, ""); // NON-NLS
		props.put(StreamProperties.PROP_TOPIC_STRING, "TEST"); // NON-NLS
		props.put(StreamProperties.PROP_USE_SSL, String.valueOf(false));
		props.put(StreamProperties.PROP_KEYSTORE, ""); // NON-NLS
		props.put(StreamProperties.PROP_KEYSTORE_PASS, ""); // NON-NLS
		input.setProperties(props.entrySet());
		testPropertyList(input, props.entrySet());
	}

	@Test
	public void testInitialize() throws Exception {
		testProperties();
		input.startStream();
		TimeUnit.SECONDS.sleep(3);
		assertTrue("MQTT stream input has to be ended", input.isInputEnded());
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeFailDueToNotAllPropertiesSet() throws Exception {
		input = new MqttStream();
		input.startStream();
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeFailDueToNotAllPropertiesSet2() throws Exception {
		input = new MqttStream();
		Map<String, String> props = new HashMap<>(1);
		props.put(StreamProperties.PROP_SERVER_URI, "tcp://localhost:1883"); // NON-NLS
		input.setProperties(props.entrySet());
		input.startStream();
	}

	@Test
	public void testRB() {
		String keyModule = "MqttStream.error.closing.receiver";
		String keyCore = "ActivityField.field.type.name.empty";
		String brbStr;

		String rbs1 = StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Mqtt resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Mqtt resource bundle entry found in core", keyModule, rbs1);
		brbStr = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, brbStr);
		rbs1 = StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in mqtt", brbStr, rbs1);
	}
}
