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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.HdfsStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class HdfsFileLineStreamTest {

	private final class TestInputStreamStub extends InputStream implements PositionedReadable, Seekable {
		@Override
		public int read() throws IOException {
			return -1;
		}

		@Override
		public int read(long position, byte[] buffer, int offset, int length) throws IOException {
			return -1;
		}

		@Override
		public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
		}

		@Override
		public void readFully(long position, byte[] buffer) throws IOException {
		}

		@Override
		public void seek(long pos) throws IOException {
		}

		@Override
		public long getPos() throws IOException {
			return 0;
		}

		@Override
		public boolean seekToNewSource(long targetPos) throws IOException {
			return false;
		}
	}

	@Test()
	public void test() throws Exception {
		FileSystem fs = mock(FileSystem.class);
		HdfsFileLineStream stream = new HdfsFileLineStream();

		TestFileList files = new TestFileList(false);

		String fileName = ("file:////" + files.get(0).getParentFile() + File.separator + files.getPrefix() // NON-NLS
				+ "*.TST").replace("\\", "/"); // NON-NLS

		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_FILENAME, fileName);
		props.put(StreamProperties.PROP_RESTORE_STATE, "false"); // NON-NLS

		when(fs.open(any(Path.class))).thenReturn(new FSDataInputStream(new TestInputStreamStub()));
		FileStatus fileStatusMock = mock(FileStatus.class);
		FileStatus[] array = new FileStatus[10];
		Arrays.fill(array, fileStatusMock);
		when(fs.listStatus(any(Path.class), any(PathFilter.class))).thenReturn(array);
		when(fileStatusMock.getModificationTime()).thenReturn(1L, 2L, 3L);
		when(fileStatusMock.getPath()).thenReturn(mock(Path.class));
		when(fs.getContentSummary(any(Path.class))).thenReturn(mock(ContentSummary.class));

		Method m = FileSystem.class.getDeclaredMethod("addFileSystemForTesting", URI.class, Configuration.class, // NON-NLS
				FileSystem.class);
		m.setAccessible(true);
		m.invoke(FileSystem.class, URI.create(fileName), new Configuration(), fs);

		StreamThread st = mock(StreamThread.class);
		st.setName("HdfsFileLineStreamTestThreadName"); // NON-NLS
		stream.setOwnerThread(st);

		stream.setProperties(props.entrySet());
		stream.startStream();

		verify(fileStatusMock, atLeastOnce()).getModificationTime();
		verify(fileStatusMock, atLeastOnce()).getPath();
		verify(fs, atLeastOnce()).listStatus(any(Path.class), any(PathFilter.class));

		stream.cleanup();
	}

	@Test
	public void testRB() {
		String keyModule = "HdfsFileLineStream.reading.changes";
		String keyCore = "ActivityField.field.type.name.empty";
		String brbStr;

		String rbs1 = StreamsResources.getString(HdfsStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Hdfs resource bundle entry not found", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Hdfs resource bundle entry found in core", rbs1, keyModule);
		brbStr = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, brbStr);
		rbs1 = StreamsResources.getString(HdfsStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in hdfs", brbStr, rbs1);
	}

}
