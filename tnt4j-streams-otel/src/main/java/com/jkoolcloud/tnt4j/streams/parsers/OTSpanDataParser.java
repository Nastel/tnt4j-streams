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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.*;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.TracesData;

/**
 * @author slb
 * @version 1.0
 * @created 2023-06-20 16:07
 */
public class OTSpanDataParser extends AbstractOTLPDataParser<TracesData> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(OTSpanDataParser.class);

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
	 * <li>{@link io.opentelemetry.proto.trace.v1.TracesData}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return data instanceof byte[] || data instanceof TracesData;
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		TracesData tracesData;
		try {
			if (data instanceof byte[]) {
				tracesData = TracesData.parseFrom((byte[]) data);
			} else {
				tracesData = (TracesData) data;
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityXmlParser.xmlDocument.parse.error"), 0);
			pe.initCause(e);

			throw pe;
		}

		ActivityContext cData = new ActivityContext(stream, data, tracesData);
		cData.setMessage(toString(tracesData));

		return cData;
	}

	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator,
			GenericActivityParser<TracesData>.ActivityContext cData, AtomicBoolean formattingNeeded)
			throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = (String[]) getPreparedLocator(locStr,
				k -> Utils.getNodePath(k, StreamsConstants.DEFAULT_PATH_DELIM));
		val = getTracesValue(path, cData.getData(), 0);

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(ProtobufStreamConstants.RESOURCE_BUNDLE_NAME),
				"ActivityProtobufParser.resolved.protobuf.value", locStr, toString(val));

		return val;
	}

	protected Object getTracesValue(String[] path, TracesData tracesData, int i) throws ParseException {
		if (ArrayUtils.isEmpty(path) || tracesData == null) {
			return null;
		}

		Descriptors.Descriptor msgDescr = tracesData.getDescriptorForType();
		Descriptors.FieldDescriptor fDescr = msgDescr.findFieldByName(path[i]);

		if (fDescr == null) {
			return null;
		}

		try {
			Object fVal = tracesData.getField(fDescr);

			fVal = xxxx(fVal, path, i);

			return fVal;
		} catch (Exception exc) {
			throw new RuntimeException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityProtobufParser.could.not.get.field", path[i], tracesData.getClass().getSimpleName(),
					toString(tracesData)), exc);
		}
	}

	private Object xxxx(Object fVal, String[] path, int i) throws ParseException {
		if (i >= path.length) {
			return fVal;
		}

		if (fVal instanceof Descriptors.EnumValueDescriptor) {
			if ("number".equalsIgnoreCase(path[i + 1])) { // NON-NLS
				return ((Descriptors.EnumValueDescriptor) fVal).getNumber();
			} else {
				return ((Descriptors.EnumValueDescriptor) fVal).getName();
			}
		} else if (fVal instanceof Message) {
			return getTracesValue(path, (TracesData) fVal, i + 1);
		} else if (fVal instanceof List) {
			List<?> rFields = (List<?>) fVal;

			if (!rFields.isEmpty()) {
				if (rFields.get(0) instanceof KeyValue) {
					if (i >= path.length - 1) {
						return getAttrsMap((List<KeyValue>) rFields);
					} else {
						for (Object rField : rFields) {
							KeyValue kv = (KeyValue) rField;
							if (kv.getKey().equals(path[i])) {
								fVal = getValueFromAny(kv.getValue());
							}
						}
					}
				} else if (rFields.size() == 1 && i < path.length - 1) {
					fVal = rFields.get(0);
				} else {
					int idx = Integer.parseInt(path[i]);
					fVal = rFields.get(idx);
				}

				return xxxx(fVal, path, i + 1);
			}
		}

		return fVal;
	}

	private Map<String, ?> getAttrsMap(List<KeyValue> attributes) {
		Map<String, Object> attrMap = new HashMap<>(attributes.size());

		for (KeyValue attr : attributes) {
			attrMap.put(attr.getKey(), getValueFromAny(attr.getValue()));
		}

		return attrMap;
	}

	private Object getValueFromAny(AnyValue value) {
		if (value == null) {
			return value;
		}

		switch (value.getValueCase()) {
		case STRING_VALUE:
			return value.getStringValue();
		case INT_VALUE:
			return value.getIntValue();
		case BOOL_VALUE:
			return value.getBoolValue();
		case ARRAY_VALUE:
			return value.getArrayValue().getValuesList();
		case DOUBLE_VALUE:
			return value.getDoubleValue();
		case KVLIST_VALUE:
			return value.getKvlistValue().getValuesList();
		case BYTES_VALUE:
			return value.getBytesValue().toByteArray();
		case VALUE_NOT_SET:
		default:
			return value;
		}
	}

	protected String toString(Message protoMsg) {
		return TextFormat.printer().printToString(protoMsg);
		// return JsonFormat.printer().includingDefaultValueFields().printToString(protoMsg);
	}
}