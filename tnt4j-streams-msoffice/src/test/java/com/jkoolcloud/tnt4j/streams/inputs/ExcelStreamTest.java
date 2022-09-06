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

package com.jkoolcloud.tnt4j.streams.inputs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.utils.MsOfficeStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class ExcelStreamTest {

	@Test
	public void testRB() {
		String keyModule = "AbstractExcelStream.no.more.sheets";
		String keyCore = "ActivityField.field.type.name.empty";
		String brbStr;

		String rbs1 = StreamsResources.getString(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Kafka resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Kafka resource bundle entry found in core", keyModule, rbs1);
		brbStr = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, brbStr);
		rbs1 = StreamsResources.getString(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in kafka", brbStr, rbs1);
	}
}
