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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathVariableResolver;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;

/**
 * Data value transformation based on XPath function expressions.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils#getStreamsXPath()
 * @see XPathVariableResolver
 * @see javax.xml.xpath.XPathFunctionResolver
 * @see javax.xml.namespace.NamespaceContext
 * @see javax.xml.xpath.XPathFunction
 * @see XPath#evaluate(String, Object)
 */
public class XPathTransformation extends AbstractScriptTransformation<Object> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(XPathTransformation.class);

	private static final String OWN_FIELD_VALUE_KEY = "<!TNT4J_XPATH_TRSF_FLD_VALUE!>"; // NON-NLS;
	private static final String OWN_FIELD_NAME_KEY = "<!TNT4J_XPATH_TRSF_FLD_NAME!>"; // NON-NLS;

	/**
	 * Constructs a new XPathTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            XPath expression code
	 */
	public XPathTransformation(String name, String scriptCode) {
		super(name, scriptCode);

		initTransformation();
	}

	/**
	 * Constructs a new XPathTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            XPath expression code
	 * @param phase
	 *            activity data value resolution phase
	 */
	public XPathTransformation(String name, String scriptCode, Phase phase) {
		super(name, scriptCode, phase);

		initTransformation();
	}

	@Override
	protected EventSink getLogger() {
		return LOGGER;
	}

	@Override
	protected String getHandledLanguage() {
		return StreamsScriptingUtils.XPATH_SCRIPT_LANG;
	}

	@Override
	public Object transform(Object value, ActivityInfo ai, String fieldName) throws TransformationException {
		Map<String, Object> valuesMap = new HashMap<>();
		valuesMap.put(OWN_FIELD_VALUE_KEY, value);
		valuesMap.put(OWN_FIELD_NAME_KEY, fieldName);

		if (ai != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, ai);

				valuesMap.put(eKV.getKey(), eKV.getValue());
			}
		}

		XPath xPath = StreamsXMLUtils.getStreamsXPath();
		xPath.setXPathVariableResolver(new StreamsVariableResolver(valuesMap));

		try {
			Object tValue = xPath.evaluate(getExpression(), (Object) null);

			logEvaluationResult(valuesMap, tValue);

			return tValue;
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName(), getPhase()), exc);
		}
	}

	private static class StreamsVariableResolver implements XPathVariableResolver {
		private Map<String, Object> valuesMap;

		private StreamsVariableResolver(Map<String, Object> valuesMap) {
			this.valuesMap = valuesMap;
		}

		@Override
		public Object resolveVariable(QName variableName) {
			Object varValue;
			if (variableName.equals(new QName(StreamsScriptingUtils.FIELD_VALUE_VARIABLE_NAME))) {
				varValue = valuesMap.get(OWN_FIELD_VALUE_KEY);
			} else if (variableName.equals(new QName(StreamsScriptingUtils.FIELD_NAME_VARIABLE_NAME))) {
				varValue = valuesMap.get(OWN_FIELD_NAME_KEY);
			} else {
				String varNameStr = "$" + variableName.toString(); // NON-NLS

				varValue = valuesMap.get(varNameStr);
			}

			return varValue == null ? "" : varValue;
		}
	}
}
