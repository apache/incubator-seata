// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: abstractGlobalEndRequest.proto

package io.seata.core.protocol.protobuf;

public final class AbstractGlobalEndRequest {
  private AbstractGlobalEndRequest() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_io_seata_protocol_protobuf_AbstractGlobalEndRequestProto_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_seata_protocol_protobuf_AbstractGlobalEndRequestProto_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\036abstractGlobalEndRequest.proto\022\032io.sea" +
      "ta.protocol.protobuf\032 abstractTransactio" +
      "nRequest.proto\032\022branchStatus.proto\"\240\001\n\035A" +
      "bstractGlobalEndRequestProto\022_\n\032abstract" +
      "TransactionRequest\030\001 \001(\0132;.io.seata.prot" +
      "ocol.protobuf.AbstractTransactionRequest" +
      "Proto\022\013\n\003xid\030\002 \001(\t\022\021\n\textraData\030\003 \001(\tB=\n" +
      "\037io.seata.core.protocol.protobufB\030Abstra" +
      "ctGlobalEndRequestP\001b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          io.seata.core.protocol.protobuf.AbstractTransactionRequest.getDescriptor(),
          io.seata.core.protocol.protobuf.BranchStatus.getDescriptor(),
        }, assigner);
    internal_static_io_seata_protocol_protobuf_AbstractGlobalEndRequestProto_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_io_seata_protocol_protobuf_AbstractGlobalEndRequestProto_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_io_seata_protocol_protobuf_AbstractGlobalEndRequestProto_descriptor,
        new java.lang.String[] { "AbstractTransactionRequest", "Xid", "ExtraData", });
    io.seata.core.protocol.protobuf.AbstractTransactionRequest.getDescriptor();
    io.seata.core.protocol.protobuf.BranchStatus.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
