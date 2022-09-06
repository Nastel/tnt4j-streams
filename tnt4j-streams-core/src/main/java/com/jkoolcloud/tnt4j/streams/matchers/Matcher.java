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

package com.jkoolcloud.tnt4j.streams.matchers;

/**
 * This interface defines common operations for data value matching expression evaluations used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public interface Matcher {
	/**
	 * Evaluates match {@code expression} against provided {@code data}.
	 *
	 * @param expression
	 *            match evaluation expression
	 * @param data
	 *            data to evaluate expression
	 * @return {@code true} if {@code data} matches {@code expression}, {@code false} - otherwise
	 * 
	 * @throws Exception
	 *             if evaluation of matcher expression fails
	 */
	boolean evaluate(String expression, Object data) throws Exception;

	/**
	 * Returns whether this matcher can evaluate expression for data in the specified format.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this matcher can evaluate expression for data in the specified format, {@code false} -
	 *         otherwise
	 */
	boolean isDataClassSupported(Object data);
}
