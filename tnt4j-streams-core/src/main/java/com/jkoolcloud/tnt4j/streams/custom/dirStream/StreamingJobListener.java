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

package com.jkoolcloud.tnt4j.streams.custom.dirStream;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.inputs.StreamingStatus;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStreamStatistics;

/**
 * A streaming job progress/status notifications listener interface. This interface can be implemented by classes that
 * are interested in streaming process progress and status changes.
 * <p>
 * Mainly it covers {@link com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener} functions mapping stream instance to
 * job definition.
 *
 * @version $Revision: 1 $
 *
 * @see DefaultStreamingJob#addStreamingJobListener(StreamingJobListener)
 * @see com.jkoolcloud.tnt4j.streams.custom.dirStream.DirStreamingManager#addStreamingJobListener(StreamingJobListener)
 */
public interface StreamingJobListener {
	/**
	 * This method gets called when streaming job progress has updated.
	 *
	 * @param job
	 *            job sending notification
	 * @param current
	 *            index of currently streamed activity item
	 * @param total
	 *            total number of activity items to stream
	 */
	void onProgressUpdate(StreamingJob job, int current, int total);

	/**
	 * This method gets called when streaming job has completed successfully.
	 *
	 * @param job
	 *            job sending notification
	 */
	void onSuccess(StreamingJob job);

	/**
	 * This method gets called when streaming job process has failed.
	 *
	 * @param job
	 *            job sending notification
	 * @param msg
	 *            text message describing failure
	 * @param exc
	 *            failure related exception
	 * @param code
	 *            failure code
	 */
	void onFailure(StreamingJob job, String msg, Throwable exc, String code);

	/**
	 * This method gets called when streaming job status has changed.
	 *
	 * @param job
	 *            job sending notification
	 * @param status
	 *            new stream job status value
	 */
	void onStatusChange(StreamingJob job, StreamingStatus status);

	/**
	 * This method gets called when streaming job has finished independent of completion state.
	 *
	 * @param job
	 *            job sending notification
	 * @param stats
	 *            stream statistics
	 */
	void onFinish(StreamingJob job, TNTInputStreamStatistics stats);

	/**
	 * This method gets called when streaming job detects some notable event.
	 *
	 * @param job
	 *            job sending notification
	 * @param level
	 *            event severity level
	 * @param message
	 *            event related message
	 * @param source
	 *            event source
	 */
	void onStreamEvent(StreamingJob job, OpLevel level, String message, Object source);
}
