/*
 * Copyright 2014-2019 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.scenario;

/**
 * This class defines TNT4J-Streams-WS response data container having original request bound.
 *
 * @param <O>
 *            type of request data
 * @param <T>
 *            type of response data
 *
 * @version $Revision: 1 $
 */
public class WsReqResponse<O, T> extends WsResponse<T> {
	private WsRequest<O> originalRequest;

	/**
	 * Constructs a new WsReqResponse. Defines response and original request data.
	 *
	 * @param responseData
	 *            response data package
	 * @param request
	 *            original request data
	 */
	public WsReqResponse(T responseData, WsRequest<O> request) {
		super(responseData, request.getTags());
		this.originalRequest = request;
	}

	/**
	 * Returns original request data.
	 * 
	 * @return original request data
	 */
	public WsRequest<O> getOriginalRequest() {
		return originalRequest;
	}
}
