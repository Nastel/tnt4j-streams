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

package com.jkoolcloud.tnt4j.streams.matchers;

import java.util.Collection;
import java.util.Map;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Data string, {@link com.jayway.jsonpath.DocumentContext} or {@link Map} value match expression evaluation based on
 * {@link com.jayway.jsonpath.JsonPath} expressions.
 *
 * @version $Revision: 2 $
 */
public class JsonPathMatcher implements Matcher {

	private static JsonPathMatcher instance;

	private JsonPathMatcher() {
	}

	/**
	 * Returns instance of JSONPath matcher to be used by {@link com.jkoolcloud.tnt4j.streams.matchers.Matchers} facade.
	 *
	 * @return default instance of JSONPath matcher
	 */
	static synchronized JsonPathMatcher getInstance() {
		if (instance == null) {
			instance = new JsonPathMatcher();
		}

		return instance;
	}

	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof String || data instanceof DocumentContext || data instanceof Map;
	}

	/**
	 * Evaluates match {@code expression} against provided {@code data} using JsonPath.
	 *
	 * @param expression
	 *            JsonPath expression to check
	 * @param data
	 *            data {@link String}, {@link com.jayway.jsonpath.DocumentContext} or {@link Map} to evaluate expression
	 * @return {@code true} if expression matches, {@code false} - otherwise
	 * 
	 * @throws com.jayway.jsonpath.JsonPathException
	 *             if any JsonPath parsing/evaluation exception occurs
	 */
	@Override
	public boolean evaluate(String expression, Object data) throws JsonPathException {
		DocumentContext jsonContext;

		if (data instanceof DocumentContext) {
			jsonContext = (DocumentContext) data;
		} else if (data instanceof Map) {
			jsonContext = JsonPath.parse(data);
		} else {
			jsonContext = JsonPath.parse(String.valueOf(data));
		}
		try {
			Object val = jsonContext.read(expression);

			if (val instanceof Boolean) {
				return (Boolean) val;
			}
			if (val instanceof Collection) {
				return !((Collection<?>) val).isEmpty();
			}
			return val != null;
		} catch (PathNotFoundException exc) {
			return false;
		}
	}
}
