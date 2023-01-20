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
 * Defines the supported raw activity data formats.
 *
 * @version $Revision: 1 $
 */
public enum ActivityFieldFormatType {
	/**
	 * Field value is base64-encoded.
	 */
	base64Binary,

	/**
	 * Field value is represented as a string of hexadecimal codes.
	 */
	hexBinary,

	/**
	 * Field value is a character string.
	 */
	string,

	/**
	 * Field value is a byte array
	 */
	bytes
}
