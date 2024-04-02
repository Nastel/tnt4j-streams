// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: opentelemetry/proto/common/v1/common.proto

// Protobuf Java Version: 3.25.3
package io.opentelemetry.proto.common.v1;

/**
 * <pre>
 * KeyValue is a key-value pair that is used to store Span attributes, Link
 * attributes, etc.
 * </pre>
 *
 * Protobuf type {@code opentelemetry.proto.common.v1.KeyValue}
 */
public final class KeyValue extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:opentelemetry.proto.common.v1.KeyValue)
    KeyValueOrBuilder {
private static final long serialVersionUID = 0L;
  // Use KeyValue.newBuilder() to construct.
  private KeyValue(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private KeyValue() {
    key_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new KeyValue();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return io.opentelemetry.proto.common.v1.CommonProto.internal_static_opentelemetry_proto_common_v1_KeyValue_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.opentelemetry.proto.common.v1.CommonProto.internal_static_opentelemetry_proto_common_v1_KeyValue_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.opentelemetry.proto.common.v1.KeyValue.class, io.opentelemetry.proto.common.v1.KeyValue.Builder.class);
  }

  private int bitField0_;
  public static final int KEY_FIELD_NUMBER = 1;
  @SuppressWarnings("serial")
  private volatile java.lang.Object key_ = "";
  /**
   * <code>string key = 1;</code>
   * @return The key.
   */
  @java.lang.Override
  public java.lang.String getKey() {
    java.lang.Object ref = key_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      key_ = s;
      return s;
    }
  }
  /**
   * <code>string key = 1;</code>
   * @return The bytes for key.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getKeyBytes() {
    java.lang.Object ref = key_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      key_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int VALUE_FIELD_NUMBER = 2;
  private io.opentelemetry.proto.common.v1.AnyValue value_;
  /**
   * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
   * @return Whether the value field is set.
   */
  @java.lang.Override
  public boolean hasValue() {
    return ((bitField0_ & 0x00000001) != 0);
  }
  /**
   * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
   * @return The value.
   */
  @java.lang.Override
  public io.opentelemetry.proto.common.v1.AnyValue getValue() {
    return value_ == null ? io.opentelemetry.proto.common.v1.AnyValue.getDefaultInstance() : value_;
  }
  /**
   * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
   */
  @java.lang.Override
  public io.opentelemetry.proto.common.v1.AnyValueOrBuilder getValueOrBuilder() {
    return value_ == null ? io.opentelemetry.proto.common.v1.AnyValue.getDefaultInstance() : value_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(key_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, key_);
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      output.writeMessage(2, getValue());
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(key_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, key_);
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, getValue());
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof io.opentelemetry.proto.common.v1.KeyValue)) {
      return super.equals(obj);
    }
    io.opentelemetry.proto.common.v1.KeyValue other = (io.opentelemetry.proto.common.v1.KeyValue) obj;

    if (!getKey()
        .equals(other.getKey())) return false;
    if (hasValue() != other.hasValue()) return false;
    if (hasValue()) {
      if (!getValue()
          .equals(other.getValue())) return false;
    }
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + KEY_FIELD_NUMBER;
    hash = (53 * hash) + getKey().hashCode();
    if (hasValue()) {
      hash = (37 * hash) + VALUE_FIELD_NUMBER;
      hash = (53 * hash) + getValue().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static io.opentelemetry.proto.common.v1.KeyValue parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static io.opentelemetry.proto.common.v1.KeyValue parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.opentelemetry.proto.common.v1.KeyValue parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.opentelemetry.proto.common.v1.KeyValue prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * KeyValue is a key-value pair that is used to store Span attributes, Link
   * attributes, etc.
   * </pre>
   *
   * Protobuf type {@code opentelemetry.proto.common.v1.KeyValue}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:opentelemetry.proto.common.v1.KeyValue)
      io.opentelemetry.proto.common.v1.KeyValueOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.opentelemetry.proto.common.v1.CommonProto.internal_static_opentelemetry_proto_common_v1_KeyValue_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.opentelemetry.proto.common.v1.CommonProto.internal_static_opentelemetry_proto_common_v1_KeyValue_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.opentelemetry.proto.common.v1.KeyValue.class, io.opentelemetry.proto.common.v1.KeyValue.Builder.class);
    }

    // Construct using io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
        getValueFieldBuilder();
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      key_ = "";
      value_ = null;
      if (valueBuilder_ != null) {
        valueBuilder_.dispose();
        valueBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.opentelemetry.proto.common.v1.CommonProto.internal_static_opentelemetry_proto_common_v1_KeyValue_descriptor;
    }

    @java.lang.Override
    public io.opentelemetry.proto.common.v1.KeyValue getDefaultInstanceForType() {
      return io.opentelemetry.proto.common.v1.KeyValue.getDefaultInstance();
    }

    @java.lang.Override
    public io.opentelemetry.proto.common.v1.KeyValue build() {
      io.opentelemetry.proto.common.v1.KeyValue result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public io.opentelemetry.proto.common.v1.KeyValue buildPartial() {
      io.opentelemetry.proto.common.v1.KeyValue result = new io.opentelemetry.proto.common.v1.KeyValue(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(io.opentelemetry.proto.common.v1.KeyValue result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.key_ = key_;
      }
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.value_ = valueBuilder_ == null
            ? value_
            : valueBuilder_.build();
        to_bitField0_ |= 0x00000001;
      }
      result.bitField0_ |= to_bitField0_;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.opentelemetry.proto.common.v1.KeyValue) {
        return mergeFrom((io.opentelemetry.proto.common.v1.KeyValue)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.opentelemetry.proto.common.v1.KeyValue other) {
      if (other == io.opentelemetry.proto.common.v1.KeyValue.getDefaultInstance()) return this;
      if (!other.getKey().isEmpty()) {
        key_ = other.key_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (other.hasValue()) {
        mergeValue(other.getValue());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              key_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000001;
              break;
            } // case 10
            case 18: {
              input.readMessage(
                  getValueFieldBuilder().getBuilder(),
                  extensionRegistry);
              bitField0_ |= 0x00000002;
              break;
            } // case 18
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private java.lang.Object key_ = "";
    /**
     * <code>string key = 1;</code>
     * @return The key.
     */
    public java.lang.String getKey() {
      java.lang.Object ref = key_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        key_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string key = 1;</code>
     * @return The bytes for key.
     */
    public com.google.protobuf.ByteString
        getKeyBytes() {
      java.lang.Object ref = key_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        key_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string key = 1;</code>
     * @param value The key to set.
     * @return This builder for chaining.
     */
    public Builder setKey(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      key_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>string key = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearKey() {
      key_ = getDefaultInstance().getKey();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <code>string key = 1;</code>
     * @param value The bytes for key to set.
     * @return This builder for chaining.
     */
    public Builder setKeyBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      key_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private io.opentelemetry.proto.common.v1.AnyValue value_;
    private com.google.protobuf.SingleFieldBuilderV3<
        io.opentelemetry.proto.common.v1.AnyValue, io.opentelemetry.proto.common.v1.AnyValue.Builder, io.opentelemetry.proto.common.v1.AnyValueOrBuilder> valueBuilder_;
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     * @return Whether the value field is set.
     */
    public boolean hasValue() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     * @return The value.
     */
    public io.opentelemetry.proto.common.v1.AnyValue getValue() {
      if (valueBuilder_ == null) {
        return value_ == null ? io.opentelemetry.proto.common.v1.AnyValue.getDefaultInstance() : value_;
      } else {
        return valueBuilder_.getMessage();
      }
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    public Builder setValue(io.opentelemetry.proto.common.v1.AnyValue value) {
      if (valueBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        value_ = value;
      } else {
        valueBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    public Builder setValue(
        io.opentelemetry.proto.common.v1.AnyValue.Builder builderForValue) {
      if (valueBuilder_ == null) {
        value_ = builderForValue.build();
      } else {
        valueBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    public Builder mergeValue(io.opentelemetry.proto.common.v1.AnyValue value) {
      if (valueBuilder_ == null) {
        if (((bitField0_ & 0x00000002) != 0) &&
          value_ != null &&
          value_ != io.opentelemetry.proto.common.v1.AnyValue.getDefaultInstance()) {
          getValueBuilder().mergeFrom(value);
        } else {
          value_ = value;
        }
      } else {
        valueBuilder_.mergeFrom(value);
      }
      if (value_ != null) {
        bitField0_ |= 0x00000002;
        onChanged();
      }
      return this;
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    public Builder clearValue() {
      bitField0_ = (bitField0_ & ~0x00000002);
      value_ = null;
      if (valueBuilder_ != null) {
        valueBuilder_.dispose();
        valueBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    public io.opentelemetry.proto.common.v1.AnyValue.Builder getValueBuilder() {
      bitField0_ |= 0x00000002;
      onChanged();
      return getValueFieldBuilder().getBuilder();
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    public io.opentelemetry.proto.common.v1.AnyValueOrBuilder getValueOrBuilder() {
      if (valueBuilder_ != null) {
        return valueBuilder_.getMessageOrBuilder();
      } else {
        return value_ == null ?
            io.opentelemetry.proto.common.v1.AnyValue.getDefaultInstance() : value_;
      }
    }
    /**
     * <code>.opentelemetry.proto.common.v1.AnyValue value = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        io.opentelemetry.proto.common.v1.AnyValue, io.opentelemetry.proto.common.v1.AnyValue.Builder, io.opentelemetry.proto.common.v1.AnyValueOrBuilder> 
        getValueFieldBuilder() {
      if (valueBuilder_ == null) {
        valueBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            io.opentelemetry.proto.common.v1.AnyValue, io.opentelemetry.proto.common.v1.AnyValue.Builder, io.opentelemetry.proto.common.v1.AnyValueOrBuilder>(
                getValue(),
                getParentForChildren(),
                isClean());
        value_ = null;
      }
      return valueBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:opentelemetry.proto.common.v1.KeyValue)
  }

  // @@protoc_insertion_point(class_scope:opentelemetry.proto.common.v1.KeyValue)
  private static final io.opentelemetry.proto.common.v1.KeyValue DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.opentelemetry.proto.common.v1.KeyValue();
  }

  public static io.opentelemetry.proto.common.v1.KeyValue getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<KeyValue>
      PARSER = new com.google.protobuf.AbstractParser<KeyValue>() {
    @java.lang.Override
    public KeyValue parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<KeyValue> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<KeyValue> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public io.opentelemetry.proto.common.v1.KeyValue getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

