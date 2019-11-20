/*
 * Copyright 2014-2018 JKOOL, LLC.
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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.quartz.DateBuilder;

/**
 * Base class defining TNT4J-Streams-WS scheduler configuration data.
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractSchedulerData implements SchedulerData {
	private Integer startDelay = null;
	private DateBuilder.IntervalUnit startDelayUnits;

	/**
	 * Returns scheduler start delay interval.
	 * 
	 * @return scheduler start delay interval
	 */
	public Integer getStartDelay() {
		return startDelay;
	}

	/**
	 * Sets scheduler start delay interval.
	 *
	 * @param startDelay
	 *            scheduler start delay interval
	 * @param tUnits
	 *            start delay time units name
	 */
	public void setStartDelay(Integer startDelay, String tUnits) {
		this.startDelay = startDelay;
		setStartDelayUnits(tUnits);
	}

	/**
	 * Sets scheduler start delay interval.
	 *
	 * @param startDelay
	 *            scheduler start delay interval
	 * @param tUnits
	 *            start delay time units
	 */
	public void setStartDelay(Integer startDelay, TimeUnit tUnits) {
		this.startDelay = startDelay;
		setStartDelayUnits(tUnits == null ? null : tUnits.name());
	}

	/**
	 * Returns start delay time units.
	 *
	 * @return start delay time units
	 */
	public DateBuilder.IntervalUnit getStartDelayUnits() {
		return startDelayUnits == null ? DateBuilder.IntervalUnit.SECOND : startDelayUnits;
	}

	/**
	 * Sets start delay time units.
	 *
	 * @param startDelayUnitsName
	 *            start delay time units name
	 */
	protected void setStartDelayUnits(String startDelayUnitsName) {
		if (StringUtils.length(startDelayUnitsName) > 1 && startDelayUnitsName.toUpperCase().endsWith("S")) { // NON-NLS
			startDelayUnitsName = startDelayUnitsName.substring(0, startDelayUnitsName.length() - 1);
		}

		this.startDelayUnits = StringUtils.isEmpty(startDelayUnitsName) ? DateBuilder.IntervalUnit.SECOND
				: DateBuilder.IntervalUnit.valueOf(startDelayUnitsName.toUpperCase());
	}

	/**
	 * Returns date/time value when to start schedule trigger.
	 *
	 * @return date/time value when to start schedule trigger
	 */
	public Date getStartAt() {
		return startDelay == null ? new Date() : DateBuilder.futureDate(startDelay, getStartDelayUnits());
	}
}
