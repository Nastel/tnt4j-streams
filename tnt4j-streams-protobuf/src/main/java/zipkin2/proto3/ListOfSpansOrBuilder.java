// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: zipkin/proto3/zipkin.proto

package zipkin2.proto3;

public interface ListOfSpansOrBuilder extends
    // @@protoc_insertion_point(interface_extends:zipkin.proto3.ListOfSpans)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated .zipkin.proto3.Span spans = 1;</code>
   */
  java.util.List<zipkin2.proto3.Span> 
      getSpansList();
  /**
   * <code>repeated .zipkin.proto3.Span spans = 1;</code>
   */
  zipkin2.proto3.Span getSpans(int index);
  /**
   * <code>repeated .zipkin.proto3.Span spans = 1;</code>
   */
  int getSpansCount();
  /**
   * <code>repeated .zipkin.proto3.Span spans = 1;</code>
   */
  java.util.List<? extends zipkin2.proto3.SpanOrBuilder> 
      getSpansOrBuilderList();
  /**
   * <code>repeated .zipkin.proto3.Span spans = 1;</code>
   */
  zipkin2.proto3.SpanOrBuilder getSpansOrBuilder(
      int index);
}