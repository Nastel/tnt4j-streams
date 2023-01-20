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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis;

import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.PacketImpl;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements Artemis core packets interceptor transmitted between client and server (broker).
 *
 * @version $Revision: 1 $
 *
 * @see org.apache.activemq.artemis.core.protocol.core.Packet
 */
public class PacketInterceptor extends AbstractArtemisInterceptor<Packet> implements Interceptor {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(PacketInterceptor.class);

	/**
	 * Defines interceptor configuration scope identifier {@value}.
	 */
	public static final String SCOPE = "core"; // NON-NLS

	/**
	 * Constructs a new PacketInterceptor.
	 */
	public PacketInterceptor() {
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
	protected boolean isCloseMessage(Packet packet) {
		return packet.getType() == PacketImpl.SESS_CLOSE;
	}

}
