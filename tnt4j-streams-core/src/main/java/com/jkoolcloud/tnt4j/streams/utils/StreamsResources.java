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

package com.jkoolcloud.tnt4j.streams.utils;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class to support I18N of TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class StreamsResources {
	/**
	 * Resource bundle name constant for TNT4J-Streams "core" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-core"; // NON-NLS

	private static final ConcurrentMap<String, ResourceBundle> resBundlesMap = new ConcurrentHashMap<>(8);

	private StreamsResources() {
	}

	/**
	 * Initializes singleton instance of resource bundle for default locale if such is not available in bundles cache
	 * map. After initialization bundle object is placed into cache map and retrieved from it on subsequent calls. Cache
	 * map entry key is {@code bundleName}.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @return default locale bound resource bundle
	 * @see ResourceBundle#getBundle(String)
	 */
	private static ResourceBundle getBundleBase(String bundleName) {
		ResourceBundle resBundle = resBundlesMap.get(bundleName);
		if (resBundle == null) {
			resBundle = ResourceBundle.getBundle(bundleName);
			resBundlesMap.putIfAbsent(bundleName, resBundle);
		}
		return resBundle;
	}

	/**
	 * Initializes singleton instance of resource bundle for default locale if such is not available in bundles cache
	 * map. After initialization bundle object is placed into cache map and retrieved from it on subsequent calls. Cache
	 * map entry key is {@code bundleName}.
	 * <p>
	 * If no bundle is associated with given {@code bundleName}, then bundle associated with
	 * {@value RESOURCE_BUNDLE_NAME} is returned.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @return default locale bound resource bundle
	 *
	 * @see #getBundleBase(String)
	 */
	public static ResourceBundle getBundle(String bundleName) {
		try {
			return getBundleBase(bundleName);
		} catch (RuntimeException exc) {
			return getBundleBase(RESOURCE_BUNDLE_NAME);
		}
	}

	private static String getResourceString(String bundleName, String key) {
		try {
			return getBundleBase(bundleName).getString(key);
		} catch (MissingResourceException mre) {
			return getBundleBase(RESOURCE_BUNDLE_NAME).getString(key);
		}
	}

	/**
	 * Returns localized string for the given key.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @param key
	 *            localized string key
	 * @return localized string
	 */
	public static String getString(String bundleName, String key) {
		if (key == null) {
			return null;
		}

		try {
			return getResourceString(bundleName, key);
		} catch (MissingResourceException mre) {
			return key;
		}
	}

	/**
	 * Returns formatted localized string for the given key.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @param key
	 *            localized string key
	 * @param args
	 *            formatted string parameters
	 * @return localized string
	 * @see MessageFormat#format(String, Object...)
	 */
	public static String getStringFormatted(String bundleName, String key, Object... args) {
		if (key == null) {
			return null;
		}

		try {
			return Utils.format(getResourceString(bundleName, key), args);
		} catch (MissingResourceException mre) {
			return key;
		}
	}

	/**
	 * Returns localized string for the given enum constant key.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @param key
	 *            localized string key
	 * @return localized string for enum constant
	 */
	public static String getString(String bundleName, Enum<?> key) {
		if (key == null) {
			return null;
		}

		try {
			return getResourceString(bundleName, String.format("%s.%s", key.getClass().getName(), key.name())); // NON-NLS
		} catch (MissingResourceException mre) {
			return key.name();
		}
	}

}
