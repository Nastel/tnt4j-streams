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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Implements a scheduled JAX-RS service call activity stream, where each call response is assumed to represent a single
 * activity or event which should be recorded.
 * <p>
 * Service call is performed by invoking {@link org.apache.http.client.HttpClient#execute(HttpUriRequest)} with GET or
 * POST method request depending on scenario step configuration parameter 'method'. Default method is GET. Stream uses
 * {@link org.apache.http.impl.conn.PoolingHttpClientConnectionManager} to handle connections pool used by HTTP client.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractHttpStream}):
 * <ul>
 * <li>MaxTotalPoolConnections - defines the maximum number of total open connections in the HTTP connections pool.
 * Default value - {@code 5}. (Optional)</li>
 * <li>DefaultMaxPerRouteConnections - defines the maximum number of concurrent connections per HTTP route. Default
 * value - {@code 2}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 3 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see org.apache.http.client.HttpClient#execute(HttpUriRequest)
 */
public class RestStream extends AbstractHttpStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(RestStream.class);

	private int maxTotalPoolConnections = 5;
	private int defaultMaxPerRouteConnections = 2;

	/**
	 * HTTP client instance used to execute JAX-RS calls.
	 */
	protected CloseableHttpClient client;

	/**
	 * Constructs an empty RestStream. Requires configuration settings to set input stream source.
	 */
	public RestStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsStreamProperties.PROP_MAX_TOTAL_POOL_CONNECTIONS.equalsIgnoreCase(name)) {
			maxTotalPoolConnections = Integer.parseInt(value);
		} else if (WsStreamProperties.PROP_DEFAULT_MAX_PER_ROUTE_CONNECTIONS.equalsIgnoreCase(name)) {
			defaultMaxPerRouteConnections = Integer.parseInt(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsStreamProperties.PROP_MAX_TOTAL_POOL_CONNECTIONS.equalsIgnoreCase(name)) {
			return maxTotalPoolConnections;
		}

		if (WsStreamProperties.PROP_DEFAULT_MAX_PER_ROUTE_CONNECTIONS.equalsIgnoreCase(name)) {
			return defaultMaxPerRouteConnections;
		}

		return super.getProperty(name);
	}

	@Override
	protected JobDetail buildJob(String group, String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(RestCallJob.class).withIdentity(jobId, group).usingJobData(jobAttrs).build();
	}

	@Override
	protected void initialize() throws Exception {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(maxTotalPoolConnections);
		cm.setDefaultMaxPerRoute(defaultMaxPerRouteConnections);
		// cm.setValidateAfterInactivity((int) TimeUnit.SECONDS.toMillis(4 * 60));
		client = HttpClients.custom().setConnectionManager(cm).build();

		super.initialize();
	}

	@Override
	protected void cleanup() {
		super.cleanup();

		Utils.close(client);
	}

	/**
	 * Performs JAX-RS service call over HTTP GET method request.
	 *
	 * @param client
	 *            HTTP client instance to execute request
	 * @param uriStr
	 *            JAX-RS service URI
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executeGET(CloseableHttpClient client, String uriStr) throws Exception {
		return executeGET(client, uriStr, null, null);
	}

	/**
	 * Performs JAX-RS service call over HTTP GET method request.
	 *
	 * @param client
	 *            HTTP client instance to execute request
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param username
	 *            user name used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @param password
	 *            password used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executeGET(CloseableHttpClient client, String uriStr, String username, String password)
			throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"RestStream.cant.execute.get.request", uriStr);
			return null;
		}

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.get.request", uriStr);

		HttpGet get = new HttpGet(uriStr);
		String respStr = executeRequest(client, get, username, password);

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.received.response", uriStr, respStr);

		return respStr;
	}

	/**
	 * Performs JAX-RS service call over HTTP POST method request.
	 *
	 * @param client
	 *            HTTP client instance to execute request
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param reqData
	 *            request data
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executePOST(CloseableHttpClient client, String uriStr, String reqData) throws Exception {
		return executePOST(client, uriStr, reqData, null, null);
	}

	/**
	 * Performs JAX-RS service call over HTTP POST method request.
	 *
	 * @param client
	 *            HTTP client instance to execute request
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param reqData
	 *            request data
	 * @param username
	 *            user name used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @param password
	 *            password used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executePOST(CloseableHttpClient client, String uriStr, String reqData, String username,
			String password) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"RestStream.cant.execute.post.request", uriStr);
			return null;
		}

		if (StringUtils.isEmpty(reqData)) {
			return executeGET(client, uriStr, username, password);
		}

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.post.request", uriStr, reqData);

		HttpPost post = new HttpPost(uriStr);
		StringEntity reqEntity = new StringEntity(reqData);

		// here instead of JSON you can also have XML
		reqEntity.setContentType("application/json"); // NON-NLS
		post.setEntity(reqEntity);

		String respStr = executeRequest(client, post, username, password);

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.received.response", uriStr, respStr);

		return respStr;
	}

	private static String executeRequest(CloseableHttpClient client, HttpUriRequest req, String username,
			String password) throws IOException {
		if (client == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "RestStream.client.null"));
		}

		CloseableHttpResponse response = null;
		try {
			HttpContext ctx = HttpClientContext.create();

			if (StringUtils.isNotEmpty(username)) {
				String credentialsStr = username + ":" + password; // NON-NLS
				String encoding = DatatypeConverter.printBase64Binary(credentialsStr.getBytes());
				req.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding); // NON-NLS
			}

			response = client.execute(req, ctx);

			return processResponse(response);
		} finally {
			Utils.close(response);
		}
	}

	private static String processResponse(CloseableHttpResponse response) throws IOException {
		HttpEntity entity = null;
		try {
			StatusLine sLine = response.getStatusLine();
			int responseCode = sLine.getStatusCode();

			if (responseCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
				throw new HttpResponseException(sLine);
			}

			entity = response.getEntity();
			return EntityUtils.toString(entity, StandardCharsets.UTF_8);
		} finally {
			EntityUtils.consumeQuietly(entity);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see #uriForGET(com.jkoolcloud.tnt4j.streams.scenario.WsRequest)
	 */
	@Override
	protected WsRequest<String> fillInRequest(WsRequest<String> req, String url) throws VoidRequestException {
		String reqData = req.getData();
		if (StringUtils.isEmpty(reqData)) {
			req.setData(url);
		}

		WsRequest<String> fReq = fillInRequest(req);
		String uriStr = uriForGET(fReq);
		fReq.setData(uriStr);

		return fReq;
	}

	/**
	 * Scheduler job to execute JAX-RS call.
	 */
	public static class RestCallJob extends CallJob {

		/**
		 * Constructs a new RestCallJob.
		 */
		public RestCallJob() {
		}

		/**
		 * Executes JAX-RS call as HTTP POST request.
		 *
		 * @param scenarioStep
		 *            scenario step to execute
		 * @param stream
		 *            stream instance to execute HTTP POST
		 */
		protected void runPOST(WsScenarioStep scenarioStep, RestStream stream) {
			if (!scenarioStep.isEmpty()) {
				String respStr;
				Semaphore acquiredSemaphore;
				WsRequest<String> processedRequest;
				for (WsRequest<String> request : scenarioStep.requestsArray()) {
					if (stream.isShotDown()) {
						return;
					}

					respStr = null;
					acquiredSemaphore = null;
					processedRequest = null;
					try {
						acquiredSemaphore = stream.acquireSemaphore(request);
						processedRequest = stream.fillInRequest(request);
						respStr = stream.executePOST(stream.client, scenarioStep.getUrlStr(),
								processedRequest.getData(), scenarioStep.getUsername(),
								decPassword(scenarioStep.getPassword()));
					} catch (IOException exc) {
						stream.logger().log(OpLevel.WARNING,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"RestStream.execute.exception", stream.getName(), processedRequest.getId(),
								exc.getMessage());
					} catch (Throwable exc) {
						Utils.logThrowable(stream.logger(), OpLevel.ERROR,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"RestStream.execute.exception", stream.getName(), processedRequest.getId(), exc);
					}

					if (StringUtils.isNotEmpty(respStr)) {
						stream.addInputToBuffer(new WsResponse<>(respStr, processedRequest));
					} else {
						stream.requestFailed(processedRequest);
						stream.releaseSemaphore(acquiredSemaphore, scenarioStep.getName(), request);
					}
				}
			}
		}

		/**
		 * Executes JAX-RS call as HTTP GET request.
		 *
		 * @param scenarioStep
		 *            scenario step to execute
		 * @param stream
		 *            stream instance to execute HTTP GET
		 */
		protected void runGET(WsScenarioStep scenarioStep, RestStream stream) {
			if (scenarioStep.isEmpty()) {
				scenarioStep.addRequest(null, scenarioStep.getUrlStr());
			}

			String respStr;
			Semaphore acquiredSemaphore;
			WsRequest<String> processedRequest;
			for (WsRequest<String> request : scenarioStep.requestsArray()) {
				if (stream.isShotDown()) {
					return;
				}

				respStr = null;
				acquiredSemaphore = null;
				processedRequest = null;
				try {
					acquiredSemaphore = stream.acquireSemaphore(request);
					processedRequest = stream.fillInRequest(request, scenarioStep.getUrlStr());
					respStr = stream.executeGET(stream.client, processedRequest.getData(), scenarioStep.getUsername(),
							decPassword(scenarioStep.getPassword()));
				} catch (VoidRequestException exc) {
					stream.logger().log(OpLevel.INFO,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractHttpStream.void.request", processedRequest.getId(), exc.getMessage());
				} catch (IOException exc) {
					stream.logger().log(OpLevel.WARNING,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"RestStream.execute.exception", stream.getName(), processedRequest.getId(),
							exc.getMessage());
				} catch (Throwable exc) {
					Utils.logThrowable(stream.logger(), OpLevel.ERROR,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"RestStream.execute.exception", stream.getName(), processedRequest.getId(), exc);
				}

				if (StringUtils.isNotEmpty(respStr)) {
					stream.addInputToBuffer(new WsResponse<>(respStr, processedRequest));
				} else {
					stream.requestFailed(processedRequest);
					stream.releaseSemaphore(acquiredSemaphore, scenarioStep.getName(), request);
				}
			}
		}

		@Override
		public void executeCalls(JobDataMap dataMap) {
			RestStream stream = (RestStream) dataMap.get(JOB_PROP_STREAM_KEY);
			WsScenarioStep scenarioStep = (WsScenarioStep) dataMap.get(JOB_PROP_SCENARIO_STEP_KEY);
			String reqMethod = scenarioStep.getMethod();

			if (StringUtils.isEmpty(reqMethod)) {
				reqMethod = ReqMethod.GET.name();
			}

			if (ReqMethod.POST.name().equalsIgnoreCase(reqMethod)) {
				runPOST(scenarioStep, stream);
			} else if (ReqMethod.GET.name().equalsIgnoreCase(reqMethod)) {
				runGET(scenarioStep, stream);
			}
		}
	}

	/**
	 * Appends JAX-RS endpoint URL with query parameter values from request data.
	 *
	 * @param request
	 *            request data package
	 * @return URL string appended with query parameter values
	 * 
	 * @throws com.jkoolcloud.tnt4j.streams.inputs.VoidRequestException
	 *             if request URL is meaningless or can't be build from request context data
	 */
	protected String uriForGET(WsRequest<String> request) throws VoidRequestException {
		String uri = request.getData();
		StringBuilder uriBuilder = new StringBuilder();
		uriBuilder.append(uri);

		int i = uri.indexOf('?');

		if (i == -1) {
			uriBuilder.append('?');
		} else if (i != uri.length() - 1) {
			uriBuilder.append('&');
		}

		List<NameValuePair> params = makeParamsList(request);
		String paramsStr = URLEncodedUtils.format(params, StandardCharsets.UTF_8);

		if (StringUtils.isNotEmpty(paramsStr)) {
			uriBuilder.append(paramsStr);
		}

		return uriBuilder.toString();
	}

	/**
	 * Builds list of request parameters by filling in values from request context data.
	 * 
	 * @param request
	 *            request data package
	 * @return list of request parameters
	 */
	protected List<NameValuePair> makeParamsList(WsRequest<String> request) {
		List<NameValuePair> params = new ArrayList<>(request.getParameters().size());

		for (Map.Entry<String, WsRequest.Parameter> param : request.getParameters().entrySet()) {
			if (param.getValue().isTransient()) {
				continue;
			}

			BasicNameValuePair httpParam = makeParam(param);
			if (httpParam != null) {
				params.add(httpParam);
			}
		}

		return params;
	}

	/**
	 * Builds HTTP parameter from request parameter data and request context data.
	 * 
	 * @param param
	 *            request parameter instance
	 * @return HTTP parameter instance, or {@code null} if parameter can't be built
	 */
	protected BasicNameValuePair makeParam(Map.Entry<String, WsRequest.Parameter> param) {
		return new BasicNameValuePair(param.getKey(), param.getValue().getValue());
	}

	/**
	 * Request method types enumeration.
	 */
	protected enum ReqMethod {
		/**
		 * Request method GET.
		 */
		GET,
		/**
		 * Request method POST.
		 */
		POST,
	}

}
