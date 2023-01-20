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

import javax.xml.xpath.XPathFunction;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Base class for abstract XPath function based data value transformation.
 *
 * @param <V>
 *            the type of transformed data value
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractFunction<V> extends AbstractValueTransformation<V, Object> implements XPathFunction {

	/**
	 * Transforms data by evaluating function on provided data value.
	 *
	 * @param value
	 *            data value to transform
	 * @param ai
	 *            activity entity instance
	 * @param fieldName
	 *            name of field performing transformation
	 * @return transformed value
	 *
	 * @throws com.jkoolcloud.tnt4j.streams.transform.TransformationException
	 *             if function evaluation fails
	 */
	@Override
	public Object transform(V value, ActivityInfo ai, String fieldName) throws TransformationException {
		try {
			return evaluate(Collections.singletonList(value));
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName(), getPhase()), exc);
		}
	}
}
