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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public abstract class ActivityParserTestBase<P extends ActivityParser> {
	P parser;

	public abstract void prepare();

	public abstract void setPropertiesTest() throws Exception;

	public abstract void isDataClassSupportedTest();

	public static Collection<Map.Entry<String, String>> makeProperty(String propertyName, String testValue) {
		Map<String, String> props = new HashMap<>(1);
		props.put(propertyName, String.valueOf(testValue));

		return props.entrySet();
	}

	public void setProperty(ActivityParser parser, String propertyName, Object testValue) {
		parser.setProperties(makeProperty(propertyName, testValue.toString()));
	}

	@Test
	public void setPropertiesNullFailTest() {
		parser.setProperties(null);
	}

	@Test
	public void parserNameTest() {
		String name = "Test"; // NON-NLS
		parser.setName(name);
		assertEquals(name, parser.getName());
	}

	// @Test
	// public void filterTest() {
	// final StreamFilter filter = mock(StreamFilter.class);
	// parser.addFilter(filter);
	// parser.addFilter(filter);
	// final ActivityInfo ai = mock(ActivityInfo.class);
	// parser.filterActivity(ai);
	// verify(filter, times(2)).doFilterActivity(ai);
	// }
}
