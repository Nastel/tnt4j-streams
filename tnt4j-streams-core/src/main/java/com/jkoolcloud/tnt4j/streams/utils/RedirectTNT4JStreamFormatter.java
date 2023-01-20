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

package com.jkoolcloud.tnt4j.streams.utils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream;

import net.minidev.json.JSONValue;

/**
 * JSON formatter extension used with {@link RedirectTNT4JStream} to redirect incoming trackable objects
 * (activities/events/snapshots) produced by other TNT4J based sources like 'tnt4j-stream-jmx' to jKoolCloud.
 * <p>
 * If object to be formatted is valid JSON, then no additional formatting is performed. JSON validity is determined by
 * invoking {@link JSONValue#isValidJson(String)}.
 *
 * @version $Revision: 1 $
 *
 * @see RedirectTNT4JStream
 * @see JSONValue#isValidJson(String)
 */
public class RedirectTNT4JStreamFormatter extends JSONFormatter {

	/**
	 * Creates a new RedirectTNT4JStreamFormatter without newlines during formatting
	 */
	public RedirectTNT4JStreamFormatter() {
		super();
	}

	/**
	 * Creates a new RedirectTNT4JStreamFormatter and conditionally format with newline
	 * 
	 * @param newLine
	 *            apply newline formatting to JSON
	 */
	public RedirectTNT4JStreamFormatter(boolean newLine) {
		super(newLine);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If {@code o} is valid JSON then no additional formatting is performed.
	 */
	@Override
	public String format(Object o, Object... objects) {
		if (o instanceof String && isJsonValid((String) o)) {
			return String.valueOf(o);
		}

		return super.format(o, objects);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If {@code msg} is valid JSON then no additional formatting is performed.
	 */
	@Override
	public String format(long ttl, Source source, OpLevel level, String msg, Object... args) {
		if (isJsonValid(msg)) {
			return msg;
		}

		return super.format(ttl, source, level, msg, args);
	}

	private boolean isJsonValid(String json) {
		boolean valid = JSONValue.isValidJson(json);

		return valid;
	}
}