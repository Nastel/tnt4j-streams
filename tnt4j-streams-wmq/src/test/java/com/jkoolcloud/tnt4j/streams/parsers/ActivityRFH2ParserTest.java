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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityRFH2ParserTest {

	@Test
	public void parseTest() throws Exception {
		ActivityRFH2Parser parser = new ActivityRFH2Parser();
		ActivityRFH2Parser.ActivityContext activityContext = parser.prepareItem(new TestUtils.SimpleTestStream(),
				Files.readAllBytes(Paths.get("./samples/rfh2_jms/rfh2_jms.bin")));
		String fXML = (String) parser.resolveLocatorValue(
				new ActivityFieldLocator(ActivityFieldLocatorType.Label.name(), ActivityRFH2Parser.FOLDERS),
				activityContext, new AtomicBoolean(false));

		String rfh2Data = new String(Files.readAllBytes(Paths.get("./samples/rfh2_jms/rfh2_data.xml")),
				StandardCharsets.UTF_8);
		assertThat("RFH2 folders XML string does not match", rfh2Data,
				CompareMatcher.isIdenticalTo(fXML).ignoreComments().ignoreWhitespace());

		Object jmsData = parser.resolveLocatorValue(
				new ActivityFieldLocator(ActivityFieldLocatorType.Label.name(), ActivityRFH2Parser.JMS_DATA),
				activityContext, new AtomicBoolean(false));
		assertTrue("JMS message data expected to be map", jmsData instanceof Map);

		@SuppressWarnings("unchecked")
		Map<String, String> jmsMap = (Map<String, String>) jmsData;

		assertEquals("JMS message map size does not match", 24, jmsMap.size());
		assertEquals("JMS message map entry value does not match", "5", jmsMap.get("retryDelay"));
	}
}