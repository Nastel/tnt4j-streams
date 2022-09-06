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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * This class defines match expressions based condition.
 *
 * @version $Revision: 1 $
 */
public class Condition {
	private String id;
	private Resolution resolution;
	private List<String> matchExpressions = new ArrayList<>();

	/**
	 * Constructs a new Condition. Defines condition name and resolution.
	 * 
	 * @param id
	 *            condition identifier
	 * @param resolution
	 *            condition resolution name
	 * 
	 * @throws java.lang.IllegalArgumentException
	 *             if resolution name is invalid
	 * @throws java.lang.NullPointerException
	 *             if provided resolution is {@code null}
	 */
	public Condition(String id, String resolution) {
		this(id, resolution == null ? null : Resolution.valueOf(resolution.toUpperCase()));
	}

	/**
	 * Constructs a new Condition. Defines condition name and resolution.
	 * 
	 * @param id
	 *            condition identifier
	 * @param resolution
	 *            condition resolution
	 * 
	 * @throws java.lang.NullPointerException
	 *             if provided resolution is {@code null}
	 */
	public Condition(String id, Resolution resolution) {
		if (resolution == null) {
			throw new NullPointerException("Resolution is null"); // NON-NLS
		}

		this.id = id;
		this.resolution = resolution;
	}

	/**
	 * Returns condition identifier.
	 * 
	 * @return condition identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns condition resolution.
	 * 
	 * @return condition resolution
	 */
	public Resolution getResolution() {
		return resolution;
	}

	/**
	 * Sets condition match expressions.
	 * 
	 * @param matchExps
	 *            condition match expressions list
	 * 
	 * @throws java.lang.IllegalArgumentException
	 *             if match expressions list is {@code null} or empty
	 */
	public void setMatchExpressions(List<String> matchExps) {
		if (CollectionUtils.isEmpty(matchExps)) {
			throw new IllegalArgumentException("Condition must have at least one match expression"); // NON-NLS
		}

		matchExpressions.clear();
		matchExpressions.addAll(matchExps);
	}

	/**
	 * Returns condition match expressions list.
	 * 
	 * @return condition match expressions list
	 */
	public List<String> getMatchExpressions() {
		return matchExpressions;
	}

	/**
	 * Checks if condition has no match expressions defined.
	 * 
	 * @return flag indicating condition has no match expressions defined
	 */
	public boolean isEmpty() {
		return CollectionUtils.isEmpty(matchExpressions);
	}

	/**
	 * Condition resolutions enumeration.
	 */
	public enum Resolution {
		/**
		 * When condition matches expression - this resolution indicates to stop stream.
		 */
		STOP,
		/**
		 * When condition matches expression - this resolution indicates to skip request.
		 */
		SKIP
	}
}
