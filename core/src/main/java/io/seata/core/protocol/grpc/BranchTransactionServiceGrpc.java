package io.seata.core.protocol.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.39.0)",
    comments = "Source: seata_grpc.proto")
public final class BranchTransactionServiceGrpc {

  private BranchTransactionServiceGrpc() {}

  public static final String SERVICE_NAME = "grpc.BranchTransactionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse> getBranchCommitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BranchCommit",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse> getBranchCommitMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse> getBranchCommitMethod;
    if ((getBranchCommitMethod = BranchTransactionServiceGrpc.getBranchCommitMethod) == null) {
      synchronized (BranchTransactionServiceGrpc.class) {
        if ((getBranchCommitMethod = BranchTransactionServiceGrpc.getBranchCommitMethod) == null) {
          BranchTransactionServiceGrpc.getBranchCommitMethod = getBranchCommitMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BranchCommit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BranchTransactionServiceMethodDescriptorSupplier("BranchCommit"))
              .build();
        }
      }
    }
    return getBranchCommitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse> getBranchRollbackMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BranchRollback",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse> getBranchRollbackMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse> getBranchRollbackMethod;
    if ((getBranchRollbackMethod = BranchTransactionServiceGrpc.getBranchRollbackMethod) == null) {
      synchronized (BranchTransactionServiceGrpc.class) {
        if ((getBranchRollbackMethod = BranchTransactionServiceGrpc.getBranchRollbackMethod) == null) {
          BranchTransactionServiceGrpc.getBranchRollbackMethod = getBranchRollbackMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BranchRollback"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BranchTransactionServiceMethodDescriptorSupplier("BranchRollback"))
              .build();
        }
      }
    }
    return getBranchRollbackMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BranchTransactionServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BranchTransactionServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BranchTransactionServiceStub>() {
        @java.lang.Override
        public BranchTransactionServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BranchTransactionServiceStub(channel, callOptions);
        }
      };
    return BranchTransactionServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BranchTransactionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BranchTransactionServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BranchTransactionServiceBlockingStub>() {
        @java.lang.Override
        public BranchTransactionServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BranchTransactionServiceBlockingStub(channel, callOptions);
        }
      };
    return BranchTransactionServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static BranchTransactionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BranchTransactionServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BranchTransactionServiceFutureStub>() {
        @java.lang.Override
        public BranchTransactionServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BranchTransactionServiceFutureStub(channel, callOptions);
        }
      };
    return BranchTransactionServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class BranchTransactionServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void branchCommit(io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBranchCommitMethod(), responseObserver);
    }

    /**
     */
    public void branchRollback(io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBranchRollbackMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getBranchCommitMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest,
                io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse>(
                  this, METHODID_BRANCH_COMMIT)))
          .addMethod(
            getBranchRollbackMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest,
                io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse>(
                  this, METHODID_BRANCH_ROLLBACK)))
          .build();
    }
  }

  /**
   */
  public static final class BranchTransactionServiceStub extends io.grpc.stub.AbstractAsyncStub<BranchTransactionServiceStub> {
    private BranchTransactionServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BranchTransactionServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BranchTransactionServiceStub(channel, callOptions);
    }

    /**
     */
    public void branchCommit(io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBranchCommitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void branchRollback(io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBranchRollbackMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class BranchTransactionServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<BranchTransactionServiceBlockingStub> {
    private BranchTransactionServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BranchTransactionServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BranchTransactionServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse branchCommit(io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBranchCommitMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse branchRollback(io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBranchRollbackMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class BranchTransactionServiceFutureStub extends io.grpc.stub.AbstractFutureStub<BranchTransactionServiceFutureStub> {
    private BranchTransactionServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BranchTransactionServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BranchTransactionServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse> branchCommit(
        io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBranchCommitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse> branchRollback(
        io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBranchRollbackMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_BRANCH_COMMIT = 0;
  private static final int METHODID_BRANCH_ROLLBACK = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BranchTransactionServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BranchTransactionServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_BRANCH_COMMIT:
          serviceImpl.branchCommit((io.seata.core.protocol.grpc.SeataGrpc.BranchCommitRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchCommitResponse>) responseObserver);
          break;
        case METHODID_BRANCH_ROLLBACK:
          serviceImpl.branchRollback((io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchRollbackResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class BranchTransactionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BranchTransactionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.seata.core.protocol.grpc.SeataGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BranchTransactionService");
    }
  }

  private static final class BranchTransactionServiceFileDescriptorSupplier
      extends BranchTransactionServiceBaseDescriptorSupplier {
    BranchTransactionServiceFileDescriptorSupplier() {}
  }

  private static final class BranchTransactionServiceMethodDescriptorSupplier
      extends BranchTransactionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BranchTransactionServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (BranchTransactionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BranchTransactionServiceFileDescriptorSupplier())
              .addMethod(getBranchCommitMethod())
              .addMethod(getBranchRollbackMethod())
              .build();
        }
      }
    }
    return result;
  }
}
