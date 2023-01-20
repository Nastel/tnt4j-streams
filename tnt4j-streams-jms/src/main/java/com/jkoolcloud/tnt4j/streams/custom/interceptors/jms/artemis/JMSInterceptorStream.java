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

import java.util.Map;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.protocol.core.Packet;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.InterceptorStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;

/**
 * Implements Artemis intercepted packets consumer stream. Stream itself just provides a buffer for maps constructed
 * from Artemis packets data to be picked and parsed by bound parsers.
 *
 * @version $Revision: 1 $
 */
public class JMSInterceptorStream extends InterceptorStream<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JMSInterceptorStream.class);

	/**
	 * Constructs a new JMSInterceptorStream.
	 */
	public JMSInterceptorStream() {
		this(JMSInterceptorStream.class.getSimpleName());
	}

	/**
	 * Constructs a new JMSInterceptorStream.
	 *
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public JMSInterceptorStream(int bufferSize) {
		super(bufferSize);
	}

	/**
	 * Constructs a new JMSInterceptorStream.
	 *
	 * @param name
	 *            stream name
	 */
	public JMSInterceptorStream(String name) {
		super(name);
	}

	/**
	 * Constructs a new JMSInterceptorStream.
	 *
	 * @param name
	 *            stream name
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public JMSInterceptorStream(String name, int bufferSize) {
		super(name, bufferSize);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);
	}

	@Override
	public Object getProperty(String name) {
		return super.getProperty(name);
	}

	@Override
	protected long getActivityItemByteSize(Map<String, ?> activityItem) {
		Object payload = activityItem.get(StreamsConstants.ACTIVITY_DATA_KEY); // TODO:

		long size = 0;
		try {
			if (payload instanceof byte[]) {
				size = ((byte[]) payload).length;
			} else if (payload instanceof String) {
				size = ((String) payload).length();
			} else if (payload instanceof Message) {
				size = ((Message) payload).getWholeMessageSize();
			} else if (payload instanceof Packet) {
				size = ((Packet) payload).getPacketSize();
			}
		} catch (RuntimeException exc) {
		}

		return size < 0 ? 0 : size;
	}

}
