// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: zipkin/proto3/zipkin.proto

package zipkin2.proto3;

/**
 * <pre>
 * The network context of a node in the service graph.
 * The next id is 5.
 * </pre>
 *
 * Protobuf type {@code zipkin.proto3.Endpoint}
 */
public final class Endpoint extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:zipkin.proto3.Endpoint)
    EndpointOrBuilder {
private static final long serialVersionUID = 0L;
  // Use Endpoint.newBuilder() to construct.
  private Endpoint(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Endpoint() {
    serviceName_ = "";
    ipv4_ = com.google.protobuf.ByteString.EMPTY;
    ipv6_ = com.google.protobuf.ByteString.EMPTY;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new Endpoint();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private Endpoint(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            serviceName_ = s;
            break;
          }
          case 18: {

            ipv4_ = input.readBytes();
            break;
          }
          case 26: {

            ipv6_ = input.readBytes();
            break;
          }
          case 32: {

            port_ = input.readInt32();
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (com.google.protobuf.UninitializedMessageException e) {
      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return zipkin2.proto3.Zipkin.internal_static_zipkin_proto3_Endpoint_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return zipkin2.proto3.Zipkin.internal_static_zipkin_proto3_Endpoint_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            zipkin2.proto3.Endpoint.class, zipkin2.proto3.Endpoint.Builder.class);
  }

  public static final int SERVICE_NAME_FIELD_NUMBER = 1;
  private volatile java.lang.Object serviceName_;
  /**
   * <pre>
   * Lower-case label of this node in the service graph, such as "favstar".
   * Leave absent if unknown.
   * This is a primary label for trace lookup and aggregation, so it should be
   * intuitive and consistent. Many use a name from service discovery.
   * </pre>
   *
   * <code>string service_name = 1;</code>
   * @return The serviceName.
   */
  @java.lang.Override
  public java.lang.String getServiceName() {
    java.lang.Object ref = serviceName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      serviceName_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * Lower-case label of this node in the service graph, such as "favstar".
   * Leave absent if unknown.
   * This is a primary label for trace lookup and aggregation, so it should be
   * intuitive and consistent. Many use a name from service discovery.
   * </pre>
   *
   * <code>string service_name = 1;</code>
   * @return The bytes for serviceName.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getServiceNameBytes() {
    java.lang.Object ref = serviceName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      serviceName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int IPV4_FIELD_NUMBER = 2;
  private com.google.protobuf.ByteString ipv4_;
  /**
   * <pre>
   * 4 byte representation of the primary IPv4 address associated with this
   * connection. Absent if unknown.
   * </pre>
   *
   * <code>bytes ipv4 = 2;</code>
   * @return The ipv4.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getIpv4() {
    return ipv4_;
  }

  public static final int IPV6_FIELD_NUMBER = 3;
  private com.google.protobuf.ByteString ipv6_;
  /**
   * <pre>
   * 16 byte representation of the primary IPv6 address associated with this
   * connection. Absent if unknown.
   * Prefer using the ipv4 field for mapped addresses.
   * </pre>
   *
   * <code>bytes ipv6 = 3;</code>
   * @return The ipv6.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getIpv6() {
    return ipv6_;
  }

  public static final int PORT_FIELD_NUMBER = 4;
  private int port_;
  /**
   * <pre>
   * Depending on context, this could be a listen port or the client-side of a
   * socket. Absent if unknown.
   * </pre>
   *
   * <code>int32 port = 4;</code>
   * @return The port.
   */
  @java.lang.Override
  public int getPort() {
    return port_;
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
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(serviceName_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, serviceName_);
    }
    if (!ipv4_.isEmpty()) {
      output.writeBytes(2, ipv4_);
    }
    if (!ipv6_.isEmpty()) {
      output.writeBytes(3, ipv6_);
    }
    if (port_ != 0) {
      output.writeInt32(4, port_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(serviceName_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, serviceName_);
    }
    if (!ipv4_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(2, ipv4_);
    }
    if (!ipv6_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(3, ipv6_);
    }
    if (port_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(4, port_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof zipkin2.proto3.Endpoint)) {
      return super.equals(obj);
    }
    zipkin2.proto3.Endpoint other = (zipkin2.proto3.Endpoint) obj;

    if (!getServiceName()
        .equals(other.getServiceName())) return false;
    if (!getIpv4()
        .equals(other.getIpv4())) return false;
    if (!getIpv6()
        .equals(other.getIpv6())) return false;
    if (getPort()
        != other.getPort()) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + SERVICE_NAME_FIELD_NUMBER;
    hash = (53 * hash) + getServiceName().hashCode();
    hash = (37 * hash) + IPV4_FIELD_NUMBER;
    hash = (53 * hash) + getIpv4().hashCode();
    hash = (37 * hash) + IPV6_FIELD_NUMBER;
    hash = (53 * hash) + getIpv6().hashCode();
    hash = (37 * hash) + PORT_FIELD_NUMBER;
    hash = (53 * hash) + getPort();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static zipkin2.proto3.Endpoint parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static zipkin2.proto3.Endpoint parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static zipkin2.proto3.Endpoint parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static zipkin2.proto3.Endpoint parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static zipkin2.proto3.Endpoint parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static zipkin2.proto3.Endpoint parseFrom(
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
  public static Builder newBuilder(zipkin2.proto3.Endpoint prototype) {
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
   * The network context of a node in the service graph.
   * The next id is 5.
   * </pre>
   *
   * Protobuf type {@code zipkin.proto3.Endpoint}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:zipkin.proto3.Endpoint)
      zipkin2.proto3.EndpointOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return zipkin2.proto3.Zipkin.internal_static_zipkin_proto3_Endpoint_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return zipkin2.proto3.Zipkin.internal_static_zipkin_proto3_Endpoint_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              zipkin2.proto3.Endpoint.class, zipkin2.proto3.Endpoint.Builder.class);
    }

    // Construct using zipkin2.proto3.Endpoint.newBuilder()
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
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      serviceName_ = "";

      ipv4_ = com.google.protobuf.ByteString.EMPTY;

      ipv6_ = com.google.protobuf.ByteString.EMPTY;

      port_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return zipkin2.proto3.Zipkin.internal_static_zipkin_proto3_Endpoint_descriptor;
    }

    @java.lang.Override
    public zipkin2.proto3.Endpoint getDefaultInstanceForType() {
      return zipkin2.proto3.Endpoint.getDefaultInstance();
    }

    @java.lang.Override
    public zipkin2.proto3.Endpoint build() {
      zipkin2.proto3.Endpoint result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public zipkin2.proto3.Endpoint buildPartial() {
      zipkin2.proto3.Endpoint result = new zipkin2.proto3.Endpoint(this);
      result.serviceName_ = serviceName_;
      result.ipv4_ = ipv4_;
      result.ipv6_ = ipv6_;
      result.port_ = port_;
      onBuilt();
      return result;
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
      if (other instanceof zipkin2.proto3.Endpoint) {
        return mergeFrom((zipkin2.proto3.Endpoint)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(zipkin2.proto3.Endpoint other) {
      if (other == zipkin2.proto3.Endpoint.getDefaultInstance()) return this;
      if (!other.getServiceName().isEmpty()) {
        serviceName_ = other.serviceName_;
        onChanged();
      }
      if (other.getIpv4() != com.google.protobuf.ByteString.EMPTY) {
        setIpv4(other.getIpv4());
      }
      if (other.getIpv6() != com.google.protobuf.ByteString.EMPTY) {
        setIpv6(other.getIpv6());
      }
      if (other.getPort() != 0) {
        setPort(other.getPort());
      }
      this.mergeUnknownFields(other.unknownFields);
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
      zipkin2.proto3.Endpoint parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (zipkin2.proto3.Endpoint) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object serviceName_ = "";
    /**
     * <pre>
     * Lower-case label of this node in the service graph, such as "favstar".
     * Leave absent if unknown.
     * This is a primary label for trace lookup and aggregation, so it should be
     * intuitive and consistent. Many use a name from service discovery.
     * </pre>
     *
     * <code>string service_name = 1;</code>
     * @return The serviceName.
     */
    public java.lang.String getServiceName() {
      java.lang.Object ref = serviceName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        serviceName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * Lower-case label of this node in the service graph, such as "favstar".
     * Leave absent if unknown.
     * This is a primary label for trace lookup and aggregation, so it should be
     * intuitive and consistent. Many use a name from service discovery.
     * </pre>
     *
     * <code>string service_name = 1;</code>
     * @return The bytes for serviceName.
     */
    public com.google.protobuf.ByteString
        getServiceNameBytes() {
      java.lang.Object ref = serviceName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        serviceName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * Lower-case label of this node in the service graph, such as "favstar".
     * Leave absent if unknown.
     * This is a primary label for trace lookup and aggregation, so it should be
     * intuitive and consistent. Many use a name from service discovery.
     * </pre>
     *
     * <code>string service_name = 1;</code>
     * @param value The serviceName to set.
     * @return This builder for chaining.
     */
    public Builder setServiceName(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      serviceName_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Lower-case label of this node in the service graph, such as "favstar".
     * Leave absent if unknown.
     * This is a primary label for trace lookup and aggregation, so it should be
     * intuitive and consistent. Many use a name from service discovery.
     * </pre>
     *
     * <code>string service_name = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearServiceName() {
      
      serviceName_ = getDefaultInstance().getServiceName();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Lower-case label of this node in the service graph, such as "favstar".
     * Leave absent if unknown.
     * This is a primary label for trace lookup and aggregation, so it should be
     * intuitive and consistent. Many use a name from service discovery.
     * </pre>
     *
     * <code>string service_name = 1;</code>
     * @param value The bytes for serviceName to set.
     * @return This builder for chaining.
     */
    public Builder setServiceNameBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      serviceName_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString ipv4_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <pre>
     * 4 byte representation of the primary IPv4 address associated with this
     * connection. Absent if unknown.
     * </pre>
     *
     * <code>bytes ipv4 = 2;</code>
     * @return The ipv4.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getIpv4() {
      return ipv4_;
    }
    /**
     * <pre>
     * 4 byte representation of the primary IPv4 address associated with this
     * connection. Absent if unknown.
     * </pre>
     *
     * <code>bytes ipv4 = 2;</code>
     * @param value The ipv4 to set.
     * @return This builder for chaining.
     */
    public Builder setIpv4(com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      ipv4_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * 4 byte representation of the primary IPv4 address associated with this
     * connection. Absent if unknown.
     * </pre>
     *
     * <code>bytes ipv4 = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearIpv4() {
      
      ipv4_ = getDefaultInstance().getIpv4();
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString ipv6_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <pre>
     * 16 byte representation of the primary IPv6 address associated with this
     * connection. Absent if unknown.
     * Prefer using the ipv4 field for mapped addresses.
     * </pre>
     *
     * <code>bytes ipv6 = 3;</code>
     * @return The ipv6.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getIpv6() {
      return ipv6_;
    }
    /**
     * <pre>
     * 16 byte representation of the primary IPv6 address associated with this
     * connection. Absent if unknown.
     * Prefer using the ipv4 field for mapped addresses.
     * </pre>
     *
     * <code>bytes ipv6 = 3;</code>
     * @param value The ipv6 to set.
     * @return This builder for chaining.
     */
    public Builder setIpv6(com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      ipv6_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * 16 byte representation of the primary IPv6 address associated with this
     * connection. Absent if unknown.
     * Prefer using the ipv4 field for mapped addresses.
     * </pre>
     *
     * <code>bytes ipv6 = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearIpv6() {
      
      ipv6_ = getDefaultInstance().getIpv6();
      onChanged();
      return this;
    }

    private int port_ ;
    /**
     * <pre>
     * Depending on context, this could be a listen port or the client-side of a
     * socket. Absent if unknown.
     * </pre>
     *
     * <code>int32 port = 4;</code>
     * @return The port.
     */
    @java.lang.Override
    public int getPort() {
      return port_;
    }
    /**
     * <pre>
     * Depending on context, this could be a listen port or the client-side of a
     * socket. Absent if unknown.
     * </pre>
     *
     * <code>int32 port = 4;</code>
     * @param value The port to set.
     * @return This builder for chaining.
     */
    public Builder setPort(int value) {
      
      port_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Depending on context, this could be a listen port or the client-side of a
     * socket. Absent if unknown.
     * </pre>
     *
     * <code>int32 port = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearPort() {
      
      port_ = 0;
      onChanged();
      return this;
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


    // @@protoc_insertion_point(builder_scope:zipkin.proto3.Endpoint)
  }

  // @@protoc_insertion_point(class_scope:zipkin.proto3.Endpoint)
  private static final zipkin2.proto3.Endpoint DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new zipkin2.proto3.Endpoint();
  }

  public static zipkin2.proto3.Endpoint getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Endpoint>
      PARSER = new com.google.protobuf.AbstractParser<Endpoint>() {
    @java.lang.Override
    public Endpoint parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new Endpoint(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<Endpoint> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Endpoint> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public zipkin2.proto3.Endpoint getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
