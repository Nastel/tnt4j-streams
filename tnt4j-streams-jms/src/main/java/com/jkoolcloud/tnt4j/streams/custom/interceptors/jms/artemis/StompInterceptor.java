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

import org.apache.activemq.artemis.core.protocol.stomp.StompFrame;
import org.apache.activemq.artemis.core.protocol.stomp.StompFrameInterceptor;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements Artemis packets interceptor transmitted over STOMP protocol.
 *
 * @version $Revision: 1 $
 * 
 * @see org.apache.activemq.artemis.core.protocol.stomp.StompFrame
 */
public class StompInterceptor extends AbstractArtemisInterceptor<StompFrame> implements StompFrameInterceptor {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StompInterceptor.class);

	/**
	 * Defines interceptor configuration scope identifier {@value}.
	 */
	public static final String SCOPE = "stomp"; // NON-NLS

	/**
	 * Constructs a new StompInterceptor.
	 */
	public StompInterceptor() {
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
	protected boolean isCloseMessage(StompFrame packet) {
		return packet.needsDisconnect();
	}

}
