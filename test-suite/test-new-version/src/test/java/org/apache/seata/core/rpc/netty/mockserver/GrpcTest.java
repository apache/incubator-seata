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
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.protocol.generated.SeataServiceGrpc;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.serializer.protobuf.GrpcSerializer;
import org.apache.seata.serializer.protobuf.generated.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class GrpcTest {

    private static ManagedChannel channel;

    private static SeataServiceGrpc.SeataServiceStub seataServiceStub;

    @BeforeAll
    public static void before() {
        ConfigurationFactory.reload();
        System.setProperty(
                ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.valueOf(ProtocolTestConstants.MOCK_SERVER_PORT));
        ConfigurationCache.clear();
        MockServer.start(ProtocolTestConstants.MOCK_SERVER_PORT);
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();

        RmClientTest.getRm("mock-action");
        channel = ManagedChannelBuilder.forAddress("127.0.0.1", ProtocolTestConstants.MOCK_SERVER_PORT)
                .usePlaintext()
                .build();
        seataServiceStub = SeataServiceGrpc.newStub(channel);
    }

    @AfterAll
    public static void after() throws InterruptedException {
        channel.shutdownNow();
        assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        // MockServer.close();
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        ConfigurationCache.clear();
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();
    }

    private GrpcMessageProto getRegisterTMRequest() {
        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
                .setApplicationId(ProtocolTestConstants.APPLICATION_ID)
                .setVersion(org.apache.seata.core.protocol.Version.getCurrent())
                .setTransactionServiceGroup(ProtocolTestConstants.SERVICE_GROUP)
                .build();
        RegisterTMRequestProto registerTMRequestProto = RegisterTMRequestProto.newBuilder()
                .setAbstractIdentifyRequest(abstractIdentifyRequestProto)
                .build();

        return GrpcMessageProto.newBuilder()
                .putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode()))
                .setBody(Any.pack(registerTMRequestProto).toByteString())
                .build();
    }

    private GrpcMessageProto getGlobalBeginRequest() {
        GlobalBeginRequestProto globalBeginRequestProto = GlobalBeginRequestProto.newBuilder()
                .setTransactionName("test-transaction")
                .setTimeout(2000)
                .build();
        return GrpcMessageProto.newBuilder()
                .putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode()))
                .setBody(Any.pack(globalBeginRequestProto).toByteString())
                .build();
    }

    private GrpcMessageProto getBranchRegisterRequest(String xid) {
        BranchRegisterRequestProto branchRegisterRequestProto = BranchRegisterRequestProto.newBuilder()
                .setXid(xid)
                .setLockKey("1")
                .setResourceId("mock-action")
                .setBranchType(BranchTypeProto.TCC)
                .setApplicationData("{\"mock\":\"mock\"}")
                .build();

        return GrpcMessageProto.newBuilder()
                .putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode()))
                .setBody(Any.pack(branchRegisterRequestProto).toByteString())
                .build();
    }

    private GrpcMessageProto getGlobalCommitRequest(String xid) {
        AbstractGlobalEndRequestProto globalEndRequestProto =
                AbstractGlobalEndRequestProto.newBuilder().setXid(xid).build();
        GlobalCommitRequestProto globalCommitRequestProto = GlobalCommitRequestProto.newBuilder()
                .setAbstractGlobalEndRequest(globalEndRequestProto)
                .build();

        return GrpcMessageProto.newBuilder()
                .putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode()))
                .setBody(Any.pack(globalCommitRequestProto).toByteString())
                .build();
    }

    private GrpcMessageProto getGlobalRollbackRequest(String xid) {
        AbstractGlobalEndRequestProto globalEndRequestProto =
                AbstractGlobalEndRequestProto.newBuilder().setXid(xid).build();
        GlobalRollbackRequestProto globalRollbackRequestProto = GlobalRollbackRequestProto.newBuilder()
                .setAbstractGlobalEndRequest(globalEndRequestProto)
                .build();

        return GrpcMessageProto.newBuilder()
                .putHeadMap(GrpcHeaderEnum.CODEC_TYPE.header, String.valueOf(SerializerType.GRPC.getCode()))
                .setBody(Any.pack(globalRollbackRequestProto).toByteString())
                .build();
    }

    @Test
    public void testCommit() throws Exception {
        assertTransactionCompletes(true);
    }

    @Test
    public void testRollback() throws Exception {
        assertTransactionCompletes(false);
    }

    private void assertTransactionCompletes(boolean commit) throws Exception {
        BlockingQueue<Object> responses = new LinkedBlockingQueue<>();
        GrpcSerializer serializer = new GrpcSerializer();
        StreamObserver<GrpcMessageProto> observer = new StreamObserver<GrpcMessageProto>() {
            @Override
            public void onNext(GrpcMessageProto message) {
                try {
                    Object response = serializer.deserialize(message.getBody().toByteArray());
                    responses.add(response);
                } catch (Throwable failure) {
                    responses.add(failure);
                }
            }

            @Override
            public void onError(Throwable failure) {
                responses.add(failure);
            }

            @Override
            public void onCompleted() {}
        };
        StreamObserver<GrpcMessageProto> requests = seataServiceStub.sendRequest(observer);
        try {
            RegisterTMResponse registration =
                    exchange(requests, responses, getRegisterTMRequest(), RegisterTMResponse.class);
            assertTrue(registration.isIdentified());
            GlobalBeginResponse begin =
                    exchange(requests, responses, getGlobalBeginRequest(), GlobalBeginResponse.class);
            String xid = begin.getXid();
            assertNotNull(xid);
            assertFalse(xid.isEmpty());
            BranchRegisterResponse branch =
                    exchange(requests, responses, getBranchRegisterRequest(xid), BranchRegisterResponse.class);
            assertTrue(branch.getBranchId() > 0);
            if (commit) {
                GlobalCommitResponse result =
                        exchange(requests, responses, getGlobalCommitRequest(xid), GlobalCommitResponse.class);
                assertEquals(GlobalStatus.Committed, result.getGlobalStatus());
                assertEquals(1, Action1Impl.getCommitTimes(xid));
                assertEquals(0, Action1Impl.getRollbackTimes(xid));
            } else {
                GlobalRollbackResponse result =
                        exchange(requests, responses, getGlobalRollbackRequest(xid), GlobalRollbackResponse.class);
                assertEquals(GlobalStatus.Rollbacked, result.getGlobalStatus());
                assertEquals(1, Action1Impl.getRollbackTimes(xid));
                assertEquals(0, Action1Impl.getCommitTimes(xid));
            }
        } finally {
            requests.onCompleted();
        }
    }

    private <T extends AbstractResultMessage> T exchange(
            StreamObserver<GrpcMessageProto> requests,
            BlockingQueue<Object> responses,
            GrpcMessageProto request,
            Class<T> type)
            throws InterruptedException {
        requests.onNext(request);
        Object response = responses.poll(10, TimeUnit.SECONDS);
        assertNotNull(response, "Timed out waiting for " + type.getSimpleName());
        if (response instanceof Throwable) {
            throw new AssertionError("gRPC request failed", (Throwable) response);
        }
        assertTrue(type.isInstance(response), "Unexpected response: " + response);
        T result = type.cast(response);
        assertEquals(ResultCode.Success, result.getResultCode(), result.toString());
        return result;
    }
}
