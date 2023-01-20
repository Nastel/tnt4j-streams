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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Data string value match expression evaluation based on Regular Expressions (RegEx).
 *
 * @version $Revision: 1 $
 */
public class RegExMatcher implements Matcher {

	private static RegExMatcher instance;

	private static final Map<String, Pattern> patternsMap = new HashMap<>(5);

	private RegExMatcher() {
	}

	static synchronized RegExMatcher getInstance() {
		if (instance == null) {
			instance = new RegExMatcher();
		}

		return instance;
	}

	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof String;
	}

	/**
	 * Evaluates match {@code expression} against provided {@code data} using RegEx.
	 *
	 * @param expression
	 *            Regex expression to check
	 * @param data
	 *            data {@link String} to evaluate expression to
	 * @return {@code true} if expression matches, {@code false} - otherwise
	 * 
	 * @throws java.util.regex.PatternSyntaxException
	 *             if the {@code expression}'s syntax is invalid
	 */
	@Override
	public boolean evaluate(String expression, Object data) throws PatternSyntaxException {
		Pattern pattern = patternsMap.get(expression);
		if (pattern == null) {
			pattern = Pattern.compile(expression);
			patternsMap.put(expression, pattern);
		}
		java.util.regex.Matcher matcher = pattern.matcher(String.valueOf(data));
		return matcher.find();
	}
}
