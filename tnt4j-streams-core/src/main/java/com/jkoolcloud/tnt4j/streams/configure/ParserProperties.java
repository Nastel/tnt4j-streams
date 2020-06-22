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
 * Lists predefined property names used by TNT4-Streams RAW data parsers.
 *
 * @version $Revision: 1 $
 */
public interface ParserProperties {
	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_PATTERN = "Pattern"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_FLD_DELIM = "FieldDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_VAL_DELIM = "ValueDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_STRIP_QUOTES = "StripQuotes"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_REQUIRE_ALL = "RequireDefault"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_STRIP_HEADERS = "StripHeaders"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_NAMESPACE = "Namespace"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 *
	 * @deprecated use {@link #PROP_ACTIVITY_DELIM} instead
	 */
	@Deprecated
	String PROP_READ_LINES = "ReadLines"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_LOC_PATH_DELIM = "LocPathDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_USE_ACTIVITY_DATA_AS_MESSAGE_FOR_UNSET = "UseActivityDataAsMessageForUnset"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_ENTRY_PATTERN = "EntryPattern"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_ACTIVITY_DELIM = "ActivityDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_NAMESPACE_AWARE = "NamespaceAware"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_MATCH_STRATEGY = "MatchStrategy"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_SUPPORTED_CLASS = "SupportedClass"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_COMPOSITE_DELIM = "CompositeStructDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_AUTO_ARRANGE_FIELDS = "AutoArrangeFields"; // NON-NLS
}
