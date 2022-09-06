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

package com.jkoolcloud.tnt4j.streams.outputs;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Implements TNT4J-Streams output logger for activities provided as JSON {@link String}s to be recorded to jKoolCloud
 * service over TNT4J and JESL APIs.
 * <p>
 * This output logger primarily is used by {@link com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream} to redirect
 * incoming activities from other TNT4J based producer APIs like 'tnt4j-stream-jmx'.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream
 * @see Tracker#log(OpLevel, String, Object...)
 */
public class JKCloudJsonOutput extends AbstractJKCloudOutput<String, String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JKCloudJsonOutput.class);

	/**
	 * Constructs a new JKCloudJsonOutput.
	 */
	public JKCloudJsonOutput() {
		super();
	}

	/**
	 * Constructs a new JKCloudJsonOutput.
	 *
	 * @param name
	 *            output name value
	 */
	public JKCloudJsonOutput(String name) {
		super(name);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 
	 * @see Tracker#log(OpLevel, String, Object...)
	 */
	@Override
	public void logItem(String ai) throws Exception {
		super.logItem(ai);
		try {
			recordActivity(getTracker(), retryPeriod, ai);
		} finally {
			notifyLoggingFinish(ai);
		}
	}

	@Override
	protected void logJKCActivity(Tracker tracker, String trackable) {
		tracker.log(OpLevel.INFO, trackable);
	}

	@Override
	public String formatStreamStatusMessage(TrackingEvent statusMessage) {
		return new JSONFormatter().format(statusMessage);
	}
}
