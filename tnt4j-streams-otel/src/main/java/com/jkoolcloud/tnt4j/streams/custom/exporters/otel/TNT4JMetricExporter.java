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
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.*;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

/**
 * @author slb
 * @version 1.0
 * @created 2020-07-29 14:10
 */
public class TNT4JMetricExporter extends AbstractTNT4JOTelExporter implements MetricExporter {

	public TNT4JMetricExporter() {
		super();
	}

	@Override
	public CompletableResultCode export(Collection<MetricData> metrics) {
		for (MetricData metric : metrics) {
			export(metric);
		}

		return CompletableResultCode.ofSuccess();
	}

	protected void export(MetricData metric) {
		Map<String, Object> metricMap = new HashMap<>();
		metricMap.put("EntityType", "METRIC"); // NON-NLS

		metricMap.put("Description", metric.getDescription()); // NON-NLS
		metricMap.put("Name", metric.getName()); // NON-NLS
		metricMap.put("Unit", metric.getUnit()); // NON-NLS
		metricMap.put("Type", metric.getType()); // NON-NLS

		toMap(metric.getResource(), metricMap);
		toMap(metric.getInstrumentationScopeInfo(), metricMap);

		Collection<? extends PointData> dataPoints = metric.getData().getPoints();

		Collection<Map<String, ?>> pointsList = new ArrayList<>(dataPoints.size());
		metricMap.put("Data", pointsList); // NON-NLS

		for (PointData point : dataPoints) {
			Map<String, Object> pointMap = new HashMap<>();

			pointMap.put("EpochNanos", point.getEpochNanos()); // NON-NLS
			pointMap.put("StartEpochNanos", point.getStartEpochNanos()); // NON-NLS

			toMap(point.getAttributes(), pointMap);

			if (point instanceof LongPointData) {
				pointMap.put("Value", ((LongPointData) point).getValue()); // NON-NLS
			}
			if (point instanceof DoublePointData) {
				pointMap.put("Value", ((DoublePointData) point).getValue()); // NON-NLS
			}
			if (point instanceof SummaryPointData) {
				SummaryPointData summaryPoint = (SummaryPointData) point;

				pointMap.put("Count", summaryPoint.getCount()); // NON-NLS
				pointMap.put("Sum", summaryPoint.getSum()); // NON-NLS

				List<ValueAtQuantile> values = summaryPoint.getValues();

				Collection<Map<String, ?>> valuesList = new ArrayList<>(values.size());
				pointMap.put("Values", valuesList); // NON-NLS

				for (ValueAtQuantile value : values) {
					Map<String, Object> valueMap = new HashMap<>(2);

					valueMap.put("Quantile", value.getQuantile()); // NON-NLS
					valueMap.put("Value", value.getValue()); // NON-NLS

					valuesList.add(valueMap);
				}
			}
			if (point instanceof HistogramPointData) {
				HistogramPointData histogramPoint = (HistogramPointData) point;

				pointMap.put("Count", histogramPoint.getCount()); // NON-NLS
				pointMap.put("Sum", histogramPoint.getSum()); // NON-NLS
				if (histogramPoint.hasMin()) {
					pointMap.put("Min", histogramPoint.getMin()); // NON-NLS
				}
				if (histogramPoint.hasMax()) {
					pointMap.put("Max", histogramPoint.getMax()); // NON-NLS
				}
				pointMap.put("Boundaries", histogramPoint.getBoundaries()); // NON-NLS
				pointMap.put("Counts", histogramPoint.getCounts()); // NON-NLS
			}
			if (point instanceof ExponentialHistogramPointData) {
				ExponentialHistogramPointData exponentialHistogramPoint = (ExponentialHistogramPointData) point;

				pointMap.put("Scale", exponentialHistogramPoint.getScale()); // NON-NLS
				pointMap.put("Count", exponentialHistogramPoint.getCount()); // NON-NLS
				pointMap.put("Sum", exponentialHistogramPoint.getSum()); // NON-NLS
				pointMap.put("ZeroCount", exponentialHistogramPoint.getZeroCount()); // NON-NLS
				if (exponentialHistogramPoint.hasMin()) {
					pointMap.put("Min", exponentialHistogramPoint.getMin()); // NON-NLS
				}
				if (exponentialHistogramPoint.hasMax()) {
					pointMap.put("Max", exponentialHistogramPoint.getMax()); // NON-NLS
				}
				toMap(exponentialHistogramPoint.getPositiveBuckets(), pointMap, "PositiveBuckets");
				toMap(exponentialHistogramPoint.getNegativeBuckets(), pointMap, "NegativeBuckets");
			}

			List<? extends ExemplarData> exemplars = point.getExemplars();
			Collection<Map<String, ?>> exemplarsList = new ArrayList<>(exemplars.size());
			pointMap.put("Exemplars", exemplarsList); // NON-NLS

			for (ExemplarData exemplar : exemplars) {
				Map<String, Object> exemplarMap = new HashMap<>();

				exemplarMap.put("EpochNanos", exemplar.getEpochNanos()); // NON-NLS

				toMap(exemplar.getFilteredAttributes(), exemplarMap, "FilteredAttributes");
				toMap(exemplar.getSpanContext(), exemplarMap);

				if (exemplar instanceof DoubleExemplarData) {
					exemplarMap.put("Value", ((DoubleExemplarData) exemplar).getValue()); // NON-NLS
				}
				if (exemplar instanceof LongExemplarData) {
					exemplarMap.put("Value", ((LongExemplarData) exemplar).getValue()); // NON-NLS
				}

				exemplarsList.add(exemplarMap);
			}

			pointsList.add(pointMap);
		}

		eManager.export(metricMap);
	}

	protected static void exponentialHistogramBucketsToMap(ExponentialHistogramBuckets buckets,
			Map<String, Object> bucketsMap) {
		bucketsMap.put("Scale", buckets.getScale()); // NON-NLS
		bucketsMap.put("Offset", buckets.getOffset()); // NON-NLS
		bucketsMap.put("BucketCounts", buckets.getBucketCounts()); // NON-NLS
		bucketsMap.put("TotalCount", buckets.getTotalCount()); // NON-NLS
	}

	protected static Map<String, ?> exponentialHistogramBucketsAsMap(ExponentialHistogramBuckets buckets) {
		Map<String, Object> bucketsMap = new HashMap<>();

		exponentialHistogramBucketsToMap(buckets, bucketsMap);

		return bucketsMap;
	}

	protected static void toMap(ExponentialHistogramBuckets buckets, Map<String, Object> eMap) {
		toMap(buckets, eMap, "Buckets"); // NON-NLS
	}

	protected static void toMap(ExponentialHistogramBuckets buckets, Map<String, Object> eMap, String key) {
		eMap.put(key, exponentialHistogramBucketsAsMap(buckets));
	}

	@Override
	public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
		return AggregationTemporality.CUMULATIVE; // TODO:
	}

	@Override
	public TNT4JMetricExporter open() throws IOException {
		return this;
	}

	public static class Builder extends AbstractBuilder {

		public Builder() {
			super();
		}

		@Override
		public TNT4JMetricExporter build() throws IOException {
			TNT4JMetricExporter exporter = new TNT4JMetricExporter();
			if (config != null) {
				exporter.configure(config);
			}
			return exporter.open();
		}
	}
}
