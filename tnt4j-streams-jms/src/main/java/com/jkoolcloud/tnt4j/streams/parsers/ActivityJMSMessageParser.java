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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.JMSParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an JMS message data structure. Message
 * payload data is put into map entry using key defined in {@link StreamsConstants#ACTIVITY_DATA_KEY}. This parser
 * supports JMS messages of those types:
 * <ul>
 * <li>{@link javax.jms.TextMessage} - activity data is message text</li>
 * <li>{@link javax.jms.BytesMessage} - activity data is message {@code byte[]} or string made from message
 * {@code byte[]} depending on property 'ConvertToString' value</li>
 * <li>{@link javax.jms.MapMessage} - activity data is message map entries</li>
 * <li>{@link javax.jms.StreamMessage} - activity data is message {@code byte[]} or string made from message
 * {@code byte[]} depending on property 'ConvertToString' value</li>
 * <li>{@link javax.jms.ObjectMessage} - activity data is message serializable object</li>
 * </ul>
 * <p>
 * NOTE: Custom messages parsing not implemented and puts just log entry.
 * <p>
 * This parser resolved data map may contain such entries:
 * <ul>
 * <li>ActivityData - JMS message payload data. In case of {@link javax.jms.MapMessage} this entry is omitted, because
 * message contained map entries are copied to data map.</li>
 * <li>MsgMetadata - JMS message metadata map containing those fields:</li>
 * <ul>
 * <li>Correlator - message correlation identifier</li>
 * <li>CorrelatorBytes - message correlation identifier bytes value</li>
 * <li>DeliveryMode - message delivery mode number</li>
 * <li>Destination - destination name this message was received from</li>
 * <li>Expiration - message's expiration time</li>
 * <li>MessageId - message identifier string</li>
 * <li>Priority - message priority level number</li>
 * <li>Redelivered - indication flag of whether this message is being redelivered</li>
 * <li>ReplyTo - destination name to which a reply to this message should be sent</li>
 * <li>Timestamp - timestamp in milliseconds</li>
 * <li>Type - message type name supplied by the client when the message was sent</li>
 * <li>CustomMsgProps - map of properties accessible over keys enumeration {@link javax.jms.Message#getPropertyNames()}
 * and values resolved over {@link javax.jms.Message#getStringProperty(String)}</li>
 * </ul>
 * <li>ActivityTransport - value is always
 * {@value com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants#TRANSPORT_JMS}.</li>
 * </ul>
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link AbstractActivityMapParser}):
 * <ul>
 * <li>ConvertToString - flag indicating whether to convert message payload {@code byte[]} data to string. Applicable to
 * {@link javax.jms.BytesMessage} and {@link javax.jms.StreamMessage}. Default value - 'false'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityJMSMessageParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityJMSMessageParser.class);

	private static final int BYTE_BUFFER_LENGTH = 1024;

	private boolean convertToString = false;

	/**
	 * Constructs a new ActivityJMSMessageParser.
	 */
	public ActivityJMSMessageParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link javax.jms.Message}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return data instanceof Message;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (JMSParserProperties.PROP_CONV_TO_STRING.equalsIgnoreCase(name)) {
			convertToString = Utils.toBoolean(value);

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (JMSParserProperties.PROP_CONV_TO_STRING.equalsIgnoreCase(name)) {
			return convertToString;
		}

		return super.getProperty(name);
	}

	/**
	 * Makes map object containing activity object data collected from JMS message payload data.
	 *
	 * @param data
	 *            activity object data object - JMS message
	 *
	 * @return activity object data map
	 */
	@Override
	protected Map<String, Object> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		Message message = (Message) data;
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put(RAW_ACTIVITY_STRING_KEY, message.toString());

		try {
			Map<String, Object> metadataMap = parseCommonMessage(message);
			dataMap.put(JMSStreamConstants.MSG_METADATA_KEY, metadataMap);

			if (message instanceof TextMessage) {
				parseTextMessage((TextMessage) message, dataMap);
			} else if (message instanceof BytesMessage) {
				parseBytesMessage((BytesMessage) message, dataMap);
			} else if (message instanceof MapMessage) {
				parseMapMessage((MapMessage) message, dataMap);
			} else if (message instanceof StreamMessage) {
				parseStreamMessage((StreamMessage) message, dataMap);
			} else if (message instanceof ObjectMessage) {
				parseObjectMessage((ObjectMessage) message, dataMap);
			} else {
				parseCustomMessage(message, dataMap);
			}
		} catch (JMSException exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityJMSMessageParser.payload.data.error", exc);
		}

		if (!dataMap.isEmpty()) {
			dataMap.put(StreamsConstants.TRANSPORT_KEY, JMSStreamConstants.TRANSPORT_JMS);
		}

		return dataMap;
	}

	/**
	 * Collects JMS {@link com.jkoolcloud.tnt4j.core.Message} common fields values into message metadata map.
	 * <p>
	 * Common message metadata fields are:
	 * <ul>
	 * <li>Correlator - message correlation identifier</li>
	 * <li>CorrelatorBytes - message correlation identifier bytes value</li>
	 * <li>DeliveryMode - message delivery mode number</li>
	 * <li>Destination - destination name this message was received from</li>
	 * <li>Expiration - message's expiration time</li>
	 * <li>MessageId - message identifier string</li>
	 * <li>Priority - message priority level number</li>
	 * <li>Redelivered - indication flag of whether this message is being redelivered</li>
	 * <li>ReplyTo - destination name to which a reply to this message should be sent</li>
	 * <li>Timestamp - timestamp in milliseconds</li>
	 * <li>Type - message type name supplied by the client when the message was sent</li>
	 * <li>CustomMsgProps - map of properties accessible over keys enumeration
	 * {@link javax.jms.Message#getPropertyNames()} and values resolved over
	 * {@link javax.jms.Message#getStringProperty(String)}</li>
	 * </ul>
	 *
	 * @param message
	 *            JMS message instance to parse
	 * @return map instance, containing resolved message metadata values
	 * @throws JMSException
	 *             if JMS exception occurs while getting common fields values from message
	 */
	protected Map<String, Object> parseCommonMessage(Message message) throws JMSException {
		Map<String, Object> msgMetaMap = new HashMap<>(10);

		msgMetaMap.put(StreamFieldType.Correlator.name(), message.getJMSCorrelationID());
		msgMetaMap.put("CorrelatorBytes", message.getJMSCorrelationIDAsBytes()); // NON-NLS
		msgMetaMap.put("DeliveryMode", message.getJMSDeliveryMode()); // NON-NLS
		// msgMetaMap.put("DeliveryTime", message.getJMSDeliveryTime()); // NON-NLS
		msgMetaMap.put("Destination", getDestinationName(message.getJMSDestination())); // NON-NLS
		msgMetaMap.put("Expiration", message.getJMSExpiration()); // NON-NLS
		msgMetaMap.put("MessageId", message.getJMSMessageID()); // NON-NLS
		msgMetaMap.put("Priority", message.getJMSPriority()); // NON-NLS
		msgMetaMap.put("Redelivered", message.getJMSRedelivered()); // NON-NLS
		msgMetaMap.put("ReplyTo", getDestinationName(message.getJMSReplyTo())); // NON-NLS
		msgMetaMap.put("Timestamp", message.getJMSTimestamp()); // NON-NLS
		msgMetaMap.put("Type", message.getJMSType()); // NON-NLS

		@SuppressWarnings("unchecked")
		Enumeration<String> propNames = message.getPropertyNames();
		Map<String, Object> customPropsMap = new HashMap<>();
		if (propNames != null) {
			while (propNames.hasMoreElements()) {
				String pName = propNames.nextElement();
				customPropsMap.put(pName, message.getStringProperty(pName));
			}
		}
		if (!customPropsMap.isEmpty()) {
			msgMetaMap.put("CustomMsgProps", customPropsMap); // NON-NLS
		}

		return msgMetaMap;
	}

	/**
	 * Resolves provided JMS {@link javax.jms.Destination} instance name.
	 *
	 * @param dest
	 *            JMS destination instance to resolve value
	 * @return resolved JMS destination name, or {@code null} if destination is {@code null}
	 * @throws JMSException
	 *             if JMS exception occurs while getting destination name value
	 */
	protected static String getDestinationName(Destination dest) throws JMSException {
		if (dest instanceof Topic) {
			return ((Topic) dest).getTopicName();
		} else if (dest instanceof Queue) {
			return ((Queue) dest).getQueueName();
		} else {
			return dest == null ? null : dest.toString();
		}
	}

	/**
	 * Parse JMS {@link TextMessage} activity info into activity data map.
	 *
	 * @param textMessage
	 *            JMS text message
	 * @param dataMap
	 *            activity data map collected from JMS {@link TextMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while getting text from message
	 */
	protected void parseTextMessage(TextMessage textMessage, Map<String, Object> dataMap) throws JMSException {
		String text = textMessage.getText();
		if (StringUtils.isNotEmpty(text)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, text);
		}
	}

	/**
	 * Parse JMS {@link BytesMessage} activity info into activity data map.
	 *
	 * @param bytesMessage
	 *            JMS bytes message
	 * @param dataMap
	 *            activity data map collected from JMS {@link BytesMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while reading bytes from message
	 */
	protected void parseBytesMessage(BytesMessage bytesMessage, Map<String, Object> dataMap) throws JMSException {
		byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(bytes);

		if (ArrayUtils.isNotEmpty(bytes)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, convertToString ? Utils.getString(bytes) : bytes);
		}
	}

	/**
	 * Parse JMS {@link MapMessage} activity info into activity data map.
	 *
	 * @param mapMessage
	 *            JMS map message
	 * @param dataMap
	 *            activity data map collected from JMS {@link MapMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while getting map entries from message
	 */
	@SuppressWarnings("unchecked")
	protected void parseMapMessage(MapMessage mapMessage, Map<String, Object> dataMap) throws JMSException {
		Enumeration<String> en = (Enumeration<String>) mapMessage.getMapNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			dataMap.put(key, mapMessage.getObject(key));
		}
	}

	/**
	 * Parse JMS {@link StreamMessage} activity info into activity data map.
	 *
	 * @param streamMessage
	 *            JMS stream message
	 * @param dataMap
	 *            activity data map collected from JMS {@link StreamMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while reading bytes from message
	 */
	protected void parseStreamMessage(StreamMessage streamMessage, Map<String, Object> dataMap) throws JMSException {
		streamMessage.reset();

		byte[] buffer = new byte[BYTE_BUFFER_LENGTH];

		int bytesRead = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.length);

		try {
			do {
				bytesRead = streamMessage.readBytes(buffer);

				baos.write(buffer);
			} while (bytesRead != 0);
		} catch (IOException exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityJMSMessageParser.bytes.buffer.error", exc);
		}

		byte[] bytes = baos.toByteArray();
		Utils.close(baos);

		if (ArrayUtils.isNotEmpty(bytes)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, convertToString ? Utils.getString(bytes) : bytes);
		}
	}

	/**
	 * Parse JMS {@link ObjectMessage} activity info into activity data map.
	 *
	 * @param objMessage
	 *            JMS object message
	 * @param dataMap
	 *            activity data map collected from JMS {@link ObjectMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while getting {@link Serializable} object from message
	 */
	protected void parseObjectMessage(ObjectMessage objMessage, Map<String, Object> dataMap) throws JMSException {
		Serializable serializableObj = objMessage.getObject();
		if (serializableObj != null) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, serializableObj);
		}
	}

	/**
	 * Parse custom message activity info into activity data map.
	 *
	 * @param message
	 *            custom JMS message
	 * @param dataMap
	 *            activity data map collected from custom JMS message
	 * @throws JMSException
	 *             if any JMS exception occurs while parsing message
	 */
	protected void parseCustomMessage(Message message, Map<String, Object> dataMap) throws JMSException {
		logger().log(OpLevel.WARNING, StreamsResources.getBundle(JMSStreamConstants.RESOURCE_BUNDLE_NAME),
				"ActivityJMSMessageParser.parsing.custom.jms.message");
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "JMS MESSAGE" }; // NON-NLS

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - {@code "JMS MESSAGE"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}
}
