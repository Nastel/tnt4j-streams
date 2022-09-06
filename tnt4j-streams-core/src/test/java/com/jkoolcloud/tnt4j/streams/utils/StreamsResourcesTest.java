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

import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsResourcesTest {

	@Test
	public void testGetBundle() {
		assertNotNull(StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME));
	}

	@Test
	public void testGetString() {
		final String bundleName = "tnt4j-streams-core"; // NON-NLS
		ResourceBundle resourceBundle = ResourceBundle.getBundle(bundleName);
		Enumeration<String> keys = resourceBundle.getKeys();
		while (keys.hasMoreElements()) {
			String nextElement = keys.nextElement();
			String resource = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, nextElement);
			assertNotNull(resource);
			if (resource.contains("{1}") && !resource.contains("{2}")) { // NON-NLS
				final String testVar = "TEST"; // NON-NLS
				String formatted = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						resource, testVar);
				MessageFormat.format(resource, testVar);
				assertEquals(String.format(resource, testVar), formatted);
			}
		}
	}

	@Test
	public void testNull() {
		Object string = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, (String) null);
		assertNull(string);
		string = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, null);
		assertNull(string);
		string = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, (Enum<?>) null);
		assertNull(string);
	}

}
