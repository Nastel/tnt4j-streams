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

package com.jkoolcloud.tnt4j.streams.custom.dirStream;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.collections4.CollectionUtils;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.*;
import com.jkoolcloud.tnt4j.streams.outputs.OutputStreamListener;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class implements a default directory files streaming job. In general, it defines stream configuration attributes
 * and initiates new stream thread when job gets invoked by executor service.
 *
 * @version $Revision: 1 $
 */
public class DefaultStreamingJob implements StreamingJob {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(DefaultStreamingJob.class);

	private static ThreadGroup streamThreads = new ThreadGroup(DefaultStreamingJob.class.getName() + "Threads"); // NON-NLS;

	private File streamCfgFile;
	private UUID jobId;

	private String tnt4jCfgFilePath = System.getProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY);

	private Collection<TNTInputStream<?, ?>> streams;
	private Collection<StreamingJobListener> jobListeners;
	private WeakReference<DirStreamingManager> managerRef;

	private boolean completed = false;

	/**
	 * Constructs a new DefaultStreamingJob.
	 *
	 * @param jobId
	 *            unique job identifier
	 * @param streamCfgFile
	 *            stream configuration file
	 * @param manager
	 *            files streaming manager instance to use
	 */
	public DefaultStreamingJob(UUID jobId, File streamCfgFile, DirStreamingManager manager) {
		this.jobId = jobId;
		this.streamCfgFile = streamCfgFile;
		this.managerRef = new WeakReference<>(manager);
	}

	@Override
	public UUID getJobId() {
		return jobId;
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

	/**
	 * Initializes and starts configuration defined {@link TNTInputStream}s when job gets invoked by executor service.
	 */
	@Override
	public void run() {
		// StreamsAgent.runFromAPI(new CfgStreamsBuilder().setConfig(streamCfgFile));

		// TODO: configuration from ZooKeeper

		managerRef.get().addRunningTask(this);

		try {
			StreamsConfigLoader cfg = new StreamsConfigLoader(streamCfgFile);

			if (cfg.isErroneous()) {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.erroneous.configuration"));
			}

			streams = cfg.getStreams();

			if (CollectionUtils.isEmpty(streams)) {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.no.activity.streams"));
			}

			StreamThread ft;
			StreamEventsRedirectListener serl = new StreamEventsRedirectListener();

			for (TNTInputStream<?, ?> stream : streams) {
				stream.addStreamListener(serl);

				stream.output().setProperty(OutputProperties.PROP_TNT4J_CONFIG_FILE, tnt4jCfgFilePath);
				stream.output().addOutputListener(serl);
				ft = new StreamThread(streamThreads, stream,
						String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
				ft.start();
			}
		} catch (SAXException | IllegalStateException e) {
			LOGGER.log(OpLevel.ERROR, Utils.getExceptionMessages(e));
			notifyError(e, "STREAM_INIT_FAIL"); // NON-NLS
			cleanup();
		} catch (Throwable e) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsAgent.start.failed", e);
			notifyError(e, "STREAM_START_FAIL"); // NON-NLS
			cleanup();
		}
	}

	public void cancel() {
		if (streams != null) {
			CountDownLatch streamsCompletionSignal = new CountDownLatch(streams.size());

			for (TNTInputStream<?, ?> stream : streams) {
				stream.getOwnerThread().addCompletionLatch(streamsCompletionSignal);

				stream.stop();
			}

			try {
				streamsCompletionSignal.await();
			} catch (InterruptedException exc) {
			}

			cleanup();
		}
	}

	public static boolean isStreamsRunning() {
		return streamThreads.activeCount() > 0;
	}

	public static void stopStreams() {
		StreamsAgent.stopStreams(streamThreads);
	}

	/**
	 * Returns text string representing streaming job.
	 *
	 * @return job string representation
	 */
	@Override
	public String toString() {
		return "DefaultStreamingJob{" + "jobId=" + jobId + '}'; // NON-NLS
	}

	/**
	 * Indicates whether some other streaming job is "equal to" this one.
	 *
	 * @param otherJob
	 *            the reference job object to compare
	 *
	 * @return {@code true} if this job is the same as the otherJob argument, {@code false} - otherwise
	 */
	@Override
	public boolean equals(Object otherJob) {
		if (this == otherJob) {
			return true;
		}
		if (otherJob == null) {
			return false;
		}

		if (otherJob instanceof String) {
			return jobId.toString().equals(otherJob.toString());
		} else if (otherJob instanceof UUID) {
			return jobId.equals(otherJob);
		} else if (otherJob instanceof DefaultStreamingJob) {
			return jobId.equals(((DefaultStreamingJob) otherJob).jobId);
		}

		return super.equals(otherJob);
	}

	/**
	 * Returns a hash code value for the streaming job.
	 *
	 * @return job hash code value
	 */
	@Override
	public int hashCode() {
		return jobId.hashCode();
	}

	private void cleanup() {
		if (completed) {
			return;
		}

		completed = true;

		if (jobListeners != null) {
			jobListeners.clear();
		}

		if (managerRef != null) {
			managerRef.get().removeRunningTask(this);
		}
	}

	private void notifyError(Throwable exc, String code) {
		if (jobListeners != null) {
			for (StreamingJobListener l : jobListeners) {
				l.onFailure(this, null, exc.getMessage(), exc, code);
			}
		}
	}

	/**
	 * Adds defined {@code StreamingJobListener} to streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be added
	 */
	public void addStreamingJobListener(StreamingJobListener l) {
		if (l == null) {
			return;
		}

		if (jobListeners == null) {
			jobListeners = new ArrayList<>();
		}

		jobListeners.add(l);
	}

	/**
	 * Removes defined {@code StreamingJobListener} from streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be removed
	 */
	public void removeStreamingJobListener(StreamingJobListener l) {
		if (l != null && jobListeners != null) {
			jobListeners.remove(l);
		}
	}

	private class StreamEventsRedirectListener implements InputStreamListener, OutputStreamListener {

		@Override
		public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onProgressUpdate(DefaultStreamingJob.this, stream, current, total);
				}
			}
		}

		@Override
		public void onSuccess(TNTInputStream<?, ?> stream) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onSuccess(DefaultStreamingJob.this, stream);
				}
			}
		}

		@Override
		public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onFailure(DefaultStreamingJob.this, stream, msg, exc, code);
				}
			}
		}

		@Override
		public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStatusChange(DefaultStreamingJob.this, stream, status);
				}
			}
		}

		@Override
		public void onFinish(TNTInputStream<?, ?> stream, TNTInputStreamStatistics stats) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onFinish(DefaultStreamingJob.this, stream, stats);
				}
			}

			cleanup();
		}

		@Override
		public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStreamEvent(DefaultStreamingJob.this, stream, level, message, source);
				}
			}
		}

		@Override
		public void onItemLogStart(TNTInputStream<?, ?> stream, Object item) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onSendEvent(DefaultStreamingJob.this, stream, (ActivityInfo) item);
				}
			}
		}

		@Override
		public void onItemLogFinish(Object item) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStreamEvent(DefaultStreamingJob.this, null, OpLevel.INFO, StreamsResources.getString(
							StreamsResources.RESOURCE_BUNDLE_NAME, "DefaultStreamingJob.item.log.finished"), item);
				}
			}
		}

		@Override
		public void onItemRecorded(Object item, Trackable trackable) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStreamEvent(DefaultStreamingJob.this, null, OpLevel.INFO, StreamsResources.getString(
							StreamsResources.RESOURCE_BUNDLE_NAME, "DefaultStreamingJob.item.recorded"), item);
				}
			}
		}
	}
}
