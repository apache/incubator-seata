// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: branchRollbackRequest.proto

package io.seata.codec.protobuf.generated;

/**
 * <pre>
 * PublishRequest is a publish request.
 * </pre>
 *
 * Protobuf type {@code io.seata.protocol.protobuf.BranchRollbackRequestProto}
 */
public  final class BranchRollbackRequestProto extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.seata.protocol.protobuf.BranchRollbackRequestProto)
    BranchRollbackRequestProtoOrBuilder {
private static final long serialVersionUID = 0L;
  // Use BranchRollbackRequestProto.newBuilder() to construct.
  private BranchRollbackRequestProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private BranchRollbackRequestProto() {
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private BranchRollbackRequestProto(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
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
            io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.Builder subBuilder = null;
            if (abstractBranchEndRequest_ != null) {
              subBuilder = abstractBranchEndRequest_.toBuilder();
            }
            abstractBranchEndRequest_ = input.readMessage(io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(abstractBranchEndRequest_);
              abstractBranchEndRequest_ = subBuilder.buildPartial();
            }

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
    return io.seata.codec.protobuf.generated.BranchRollbackRequest.internal_static_io_seata_protocol_protobuf_BranchRollbackRequestProto_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.seata.codec.protobuf.generated.BranchRollbackRequest.internal_static_io_seata_protocol_protobuf_BranchRollbackRequestProto_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.seata.codec.protobuf.generated.BranchRollbackRequestProto.class, io.seata.codec.protobuf.generated.BranchRollbackRequestProto.Builder.class);
  }

  public static final int ABSTRACTBRANCHENDREQUEST_FIELD_NUMBER = 1;
  private io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto abstractBranchEndRequest_;
  /**
   * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
   */
  public boolean hasAbstractBranchEndRequest() {
    return abstractBranchEndRequest_ != null;
  }
  /**
   * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
   */
  public io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto getAbstractBranchEndRequest() {
    return abstractBranchEndRequest_ == null ? io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.getDefaultInstance() : abstractBranchEndRequest_;
  }
  /**
   * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
   */
  public io.seata.codec.protobuf.generated.AbstractBranchEndRequestProtoOrBuilder getAbstractBranchEndRequestOrBuilder() {
    return getAbstractBranchEndRequest();
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
    if (abstractBranchEndRequest_ != null) {
      output.writeMessage(1, getAbstractBranchEndRequest());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (abstractBranchEndRequest_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getAbstractBranchEndRequest());
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
    if (!(obj instanceof io.seata.codec.protobuf.generated.BranchRollbackRequestProto)) {
      return super.equals(obj);
    }
    io.seata.codec.protobuf.generated.BranchRollbackRequestProto other = (io.seata.codec.protobuf.generated.BranchRollbackRequestProto) obj;

    if (hasAbstractBranchEndRequest() != other.hasAbstractBranchEndRequest()) return false;
    if (hasAbstractBranchEndRequest()) {
      if (!getAbstractBranchEndRequest()
          .equals(other.getAbstractBranchEndRequest())) return false;
    }
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
    if (hasAbstractBranchEndRequest()) {
      hash = (37 * hash) + ABSTRACTBRANCHENDREQUEST_FIELD_NUMBER;
      hash = (53 * hash) + getAbstractBranchEndRequest().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto parseFrom(
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
  public static Builder newBuilder(io.seata.codec.protobuf.generated.BranchRollbackRequestProto prototype) {
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
   * PublishRequest is a publish request.
   * </pre>
   *
   * Protobuf type {@code io.seata.protocol.protobuf.BranchRollbackRequestProto}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.seata.protocol.protobuf.BranchRollbackRequestProto)
      io.seata.codec.protobuf.generated.BranchRollbackRequestProtoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.seata.codec.protobuf.generated.BranchRollbackRequest.internal_static_io_seata_protocol_protobuf_BranchRollbackRequestProto_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.seata.codec.protobuf.generated.BranchRollbackRequest.internal_static_io_seata_protocol_protobuf_BranchRollbackRequestProto_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.seata.codec.protobuf.generated.BranchRollbackRequestProto.class, io.seata.codec.protobuf.generated.BranchRollbackRequestProto.Builder.class);
    }

    // Construct using io.seata.codec.protobuf.generated.BranchRollbackRequestProto.newBuilder()
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
      if (abstractBranchEndRequestBuilder_ == null) {
        abstractBranchEndRequest_ = null;
      } else {
        abstractBranchEndRequest_ = null;
        abstractBranchEndRequestBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.seata.codec.protobuf.generated.BranchRollbackRequest.internal_static_io_seata_protocol_protobuf_BranchRollbackRequestProto_descriptor;
    }

    @java.lang.Override
    public io.seata.codec.protobuf.generated.BranchRollbackRequestProto getDefaultInstanceForType() {
      return io.seata.codec.protobuf.generated.BranchRollbackRequestProto.getDefaultInstance();
    }

    @java.lang.Override
    public io.seata.codec.protobuf.generated.BranchRollbackRequestProto build() {
      io.seata.codec.protobuf.generated.BranchRollbackRequestProto result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public io.seata.codec.protobuf.generated.BranchRollbackRequestProto buildPartial() {
      io.seata.codec.protobuf.generated.BranchRollbackRequestProto result = new io.seata.codec.protobuf.generated.BranchRollbackRequestProto(this);
      if (abstractBranchEndRequestBuilder_ == null) {
        result.abstractBranchEndRequest_ = abstractBranchEndRequest_;
      } else {
        result.abstractBranchEndRequest_ = abstractBranchEndRequestBuilder_.build();
      }
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
      if (other instanceof io.seata.codec.protobuf.generated.BranchRollbackRequestProto) {
        return mergeFrom((io.seata.codec.protobuf.generated.BranchRollbackRequestProto)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.seata.codec.protobuf.generated.BranchRollbackRequestProto other) {
      if (other == io.seata.codec.protobuf.generated.BranchRollbackRequestProto.getDefaultInstance()) return this;
      if (other.hasAbstractBranchEndRequest()) {
        mergeAbstractBranchEndRequest(other.getAbstractBranchEndRequest());
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
      io.seata.codec.protobuf.generated.BranchRollbackRequestProto parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.seata.codec.protobuf.generated.BranchRollbackRequestProto) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto abstractBranchEndRequest_;
    private com.google.protobuf.SingleFieldBuilderV3<
        io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto, io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.Builder, io.seata.codec.protobuf.generated.AbstractBranchEndRequestProtoOrBuilder> abstractBranchEndRequestBuilder_;
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public boolean hasAbstractBranchEndRequest() {
      return abstractBranchEndRequestBuilder_ != null || abstractBranchEndRequest_ != null;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto getAbstractBranchEndRequest() {
      if (abstractBranchEndRequestBuilder_ == null) {
        return abstractBranchEndRequest_ == null ? io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.getDefaultInstance() : abstractBranchEndRequest_;
      } else {
        return abstractBranchEndRequestBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public Builder setAbstractBranchEndRequest(io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto value) {
      if (abstractBranchEndRequestBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        abstractBranchEndRequest_ = value;
        onChanged();
      } else {
        abstractBranchEndRequestBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public Builder setAbstractBranchEndRequest(
        io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.Builder builderForValue) {
      if (abstractBranchEndRequestBuilder_ == null) {
        abstractBranchEndRequest_ = builderForValue.build();
        onChanged();
      } else {
        abstractBranchEndRequestBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public Builder mergeAbstractBranchEndRequest(io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto value) {
      if (abstractBranchEndRequestBuilder_ == null) {
        if (abstractBranchEndRequest_ != null) {
          abstractBranchEndRequest_ =
            io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.newBuilder(abstractBranchEndRequest_).mergeFrom(value).buildPartial();
        } else {
          abstractBranchEndRequest_ = value;
        }
        onChanged();
      } else {
        abstractBranchEndRequestBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public Builder clearAbstractBranchEndRequest() {
      if (abstractBranchEndRequestBuilder_ == null) {
        abstractBranchEndRequest_ = null;
        onChanged();
      } else {
        abstractBranchEndRequest_ = null;
        abstractBranchEndRequestBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.Builder getAbstractBranchEndRequestBuilder() {
      
      onChanged();
      return getAbstractBranchEndRequestFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    public io.seata.codec.protobuf.generated.AbstractBranchEndRequestProtoOrBuilder getAbstractBranchEndRequestOrBuilder() {
      if (abstractBranchEndRequestBuilder_ != null) {
        return abstractBranchEndRequestBuilder_.getMessageOrBuilder();
      } else {
        return abstractBranchEndRequest_ == null ?
            io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.getDefaultInstance() : abstractBranchEndRequest_;
      }
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndRequestProto abstractBranchEndRequest = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto, io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.Builder, io.seata.codec.protobuf.generated.AbstractBranchEndRequestProtoOrBuilder> 
        getAbstractBranchEndRequestFieldBuilder() {
      if (abstractBranchEndRequestBuilder_ == null) {
        abstractBranchEndRequestBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto, io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto.Builder, io.seata.codec.protobuf.generated.AbstractBranchEndRequestProtoOrBuilder>(
                getAbstractBranchEndRequest(),
                getParentForChildren(),
                isClean());
        abstractBranchEndRequest_ = null;
      }
      return abstractBranchEndRequestBuilder_;
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


    // @@protoc_insertion_point(builder_scope:io.seata.protocol.protobuf.BranchRollbackRequestProto)
  }

  // @@protoc_insertion_point(class_scope:io.seata.protocol.protobuf.BranchRollbackRequestProto)
  private static final io.seata.codec.protobuf.generated.BranchRollbackRequestProto DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.seata.codec.protobuf.generated.BranchRollbackRequestProto();
  }

  public static io.seata.codec.protobuf.generated.BranchRollbackRequestProto getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BranchRollbackRequestProto>
      PARSER = new com.google.protobuf.AbstractParser<BranchRollbackRequestProto>() {
    @java.lang.Override
    public BranchRollbackRequestProto parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new BranchRollbackRequestProto(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<BranchRollbackRequestProto> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<BranchRollbackRequestProto> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public io.seata.codec.protobuf.generated.BranchRollbackRequestProto getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

