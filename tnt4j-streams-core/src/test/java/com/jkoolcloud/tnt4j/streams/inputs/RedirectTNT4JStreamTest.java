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

package com.jkoolcloud.tnt4j.streams.inputs;

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class RedirectTNT4JStreamTest {

	RedirectTNT4JStream rs;

	@Before
	public void prepareTests() {
		rs = new RedirectTNT4JStream();
	}

	@Test
	public void setPropertiesFileTest() throws Exception {
		File tempFile = File.createTempFile("Test", ".test");
		tempFile.deleteOnExit();
		Map<String, String> props = new HashMap<>(3);
		props.put(StreamProperties.PROP_FILENAME, tempFile.getAbsolutePath());
		props.put(StreamProperties.PROP_BUFFER_SIZE, String.valueOf(55));
		props.put(StreamProperties.PROP_FULL_BUFFER_ADD_POLICY, "WAIT"); // NON-NLS
		rs.setProperties(props.entrySet());
		testPropertyList(rs, props.entrySet());
		rs.startStream();
	}

	@Test
	public void setPropertiesSocketTest() throws Exception {
		Map<String, String> props = new HashMap<>(1);
		props.put(StreamProperties.PROP_PORT, String.valueOf(9010));
		rs.setProperties(props.entrySet());
		testPropertyList(rs, props.entrySet());
		rs.startStream();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPropertiesSetShouldFailTest() {
		Map<String, String> props = new HashMap<>(3);
		props.put(StreamProperties.PROP_FILENAME, "TestFile"); // NON-NLS
		props.put(StreamProperties.PROP_RESTART_ON_CLOSE, String.valueOf(true));
		props.put(StreamProperties.PROP_PORT, String.valueOf(9009));

		rs.setProperties(props.entrySet());
	}

}
