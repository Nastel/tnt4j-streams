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

package com.jkoolcloud.tnt4j.streams.reference;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.matchers.Matchers;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Defines activity parser reference having optional array of tags and match expressions bound to this reference.
 *
 * @version $Revision: 1 $
 */
public class MatchingParserReference extends ParserReference {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MatchingParserReference.class);

	private List<String> matchExpressions;

	/**
	 * Constructs a new MatchingParserReference using defined referred parser instance.
	 * 
	 * @param parser
	 *            referred parser instance
	 */
	public MatchingParserReference(ActivityParser parser) {
		super(parser);
	}

	/**
	 * Returns activity data match evaluation expressions list used to determine if data should be parsed by referenced
	 * parser.
	 *
	 * @return match evaluation expressions list
	 */
	public List<String> getMatchExpressions() {
		return matchExpressions;
	}

	/**
	 * Adds activity data match evaluation expression used to determine if data should be parsed by referenced parser.
	 *
	 * @param matchExpression
	 *            match evaluation expression
	 */
	public void addMatchExpression(String matchExpression) {
		if (matchExpressions == null) {
			matchExpressions = new ArrayList<>();
		}
		matchExpressions.add(matchExpression);
	}

	/**
	 * Sets activity data match evaluation expressions used to determine if data should be parsed by referenced parser.
	 *
	 * @param matchExpressions
	 *            match evaluation expressions list
	 */
	public void setMatchExpressions(List<String> matchExpressions) {
		this.matchExpressions = matchExpressions;
	}

	/**
	 * Checks if stacked parser reference defined match evaluation expressions evaluates to positive match value for
	 * provided activity data <tt>value</tt> or parsing context <tt>ai</tt>.
	 *
	 * @param caller
	 *            caller object instance
	 * @param value
	 *            activity data package to be parsed by stacked parser
	 * @param ai
	 *            activity entity providing parsing context data
	 * @param field
	 *            activity field
	 * @return {@code null} if reference match expressions list is empty or {@code null}, {@code true} if referenced
	 *         stacked parser matches activity <tt>data</tt> or parsing context <tt>ai</tt>, {@code false} - otherwise
	 */
	public Boolean matchExp(NamedObject caller, Object value, ActivityInfo ai, ActivityField field) {
		if (CollectionUtils.isEmpty(matchExpressions)) {
			return null;
		}

		for (String matchExpression : matchExpressions) {
			boolean match;
			try {
				match = Matchers.evaluate(matchExpression, value, ai);
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"MatchingParserReference.field.match.evaluation", caller.getName(), field.getFieldTypeName(),
						getParser().getName(), matchExpression, match);
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"MatchingParserReference.field.match.evaluation.failed", caller.getName(),
						field.getFieldTypeName(), getParser().getName(), matchExpression, exc);
				match = false;
			}

			if (!match) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if stream parser reference defined match evaluation expressions evaluates to positive match value for
	 * provided activity data <tt>value</tt>.
	 *
	 * @param caller
	 *            caller object instance
	 * @param value
	 *            activity data package to be parsed by parser
	 * @return {@code null} if reference match expressions list is empty or {@code null}, {@code true} if referenced
	 *         parser matches activity <tt>data</tt>, {@code false} - otherwise
	 */
	public Boolean matchExp(NamedObject caller, Object value) {
		if (CollectionUtils.isEmpty(matchExpressions)) {
			return null;
		}

		for (String matchExpression : matchExpressions) {
			boolean match;
			try {
				match = Matchers.evaluate(matchExpression, value);
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"MatchingParserReference.stream.match.evaluation", caller.getName(), getParser().getName(),
						matchExpression, match);
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"MatchingParserReference.stream.match.evaluation.failed", caller.getName(),
						getParser().getName(), matchExpression, exc);
				match = false;
			}

			if (!match) {
				return false;
			}
		}

		return true;
	}
}
