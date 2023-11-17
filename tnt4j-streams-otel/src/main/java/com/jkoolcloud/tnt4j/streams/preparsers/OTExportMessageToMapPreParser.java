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

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.StringKeyValue;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.DoubleSummaryDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

/**
 * @author slb
 * @version 1.0
 * @created 2022-06-08 12:02
 */
public class OTExportMessageToMapPreParser extends AbstractPreParser<Message, Map<String, ?>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(OTExportMessageToMapPreParser.class);

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest}</li>
	 * <li>{@link io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest}</li>
	 * <li>{@link io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof ExportMetricsServiceRequest || data instanceof ExportTraceServiceRequest
				|| data instanceof ExportLogsServiceRequest;
	}

	@Override
	public Map<String, ?> preParse(Message reqMessage) throws Exception {
		Map<String, Object> reqMap = new LinkedHashMap<>();

		toMap("request", reqMessage, reqMap); // NON-NLS

		return reqMap;
	}

	public void toMap(String key, Message message, Map<String, Object> reqMap) throws Exception {
		if (message instanceof ExportMetricsServiceRequest) {
			ExportMetricsServiceRequest eMsg = (ExportMetricsServiceRequest) message;
			Collection<ResourceMetrics> resMetrics = eMsg.getResourceMetricsList();

			if (CollectionUtils.isNotEmpty(resMetrics)) {
				for (ResourceMetrics rm : resMetrics) {
					boolean hasResource = rm.hasResource();
					Resource resource = rm.getResource();

					List<KeyValue> attributes = resource.getAttributesList();
					int droppedAttrsCount = resource.getDroppedAttributesCount();

					// List<InstrumentationLibraryMetrics> instLibMetrics = rm.getInstrumentationLibraryMetricsList();
					//
					// if (CollectionUtils.isNotEmpty(instLibMetrics)) {
					// for (InstrumentationLibraryMetrics instMetrics : instLibMetrics) {
					// boolean hasIntLib = instMetrics.hasInstrumentationLibrary();
					// InstrumentationLibrary instLib = instMetrics.getInstrumentationLibrary();
					//
					// instLib.getVersion();
					// instLib.getName();
					//
					// List<Metric> metrics = instMetrics.getMetricsList();
					//
					// if (CollectionUtils.isNotEmpty(metrics)) {
					// for (Metric metric : metrics) {
					// metric.getName();
					// metric.getDescription();
					// metric.getUnit();
					// metric.hasIntGauge();
					// metric.getIntGauge();
					// metric.hasDoubleGauge();
					// metric.getDoubleGauge();
					// metric.hasIntSum();
					// metric.getIntSum();
					// metric.hasDoubleSum();
					// metric.getDoubleSum();
					// metric.hasIntHistogram();
					// metric.getIntHistogram();
					// metric.hasDoubleHistogram();
					// metric.getDoubleHistogram();
					// metric.hasDoubleSummary();
					// metric.getDoubleSummary();
					// metric.getDataCase();
					// }
					// }
					// }
					// }
				}
			}
		} else if (message instanceof ExportLogsServiceRequest) {
			ExportLogsServiceRequest eMsg = (ExportLogsServiceRequest) message;

			Collection<ResourceLogs> resLogs = eMsg.getResourceLogsList();
			if (CollectionUtils.isNotEmpty(resLogs)) {
				for (ResourceLogs resLog : resLogs) {
					boolean hasResource = resLog.hasResource();
					Resource resource = resLog.getResource();

					List<KeyValue> attributes = resource.getAttributesList();
					int droppedAttrsCount = resource.getDroppedAttributesCount();

					// List<InstrumentationLibraryLogs> instLibLogs = resLog.getInstrumentationLibraryLogsList();
					//
					// if (CollectionUtils.isNotEmpty(instLibLogs)) {
					// for (InstrumentationLibraryLogs instLog : instLibLogs) {
					// boolean hasIntLib = instLog.hasInstrumentationLibrary();
					// InstrumentationLibrary instLib = instLog.getInstrumentationLibrary();
					//
					// instLib.getVersion();
					// instLib.getName();
					//
					// List<LogRecord> logs = instLog.getLogsList();
					//
					// if (CollectionUtils.isNotEmpty(logs)) {
					// for (LogRecord log : logs) {
					// log.getTimeUnixNano();
					// log.getSeverityNumberValue();
					// log.getSeverityNumber();
					// log.getSeverityText();
					// log.hasBody();
					// log.getBody();
					// log.getAttributesList();
					// log.getDroppedAttributesCount();
					// log.getFlags();
					// log.getTraceId();
					// log.getSpanId();
					// }
					// }
					// }
					// }
				}
			}
		} else if (message instanceof ExportTraceServiceRequest) {
			ExportTraceServiceRequest eMsg = (ExportTraceServiceRequest) message;

			Collection<ResourceSpans> resSpans = eMsg.getResourceSpansList();
			if (CollectionUtils.isNotEmpty(resSpans)) {
				for (ResourceSpans resSpan : resSpans) {
					boolean hasResource = resSpan.hasResource();
					Resource resource = resSpan.getResource();

					List<KeyValue> attributes = resource.getAttributesList();
					int droppedAttrsCount = resource.getDroppedAttributesCount();

					// resSpan.getInstrumentationLibrarySpansList();
				}
			}
		} else {
			Map<Descriptors.FieldDescriptor, Object> allFields = message.getAllFields();

			Map<String, Object> msgMap = new LinkedHashMap<>();
			reqMap.put(key, msgMap);

			for (Map.Entry<Descriptors.FieldDescriptor, Object> field : allFields.entrySet()) {
				fieldToMap(field.getKey().getName(), field.getValue(), msgMap);

				// Descriptors.FieldDescriptor fieldDescriptor = field.getKey();
				// Object fieldValue = field.getValue();
				// Object simpleFieldValue = simplifyValue(message, fieldDescriptor, fieldValue);
				// if (simpleFieldValue != null) {
				// String fieldName = fieldDescriptor.getName();
				// msgMap.put(fieldName, simpleFieldValue);
				// }
			}
		}
	}

	private void fieldToMap(String name, Object value, Map<String, Object> msgMap) throws Exception {
		if (value instanceof Collection) {
			Collection<?> vColl = (Collection<?>) value;
			Map<String, Object> vcMap = new LinkedHashMap<>();
			msgMap.put(name, vcMap);

			int idx = 0;
			for (Object ve : vColl) {
				fieldToMap(String.valueOf(idx++), ve, vcMap);
			}
		} else if (value instanceof DoubleSummaryDataPoint.ValueAtQuantile) {
			DoubleSummaryDataPoint.ValueAtQuantile vaq = (DoubleSummaryDataPoint.ValueAtQuantile) value;
			msgMap.put(String.valueOf(vaq.getQuantile()), vaq.getValue());
		} else if (value instanceof StringKeyValue) {
			StringKeyValue skv = (StringKeyValue) value;
			msgMap.put(skv.getKey(), skv.getValue());
		} else if (value instanceof KeyValue) {
			KeyValue kv = (KeyValue) value;
			fieldToMap(kv.getKey(), getValue(kv.getValue()), msgMap);
		} else if (value instanceof AnyValue) {
			fieldToMap(name, getValue((AnyValue) value), msgMap);
		} else if (value instanceof Message) {
			toMap(name, (Message) value, msgMap);
		} else if (value instanceof Enum) {
			msgMap.put(name, value.toString());
		} else if (value instanceof ByteString) {
			msgMap.put(name, ((ByteString) value).toByteArray());
		} else {
			msgMap.put(name, value);
		}
	}

	private Object getValue(AnyValue value) {
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
		case VALUE_NOT_SET:
		default:
			return value;
		}
	}

	protected Object simplifyValue(Message message, Descriptors.FieldDescriptor fieldDescriptor, Object fieldValue)
			throws Exception {
		Object result = null;
		if (fieldValue != null) {
			if (fieldDescriptor.isRepeated()) {
				if (message.getRepeatedFieldCount(fieldDescriptor) > 0) {
					List<?> originals = (List<?>) fieldValue;
					List<Object> copies = new ArrayList<>(originals.size());
					for (Object original : originals) {
						copies.add(convertAtomicVal(fieldDescriptor, original));
					}
					result = copies;
				}
			} else {
				result = convertAtomicVal(fieldDescriptor, fieldValue);
			}
		}
		return result;
	}

	protected Object convertAtomicVal(Descriptors.FieldDescriptor fieldDescriptor, Object fieldValue) throws Exception {
		Object result = null;
		if (fieldValue != null) {
			switch (fieldDescriptor.getJavaType()) {
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case BOOLEAN:
			case STRING:
				result = fieldValue;
				break;
			case BYTE_STRING:
				result = ((ByteString) fieldValue).toByteArray();
				break;
			case ENUM:
				break;
			case MESSAGE:
				result = preParse((Message) fieldValue);
				break;
			}
		}
		return result;
	}

	@Override
	public String dataTypeReturned() {
		return "MAP"; // NON-NLS
	}
}
