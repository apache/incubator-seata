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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.netty.NettyPoolKey.TransactionRole;
import org.apache.seata.core.rpc.processor.Pair;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for AbstractNettyRemotingServer
 */
public class AbstractNettyRemotingServerTest {

    private TestNettyRemotingServer server;
    private ThreadPoolExecutor messageExecutor;
    private NettyServerConfig serverConfig;

    @BeforeEach
    public void setUp() {
        serverConfig = new NettyServerConfig();
        messageExecutor = new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("test", 1));
        server = new TestNettyRemotingServer(messageExecutor, serverConfig);
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            try {
                server.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (messageExecutor != null) {
            messageExecutor.shutdown();
        }
    }

    @Test
    public void testSendSyncRequestWithResourceIdSuccess() throws Exception {
        String resourceId = "testResource";
        String clientId = "testClient";
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getChannel(resourceId, clientId, false))
                    .thenReturn(channel);

            assertThrows(Exception.class, () -> {
                server.sendSyncRequest(resourceId, clientId, request, false);
            });
        }
    }

    @Test
    public void testSendSyncRequestWithResourceIdChannelNotFound() {
        String resourceId = "testResource";
        String clientId = "testClient";
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getChannel(resourceId, clientId, false))
                    .thenReturn(null);

            IOException exception = assertThrows(IOException.class, () -> {
                server.sendSyncRequest(resourceId, clientId, request, false);
            });

            assertTrue(exception.getMessage().contains("rm client is not connected"));
            assertTrue(exception.getMessage().contains(resourceId));
            assertTrue(exception.getMessage().contains(clientId));
        }
    }

    @Test
    public void testSendSyncRequestWithResourceIdTryOtherApp() {
        String resourceId = "testResource";
        String clientId = "testClient";
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getChannel(resourceId, clientId, true))
                    .thenReturn(null);

            IOException exception = assertThrows(IOException.class, () -> {
                server.sendSyncRequest(resourceId, clientId, request, true);
            });

            assertTrue(exception.getMessage().contains("rm client is not connected"));
        }
    }

    @Test
    public void testSendSyncRequestWithNullChannel() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        IOException exception = assertThrows(IOException.class, () -> {
            server.sendSyncRequest(null, request);
        });

        assertTrue(exception.getMessage().contains("client is not connected"));
    }

    @Test
    public void testSendSyncRequestWithValidChannel() throws Exception {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        assertThrows(Exception.class, () -> {
            server.sendSyncRequest(channel, request);
        });
    }

    @Test
    public void testSendAsyncRequestWithNullChannel() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        IOException exception = assertThrows(IOException.class, () -> {
            server.sendAsyncRequest(null, request);
        });

        assertTrue(exception.getMessage().contains("client is not connected"));
    }

    @Test
    public void testSendAsyncRequestSuccess() throws Exception {
        Channel channel = mock(Channel.class);
        ChannelPromise promise = mock(ChannelPromise.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel.writeAndFlush(any())).thenReturn(promise);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        server.sendAsyncRequest(channel, request);

        verify(channel, times(1)).writeAndFlush(any(RpcMessage.class));
    }

    @Test
    public void testSendAsyncResponseWithHeartbeat() {
        Channel channel = mock(Channel.class);
        ChannelPromise promise = mock(ChannelPromise.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel.writeAndFlush(any())).thenReturn(promise);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);

        HeartbeatMessage heartbeat = HeartbeatMessage.PING;

        server.sendAsyncResponse(rpcMessage, channel, heartbeat);

        verify(channel, times(1)).writeAndFlush(any(RpcMessage.class));
    }

    @Test
    public void testSendAsyncResponseWithNormalMessage() {
        Channel channel = mock(Channel.class);
        Channel clientChannel = mock(Channel.class);
        ChannelPromise promise = mock(ChannelPromise.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(clientChannel.isActive()).thenReturn(true);
        when(clientChannel.isWritable()).thenReturn(true);
        when(clientChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(clientChannel.writeAndFlush(any())).thenReturn(promise);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getSameClientChannel(channel))
                    .thenReturn(clientChannel);

            server.sendAsyncResponse(rpcMessage, channel, request);

            verify(clientChannel, times(1)).writeAndFlush(any(RpcMessage.class));
        }
    }

    @Test
    public void testSendAsyncResponseWithNullClientChannel() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getSameClientChannel(channel))
                    .thenReturn(null);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                server.sendAsyncResponse(rpcMessage, channel, request);
            });

            assertTrue(exception.getMessage().contains("channel is error"));
        }
    }

    @Test
    public void testRegisterProcessor() {
        RemotingProcessor processor = mock(RemotingProcessor.class);
        ExecutorService executor = mock(ExecutorService.class);

        server.registerProcessor(1, processor, executor);

        Pair<RemotingProcessor, ExecutorService> pair = server.processorTable.get(1);
        assertNotNull(pair);
        assertEquals(processor, pair.getFirst());
        assertEquals(executor, pair.getSecond());
    }

    @Test
    public void testGetListenPort() {
        int port = server.getListenPort();
        assertTrue(port >= 0);
    }

    @Test
    public void testDebugLog() {
        server.debugLog("Test log: {}", "test");
    }

    @Test
    public void testProcessMessageWithMergedWarpMessageVersion230() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);

        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        GlobalBeginRequest request1 = new GlobalBeginRequest();
        request1.setTransactionName("test-tx-1");
        GlobalBeginRequest request2 = new GlobalBeginRequest();
        request2.setTransactionName("test-tx-2");

        mergedMessage.msgs.add(request1);
        mergedMessage.msgs.add(request2);
        mergedMessage.msgIds.add(101);
        mergedMessage.msgIds.add(102);

        rpcMessage.setBody(mergedMessage);

        RpcContext rpcContext = mock(RpcContext.class);
        when(rpcContext.getVersion()).thenReturn("2.3.0");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(rpcContext);

            server.processMessage(ctx, rpcMessage);
        }
    }

    @Test
    public void testProcessMessageWithMergedWarpMessageOldVersion() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);

        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");
        mergedMessage.msgs.add(request);
        mergedMessage.msgIds.add(101);

        rpcMessage.setBody(mergedMessage);

        RpcContext rpcContext = mock(RpcContext.class);
        when(rpcContext.getVersion()).thenReturn("2.2.0");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(rpcContext);

            server.processMessage(ctx, rpcMessage);
        }
    }

    @Test
    public void testProcessMessageWithNonMergedMessage() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");
        rpcMessage.setBody(request);

        RpcContext rpcContext = mock(RpcContext.class);
        when(rpcContext.getVersion()).thenReturn("2.3.0");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(rpcContext);

            server.processMessage(ctx, rpcMessage);
        }
    }

    @Test
    public void testServerHandlerChannelReadWithRpcMessage() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");
        rpcMessage.setBody(request);

        RpcContext rpcContext = mock(RpcContext.class);
        when(rpcContext.getVersion()).thenReturn("2.3.0");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(rpcContext);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            handler.channelRead(ctx, rpcMessage);
        }
    }

    @Test
    public void testServerHandlerChannelReadWithInvalidMessage() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        String invalidMessage = "This is not an RpcMessage";

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.channelRead(ctx, invalidMessage);
    }

    @Test
    public void testServerHandlerChannelWritabilityChanged() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.isWritable()).thenReturn(true);
        when(ctx.fireChannelWritabilityChanged()).thenReturn(ctx);

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.channelWritabilityChanged(ctx);

        verify(ctx, times(1)).fireChannelWritabilityChanged();
    }

    @Test
    public void testServerHandlerChannelWritabilityChangedNotWritable() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.isWritable()).thenReturn(false);
        when(ctx.fireChannelWritabilityChanged()).thenReturn(ctx);

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.channelWritabilityChanged(ctx);

        verify(ctx, times(1)).fireChannelWritabilityChanged();
    }

    @Test
    public void testServerHandlerChannelInactiveWithShutdownExecutor() throws Exception {
        messageExecutor.shutdown();

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(ctx.fireChannelInactive()).thenReturn(ctx);

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.channelInactive(ctx);
    }

    @Test
    public void testServerHandlerChannelInactiveNormal() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(ctx.fireChannelInactive()).thenReturn(ctx);

        RpcContext rpcContext = mock(RpcContext.class);
        when(rpcContext.getClientRole()).thenReturn(null);

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(rpcContext);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            handler.channelInactive(ctx);
        }
    }

    @Test
    public void testServerHandlerHandleDisconnectWithRpcContext() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(ctx.fireChannelInactive()).thenReturn(ctx);

        RpcContext rpcContext = mock(RpcContext.class);
        when(rpcContext.getClientRole()).thenReturn(TransactionRole.TMROLE);

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(rpcContext);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            handler.channelInactive(ctx);

            verify(rpcContext, times(1)).release();
        }
    }

    @Test
    public void testServerHandlerHandleDisconnectWithoutRpcContext() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(null);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            // This should not throw an exception
        }
    }

    @Test
    public void testServerHandlerExceptionCaughtWithDecoderException() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(ctx.fireExceptionCaught(any())).thenReturn(ctx);

        DecoderException decoderException = new DecoderException("Decoder error");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(null);
            channelManager.when(() -> ChannelManager.releaseRpcContext(channel)).then(invocation -> null);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            handler.exceptionCaught(ctx, decoderException);
        }
    }

    @Test
    public void testServerHandlerExceptionCaughtNormal() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(ctx.fireExceptionCaught(any())).thenReturn(ctx);

        RuntimeException exception = new RuntimeException("Test exception");

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(mock(RpcContext.class));
            channelManager.when(() -> ChannelManager.releaseRpcContext(channel)).then(invocation -> null);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            handler.exceptionCaught(ctx, exception);

            channelManager.verify(() -> ChannelManager.releaseRpcContext(channel), times(1));
        }
    }

    @Test
    public void testServerHandlerUserEventTriggeredWithReaderIdle() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        ChannelPromise promise = mock(ChannelPromise.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(ctx.disconnect()).thenReturn(promise);
        when(ctx.close()).thenReturn(promise);

        IdleStateEvent idleStateEvent = mock(IdleStateEvent.class);
        when(idleStateEvent.state()).thenReturn(IdleState.READER_IDLE);

        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager
                    .when(() -> ChannelManager.getContextFromIdentified(channel))
                    .thenReturn(null);

            AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
            handler.userEventTriggered(ctx, idleStateEvent);

            verify(ctx, times(1)).disconnect();
            verify(ctx, times(1)).close();
        }
    }

    @Test
    public void testServerHandlerUserEventTriggeredWithWriterIdle() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        IdleStateEvent idleStateEvent = mock(IdleStateEvent.class);
        when(idleStateEvent.state()).thenReturn(IdleState.WRITER_IDLE);

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.userEventTriggered(ctx, idleStateEvent);
    }

    @Test
    public void testServerHandlerUserEventTriggeredWithOtherEvent() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        String otherEvent = "Other event";

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.userEventTriggered(ctx, otherEvent);
    }

    @Test
    public void testServerHandlerClose() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        ChannelPromise promise = mock(ChannelPromise.class);
        when(ctx.close(eq(promise))).thenReturn(promise);

        AbstractNettyRemotingServer.ServerHandler handler = server.new ServerHandler();
        handler.close(ctx, promise);

        verify(ctx, times(1)).close(eq(promise));
    }

    /**
     * Concrete test implementation of AbstractNettyRemotingServer
     */
    static class TestNettyRemotingServer extends AbstractNettyRemotingServer {

        public TestNettyRemotingServer(ThreadPoolExecutor messageExecutor, NettyServerConfig nettyServerConfig) {
            super(messageExecutor, nettyServerConfig);
        }

        @Override
        public void destroyChannel(String serverAddress, Channel channel) {
            // Test implementation
            if (channel != null) {
                channel.close();
            }
        }

        @Override
        protected void sendAsync(Channel channel, RpcMessage rpcMessage) {
            // Test implementation - just write to channel without complex logic
            if (channel != null && rpcMessage != null) {
                channel.writeAndFlush(rpcMessage);
            }
        }
    }
}
