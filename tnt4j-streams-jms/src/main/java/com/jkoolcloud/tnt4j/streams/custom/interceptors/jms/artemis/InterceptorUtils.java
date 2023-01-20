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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

import io.netty.channel.ChannelId;

/**
 * General utility methods used by TNT4J-Streams-JMS Artemis interceptors.
 *
 * @version $Revision: 1 $
 */
public class InterceptorUtils {

	private static final int DEFAULT_DEPTH = 10;

	private InterceptorUtils() {
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}. Default drill down depth
	 * level is {@code 10}.
	 * <p>
	 * Produced map contains all object properties values at top level. Entry key is appended with property name at
	 * certain object value level and delimited by {@code ":"}.
	 *
	 * @param key
	 *            object properties key
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 *
	 * @see #toMapFlat(String, Object, java.util.Map,int)
	 */
	public static void toMapFlat(String key, Object obj, Map<String, Object> propsMap)
			throws IllegalArgumentException, Exception {
		toMapFlat(key, obj, propsMap, DEFAULT_DEPTH);
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}.
	 * <p>
	 * Produced map contains all object properties values at top level. Entry key is appended with property name at
	 * certain object value level and delimited by {@code ":"}.
	 *
	 * @param key
	 *            object properties key
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 * @param depthLevel
	 *            max depth level of drill down into object properties
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 *
	 * @see #toMapFlat(Object, java.util.Map, String, int, String...)
	 */
	public static void toMapFlat(String key, Object obj, Map<String, Object> propsMap, int depthLevel)
			throws IllegalArgumentException, Exception {
		toMapFlat(obj, propsMap, key, depthLevel);
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}. Default drill down depth
	 * level is {@code 10}.
	 * <p>
	 * Produced map contains all object properties values at top level. Entry key is appended with property name at
	 * certain object value level and delimited by {@code ":"}.
	 *
	 * @param key
	 *            object properties key
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 * @param excludes
	 *            property names to exclude from drilling down
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 *
	 * @see #toMapFlat(Object, java.util.Map, String, int, String...)
	 */
	public static void toMapFlat(String key, Object obj, Map<String, Object> propsMap, String... excludes)
			throws IllegalArgumentException, Exception {
		toMapFlat(obj, propsMap, key, DEFAULT_DEPTH, excludes);
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}.
	 * <p>
	 * Produced map contains all object properties values at top level. Entry key is appended with property name at
	 * certain object value level and delimited by {@code ":"}.
	 *
	 * @param key
	 *            object properties key
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 * @param depthLevel
	 *            max depth level of drill down into object properties
	 * @param excludes
	 *            property names to exclude from drilling down
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 *
	 * @see #toMapFlat(Object, java.util.Map, String, int, String...)
	 */
	public static void toMapFlat(String key, Object obj, Map<String, Object> propsMap, int depthLevel,
			String... excludes) throws IllegalArgumentException, Exception {
		toMapFlat(obj, propsMap, key, depthLevel, excludes);
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}.
	 * <p>
	 * If property value is complex object, then properties collection for that object is performed recursively by
	 * reducing drill down level by {@code 1} and making property name key prefix from current prefix and property name.
	 * <p>
	 * Produced map contains all object properties values at top level. Entry key is appended with property name at
	 * certain object value level and delimited by {@code ":"}.
	 *
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 * @param key
	 *            object properties key
	 * @param depthLevel
	 *            max depth level of drill down into object properties
	 * @param excludes
	 *            property names to exclude from drilling down
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 */
	public static void toMapFlat(Object obj, Map<String, Object> propsMap, String key, int depthLevel,
			String... excludes) throws IllegalArgumentException, Exception {
		toMap(InterceptionsManager.MapConstructionStrategy.FLAT, obj, propsMap, key, depthLevel, excludes);
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}. Default drill down depth
	 * level is {@code 10}.
	 * <p>
	 * Produced map entries may contain sub-maps for complex property values.
	 *
	 * @param key
	 *            object properties key
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 * @param excludes
	 *            property names to exclude from drilling down
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 *
	 * @see #toMapDeep(Object, java.util.Map, String, int, String...)
	 */
	public static void toMapDeep(String key, Object obj, Map<String, Object> propsMap, String... excludes)
			throws IllegalArgumentException, Exception {
		toMapDeep(obj, propsMap, key, DEFAULT_DEPTH, excludes);
	}

	/**
	 * Collects provided object {@code obj} properties into provided map {@code propsMap}.
	 * <p>
	 * If property value is complex object, then properties collection for that object is performed recursively by
	 * reducing drill down level by {@code 1} and making dedicated properties map for that value object.
	 * <p>
	 * Produced map entries may contain sub-maps for complex property values.
	 *
	 * @param obj
	 *            the object to collect properties
	 * @param propsMap
	 *            map instance to put object properties into
	 * @param key
	 *            object properties key
	 * @param depthLevel
	 *            max depth level of drill down into object properties
	 * @param excludes
	 *            property names to exclude from drilling down
	 *
	 * @throws IllegalArgumentException
	 *             if property name key prefix gets longer than {@code 256} symbols
	 * @throws Exception
	 *             if properties collection fails because of unexpected reasons
	 */
	public static void toMapDeep(Object obj, Map<String, Object> propsMap, String key, int depthLevel,
			String... excludes) throws IllegalArgumentException, Exception {
		toMap(InterceptionsManager.MapConstructionStrategy.DEEP, obj, propsMap, key, depthLevel, excludes);
	}

	private static void toMap(InterceptionsManager.MapConstructionStrategy mapStrategy, Object obj,
			Map<String, Object> propsMap, String key, int depthLevel, String... excludes)
			throws IllegalArgumentException, Exception {
		Map<String, Object> objPropsMap;
		if (mapStrategy == InterceptionsManager.MapConstructionStrategy.FLAT) {
			if (StringUtils.length(key) > 256) {
				throw new IllegalArgumentException("Map entry key got too long:" + key); // NON-NLS
			}
			objPropsMap = propsMap;
		} else {
			objPropsMap = new LinkedHashMap<>();
			propsMap.put(key, objPropsMap);
		}

		String mapKey = mapKey(mapStrategy, key, "class"); // NON-NLS
		objPropsMap.put(mapKey, obj.getClass().getName()); // NON-NLS
		PropertyDescriptor[] pDescriptors = PropertyUtils.getPropertyDescriptors(obj);
		for (PropertyDescriptor descriptor : pDescriptors) {
			String name = descriptor.getName();
			if (descriptor.getReadMethod() != null) {
				mapKey = mapKey(mapStrategy, key, name);
				try {
					Object pValue = PropertyUtils.getProperty(obj, name);
					if (depthLevel > 0 && isDrillableObject(pValue) && !excluded(name, excludes)) {
						toMap(mapStrategy, pValue, objPropsMap, mapKey, depthLevel - 1, excludes);
					} else {
						objPropsMap.put(mapKey, unwrapValue(pValue));
					}
				} catch (InvocationTargetException exc) {
					Throwable rExc = ExceptionUtils.getRootCause(exc);
					if (rExc instanceof IllegalStateException && rExc.getMessage().contains("encoded/decoded yet")) { // NON-NLS
						objPropsMap.put(mapKey, -1);
					} else {
						objPropsMap.put(mapKey, "FAILED: " + Utils.getExceptionMessages(exc)); // NON-NLS
					}
				} catch (Throwable exc) {
					objPropsMap.put(mapKey, "FAILED: " + Utils.getExceptionMessages(exc)); // NON-NLS
				}
			}
		}

		// return PropertyUtils.describe(obj);
	}

	private static String mapKey(InterceptionsManager.MapConstructionStrategy mapStrategy, String prefix,
			String pName) {
		return mapStrategy == InterceptionsManager.MapConstructionStrategy.FLAT ? prefix + ":" + pName : pName; // NON-NLS
	}

	private static boolean isDrillableObject(Object obj) {
		return obj != null //
				&& !(obj instanceof Class) //
				&& !ClassUtils.isPrimitiveOrWrapper(obj.getClass()) //
				&& !ClassUtils.isAssignable(obj.getClass(), String.class) //
				&& !obj.getClass().isEnum() //
				&& !obj.getClass().isArray() //
				&& !ClassUtils.isAssignable(obj.getClass(), Collection.class) //
				&& !ClassUtils.isAssignable(obj.getClass(), Map.class) //
				&& !isStringWrapper(obj);
	}

	private static boolean isStringWrapper(Object obj) {
		return obj instanceof UUID //
				|| obj instanceof org.apache.activemq.artemis.utils.UUID //
				|| obj instanceof ChannelId;
	}

	private static Object unwrapValue(Object val) {
		if (isStringWrapper(val)) {
			return val.toString();
		}

		return val;
	}

	private static boolean excluded(String name, String... excludes) {
		if (excludes != null && name != null) {
			for (String exclude : excludes) {
				if (name.equals(exclude)) {
					return true;
				}
			}
		}

		return false;
	}

	// public static void toMap(String keyPrefix, Object obj, Map<String, Object> tMap) throws Exception {
	// putAllFromTo(keyPrefix, toMap(obj), tMap);
	// }
	//
	// public static void putAllFromTo(String keyPrefix, Map<String, ?> sMap, Map<String, Object> tMap) {
	// for (Map.Entry<String, ?> ppe : sMap.entrySet()) {
	// tMap.put(keyPrefix + ppe.getKey(), ppe.getValue());
	// }
	// }
}
