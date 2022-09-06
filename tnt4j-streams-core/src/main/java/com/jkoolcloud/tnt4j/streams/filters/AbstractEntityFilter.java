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

package com.jkoolcloud.tnt4j.streams.filters;

/**
 * Base class for streamed entity data value filter.
 * 
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.filters.DefaultValueFilter
 * @see com.jkoolcloud.tnt4j.streams.filters.AbstractExpressionFilter
 */
public abstract class AbstractEntityFilter<T> implements StreamEntityFilter<T> {

	/**
	 * Checks if filtered value is "filtered out" depending on filter handle type and match flag value.
	 * 
	 * @param ht
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} value
	 * @param match
	 *            flag indicating whether value was matching filter
	 * @return {@code true} if match and exclude or does not match and include
	 */
	protected static boolean isFilteredOut(HandleType ht, boolean match) {
		return match ? ht == HandleType.EXCLUDE : ht == HandleType.INCLUDE;
	}

	/**
	 * Returns filter type.
	 * 
	 * @return filter type
	 */
	public abstract StreamFilterType getFilterType();

	/**
	 * Performs filter initialization.
	 */
	protected abstract void initFilter();

	/**
	 * Returns filter handle type.
	 *
	 * @return filter handle type
	 */
	public abstract HandleType getHandleType();
}
