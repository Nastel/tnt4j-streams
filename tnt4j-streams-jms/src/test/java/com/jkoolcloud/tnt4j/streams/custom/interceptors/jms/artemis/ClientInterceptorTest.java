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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis;

import static org.junit.Assert.assertEquals;

import javax.jms.*;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * @author akausinis
 * @version 1.0
 */
public class ClientInterceptorTest {
	private static EventSink LOGGER = LoggerUtils.getLoggerSink(ClientInterceptorTest.class);

	@Test
	@Ignore("Integration test")
	public void testClientTextInterceptor() throws JMSException {
		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY,
				"./samples/artemis-interceptors/mybroker0/etc/tnt4j/tnt4j.properties");
		System.setProperty("log4j2.configurationFile", "file:../config/log4j2.xml");

		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory( //
				"tcp://localhost:61616?" //
						+ "incomingInterceptorList=" + PacketInterceptor.class.getName() + "&" //
						+ "outgoingInterceptorList=" + PacketInterceptor.class.getName() //
		);
		try (Connection connection = cf.createConnection()) {
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue("InterceptorsTestQueue");
			MessageProducer producer = session.createProducer(queue);

			String msgText = "This is a text test message";
			String pName = "testProperty";
			String pValue = "testValue";

			TextMessage message = session.createTextMessage(msgText);
			message.setStringProperty(pName, pValue);

			String sndMsgText = message.getText();

			LOGGER.log(OpLevel.INFO, "===>> Sending message [" + sndMsgText + "] with String property: "
					+ message.getStringProperty(pName));

			// SessionSendMessage_V2
			producer.send(message);

			MessageConsumer messageConsumer = session.createConsumer(queue);
			connection.start();

			// SessionReceiveMessage
			TextMessage messageReceived = (TextMessage) messageConsumer.receive(5000);
			String rcvMsgText = messageReceived.getText();
			String rcvPValue = messageReceived.getStringProperty(pName);

			LOGGER.log(OpLevel.INFO, "<<=== Received message [" + rcvMsgText + "] with String property: " + rcvPValue);

			assertEquals("Received message text is unexpected", msgText, rcvMsgText);
			assertEquals("Received property value is unexpected", pValue, rcvPValue);
			connection.stop();
		} finally {
			StreamsAgent.waitForStreamsToComplete();
		}
	}

	@Test
	@Ignore("Integration test")
	public void testClientMapInterceptor() throws JMSException {
		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY,
				"./samples/artemis-interceptors/mybroker0/etc/tnt4j/tnt4j.properties");
		System.setProperty("log4j2.configurationFile", "file:../config/log4j2.xml");

		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory( //
				"tcp://localhost:61616?" //
						+ "incomingInterceptorList=" + PacketInterceptor.class.getName() + "&" //
						+ "outgoingInterceptorList=" + PacketInterceptor.class.getName() //
		);
		try (Connection connection = cf.createConnection()) {
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue("InterceptorsTestQueue2");
			MessageProducer producer = session.createProducer(queue);

			String msgText = "This is a text test message";
			String pName = "testProperty";
			String pValue = "testValue";

			MapMessage message = session.createMapMessage();
			message.setString("MsgText", msgText);
			message.setInt("MsgInt", 55645);
			message.setStringProperty(pName, pValue);

			String sndMsgText = message.getString("MsgText");

			LOGGER.log(OpLevel.INFO, "===>> Sending message [" + sndMsgText + "] with String property: "
					+ message.getStringProperty(pName));

			// SessionSendMessage_V2
			producer.send(message);

			MessageConsumer messageConsumer = session.createConsumer(queue);
			connection.start();

			// SessionReceiveMessage
			MapMessage messageReceived = (MapMessage) messageConsumer.receive(5000);
			String rcvMsgText = messageReceived.getString("MsgText");
			int rcvMsgInt = messageReceived.getInt("MsgInt");
			String rcvPValue = messageReceived.getStringProperty(pName);

			LOGGER.log(OpLevel.INFO, "<<=== Received message [" + rcvMsgText + "] with String property: " + rcvPValue);

			assertEquals("Received message text is unexpected", msgText, rcvMsgText);
			assertEquals("Received message int value is unexpected", 55645, rcvMsgInt);
			assertEquals("Received property value is unexpected", pValue, rcvPValue);
			connection.stop();
		} finally {
			StreamsAgent.waitForStreamsToComplete();
		}
	}
}
