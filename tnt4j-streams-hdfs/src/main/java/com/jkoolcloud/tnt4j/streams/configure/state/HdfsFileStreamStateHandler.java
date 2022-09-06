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

package com.jkoolcloud.tnt4j.streams.configure.state;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements streamed HDFS files access state handler.
 * 
 * @version $Revision: 1 $
 */
public class HdfsFileStreamStateHandler extends AbstractFileStreamStateHandler<Path> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(HdfsFileStreamStateHandler.class);

	FileSystem fs;

	/**
	 * Constructs a new HdfsFileStreamStateHandler.
	 * 
	 * @param fs
	 *            file system
	 */
	HdfsFileStreamStateHandler(FileSystem fs) {
		this.fs = fs;
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Constructs a new HdfsFileStreamStateHandler. Performs search of persisted streaming state and loads it if such is
	 * available.
	 *
	 * @param activityFiles
	 *            files processed by stream
	 * @param fs
	 *            file system
	 * @param streamName
	 *            stream name
	 */
	public HdfsFileStreamStateHandler(Path[] activityFiles, FileSystem fs, String streamName) {
		this(fs);
		initialize(activityFiles, streamName);
	}

	@Override
	String getParent(Path[] activityFiles) {
		return activityFiles[0].getParent().getName();
	}

	@Override
	Reader openFile(Path file) throws IOException {
		return new InputStreamReader(fs.open(file), Charset.defaultCharset());
	}

	@Override
	public boolean isStreamedFileAvailable() {
		try {
			FileStatus fStatus = file == null ? null : fs.getFileStatus(file);
			return fStatus != null && fStatus.isFile();
		} catch (IOException exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileStreamStateHandler.file.error", exc);
			return false;
		}
	}
}
