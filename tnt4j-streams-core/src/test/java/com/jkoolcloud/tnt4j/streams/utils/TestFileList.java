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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author akausinis
 * @version 1.0
 */
public class TestFileList extends ArrayList<File> {

	private static final String SUFFIX = ".TST"; // NON-NLS
	public static final int TEST_FILE_LIST_SIZE = 5;
	private static final long serialVersionUID = 1L;

	private String prefix;

	public TestFileList() throws IOException, InterruptedException {
		this(false);
	}

	public TestFileList(boolean fill) throws IOException, InterruptedException {
		super();
		final int count = TEST_FILE_LIST_SIZE;
		prefix = "TEST" + String.valueOf(System.currentTimeMillis()).substring(5); // NON-NLS

		for (Integer i = 0; i < count; i++) {
			File tempFile = File.createTempFile(prefix, SUFFIX);
			if (fill) {
				FileOutputStream fos = new FileOutputStream(tempFile);
				fos.write("TEST".getBytes()); // NON-NLS
				fos.write(String.valueOf(i).getBytes());
				fos.close();
			}
			tempFile.deleteOnExit();
			if (count / 2 >= i) {
				// tempFile.setLastModified(System.currentTimeMillis() + 50000);
			}
			this.add(tempFile);
			Thread.sleep(1);
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public void cleanup() {
		for (File file : this) {
			file.delete();
		}
	}

	public String getWildcardName() {
		return this.get(0).getParent() + File.separator + prefix + "*" + SUFFIX; // NON-NLS

	}
}
