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

package com.jkoolcloud.tnt4j.streams.scenario;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

/**
 * This class defines TNT4J-Streams-WS configuration simple scheduler configuration data.
 *
 * @version $Revision: 1 $
 */
public class SimpleSchedulerData extends RepeatingSchedulerData {
	private long interval = 0;
	private TimeUnit units;

	/**
	 * Constructs a new SimpleSchedulerData. Defines invocations interval in provided time units.
	 *
	 * @param interval
	 *            request/call/command invocations interval
	 * @param units
	 *            interval time units
	 */
	public SimpleSchedulerData(long interval, TimeUnit units) {
		this.interval = interval;
		this.units = units;
	}

	/**
	 * Constructs a new SimpleSchedulerData. Defines invocations interval in provided time units.
	 *
	 * @param interval
	 *            request/call/command invocations interval
	 * @param unitsName
	 *            interval time units name
	 */
	public SimpleSchedulerData(long interval, String unitsName) {
		this.interval = interval;
		setUnits(unitsName);
	}

	/**
	 * Returns request/call/command invocations interval.
	 *
	 * @return request /call/command invocations interval
	 */
	public long getInterval() {
		return interval;
	}

	/**
	 * Returns invocation interval time units.
	 *
	 * @return interval time units
	 */
	public TimeUnit getUnits() {
		return units == null ? TimeUnit.SECONDS : units;
	}

	/**
	 * Sets invocation interval time units.
	 *
	 * @param units
	 *            interval time units.
	 */
	public void setUnits(TimeUnit units) {
		this.units = units;
	}

	/**
	 * Sets invocation interval time units.
	 *
	 * @param unitsName
	 *            interval time units name
	 */
	public void setUnits(String unitsName) {
		this.units = StringUtils.isEmpty(unitsName) ? TimeUnit.SECONDS : TimeUnit.valueOf(unitsName.toUpperCase());
	}
}
