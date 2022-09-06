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

package com.jkoolcloud.tnt4j.streams.preparsers;

import com.jkoolcloud.tnt4j.streams.configure.NamedObject;

/**
 * This interface defines common operations for RAW activity data pre-parsers.
 *
 * @param <V>
 *            the type of activity data to convert
 * @param <O>
 *            type of converted activity data
 *
 * @version $Revision: 1 $
 */
public interface ActivityDataPreParser<V, O> extends NamedObject {
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
	O preParse(V data) throws Exception;

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
	 * Returns "logical" type of converted activity data entries.
	 *
	 * @return "logical" type of converted activity data entries, or {@code "OBJECT"}/{@code null} if there is no
	 *         particular type
	 */
	String dataTypeReturned();

	/**
	 * Returns flag indicating if pre-parser uses parent parser to read initial data from stream.
	 * 
	 * @return {@code true} if pre-parser uses parent parser to read data from stream, {@code false} - if pre-parser
	 *         uses own stream provided data reading
	 */
	boolean isUsingParserForInput();
}
