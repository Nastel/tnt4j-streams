/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.build.CfgStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsAgentTest {

	private StringWriter console;

	@Test
	public void testHelpArgument() throws Exception {
		interceptConsole();
		StreamsAgent.main("-h"); // NON-NLS
		System.out.flush();
		String string = console.getBuffer().toString();
		String expected = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.help")
				+ Utils.NEW_LINE;
		assertTrue("Console output does not contain expected string", string.contains(expected));
		Utils.close(console);
	}

	@Test
	public void testArgumentsFail() throws Exception {
		interceptConsole();
		String argument = "-test"; // NON-NLS
		StreamsAgent.main(argument);
		System.out.flush();
		String string = console.getBuffer().toString();
		String expected = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"StreamsAgent.invalid.argument", argument);
		expected += Utils.NEW_LINE;
		expected += StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.help");
		expected += Utils.NEW_LINE;
		assertTrue("Console output does not contain expected string", string.contains(expected));
		Utils.close(console);
	}

	@Test
	public void testFileEmptyFail() throws Exception {
		interceptConsole();
		String argument = "-f:"; // NON-NLS
		StreamsAgent.main(argument);
		System.out.flush();
		String string = console.getBuffer().toString();
		String expected = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"StreamsAgent.missing.cfg.file", argument);
		expected += Utils.NEW_LINE;
		expected += StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.help");
		expected += Utils.NEW_LINE;
		assertTrue("Console output does not contain expected string", string.contains(expected));
		Utils.close(console);
	}

	@Test
	public void testRunFromAPI() throws Exception {
		String testStreamName = "TestStream"; // NON-NLS
		File tempConfFile = File.createTempFile("testConfiguration", ".xml");
		FileWriter fw = new FileWriter(tempConfFile);
		String sb = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + Utils.NEW_LINE + "<tnt-data-source" + Utils.NEW_LINE // NON-NLS
				+ "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + Utils.NEW_LINE // NON-NLS
				+ "        xsi:noNamespaceSchemaLocation=\"https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd\">" // NON-NLS
				+ Utils.NEW_LINE + "    <stream name=\"" + testStreamName // NON-NLS
				+ "\" class=\"com.jkoolcloud.tnt4j.streams.inputs.CharacterStream\">" + Utils.NEW_LINE // NON-NLS
				+ "        <property name=\"HaltIfNoParser\" value=\"false\"/>" + Utils.NEW_LINE // NON-NLS
				+ "        <property name=\"Port\" value=\"9595\"/>" + Utils.NEW_LINE + "    </stream>" + Utils.NEW_LINE // NON-NLS
				+ "</tnt-data-source>"; // NON-NLS
		fw.write(sb);
		fw.flush();
		Utils.close(fw);
		StreamsAgent.runFromAPI(new CfgStreamsBuilder().setConfig(tempConfFile.getAbsolutePath()));
		Thread.sleep(500);
		tempConfFile.delete();
		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		for (Thread thread : threads) {
			if (thread.getName().contains(testStreamName)) {
				return;
			} else {
				continue;
			}
		}
		fail("No streams thread created");
	}

	private void interceptConsole() throws InterruptedException {
		console = new StringWriter();
		WriterOutputStream writerOutputStream = new WriterOutputStream(console, StandardCharsets.UTF_8);
		PrintStream out = new PrintStream(writerOutputStream);
		System.setOut(out);
		Thread.sleep(50);
	}
}