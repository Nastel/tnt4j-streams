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

package com.jkoolcloud.tnt4j.streams.configure;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsConfigLoaderTest {
	private static final boolean DEFAULT_CONFIG = true;
	private static String TEST_FILE_NAME;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initSamplesDir();
	}

	private static void initSamplesDir() throws Exception {
		File samplesDir = new File("./samples/");
		if (!samplesDir.isDirectory()) {
			samplesDir = new File("./tnt4j-streams-core/samples/");
			if (!samplesDir.isDirectory()) {
				fail("Samples root directory doesn't exist");
			}
		}

		TEST_FILE_NAME = samplesDir.getPath() + "/apache-access-multi-log/tnt-data-source.xml"; // NON-NLS

		File testfile = new File(TEST_FILE_NAME);
		assertTrue("Test configuration failure: XML file doesn't exist ", testfile.exists());
	}

	@AfterClass
	public static void tearDown() throws Exception {
	}

	@Test
	public void testStreamsConfig() throws Exception {
		StreamsConfigLoader streamsConfig = new StreamsConfigLoader();
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

	private void isStreamsAndParsersLoaded(StreamsConfigLoader streamsConfig) {
		if (DEFAULT_CONFIG) {
			assertNotNull("Has no parsers defined", streamsConfig.getParsers());
			assertNotNull("Has no streams defined", streamsConfig.getStreams());
		}
	}

	@Test
	public void testStreamsConfigConfigWithFileName() throws Exception {
		StreamsConfigLoader streamsConfig = new StreamsConfigLoader(TEST_FILE_NAME);
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

	@Test
	public void testStreamsConfigSAXException() throws Exception {
		File tempFile = File.createTempFile("testStreams", null);
		File configFile = new File(TEST_FILE_NAME);
		FileWriter fw = new FileWriter(tempFile);
		FileReader fr = new FileReader(configFile);
		long filSize = configFile.length();
		char[] buffer = new char[(int) filSize];
		fr.read(buffer);
		fw.write(buffer);
		fw.write("ERROR<>"); // NON-NLS
		Utils.close(fw);
		Utils.close(fr);
		new StreamsConfigLoader(TEST_FILE_NAME);
	}

	@Test(expected = IOException.class)
	public void testStreamsConfigConfigWithFileNameNotFound() throws Exception {
		new StreamsConfigLoader(TEST_FILE_NAME + "ERROR"); // NON-NLS
	}

	@Test
	public void testStreamsConfigFile() throws Exception {
		File configFile = new File(TEST_FILE_NAME);
		StreamsConfigLoader streamsConfig = new StreamsConfigLoader(configFile);
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

	@Test
	public void testStreamsConfigReader() throws Exception {
		File configFile = new File(TEST_FILE_NAME);
		FileReader fileReader = new FileReader(configFile);
		StreamsConfigLoader streamsConfig = new StreamsConfigLoader(fileReader);
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
		Utils.close(fileReader);
	}

}
