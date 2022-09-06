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

import com.ibm.mq.MQMessage;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements a WebSphere MQ activity stream, where activity data is {@link com.ibm.mq.MQMessage}.
 * <p>
 * This activity stream requires parsers that can support {@link com.ibm.mq.MQMessage} data.
 * <p>
 * This activity stream supports configuration properties from {@link AbstractWmqStream} (and higher hierarchy streams).
 *
 * @version $Revision: 3 $
 */
public class WmqStream extends AbstractWmqStream<MQMessage> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(WmqStream.class);

	/**
	 * Constructs an empty WmqStream. Requires configuration settings to set input source.
	 */
	public WmqStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected MQMessage getActivityDataFromMessage(MQMessage mqMsg) throws Exception {
		return mqMsg;
	}
}
