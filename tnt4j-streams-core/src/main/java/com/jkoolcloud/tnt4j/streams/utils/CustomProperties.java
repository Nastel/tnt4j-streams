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

import java.util.HashMap;

/**
 * Provides custom properties definition map for streams, parsers and etch streaming entities.
 *
 * @param <V>
 *            the type of properties values
 *
 * @version $Revision: 1 $
 */
public class CustomProperties<V> extends HashMap<String, V> {
	private static final long serialVersionUID = -6360951538934181832L;

	/**
	 * The constant defining custom stream/parser property prefix. Custom properties use is upon individual needs -
	 * streams and parsers don't use them directly. It can be used to enrich streaming/parsing context.
	 */
	public static final String CONTEXT_PROPERTY_PREFIX = "ctx:"; // NON-NLS

	private final String prefix;

	/**
	 * Constructs a new CustomProperties instance.
	 */
	public CustomProperties() {
		this(CONTEXT_PROPERTY_PREFIX);
	}

	/**
	 * Constructs a new CustomProperties instance.
	 *
	 * @param initialCapacity
	 *            the initial capacity of properties map
	 */
	public CustomProperties(int initialCapacity) {
		this(CONTEXT_PROPERTY_PREFIX, initialCapacity);
	}

	/**
	 * Constructs a new CustomProperties instance.
	 * 
	 * @param prefix
	 *            the prefix for properties stored by this map
	 */
	public CustomProperties(String prefix) {
		super();

		this.prefix = prefix;
	}

	/**
	 * Constructs a new CustomProperties instance.
	 *
	 * @param prefix
	 *            the prefix for properties stored by this map
	 * @param initialCapacity
	 *            the initial capacity of properties map
	 */
	public CustomProperties(String prefix, int initialCapacity) {
		super(initialCapacity);

		this.prefix = prefix;
	}

	/**
	 * Checks if provided property {@code name} is prefixed by {@code prefix} set for this properties map.
	 * 
	 * @param name
	 *            the property name
	 * @return {@code true} if property name is prefixed by {@code prefix} set for this properties map and property name
	 *         is longer than prefix length, {@code false} - otherwise
	 */
	public boolean isPropertyNameForThisMap(String name) {
		return isPrefixedPropertyName(name, prefix);
	}

	/**
	 * Checks if provided property {@code name} is prefixed by {@value CONTEXT_PROPERTY_PREFIX}.
	 *
	 * @param name
	 *            the property name
	 *
	 * @return {@code true} if property name is prefixed by {@value CONTEXT_PROPERTY_PREFIX} and property name is longer
	 *         than prefix length, {@code false} - otherwise
	 */
	public static boolean isCustomPropertyName(String name) {
		return isPrefixedPropertyName(name, CONTEXT_PROPERTY_PREFIX);
	}

	/**
	 * Checks if provided property {@code name} is prefixed by {@code prefix} value.
	 *
	 * @param name
	 *            the property name
	 * @param prefix
	 *            the prefix value
	 *
	 * @return {@code true} if property name is prefixed by {@code prefix} and property name is longer than
	 *         {@code prefix} length, {@code false} - otherwise
	 */
	public static boolean isPrefixedPropertyName(String name, String prefix) {
		return name.toLowerCase().startsWith(prefix) && name.length() > prefix.length();
	}

	/**
	 * Sets property value.
	 *
	 * @param name
	 *            the property name
	 * @param value
	 *            the new value for the property
	 *
	 * @return the previous value of the property
	 */
	public V setProperty(String name, V value) {
		return put(name.substring(CONTEXT_PROPERTY_PREFIX.length()).toLowerCase(), value);
	}

	/**
	 * Returns property value.
	 * <p>
	 * NOTE: When provided property {@code name} is without {@value CONTEXT_PROPERTY_PREFIX} prefix, be sure property
	 * name does not match property name from any other scope. Such case may result unexpected value to be resolved.
	 *
	 * @param name
	 *            the property name, can be prefixed by {@value CONTEXT_PROPERTY_PREFIX} or without prefix
	 *
	 * @return the property value
	 */
	public V getProperty(String name) {
		if (name.toLowerCase().startsWith(CONTEXT_PROPERTY_PREFIX)) {
			name = name.substring(CONTEXT_PROPERTY_PREFIX.length());
		}

		return get(name.toLowerCase());
	}

}
