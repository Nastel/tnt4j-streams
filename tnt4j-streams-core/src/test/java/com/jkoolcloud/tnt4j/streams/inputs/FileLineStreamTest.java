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

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class FileLineStreamTest {

	FileLineStream fls;

	@Before
	public void prepareTest() {
		fls = new FileLineStream();

		StreamThread st = mock(StreamThread.class);
		st.setName("FileLineStreamTestThreadName"); // NON-NLS
		fls.setOwnerThread(st);
	}

	@Test
	public void searchFilesTest() throws Exception {
		TestFileList files = new TestFileList(false);

		String fileName = files.get(0).getParentFile() + File.separator + files.getPrefix() + "*.TST"; // NON-NLS

		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_FILENAME, fileName);
		props.put(StreamProperties.PROP_RESTORE_STATE, String.valueOf(false));
		fls.setProperties(props.entrySet());
		fls.startStream();
		AbstractFileLineStream<Path>.FileWatcher fileWatcher = fls.createFileWatcher();
		// TODO assert smth
		fileWatcher.shutdown();

		fls.cleanup();
		files.cleanup();
	}

	@Test
	public void testProperties() throws Exception {
		TestFileList testFiles = new TestFileList(true);
		Map<String, String> props = new HashMap<>(6);
		props.put(StreamProperties.PROP_FILENAME, testFiles.getWildcardName());
		props.put(StreamProperties.PROP_START_FROM_LATEST, String.valueOf(false));
		props.put(StreamProperties.PROP_FILE_READ_DELAY, String.valueOf(0));
		props.put(StreamProperties.PROP_FILE_POLLING, String.valueOf(false));
		props.put(StreamProperties.PROP_RESTORE_STATE, String.valueOf(false));
		props.put(StreamProperties.PROP_USE_EXECUTOR_SERVICE, String.valueOf(false));
		fls.setProperties(props.entrySet());
		testPropertyList(fls, props.entrySet());
		fls.startStream();
		assertEquals("TEST0", fls.getNextItem().toString());
		assertEquals("TEST1", fls.getNextItem().toString());
		assertEquals("TEST2", fls.getNextItem().toString());
		assertEquals("TEST3", fls.getNextItem().toString());
		assertEquals("TEST4", fls.getNextItem().toString());
	}

}
