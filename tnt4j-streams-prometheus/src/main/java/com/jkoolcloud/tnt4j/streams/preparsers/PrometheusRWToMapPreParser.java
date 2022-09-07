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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

import prometheus.Remote;
import prometheus.Types;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from binary data {@code byte[]} of *
 * Prometheus Remote-Write protobuf message to string keyed map.
 *
 * @version $Revision: 1 $
 */
public class PrometheusRWToMapPreParser extends AbstractPreParser<byte[], Map<String, ?>> {

	private final Cache<String, Object> metaDataCache = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterAccess(60, TimeUnit.MINUTES).build();

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@code byte[]}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof byte[];
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> preParse(byte[] data) throws Exception {
		Remote.WriteRequest writeRequest = Remote.WriteRequest.parseFrom(data);
		List<Types.MetricMetadata> metadataList = writeRequest.getMetadataList();

		if (CollectionUtils.isNotEmpty(metadataList)) {
			for (Types.MetricMetadata md : metadataList) {
				Map<String, Object> mdMap = new LinkedHashMap<>(5);
				metaDataCache.put(md.getMetricFamilyName(), mdMap); // NON-NLS

				mdMap.put("help", md.getHelp()); // NON-NLS
				mdMap.put("unit", md.getUnit()); // NON-NLS
				mdMap.put("family", md.getMetricFamilyName()); // NON-NLS
				mdMap.put("type", md.getType()); // NON-NLS
			}
		}

		List<Types.TimeSeries> timeSeriesList = writeRequest.getTimeseriesList();

		Map<String, Object> reqMap = new LinkedHashMap<>();

		if (CollectionUtils.isNotEmpty(timeSeriesList)) {
			for (Types.TimeSeries ts : timeSeriesList) {
				Map<String, Object> tsMap = new LinkedHashMap<>();

				if (ts.getLabelsCount() > 0) {
					Map<String, Object> tsLabelsMap = new LinkedHashMap<>();
					tsMap.put("labels", tsLabelsMap); // NON-NLS

					labelsToMap(ts.getLabelsList(), tsLabelsMap);
				}

				if (ts.getSamplesCount() > 0) {
					Map<String, Object> tsSamplesMap = new LinkedHashMap<>();
					tsMap.put("samples", tsSamplesMap); // NON-NLS

					samplesToMap(ts.getSamplesList(), tsSamplesMap);
				}

				if (ts.getExemplarsCount() > 0) {
					Map<String, Object> tsExemplarsMap = new LinkedHashMap<>();
					tsMap.put("exemplars", tsExemplarsMap); // NON-NLS

					List<Types.Exemplar> exemplarsList = ts.getExemplarsList();
					for (Types.Exemplar e : exemplarsList) {
						Map<String, Object> exemplarMap = new LinkedHashMap<>();
						if (e.getLabelsCount() > 0) {
							labelsToMap(e.getLabelsList(), exemplarMap);
						}
						exemplarMap.put("timestamp", e.getTimestamp()); // NON-NLS
						exemplarMap.put("value", e.getValue()); // NON-NLS

						tsExemplarsMap.put(String.valueOf(e.getTimestamp()), exemplarMap);
					}
				}

				Map<String, Object> eMap = (Map<String, Object>) tsMap.get("labels"); // NON-NLS
				String tsName = eMap == null ? null : (String) eMap.get("__name__"); // NON-NLS

				eMap = StringUtils.isEmpty(tsName) ? null : (Map<String, Object>) metaDataCache.getIfPresent(tsName);
				if (eMap != null) {
					tsMap.put("metadata", eMap); // NON-NLS
				}

				Collection<Map<String, ?>> tsCollection = StringUtils.isEmpty(tsName) ? null
						: (Collection<Map<String, ?>>) reqMap.get(tsName);
				if (tsCollection == null) {
					tsCollection = new ArrayList<>();
					reqMap.put(tsName, tsCollection); // NON-NLS
				}

				Map<String, ?> samplesMap = (Map<String, ?>) tsMap.get("samples"); // NON-NLS
				if (samplesMap != null) {
					for (Map.Entry<String, ?> se : samplesMap.entrySet()) {
						Map<String, Object> tscMap = samplesMap.size() > 1 ? Utils.copyMap(tsMap) : tsMap;
						tscMap.put("samples", se.getValue()); // NON-NLS

						tsCollection.add(tscMap);
					}
				}
			}
		}

		return reqMap;
	}

	private static void labelsToMap(List<Types.Label> labels, Map<String, Object> map) {
		for (Types.Label l : labels) {
			map.put(l.getName(), l.getValue());
		}
	}

	private static void samplesToMap(List<Types.Sample> samples, Map<String, Object> map) {
		for (Types.Sample s : samples) {
			Map<String, Object> sampleMap = new LinkedHashMap<>(2);
			sampleMap.put("timestamp", s.getTimestamp()); // NON-NLS
			sampleMap.put("value", s.getValue()); // NON-NLS

			map.put(String.valueOf(s.getTimestamp()), sampleMap);
		}
	}

	@Override
	public String dataTypeReturned() {
		return "MAP"; // NON-NLS
	}
}
