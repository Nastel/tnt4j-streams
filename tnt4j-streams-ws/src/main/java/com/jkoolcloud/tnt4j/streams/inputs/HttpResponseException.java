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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.StatusLine;

/**
 * This exception encapsulates HTTP response errors, status codes.
 *
 * @version $Revision: 1 $
 */
public class HttpResponseException extends org.apache.http.client.HttpResponseException {

	/**
	 * Constructs a new HttpResponseException.
	 *
	 * @param statusCode
	 *            response status code
	 * @param statusReason
	 *            response status reason phrase
	 */
	public HttpResponseException(int statusCode, String statusReason) {
		super(statusCode, statusReason);
	}

	/**
	 * Constructs a new HttpResponseException.
	 * 
	 * @param sLine
	 *            status line of response
	 */
	public HttpResponseException(StatusLine sLine) {
		super(sLine.getStatusCode(), sLine.getReasonPhrase());
	}

	@Override
	public String getMessage() {
		String message = super.getMessage();

		return StringUtils.isEmpty(message) ? String.valueOf(getStatusCode())
				: getStatusCode() + " (" + super.getMessage() + ")"; // NON-NLS
	}
}
