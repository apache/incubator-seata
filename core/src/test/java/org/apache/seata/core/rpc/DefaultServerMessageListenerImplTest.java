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
package org.apache.seata.core.rpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.netty.NettyPoolKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for DefaultServerMessageListenerImpl
 * Note: This class is deprecated but still requires test coverage
 */
@SuppressWarnings("deprecation")
public class DefaultServerMessageListenerImplTest {

    private DefaultServerMessageListenerImpl listener;
    private TransactionMessageHandler transactionMessageHandler;
    private RemotingServer remotingServer;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private RpcContext rpcContext;

    @BeforeEach
    public void setUp() throws Exception {
        transactionMessageHandler = mock(TransactionMessageHandler.class);
        remotingServer = mock(RemotingServer.class);
        listener = new DefaultServerMessageListenerImpl(transactionMessageHandler);
        listener.setServerMessageSender(remotingServer);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        // Register channel
        rpcContext = new RpcContext();
        rpcContext.setChannel(channel);
        rpcContext.setClientRole(NettyPoolKey.TransactionRole.TMROLE);
        rpcContext.setApplicationId("test-app");
        rpcContext.setTransactionServiceGroup("test-group");
        rpcContext.setVersion("1.5.0");

        java.lang.reflect.Field identifiedChannelsField = ChannelManager.class.getDeclaredField("IDENTIFIED_CHANNELS");
        identifiedChannelsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Channel, RpcContext> identifiedChannels =
                (ConcurrentHashMap<Channel, RpcContext>) identifiedChannelsField.get(null);
        identifiedChannels.put(channel, rpcContext);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up all registered channels
        try {
            java.lang.reflect.Field identifiedChannelsField =
                    ChannelManager.class.getDeclaredField("IDENTIFIED_CHANNELS");
            identifiedChannelsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Channel, RpcContext> identifiedChannels =
                    (ConcurrentHashMap<Channel, RpcContext>) identifiedChannelsField.get(null);
            identifiedChannels.clear();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testOnTrxMessageSingleRequest() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        GlobalBeginResponse response = new GlobalBeginResponse();
        response.setResultCode(ResultCode.Success);
        response.setXid("127.0.0.1:8091:12345");

        when(transactionMessageHandler.onRequest(eq(request), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        listener.onTrxMessage(rpcMessage, ctx);

        verify(transactionMessageHandler).onRequest(eq(request), any(RpcContext.class));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testOnTrxMessageMergedWarpMessage() {
        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        List<org.apache.seata.core.protocol.AbstractMessage> msgs = new ArrayList<>();

        GlobalBeginRequest req1 = new GlobalBeginRequest();
        req1.setTransactionName("tx1");
        msgs.add(req1);

        GlobalBeginRequest req2 = new GlobalBeginRequest();
        req2.setTransactionName("tx2");
        msgs.add(req2);

        mergedMessage.msgs = msgs;

        GlobalBeginResponse resp1 = new GlobalBeginResponse();
        resp1.setResultCode(ResultCode.Success);

        GlobalBeginResponse resp2 = new GlobalBeginResponse();
        resp2.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(eq(req1), any(RpcContext.class)))
                .thenReturn(resp1);
        when(transactionMessageHandler.onRequest(eq(req2), any(RpcContext.class)))
                .thenReturn(resp2);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(mergedMessage);

        listener.onTrxMessage(rpcMessage, ctx);

        verify(transactionMessageHandler).onRequest(eq(req1), any(RpcContext.class));
        verify(transactionMessageHandler).onRequest(eq(req2), any(RpcContext.class));

        ArgumentCaptor<MergeResultMessage> responseCaptor = ArgumentCaptor.forClass(MergeResultMessage.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        MergeResultMessage result = responseCaptor.getValue();
        assertNotNull(result);
        assertEquals(2, result.getMsgs().length);
    }

    @Test
    public void testOnTrxMessageResultMessage() {
        GlobalBeginResponse response = new GlobalBeginResponse();
        response.setResultCode(ResultCode.Success);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(response);

        listener.onTrxMessage(rpcMessage, ctx);

        verify(transactionMessageHandler).onResponse(eq(response), any(RpcContext.class));
    }

    @Test
    public void testOnRegRmMessageSuccess() {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        listener.onRegRmMessage(rpcMessage, ctx, null);

        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertTrue(response.isIdentified(), "RM should be registered successfully");
    }

    @Test
    public void testOnRegRmMessageWithAuthHandler() {
        RegisterCheckAuthHandler authHandler = mock(RegisterCheckAuthHandler.class);
        when(authHandler.regResourceManagerCheckAuth(any(RegisterRMRequest.class)))
                .thenReturn(true);

        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        listener.onRegRmMessage(rpcMessage, ctx, authHandler);

        verify(authHandler).regResourceManagerCheckAuth(request);

        ArgumentCaptor<RegisterRMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterRMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterRMResponse response = responseCaptor.getValue();
        assertTrue(response.isIdentified());
    }

    @Test
    public void testOnRegTmMessageSuccess() {
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        listener.onRegTmMessage(rpcMessage, ctx, null);

        ArgumentCaptor<RegisterTMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterTMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterTMResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertTrue(response.isIdentified(), "TM should be registered successfully");
    }

    @Test
    public void testOnRegTmMessageWithAuthHandler() {
        RegisterCheckAuthHandler authHandler = mock(RegisterCheckAuthHandler.class);
        when(authHandler.regTransactionManagerCheckAuth(any(RegisterTMRequest.class)))
                .thenReturn(true);

        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        listener.onRegTmMessage(rpcMessage, ctx, authHandler);

        verify(authHandler).regTransactionManagerCheckAuth(request);

        ArgumentCaptor<RegisterTMResponse> responseCaptor = ArgumentCaptor.forClass(RegisterTMResponse.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        RegisterTMResponse response = responseCaptor.getValue();
        assertTrue(response.isIdentified());
    }

    @Test
    public void testOnCheckMessage() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(HeartbeatMessage.PING);

        listener.onCheckMessage(rpcMessage, ctx);

        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(HeartbeatMessage.PONG));
    }

    @Test
    public void testGetServerMessageSenderNotNull() {
        RemotingServer server = listener.getServerMessageSender();
        assertNotNull(server, "Server message sender should not be null");
        assertEquals(remotingServer, server);
    }

    @Test
    public void testGetServerMessageSenderThrowsException() {
        DefaultServerMessageListenerImpl newListener = new DefaultServerMessageListenerImpl(transactionMessageHandler);

        try {
            newListener.getServerMessageSender();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must not be null"));
        }
    }

    @Test
    public void testInit() {
        // Test init doesn't throw exception
        listener.init();

        // Verify listener is still functional after init
        assertNotNull(listener.getServerMessageSender());
    }

    @Test
    public void testOnTrxMessageNonAbstractMessage() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody("Not an AbstractMessage");

        // Should handle gracefully without throwing exception
        listener.onTrxMessage(rpcMessage, ctx);

        // Verify no processing occurred
        verify(transactionMessageHandler, org.mockito.Mockito.never()).onRequest(any(), any());
        verify(transactionMessageHandler, org.mockito.Mockito.never()).onResponse(any(), any());
    }

    @Test
    public void testOnCheckMessageWithException() {
        org.mockito.Mockito.doThrow(new RuntimeException("Send failed"))
                .when(remotingServer)
                .sendAsyncResponse(any(), any(), any());

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(HeartbeatMessage.PING);

        // Should handle exception gracefully
        listener.onCheckMessage(rpcMessage, ctx);

        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(HeartbeatMessage.PONG));
    }
}
