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

package com.jkoolcloud.tnt4j.streams.custom.format.castiron;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.jkoolcloud.tnt4j.config.ConfigFactory;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.tracker.DefaultTrackerFactory;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;

/**
 * @author akausinis
 * @version 1.0
 */
public class FactNameValueFormatterTest {

	@Test
	public void testFormat() throws IOException {
		FactNameValueFormatter formatter = new FactNameValueFormatter();
		Map<String, Object> properties = new HashMap<>();
		properties.put("PathLevelAttributes", "StartTime;Severity");
		formatter.setConfiguration(properties);
		ConfigFactory configFactory = DefaultConfigFactory.getInstance();

		Tracker tracker = new DefaultTrackerFactory()
				.getInstance(configFactory.getConfig(getClass().getName()).build());
		TrackingActivity activity = tracker.newActivity(OpLevel.INFO, "TestName");

		String result = formatter.format(activity);
		System.out.println(result);
		assertNotNull(result);
		assertTrue(result.startsWith("OBJ:Streams"));
		assertTrue(result.contains("Activities"));
		assertTrue(StringUtils.countMatches(result, ",") == 14);
	}
}