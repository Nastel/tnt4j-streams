/*
 * Copyright 2014-2017 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.preparsers;

/**
 * This interface defines common operations for RAW activity data pre-parsers.
 *
 * @param <O>
 *            type of converted activity data
 *
 * @version $Revision: 1 $
 */
public interface ActivityDataPreParser<O> {
	/**
	 * Converts RAW activity data to format activity data parser can handle.
	 *
	 * @param data
	 *            RAW activity data package
	 * @return converted activity data package
	 * @throws Exception
	 *             if RAW activity data pre-parsing fails
	 *
	 * @see com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#preParse(com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream,
	 *      Object)
	 */
	O preParse(Object data) throws Exception;

	/**
	 * Returns whether this pre-parser supports the given format of the RAW activity data. This is used by activity
	 * parsers to determine if the pre-parser can process RAW activity data in the format that stream provides.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this pre-parser can process data in the specified format, {@code false} - otherwise
	 */
	boolean isDataClassSupported(Object data);

	/**
	 * Returns type of converted activity data entries.
	 *
	 * @return type of converted activity data entries
	 */
	String dataTypeReturned();
}
