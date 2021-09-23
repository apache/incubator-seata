package io.seata.core.protocol.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.39.0)",
    comments = "Source: seata_grpc.proto")
public final class ResourceManagerServiceGrpc {

  private ResourceManagerServiceGrpc() {}

  public static final String SERVICE_NAME = "grpc.ResourceManagerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse> getBranchRegisterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BranchRegister",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse> getBranchRegisterMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse> getBranchRegisterMethod;
    if ((getBranchRegisterMethod = ResourceManagerServiceGrpc.getBranchRegisterMethod) == null) {
      synchronized (ResourceManagerServiceGrpc.class) {
        if ((getBranchRegisterMethod = ResourceManagerServiceGrpc.getBranchRegisterMethod) == null) {
          ResourceManagerServiceGrpc.getBranchRegisterMethod = getBranchRegisterMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BranchRegister"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ResourceManagerServiceMethodDescriptorSupplier("BranchRegister"))
              .build();
        }
      }
    }
    return getBranchRegisterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse> getBranchReportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BranchReport",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest,
      io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse> getBranchReportMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse> getBranchReportMethod;
    if ((getBranchReportMethod = ResourceManagerServiceGrpc.getBranchReportMethod) == null) {
      synchronized (ResourceManagerServiceGrpc.class) {
        if ((getBranchReportMethod = ResourceManagerServiceGrpc.getBranchReportMethod) == null) {
          ResourceManagerServiceGrpc.getBranchReportMethod = getBranchReportMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest, io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BranchReport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ResourceManagerServiceMethodDescriptorSupplier("BranchReport"))
              .build();
        }
      }
    }
    return getBranchReportMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse> getLockQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LockQuery",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse> getLockQueryMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse> getLockQueryMethod;
    if ((getLockQueryMethod = ResourceManagerServiceGrpc.getLockQueryMethod) == null) {
      synchronized (ResourceManagerServiceGrpc.class) {
        if ((getLockQueryMethod = ResourceManagerServiceGrpc.getLockQueryMethod) == null) {
          ResourceManagerServiceGrpc.getLockQueryMethod = getLockQueryMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LockQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ResourceManagerServiceMethodDescriptorSupplier("LockQuery"))
              .build();
        }
      }
    }
    return getLockQueryMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ResourceManagerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ResourceManagerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ResourceManagerServiceStub>() {
        @java.lang.Override
        public ResourceManagerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ResourceManagerServiceStub(channel, callOptions);
        }
      };
    return ResourceManagerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ResourceManagerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ResourceManagerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ResourceManagerServiceBlockingStub>() {
        @java.lang.Override
        public ResourceManagerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ResourceManagerServiceBlockingStub(channel, callOptions);
        }
      };
    return ResourceManagerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ResourceManagerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ResourceManagerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ResourceManagerServiceFutureStub>() {
        @java.lang.Override
        public ResourceManagerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ResourceManagerServiceFutureStub(channel, callOptions);
        }
      };
    return ResourceManagerServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ResourceManagerServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void branchRegister(io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBranchRegisterMethod(), responseObserver);
    }

    /**
     */
    public void branchReport(io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBranchReportMethod(), responseObserver);
    }

    /**
     */
    public void lockQuery(io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLockQueryMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getBranchRegisterMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest,
                io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse>(
                  this, METHODID_BRANCH_REGISTER)))
          .addMethod(
            getBranchReportMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest,
                io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse>(
                  this, METHODID_BRANCH_REPORT)))
          .addMethod(
            getLockQueryMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest,
                io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse>(
                  this, METHODID_LOCK_QUERY)))
          .build();
    }
  }

  /**
   */
  public static final class ResourceManagerServiceStub extends io.grpc.stub.AbstractAsyncStub<ResourceManagerServiceStub> {
    private ResourceManagerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ResourceManagerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ResourceManagerServiceStub(channel, callOptions);
    }

    /**
     */
    public void branchRegister(io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBranchRegisterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void branchReport(io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBranchReportMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void lockQuery(io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLockQueryMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ResourceManagerServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ResourceManagerServiceBlockingStub> {
    private ResourceManagerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ResourceManagerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ResourceManagerServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse branchRegister(io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBranchRegisterMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse branchReport(io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBranchReportMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse lockQuery(io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLockQueryMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ResourceManagerServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ResourceManagerServiceFutureStub> {
    private ResourceManagerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ResourceManagerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ResourceManagerServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse> branchRegister(
        io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBranchRegisterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse> branchReport(
        io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBranchReportMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse> lockQuery(
        io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLockQueryMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_BRANCH_REGISTER = 0;
  private static final int METHODID_BRANCH_REPORT = 1;
  private static final int METHODID_LOCK_QUERY = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ResourceManagerServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ResourceManagerServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_BRANCH_REGISTER:
          serviceImpl.branchRegister((io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse>) responseObserver);
          break;
        case METHODID_BRANCH_REPORT:
          serviceImpl.branchReport((io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse>) responseObserver);
          break;
        case METHODID_LOCK_QUERY:
          serviceImpl.lockQuery((io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse>) responseObserver);
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

  private static abstract class ResourceManagerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ResourceManagerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.seata.core.protocol.grpc.SeataGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ResourceManagerService");
    }
  }

  private static final class ResourceManagerServiceFileDescriptorSupplier
      extends ResourceManagerServiceBaseDescriptorSupplier {
    ResourceManagerServiceFileDescriptorSupplier() {}
  }

  private static final class ResourceManagerServiceMethodDescriptorSupplier
      extends ResourceManagerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ResourceManagerServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ResourceManagerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ResourceManagerServiceFileDescriptorSupplier())
              .addMethod(getBranchRegisterMethod())
              .addMethod(getBranchReportMethod())
              .addMethod(getLockQueryMethod())
              .build();
        }
      }
    }
    return result;
  }
}
