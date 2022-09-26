/*
 * Copyright 2014-2022 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpCompCode;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.GeneralResponseTemplate;
import com.jkoolcloud.tnt4j.streams.configure.HttpResponseTemplate;
import com.jkoolcloud.tnt4j.streams.configure.ServletStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.configure.build.CfgStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.build.POJOStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.configure.build.StreamsBuilder;
import com.jkoolcloud.tnt4j.streams.inputs.*;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * HTTP servlet implementing web-app to TNT4J-Streams pipe. HTTP requests are processed into activity data maps and
 * passed to bound TNT4J-Stream instances. Servlet accepts POST and GET method requests. GET depicts current running
 * streams states. POST consumes request provided data as activity data package for servlet bound streams.
 * <p>
 * This servlet can also run TNT4J-Streams processing any other publicly available activity data source, such as: files,
 * queues, topics, etc.
 * <p>
 * HTTP requests are processed into activity data maps having such layout:
 * <ul>
 * <li>{@value StreamsConstants#ACTIVITY_DATA_KEY} - raw activity data as {@code byte[]} retrieved from http
 * request.</li>
 * <li>HTTP form data parameters entries.</li>
 * <li>Metadata - request metadata values map. See {@link #getMetadataMap(javax.servlet.http.HttpServletRequest)} for
 * entries set.</li>
 * <li>{@value StreamsConstants#HEADERS_KEY} - request header entries map.</li>
 * <li>Attributes - request attribute entries map.</li>
 * <li>{@value StreamsConstants#TRANSPORT_KEY} - request transport. Value is
 * {@value StreamsConstants#TRANSPORT_HTTP}</li>
 * </ul>
 * <p>
 * Servlet produced HTTP response for POST may be one of:
 * <ul>
 * <li>OK (200) - request got successfully processed.</li>
 * <li>SERVICE_UNAVAILABLE (503) - if servlet has no any HTTP requests processing stream bound</li>
 * <li>NO_CONTENT (204) - if request has no any RAW or FORM parameters provided payload data</li>
 * <li>INSUFFICIENT_STORAGE (507) - if servlet bound streams buffer got overflown.</li>
 * <li>INTERNAL_SERVER_ERROR (500) - if any unhandled exception occurs.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.inputs.HttpServletStream
 */
public class TNT4JStreamsServlet extends HttpServlet {
	private static final long serialVersionUID = -8114776451549344270L;

	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(TNT4JStreamsServlet.class);

	private static final String CFG_KEY_STREAM_CONFIG_DIR = "streams.configs.dir"; // NON-NLS
	private static final String CFG_KEY_TNT4J_CONFIG = "tnt4j.config"; // NON-NLS
	private static final String CFG_KEY_LOG4J_CONFIG = "log4j2.config"; // NON-NLS
	private static final String CFG_KEY_STREAMS_CONFIG = "streams.config"; // NON-NLS

	private static final String DEFAULT_TNT4J_CFG_FILENAME = "tnt4j.properties"; // NON-NLS
	private static final String DEFAULT_LOG4J_CFG_FILENAME = "log4j2.xml"; // NON-NLS
	private static final String DEFAULT_STREAMS_CFG_FILENAME = "tnt-data-source.xml"; // NON-NLS

	private static final String SYSOUT_PREFIX = "==>>"; // NON-NLS
	private static final String UNKNOWN = "UNKNOWN"; // NON-NLS

	/**
	 * Map of this servlet bound streams states.
	 */
	protected final Map<String, StreamState> streamsStatesMap = new HashMap<>(3);
	/**
	 * Set of this servlet bound {@link com.jkoolcloud.tnt4j.streams.inputs.HttpServletStream} instances used to process
	 * servlet POST requests.
	 */
	protected final Set<HttpServletStream> httpServletStreams = new HashSet<>();

	/**
	 * The HTTP response template used by this servlet.
	 */
	protected HttpResponseTemplate responseTemplate;

	@Override
	public void init(ServletConfig config) throws ServletException {
		String cfgDirPath = config.getInitParameter(CFG_KEY_STREAM_CONFIG_DIR);

		String cfgFileTnt4j = getCfgProperty(config, CFG_KEY_TNT4J_CONFIG, DEFAULT_TNT4J_CFG_FILENAME);
		String cfgFileLog4j = getCfgProperty(config, CFG_KEY_LOG4J_CONFIG, DEFAULT_LOG4J_CFG_FILENAME);
		String cfgFileStreams = getCfgProperty(config, CFG_KEY_STREAMS_CONFIG, DEFAULT_STREAMS_CFG_FILENAME);

		if (StringUtils.isNotEmpty(cfgDirPath)) {
			File cfgDir = new File(cfgDirPath);
			if (cfgDir.exists()) {
				cfgFileTnt4j = getCfgFilePath(cfgDir, DEFAULT_TNT4J_CFG_FILENAME, cfgFileTnt4j);
				cfgFileLog4j = getCfgFilePath(cfgDir, DEFAULT_LOG4J_CFG_FILENAME, cfgFileLog4j);
				cfgFileStreams = getCfgFilePath(cfgDir, DEFAULT_STREAMS_CFG_FILENAME, cfgFileStreams);
			}
		}

		System.out.println(SYSOUT_PREFIX + StreamsResources.getStringFormatted(
				ServletStreamConstants.RESOURCE_BUNDLE_NAME, "TNT4JStreamsServlet.init.streams.cfg.dir", cfgDirPath));
		System.out.println(SYSOUT_PREFIX + StreamsResources.getStringFormatted(
				ServletStreamConstants.RESOURCE_BUNDLE_NAME, "TNT4JStreamsServlet.init.tnt4j.cfg.file", cfgFileTnt4j));
		System.out.println(SYSOUT_PREFIX + StreamsResources.getStringFormatted(
				ServletStreamConstants.RESOURCE_BUNDLE_NAME, "TNT4JStreamsServlet.init.log4j.cfg.file", cfgFileLog4j));
		System.out.println(
				SYSOUT_PREFIX + StreamsResources.getStringFormatted(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.init.streams.cfg.file", cfgFileStreams));

		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, cfgFileTnt4j);

		try (InputStream is = openStream(cfgFileLog4j)) {
			LoggerUtils.loadLog4j2Config(is);
		} catch (Exception exc) {
			throw new ServletException(StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
					"TNT4JStreamsServlet.init.fail.log4j"), exc);
		}

		InputStreamListener l = new InputStreamListener() {
			@Override
			public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
				StreamState streamState = getStreamState(stream.getName());
				streamState.progressStr = StreamsResources.getStringFormatted(
						ServletStreamConstants.RESOURCE_BUNDLE_NAME, "TNT4JStreamsServlet.StreamState.progress",
						stream.getName(), current, (total == -1 ? UNKNOWN : total));
			}

			@Override
			public void onSuccess(TNTInputStream<?, ?> stream) {
				StreamState streamState = getStreamState(stream.getName());
				streamState.completionStr = StreamsResources.getStringFormatted(
						ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.StreamState.completion.success", stream.getName());
			}

			@Override
			public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
				StreamState streamState = getStreamState(stream.getName());
				streamState.completionStr = StreamsResources.getStringFormatted(
						ServletStreamConstants.RESOURCE_BUNDLE_NAME, "TNT4JStreamsServlet.StreamState.completion.fail",
						stream.getName(), msg, code, exc);
			}

			@Override
			public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
				StreamState streamState = getStreamState(stream.getName());
				streamState.statusStr = StreamsResources.getStringFormatted(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.StreamState.status", stream.getName(), status.name());
			}

			@Override
			public void onFinish(TNTInputStream<?, ?> stream, TNTInputStreamStatistics stats) {
				StreamState streamState = getStreamState(stream.getName());
				streamState.finishStr = StreamsResources.getStringFormatted(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.StreamState.finish", stream.getName(),
						Utils.toString(TNTInputStreamStatistics.getMetrics().getMetrics()));
			}

			@Override
			public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
				StreamState streamState = getStreamState(stream.getName());
				streamState.eventStr = StreamsResources.getStringFormatted(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.StreamState.event", stream.getName(), level, message, source);
			}

			private StreamState getStreamState(String streamName) {
				StreamState streamState = streamsStatesMap.get(streamName);
				if (streamState == null) {
					streamState = new StreamState();
					streamsStatesMap.put(streamName, streamState);
				}

				return streamState;
			}
		};

		try (InputStream is = openStream(cfgFileStreams)) {
			StreamsBuilder streamsBuilder = new CfgStreamsBuilder().setConfig(is);
			StreamsConfigLoader cfgLoader = ((CfgStreamsBuilder) streamsBuilder).loadConfig(false, true);

			Collection<TNTInputStream<?, ?>> streams = streamsBuilder.getStreams();
			if (CollectionUtils.isNotEmpty(streams)) {
				for (TNTInputStream<?, ?> stream : streams) {
					if (stream instanceof HttpServletStream) {
						stream.addStreamListener(l);
						httpServletStreams.add((HttpServletStream) stream);
					}
				}
			} else {
				HttpServletStream httpServletStream = new HttpServletStream(
						TNT4JStreamsServlet.class.getSimpleName() + "Stream"); // NON-NLS
				httpServletStream.addParsers(cfgLoader.getParsers());
				httpServletStream.addStreamListener(l);
				httpServletStreams.add(httpServletStream);

				streamsBuilder = new POJOStreamsBuilder().addStream(httpServletStream);
			}

			StreamsAgent.runFromAPI(streamsBuilder);

			Iterator<HttpServletStream> httpServletStreamsItr = httpServletStreams.iterator();
			if (httpServletStreamsItr.hasNext()) {
				HttpServletStream httpServletStream = httpServletStreamsItr.next();
				responseTemplate = new GeneralResponseTemplate(
						(String) httpServletStream.getProperty(ServletStreamProperties.PROP_RESPONSE_TEMPLATE));
			} else {
				responseTemplate = new GeneralResponseTemplate(HttpServletStream.DEFAULT_RESPONSE_TEMPLATE);
			}
		} catch (Throwable exc) {
			throw new ServletException(StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
					"TNT4JStreamsServlet.init.fail.streams"), exc);
		}

		super.init(config);
	}

	/**
	 * Gets servlet configuration provided initialization parameter value.
	 *
	 * @param cfg
	 *            servlet configuration instance to get values from
	 * @param name
	 *            initialization parameter name
	 * @param defaultValue
	 *            default value to use if not defined by servlet configuration
	 * @return servlet configuration parameter value or provided default value if configuration does not define
	 *         initialization parameter
	 */
	protected String getCfgProperty(ServletConfig cfg, String name, String defaultValue) {
		String value = cfg.getInitParameter(name);
		return StringUtils.isEmpty(value) ? defaultValue : value;
	}

	/**
	 * Gets configuration file absolute path if one exists in provided configuration dir path. If it does not exist -
	 * original {@code cfgFilename} provided value is returned.
	 *
	 * @param cfgDir
	 *            streams configuration dir path
	 * @param dfCfgFilename
	 *            default configuration file name
	 * @param cfgFilename
	 *            configuration file name and path
	 * @return absolute configuration file path
	 */
	protected String getCfgFilePath(File cfgDir, String dfCfgFilename, String cfgFilename) {
		File cfgFile = new File(cfgDir, dfCfgFilename);
		if (cfgFile.exists() && cfgFilename.equals(dfCfgFilename)) {
			return cfgFile.getAbsolutePath();
		}

		return cfgFilename;
	}

	/**
	 * Opens input stream for provided resource: filename, path, URI.
	 *
	 * @param resource
	 *            resource descriptor: filename, path, URI
	 * @return opened input stream for provided resource, or {@code null} if stream can't be opened
	 * 
	 * @throws java.io.IOException
	 *             if provided resource can't be opened
	 */
	protected InputStream openStream(String resource) throws IOException {
		InputStream in = Utils.getResourceAsStream(resource);
		if (in == null) {
			try {
				in = Files.newInputStream(Paths.get(resource));
			} catch (FileNotFoundException | SecurityException e) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.input.open.fail.file"), e);
			}
		}
		if (in == null) {
			try {
				URI uri = URI.create(resource);
				URL url = uri.toURL();
				in = url.openStream();
			} catch (IllegalArgumentException | IOException e) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.input.open.fail.uri"), e);
			}
		}

		if (in == null) {
			throw new IOException(StreamsResources.getStringFormatted(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
					"TNT4JStreamsServlet.input.open.resource.fail", resource));
		}

		return in;
	}

	/**
	 * Produces HTTP response depicting current state of running streams.
	 * 
	 * @param req
	 *            HTTP request instance containing the request the client has made of the servlet
	 * @param resp
	 *            HTTP response instance containing the response the servlet sends to the client
	 * 
	 * @throws ServletException
	 *             if the request for the GET could not be handled
	 * @throws IOException
	 *             if an input or output error is detected when the servlet handles the GET request
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try (PrintWriter out = resp.getWriter()) {
			out.println("<!DOCTYPE html>"); // NON-NLS
			out.println("<html lang=\"en\">"); // NON-NLS
			out.println("<head>"); // NON-NLS
			out.println("<meta charset=\"UTF-8\">"); // NON-NLS
			out.println("<meta name=\"description\" content=\"TNT4J-Streams servlet status.\">"); // NON-NLS
			out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"); // NON-NLS
			out.println("<title>Streaming status</title>"); // NON-NLS
			out.println("</head>"); // NON-NLS
			out.println("<body>"); // NON-NLS
			if (streamsStatesMap.isEmpty()) {
				out.println("<b>" + StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
						"TNT4JStreamsServlet.stream.state.unknown") + "</b>");
			} else {
				for (Map.Entry<String, StreamState> state : streamsStatesMap.entrySet()) {
					out.println("<b>" + StreamsResources.getStringFormatted(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
							"TNT4JStreamsServlet.stream.state", state.getKey()) + "</b>");
					out.println("<pre>" + state.getValue().statusStr + "</pre>"); // NON-NLS
					out.println("<pre>" + state.getValue().progressStr + "</pre>"); // NON-NLS
					out.println("<pre>" + state.getValue().eventStr + "</pre>"); // NON-NLS
					out.println("<pre>" + state.getValue().completionStr + "</pre>"); // NON-NLS
					out.println("<pre>" + state.getValue().finishStr + "</pre>"); // NON-NLS
					out.println("<br>");
				}
			}
			out.println("</body>"); // NON-NLS
			out.println("</html>"); // NON-NLS

			out.flush();
		}
	}

	/**
	 * Consumes HTTP request as activity data. Builds activity data map containing values provided by HTTP request. Then
	 * this map is passed to bound {@link com.jkoolcloud.tnt4j.streams.inputs.HttpServletStream} instances. If no any
	 * stream is bound - 503 (SERVICE_UNAVAILABLE) response is sent to the client.
	 * <p>
	 * HTTP request data map may contain such entries:
	 * <ul>
	 * <li>{@value StreamsConstants#ACTIVITY_DATA_KEY} - raw activity data as {@code byte[]} retrieved from http
	 * request.</li>
	 * <li>HTTP form data parameters entries.</li>
	 * <li>Metadata - request metadata values map. See {@link #getMetadataMap(javax.servlet.http.HttpServletRequest)}
	 * for entries set.</li>
	 * <li>{@value StreamsConstants#HEADERS_KEY} - request header entries map.</li>
	 * <li>Attributes - request attribute entries map.</li>
	 * <li>{@value StreamsConstants#TRANSPORT_KEY} - request transport. Value is
	 * {@value StreamsConstants#TRANSPORT_HTTP}</li>
	 * </ul>
	 * <p>
	 * HTTP response may be one of:
	 * <ul>
	 * <li>OK (200) - request got successfully processed.</li>
	 * <li>SERVICE_UNAVAILABLE (503) - if servlet has no any HTTP requests processing stream bound</li>
	 * <li>NO_CONTENT (204) - if request has no any RAW or FORM parameters provided payload data</li>
	 * <li>INSUFFICIENT_STORAGE (507) - if servlet bound streams buffer got overflown.</li>
	 * <li>INTERNAL_SERVER_ERROR (500) - if any unhandled exception occurs.</li>
	 * </ul>
	 * 
	 * @param req
	 *            HTTP request instance containing the request the client has made of the servlet
	 * @param resp
	 *            HTTP response instance containing the response the servlet sends to the client
	 * 
	 * @throws ServletException
	 *             if the request for the POST could not be handled
	 * @throws IOException
	 *             if an input or output error is detected when the servlet handles the POST request
	 * 
	 * @see #getMetadataMap(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (httpServletStreams.isEmpty()) {
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			String msg = StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
					"TNT4JStreamsServlet.service.unavailable");
			setResponse(resp, req, msg, OpCompCode.ERROR, responseTemplate);
			LOGGER.log(OpLevel.WARNING, msg);
		}

		boolean activityAvailable = false;
		boolean added = false;

		Map<String, Object> reqMap = new HashMap<>();

		byte[] bytes = IOUtils.toByteArray(req.getInputStream());
		if (ArrayUtils.isNotEmpty(bytes)) {
			String reqContType = req.getContentType();
			ContentType contType = ContentType.parse(reqContType);
			Object entityData = bytes;
			if (contType != null && contType.getCharset() != null) {
				entityData = new String(bytes, contType.getCharset());
			}

			reqMap.put(StreamsConstants.ACTIVITY_DATA_KEY, entityData);
		}

		Enumeration<String> names = req.getParameterNames();
		if (names != null) {
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				reqMap.put(name, req.getParameterValues(name));
			}
		}

		if (!reqMap.isEmpty()) {
			activityAvailable = true;

			reqMap.put("Metadata", getMetadataMap(req)); // NON-NLS

			names = req.getHeaderNames();
			if (names != null) {
				Map<String, Object> headersMap = new HashMap<>();
				while (names.hasMoreElements()) {
					String name = names.nextElement();
					headersMap.put(name, req.getHeader(name));
				}
				reqMap.put(StreamsConstants.HEADERS_KEY, headersMap);
			}
			names = req.getAttributeNames();
			if (names != null) {
				Map<String, Object> attributesMap = new HashMap<>();
				while (names.hasMoreElements()) {
					String name = names.nextElement();
					attributesMap.put(name, req.getAttribute(name));
				}
				reqMap.put("Attributes", attributesMap); // NON-NLS
			}

			reqMap.put(StreamsConstants.TRANSPORT_KEY, StreamsConstants.TRANSPORT_HTTP);

			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
					"TNT4JStreamsServlet.passing.map.to.streams"), req.getRequestedSessionId());
			for (HttpServletStream httpServletStream : httpServletStreams) {
				added |= httpServletStream.addInputToBuffer(reqMap);
			}
		}

		if (!activityAvailable) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "HttpStream.no.activity");
			setResponse(resp, req, msg, OpCompCode.ERROR, responseTemplate);
			LOGGER.log(OpLevel.DEBUG, msg);
		} else if (!added) {
			resp.setStatus(HttpStatus.SC_INSUFFICIENT_STORAGE);
			String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"HttpStream.activities.buffer.size.limit");
			setResponse(resp, req, msg, OpCompCode.ERROR, responseTemplate);
			LOGGER.log(OpLevel.WARNING, msg);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
			String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "HttpStream.ok");
			setResponse(resp, req, msg, OpCompCode.SUCCESS, responseTemplate);
			LOGGER.log(OpLevel.DEBUG, msg);
		}
	}

	/**
	 * Produces HTTP request metadata map.
	 * <p>
	 * Metadata map may contain such HTTP request data entries:
	 * <ul>
	 * <li>Method - request method name</li>
	 * <li>Protocol - request protocol descriptor.</li>
	 * <li>AuthType - request authentication scheme type.</li>
	 * <li>RemoteAddr - request remote address.</li>
	 * <li>ContextPath - request context path.</li>
	 * <li>PathInfo - request path info.</li>
	 * <li>PathTranslated - request translated path.</li>
	 * <li>ServletPath - request servlet path.</li>
	 * <li>RequestURL - request URL string.</li>
	 * <li>RequestURI - request URI string.</li>
	 * <li>RequestedSessionId - request requested session id.</li>
	 * <li>RemoteUser - request remote user name.</li>
	 * <li>QueryString - request query string.</li>
	 * <li>Scheme - request scheme name.</li>
	 * <li>CharacterEncoding - request character encoding.</li>
	 * <li>ContentLength - request content length.</li>
	 * <li>ContentType - request content type.</li>
	 * <li>LocalAddr - request local address.</li>
	 * <li>LocalName - request local machine host name.</li>
	 * <li>LocalPort - request local port number.</li>
	 * <li>RemoteHost - request remote machine host name.</li>
	 * <li>RemotePort - request remote port number.</li>
	 * <li>ServerName - request server host name.</li>
	 * <li>getServerPort - request server port name.</li>
	 * <li>AsyncSupported - request flag indicating if asynchronous operation is supported.</li>
	 * <li>AsyncStarted - request flag indicating if asynchronous operation started.</li>
	 * <li>TrailerFieldsReady - request flag indicating if trailer fields are ready to read.</li>
	 * <li>RequestedSessionIdValid - request flag indicating if requested session ID is still valid.</li>
	 * <li>RequestedSessionIdFromURL - request flag indicating if requested session ID was conveyed to the server as
	 * part of the request URL</li>
	 * <li>RequestedSessionIdFromCookie - request flag indicating if requested session ID was conveyed to the server as
	 * an HTTP cookie</li>
	 * <li>Secure - request flag indicating if request was made using a secure channel, such as HTTPS.</li>
	 * </ul>
	 *
	 * @param req
	 *            HTTP request instance to collect metadata
	 * @return HTTP request collected metadata values map
	 */
	protected Map<String, Object> getMetadataMap(HttpServletRequest req) {
		Map<String, Object> metadataMap = new HashMap<>();

		metadataMap.put("Method", req.getMethod()); // NON-NLS
		metadataMap.put("Protocol", req.getProtocol()); // NON-NLS
		metadataMap.put("AuthType", req.getAuthType()); // NON-NLS
		metadataMap.put("RemoteAddr", req.getRemoteAddr()); // NON-NLS
		metadataMap.put("ContextPath", req.getContextPath()); // NON-NLS
		metadataMap.put("PathInfo", req.getPathInfo()); // NON-NLS
		metadataMap.put("PathTranslated", req.getPathTranslated()); // NON-NLS
		metadataMap.put("ServletPath", req.getServletPath()); // NON-NLS
		metadataMap.put("RequestURL", req.getRequestURL()); // NON-NLS
		metadataMap.put("RequestURI", req.getRequestURI()); // NON-NLS
		metadataMap.put("RequestedSessionId", req.getRequestedSessionId()); // NON-NLS
		metadataMap.put("RemoteUser", req.getRemoteUser()); // NON-NLS
		metadataMap.put("QueryString", req.getQueryString()); // NON-NLS
		metadataMap.put("Scheme", req.getScheme()); // NON-NLS

		metadataMap.put("CharacterEncoding", req.getCharacterEncoding()); // NON-NLS
		metadataMap.put("ContentLength", req.getContentLength()); // NON-NLS
		metadataMap.put("ContentType", req.getContentType()); // NON-NLS
		metadataMap.put("LocalAddr", req.getLocalAddr()); // NON-NLS
		metadataMap.put("LocalName", req.getLocalName()); // NON-NLS
		metadataMap.put("LocalPort", req.getLocalPort()); // NON-NLS
		metadataMap.put("RemoteHost", req.getRemoteHost()); // NON-NLS
		metadataMap.put("RemotePort", req.getRemotePort()); // NON-NLS
		metadataMap.put("ServerName", req.getServerName()); // NON-NLS
		metadataMap.put("getServerPort", req.getServerPort()); // NON-NLS

		metadataMap.put("AsyncSupported", req.isAsyncSupported()); // NON-NLS
		metadataMap.put("AsyncStarted", req.isAsyncStarted()); // NON-NLS
		metadataMap.put("TrailerFieldsReady", req.isTrailerFieldsReady()); // NON-NLS
		metadataMap.put("RequestedSessionIdValid", req.isRequestedSessionIdValid()); // NON-NLS
		metadataMap.put("RequestedSessionIdFromURL", req.isRequestedSessionIdFromURL()); // NON-NLS
		metadataMap.put("RequestedSessionIdFromCookie", req.isRequestedSessionIdFromCookie()); // NON-NLS
		metadataMap.put("Secure", req.isSecure()); // NON-NLS

		return metadataMap;
	}

	/**
	 * Writes response message string to provided HTTP response instance.
	 *
	 * @param resp
	 *            response instance to write to
	 * @param req
	 *            request instance to be used to fill response data
	 * @param msg
	 *            response message string to write
	 * @param msgType
	 *            response message type
	 * @param rt
	 *            response template to use
	 * @throws IOException
	 *             if response write fails
	 */
	protected void setResponse(HttpServletResponse resp, HttpServletRequest req, String msg, OpCompCode msgType,
			HttpResponseTemplate rt) throws IOException {
		Map<String, ?> headersMap = rt.fillHeaders(req);
		for (Map.Entry<String, ?> he : headersMap.entrySet()) {
			Object hValue = he.getValue();
			if (hValue instanceof Long) {
				resp.setDateHeader(he.getKey(), (long) hValue);
			} else if (hValue instanceof Integer) {
				resp.setIntHeader(he.getKey(), (int) hValue);
			} else {
				resp.setHeader(he.getKey(), Utils.toString(hValue));
			}
		}

		String responseStr = rt.fillBody(req, msg, msgType);

		resp.getWriter().write(responseStr);
		resp.getWriter().flush();
	}

	@Override
	public String getServletInfo() {
		return StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.servlet.info");
	}

	@Override
	public void destroy() {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.destroy.start"), getServletInfo());

		try {
			if (!httpServletStreams.isEmpty()) {
				for (HttpServletStream httpServletStream : httpServletStreams) {
					httpServletStream.markEnded();
				}
			}

			httpServletStreams.clear();
		} finally {
			StreamsAgent.waitForStreamsToComplete();

			super.destroy();
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.destroy.end"), getServletInfo());
	}

	/**
	 * This class provides stream state information data.
	 */
	static class StreamState {
		private String progressStr = StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.StreamState.progress.init");
		private String completionStr = StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.StreamState.completion.init");
		private String statusStr = StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.StreamState.status.init");
		private String finishStr = StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.StreamState.finish.init");
		private String eventStr = StreamsResources.getString(ServletStreamConstants.RESOURCE_BUNDLE_NAME,
				"TNT4JStreamsServlet.StreamState.event.init");
	}
}
