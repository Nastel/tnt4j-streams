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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.util.List;

/**
 * A streaming executor tasks notifications listener interface. This interface can be implemented by classes that are
 * interested in streaming tasks handling by stream executor.
 * <p>
 * Note, that {@code TNTInputStream} should use executors to get this listener notified.
 *
 * @version $Revision: 1 $
 *
 * @see TNTInputStream#addStreamTasksListener(StreamTasksListener)
 */
public interface StreamTasksListener {

	/**
	 * This method gets called when stream executor service has rejected offered activity items streaming task to queue.
	 *
	 * @param stream
	 *            stream sending notification
	 * @param task
	 *            executor rejected task
	 */
	void onReject(TNTInputStream<?, ?> stream, Runnable task);

	/**
	 * This method gets called when stream executor service has been shot down and some of unprocessed activity items
	 * streaming tasks has been dropped of the queue.
	 *
	 * @param stream
	 *            stream sending notification
	 * @param tasks
	 *            list of executor dropped of tasks
	 */
	void onDropOff(TNTInputStream<?, ?> stream, List<Runnable> tasks);
}
