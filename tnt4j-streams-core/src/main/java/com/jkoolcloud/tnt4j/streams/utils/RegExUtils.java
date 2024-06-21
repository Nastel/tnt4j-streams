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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * General Regular Expression (RegEx) utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class RegExUtils {

	private static final Pattern GROUP_NAME_PATTERN = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

	private RegExUtils() {
	}

	/**
	 * Retrieves a list of named capturing group names from a provided {@link Pattern}.
	 *
	 * @param pattern
	 *            the compiled Pattern object
	 * @param excludedNames
	 *            excluded group names
	 *
	 * @return list of named capturing group names
	 */
	public static List<String> getNamedGroups(Pattern pattern, String... excludedNames) {
		List<String> namedGroups = new ArrayList<>();
		// Get the named group names from the pattern
		String regex = pattern.pattern();
		Matcher matcher = GROUP_NAME_PATTERN.matcher(regex);
		while (matcher.find()) {
			String gName = matcher.group(1);
			if (!StringUtils.equalsAny(gName, excludedNames)) {
				namedGroups.add(gName);
			}
		}
		return namedGroups;
	}
}
