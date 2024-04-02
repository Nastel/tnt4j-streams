// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: opentelemetry/proto/trace/v1/trace.proto

// Protobuf Java Version: 3.25.3
package io.opentelemetry.proto.trace.v1;

/**
 * <pre>
 * SpanFlags represents constants used to interpret the
 * Span.flags field, which is protobuf 'fixed32' type and is to
 * be used as bit-fields. Each non-zero value defined in this enum is
 * a bit-mask.  To extract the bit-field, for example, use an
 * expression like:
 *
 *   (span.flags &amp; SPAN_FLAGS_TRACE_FLAGS_MASK)
 *
 * See https://www.w3.org/TR/trace-context-2/#trace-flags for the flag definitions.
 *
 * Note that Span flags were introduced in version 1.1 of the
 * OpenTelemetry protocol.  Older Span producers do not set this
 * field, consequently consumers should not rely on the absence of a
 * particular flag bit to indicate the presence of a particular feature.
 * </pre>
 *
 * Protobuf enum {@code opentelemetry.proto.trace.v1.SpanFlags}
 */
public enum SpanFlags
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <pre>
   * The zero value for the enum. Should not be used for comparisons.
   * Instead use bitwise "and" with the appropriate mask as shown above.
   * </pre>
   *
   * <code>SPAN_FLAGS_DO_NOT_USE = 0;</code>
   */
  SPAN_FLAGS_DO_NOT_USE(0),
  /**
   * <pre>
   * Bits 0-7 are used for trace flags.
   * </pre>
   *
   * <code>SPAN_FLAGS_TRACE_FLAGS_MASK = 255;</code>
   */
  SPAN_FLAGS_TRACE_FLAGS_MASK(255),
  /**
   * <pre>
   * Bits 8 and 9 are used to indicate that the parent span or link span is remote.
   * Bit 8 (`HAS_IS_REMOTE`) indicates whether the value is known.
   * Bit 9 (`IS_REMOTE`) indicates whether the span or link is remote.
   * </pre>
   *
   * <code>SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK = 256;</code>
   */
  SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK(256),
  /**
   * <code>SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK = 512;</code>
   */
  SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK(512),
  UNRECOGNIZED(-1),
  ;

  /**
   * <pre>
   * The zero value for the enum. Should not be used for comparisons.
   * Instead use bitwise "and" with the appropriate mask as shown above.
   * </pre>
   *
   * <code>SPAN_FLAGS_DO_NOT_USE = 0;</code>
   */
  public static final int SPAN_FLAGS_DO_NOT_USE_VALUE = 0;
  /**
   * <pre>
   * Bits 0-7 are used for trace flags.
   * </pre>
   *
   * <code>SPAN_FLAGS_TRACE_FLAGS_MASK = 255;</code>
   */
  public static final int SPAN_FLAGS_TRACE_FLAGS_MASK_VALUE = 255;
  /**
   * <pre>
   * Bits 8 and 9 are used to indicate that the parent span or link span is remote.
   * Bit 8 (`HAS_IS_REMOTE`) indicates whether the value is known.
   * Bit 9 (`IS_REMOTE`) indicates whether the span or link is remote.
   * </pre>
   *
   * <code>SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK = 256;</code>
   */
  public static final int SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK_VALUE = 256;
  /**
   * <code>SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK = 512;</code>
   */
  public static final int SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK_VALUE = 512;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static SpanFlags valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static SpanFlags forNumber(int value) {
    switch (value) {
      case 0: return SPAN_FLAGS_DO_NOT_USE;
      case 255: return SPAN_FLAGS_TRACE_FLAGS_MASK;
      case 256: return SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK;
      case 512: return SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<SpanFlags>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      SpanFlags> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<SpanFlags>() {
          public SpanFlags findValueByNumber(int number) {
            return SpanFlags.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return io.opentelemetry.proto.trace.v1.TraceProto.getDescriptor().getEnumTypes().get(0);
  }

  private static final SpanFlags[] VALUES = values();

  public static SpanFlags valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private SpanFlags(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:opentelemetry.proto.trace.v1.SpanFlags)
}

