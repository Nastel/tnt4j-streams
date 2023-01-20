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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.Ignore;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.JMSStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.solacesystems.jms.SolJmsUtility;
import com.solacesystems.jms.SupportedProperty;

/**
 * @author akausinis
 * @version 1.0
 */
public class JMSStreamTest {
	JMSStream input;

	@Test
	public void testProperties() {
		input = new JMSStream();
		Map<String, String> props = new HashMap<>(4);
		props.put(StreamProperties.PROP_SERVER_URI, "localhost"); // NON-NLS
		props.put(StreamProperties.PROP_QUEUE_NAME, "test"); // NON-NLS
		props.put(StreamProperties.PROP_JNDI_FACTORY, "JNDI"); // NON-NLS
		props.put(JMSStreamProperties.PROP_JMS_CONN_FACTORY, "JMS"); // NON-NLS
		input.setProperties(props.entrySet());
		testPropertyList(input, props.entrySet());
	}

	// @Test
	// public void testInitialize() throws Exception {
	// testProperties();
	// input.startStream();
	// assertTrue(input.jmsDataReceiver.isAlive());
	// }

	@Test
	@Ignore("integration test")
	public void testSolaceCreate() throws Exception {
		String[] args = { "smf://mr-91b692dvft.messaging.solace.cloud:21248", "solace-cloud-client@msgvpn-91b693373t",
				"28ct4kn3vt44knm8nf0soghbg0" };

		String CONNECTION_FACTORY_JNDI_NAME = "/jms/cf/another";

		String[] split = args[1].split("@");

		String host = args[0];
		String vpnName = split[1];
		String username = split[0];
		String password = args[2];

		Hashtable<String, Object> env = new Hashtable<>();
		env.put(InitialContext.INITIAL_CONTEXT_FACTORY, "com.solacesystems.jndi.SolJNDIInitialContextFactory");
		env.put(InitialContext.PROVIDER_URL, host);
		env.put(Context.SECURITY_PRINCIPAL, username + '@' + vpnName);
		env.put(Context.SECURITY_CREDENTIALS, password);

		InitialContext initialContext = new InitialContext(env);
		ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(CONNECTION_FACTORY_JNDI_NAME);

		Connection connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, SupportedProperty.SOL_CLIENT_ACKNOWLEDGE);

		String TOPIC_NAME = "Marius";

		TextMessage message = session.createTextMessage("Hello world!");

		Destination q1 = (Destination) initialContext.lookup("JMS\\T1");
		Destination q2 = (Destination) initialContext.lookup("JMS\\T2");
		MessageProducer producer = session.createProducer(q1);

		CountDownLatch latch = new CountDownLatch(1);
		MessageConsumer messageConsumer = session.createConsumer(q1);

		messageConsumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					if (message instanceof TextMessage) {
						System.out.printf("TextMessage received: '%s'%n", ((TextMessage) message).getText());
					} else {
						System.out.println("Message received.");
					}
					System.out.printf("Message Content:%n%s%n", SolJmsUtility.dumpMessage(message));
					latch.countDown(); // unblock the main thread
				} catch (JMSException ex) {
					System.out.println("Error processing incoming message.");
					ex.printStackTrace();
				}
			}
		});

		connection.start();

		producer.send(q1, message, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
		producer.send(q2, message, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);

		latch.await();
	}
}
