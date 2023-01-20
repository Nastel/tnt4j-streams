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

package com.jkoolcloud.tnt4j.streams.outputs;

import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * A streaming output notifications listener interface. This interface can be implemented by classes that are interested
 * in streaming output process progress and status changes.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.outputs.AbstractJKCloudOutput#addOutputListener(OutputStreamListener)
 */
public interface OutputStreamListener {

	/**
	 * This method gets called when activity item has been entered logging procedure.
	 *
	 * @param stream
	 *            stream logging activity item
	 * @param item
	 *            logged activity item
	 */
	void onItemLogStart(TNTInputStream<?, ?> stream, Object item);

	/**
	 * This method gets called when activity item logging procedure has been completed.
	 *
	 * @param item
	 *            logged activity item
	 */
	void onItemLogFinish(Object item);

	/**
	 * This method gets called when activity item entity (itself or child) has been sent over tracker/logger.
	 *
	 * @param item
	 *            recorded activity item
	 * @param trackable
	 *            recorded trackable instance
	 */
	void onItemRecorded(Object item, Trackable trackable);

}
