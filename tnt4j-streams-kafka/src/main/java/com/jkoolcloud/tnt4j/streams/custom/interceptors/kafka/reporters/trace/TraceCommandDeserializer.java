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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.trace;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.TimestampFormatter;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Topic messages tracing command messages deserializer. Command messages are received from dedicated Kafka topic.
 * <p>
 * It builds {@link TraceCommandDeserializer.TopicTraceCommand} used to handle topic messages tracing. Command defines
 * if messages tracing:
 * <ul>
 * <li>is turned on - {@code active}, string value after "trace" command message token. Activity value is "ON" and
 * "OFF"</li>
 * <li>is bound to particular topic name - {@code topic}, string value in front of "topic" command message token</li>
 * <li>has to trace particular count of messages - {@code count}, integer value in front of "messages" command message
 * token</li>
 * <li>has to start from particular time - {@code startFrom}, date/time value in front of "between" command message
 * token</li>
 * <li>has to end at particular time - {@code endAt}, date/time value after "between" or "until" command message
 * tokens</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class TraceCommandDeserializer implements Deserializer<TraceCommandDeserializer.TopicTraceCommand> {
	/**
	 * The constant for a command message token defining command applicable to all topics
	 */
	public static final String MASTER_CONFIG = "All topics"; // NON-NLS
	/**
	 * The constant for a command message token defining traced messages topic name.
	 */
	public static final String TOPIC = "Topic"; // NON-NLS
	/**
	 * The constant for a command message used date/time pattern.
	 */
	public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm"; // NON-NLS
	/**
	 * The constant for a command message token defining trace schedule start and end values.
	 */
	public static final String BETWEEN = "between"; // NON-NLS
	/**
	 * The constant for a command message token defining trace schedule end value starting tracing from now.
	 */
	public static final String UNTIL = "until"; // NON-NLS
	/**
	 * The constant for a command message token defining traced messages count.
	 */
	public static final String MESSAGES = "messages"; // NON-NLS
	/**
	 * The constant for a command message token defining tracing state.
	 */
	public static final String TRACE = "trace"; // NON-NLS
	/**
	 * The constant for a command message token defining tracing ON state value.
	 */
	public static final String ON = "on"; // NON-NLS
	/**
	 * The constant for a command message token defining tracing OFF state value.
	 */
	public static final String OFF = "off"; // NON-NLS

	private StringDeserializer cmdDeserializer = new StringDeserializer();

	/**
	 * Parses trace command {@code message} and builds command instance handling topic messages tracing activity.
	 *
	 * @param message
	 *            command message to interpret
	 * @return topic trace command built from message, or {@code null} if build fails
	 * @throws Exception
	 *             if command message can't be parsed
	 */
	public static TopicTraceCommand interpret(String message) throws Exception {
		String args[] = message.split(" ");
		TopicTraceCommand command = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (StringUtils.isEmpty(arg)) {
				continue;
			}
			if (arg.equalsIgnoreCase(TRACE)) {
				command = new TopicTraceCommand();
			}

			if (arg.equalsIgnoreCase(OFF)) {
				command.active = false;
			}

			if (StringUtils.isNumeric(arg) && args[i + 1].equalsIgnoreCase(MESSAGES)) {
				command.count = Integer.parseInt(arg);
			}

			if (arg.equalsIgnoreCase(UNTIL)) {
				TimestampFormatter formatter = new TimestampFormatter(DATE_PATTERN, null, null);
				String dateTime = args[++i] + ' ' + args[++i];
				UsecTimestamp timestamp = formatter.parse(dateTime);
				command.endAt = timestamp.getTimeMillis();
			}

			if (arg.equalsIgnoreCase(BETWEEN)) {
				TimestampFormatter formatter = new TimestampFormatter(DATE_PATTERN, null, null);
				String dateTime = args[++i] + ' ' + args[++i];
				UsecTimestamp begin = formatter.parse(dateTime);
				dateTime = args[++i] + ' ' + args[++i];
				UsecTimestamp end = formatter.parse(dateTime);
				command.startFrom = begin.getTimeMillis();
				command.endAt = end.getTimeMillis();
			}

			if (arg.equalsIgnoreCase(TOPIC)) {
				command.topic = args[++i];
			}
		}

		return command;
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		cmdDeserializer.configure(configs, isKey);
	}

	@Override
	public TopicTraceCommand deserialize(String s, byte[] bytes) {
		String cmdStr = cmdDeserializer.deserialize(s, bytes);
		try {
			return interpret(cmdStr);
		} catch (Exception e) {
			throw new SerializationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TraceCommandDeserializer.cmd.parse.failed", Utils.getExceptionMessages(e)));
		}
	}

	@Override
	public void close() {
	}

	/**
	 * Topic messages trace handling command.
	 */
	public static class TopicTraceCommand {
		/**
		 * Flag indicating if tracing is active.
		 */
		boolean active = true;
		/**
		 * Topic name to trace messages.
		 */
		public String topic = MASTER_CONFIG;
		/**
		 * Trace iteration count.
		 */
		Integer count;
		/**
		 * Tracing start timestamp value.
		 */
		Long startFrom;
		/**
		 * Tracing termination timestamp value.
		 */
		Long endAt;

		/**
		 * Checks if {@code topic} matches messages tracing command state and message should be traced.
		 * <p>
		 * Match is positive if:
		 * <ul>
		 * <li>{@code active} is {@code true}</li>
		 * <li>{@code topic} equals parameter defined topic</li>
		 * <li>{@code count} is {@code >=0}</li>
		 * <li>{@code startFrom} is {@code < now}</li>
		 * <li>{@code endAt} is {@code > now}</li>
		 * </ul>
		 *
		 * @param topic
		 *            topic name of message
		 * @param needMarkCount
		 *            flag indicating to decrement messages traces counter
		 * @return {@code true} if topic matches messages tracing command state, {@code false} - otherwise
		 */
		public boolean match(String topic, boolean needMarkCount) {
			if (StringUtils.equalsAny(this.topic, topic, MASTER_CONFIG)) {
				if (count != null) {
					if (needMarkCount) {
						count--;
					}
					if (count < 0) {
						return false;
					}
				}
				if (!active) {
					return false;
				}
				if (startFrom != null && startFrom.compareTo(System.currentTimeMillis()) > 0) {
					return false;
				}
				if (endAt != null && endAt.compareTo(System.currentTimeMillis()) < 0) {
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("TopicTraceCommand for "); // NON-NLS
			if (StringUtils.isNotEmpty(topic)) {
				sb.append(topic);
			} else {
				sb.append(MASTER_CONFIG);
			}
			sb.append(' ');
			sb.append(active ? "enabled " : "disabled "); // NON-NLS
			if (count != null) {
				sb.append("for "); // NON-NLS
				sb.append(count);
				sb.append(" messages"); // NON-NLS
			}
			if (startFrom != null) {
				sb.append("starting "); // NON-NLS
				sb.append(new Date(startFrom));
			}
			if (endAt != null) {
				sb.append(" until "); // NON-NLS
				sb.append(new Date(endAt));
			}

			return sb.toString();
		}
	}
}
