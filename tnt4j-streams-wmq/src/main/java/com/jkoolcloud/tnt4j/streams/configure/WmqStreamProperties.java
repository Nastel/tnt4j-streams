/*
 * Copyright 2014-2019 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * Lists predefined property names used by TNT4-Streams WMQ input streams.
 *
 * @version $Revision: 1 $
 */
public interface WmqStreamProperties extends StreamProperties {

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_QMGR_NAME = "QueueManager"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_SUB_NAME = "Subscription"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_CHANNEL_NAME = "Channel"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_STRIP_HEADERS = "StripHeaders"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TRACE_OPERATIONS = "TraceOperations"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_EXCLUDED_REASON_CODES = "ExcludedRC"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_SUPPRESS_BROWSE_GETS = "SuppressBrowseGets"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String OPEN_OPTIONS = "OpenOptions"; // NON-NLS

}
