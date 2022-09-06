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

package com.jkoolcloud.tnt4j.streams.inputs;

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.mq.MQDestination;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.jkoolcloud.tnt4j.streams.configure.WmqStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * @author akausinis
 * @version 1.0
 */
public class WmqStreamTest {
	WmqStream wmqStream = new WmqStream();

	@Test
	public void propertiesSetTest() {
		Map<String, String> props = new HashMap<>(9);
		props.put(WmqStreamProperties.PROP_QMGR_NAME, "TEST"); // NON-NLS
		props.put(WmqStreamProperties.PROP_QUEUE_NAME, "TEST"); // NON-NLS
		props.put(WmqStreamProperties.PROP_TOPIC_NAME, "TEST"); // NON-NLS
		props.put(WmqStreamProperties.PROP_SUB_NAME, "TEST"); // NON-NLS
		props.put(WmqStreamProperties.PROP_TOPIC_STRING, "TEST"); // NON-NLS
		props.put(WmqStreamProperties.PROP_HOST, "localhost"); // NON-NLS
		props.put(WmqStreamProperties.PROP_PORT, String.valueOf(8080));
		props.put(WmqStreamProperties.PROP_CHANNEL_NAME, "TEST"); // NON-NLS
		props.put(WmqStreamProperties.PROP_STRIP_HEADERS, String.valueOf(false));
		wmqStream.setProperties(props.entrySet());
		testPropertyList(wmqStream, props.entrySet());
	}

	@Test
	public void testInitialize() throws Exception {
		propertiesSetTest();
		wmqStream.startStream();
		assertNotNull("Message GET options is null", wmqStream.gmo);
	}

	@Test(expected = MQException.class)
	public void testConnects() throws Exception {
		propertiesSetTest();
		wmqStream.connectToQmgr();
	}

	@Test()
	public void isConnectedToQmgr() throws Exception {
		MQQueueManager mqqManager = mock(MQQueueManager.class);
		MQException mqe = mock(MQException.class);
		wmqStream.qmgr = mqqManager;

		when(mqqManager.isConnected()).thenReturn(true);
		when(mqe.getCompCode()).thenReturn(MQConstants.MQCC_FAILED);

		when(mqe.getReason()).thenReturn(MQConstants.MQRC_CONNECTION_BROKEN, MQConstants.MQRC_CONNECTION_ERROR,
				MQConstants.MQRC_Q_MGR_NOT_ACTIVE, MQConstants.MQRC_Q_MGR_NOT_AVAILABLE,
				MQConstants.MQRC_Q_MGR_QUIESCING, MQConstants.MQRC_Q_MGR_STOPPING,
				MQConstants.MQRC_CONNECTION_QUIESCING, MQConstants.MQRC_CONNECTION_STOPPING);
		for (int i = 1; i <= 8; i++) {
			assertFalse("Shall not be connected to QM", wmqStream.isConnectedToQmgr(mqe));
		}

		when(mqe.getReason()).thenReturn(MQConstants.MQRC_AIR_ERROR);
		assertTrue("Shall be connected to QM", wmqStream.isConnectedToQmgr(mqe));
	}

	@Test
	public void testCloseQmgrConnection() throws MQException {
		MQQueueManager mqqManager = mock(MQQueueManager.class);
		MQDestination mqDestination = mock(MQDestination.class);

		wmqStream.dest = mqDestination;
		wmqStream.qmgr = mqqManager;

		wmqStream.cleanup();

		verify(mqqManager).disconnect();
		verify(mqDestination).close();
	}

	@Test
	public void testCloseQmgrConnectionException() throws MQException {
		MQQueueManager mqqManager = mock(MQQueueManager.class);
		MQDestination mqDestination = mock(MQDestination.class);

		doThrow(new MQException(1, 1, this)).when(mqqManager).disconnect();
		doThrow(new MQException(1, 1, this)).when(mqDestination).close();

		wmqStream.dest = mqDestination;
		wmqStream.qmgr = mqqManager;

		wmqStream.cleanup();

		// Verify doesn't throw
	}

	@Test
	public void testRB() {
		String keyModule = "WmqStream.stripped.wmq";
		String keyCore = "ActivityField.field.type.name.empty";
		String brbStr;

		String rbs1 = StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Wmq resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Wmq resource bundle entry found in core", keyModule, rbs1);
		brbStr = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, brbStr);
		rbs1 = StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in wmq", brbStr, rbs1);
	}
}
