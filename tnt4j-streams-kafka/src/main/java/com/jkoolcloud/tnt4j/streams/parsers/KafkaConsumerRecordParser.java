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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an plain java {@link ConsumerRecord} data
 * structure, where each field is represented by declared class field and the field name is used to map each field into
 * its corresponding activity field.
 * <p>
 * List of supported field names:
 * <ul>
 * <li>topic - topic this record is received from</li>
 * <li>partition - partition from which this record is received</li>
 * <li>offset - position of this record in the corresponding Kafka partition</li>
 * <li>timestamp - timestamp of this record</li>
 * <li>timestampType - timestamp type of this record</li>
 * <li>checksum - checksum (CRC32) of the record</li>
 * <li>serializedKeySize - size of the serialized, uncompressed key in bytes</li>
 * <li>serializedValueSize - size of the serialized, uncompressed value in bytes</li>
 * <li>key - record key</li>
 * <li>value - record data</li>
 * <li>headers - record headers</li>
 * <li>leaderEpoch - record leader epoch</li>
 * </ul>
 * <p>
 * If {@code key} or {@code value} contains complex data, use stacked parsers to parse that data. Or if it can be
 * treated as simple Java object (POJO), particular field value can be resolved defining class field names within
 * locator path string. Locator path string should be used resolving particular {@code headers} collection contained
 * value: path element should define header key or index.
 * <p>
 * This activity parser supports configuration properties from {@link GenericActivityParser} (and higher hierarchy
 * parsers).
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class KafkaConsumerRecordParser extends GenericActivityParser<ConsumerRecord<Object, Object>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(KafkaConsumerRecordParser.class);

	/**
	 * Constructs a new KafkaConsumerRecordParser.
	 */
	public KafkaConsumerRecordParser() {
		super(ActivityFieldDataType.AsInput);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.apache.kafka.clients.consumer.ConsumerRecord}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return ConsumerRecord.class.isInstance(data);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
	}

	@Override
	public Object getProperty(String name) {
		return super.getProperty(name);
	}

	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] valPath = Utils.getNodePath(locStr, StreamsConstants.DEFAULT_PATH_DELIM);
		try {
			val = getRecordValue(valPath, cData.getData(), 0);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"KafkaConsumerRecordParser.resolve.locator.value.failed", exc);
		}

		return val;
	}

	/**
	 * Resolves {@link org.apache.kafka.clients.consumer.ConsumerRecord} instance field value defined by {@code cRecord}
	 * fields names {@code path} array.
	 * <p>
	 * If consumer record {@code key} and {@code value} fields classes are known, it can be processed further defining
	 * field names of those classes as {@code path} elements.
	 *
	 * @param path
	 *            fields path as array of consumer record field names
	 * @param cRecord
	 *            consumer record instance to resolve value
	 * @param i
	 *            processed locator path element index
	 * @return resolved consumer record value, or {@code null} if value is not resolved
	 * @throws java.lang.RuntimeException
	 *             if field can't be found or accessed
	 *
	 * @see KafkaUtils#getHeadersValue(String[], org.apache.kafka.common.header.Headers, int)
	 * @see Utils#getFieldValue(String[], Object, int)
	 */
	protected Object getRecordValue(String[] path, ConsumerRecord<?, ?> cRecord, int i) throws RuntimeException {
		if (ArrayUtils.isEmpty(path) || cRecord == null) {
			return null;
		}

		Object val = null;
		String propStr = path[i];

		if ("topic".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.topic();
		} else if ("partition".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.partition();
		} else if ("offset".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.offset();
		} else if ("timestamp".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.timestamp();
		} else if ("timestampType".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.timestampType();
		} else if ("checksum".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.checksum();
		} else if ("serializedKeySize".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.serializedKeySize();
		} else if ("serializedValueSize".equalsIgnoreCase(propStr)) { // NON-NLS
			val = cRecord.serializedValueSize();
		} else if ("headers".equalsIgnoreCase(propStr)) { // NON-NLS
			val = KafkaUtils.getHeadersValue(path, cRecord.headers(), i + 1);
		} else if ("key".equalsIgnoreCase(propStr)) { // NON-NLS
			val = Utils.getFieldValue(path, cRecord.key(), i + 1);
		} else if ("value".equalsIgnoreCase(propStr)) { // NON-NLS
			val = Utils.getFieldValue(path, cRecord.value(), i + 1);
		} else if ("leaderEpoch".equalsIgnoreCase(propStr)) { // NON-NLS
			val = Utils.getFieldValue(path, cRecord.leaderEpoch().orElse(null), i + 1);
		}

		return val;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
