/*
 * Copyright 2014-2020 JKOOL, LLC.
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

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Base class for scheduled HTTP based service request/call/query produced activity stream, where each
 * call/request//query response is assumed to represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractWsStream}):
 * <ul>
 * <li>DisableSSL - flag indicating that stream should disable SSL context verification. Default value - {@code false}.
 * (Optional)</li>
 * </ul>
 * 
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractHttpStream extends AbstractWsStream<String, String> {

	private boolean disableSSL = false;

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsStreamProperties.PROP_DISABLE_SSL.equalsIgnoreCase(name)) {
			disableSSL = Utils.toBoolean(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsStreamProperties.PROP_DISABLE_SSL.equalsIgnoreCase(name)) {
			return disableSSL;
		}

		return super.getProperty(name);
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (disableSSL) {
			disableSslVerification();
		}
	}

	@Override
	protected long getActivityItemByteSize(WsResponse<String, String> item) {
		return item == null || item.getData() == null ? 0 : item.getData().getBytes().length;
	}

	/**
	 * Disables SSL context verification.
	 */
	protected void disableSslVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL"); // NON-NLS
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (GeneralSecurityException exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME), "WsStream.disable.ssl.failed",
					exc);
		}
	}

	/**
	 * Fills in request data and parameter values having variable expressions.
	 * 
	 * @param req
	 *            request instance to fill in data
	 * @param url
	 *            optional request URL
	 * @return request instance having filled in values
	 * 
	 * @throws VoidRequestException
	 *             if request can't be build from request context data or built URL is meaningless
	 * 
	 * @see #fillInRequest(com.jkoolcloud.tnt4j.streams.scenario.WsRequest)
	 */
	protected abstract WsRequest<String> fillInRequest(WsRequest<String> req, String url) throws VoidRequestException;
}
