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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class defines TNT4J-Streams-WS request data container.
 *
 * @param <T>
 *            type of request data
 *
 * @version $Revision: 2 $
 */
public class WsRequest<T> {
	private String id;
	private String[] tags;
	private T data;
	private T sentData;
	private Map<String, Parameter> parameters = new HashMap<>();
	private WsScenarioStep scenarioStep;

	/**
	 * Constructs a new WsRequest. Defines request data and tag as {@code null}.
	 *
	 * @param requestData
	 *            request data package
	 */
	public WsRequest(T requestData) {
		this(requestData, null);
	}

	/**
	 * Constructs a new WsRequest. Defines request data and tag.
	 *
	 * @param requestData
	 *            request data package
	 * @param tags
	 *            request tags
	 */
	public WsRequest(T requestData, String... tags) {
		this.data = requestData;
		this.tags = tags;
	}

	/**
	 * Returns request identifier
	 * 
	 * @return request identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets request identifier.
	 * 
	 * @param id
	 *            request identifier
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns request tag strings array.
	 *
	 * @return request tag strings array
	 */
	public String[] getTags() {
		return tags;
	}

	/**
	 * Returns request data package.
	 *
	 * @return request data package
	 */
	public T getData() {
		return data;
	}

	@Override
	public String toString() {
		return String.valueOf(data);
	}

	public T getSentData() {
		return sentData;
	}

	public void setSentData(T sentData) {
		this.sentData = sentData;
	}

	/**
	 * Returns request (command/query/etc.) parameters map.
	 *
	 * @return request parameters map
	 */
	public Map<String, Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Sets parameters map for this request.
	 * 
	 * @param params
	 *            parameters map to be set for this request
	 */
	public void setParameters(Map<String, Parameter> params) {
		if (params != null) {
			parameters.putAll(params);
		}
	}

	/**
	 * Returns request parameter mapped to provided key.
	 * 
	 * @param pKey
	 *            parameter key
	 * @return request parameter mapped to provided key, or {@code null} if request has no such parameter
	 */
	public Parameter getParameter(String pKey) {
		return parameters.get(pKey);
	}

	/**
	 * Returns request parameter value.
	 * 
	 * @param pKey
	 *            parameter key
	 * @return request parameter value, or {@code null} if request has no such parameter
	 * 
	 * @see #getParameter(String)
	 * @see com.jkoolcloud.tnt4j.streams.scenario.WsRequest.Parameter#getValue()
	 */
	public String getParameterValue(String pKey) {
		Parameter param = getParameter(pKey);

		return param == null ? null : param.getValue();
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param parameter
	 *            request parameter
	 */
	public void addParameter(Parameter parameter) {
		if (StringUtils.isEmpty(parameter.id)) {
			parameter.id = String.valueOf(parameters.size() + 1);
		}
		parameters.put(parameter.id, parameter);
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param id
	 *            parameter identifier
	 * @param value
	 *            parameter value
	 */
	public void addParameter(String id, String value) {
		addParameter(new Parameter(id, value));
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param id
	 *            parameter identifier
	 * @param value
	 *            parameter value
	 * @param type
	 *            parameter type
	 */
	public void addParameter(String id, String value, String type) {
		addParameter(new Parameter(id, value, type));
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param id
	 *            parameter identifier
	 * @param value
	 *            parameter value
	 * @param type
	 *            parameter type
	 * @param format
	 *            parameter format
	 */
	public void addParameter(String id, String value, String type, String format) {
		addParameter(new Parameter(id, value, type, format));
	}

	/**
	 * Returns scenario step bound to this request.
	 * 
	 * @return scenario step bound to this request
	 */
	public WsScenarioStep getScenarioStep() {
		return scenarioStep;
	}

	/**
	 * Sets scenario step bound to this request.
	 * 
	 * @param scenarioStep
	 *            scenario step to bind to this request
	 */
	public void setScenarioStep(WsScenarioStep scenarioStep) {
		this.scenarioStep = scenarioStep;
	}

	/**
	 * Class defining request parameter properties.
	 */
	public static class Parameter {
		private String id;
		private String value;
		private String type;
		private String format;

		/**
		 * Constructs a new Parameter. Defines parameter identifier and value.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 */
		public Parameter(String id, String value) {
			this(id, value, null, null);
		}

		/**
		 * Constructs a new Parameter. Defines parameter identifier, value and type.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 * @param type
		 *            parameter type
		 */
		public Parameter(String id, String value, String type) {
			this(id, value, type, null);
		}

		/**
		 * Constructs a new Parameter. Defines parameter identifier, value and type.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 * @param type
		 *            parameter type
		 * @param format
		 *            parameter format
		 */
		public Parameter(String id, String value, String type, String format) {
			this.id = id;
			this.value = value;
			this.type = type;
			this.format = format;
		}

		/**
		 * Returns parameter identifier.
		 *
		 * @return parameter identifier
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns parameter value.
		 *
		 * @return parameter value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Returns parameter type.
		 *
		 * @return parameter type
		 */
		public String getType() {
			return type;
		}

		/**
		 * Returns parameter format.
		 *
		 * @return parameter format
		 */
		public String getFormat() {
			return format;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Parameter{"); // NON-NLS
			sb.append("id=").append(Utils.sQuote(id)); // NON-NLS
			sb.append(", value=").append(Utils.sQuote(value)); // NON-NLS
			sb.append(", type=").append(Utils.sQuote(type)); // NON-NLS
			sb.append(", format=").append(Utils.sQuote(format)); // NON-NLS
			sb.append('}'); // NON-NLS
			return sb.toString();
		}
	}
}
