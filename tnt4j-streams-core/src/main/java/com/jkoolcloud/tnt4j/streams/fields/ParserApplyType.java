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

package com.jkoolcloud.tnt4j.streams.fields;

/**
 * List the supported stacked parser application phase types.
 *
 * @version $Revision: 1 $
 */
public enum ParserApplyType {
	/**
	 * Apply stacked parser on field resolved value.
	 */
	Field,

	/**
	 * Apply stacked parser on activity entity stored value.
	 */
	Activity
}
