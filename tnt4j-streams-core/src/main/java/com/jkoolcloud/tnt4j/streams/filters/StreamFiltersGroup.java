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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * Stream entities data (activity/field/locator) filters group.
 * <p>
 * Group containing multiple filters defined applies them sequentially and stops processing when founds first value
 * matching filter having handle type {@link HandleType#EXCLUDE}.
 *
 * @version $Revision: 1 $
 */
public class StreamFiltersGroup<T> implements StreamEntityFilter<T> {
	private String name;

	private List<AbstractEntityFilter<T>> activityFilters = new ArrayList<>();
	private boolean filteredByDefault = false;

	/**
	 * Constructs a new StreamFiltersGroup.
	 *
	 * @param name
	 *            filters group name
	 */
	public StreamFiltersGroup(String name) {
		this.name = name;
	}

	/**
	 * Returns name of filters group
	 *
	 * @return name of filters group
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds filter to this group.
	 *
	 * @param filter
	 *            filter instance to be added to group
	 */
	public void addFilter(AbstractEntityFilter<T> filter) {
		if (filter.getHandleType() == HandleType.INCLUDE) {
			// if the filter group contains include filter not matching should be excluded
			filteredByDefault = true;
		}
		activityFilters.add(filter);
	}

	/**
	 * Removes filter from this group.
	 * 
	 * @param filter
	 *            filter instance to be removed from group
	 */
	public void removeFilter(AbstractEntityFilter<T> filter) {
		activityFilters.remove(filter);
	}

	/**
	 * Returns group filters list.
	 *
	 * @return group filters list
	 */
	public List<AbstractEntityFilter<T>> getFilters() {
		return activityFilters;
	}

	@Override
	public boolean doFilter(T value, ActivityInfo ai, String fName) throws FilterException {
		if (CollectionUtils.isNotEmpty(activityFilters)) {
			for (AbstractEntityFilter<T> aFilter : activityFilters) {
				boolean filtered;
				try {
					filtered = aFilter.doFilter(value, ai, fName);
				} catch (Throwable e) {
					filtered = filteredByDefault;
				}
				if (filtered) {
					if (aFilter.getHandleType() == HandleType.EXCLUDE) {
						return true;
					}
				} else {
					if (aFilter.getHandleType() == HandleType.INCLUDE) {
						return false;
					}
				}
			}
		}

		return filteredByDefault;
	}

	@Override
	public boolean doFilter(Map<String, ?> valBindings) throws FilterException {
		if (CollectionUtils.isNotEmpty(activityFilters)) {
			for (AbstractEntityFilter<T> aFilter : activityFilters) {
				boolean filtered;
				try {
					filtered = aFilter.doFilter(valBindings);
				} catch (Throwable e) {
					filtered = filteredByDefault;
				}
				if (filtered) {
					if (aFilter.getHandleType() == HandleType.EXCLUDE) {
						return true;
					}
				} else {
					if (aFilter.getHandleType() == HandleType.INCLUDE) {
						return false;
					}
				}
			}
		}

		return filteredByDefault;
	}

}
