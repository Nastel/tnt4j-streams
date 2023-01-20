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

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.jkoolcloud.tnt4j.core.OpCompCode;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class defines general HTTP response template. Template configuration is considered to be JSON string. Enclosing
 * JSON object start/end brackets may be ommited.
 * <p>
 * Template configuration blocks:
 * <ul>
 * <li>$headers - defines response headers configuration map block</li>
 * <li>$body - defines response body configuration map block</li>
 * <li>$optional - defines optional response section block - if value for eny variable within this block is not
 * resolvable, then such entry is ommited in response payload. It can be within {@code $headers} and {@code $body}
 * blocks</li>
 * </ul>
 * <p>
 * Template body configuration entries:
 * <ul>
 * <li>type - defines response content type: JSON, TEXT, HTML and XML. If not explicitly defined, it sets appropriate
 * response header {@code content-type} value</li>
 * <li>payload - defines body payload template. It can be single string, strings array or map (usually for JSON type
 * responses). See template configuration samples below</li>
 * </ul>
 * <p>
 * Supported variables:
 * <ul>
 * <li>now - variable representing current timestamp</li>
 * <li>message - variable representing any stream provided status message</li>
 * <li>errorMessage - variable representing stream provided error message</li>
 * <li>successMessage - variable representing stream provided success message</li>
 * <li>warnMessage - variable representing stream provided warning message</li>
 * <li>header:[header_name] - variables represents request header named {@code header_name} value</li>
 * <li>attr:[attribute_name] - variables represents request attribute named {@code attribute_name} value</li>
 * <li>param:[parameter_name] - variables represents request parameter named {@code parameter_name} value</li>
 * </ul>
 * <p>
 * Template configuration samples:
 * 
 * JSON response template:
 * 
 * <pre>
 * "$headers": {
 *     "content-type": "application/json"
 * },
 * "$body": {
 *     "type": "JSON",
 *     "payload": {
 *         "requestId": "${header:x-amz-firehose-request-id}",
 *         "timestamp": "${now}",
 *         "$optional": {
 *             "errorMessage": "${errorMessage}"
 *         }
 *     }
 * }
 * </pre>
 * 
 * HTML response template:
 * 
 * <pre>
 * "$body": {
 *     "type": "HTML",
 * 	   "payload": [
 * 	       "&lt;html&gt;&lt;body&gt;&lt;h1&gt;${message}&lt;/h1&gt;&lt;/body&gt;&lt;/html&gt;"
 * 	   ]
 * }
 * </pre>
 *
 * TEXT response template:
 *
 * <pre>
 * "$body": {
 *     "type": "TEXT",
 * 	   "payload": "Response status: ${message}"
 * }
 * </pre>
 *
 * @version $Revision: 1 $
 */
public class GeneralResponseTemplate implements HttpResponseTemplate {

	protected static final String TYPE_TEXT = "TEXT"; // NON-NLS
	protected static final String TYPE_JSON = "JSON"; // NON-NLS
	protected static final String TYPE_HTML = "HTML"; // NON-NLS
	protected static final String TYPE_XML = "XML"; // NON-NLS

	protected static final String CONTENT_TYPE_TEXT = "text/plain"; // NON-NLS
	protected static final String CONTENT_TYPE_HTML = "text/html"; // NON-NLS
	protected static final String CONTENT_TYPE_JSON = "application/json"; // NON-NLS
	protected static final String CONTENT_TYPE_XML = "application/xml"; // NON-NLS

	protected static final String HEADER_CONTENT_TYPE = "content-type"; // NON-NLS

	protected static final String BLOCK_HEADERS = "$headers"; // NON-NLS
	protected static final String BLOCK_BODY = "$body"; // NON-NLS
	protected static final String BLOCK_OPTIONAL = "$optional"; // NON-NLS

	protected static final String ENTRY_TYPE = "type"; // NON-NLS
	protected static final String ENTRY_PAYLOAD = "payload"; // NON-NLS

	protected static final String JSON_START_OBJ = "{"; // NON-NLS
	protected static final String JSON_START_ARRAY = "["; // NON-NLS
	protected static final String JSON_END_OBJ = "}"; // NON-NLS

	protected static final String VAR_NAME_NOW = "now"; // NON-NLS
	protected static final String VAR_NAME_MSG = "message"; // NON-NLS
	protected static final String VAR_NAME_ERR_MSG = "errorMessage"; // NON-NLS
	protected static final String VAR_NAME_SCS_MSG = "successMessage"; // NON-NLS
	protected static final String VAR_NAME_WRN_MSG = "warnMessage"; // NON-NLS

	protected static final String VAR_NAME_PREFIX_HEADER = "header:"; // NON-NLS
	protected static final String VAR_NAME_PREFIX_ATTR = "attr:"; // NON-NLS
	protected static final String VAR_NAME_PREFIX_PARAM = "param:"; // NON-NLS

	protected final Map<String, String> headers = new HashMap<>(5);
	protected final Map<String, Object> body = new HashMap<>(5);

	/**
	 * Constructs a new GeneralResponseTemplate.
	 *
	 * @param template
	 *            template configuration JSON string
	 */
	public GeneralResponseTemplate(String template) {
		parse(template, headers, body);
	}

	/**
	 * Parses provided template configuration JSON string into header and body configuration maps.
	 *
	 * @param template
	 *            template configuration JSON string
	 * @param headers
	 *            headers configuration map to parse configuration into
	 * @param body
	 *            body configuration map to parse configuration into
	 */
	@SuppressWarnings("unchecked")
	protected void parse(String template, Map<String, String> headers, Map<String, Object> body) {
		if (!StringUtils.startsWithAny(template, JSON_START_OBJ, JSON_START_ARRAY)) {
			template = JSON_START_OBJ + template + JSON_END_OBJ;
		}
		Map<String, ?> templateMap = Utils.fromJsonToMap(template, false);

		Map<String, ?> tmHeaders = (Map<String, ?>) templateMap.get(BLOCK_HEADERS);
		if (tmHeaders != null) {
			for (Map.Entry<String, ?> the : tmHeaders.entrySet()) {
				headers.put(the.getKey(), Utils.toString(the.getValue()));
			}
		}

		Map<String, ?> tmBody = (Map<String, ?>) templateMap.get(BLOCK_BODY);
		if (tmBody != null) {
			body.putAll(tmBody);
		}
	}

	@Override
	public Map<String, String> fillHeaders(HttpServletRequest req) {
		Map<String, String> fHeaders = new HashMap<>(headers);
		Set<String> keysToRemove = new HashSet<>(5);

		for (Map.Entry<String, String> headerE : fHeaders.entrySet()) {
			String hValue = headerE.getValue();
			if (Utils.isVariableExpression(hValue)) {
				hValue = processVars(hValue, req, null, null);

				headerE.setValue(hValue);
				if (StringUtils.trimToNull(hValue) == null) {
					keysToRemove.add(headerE.getKey());
				}
			}
		}

		String bodyType = (String) body.get(ENTRY_TYPE);
		if (StringUtils.isNotEmpty(bodyType) && fHeaders.get(HEADER_CONTENT_TYPE) == null) {
			switch (bodyType.toUpperCase()) {
			case TYPE_JSON:
				fHeaders.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
				break;
			case TYPE_XML:
				fHeaders.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML);
				break;
			case TYPE_HTML:
				fHeaders.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML);
				break;
			case TYPE_TEXT:
			default:
				fHeaders.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_TEXT);
				break;
			}
		}

		if (!keysToRemove.isEmpty()) {
			for (String rKey : keysToRemove) {
				headers.remove(rKey);
			}
		}

		return fHeaders;
	}

	/**
	 * Fills variable string with values.
	 *
	 * @param varString
	 *            string containing variable expressions
	 * @param req
	 *            Http request used to resolve value
	 * @param msg
	 *            response message
	 * @param msgType
	 *            response message type
	 * @return variable string prefilled with values
	 */
	protected static String processVars(String varString, HttpServletRequest req, String msg, OpCompCode msgType) {
		Collection<String> exprVars = new ArrayList<>();
		Utils.resolveExpressionVariables(exprVars, varString);

		if (CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				String varName = Utils.getVarName(eVar);
				Object varValue = getVarValue(varName, req, msg, msgType);
				if (varValue != null) {
					varString = varString.replace(eVar, Utils.toString(varValue));
				} else {
					varString = varString.replace(eVar, "");
				}
			}
		}

		return varString;
	}

	/**
	 * Resolves provided variable value from provided context: Http request and response message.
	 *
	 * @param varName
	 *            variable name
	 * @param req
	 *            Http request used to resolve value
	 * @param msg
	 *            response message
	 * @param msgType
	 *            response message type
	 * @return resolved variable value, or {@code null} if value is unresolvable
	 */
	protected static Object getVarValue(String varName, HttpServletRequest req, String msg, OpCompCode msgType) {
		if (VAR_NAME_NOW.equalsIgnoreCase(varName)) {
			return UsecTimestamp.now().getTimeMillis();
		} else if (VAR_NAME_MSG.equalsIgnoreCase(varName)) {
			return msg;
		} else if (VAR_NAME_ERR_MSG.equalsIgnoreCase(varName)) {
			return msgType == OpCompCode.ERROR ? msg : null;
		} else if (VAR_NAME_SCS_MSG.equalsIgnoreCase(varName)) {
			return msgType == OpCompCode.SUCCESS ? msg : null;
		} else if (VAR_NAME_WRN_MSG.equalsIgnoreCase(varName)) {
			return msgType == OpCompCode.WARNING ? msg : null;
		} else if (varName.startsWith(VAR_NAME_PREFIX_HEADER)) {
			String hName = varName.substring(VAR_NAME_PREFIX_HEADER.length());
			return req == null ? null : req.getHeader(hName);
		} else if (varName.startsWith(VAR_NAME_PREFIX_ATTR)) {
			String aName = varName.substring(VAR_NAME_PREFIX_ATTR.length());
			return req == null ? null : req.getAttribute(aName);
		} else if (varName.startsWith(VAR_NAME_PREFIX_PARAM)) {
			String pName = varName.substring(VAR_NAME_PREFIX_PARAM.length());
			return req == null ? null : req.getParameter(pName);
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String fillBody(HttpServletRequest req, String msg, OpCompCode msgType) {
		String bodyType = (String) body.get(ENTRY_TYPE);
		Object bodyPayload = body.get(ENTRY_PAYLOAD);

		String bodyStr = "";
		if (bodyPayload instanceof Collection) {
			Collection<String> bodyLines = (Collection<String>) bodyPayload;

			for (String bodyLine : bodyLines) {
				if (Utils.isVariableExpression(bodyLine)) {
					bodyLine = processVars(bodyLine, req, msg, msgType);
				}

				bodyStr += bodyLine;
			}
		} else if (bodyPayload instanceof Map) {
			Map<String, Object> bodyMap = new HashMap<>((Map<String, ?>) bodyPayload);
			Map<String, Object> optionalsMap = new HashMap<>(3);

			for (Map.Entry<String, Object> bodyE : bodyMap.entrySet()) {
				Object befValue = bodyE.getValue();
				if (befValue instanceof String) {
					String strValue = (String) befValue;
					if (Utils.isVariableExpression(strValue)) {
						strValue = processVars(strValue, req, msg, msgType);

						bodyE.setValue(strValue);
					}
				} else if (BLOCK_OPTIONAL.equals(bodyE.getKey())) {
					Map<String, ?> bOptionals = (Map<String, ?>) bodyE.getValue();

					for (Map.Entry<String, ?> optionalE : bOptionals.entrySet()) {
						Object oValue = optionalE.getValue();
						if (oValue instanceof String) {
							String strValue = (String) oValue;
							if (Utils.isVariableExpression(strValue)) {
								strValue = processVars(strValue, req, msg, msgType);
								strValue = StringUtils.trimToNull(strValue);
							}

							oValue = strValue;
						}

						if (oValue != null) {
							optionalsMap.put(optionalE.getKey(), oValue);
						}
					}
				}
			}

			bodyMap.putAll(optionalsMap);
			bodyMap.remove(BLOCK_OPTIONAL);

			if (TYPE_JSON.equalsIgnoreCase(bodyType)) {
				Gson gson = new Gson();
				bodyStr = gson.toJson(bodyMap);
			}
		} else {
			bodyStr = Utils.toString(bodyPayload);

			if (Utils.isVariableExpression(bodyStr)) {
				bodyStr = processVars(bodyStr, req, msg, msgType);
			}
		}

		return bodyStr;
	}
}
