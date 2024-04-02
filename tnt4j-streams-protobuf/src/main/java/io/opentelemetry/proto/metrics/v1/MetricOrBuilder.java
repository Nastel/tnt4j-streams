// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: opentelemetry/proto/metrics/v1/metrics.proto

// Protobuf Java Version: 3.25.3
package io.opentelemetry.proto.metrics.v1;

public interface MetricOrBuilder extends
    // @@protoc_insertion_point(interface_extends:opentelemetry.proto.metrics.v1.Metric)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * name of the metric.
   * </pre>
   *
   * <code>string name = 1;</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <pre>
   * name of the metric.
   * </pre>
   *
   * <code>string name = 1;</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <pre>
   * description of the metric, which can be used in documentation.
   * </pre>
   *
   * <code>string description = 2;</code>
   * @return The description.
   */
  java.lang.String getDescription();
  /**
   * <pre>
   * description of the metric, which can be used in documentation.
   * </pre>
   *
   * <code>string description = 2;</code>
   * @return The bytes for description.
   */
  com.google.protobuf.ByteString
      getDescriptionBytes();

  /**
   * <pre>
   * unit in which the metric value is reported. Follows the format
   * described by http://unitsofmeasure.org/ucum.html.
   * </pre>
   *
   * <code>string unit = 3;</code>
   * @return The unit.
   */
  java.lang.String getUnit();
  /**
   * <pre>
   * unit in which the metric value is reported. Follows the format
   * described by http://unitsofmeasure.org/ucum.html.
   * </pre>
   *
   * <code>string unit = 3;</code>
   * @return The bytes for unit.
   */
  com.google.protobuf.ByteString
      getUnitBytes();

  /**
   * <code>.opentelemetry.proto.metrics.v1.Gauge gauge = 5;</code>
   * @return Whether the gauge field is set.
   */
  boolean hasGauge();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Gauge gauge = 5;</code>
   * @return The gauge.
   */
  io.opentelemetry.proto.metrics.v1.Gauge getGauge();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Gauge gauge = 5;</code>
   */
  io.opentelemetry.proto.metrics.v1.GaugeOrBuilder getGaugeOrBuilder();

  /**
   * <code>.opentelemetry.proto.metrics.v1.Sum sum = 7;</code>
   * @return Whether the sum field is set.
   */
  boolean hasSum();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Sum sum = 7;</code>
   * @return The sum.
   */
  io.opentelemetry.proto.metrics.v1.Sum getSum();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Sum sum = 7;</code>
   */
  io.opentelemetry.proto.metrics.v1.SumOrBuilder getSumOrBuilder();

  /**
   * <code>.opentelemetry.proto.metrics.v1.Histogram histogram = 9;</code>
   * @return Whether the histogram field is set.
   */
  boolean hasHistogram();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Histogram histogram = 9;</code>
   * @return The histogram.
   */
  io.opentelemetry.proto.metrics.v1.Histogram getHistogram();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Histogram histogram = 9;</code>
   */
  io.opentelemetry.proto.metrics.v1.HistogramOrBuilder getHistogramOrBuilder();

  /**
   * <code>.opentelemetry.proto.metrics.v1.ExponentialHistogram exponential_histogram = 10;</code>
   * @return Whether the exponentialHistogram field is set.
   */
  boolean hasExponentialHistogram();
  /**
   * <code>.opentelemetry.proto.metrics.v1.ExponentialHistogram exponential_histogram = 10;</code>
   * @return The exponentialHistogram.
   */
  io.opentelemetry.proto.metrics.v1.ExponentialHistogram getExponentialHistogram();
  /**
   * <code>.opentelemetry.proto.metrics.v1.ExponentialHistogram exponential_histogram = 10;</code>
   */
  io.opentelemetry.proto.metrics.v1.ExponentialHistogramOrBuilder getExponentialHistogramOrBuilder();

  /**
   * <code>.opentelemetry.proto.metrics.v1.Summary summary = 11;</code>
   * @return Whether the summary field is set.
   */
  boolean hasSummary();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Summary summary = 11;</code>
   * @return The summary.
   */
  io.opentelemetry.proto.metrics.v1.Summary getSummary();
  /**
   * <code>.opentelemetry.proto.metrics.v1.Summary summary = 11;</code>
   */
  io.opentelemetry.proto.metrics.v1.SummaryOrBuilder getSummaryOrBuilder();

  /**
   * <pre>
   * Additional metadata attributes that describe the metric. [Optional].
   * Attributes are non-identifying.
   * Consumers SHOULD NOT need to be aware of these attributes.
   * These attributes MAY be used to encode information allowing
   * for lossless roundtrip translation to / from another data model.
   * Attribute keys MUST be unique (it is not allowed to have more than one
   * attribute with the same key).
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.common.v1.KeyValue metadata = 12;</code>
   */
  java.util.List<io.opentelemetry.proto.common.v1.KeyValue> 
      getMetadataList();
  /**
   * <pre>
   * Additional metadata attributes that describe the metric. [Optional].
   * Attributes are non-identifying.
   * Consumers SHOULD NOT need to be aware of these attributes.
   * These attributes MAY be used to encode information allowing
   * for lossless roundtrip translation to / from another data model.
   * Attribute keys MUST be unique (it is not allowed to have more than one
   * attribute with the same key).
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.common.v1.KeyValue metadata = 12;</code>
   */
  io.opentelemetry.proto.common.v1.KeyValue getMetadata(int index);
  /**
   * <pre>
   * Additional metadata attributes that describe the metric. [Optional].
   * Attributes are non-identifying.
   * Consumers SHOULD NOT need to be aware of these attributes.
   * These attributes MAY be used to encode information allowing
   * for lossless roundtrip translation to / from another data model.
   * Attribute keys MUST be unique (it is not allowed to have more than one
   * attribute with the same key).
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.common.v1.KeyValue metadata = 12;</code>
   */
  int getMetadataCount();
  /**
   * <pre>
   * Additional metadata attributes that describe the metric. [Optional].
   * Attributes are non-identifying.
   * Consumers SHOULD NOT need to be aware of these attributes.
   * These attributes MAY be used to encode information allowing
   * for lossless roundtrip translation to / from another data model.
   * Attribute keys MUST be unique (it is not allowed to have more than one
   * attribute with the same key).
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.common.v1.KeyValue metadata = 12;</code>
   */
  java.util.List<? extends io.opentelemetry.proto.common.v1.KeyValueOrBuilder> 
      getMetadataOrBuilderList();
  /**
   * <pre>
   * Additional metadata attributes that describe the metric. [Optional].
   * Attributes are non-identifying.
   * Consumers SHOULD NOT need to be aware of these attributes.
   * These attributes MAY be used to encode information allowing
   * for lossless roundtrip translation to / from another data model.
   * Attribute keys MUST be unique (it is not allowed to have more than one
   * attribute with the same key).
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.common.v1.KeyValue metadata = 12;</code>
   */
  io.opentelemetry.proto.common.v1.KeyValueOrBuilder getMetadataOrBuilder(
      int index);

  io.opentelemetry.proto.metrics.v1.Metric.DataCase getDataCase();
}
