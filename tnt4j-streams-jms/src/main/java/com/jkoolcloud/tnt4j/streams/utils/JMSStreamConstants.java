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

package com.jkoolcloud.tnt4j.streams.utils;

/**
 * TNT4J-Streams "JMS" module constants.
 *
 * @version $Revision: 1 $
 */
public final class JMSStreamConstants {
	/**
	 * Resource bundle name constant for TNT4J-Streams "jms" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-jms"; // NON-NLS

	/**
	 * The constant to indicate activity transport is JMS.
	 */
	public static final String TRANSPORT_JMS = "JMS"; // NON-NLS

	/**
	 * The constant to indicate default map key for JMS message metadata values map.
	 */
	public static final String MSG_METADATA_KEY = "MsgMetadata"; // NON-NLS

	private JMSStreamConstants() {
	}
}
