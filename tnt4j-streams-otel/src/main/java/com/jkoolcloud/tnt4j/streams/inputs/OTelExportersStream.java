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

import java.util.Map;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author slb
 * @version 1.0
 * @created 2023-06-08 13:15
 */
public class OTelExportersStream extends InterceptorStream<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(OTelExportersStream.class);

	/**
	 * Constructs a new OTelExportersStream.
	 */
	public OTelExportersStream() {
		this(OTelExportersStream.class.getSimpleName());
	}

	/**
	 * Constructs a new OTelExportersStream.
	 *
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public OTelExportersStream(int bufferSize) {
		super(bufferSize);
	}

	/**
	 * Constructs a new OTelExportersStream.
	 *
	 * @param name
	 *            stream name
	 */
	public OTelExportersStream(String name) {
		super(name);
	}

	/**
	 * Constructs a new OTelExportersStream.
	 *
	 * @param name
	 *            stream name
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public OTelExportersStream(String name, int bufferSize) {
		super(name, bufferSize);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected long getActivityItemByteSize(Map<String, ?> activityItem) {
		Object payload = activityItem.get(StreamsConstants.ACTIVITY_DATA_KEY);

		if (payload instanceof byte[]) {
			return ((byte[]) payload).length;
		} else if (payload instanceof String) {
			return ((String) payload).length();
		}

		Integer cLength = (Integer) Utils.getMapValueByPath("Metadata.ContentLength", activityItem);
		if (cLength != null) {
			return cLength.longValue();
		}

		return 0;
	}

}
