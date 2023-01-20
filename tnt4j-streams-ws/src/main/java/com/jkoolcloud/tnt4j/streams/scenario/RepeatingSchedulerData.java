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

package com.jkoolcloud.tnt4j.streams.scenario;

/**
 * Base class defining TNT4J-Streams-WS repeating scheduler configuration data.
 *
 * @version $Revision: 1 $
 */
public abstract class RepeatingSchedulerData extends AbstractSchedulerData {
	private Integer repeatCount = -1;

	/**
	 * Returns invocations count.
	 *
	 * @return invocations count
	 */
	public Integer getRepeatCount() {
		return repeatCount;
	}

	/**
	 * Sets invocations count.
	 *
	 * @param repeatCount
	 *            invocations count
	 */
	public void setRepeatCount(Integer repeatCount) {
		this.repeatCount = repeatCount;
	}
}
