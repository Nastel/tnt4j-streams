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

package com.jkoolcloud.tnt4j.streams.scenario;

import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * Base class for TNT4J-Streams-WS configuration entities: scenario, step, etc.
 *
 * @version $Revision: 1 $
 */
public abstract class WsScenarioEntity {
	private String name;

	private String urlStr;
	private String method;
	private String username;
	private String password;

	/**
	 * Constructs a new WsScenarioEntity. Defines scenario entity name.
	 *
	 * @param name
	 *            scenario entity name
	 */
	protected WsScenarioEntity(String name) {
		this.name = name;
	}

	/**
	 * Returns scenario entity name.
	 *
	 * @return scenario entity name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns service URL string.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream} and
	 * {@link com.jkoolcloud.tnt4j.streams.inputs.WsStream}.
	 *
	 * @return service URL string.
	 */
	public String getUrlStr() {
		return urlStr;
	}

	/**
	 * Sets service URL string.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream} and
	 * {@link com.jkoolcloud.tnt4j.streams.inputs.WsStream}.
	 *
	 * @param urlStr
	 *            service URL string.
	 */
	public void setUrlStr(String urlStr) {
		this.urlStr = urlStr;
	}

	/**
	 * Returns request invocation method name.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @return request invocation method name
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Sets request invocation method name. It can be GET or POST.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @param method
	 *            request invocation method name
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Sets user credentials (user name and password) used to perform request if service authentication is needed.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @param username
	 *            user name used for authentication
	 * @param password
	 *            password used for authentication
	 */
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Returns user name used to perform request if service authentication is needed.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @return user name used for authentication
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns password used to perform request if service authentication is needed.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @return password used for authentication
	 */
	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append('{'); // NON-NLS
		sb.append("name=").append(Utils.quote(name)); // NON-NLS
		sb.append(", urlStr=").append(Utils.quote(urlStr)); // NON-NLS
		sb.append(", method=").append(Utils.quote(method)); // NON-NLS
		sb.append('}'); // NON-NLS
		return sb.toString();
	}

}
