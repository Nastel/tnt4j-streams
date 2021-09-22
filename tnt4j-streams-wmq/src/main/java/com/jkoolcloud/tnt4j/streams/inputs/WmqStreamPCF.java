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

import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFConstants;
import com.ibm.mq.headers.pcf.PCFContent;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Implements a WebSphere MQ activity stream, where activity data is {@link MQMessage} transformed to
 * {@link PCFContent}.
 * <p>
 * This activity stream requires parsers that can support {@link PCFContent} data.
 * <p>
 * This activity stream supports configuration properties from {@link AbstractWmqStream} (and higher hierarchy streams).
 *
 * @version $Revision: 2 $
 */
public class WmqStreamPCF extends AbstractWmqStream<PCFContent> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(WmqStreamPCF.class);

	/**
	 * Constructs an empty WmqStreamPCF. Requires configuration settings to set input source.
	 */
	public WmqStreamPCF() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected PCFContent getActivityDataFromMessage(MQMessage mqMsg) throws Exception {
		PCFMessage msgData = new PCFMessage(mqMsg);
		// appending missing MQMD fields to PCF message
		handleMQMDParameters(msgData, mqMsg);

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStream.message.data", msgData.size(), msgData);

		return msgData;
	}

	private void handleMQMDParameters(PCFMessage msgData, MQMessage mqMsg) {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStreamPCF.adding.mq.to.pcf");

		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_REPORT, mqMsg.report);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_MSG_TYPE, mqMsg.messageType);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_EXPIRY, mqMsg.expiry);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_FEEDBACK, mqMsg.feedback);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_ENCODING, mqMsg.encoding);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIA_CODED_CHAR_SET_ID, mqMsg.characterSet);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACH_FORMAT_NAME, mqMsg.format);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_PRIORITY, mqMsg.priority);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_PERSISTENCE, mqMsg.persistence);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQBACF_MSG_ID, mqMsg.messageId);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQBACF_CORREL_ID, mqMsg.correlationId);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_BACKOUT_COUNT, mqMsg.backoutCount);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_REPLY_TO_Q, mqMsg.replyToQueueName);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_REPLY_TO_Q_MGR, mqMsg.replyToQueueManagerName);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_USER_IDENTIFIER, mqMsg.userId);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQBACF_ACCOUNTING_TOKEN, mqMsg.accountingToken);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_APPL_IDENTITY_DATA, mqMsg.applicationIdData);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIA_APPL_TYPE, mqMsg.putApplicationType);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_APPL_NAME, mqMsg.putApplicationName);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_APPL_ORIGIN_DATA, mqMsg.applicationOriginData);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQBACF_GROUP_ID, mqMsg.groupId);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACH_MSG_SEQUENCE_NUMBER, mqMsg.messageSequenceNumber);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_OFFSET, mqMsg.offset);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_MSG_FLAGS, mqMsg.messageFlags);
		addMQMDParameterToPCFMessage(msgData, MQConstants.MQIACF_ORIGINAL_LENGTH, mqMsg.originalLength);

		if (mqMsg.putDateTime != null) {
			addMQMDParameterToPCFMessage(msgData, MQConstants.MQIAMO64_HIGHRES_TIME,
					mqMsg.putDateTime.getTimeInMillis());
			String putDTString = DateFormatUtils.format(mqMsg.putDateTime.getTime(),
					WmqStreamConstants.PUT_DATE_TIME_PATTERN, TimeZone.getTimeZone("GMT"));
			String[] putDTTokens = putDTString.split(" "); // NON-NLS

			if (putDTTokens.length > 0 && StringUtils.isNotEmpty(putDTTokens[0])) {
				addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_PUT_DATE, putDTTokens[0]);
			}
			if (putDTTokens.length > 1 && StringUtils.isNotEmpty(putDTTokens[1])) {
				addMQMDParameterToPCFMessage(msgData, MQConstants.MQCACF_PUT_TIME, putDTTokens[1]);
			}
		}
	}

	private void addMQMDParameterToPCFMessage(PCFMessage msg, int paramId, Object val) {
		if (val == null) {
			return;
		}

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStreamPCF.adding.pcf.param", PCFConstants.lookupParameter(paramId), paramId, Utils.toString(val));

		if (val instanceof Integer) {
			msg.addParameter(getMQMDParamId(paramId), (int) val);
		} else if (val instanceof Long) {
			msg.addParameter(getMQMDParamId(paramId), (long) val);
		} else if (val instanceof byte[]) {
			msg.addParameter(getMQMDParamId(paramId), (byte[]) val);
		} else if (val instanceof int[]) {
			msg.addParameter(getMQMDParamId(paramId), (int[]) val);
		} else if (val instanceof String) {
			msg.addParameter(getMQMDParamId(paramId), (String) val);
		} else if (val instanceof long[]) {
			msg.addParameter(getMQMDParamId(paramId), (long[]) val);
		} else if (val instanceof String[]) {
			msg.addParameter(getMQMDParamId(paramId), (String[]) val);
		} else {
			throw new IllegalArgumentException(StreamsResources.getStringFormatted(
					WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStreamPCF.parameter.incompatible",
					PCFConstants.lookupParameter(paramId), paramId, val.getClass().getSimpleName()));
		}
	}

	private static int getMQMDParamId(int paramId) {
		return WmqStreamConstants.PCF_MQMD_HEADER + paramId;
	}
}
