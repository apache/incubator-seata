/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.rpc.netty.mockserver;

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.serializer.protobuf.generated.*;
import org.apache.seata.core.protocol.generated.SeataServiceGrpc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcTest {

    private static ManagedChannel channel;

    private static SeataServiceGrpc.SeataServiceStub seataServiceStub;

    @BeforeAll
    public static void before() {
        ConfigurationFactory.reload();
        ConfigurationTestHelper.putConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.valueOf(ProtocolTestConstants.MOCK_SERVER_PORT));
        MockServer.start(ProtocolTestConstants.MOCK_SERVER_PORT);
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();

        channel = ManagedChannelBuilder.forAddress("127.0.0.1", ProtocolTestConstants.MOCK_SERVER_PORT).usePlaintext().build();
        seataServiceStub = SeataServiceGrpc.newStub(channel);
    }

    @AfterAll
    public static void after() {
        //MockServer.close();
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();
    }

    private GrpcMessageProto getRegisterTMRequest() {
        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
                .setApplicationId("test-applicationId")
                .build();
        RegisterTMRequestProto registerTMRequestProto = RegisterTMRequestProto.newBuilder()
                .setAbstractIdentifyRequest(abstractIdentifyRequestProto)
                .build();

        return GrpcMessageProto.newBuilder().putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode())).setBody(Any.pack(registerTMRequestProto).toByteString()).build();
    }

    private GrpcMessageProto getGlobalBeginRequest() {
        GlobalBeginRequestProto globalBeginRequestProto = GlobalBeginRequestProto.newBuilder()
                .setTransactionName("test-transaction")
                .setTimeout(2000)
                .build();
        return GrpcMessageProto.newBuilder().putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode())).setBody(Any.pack(globalBeginRequestProto).toByteString()).build();
    }

    private GrpcMessageProto getBranchRegisterRequest() {
        BranchRegisterRequestProto branchRegisterRequestProto = BranchRegisterRequestProto.newBuilder()
                .setXid("1")
                .setLockKey("1")
                .setResourceId("test-resource")
                .setBranchType(BranchTypeProto.TCC)
                .setApplicationData("{\"mock\":\"mock\"}")
                .build();

        return GrpcMessageProto.newBuilder().putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode())).setBody(Any.pack(branchRegisterRequestProto).toByteString()).build();
    }

    private GrpcMessageProto getGlobalCommitRequest() {
        AbstractGlobalEndRequestProto globalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
                .setXid("1")
                .build();
        GlobalCommitRequestProto globalCommitRequestProto = GlobalCommitRequestProto.newBuilder()
                .setAbstractGlobalEndRequest(globalEndRequestProto)
                .build();

        return GrpcMessageProto.newBuilder().putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode())).setBody(Any.pack(globalCommitRequestProto).toByteString()).build();
    }

    private GrpcMessageProto getGlobalRollbackRequest() {
        AbstractGlobalEndRequestProto globalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
                .setXid("1")
                .build();
        GlobalRollbackRequestProto globalRollbackRequestProto = GlobalRollbackRequestProto.newBuilder()
                .setAbstractGlobalEndRequest(globalEndRequestProto)
                .build();

        return GrpcMessageProto.newBuilder().putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode())).setBody(Any.pack(globalRollbackRequestProto).toByteString()).build();
    }

    @Test
    public void testCommit() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(4);
        StreamObserver<GrpcMessageProto> streamObserver = new StreamObserver<GrpcMessageProto>() {
            @Override
            public void onNext(GrpcMessageProto grpcMessageProto) {
                System.out.println("receive : " + grpcMessageProto.toString());
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {

            }
        };

        StreamObserver<GrpcMessageProto> response = seataServiceStub.sendRequest(streamObserver);
        response.onNext(getRegisterTMRequest());
        response.onNext(getGlobalBeginRequest());
        response.onNext(getBranchRegisterRequest());
        response.onNext(getGlobalCommitRequest());

        response.onCompleted();

        countDownLatch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testRollback() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(4);
        StreamObserver<GrpcMessageProto> streamObserver = new StreamObserver<GrpcMessageProto>() {
            @Override
            public void onNext(GrpcMessageProto grpcMessageProto) {
                System.out.println("receive : " + grpcMessageProto.toString());
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {

            }
        };

        StreamObserver<GrpcMessageProto> response = seataServiceStub.sendRequest(streamObserver);
        response.onNext(getRegisterTMRequest());
        response.onNext(getGlobalBeginRequest());
        response.onNext(getBranchRegisterRequest());
        response.onNext(getGlobalRollbackRequest());

        response.onCompleted();

        countDownLatch.await(10, TimeUnit.SECONDS);
    }
}
