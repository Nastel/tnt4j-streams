/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

/**
 * General utility methods used by TNT4J-Streams Kafka module.
 * 
 * @version $Revision: 1 $
 */
public class KafkaUtils {
	/**
	 * Resolves {@link org.apache.kafka.common.header.Headers} contained header value defined by {@code headers}
	 * keys/indices {@code path} array.
	 *
	 * @param path
	 *            keys/indices path as array of consumer record headers
	 * @param headers
	 *            consumer record headers to resolve value
	 * @param i
	 *            processed locator path element index
	 * @return resolved consumer record headers value, or {@code null} if value is not resolved
	 * @throws java.lang.RuntimeException
	 *             if field can't be found or accessed
	 *
	 * @see #getHeadersValue(String[], Iterable, int)
	 */
	public static byte[] getHeadersValue(String[] path, Headers headers, int i) throws RuntimeException {
		if (ArrayUtils.isEmpty(path) || headers == null) {
			return null;
		}

		byte[] val = null;
		String headerStr = path[i];
		try {
			val = resolveHeaderByIndex(headers, Integer.parseInt(headerStr));
		} catch (NumberFormatException exc) {
			Iterable<Header> hHeaders = headers.headers(headerStr);

			if (hHeaders != null && i < path.length - 1) {
				val = getHeadersValue(path, hHeaders, i + 1);
			}
		}

		return val;
	}

	/**
	 * Resolves {@link org.apache.kafka.common.header.Header} value from {@code headers} collection defined by
	 * keys/indices {@code path} array.
	 *
	 * @param path
	 *            keys/indices path as array of consumer record headers
	 * @param headers
	 *            consumer record headers collection to resolve value
	 * @param i
	 *            processed locator path element index
	 * @return resolved consumer record header value, or {@code null} if value is not resolved
	 * @throws java.lang.RuntimeException
	 *             if field can't be found or accessed
	 */
	public static byte[] getHeadersValue(String[] path, Iterable<Header> headers, int i) throws RuntimeException {
		if (ArrayUtils.isEmpty(path) || headers == null) {
			return null;
		}

		byte[] val = null;
		String headerStr = path[i];
		try {
			val = resolveHeaderByIndex(headers, Integer.parseInt(headerStr));
		} catch (NumberFormatException exc) {
			for (Header header : headers) {
				if (header.key().equals(headerStr)) {
					val = header.value();
					break;
				}
			}
		}

		return val;
	}

	/**
	 * Resolves consumer record header value defined by header index {@code hIdx}.
	 * <p>
	 * Value gets resolved only if {@code headers} is not {@code null} and {@code hIdx} is less than size of
	 * {@code headers} collection.
	 *
	 * @param headers
	 *            consumer record headers collection to resolve value
	 * @param hIdx
	 *            header index to get value
	 * @return resolved consumer record header value, or {@code null} if value is not resolved
	 */
	public static byte[] resolveHeaderByIndex(Iterable<Header> headers, int hIdx) {
		if (headers != null) {
			int idx = 0;
			for (Header header : headers) {
				if (idx == hIdx) {
					return header.value();
				}
				idx++;
			}
		}

		return null;
	}
}
