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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class ZipLineStreamTest {

	ZipLineStream zs;

	@Before
	public void prepare() {
		zs = new ZipLineStream();
	}

	@Test
	public void testProperties() {
		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_FILENAME, "test.zip"); // NON-NLS
		props.put(StreamProperties.PROP_ARCH_TYPE, "ZIP"); // NON-NLS
		zs.setProperties(props.entrySet());
		testPropertyList(zs, props.entrySet());
	}

	@Test
	public void initializeTest() throws Exception {
		TestFileList testFiles = new TestFileList(true);
		byte[] buffer = new byte[1024];
		File zipFile = File.createTempFile("testZip", ".zip");
		zipFile.deleteOnExit();
		OutputStream os = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(os);
		for (File testfile : testFiles) {
			ZipEntry zipEntry = new ZipEntry(testfile.getName());
			zos.putNextEntry(zipEntry);
			FileInputStream fis = new FileInputStream(testfile);
			int length;
			while ((length = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, length);
			}
			zipEntry.setSize(buffer.length);
			zos.closeEntry();
			fis.close();
		}
		zos.close();
		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_FILENAME, zipFile.getAbsolutePath());
		props.put(StreamProperties.PROP_ARCH_TYPE, "ZIP"); // NON-NLS
		zs.setProperties(props.entrySet());
		zs.startStream();
		assertEquals("TEST0", zs.getNextItem());
		assertEquals("TEST1", zs.getNextItem());
		assertEquals("TEST2", zs.getNextItem());
		assertEquals("TEST3", zs.getNextItem());
		assertEquals("TEST4", zs.getNextItem());
		zs.cleanup();
	}

}
