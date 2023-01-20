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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.jkoolcloud.tnt4j.core.OpCompCode;

/**
 * This interface defines common operations for Http response templates used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public interface HttpResponseTemplate {

	/**
	 * Fills in template defined headers to be added to Http response.
	 * 
	 * @param req
	 *            Http request used to collect values
	 * @return map of response headers to add
	 */
	public Map<String, String> fillHeaders(HttpServletRequest req);

	/**
	 * Fills in template defined Http response body string.
	 * 
	 * @param req
	 *            Http request used to collect values
	 * @param msg
	 *            response message
	 * @param msgType
	 *            response message type
	 * @return response body string
	 */
	public String fillBody(HttpServletRequest req, String msg, OpCompCode msgType);
}
