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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class AbstractFileLineStreamTest {
	private FileLineStream afls = new FileLineStream();

	@Test
	public void setPropertiesTest() {
		final String fileName = "c:/Windows/schemas/TSWorkSpace/tswcx.xml"; // NON-NLS
		Map<String, String> props = new HashMap<>(1);
		props.put(StreamProperties.PROP_FILENAME, fileName);
		afls.setProperties(props.entrySet());
		Object property = afls.getProperty(StreamProperties.PROP_FILENAME);
		assertEquals(fileName, property);
	}

}
