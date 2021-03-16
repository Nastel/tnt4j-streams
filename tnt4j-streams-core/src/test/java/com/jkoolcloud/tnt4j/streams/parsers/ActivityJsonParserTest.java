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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.*;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import org.junit.Test;
import org.mockito.Mockito;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.preparsers.BinaryToStringPreParser;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJsonParserTest {

	ActivityJsonParser parser = new ActivityJsonParser();
	AbstractBufferedStream<?> stream = Mockito.mock(TestUtils.SimpleTestStream.class, Mockito.CALLS_REAL_METHODS);

	@Test
	public void setPropertiesWhenNullTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		// final Collection<Entry<String, String>> props = getPropertyList().build();
		parser.setProperties(null);
	}

	@Test
	public void setPropertiesWhenPropDoesNotMatchTest() {

		Map<String, String> props = new HashMap<>(1);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		parser.setProperties(props.entrySet());
	}

	@Test
	public void parseWhenDataIsNullTest() throws Exception {
		assertNull(parser.parse(stream, null));
	}

	@Test(expected = ParseException.class)
	public void parseExceptionTest() throws Exception {
		Map<String, String> myMap = new HashMap<>();
		parser.parse(stream, myMap);
	}

	@Test
	public void parseTest() throws Exception {
		String FIELD_NAME = "test_name";
		ActivityField field = new ActivityField(FIELD_NAME); // NON-NLS
		field.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$.test"));
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		parser.addField(field);
		ActivityInfo ai = parser.parse(stream, jsonContext);
		ai.getFieldValue("test");
		assertEquals(ai.getFieldValue(FIELD_NAME), "OK");
	}

	@Test
	public void parseWhenStringIsEmptyTest() throws Exception {
		String jsonString = "";
		assertNull(parser.parse(stream, jsonString));
	}

	private ActivityJsonParser.ActivityContext makeContext(TNTInputStream<?, ?> stream, DocumentContext data) {
		return parser.new ActivityContext(stream, null, data);
	}

	@Test(expected = ParseException.class)
	public void getLocatorValueTest() throws Exception {
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "test", // NON-NLS
				ActivityFieldDataType.Number);
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		parser.getLocatorValue(aLocator, makeContext(stream, jsonContext));
	}

	@Test
	public void getLocatorValueWhenLocatorIsNullTest() throws Exception {
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertNull(parser.getLocatorValue(null, makeContext(stream, jsonContext)));
	}

	@Test
	public void getLocatorValueWhenLocatorEmptyTest() throws Exception {
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertNull(parser.getLocatorValue(aLocator, makeContext(stream, jsonContext)));
	}

	@Test
	public void getLocatorValueWhenTypeExpectedTest() throws Exception {
		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		props.put(StreamProperties.PROP_EXECUTOR_THREADS_QTY, String.valueOf(5));
		stream.setProperties(props.entrySet());
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp,
				"ExecutorThreadsQuantity", ActivityFieldDataType.Number); // NON-NLS
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertEquals(5, parser.getLocatorValue(aLocator, makeContext(stream, jsonContext)));
	}

	@Test
	public void getLocatorValueStartsWithJsonPathTest() throws Exception {
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$.test"); // NON-NLS
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertEquals("OK", parser.getLocatorValue(aLocator, makeContext(stream, jsonContext)));
	}

	@Test
	public void getLocatorValueJsonPathIsListTest() throws Exception {
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$.values"); // NON-NLS
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\",\"values\":[4, 5, 6]}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		List<Object> testData = new ArrayList<>(Arrays.asList(4, 5, 6));
		testData.equals(parser.getLocatorValue(aLocator, makeContext(stream, jsonContext)));
	}

	@Test
	public void getLocatorValueJsonPathIsEmptyListTest() throws Exception {
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$.values"); // NON-NLS
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\",\"values\":[]}"; // NON-NLS
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		List<Object> testData = new ArrayList<>();
		assertNull(parser.getLocatorValue(aLocator, makeContext(stream, jsonContext)));
	}

	@Test
	public void getNextJSONStringWhenNullTest() {
		Map<String, String> props = new HashMap<>(1);
		props.put("ActivityDelim", "EOF"); // NON-NLS
		parser.setProperties(props.entrySet());
		assertNull(parser.getNextActivityString(null));
	}

	@Test
	public void getNextJSONStringWhenByteArrayTest() {
		Map<String, String> props = new HashMap<>(1);
		props.put("ActivityDelim", "EOF"); // NON-NLS
		parser.setProperties(props.entrySet());
		byte[] myVar = "testing some data".getBytes(); // NON-NLS
		assertEquals(Utils.getString(myVar), parser.getNextActivityString(myVar));
	}

	@Test
	public void getNextJSONStringWhenBufferedReaderTest() {
		Map<String, String> props = new HashMap<>(1);
		props.put("ActivityDelim", "EOF"); // NON-NLS
		parser.setProperties(props.entrySet());
		InputStream textStream = new ByteArrayInputStream("testing some data".getBytes()); // NON-NLS
		BufferedReader br = new BufferedReader(new InputStreamReader(textStream));
		assertEquals("testing some data", parser.getNextActivityString(br));
	}

	@Test
	public void getNextJSONStringWhenBufferedReaderTrueTest() {
		Map<String, String> props = new HashMap<>(1);
		props.put("ActivityDelim", "EOL"); // NON-NLS
		parser.setProperties(props.entrySet());
		InputStream textStream = new ByteArrayInputStream("testing some data".getBytes()); // NON-NLS
		BufferedReader br = new BufferedReader(new InputStreamReader(textStream));
		assertEquals("testing some data", parser.getNextActivityString(br));
	}

	@Test
	public void getNextJSONStringWhenReaderTest() {
		Map<String, String> props = new HashMap<>(1);
		props.put("ActivityDelim", "EOF"); // NON-NLS
		parser.setProperties(props.entrySet());
		String stringToBeParsed = "Testing some tests"; // NON-NLS
		StringReader reader = new StringReader(stringToBeParsed);
		assertEquals(stringToBeParsed, parser.getNextActivityString(reader));
	}

	@Test
	public void getNextJSONStringWhenInputStreamTest() {
		Map<String, String> props = new HashMap<>(1);
		props.put("ActivityDelim", "EOF"); // NON-NLS
		parser.setProperties(props.entrySet());
		String stringToBeParsed = "Testing"; // NON-NLS
		InputStream inputStream = new ByteArrayInputStream(stringToBeParsed.getBytes());
		assertEquals(stringToBeParsed, parser.getNextActivityString(inputStream));
	}

	@Test
	public void testPreparserValidity() {
		ActivityJsonParser parser = new ActivityJsonParser();
		BinaryToStringPreParser preParser = new BinaryToStringPreParser();
		parser.addReference(preParser);

		boolean match = parser.isLogicalTypeSupported(preParser.dataTypeReturned());

		assertTrue(match);
	}

}
