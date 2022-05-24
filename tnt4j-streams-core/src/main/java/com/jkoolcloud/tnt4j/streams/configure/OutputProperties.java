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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * Lists predefined property names used by TNT4-Streams outputs.
 *
 * @version $Revision: 1 $
 */
public interface OutputProperties {
	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TNT4J_CONFIG_FILE = "TNT4JConfigFile"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TNT4J_PROPERTY = "TNT4JProperty"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RESOLVE_SERVER = "ResolveServerFromDNS"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 *
	 * @deprecated use {@link #PROP_SPLIT_RELATIVES} instead
	 */
	@Deprecated
	String PROP_TURN_OUT_CHILDREN = "TurnOutActivityChildren"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	@Deprecated
	String PROP_SPLIT_RELATIVES = "SplitRelatives"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_BUILD_FQN_FROM_DATA = "BuildSourceFQNFromStreamedData"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TNT4J_CONFIG_ZK_NODE = "TNT4JConfigZKNode"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_SOURCE_FQN = "SourceFQN"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RETRY_STATE_CHECK = "RetryStateCheck"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RETRY_PERIOD = "RetryPeriod"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property. Alias for {@link #PROP_RETRY_PERIOD}.
	 */
	String PROP_RETRY_INTERVAL = "RetryInterval"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_SEND_STREAM_STATES = "SendStreamStates"; // NON-NLS
}
