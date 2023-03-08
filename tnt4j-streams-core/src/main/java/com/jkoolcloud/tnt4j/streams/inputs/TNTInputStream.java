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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.codahale.metrics.Timer;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.outputs.TNTStreamOutput;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Base class that all activity streams must extend. It provides some base functionality useful for all activity
 * streams.
 * <p>
 * All activity streams should support the following configuration properties:
 * <ul>
 * <li>DateTime - default date/time to associate with activities. (Optional)</li>
 * <li>UseExecutors - identifies whether stream should use executor service to process activities data items
 * asynchronously or not. Default value - {@code false}. (Optional)</li>
 * <li>ExecutorThreadsQuantity - defines executor service thread pool size. Default value - {@code 4}. (Optional)</li>
 * <li>ExecutorRejectedTaskOfferTimeout - time to wait (in seconds) for a executor service to terminate. Default value -
 * {@code 20}. (Optional)</li>
 * <li>ExecutorsBoundedModel - identifies whether executor service should use bounded tasks queue model. Default value -
 * {@code false}. (Optional)</li>
 * <li>ExecutorsTerminationTimeout - time to wait (in seconds) for a task to be inserted into bounded queue if max.
 * queue size is reached. Default value - {@code 20}. (Optional, actual only if {@code ExecutorsBoundedModel} is set to
 * {@code true})</li>
 * <li>PingLogActivityCount - defines repetitive number of streamed activity entities to put "ping" log entry with
 * stream statistics. Default value - {@code -1} meaning "NEVER". (Optional, can be OR'ed with
 * {@code PingLogActivityDelay})</li>
 * <li>PingLogActivityDelay - defines repetitive interval in seconds between "ping" log entries with stream statistics.
 * Default value - {@code -1} meaning "NEVER". (Optional, can be OR'ed with {@code PingLogActivityCount})</li>
 * <li>Set of user defined stream context properties. To define stream context property add
 * {@value CustomProperties#CONTEXT_PROPERTY_PREFIX} prefix to property name. These properties are not used directly by
 * stream itself, but can be used in stream bound parsers configuration over dynamic locators or variable expressions to
 * enrich parsing context. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled RAW activity data
 * @param <O>
 *            the type of handled output data
 *
 * @version $Revision: 3 $
 *
 * @see java.util.concurrent.ExecutorService
 * @see com.jkoolcloud.tnt4j.streams.outputs.TNTStreamOutput
 */
public abstract class TNTInputStream<T, O> implements Runnable, NamedObject {

	private static final long DEFAULT_STREAM_TERMINATION_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

	private static final int DEFAULT_EXECUTOR_THREADS_QTY = 4;
	private static final int DEFAULT_EXECUTORS_TERMINATION_TIMEOUT = 20;
	private static final int DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT = 20;

	/**
	 * StreamThread running this stream.
	 */
	private StreamThread ownerThread = null;

	private AtomicBoolean failureFlag = new AtomicBoolean(false);
	private long startTime = -1;
	private long endTime = -1;
	private long lastActivityTime = -1;
	private String name;

	private TNTStreamOutput<O> out;

	private TNTInputStreamStatistics statistics;

	private final List<InputStreamListener> streamListeners = new ArrayList<>(3);
	private final List<StreamTasksListener> streamTasksListeners = new ArrayList<>(3);
	private final Map<StreamItemProcessingListener<Timer.Context>, StreamItemProcessingListener.Context<Timer.Context>> streamItemProcessingListeners = new HashMap<>();
	private final List<StreamItemAccountingListener> streamItemAccountingListeners = new ArrayList<>(3);

	private boolean useExecutorService = false;
	private ExecutorService streamExecutorService = null;

	// executor service related properties
	private boolean boundedExecutorModel = false;
	private int executorThreadsQty = DEFAULT_EXECUTOR_THREADS_QTY;
	private int executorsTerminationTimeout = DEFAULT_EXECUTORS_TERMINATION_TIMEOUT;
	private int executorRejectedTaskOfferTimeout = DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT;

	private int pingLogActivitiesCount = -1;
	private int pingLogActivitiesDelay = -1;

	private CustomProperties<String> customProperties = new CustomProperties<>(5);

	private Thread sh;

	/**
	 * Returns logger used by this stream.
	 *
	 * @return stream logger
	 */
	protected abstract EventSink logger();

	/**
	 * Gets stream output handler.
	 *
	 * @return stream output handler
	 */
	public TNTStreamOutput<O> getOutput() {
		return out;
	}

	/**
	 * Sets stream output handler.
	 *
	 * @param out
	 *            stream output handler
	 */
	protected void setOutput(TNTStreamOutput<O> out) {
		this.out = out;
		out.setStream(this);
	}

	/**
	 * Ensures stream output handler is set and returns stream output handler instance.
	 *
	 * @return stream output handler
	 *
	 * @see #ensureOutputSet()
	 */
	public TNTStreamOutput<O> output() {
		ensureOutputSet();

		return out;
	}

	/**
	 * Sets default stream output handler. It may happen when stream configuration does not define particular output
	 * handler reference (e.g., from older TNT4J-Streams API versions).
	 */
	protected abstract void setDefaultStreamOutput();

	/**
	 * Get the thread owning this stream.
	 *
	 * @return owner thread
	 */
	public StreamThread getOwnerThread() {
		return ownerThread;
	}

	/**
	 * Set the thread owning this stream.
	 *
	 * @param ownerThread
	 *            thread owning this stream
	 */
	public void setOwnerThread(StreamThread ownerThread) {
		this.ownerThread = ownerThread;
	}

	/**
	 * Checks whether this stream has owner thread assigned.
	 *
	 * @return {@code true} if owner thread is assigned, {@code false} - if owner thread is {@code null}
	 */
	protected boolean isOwned() {
		return ownerThread != null;
	}

	/**
	 * Sets configuration properties for this activity stream.
	 * <p>
	 * This method is invoked by the configuration loader in response to the {@code property} configuration elements. It
	 * is invoked once per stream definition, with all property names and values specified for this stream. Subclasses
	 * should generally override this method to process custom properties, and invoke the base class method to handle
	 * any built-in properties.
	 *
	 * @param props
	 *            configuration properties to set
	 *
	 * @see #setProperty(String, String)
	 * @see #applyProperties()
	 * @see #initialize()
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	/**
	 * Sets configuration property for this activity stream.
	 *
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 *
	 * @throws java.lang.IllegalArgumentException
	 *             if configuration property can't be applied
	 *
	 * @see #setProperties(java.util.Collection)
	 */
	public void setProperty(String name, String value) {
		if (StreamProperties.PROP_USE_EXECUTOR_SERVICE.equalsIgnoreCase(name)) {
			useExecutorService = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_EXECUTOR_THREADS_QTY.equalsIgnoreCase(name)) {
			executorThreadsQty = Integer.parseInt(value);
		} else if (StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
			executorRejectedTaskOfferTimeout = Integer.parseInt(value);
		} else if (StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT.equalsIgnoreCase(name)) {
			executorsTerminationTimeout = Integer.parseInt(value);
		} else if (StreamProperties.PROP_EXECUTORS_BOUNDED.equalsIgnoreCase(name)) {
			boundedExecutorModel = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_PING_LOG_ACTIVITY_COUNT.equalsIgnoreCase(name)) {
			pingLogActivitiesCount = Integer.parseInt(value);
		} else if (StreamProperties.PROP_PING_LOG_ACTIVITY_DELAY.equalsIgnoreCase(name)) {
			pingLogActivitiesDelay = Integer.parseInt(value);
		} else if (customProperties.isPropertyNameForThisMap(name)) {
			customProperties.setProperty(name, value);
		}

		output().setProperty(name, value);
	}

	/**
	 * Applies activity stream configuration properties.
	 * 
	 * @throws Exception
	 *             indicates that stream is not configured properly and cannot continue
	 *
	 * @see #setProperties(java.util.Collection)
	 */
	protected void applyProperties() throws Exception {
	}

	/**
	 * Get value of specified property. If subclasses override {@link #setProperties(Collection)}, they should generally
	 * override this method as well to return the value of custom properties, and invoke the base class method to handle
	 * any built-in properties.
	 *
	 * @param name
	 *            name of property whose value is to be retrieved
	 * @return value for property, or {@code null} if property does not exist
	 *
	 * @see #setProperty(String, String)
	 */
	public Object getProperty(String name) {
		if (StreamProperties.PROP_DATETIME.equalsIgnoreCase(name)) {
			return getDate();
		}
		if (StreamProperties.PROP_USE_EXECUTOR_SERVICE.equalsIgnoreCase(name)) {
			return useExecutorService;
		}
		if (StreamProperties.PROP_EXECUTOR_THREADS_QTY.equalsIgnoreCase(name)) {
			return executorThreadsQty;
		}
		if (StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
			return executorRejectedTaskOfferTimeout;
		}
		if (StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT.equalsIgnoreCase(name)) {
			return executorsTerminationTimeout;
		}
		if (StreamProperties.PROP_EXECUTORS_BOUNDED.equalsIgnoreCase(name)) {
			return boundedExecutorModel;
		}
		if (StreamProperties.PROP_STREAM_NAME.equalsIgnoreCase(name)) {
			return this.name;
		}
		if (StreamProperties.PROP_PING_LOG_ACTIVITY_COUNT.equalsIgnoreCase(name)) {
			return pingLogActivitiesCount;
		}
		if (StreamProperties.PROP_PING_LOG_ACTIVITY_DELAY.equalsIgnoreCase(name)) {
			return pingLogActivitiesDelay;
		}

		String cPropertyValue = customProperties.getProperty(name);
		if (cPropertyValue != null) {
			return cPropertyValue;
		}

		return null;
	}

	/**
	 * Initialize the stream.
	 * <p>
	 * This method is called by default {@link #run()} method to perform any necessary initializations before the stream
	 * starts processing, including verifying that all required properties are set. If subclasses override this method
	 * to perform any custom initializations, they must call the base class method. If subclass also overrides the
	 * {@link #run()} method, it must call this at start of {@link #run()} method before entering into processing loop.
	 *
	 * @throws Exception
	 *             indicates that stream is not configured properly and cannot continue
	 *
	 * @see #ensureOutputSet()
	 * @see #setProperties(Collection)
	 */
	protected void initialize() throws Exception {
		statistics = TNTInputStreamStatistics.getStatisticsModule(this);
		statistics.startStatisticsReporting(pingLogActivitiesDelay, pingLogActivitiesCount);

		addItemProcessingListener(statistics);
		addStreamItemAccountingListener(statistics);

		ensureOutputSet();
		getOutput().addOutputListener(statistics);
		out.initialize();

		if (useExecutorService) {
			streamExecutorService = boundedExecutorModel
					? getBoundedExecutorService(executorThreadsQty, executorRejectedTaskOfferTimeout)
					: getDefaultExecutorService(executorThreadsQty);
		} else {
			out.handleConsumerThread(isOwned() ? ownerThread : Thread.currentThread());
		}
	}

	/**
	 * Checks stream output handler instance and if it is {@code null} - sets default one.
	 */
	public void ensureOutputSet() {
		if (out == null) {
			setDefaultStreamOutput();
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTInputStream.output.undefined");
		}
	}

	/**
	 * Starts stream data input.
	 *
	 * @throws Exception
	 *             indicates that stream was unable to start and cannot continue
	 */
	protected void start() throws Exception {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.ready", getClass().getSimpleName(), getName());

		startTime = System.currentTimeMillis();
		notifyStatusChange(StreamStatus.STARTED);
	}

	/**
	 * Initializes stream and starts data input.
	 *
	 * @throws Exception
	 *             indicates that stream is not configured properly or was unable to start and cannot continue
	 */
	public void startStream() throws Exception {
		applyProperties();
		initialize();
		start();

		StreamsCache.referStream();

		sh = new Thread(() -> stop(), getName() + "_ShutdownHookThread");
		Runtime.getRuntime().addShutdownHook(sh);
	}

	/**
	 * Creates default thread pool executor service for a given number of threads. Using this executor service tasks
	 * queue size is unbound. Thus memory use may be high to store all producer thread created tasks.
	 *
	 * @param threadsQty
	 *            the number of threads in the pool
	 *
	 * @return the newly created thread pool executor
	 *
	 * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory)
	 */
	private ExecutorService getDefaultExecutorService(int threadsQty) {
		StreamsThreadFactory stf = new StreamsThreadFactory("StreamDefaultExecutorThread-"); // NON-NLS
		stf.addThreadFactoryListener(new StreamsThreadFactoryListener());

		ThreadPoolExecutor tpe = new ThreadPoolExecutor(threadsQty, threadsQty, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(), stf);

		return tpe;
	}

	private class StreamsThreadFactoryListener implements StreamsThreadFactory.StreamsThreadFactoryListener {
		@Override
		public void newThreadCreated(Thread t) {
			try {
				out.handleConsumerThread(t);
			} catch (IllegalStateException exc) {
				logger().log(OpLevel.FAILURE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTInputStream.tracker.check.state.failed");
				halt(true);
			}
		}
	}

	/**
	 * Creates thread pool executor service for a given number of threads with bounded tasks queue - queue size is
	 * 2x{@code threadsQty}. When queue size is reached, new tasks are offered to queue using defined offer timeout. If
	 * task can't be put into queue over this time, task is skipped with making warning log entry. Thus memory use does
	 * not grow drastically if consumers can't keep up the pace of producers filling in the queue, making producers
	 * synchronize with consumers.
	 *
	 * @param threadsQty
	 *            the number of threads in the pool
	 * @param offerTimeout
	 *            how long to wait before giving up on offering task to queue
	 *
	 * @return the newly created thread pool executor
	 *
	 * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory)
	 */
	private ExecutorService getBoundedExecutorService(int threadsQty, int offerTimeout) {
		StreamsThreadFactory stf = new StreamsThreadFactory("StreamBoundedExecutorThread-"); // NON-NLS
		stf.addThreadFactoryListener(new StreamsThreadFactoryListener());

		ThreadPoolExecutor tpe = new ThreadPoolExecutor(threadsQty, threadsQty, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(threadsQty * 2), stf);

		tpe.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					boolean added = executor.getQueue().offer(r, offerTimeout, TimeUnit.SECONDS);
					if (!added) {
						logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"TNTInputStream.tasks.buffer.limit", offerTimeout);
						notifyStreamTaskRejected(r);
					}
				} catch (InterruptedException exc) {
					halt(true);
				}
			}
		});

		return tpe;
	}

	/**
	 * Adds reference to specified entity object being used by this stream.
	 *
	 * @param refObject
	 *            entity object to reference
	 * @throws IllegalStateException
	 *             if referenced object can't be linked to stream
	 */
	@SuppressWarnings("unchecked")
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof TNTStreamOutput) {
			setOutput((TNTStreamOutput<O>) refObject);
		}
	}

	/**
	 * Get the position in the source activity data currently being processed. For line-based data sources, this is
	 * generally the line number of currently processed file or other text source. If activity items source (e.g., file)
	 * changes - activity position gets reset.
	 * <p>
	 * Subclasses should override this to provide meaningful information, if relevant. The default implementation just
	 * returns 0.
	 *
	 * @return current position in activity data source being processed, or {@code 0} if activity position can't be
	 *         determined
	 *
	 * @see #getCurrentActivity()
	 */
	public int getActivityPosition() {
		return getCurrentActivity();
	}

	/**
	 * Returns currently streamed activity item index. Index is constantly incremented when streaming begins and
	 * activity items gets available to stream.
	 * <p>
	 * It does not matter if activity item source changes (e.g., file). To get actual source dependent position see
	 * {@link #getActivityPosition()}.
	 *
	 * @return currently processed activity item index
	 *
	 * @see #getActivityPosition()
	 * @see #getTotalActivities()
	 */
	public int getCurrentActivity() {
		return statistics.getCurrentActivity();
	}

	/**
	 * Increments index of currently processed activity item.
	 *
	 * @return new value of current activity index
	 */
	protected int incrementCurrentActivitiesCount() {
		return statistics.incrementCurrentActivitiesCount();
	}

	/**
	 * Returns total number of activity items to be streamed.
	 *
	 * @return total number of activities available to stream, or {@code -1} if total number of activities is
	 *         undetermined
	 *
	 * @see #getCurrentActivity()
	 */
	public int getTotalActivities() {
		return -1;
	}

	/**
	 * Returns size in bytes of activity data items available to stream. If total size can't be determined, then
	 * {@code 0} is returned.
	 *
	 * @return total size in bytes of activity data items
	 */
	public long getTotalBytes() {
		return 0;
	}

	/**
	 * Returns size in bytes if streamed activity data items.
	 *
	 * @return streamed activity data items size in bytes
	 */
	public long getStreamedBytesCount() {
		return statistics.getStreamedBytesCount();
	}

	/**
	 * Increments processing skipped activity items count.
	 */
	protected void incrementSkippedActivitiesCount() {
		synchronized (streamItemAccountingListeners) {
			for (StreamItemAccountingListener streamItemAccountingListener : streamItemAccountingListeners) {
				streamItemAccountingListener.onItemSkipped();
			}
		}
	}

	/**
	 * Increments processing filtered activity items count.
	 */
	protected void incrementFilteredActivitiesCount() {
		synchronized (streamItemAccountingListeners) {
			for (StreamItemAccountingListener streamItemAccountingListener : streamItemAccountingListeners) {
				streamItemAccountingListener.onItemFiltered();
			}
		}
	}

	/**
	 * Increments processing skipped lost activity items count.
	 */
	protected void incrementLostActivitiesCount() {
		synchronized (streamItemAccountingListeners) {
			for (StreamItemAccountingListener streamItemAccountingListener : streamItemAccountingListeners) {
				streamItemAccountingListener.onItemLost();
			}
		}
	}

	/**
	 * Updates activities skipped and total counts and fires progress update notification when activity data gets
	 * filtered out by streaming settings (e.g., file lines range).
	 */
	protected void skipFilteredActivities() {
		incrementSkippedActivitiesCount();
		notifyProgressUpdate(getCurrentActivity(), getTotalActivities());
	}

	/**
	 * Adds number of bytes to streamed bytes counter.
	 *
	 * @param bytesCount
	 *            number of bytes to add
	 */
	protected void addStreamedBytesCount(long bytesCount) {
		synchronized (streamItemAccountingListeners) {
			for (StreamItemAccountingListener streamItemAccountingListener : streamItemAccountingListeners) {
				streamItemAccountingListener.onBytesStreamed(bytesCount);
				streamItemAccountingListener.updateTotal(this, getTotalBytes(), getTotalActivities());
			}
		}
	}

	/**
	 * Returns duration of streaming process.
	 *
	 * @return duration of steaming process
	 */
	public long getElapsedTime() {
		long et = endTime < 0 ? System.currentTimeMillis() : endTime;

		return startTime < 0 ? -1 : et - startTime;
	}

	/**
	 * Returns average stream rate in activities per second.
	 *
	 * @return average stream rate in activities per second
	 */
	public double getAverageActivityRate() {
		long lat = lastActivityTime < 0 ? System.currentTimeMillis() : lastActivityTime;
		double et = startTime < 0 ? 0 : lat - startTime;
		return (getCurrentActivity() / et) * 1000;
	}

	/**
	 * Creates snapshot of instant stream statistics.
	 *
	 * @return snapshot of instant stream statistics
	 */
	public TNTInputStreamStatistics getStreamStatistics() {
		return statistics;
	}

	/**
	 * Get the next raw activity data item to be processed. All subclasses must implement this.
	 *
	 * @return next raw activity data item, or {@code null} if there is no next item
	 * @throws Exception
	 *             if any errors occurred getting next item
	 */
	public abstract T getNextItem() throws Exception;

	/**
	 * Gets the default date/time to use for activity entries that do not contain a date. Default implementation returns
	 * the current date.
	 *
	 * @return default date/time to use for activity entries
	 */
	public Date getDate() {
		return new Date();
	}

	/**
	 * Signals that this stream has finished processing so controlling thread will set to stopping state and will
	 * terminate if {@code halt} is set to {@code true}.
	 *
	 * @param terminate
	 *            flag indicating controlling thread to terminate
	 */
	public void halt(boolean terminate) {
		shutdownExecutors();

		if (isOwned()) {
			ownerThread.halt(terminate);
		}
	}

	/**
	 * Indicates whether this stream has stopped.
	 *
	 * @return {@code true} if stream has stopped processing, {@code false} - otherwise
	 */
	public boolean isHalted() {
		return isOwned() ? ownerThread.isStopRunning() : false;
	}

	/**
	 * Causes stream owner thread to sleep for a defined period of time.
	 *
	 * @param period
	 *            sleep period in milliseconds
	 */
	protected void sleep(long period) {
		logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.will.retry", TimeUnit.MILLISECONDS.toSeconds(period));
		StreamsThread.sleep(period);
	}

	/**
	 * Cleanup the stream.
	 * <p>
	 * This method is called by default {@link #run()} method to perform any necessary cleanup before the stream stops
	 * processing, releasing any resources created by {@link #initialize()} method. If subclasses override this method
	 * to perform any custom cleanup, they must call the base class method. If subclass also overrides the
	 * {@link #run()} method, it must call this at end of {@link #run()} method before returning.
	 */
	protected void cleanup() {
		cleanupStreamInternals();

		if (out != null) {
			out.cleanup();
		}

		StreamsCache.unreferStream();
	}

	protected void cleanupStreamInternals() {
	}

	private void removeListeners() {
		synchronized (streamListeners) {
			if (CollectionUtils.isNotEmpty(streamListeners)) {
				streamListeners.clear();
			}
		}

		synchronized (streamTasksListeners) {
			if (CollectionUtils.isNotEmpty(streamTasksListeners)) {
				streamTasksListeners.clear();
			}
		}

		synchronized (streamItemAccountingListeners) {
			if (CollectionUtils.isNotEmpty(streamItemAccountingListeners)) {
				streamItemAccountingListeners.clear();
			}
		}

		synchronized (streamItemProcessingListeners) {
			if (MapUtils.isNotEmpty(streamItemProcessingListeners)) {
				streamItemProcessingListeners.clear();
			}
		}
	}

	private synchronized void shutdownExecutors() {
		if (streamExecutorService == null || streamExecutorService.isShutdown()) {
			return;
		}

		streamExecutorService.shutdown();
		try {
			streamExecutorService.awaitTermination(executorsTerminationTimeout, TimeUnit.SECONDS);
		} catch (InterruptedException exc) {
			halt(true);
		} finally {
			List<Runnable> droppedTasks = streamExecutorService.shutdownNow();

			if (CollectionUtils.isNotEmpty(droppedTasks)) {
				notifyStreamTasksDropOff(droppedTasks);
			}
		}
	}

	/**
	 * Starts input stream processing. Implementing {@link Runnable} interface makes it possible to process each stream
	 * in separate thread.
	 */
	@Override
	public void run() {
		notifyStatusChange(StreamStatus.NEW);

		logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.starting", name);
		if (!isOwned()) {
			IllegalStateException e = new IllegalStateException(StreamsResources
					.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.no.owner.thread"));
			notifyFailed(null, e, null);

			throw e;
		}

		try {
			startStream();

			while (!isHalted()) {
				try {
					beforeNextItem();

					T item = getNextItem();

					afterNextItem();

					if (item == null) {
						logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"TNTInputStream.data.stream.ended", name);
						if (!isHalted()) {
							halt(false); // no more data items to process
						}
					} else {
						if (streamExecutorService == null) {
							processActivityItem_(item, failureFlag);
						} else {
							streamExecutorService
									.submit(new ActivityItemProcessingTask(item, failureFlag, getActivityPosition()));
						}
					}
				} catch (IllegalStateException ise) {
					Utils.logThrowable(logger(), OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"TNTInputStream.failed.record.activity.at", getActivityPosition(), ise);
					failureFlag.set(true);
					notifyFailed(ise.getMessage(), ise, null);
					halt(false);
				} catch (Exception exc) {
					Utils.logThrowable(logger(), OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"TNTInputStream.failed.record.activity.at", getActivityPosition(), exc);
					notifyStreamEvent(OpLevel.ERROR,
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"TNTInputStream.failed.record.activity.at", getActivityPosition(),
									Utils.getExceptionMessages(exc)),
							getActivityPosition());
					incrementSkippedActivitiesCount();
				}
			}
		} catch (Throwable e) {
			Utils.logThrowable(logger(), OpLevel.CRITICAL,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTInputStream.fatal.stream.failure", name, e);
			failureFlag.set(true);
			try {
				notifyFailed(e.getMessage(), e, null);
			} catch (Throwable exc) {
			}
		} finally {
			shutdownStream();
		}
	}

	/**
	 * Checks if stream has been started shot down process.
	 *
	 * @return {@code true} if stream already has end time set, {@code false} - otherwise
	 */
	protected boolean isShotDown() {
		return endTime != -1;
	}

	private void shutdownStream() {
		if (isShotDown()) {
			return;
		}

		endTime = System.currentTimeMillis();
		if (!failureFlag.get()) {
			notifyStreamSuccess();
		}

		try {
			cleanup();
		} catch (Throwable exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTInputStream.failed.cleanup.stream", exc);
			notifyStreamEvent(OpLevel.ERROR, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.failed.cleanup.stream", Utils.getExceptionMessages(exc)), name);
		}

		notifyFinished();
		removeListeners();

		new StreamStatisticsReporter(TNTInputStreamStatistics.getMetrics(this), null).report(logger());
		TNTInputStreamStatistics.clear(this);

		logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.thread.ended", Thread.currentThread().getName());

		if (isOwned()) {
			ownerThread.notifyCompleted();
		}
	}

	/**
	 * Performs processing of raw activity data item: it may be parsing, redirecting, etc.
	 *
	 * @param item
	 *            raw activity data item
	 * @param failureFlag
	 *            item processing failure flag instance
	 * @throws Exception
	 *             if any errors occurred while processing item
	 */
	protected abstract void processActivityItem(T item, AtomicBoolean failureFlag) throws Exception;

	private AtomicInteger cai = new AtomicInteger(0);
	private long lastLogTime = System.currentTimeMillis();
	private AtomicInteger processingCount = new AtomicInteger();

	private void processActivityItem_(T item, AtomicBoolean failureFlag) throws Exception {
		beforeProcessItem();
		startProcessingTask();
		try {
			processActivityItem(item, failureFlag);
		} finally {
			endProcessingTask();
		}
		afterProcessItem();
		lastActivityTime = System.currentTimeMillis();

		// TODO: make ping logger class running separate thread.
		/*
		 * if ((pingLogActivitiesCount > 0 && cai.incrementAndGet() % pingLogActivitiesCount == 0) ||
		 * (pingLogActivitiesDelay > 0 && lastActivityTime - lastLogTime >
		 * TimeUnit.SECONDS.toMillis(pingLogActivitiesDelay))) { lastLogTime = System.currentTimeMillis();
		 * logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
		 * "TNTInputStream.stream.statistics", name, getStreamStatistics()); }
		 */
	}

	/**
	 * Marks start of some stream processing task, e.g. parsing, data polling, etc.
	 *
	 * @see #endProcessingTask()
	 */
	protected void startProcessingTask() {
		processingCount.incrementAndGet();
	}

	/**
	 * Marks end of some stream processing task.
	 *
	 * @see #startProcessingTask()
	 */
	protected void endProcessingTask() {
		processingCount.decrementAndGet();
	}

	/**
	 * Returns flag indicating if stream has any pending tasks on executor service queue.
	 *
	 * @return {@code true} if executor service is running and executor tasks queue is not empty, {@code false} -
	 *         otherwise
	 */
	protected boolean hasPendingExecutions() {
		return streamExecutorService != null && !((ThreadPoolExecutor) streamExecutorService).getQueue().isEmpty();
	}

	/**
	 * Returns flag indicating if there are some currently running stream processing tasks.
	 *
	 * @return {@code true} if stream has running any processing tasks, {@code false} - otherwise.
	 *
	 * @see #processingCount()
	 */
	protected boolean isProcessing() {
		return processingCount.get() > 0;
	}

	/**
	 * Returns number of currently running stream processing tasks.
	 *
	 * @return number of currently running stream processing tasks
	 */
	protected int processingCount() {
		return processingCount.get();
	}

	/**
	 * Returns flag indicating if stream has no currently running stream processing tasks and pending executor service
	 * tasks.
	 *
	 * @return {@code true} if stream has no currently running stream processing tasks and pending executor service
	 *         tasks, {@code false} - otherwise
	 *
	 * @see #isProcessing()
	 * @see #hasPendingExecutions()
	 */
	protected boolean isIdling() {
		return !isProcessing() && !hasPendingExecutions();
	}

	/**
	 * Signals that streaming process has to be stopped/canceled and invokes status change event.
	 */
	public void stop() {
		if (!isShotDown()) {
			halt(false);

			stopInternals();

			if (isOwned()) {
				ownerThread.waitFor(DEFAULT_STREAM_TERMINATION_TIMEOUT);
			}

			notifyStatusChange(StreamStatus.STOP);

			if (!isShotDown()) {
				shutdownStream();
			}
		}

		if (sh != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(sh);
			} catch (IllegalStateException exc) {
			}
			sh = null;
		}
	}

	/**
	 * Performs internal stop actions, like immediately stopping input reading.
	 */
	protected void stopInternals() {
	}

	/**
	 * Returns stream name value.
	 *
	 * @return stream name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets stream name value.
	 *
	 * @param name
	 *            stream name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds defined {@code InputStreamListener} to stream listeners list.
	 *
	 * @param l
	 *            the {@code InputStreamListener} to be added
	 */
	public void addStreamListener(InputStreamListener l) {
		if (l == null) {
			return;
		}

		synchronized (streamListeners) {
			if (!streamListeners.contains(l)) {
				streamListeners.add(l);
			}
		}
	}

	/**
	 * Removes defined {@code InputStreamListener} from stream listeners list.
	 *
	 * @param l
	 *            the {@code InputStreamListener} to be removed
	 */
	public void removeStreamListener(InputStreamListener l) {
		synchronized (streamListeners) {
			if (l != null) {
				streamListeners.remove(l);
			}
		}
	}

	/**
	 * Adds defined {@code StreamItemAccountingListener} to stream items accounting listeners list.
	 *
	 * @param l
	 *            the {@code StreamItemAccountingListener} to be added
	 */
	public void addStreamItemAccountingListener(StreamItemAccountingListener l) {
		if (l == null) {
			return;
		}

		synchronized (streamItemAccountingListeners) {
			if (!streamItemAccountingListeners.contains(l)) {
				streamItemAccountingListeners.add(l);
			}
		}
	}

	/**
	 * Removes defined {@code StreamItemAccountingListener} from stream items accounting listeners list.
	 *
	 * @param l
	 *            the {@code StreamItemAccountingListener} to be removed
	 */
	public void removeStreamItemAccountingListener(StreamItemAccountingListener l) {
		synchronized (streamItemAccountingListeners) {
			if (l != null) {
				streamItemAccountingListeners.remove(l);
			}
		}
	}

	/**
	 * Adds defined {@code StreamItemProcessingListener} to stream items processing listeners list.
	 *
	 * @param l
	 *            the {@code StreamItemProcessingListener} to be added
	 */
	public void addItemProcessingListener(StreamItemProcessingListener<Timer.Context> l) {
		if (l == null) {
			return;
		}

		synchronized (streamItemProcessingListeners) {
			if (!streamItemProcessingListeners.containsKey(l)) {
				streamItemProcessingListeners.put(l, null);
			}
		}
	}

	/**
	 * Removes defined {@code StreamItemProcessingListener} from stream items processing listeners list.
	 *
	 * @param l
	 *            the {@code StreamItemProcessingListener} to be removed
	 */
	public void removeItemProcessingListener(StreamItemProcessingListener<Timer.Context> l) {
		synchronized (streamItemProcessingListeners) {
			if (l != null) {
				streamItemProcessingListeners.remove(l);
			}
		}
	}

	/**
	 * Notifies stream items processing listeners that stream is going to get next item to process.
	 */
	protected void beforeNextItem() {
		synchronized (streamItemProcessingListeners) {
			for (Map.Entry<StreamItemProcessingListener<Timer.Context>, StreamItemProcessingListener.Context<Timer.Context>> entry : streamItemProcessingListeners
					.entrySet()) {
				entry.setValue(entry.getKey().beforeNextItem());
			}
		}
	}

	/**
	 * Notifies stream items processing listeners that stream has got next item to process.
	 */
	protected void afterNextItem() {
		synchronized (streamItemProcessingListeners) {
			for (Map.Entry<StreamItemProcessingListener<Timer.Context>, StreamItemProcessingListener.Context<Timer.Context>> entry : streamItemProcessingListeners
					.entrySet()) {
				entry.getKey().afterNextItem(entry.getValue());
			}
		}
	}

	/**
	 * Notifies stream items processing listeners that stream is starting activity item processing/parsing.
	 */
	protected void beforeProcessItem() {
		synchronized (streamItemProcessingListeners) {
			for (Map.Entry<StreamItemProcessingListener<Timer.Context>, StreamItemProcessingListener.Context<Timer.Context>> entry : streamItemProcessingListeners
					.entrySet()) {
				entry.setValue(entry.getKey().beforeProcessItem());
			}
		}
	}

	/**
	 * Notifies stream items processing listeners that stream has completed activity item processing (parsing).
	 */
	protected void afterProcessItem() {
		synchronized (streamItemProcessingListeners) {
			for (Map.Entry<StreamItemProcessingListener<Timer.Context>, StreamItemProcessingListener.Context<Timer.Context>> entry : streamItemProcessingListeners
					.entrySet()) {
				entry.getKey().afterProcessItem(entry.getValue());
			}
		}
	}

	/**
	 * Notifies that activity items streaming process progress has updated.
	 *
	 * @param curr
	 *            index of currently streamed activity item
	 * @param total
	 *            total number of activity items to stream
	 */
	protected void notifyProgressUpdate(int curr, int total) {
		synchronized (streamListeners) {
			for (InputStreamListener l : streamListeners) {
				l.onProgressUpdate(this, curr, total);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process has completed successfully.
	 */
	public void notifyStreamSuccess() {
		notifyStatusChange(StreamStatus.SUCCESS);
		synchronized (streamListeners) {
			for (InputStreamListener l : streamListeners) {
				l.onSuccess(this);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process has failed.
	 *
	 * @param msg
	 *            failure message
	 * @param exc
	 *            failure exception
	 * @param code
	 *            failure code
	 */
	protected void notifyFailed(String msg, Throwable exc, String code) {
		notifyStatusChange(StreamStatus.FAILURE);
		synchronized (streamListeners) {
			for (InputStreamListener l : streamListeners) {
				l.onFailure(this, msg, exc, code);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process status has changed.
	 *
	 * @param newStatus
	 *            new stream status value
	 */
	protected void notifyStatusChange(StreamStatus newStatus) {
		synchronized (streamListeners) {
			for (InputStreamListener l : streamListeners) {
				l.onStatusChange(this, newStatus);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process has finished independent of completion state.
	 */
	protected void notifyFinished() {
		synchronized (streamListeners) {
			TNTInputStreamStatistics stats = getStreamStatistics();
			for (InputStreamListener l : streamListeners) {
				l.onFinish(this, stats);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process detects some notable event.
	 *
	 * @param level
	 *            event severity level
	 * @param message
	 *            event related message
	 * @param source
	 *            event source
	 */
	protected void notifyStreamEvent(OpLevel level, String message, Object source) {
		synchronized (streamListeners) {
			for (InputStreamListener l : streamListeners) {
				l.onStreamEvent(this, level, message, source);
			}
		}
	}

	/**
	 * Adds defined {@code StreamTasksListener} to stream tasks listeners list.
	 *
	 * @param l
	 *            the {@code StreamTasksListener} to be added
	 */
	public void addStreamTasksListener(StreamTasksListener l) {
		if (l == null) {
			return;
		}

		synchronized (streamTasksListeners) {
			if (!streamTasksListeners.contains(l)) {
				streamTasksListeners.add(l);
			}
		}
	}

	/**
	 * Removes defined {@code StreamTasksListener} from stream tasks listeners list.
	 *
	 * @param l
	 *            the {@code StreamTasksListener} to be removed
	 */
	public void removeStreamTasksListener(StreamTasksListener l) {
		synchronized (streamTasksListeners) {
			if (l != null) {
				streamTasksListeners.remove(l);
			}
		}
	}

	/**
	 * Notifies that stream executor service has rejected offered activity items streaming task to queue.
	 *
	 * @param task
	 *            executor rejected task
	 */
	protected void notifyStreamTaskRejected(Runnable task) {
		synchronized (streamTasksListeners) {
			for (StreamTasksListener l : streamTasksListeners) {
				l.onReject(this, task);
			}
		}
	}

	/**
	 * Notifies that stream executor service has been shot down and some of unprocessed activity items streaming tasks
	 * has been dropped of the queue.
	 *
	 * @param tasks
	 *            list of executor dropped of tasks
	 */
	protected void notifyStreamTasksDropOff(List<Runnable> tasks) {
		synchronized (streamTasksListeners) {
			for (StreamTasksListener l : streamTasksListeners) {
				l.onDropOff(this, tasks);
			}
		}
	}

	/**
	 * Checks is provided password string is encoded by Base64 and tries to decrypt it. If decryption fails or provided
	 * password is {@code null} - original password string is returned.
	 * 
	 * @param passStr
	 *            password string
	 * @return decrypted password string, or original password string value if decryption fails or provided password
	 *         string is {@code null}
	 *
	 * @see com.jkoolcloud.tnt4j.streams.utils.SecurityUtils#getPass2(String)
	 */
	protected static String decPassword(String passStr) {
		return SecurityUtils.getPass2(passStr);
	}

	/**
	 * Placeholder method for plaint text password encryption within streams flow (if such feature will be demanded).
	 *
	 * @param passStr
	 *            plain text password string
	 * @return original plain text password string
	 */
	protected static String encPassword(String passStr) {
		return passStr;
	}

	private class ActivityItemProcessingTask implements Runnable {
		private T item;
		private AtomicBoolean failureFlag;
		private int activityPosition;

		/**
		 * Constructs a new ActivityItemProcessingTask.
		 *
		 * @param activityItem
		 *            raw activity data item to process asynchronously
		 * @param failureFlag
		 *            failure flag to set value if task processing fails
		 * @param activityPosition
		 *            streamed activity position index
		 */
		ActivityItemProcessingTask(T activityItem, AtomicBoolean failureFlag, int activityPosition) {
			this.item = activityItem;
			this.failureFlag = failureFlag;
			this.activityPosition = activityPosition;
		}

		@Override
		public void run() {
			try {
				processActivityItem_(item, failureFlag);
			} catch (Exception e) { // TODO: better handling
				Utils.logThrowable(logger(), OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTInputStream.failed.record.activity.at", activityPosition, e);
				notifyStreamEvent(OpLevel.ERROR,
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"TNTInputStream.failed.record.activity.at", activityPosition,
								Utils.getExceptionMessages(e)),
						item);
				incrementSkippedActivitiesCount();
			}
		}

		/**
		 * Return string representing class name of task object and wrapped activity item data.
		 *
		 * @return a string representing activity item processing task
		 */
		@Override
		public String toString() {
			return ActivityItemProcessingTask.class.getSimpleName() + " {item=" + item + '}'; // NON-NLS
		}
	}

	/**
	 * TNT4J-Streams thread factory.
	 *
	 * @version $Revision: 1 $
	 */
	public static class StreamsThreadFactory implements ThreadFactory {
		private AtomicInteger count = new AtomicInteger(1);
		private String prefix;
		private List<StreamsThreadFactoryListener> listeners = null;

		/**
		 * Constructs a new StreamsThreadFactory.
		 *
		 * @param prefix
		 *            thread name prefix
		 */
		public StreamsThreadFactory(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			StreamsThread task = new StreamsThread(r, prefix + count.getAndIncrement());
			task.setDaemon(true);

			notifyNewThreadCreated(task);

			return task;
		}

		private void notifyNewThreadCreated(Thread t) {
			if (listeners != null) {
				for (StreamsThreadFactoryListener l : listeners) {
					l.newThreadCreated(t);
				}
			}
		}

		/**
		 * Adds defined {@code StreamsThreadFactoryListener} to thread factory listeners list.
		 *
		 * @param l
		 *            the {@code StreamsThreadFactoryListener} to be added
		 */
		public void addThreadFactoryListener(StreamsThreadFactoryListener l) {
			if (l == null) {
				return;
			}

			if (listeners == null) {
				listeners = new ArrayList<>();
			}

			listeners.add(l);
		}

		/**
		 * Removes defined {@code StreamsThreadFactoryListener} from thread factory listeners list.
		 *
		 * @param l
		 *            the {@code StreamsThreadFactoryListener} to be removed
		 */
		public void removeThreadFactoryListener(StreamsThreadFactoryListener l) {
			if (l != null && listeners != null) {
				listeners.remove(l);
			}
		}

		/**
		 * A {@link StreamsThreadFactory} listener interface.
		 */
		public interface StreamsThreadFactoryListener {
			/**
			 * This method gets called when {@link StreamsThreadFactory} creates new thread.
			 *
			 * @param t
			 *            factory created thread
			 */
			void newThreadCreated(Thread t);
		}
	}
}
