/*
 * Copyright 2014-2019 JKOOL, LLC.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQHeaderIterator;
import com.ibm.msg.client.commonservices.trace.Trace;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.WmqStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Base class for WebSphere MQ activity stream, where activity data containing {@link MQMessage} is read from the
 * specified WMQ Object (queue or topic) on the given (possibly remote) queue manager.
 * <p>
 * It currently does not strip off any WMQ headers, assuming that the message data only contains the actual input for
 * the configured parsers.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>QueueManager - Queue manager name. Default value - {@code null}. (Optional)</li>
 * <li>Queue - Queue name. (Required - at least one of 'Queue', 'Topic', 'Subscription', 'TopicString')</li>
 * <li>Topic - Topic name. (Required - at least one of 'Queue', 'Topic', 'Subscription', 'TopicString')</li>
 * <li>Subscription - Subscription name. (Required - at least one of 'Queue', 'Topic', 'Subscription',
 * 'TopicString')</li>
 * <li>TopicString - Topic string. (Required - at least one of 'Queue', 'Topic', 'Subscription', 'TopicString')</li>
 * <li>Host - WMQ connection host name. In addition supports WMQ connection format - HOST(PORT) and HOST:PORT. Also can
 * have multiple values delimited using {@code ","} symbol. Default value - {@code null}. (Optional)</li>
 * <li>Port - WMQ connection port number. Default value - {@code 1414}. (Optional)</li>
 * <li>UserName - WMQ user identifier. Default value - {@code null}. (Optional)</li>
 * <li>Password - WMQ user password. Default value - {@code null}. (Optional)</li>
 * <li>Channel - Server connection channel name. Default value - {@code "SYSTEM.DEF.SVRCONN"}. (Optional)</li>
 * <li>StripHeaders - identifies whether stream should strip WMQ message headers. Default value - {@code true}.
 * (Optional)</li>
 * <li>StreamReconnectDelay - delay in seconds between queue manager reconnection or failed queue GET iterations.
 * Default value - {@code 15sec}. (Optional)</li>
 * <li>OpenOptions - defines open options value used to access queue or topic. It can define numeric options value or
 * concatenation of MQ constant names/values delimited by {@code '|'} symbol. If options definition starts with
 * {@value #FORCE_OPEN_OPTION}, it means that this options set should be used as complete and passed to Queue Manager
 * without changes. By default these open options are appended to predefined set of: <br>
 * Predefined set of open options for queue:
 * <ul>
 * <li>MQOO_FAIL_IF_QUIESCING</li>
 * <li>MQOO_INPUT_AS_Q_DEF</li>
 * <li>MQOO_SAVE_ALL_CONTEXT</li>
 * <li>MQOO_INQUIRE</li>
 * </ul>
 * Predefined set of open options for topic:
 * <ul>
 * <li>MQSO_FAIL_IF_QUIESCING</li>
 * <li>MQSO_CREATE</li>
 * <li>MQSO_MANAGED - if subscription name is empty</li>
 * <li>MQSO_RESUME - if subscription name is defined</li>
 * </ul>
 * (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of RAW activity data retrieved from {@link MQMessage}
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractWmqStream<T> extends TNTParseableInputStream<T> {
	/**
	 * Limit on number of consecutive read failures. When limit is reached, we're going to assume that there is an issue
	 * with the queue manager, or some other unrecoverable condition, and therefore close and reopen the connection.
	 */
	protected static final int MAX_CONSECUTIVE_FAILURES = 5;

	/**
	 * Delay between queue manager connection retries, in milliseconds.
	 */
	protected static final long QMGR_CONN_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(15);

	/**
	 * Options definition sting token identifying these options as complete set to be set.
	 */
	protected static final String FORCE_OPEN_OPTION = "!"; // NON-NLS

	/**
	 * QM connections (hosts) delimiter symbol.
	 */
	protected static final String CONN_DELIM = ","; // NON-NLS
	/**
	 * QM connection host and port delimiter symbol.
	 */
	protected static final String PORT_DELIM = ":"; // NON-NLS
	/**
	 * QM connection port definition start symbol.
	 */
	protected static final String PORT_S = "("; // NON-NLS
	/**
	 * QM connection port definition end symbol.
	 */
	protected static final String PORT_E = ")"; // NON-NLS

	/**
	 * Key for message read failures counter.
	 */
	protected static final String READ_FAIL_COUNT_KEY = "ReadFailCount"; // NON-NLS
	/**
	 * Key for queue manager connection failures counter.
	 */
	protected static final String CONN_FAIL_COUNT_KEY = "ConnFailCount"; // NON-NLS

	/**
	 * Represents Queue Manager connected to
	 */
	protected MQQueueManager qmgr = null;

	/**
	 * Represents Object (queue/topic) to read activity data messages from
	 */
	protected MQDestination dest = null;

	/**
	 * Get options used for reading messages from specified object
	 */
	protected MQGetMessageOptions gmo = null;

	/**
	 * Registry map for failure counters. Currently two types of failures are supported out of the box: READ and
	 * CONNECT.
	 *
	 * @see #READ_FAIL_COUNT_KEY
	 * @see #CONN_FAIL_COUNT_KEY
	 * @see #MAX_CONSECUTIVE_FAILURES
	 */
	protected Map<String, AtomicInteger> failCountsMap = new HashMap<>(2);
	{
		failCountsMap.put(READ_FAIL_COUNT_KEY, new AtomicInteger());
		failCountsMap.put(CONN_FAIL_COUNT_KEY, new AtomicInteger());
	}

	// Stream properties
	private String qmgrName = null;
	private String queueName = null;
	private String topicName = null;
	private String subName = null;
	private String topicString = null;
	private boolean stripHeaders = true;
	private String connectionStr = null;
	// QM connection parameters
	private Hashtable<String, Object> mqConnProps = new Hashtable<>(6);

	private long reconnectDelay = QMGR_CONN_RETRY_INTERVAL;

	private int openOptions;
	private boolean forceOpenOptions;

	private MQMessage mqMsg;
	private Queue<Pair<String, Integer>> connections = new ArrayDeque<>(5);

	protected AbstractWmqStream() {
		mqConnProps.put(CMQC.PORT_PROPERTY, 1414);
		mqConnProps.put(CMQC.CHANNEL_PROPERTY, "SYSTEM.DEF.SVRCONN"); // NON-NLS
		mqConnProps.put(CMQC.CONNECT_OPTIONS_PROPERTY, CMQC.MQCNO_HANDLE_SHARE_NONE);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WmqStreamProperties.PROP_QMGR_NAME.equalsIgnoreCase(name)) {
			qmgrName = value;
		} else if (WmqStreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			queueName = value;
		} else if (WmqStreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			topicName = value;
		} else if (WmqStreamProperties.PROP_SUB_NAME.equalsIgnoreCase(name)) {
			subName = value;
		} else if (WmqStreamProperties.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			topicString = value;
		} else if (WmqStreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				mqConnProps.put(CMQC.HOST_NAME_PROPERTY, value);
			}
		} else if (WmqStreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				mqConnProps.put(CMQC.PORT_PROPERTY, Integer.decode(value));
			}
		} else if (WmqStreamProperties.PROP_CHANNEL_NAME.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				mqConnProps.put(CMQC.CHANNEL_PROPERTY, value);
			}
		} else if (WmqStreamProperties.PROP_STRIP_HEADERS.equalsIgnoreCase(name)) {
			stripHeaders = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				mqConnProps.put(CMQC.USER_ID_PROPERTY, value);
			}
		} else if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				mqConnProps.put(CMQC.PASSWORD_PROPERTY, decPassword(value));
			}
		} else if (StreamProperties.PROP_RECONNECT_DELAY.equalsIgnoreCase(name)) {
			reconnectDelay = Integer.decode(value);
		} else if (WmqStreamProperties.OPEN_OPTIONS.equalsIgnoreCase(name)) {
			openOptions = initOpenOptions(value);
		} else if (!StreamsConstants.isStreamCfgProperty(name, WmqStreamProperties.class)) {
			String[] mqcNameTokens = name.split("\\.");
			String mqcName = mqcNameTokens[mqcNameTokens.length - 1];
			Object mqcVal = MQConstants.getValue(mqcName);

			if (mqcVal == null) {
				mqcName = MQConstants.lookup(name, ".*");// NON-NLS
				if (StringUtils.isNotEmpty(mqcName)) {
					mqcVal = name;
				}
			}

			if (mqcVal != null) {
				Object cVal = Utils.getBoolean(value);
				if (cVal == null) {
					try {
						cVal = Integer.parseInt(value);
					} catch (Exception exc) {
					}

					if (cVal == null) {
						cVal = decPassword(value);
					}
				}

				mqConnProps.put(String.valueOf(mqcVal), cVal);
			}
		}
	}

	/**
	 * Initiates open options value used to access queue or topic. {@code optionsStr} can define numeric options value
	 * or concatenation of MQ constant names/values delimited by {@code '|'} symbol. If {@code optionsStr} starts with
	 * {@value #FORCE_OPEN_OPTION}, it means that this options set should be used as complete and passed to Queue
	 * Manager without changes. By default these open options are appended to predefined set of:
	 * <p>
	 * Predefined set of open options for queue:
	 * <ul>
	 * <li>MQOO_FAIL_IF_QUIESCING</li>
	 * <li>MQOO_INPUT_AS_Q_DEF</li>
	 * <li>MQOO_SAVE_ALL_CONTEXT</li>
	 * <li>MQOO_INQUIRE</li>
	 * </ul>
	 * <p>
	 * Predefined set of open options for topic:
	 * <ul>
	 * <li>MQSO_FAIL_IF_QUIESCING</li>
	 * <li>MQSO_CREATE</li>
	 * <li>MQSO_MANAGED - if subscription name is empty</li>
	 * <li>MQSO_RESUME - if subscription name is defined</li>
	 * </ul>
	 *
	 * @param optionsStr
	 *            open options definition string
	 * @return open options value
	 *
	 * @see #connectToQmgr()
	 */
	protected int initOpenOptions(String optionsStr) {
		int openOptions = 0;

		if (StringUtils.isEmpty(optionsStr)) {
			return openOptions;
		}

		optionsStr = optionsStr.trim();
		forceOpenOptions = optionsStr.startsWith(FORCE_OPEN_OPTION);
		if (forceOpenOptions) {
			optionsStr = optionsStr.substring(1);
		}
		if (StringUtils.isNumeric(optionsStr)) {
			openOptions = Integer.parseInt(optionsStr);
			return openOptions;
		}

		String[] options = Utils.splitValue(optionsStr);
		for (String option : options) {
			try {
				openOptions |= WmqUtils.getParamId(option.trim());
			} catch (NoSuchElementException e) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"WmqStream.error.option.resolve.failed", option);
			}
		}
		return openOptions;
	}

	/**
	 * Initiates QM connections. {@code connection} can define one or multiple QM connections delimited using
	 * {@code ","} symbol. Single connection definition can be: HOST, HOST(PORT) or HOST:PORT. When multiple HOST values
	 * are defined without dedicated port definition, port value is taken from
	 * {@link com.jkoolcloud.tnt4j.streams.configure.WmqStreamProperties#PROP_PORT} property definition.
	 * 
	 * @param connection
	 *            QM connection(s) definition string
	 * @return QM connections definition string, or {@code null} if {@code connection} defines single QM connection host
	 *         name
	 */
	protected String initConnections(String connection) {
		connections.clear();

		if (StringUtils.isNotEmpty(connection)) {
			String[] connStrs = connection.split(CONN_DELIM);

			for (String connStr : connStrs) {
				Pair<String, Integer> conn;
				if (connStr.contains(PORT_DELIM)) {
					String[] ct = connStr.split(PORT_DELIM);
					conn = new ImmutablePair<>(ct[0].trim(), Integer.decode(ct[1].trim()));
				} else if (connStr.contains(PORT_S)) {
					String[] ct = new String[2];

					int bi = 0;
					int ei = connStr.indexOf(PORT_S);
					ct[0] = connStr.substring(bi, ei);
					bi = ei + 1;
					ei = connStr.indexOf(PORT_E);
					if (ei == -1) {
						ei = connStr.length();
					}
					ct[1] = connStr.substring(bi, ei);

					conn = new ImmutablePair<>(ct[0].trim(), Integer.decode(ct[1].trim()));
				} else {
					conn = new ImmutablePair<>(connStr.trim(), (int) mqConnProps.get(CMQC.PORT_PROPERTY));
				}

				connections.add(conn);
			}
		}

		if (!connections.isEmpty()) {
			swapConnection();

			return connection;
		} else {
			return null;
		}
	}

	/**
	 * Swaps QM connection credentials (host and port) to next available connections list item.
	 */
	protected void swapConnection() {
		Pair<String, Integer> conn = connections.poll();
		if (conn != null) {
			connections.add(conn);

			mqConnProps.put(CMQC.HOST_NAME_PROPERTY, conn.getKey());
			mqConnProps.put(CMQC.PORT_PROPERTY, conn.getValue());
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WmqStreamProperties.PROP_QMGR_NAME.equalsIgnoreCase(name)) {
			return qmgrName;
		}
		if (WmqStreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return queueName;
		}
		if (WmqStreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (WmqStreamProperties.PROP_SUB_NAME.equalsIgnoreCase(name)) {
			return subName;
		}
		if (WmqStreamProperties.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			return topicString;
		}
		if (WmqStreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return connectionStr == null ? mqConnProps.get(CMQC.HOST_NAME_PROPERTY) : connectionStr;
		}
		if (WmqStreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return mqConnProps.get(CMQC.PORT_PROPERTY);
		}
		if (WmqStreamProperties.PROP_CHANNEL_NAME.equalsIgnoreCase(name)) {
			return mqConnProps.get(CMQC.CHANNEL_PROPERTY);
		}
		if (WmqStreamProperties.PROP_STRIP_HEADERS.equalsIgnoreCase(name)) {
			return stripHeaders;
		}
		if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			return mqConnProps.get(CMQC.USER_ID_PROPERTY);
		}
		if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			return encPassword((String) mqConnProps.get(CMQC.PASSWORD_PROPERTY));
		}
		if (StreamProperties.PROP_RECONNECT_DELAY.equalsIgnoreCase(name)) {
			return reconnectDelay;
		}
		if (WmqStreamProperties.OPEN_OPTIONS.equalsIgnoreCase(name)) {
			return openOptions;
		}

		return super.getProperty(name);
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(queueName) && StringUtils.isEmpty(topicString) && StringUtils.isEmpty(topicName)
				&& StringUtils.isEmpty(subName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqStream.must.specify.one", StreamProperties.PROP_QUEUE_NAME, StreamProperties.PROP_TOPIC_NAME,
					StreamProperties.PROP_TOPIC_STRING, WmqStreamProperties.PROP_SUB_NAME));
		}

		connectionStr = initConnections((String) mqConnProps.get(CMQC.HOST_NAME_PROPERTY));

		// Prevents WMQ library from writing exceptions to stderr
		MQException.log = null;
		gmo = new MQGetMessageOptions();
		gmo.waitInterval = CMQC.MQWI_UNLIMITED;
		gmo.options &= ~CMQC.MQGMO_NO_SYNCPOINT;
		gmo.options |= CMQC.MQGMO_SYNCPOINT | CMQC.MQGMO_WAIT;
	}

	/**
	 * Interrupts owner thread to interrupt sleep between QM reconnect attempts and closes target {@link #dest} if
	 * opened.
	 *
	 * @see #closeDestination()
	 */
	@Override
	protected void stopInternals() {
		// Prevents generating FDC trace files for waiting MQGET interrupt.
		traceOff(true);

		if (isOwned()) {
			getOwnerThread().interrupt();
		}

		closeDestination();

		// Restore WMQ tracing.
		traceOff(false);
	}

	private void traceOff(boolean off) {
		try {
			Field f = Trace.class.getDeclaredField("ffstSuppressionProbeIDs");
			f.setAccessible(true);
			Object obj = f.get(null);

			Method m = obj.getClass().getMethod(off ? "add" : "remove", Object.class);
			m.invoke(obj, "01"); // NON-NLS
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.DEBUG,
					StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"traceOff(boolean) failed: {0}", exc); // NON-NLS
		}
	}

	/**
	 * Checks if connection to queue manager is opened.
	 *
	 * @param mqe
	 *            MQ exception object
	 * @return flag indicating if connected to queue manager
	 */
	protected boolean isConnectedToQmgr(MQException mqe) {
		if (qmgr == null || !qmgr.isConnected()) {
			return false;
		}
		if (mqe != null && mqe.getCompCode() == MQConstants.MQCC_FAILED) {
			switch (mqe.getReason()) {
			case MQConstants.MQRC_CONNECTION_BROKEN:
			case MQConstants.MQRC_CONNECTION_ERROR:
			case MQConstants.MQRC_Q_MGR_NOT_ACTIVE:
			case MQConstants.MQRC_Q_MGR_NOT_AVAILABLE:
			case MQConstants.MQRC_Q_MGR_QUIESCING:
			case MQConstants.MQRC_Q_MGR_STOPPING:
			case MQConstants.MQRC_CONNECTION_QUIESCING:
			case MQConstants.MQRC_CONNECTION_STOPPING:
				return false;
			default:
				break;
			}
		}
		return true;
	}

	/**
	 * Establish connection to queue manager and open necessary objects for retrieving messages
	 *
	 * @throws Exception
	 *             if exception occurs connecting to queue manager or opening required objects
	 *
	 * @see #initOpenOptions(String)
	 */
	protected void connectToQmgr() throws Exception {
		qmgr = null;
		dest = null;

		if (StringUtils.isEmpty(qmgrName)) {
			logger().log(OpLevel.INFO, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqStream.connecting.default", mqConnProps);
		} else {
			logger().log(OpLevel.INFO, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqStream.connecting.qm", qmgrName, mqConnProps);
		}
		qmgr = new MQQueueManager(qmgrName, mqConnProps);
		if (StringUtils.isNotEmpty(topicString) || StringUtils.isNotEmpty(topicName)
				|| StringUtils.isNotEmpty(subName)) {
			if (!forceOpenOptions) {
				openOptions |= CMQC.MQSO_FAIL_IF_QUIESCING | CMQC.MQSO_CREATE
						| (StringUtils.isEmpty(subName) ? CMQC.MQSO_MANAGED : CMQC.MQSO_RESUME);
			}
			if (StringUtils.isNotEmpty(subName)) {
				logger().log(OpLevel.INFO, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"WmqStream.subscribing.to.topic1", topicString, topicName, subName,
						String.format("%08X", openOptions), MQConstants.decodeOptions(openOptions, "MQSO_.*")); // NON-NLS
				dest = qmgr.accessTopic(topicString, topicName, openOptions, null, subName);
			} else {
				logger().log(OpLevel.INFO, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"WmqStream.subscribing.to.topic2", topicString, topicName, String.format("%08X", openOptions), // NON-NLS
						MQConstants.decodeOptions(openOptions, "MQSO_.*")); // NON-NLS
				dest = qmgr.accessTopic(topicString, topicName, CMQC.MQTOPIC_OPEN_AS_SUBSCRIPTION, openOptions);
			}
		} else {
			if (!forceOpenOptions) {
				openOptions |= CMQC.MQOO_FAIL_IF_QUIESCING | CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_SAVE_ALL_CONTEXT
						| CMQC.MQOO_INQUIRE;
			}
			logger().log(OpLevel.INFO, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqStream.opening.queue", qmgrName, String.format("%08X", openOptions), // NON-NLS
					MQConstants.decodeOptions(openOptions, "MQOO_.*")); // NON-NLS
			dest = qmgr.accessQueue(queueName, openOptions);
		}
		logger().log(OpLevel.INFO, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStream.reading.from", dest.getName().trim(), String.format("%08X", gmo.options), // NON-NLS
				MQConstants.decodeOptions(gmo.options, "MQGMO_.*")); // NON-NLS
		resetFailCounts();
	}

	/**
	 * Resets all registered failure counters.
	 */
	protected void resetFailCounts() {
		resetFailCount(READ_FAIL_COUNT_KEY);
		resetFailCount(CONN_FAIL_COUNT_KEY);
	}

	/**
	 * Resets count of defined failures counter to {@code 0}.
	 * 
	 * @param fKey
	 *            failure counter key
	 */
	protected void resetFailCount(String fKey) {
		failCountsMap.get(fKey).set(0);
	}

	/**
	 * Increments count of defined failures counter by {@code 1}.
	 * 
	 * @param fKey
	 *            failure counter key
	 * @return updated failures count value
	 */
	protected int incrementFailCount(String fKey) {
		return failCountsMap.get(fKey).incrementAndGet();
	}

	/**
	 * Checks if failures counter reached limit of consecutive failures.
	 * 
	 * @param fKey
	 *            failure counter key
	 * @param max
	 *            maximum number of consecutive failures
	 * @return {@code true} if current failures count is greater or equals maximum number of consecutive failures,
	 *         {@code false} - otherwise
	 */
	protected boolean isFailLimitReached(String fKey, int max) {
		return failCountsMap.get(fKey).get() >= max;
	}

	@Override
	public T getNextItem() throws Exception {
		while (true) {
			while (!isHalted() && !isConnectedToQmgr(null)) {
				try {
					connectToQmgr();
				} catch (MQException mqe) {
					if (isConnectedToQmgr(mqe)) {
						// connection to qmgr was successful, so we were not able to open/subscribe to required
						// queue/topic, so exit
						logger().log(OpLevel.ERROR, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
								"WmqStream.failed.opening", formatMqException(mqe));
						return null;
					}

					incrementFailCount(CONN_FAIL_COUNT_KEY);
					logger().log(OpLevel.ERROR, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
							"WmqStream.failed.to.connect", formatMqException(mqe));
					if (!isHalted()) {
						if (isFailLimitReached(CONN_FAIL_COUNT_KEY, connections.size())) {
							logger().log(OpLevel.WARNING,
									StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
									"WmqStream.reached.conn.limit", connections.size());
							sleep(reconnectDelay);
							resetFailCount(CONN_FAIL_COUNT_KEY);
						}

						if (CollectionUtils.size(connections) > 1) {
							swapConnection();
						}
					}
				}
			}

			if (isHalted() || !isConnectedToQmgr(null)) {
				// stream is halted or not connected to qmgr, so exit
				return null;
			}

			try {
				MQMessage mqMsg = new MQMessage();
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"WmqStream.waiting.for.message", dest.getName().trim());
				dest.get(mqMsg, gmo);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"WmqStream.read.msg", dest.getName().trim(), mqMsg.getMessageLength());
				// TODO: MQCFH mqcfh = new MQCFH(mqMsg); mqcfh.control != MQConstants.MQCFC_LAST;
				if (stripHeaders) {
					MQHeaderIterator hdrIt = new MQHeaderIterator(mqMsg);
					hdrIt.skipHeaders();
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
							"WmqStream.stripped.wmq");
				}
				T msgData = getActivityDataFromMessage(mqMsg);
				qmgr.commit();
				resetFailCount(READ_FAIL_COUNT_KEY);
				addStreamedBytesCount(mqMsg.getMessageLength());
				// logger().log(OpLevel.DEBUG, "QUEUE {0} DEPTH: {1}", queueName, ((MQQueue) dest).getCurrentDepth());
				return msgData;
			} catch (MQException mqe) {
				if (isHalted() && mqe.getReason() == CMQC.MQRC_UNEXPECTED_ERROR) {
					// stream is halted and most likely dest.get(MQMessage) was interrupted by stream stop method
					// invoking dest.close()
					return null;
				}

				incrementFailCount(READ_FAIL_COUNT_KEY);
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"WmqStream.failed.reading", dest.getName().trim(), formatMqException(mqe));
				boolean throwException = true;
				if (isFailLimitReached(READ_FAIL_COUNT_KEY, MAX_CONSECUTIVE_FAILURES)) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
							"WmqStream.reached.limit", MAX_CONSECUTIVE_FAILURES);
					closeQmgrConnection();
					resetFailCount(READ_FAIL_COUNT_KEY);
				} else {
					if (!isHalted()) {
						switch (mqe.getReason()) {
						case CMQC.MQRC_GET_INHIBITED:
							sleep(reconnectDelay);
							throwException = false;
							break;
						default:
							break;
						}
					}
				}

				if (throwException) {
					throw mqe;
				}
			}
		}
	}

	/**
	 * Gets RAW activity data from provided {@link MQMessage}.
	 *
	 * @param message
	 *            MQ message to get RAW activity data
	 * @return RAW activity data retrieved from MQ message
	 * @throws Exception
	 *             if any errors occurred getting RAW activity data from MQ message
	 */
	protected abstract T getActivityDataFromMessage(MQMessage message) throws Exception;

	/**
	 * Closes open objects and disconnects from queue manager.
	 *
	 * @see #closeDestination()
	 * @see #disconnectQM()
	 */
	protected void closeQmgrConnection() {
		closeDestination();
		disconnectQM();
	}

	/**
	 * Closes opened MQ objects used for retrieving messages.
	 */
	protected void closeDestination() {
		if (dest != null) {
			try {
				dest.close();
			} catch (MQException mqe) {
				try {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
							"WmqStream.error.closing", dest.getClass().getName(), dest.getName(),
							formatMqException(mqe));
				} catch (MQException e) {
				}
			}
			dest = null;
		}
	}

	/**
	 * Disconnects from queue manager if connection is opened.
	 */
	protected void disconnectQM() {
		if (qmgr != null) {
			try {
				qmgr.disconnect();
			} catch (MQException mqe) {
				try {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
							"WmqStream.error.closing.qmgr", qmgr.getName(), formatMqException(mqe));
				} catch (MQException e) {
				}
			}
			qmgr = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Closes open objects and disconnects from queue manager.
	 */
	@Override
	protected void cleanup() {
		closeQmgrConnection();

		super.cleanup();
	}

	/**
	 * Formats display string for WMQ Exceptions.
	 * <p>
	 * This implementation appends the {@code MQRC_} label for the reason code.
	 *
	 * @param mqe
	 *            WMQ exception
	 * @return string identifying exception, including {@code MQRC_} constant label
	 */
	protected static String formatMqException(MQException mqe) {
		return String.format("%s (%s)", mqe, MQConstants.lookupReasonCode(mqe.getReason())); // NON-NLS
	}
}
