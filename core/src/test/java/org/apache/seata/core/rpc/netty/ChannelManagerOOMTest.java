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
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.apache.seata.core.rpc.processor.server.RegRmProcessor;
import org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Closed client channels must leave the identified channels of {@link ChannelManager}, without breaking the
 * requests and responses that are still in flight for them.
 */
class ChannelManagerOOMTest {

    private static final String APP_ID = "business-app";
    private static final String TX_GROUP = "default_tx_group";
    private static final String RESOURCE_ID = "jdbc:mysql://127.0.0.1:3306/seata";

    private ThreadPoolExecutor messageExecutor;
    private TestServer server;
    private AbstractNettyRemotingServer.ServerHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        clearChannelMaps();
        messageExecutor = new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("oom-test", 1));
        server = new TestServer(messageExecutor);
        handler = server.new ServerHandler();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            server.destroy();
        } catch (Exception e) {
            // Ignore
        }
        messageExecutor.shutdown();
        clearChannelMaps();
    }

    @Test
    void repeatedReconnectsDoNotAccumulateClosedChannels() throws Exception {
        for (int i = 0; i < 200; i++) {
            Channel tmChannel = channel(20000 + i, false);
            ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), tmChannel);
            handler.channelInactive(ctx(tmChannel));

            Channel rmChannel = channel(30000 + i, false);
            ChannelManager.registerRMChannel(rmRequest(), rmChannel);
            handler.channelInactive(ctx(rmChannel));
        }

        server.elapseRetention();

        assertTrue(channelMap("IDENTIFIED_CHANNELS").isEmpty());
        assertEquals(0, contextCount(channelMap("TM_CHANNELS")));
        assertEquals(0, contextCount(channelMap("RM_CHANNELS")));
    }

    @Test
    void rmRegistrationHandledAfterCloseDoesNotLeak() throws Exception {
        Channel channel = channel(40010, false);
        ChannelHandlerContext ctx = ctx(channel);

        // the channel closes while its RegisterRM request still waits in the executor
        handler.channelInactive(ctx);
        new RegRmProcessor(mock(RemotingServer.class)).process(ctx, registerRequest(rmRequest()));

        assertFalse(ChannelManager.isRegistered(channel));
        assertEquals(0, contextCount(channelMap("RM_CHANNELS")));
    }

    @Test
    void rmRegistrationDuringRetentionDoesNotRestoreRouting() throws Exception {
        Channel channel = channel(40011, false);
        ChannelManager.registerRMChannel(rmRequest(), channel);
        ChannelHandlerContext ctx = ctx(channel);
        RegisterRMRequest anotherResource = new RegisterRMRequest(APP_ID, TX_GROUP);
        anotherResource.setResourceIds("jdbc:mysql://127.0.0.1:3306/another");

        handler.channelInactive(ctx);
        new RegRmProcessor(mock(RemotingServer.class)).process(ctx, registerRequest(anotherResource));

        assertFalse(ChannelManager.isRegistered(channel));
        assertEquals(0, contextCount(channelMap("RM_CHANNELS")));
    }

    @Test
    void closedChannelStaysIdentifiedUntilRetentionElapses() throws Exception {
        Channel channel = channel(40001, false);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);

        handler.channelInactive(ctx(channel));
        assertTrue(ChannelManager.isRegistered(channel));

        server.elapseRetention();
        assertFalse(ChannelManager.isRegistered(channel));
    }

    @Test
    void responseForClosedTmChannelFallsBackToAnotherChannelOfTheSameClient() throws Exception {
        Channel live = channel(40002, true);
        when(live.isWritable()).thenReturn(true);
        when(live.writeAndFlush(any())).thenReturn(mock(ChannelPromise.class));
        Channel closed = channel(40003, false);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), live);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), closed);

        handler.channelInactive(ctx(closed));
        server.sendAsyncResponse(new RpcMessage(), closed, new GlobalCommitResponse());

        verify(live).writeAndFlush(any(RpcMessage.class));
        verify(closed, never()).writeAndFlush(any());
    }

    @Test
    void requestQueuedBeforeCloseIsStillHandled() throws Exception {
        Channel channel = channel(40004, false);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);
        ChannelHandlerContext ctx = ctx(channel);
        TransactionMessageHandler transactionMessageHandler = mock(TransactionMessageHandler.class);
        when(transactionMessageHandler.onRequest(any(), any())).thenReturn(new GlobalBeginResponse());
        ServerOnRequestProcessor processor =
                new ServerOnRequestProcessor(mock(RemotingServer.class), transactionMessageHandler);
        RpcMessage request = new RpcMessage();
        request.setBody(new GlobalBeginRequest());

        handler.channelInactive(ctx);
        processor.process(ctx, request);

        verify(transactionMessageHandler).onRequest(any(GlobalBeginRequest.class), notNull());
        verify(ctx, never()).close();
    }

    @Test
    void exceptionOnOpenChannelKeepsItServable() throws Exception {
        Channel channel = channel(40005, true);
        ChannelManager.registerRMChannel(rmRequest(), channel);
        ChannelHandlerContext ctx = ctx(channel);
        RemotingProcessor processor = mock(RemotingProcessor.class);
        server.registerProcessor(MessageType.TYPE_BRANCH_REGISTER, processor, null);

        handler.exceptionCaught(ctx, new DecoderException("broken frame"));
        assertTrue(ChannelManager.isRegistered(channel));

        server.processMessage(ctx, mergedRequest(new BranchRegisterRequest(), 1));
        verify(processor).process(eq(ctx), any(RpcMessage.class));
    }

    @Test
    void mergedRequestFromUnidentifiedChannelIsDispatchedWhole() throws Exception {
        ChannelHandlerContext ctx = ctx(channel(40006, true));
        RemotingProcessor processor = mock(RemotingProcessor.class);
        server.registerProcessor(MessageType.TYPE_SEATA_MERGE, processor, null);
        RpcMessage merged = mergedRequest(new BranchRegisterRequest(), 1);

        server.processMessage(ctx, merged);

        verify(processor).process(ctx, merged);
    }

    @Test
    void removalKeepsTheContextTheChannelIsBoundTo() throws Exception {
        Channel channel = channel(40007, true);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);

        ChannelManager.removeIdentifiedChannel(channel, new RpcContext());

        assertTrue(ChannelManager.isRegistered(channel));
    }

    @Test
    void removalIsScheduledOnTheTimer() throws Exception {
        Channel channel = channel(40008, false);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);
        server.captureRemovals = false;

        handler.channelInactive(ctx(channel));

        assertTrue(ChannelManager.isRegistered(channel));
        ScheduledThreadPoolExecutor timer = (ScheduledThreadPoolExecutor) server.timerExecutor;
        assertEquals(1, timer.getQueue().size());
        assertTrue(((ScheduledFuture<?>) timer.getQueue().peek()).getDelay(TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void removalRunsAtOnceWhenTheServerIsShuttingDown() throws Exception {
        Channel channel = channel(40009, false);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);
        server.captureRemovals = false;
        server.timerExecutor.shutdown();

        server.removeIdentifiedChannelLater(channel, ChannelManager.getContextFromIdentified(channel));

        assertFalse(ChannelManager.isRegistered(channel));
    }

    private static RegisterRMRequest rmRequest() {
        RegisterRMRequest request = new RegisterRMRequest(APP_ID, TX_GROUP);
        request.setResourceIds(RESOURCE_ID);
        return request;
    }

    private static RpcMessage registerRequest(RegisterRMRequest request) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(request);
        return rpcMessage;
    }

    private static RpcMessage mergedRequest(AbstractMessage message, int msgId) {
        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();
        mergedWarpMessage.msgs.add(message);
        mergedWarpMessage.msgIds.add(msgId);
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(mergedWarpMessage);
        return rpcMessage;
    }

    private static Channel channel(int remotePort, boolean active) {
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", remotePort));
        when(channel.isActive()).thenReturn(active);
        return channel;
    }

    private static ChannelHandlerContext ctx(Channel channel) {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(channel);
        return ctx;
    }

    private static Map<?, ?> channelMap(String fieldName) throws Exception {
        Field field = ChannelManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<?, ?>) field.get(null);
    }

    private static int contextCount(Map<?, ?> map) {
        int count = 0;
        for (Object value : map.values()) {
            count += value instanceof Map ? contextCount((Map<?, ?>) value) : 1;
        }
        return count;
    }

    private static void clearChannelMaps() throws Exception {
        channelMap("IDENTIFIED_CHANNELS").clear();
        channelMap("TM_CHANNELS").clear();
        channelMap("RM_CHANNELS").clear();
    }

    /**
     * Holds the delayed removals back until the test lets the retention time elapse.
     */
    static class TestServer extends AbstractNettyRemotingServer {

        private final List<Runnable> pendingRemovals = new ArrayList<>();

        private boolean captureRemovals = true;

        TestServer(ThreadPoolExecutor messageExecutor) {
            super(messageExecutor, new NettyServerConfig());
        }

        @Override
        public void destroyChannel(String serverAddress, Channel channel) {}

        @Override
        protected void removeIdentifiedChannelLater(Channel channel, RpcContext rpcContext) {
            if (captureRemovals) {
                pendingRemovals.add(() -> ChannelManager.removeIdentifiedChannel(channel, rpcContext));
            } else {
                super.removeIdentifiedChannelLater(channel, rpcContext);
            }
        }

        void elapseRetention() {
            pendingRemovals.forEach(Runnable::run);
            pendingRemovals.clear();
        }
    }
}
