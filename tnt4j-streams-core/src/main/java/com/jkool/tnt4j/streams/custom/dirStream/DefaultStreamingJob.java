/*
 * Copyright 2014-2016 JKOOL, LLC.
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

package com.jkool.tnt4j.streams.custom.dirStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkool.tnt4j.streams.inputs.InputStreamListener;
import com.jkool.tnt4j.streams.inputs.StreamStatus;
import com.jkool.tnt4j.streams.inputs.StreamThread;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class DefaultStreamingJob implements StreamingJob {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(DefaultStreamingJob.class);

	private File jobCfgFile;
	private UUID jobId;

	private String tnt4jCfgFilePath;

	private List<StreamingJobListener> jobListeners;

	public DefaultStreamingJob(UUID jobId, File jobCfgFile) {
		this.jobId = jobId;
		this.jobCfgFile = jobCfgFile;
	}

	/**
	 * Gets job identifier.
	 *
	 * @return job identifier UUID.
	 */
	public UUID getJobId() {
		return jobId;
	}

	/**
	 * Sets job identifier.
	 *
	 * @param jobId
	 *            job identifier UUID
	 */
	public void setJobId(UUID jobId) {
		this.jobId = jobId;
	}

	/**
	 * Gets path string of TNT4J configuration file.
	 *
	 * @return returns path of TNT4J configuration file
	 */
	public String getTnt4jCfgFilePath() {
		return tnt4jCfgFilePath;
	}

	/**
	 * Sets path string of TNT4J configuration file.
	 *
	 * @param tnt4jCfgFilePath
	 *            path of TNT4J configuration file
	 */
	public void setTnt4jCfgFilePath(String tnt4jCfgFilePath) {
		this.tnt4jCfgFilePath = tnt4jCfgFilePath;
	}

	@Override
	public void run() {
		// StreamsAgent.runFromAPI(jobCfgFile);

		try {
			StreamsConfigLoader cfg = new StreamsConfigLoader(jobCfgFile);
			Collection<TNTInputStream> streams = cfg.getStreams();

			if (CollectionUtils.isEmpty(streams)) {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"StreamsAgent.no.activity.streams"));
			}

			ThreadGroup streamThreads = new ThreadGroup(DefaultStreamingJob.class.getName() + "Threads"); // NON-NLS
			StreamThread ft;

			DefaultStreamListener dsl = new DefaultStreamListener();

			for (TNTInputStream stream : streams) {
				stream.setTnt4jCfgFilePath(tnt4jCfgFilePath);
				stream.addStreamListener(dsl);
				ft = new StreamThread(streamThreads, stream,
						String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
				ft.start();
			}
		} catch (Exception e) {
			LOGGER.log(OpLevel.ERROR, String.valueOf(e.getLocalizedMessage()), e);
		}
	}

	@Override
	public String toString() {
		return "DefaultStreamingJob{" + "jobId=" + jobId + '}';
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}

		if (other instanceof String) {
			return jobId.toString().equals(other.toString());
		} else if (other instanceof UUID) {
			return jobId.equals(other);
		} else if (other instanceof DefaultStreamingJob) {
			return jobId.equals(((DefaultStreamingJob) other).jobId);
		}

		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return jobId.hashCode();
	}

	private void cleanup() {
		if (jobListeners != null) {
			jobListeners.clear();
		}
	}

	/**
	 * Adds defined {@code StreamingJobListener} to streaming jobs listeners
	 * list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be added
	 */
	public void addStreamingJobListener(StreamingJobListener l) {
		if (l == null) {
			return;
		}

		if (jobListeners == null) {
			jobListeners = new ArrayList<StreamingJobListener>();
		}

		jobListeners.add(l);
	}

	/**
	 * Removes defined {@code StreamingJobListener} from streaming jobs
	 * listeners list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be removed
	 */
	public void removeStreamingJobListener(StreamingJobListener l) {
		if (l != null && jobListeners != null) {
			jobListeners.remove(l);
		}
	}

	private class DefaultStreamListener implements InputStreamListener<Object> {

		@Override
		public void onProgressUpdate(TNTInputStream stream, int current, int total) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onProgressUpdate(DefaultStreamingJob.this, current, total);
				}
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void onSuccess(TNTInputStream stream, Object result) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onSuccess(DefaultStreamingJob.this, result);
				}
			}
		}

		@Override
		public void onFailure(TNTInputStream stream, String msg, Throwable exc, String code) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onFailure(DefaultStreamingJob.this, msg, exc, code);
				}
			}
		}

		@Override
		public void onStatusChange(TNTInputStream stream, StreamStatus status) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStatusChange(DefaultStreamingJob.this, status);
				}
			}
		}

		@Override
		public void onFinish(TNTInputStream stream) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onFinish(DefaultStreamingJob.this);
				}
			}

			cleanup();
		}

		@Override
		public void onStreamEvent(TNTInputStream stream, OpLevel level, String message, Object source) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStreamEvent(DefaultStreamingJob.this, level, message, source);
				}
			}

			cleanup();
		}
	}
}
