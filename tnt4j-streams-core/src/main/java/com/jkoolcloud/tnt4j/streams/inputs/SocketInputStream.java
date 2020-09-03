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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a TCP {@link java.net.Socket} transmitted activity stream, where each data package transmission is assumed
 * to represent a binary data (byte array). Running this stream {@link java.net.ServerSocket} is started on
 * configuration defined port.
 * <p>
 * This activity stream requires parsers that can support {@code byte[]} data, or use pre-parser to convert binary data
 * to parser supported format.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>Port - port number to run server socket. (Optional - default 12569 used if not defined)</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class SocketInputStream extends AbstractBufferedStream<byte[]> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SocketInputStream.class);

	private static final int DEFAULT_PORT = 12569;

	private int serverPort = DEFAULT_PORT;

	private SocketInputProcessor dataTransmitProcessor;

	/**
	 * Constructs an empty SocketInputStream. Requires configuration settings to set input stream source.
	 */
	public SocketInputStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			serverPort = Integer.valueOf(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return serverPort;
		}
		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		dataTransmitProcessor = new SocketInputProcessor(serverPort);
		dataTransmitProcessor.initialize();
	}

	@Override
	protected void start() throws Exception {
		super.start();

		dataTransmitProcessor.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		if (dataTransmitProcessor != null) {
			dataTransmitProcessor.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return dataTransmitProcessor.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(byte[] itemBytes) {
		return itemBytes == null ? 0 : itemBytes.length;
	}

	/**
	 * Runs {@link java.net.ServerSocket} instance, accepts incoming connections and puts {@link Socket} transmitted
	 * binary data packages into buffer.
	 */
	protected class SocketInputProcessor extends InputProcessor {
		private ServerSocket srvSocket = null;
		private final List<SocketInputHandler> socketHandlersList = new ArrayList<>();

		private int socketPort;

		/**
		 * Instantiates a new Socket stream input processor.
		 */
		private SocketInputProcessor(int socketPort) {
			super("SocketInputStream.SocketInputProcessor"); // NON-NLS

			this.socketPort = socketPort;
		}

		@Override
		protected void initialize(Object... params) throws Exception {
			srvSocket = new ServerSocket(socketPort);
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"SocketInputStream.waiting.for.connection", socketPort);
		}

		/**
		 * Accepts incoming connections and initiates input processor for opened connection {@link java.net.Socket}
		 * {@link java.io.InputStream} transmitted data.
		 */
		@Override
		public void run() {
			while (!isStopRunning() && !srvSocket.isClosed()) {
				SocketInputHandler socketInputHandler = null;
				try {
					Socket socket = srvSocket.accept();
					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"SocketInputStream.accepted.connection", socket);
					synchronized (socketHandlersList) {
						socketInputHandler = new SocketInputHandler(socket);
						socketHandlersList.add(socketInputHandler);
						socketInputHandler.start();
					}
				} catch (Throwable exc) {
					Utils.logThrowable(logger(), OpLevel.INFO,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"SocketInputStream.failed.connection", exc);

					Utils.close(socketInputHandler);
				}
			}
		}

		@Override
		void closeInternals() throws Exception {
			if (srvSocket != null) {
				Utils.close(srvSocket);
				srvSocket = null;
			}

			synchronized (socketHandlersList) {
				for (SocketInputHandler socketHandler : socketHandlersList) {
					Utils.close(socketHandler);
				}
			}
		}

		/**
		 * Reads {@link Socket} transmitted data and puts it to the input buffer.
		 */
		protected class SocketInputHandler extends StreamsThread implements Closeable {
			private Socket socket;
			private BufferedInputStream socketInputStream;

			/**
			 * Instantiates a new Socket stream input handler.
			 * 
			 * @param socket
			 *            socket instance to acquire transmitted data
			 * @throws IOException
			 *             if fails to get socket input stream
			 */
			SocketInputHandler(Socket socket) throws IOException {
				super("socket-input-handler-" + socket.getLocalPort()); // NON-NLS

				this.socket = socket;
				socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(30));
				this.socketInputStream = new BufferedInputStream(socket.getInputStream());
			}

			/**
			 * Reads transmitted data bytes from {@link Socket} {@link java.io.InputStream} and adds to a input buffer.
			 */
			@Override
			public void run() {
				while (!isStopRunning() && (!socket.isInputShutdown() || !socket.isClosed())) {
					try {
						byte[] tBytes = IOUtils.toByteArray(socketInputStream);
						addInputToBuffer(tBytes);
						halt(true);
					} catch (Throwable exc) {
						Utils.logThrowable(logger(), OpLevel.ERROR,
								StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"SocketInputStream.failed.read", socket, exc);
					}
				}

				close();
			}

			@Override
			public void close() {
				Utils.close(socketInputStream);

				if (socket != null) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"SocketInputStream.closing.connection", socket);

					Utils.close(socket);
					socket = null;
				}

				synchronized (socketHandlersList) {
					socketHandlersList.remove(this);
				}
			}
		}
	}
}
