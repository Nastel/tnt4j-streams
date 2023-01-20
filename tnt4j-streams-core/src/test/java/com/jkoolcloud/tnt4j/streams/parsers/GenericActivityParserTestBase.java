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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.UtilsTest;

/**
 * @author akausinis
 * @version 1.0
 */
public abstract class GenericActivityParserTestBase<P extends GenericActivityParser<T>, T>
		extends ActivityParserTestBase<P> {

	@Override
	@Test
	public void isDataClassSupportedTest() {
		assertTrue(parser.isDataClassSupported("TEST")); // NON-NLS
		assertTrue(parser.isDataClassSupported("TEST".getBytes())); // NON-NLS
		assertTrue(parser.isDataClassSupported(mock(Reader.class)));
		assertTrue(parser.isDataClassSupported(mock(InputStream.class)));
		assertFalse(parser.isDataClassSupported(this.getClass()));
	}

	public void parsePreparedItemTest() {
		mock(TNTInputStream.class);
	}

	@Test
	public void getNextString() throws Exception {
		final String testString = "Test\n"; // NON-NLS
		final String expectedString = "Test"; // NON-NLS
		StringReader reader = UtilsTest.toReader(testString);
		ByteArrayInputStream inputStream = UtilsTest.toInputStream(testString);
		List<Object> testCases = new ArrayList<Object>() {
			private static final long serialVersionUID = 1L;
			{
				add(expectedString);
				add(expectedString.getBytes());
				add(reader);
				add(inputStream);
			}
		};
		for (Object data : testCases) {
			System.out.println(data.getClass());
			assertEquals(expectedString, parser.getNextActivityString(data));
		}
		assertNull(parser.getNextActivityString(null));
	}

	@Test
	public void getNextStringWhenBufferedReaderInstanceTest() {
		InputStream textStream = new ByteArrayInputStream("test".getBytes()); // NON-NLS
		BufferedReader br = new BufferedReader(new InputStreamReader(textStream));
		assertEquals("test", parser.getNextActivityString(br));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getNextStringWhenOtherInstanceTest() {
		parser.getNextActivityString(555);
	}

	protected P.ActivityContext makeContext(TNTInputStream<?, ?> stream, T data) {
		return parser.new ActivityContext(stream, null, data);
	}
}
