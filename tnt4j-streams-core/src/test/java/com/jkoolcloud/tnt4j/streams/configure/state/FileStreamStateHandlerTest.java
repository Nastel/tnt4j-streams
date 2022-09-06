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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class FileStreamStateHandlerTest {
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

	@Test
	public void findStreamingFile() throws Exception {
		FileStreamStateHandler rwd = new FileStreamStateHandler();

		File testFilesDir = new File(samplesDir, "/multiple-logs/");
		Path path = Paths.get(testFilesDir.getAbsolutePath());
		Path[] testFiles = Utils.searchFiles(path.toString() + File.separator + "orders*", null); // NON-NLS
		FileAccessState newFAS = new FileAccessState();

		int count = 0;
		Path fileToSearchFor = null;
		int lineLastRead = 0;
		File fileWritten = null;
		for (Path testFile : testFiles) {
			count++;
			LineNumberReader reader;

			Long fileCRC = rwd.getFileCrc(testFile);
			if (count == 2) {
				newFAS.currentFileCrc = fileCRC;
				fileToSearchFor = testFile;
			}

			reader = Utils.getFileReader(testFile.toFile());
			reader.setLineNumber(0);
			String line = reader.readLine();
			int count2 = 0;
			while (line != null) {
				count2++;
				Checksum crcLine = new CRC32();
				byte[] bytes4Line = line.getBytes();
				crcLine.update(bytes4Line, 0, bytes4Line.length);
				long lineCRC = crcLine.getValue();
				int lineNumber = reader.getLineNumber();
				System.out.println("for " + lineNumber + " line CRC is " + lineCRC); // NON-NLS
				if (count2 == 3) {
					newFAS.currentLineCrc = lineCRC;
					newFAS.currentLineNumber = lineNumber;
					newFAS.lastReadTime = System.currentTimeMillis();
					lineLastRead = lineNumber;
				}
				line = reader.readLine();
			}
			fileWritten = AbstractFileStreamStateHandler.writeState(newFAS, testFilesDir, "TestStream"); // NON-NLS
			Utils.close(reader);
		}

		Path findLastProcessed = rwd.findStreamingFile(newFAS, testFiles);
		assertEquals(fileToSearchFor, findLastProcessed);
		int lineLastReadRecorded = rwd.checkLine(findLastProcessed, newFAS);
		assertEquals(lineLastRead, lineLastReadRecorded);
		fileWritten.delete();
	}

}
