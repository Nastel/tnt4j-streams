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
import java.util.*;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * @author slb
 * @version 1.0
 * @created 2020-07-29 14:11
 */
public class TNT4JSpanExporter extends AbstractTNT4JOTelExporter implements SpanExporter {

	public TNT4JSpanExporter() {
		super();
	}

	@Override
	public CompletableResultCode export(Collection<SpanData> spans) {
		for (SpanData span : spans) {
			export(span);
		}

		return CompletableResultCode.ofSuccess();
	}

	protected void export(SpanData span) {
		Map<String, Object> spanMap = new HashMap<>();
		spanMap.put("EntityType", "SPAN"); // NON-NLS

		spanMap.put("Name", span.getName()); // NON-NLS
		spanMap.put("Kind", span.getKind()); // NON-NLS
		spanMap.put("TraceId", span.getTraceId()); // NON-NLS
		spanMap.put("SpanId", span.getSpanId()); // NON-NLS
		spanMap.put("ParentSpanId", span.getParentSpanId()); // NON-NLS
		spanMap.put("StartEpochNanos", span.getStartEpochNanos()); // NON-NLS
		spanMap.put("EndEpochNanos", span.getEndEpochNanos()); // NON-NLS
		spanMap.put("Ended", span.hasEnded()); // NON-NLS
		spanMap.put("TotalAttributeCount", span.getTotalAttributeCount()); // NON-NLS
		spanMap.put("TotalRecordedEvents", span.getTotalRecordedEvents()); // NON-NLS
		spanMap.put("TotalRecordedLinks", span.getTotalRecordedLinks()); // NON-NLS

		toMap(span.getAttributes(), spanMap);
		toMap(span.getSpanContext(), spanMap);
		toMap(span.getParentSpanContext(), spanMap, "ParentSpanContext");

		StatusData statusData = span.getStatus();
		Map<String, Object> statusMap = new HashMap<>();
		spanMap.put("Status", statusMap); // NON-NLS

		statusMap.put("Code", statusData.getStatusCode()); // NON-NLS
		statusMap.put("Description", statusData.getDescription()); // NON-NLS

		toMap(span.getResource(), spanMap);
		toMap(span.getInstrumentationScopeInfo(), spanMap);

		List<EventData> events = span.getEvents();
		Collection<Map<String, ?>> eventsList = new ArrayList<>(events.size());

		for (EventData event : events) {
			Map<String, Object> eventMap = new HashMap<>();

			eventMap.put("Name", event.getName()); // NON-NLS
			eventMap.put("EpochNanos", event.getEpochNanos()); // NON-NLS
			eventMap.put("DroppedAttributesCount", event.getDroppedAttributesCount()); // NON-NLS
			eventMap.put("TotalAttributeCount", event.getTotalAttributeCount()); // NON-NLS

			toMap(event.getAttributes(), eventMap);

			eventsList.add(eventMap);
		}

		List<LinkData> links = span.getLinks();
		Collection<Map<String, ?>> linksList = new ArrayList<>(links.size());

		for (LinkData link : links) {
			Map<String, Object> linkMap = new HashMap<>();

			linkMap.put("TotalAttributeCount", link.getTotalAttributeCount()); // NON-NLS

			toMap(link.getAttributes(), linkMap);
			toMap(link.getSpanContext(), linkMap);

			linksList.add(linkMap);
		}

		eManager.export(spanMap);
	}

	@Override
	public TNT4JSpanExporter open() throws IOException {
		return this;
	}

	public static class Builder extends AbstractBuilder {

		public Builder() {
			super();
		}

		@Override
		public TNT4JSpanExporter build() throws IOException {
			TNT4JSpanExporter exporter = new TNT4JSpanExporter();
			if (config != null) {
				exporter.configure(config);
			}
			return exporter.open();
		}
	}
}
