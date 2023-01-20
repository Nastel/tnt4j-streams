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

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class DirStreamingManagerTest {

	DirStreamingManager dsm;

	@Before
	public void prepare() throws IOException {
		File tmpFile = File.createTempFile("TEST", ".TST");
		tmpFile.deleteOnExit();
		File parentFile = tmpFile.getParentFile();
		dsm = new DirStreamingManager(parentFile.getAbsolutePath(), "tnt-data-source*.xml"); // NON-NLS
	}

	@Test
	public void testStart() throws IOException {
		StreamingJobListener mock = mock(StreamingJobListener.class);
		File tmpFile = File.createTempFile("tnt-data-source_" + UUID.randomUUID().toString(), ".XML");
		tmpFile.deleteOnExit();
		dsm.addStreamingJobListener(mock);
		dsm.start();
	}

	@Test
	public void testStop() {
		dsm.stop();
	}

	@Test
	public void testSetTnt4jCfgFilePath() {
		dsm.setTnt4jCfgFilePath("");
	}

	@Test
	public void testAddStreamingJobListener() {
	}

}
