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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.transform.beans.Strings;

/**
 * Data value transformation function replacing {@code regex} matching segment within provided {@code text} with
 * provided {@code replacement}.
 * <p>
 * Syntax to be use in code: 'ts:replaceRegex(text, regex, replacement)' where:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'replaceRegex' - function name</li>
 * <li>'text' - function argument defining text to do replacements</li>
 * <li>'regex' - function argument defining regular expression to match text segments to be replaced</li>
 * <li>'replacement' - function argument defining replacement string</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class FuncReplaceRegex extends AbstractRegexFunction {
	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "replaceRegex"; // NON-NLS

	/**
	 * Constructs a new replaceRegex() function instance.
	 */
	public FuncReplaceRegex() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Replaces {@code regex} matching segment within provided {@code text} with provided {@code replacement}.
	 * <p>
	 * Text can be provided as {@link java.lang.String}, {@link org.w3c.dom.Node} or {@link org.w3c.dom.NodeList} (first
	 * node item containing text).
	 * <p>
	 * Function arguments sequence:
	 * <ul>
	 * <li>1 - text to do replacements. Required.</li>
	 * <li>2 - regex expression to match. Required.</li>
	 * <li>3 - replacement string. Optional - default is {@code ""}.</li>
	 * </ul>
	 *
	 * @param args
	 *            function arguments list
	 * @return text string having regex matching sections replaced with provided replacement
	 *
	 * @see #getText(Object)
	 * @see Strings#replaceRegex(String, String, String)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(List args) {
		int argSize = CollectionUtils.size(args);
		Object textParam = argSize > 0 ? args.get(0) : null;
		String regex = argSize > 1 ? (String) args.get(1) : null;
		String replacement = argSize > 2 ? (String) args.get(2) : "";

		if (textParam == null || StringUtils.isEmpty(regex)) {
			return textParam;
		}

		String text = getText(textParam);

		return Strings.replaceRegex(text, regex, replacement);
	}
}
