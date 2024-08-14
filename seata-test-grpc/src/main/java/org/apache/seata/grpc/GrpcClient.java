package org.apache.seata.grpc;

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.seata.core.serializer.protobuf.generated.AbstractIdentifyRequestProto;
import org.apache.seata.core.serializer.protobuf.generated.RegisterTMRequestProto;
import org.apache.seata.grpc.generated.GrpcMessageProto;
import org.apache.seata.grpc.generated.SeataServiceGrpc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

    private final ManagedChannel channel;
    private final SeataServiceGrpc.SeataServiceStub seataServiceStub;

    public GrpcClient(String localhost, int port) {
        this(ManagedChannelBuilder.forAddress(localhost, port).usePlaintext().build());
    }

    public GrpcClient(ManagedChannel channel) {
        this.channel = channel;
        seataServiceStub = SeataServiceGrpc.newStub(channel);
    }

    public static void main(String[] args) {
        GrpcClient grpcClient = new GrpcClient("localhost", 8091);
        grpcClient.doRequest();
    }

    private void doRequest() {
        final CountDownLatch finish = new CountDownLatch(1);

        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
                .setApplicationId("test-grpc")
                .build();
        RegisterTMRequestProto registerTMRequestProto = RegisterTMRequestProto.newBuilder()
                .setAbstractIdentifyRequest(abstractIdentifyRequestProto)
                .build();

        GrpcMessageProto request = GrpcMessageProto.newBuilder().setBody(Any.pack(registerTMRequestProto)).build();
        StreamObserver<GrpcMessageProto> response;

        try {

            response = seataServiceStub.sendRequest(new StreamObserver<GrpcMessageProto>() {
                @Override
                public void onNext(GrpcMessageProto grpcMessageProto) {
                    System.out.println("receive : " + grpcMessageProto.toString());
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });

            response.onNext(request);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            finish.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
