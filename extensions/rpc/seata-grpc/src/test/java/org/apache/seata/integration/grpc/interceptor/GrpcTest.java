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
package org.apache.seata.integration.grpc.interceptor;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.grpc.interceptor.client.ClientTransactionInterceptor;
import org.apache.seata.integration.grpc.interceptor.proto.ContextRpcGrpc;
import org.apache.seata.integration.grpc.interceptor.proto.Request;
import org.apache.seata.integration.grpc.interceptor.proto.Response;
import org.apache.seata.integration.grpc.interceptor.server.ServerTransactionInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GrpcTest {

    private final ServerInterceptor mockServerInterceptor =
            mock(ServerInterceptor.class, delegatesTo(new ServerTransactionInterceptor()));
    private static final String XID = "192.168.0.1:8091:10086";
    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void tearDown() throws InterruptedException {
        RootContext.unbind();
        RootContext.unbindBranchType();
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void clientHeaderDeliveredToServer() throws Exception {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        String[] context = new String[] {null, null};

        // server
        server = NettyServerBuilder.forPort(0)
                .directExecutor()
                .addService(ServerInterceptors.intercept(
                        new ContextRpcGrpc.ContextRpcImplBase() {
                            @Override
                            public void contextRpc(Request request, StreamObserver<Response> responseObserver) {
                                context[0] = RootContext.getXID();
                                context[1] = RootContext.getBranchType().name();
                                countDownLatch.countDown();
                                responseObserver.onNext(Response.newBuilder()
                                        .setGreet("hello! " + request.getName())
                                        .build());
                                responseObserver.onCompleted();
                            }
                        },
                        mockServerInterceptor))
                .build()
                .start();

        // client
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort())
                .usePlaintext()
                .directExecutor()
                .build();
        ContextRpcGrpc.ContextRpcFutureStub stub =
                ContextRpcGrpc.newFutureStub(ClientInterceptors.intercept(channel, new ClientTransactionInterceptor()));
        RootContext.bind(XID);
        RootContext.bindBranchType(BranchType.TCC);
        ListenableFuture<Response> future =
                stub.contextRpc(Request.newBuilder().setName("seata").build());
        assertEquals("hello! seata", future.get().getGreet());

        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(mockServerInterceptor)
                .interceptCall(ArgumentMatchers.any(), metadataCaptor.capture(), ArgumentMatchers.any());
        assertEquals(XID, metadataCaptor.getValue().get(GrpcHeaderKey.XID_HEADER_KEY));
        assertEquals(BranchType.TCC.name(), metadataCaptor.getValue().get(GrpcHeaderKey.BRANCH_HEADER_KEY));

        assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
        assertEquals(XID, context[0]);
        assertEquals(BranchType.TCC.name(), context[1]);
    }
}
