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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsCacheTest {

	@Test
	public void cachePersistingTest() {
		Map<String, String> props = new HashMap<>();
		props.put("Persisted", "true");

		StreamsCache.setProperties(props.entrySet());

		final Date date = new Date();
		StreamsCache.addEntry("string", "string", "${string}", null);
		StreamsCache.addEntry("double", "double", "${double}", null);
		StreamsCache.addEntry("byteArray", "byteArray", "${byteArray}", null);
		StreamsCache.addEntry("date", "date", "${date}", null);
		StreamsCache.cacheValues(new ActivityInfo() {
			{
				addActivityProperty("string", "value1");
				addActivityProperty("double", Double.MAX_VALUE);
				addActivityProperty("byteArray", new byte[] { 2, 2, 2, 2, 2 });

				addActivityProperty("date", date);
			}
		}, "Test Parser");

		StreamsCache.cleanup();

		StreamsCache.setProperties(props.entrySet());

		Assert.assertEquals(StreamsCache.getValue("string"), "value1");
		Assert.assertEquals(StreamsCache.getValue("double"), Double.MAX_VALUE);
		Assert.assertArrayEquals((byte[]) StreamsCache.getValue("byteArray"), new byte[] { 2, 2, 2, 2, 2 });
		Assert.assertTrue(date.compareTo((Date) StreamsCache.getValue("date")) == 0);
	}
}