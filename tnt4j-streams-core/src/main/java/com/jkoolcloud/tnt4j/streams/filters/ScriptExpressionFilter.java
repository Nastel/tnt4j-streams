/*
 * Copyright 2014-2020 JKOOL, LLC.
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

import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;

/**
 * Data value filtering based on script expressions.
 * <p>
 * Supported scripting languages:
 * <ul>
 * <li>{@value StreamsScriptingUtils#GROOVY_LANG}</li>
 * <li>{@value StreamsScriptingUtils#JAVA_SCRIPT_LANG}</li>
 * </ul>
 * 
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#compileScript(String, String)
 * @see javax.script.CompiledScript#eval(javax.script.Bindings)
 */
public class ScriptExpressionFilter extends AbstractExpressionFilter<Object> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ScriptExpressionFilter.class);

	private final String lang;
	private CompiledScript script;

	/**
	 * Constructs a new ScriptExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param lang
	 *            handled script language
	 * @param filterExpression
	 *            filter expression string
	 */
	public ScriptExpressionFilter(String lang, String filterExpression) {
		super(filterExpression);
		this.lang = lang;

		initFilter();
	}

	/**
	 * Constructs a new ScriptExpressionFilter.
	 *
	 * @param lang
	 *            handled script language
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	public ScriptExpressionFilter(String lang, String handleType, String filterExpression) {
		super(handleType, filterExpression);
		this.lang = lang;

		initFilter();
	}

	@Override
	protected EventSink getLogger() {
		return LOGGER;
	}

	@Override
	protected String getHandledLanguage() {
		return lang;
	}

	@Override
	protected void initFilter() {
		super.initFilter();

		try {
			script = StreamsScriptingUtils.compileScript(lang, getExpression());
		} catch (ScriptException exc) {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ExpressionFilter.invalid.script", lang, filterExpression),
					exc);
		}
	}

	@Override
	public boolean doFilter(Object value, ActivityInfo ai) throws FilterException {
		Bindings bindings = new SimpleBindings();
		bindings.put(StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR, value);

		if (ai != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, ai);

				bindings.put(eKV.getKey(), eKV.getValue());
			}
		}

		return evaluate(bindings);
	}

	@Override
	public boolean doFilter(Map<String, ?> valBindings) throws FilterException {
		Bindings bindings = new SimpleBindings();

		if (valBindings != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, valBindings);

				bindings.put(eKV.getKey(), eKV.getValue());
			}
		}

		return evaluate(bindings);
	}

	private boolean evaluate(Bindings bindings) throws FilterException {
		try {
			boolean match = (boolean) script.eval(bindings);

			logEvaluationResult(bindings, match);

			return isFilteredOut(getHandleType(), match);
		} catch (Exception exc) {
			throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ExpressionFilter.filtering.failed", filterExpression), exc);
		}
	}
}
