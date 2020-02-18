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

package com.jkoolcloud.tnt4j.streams.utils;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * Base class for Streams threads.
 *
 * @version $Revision: 1 $
 *
 * @see java.lang.Thread
 */
public class StreamsThread extends Thread {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsThread.class);

	/**
	 * Flag indicating that thread should stop running.
	 */
	protected boolean stopRunning = false;

	/**
	 * Constructs a new Streams thread.
	 *
	 * @see Thread#Thread()
	 */
	public StreamsThread() {
		setDefaultName(null);
	}

	/**
	 * Constructs a new Streams thread.
	 *
	 * @param target
	 *            object whose {@link Runnable#run()} method is called
	 * @see Thread#Thread(Runnable)
	 */
	public StreamsThread(Runnable target) {
		super(target);
		setDefaultName(null);
	}

	/**
	 * Constructs a new Streams thread.
	 *
	 * @param target
	 *            object whose {@link Runnable#run()} method is called
	 * @param name
	 *            name of thread
	 * @see Thread#Thread(Runnable, String)
	 */
	public StreamsThread(Runnable target, String name) {
		super(target, name);
		setDefaultName(name);
	}

	/**
	 * Constructs a new Streams thread.
	 *
	 * @param name
	 *            name of thread
	 * @see Thread#Thread(String)
	 */
	public StreamsThread(String name) {
		super(name);
		setDefaultName(name);
	}

	/**
	 * Constructs a new Streams thread.
	 *
	 * @param threadGrp
	 *            thread group to add new thread to
	 * @param target
	 *            object whose {@link Runnable#run()} method is called
	 * @see Thread#Thread(ThreadGroup, Runnable)
	 */
	public StreamsThread(ThreadGroup threadGrp, Runnable target) {
		super(threadGrp, target);
		setDefaultName(null);
	}

	/**
	 * Constructs a new Streams thread.
	 *
	 * @param threadGrp
	 *            thread group to add new thread to
	 * @param target
	 *            object whose {@link Runnable#run()} method is called
	 * @param name
	 *            name of thread
	 * @see Thread#Thread(ThreadGroup, Runnable, String)
	 */
	public StreamsThread(ThreadGroup threadGrp, Runnable target, String name) {
		super(threadGrp, target, name);
		setDefaultName(name);
	}

	/**
	 * Constructs a new Streams thread.
	 *
	 * @param threadGrp
	 *            thread group to add new thread to
	 * @param name
	 *            name of thread
	 * @see Thread#Thread(ThreadGroup, String)
	 */
	public StreamsThread(ThreadGroup threadGrp, String name) {
		super(threadGrp, name);
		setDefaultName(name);
	}

	/**
	 * Sets default name for a Streams thread.
	 * <p>
	 * Prefixes current (default) thread name with thread's ID and strips off leading "com.jkoolcloud.tnt4j.streams."
	 * from thread name.
	 *
	 * @param name
	 *            user defined thread name or {@code null} if undefined
	 */
	protected void setDefaultName(String name) {
		String dName = StringUtils.isEmpty(name) ? getClass().getName() : name;
		dName = String.format("%s:%s", getId(), dName.replaceFirst("com.jkoolcloud.tnt4j.streams.", "")); // NON-NLS

		setName(dName);
	}

	/**
	 * Indicates whether the thread was signaled to stop running.
	 *
	 * @return {@code true} if thread signaled to stop, {@code false} if not
	 */
	public boolean isStopRunning() {
		return stopRunning;
	}

	/**
	 * Stops this thread.
	 *
	 * @param interrupt
	 *            flag indicating whether to interrupt this thread
	 * @see Thread#interrupt()
	 */
	public void halt(boolean interrupt) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				interrupt ? "StreamsThread.halt" : "StreamsThread.stop", getName());
		stopRunning = true;

		if (interrupt) {
			interrupt();
		}
	}

	/**
	 * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of
	 * milliseconds. This method differs from {@link Thread#sleep(long)} (which it uses) in that it does not throw any
	 * exceptions. If the sleep is interrupted, then this method will just return.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 * @see Thread#sleep(long)
	 */
	public static void sleep(long millis) {
		Duration sd = Duration.arm();
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsThread.sleep.interrupted", sd.duration(), millis);
		}
	}

	/**
	 * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of
	 * milliseconds. This method differs from {@link Thread#sleep(long)} (which it uses) in that it does not throw any
	 * exceptions, and if the sleep is interrupted other than to signal thread to terminate, this method will cause
	 * current thread to go "back to sleep" for the remainder of the time.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 * @see Thread#sleep(long)
	 */
	public void sleepFully(long millis) {
		Duration st = Duration.arm();
		long remainMillis = millis;
		int interruptCount = 0;
		while (remainMillis > 0L) {
			try {
				st.set();
				Thread.sleep(remainMillis);
				remainMillis = 0L; // not interrupted, stop sleeping
			} catch (InterruptedException e) {
				// if sleep interrupted because thread was signaled to terminate, then return
				if (stopRunning) {
					return;
				}
				// subtract from sleep time the amount of time we were actually asleep
				long sleepMillis = st.duration();
				remainMillis -= sleepMillis;
				interruptCount++;
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsThread.sleepFully.interrupted", interruptCount, sleepMillis, millis);
				if (remainMillis > 0L) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsThread.sleepFully.remaining", remainMillis);
				}
			}
		}
	}

	/**
	 * Waits at most millis milliseconds for this thread to die. A timeout of 0 means to wait forever. Differs from
	 * {@link java.lang.Thread#join()}, which it wraps, in that it does not throw an exception when interrupted.
	 *
	 * @param millis
	 *            time to wait in milliseconds
	 * @see Thread#join()
	 */
	public void waitFor(long millis) {
		Duration wd = Duration.arm();
		try {
			join(millis);
		} catch (InterruptedException e) {
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"StreamsThread.wait.for", getName(), wd.duration());
	}
}
