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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathFunction;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract XPath function based data value transformation.
 *
 * @param <V>
 *            the type of transformed data value
 *
 * @version $Revision: 2 $
 */
public abstract class AbstractFunction<V> extends AbstractValueTransformation<V, Object> implements XPathFunction {

	/**
	 * Transforms data by evaluating function on provided data value.
	 *
	 * @param value
	 *            data value to transform
	 * @param context
	 *            transformation context map containing references to activity info, field, parser, stream and etc.
	 * @return transformed value
	 *
	 * @throws com.jkoolcloud.tnt4j.streams.transform.TransformationException
	 *             if function evaluation fails
	 */
	@Override
	public Object transform(V value, Map<String, ?> context) throws TransformationException {
		try {
			return evaluate(Collections.singletonList(value));
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName(), getPhase()), exc);
		}
	}

	/**
	 * Returns text content obtained from provided parameter instance.
	 * 
	 * @param param
	 *            parameter instance to obtain text content
	 * @return text content obtained from provided parameter
	 */
	protected static String getText(Object param) {
		String testStr = null;
		if (param instanceof String) {
			testStr = (String) param;
		} else if (param instanceof Node) {
			testStr = ((Node) param).getTextContent();
		} else if (param instanceof NodeList) {
			NodeList nodes = (NodeList) param;

			if (nodes.getLength() > 0) {
				Node node = nodes.item(0);
				testStr = node.getTextContent();
			}
		}

		return testStr;
	}

	/**
	 * Translates provided arguments list into string array, starting from defined list element index {@code startIdx}.
	 * 
	 * @param args
	 *            arguments list
	 * @param startIdx
	 *            starting argument index
	 * @return argument strings array
	 */
	protected static String[] toArray(List<?> args, int startIdx) {
		if (args == null) {
			return null;
		}

		String[] strings = new String[args.size() - startIdx];
		for (int i = startIdx; i < args.size(); i++) {
			strings[i - startIdx] = Utils.toString(args.get(i));
		}

		return strings;
	}
}
