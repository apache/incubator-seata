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
package org.apache.seata.core.rpc.processor.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.UnregisterRMRequest;
import org.apache.seata.core.protocol.UnregisterRMResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for UnregRmProcessor
 */
public class UnregRmProcessorTest {

    private UnregRmProcessor processor;
    private RemotingServer remotingServer;
    private ChannelHandlerContext ctx;
    private Channel channel;

    @BeforeEach
    public void setUp() {
        remotingServer = mock(RemotingServer.class);
        processor = new UnregRmProcessor(remotingServer);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
    }

    @AfterEach
    public void tearDown() {
        try {
            ChannelManager.releaseRpcContext(channel);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void processUnregisterSuccessTest() throws Exception {
        // First register the RM
        RegisterRMRequest registerRequest = new RegisterRMRequest();
        registerRequest.setApplicationId("test-app");
        registerRequest.setTransactionServiceGroup("test-group");
        registerRequest.setVersion("2.6.0");
        registerRequest.setResourceIds("jdbc:mysql://localhost:3306/db1");
        ChannelManager.registerRMChannel(registerRequest, channel);

        // Unregister
        UnregisterRMRequest unregRequest = new UnregisterRMRequest();
        unregRequest.setApplicationId("test-app");
        unregRequest.setTransactionServiceGroup("test-group");
        unregRequest.setResourceIds("jdbc:mysql://localhost:3306/db1");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(unregRequest);

        processor.process(ctx, rpcMessage);

        ArgumentCaptor<UnregisterRMResponse> responseCaptor = ArgumentCaptor.forClass(UnregisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        UnregisterRMResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertTrue(response.isIdentified());
    }

    @Test
    public void processUnregisterUnknownChannelTest() throws Exception {
        UnregisterRMRequest unregRequest = new UnregisterRMRequest();
        unregRequest.setApplicationId("test-app");
        unregRequest.setTransactionServiceGroup("test-group");
        unregRequest.setResourceIds("jdbc:mysql://localhost:3306/db1");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(unregRequest);

        processor.process(ctx, rpcMessage);

        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), any(UnregisterRMResponse.class));
    }
}
