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

package com.jkoolcloud.tnt4j.streams.filters;

/**
 * Provides list of stream entity filters value evaluation types.
 *
 * @version $Revision: 1 $
 */
public enum EvaluationType {
	/**
	 * Value is same as filtered object.
	 */
	IS,

	/**
	 * Value is contained within filtered object.
	 */
	CONTAINS,

	/**
	 * Value matching using wildcards.
	 */
	WILDCARD,

	/**
	 * Value matching using regular expressions.
	 */
	REGEX
}
