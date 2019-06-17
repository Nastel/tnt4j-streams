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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for buffered input activity stream. RAW activity data retrieved from input source is placed into blocking
 * queue to be asynchronously processed by consumer thread(s).
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>BufferSize - maximal buffer queue capacity. Default value - {@code 1024}. (Optional)</li>
 * <li>BufferDropWhenFull - flag indicating to drop buffer queue offered RAW activity data entries when queue gets full.
 * Default value - {@code false}. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 2 $
 *
 * @see ArrayBlockingQueue
 */
public abstract class AbstractBufferedStream<T> extends TNTParseableInputStream<T> {
	private static final int DEFAULT_INPUT_BUFFER_SIZE = 1024;
	private static final Object DIE_MARKER = new Object();

	private int bufferSize;
	private boolean dropDataWhenBufferFull = false;

	/**
	 * RAW activity data items buffer queue. Items in this queue are processed asynchronously by consumer thread(s).
	 */
	protected BlockingQueue<Object> inputBuffer;
	private ThreadLocal<T> currentItem = new ThreadLocal<>();

	/**
	 * Constructs a new AbstractBufferedStream.
	 */
	protected AbstractBufferedStream() {
		this(DEFAULT_INPUT_BUFFER_SIZE);
	}

	/**
	 * Constructs a new AbstractBufferedStream.
	 *
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	protected AbstractBufferedStream(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_BUFFER_SIZE.equalsIgnoreCase(name)) {
			bufferSize = Integer.parseInt(value);
		} else if (StreamProperties.PROP_BUFFER_DROP_WHEN_FULL.equalsIgnoreCase(name)) {
			dropDataWhenBufferFull = Utils.toBoolean(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_BUFFER_SIZE.equalsIgnoreCase(name)) {
			return bufferSize;
		}
		if (StreamProperties.PROP_BUFFER_DROP_WHEN_FULL.equalsIgnoreCase(name)) {
			return dropDataWhenBufferFull;
		}
		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		inputBuffer = new ArrayBlockingQueue<>(bufferSize, true);

		super.initialize();
	}

	/**
	 * Adds terminator object to input buffer.
	 */
	@Override
	protected void stopInternals() {
		offerDieMarker();
	}

	/**
	 * Adds "DIE" marker object to input buffer to mark "logical" data flow has ended.
	 *
	 * @see #offerDieMarker(boolean)
	 */
	protected void offerDieMarker() {
		offerDieMarker(false);
	}

	/**
	 * Adds "DIE" marker object to input buffer to mark "logical" data flow has ended.
	 *
	 * @param forceClear
	 *            flag indicating to clear input buffer contents before putting "DIE" marker object into it
	 */
	protected void offerDieMarker(boolean forceClear) {
		if (inputBuffer != null) {
			if (forceClear) {
				inputBuffer.clear();
			}
			inputBuffer.offer(DIE_MARKER);
		}
	}

	/**
	 * Get the next activity data item to be processed. Method blocks and waits for activity input data available in
	 * input buffer. Input buffer is filled by {@link InputProcessor} thread.
	 * <p>
	 * In case item is consumable by multiple iterations (like {@link java.sql.ResultSet}),
	 * {@link #isItemConsumed(Object)} shall be overridden by stream implementing class to determine if item has been
	 * got consumed (e.g. cursor has moved to last available position). {@link #initItemForParsing(Object)} shall be
	 * overridden by stream implementing class if any procedure is required to initialize activity data before parsing
	 * (e.g. move cursor position to initial value).
	 *
	 * @return next activity data item, or {@code null} if there is no next item
	 *
	 * @throws Exception
	 *             if any errors occurred getting next item
	 *
	 * @see #isItemConsumed(Object)
	 * @see #initItemForParsing(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T getNextItem() throws Exception {
		if (inputBuffer == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"AbstractBufferedStream.changes.buffer.uninitialized"));
		}

		while (true) {
			// Buffer is empty and producer input is ended. No more items going to be available.
			if (inputBuffer.isEmpty() && isInputEnded()) {
				return null;
			}

			T item = getCurrentItem();
			if (item == null || isItemConsumed(item)) {
				Object qe = getItemFromBuffer();

				// Producer input was slower than consumer, but was able to put "DIE" marker object
				// to queue. No more items going to be available.
				if (DIE_MARKER.equals(qe)) {
					return null;
				}

				item = (T) qe;
				setCurrentItem(item);

				if (item == null) {
					continue;
				}

				addStreamedBytesCount(getActivityItemByteSize(item));
				boolean hasParsableData = initItemForParsing(item);

				if (!hasParsableData) {
					setCurrentItem(null);
					continue;
				}
			}

			return item;
		}
	}

	/**
	 * Checks whether provided RAW activity data item is consumed, and stream should take next item from buffer.
	 *
	 * @param item
	 *            activity data item to check
	 *
	 * @return {@code true} if activity data item is consumed, {@code false} - otherwise
	 */
	protected boolean isItemConsumed(T item) {
		return true;
	}

	/**
	 * Performs activity data item initialization before parsing procedure and checks whether it has any parseable
	 * payload.
	 *
	 * @param item
	 *            activity data item to initialize before parsing
	 *
	 * @return {@code true} if activity item data has any parseable payload, {@code false} - otherwise
	 */
	protected boolean initItemForParsing(T item) {
		return true;
	}

	/**
	 * Picks activity data item from buffer to be processed by parsers.
	 * 
	 * @return activity data item from buffer to be processed
	 * @throws InterruptedException
	 *             if interrupted while waiting for activity item data to get available in the buffer
	 */
	protected Object getItemFromBuffer() throws InterruptedException {
		return inputBuffer.take();
	}

	/**
	 * Return currently processed activity item data.
	 * 
	 * @return currently processed activity item data
	 */
	protected T getCurrentItem() {
		return currentItem.get();
	}

	/**
	 * Sets currently processed activity item data.
	 * 
	 * @param item
	 *            activity item data to be currently processed
	 */
	protected void setCurrentItem(T item) {
		currentItem.set(item);
	}

	/**
	 * Gets activity data item size in bytes.
	 *
	 * @param activityItem
	 *            activity data item
	 * @return activity data item size in bytes
	 */
	protected abstract long getActivityItemByteSize(T activityItem);

	@Override
	protected void cleanup() {
		if (inputBuffer != null) {
			inputBuffer.clear();
		}
		super.cleanup();
	}

	/**
	 * Adds input data to buffer for asynchronous processing. Input data may not be added, if buffer size limit is
	 * exceeded and stream configuration parameter {@code 'BufferDropWhenFull'} value is {@code true}.
	 *
	 * @param inputData
	 *            input data to add to buffer
	 * @return {@code true} if input data is added to buffer, {@code false} - otherwise
	 *
	 * @throws IllegalStateException
	 *             if buffer queue is not initialized
	 *
	 * @see BlockingQueue#offer(Object)
	 * @see BlockingQueue#put(Object)
	 */
	protected boolean addInputToBuffer(T inputData) throws IllegalStateException {
		if (inputBuffer == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"AbstractBufferedStream.changes.buffer.uninitialized"));
		}
		if (inputData != null && !isHalted()) {
			if (dropDataWhenBufferFull) {
				boolean added = inputBuffer.offer(inputData);
				if (!added) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"AbstractBufferedStream.changes.buffer.limit", inputData);
					incrementLostActivitiesCount();
				}
				return added;
			} else {
				try {
					inputBuffer.put(inputData);
					return true;
				} catch (InterruptedException exc) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"AbstractBufferedStream.put.interrupted", inputData);
					incrementLostActivitiesCount();
				}
			}
		}
		return false;
	}

	/**
	 * Checks if stream data input has ended.
	 *
	 * @return {@code true} if stream input ended, {@code false} - otherwise
	 */
	protected abstract boolean isInputEnded();

	/**
	 * Base class containing common features for stream input processor thread.
	 */
	protected abstract class InputProcessor extends StreamsThread {

		private boolean inputEnd = false;

		/**
		 * Constructs a new InputProcessor.
		 *
		 * @param name
		 *            the name of the new thread
		 */
		protected InputProcessor(String name) {
			super(name);

			setDaemon(true);
		}

		/**
		 * Initializes input processor.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             indicates that input processor initialization failed
		 */
		protected abstract void initialize(Object... params) throws Exception;

		/**
		 * Checks if input processor should stop running.
		 *
		 * @return {@code true} input processor is interrupted or parent thread is halted
		 */
		protected boolean isStopping() {
			return isStopRunning() || isHalted();
		}

		/**
		 * Closes opened data input resources and marks stream data input as ended.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		private void close() throws Exception {
			try {
				closeInternals();
			} finally {
				markInputEnd();
			}
		}

		/**
		 * Closes opened data input resources.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		abstract void closeInternals() throws Exception;

		/**
		 * Shuts down stream input processor: interrupts thread and closes opened data input resources.
		 */
		protected void shutdown() {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"AbstractBufferedStream.input.shutdown", AbstractBufferedStream.this.getName(), getName());
			if (isStopRunning() && inputEnd) {
				// shot down already.
				return;
			}

			halt();
			try {
				close();
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractBufferedStream.input.close.error", exc);
			}
		}

		/**
		 * Changes stream data input ended flag value to {@code true}
		 */
		protected void markInputEnd() {
			// mark input end (in case producer thread is faster than consumer).
			inputEnd = true;
			// add "DIE" marker to buffer (in case producer thread is slower
			// than waiting consumer).
			offerDieMarker();
		}

		/**
		 * Checks if stream data input is ended.
		 *
		 * @return {@code true} if stream input ended, {@code false} - otherwise
		 */
		protected boolean isInputEnded() {
			return inputEnd;
		}
	}
}
