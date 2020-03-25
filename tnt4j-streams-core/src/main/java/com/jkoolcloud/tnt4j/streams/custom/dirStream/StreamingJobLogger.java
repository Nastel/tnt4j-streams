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

import java.io.File;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.StreamingStatus;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStreamStatistics;

/**
 * For internal development use.
 *
 * @version $Revision: 1 $
 */
public class StreamingJobLogger implements StreamingJobListener, JobFilesListener {

	public StreamingJobLogger() {
	}

	@Override
	public void onProgressUpdate(StreamingJob job, int current, int total) {
		System.out.println("Streaming progress update: job=" + job + " progress=" + current + "/" + total); // NON-NLS
	}

	@Override
	public void onSuccess(StreamingJob job) {
		System.out.println("Streaming job success: job=" + job); // NON-NLS
	}

	@Override
	public void onFailure(StreamingJob job, String msg, Throwable exc, String code) {
		System.out.println("Streaming job error: job=" + job + " msg=" + msg + " code=" + code + " exc=" + exc); // NON-NLS
	}

	@Override
	public void onStatusChange(StreamingJob job, StreamingStatus status) {
		System.out.println("Streaming job status change: job=" + job + " status=" + status); // NON-NLS
	}

	@Override
	public void onFinish(StreamingJob job, TNTInputStreamStatistics stats) {
		System.out.println("Streaming job finished: job=" + job + " stats=" + stats); // NON-NLS
	}

	@Override
	public void onStreamEvent(StreamingJob job, OpLevel level, String message, Object source) {
		System.out.println("Streaming job event occurred: job=" + job + " level=" + level + " msg=" + message // NON-NLS
				+ " source=" + source); // NON-NLS
	}

	@Override
	public void onSendEvent(StreamingJob job, ActivityInfo ai) {
		System.out.println("Streaming send event occurred: job=" + job + " activity =" + ai); // NON-NLS
	}

	@Override
	public void onJobFileStateChanged(File jobCfgFile, String jobId, JobFileState fileState) {
		System.out.println("Streaming job configuration file state changed: job=" + jobId + " file=" + jobCfgFile // NON-NLS
				+ " state=" + fileState); // NON-NLS
	}
}
