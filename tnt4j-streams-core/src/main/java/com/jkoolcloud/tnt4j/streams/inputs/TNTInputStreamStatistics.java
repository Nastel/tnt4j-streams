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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;

import com.codahale.metrics.*;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jmx.ObjectNameFactory;
import com.codahale.metrics.jvm.JmxAttributeGauge;
import com.jkoolcloud.tnt4j.core.Activity;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.outputs.OutputStreamListener;
import com.jkoolcloud.tnt4j.streams.utils.Duration;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Class accounting running stream(s) statistics covering processing counts/timing and other various metrics.
 * 
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.inputs.StreamStatisticsReporter
 */
public class TNTInputStreamStatistics
		implements StreamItemProcessingListener<Timer.Context>, StreamItemAccountingListener, OutputStreamListener {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(TNTInputStreamStatistics.class);

	/**
	 * Constant defining stream start time counter name.
	 */
	static final String START_TIME_KEY = ":start time"; // NON-NLS

	private static TNTInputStreamStatistics delegate;
	private static Map<TNTInputStream<?, ?>, TNTInputStreamStatistics> streamStatistics = new HashMap<>();

	private final MetricRegistry metrics = new MetricRegistry();
	private final HashMap<Object, Timer.Context> pendingOutputs = new HashMap<>();
	private final JmxReporter jmxReporter = JmxReporter.forRegistry(metrics).inDomain("com.jkoolcloud.tnt4j.streams") // NON-NLS
			.createsObjectNamesWith(new StreamsStatsObjectNameFactory()).build();
	private TNTInputStream<?, ?> refStream = null;

	private Map<String, StreamTotals> streamTotalsMap = new HashMap<>();
	private Integer reporterCount;

	private Slf4jReporter sfl4jReporter;

	private Timer streamsItemsTimer;
	private Timer processingTimer;
	private Timer outputTimer;
	private Counter skippedActivitiesCount;
	private Counter filteredActivitiesCount;
	private Counter lostActivitiesCount;
	private Meter currentActivity;
	private Counter processedActivitiesCount;
	private Counter outputEvents;
	private Counter outputActivities;
	private Counter outputSnapshots;
	private Counter outputOther;
	private Gauge<Long> bytesTotal;
	private Gauge<Integer> activitiesTotal;
	private Counter bytesStreamed;

	private TNTInputStreamStatistics() {
		this(null);
	}

	private TNTInputStreamStatistics(TNTInputStream<?, ?> stream) {
		String streamName = "Agent"; // NON-NLS
		if (stream != null) {
			streamName = stream.getName();
			refStream = stream;
		}

		if (stream == null) {
			try {
				Gauge<?> startTimeGauge;
				try {
					startTimeGauge = new JmxAttributeGauge(new ObjectName("java.lang:type=Runtime"), "StartTime"); // NON-NLS
					startTimeGauge.getValue();
				} catch (Throwable exc) {
					startTimeGauge = new DefaultSettableGauge<>(System.currentTimeMillis());
				}
				Gauge<?> startTime = metrics.register(streamName + START_TIME_KEY, startTimeGauge);
				Gauge<String> elapsedTimeGauge = metrics.register(streamName + ":elapsed time", new Gauge<>() { // NON-NLS
					@Override
					public String getValue() {
						return Duration.durationHMS((Long) startTime.getValue());
					}
				});
				Gauge<String> cacheGauge = metrics.register(streamName + ":cache load", new Gauge<>() { // NON-NLS
					@Override
					public String getValue() {
						return StreamsCache.cacheSize() + "/" + StreamsCache.cacheMaxSize(); // NON-NLS
					}
				});
			} catch (Exception e) {
			}
		} else {
			Counter startTime = metrics.counter(streamName + START_TIME_KEY);
			startTime.inc(System.currentTimeMillis());
			Gauge<String> elapsedTimeGauge = metrics.register(streamName + ":elapsed time", new Gauge<>() { // NON-NLS
				@Override
				public String getValue() {
					return Duration.durationHMS(startTime.getCount());
				}
			});
		}

		jmxReporter.start();
		streamsItemsTimer = metrics.timer(streamName + ":input timer"); // NON-NLS
		processingTimer = metrics.timer(streamName + ":processing timer"); // NON-NLS
		outputTimer = metrics.timer(streamName + ":output:timer"); // NON-NLS

		skippedActivitiesCount = metrics.counter(streamName + ":skipped entities"); // NON-NLS
		filteredActivitiesCount = metrics.counter(streamName + ":filtered entities"); // NON-NLS
		lostActivitiesCount = metrics.counter(streamName + ":lost entities"); // NON-NLS
		currentActivity = metrics.meter(streamName + ":current entity"); // NON-NLS
		processedActivitiesCount = metrics.counter(streamName + ":processed entities"); // NON-NLS

		outputEvents = metrics.counter(streamName + ":output:events"); // NON-NLS
		outputActivities = metrics.counter(streamName + ":output:activities"); // NON-NLS
		outputSnapshots = metrics.counter(streamName + ":output:snapshots"); // NON-NLS
		outputOther = metrics.counter(streamName + ":output:others"); // NON-NLS

		bytesTotal = metrics.register(streamName + ":total bytes", new Gauge<>() { // NON-NLS
			@Override
			public Long getValue() {
				return getTotalBytesCount();
			}
		});
		activitiesTotal = metrics.register(streamName + ":total activities", new Gauge<>() { // NON-NLS
			@Override
			public Integer getValue() {
				return getTotalActivitiesCount();
			}
		});

		bytesStreamed = metrics.counter(streamName + ":bytes streamed"); // NON-NLS
	}

	/**
	 * Returns aggregated statistics accounting module for all running streams.
	 *
	 * @return statistics accounting module instance
	 */
	public static TNTInputStreamStatistics getMainStatisticsModule() {
		synchronized (TNTInputStreamStatistics.class) {
			if (delegate == null) {
				TNTInputStreamStatistics.delegate = new TNTInputStreamStatistics();
			}
		}
		return TNTInputStreamStatistics.delegate;
	}

	/**
	 * Returns statistics accounting module for provided {@code stream} instance.
	 *
	 * @param stream
	 *            stream instance to get statistics module for
	 *
	 * @return statistics accounting module instance
	 */
	public static TNTInputStreamStatistics getStreamSpecificStatisticsModule(TNTInputStream<?, ?> stream) {
		TNTInputStreamStatistics tntInputStreamStatistics = streamStatistics.get(stream);
		if (tntInputStreamStatistics == null) {
			TNTInputStreamStatistics newStatisticsForStream = new TNTInputStreamStatistics(stream);
			streamStatistics.put(stream, newStatisticsForStream);
			return newStatisticsForStream;
		}
		return tntInputStreamStatistics;
	}

	/**
	 * Returns statistics accounting module for provided {@code stream} instance. If {@code stream} is {@code null} -
	 * then aggregated statistics accounting module for all running streams is returned.
	 *
	 * @param stream
	 *            stream instance to get statistics module for
	 *
	 * @return statistics accounting module instance
	 */
	public static TNTInputStreamStatistics getStatisticsModule(TNTInputStream<?, ?> stream) {
		if (stream == null) {
			return getMainStatisticsModule();
		}
		return getStreamSpecificStatisticsModule(stream);
	}

	@Override
	public Context<Timer.Context> beforeNextItem() {
		return new Context<>(streamsItemsTimer.time());
	}

	@Override
	public void afterNextItem(Context<Timer.Context> context) {
		if (context != null) {
			context.getItem().stop();
		}
	}

	@Override
	public Context<Timer.Context> beforeProcessItem() {
		return new Context<>(processingTimer.time());
	}

	@Override
	public void afterProcessItem(Context<Timer.Context> context) {
		if (context != null) {
			context.getItem().stop();
		}

		onItemProcessed();
	}

	@Override
	public void onItemLost() {
		getMainStatisticsModule().lostActivitiesCount.inc();
		lostActivitiesCount.inc();
	}

	@Override
	public void onItemFiltered() {
		getMainStatisticsModule().filteredActivitiesCount.inc();
		filteredActivitiesCount.inc();
	}

	@Override
	public void onItemSkipped() {
		getMainStatisticsModule().skippedActivitiesCount.inc();
		skippedActivitiesCount.inc();
	}

	@Override
	public void onItemProcessed() {
		getMainStatisticsModule().processedActivitiesCount.inc();
		processedActivitiesCount.inc();
	}

	@Override
	public void onBytesStreamed(long bytes) {
		getMainStatisticsModule().bytesStreamed.inc(bytes);
		bytesStreamed.inc(bytes);
	}

	/**
	 * Returns currently streamed activity item index. Index is constantly incremented when streaming begins and
	 * activity items gets available to stream.
	 * 
	 * @return currently processed activity item index
	 */
	public int getCurrentActivity() {
		return (int) currentActivity.getCount();
	}

	/**
	 * Returns size in bytes of activity data items available to stream. If total size can't be determined, then
	 * {@code 0} is returned.
	 *
	 * @return total size in bytes of activity data items
	 */
	public long getTotalBytesCount() {
		return streamTotalsMap.values().stream().mapToLong(s -> s == null ? 0 : s.bytesTotalValue).sum();
	}

	/**
	 * Returns count of activity items available to stream. If total activities count can't be determined, then
	 * {@code 0} is returned.
	 * 
	 * @return total count of activities to be processed by steam
	 */
	public int getTotalActivitiesCount() {
		return streamTotalsMap.values().stream().mapToInt(s -> s == null ? 0 : s.activitiesTotalValue).sum();
	}

	/**
	 * Returns size in bytes if streamed activity data items.
	 *
	 * @return streamed activity data items size in bytes
	 */
	public long getStreamedBytesCount() {
		getMainStatisticsModule().bytesStreamed.getCount();
		return bytesStreamed.getCount();
	}

	/**
	 * Increments index of currently processed activity item.
	 *
	 * @return new value of current activity index
	 */
	protected int incrementCurrentActivitiesCount() {
		getMainStatisticsModule().currentActivity.mark();
		currentActivity.mark();
		return getCurrentActivity();
	}

	/**
	 * Starts periodic streams statistics reporting ("ping") as logger log entries.
	 *
	 * @param time
	 *            defines repetitive interval in seconds between "ping" log entries with stream statistics. Default
	 *            value - {@code -1} meaning "NEVER". (Optional, can be OR'ed with {@code count})
	 * @param count
	 *            repetitive number of streamed activity entities to put "ping" log entry with stream statistics.
	 *            Default value - {@code -1} meaning "NEVER". (Optional, can be OR'ed with {@code time})
	 * 
	 */
	public void startStatisticsReporting(int time, Integer count) {
		if (time == -1) {
			return;
		}
		if (count != -1) {
			reporterCount = count;
		}

		if (LOGGER.getSinkHandle() instanceof Logger) {
			sfl4jReporter = Slf4jReporter.forRegistry(metrics).filter(new MetricFilter() {
				@Override
				public boolean matches(String s, Metric metric) {
					boolean shouldReportMetrics = reporterCount == null || reporterCount++ > 0;
					if (!shouldReportMetrics) {
						sfl4jReporter.stop();
					}
					return shouldReportMetrics;
				}
			}).outputTo((Logger) LOGGER.getSinkHandle()).convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS).build();

			sfl4jReporter.start(time, TimeUnit.SECONDS);
		}
	}

	@Override
	public void updateTotal(TNTInputStream<?, ?> stream, long bytes, int activities) {
		getMainStatisticsModule().updateTotalsEntry(stream, bytes, activities);
		updateTotalsEntry(stream, bytes, activities);
	}

	private void updateTotalsEntry(TNTInputStream<?, ?> stream, long bytes, int activities) {
		StreamTotals st = streamTotalsMap.get(stream.getName());
		if (st == null) {
			st = new StreamTotals();
			streamTotalsMap.put(stream.getName(), st);
		}

		st.bytesTotalValue = bytes;
		st.activitiesTotalValue = activities;
	}

	// /**
	// * Returns stream statistics as text string.
	// *
	// * @return stream statistics text string
	// */
	// @Override
	// public String toString() {
	// return "[" + "activities.total=" + totalActivities.getCount() + ", activities.current=" + getCurrentActivity() //
	// NON-NLS
	// + ", activities.skipped=" + skippedActivitiesCount.getCount() + ", activities.filtered=" // NON-NLS
	// + filteredActivitiesCount.getCount() + ", activities.lost=" + lostActivitiesCount.getCount() // NON-NLS
	// + ", bytes.total=" + bytesTotal.getValue() + ", bytes.streamed=" + bytesStreamed.getCount() // NON-NLS
	// + ", time.elapsed=" + Duration.durationHMS(streamTimer.getCount()) // NON-NLS
	// + ", rate.average=" + String.format("%.2f", totalActivities.getMeanRate()) + "aps" // NON-NLS
	// + ", rate.processing="
	// + TimeUnit.SECONDS.convert(Math.round(processingTimer.getSnapshot().getMean()), TimeUnit.NANOSECONDS)
	// + "aps" // NON-NLS
	// + ", read mean time= " + String.format("%.0f", streamsItemsTimer.getSnapshot().getMean()) + "ns" // NON-NLS
	// + ", processing mean time=" + String.format("%.0f", processingTimer.getSnapshot().getMean()) + "ns" // NON-NLS
	// + ", output mean time=" + String.format("%.0f", outputTimer.getSnapshot().getMean()) + "ns" // NON-NLS
	// + ", activities out=" + outputActivities.getCount() + ", events out=" + outputEvents.getCount() // NON-NLS
	// + ", snapshots out=" + outputSnapshots.getCount() + ", other out=" + outputOther.getCount() // NON-NLS
	// + "]"; // NON-NLS
	// }

	@Override
	public void onItemLogStart(TNTInputStream<?, ?> stream, Object item) {
		Timer.Context time = outputTimer.time();
		pendingOutputs.put(item, time);
	}

	@Override
	public void onItemLogFinish(Object item) {
		Timer.Context time = pendingOutputs.remove(item);
		if (time != null) { // case of child trackable applied, the time is not measured.
			time.stop();
		}

		if (item instanceof ActivityInfo) {
			OpType eventType = ((ActivityInfo) item).getEventType();

			if (eventType == null) {
				outputOther.inc();
				getMainStatisticsModule().outputOther.inc();
			} else {
				switch (eventType) {
				case ACTIVITY:
					outputActivities.inc();
					getMainStatisticsModule().outputActivities.inc();
					break;
				case SNAPSHOT:
				case DATASET:
					outputSnapshots.inc();
					getMainStatisticsModule().outputSnapshots.inc();
					break;
				case LOG:
				default:
					outputEvents.inc();
					getMainStatisticsModule().outputEvents.inc();
					break;
				}
			}
		}
	}

	@Override
	public void onItemRecorded(Object item, Trackable trackable) {
		Integer snapshotCount = null;
		if (trackable instanceof Activity) {
			snapshotCount = ((Activity) trackable).getSnapshotCount();
		} else if (trackable instanceof TrackingEvent) {
			snapshotCount = ((TrackingEvent) trackable).getOperation().getSnapshotCount();
		}

		if (snapshotCount != null) {
			outputSnapshots.inc(snapshotCount);
			getMainStatisticsModule().outputSnapshots.inc(snapshotCount);
		}
	}

	/**
	 * Returns metrics registry instance for provided {@code stream} instance.
	 *
	 * @param stream
	 *            stream instance to get metrics for
	 *
	 * @return metrics registry instance
	 */
	public static MetricRegistry getMetrics(TNTInputStream<?, ?> stream) {
		return getStatisticsModule(stream).metrics;
	}

	/**
	 * Returns aggregated metrics registry instance for all running streams.
	 *
	 * @return metrics registry instance
	 */
	public static MetricRegistry getMetrics() {
		return getStatisticsModule(null).metrics;
	}

	/**
	 * Unbinds stream instance statistics accounting.
	 *
	 * @param stream
	 *            stream instance to unbind statistics accounting
	 */
	public static void clear(TNTInputStream<?, ?> stream) {
		MetricRegistry mRegistry = null;
		if (stream == null) {
			if (delegate != null) {
				mRegistry = delegate.metrics;
				Utils.close(delegate.jmxReporter);
			}
		} else {
			TNTInputStreamStatistics ss = streamStatistics.remove(stream);
			if (ss != null) {
				mRegistry = ss.metrics;
				Utils.close(ss.jmxReporter);
			}
		}

		if (mRegistry != null) {
			mRegistry.removeMatching(MetricFilter.ALL);
		}
	}

	/**
	 * Unbinds aggregated statistics accounting for all running streams.
	 */
	public static void clear() {
		clear(null);
	}

	private static class StreamsStatsObjectNameFactory implements ObjectNameFactory {
		@Override
		public ObjectName createName(String type, String domain, String name) {
			try {
				Hashtable<String, String> properties = new Hashtable<>();
				String[] nameTokens = name.split(":"); // NON-NLS
				if (nameTokens.length == 3) {
					properties.put("type", nameTokens[0]); // NON-NLS
					properties.put("scope", nameTokens[1]); // NON-NLS
					properties.put("value", nameTokens[2]); // NON-NLS
				} else if (nameTokens.length == 2) {
					properties.put("type", nameTokens[0]); // NON-NLS
					properties.put("name", nameTokens[1]); // NON-NLS
				} else {
					properties.put("type", "Other"); // NON-NLS
					properties.put("name", name); // NON-NLS
				}

				ObjectName objectName = new ObjectName(domain, properties);
				if (objectName.isDomainPattern()) {
					domain = ObjectName.quote(domain);
				}

				for (Map.Entry<String, String> pe : properties.entrySet()) {
					if (objectName.isPropertyValuePattern(pe.getKey())) {
						properties.put(pe.getKey(), ObjectName.quote(pe.getValue()));
					}
				}

				objectName = new ObjectName(domain, properties);
				return objectName;
			} catch (MalformedObjectNameException exc) {
				try {
					return new ObjectName(domain, "name", ObjectName.quote(name)); // NON-NLS
				} catch (MalformedObjectNameException exc2) {
					LOGGER.log(OpLevel.WARNING, "Unable to register MBean: type={0}, name={1}", type, name, exc2); // NON-NLS
					throw new RuntimeException(exc2);
				}
			}
		}
	}

	private static class StreamTotals {
		private Long bytesTotalValue = 0L;
		private Integer activitiesTotalValue = 0;
	}
}
