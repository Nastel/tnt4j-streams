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

package com.jkoolcloud.tnt4j.streams.parsers.data;

import java.util.Map;

/**
 * This interface defines basic parsers processed activity data entity pack having raw activity data and additional
 * activity data associated metadata map.
 * 
 * @param <T>
 *            the type of raw activity data
 *
 * @version $Revision: 1 $
 */
public interface ActivityData<T> {
	/**
	 * Sets RAW activity data to parse.
	 * 
	 * @param data
	 *            raw activity data to parse
	 */
	void setData(T data);

	/**
	 * Returns RAW activity data to parse.
	 * 
	 * @return raw activity data to parse
	 */
	T getData();

	/**
	 * Checks if this parsed activity data pack has some metadata entries.
	 * 
	 * @return {@code true} if this activity data pack has some metadata entries, {@code false} - otherwise
	 */
	boolean hasMetadata();

	/**
	 * Returns metadata map for this parsed activity data pack.
	 * 
	 * @return metadata map for this parsed activity data pack
	 */
	Map<String, ?> getMetadata();
}
