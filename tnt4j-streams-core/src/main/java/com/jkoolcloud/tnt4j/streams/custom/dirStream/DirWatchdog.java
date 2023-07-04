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
import java.io.FileFilter;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * This class implements directory watchdog.
 * <p>
 * Monitoring is performed with defined refresh interval. Default refresh interval value is 5sec, but can be overridden
 * defining new value in milliseconds scale.
 * <p>
 * Monitored files can be filtered defining file filter.
 * <p>
 * Notifications about file or file state changes are published over added file alteration listeners.
 *
 * @version $Revision: 1 $
 */
public class DirWatchdog {
	private static final long DEFAULT_WATCH_REFRESH_INTERVAL = TimeUnit.SECONDS.toMillis(5);

	private String dirPath;
	private long interval;
	private FileFilter filter = null;

	private FileAlterationObserver observer = null;
	private FileAlterationMonitor monitor = null;

	/**
	 * Constructs a new DirWatchdog. Monitoring is performed on all files without filtering with refresh interval every
	 * 5sec.
	 *
	 * @param dirPath
	 *            watched directory path string
	 */
	public DirWatchdog(String dirPath) {
		this(dirPath, DEFAULT_WATCH_REFRESH_INTERVAL, null);
	}

	/**
	 * Constructs a new DirWatchdog. Monitoring is performed on all files without filtering.
	 *
	 * @param dirPath
	 *            watched directory path string
	 * @param refreshInterval
	 *            watchdog refresh interval in milliseconds
	 */
	public DirWatchdog(String dirPath, long refreshInterval) {
		this(dirPath, refreshInterval, null);
	}

	/**
	 * Constructs a new DirWatchdog. Sets default refresh interval to 5 sec.
	 *
	 * @param dirPath
	 *            watched directory path string
	 * @param filter
	 *            file filter
	 */
	public DirWatchdog(String dirPath, FileFilter filter) {
		this(dirPath, DEFAULT_WATCH_REFRESH_INTERVAL, filter);
	}

	/**
	 * Constructs a new DirWatchdog.
	 *
	 * @param dirPath
	 *            watched directory path string
	 * @param refreshInterval
	 *            watchdog refresh interval in milliseconds
	 * @param filter
	 *            file filter
	 */
	public DirWatchdog(String dirPath, long refreshInterval, FileFilter filter) {
		if (StringUtils.isEmpty(dirPath)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "DirWatchdog.empty.dir.path"));
		}

		this.dirPath = dirPath;
		this.interval = refreshInterval;
		this.filter = filter;

		initialize();
	}

	private void initialize() {
		observer = new FileAlterationObserver(new File(dirPath), filter);

		monitor = new FileAlterationMonitor(interval);
		monitor.addObserver(observer);
	}

	/**
	 * Adds defined {@link FileAlterationListener} to directory observer file alteration listeners list.
	 *
	 * @param l
	 *            {@link FileAlterationListener} to be added
	 */
	public void addObserverListener(FileAlterationListener l) {
		if (l != null) {
			observer.addListener(l);
		}
	}

	/**
	 * Starts directory watchdog.
	 *
	 * @throws Exception
	 *             if exception occurs while starting monitor
	 */
	public void start() throws Exception {
		if (monitor != null) {
			monitor.start();
		}
	}

	/**
	 * Stops directory watchdog.
	 *
	 * @throws Exception
	 *             if exception occurs while stopping monitor
	 */
	public void stop() throws Exception {
		if (monitor != null) {
			monitor.stop();
		}
	}

	/**
	 * Creates default file filter matching file wildcard name pattern.
	 *
	 * @param fileWildcardName
	 *            file wildcard name pattern string
	 *
	 * @return created file filter
	 */
	public static FileFilter getDefaultFilter(String fileWildcardName) {
		if (StringUtils.isEmpty(fileWildcardName)) {
			return null;
		}

		IOFileFilter directories = FileFilterUtils.and(FileFilterUtils.directoryFileFilter(), HiddenFileFilter.VISIBLE);

		IOFileFilter files = FileFilterUtils.and(FileFilterUtils.fileFileFilter(),
				WildcardFileFilter.builder().setWildcards(fileWildcardName).setIoCase(IOCase.INSENSITIVE).get());

		IOFileFilter filter = FileFilterUtils.or(directories, files);

		return filter;
	}
}
