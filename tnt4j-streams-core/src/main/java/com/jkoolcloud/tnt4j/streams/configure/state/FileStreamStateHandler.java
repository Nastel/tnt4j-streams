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

package com.jkoolcloud.tnt4j.streams.configure.state;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements streamed files access state handler.
 *
 * @version $Revision: 1 $
 */
public class FileStreamStateHandler extends AbstractFileStreamStateHandler<Path> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(FileStreamStateHandler.class);

	/**
	 * Constructs a new FileStreamStateHandler.
	 */
	FileStreamStateHandler() {
		super();
	}

	/**
	 * Constructs a new FileStreamStateHandler. Performs search of persisted streaming state and loads it if such is
	 * available.
	 *
	 * @param activityFiles
	 *            files processed by stream
	 * @param streamName
	 *            stream name
	 */
	public FileStreamStateHandler(Path[] activityFiles, String streamName) {
		super(activityFiles, streamName);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	String getParent(Path[] activityFiles) {
		return activityFiles[0].getParent().toString();
	}

	@Override
	Reader openFile(Path file) throws IOException {
		return Files.newBufferedReader(file, Charset.defaultCharset());
	}

	@Override
	public boolean isStreamedFileAvailable() {
		return file != null && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
	}

	@Override
	FileAccessState loadState(String path, String streamName) throws IOException, JAXBException {
		// Case 1: state file in same dir as streamed files
		FileAccessState fas = loadStateFile(path, streamName);
		if (fas == null) {
			// Case 2: state file is in temp. dir
			fas = super.loadState(path, streamName);
		}

		return fas;
	}
}
