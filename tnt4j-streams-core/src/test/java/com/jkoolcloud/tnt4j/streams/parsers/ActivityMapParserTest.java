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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityMapParserTest extends ActivityParserTestBase<ActivityMapParser> {
	@Override
	@Before
	public void prepare() {
		parser = new ActivityMapParser();
	}

	@Override
	@Test
	public void isDataClassSupportedTest() {
		assertTrue(parser.isDataClassSupported(mock(Map.class)));
		assertFalse(parser.isDataClassSupported(String.class));
	}

	@Test
	public void getDataMapTest() throws Exception {
		Map<String, byte[]> data = new HashMap<>();
		data.put(StreamsConstants.ACTIVITY_DATA_KEY, "TEST".getBytes()); // NON-NLS
		assertNotNull(parser.getDataMap(data));
	}

	@Override
	public void setPropertiesTest() throws Exception {
		// TODO Auto-generated method stub

	}
}
