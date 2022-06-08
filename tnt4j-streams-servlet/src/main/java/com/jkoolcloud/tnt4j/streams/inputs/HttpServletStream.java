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

import java.util.Map;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ServletStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements HTTP servlet requests received activities consumer stream. Stream itself just provides a buffer for HTTP
 * servlet requests data to be picked and parsed by bound parsers.
 *
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.TNT4JStreamsServlet
 */
public class HttpServletStream extends InterceptorStream<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(HttpServletStream.class);

	/**
	 * Defines default response template used by HTTP streams.
	 */
	public static final String DEFAULT_RESPONSE_TEMPLATE = "" //
			+ "\"$body\": {" //
			+ "    \"type\": \"HTML\"," //
			+ "	   \"payload\": [" //
			+ "        \"<html><body><h1>${message}</h1></body></html>\"" //
			+ "    ]" //
			+ "}";

	private String responseTemplate = DEFAULT_RESPONSE_TEMPLATE;

	/**
	 * Constructs a new HttpServletStream.
	 */
	public HttpServletStream() {
		this(HttpServletStream.class.getSimpleName());
	}

	/**
	 * Constructs a new HttpServletStream.
	 *
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public HttpServletStream(int bufferSize) {
		super(bufferSize);
	}

	/**
	 * Constructs a new HttpServletStream.
	 *
	 * @param name
	 *            stream name
	 */
	public HttpServletStream(String name) {
		super(name);
	}

	/**
	 * Constructs a new HttpServletStream.
	 *
	 * @param name
	 *            stream name
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	public HttpServletStream(String name, int bufferSize) {
		super(name, bufferSize);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (ServletStreamProperties.PROP_RESPONSE_TEMPLATE.equalsIgnoreCase(name)) {
			responseTemplate = value;
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ServletStreamProperties.PROP_RESPONSE_TEMPLATE.equalsIgnoreCase(name)) {
			return responseTemplate;
		}
		return super.getProperty(name);
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
