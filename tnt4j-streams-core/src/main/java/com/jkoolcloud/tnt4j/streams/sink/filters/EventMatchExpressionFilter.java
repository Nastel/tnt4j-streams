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

package com.jkoolcloud.tnt4j.streams.sink.filters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.config.Configurable;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.SinkEventFilter;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.streams.matchers.Matchers;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * This class implements TNT4J Sink filter using {@link com.jkoolcloud.tnt4j.streams.matchers.Matchers} supported
 * expressions to filter trackables passed through filter bound sink.
 *
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.matchers.Matchers
 */
public class EventMatchExpressionFilter implements SinkEventFilter, Configurable {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(EventMatchExpressionFilter.class);

	private Map<String, ?> config;

	private String matchExp;

	/**
	 * Constructs a new EventMatchExpressionFilter instance.
	 */
	public EventMatchExpressionFilter() {
	}

	@Override
	public boolean filter(EventSink sink, TrackingEvent event) {
		return filterTrackable(sink, event);
	}

	@Override
	public boolean filter(EventSink sink, TrackingActivity activity) {
		return filterTrackable(sink, activity);
	}

	@Override
	public boolean filter(EventSink sink, Snapshot snapshot) {
		return filterTrackable(sink, snapshot);
	}

	@Override
	public boolean filter(EventSink sink, long ttl, Source source, OpLevel level, String msg, Object... args) {
		return false;
	}

	/**
	 * Checks if given trackable passes the filter conditions and shall be passed (if returns {@code true}) through
	 * filter bound sink.
	 *
	 * @param sink
	 *            event sink where filter request is coming from
	 * @param trackable
	 *            trackable instance to check
	 *
	 * @return {@code true} if trackable passed all filter conditions, {@code false} - otherwise
	 * 
	 * @see com.jkoolcloud.tnt4j.streams.matchers.Matchers#evaluateBindings(String, java.util.Map)
	 */
	protected boolean filterTrackable(EventSink sink, Trackable trackable) {
		if (StringUtils.isEmpty(matchExp)) {
			return false;
		}

		Set<String> vars = new HashSet<>();
		com.jkoolcloud.tnt4j.streams.utils.Utils.resolveExpressionVariables(vars, matchExp);

		Map<String, Object> valBindings = new HashMap<>(vars.size());
		if (CollectionUtils.isNotEmpty(vars)) {
			for (String rdVar : vars) {
				Object vValue = trackable.getFieldValue(com.jkoolcloud.tnt4j.streams.utils.Utils.getVarName(rdVar));
				valBindings.put(rdVar, vValue);
			}
		}

		try {
			return Matchers.evaluateBindings(matchExp, valBindings);
		} catch (Exception exc) {
			com.jkoolcloud.tnt4j.streams.utils.Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"EventMatchExpressionFilter.match.evaluation.failed", matchExp, exc);
			return false;
		}
	}

	@Override
	public Map<String, ?> getConfiguration() {
		return config;
	}

	@Override
	public void setConfiguration(Map<String, ?> settings) throws ConfigException {
		config = settings;

		matchExp = Utils.getString("MatchExp", settings, "");
		if (StringUtils.isEmpty(matchExp)) {
			throw new ConfigException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"EventMatchExpressionFilter.empty.match.expression"), config);
		}
	}
}
