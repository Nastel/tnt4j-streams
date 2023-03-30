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

package com.jkoolcloud.tnt4j.streams.inputs.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.FileLineStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Input stream extension to support reading multiple files matching defined system-dependent file name or wildcard file
 * name pattern.
 * <p>
 * When reading of file is complete, inner {@link FileInputStream} for that file is closed and new instance of inner
 * {@link FileInputStream} is created for next existing file from list.
 *
 * @version $Revision: 1 $
 *
 * @see FileInputStream
 * @see Utils#listFilesByName(String, java.nio.file.FileSystem)
 */
public class FilesInputStream extends InputStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(FileLineStream.class);

	private Iterator<Path> files;
	private InputStream fis;

	/**
	 * Constructs a new FilesInputStream.
	 *
	 * @param fileName
	 *            system-dependent file name or wildcard file name pattern to read matching files
	 */
	public FilesInputStream(String fileName) {
		try {
			files = Arrays.asList(Utils.listFilesByName(fileName)).iterator();
		} catch (IOException e) {
			nextFileStream();
		}

		nextFileStream();
	}

	/**
	 * Continues reading form next available file if an EOF is reached.
	 */
	protected void nextFileStream() {
		if (fis != null) {
			Utils.close(fis);
		}

		while (true) {
			if (files.hasNext()) {
				Path f = files.next();
				if (Files.exists(f)) {
					try {
						fis = Files.newInputStream(f);
						break;
					} catch (FileNotFoundException exc) {
						LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FeedInputStream.file.not.found", f);
					} catch (IOException e) {
						LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FeedInputStream.file.not.found", f); // TODO
					}
				}
			} else {
				fis = null;
				break;
			}
		}
	}

	@Override
	public int available() throws IOException {
		if (fis == null) {
			return 0;
		}

		return fis.available();
	}

	@Override
	public int read() throws IOException {
		if (fis == null) {
			return -1;
		}

		int c = fis.read();
		if (c == -1) {
			nextFileStream();
			return read();
		}
		return c;
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if (fis == null) {
			return -1;
		}

		int n = fis.read(b, off, len);
		if (n <= 0) {
			nextFileStream();
			return read(b, off, len);
		}
		return n;
	}

	@Override
	public void close() throws IOException {
		do {
			nextFileStream();
		} while (fis != null);
	}
}
