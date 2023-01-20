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
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityRegExParserTest extends ActivityParserTestBase<ActivityRegExParser> {

	private static final String TEST_STRING = "TEST_STRING"; // NON-NLS
	private static final String TEST_PATTERN = "(\\S+)"; // NON-NLS
	private TNTInputStream<?, ?> stream = mock(TNTInputStream.class);

	@Override
	@Before
	public void prepare() {
		parser = new ActivityRegExParser();
	}

	@Override
	@Test
	public void setPropertiesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, TEST_PATTERN);
	}

	@Test
	@Override
	public void isDataClassSupportedTest() {
		assertTrue(parser.isDataClassSupported("TEST")); // NON-NLS
		assertTrue(parser.isDataClassSupported("TEST".getBytes())); // NON-NLS
		assertTrue(parser.isDataClassSupported(mock(Reader.class)));
		assertTrue(parser.isDataClassSupported(mock(InputStream.class)));
		assertFalse(parser.isDataClassSupported(this.getClass()));
	}

	@Test
	public void addField() {
		ActivityField field = new ActivityField("Test"); // NON-NLS
		ActivityFieldLocator locator = mock(ActivityFieldLocator.class);
		when(locator.getType()).thenReturn(ActivityFieldLocatorType.Label.name());
		field.addLocator(locator);
		parser.addField(field);
	}

	@Test
	public void addGroupField() {
		ActivityField field = new ActivityField("TestGroup"); // NON-NLS
		ActivityFieldLocator locator = mock(ActivityFieldLocator.class);
		when(locator.getType()).thenReturn(ActivityFieldLocatorType.Index.name());
		field.addLocator(locator);
		parser.addField(field);
	}

	@Test
	public void testParse() throws Exception {
		setPropertiesTest();
		addField();
		addGroupField();
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		parser.addField(mock(ActivityField.class));
		assertNotNull(parser.parse(stream, TEST_STRING));
	}

	@Test
	public void matches() {
		Pattern pattern = Pattern.compile(TEST_PATTERN);
		Matcher matcher = pattern.matcher(TEST_STRING);
		assertTrue(matcher.matches());
	}

	@Test
	public void setPropertiesNotEqualsPropNameTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_NAMESPACE, "test"); // NON-NLS
	}

	@Test
	public void setPropertiesWhenPropValueIsEmptyTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "");
	}

	@Test
	public void addFieldWhenDataIsNullTest() {
		ActivityField af = new ActivityField("Test"); // NON-NLS
		parser.addField(af);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFieldExceptionTest() {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "REMatchId"); // NON-NLS
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "REMatchId"); // NON-NLS
		ActivityField af = new ActivityField("test"); // NON-NLS
		af.addLocator(locator);
		parser.addField(af);
		af.addLocator(locator2);
		parser.addField(af);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFieldExceptionOtherTest() {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "REMatchId"); // NON-NLS
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "REMatchId"); // NON-NLS
		ActivityField af = new ActivityField("test"); // NON-NLS
		af.addLocator(locator2);
		parser.addField(af);
		af.addLocator(locator);
		parser.addField(af);
	}

	@Test(expected = IllegalStateException.class)
	public void parseWhenPatternNullTest() throws Exception {
		parser.parse(stream, "");
	}

	@Test
	public void parseWhenWhenDataIsNullTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "test"); // NON-NLS
		assertNull(parser.parse(stream, null));
	}

	@Test
	public void parseWhenWhenDataIsEmptyTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "test"); // NON-NLS
		assertNull(parser.parse(stream, ""));
	}

	@Test
	public void parseWhenNoMatchesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "(\\d+)"); // NON-NLS
		assertNull(parser.parse(stream, "test")); // NON-NLS
	}

	@Test
	public void parseWhenMatchGroupsTest() throws Exception {
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "0"); // NON-NLS
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "1"); // NON-NLS
		ActivityFieldLocator locator3 = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2"); // NON-NLS
		ActivityField af1 = new ActivityField("test1"); // NON-NLS
		ActivityField af2 = new ActivityField("test2"); // NON-NLS
		ActivityField af3 = new ActivityField("test3"); // NON-NLS
		af1.addLocator(locator1);
		af2.addLocator(locator2);
		af3.addLocator(locator3);
		parser.addField(af1);
		parser.addField(af2);
		parser.addField(af3);
		setProperty(parser, ParserProperties.PROP_PATTERN, "(\\d+)"); // NON-NLS
		setProperty(parser, ParserProperties.PROP_MATCH_STRATEGY, "FIND"); // NON-NLS
		ActivityInfo ai = parser.parse(stream, "1111"); // NON-NLS
		assertEquals(ai.getFieldValue("test1"), "1111"); // NON-NLS
		assertEquals(ai.getFieldValue("test2"), "1111"); // NON-NLS
		assertEquals(ai.getFieldValue("test3"), null); // NON-NLS
	}

	@Test
	public void parseWhenMatchMapIsEmptyTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_PATTERN, "(\\d+)"); // NON-NLS
		parser.parse(stream, "1111"); // NON-NLS
	}

	@Test
	public void parseMatchMapOneEntryTest() throws Exception {
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "0"); // NON-NLS
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "1"); // NON-NLS
		ActivityFieldLocator locator3 = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2"); // NON-NLS
		ActivityFieldLocator locator4 = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "0"); // NON-NLS
		ActivityField af1 = new ActivityField("test1"); // NON-NLS
		ActivityField af2 = new ActivityField("test2"); // NON-NLS
		ActivityField af3 = new ActivityField("test3"); // NON-NLS
		ActivityField af4 = new ActivityField("test4"); // NON-NLS
		af1.addLocator(locator1);
		af2.addLocator(locator2);
		af3.addLocator(locator3);
		af4.addLocator(locator4);
		parser.addField(af1);
		parser.addField(af2);
		parser.addField(af3);
		parser.addField(af4);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+"); // NON-NLS
		setProperty(parser, ParserProperties.PROP_MATCH_STRATEGY, "FIND"); // NON-NLS
		ActivityInfo ai = parser.parse(stream, "1111555999"); // NON-NLS
		assertEquals(ai.getFieldValue("test1"), "1111555999"); // NON-NLS
		assertEquals(ai.getFieldValue("test2"), null); // NON-NLS
		assertEquals(ai.getFieldValue("test3"), null); // NON-NLS
		assertEquals(ai.getFieldValue("test4"), "1111555999"); // NON-NLS
	}

	@Test
	public void parseMatchMapTwoEntriesTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "1");
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2");
		ActivityField af = new ActivityField("test"); // NON-NLS
		af.addLocator(locator);
		af.addLocator(locator1);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+"); // NON-NLS
		parser.parse(stream, "1111555999"); // NON-NLS
	}

	@Test
	public void parseGroupMapTwoEntriesTest() throws Exception {
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "0");
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp, "DateTime");
		ActivityField af = new ActivityField("test"); // NON-NLS
		af.addLocator(locator);
		af.addLocator(locator1);
		parser.addField(af);
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\d+"); // NON-NLS
		ActivityInfo ai = parser.parse(stream, "1111555999"); // NON-NLS
		assertTrue(ai.getFieldValue("test").toString().startsWith("1111555999")); // NON-NLS
	}

	@Test
	public void parseNamedLocatorTest() throws Exception {
		ActivityFieldLocator locator1 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "CoID"); // NON-NLS
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "ProcessArea"); // NON-NLS
		ActivityFieldLocator locator3 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "InterfaceID"); // NON-NLS
		ActivityFieldLocator locator4 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "HopNr"); // NON-NLS
		ActivityField af1 = new ActivityField("test1"); // NON-NLS
		ActivityField af2 = new ActivityField("test2"); // NON-NLS
		ActivityField af3 = new ActivityField("test3"); // NON-NLS
		ActivityField af4 = new ActivityField("test4"); // NON-NLS
		af1.addLocator(locator1);
		af2.addLocator(locator2);
		af3.addLocator(locator3);
		af4.addLocator(locator4);
		parser.addField(af1);
		parser.addField(af2);
		parser.addField(af3);
		parser.addField(af4);

		setProperty(parser, ParserProperties.PROP_PATTERN,
				"(?<CoID>.*)\\.(?<ProcessArea>.*)\\.(?<InterfaceID>.*)\\.(?<HopNr>.*)"); // NON-NLS
		ActivityInfo ai = parser.parse(stream, "MON.WHL.10232.006"); // NON-NLS

		assertEquals(ai.getFieldValue("test1"), ("MON")); // NON-NLS
		assertEquals(ai.getFieldValue("test2"), ("WHL")); // NON-NLS
		assertEquals(ai.getFieldValue("test3"), ("10232")); // NON-NLS
		assertEquals(ai.getFieldValue("test4"), ("006")); // NON-NLS
	}
}
