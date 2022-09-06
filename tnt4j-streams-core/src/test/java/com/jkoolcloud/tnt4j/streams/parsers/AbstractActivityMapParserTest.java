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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class AbstractActivityMapParserTest {

	ActivityMapParser testParser = new ActivityMapParser();
	AbstractBufferedStream<?> stream = new TestUtils.SimpleTestStream();

	@Test
	public void setPropertiesTest() {
		AbstractActivityMapParser testParser = Mockito.mock(ActivityMapParser.class, Mockito.CALLS_REAL_METHODS);
		HashMap<String, String> myMap = new HashMap<>();
		myMap.put(ParserProperties.PROP_VAL_DELIM, ";");
		myMap.put(ParserProperties.PROP_LOC_PATH_DELIM, "TEST_DELIM"); // NON-NLS
		Collection<Map.Entry<String, String>> props = myMap.entrySet();
		testParser.setProperties(props);
	}

	@Test
	public void parseWhenDataIsNullTest() throws Exception {
		TNTInputStream<?, ?> my = Mockito.mock(TNTInputStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.parse(my, null));
	}

	@Test
	public void parseTest() throws Exception {
		Map<String, String> myMap = new HashMap<>();
		myMap.put("test", "OK"); // NON-NLS
		myMap.put("status", "finished"); // NON-NLS
		testParser.parse(stream, myMap);
	}

	@Test
	public void parseWhenDataIsEmptyTest() throws Exception {
		Map<String, String> myMap = new HashMap<>();
		assertNull(testParser.parse(stream, myMap));
	}

	private ActivityMapParser.ActivityContext makeContext(TNTInputStream<?, ?> stream, Map<String, ?> data) {
		return testParser.new ActivityContext(stream, null, data);
	}

	@Test
	public void getLocatorValueTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "555"); // NON-NLS
		Map<String, String> myMap = new HashMap<>();
		myMap.put("test", "OK"); // NON-NLS
		myMap.put("555", "hello"); // NON-NLS
		assertEquals("hello", testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueWhenFieldLocatorNullTest() throws Exception {
		Map<String, String> myMap = new HashMap<>();
		myMap.put("test", "OK"); // NON-NLS
		myMap.put("status", "finished"); // NON-NLS
		assertNull(testParser.getLocatorValue(null, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueWhenLocatorEmptyTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "");
		Map<String, String> myMap = new HashMap<>();
		myMap.put("test", "OK"); // NON-NLS
		myMap.put("status", "finished"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueTypeTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp, "333"); // NON-NLS
		Map<String, String> myMap = new HashMap<>();
		myMap.put("test", "OK"); // NON-NLS
		myMap.put("status", "finished"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueWhenDataIsNullTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, null)));
	}

	@Test
	public void getLocatorValuePathTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333.555"); // NON-NLS
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("333", new HashMap<String, String>()); // NON-NLS
		myMap.put("555", Arrays.asList("test1")); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValuePathListTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333.0.222"); // NON-NLS
		Map<String, Object> testMap = new HashMap<>();
		testMap.put("test_key", "test_value"); // NON-NLS
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("333", Arrays.asList(testMap, "test2", "test3")); // NON-NLS
		myMap.put("status", "TEST"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueNumberExceptionTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333.test.222", // NON-NLS
				ActivityFieldDataType.AsInput);
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("333", Arrays.asList("test1", "test2", "test3")); // NON-NLS
		myMap.put("status", "TEST"); // NON-NLS
		ActivityMapParser.ActivityContext ctx = makeContext(stream, myMap);
		Object output = testParser.getLocatorValue(fieldLocator, ctx);
		assertNull(output);
		fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333", ActivityFieldDataType.AsInput); // NON-NLS
		output = testParser.getLocatorValue(fieldLocator, ctx);
		assertEquals(myMap.get("333"), output); // NON-NLS
		fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333.1", ActivityFieldDataType.AsInput); // NON-NLS
		output = testParser.getLocatorValue(fieldLocator, ctx);
		assertEquals("test2", output); // NON-NLS
	}

	@Test
	public void getLocatorValueInstanceTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "333"); // NON-NLS
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("333", "TEST1"); // NON-NLS
		assertEquals("TEST1", testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueEmptyLocatorTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "1"); // NON-NLS
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("333", "TEST1"); // NON-NLS
		assertNull(testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap)));
	}

	@Test
	public void getLocatorValueIndexAsTypeTest() throws Exception {
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "1"); // NON-NLS
		Map<String, Object> myMap = new HashMap<>();
		myMap.put("333", "TEST1"); // NON-NLS
		testParser.getLocatorValue(fieldLocator, makeContext(stream, myMap));
	}

}
