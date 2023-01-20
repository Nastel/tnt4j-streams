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

package com.jkoolcloud.tnt4j.streams.scenario;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class defines TNT4J-Streams-WS request data container.
 *
 * @param <T>
 *            type of request data
 *
 * @version $Revision: 3 $
 */
public class WsRequest<T> implements AutoIdGenerator, Cloneable {
	private String id;
	private String[] tags;
	private T data;
	private Map<String, Parameter> parameters = new LinkedHashMap<>();
	private List<Condition> conditions = new ArrayList<>();
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
	 * Returns request identifier.
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

	/**
	 * Sets request data package.
	 * 
	 * @param data
	 *            request data package
	 */
	public void setData(T data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return String.valueOf(data);
	}

	/**
	 * Returns fully qualified name of request.
	 * 
	 * @return fully qualified name of request
	 */
	public String fqn() {
		return scenarioStep.getName() + ":" + id; // NON-NLS
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
	 * Returns request (command/query/etc.) parameters and context properties values map. Context properties are picked
	 * from scenario and step.
	 * 
	 * @return request parameters and context properties values map
	 */
	public Map<String, ?> getParametersMap() {
		Map<String, Object> pMap = new HashMap<>(parameters.size());

		if (scenarioStep.getScenario() != null && scenarioStep.getScenario().hasProperties()) {
			pMap.putAll(scenarioStep.getScenario().getPropertiesMap());
		}

		if (scenarioStep.hasProperties()) {
			pMap.putAll(scenarioStep.getPropertiesMap());
		}

		for (Map.Entry<String, Parameter> me : parameters.entrySet()) {
			pMap.put(me.getValue().id, me.getValue().value);
		}

		return pMap;
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
	public Object getParameterValue(String pKey) {
		Parameter param = getParameter(pKey);

		return param == null ? null : param.getValue();
	}

	/**
	 * Returns request parameter value as string.
	 * 
	 * @param pKey
	 *            parameter key
	 * @return request parameter value as string, or {@code null} if request has no such parameter
	 * 
	 * @see #getParameter(String)
	 * @see com.jkoolcloud.tnt4j.streams.scenario.WsRequest.Parameter#getStringValue()
	 */
	public String getParameterStringValue(String pKey) {
		Parameter param = getParameter(pKey);

		return param == null ? null : param.getStringValue();
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
	 * Adds condition for this request.
	 * 
	 * @param id
	 *            condition identifier
	 * @param resolution
	 *            condition resolution name
	 * @param matchExps
	 *            condition match expressions list
	 * @return constructed condition instance
	 */
	public Condition addCondition(String id, String resolution, List<String> matchExps) {
		Condition cond = new Condition(StringUtils.isEmpty(id) ? getAutoId() : id, resolution);
		cond.setMatchExpressions(matchExps);

		conditions.add(cond);

		return cond;
	}

	/**
	 * Returns request (command/query/etc.) conditions list.
	 * 
	 * @return request bound conditions list
	 */
	public List<Condition> getConditions() {
		return conditions;
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
	 * @param transient_
	 *            transiency flag
	 */
	public void addParameter(String id, String value, boolean transient_) {
		addParameter(new Parameter(id, value, transient_));
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
	 * @param transient_
	 *            transiency flag
	 */
	public void addParameter(String id, String value, String type, String format, boolean transient_) {
		addParameter(new Parameter(id, value, type, format, transient_));
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WsRequest) {
			WsRequest<?> other = (WsRequest<?>) obj;
			return id == null ? data.equals(other.data) : id.equals(other.id);
		}

		return false;
	}

	/**
	 * Creates and returns a copy of this WS request.
	 * <p>
	 * Parameters collection elements are cloned into new collection. No changes made on cloned request instance
	 * parameters values shall make impact for this instance values.
	 * 
	 * @return cloned instance of this WS request
	 */
	@Override
	public WsRequest<T> clone() {
		WsRequest<T> cReq = new WsRequest<>(data, tags);
		cReq.id = id;
		cReq.scenarioStep = scenarioStep;

		if (isParametersDynamic()) {
			for (Map.Entry<String, Parameter> param : parameters.entrySet()) {
				cReq.addParameter(param.getValue().clone());
			}
		} else {
			cReq.parameters.putAll(parameters);
		}

		cReq.conditions.addAll(conditions);

		return cReq;
	}

	/**
	 * Checks if this request has any dynamically resolvable variable expressions within data or parameters definitions.
	 * 
	 * @return {@code true} if request data or parameters has variable expressions, {@code false} - otherwise
	 * 
	 * @see #isDataDynamic()
	 * @see #isParametersDynamic()
	 */
	public boolean isDynamic() {
		return isDataDynamic() || isParametersDynamic();
	}

	/**
	 * Checks if this request has any dynamically resolvable variable expressions within data.
	 * 
	 * @return {@code true} if request data has variable expressions, {@code false} - otherwise
	 */
	public boolean isDataDynamic() {
		return Utils.isVariableExpression(Utils.toString(data));
	}

	/**
	 * Checks if this request has any dynamically resolvable variable expressions within parameters definitions.
	 * 
	 * @return {@code true} if request parameters has variable expressions, {@code false} - otherwise
	 */
	public boolean isParametersDynamic() {
		for (Map.Entry<String, Parameter> pe : parameters.entrySet()) {
			if (pe.getValue().isDynamic()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Class defining request parameter properties.
	 */
	public static class Parameter implements Cloneable {
		private String id;
		private Object value;
		private String type;
		private String format;
		private boolean transient_ = false;

		/**
		 * Constructs a new Parameter. Defines parameter identifier and value.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 */
		public Parameter(String id, Object value) {
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
		public Parameter(String id, Object value, String type) {
			this(id, value, type, null);
		}

		/**
		 * Constructs a new Parameter. Defines parameter identifier, value and type.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 * @param transient_
		 *            transiency flag
		 */
		public Parameter(String id, Object value, boolean transient_) {
			this(id, value, null, null, transient_);
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
		public Parameter(String id, Object value, String type, String format) {
			this(id, value, type, format, false);
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
		 * @param transient_
		 *            transiency flag
		 */
		public Parameter(String id, Object value, String type, String format, boolean transient_) {
			this.id = id;
			this.value = value;
			this.type = type;
			this.format = format;
			this.transient_ = transient_;
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
		 * Sets parameter value.
		 * 
		 * @param value
		 *            parameter value
		 */
		public void setValue(Object value) {
			this.value = value;
		}

		/**
		 * Returns parameter value.
		 *
		 * @return parameter value
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * Returns parameter value as string.
		 *
		 * @return parameter value as string
		 */
		public String getStringValue() {
			return Utils.toString(value);
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

		/**
		 * Returns parameter transiency flag.
		 * <p>
		 * When parameter is marked transient, value of it shall be not used directly within request formation. It may
		 * be some interim value used within mapping request and response.
		 * 
		 * @return {@code true} if parameter is transient, {@code false} - otherwise
		 */
		public boolean isTransient() {
			return transient_;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Parameter{"); // NON-NLS
			sb.append("id=").append(Utils.sQuote(id)); // NON-NLS
			sb.append(", value=").append(Utils.sQuote(getStringValue())); // NON-NLS
			sb.append(", type=").append(Utils.sQuote(type)); // NON-NLS
			sb.append(", format=").append(Utils.sQuote(format)); // NON-NLS
			sb.append(", transient=").append(Utils.sQuote(transient_)); // NON-NLS
			sb.append('}'); // NON-NLS
			return sb.toString();
		}

		/**
		 * Creates and returns a copy of this request parameter.
		 * 
		 * @return cloned instance of this request parameter
		 */
		@Override
		public Parameter clone() {
			Parameter cParam = new Parameter(id, value, type, format, transient_);

			return cParam;
		}

		/**
		 * Checks if this request parameter has any dynamically resolvable variable expressions within identifier or
		 * value.
		 * 
		 * @return {@code true} if parameter identifier or value has variable expressions, {@code false} - otherwise
		 */
		public boolean isDynamic() {
			return Utils.isVariableExpression(id) || Utils.isVariableExpression(getStringValue());
		}
	}
}
