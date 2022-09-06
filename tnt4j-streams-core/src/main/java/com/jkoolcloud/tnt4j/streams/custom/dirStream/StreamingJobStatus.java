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

package com.jkoolcloud.tnt4j.streams.custom.dirStream;

import com.jkoolcloud.tnt4j.streams.inputs.StreamingStatus;

/**
 * Defines supported streaming job status values.
 *
 * @version $Revision: 1 $
 */
public enum StreamingJobStatus implements StreamingStatus {
	/**
	 * Indicates streaming job was rejected while adding to executor queue.
	 */
	REJECT,

	/**
	 * Indicates streaming jobs were dropped off from executor queue before executing (e.g., executor was shot down
	 * before starting to process that job).
	 */
	DROP_OFF,
}
