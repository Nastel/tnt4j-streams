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

package com.jkoolcloud.tnt4j.streams.management;

/**
 * This interface defines management operations and attributes to be performed over JMX MBean for
 * {@link com.jkoolcloud.tnt4j.streams.StreamsAgent}.
 *
 * @version $Revision: 1 $
 */
public interface AgentMBean {
	/**
	 * Pauses all running streams.
	 */
	void pauseAll();

	/**
	 * Pauses set of running streams. Names for multiple streams can be provided using delimiter symbol
	 * {@value com.jkoolcloud.tnt4j.streams.utils.Utils#TAG_DELIM}.
	 * 
	 * @param names
	 *            set of stream names to pause
	 */
	void pause(String names);

	/**
	 * Stops all running streams.
	 */
	void stopAll();

	/**
	 * Stops set of running streams. Names for multiple streams can be provided using delimiter symbol
	 * {@value com.jkoolcloud.tnt4j.streams.utils.Utils#TAG_DELIM}.
	 * 
	 * @param names
	 *            set of stream names to stop
	 */
	void stop(String names);

	/**
	 * Restarts all running streams.
	 */
	void restartAll();

	/**
	 * Restarts set of running streams. Names for multiple streams can be provided using delimiter symbol
	 * {@value com.jkoolcloud.tnt4j.streams.utils.Utils#TAG_DELIM}.
	 * 
	 * @param names
	 *            set of stream names to restart
	 */
	void restart(String names);

	/**
	 * Returns array of running stream names.
	 * 
	 * @return array of running stream names
	 */
	String[] getRunningStreamNames();

	/**
	 * Checks if there are active stream threads running on runner JVM.
	 * 
	 * @return {@code true} if streams threads group is not {@code null} and has active treads running, {@code false} -
	 *         otherwise
	 */
	boolean isRunning();
}
