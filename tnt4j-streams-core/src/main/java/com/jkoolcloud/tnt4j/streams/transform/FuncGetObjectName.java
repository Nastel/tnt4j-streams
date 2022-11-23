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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Data value transformation function resolving object name from provided fully qualified object name.
 * <p>
 * Syntax to be use in code: 'ts:getObjectName(objectFQN, options)' were:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'getObjectName' - function name</li>
 * <li>'objectFQN' - function argument defining fully qualified object name</li>
 * <li>'options' - object name resolution options:
 * <ul>
 * <li>resolution options: DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL. Optional.</li>
 * <li>search symbols. Optional.</li>
 * <li>replacement symbols. Optional</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class FuncGetObjectName extends AbstractFunction<String> {
	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "getObjectName"; // NON-NLS

	/**
	 * Constructs a new getObjectName() function instance.
	 */
	public FuncGetObjectName() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Resolves desired object name from provided fully qualified object name. Fully qualified object name can be
	 * provided as {@link java.lang.String}, {@link org.w3c.dom.Node} or {@link org.w3c.dom.NodeList} (first node item
	 * containing object name).
	 * <p>
	 * Function arguments sequence:
	 * <ul>
	 * <li>1 - fully qualified object name. Required.</li>
	 * <li>2 - resolution options: DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL. Optional.</li>
	 * <li>3 - search symbols. Optional.</li>
	 * <li>4 - replacement symbols. Optional</li>
	 * </ul>
	 *
	 * @param args
	 *            function arguments list
	 * @return object name resolved form provided fully qualified object name
	 *
	 * @see org.w3c.dom.Node
	 * @see org.w3c.dom.NodeList
	 * @see Utils#resolveObjectName(String, String...)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(List args) {
		Object param = CollectionUtils.isEmpty(args) ? null : args.get(0);

		if (param == null) {
			return param;
		}

		String objectFQN = null;
		if (param instanceof String) {
			objectFQN = (String) param;
		} else if (param instanceof Node) {
			objectFQN = ((Node) param).getTextContent();
		} else if (param instanceof NodeList) {
			NodeList nodes = (NodeList) param;

			if (nodes.getLength() > 0) {
				Node node = nodes.item(0);
				objectFQN = node.getTextContent();
			}
		}

		return StringUtils.trim(Utils.resolveObjectName(objectFQN, toArray(args)));
	}

	private static String[] toArray(List<?> args) {
		if (args == null) {
			return null;
		}

		String[] strings = new String[args.size() - 1];
		for (int i = 1; i < args.size(); i++) {
			strings[i - 1] = (String) args.get(i);
		}

		return strings;
	}
}
