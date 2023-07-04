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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URLEncodedUtils;
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
 * Service call is performed by invoking
 * {@link org.apache.hc.client5.http.classic.HttpClient#execute(org.apache.hc.core5.http.ClassicHttpRequest, org.apache.hc.core5.http.protocol.HttpContext, org.apache.hc.core5.http.io.HttpClientResponseHandler)}
 * with GET or POST method request depending on scenario step configuration parameter 'method'. Default method is GET.
 * Stream uses {@link org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager} to handle connections pool
 * used by HTTP client.
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
 * @see org.apache.hc.client5.http.classic.HttpClient#execute(org.apache.hc.core5.http.ClassicHttpRequest,
 *      org.apache.hc.core5.http.protocol.HttpContext, org.apache.hc.core5.http.io.HttpClientResponseHandler)
 *      org.apache.hc.core5.http.protocol.HttpContext)
 */
public class RestStream extends AbstractHttpStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(RestStream.class);

	private static final String REQ_HEAD_PARAM_PREFIX = "H:"; // NON-NLS
	private static final String REQ_HEAD_PARAM_AUTH = REQ_HEAD_PARAM_PREFIX + HttpHeaders.AUTHORIZATION; //
	private static final BasicHttpClientResponseHandler respHandler = new BasicHttpClientResponseHandler();

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
	 * @param req
	 *            request instance
	 * @return service response string
	 * 
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executeGET(CloseableHttpClient client, WsRequest<String> req) throws Exception {
		return executeGET(client, req.getData(), req.getParameters());
	}

	/**
	 * Performs JAX-RS service call over HTTP GET method request.
	 *
	 * @param client
	 *            HTTP client instance to execute request
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param reqParams
	 *            request parameters map
	 * @return service response string
	 * 
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executeGET(CloseableHttpClient client, String uriStr,
			Map<String, WsRequest.Parameter> reqParams) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"RestStream.cant.execute.get.request", uriStr);
			return null;
		}

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.get.request", uriStr);

		HttpGet get = new HttpGet(uriStr);
		String respStr = executeRequest(client, get, reqParams);

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.received.response", uriStr, respStr);

		return respStr;
	}

	/**
	 * Performs JAX-RS service call over HTTP POST method request.
	 *
	 * @param client
	 *            HTTP client instance to execute request
	 * @param req
	 *            request instance
	 * @return service response string
	 * 
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executePOST(CloseableHttpClient client, WsRequest<String> req) throws Exception {
		return executePOST(client, req.getScenarioStep().getUrlStr(), req.getData(), req.getParameters());
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
	 * @param reqParams
	 *            request parameters map
	 * @return service response string
	 * 
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executePOST(CloseableHttpClient client, String uriStr, String reqData,
			Map<String, WsRequest.Parameter> reqParams) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"RestStream.cant.execute.post.request", uriStr);
			return null;
		}

		if (StringUtils.isEmpty(reqData)) {
			return executeGET(client, uriStr, reqParams);
		}

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.post.request", uriStr, reqData);

		HttpPost post = new HttpPost(uriStr);
		// here instead of JSON you can also have XML
		StringEntity reqEntity = new StringEntity(reqData, ContentType.APPLICATION_JSON);

		post.setEntity(reqEntity);

		String respStr = executeRequest(client, post, reqParams);

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.received.response", uriStr, respStr);

		return respStr;
	}

	private static String executeRequest(CloseableHttpClient client, HttpUriRequest req,
			Map<String, WsRequest.Parameter> reqParams) throws Exception {
		if (client == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "RestStream.client.null"));
		}

		CloseableHttpResponse response = null;
		try {
			HttpContext ctx = HttpClientContext.create();

			if (reqParams != null) {
				int idx = 0;
				for (Map.Entry<String, WsRequest.Parameter> pe : reqParams.entrySet()) {
					if (pe.getKey().startsWith(REQ_HEAD_PARAM_PREFIX) && !pe.getValue().isTransient()) {
						String pName = pe.getKey().substring(REQ_HEAD_PARAM_PREFIX.length());
						if (StringUtils.isEmpty(pName)) {
							throw new IllegalArgumentException(StreamsResources.getStringFormatted(
									WsStreamConstants.RESOURCE_BUNDLE_NAME, "RestStream.header.param.null", idx));
						}
						req.addHeader(pName, pe.getValue().getStringValue());
					}
					idx++;
				}
			}

			return client.execute(req, ctx, respHandler);
		} finally {
			Utils.close(response);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #makeCredentialsParam(String, String)
	 */
	@Override
	protected WsRequest<String> fillInRequest(WsRequest<String> req) throws VoidRequestException {
		if (req.getParameter(REQ_HEAD_PARAM_AUTH) == null) {
			WsRequest.Parameter hAuthParam = makeCredentialsParam(req.getScenarioStep().getUsername(),
					req.getScenarioStep().getPassword());

			if (hAuthParam != null) {
				req.addParameter(hAuthParam);
			}
		}

		return super.fillInRequest(req);
	}

	/**
	 * Creates request parameter of HTTP message header {@code "Authorization"} for basic authentication.
	 * 
	 * @param username
	 *            user name used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @param password
	 *            password used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @return request parameter instance containing basic authentication value, or {@code null} if no proper
	 *         credentials provided
	 */
	protected static WsRequest.Parameter makeCredentialsParam(String username, String password) {
		if (StringUtils.isNotEmpty(username)) {
			String credentialsStr = username + ":" + decPassword(password); // NON-NLS
			String encoding = Utils.base64EncodeStr(credentialsStr.getBytes());

			return new WsRequest.Parameter(REQ_HEAD_PARAM_AUTH, "Basic " + encoding); // NON-NLS);
		}

		return null;
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

					if (stream.isDropRecurring(request)) {
						continue;
					}

					respStr = null;
					acquiredSemaphore = null;
					processedRequest = null;
					try {
						acquiredSemaphore = stream.acquireSemaphore(request);
						processedRequest = stream.fillInRequest(request);
						respStr = stream.executePOST(stream.client, processedRequest);
					} catch (VoidRequestException exc) {
						stream.logger().log(OpLevel.INFO,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"AbstractWsStream.void.request", request.getId(), exc.getMessage());
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

				if (stream.isDropRecurring(request)) {
					continue;
				}

				respStr = null;
				acquiredSemaphore = null;
				processedRequest = null;
				try {
					acquiredSemaphore = stream.acquireSemaphore(request);
					processedRequest = stream.fillInRequest(request, scenarioStep.getUrlStr());
					respStr = stream.executeGET(stream.client, processedRequest);
				} catch (VoidRequestException exc) {
					stream.logger().log(OpLevel.INFO,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.void.request", request.getId(), exc.getMessage());
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
	 * @throws VoidRequestException
	 *             if request can't be build from request context data or built URL is meaningless
	 */
	protected String uriForGET(WsRequest<String> request) throws VoidRequestException {
		String uri = request.getData();

		Map<String, String> qMap;
		try {
			URI url = new URI(uri);
			qMap = getQueryMap(url.getQuery());
			// uri = url.getPath();
		} catch (URISyntaxException e) {
			qMap = new HashMap<>();
		}

		StringBuilder uriBuilder = new StringBuilder();
		uriBuilder.append(uri);

		List<NameValuePair> params = makeParamsList(request);
		String paramsStr = URLEncodedUtils.format(params, StandardCharsets.UTF_8);

		if (StringUtils.isNotEmpty(paramsStr)) {
			int i = uri.indexOf('?');

			if (i == -1) {
				uriBuilder.append('?');
			} else if (i != uri.length() - 1) {
				uriBuilder.append('&');
			}

			uriBuilder.append(paramsStr);
		}

		return uriBuilder.toString();
	}

	public static Map<String, String> getQueryMap(String query) {
		Map<String, String> map = new HashMap<>();

		if (StringUtils.isNotEmpty(query)) {
			String[] params = query.split("&");
			for (String param : params) {
				String name = param.split("=")[0];
				String value = param.split("=")[1];
				map.put(name, value);
			}
		}

		return map;
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
			if (param.getValue().isTransient() || param.getKey().startsWith(REQ_HEAD_PARAM_PREFIX)) {
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
		return new BasicNameValuePair(param.getKey(), param.getValue().getStringValue());
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
