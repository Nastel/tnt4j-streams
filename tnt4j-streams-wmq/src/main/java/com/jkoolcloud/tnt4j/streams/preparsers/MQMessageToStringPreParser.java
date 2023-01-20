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

package com.jkoolcloud.tnt4j.streams.preparsers;

import com.ibm.mq.MQMessage;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * RAW activity data pre-parser capable to convert incoming activity data from {@link com.ibm.mq.MQMessage} to
 * {@link java.lang.String} format.
 * 
 * @version $Revision: 1 $
 */
public class MQMessageToStringPreParser extends AbstractMQMessagePreParser<String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MQMessageToStringPreParser.class);

	@Override
	public String preParse(MQMessage mqMsg) throws Exception {
		String msgData = mqMsg.readStringOfByteLength(mqMsg.getDataLength());
		LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStream.message.data", msgData.length(), msgData);

		return msgData;
	}

	@Override
	public String dataTypeReturned() {
		return "TEXT"; // NON-NLS
	}
}
