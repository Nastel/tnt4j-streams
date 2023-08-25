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

package com.jkoolcloud.tnt4j.streams.custom.exporters.otel;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

/**
 * @author slb
 * @version 1.0
 * @created 2022-09-12 10:33
 */
public class TNT4JLogExporter extends AbstractTNT4JOTelExporter implements LogRecordExporter {

	public TNT4JLogExporter() {
		super();
	}

	@Override
	public CompletableResultCode export(Collection<LogRecordData> logs) {
		for (LogRecordData log : logs) {
			export(log);
		}

		return CompletableResultCode.ofSuccess();
	}

	protected void export(LogRecordData log) {
		Map<String, Object> logMap = new HashMap<>();
		logMap.put("EntityType", "LOG"); // NON-NLS

		logMap.put("TotalAttributeCount", log.getTotalAttributeCount()); // NON-NLS
		logMap.put("EpochNanos", log.getTimestampEpochNanos()); // NON-NLS
		logMap.put("ObservedEpochNanos", log.getObservedTimestampEpochNanos()); // NON-NLS

		toMap(log.getAttributes(), logMap);

		Severity severity = log.getSeverity();

		Map<String, Object> severityMap = new HashMap<>();
		logMap.put("Severity", severityMap); // NON-NLS

		if (StringUtils.isNotEmpty(log.getSeverityText())) {
			severityMap.put("Text", log.getSeverityText()); // NON-NLS
		}

		severityMap.put("Name", severity.name()); // NON-NLS
		severityMap.put("Number", severity.getSeverityNumber()); // NON-NLS

		toMap(log.getResource(), logMap);
		toMap(log.getInstrumentationScopeInfo(), logMap);
		toMap(log.getSpanContext(), logMap);

		Body body = log.getBody();

		Map<String, Object> bodyMap = new HashMap<>();
		logMap.put("Body", bodyMap); // NON-NLS

		bodyMap.put("Type", body.getType()); // NON-NLS
		bodyMap.put("Text", body.asString()); // NON-NLS

		eManager.export(logMap);
	}

	protected static OpLevel mapSeverity(Severity sev) {
		switch (sev) {
		case FATAL:
		case FATAL2:
		case FATAL3:
		case FATAL4:
			return OpLevel.FATAL;
		case ERROR:
		case ERROR2:
		case ERROR3:
		case ERROR4:
			return OpLevel.ERROR;
		case WARN:
		case WARN2:
		case WARN3:
		case WARN4:
			return OpLevel.WARNING;
		case INFO:
		case INFO2:
		case INFO3:
		case INFO4:
			return OpLevel.INFO;
		case DEBUG:
		case DEBUG2:
		case DEBUG3:
		case DEBUG4:
			return OpLevel.DEBUG;
		case TRACE:
		case TRACE2:
		case TRACE3:
		case TRACE4:
			return OpLevel.TRACE;
		default:
			return OpLevel.NONE;
		}
	}

	@Override
	public TNT4JLogExporter open() throws IOException {
		return this;
	}

	public static class Builder extends AbstractBuilder {

		public Builder() {
			super();
		}

		@Override
		public TNT4JLogExporter build() throws IOException {
			TNT4JLogExporter exporter = new TNT4JLogExporter();
			if (config != null) {
				exporter.configure(config);
			}
			return exporter.open();
		}
	}
}
