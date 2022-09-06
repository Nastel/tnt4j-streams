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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.util.HashMap;
import java.util.Map;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements default activity data parser that assumes each activity data item is an {@link Map} data structure, where
 * each field is represented by a key/value pair and the name is used to map each field into its corresponding activity
 * field.
 * <p>
 * Additionally this parser makes activity data transformation from {@code byte[]} to {@link String}.
 * <p>
 * This activity parser supports configuration properties from
 * {@link com.jkoolcloud.tnt4j.streams.parsers.AbstractActivityMapParser} (and higher hierarchy parsers).
 *
 * @version $Revision: 1 $
 */
public class ActivityMapParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityMapParser.class);

	/**
	 * Constructs a new ActivityMapParser.
	 */
	public ActivityMapParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Casts specified data object to map and applies default activity data transformation from {@code byte[]} to
	 * {@link String}.
	 *
	 * @param data
	 *            activity object data object
	 *
	 * @return activity object data map
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		Map<String, Object> map = null;
		if (data instanceof Map) {
			map = (Map<String, Object>) data;
		} else if (data instanceof Map.Entry) {
			map = makeMap((Map.Entry<?, ?>) data);
		}

		return map;
	}

	/**
	 * Makes map instance from a single map entry.
	 * 
	 * @param mEntry
	 *            map entry to make map
	 * @return map containing provided single entry
	 */
	protected static Map<String, Object> makeMap(Map.Entry<?, ?> mEntry) {
		Map<String, Object> map = new HashMap<>(1);

		map.put(Utils.toString(mEntry.getKey()), mEntry.getValue());

		return map;
	}
}
