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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJavaObjectParserTest {
	ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
	AbstractBufferedStream<?> stream = Mockito.mock(TestUtils.SimpleTestStream.class, Mockito.CALLS_REAL_METHODS);

	@Test
	public void setPropertiesTest() {

		HashMap<String, String> myMap = new HashMap<>();
		myMap.put(ParserProperties.PROP_LOC_PATH_DELIM, "TEST_DELIM"); // NON-NLS
		Collection<Map.Entry<String, String>> props = myMap.entrySet();
		testParser.setProperties(props);
	}

	@Test
	public void setPropertiesWhenNullTest() {
		testParser.setProperties(null);
	}

	@Test
	public void isDataClassSupportedTest() throws Exception {
		assertTrue(testParser.isDataClassSupported("TEST")); // NON-NLS
	}

	@Test
	public void parseTest() throws Exception {
		testParser.parse(stream, "test"); // NON-NLS
	}

	@Test
	public void parseWhenDataNullTest() throws Exception {
		assertNull(testParser.parse(stream, null));
	}

	private ActivityJavaObjectParser.ActivityContext makeContext(TNTInputStream<?, ?> stream, Object data) {
		return testParser.new ActivityContext(stream, null, data);
	}

	@Test
	public void getLocatorValueExceptionTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "555"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, "")));
	}

	@Test
	public void getLocatorValueWhenLocatorIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		assertNull(testParser.getLocatorValue(null, makeContext(stream, "")));
	}

	@Test(expected = NumberFormatException.class)
	public void getLocatorValueWhenLocatorIsEmptyTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "");
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, "")));
	}

	@Test
	public void getLocatorValueWhenTypeisStreamPropTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp,
				StreamProperties.PROP_EXECUTOR_THREADS_QTY);
		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		props.put(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5");
		stream.setProperties(props.entrySet());
		assertEquals(5, testParser.getLocatorValue(fieldLocator, makeContext(stream, "")));
	}

	@Test(expected = NumberFormatException.class)
	public void getFieldValueWhenDataIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "test"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, null)));
	}

	@Test(expected = NumberFormatException.class)
	public void getFieldValueWhenPathIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "."); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, "")));
	}

	@Test(expected = NumberFormatException.class)
	@Ignore("Not finished")
	public void getFieldValueTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label,
				"testName.testNumber"); // NON-NLS
		MyClassTest prop = new MyClassTest();
		testParser.getLocatorValue(fieldLocator, makeContext(stream, prop));
	}

	@Test(expected = NumberFormatException.class)
	@Ignore("Not finished")
	public void getFieldValueWhenTwoSameFieldsTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label,
				"testNumber.isActive"); // NON-NLS
		MyClassTest prop = new MyClassTest();
		Object locValue = testParser.getLocatorValue(fieldLocator, makeContext(stream, prop));
		assertNotNull(locValue);
		assertEquals("Expected value does not match", prop.isActive, locValue);
	}

	@Test(expected = NumberFormatException.class)
	public void getFieldValueWhenOneFieldTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "testNumber"); // NON-NLS
		MyClassTest prop = new MyClassTest();
		Object locValue = testParser.getLocatorValue(fieldLocator, makeContext(stream, prop));
		assertNotNull(locValue);
		assertEquals("Expected value does not match", prop.testNumber, locValue);
	}

	public class MyClassTest {
		public String testName = "Test Name"; // NON-NLS
		public int testNumber = 123456789;
		public boolean isActive = false;
	}
}