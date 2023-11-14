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

package com.jkoolcloud.tnt4j.streams.custom.format.openmetrics;

/**
 * This class provides key/value formatting for tnt4j activities, events and snapshots. The output format follows the
 * following format:
 * <p>
 * {@code "OBJ:object-path\name1=value1,....,object-path\nameN=valueN"}.
 * </p>
 * Newline is added at the end of each line.
 *
 * @version $Revision: 2 $
 *
 * @deprecated this class is left only for backward compatibility. Use
 *             {@link com.jkoolcloud.tnt4j.streams.custom.format.autopilot.FactPathValueFormatter} instead.
 */
@Deprecated
public class FactPathValueFormatter
		extends com.jkoolcloud.tnt4j.streams.custom.format.autopilot.FactPathValueFormatter {

	/**
	 * Constructs a new instance of {@code FactPathValueFormatter}.
	 */
	public FactPathValueFormatter() {
		super();
	}
}
