/*
 * Copyright 2014-2020 JKOOL, LLC.
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

import org.apache.commons.collections4.MapUtils;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class implements common parsers processed activity data entity pack having raw activity data and additional
 * activity data associated metadata map.
 * 
 * @param <T>
 *            the type of raw activity data
 *
 * @version $Revision: 1 $
 */
public class CommonActivityData<T> implements ActivityData<T> {
	private T data;
	private Map<String, ?> metaData;

	/**
	 * Constructs a new CommonActivityData.
	 * 
	 * @param data
	 *            raw activity data to parse
	 */
	public CommonActivityData(T data) {
		this(data, null);
	}

	/**
	 * Constructs a new CommonActivityData.
	 * 
	 * @param data
	 *            raw activity data to parse
	 * @param metaData
	 *            activity associated metadata
	 */
	public CommonActivityData(T data, Map<String, ?> metaData) {
		this.data = data;
		this.metaData = metaData;
	}

	@Override
	public void setData(T data) {
		this.data = data;
	}

	@Override
	public T getData() {
		return data;
	}

	@Override
	public boolean hasMetadata() {
		return MapUtils.isNotEmpty(metaData);
	}

	@Override
	public Map<String, ?> getMetadata() {
		return metaData;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CommonActivityData{"); // NON-NLS
		sb.append("data=").append(Utils.toString(data)); // NON-NLS
		sb.append(", metadata=").append(metaData.toString()); // NON-NLS
		sb.append('}'); // NON-NLS
		return sb.toString();
	}
}
