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

package com.jkoolcloud.tnt4j.streams.inputs;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements some API (e.g. Kafka, ActiveMQ, etc.) intercepted messages stream. Stream itself does nothing and used
 * just to initiate streaming process. Reporter uses only stream output to send intercepted messages data to jKoolCloud.
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 */
public class InterceptorStream<T> extends AbstractBufferedStream<T> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(InterceptorStream.class);

	private boolean ended = false;

	/**
	 * Constructs a new InterceptorStream.
	 */
	public InterceptorStream() {
		super();
	}

	/**
	 * Constructs a new InterceptorStream.
	 * 
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public InterceptorStream(int bufferSize) {
		super(bufferSize);
	}

	/**
	 * Constructs a new InterceptorStream.
	 *
	 * @param name
	 *            stream name
	 */
	public InterceptorStream(String name) {
		setName(name);
	}

	/**
	 * Constructs a new InterceptorStream.
	 *
	 * @param name
	 *            stream name
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public InterceptorStream(String name, int bufferSize) {
		this(bufferSize);

		setName(name);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected void start() throws Exception {
		super.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	public boolean addInputToBuffer(T inputData) throws IllegalStateException {
		return super.addInputToBuffer(inputData);
	}

	@Override
	protected ActivityInfo makeActivityInfo(T data) throws Exception {
		if (data instanceof ActivityInfo) {
			return (ActivityInfo) data;
		} else {
			return super.makeActivityInfo(data);
		}
	}

	@Override
	protected long getActivityItemByteSize(T activityItem) {
		return 0; // TODO
	}

	@Override
	protected boolean isInputEnded() {
		return ended;
	}

	/**
	 * Marks stream input end.
	 */
	public void markEnded() {
		ended = true;
		offerDieMarker();
	}
}
