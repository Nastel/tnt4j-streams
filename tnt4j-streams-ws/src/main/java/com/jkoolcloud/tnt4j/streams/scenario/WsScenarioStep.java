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

import java.util.*;
import java.util.concurrent.Semaphore;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class defines TNT4J-Streams-WS configuration scenario step.
 *
 * @version $Revision: 1 $
 */
public class WsScenarioStep extends WsScenarioEntity implements AutoIdGenerator {
	private final List<WsRequest<String>> requests = new ArrayList<>();
	private Map<String, String> properties;

	private SchedulerData schedulerData;

	private WsScenario scenario;

	private Semaphore semaphore = null;

	/**
	 * Constructs a new WsScenarioStep. Defines scenario step name.
	 *
	 * @param name
	 *            scenario step name
	 */
	public WsScenarioStep(String name) {
		super(name);
	}

	/**
	 * Returns requests/commands list for this step.
	 *
	 * @return requests list for this step
	 */
	public List<WsRequest<String>> getRequests() {
		return requests;
	}

	/**
	 * Returns requests/commands array for this step.
	 * 
	 * @return requests array for this step
	 */
	@SuppressWarnings("unchecked")
	public WsRequest<String>[] requestsArray() {
		return requests.toArray(new WsRequest[requests.size()]);
	}

	/**
	 * Returns request/command instance having defined identifier {@code reqId}. Identifiers comparison is case
	 * insensitive.
	 * 
	 * @param reqId
	 *            request identifier
	 * @return request instance having defined identifier
	 */
	public WsRequest<String> getRequest(String reqId) {
		synchronized (requests) {
			if (StringUtils.isNotEmpty(reqId)) {
				for (WsRequest<String> req : requests) {
					if (reqId.equalsIgnoreCase(req.getId())) {
						return req;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Adds request/command data for this step. Request tag is set to {@code null}.
	 *
	 * @param id
	 *            request identifier
	 * @param request
	 *            request data
	 * @return constructed request instance
	 *
	 * @see #addRequest(String, String, String...)
	 */
	public WsRequest<String> addRequest(String id, String request) {
		return addRequest(id, request, null);
	}

	/**
	 * Adds request/command data and tag for this step.
	 *
	 * @param id
	 *            request identifier
	 * @param request
	 *            request data
	 * @param tags
	 *            request tags
	 * @return constructed request instance
	 */
	public WsRequest<String> addRequest(String id, String request, String... tags) {
		WsRequest<String> req = new WsRequest<>(request, tags);
		synchronized (requests) {
			req.setId(StringUtils.isEmpty(id) ? getAutoId() : id);
			req.setScenarioStep(this);
			requests.add(req);
		}

		return req;
	}

	/**
	 * Adds request/command data for this step.
	 *
	 * @param request
	 *            request instance to add
	 */
	public void addRequest(WsRequest<String> request) {
		synchronized (requests) {
			if (StringUtils.isEmpty(request.getId())) {
				request.setId(getAutoId());
			}
			request.setScenarioStep(this);

			requests.add(request);
		}
	}

	/**
	 * Removes request/command data from this step requests list.
	 * 
	 * @param request
	 *            request instance to remove
	 */
	public void removeRequest(WsRequest<String> request) {
		synchronized (requests) {
			requests.remove(request);
		}
		request.setScenarioStep(null);
	}

	@Override
	public String getUrlStr() {
		String url = super.getUrlStr();

		return url == null ? scenario == null ? null : scenario.getUrlStr() : url;
	}

	@Override
	public String getMethod() {
		String method = super.getMethod();

		return method == null ? scenario == null ? null : scenario.getMethod() : method;
	}

	@Override
	public String getUsername() {
		String username = super.getUsername();

		return username == null ? scenario == null ? null : scenario.getUsername() : username;
	}

	@Override
	public String getPassword() {
		String username = super.getUsername();
		String password = super.getPassword();

		return username == null ? scenario == null ? null : scenario.getPassword() : password;
	}

	/**
	 * Checks if scenario step has no requests defined.
	 *
	 * @return flag indicating scenario has no requests defined
	 */
	public boolean isEmpty() {
		synchronized (requests) {
			return CollectionUtils.isEmpty(requests);
		}
	}

	/**
	 * Returns request/command scheduler configuration data.
	 *
	 * @return scheduler configuration data
	 */
	public SchedulerData getSchedulerData() {
		return schedulerData;
	}

	/**
	 * Sets request/command scheduler configuration data.
	 *
	 * @param schedulerData
	 *            scheduler configuration data
	 */
	public void setSchedulerData(SchedulerData schedulerData) {
		this.schedulerData = schedulerData;
	}

	/**
	 * Sets scenario instance this step belongs to.
	 *
	 * @param scenario
	 *            scenario instance this step belongs to
	 */
	void setScenario(WsScenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * Returns scenario instance this step belongs to.
	 * 
	 * @return scenario instance this step belongs to
	 */
	public WsScenario getScenario() {
		return scenario;
	}

	/**
	 * Searches step properties map for property having defined name and returns that property value. If step has no
	 * property with defined name - {@code null} is returned.
	 *
	 * @param propName
	 *            the property name
	 * @return the value of step property having defined name, or {@code null} is step has no property with defined name
	 */
	public String getProperty(String propName) {
		return properties == null ? null : properties.get(propName);
	}

	/**
	 * Sets property for this step.
	 * 
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 */
	public void setProperty(String name, String value) {
		if (properties == null) {
			properties = new HashMap<>();
		}

		properties.put(name, value);

		if (WsStreamProperties.PROP_SYNCHRONIZE_REQUESTS.equalsIgnoreCase(name)) {
			boolean sync = Utils.toBoolean(value);
			if (sync) {
				this.semaphore = new Semaphore(1);
			}
		}
	}

	/**
	 * Sets properties values map for this step.
	 *
	 * @param props
	 *            collection of properties to set for this step
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	/**
	 * Returns properties collection for this step.
	 * 
	 * @return properties collection for this step
	 */
	public Collection<Map.Entry<String, String>> getProperties() {
		return properties == null ? null : properties.entrySet();
	}

	/**
	 * Returns requests synchronization semaphore instance for this step.
	 * 
	 * @return step requests synchronization semaphore instance, or {@code null} is step requests does not require
	 *         synchronization
	 */
	public Semaphore getSemaphore() {
		return semaphore;
	}
}
