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

package com.jkoolcloud.tnt4j.streams.matchers;

import java.lang.reflect.Method;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Data string value match expression evaluation based on use of {@link org.apache.commons.lang3.StringUtils} provided
 * methods.
 *
 * @version $Revision: 1 $
 *
 * @see org.apache.commons.lang3.StringUtils
 */
public class StringMatcher implements Matcher {

	private static final Method[] SU_METHODS = StringUtils.class.getDeclaredMethods();

	private static StringMatcher instance;

	private StringMatcher() {
	}

	/**
	 * Returns instance of string matcher to be used by {@link com.jkoolcloud.tnt4j.streams.matchers.Matchers} facade.
	 *
	 * @return default instance of string matcher
	 */
	static synchronized StringMatcher getInstance() {
		if (instance == null) {
			instance = new StringMatcher();
		}

		return instance;
	}

	/**
	 * Returns whether this matcher can evaluate expression for data in the specified format.
	 * <p>
	 * This matcher supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this matcher can evaluate expression for data in the specified format, {@code false} -
	 *         otherwise
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof String;
	}

	/**
	 * Checks expression by evaluating {@link StringUtils} class provided methods on data string.
	 *
	 * @param expression
	 *            expression to check - it must contain method name from {@link StringUtils} class, and arguments. First
	 *            method parameter is always {@code data} value, so only subsequent parameters shall be defined in
	 *            expression e.g. {@code "contains(PAYMENT)"}, {@code "isEmpty()"}. To invert evaluation value start
	 *            expression definition with {@code '!'} char.
	 * @param data
	 *            data string to evaluate expression against
	 * @return {@code true} if {@link StringUtils} method returned {@code true} (or in case of compare returns
	 *         {@code 0}), {@code false} - otherwise
	 * 
	 * @throws java.lang.Exception
	 *             if there is no specified method found in {@link org.apache.commons.lang3.StringUtils} class or
	 *             {@code expression} can't be evaluated for other reasons
	 */
	@Override
	public boolean evaluate(String expression, Object data) throws Exception {
		boolean invert = expression.charAt(0) == '!';
		String methodName = expression.substring(invert ? 1 : 0, expression.indexOf("(")); // NON-NLS
		String[] arguments = expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")")).split(","); // NON-NLS
		Object[] convertedArguments = new Object[arguments.length];
		Method method = null;

		boolean hasNoArguments = hasNoArguments(arguments);
		if (hasNoArguments) {
			method = StringUtils.class.getDeclaredMethod(methodName, CharSequence.class);
		} else {
			method = findMatchingMethodAndConvertArgs(methodName, arguments, convertedArguments, SU_METHODS);
		}
		if (method != null) {
			if (hasNoArguments) {
				boolean result = returnBoolean(method.invoke(null, String.valueOf(data)));
				return invert != result;
			} else {
				// Arrays.
				Object[] predefinedArgs = { String.valueOf(data) };
				Object[] allArgs = ArrayUtils.addAll(predefinedArgs, convertedArguments);
				boolean result = returnBoolean(method.invoke(null, allArgs));
				return invert != result;
			}
		} else {
			throw new RuntimeException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StringMatcher.no.such.method", methodName));
		}
	}

	private static Method findMatchingMethodAndConvertArgs(Object methodName, String[] arguments,
			Object[] convertedArguments, Method[] methods) {
		for (Method method : methods) {
			if (method.getName().equals(methodName) && method.getParameterTypes().length == arguments.length + 1) {
				boolean methodMatch = true;
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (int i = 1; i < parameterTypes.length; i++) {
					Class<?> parameterType = parameterTypes[i];
					if (CharSequence.class.isAssignableFrom(parameterType)) {
						convertedArguments[i - 1] = arguments[i - 1];
					} else {
						try {
							if (parameterType.isPrimitive()) {
								parameterType = ClassUtils.primitiveToWrapper(parameterType);
							}
							Method converterMethod = parameterType.getMethod("valueOf", String.class);
							convertedArguments[i - 1] = converterMethod.invoke(null, arguments[i - 1]);
						} catch (Exception e) {
							methodMatch = false;
							break;
						}
					}
				}

				if (methodMatch) {
					return method;
				}
			}
		}
		return null;
	}

	private static boolean returnBoolean(Object result) {
		if (result instanceof Integer) {
			return new Integer(0).equals(result);
		}
		if (result instanceof Boolean) {
			return (boolean) result;
		}
		return false;
	}

	private static boolean hasNoArguments(String[] arguments) {
		return ArrayUtils.isEmpty(arguments) || (arguments.length == 1 && arguments[0].isEmpty());
	}
}
