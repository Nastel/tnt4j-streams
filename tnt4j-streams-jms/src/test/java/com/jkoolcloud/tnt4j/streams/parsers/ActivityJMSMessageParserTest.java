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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.StringTokenizer;

import javax.jms.*;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.solacesystems.jms.message.SolTextMessage;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJMSMessageParserTest extends ActivityParserTestBase<ActivityJMSMessageParser> {

	@Override
	@Before
	public void prepare() {
		parser = new ActivityJMSMessageParser();
	}

	@Test
	@Override
	public void isDataClassSupportedTest() {
		assertTrue("javax.jms.Message.class shall be supported ", parser.isDataClassSupported(mock(Message.class)));
		assertFalse("ActivityJMSMessageParser does not support Strings", parser.isDataClassSupported(String.class));
	}

	@Override
	public void setPropertiesTest() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	public void getDataMapTest() throws JMSException {
		TextMessage message = mock(TextMessage.class);
		String string = "TEST"; // NON-NLS
		when(message.getText()).thenReturn(string);
		parser.getDataMap(message);

		BytesMessage messageB = mock(BytesMessage.class);
		parser.getDataMap(messageB);
		verify(messageB).readBytes(any(byte[].class));

		MapMessage messageM = mock(MapMessage.class);
		StringTokenizer tokenizer = new StringTokenizer("TEST,TEST,TEST", ","); // NON-NLS
		when(messageM.getMapNames()).thenReturn(tokenizer);
		parser.getDataMap(messageM);
		verify(messageM, times(3)).getObject(anyString());

		StreamMessage messageS = mock(StreamMessage.class);
		parser.getDataMap(messageS);
		verify(messageS).readBytes(any(byte[].class));

		ObjectMessage messageO = mock(ObjectMessage.class);
		parser.getDataMap(messageO);
		verify(messageO).getObject();

	}

	@Test
	public void testRB() {
		String keyModule = "ActivityJMSMessageParser.payload.data.error";
		String keyCore = "ActivityField.field.type.name.empty";
		String brbStr;

		String rbs1 = StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("JMS resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("JMS resource bundle entry found in core", keyModule, rbs1);
		brbStr = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, brbStr);
		rbs1 = StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in jms", brbStr, rbs1);
	}

	@Test
	public void testSolaceTextMessage() throws Exception {
		TextMessage message = new SolTextMessage();

		message.setJMSDeliveryMode(1);
		message.setJMSDestination(new Queue() {
			@Override
			public String getQueueName() throws JMSException {
				return "VMSTQ.TEST.REST.VENOM.DEV.00";
			}
		});
		// ActivityField field1 = new ActivityField("ALL");
		ActivityField field2 = new ActivityField("DESTINATION");
		ActivityField field3 = new ActivityField("ALL_LEFT");
		// field1.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "*"));
		field2.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "MsgMetadata.Destination"));
		field3.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "#"));
		// parser.addField(field1);
		parser.addField(field2);
		parser.addField(field3);
		ActivityInfo ai = parser.parse(new TestUtils.SimpleTestStream(), message);
		assertNull(ai.getFieldValue("MsgMetadata.Destination"));
	}

}
