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
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.netty.NettyPoolKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for ServerOnResponseProcessor
 */
public class ServerOnResponseProcessorTest {

    private ServerOnResponseProcessor processor;
    private TransactionMessageHandler transactionMessageHandler;
    private ConcurrentHashMap<Integer, MessageFuture> futures;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private RpcMessage rpcMessage;

    @BeforeEach
    public void setUp() throws Exception {
        transactionMessageHandler = mock(TransactionMessageHandler.class);
        futures = new ConcurrentHashMap<>();
        processor = new ServerOnResponseProcessor(transactionMessageHandler, futures);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        rpcMessage = new RpcMessage();
        rpcMessage.setId(1);

        // Register channel to avoid NPE in getContextFromIdentified
        RpcContext rpcContext = new RpcContext();
        rpcContext.setChannel(channel);
        rpcContext.setClientRole(NettyPoolKey.TransactionRole.RMROLE);
        rpcContext.setApplicationId("test-app");
        rpcContext.setTransactionServiceGroup("test-group");

        java.lang.reflect.Field identifiedChannelsField = ChannelManager.class.getDeclaredField("IDENTIFIED_CHANNELS");
        identifiedChannelsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Channel, RpcContext> identifiedChannels =
                (ConcurrentHashMap<Channel, RpcContext>) identifiedChannelsField.get(null);
        identifiedChannels.put(channel, rpcContext);
    }

    @AfterEach
    public void tearDown() {
        futures.clear();
        try {
            ChannelManager.releaseRpcContext(channel);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testProcessWithMessageFutureExists() throws Exception {
        // Setup message future
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(3000);
        futures.put(rpcMessage.getId(), messageFuture);

        BranchCommitResponse response = new BranchCommitResponse();
        response.setResultCode(ResultCode.Success);
        rpcMessage.setBody(response);

        // Execute
        processor.process(ctx, rpcMessage);

        // Verify future is removed and result is set
        assertEquals(0, futures.size(), "Future should be removed");
        // Verify result can be retrieved from future
        Object result = messageFuture.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(response, result, "Result should be set in future");
        verify(transactionMessageHandler, never()).onResponse(any(), any());
    }

    @Test
    public void testProcessWithMessageFutureNotExistsButChannelRegistered() throws Exception {
        // Channel is already registered in setUp
        BranchCommitResponse response = new BranchCommitResponse();
        response.setResultCode(ResultCode.Success);
        rpcMessage.setBody(response);

        // Execute
        processor.process(ctx, rpcMessage);

        // Verify transaction handler is called
        ArgumentCaptor<AbstractResultMessage> messageCaptor = ArgumentCaptor.forClass(AbstractResultMessage.class);
        ArgumentCaptor<RpcContext> contextCaptor = ArgumentCaptor.forClass(RpcContext.class);
        verify(transactionMessageHandler).onResponse(messageCaptor.capture(), contextCaptor.capture());

        assertSame(response, messageCaptor.getValue());
        assertNotNull(contextCaptor.getValue());
    }

    @Test
    public void testProcessWithChannelNotRegistered() throws Exception {
        // Create a new unregistered channel
        Channel unregisteredChannel = mock(Channel.class);
        ChannelHandlerContext unregisteredCtx = mock(ChannelHandlerContext.class);
        when(unregisteredCtx.channel()).thenReturn(unregisteredChannel);
        when(unregisteredChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.2", 8081));

        BranchCommitResponse response = new BranchCommitResponse();
        response.setResultCode(ResultCode.Success);
        rpcMessage.setBody(response);

        // Execute with unregistered channel - should disconnect and close without NPE
        processor.process(unregisteredCtx, rpcMessage);

        // Verify that disconnect and close were called
        verify(unregisteredCtx).disconnect();
        verify(unregisteredCtx).close();

        // Verify transaction handler was not called
        verify(transactionMessageHandler, never()).onResponse(any(), any());
    }

    @Test
    public void testProcessBranchRollbackResponse() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(3000);
        futures.put(rpcMessage.getId(), messageFuture);

        BranchRollbackResponse response = new BranchRollbackResponse();
        response.setResultCode(ResultCode.Success);
        rpcMessage.setBody(response);

        processor.process(ctx, rpcMessage);

        Object result = messageFuture.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(response, result);
    }

    @Test
    public void testProcessMultipleResponses() throws Exception {
        // Process multiple responses
        for (int i = 0; i < 3; i++) {
            RpcMessage msg = new RpcMessage();
            msg.setId(100 + i);
            MessageFuture future = new MessageFuture();
            future.setRequestMessage(msg);
            future.setTimeout(3000);
            futures.put(msg.getId(), future);

            BranchCommitResponse response = new BranchCommitResponse();
            response.setResultCode(ResultCode.Success);
            msg.setBody(response);

            processor.process(ctx, msg);

            Object result = future.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertEquals(response, result);
        }

        assertEquals(0, futures.size(), "All futures should be removed");
    }

    @Test
    public void testProcessWithFailedResponse() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(3000);
        futures.put(rpcMessage.getId(), messageFuture);

        BranchCommitResponse response = new BranchCommitResponse();
        response.setResultCode(ResultCode.Failed);
        response.setMsg("Commit failed");
        rpcMessage.setBody(response);

        processor.process(ctx, rpcMessage);

        Object result = messageFuture.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(response, result);
        assertEquals(ResultCode.Failed, ((BranchCommitResponse) result).getResultCode());
    }

    @Test
    public void testProcessWithNullBody() throws Exception {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(3000);
        futures.put(rpcMessage.getId(), messageFuture);

        rpcMessage.setBody(null);

        processor.process(ctx, rpcMessage);

        // Future should still be removed even with null body
        assertEquals(0, futures.size(), "Future should be removed");
    }

    @Test
    public void testProcessCloseChannelException() throws Exception {
        // Create an unregistered channel
        Channel unregisteredChannel = mock(Channel.class);
        ChannelHandlerContext unregisteredCtx = mock(ChannelHandlerContext.class);
        when(unregisteredCtx.channel()).thenReturn(unregisteredChannel);
        when(unregisteredChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.3", 8082));
        when(unregisteredChannel.toString()).thenReturn("Channel[127.0.0.3:8082]");
        when(unregisteredCtx.disconnect()).thenThrow(new RuntimeException("Disconnect failed"));

        BranchCommitResponse response = new BranchCommitResponse();
        rpcMessage.setBody(response);

        // Should handle disconnect exception gracefully without NPE
        processor.process(unregisteredCtx, rpcMessage);

        // Verify disconnect was attempted
        verify(unregisteredCtx).disconnect();
        // Verify close was still attempted despite disconnect failure
        verify(unregisteredCtx).close();
    }

    @Test
    public void testProcessCloseChannelExceptionOnClose() throws Exception {
        // Create an unregistered channel
        Channel unregisteredChannel = mock(Channel.class);
        ChannelHandlerContext unregisteredCtx = mock(ChannelHandlerContext.class);
        when(unregisteredCtx.channel()).thenReturn(unregisteredChannel);
        when(unregisteredChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.4", 8083));
        when(unregisteredChannel.toString()).thenReturn("Channel[127.0.0.4:8083]");
        when(unregisteredCtx.close()).thenThrow(new RuntimeException("Close failed"));

        BranchCommitResponse response = new BranchCommitResponse();
        rpcMessage.setBody(response);

        // Should handle close exception gracefully
        processor.process(unregisteredCtx, rpcMessage);

        // Verify disconnect was attempted
        verify(unregisteredCtx).disconnect();
        // Verify close was attempted and exception was caught
        verify(unregisteredCtx).close();
    }
}
