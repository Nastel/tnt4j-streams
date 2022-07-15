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

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

/**
 * Implements Artemis packets interceptor transmitted over MQTT protocol.
 *
 * @version $Revision: 1 $
 *
 * @see io.netty.handler.codec.mqtt.MqttMessage
 */
public class MQTTInterceptor extends AbstractArtemisInterceptor<MqttMessage>
		implements org.apache.activemq.artemis.core.protocol.mqtt.MQTTInterceptor {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MQTTInterceptor.class);

	/**
	 * Defines interceptor configuration scope identifier {@value}.
	 */
	public static final String SCOPE = "mqtt"; // NON-NLS

	/**
	 * Constructs a new MQTTInterceptor.
	 */
	public MQTTInterceptor() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected String getScope() {
		return SCOPE;
	}

	@Override
	protected boolean isCloseMessage(MqttMessage packet) {
		return packet.fixedHeader().messageType() == MqttMessageType.DISCONNECT;
	}

}
