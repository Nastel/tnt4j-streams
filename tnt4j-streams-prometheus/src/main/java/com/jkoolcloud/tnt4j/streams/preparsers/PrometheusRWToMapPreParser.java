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
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

import prometheus.Remote;
import prometheus.Types;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from binary data {@code byte[]} of
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

		if (writeRequest.getMetadataCount() > 0) {
			List<Types.MetricMetadata> metadataList = writeRequest.getMetadataList();
			for (Types.MetricMetadata md : metadataList) {
				Map<String, Object> mdMap = new LinkedHashMap<>(5);
				metaDataCache.put(md.getMetricFamilyName(), mdMap); // NON-NLS

				mdMap.put("help", md.getHelp()); // NON-NLS
				mdMap.put("unit", md.getUnit()); // NON-NLS
				mdMap.put("family", md.getMetricFamilyName()); // NON-NLS
				mdMap.put("type", md.getType().name()); // NON-NLS
				mdMap.put("typeNumber", md.getType().getNumber()); // NON-NLS
			}
		}

		Map<String, Object> reqMap = null;

		if (writeRequest.getTimeseriesCount() > 0) {
			reqMap = new HashMap<>();
			List<Types.TimeSeries> timeSeriesList = writeRequest.getTimeseriesList();
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

				if (ts.getHistogramsCount() > 0) {
					Map<String, Object> tsHistogramsMap = new LinkedHashMap<>();
					tsMap.put("histograms", tsHistogramsMap); // NON-NLS

					List<Types.Histogram> histogramsList = ts.getHistogramsList();
					for (Types.Histogram h : histogramsList) {
						Map<String, Object> histogramMap = new LinkedHashMap<>();

						histogramMap.put("countCase", h.getCountCase().name()); // NON-NLS
						histogramMap.put("countCaseNumber", h.getCountCase().getNumber()); // NON-NLS
						if (h.hasCountFloat()) {
							histogramMap.put("count", h.getCountFloat()); // NON-NLS
						}
						if (h.hasCountInt()) {
							histogramMap.put("count", h.getCountInt()); // NON-NLS
						}
						if (h.getNegativeCountsCount() > 0) {
							histogramMap.put("negativeCounts", h.getNegativeCountsList().toArray(new Double[0])); // NON-NLS
						}
						if (h.getNegativeDeltasCount() > 0) {
							histogramMap.put("negativeDeltas", h.getNegativeDeltasList().toArray(new Long[0])); // NON-NLS
						}
						if (h.getNegativeSpansCount() > 0) {
							Map<String, Object> spansMap = new LinkedHashMap<>();
							tsMap.put("negativeSpans", spansMap); // NON-NLS

							spansToMap(h.getNegativeSpansList(), spansMap);
						}
						if (h.getPositiveCountsCount() > 0) {
							histogramMap.put("positiveCounts", h.getPositiveCountsList().toArray(new Double[0])); // NON-NLS
						}
						if (h.getPositiveDeltasCount() > 0) {
							histogramMap.put("positiveDeltas", h.getPositiveDeltasList().toArray(new Long[0])); // NON-NLS
						}
						if (h.getPositiveSpansCount() > 0) {
							Map<String, Object> spansMap = new LinkedHashMap<>();
							tsMap.put("positiveSpans", spansMap); // NON-NLS

							spansToMap(h.getPositiveSpansList(), spansMap);
						}
						histogramMap.put("resetHint", h.getResetHint().name()); // NON-NLS
						histogramMap.put("resetHintValue", h.getResetHintValue()); // NON-NLS
						histogramMap.put("schema", h.getSchema()); // NON-NLS
						histogramMap.put("sum", h.getSum()); // NON-NLS
						histogramMap.put("timestamp", h.getTimestamp()); // NON-NLS
						histogramMap.put("zeroCountCase", h.getZeroCountCase().name()); // NON-NLS
						if (h.hasZeroCountFloat()) {
							histogramMap.put("zeroCount", h.getZeroCountFloat()); // NON-NLS
						}
						if (h.hasZeroCountInt()) {
							histogramMap.put("zeroCount", h.getZeroCountInt()); // NON-NLS
						}
						histogramMap.put("zeroThreshold", h.getZeroThreshold()); // NON-NLS

						tsHistogramsMap.put(String.valueOf(h.getTimestamp()), histogramMap);
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
				Map<String, ?> exemplarsMap = (Map<String, ?>) tsMap.get("exemplars"); // NON-NLS
				Map<String, ?> histogramsMap = (Map<String, ?>) tsMap.get("histograms"); // NON-NLS
				if (samplesMap != null) {
					for (Map.Entry<String, ?> se : samplesMap.entrySet()) {
						Map<String, Object> tscMap = samplesMap.size() > 1 ? Utils.copyMap(tsMap) : tsMap;
						tscMap.put("samples", se.getValue()); // NON-NLS

						if (exemplarsMap != null) {
							Map<String, ?> exempMap = (Map<String, ?>) exemplarsMap.get(se.getKey());

							if (exempMap != null) {
								tscMap.put("exemplars", exempMap); // NON-NLS
							}
						}
						if (histogramsMap != null) {
							Map<String, ?> histMap = (Map<String, ?>) histogramsMap.get(se.getKey());

							if (histMap != null) {
								tscMap.put("histograms", histMap); // NON-NLS
							}
						}

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

	private static void spansToMap(List<Types.BucketSpan> spans, Map<String, Object> map) {
		Integer[] lengths = new Integer[spans.size()];
		Integer[] offsets = new Integer[spans.size()];

		for (int i = 0; i < spans.size(); i++) {
			Types.BucketSpan bs = spans.get(i);

			lengths[i] = bs.getLength();
			offsets[i] = bs.getOffset();
		}

		map.put("lengths", lengths); // NON-NLS
		map.put("offsets", offsets); // NON-NLS
	}

	@Override
	public String dataTypeReturned() {
		return "MAP"; // NON-NLS
	}
}
