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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.ssl.SSLContexts;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a Http requests transmitted activity stream, where each request body is assumed to represent:
 * <ul>
 * <li>a single activity event sent as form data (parameter keys/values set)</li>
 * <li>a byte array as request payload data (e.g., log file contents)</li>
 * </ul>
 * <p>
 * Running this stream Http server is started on configuration defined port.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On message reception message data is packed
 * into {@link Map} filling these entries:
 * <ul>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from http request.</li>
 * <li>Form - HTTP request form parameters map.</li>
 * <li>ActivityTransport - activity transport definition: {@value StreamsConstants#TRANSPORT_HTTP}.</li>
 * <li>Headers - HTTP request headers map.</li>
 * <li>Line - HTTP request metadata values map: 'Method', 'Protocol', 'Uri', 'ReqUri', 'Authority', 'Path',
 * 'Scheme'</li>
 * <li>Entity - HTTP request entity metadata values map: 'Content-Length', 'Content-Encoding', 'Content-Type',
 * 'Chunked', 'Repeatable', 'Streaming'</li>
 * </ul>
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>Host - host name/IP to run Http server. (Optional)</li>
 * <li>Port - port number to run Http server. (Optional - default 8080 used if not defined)</li>
 * <li>UseSSL - flag indicating to use SSL. (Optional)</li>
 * <li>Keystore - keystore path. (Optional)</li>
 * <li>KeystorePass - keystore password. (Optional)</li>
 * <li>KeyPass - key password. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see ActivityMapParser
 */
public class HttpStream extends AbstractBufferedStream<Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(HttpStream.class);

	private static final String HTML_MSG_PATTERN = "<html><body><h1>{0}</h1></body></html>"; // NON-NLS

	private static final int DEFAULT_HTTP_PORT = 8080;
	private static final int DEFAULT_HTTPS_PORT = 8443;

	private static final long SOCKET_TIMEOUT = TimeUnit.SECONDS.toMillis(15);
	private static final boolean TCP_NO_DELAY = true;

	private String serverHost = null;
	private Integer serverPort = null;
	private boolean useSSL = false;
	private String keystore = null;
	private String keystorePass = null;
	private String keyPass = null;

	private HttpStreamRequestHandler requestHandler;

	/**
	 * Constructs an empty HttpStream. Requires configuration settings to set input stream source.
	 */
	public HttpStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			serverHost = value;
		} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			serverPort = Integer.valueOf(value);
		} else if (StreamProperties.PROP_USE_SSL.equalsIgnoreCase(name)) {
			useSSL = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			keystore = value;
		} else if (StreamProperties.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			keystorePass = decPassword(value);
		} else if (StreamProperties.PROP_KEY_PASS.equalsIgnoreCase(name)) {
			keyPass = decPassword(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return serverHost;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return serverPort;
		}
		if (StreamProperties.PROP_USE_SSL.equalsIgnoreCase(name)) {
			return useSSL;
		}
		if (StreamProperties.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			return keystore;
		}
		if (StreamProperties.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			return encPassword(keystorePass);
		}
		if (StreamProperties.PROP_KEY_PASS.equalsIgnoreCase(name)) {
			return encPassword(keyPass);
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		requestHandler = createHandler();
		requestHandler.initialize();
	}

	protected HttpStreamRequestHandler createHandler() throws Exception {
		return new HttpStreamRequestHandler();
	}

	@Override
	protected void start() throws Exception {
		super.start();

		requestHandler.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		if (requestHandler != null) {
			requestHandler.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return requestHandler.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(Map<String, ?> itemMap) {
		Object payload = itemMap.get(StreamsConstants.ACTIVITY_DATA_KEY);

		if (payload instanceof byte[]) {
			return ((byte[]) payload).length;
		} else if (payload instanceof String) {
			return ((String) payload).length();
		}

		return 0;
	}

	private static class HttpStreamExceptionLogger implements ExceptionListener {
		@Override
		public void onError(Exception ex) {
			if (ex instanceof SocketTimeoutException) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"HttpStream.connection.timed.out");
			} else if (ex instanceof ConnectionClosedException) {
				LOGGER.log(OpLevel.ERROR, Utils.getExceptionMessages(ex));
			} else if (ex instanceof SocketException && ex.getMessage().contains("closed")) { // NON-NLS
				LOGGER.log(OpLevel.WARNING, ex.getMessage());
			} else if (ex instanceof javax.net.ssl.SSLHandshakeException) {
				LOGGER.log(OpLevel.ERROR, Utils.getExceptionMessages(ex));
			} else {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"HttpStream.http.server.exception", ex);
			}
		}

		@Override
		public void onError(HttpConnection conn, Exception ex) {
			onError(ex);
		}
	}

	/**
	 * Runs {@link org.apache.hc.core5.http.impl.bootstrap.HttpServer} and handles server received
	 * {@link org.apache.hc.core5.http.HttpRequest}s.
	 */
	protected class HttpStreamRequestHandler extends InputProcessor implements HttpRequestHandler {

		private HttpServer server;

		/**
		 * Instantiates a new Http stream request handler.
		 */
		HttpStreamRequestHandler() {
			super("HttpStream.HttpStreamRequestHandler"); // NON-NLS
		}

		/**
		 * Input request handler initialization - HTTP server configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize request handler and configure HTTP server
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			SSLContext sslcontext = null;
			String host;
			int port;

			if (useSSL) {
				// Initialize SSL context
				URL url = new URL(keystore);
				sslcontext = SSLContexts.custom()
						.loadKeyMaterial(url, (keystorePass == null ? "" : keystorePass).toCharArray(),
								(keyPass == null ? "" : keyPass).toCharArray())
						.build();
				port = DEFAULT_HTTPS_PORT;
			} else {
				port = DEFAULT_HTTP_PORT;
			}

			host = serverHost;
			if (serverPort != null) {
				port = serverPort;
			}

			SocketConfig socketConfig = SocketConfig.custom().setSoTimeout((int) SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
					.setTcpNoDelay(TCP_NO_DELAY).build();
			server = ServerBootstrap.bootstrap().setListenerPort(port).setSocketConfig(socketConfig)
					.setSslContext(sslcontext).setExceptionListener(new HttpStreamExceptionLogger()).register("*", this)
					.registerVirtual(host, "*", this).create();
		}

		/**
		 * Starts HTTP server to receive incoming data. Shuts down this request handler if exception occurs.
		 */
		@Override
		public void run() {
			if (server != null) {
				try {
					server.start();
				} catch (IOException exc) {
					Utils.logThrowable(logger(), OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"AbstractBufferedStream.input.start.failed", exc);
					shutdown();
				}
			}
		}

		/**
		 * Closes running HTTP server.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		@Override
		void closeInternals() throws Exception {
			if (server != null) {
				// server.shutdown(5, TimeUnit.SECONDS);
				// server.awaitTermination ();
				server.stop();
				server = null;
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This method buffers a map structured content of next raw activity data item received over Http request
		 * handler. Buffered {@link Map} contains:
		 * <ul>
		 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#ACTIVITY_DATA_KEY} - request payload data
		 * (binary or string)</li>
		 * <li>{@code "Form"} - form parameters map, if request is of type
		 * {@code "application/x-www-form-urlencoded"}</li>
		 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TRANSPORT_KEY} - value
		 * {@value StreamsConstants#TRANSPORT_HTTP}</li>
		 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#HEADERS_KEY} - request headers map</li>
		 * <li>{@code "Line"} - request metadata values map: 'Method', 'Protocol', 'Uri', 'ReqUri', 'Authority', 'Path',
		 * 'Scheme'</li>
		 * <li>{@code "Entity"} - request entity metadata values map: 'Content-Length', 'Content-Encoding',
		 * 'Content-Type', 'Chunked', 'Repeatable', 'Streaming'</li>
		 * </ul>
		 */
		@Override
		public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
				throws HttpException, IOException {
			HttpEntity reqEntity = request.getEntity();
			boolean activityAvailable = false;
			boolean added = false;

			if (reqEntity != null) {
				try {
					Map<String, Object> reqMap = new HashMap<>();
					if (ContentType.parse(reqEntity.getContentType()).equals(ContentType.APPLICATION_FORM_URLENCODED)) {
						List<NameValuePair> reqParams = EntityUtils.parse(reqEntity);
						if (reqParams != null) {
							Map<String, Object> paramsMap = new HashMap<>();
							for (NameValuePair param : reqParams) {
								paramsMap.put(param.getName(), param.getValue());
							}
							reqMap.put("Form", paramsMap); // NON-NLS
						}
					} else {
						ContentType reqContType = ContentType.parse(reqEntity.getContentType());
						if (reqContType != null && reqContType.getCharset() == null) {
							ContentType defaultContType = ContentType.getByMimeType(reqContType.getMimeType());
							if (defaultContType != null) {
								reqContType = defaultContType;
							}
						}
						Object entityData;
						if (reqContType == null || reqContType.getCharset() == null) {
							entityData = EntityUtils.toByteArray(reqEntity);
						} else {
							entityData = EntityUtils.toString(reqEntity, reqContType.getCharset());
						}
						if (entityData != null) {
							reqMap.put(StreamsConstants.ACTIVITY_DATA_KEY, entityData);
						}
					}

					if (!reqMap.isEmpty()) {
						activityAvailable = true;

						collectRequestMetadata(request, reqEntity, reqMap);

						reqMap.put(StreamsConstants.TRANSPORT_KEY, StreamsConstants.TRANSPORT_HTTP);
						added = addInputToBuffer(reqMap);
					}
				} finally {
					EntityUtils.consumeQuietly(reqEntity);
					reqEntity.close();
				}
			}

			if (!activityAvailable) {
				response.setCode(HttpStatus.SC_NO_CONTENT);
				String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"HttpStream.no.activity");
				response.setEntity(createHtmlStringEntity(msg));
				logger().log(OpLevel.DEBUG, msg);
			} else if (!added) {
				response.setCode(HttpStatus.SC_INSUFFICIENT_STORAGE);
				String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"HttpStream.activities.buffer.size.limit");
				response.setEntity(createHtmlStringEntity(msg));
				logger().log(OpLevel.WARNING, msg);
			} else {
				response.setCode(HttpStatus.SC_OK);
				String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "HttpStream.ok");
				response.setEntity(createHtmlStringEntity(msg));
				logger().log(OpLevel.DEBUG, msg);
			}
		}

		private StringEntity createHtmlStringEntity(String msg) {
			StringEntity entity = new StringEntity(Utils.format(HTML_MSG_PATTERN, msg), ContentType.TEXT_HTML);

			return entity;
		}

		private void collectRequestMetadata(HttpRequest request, HttpEntity reqEntity, Map<String, Object> reqMap) {
			Map<String, Object> headersMap = new HashMap<>();
			Iterator<Header> hIterator = request.headerIterator();
			if (hIterator != null) {
				while (hIterator.hasNext()) {
					Header header = hIterator.next();
					headersMap.put(header.getName(), header.getValue());
				}
			}
			if (!headersMap.isEmpty()) {
				reqMap.put(StreamsConstants.HEADERS_KEY, headersMap);
			}

			Map<String, Object> lineMap = new HashMap<>();
			lineMap.put("Method", request.getMethod()); // NON-NLS
			lineMap.put("Protocol", request.getVersion().toString()); // NON-NLS
			try {
				lineMap.put("Uri", request.getUri().toString()); // NON-NLS
			} catch (Exception e) {
			}
			lineMap.put("ReqUri", request.getRequestUri()); // NON-NLS
			URIAuthority uriAuth = request.getAuthority();
			if (uriAuth != null) {
				lineMap.put("Authority", uriAuth.toString()); // NON-NLS
			}
			lineMap.put("Path", request.getPath()); // NON-NLS
			lineMap.put("Scheme", request.getScheme()); // NON-NLS
			reqMap.put("Line", lineMap); // NON-NLS

			Map<String, Object> entityMap = new HashMap<>();
			entityMap.put(HttpHeaders.CONTENT_LENGTH, reqEntity.getContentLength()); // NON-NLS
			entityMap.put(HttpHeaders.CONTENT_ENCODING, reqEntity.getContentEncoding());
			entityMap.put(HttpHeaders.CONTENT_TYPE, reqEntity.getContentType());
			entityMap.put("Chunked", reqEntity.isChunked()); // NON-NLS
			entityMap.put("Repeatable", reqEntity.isRepeatable()); // NON-NLS
			entityMap.put("Streaming", reqEntity.isStreaming()); // NON-NLS
			reqMap.put("Entity", entityMap); // NON-NLS
		}
	}
}