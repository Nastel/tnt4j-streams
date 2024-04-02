// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: opentelemetry/proto/logs/v1/logs.proto

// Protobuf Java Version: 3.25.3
package io.opentelemetry.proto.logs.v1;

public interface LogsDataOrBuilder extends
    // @@protoc_insertion_point(interface_extends:opentelemetry.proto.logs.v1.LogsData)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * An array of ResourceLogs.
   * For data coming from a single resource this array will typically contain
   * one element. Intermediary nodes that receive data from multiple origins
   * typically batch the data before forwarding further and in that case this
   * array will contain multiple elements.
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.logs.v1.ResourceLogs resource_logs = 1;</code>
   */
  java.util.List<io.opentelemetry.proto.logs.v1.ResourceLogs> 
      getResourceLogsList();
  /**
   * <pre>
   * An array of ResourceLogs.
   * For data coming from a single resource this array will typically contain
   * one element. Intermediary nodes that receive data from multiple origins
   * typically batch the data before forwarding further and in that case this
   * array will contain multiple elements.
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.logs.v1.ResourceLogs resource_logs = 1;</code>
   */
  io.opentelemetry.proto.logs.v1.ResourceLogs getResourceLogs(int index);
  /**
   * <pre>
   * An array of ResourceLogs.
   * For data coming from a single resource this array will typically contain
   * one element. Intermediary nodes that receive data from multiple origins
   * typically batch the data before forwarding further and in that case this
   * array will contain multiple elements.
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.logs.v1.ResourceLogs resource_logs = 1;</code>
   */
  int getResourceLogsCount();
  /**
   * <pre>
   * An array of ResourceLogs.
   * For data coming from a single resource this array will typically contain
   * one element. Intermediary nodes that receive data from multiple origins
   * typically batch the data before forwarding further and in that case this
   * array will contain multiple elements.
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.logs.v1.ResourceLogs resource_logs = 1;</code>
   */
  java.util.List<? extends io.opentelemetry.proto.logs.v1.ResourceLogsOrBuilder> 
      getResourceLogsOrBuilderList();
  /**
   * <pre>
   * An array of ResourceLogs.
   * For data coming from a single resource this array will typically contain
   * one element. Intermediary nodes that receive data from multiple origins
   * typically batch the data before forwarding further and in that case this
   * array will contain multiple elements.
   * </pre>
   *
   * <code>repeated .opentelemetry.proto.logs.v1.ResourceLogs resource_logs = 1;</code>
   */
  io.opentelemetry.proto.logs.v1.ResourceLogsOrBuilder getResourceLogsOrBuilder(
      int index);
}
