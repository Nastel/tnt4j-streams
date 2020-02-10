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

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements a MQTT topics transmitted activity stream, where each message body is assumed to represent a single
 * activity or event which should be recorded. Topic to listen is defined using "Topic" property in stream
 * configuration.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On message reception message data is packed
 * into {@link Map} filling these entries:
 * <ul>
 * <li>ActivityTopic - topic name message with activity data was received.</li>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from message.</li>
 * <li>ActivityTransport - activity transport definition: 'Mqtt'.</li>
 * <li>MessageId - message identifier number.</li>
 * <li>Qos - message Quality of Service numeric value.</li>
 * <li>Duplicate - message is duplicate flag value.</li>
 * <li>Retained - message is retained flag value.</li>
 * </ul>
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>ServerURI - Mqtt server URI. (Required)</li>
 * <li>TopicString - the topic to subscribe to, which can include wildcards. (Required)</li>
 * <li>UserName - authentication user name. (Optional)</li>
 * <li>Password - user password. (Optional)</li>
 * <li>UseSSL - flag indicating to use SSL. (Optional)</li>
 * <li>Keystore - keystore path. (Optional)</li>
 * <li>KeystorePass - keystore password. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see ActivityMapParser
 * @see org.eclipse.paho.client.mqttv3.MqttClient
 * @see org.eclipse.paho.client.mqttv3.MqttCallback
 */
public class MqttStream extends AbstractBufferedStream<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MqttStream.class);

	private static final String SSL_PROTOCOL = "SSL"; // NON-NLS
	private static final String KEYSTORE_TYPE = KeyStore.getDefaultType();

	// Stream properties
	private String serverURI = null;
	private String userName = null;
	private String password = null;
	private String topic = null;
	private boolean useSSL = false;
	private String keystore = null;
	private String keystorePass = null;

	private MqttDataReceiver mqttDataReceiver;

	/**
	 * Constructs an empty MqttStream. Requires configuration settings to set input stream source.
	 */
	public MqttStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_SERVER_URI.equalsIgnoreCase(name)) {
			serverURI = value;
		} else if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			userName = value;
		} else if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			password = decPassword(value);
		} else if (StreamProperties.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			topic = value;
		} else if (StreamProperties.PROP_USE_SSL.equalsIgnoreCase(name)) {
			useSSL = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			keystore = value;
		} else if (StreamProperties.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			keystorePass = decPassword(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_SERVER_URI.equalsIgnoreCase(name)) {
			return serverURI;
		}
		if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			return userName;
		}
		if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			return encPassword(password);
		}
		if (StreamProperties.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			return topic;
		}
		if (StreamProperties.PROP_USE_SSL.equalsIgnoreCase(name)) {
			return useSSL;
		}
		if (StreamProperties.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			return keystore;
		}
		if (StreamProperties.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			return encPassword(keystorePass);
		}

		return super.getProperty(name);
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(serverURI)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_SERVER_URI));
		}
		if (StringUtils.isEmpty(topic)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_TOPIC_STRING));
		} else {
			// remove leading and trailing slashes to comply MQTT topic
			// naming.
			topic = topic.replaceAll("^/+", "").replaceAll("/+$", ""); // NON-NLS
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		mqttDataReceiver = new MqttDataReceiver();
		mqttDataReceiver.initialize();
	}

	@Override
	protected void start() throws Exception {
		super.start();

		mqttDataReceiver.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		if (mqttDataReceiver != null) {
			mqttDataReceiver.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return mqttDataReceiver.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(Map<String, ?> itemMap) {
		byte[] payload = (byte[]) itemMap.get(StreamsConstants.ACTIVITY_DATA_KEY);

		return payload == null ? 0 : payload.length;
	}

	/**
	 * Mqtt messages receiver thread. It implements {@link MqttCallback} interface and initiates Mqtt client to receive
	 * and handle Mqtt messages data.
	 */
	private class MqttDataReceiver extends InputProcessor implements MqttCallback {

		private MqttConnectOptions options;
		private MqttClient client;

		private MqttDataReceiver() {
			super("MqttStream.MqttDataReceiver"); // NON-NLS
		}

		/**
		 * Input data receiver initialization - Mqtt client configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize Mqtt data receiver and configure Mqtt client
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			client = new MqttClient(serverURI, MqttClient.generateClientId(), new MemoryPersistence());
			client.setCallback(this);

			options = new MqttConnectOptions();
			if (StringUtils.isNotEmpty(userName)) {
				options.setUserName(userName);
				options.setPassword((password == null ? "" : password).toCharArray());
			}

			if (useSSL) {
				SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);

				KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(keystore);
					keyStore.load(fis, keystorePass.toCharArray());
				} finally {
					Utils.close(fis);
				}

				TrustManagerFactory trustManagerFactory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(keyStore);
				sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

				options.setSocketFactory(sslContext.getSocketFactory());
			}
		}

		/**
		 * Connects client to Mqtt server and subscribes defined topic. Shuts down this data receiver if exception
		 * occurs.
		 */
		@Override
		public void run() {
			if (client != null) {
				try {
					client.connect(options == null ? new MqttConnectOptions() : options);
					client.subscribe(topic);
				} catch (MqttException exc) {
					Utils.logThrowable(logger(), OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"AbstractBufferedStream.input.start.failed", exc);
					shutdown();
				}
			}
		}

		/**
		 * Closes opened Mqtt client.
		 *
		 * @throws MqttException
		 *             if Mqtt fails to disconnect client due to internal error
		 */
		@Override
		void closeInternals() throws MqttException {
			if (client.isConnected()) {
				client.unsubscribe(topic);
				client.disconnect();
			}
			client.close();
		}

		@Override
		public void connectionLost(Throwable cause) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(MqttStreamConstants.RESOURCE_BUNDLE_NAME), "MqttStream.connection.lost",
					cause);

			try {
				closeInternals();
			} catch (MqttException exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(MqttStreamConstants.RESOURCE_BUNDLE_NAME),
						"MqttStream.error.closing.receiver", exc);
			}

			try {
				initialize();
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(MqttStreamConstants.RESOURCE_BUNDLE_NAME),
						"MqttStream.error.reconnecting.receiver", exc);
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This method buffers a map structured content of next raw activity data item received over Mqtt callback.
		 * Buffered {@link Map} contains:
		 * <ul>
		 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TOPIC_KEY}</li>
		 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#ACTIVITY_DATA_KEY}</li>
		 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TRANSPORT_KEY}</li>
		 * </ul>
		 */
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			if (message == null) {
				return;
			}

			String msgData = Utils.getString(message.getPayload());

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(MqttStreamConstants.RESOURCE_BUNDLE_NAME),
					"MqttStream.message.received", msgData);

			Map<String, Object> msgDataMap = new HashMap<>();

			if (ArrayUtils.isNotEmpty(message.getPayload())) {
				msgDataMap.put(StreamsConstants.TOPIC_KEY, topic);
				msgDataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, message.getPayload());
				msgDataMap.put(StreamsConstants.TRANSPORT_KEY, MqttStreamConstants.TRANSPORT_MQTT);
				msgDataMap.put("MessageId", message.getId()); // NON-NLS
				msgDataMap.put("Qos", message.getQos()); // NON-NLS
				msgDataMap.put("Duplicate", message.isDuplicate()); // NON-NLS
				msgDataMap.put("Retained", message.isRetained()); // NON-NLS
			}

			if (!msgDataMap.isEmpty()) {
				addInputToBuffer(msgDataMap);
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
		}
	}

}
