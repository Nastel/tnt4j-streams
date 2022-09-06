/*
 * Copyright 2014-2022 JKOOL, LLC.
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
 * List the supported raw field value data types.
 *
 * @version $Revision: 1 $
 */
public enum ActivityFieldDataType {
	/**
	 * Raw data value is interpreted as a character string.
	 */
	String,

	/**
	 * Raw data value is interpreted as a numeric value, optionally in a particular format.
	 */
	Number,

	/**
	 * Raw data value is interpreted as a sequence of bytes.
	 */
	Binary,

	/**
	 * Raw data value is interpreted as a date, time, or date/time expression, optionally in a particular format.
	 */
	DateTime,

	/**
	 * Raw data value is interpreted as a numeric timestamp, optionally in the specified units (assumed to be in
	 * milliseconds if not specified).
	 */
	Timestamp,

	/**
	 * Raw data value is interpreted as a generic value and streams should try to predict real value out of it: boolean,
	 * number, timestamp, date, string.
	 */
	Generic,

	/**
	 * Raw data value is interpreted as it is received from input without changes.
	 */
	AsInput
}
