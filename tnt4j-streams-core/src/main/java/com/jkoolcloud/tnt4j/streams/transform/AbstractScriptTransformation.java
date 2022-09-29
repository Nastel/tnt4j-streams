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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract script based data value transformation.
 *
 * @param <V>
 *            the type of transformed data value
 *
 * @version $Revision: 2 $
 *
 * @see ScriptTransformation
 * @see XPathTransformation
 */
public abstract class AbstractScriptTransformation<V> extends AbstractValueTransformation<V, Object> {

	private String scriptCode;

	/**
	 * Set for variables of transformation expression contained activity fields.
	 */
	protected Set<String> exprVars;
	/**
	 * Pre-processed transformation expression.
	 */
	protected String ppExpression;
	/**
	 * Map for variable placeholders of transformation expression contained activity fields.
	 */
	protected Map<String, String> placeHoldersMap;

	/**
	 * Constructs a new AbstractScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 */
	protected AbstractScriptTransformation(String name, String scriptCode) {
		this(name, scriptCode, null);
	}

	/**
	 * Constructs a new AbstractScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 * @param phase
	 *            activity data value resolution phase
	 */
	protected AbstractScriptTransformation(String name, String scriptCode, Phase phase) {
		this.scriptCode = scriptCode;
		setName(StringUtils.isEmpty(name) ? scriptCode : name);
		setPhase(phase);
	}

	/**
	 * Returns logger used by this transformation.
	 *
	 * @return transformation logger
	 */
	protected abstract EventSink getLogger();

	/**
	 * Returns transformation used expressions evaluation language descriptor string.
	 *
	 * @return expressions evaluation language descriptor string
	 */
	protected abstract String getHandledLanguage();

	/**
	 * Returns transformation script code string.
	 *
	 * @return transformation script code
	 */
	public String getScriptCode() {
		return scriptCode;
	}

	/**
	 * Returns transformation expression code string.
	 *
	 * @return transformation expression
	 */
	public String getExpression() {
		return StringUtils.isEmpty(ppExpression) ? scriptCode : ppExpression;
	}

	/**
	 * Creates transformation instance based on provided parameters.
	 *
	 * @param name
	 *            transformation name
	 * @param lang
	 *            scripting/expression language:
	 *            {@value com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#GROOVY_LANG},
	 *            {@value com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#JAVA_SCRIPT_LANG} ('js', 'jscript')
	 *            or {@value com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#XPATH_SCRIPT_LANG}
	 * @param code
	 *            transformation script code
	 * @param phaseName
	 *            data resolution phase name, when to apply transformation:
	 *            '{@link com.jkoolcloud.tnt4j.streams.transform.ValueTransformation.Phase#RAW}',
	 *            '{@link com.jkoolcloud.tnt4j.streams.transform.ValueTransformation.Phase#FORMATTED}' (default) or
	 *            '{@link com.jkoolcloud.tnt4j.streams.transform.ValueTransformation.Phase#AGGREGATED}'
	 * @return created transformation instance
	 *
	 * @throws IllegalArgumentException
	 *             if transformation can not be created for provided language
	 */
	public static ValueTransformation<Object, Object> createScriptTransformation(String name, String lang, String code,
			String phaseName) throws IllegalArgumentException {
		if (StringUtils.isEmpty(lang)) {
			lang = StreamsScriptingUtils.JAVA_SCRIPT_LANG;
		}

		Phase phase = null;
		if (StringUtils.isEmpty(phaseName)) {
			phase = Phase.FORMATTED;
		} else {
			try {
				phase = Phase.valueOf(phaseName.toUpperCase());
			} catch (Exception exc) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ScriptTransformation.unknown.phase", phase));
			}
		}

		if (StreamsScriptingUtils.GROOVY_LANG.equalsIgnoreCase(lang)) {
			return new ScriptTransformation(StreamsScriptingUtils.GROOVY_LANG, name, code, phase);
		} else if (StringUtils.equalsAnyIgnoreCase(lang, StreamsScriptingUtils.JAVA_SCRIPT_LANG, "js", "jscript")) { // NON-NLS
			return new ScriptTransformation(StreamsScriptingUtils.JAVA_SCRIPT_LANG, name, code, phase);
		} else if (StreamsScriptingUtils.XPATH_SCRIPT_LANG.equalsIgnoreCase(lang)) {
			return new XPathTransformation(name, code, phase);
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"ScriptTransformation.unknown.language", lang));
	}

	/**
	 * Performs transformation initialization: resolves script defined expression variables.
	 */
	protected void initTransformation() {
		exprVars = new HashSet<>();
		placeHoldersMap = new HashMap<>();
		Utils.resolveExpressionVariables(exprVars, scriptCode);

		String expString = scriptCode;
		if (CollectionUtils.isNotEmpty(exprVars)) {
			String varPlh;
			int idx = 0;
			for (String eVar : exprVars) {
				varPlh = "$TNT4J_ST_TRSF_PLH" + (idx++); // NON-NLS
				expString = expString.replace(eVar, varPlh);
				placeHoldersMap.put(eVar, varPlh);
			}
		}

		ppExpression = expString;
	}

	/**
	 * Checks if transformation has references to activity entity fields.
	 * 
	 * @return {@code true} if transformation expression contains activity entity field name variables, {@code false} -
	 *         otherwise
	 */
	@Override
	public boolean hasActivityReferences() {
		return Utils.isVariableExpression(scriptCode);
	}

	/**
	 * Returns set of expressions variable placeholders - usually names of referenced activity entity fields.
	 *
	 * @return set of expressions variable placeholders
	 */
	public Set<String> getExpressionVariables() {
		return exprVars;
	}

	/**
	 * Resolves activity entity field value for expression variable defined field name.
	 *
	 * @param eVar
	 *            expression variable containing field name
	 * @param activityInfo
	 *            activity entity instance to resolve field value
	 * @return resolved activity entity field value
	 */
	protected Property resolveFieldKeyAndValue(String eVar, ActivityInfo activityInfo) {
		Object fValue = activityInfo.getFieldValue(eVar);
		String fieldName = placeHoldersMap.get(eVar);

		return new Property(StringUtils.isEmpty(fieldName) ? eVar : fieldName, fValue);
	}

	/**
	 * Logs transformation expression evaluation result.
	 * <p>
	 * Log entry is build only if logger log level {@link com.jkoolcloud.tnt4j.core.OpLevel#TRACE} is set.
	 *
	 * @param varsMap
	 *            variables binding map
	 * @param tValue
	 *            transformation evaluation result
	 *
	 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#describeExpression(String, java.util.Map, String,
	 *      java.util.Collection, java.util.Map)
	 */
	protected void logEvaluationResult(Map<String, Object> varsMap, Object tValue) {
		LoggerUtils.log(getLogger(), OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ScriptTransformation.evaluation.result",
				() -> Utils.args(StreamsScriptingUtils.describeExpression(scriptCode, varsMap, getHandledLanguage(),
						exprVars, placeHoldersMap), Utils.toString(tValue)));
	}
}
