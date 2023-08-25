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
import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;

/**
 * @author slb
 * @version 1.0
 * @created 2023-06-12 18:12
 */
public abstract class AbstractTNT4JOTelExporter implements TNT4JOTelExporter {

	private Map<String, ?> configs;
	protected final ExportsManager eManager;

	public AbstractTNT4JOTelExporter() {
		eManager = ExportsManager.getInstance();
		eManager.bindReference(this);
	}

	public CompletableResultCode flush() {
		return CompletableResultCode.ofSuccess();
	}

	public CompletableResultCode shutdown() {
		eManager.unbindReference(this);

		return CompletableResultCode.ofSuccess();
	}

	@Override
	public void configure(Map<String, ?> configs) {
		this.configs = configs;
	}

	@Override
	public Map<String, ?> getConfig() {
		return configs;
	}

	public static abstract class AbstractBuilder {

		Map<String, ?> config;

		protected AbstractBuilder() {
		}

		public AbstractBuilder setConfig(Map<String, ?> config) {
			this.config = config;

			return this;
		}

		public abstract TNT4JOTelExporter build() throws IOException;
	}

	protected static void resourceToMap(Resource resource, Map<String, Object> resMap) {
		resMap.put("Schema", resource.getSchemaUrl()); // NON-NLS
		toMap(resource.getAttributes(), resMap);
	}

	protected static Map<String, ?> resourceAsMap(Resource resource) {
		Map<String, Object> resMap = new HashMap<>();

		resourceToMap(resource, resMap);

		return resMap;
	}

	protected static void toMap(Resource resource, Map<String, Object> eMap) {
		toMap(resource, eMap, "Resource"); // NON-NLS
	}

	protected static void toMap(Resource resource, Map<String, Object> eMap, String key) {
		eMap.put(key, resourceAsMap(resource));
	}

	protected static void attributesToMap(Attributes attributes, Map<String, Object> attrsMap) {
		attributes.forEach((key, value) -> attrsMap.put(key.getKey(), value));
	}

	protected static Map<String, ?> attributesAsMap(Attributes attributes) {
		Map<String, Object> attrsMap = new HashMap<>();

		attributesToMap(attributes, attrsMap);

		return attrsMap;
	}

	protected static void toMap(Attributes attributes, Map<String, Object> eMap) {
		toMap(attributes, eMap, "Attributes"); // NON-NLS
	}

	protected static void toMap(Attributes attributes, Map<String, Object> eMap, String key) {
		eMap.put(key, attributesAsMap(attributes));
	}

	protected static void instrumentationScopeInfoToMap(InstrumentationScopeInfo isInfo,
			Map<String, Object> isInfoMap) {
		isInfoMap.put("Name", isInfo.getName()); // NON-NLS
		isInfoMap.put("Version", isInfo.getVersion()); // NON-NLS
		isInfoMap.put("Schema", isInfo.getSchemaUrl()); // NON-NLS

		toMap(isInfo.getAttributes(), isInfoMap);
	}

	protected static Map<String, ?> instrumentationScopeInfoAsMap(InstrumentationScopeInfo isInfo) {
		Map<String, Object> isInfoMap = new HashMap<>();

		instrumentationScopeInfoToMap(isInfo, isInfoMap);

		return isInfoMap;
	}

	protected static void toMap(InstrumentationScopeInfo isInfo, Map<String, Object> eMap) {
		toMap(isInfo, eMap, "InstrumentationScope"); // NON-NLS
	}

	protected static void toMap(InstrumentationScopeInfo isInfo, Map<String, Object> eMap, String key) {
		eMap.put(key, instrumentationScopeInfoAsMap(isInfo));
	}

	protected static void spanContextToMap(SpanContext spanCtx, Map<String, Object> spanCtxMap) {
		spanCtxMap.put("TraceId", spanCtx.getTraceId()); // NON-NLS
		spanCtxMap.put("TraceIdBytes", spanCtx.getTraceIdBytes()); // NON-NLS
		spanCtxMap.put("SpanId", spanCtx.getSpanId()); // NON-NLS
		spanCtxMap.put("SpanIdBytes", spanCtx.getSpanIdBytes()); // NON-NLS
		spanCtxMap.put("Sampled", spanCtx.isSampled()); // NON-NLS
		spanCtxMap.put("Valid", spanCtx.isValid()); // NON-NLS
		spanCtxMap.put("Remote", spanCtx.isRemote()); // NON-NLS
		spanCtxMap.put("FlagsHex", spanCtx.getTraceFlags().asHex()); // NON-NLS
		spanCtxMap.put("FlagsByte", spanCtx.getTraceFlags().asByte()); // NON-NLS

		TraceState tState = spanCtx.getTraceState();

		Map<String, Object> traceStateMap = new HashMap<>();
		spanCtxMap.put("TraceState", traceStateMap); // NON-NLS

		tState.forEach((key, value) -> traceStateMap.put(key, value));
	}

	protected static Map<String, ?> spanContextAsMap(SpanContext spanCtx) {
		Map<String, Object> spanCtxMap = new HashMap<>();

		spanContextToMap(spanCtx, spanCtxMap);

		return spanCtxMap;
	}

	protected static void toMap(SpanContext spanCtx, Map<String, Object> eMap) {
		toMap(spanCtx, eMap, "SpanContext"); // NON-NLS
	}

	protected static void toMap(SpanContext spanCtx, Map<String, Object> eMap, String key) {
		eMap.put(key, spanContextAsMap(spanCtx));
	}

}
