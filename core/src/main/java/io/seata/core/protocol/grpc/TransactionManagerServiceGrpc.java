package io.seata.core.protocol.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.39.0)",
    comments = "Source: seata_grpc.proto")
public final class TransactionManagerServiceGrpc {

  private TransactionManagerServiceGrpc() {}

  public static final String SERVICE_NAME = "grpc.TransactionManagerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse> getBeginMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Begin",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse> getBeginMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse> getBeginMethod;
    if ((getBeginMethod = TransactionManagerServiceGrpc.getBeginMethod) == null) {
      synchronized (TransactionManagerServiceGrpc.class) {
        if ((getBeginMethod = TransactionManagerServiceGrpc.getBeginMethod) == null) {
          TransactionManagerServiceGrpc.getBeginMethod = getBeginMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Begin"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TransactionManagerServiceMethodDescriptorSupplier("Begin"))
              .build();
        }
      }
    }
    return getBeginMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse> getGetStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetStatus",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse> getGetStatusMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse> getGetStatusMethod;
    if ((getGetStatusMethod = TransactionManagerServiceGrpc.getGetStatusMethod) == null) {
      synchronized (TransactionManagerServiceGrpc.class) {
        if ((getGetStatusMethod = TransactionManagerServiceGrpc.getGetStatusMethod) == null) {
          TransactionManagerServiceGrpc.getGetStatusMethod = getGetStatusMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TransactionManagerServiceMethodDescriptorSupplier("GetStatus"))
              .build();
        }
      }
    }
    return getGetStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse> getGlobalReportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GlobalReport",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse> getGlobalReportMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse> getGlobalReportMethod;
    if ((getGlobalReportMethod = TransactionManagerServiceGrpc.getGlobalReportMethod) == null) {
      synchronized (TransactionManagerServiceGrpc.class) {
        if ((getGlobalReportMethod = TransactionManagerServiceGrpc.getGlobalReportMethod) == null) {
          TransactionManagerServiceGrpc.getGlobalReportMethod = getGlobalReportMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GlobalReport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TransactionManagerServiceMethodDescriptorSupplier("GlobalReport"))
              .build();
        }
      }
    }
    return getGlobalReportMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse> getCommitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Commit",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse> getCommitMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse> getCommitMethod;
    if ((getCommitMethod = TransactionManagerServiceGrpc.getCommitMethod) == null) {
      synchronized (TransactionManagerServiceGrpc.class) {
        if ((getCommitMethod = TransactionManagerServiceGrpc.getCommitMethod) == null) {
          TransactionManagerServiceGrpc.getCommitMethod = getCommitMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Commit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TransactionManagerServiceMethodDescriptorSupplier("Commit"))
              .build();
        }
      }
    }
    return getCommitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse> getRollbackMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Rollback",
      requestType = io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest.class,
      responseType = io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest,
      io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse> getRollbackMethod() {
    io.grpc.MethodDescriptor<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse> getRollbackMethod;
    if ((getRollbackMethod = TransactionManagerServiceGrpc.getRollbackMethod) == null) {
      synchronized (TransactionManagerServiceGrpc.class) {
        if ((getRollbackMethod = TransactionManagerServiceGrpc.getRollbackMethod) == null) {
          TransactionManagerServiceGrpc.getRollbackMethod = getRollbackMethod =
              io.grpc.MethodDescriptor.<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest, io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Rollback"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TransactionManagerServiceMethodDescriptorSupplier("Rollback"))
              .build();
        }
      }
    }
    return getRollbackMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TransactionManagerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TransactionManagerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TransactionManagerServiceStub>() {
        @java.lang.Override
        public TransactionManagerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TransactionManagerServiceStub(channel, callOptions);
        }
      };
    return TransactionManagerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TransactionManagerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TransactionManagerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TransactionManagerServiceBlockingStub>() {
        @java.lang.Override
        public TransactionManagerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TransactionManagerServiceBlockingStub(channel, callOptions);
        }
      };
    return TransactionManagerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TransactionManagerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TransactionManagerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TransactionManagerServiceFutureStub>() {
        @java.lang.Override
        public TransactionManagerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TransactionManagerServiceFutureStub(channel, callOptions);
        }
      };
    return TransactionManagerServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class TransactionManagerServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void begin(io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBeginMethod(), responseObserver);
    }

    /**
     */
    public void getStatus(io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetStatusMethod(), responseObserver);
    }

    /**
     */
    public void globalReport(io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGlobalReportMethod(), responseObserver);
    }

    /**
     */
    public void commit(io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCommitMethod(), responseObserver);
    }

    /**
     */
    public void rollback(io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRollbackMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getBeginMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest,
                io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse>(
                  this, METHODID_BEGIN)))
          .addMethod(
            getGetStatusMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest,
                io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse>(
                  this, METHODID_GET_STATUS)))
          .addMethod(
            getGlobalReportMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest,
                io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse>(
                  this, METHODID_GLOBAL_REPORT)))
          .addMethod(
            getCommitMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest,
                io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse>(
                  this, METHODID_COMMIT)))
          .addMethod(
            getRollbackMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest,
                io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse>(
                  this, METHODID_ROLLBACK)))
          .build();
    }
  }

  /**
   */
  public static final class TransactionManagerServiceStub extends io.grpc.stub.AbstractAsyncStub<TransactionManagerServiceStub> {
    private TransactionManagerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TransactionManagerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TransactionManagerServiceStub(channel, callOptions);
    }

    /**
     */
    public void begin(io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBeginMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getStatus(io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void globalReport(io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGlobalReportMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commit(io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCommitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void rollback(io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest request,
        io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRollbackMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class TransactionManagerServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<TransactionManagerServiceBlockingStub> {
    private TransactionManagerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TransactionManagerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TransactionManagerServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse begin(io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBeginMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse getStatus(io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetStatusMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse globalReport(io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGlobalReportMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse commit(io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCommitMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse rollback(io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRollbackMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class TransactionManagerServiceFutureStub extends io.grpc.stub.AbstractFutureStub<TransactionManagerServiceFutureStub> {
    private TransactionManagerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TransactionManagerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TransactionManagerServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse> begin(
        io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBeginMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse> getStatus(
        io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetStatusMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse> globalReport(
        io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGlobalReportMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse> commit(
        io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCommitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse> rollback(
        io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRollbackMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_BEGIN = 0;
  private static final int METHODID_GET_STATUS = 1;
  private static final int METHODID_GLOBAL_REPORT = 2;
  private static final int METHODID_COMMIT = 3;
  private static final int METHODID_ROLLBACK = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final TransactionManagerServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(TransactionManagerServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_BEGIN:
          serviceImpl.begin((io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse>) responseObserver);
          break;
        case METHODID_GET_STATUS:
          serviceImpl.getStatus((io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse>) responseObserver);
          break;
        case METHODID_GLOBAL_REPORT:
          serviceImpl.globalReport((io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse>) responseObserver);
          break;
        case METHODID_COMMIT:
          serviceImpl.commit((io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse>) responseObserver);
          break;
        case METHODID_ROLLBACK:
          serviceImpl.rollback((io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest) request,
              (io.grpc.stub.StreamObserver<io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse>) responseObserver);
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

  private static abstract class TransactionManagerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TransactionManagerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.seata.core.protocol.grpc.SeataGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TransactionManagerService");
    }
  }

  private static final class TransactionManagerServiceFileDescriptorSupplier
      extends TransactionManagerServiceBaseDescriptorSupplier {
    TransactionManagerServiceFileDescriptorSupplier() {}
  }

  private static final class TransactionManagerServiceMethodDescriptorSupplier
      extends TransactionManagerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    TransactionManagerServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (TransactionManagerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TransactionManagerServiceFileDescriptorSupplier())
              .addMethod(getBeginMethod())
              .addMethod(getGetStatusMethod())
              .addMethod(getGlobalReportMethod())
              .addMethod(getCommitMethod())
              .addMethod(getRollbackMethod())
              .build();
        }
      }
    }
    return result;
  }
}
