/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.integration.grpc.interceptor;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.seata.core.context.RootContext;
import io.seata.integration.grpc.interceptor.client.ClientTransactionInterceptor;
import io.seata.integration.grpc.interceptor.proto.ContextRpcGrpc;
import io.seata.integration.grpc.interceptor.proto.Request;
import io.seata.integration.grpc.interceptor.proto.Response;
import io.seata.integration.grpc.interceptor.server.ServerTransactionInterceptor;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author eddyxu1213@126.com
 */
public class GrpcTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final ServerInterceptor mockServerInterceptor = mock(ServerInterceptor.class, delegatesTo(new ServerTransactionInterceptor()));


    @Test
    public void clientHeaderDeliveredToServer() throws Exception {

        String serverName = InProcessServerBuilder.generateName();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String[] context = {null};

        //executor
        Executor executorService = new ThreadPoolExecutor(2, 2, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "contextText-" + System.currentTimeMillis());
            }
        });

        //server
        grpcCleanup.register(InProcessServerBuilder.forName(serverName).executor(executorService)
            .addService(ServerInterceptors.intercept(new ContextRpcGrpc.ContextRpcImplBase() {
                @Override
                public void contextRpc(Request request, StreamObserver<Response> responseObserver) {
                    context[0] = RootContext.getXID();
                    countDownLatch.countDown();
                    responseObserver.onNext(Response.newBuilder().setGreet("hello! " + request.getName()).build());
                    responseObserver.onCompleted();
                }
            }, mockServerInterceptor))
            .build().start());

        //client
        ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).executor(executorService).build());
        ContextRpcGrpc.ContextRpcFutureStub stub = ContextRpcGrpc.newFutureStub(
            ClientInterceptors.intercept(channel, new ClientTransactionInterceptor()));
        RootContext.bind("test_context");
        ListenableFuture<Response> future = stub.contextRpc(Request.newBuilder().setName("seata").build());
        assertEquals("hello! seata", future.get().getGreet());

        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(mockServerInterceptor).interceptCall(ArgumentMatchers.any(), metadataCaptor.capture(), ArgumentMatchers.any());
        assertEquals("test_context", metadataCaptor.getValue().get(GrpcHeaderKey.HEADER_KEY));

        countDownLatch.await();
        assertEquals("test_context", context[0]);
    }
}
