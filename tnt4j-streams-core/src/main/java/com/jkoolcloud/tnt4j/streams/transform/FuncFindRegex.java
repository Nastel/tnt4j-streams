/*
 * Copyright 2014-2024 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Data value transformation function finding {@code regex} matching segment within provided {@code text}.
 * <p>
 * Syntax to be use in code: 'ts:findRegex(text, regex, group)' where:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'findRegex' - function name</li>
 * <li>'text' - function argument defining text to find text segment</li>
 * <li>'regex' - function argument defining regular expression to find matching text segment</li>
 * <li>'group' - function argument defining group name to get found segment</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class FuncFindRegex extends AbstractRegexFunction {
	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "findRegex"; // NON-NLS

	/**
	 * Constructs a new replaceRegex() function instance.
	 */
	public FuncFindRegex() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Finds {@code regex} matching segment within provided {@code text}.
	 * <p>
	 * Text can be provided as {@link java.lang.String}, {@link org.w3c.dom.Node} or {@link org.w3c.dom.NodeList} (first
	 * node item containing text).
	 * <p>
	 * Function arguments sequence:
	 * <ul>
	 * <li>1 - text to find text segment. Required.</li>
	 * <li>2 - regex expression to find. Required.</li>
	 * <li>3 - segment match group name. Optional. By default 1st group will be assigned to result</li>
	 * </ul>
	 *
	 * @param args
	 *            function arguments list
	 * @return text string having regex matching sections replaced with provided replacement
	 *
	 * @see #getText(Object)
	 * @see java.util.regex.Matcher#replaceAll(String)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(List args) {
		int argSize = CollectionUtils.size(args);
		Object textParam = argSize > 0 ? args.get(0) : null;
		String regex = argSize > 1 ? (String) args.get(1) : null;
		String group = argSize > 2 ? (String) args.get(2) : null;

		if (textParam == null || StringUtils.isEmpty(regex)) {
			return textParam;
		}

		String text = getText(textParam);

		Matcher m;
		regexLock.lock();
		try {
			Pattern regexPattern = REGEX_MAP.computeIfAbsent(regex, rKey -> Pattern.compile(regex));
			m = regexPattern.matcher(text);
		} finally {
			regexLock.unlock();
		}

		String match = null;
		if (m.find()) {
			match = StringUtils.isEmpty(group) ? m.group(1) : m.group(group);
		}

		return match;
	}
}
