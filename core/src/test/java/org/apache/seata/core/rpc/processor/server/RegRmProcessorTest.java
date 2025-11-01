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
import org.apache.seata.core.protocol.RegisterRMResponse;
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
 * Test for RegRmProcessor
 */
public class RegRmProcessorTest {

    private RegRmProcessor processor;
    private RemotingServer remotingServer;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private RpcMessage rpcMessage;
    private RegisterRMRequest request;

    @BeforeEach
    public void setUp() {
        remotingServer = mock(RemotingServer.class);
        processor = new RegRmProcessor(remotingServer);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");

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
    public void testProcessRmRegisterSuccess() throws Exception {
        // Execute
        processor.process(ctx, rpcMessage);

        // Verify response
        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isIdentified(), "RM should be successfully registered");
    }

    @Test
    public void testProcessRmRegisterWithMultipleResources() throws Exception {
        request.setResourceIds("jdbc:mysql://localhost:3306/db1,jdbc:mysql://localhost:3306/db2");

        processor.process(ctx, rpcMessage);

        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        assertTrue(response.isIdentified(), "RM with multiple resources should be registered");
    }

    @Test
    public void testProcessRmRegisterWithEmptyResourceIds() throws Exception {
        request.setResourceIds("");

        processor.process(ctx, rpcMessage);

        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        // Should succeed even with empty resource IDs
        assertTrue(response.isIdentified(), "RM should be registered even with empty resources");
    }

    @Test
    public void testProcessRmRegisterWithNullResourceIds() throws Exception {
        request.setResourceIds(null);

        processor.process(ctx, rpcMessage);

        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        assertTrue(response.isIdentified(), "RM should be registered even with null resources");
    }

    @Test
    public void testProcessRmRegisterWithAuthHandler() throws Exception {
        // Note: Auth handler is loaded via SPI, so we can't easily mock it
        // This test verifies the default behavior without auth handler
        processor.process(ctx, rpcMessage);

        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        assertTrue(response.isIdentified(), "RM should be registered when no auth handler");
    }

    @Test
    public void testProcessWithDifferentVersions() throws Exception {
        String[] versions = {"1.0.0", "1.5.0", "2.0.0"};

        for (String version : versions) {
            Channel versionChannel = mock(Channel.class);
            ChannelHandlerContext versionCtx = mock(ChannelHandlerContext.class);
            when(versionCtx.channel()).thenReturn(versionChannel);
            when(versionChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

            RegisterRMRequest versionRequest = new RegisterRMRequest();
            versionRequest.setApplicationId("test-app");
            versionRequest.setTransactionServiceGroup("test-group");
            versionRequest.setVersion(version);
            versionRequest.setResourceIds("jdbc:mysql://localhost:3306/seata");

            RpcMessage versionRpcMessage = new RpcMessage();
            versionRpcMessage.setId(1);
            versionRpcMessage.setBody(versionRequest);

            processor.process(versionCtx, versionRpcMessage);

            verify(remotingServer)
                    .sendAsyncResponse(eq(versionRpcMessage), eq(versionChannel), any(RegisterRMResponse.class));
        }
    }

    @Test
    public void testProcessMultipleRegistrations() throws Exception {
        // Register multiple RMs
        for (int i = 0; i < 3; i++) {
            Channel multiChannel = mock(Channel.class);
            ChannelHandlerContext multiCtx = mock(ChannelHandlerContext.class);
            when(multiCtx.channel()).thenReturn(multiChannel);
            when(multiChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0." + i, 8080 + i));

            RegisterRMRequest multiRequest = new RegisterRMRequest();
            multiRequest.setApplicationId("test-app-" + i);
            multiRequest.setTransactionServiceGroup("test-group");
            multiRequest.setVersion("1.5.0");
            multiRequest.setResourceIds("jdbc:mysql://localhost:3306/db" + i);

            RpcMessage multiRpcMessage = new RpcMessage();
            multiRpcMessage.setId(i);
            multiRpcMessage.setBody(multiRequest);

            processor.process(multiCtx, multiRpcMessage);
        }

        // Verify all registrations were processed
        verify(remotingServer, org.mockito.Mockito.times(3))
                .sendAsyncResponse(any(RpcMessage.class), any(Channel.class), any(RegisterRMResponse.class));
    }

    @Test
    public void testProcessWithEmptyApplicationId() throws Exception {
        request.setApplicationId("");

        processor.process(ctx, rpcMessage);

        // Should still process but may fail validation
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), any(RegisterRMResponse.class));
    }

    @Test
    public void testProcessWithNullTransactionServiceGroup() throws Exception {
        request.setTransactionServiceGroup(null);

        processor.process(ctx, rpcMessage);

        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), any(RegisterRMResponse.class));
    }

    @Test
    public void testProcessReRegisterSameChannel() throws Exception {
        // First registration
        processor.process(ctx, rpcMessage);

        // Second registration on same channel (should update resources)
        RegisterRMRequest secondRequest = new RegisterRMRequest();
        secondRequest.setApplicationId("test-app");
        secondRequest.setTransactionServiceGroup("test-group");
        secondRequest.setVersion("1.5.0");
        secondRequest.setResourceIds("jdbc:mysql://localhost:3306/new_db");

        RpcMessage secondRpcMessage = new RpcMessage();
        secondRpcMessage.setId(2);
        secondRpcMessage.setBody(secondRequest);

        processor.process(ctx, secondRpcMessage);

        // Verify both registrations were processed
        verify(remotingServer, org.mockito.Mockito.times(2))
                .sendAsyncResponse(any(RpcMessage.class), eq(channel), any(RegisterRMResponse.class));
    }
}
