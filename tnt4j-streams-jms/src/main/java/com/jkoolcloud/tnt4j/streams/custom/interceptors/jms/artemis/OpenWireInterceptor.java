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

import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConnectionControl;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements Artemis packets interceptor transmitted over OpenWire protocol.
 *
 * @version $Revision: 1 $
 *
 * @see org.apache.activemq.command.Command
 */
public class OpenWireInterceptor extends AbstractArtemisInterceptor<Command>
		implements org.apache.activemq.artemis.core.protocol.openwire.OpenWireInterceptor {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(OpenWireInterceptor.class);

	/**
	 * Defines interceptor configuration scope identifier {@value}.
	 */
	public static final String SCOPE = "owire"; // NON-NLS

	/**
	 * Constructs a new OpenWireInterceptor.
	 */
	public OpenWireInterceptor() {
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
	protected boolean isCloseMessage(Command packet) {
		return (packet.isConnectionControl()
				&& (((ConnectionControl) packet).isExit() || ((ConnectionControl) packet).isClose()))
				|| packet.isShutdownInfo();
	}

}
