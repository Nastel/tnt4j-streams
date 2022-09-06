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

import java.io.InputStream;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Extends {@link ZipLineStream} to allow loading Zip file from HDFS.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports configuration properties from {@link ZipLineStream} (and higher hierarchy streams).
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class HdfsZipLineStream extends ZipLineStream {
	private FileSystem fs = null;

	/**
	 * Constructs a new HdfsZipLineStream.
	 */
	public HdfsZipLineStream() {
		super();
	}

	/**
	 * Loads zip file as input stream to read.
	 * 
	 * @param zipPath
	 *            Hdfs zip file path URL string
	 *
	 * @return file data input stream to read
	 *
	 * @throws Exception
	 *             If path defined file is not found or can't be opened
	 */
	@Override
	protected InputStream loadFile(String zipPath) throws Exception {
		URI fileUri = new URI(zipPath);
		if (fs == null) {
			fs = FileSystem.get(fileUri, new Configuration());
		}
		Path filePath = new Path(fileUri);

		return fs.open(filePath);
	}

	@Override
	protected void cleanup() {
		super.cleanup();

		Utils.close(fs);
	}
}
