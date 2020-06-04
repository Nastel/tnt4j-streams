/*
 * Copyright 2014-2020 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.custom.preparsers;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.ibm.mq.MQMessage;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs;
import com.jkoolcloud.tnt4j.streams.preparsers.AbstractMQMessagePreParser;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * RAW activity data pre-parser capable to convert incoming activity data from {@link com.ibm.mq.MQMessage} binary
 * payload data to {@link java.util.Map} of MQ Probe structures:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQAPINT}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAZOS}</li>
 * </ul>
 * 
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQAPINT
 * @see com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAZOS
 */
public class MQMessageToMQProbeStructMap extends AbstractMQMessagePreParser<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MQMessageToMQProbeStructMap.class);

	@Override
	public Map<String, ?> preParse(MQMessage mqMsg) throws Exception {
		int enc = mqMsg.encoding;
		int charSet = mqMsg.characterSet;

		byte[] msgData = new byte[mqMsg.getDataLength()];
		mqMsg.readFully(msgData);
		LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStream.message.data", msgData.length, Utils.toHexDump(msgData));

		ByteBuffer bb = ByteBuffer.wrap(msgData);

		byte[] bStrId = new byte[4];
		System.arraycopy(msgData, 0, bStrId, 0, 4);
		String strId = WmqUtils.getString(bStrId, charSet);

		MQProbeStructs.MQProbeRootStruct mqStruct = null;

		try {
			switch (strId) {
			case "APIE": // NON-NLS
				mqStruct = MQProbeStructs.TAMQAPINT.read(bb, enc, charSet);
				break;
			case MQProbeStructs.TAMQBATCH.STRUC_ID:
				mqStruct = MQProbeStructs.TAZOS.read(bb, enc, charSet);
				break;
			default:
			}
		} catch (BufferUnderflowException exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"MQMessageToMQProbeStructMap.buffer.underflow", bb.position(), bb.remaining(), bb.limit(),
					bb.capacity(), exc);
		}

		return mqStruct == null ? null : mqStruct.asMap();
	}

	@Override
	public String dataTypeReturned() {
		return "MAP"; // NON-NLS
	}
}
