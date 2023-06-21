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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.trace.v1.TracesData;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from OpenTelemetry protobuf messages
 * {@link io.opentelemetry.proto.trace.v1.TracesData}, {@link io.opentelemetry.proto.metrics.v1.MetricsData} and
 * {@link io.opentelemetry.proto.logs.v1.LogsData} to string keyed map.
 *
 * @version $Revision: 1 $
 */
public class OTLPMessageToMapPreParser extends ProtoMessageToMapPreParser {

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link io.opentelemetry.proto.trace.v1.TracesData}</li>
	 * <li>{@link io.opentelemetry.proto.metrics.v1.MetricsData}</li>
	 * <li>{@link io.opentelemetry.proto.logs.v1.LogsData}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof TracesData || data instanceof MetricsData || data instanceof LogsData;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object convertList(Descriptors.FieldDescriptor fieldDescriptor, List<?> originals) throws Exception {
		if (!originals.isEmpty() && originals.get(0) instanceof KeyValue) {
			return keyValueListToMap((List<KeyValue>) originals);
		} else {
			return super.convertList(fieldDescriptor, originals);
		}
	}

	@Override
	protected Object getAtomicValue(Descriptors.FieldDescriptor fieldDescriptor, Object fieldValue) throws Exception {
		if (fieldValue instanceof AnyValue) {
			return getValueFromAny((AnyValue) fieldValue);
		} else {
			return super.getAtomicValue(fieldDescriptor, fieldValue);
		}
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
			List<AnyValue> valuesList = value.getArrayValue().getValuesList();
			List<Object> objList = new ArrayList<>(valuesList.size());

			for (AnyValue av : valuesList) {
				objList.add(getValueFromAny(av));
			}

			return objList;
		case DOUBLE_VALUE:
			return value.getDoubleValue();
		case KVLIST_VALUE:
			return keyValueListToMap(value.getKvlistValue().getValuesList());
		case BYTES_VALUE:
			return value.getBytesValue().toByteArray();
		case VALUE_NOT_SET:
		default:
			return value;
		}
	}

	private Map<String, ?> keyValueListToMap(List<KeyValue> kvList) {
		Map<String, Object> kvMap = new HashMap<>(kvList.size());

		for (KeyValue kv : kvList) {
			kvMap.put(kv.getKey(), getValueFromAny(kv.getValue()));
		}

		return kvMap;
	}
}
