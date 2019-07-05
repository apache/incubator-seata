// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: branchCommitResponse.proto

package io.seata.codec.protobuf.generated;

/**
 * <pre>
 * PublishRequest is a publish request.
 * </pre>
 *
 * Protobuf type {@code io.seata.protocol.protobuf.BranchCommitResponseProto}
 */
public  final class BranchCommitResponseProto extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.seata.protocol.protobuf.BranchCommitResponseProto)
    BranchCommitResponseProtoOrBuilder {
private static final long serialVersionUID = 0L;
  // Use BranchCommitResponseProto.newBuilder() to construct.
  private BranchCommitResponseProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private BranchCommitResponseProto() {
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private BranchCommitResponseProto(
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
            io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.Builder subBuilder = null;
            if (abstractBranchEndResponse_ != null) {
              subBuilder = abstractBranchEndResponse_.toBuilder();
            }
            abstractBranchEndResponse_ = input.readMessage(io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(abstractBranchEndResponse_);
              abstractBranchEndResponse_ = subBuilder.buildPartial();
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
    return io.seata.codec.protobuf.generated.BranchCommitResponse.internal_static_io_seata_protocol_protobuf_BranchCommitResponseProto_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.seata.codec.protobuf.generated.BranchCommitResponse.internal_static_io_seata_protocol_protobuf_BranchCommitResponseProto_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.seata.codec.protobuf.generated.BranchCommitResponseProto.class, io.seata.codec.protobuf.generated.BranchCommitResponseProto.Builder.class);
  }

  public static final int ABSTRACTBRANCHENDRESPONSE_FIELD_NUMBER = 1;
  private io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto abstractBranchEndResponse_;
  /**
   * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
   */
  public boolean hasAbstractBranchEndResponse() {
    return abstractBranchEndResponse_ != null;
  }
  /**
   * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
   */
  public io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto getAbstractBranchEndResponse() {
    return abstractBranchEndResponse_ == null ? io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.getDefaultInstance() : abstractBranchEndResponse_;
  }
  /**
   * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
   */
  public io.seata.codec.protobuf.generated.AbstractBranchEndResponseProtoOrBuilder getAbstractBranchEndResponseOrBuilder() {
    return getAbstractBranchEndResponse();
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
    if (abstractBranchEndResponse_ != null) {
      output.writeMessage(1, getAbstractBranchEndResponse());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (abstractBranchEndResponse_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getAbstractBranchEndResponse());
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
    if (!(obj instanceof io.seata.codec.protobuf.generated.BranchCommitResponseProto)) {
      return super.equals(obj);
    }
    io.seata.codec.protobuf.generated.BranchCommitResponseProto other = (io.seata.codec.protobuf.generated.BranchCommitResponseProto) obj;

    if (hasAbstractBranchEndResponse() != other.hasAbstractBranchEndResponse()) return false;
    if (hasAbstractBranchEndResponse()) {
      if (!getAbstractBranchEndResponse()
          .equals(other.getAbstractBranchEndResponse())) return false;
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
    if (hasAbstractBranchEndResponse()) {
      hash = (37 * hash) + ABSTRACTBRANCHENDRESPONSE_FIELD_NUMBER;
      hash = (53 * hash) + getAbstractBranchEndResponse().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto parseFrom(
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
  public static Builder newBuilder(io.seata.codec.protobuf.generated.BranchCommitResponseProto prototype) {
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
   * Protobuf type {@code io.seata.protocol.protobuf.BranchCommitResponseProto}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.seata.protocol.protobuf.BranchCommitResponseProto)
      io.seata.codec.protobuf.generated.BranchCommitResponseProtoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.seata.codec.protobuf.generated.BranchCommitResponse.internal_static_io_seata_protocol_protobuf_BranchCommitResponseProto_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.seata.codec.protobuf.generated.BranchCommitResponse.internal_static_io_seata_protocol_protobuf_BranchCommitResponseProto_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.seata.codec.protobuf.generated.BranchCommitResponseProto.class, io.seata.codec.protobuf.generated.BranchCommitResponseProto.Builder.class);
    }

    // Construct using io.seata.codec.protobuf.generated.BranchCommitResponseProto.newBuilder()
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
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponse_ = null;
      } else {
        abstractBranchEndResponse_ = null;
        abstractBranchEndResponseBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.seata.codec.protobuf.generated.BranchCommitResponse.internal_static_io_seata_protocol_protobuf_BranchCommitResponseProto_descriptor;
    }

    @java.lang.Override
    public io.seata.codec.protobuf.generated.BranchCommitResponseProto getDefaultInstanceForType() {
      return io.seata.codec.protobuf.generated.BranchCommitResponseProto.getDefaultInstance();
    }

    @java.lang.Override
    public io.seata.codec.protobuf.generated.BranchCommitResponseProto build() {
      io.seata.codec.protobuf.generated.BranchCommitResponseProto result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public io.seata.codec.protobuf.generated.BranchCommitResponseProto buildPartial() {
      io.seata.codec.protobuf.generated.BranchCommitResponseProto result = new io.seata.codec.protobuf.generated.BranchCommitResponseProto(this);
      if (abstractBranchEndResponseBuilder_ == null) {
        result.abstractBranchEndResponse_ = abstractBranchEndResponse_;
      } else {
        result.abstractBranchEndResponse_ = abstractBranchEndResponseBuilder_.build();
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
      if (other instanceof io.seata.codec.protobuf.generated.BranchCommitResponseProto) {
        return mergeFrom((io.seata.codec.protobuf.generated.BranchCommitResponseProto)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.seata.codec.protobuf.generated.BranchCommitResponseProto other) {
      if (other == io.seata.codec.protobuf.generated.BranchCommitResponseProto.getDefaultInstance()) return this;
      if (other.hasAbstractBranchEndResponse()) {
        mergeAbstractBranchEndResponse(other.getAbstractBranchEndResponse());
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
      io.seata.codec.protobuf.generated.BranchCommitResponseProto parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.seata.codec.protobuf.generated.BranchCommitResponseProto) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto abstractBranchEndResponse_;
    private com.google.protobuf.SingleFieldBuilderV3<
        io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto, io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.Builder, io.seata.codec.protobuf.generated.AbstractBranchEndResponseProtoOrBuilder> abstractBranchEndResponseBuilder_;
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public boolean hasAbstractBranchEndResponse() {
      return abstractBranchEndResponseBuilder_ != null || abstractBranchEndResponse_ != null;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto getAbstractBranchEndResponse() {
      if (abstractBranchEndResponseBuilder_ == null) {
        return abstractBranchEndResponse_ == null ? io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.getDefaultInstance() : abstractBranchEndResponse_;
      } else {
        return abstractBranchEndResponseBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder setAbstractBranchEndResponse(io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto value) {
      if (abstractBranchEndResponseBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        abstractBranchEndResponse_ = value;
        onChanged();
      } else {
        abstractBranchEndResponseBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder setAbstractBranchEndResponse(
        io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.Builder builderForValue) {
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponse_ = builderForValue.build();
        onChanged();
      } else {
        abstractBranchEndResponseBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder mergeAbstractBranchEndResponse(io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto value) {
      if (abstractBranchEndResponseBuilder_ == null) {
        if (abstractBranchEndResponse_ != null) {
          abstractBranchEndResponse_ =
            io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.newBuilder(abstractBranchEndResponse_).mergeFrom(value).buildPartial();
        } else {
          abstractBranchEndResponse_ = value;
        }
        onChanged();
      } else {
        abstractBranchEndResponseBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public Builder clearAbstractBranchEndResponse() {
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponse_ = null;
        onChanged();
      } else {
        abstractBranchEndResponse_ = null;
        abstractBranchEndResponseBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.Builder getAbstractBranchEndResponseBuilder() {
      
      onChanged();
      return getAbstractBranchEndResponseFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    public io.seata.codec.protobuf.generated.AbstractBranchEndResponseProtoOrBuilder getAbstractBranchEndResponseOrBuilder() {
      if (abstractBranchEndResponseBuilder_ != null) {
        return abstractBranchEndResponseBuilder_.getMessageOrBuilder();
      } else {
        return abstractBranchEndResponse_ == null ?
            io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.getDefaultInstance() : abstractBranchEndResponse_;
      }
    }
    /**
     * <code>.io.seata.protocol.protobuf.AbstractBranchEndResponseProto abstractBranchEndResponse = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto, io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.Builder, io.seata.codec.protobuf.generated.AbstractBranchEndResponseProtoOrBuilder> 
        getAbstractBranchEndResponseFieldBuilder() {
      if (abstractBranchEndResponseBuilder_ == null) {
        abstractBranchEndResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto, io.seata.codec.protobuf.generated.AbstractBranchEndResponseProto.Builder, io.seata.codec.protobuf.generated.AbstractBranchEndResponseProtoOrBuilder>(
                getAbstractBranchEndResponse(),
                getParentForChildren(),
                isClean());
        abstractBranchEndResponse_ = null;
      }
      return abstractBranchEndResponseBuilder_;
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


    // @@protoc_insertion_point(builder_scope:io.seata.protocol.protobuf.BranchCommitResponseProto)
  }

  // @@protoc_insertion_point(class_scope:io.seata.protocol.protobuf.BranchCommitResponseProto)
  private static final io.seata.codec.protobuf.generated.BranchCommitResponseProto DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.seata.codec.protobuf.generated.BranchCommitResponseProto();
  }

  public static io.seata.codec.protobuf.generated.BranchCommitResponseProto getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BranchCommitResponseProto>
      PARSER = new com.google.protobuf.AbstractParser<BranchCommitResponseProto>() {
    @java.lang.Override
    public BranchCommitResponseProto parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new BranchCommitResponseProto(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<BranchCommitResponseProto> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<BranchCommitResponseProto> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public io.seata.codec.protobuf.generated.BranchCommitResponseProto getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

