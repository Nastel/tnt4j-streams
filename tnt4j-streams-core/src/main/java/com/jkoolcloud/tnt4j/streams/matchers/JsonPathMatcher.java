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

package com.jkoolcloud.tnt4j.streams.matchers;

import java.util.Collection;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Data string or {@link com.jayway.jsonpath.DocumentContext} value match expression evaluation based on
 * {@link com.jayway.jsonpath.JsonPath} expressions.
 *
 * @version $Revision: 1 $
 */
public class JsonPathMatcher implements Matcher {

	private static JsonPathMatcher instance;

	private JsonPathMatcher() {
	}

	/**
	 * Returns instance of JSON matcher to be used by {@link com.jkoolcloud.tnt4j.streams.matchers.Matchers} facade.
	 *
	 * @return default instance of JSON matcher
	 */
	static synchronized JsonPathMatcher getInstance() {
		if (instance == null) {
			instance = new JsonPathMatcher();
		}

		return instance;
	}

	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof String || data instanceof DocumentContext;
	}

	/**
	 * Evaluates match {@code expression} against provided {@code data} using JsonPath.
	 *
	 * @param expression
	 *            JsonPath expression to check
	 * @param data
	 *            data {@link String} or {@link com.jayway.jsonpath.DocumentContext} to evaluate expression to
	 * @return {@code true} if expression matches, {@code false} - otherwise
	 * 
	 * @throws com.jayway.jsonpath.JsonPathException
	 *             if any JsonPath parsing/evaluation exception occurs
	 */
	@Override
	public boolean evaluate(String expression, Object data) throws JsonPathException {
		DocumentContext jsonContext = data instanceof DocumentContext ? (DocumentContext) data
				: JsonPath.parse(String.valueOf(data));
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
