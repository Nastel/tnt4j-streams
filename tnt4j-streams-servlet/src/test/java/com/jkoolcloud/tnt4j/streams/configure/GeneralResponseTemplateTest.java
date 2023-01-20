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

package com.jkoolcloud.tnt4j.streams.configure;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.jkoolcloud.tnt4j.core.OpCompCode;
import com.jkoolcloud.tnt4j.streams.inputs.HttpServletStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class GeneralResponseTemplateTest {

	@Test
	public void testResponseTemplateLoadDefault() {
		HttpResponseTemplate rt = new GeneralResponseTemplate(HttpServletStream.DEFAULT_RESPONSE_TEMPLATE);

		String errorMsg = "This is test error message"; // NON-NLS
		String bodyStr = rt.fillBody(null, errorMsg, OpCompCode.ERROR);

		assertNotNull("Response body is null", bodyStr); // NON-NLS
		assertTrue("Response does not contain provided message", bodyStr.contains(errorMsg)); // NON-NLS
		assertTrue("Response does not contain wrapping html", bodyStr.contains("<body>")); // NON-NLS
	}

	@Test
	public void testResponseTemplateLoadCustomLine() {
		String template = "" //
				+ "\"$headers\": {\n" //
				+ "    \"content-type\": \"application/json\"\n" //
				+ "},\n" //
				+ "\"$body\": {\n" //
				+ "    \"type\": \"TEXT\",\n" //
				+ "    \"payload\": \"Status message: ${message}\"\n" //
				+ "}";

		HttpResponseTemplate rt = new GeneralResponseTemplate(template);

		String errorMsg = "This is test error message"; // NON-NLS
		String bodyStr = rt.fillBody(null, errorMsg, OpCompCode.ERROR);

		assertNotNull("Response body is null", bodyStr); // NON-NLS
		assertTrue("Response does not contain provided message", bodyStr.contains(errorMsg)); // NON-NLS
		assertTrue("Response does not contain wrapping text", bodyStr.startsWith("Status message:")); // NON-NLS
	}

	@Test
	public void testResponseTemplateLoadCustomMap() {
		String template = "" //
				+ "\"$headers\": {\n" //
				+ "    \"content-type\": \"application/json\"\n" //
				+ "},\n" //
				+ "\"$body\": {\n" //
				+ "    \"type\": \"JSON\",\n" //
				+ "    \"payload\": {\n" //
				+ "        \"requestId\": \"${header:x-amz-firehose-request-id}\",\n" //
				+ "        \"timestamp\": \"${now}\",\n" //
				+ "        \"$optional\": {\n" //
				+ "            \"errorMessage\": \"${errorMessage}\"\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}";

		HttpResponseTemplate rt = new GeneralResponseTemplate(template);

		Map<String, ?> headersMap = rt.fillHeaders(null);

		String errorMsg = "This is test error message"; // NON-NLS
		String successMsg = "This is test success message"; // NON-NLS

		String bodyStrErr = rt.fillBody(null, errorMsg, OpCompCode.ERROR);
		String bodyStrScs = rt.fillBody(null, successMsg, OpCompCode.SUCCESS);

		assertNotNull("Headers map is null", headersMap); // NON-NLS
		assertTrue("Unexpected headers map size", headersMap.size() == 1); // NON-NLS
		assertTrue("Map does not contain expected entry", headersMap.get("content-type") != null); // NON-NLS

		assertNotNull("Error response body is null", bodyStrErr); // NON-NLS
		assertTrue("Error response does not contain provided message", bodyStrErr.contains(errorMsg)); // NON-NLS
		assertTrue("Error response does not contain wrapping text", bodyStrErr.contains("errorMessage")); // NON-NLS
		assertTrue("Error response does not contain required fields",
				bodyStrErr.contains("requestId") && bodyStrErr.contains("timestamp")); // NON-NLS

		assertNotNull("Success response body is null", bodyStrScs); // NON-NLS
		assertTrue("Success response does contains message", !bodyStrScs.contains(successMsg)); // NON-NLS
		assertTrue("Success response does contains error message", !bodyStrScs.contains("errorMessage")); // NON-NLS
		assertTrue("Success response does not contain required fields",
				bodyStrScs.contains("requestId") && bodyStrScs.contains("timestamp")); // NON-NLS
	}
}
