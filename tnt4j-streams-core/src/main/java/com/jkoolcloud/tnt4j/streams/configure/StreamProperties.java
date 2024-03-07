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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * Lists predefined property names used by TNT4-Streams input streams.
 *
 * @version $Revision: 1 $
 */
public interface StreamProperties extends OutputProperties {

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_DATETIME = "DateTime"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_STREAM_NAME = "StreamName"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_FILENAME = "FileName"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_HOST = "Host"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_PORT = "Port"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_QUEUE_NAME = "Queue"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TOPIC_NAME = "Topic"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TOPIC_STRING = "TopicString"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_START_FROM_LATEST = "StartFromLatest"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_FILE_READ_DELAY = "FileReadDelay"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_HALT_ON_PARSER = "HaltIfNoParser"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_USE_EXECUTOR_SERVICE = "UseExecutors"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_EXECUTOR_THREADS_QTY = "ExecutorThreadsQuantity"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_EXECUTOR_QUEUE_DEPTH = "ExecutorQueueDepth"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property. Value in seconds.
	 */
	String PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT = "ExecutorRejectedTaskOfferTimeout"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_EXECUTORS_TERMINATION_TIMEOUT = "ExecutorsTerminationTimeout"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_EXECUTORS_BOUNDED = "ExecutorsBoundedModel"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_EXECUTORS_IMMEDIATE_SHUTDOWN = "ExecutorsImmediateShutdown"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_KEYSTORE = "Keystore"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_KEYSTORE_PASS = "KeystorePass"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_KEY_PASS = "KeyPass"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_JNDI_FACTORY = "JNDIFactory"; // NON-NLS //used only by JMS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_SERVER_URI = "ServerURI"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_USERNAME = "UserName"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_PASSWORD = "Password"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_USE_SSL = "UseSSL"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RESTART_ON_CLOSE = "RestartOnInputClose"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_ARCH_TYPE = "ArchType"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_BUFFER_SIZE = "BufferSize"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_FULL_BUFFER_ADD_POLICY = "FullBufferAddPolicy"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_FILE_POLLING = "FilePolling"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RESTORE_STATE = "RestoreState"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_START_SERVER = "StartServer"; // NON-NLS //used only by Kafka

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_INPUT_CLOSEABLE = "InputCloseable"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RANGE_TO_STREAM = "RangeToStream"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_RECONNECT_DELAY = "StreamReconnectDelay"; // NON-NLS

	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	String PROP_GROUPING_ACTIVITY_NAME = "GroupingActivityName"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_ACTIVITY_DELIM = "ActivityDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_KEEP_LINE_SEPARATORS = "KeepLineSeparators"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_PING_LOG_ACTIVITY_COUNT = "PingLogActivityCount"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_PING_LOG_ACTIVITY_DELAY = "PingLogActivityDelay"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_TRUNCATED_FILE_POLICY = "TruncatedFilePolicy"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	String PROP_CHARSET = "Charset"; // NON-NLS
}
