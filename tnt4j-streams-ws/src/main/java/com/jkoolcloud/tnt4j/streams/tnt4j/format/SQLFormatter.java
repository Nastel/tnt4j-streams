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

package com.jkoolcloud.tnt4j.streams.tnt4j.format;

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
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.source.DefaultSourceFactory;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Implementation of {@link com.jkoolcloud.tnt4j.format.Formatter} interface provides formatting of
 * {@link TrackingActivity}, {@link TrackingEvent}, {@link Snapshot}, {@link String} message and
 * {@link java.lang.Object} into JDBC compliant SQL query.
 *
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.tnt4j.sink.JDBCEventSink
 */
public class SQLFormatter implements EventFormatter, Configurable {
	private static final String ACTIVITY_QUERY_KEY = "ActivityQuery"; // NON-NLS
	private static final String EVENT_QUERY_KEY = "EventQuery"; // NON-NLS
	private static final String SNAPSHOT_QUERY_KEY = "SnapshotQuery"; // NON-NLS
	private static final String MESSAGE_QUERY_KEY = "MessageQuery"; // NON-NLS
	private static final String OBJECT_QUERY_KEY = "ObjectQuery"; // NON-NLS

	private Map<String, ?> config = null;

	@Override
	public void setConfiguration(Map<String, ?> settings) throws ConfigException {
		config = settings;
	}

	@Override
	public Map<String, ?> getConfiguration() {
		return config;
	}

	@Override
	public String format(TrackingEvent event) {
		String qt = getQueryTemplate(EVENT_QUERY_KEY);

		return fillInQueryTemplate(qt, event);
	}

	@Override
	public String format(TrackingActivity activity) {
		String qt = getQueryTemplate(ACTIVITY_QUERY_KEY);

		return fillInQueryTemplate(qt, activity);
	}

	@Override
	public String format(Snapshot snapshot) {
		String qt = getQueryTemplate(SNAPSHOT_QUERY_KEY);

		return fillInQueryTemplate(qt, snapshot);
	}

	@Override
	public String format(long ttl, Source src, OpLevel level, String msg, Object... args) {
		String qt = getQueryTemplate(MESSAGE_QUERY_KEY);

		return fillInQueryTemplate(qt, ttl, src, level, msg, args);
	}

	@Override
	public String format(Object obj, Object... args) {
		if (obj instanceof TrackingActivity) {
			return format((TrackingActivity) obj);
		} else if (obj instanceof TrackingEvent) {
			return format((TrackingEvent) obj);
		} else if (obj instanceof Snapshot) {
			return format((Snapshot) obj);
		} else {
			String qt = getQueryTemplate(OBJECT_QUERY_KEY);

			return fillInQueryTemplate(qt, Utils.format(Utils.toString(obj), args));
		}
	}

	/**
	 * Picks query template from formatter configuration by provided template key.
	 * 
	 * @param qKey
	 *            query template key
	 * @return query template string
	 * 
	 * @throws java.lang.IllegalStateException
	 *             if query templates not configured or if no such query template defined
	 */
	protected String getQueryTemplate(String qKey) {
		if (config == null) {
			throw new IllegalStateException("Query templates not configured");
		}

		String queryT = (String) config.get(qKey);
		if (StringUtils.isEmpty(queryT)) {
			throw new IllegalStateException("Query template for key '" + qKey + "' not found");
		}

		return queryT;
	}

	/**
	 * Fills-in provided query template with values from provided trackable instance.
	 * 
	 * @param qt
	 *            query template to fill in
	 * @param t
	 *            trackable to pick fill-in values
	 * @return filled-in query string
	 */
	protected String fillInQueryTemplate(String qt, Trackable t) {
		Set<String> vars = new HashSet<>();
		Utils.resolveExpressionVariables(vars, qt);

		String query = qt;
		if (CollectionUtils.isNotEmpty(vars)) {
			for (String qVar : vars) {
				Object cValue = t.getFieldValue(Utils.getVarName(qVar));
				String varVal = Utils.toString(cValue);
				query = query.replace(qVar, varVal);
			}

			if (!query.equals(qt) && Utils.isVariableExpression(query)) {
				return fillInQueryTemplate(query, t);
			}
		}

		return query;
	}

	/**
	 * Fills-in provided query template with provided message values.
	 * 
	 * @param qt
	 *            query template to fill in
	 * @param ttl
	 *            time to live in seconds
	 * @param src
	 *            event source
	 * @param level
	 *            severity level
	 * @param msg
	 *            message to be formatted
	 * @param args
	 *            arguments associated with the object
	 * @return filled-in query string
	 */
	protected String fillInQueryTemplate(String qt, long ttl, Source src, OpLevel level, String msg, Object... args) {
		Set<String> vars = new HashSet<>();
		Utils.resolveExpressionVariables(vars, qt);

		String query = qt;
		if (CollectionUtils.isNotEmpty(vars)) {
			for (String qVar : vars) {
				String vName = Utils.getVarName(qVar);
				String varVal = null;
				switch (vName.toLowerCase()) {
				case "ttl": // NON-NLS
					varVal = String.valueOf(ttl);
					break;
				case "source": // NON-NLS
					varVal = src != null ? src.getFQName()
							: DefaultSourceFactory.getInstance().getRootSource().getFQName();
					break;
				case "level": // NON-NLS
					varVal = level == null ? null : level.name();
					break;
				case "msg": // NON-NLS
					varVal = Utils.format(msg, args);
					break;
				default:
					break;
				}

				query = query.replace(qVar, varVal);
			}

			if (!query.equals(qt) && Utils.isVariableExpression(query)) {
				return fillInQueryTemplate(query, ttl, src, level, msg, args);
			}
		}

		return query;
	}

	/**
	 * Fills-in provided query template with provided object value.
	 * 
	 * @param qt
	 *            query template to fill in
	 * @param objStr
	 *            string serialized string
	 * @return filled-in query string
	 */
	protected String fillInQueryTemplate(String qt, String objStr) {
		Set<String> vars = new HashSet<>();
		Utils.resolveExpressionVariables(vars, qt);

		String query = qt;
		if (CollectionUtils.isNotEmpty(vars)) {
			for (String qVar : vars) {
				query = query.replace(qVar, objStr);
			}

			if (!query.equals(qt) && Utils.isVariableExpression(query)) {
				return fillInQueryTemplate(query, objStr);
			}
		}

		return query;
	}
}
