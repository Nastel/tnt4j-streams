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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Base class for abstract Regular Expressions based data value transformations.
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractRegexFunction extends AbstractFunction<String> {

	/**
	 * Compiled regular expressions map.
	 */
	protected static final Map<String, Pattern> REGEX_MAP = new HashMap<>();

	/**
	 * Regular Expression retrieval from map/compilation lock.
	 */
	protected Lock regexLock = new ReentrantLock();
}
