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

import java.io.FileNotFoundException;
import java.util.Map;

import org.logstash.beats.IMessageListener;
import org.logstash.beats.Message;
import org.logstash.beats.Server;
import org.logstash.netty.SslSimpleBuilder;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ElasticBeatsStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.BeatsStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

import io.netty.channel.ChannelHandlerContext;

/**
 * Implements Elastic Beats provided data (over Logstash messages) activity stream, where each message body is assumed
 * to represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data.
 * <p>
 * Running this stream Logstash server is started on configuration defined (host and) port to consume Elastic Beats
 * provided data as Logstash messages. So on Elastic Beats (messages producer) environment you have to configure output
 * to send data to this stream running Logstash server host and port.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>Host - host name to bind for stream started Logstash server. Default value - {@code "localhost"}. (Optional)</li>
 * <li>Port - port number to bind for stream started Logstash server. Default value - {@code 5044}. (Optional)</li>
 * <li>SSLCertificateFilePath - SSL certificate file path. (Optional)</li>
 * <li>SSLKeyFilePath - SSL key file path. (Optional)</li>
 * <li>PassPhrase - SSL key pass phrase. (Optional)</li>
 * <li>Timeout - connection timeout in seconds. Default value - {@code 30}. (Optional)</li>
 * <li>ThreadCount - number of threads used by Logstash server. Default value - {@code 1}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser
 */
public class ElasticBeatsStream extends AbstractBufferedStream<Map<?, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ElasticBeatsStream.class);

	private String host = "localhost"; // NON-NLS
	private int port = 5044;
	private String sslCertificateFilePath;
	private String sslKeyFilePath;
	private String passPhrase;
	private int timeout = 30;
	private int threadCount = 1;

	private LogstashInputProcessor processor;

	/**
	 * Constructs an empty ElasticBeatsStream. Requires configuration settings to set input stream source.
	 */
	public ElasticBeatsStream() {
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
			host = value;
		} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			port = Integer.parseInt(value);
		} else if (ElasticBeatsStreamProperties.PROP_SSL_CERTIFICATE_FILE_PATH.equalsIgnoreCase(name)) {
			sslCertificateFilePath = value;
		} else if (ElasticBeatsStreamProperties.PROP_SSL_KEY_FILE_PATH.equalsIgnoreCase(name)) {
			sslKeyFilePath = value;
		} else if (ElasticBeatsStreamProperties.PROP_PASSPHRASE.equalsIgnoreCase(name)) {
			passPhrase = decPassword(value);
		} else if (ElasticBeatsStreamProperties.PROP_TIMEOUT.equalsIgnoreCase(name)) {
			timeout = Integer.parseInt(value);
		} else if (ElasticBeatsStreamProperties.PROP_THREAD_COUNT.equalsIgnoreCase(name)) {
			threadCount = Integer.parseInt(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return port;
		}
		if (ElasticBeatsStreamProperties.PROP_SSL_CERTIFICATE_FILE_PATH.equalsIgnoreCase(name)) {
			return sslCertificateFilePath;
		}
		if (ElasticBeatsStreamProperties.PROP_SSL_KEY_FILE_PATH.equalsIgnoreCase(name)) {
			return sslKeyFilePath;
		}
		if (ElasticBeatsStreamProperties.PROP_PASSPHRASE.equalsIgnoreCase(name)) {
			return encPassword(passPhrase);
		}
		if (ElasticBeatsStreamProperties.PROP_TIMEOUT.equalsIgnoreCase(name)) {
			return timeout;
		}
		if (ElasticBeatsStreamProperties.PROP_THREAD_COUNT.equalsIgnoreCase(name)) {
			return threadCount;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		processor = new LogstashInputProcessor();
		processor.initialize();
	}

	@Override
	protected void start() throws Exception {
		super.start();

		processor.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		if (processor != null) {
			processor.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return processor.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(Map<?, ?> activityItem) {
		return 0; // TODO
	}

	/**
	 * Logstash messages receiver thread. It implements {@link org.logstash.beats.IMessageListener} interface and
	 * initiates server to receive and handle Elastic Beats provided data as Logstash messages data.
	 */
	private class LogstashInputProcessor extends InputProcessor implements IMessageListener {

		private Server server;

		private LogstashInputProcessor() {
			super("ElasticBeatsStream.LogstashInputProcessor"); // NON-NLS
		}

		/**
		 * Input data receiver initialization - Logstash server configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize message listener and configure Logstash server connection
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			server = new Server(host, port, timeout, threadCount);
			if (sslCertificateFilePath != null && sslKeyFilePath != null && passPhrase != null) {
				try {
					server.enableSSL(new SslSimpleBuilder(sslCertificateFilePath, sslKeyFilePath, passPhrase));
				} catch (FileNotFoundException e) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(BeatsStreamConstants.RESOURCE_BUNDLE_NAME),
							"ElasticBeatsStream.ssl.file.not.found.error", e);
				}
			}
			server.setMessageListener(this);
		}

		/**
		 * Starts Logstash to receive Elastic Beats incoming data. Shuts down this data receiver if exception occurs.
		 */
		@Override
		public void run() {
			if (server != null) {
				try {
					server.listen();
				} catch (Exception exc) {
					Utils.logThrowable(logger(), OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"AbstractBufferedStream.input.start.failed", exc);
					shutdown();
				}
			}
		}

		/**
		 * Closes Logstash server.
		 */
		@Override
		void closeInternals() throws Exception {
			if (server != null) {
				server.stop();
			}
		}

		@Override
		public void onNewMessage(ChannelHandlerContext ctx, Message message) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(BeatsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ElasticBeatsStream.message.received", ctx, message.getIdentityStream(), message.getSequence());
			addInputToBuffer(message.getData());
		}

		@Override
		public void onNewConnection(ChannelHandlerContext ctx) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(BeatsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ElasticBeatsStream.connection.opened", ctx);
		}

		@Override
		public void onConnectionClose(ChannelHandlerContext ctx) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(BeatsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ElasticBeatsStream.connection.closed", ctx);
		}

		@Override
		public void onException(ChannelHandlerContext ctx, Throwable cause) {
			logger().log(OpLevel.ERROR, StreamsResources.getBundle(BeatsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ElasticBeatsStream.exception.occurred", ctx, cause);
		}

		@Override
		public void onChannelInitializeException(ChannelHandlerContext ctx, Throwable cause) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(BeatsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ElasticBeatsStream.channel.initialization.error", ctx, cause);
		}
	}
}
