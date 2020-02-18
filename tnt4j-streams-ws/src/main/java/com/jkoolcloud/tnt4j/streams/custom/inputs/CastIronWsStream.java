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

package com.jkoolcloud.tnt4j.streams.custom.inputs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.soap.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.WsStream;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenario;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Class implementing IBM Cast Iron WebService reported Logs, Jobs and other metrics streaming.
 * <p>
 * This stream performs {@code "login"} request to obtain session identifier (token) used to retrieve metrics data. On
 * metrics retrieval request failure, {@code "login"} request is reissued.
 * <p>
 * Default metrics retrieval requests are issued sequentially: first - is requested a number of available entries, and
 * second - issuing "search" (list) request providing offset and number of entries to return. Other metrics retrieval
 * scenarios are possible by providing different scenario steps/requests configuration.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.streams.inputs.WsStream}):
 * <ul>
 * <li>SecurityCachedTokenKey - defines streams cache entry key referring {@code "login"} request received session ID
 * token. Default value - {@code "Token"}. (Optional)</li>
 * <li>SecurityResponseParserTag - defines tag value used to map {@code "login"} request data and parser used to parse
 * it. Default value - {@code "login"}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class CastIronWsStream extends WsStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(CastIronWsStream.class);

	private String tokenCacheKey = "Token"; // NON-NLS
	private String securityResponseParserTag = "login"; // NON-NLS

	private final Lock faultHandlingLock = new ReentrantLock();

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsStreamProperties.PROP_SECURITY_CACHED_TOKEN_KEY.equalsIgnoreCase(name)) {
			tokenCacheKey = value;
		} else if (WsStreamProperties.PROP_SECURITY_RESPONSE_PARSER_TAG.equalsIgnoreCase(name)) {
			securityResponseParserTag = value;
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsStreamProperties.PROP_SECURITY_CACHED_TOKEN_KEY.equalsIgnoreCase(name)) {
			return tokenCacheKey;
		}
		if (WsStreamProperties.PROP_SECURITY_RESPONSE_PARSER_TAG.equalsIgnoreCase(name)) {
			return securityResponseParserTag;
		}

		return super.getProperty(name);
	}

	/**
	 * Appends SOAP request message with {@code "sessionId"} header having token received from scenario {@code "login"}
	 * step and saved in streams cache.
	 *
	 * @param soapRequest
	 *            SOAP request message instance
	 * @throws javax.xml.soap.SOAPException
	 *             if there was an error adding the SOAP message header
	 */
	@Override
	protected void addSoapHeaders(SOAPMessage soapRequest) throws SOAPException {
		SOAPFactory soapFactory = SOAPFactory.newInstance();
		SOAPElement sessionIdElem = soapFactory.createElement("sessionId", "sec", // NON-NLS
				"http://www.approuter.com/schemas/2008/1/security"); // NON-NLS

		String cachedToken = String.valueOf(StreamsCache.getValue(tokenCacheKey));
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"CastIronStream.adding.req.header", sessionIdElem.getLocalName(), cachedToken);
		sessionIdElem.setTextContent(cachedToken);
		soapRequest.getSOAPHeader().addChildElement(sessionIdElem);
	}

	/**
	 * Handles response contained SOAP fault. If fault indicates expired/invalid session id reissues {@code "login"}
	 * request.
	 *
	 * @param fault
	 *            SOAP fault to handle
	 * @param scenario
	 *            scenario of failed request
	 */
	@Override
	protected void handleFault(SOAPFault fault, WsScenario scenario) {
		if (fault.getFaultString().equals("Expired or invalid session ID")) { // NON-NLS // TODO use code
			faultHandlingLock.lock();
			try {
				if (scenario.getLoginStep() == null || scenario.getLoginStep().isEmpty()) {
					throw new RuntimeException(
							StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
									"CastIronStream.empty.login.step", scenario.getName()));
				}

				login(scenario.getLoginStep());
				String cachedToken = String.valueOf(StreamsCache.getValue(tokenCacheKey));
				throw new RuntimeException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
						"CastIronStream.logged.in.after.fault", scenario.getName(), cachedToken));
			} finally {
				faultHandlingLock.unlock();
			}
		} else {
			super.handleFault(fault, scenario);
		}
	}

	private void login(WsScenarioStep loginStep) {
		if (loginStep == null || loginStep.isEmpty()) {
			return; // TODO: ???
		}

		try {
			RequestDataAndHeaders requestDataAndHeaders = new RequestDataAndHeaders()
					.resolve(fillInRequestData(loginStep.getRequests().get(0).getData()));
			SOAPMessage soapRequest = createMessage(requestDataAndHeaders.getRequest(),
					requestDataAndHeaders.getHeaders(), false);
			logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"CastIronStream.login.request", toXMLString(soapRequest));

			SOAPMessage soapResponse = createSOAPConnection().call(soapRequest,
					fillInRequestData(loginStep.getUrlStr()));

			if (soapResponse.getSOAPBody().hasFault()) {
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"CastIronStream.login.failed", soapResponse.getSOAPBody().getFault().getFaultString());
			}

			String responseString = toXMLString(soapResponse);

			logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"CastIronStream.login.response", responseString);

			applyParsers(responseString, securityResponseParserTag);
		} catch (Throwable exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME), "CastIronStream.login.failed",
					exc);
		}
	}

	/**
	 * Resolved tags values from SOAP received response data to map parsers to be used to parse it. Tag is SOAP response
	 * message method name.
	 *
	 * @param data
	 *            activity data item to get tags
	 * @return resolved data tags array, or {@code null} if there is no tags resolved
	 */
	@Override
	public String[] getDataTags(Object data) {
		try (InputStream is = new ByteArrayInputStream(String.valueOf(data).getBytes())) {
			SOAPMessage request = MessageFactory.newInstance().createMessage(null, is);
			String currentMethod = request.getSOAPBody().getFirstChild().getLocalName().replace("Response", ""); // NON-NLS

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"CastIronStream.parser.tag.resolved", currentMethod);

			return new String[] { currentMethod };
		} catch (Throwable exc) {
			logger().log(OpLevel.ERROR, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"CastIronStream.parser.tag.resolve.failed", Utils.getExceptionMessages(exc), data);
		}

		return null;
	}
}
