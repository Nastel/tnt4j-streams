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

package com.jkoolcloud.tnt4j.streams.custom.inputs.castiron;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;

/**
 * @author akausinis
 * @version 1.0
 */
public class CastIronWsStreamTest {
	@Test
	public void testPreProcess() {
		CastIronWsStream stream = new CastIronWsStream();
		String req = "<![CDATA[\n" + "\t SOAPAction:https://192.168.3.3/ws/lognotif\n"
				+ "\t <log:searchLogs xmlns:log=\"http://www.approuter.com/schemas/2008/1/lognotif\">\n"
				+ "\t\t <log:logComponent>all</log:logComponent>\n" + "\t\t <log:logLevel>all</log:logLevel>\n"
				+ "\t\t <log:maxDaysOld>-1</log:maxDaysOld>\n" + "\t\t <log:status>all</log:status>\n"
				+ "\t\t <log:page>0</log:page>\n" + "\t\t <log:pageSize>${XDifference}</log:pageSize>\n"
				+ "\t </log:searchLogs>\n" + "]]>";

		StreamsCache.addEntry("XDifference", "XDifference", "${XDifference}", "17");
		StreamsCache.initialize();
		ActivityInfo ai = Mockito.mock(ActivityInfo.class);
		Mockito.when(ai.getFieldValue("${XDifference}")).thenReturn("10");
		StreamsCache.cacheValues(ai, "Test");

		String result = stream.fillInRequestData(req);

		assertEquals(result,
				"<![CDATA[\n" + "\t SOAPAction:https://192.168.3.3/ws/lognotif\n"
						+ "\t <log:searchLogs xmlns:log=\"http://www.approuter.com/schemas/2008/1/lognotif\">\n"
						+ "\t\t <log:logComponent>all</log:logComponent>\n" + "\t\t <log:logLevel>all</log:logLevel>\n"
						+ "\t\t <log:maxDaysOld>-1</log:maxDaysOld>\n" + "\t\t <log:status>all</log:status>\n"
						+ "\t\t <log:page>0</log:page>\n" + "\t\t <log:pageSize>10</log:pageSize>\n"
						+ "\t </log:searchLogs>\n" + "]]>");

	}

}
