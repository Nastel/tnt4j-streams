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

import java.util.regex.Pattern;

/**
 * TNT4J-Streams constants.
 *
 * @version $Revision: 1 $
 */
public final class StreamsConstants {

	/**
	 * The constant to indicate default map key for topic definition.
	 */
	public static final String TOPIC_KEY = "ActivityTopic"; // NON-NLS
	/**
	 * The constant to indicate default map key for activity data definition.
	 */
	public static final String ACTIVITY_DATA_KEY = "ActivityData"; // NON-NLS
	/**
	 * The constant to indicate default map key for activity transport definition.
	 */
	public static final String TRANSPORT_KEY = "ActivityTransport"; // NON-NLS

	/**
	 * The constant to indicate activity transport is HTTP.
	 */
	public static final String TRANSPORT_HTTP = "Http"; // NON-NLS

	/**
	 * Default object identification path delimiter used by streams parsers.
	 */
	public static final String DEFAULT_PATH_DELIM = "."; // NON-NLS

	/**
	 * The constant for locator path node token meaning complete map.
	 */
	public static final String MAP_NODE_TOKEN = "*"; // NON-NLS

	/**
	 * The constant for locator path node token meaning map entries not mapped manually.
	 */
	public static final String MAP_UNMAPPED_TOKEN = "#"; // NON-NLS

	/**
	 * Property name tokens delimiter for properties having multilevel map value.
	 */
	public static final String MAP_PROP_NAME_TOKENS_DELIM = "_"; // NON-NLS

	/**
	 * The constant defining string delimiting multiple values of stream entity configuration properties having same
	 * name: e.g.: Namespace, ConfRegexMapping.
	 */
	public static final String MULTI_PROPS_DELIMITER = "&|@"; // NON-NLS

	/**
	 * The constant defining key for streams cache entry containing stream events grouping ACTIVITY identifier, to be
	 * used as streamed entities parent identifier.
	 */
	public static final String STREAM_GROUPING_ACTIVITY_ID_CACHE_KEY = ".GroupingActivityId"; // NON-NLS

	/**
	 * The constant defining {@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity} type locator
	 * prefix to resolve field value of parent activity entity.
	 */
	public static final String PARENT_REFERENCE_PREFIX = "^.";// NON-NLS

	/**
	 * The constant defining value placeholder to set child entity ordinal index within parent entity.
	 */
	public static final String CHILD_ORDINAL_INDEX = "$indexOf$"; // NON-NLS

	private StreamsConstants() {
	}

	/**
	 * Checks if field reference refers parent activity entity field. Parent activity entity references starts with
	 * {@value #PARENT_REFERENCE_PREFIX} prefix.
	 * 
	 * @param fRef
	 *            reference string to check
	 * @return {@code true} if reference string starts with {@value #PARENT_REFERENCE_PREFIX}, {@code false} - otherwise
	 */
	public static boolean isParentEntityRef(String fRef) {
		return fRef.startsWith(PARENT_REFERENCE_PREFIX);
	}

	/**
	 * Removed parent activity entity reference prefix from provided field reference.
	 * 
	 * @param fieldName
	 *            field name
	 * @return field name without parent activity entity reference prefix
	 */
	public static String getParentFieldName(String fieldName) {
		return fieldName.startsWith(PARENT_REFERENCE_PREFIX) ? fieldName.substring(PARENT_REFERENCE_PREFIX.length())
				: fieldName;
	}

	/**
	 * Splits string containing multiple property definitions to individual properties array.
	 * 
	 * @param pValue
	 *            string defining multiple properties
	 * @return array of individual properties definitions
	 */
	public static String[] getMultiProperties(String pValue) {
		return pValue.split(Pattern.quote(MULTI_PROPS_DELIMITER));
	}
}
