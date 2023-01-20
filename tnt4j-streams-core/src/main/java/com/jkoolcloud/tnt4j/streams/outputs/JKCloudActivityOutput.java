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

package com.jkoolcloud.tnt4j.streams.outputs;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.Activity;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.DefaultSourceFactory;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.source.SourceFactory;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Implements TNT4J-Streams output logger for activities provided as {@link ActivityInfo} entities to be recorded to
 * jKoolCloud over TNT4J and JESL APIs.
 * <p>
 * This output supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.streams.outputs.AbstractJKCloudOutput}):
 * <ul>
 * <li>ResolveServerFromDNS - flag indicating whether to resolve activity entity host name/IP from DNS server. Default
 * value - {@code false}. (Optional, deprecated - use * parser metadata field
 * {@value ActivityField#META_FIELD_RESOLVE_SERVER} to set value for individual entities)</li>
 * <li>SplitRelatives - flag indicating whether to send activity entity child entities independently merging data from
 * both parent and child entity fields into produced entity. Default value - {@code false}. (Optional, deprecated - use
 * parser metadata field {@value ActivityField#META_FIELD_SPLIT_RELATIVES} to set value for individual entities)</li>
 * <li>BuildSourceFQNFromStreamedData - flag indicating whether to set streamed activity entity {@link Source} FQN build
 * from activity fields data instead of default on configured in 'tnt4j.properties'. Default value - {@code true}.
 * (Optional)</li>
 * <li>SourceFQN - {@link Source} FQN pattern to be used when building it from streamed activity entity fields values.
 * Format is: SourceType1=${FieldName1}#SourceType2=${FieldName2}#SourceType3=${FieldName3}... . Default value -
 * 'APPL=${ApplName}#USER=${UserName}#SERVER=${ServerName}#NETADDR=${ServerIp}#GEOADDR=${Location}'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ActivityInfo#buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
 */
public class JKCloudActivityOutput extends AbstractJKCloudOutput<ActivityInfo, Trackable> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JKCloudActivityOutput.class);
	private static final String DEFAULT_SOURCE_FQN = "APPL=${ApplName}#SERVER=${ServerName}#NETADDR=${ServerIp}#GEOADDR=${Location}"; // NON-NLS

	@Deprecated
	private boolean resolveServer = false;
	@Deprecated
	private boolean splitRelatives = false;
	private boolean buildFQNFromData = true;
	private String sourceFQN = null;

	/**
	 * Constructs a new JKCloudActivityOutput.
	 */
	public JKCloudActivityOutput() {
		super();
	}

	/**
	 * Constructs a new JKCloudActivityOutput.
	 *
	 * @param name
	 *            output name value
	 */
	public JKCloudActivityOutput(String name) {
		super(name);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setProperty(String name, Object value) {
		super.setProperty(name, value);

		if (OutputProperties.PROP_RESOLVE_SERVER.equalsIgnoreCase(name)) {
			resolveServer = Utils.toBoolean((String) value);
		} else if (StringUtils.equalsAnyIgnoreCase(name, OutputProperties.PROP_SPLIT_RELATIVES,
				OutputProperties.PROP_TURN_OUT_CHILDREN)) {
			splitRelatives = Utils.toBoolean((String) value);
		} else if (OutputProperties.PROP_BUILD_FQN_FROM_DATA.equalsIgnoreCase(name)) {
			buildFQNFromData = Utils.toBoolean((String) value);
		} else if (OutputProperties.PROP_SOURCE_FQN.equalsIgnoreCase(name)) {
			sourceFQN = (String) value;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 
	 * @see ActivityInfo#buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
	 */
	@Override
	public void logItem(ActivityInfo ai) throws Exception {
		super.logItem(ai);
		try {
			Tracker tracker = getTracker();
			ai.resolveServer(getBooleanValue(ai.getFieldValue(ActivityField.META_FIELD_RESOLVE_SERVER), resolveServer));
			String aiFQN = buildFQNFromData ? StringUtils.isEmpty(sourceFQN) ? DEFAULT_SOURCE_FQN : sourceFQN : null;

			Map<Trackable, ActivityInfo> childMap = new LinkedHashMap<>();
			if (getBooleanValue(ai.getFieldValue(ActivityField.META_FIELD_SPLIT_RELATIVES), splitRelatives)
					&& ai.hasChildren()) {
				ai.buildSplitRelatives(tracker, childMap);
			} else {
				Trackable trackable = ai.buildTrackable(tracker, childMap);
				recordActivity(tracker, trackable, ai, aiFQN);
			}

			for (Map.Entry<Trackable, ActivityInfo> child : childMap.entrySet()) {
				recordActivity(tracker, child.getKey(), child.getValue(), aiFQN);
			}
		} finally {
			notifyLoggingFinish(ai);
		}
	}

	private static boolean getBooleanValue(Object metaValue, boolean defaultVal) {
		return metaValue == null //
				? defaultVal //
				: Utils.getBoolean(metaValue);
	}

	private void recordActivity(Tracker tracker, Trackable trackable, ActivityInfo ai, String aiFQN) throws Exception {
		alterTrackableSource(tracker, trackable, ai, aiFQN);
		recordActivity(tracker, retryPeriod, trackable);
		notifyEntityRecorded(ai, trackable);
	}

	private void alterTrackableSource(Tracker tracker, Trackable trackable, ActivityInfo ai, String fqn) {
		Source tSrc = getItemSource(tracker, ai, fqn);
		trackable.setSource(tSrc);

		Collection<Snapshot> snapshots = null;
		if (trackable instanceof Activity) {
			snapshots = ((Activity) trackable).getSnapshots();
		} else if (trackable instanceof TrackingEvent) {
			snapshots = ((TrackingEvent) trackable).getOperation().getSnapshots();
		}

		if (CollectionUtils.isNotEmpty(snapshots)) {
			List<ActivityInfo> cais = ai.getChildren(true);
			for (Snapshot s : snapshots) {
				ActivityInfo cai = getChildItem(cais, s.getTrackingId());
				s.setSource(cai == null ? tSrc : getItemSource(tracker, cai, fqn));
			}
		}
	}

	private Source getItemSource(Tracker tracker, ActivityInfo ai, String fqn) {
		if (StringUtils.isNotEmpty(fqn)) {
			return buildSource(tracker, ai.getSourceFQN(fqn));
		} else {
			return getDefaultSource();
		}
	}

	private static Source buildSource(Tracker tracker, String sourceFQN) {
		if (StringUtils.isEmpty(sourceFQN)) {
			return null;
		}
		SourceFactory sf = tracker == null ? DefaultSourceFactory.getInstance()
				: tracker.getConfiguration().getSourceFactory();
		Source source = sf.newFromFQN(sourceFQN);
		source.setSSN(sf.getSSN());

		return source;
	}

	private static ActivityInfo getChildItem(List<ActivityInfo> children, String tId) {
		if (CollectionUtils.isNotEmpty(children) && StringUtils.isNotEmpty(tId)) {
			for (ActivityInfo cai : children) {
				if (tId.equals(cai.getTrackingId())) {
					return cai;
				}
			}
		}

		return null;
	}

	@Override
	protected void logJKCActivity(Tracker tracker, Trackable trackable) {
		if (trackable instanceof TrackingActivity) {
			tracker.tnt((TrackingActivity) trackable);
		} else if (trackable instanceof Snapshot) {
			tracker.tnt((Snapshot) trackable);
		} else {
			tracker.tnt((TrackingEvent) trackable);
		}
	}

	@Override
	public Trackable formatStreamStatusMessage(TrackingEvent statusMessage) {
		return statusMessage;
	}
}
