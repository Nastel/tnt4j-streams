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

package com.jkoolcloud.tnt4j.streams.inputs;

import static com.jkoolcloud.tnt4j.streams.inputs.TNTInputStreamStatistics.START_TIME_KEY;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.*;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.Duration;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Utility class used to report streams statistics as log entries.
 * 
 * @version $Revision: 1 $
 */
public class StreamStatisticsReporter implements Reporter {

	private static final double UNIT_SWITCH_OFFSET = .9;

	private final MetricRegistry registry;
	private TimeUnit ratesUnit;

	/**
	 * Constructs a new StreamStatisticsReporter.
	 *
	 * @param registry
	 *            streams metrics registry instance
	 * @param ratesUnit
	 *            timing values reporting units
	 */
	public StreamStatisticsReporter(MetricRegistry registry, TimeUnit ratesUnit) {
		this.registry = registry;
		this.ratesUnit = ratesUnit == null ? TimeUnit.SECONDS : ratesUnit;
	}

	/**
	 * Reports streams statistics as provided {@code logger} log entries.
	 */
	public void report(EventSink logger) {
		synchronized (registry) {
			report(logger, registry.getGauges(), registry.getCounters(), registry.getHistograms(), registry.getMeters(),
					registry.getTimers());
		}
	}

	@SuppressWarnings("rawtypes")
	private void report(EventSink logger, SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
			SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			logGauge(b, entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			if (entry.getKey().endsWith(START_TIME_KEY)) {
				logElapsedTime(b, entry.getValue());
			} else {
				logCounter(b, entry.getKey(), entry.getValue());
			}
		}

		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			logHistogram(b, entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			logMeter(b, entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			logTimer(b, entry.getKey(), entry.getValue());
		}
		logger.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.statistics", b);
	}

	private void logElapsedTime(StringBuilder b, Counter value) {
		b.append("Elapsed time"); // NON-NLS
		b.append(" = "); // NON-NLS
		b.append(Duration.durationHMS(value.getCount()));
		b.append(", "); // NON-NLS
	}

	private void logTimer(StringBuilder b, String key, Timer value) {
		b.append(key);
		b.append(" = "); // NON-NLS
		adaptiveDuration(b, value.getSnapshot().getMean());
		b.append(", "); // NON-NLS
	}

	private void adaptiveDuration(StringBuilder b, double value) {
		TimeUnit durationUnit = TimeUnit.SECONDS;
		double d = convertDuration(value, durationUnit);
		if (d < UNIT_SWITCH_OFFSET) {
			durationUnit = TimeUnit.MILLISECONDS;
		}
		if (d < UNIT_SWITCH_OFFSET) {
			d = convertDuration(value, durationUnit);
		}
		if (d < UNIT_SWITCH_OFFSET) {
			durationUnit = TimeUnit.MICROSECONDS;
			d = convertDuration(value, durationUnit);
		}
		if (d < UNIT_SWITCH_OFFSET) {
			durationUnit = TimeUnit.NANOSECONDS;
			d = convertDuration(value, durationUnit);
		}

		b.append(d);
		b.append(" "); // NON-NLS
		b.append(shortName(durationUnit));
	}

	private void logMeter(StringBuilder b, String key, Meter value) {
		b.append(key);
		b.append(" = "); // NON-NLS
		b.append(value.getCount());
		b.append(", "); // NON-NLS

		b.append(key);
		b.append(" rate "); // NON-NLS
		b.append(" = "); // NON-NLS
		b.append(value.getMeanRate());
		b.append(" act/"); // NON-NLS
		b.append(shortName(ratesUnit));

		b.append(", "); // NON-NLS
	}

	private void logHistogram(StringBuilder b, String key, Histogram value) {
		b.append(key);
		adaptiveDuration(b, value.getSnapshot().getMean());
		b.append(", "); // NON-NLS
	}

	private void logCounter(StringBuilder b, String key, Counter value) {
		b.append(key);
		b.append(" = "); // NON-NLS
		b.append(value.getCount());
		b.append(", "); // NON-NLS
	}

	private void logGauge(StringBuilder b, String key, Gauge<?> value) {
		b.append(key);
		b.append(" = "); // NON-NLS
		b.append(value.getValue());
		b.append(", "); // NON-NLS
	}

	private double convertDuration(double duration, TimeUnit durationUnit) {
		return durationUnit.convert((long) duration, TimeUnit.NANOSECONDS);
	}

	private double convertRate(double rate) {
		return ratesUnit.convert((long) rate, TimeUnit.SECONDS);
	}

	private static String shortName(TimeUnit ratesUnit) {
		switch (ratesUnit) {
		case MILLISECONDS:
			return "ms"; // NON-NLS
		case SECONDS:
			return "s"; // NON-NLS
		case MICROSECONDS:
			return "µs"; // NON-NLS
		case NANOSECONDS:
			return "ns"; // NON-NLS
		case DAYS:
			return "day"; // NON-NLS
		case HOURS:
			return "hour"; // NON-NLS
		case MINUTES:
			return "m"; // NON-NLS

		}
		return "n/a"; // NON-NLS
	}

	@Override
	public void close() throws IOException {
	}
}
