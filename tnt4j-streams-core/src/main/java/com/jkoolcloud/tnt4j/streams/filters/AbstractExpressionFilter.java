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

package com.jkoolcloud.tnt4j.streams.filters;

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
 * Base class for abstract expression based data value filter.
 *
 * @param <T>
 *            the type of filtered data value
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.filters.ScriptExpressionFilter
 * @see com.jkoolcloud.tnt4j.streams.filters.XPathExpressionFilter
 */
public abstract class AbstractExpressionFilter<T> extends AbstractEntityFilter<T> {

	/**
	 * Filter expression string.
	 */
	protected String filterExpression;
	private HandleType handleType;

	/**
	 * Set for variables of filter expression contained activity fields.
	 */
	protected Set<String> exprVars;
	/**
	 * Pre-processed filter expression.
	 */
	protected String ppExpression;
	/**
	 * Map for variable placeholders of filter expression contained activity fields.
	 */
	protected Map<String, String> placeHoldersMap;

	/**
	 * Constructs a new AbstractExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	protected AbstractExpressionFilter(String filterExpression) {
		this(null, filterExpression);
	}

	/**
	 * Constructs a new AbstractExpressionFilter.
	 * 
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	protected AbstractExpressionFilter(String handleType, String filterExpression) {
		this.handleType = StringUtils.isEmpty(handleType) ? HandleType.INCLUDE
				: HandleType.valueOf(handleType.toUpperCase());

		if (StringUtils.isEmpty(filterExpression)) {
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "ExpressionFilter.empty.expression"));
		}

		this.filterExpression = filterExpression;
	}

	/**
	 * Returns logger used by this filter.
	 *
	 * @return filter logger
	 */
	protected abstract EventSink getLogger();

	/**
	 * Returns filter used expressions evaluation language descriptor string.
	 *
	 * @return expressions evaluation language descriptor string
	 */
	protected abstract String getHandledLanguage();

	/**
	 * Returns filter expression string.
	 * 
	 * @return filter expressions string
	 */
	protected String getExpression() {
		return StringUtils.isEmpty(ppExpression) ? filterExpression : ppExpression;
	}

	@Override
	public HandleType getHandleType() {
		return handleType;
	}

	@Override
	protected void initFilter() {
		exprVars = new HashSet<>();
		placeHoldersMap = new HashMap<>();
		Utils.resolveExpressionVariables(exprVars, filterExpression);

		String expString = filterExpression;
		if (CollectionUtils.isNotEmpty(exprVars)) {
			String varPlh;
			int idx = 0;
			for (String eVar : exprVars) {
				varPlh = "$TNT4J_ST_FLTR_PLH" + (idx++); // NON-NLS
				expString = expString.replace(eVar, varPlh);
				placeHoldersMap.put(eVar, varPlh);
			}
		}

		ppExpression = expString;
	}

	/**
	 * Returns set of expressions variable placeholders - usually names of referenced activity entity fields.
	 *
	 * @return set of expressions variable placeholders
	 */
	public Set<String> getExpressionVariables() {
		return exprVars;
	}

	@Override
	public StreamFilterType getFilterType() {
		return StreamFilterType.EXPRESSION;
	}

	/**
	 * Creates expression filter instance based on provided parameters.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param lang
	 *            scripting/expression language:
	 *            {@value com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#GROOVY_LANG},
	 *            {@value com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#JAVA_SCRIPT_LANG} ('js', 'jscript')
	 *            or {@value com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#XPATH_SCRIPT_LANG}
	 * @param expression
	 *            filter expression string
	 * @return created expression filter instance
	 *
	 * @throws IllegalArgumentException
	 *             if filter can not be created for provided language
	 */
	public static AbstractExpressionFilter<Object> createExpressionFilter(String handleType, String lang,
			String expression) throws IllegalArgumentException {
		if (StringUtils.isEmpty(lang)) {
			lang = StreamsScriptingUtils.JAVA_SCRIPT_LANG;
		}

		if (StreamsScriptingUtils.GROOVY_LANG.equalsIgnoreCase(lang)) {
			return new ScriptExpressionFilter(StreamsScriptingUtils.GROOVY_LANG, handleType, expression);
		} else if (StringUtils.equalsAnyIgnoreCase(lang, StreamsScriptingUtils.JAVA_SCRIPT_LANG, "js", "jscript")) { // NON-NLS
			return new ScriptExpressionFilter(StreamsScriptingUtils.JAVA_SCRIPT_LANG, handleType, expression);
		} else if (StreamsScriptingUtils.XPATH_SCRIPT_LANG.equalsIgnoreCase(lang)) {
			return new XPathExpressionFilter(handleType, expression);
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"ExpressionFilter.unknown.language", lang));
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
	 * Resolves value bound for expression variable.
	 * 
	 * @param eVar
	 *            expression variable
	 * @param valBindings
	 *            expression variable and value bindings map
	 * @return resolved expression variable bound value
	 */
	protected Property resolveFieldKeyAndValue(String eVar, Map<String, ?> valBindings) {
		Object fValue = valBindings.get(eVar);
		String fieldName = placeHoldersMap.get(eVar);

		return new Property(StringUtils.isEmpty(fieldName) ? eVar : fieldName, fValue);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("AbstractExpressionFilter{"); // NON-NLS
		sb.append("handleType=").append(handleType); // NON-NLS
		sb.append(", filterExpression=").append(Utils.sQuote(filterExpression)); // NON-NLS
		sb.append("}"); // NON-NLS
		return sb.toString();
	}

	/**
	 * Logs expression evaluation match result.
	 * <p>
	 * Log entry is build only if logger log level {@link com.jkoolcloud.tnt4j.core.OpLevel#TRACE} is set.
	 * 
	 * @param varsMap
	 *            variables binding map
	 * @param match
	 *            expression match result
	 *
	 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#describeExpression(String, java.util.Map, String,
	 *      java.util.Collection, java.util.Map)
	 */
	protected void logEvaluationResult(Map<String, ?> varsMap, boolean match) {
		LoggerUtils.log(getLogger(), OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ExpressionFilter.evaluation.result",
				() -> Utils.args(StreamsScriptingUtils.describeExpression(filterExpression, varsMap,
						getHandledLanguage(), exprVars, placeHoldersMap), match));
	}
}
