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
import static org.mockito.Mockito.*;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityTokenParserTest extends GenericActivityParserTestBase<ActivityTokenParser, String[]> {

	@Override
	@Test
	public void setPropertiesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_FLD_DELIM, ";");
		setProperty(parser, ParserProperties.PROP_PATTERN, "\\S+"); // NON-NLS
		setProperty(parser, ParserProperties.PROP_STRIP_QUOTES, true);

	}

	@Override
	@Before
	public void prepare() {
		parser = new ActivityTokenParser();
	}

	@Test
	public void testParse() throws Exception {
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		final Object data = "TEST"; // NON-NLS
		setPropertiesTest();
		assertNotNull(parser.parse(stream, data));
	}

	@Test
	public void testParseDoensMatch() throws Exception {
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		final Object data = "TEST TTT"; // NON-NLS
		setPropertiesTest();
		assertNull(parser.parse(stream, data));
	}

	@Test
	public void testGetLocatorValueAsProperty() throws ParseException {
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp, "TEST"); // NON-NLS
		parser.getLocatorValue(locator, makeContext(stream, null));
		verify(stream).getProperty(any(String.class));
	}

	@Test
	public void testGetLocatorAsIndex() throws ParseException {
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2");
		String[] fields = { "FAIL", "GOOD" }; // NON-NLS
		Object result = parser.getLocatorValue(locator, makeContext(stream, fields));
		assertEquals("GOOD", result);
	}
}
