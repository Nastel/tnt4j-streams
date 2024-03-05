/*
 * Copyright 2014-2024 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Data value transformation function resolving file name from provided file path.
 * <p>
 * Syntax to be use in code: 'ts:getFileName(filePath)' where:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'getFileName' - function name</li>
 * <li>'filePath' - function argument defining file path</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class FuncGetFileName extends AbstractFunction<String> {

	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "getFileName"; // NON-NLS

	/**
	 * Constructs a new getFileName() function instance.
	 */
	public FuncGetFileName() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Resolves file name from provided file path.
	 * <p>
	 * File path can be provided as {@link java.lang.String}, {@link org.w3c.dom.Node} or {@link org.w3c.dom.NodeList}
	 * (first node item containing file path).
	 *
	 * @param args
	 *            arguments list containing file path as first item
	 * @return file name resolved from provided path
	 *
	 * @see #getText(Object)
	 * @see Utils#getFileName(String)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(List args) {
		Object param = CollectionUtils.isEmpty(args) ? null : args.get(0);

		if (param == null) {
			return param;
		}

		String filePath = getText(param);

		return StringUtils.trim(Utils.getFileName(filePath));
	}
}
