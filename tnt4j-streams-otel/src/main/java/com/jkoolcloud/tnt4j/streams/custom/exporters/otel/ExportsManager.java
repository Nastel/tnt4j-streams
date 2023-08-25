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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.OTelExportersStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.OTelStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author slb
 * @version 1.0
 * @created 2023-06-08 13:20
 */
public class ExportsManager {

	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ExportsManager.class);

	private final Set<TNT4JOTelExporter> references = new LinkedHashSet<>(5);
	private final Set<OTelExportersStream> oTelExportersStreams = new HashSet<>(3);

	private static ExportsManager instance;

	private ExportsManager() {
	}

	protected void initialize() {
	}

	/**
	 * Gets instance of exports manager.
	 *
	 * @return instance of exports manager
	 */
	public static synchronized ExportsManager getInstance() {
		if (instance == null) {
			instance = new ExportsManager();
			instance.initialize();
		}

		return instance;
	}

	public void export(Map<String, ?> otelDataMap) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(OTelStreamConstants.RESOURCE_BUNDLE_NAME,
				"InterceptionsManager.passing.map.to.streams"), Utils.toString(otelDataMap));
		for (OTelExportersStream oTelExportersStream : oTelExportersStreams) {
			oTelExportersStream.addInputToBuffer(otelDataMap);
		}
	}

	/**
	 * Binds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to bind
	 */
	public void bindReference(TNT4JOTelExporter ref) {
		references.add(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(OTelStreamConstants.RESOURCE_BUNDLE_NAME),
				"ExportsManager.bind.reference", ref, references.size());
	}

	/**
	 * Unbinds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to unbind
	 */
	public void unbindReference(TNT4JOTelExporter ref) {
		references.remove(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(OTelStreamConstants.RESOURCE_BUNDLE_NAME),
				"ExportsManager.unbind.reference", ref, references.size());

		finalizeReferences();
	}

	private void finalizeReferences() {
		if (references.isEmpty()) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(OTelStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.shutdown.streams", oTelExportersStreams.size());

			for (OTelExportersStream oTelExportersStream : oTelExportersStreams) {
				oTelExportersStream.markEnded();
			}

			oTelExportersStreams.clear();
		}
	}

	/**
	 * Cleans and unbinds all references bound to this manager.
	 */
	public void shutdown() {
		int refCount = references.size();
		references.clear();

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(OTelStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.unbind.all.reference", refCount);

		finalizeReferences();
	}
}
