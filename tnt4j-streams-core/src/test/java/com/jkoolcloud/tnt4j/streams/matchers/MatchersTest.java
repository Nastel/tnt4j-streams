/*
 * Copyright 2014-2017 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * @author akausinis
 * @version 1.0
 */
public class MatchersTest {

	@Test
	public void contextExpressionTest() throws Exception {
		ActivityField objNameField = new ActivityField("ObjectName");
		ActivityField objPathField = new ActivityField("ObjectPath");
		ActivityField nullField = new ActivityField("NullField");

		ActivityInfo ai = new ActivityInfo();
		ai.setFieldValue(objNameField, "ccc");
		ai.setFieldValue(objPathField, "/xxx/yyy/obj.com");
		ai.setFieldValue(nullField, null);

		assertTrue(Matchers.evaluate("groovy:${ObjectName} == \"ccc\"", ai));
		assertTrue(Matchers.evaluate("groovy:${ObjectName}.startsWith(\"cc\")", ai));
		assertFalse(Matchers.evaluate("js:${ObjectName}.toUpperCase().indexOf(\"ccc\") == 0", ai));
		assertTrue(Matchers.evaluate("xpath:boolean(ts:getFileName(${ObjectPath}))", ai));
		assertFalse(Matchers.evaluate("xpath:boolean(ts:getFileName(${NullField}))", ai));

		Map<String, String> map = new HashMap<>(1);
		map.put("key", "value");

		assertTrue(Matchers.evaluate("groovy:$fieldValue instanceof Map", map));
		assertFalse(Matchers.evaluate("groovy:$fieldValue instanceof String", map));
	}
}
