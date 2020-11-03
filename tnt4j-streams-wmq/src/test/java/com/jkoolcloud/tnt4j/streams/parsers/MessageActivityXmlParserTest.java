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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.WmqParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;

/**
 * @author akausinis
 * @version 1.0
 */
public class MessageActivityXmlParserTest {

	@Test
	public void testProperties() {
		Map<String, String> propertiesMap = new HashMap<String, String>() {
			private static final long serialVersionUID = -8871102284218988719L;

			{
				put(WmqParserProperties.PROP_NAMESPACE_AWARE, "false");
			}
		};
		MessageActivityXmlParser parser = new MessageActivityXmlParser();
		parser.setProperties(propertiesMap.entrySet());
		assertEquals("Unexpected NamespaceAware property value", false, parser.namespaceAware);
	}

	@Test
	public void testApplyFieldValue() throws Exception {
		MessageActivityXmlParser parser = new MessageActivityXmlParser();
		ActivityInfo ai = new ActivityInfo();
		ActivityField field = mock(ActivityField.class);
		Object value = "1, TEST, TEST, TEST,TEST, TEST, TEST, TEST"; // NON-NLS
		when(field.getFieldType()).thenReturn(StreamFieldType.Correlator);
		when(field.aggregateFieldValue(value, ai)).thenReturn(value);
		parser.applyFieldValue(ai, field, value);
		verify(field).getFieldType();
		Collection<String> correl = ai.getCorrelator();
		assertEquals("Correlators count not match", 2, correl.size());
	}

	@Test
	public void testCalculateSignatureFromMsgId() throws Exception {
		MessageActivityXmlParser parser = new MessageActivityXmlParser();

		ActivityFieldLocator loc = new ActivityFieldLocator(null);
		loc.setDataType(ActivityFieldDataType.Binary);
		loc.setFormat("hexBinary", null);
		loc.setRequired("false");

		ActivityField field = new ActivityField("TrackingId");
		field.setValueType("signature");
		field.setRequired("false");
		field.addLocator(loc);

		Object locVal = loc.formatValue("414d51205639546573742020202020201fd7725b29317729");

		parser.applyFieldValue(mock(ActivityInfo.class), field, locVal);
	}

}
