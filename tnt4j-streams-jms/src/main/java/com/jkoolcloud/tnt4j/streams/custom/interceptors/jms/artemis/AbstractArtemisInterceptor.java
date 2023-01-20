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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.BaseInterceptor;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;

import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * Base class for ActiveMQ Artemis packets interceptor. Performs intercepted packet and connection attributes collection
 * into map to processed by {@link InterceptionsManager}.
 * <p>
 * Produced map contains these root level entries:
 * <ul>
 * <li>{@code "packet"} - intercepted packet attributes</li>
 * <li>{@code "conn"} - intercepted connection attributes</li>
 * <li>{@code "ictx"} - interceptor context attributes</li>
 * </ul>
 * <p>
 * Produced map may be laid out as:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.InterceptionsManager.MapConstructionStrategy#FLAT}
 * - map is single level (flat) where entry key is made by appending complex type property names path.</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.InterceptionsManager.MapConstructionStrategy#DEEP}
 * - map is constructed by making dedicated value map for complex type properties.</li>
 * </ul>
 *
 * @param <P>
 *            type of interceptor handled packet
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractArtemisInterceptor<P> implements BaseInterceptor<P> {

	private static final String PACKET_ROOT_KEY = "packet"; // NON-NLS
	private static final String CONN_ROOT_KEY = "conn"; // NON-NLS
	private static final String CTX_ROOT_KEY = "ictx"; // NON-NLS

	private static final String INTERCEPT_TIME_KEY = "interceptTimestamp"; // NON-NLS

	private static final String[] PACKET_EXCLUDES = new String[] { //
			"pagingStoreFactory" // NON-NLS
	};
	private static final String[] CONN_EXCLUDES = new String[] { //
			"protocolConnection" // NON-NLS
			, "incomingInterceptors" // NON-NLS
			, "transferLock" // NON-NLS
			, "outgoingInterceptors" // NON-NLS
	};

	// private ExecutorService executorService = new ThreadPoolExecutor();

	/**
	 * Interceptions manager instance handling interceptions flow for this interceptor.
	 */
	protected static final InterceptionsManager iManager = InterceptionsManager.getInstance();

	private boolean bound = false;
	private boolean shutdown = false;

	/**
	 * Constructs a new AbstractArtemisInterceptor.
	 */
	protected AbstractArtemisInterceptor() {
		// bind ();
	}

	/**
	 * Returns logger instance for ths interceptor.
	 *
	 * @return the logger instance for this interceptor
	 */
	protected abstract EventSink logger();

	/**
	 * Returns configuration scope for this interceptor.
	 *
	 * @return the configuration scope for this interceptor
	 */
	protected abstract String getScope();

	/**
	 * Binds this interceptor reference to interceptions manager.
	 */
	private void bind() {
		iManager.bindReference(this);

		bound = true;
	}

	/**
	 * Unbinds this interceptor reference from interceptions manager.
	 */
	public void unbind() {
		iManager.unbindReference(this);

		// bound = false;
		shutdown = true;
	}

	@Override
	public boolean intercept(P packet, RemotingConnection connection) throws ActiveMQException {
		if (!shutdown && packet != null) {
			long iTime = System.currentTimeMillis();

			if (!bound) {
				bind();
			}

			if (iManager.shallIntercept(getScope(), packet.getClass())) {
				Map<String, Object> propsMap = new LinkedHashMap<>();

				try {
					switch (iManager.getInterceptedDataMapLayout(getScope())) {
					case DEEP:
						InterceptorUtils.toMapDeep(PACKET_ROOT_KEY, packet, propsMap, PACKET_EXCLUDES);
						InterceptorUtils.toMapDeep(CONN_ROOT_KEY, connection, propsMap, CONN_EXCLUDES);

						Map<String, Object> ctxMap = new LinkedHashMap<>(3);
						ctxMap.put(INTERCEPT_TIME_KEY, iTime);

						propsMap.put(CTX_ROOT_KEY, ctxMap);
						break;
					case FLAT:
					default:
						InterceptorUtils.toMapFlat(PACKET_ROOT_KEY, packet, propsMap, PACKET_EXCLUDES);
						InterceptorUtils.toMapFlat(CONN_ROOT_KEY, connection, propsMap, CONN_EXCLUDES);

						propsMap.put(CTX_ROOT_KEY + ":" + INTERCEPT_TIME_KEY, iTime); // NON-NLS
						break;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}

				postProcess(packet, connection, propsMap);

				iManager.intercept(propsMap);
			}

			if (connection.isClient() && isCloseMessage(packet)) {
				unbind();
				iManager.shutdown();
			}
		}

		return true;
	}

	/**
	 * Checks if provided packet is connection close message according to this interceptor handled protocol.
	 *
	 * @param packet
	 *            packet instance to check
	 *
	 * @return {@code true} if packet is protocol connection close message, {@code false} - otherwise
	 */
	protected abstract boolean isCloseMessage(P packet);

	/**
	 * Performs actions on intercepted packet, connection and collected attributes map right before passing it to
	 * interceptions manager.
	 *
	 * @param packet
	 *            intercepted packet instance
	 * @param connection
	 *            intercepted connection instance
	 * @param propsMap
	 *            intercepted packet and connection attributes map
	 */
	protected void postProcess(P packet, RemotingConnection connection, Map<String, Object> propsMap) {
	}
}
