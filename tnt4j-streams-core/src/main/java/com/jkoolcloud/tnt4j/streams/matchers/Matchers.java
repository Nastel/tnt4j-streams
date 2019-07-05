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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.configure.jaxb.ScriptLangs;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.filters.AbstractExpressionFilter;
import com.jkoolcloud.tnt4j.streams.filters.HandleType;
import com.jkoolcloud.tnt4j.streams.filters.StreamEntityFilter;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Facade for {@link Matcher}s evaluation of match expression against activity data.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.matchers.StringMatcher
 * @see com.jkoolcloud.tnt4j.streams.matchers.RegExMatcher
 * @see com.jkoolcloud.tnt4j.streams.matchers.XPathMatcher
 * @see com.jkoolcloud.tnt4j.streams.matchers.JsonPathMatcher
 */
public class Matchers {
	/**
	 * Match expression definition pattern.
	 */
	protected static final Pattern EVAL_EXP_PATTERN = Pattern.compile("((?<type>[a-zA-Z]*):)?(?<evalExp>.+)"); // NON-NLS

	private static Map<String, StreamEntityFilter<Object>> langEvaluatorsCache = new HashMap<>(5);

	/**
	 * Evaluates match <tt>expression</tt> against provided activity <tt>data</tt>.
	 *
	 * @param expression
	 *            match expression string defining type of expression and evaluation expression delimited by
	 *            {@code ':'}, e.g. {@code "regex:.*"}. If type of expression is not defined, default is
	 *            {@code "string"}
	 * @param data
	 *            data to evaluate expression
	 * @return {@code true} if <tt>data</tt> matches <tt>expression</tt>, {@code false} - otherwise
	 * @throws Exception
	 *             if evaluation expression is empty or evaluation of match expression fails
	 */
	public static boolean evaluate(String expression, Object data) throws Exception {
		String[] expTokens = tokenizeExpression(expression);
		String evalType = expTokens[0];
		String evalExpression = expTokens[1];

		if (StringUtils.isEmpty(evalType)) {
			evalType = "STRING"; // NON-NLS
		}

		switch (evalType.toUpperCase()) {
		case "XPATH": // NON-NLS
			return validateAndProcess(XPathMatcher.getInstance(), evalExpression, data);
		case "REGEX": // NON-NLS
		case "REGEXP": // NON-NLS
			return validateAndProcess(RegExMatcher.getInstance(), evalExpression, data);
		case "JPATH": // NON-NLS
		case "JSONPATH": // NON-NLS
			return validateAndProcess(JsonPathMatcher.getInstance(), evalExpression, data);
		case "STRING": // NON-NLS
			return validateAndProcess(StringMatcher.getInstance(), evalExpression, data);
		default:
			return evaluate(evalType, evalExpression, data);
		}
	}

	private static boolean validateAndProcess(Matcher matcher, String expression, Object data) throws Exception {
		if (matcher.isDataClassSupported(data)) {
			return matcher.evaluate(expression, data);
		} else {
			return false;
		}
	}

	private static boolean evaluate(String evalLang, String evalExp, Object data) throws Exception {
		String expression = evalLang + ':' + evalExp;
		StreamEntityFilter<Object> ef = langEvaluatorsCache.get(expression);
		if (ef == null) {
			ef = AbstractExpressionFilter.createExpressionFilter(HandleType.EXCLUDE.name(), evalLang, evalExp);
			langEvaluatorsCache.put(expression, ef);
		}

		return ef.doFilter(data, null);
	}

	/**
	 * Evaluates match <tt>expression</tt> against provided activity entity <tt>ai</tt> data.
	 * 
	 * @param expression
	 *            match expression string defining language used to evaluate expression and evaluation expression
	 *            delimited by {@code ':'}, e.g. {@code "groovy:${ObjectName} == 'Foo'"}. If language is not defined,
	 *            default is {@link com.jkoolcloud.tnt4j.streams.configure.jaxb.ScriptLangs#GROOVY}
	 * @param ai
	 *            activity entity instance to get context values for evaluation
	 * @return {@code true} if activity entity <tt>ai</tt> data values matches <tt>expression</tt>, {@code false} -
	 *         otherwise
	 * @throws java.lang.Exception
	 *             if evaluation expression is empty or evaluation of match expression fails
	 */
	public static boolean evaluate(String expression, ActivityInfo ai) throws Exception {
		StreamEntityFilter<Object> ef = langEvaluatorsCache.get(expression);
		if (ef == null) {
			String[] expTokens = tokenizeExpression(expression);
			String lang = expTokens[0];
			String evalExpression = expTokens[1];

			if (StringUtils.isEmpty(lang)) {
				lang = ScriptLangs.GROOVY.value();
			}

			ef = AbstractExpressionFilter.createExpressionFilter(HandleType.EXCLUDE.name(), lang, evalExpression);
			langEvaluatorsCache.put(expression, ef);
		}

		return ef.doFilter(null, ai);
	}

	private static String[] tokenizeExpression(String expression) {
		java.util.regex.Matcher m = EVAL_EXP_PATTERN.matcher(expression);
		if (!m.matches()) {
			throw new IllegalArgumentException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "Matchers.expression.invalid", expression));
		}

		String evalType = m.group("type"); // NON-NLS
		String evalExpression = m.group("evalExp"); // NON-NLS

		if (StringUtils.isEmpty(evalExpression)) {
			throw new IllegalArgumentException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "Matchers.expression.empty", expression));
		}

		return new String[] { evalType, evalExpression };
	}

	/**
	 * Evaluates match <tt>expression</tt> against provided activity entity <tt>ai</tt> data or activity <tt>data</tt>
	 * value.
	 * 
	 * @param expression
	 *            match expression string
	 * @param data
	 *            data to evaluate expression
	 * @param ai
	 *            activity entity instance to get context values for evaluation
	 * @return {@code true} if activity entity <tt>ai</tt> data <tt>data</tt> values matches <tt>expression</tt>,
	 *         {@code false} - otherwise
	 * @throws Exception
	 *             if evaluation expression is empty or evaluation of match expression fails
	 *
	 * @see #evaluate(String, Object)
	 * @see #evaluate(String, com.jkoolcloud.tnt4j.streams.fields.ActivityInfo)
	 */
	public static boolean evaluate(String expression, Object data, ActivityInfo ai) throws Exception {
		if (Utils.isVariableExpression(expression)) {
			return evaluate(expression, ai);
		}
		return evaluate(expression, data);
	}
}
