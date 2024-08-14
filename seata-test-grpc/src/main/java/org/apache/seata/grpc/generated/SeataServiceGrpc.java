package org.apache.seata.grpc.generated;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.27.1)",
    comments = "Source: grpcMessage.proto")
public final class SeataServiceGrpc {

  private SeataServiceGrpc() {}

  public static final String SERVICE_NAME = "org.apache.seata.core.protocol.SeataService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<GrpcMessageProto,
      GrpcMessageProto> getSendRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sendRequest",
      requestType = GrpcMessageProto.class,
      responseType = GrpcMessageProto.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<GrpcMessageProto,
      GrpcMessageProto> getSendRequestMethod() {
    io.grpc.MethodDescriptor<GrpcMessageProto, GrpcMessageProto> getSendRequestMethod;
    if ((getSendRequestMethod = SeataServiceGrpc.getSendRequestMethod) == null) {
      synchronized (SeataServiceGrpc.class) {
        if ((getSendRequestMethod = SeataServiceGrpc.getSendRequestMethod) == null) {
          SeataServiceGrpc.getSendRequestMethod = getSendRequestMethod =
              io.grpc.MethodDescriptor.<GrpcMessageProto, GrpcMessageProto>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sendRequest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GrpcMessageProto.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GrpcMessageProto.getDefaultInstance()))
              .setSchemaDescriptor(new SeataServiceMethodDescriptorSupplier("sendRequest"))
              .build();
        }
      }
    }
    return getSendRequestMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SeataServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SeataServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SeataServiceStub>() {
        @Override
        public SeataServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SeataServiceStub(channel, callOptions);
        }
      };
    return SeataServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SeataServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SeataServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SeataServiceBlockingStub>() {
        @Override
        public SeataServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SeataServiceBlockingStub(channel, callOptions);
        }
      };
    return SeataServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SeataServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SeataServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SeataServiceFutureStub>() {
        @Override
        public SeataServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SeataServiceFutureStub(channel, callOptions);
        }
      };
    return SeataServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class SeataServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<GrpcMessageProto> sendRequest(
        io.grpc.stub.StreamObserver<GrpcMessageProto> responseObserver) {
      return asyncUnimplementedStreamingCall(getSendRequestMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendRequestMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                GrpcMessageProto,
                GrpcMessageProto>(
                  this, METHODID_SEND_REQUEST)))
          .build();
    }
  }

  /**
   */
  public static final class SeataServiceStub extends io.grpc.stub.AbstractAsyncStub<SeataServiceStub> {
    private SeataServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SeataServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SeataServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<GrpcMessageProto> sendRequest(
        io.grpc.stub.StreamObserver<GrpcMessageProto> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getSendRequestMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class SeataServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<SeataServiceBlockingStub> {
    private SeataServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SeataServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SeataServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class SeataServiceFutureStub extends io.grpc.stub.AbstractFutureStub<SeataServiceFutureStub> {
    private SeataServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SeataServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SeataServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SEND_REQUEST = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SeataServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SeataServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_REQUEST:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sendRequest(
              (io.grpc.stub.StreamObserver<GrpcMessageProto>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class SeataServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SeataServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return GrpcMessage.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SeataService");
    }
  }

  private static final class SeataServiceFileDescriptorSupplier
      extends SeataServiceBaseDescriptorSupplier {
    SeataServiceFileDescriptorSupplier() {}
  }

  private static final class SeataServiceMethodDescriptorSupplier
      extends SeataServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SeataServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (SeataServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SeataServiceFileDescriptorSupplier())
              .addMethod(getSendRequestMethod())
              .build();
        }
      }
    }
    return result;
  }
}
