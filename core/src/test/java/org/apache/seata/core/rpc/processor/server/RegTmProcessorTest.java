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
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
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
 * Test for RegTmProcessor
 */
public class RegTmProcessorTest {

    private RegTmProcessor processor;
    private RemotingServer remotingServer;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private RpcMessage rpcMessage;
    private RegisterTMRequest request;

    @BeforeEach
    public void setUp() {
        remotingServer = mock(RemotingServer.class);
        processor = new RegTmProcessor(remotingServer);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");

        rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);
    }

    @AfterEach
    public void tearDown() {
        // Clean up any registered channels
        try {
            org.apache.seata.core.rpc.netty.ChannelManager.releaseRpcContext(channel);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testProcessTmRegisterSuccess() throws Exception {
        // Execute
        processor.process(ctx, rpcMessage);

        // Verify response
        ArgumentCaptor<RegisterTMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterTMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterTMResponse response = responseCaptor.getValue();
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isIdentified(), "TM should be successfully registered");
    }

    @Test
    public void testProcessTmRegisterWithAuthHandler() throws Exception {
        // Note: Auth handler is loaded via SPI, so we can't easily mock it
        // This test verifies the default behavior without auth handler
        processor.process(ctx, rpcMessage);

        ArgumentCaptor<RegisterTMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterTMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterTMResponse response = responseCaptor.getValue();
        assertTrue(response.isIdentified(), "TM should be registered when no auth handler");
    }

    @Test
    public void testProcessWithDifferentVersions() throws Exception {
        String[] versions = {"1.0.0", "1.5.0", "2.0.0"};

        for (String version : versions) {
            Channel versionChannel = mock(Channel.class);
            ChannelHandlerContext versionCtx = mock(ChannelHandlerContext.class);
            when(versionCtx.channel()).thenReturn(versionChannel);
            when(versionChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

            RegisterTMRequest versionRequest = new RegisterTMRequest();
            versionRequest.setApplicationId("test-app");
            versionRequest.setTransactionServiceGroup("test-group");
            versionRequest.setVersion(version);

            RpcMessage versionRpcMessage = new RpcMessage();
            versionRpcMessage.setId(1);
            versionRpcMessage.setBody(versionRequest);

            processor.process(versionCtx, versionRpcMessage);

            verify(remotingServer)
                    .sendAsyncResponse(eq(versionRpcMessage), eq(versionChannel), any(RegisterTMResponse.class));
        }
    }

    @Test
    public void testProcessWithEmptyApplicationId() throws Exception {
        request.setApplicationId("");

        processor.process(ctx, rpcMessage);

        // Should still process but may fail validation
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), any(RegisterTMResponse.class));
    }

    @Test
    public void testProcessWithNullTransactionServiceGroup() throws Exception {
        request.setTransactionServiceGroup(null);

        processor.process(ctx, rpcMessage);

        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), any(RegisterTMResponse.class));
    }
}
