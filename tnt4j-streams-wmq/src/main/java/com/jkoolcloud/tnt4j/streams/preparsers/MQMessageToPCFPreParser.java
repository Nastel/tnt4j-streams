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

package com.jkoolcloud.tnt4j.streams.preparsers;

import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.WmqStreamPCF;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * RAW activity data pre-parser capable to convert incoming activity data from {@link com.ibm.mq.MQMessage} to
 * {@link com.ibm.mq.headers.pcf.PCFMessage} format.
 * 
 * @version $Revision: 2 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser
 */
public class MQMessageToPCFPreParser extends AbstractMQMessagePreParser<PCFMessage> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MQMessageToPCFPreParser.class);

	@Override
	public PCFMessage preParse(MQMessage mqMsg) throws Exception {
		return WmqStreamPCF.msgToPCF(mqMsg);
	}

	@Override
	public String dataTypeReturned() {
		return "PCF MESSAGE"; // NON-NLS
	}
}
