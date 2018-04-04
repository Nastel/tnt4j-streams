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

import java.lang.IllegalStateException;
import java.util.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.JMSStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a JMS message transported activity stream, where each JMS message payload data carried data is assumed to
 * represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support JMS {@link Message} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>java.naming.provider.url - JMS server URL. (Required)</li>
 * <li>Queue - queue destination name. (Required - just one of 'Queue' or 'Topic')</li>
 * <li>Topic - topic destination name. (Required - just one of 'Queue' or 'Topic')</li>
 * <li>java.naming.factory.initial - JNDI context factory name. (Required)</li>
 * <li>JMSConnFactory - JMS connection factory name. (Required)</li>
 * <li>list of JNDI context configuration properties supported by JMS server implementation. See
 * {@link javax.naming.Context} for more details. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JMSStream extends AbstractBufferedStream<Message> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JMSStream.class);
	private static final String DEFINITION_DELIMITER = ",";// NON-NLS

	// Stream properties
	private String[] queueNames = null;
	private String[] topicNames = null;
	private String jmsConnFactory = null;
	// JMS Context properties
	private Properties ctxProps = new Properties();

	private List<JMSDataReceiver> jmsDataReceivers = new ArrayList<>();

	/**
	 * Constructs an empty JMSStream. Requires configuration settings to set input stream source.
	 */
	public JMSStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return StringUtils.join(queueNames, DEFINITION_DELIMITER);
		}
		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return StringUtils.join(topicNames, DEFINITION_DELIMITER);
		}
		if (JMSStreamProperties.PROP_JMS_CONN_FACTORY.equalsIgnoreCase(name)) {
			return jmsConnFactory;
		}
		String cpv = ctxProps.getProperty(name);
		if (cpv != null) {
			return cpv;
		}

		return super.getProperty(name);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();
				if (StreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
					queueNames = value.split(DEFINITION_DELIMITER);
				} else if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
					topicNames = value.split(DEFINITION_DELIMITER);
				} else if (JMSStreamProperties.PROP_JMS_CONN_FACTORY.equalsIgnoreCase(name)) {
					jmsConnFactory = value;
				} else {
					ctxProps.put(name, value);
				}
			}
		}
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(ctxProps.getProperty(Context.PROVIDER_URL))) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", Context.PROVIDER_URL));
		}

		if (ArrayUtils.isEmpty(queueNames) && ArrayUtils.isEmpty(topicNames)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined.one.of", StreamProperties.PROP_QUEUE_NAME,
					StreamProperties.PROP_TOPIC_NAME));
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		Context ic = new InitialContext(ctxProps);
		JMSDataReceiver jmsDataReceiver = new JMSDataReceiver();

		if (queueNames != null) {
			for (String queueName : queueNames) {
				jmsDataReceivers.add(jmsDataReceiver);
				jmsDataReceiver.initialize(ic, queueName, jmsConnFactory);
			}
		}

		if (topicNames != null) {
			for (String topicName : topicNames) {
				jmsDataReceivers.add(jmsDataReceiver);
				jmsDataReceiver.initialize(ic, topicName, jmsConnFactory);
			}
		}
	}

	@Override
	protected void start() throws Exception {
		super.start();

		for (JMSDataReceiver jmsDataReceiver : jmsDataReceivers) {
			jmsDataReceiver.start();
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		for (JMSDataReceiver jmsDataReceiver : jmsDataReceivers) {
			jmsDataReceiver.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		boolean flag = true;

		for (JMSDataReceiver jmsDataReceiver : jmsDataReceivers) {
			if (!jmsDataReceiver.isInputEnded()) {
				flag = false;
			}
		}

		return flag;
	}

	@Override
	protected long getActivityItemByteSize(Message itemMsg) {
		try {
			if (itemMsg instanceof BytesMessage) {
				return ((BytesMessage) itemMsg).getBodyLength();
			} else if (itemMsg instanceof MapMessage) {

			} else if (itemMsg instanceof ObjectMessage) {

			} else if (itemMsg instanceof StreamMessage) {

			} else if (itemMsg instanceof TextMessage) {
				String text = ((TextMessage) itemMsg).getText();
				return text == null ? 0 : text.length();
			}
		} catch (JMSException exc) {
		}

		return 0; // TODO
	}

	private class JMSDataReceiver extends InputProcessor implements MessageListener {

		private ConnectionFactory jmsConFactory;
		private Connection jmsCon;
		private Session jmsSession;
		private MessageConsumer jmsReceiver;
		private Destination destination;

		private JMSDataReceiver() {
			super("JMSStream.JMSDataReceiver"); // NON-NLS
		}

		/**
		 * Input data receiver initialization - JMS client configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize data receiver and configure JMS client
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			Context ctx = (Context) params[0];
			String destinationName = (String) params[1];
			String jmsConnFactoryName = (String) params[2];

			jmsConFactory = (ConnectionFactory) ctx.lookup(jmsConnFactoryName);
			jmsCon = jmsConFactory.createConnection();
			jmsSession = jmsCon.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = (Destination) ctx.lookup(destinationName);
			jmsReceiver = jmsSession.createConsumer(destination);
			jmsReceiver.setMessageListener(this);
		}

		/**
		 * Starts JMS client to receive incoming data. Shuts down this data receiver if exception occurs.
		 */
		@Override
		public void run() {
			if (jmsCon != null) {
				try {
					jmsCon.start();
				} catch (JMSException exc) {
					Utils.logThrowable(logger(), OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"AbstractBufferedStream.input.start.failed", exc);
					shutdown();
				}
			}
		}

		/**
		 * Closes JMS client objects.
		 *
		 * @throws JMSException
		 *             if JMS fails to close objects due to internal error
		 */
		@Override
		void closeInternals() throws JMSException {
			if (jmsReceiver != null) {
				jmsReceiver.close();
			}
			if (jmsSession != null) {
				jmsSession.close();
			}
			if (jmsCon != null) {
				jmsCon.close();
			}
		}

		/**
		 * Adds received JMS message to input buffer destination.
		 *
		 * @param msg
		 *            received JMS message
		 */
		@Override
		public void onMessage(Message msg) {
			if (msg == null) {
				return;
			}

			addInputToBuffer(msg);
		}
	}
}
