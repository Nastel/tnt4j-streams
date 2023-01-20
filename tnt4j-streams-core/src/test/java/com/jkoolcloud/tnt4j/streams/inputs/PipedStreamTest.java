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

package com.jkoolcloud.tnt4j.streams.inputs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class PipedStreamTest {

	private static File samplesDir;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initSamplesDir();
	}

	private static void initSamplesDir() throws Exception {
		samplesDir = new File("./samples/");
		if (!samplesDir.isDirectory()) {
			samplesDir = new File("./tnt4j-streams-core/samples/");
			if (!samplesDir.isDirectory()) {
				fail("Samples root directory doesn't exist");
			}
		}
	}

	@Test(timeout = 1000L)
	public void testPipeFlow() throws Exception {
		System.setIn(Files.newInputStream(new File(samplesDir, "/piping-stream/orders.log").toPath()));

		PipedStream pipedStream = new PipedStream();
		pipedStream.startStream();
		String line = pipedStream.getNextItem();
		pipedStream.cleanup();

		assertNotNull("Next item line is null", line);
		assertEquals("Expected next item line length does not match", 89, line.length());
	}

	@Test(expected = IllegalStateException.class)
	public void testNullRawInput() throws Exception {
		PipedStream pipedStream = new PipedStream((Reader) null);
		pipedStream.startStream();
		pipedStream.cleanup();
	}

	@Test(expected = IllegalStateException.class)
	public void testNextItemAfterCleanup() throws Exception {
		PipedStream pipedStream = new PipedStream();
		pipedStream.startStream();
		pipedStream.cleanup();
		pipedStream.getNextItem();
	}
}