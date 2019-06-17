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
import java.util.concurrent.Semaphore;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.quartz.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.scenario.WsReqResponse;
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
 * {@link AbstractWsStream}):
 * <ul>
 * <li>MaxTotalPoolConnections - defines the maximum number of total open connections in the HTTP connections pool.
 * Default value - {@code 5}. (Optional)</li>
 * <li>DefaultMaxPerRouteConnections - defines the maximum number of concurrent connections per HTTP route. Default
 * value - {@code 2}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see org.apache.http.client.HttpClient#execute(HttpUriRequest)
 */
public class RestStream extends AbstractWsStream<String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(RestStream.class);

	private int maxTotalPoolConnections = 5;
	private int defaultMaxPerRouteConnections = 2;

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
	protected long getActivityItemByteSize(WsResponse<String> item) {
		return item == null || item.getData() == null ? 0 : item.getData().getBytes().length;
	}

	@Override
	protected JobDetail buildJob(String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(RestCallJob.class).withIdentity(jobId).usingJobData(jobAttrs).build();
	}

	@Override
	protected void initialize() throws Exception {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(maxTotalPoolConnections);
		cm.setDefaultMaxPerRoute(defaultMaxPerRouteConnections);
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

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
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

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
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
	 * Performs pre-processing of request/command/query data provided over URL string: it can be expression evaluation,
	 * filling in variable values and so on.
	 *
	 * @param requestData
	 *            URL string having request/command/query data
	 * @return preprocessed request/command/query data URL string
	 */
	protected String preProcessURL(String requestData) {
		return preProcess(requestData);
	}

	/**
	 * Scheduler job to execute JAX-RS call.
	 */
	public static class RestCallJob implements Job {

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
				String reqDataStr;
				String respStr;
				Semaphore aquiredSemaphore = null;
				for (WsRequest<String> request : scenarioStep.getRequests()) {
					reqDataStr = null;
					respStr = null;
					try {
						aquiredSemaphore = acquireSemaphore(stream, request);
						reqDataStr = stream.preProcess(request.getData());
						request.setSentData(reqDataStr);
						respStr = stream.executePOST(stream.client, scenarioStep.getUrlStr(), reqDataStr,
								scenarioStep.getUsername(), scenarioStep.getPassword());
					} catch (Throwable exc) {
						Utils.logThrowable(stream.logger(), OpLevel.ERROR,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"RestStream.execute.exception", stream.getName(), exc);
					}

					if (StringUtils.isNotEmpty(respStr)) {
						stream.addInputToBuffer(new WsReqResponse<>(respStr, request));
					} else {
						releaseSemaphore(aquiredSemaphore, stream, scenarioStep.getName(), request);
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

			String reqUrl;
			String respStr;
			for (WsRequest<String> request : scenarioStep.getRequests()) {
				reqUrl = null;
				respStr = null;
				try {
					reqUrl = stream.preProcessURL(scenarioStep.getUrlStr());
					request.setSentData(reqUrl);
					respStr = stream.executeGET(stream.client, reqUrl, scenarioStep.getUsername(),
							scenarioStep.getPassword());
				} catch (Throwable exc) {
					Utils.logThrowable(stream.logger(), OpLevel.ERROR,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"RestStream.execute.exception", stream.getName(), exc);
				}

				if (StringUtils.isNotEmpty(respStr)) {
					stream.addInputToBuffer(new WsReqResponse<>(respStr, request));
				}
			}
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
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
