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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.management.MBeansManager;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class implements directory files streaming manager. Manager monitors directory (and subdirectories) for stream
 * configuration files (e.g., tnt-data-source*.xml) and invokes streaming actions on stream configuration files changes.
 * Stream configuration file name must also contain job identifier {@link UUID} set by job producer (e.g.,
 * tnt-data-source_123e4567-e89b-12d3-a456-426655440000.xml) to make events/actions correlations between all
 * participating pats: job producer, TNT4J-Streams API and jKoolCloud. Without identifier, stream job configurations
 * won't be processed.
 * <p>
 * Because streaming actions are invoked on stream configuration file changes, it is recommended to upload (make to be
 * available for steaming) actual activities RAW data files before stream configuration file gets available in monitored
 * directory.
 * <p>
 * Streaming jobs are processed using {@link ThreadPoolExecutor} with 10 core threads and max 100 threads by default.
 * <p>
 * Directory monitoring is performed using {@link DirWatchdog}. Manager handles those watchdog invoked file
 * notifications:
 * <ul>
 * <li>Create - creates new streaming job and enqueues it to executor service.</li>
 * <li>Change - creates new streaming job if such job is not available in executor queue.</li>
 * <li>Delete - removes streaming job from executor queue.</li>
 * </ul>
 *
 * <p>
 * Sample:
 * 
 * <pre>
 * String dirPath = "./../temp/";
 * String fwn = "tnt-data-source*.xml";
 * 
 * final DirStreamingManager dm = new DirStreamingManager(dirPath, fwn);
 * dm.setTnt4jCfgFilePath("./../config/tnt4j.properties");
 * dm.addStreamingJobListener(new StreamingJobLogger());
 * 
 * Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
 * 	&#64;Override
 * 	public void run() {
 * 		System.out.println("JVM exiting!...");
 * 		synchronized (dm) {
 * 			dm.notify();
 * 		}
 * 		dm.stop();
 * 	}
 * }));
 * 
 * dm.start();
 * synchronized (dm) {
 * 	dm.wait();
 * }
 * </pre>
 *
 * @version $Revision: 1 $
 *
 * @see ThreadPoolExecutor
 * @see DirWatchdog
 * @see DefaultStreamingJob
 */
public class DirStreamingManager {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(DirStreamingManager.class);

	private static final int CORE_TREAD_POOL_SIZE = 10;
	private static final int MAX_TREAD_POOL_SIZE = 100;
	private static final long KEEP_ALIVE_TIME = 2 * 60L;
	private static final long DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT = 5 * 60L;
	private static final long DEFAULT_EXECUTORS_TERMINATION_TIMEOUT = 20;

	private long offerTimeout = DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT;
	private long executorsTerminationTimeout = DEFAULT_EXECUTORS_TERMINATION_TIMEOUT;

	private String dirPath;
	private String fileWildcardName;

	private ThreadPoolExecutor executorService;
	private List<Runnable> runningJobs = Collections.synchronizedList(new ArrayList<>());
	private DirWatchdog dirWatchdog;

	private String tnt4jCfgFilePath;

	private List<StreamingJobListener> streamingJobListeners;
	private List<JobFilesListener> jobFileListeners;

	/**
	 * Constructs an empty DirStreamingManager.
	 */
	protected DirStreamingManager() {
	}

	/**
	 * Constructs a new DirStreamingManager. Defines watched directory path.
	 *
	 * @param dirPath
	 *            watched directory path
	 */
	public DirStreamingManager(String dirPath) {
		this(dirPath, null);
	}

	/**
	 * Constructs a new DirStreamingManager. Defines watched directory path and monitored files wildcard name pattern.
	 *
	 * @param dirPath
	 *            watched directory path
	 * @param fileWildcardName
	 *            monitored files wildcard name pattern
	 */
	public DirStreamingManager(String dirPath, String fileWildcardName) {
		this.dirPath = dirPath;
		this.fileWildcardName = fileWildcardName;

		initialize();
	}

	private void initialize() {
		executorService = new ThreadPoolExecutor(CORE_TREAD_POOL_SIZE, MAX_TREAD_POOL_SIZE, KEEP_ALIVE_TIME,
				TimeUnit.SECONDS, new LinkedBlockingQueue<>(MAX_TREAD_POOL_SIZE * 2),
				new TNTInputStream.StreamsThreadFactory("DirStreamingManagerExecutorThread-")); // NON-NLS

		executorService.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					boolean added = executor.getQueue().offer(r, offerTimeout, TimeUnit.SECONDS);
					if (!added) {
						LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"TNTInputStream.tasks.buffer.limit", offerTimeout);
						notifyStreamingJobRejected(r);
					}
				} catch (InterruptedException exc) {
					Utils.logThrowable(LOGGER, OpLevel.WARNING,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"DirStreamingManager.job.offer.interrupted", ((StreamingJob) r).getJobId(), exc);
				}
			}
		});

		dirWatchdog = new DirWatchdog(dirPath, DirWatchdog.getDefaultFilter(fileWildcardName));
		dirWatchdog.addObserverListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileCreate(File file) {
				handleJobConfigCreate(file);
			}

			@Override
			public void onFileChange(File file) {
				handleJobConfigChange(file);
			}

			@Override
			public void onFileDelete(File file) {
				handleJobConfigRemoval(file);
			}
		});

		MBeansManager.registerMBeans();

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.dir.monitoring.started", dirPath, fileWildcardName);
	}

	/**
	 * Starts directory files streaming manager.
	 */
	public void start() {
		try {
			dirWatchdog.start();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"DirStreamingManager.could.not.start.watchdog", exc);
			stop();
		}
	}

	/**
	 * Stops directory streaming manager.
	 */
	public void stop() {
		cleanup();
	}

	private synchronized void shutdownExecutors() {
		if (executorService == null || executorService.isShutdown()) {
			return;
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(executorsTerminationTimeout, TimeUnit.SECONDS);
		} catch (InterruptedException exc) {
		} finally {
			List<Runnable> droppedJobs = executorService.shutdownNow();

			if (CollectionUtils.isNotEmpty(droppedJobs)) {
				for (Runnable job : droppedJobs) {
					notifyStreamingJobDropOff(job);
				}
			}
		}
	}

	/**
	 * Cleans up directory files streaming manager referenced objects after manager has stopped. Stops directory
	 * watchdog and shuts down executors.
	 */
	protected void cleanup() {
		try {
			dirWatchdog.stop();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"DirStreamingManager.could.not.stop.watchdog", exc);
		}

		shutdownExecutors();

		MBeansManager.unregisterMBeans();
	}

	/**
	 * Handles streaming job configuration file creation notification.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 */
	protected void handleJobConfigCreate(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.created", jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"DirStreamingManager.job.id.not.found", jobCfgFile.getName());
			return;
		}

		notifyJobFileAdded(jobCfgFile, jobId.toString(), JobFileState.ADDED);

		DefaultStreamingJob sJob = new DefaultStreamingJob(jobId, jobCfgFile, this);
		sJob.setTnt4jCfgFilePath(tnt4jCfgFilePath);

		if (CollectionUtils.isNotEmpty(streamingJobListeners)) {
			for (StreamingJobListener sjl : streamingJobListeners) {
				sJob.addStreamingJobListener(sjl);
			}
		}

		executorService.submit(sJob);
	}

	/**
	 * Handles streaming job configuration file change notification.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 */
	@SuppressWarnings("unlikely-arg-type")
	protected void handleJobConfigChange(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.changed", jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"DirStreamingManager.job.id.not.found", jobCfgFile.getName());
			return;
		}

		notifyJobFileAdded(jobCfgFile, jobId.toString(), JobFileState.CHANGED);

		synchronized (executorService) {
			// TODO: check job state - if already processing halt and restart???
			if (!executorService.getQueue().contains(jobId)) {
				handleJobConfigCreate(jobCfgFile);
			}
		}
	}

	/**
	 * Handles streaming job configuration file removal notification.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 */
	@SuppressWarnings("unlikely-arg-type")
	protected void handleJobConfigRemoval(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.deleted", jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"DirStreamingManager.job.id.not.found", jobCfgFile.getName());
			return;
		}

		notifyJobFileAdded(jobCfgFile, jobId.toString(), JobFileState.REMOVED);

		// TODO: maybe use cancel?
		synchronized (executorService) {
			for (Runnable r : executorService.getQueue()) {
				DefaultStreamingJob sJob = (DefaultStreamingJob) r;

				if (sJob.equals(jobId)) {
					// /sJob.cancel();
					executorService.remove(sJob);
					break;
				}
			}
		}
	}

	/**
	 * Adds running job instance to running tasks list.
	 *
	 * @param job
	 *            running job instance
	 */
	void addRunningTask(DefaultStreamingJob job) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.add.running", job.getJobId());
		runningJobs.add(job);
	}

	/**
	 * Removed stopped job instance from running tasks list.
	 *
	 * @param job
	 *            stopped job instance
	 */
	void removeRunningTask(DefaultStreamingJob job) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.remove.running", job.getJobId());
		runningJobs.remove(job);
	}

	/**
	 * Cancels streaming job execution.
	 *
	 * @param jobId
	 *            unique job identifier
	 */
	public void cancelJob(String jobId) {
		if (StringUtils.isEmpty(jobId)) {
			return;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.cancel", jobId);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.cancel.checking.running", jobId);

		for (Runnable r : runningJobs) {
			DefaultStreamingJob sJob = (DefaultStreamingJob) r;

			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"DirStreamingManager.job.running", sJob.getJobId());

			if (sJob.equals(jobId)) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"DirStreamingManager.job.cancel.running", jobId);
				sJob.cancel();
				runningJobs.remove(sJob);
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"DirStreamingManager.job.cancel.completed", jobId, runningJobs.size());
				return;
			}
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.cancel.checking.pending", jobId);

		synchronized (executorService) {
			for (Runnable r : executorService.getQueue()) {
				DefaultStreamingJob sJob = (DefaultStreamingJob) r;

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"DirStreamingManager.job.pending", sJob.getJobId());

				if (sJob.equals(jobId)) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"DirStreamingManager.job.cancel.pending", jobId);
					sJob.cancel();
					executorService.remove(sJob);
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"DirStreamingManager.job.cancel.completed", jobId, executorService.getQueue().size());
					return;
				}
			}
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"DirStreamingManager.job.not.found", jobId);
	}

	/**
	 * Sets TNT4J configuration file (tnt4j.properties) path.
	 *
	 * @param tnt4jCfgFilePath
	 *            TNT4J configuration file path string
	 */
	public void setTnt4jCfgFilePath(String tnt4jCfgFilePath) {
		this.tnt4jCfgFilePath = tnt4jCfgFilePath;
	}

	/**
	 * Adds defined {@link StreamingJobListener} to streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@link StreamingJobListener} to be added
	 */
	public void addStreamingJobListener(StreamingJobListener l) {
		if (l == null) {
			return;
		}

		if (streamingJobListeners == null) {
			streamingJobListeners = new ArrayList<>();
		}

		streamingJobListeners.add(l);
	}

	/**
	 * Removes defined {@link StreamingJobListener} from streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@link StreamingJobListener} to be removed
	 */
	public void removeStreamingJobListener(StreamingJobListener l) {
		if (l != null && streamingJobListeners != null) {
			streamingJobListeners.remove(l);
		}
	}

	/**
	 * Notifies that stream executor service has rejected offered files streaming job to queue.
	 *
	 * @param job
	 *            executor rejected files streaming job
	 */
	protected void notifyStreamingJobRejected(Runnable job) {
		if (streamingJobListeners != null) {
			for (StreamingJobListener l : streamingJobListeners) {
				l.onStatusChange((StreamingJob) job, null, StreamingJobStatus.REJECT);
			}
		}
	}

	/**
	 * Notifies that stream executor service has been shot down and unprocessed files streaming job has been dropped off
	 * the queue.
	 *
	 * @param job
	 *            executor dropped off files streaming job
	 */
	protected void notifyStreamingJobDropOff(Runnable job) {
		if (streamingJobListeners != null) {
			for (StreamingJobListener l : streamingJobListeners) {
				l.onStatusChange((StreamingJob) job, null, StreamingJobStatus.DROP_OFF);
			}
		}
	}

	/**
	 * Adds defined {@link JobFilesListener} to streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@link JobFilesListener} to be added
	 */
	public void addJobFileListener(JobFilesListener l) {
		if (l == null) {
			return;
		}

		if (jobFileListeners == null) {
			jobFileListeners = new ArrayList<>();
		}

		jobFileListeners.add(l);
	}

	/**
	 * Removes defined {@link JobFilesListener} from streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@link JobFilesListener} to be removed
	 */
	public void removeJobFileListener(JobFilesListener l) {
		if (l != null && jobFileListeners != null) {
			jobFileListeners.remove(l);
		}
	}

	/**
	 * Notifies that watchdog found new streaming configuration file.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 * @param jobId
	 *            job identifier
	 * @param fileState
	 *            new watchdog detected streaming job configuration file state
	 */
	protected void notifyJobFileAdded(File jobCfgFile, String jobId, JobFileState fileState) {
		if (jobFileListeners != null) {
			for (JobFilesListener l : jobFileListeners) {
				l.onJobFileStateChanged(jobCfgFile, jobId, fileState);
			}
		}
	}

	// /**
	// * Sample how to run directory streaming as standalone application.
	// *
	// * @param args
	// * command-line arguments. Supported arguments: dirPath
	// * fileWildcardName tnt4jCfgFilePath
	// *
	// * @throws Exception
	// * if an error occurs while running application
	// */
	// public static void main(String... args) throws Exception {
	// final DirStreamingManager dm = new DirStreamingManager(args[0], args[1]);
	// dm.setTnt4jCfgFilePath(args[2]);
	//
	// Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	// @Override
	// public void run() {
	// System.out.println("JVM exiting!...");
	// synchronized (dm) {
	// dm.notify();
	// }
	// dm.stop();
	// }
	// }));
	//
	// dm.start();
	// synchronized (dm) {
	// dm.wait();
	// }
	// }
}
