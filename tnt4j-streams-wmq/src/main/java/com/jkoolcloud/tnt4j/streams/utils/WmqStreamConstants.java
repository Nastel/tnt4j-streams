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

package com.jkoolcloud.tnt4j.streams.utils;

/**
 * TNT4J-Streams "Wmq" module constants.
 *
 * @version $Revision: 1 $
 */
public final class WmqStreamConstants {

	/**
	 * Resource bundle name constant for TNT4J-Streams "wmq" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-wmq"; // NON-NLS

	/**
	 * Custom PCF parameter identifier to store PCF message contained group parameters (MQCFGR) count.
	 */
	public static final int GROUPS_COUNT = 919191919;
	/**
	 * Custom PCF parameter identifier to store processed PCF message group parameter (MQCFGR) index.
	 */
	public static final int GROUP_MARKER = 929292929;
	/**
	 * Custom PCF parameter identifier to store {@link com.ibm.mq.MQMessage} MQMD header values.
	 */
	public static final int PCF_MQMD_HEADER = 932100000;

	/**
	 * Field "value type" attribute value identifying to initialize WMQ message signature calculation.
	 */
	public static final String VT_SIGNATURE = "signature"; // NON-NLS

	/**
	 * PUT_DATE attribute value format pattern.
	 */
	public static final String PUT_DATE_PATTERN = "yyyyMMdd"; // NON-NLS
	/**
	 * PUT_TIME attribute value format pattern.
	 */
	public static final String PUT_TIME_PATTERN = "HHmmssSS"; // NON-NLS
	/**
	 * PUT_DATE and PUT_TIME attributes concatenated value format pattern.
	 */
	public static final String PUT_DATE_TIME_PATTERN = PUT_DATE_PATTERN + " " + PUT_TIME_PATTERN; // NON-NLS

	private WmqStreamConstants() {
	}
}
