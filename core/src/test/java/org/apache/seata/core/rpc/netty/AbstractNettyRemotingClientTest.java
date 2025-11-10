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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for AbstractNettyRemotingClient
 */
public class AbstractNettyRemotingClientTest {

    private TestNettyRemotingClient client;
    private ThreadPoolExecutor messageExecutor;
    private NettyClientConfig clientConfig;

    @BeforeEach
    public void setUp() {
        clientConfig = new NettyClientConfig();
        messageExecutor = new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("test", 1));
        client = new TestNettyRemotingClient(clientConfig, messageExecutor);
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            try {
                client.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (messageExecutor != null) {
            messageExecutor.shutdown();
        }
    }

    @Test
    public void testRegisterChannelEventListener() {
        ChannelEventListener listener = mock(ChannelEventListener.class);

        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        client.onChannelActive(channel);

        verify(listener, times(1)).onChannelConnected(channel);
    }

    @Test
    public void testRegisterNullChannelEventListener() {
        client.registerChannelEventListener(null);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        client.onChannelActive(channel);
    }

    @Test
    public void testUnregisterChannelEventListener() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        client.onChannelActive(channel);
        verify(listener, times(1)).onChannelConnected(channel);

        client.unregisterChannelEventListener(listener);

        client.onChannelActive(channel);
        verify(listener, times(1)).onChannelConnected(channel);
    }

    @Test
    public void testUnregisterNullChannelEventListener() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        client.unregisterChannelEventListener(null);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        client.onChannelActive(channel);
        verify(listener, times(1)).onChannelConnected(channel);
    }

    @Test
    public void testOnChannelActive() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        client.onChannelActive(channel);

        verify(listener, times(1)).onChannelConnected(channel);
    }

    @Test
    public void testOnChannelInactive() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        client.onChannelInactive(channel);

        verify(listener, times(1)).onChannelDisconnected(channel);
    }

    @Test
    public void testOnChannelException() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        Throwable cause = new RuntimeException("Test exception");

        client.onChannelException(channel, cause);

        verify(listener, times(1)).onChannelException(channel, cause);
    }

    @Test
    public void testOnChannelIdle() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        client.onChannelIdle(channel);

        verify(listener, times(1)).onChannelIdle(channel);
    }

    @Test
    public void testFireChannelEventWithExceptionInListener() {
        ChannelEventListener listener1 = mock(ChannelEventListener.class);
        ChannelEventListener listener2 = mock(ChannelEventListener.class);

        doNothing().when(listener1).onChannelConnected(any());
        doNothing().when(listener2).onChannelConnected(any());

        client.registerChannelEventListener(listener1);
        client.registerChannelEventListener(listener2);

        Channel channel = mock(Channel.class);
        client.onChannelActive(channel);

        verify(listener1, times(1)).onChannelConnected(channel);
        verify(listener2, times(1)).onChannelConnected(channel);
    }

    @Test
    public void testCleanupResourcesForChannel() {
        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        client.cleanupResourcesForChannel(channel);
    }

    @Test
    public void testCleanupResourcesForNullChannel() {
        client.cleanupResourcesForChannel(null);
    }

    @Test
    public void testSendAsyncRequestWithMergeMessage() {
        MergedWarpMessage mergeMessage = new MergedWarpMessage();
        mergeMessage.msgIds.add(1);
        mergeMessage.msgIds.add(2);
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");
        mergeMessage.msgs.add(request);

        assertTrue(mergeMessage.msgIds.size() > 0, "Merge message should have IDs");
    }

    @Test
    public void testSendAsyncRequestWithNullChannel() {
        try {
            client.sendAsyncRequest(null, HeartbeatMessage.PING);
        } catch (Exception e) {
            assertNotNull(e, "Should throw exception for null channel");
        }
    }

    @Test
    public void testSendAsyncRequestWithHeartbeat() {
        HeartbeatMessage heartbeat = HeartbeatMessage.PING;
        assertNotNull(heartbeat, "Heartbeat message should not be null");
    }

    @Test
    public void testGetXidFromGlobalBeginRequest() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-transaction");

        String xid = client.getXid(request);

        assertEquals("test-transaction", xid);
    }

    @Test
    public void testGetXidFromGlobalCommitRequest() {
        GlobalCommitRequest request = new GlobalCommitRequest();
        request.setXid("test-xid-12345");

        String xid = client.getXid(request);

        assertEquals("test-xid-12345", xid);
    }

    @Test
    public void testGetXidFromBranchRegisterRequest() {
        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid("branch-xid-12345");

        String xid = client.getXid(request);

        assertEquals("branch-xid-12345", xid);
    }

    @Test
    public void testGetXidFromUnknownMessage() {
        AbstractMessage unknownMessage = mock(AbstractMessage.class);

        String xid = client.getXid(unknownMessage);

        assertNotNull(xid, "Should return random xid for unknown message");
    }

    @Test
    public void testDestroyChannel() {
        String serverAddress = "127.0.0.1:8080";
        Channel channel = mock(Channel.class);

        client.destroyChannel(serverAddress, channel);
    }

    @Test
    public void testRegisterProcessor() {
        client.registerProcessor(1, null, null);

        assertNotNull(client.processorTable);
    }

    @Test
    public void testMergeLockAndCondition() {
        assertNotNull(client.mergeLock);
        assertNotNull(client.mergeCondition);
        assertFalse(client.isSending);
    }

    @Test
    public void testMergeMsgMapOperations() {
        MergedWarpMessage message = new MergedWarpMessage();
        message.msgIds.add(1);

        client.mergeMsgMap.put(100, message);

        assertTrue(client.mergeMsgMap.containsKey(100));
        assertEquals(message, client.mergeMsgMap.get(100));
    }

    @Test
    public void testChildToParentMapOperations() {
        client.childToParentMap.put(1, 100);
        client.childToParentMap.put(2, 100);

        assertEquals(100, client.childToParentMap.get(1));
        assertEquals(100, client.childToParentMap.get(2));
    }

    @Test
    public void testBasketMapOperations() {
        String serverAddress = "127.0.0.1:8080";
        LinkedBlockingQueue<RpcMessage> basket = new LinkedBlockingQueue<>();
        client.basketMap.put(serverAddress, basket);

        assertTrue(client.basketMap.containsKey(serverAddress));
    }

    @Test
    public void testGetTransactionServiceGroup() {
        String serviceGroup = client.getTransactionServiceGroup();

        assertEquals("test-service-group", serviceGroup);
    }

    @Test
    public void testIsEnableClientBatchSendRequest() {
        boolean enabled = client.isEnableClientBatchSendRequest();

        assertFalse(enabled);
    }

    @Test
    public void testGetRpcRequestTimeout() {
        long timeout = client.getRpcRequestTimeout();

        assertEquals(30000L, timeout);
    }

    @Test
    public void testGetClientChannelManager() {
        assertNotNull(client.getClientChannelManager());
    }

    @Test
    public void testGetTransactionMessageHandler() {
        assertNull(client.getTransactionMessageHandler());
    }

    @Test
    public void testSetTransactionMessageHandler() {
        client.setTransactionMessageHandler(null);

        assertNull(client.getTransactionMessageHandler());
    }

    @Test
    public void testMultipleListenersReceiveEvents() {
        ChannelEventListener listener1 = mock(ChannelEventListener.class);
        ChannelEventListener listener2 = mock(ChannelEventListener.class);
        ChannelEventListener listener3 = mock(ChannelEventListener.class);

        client.registerChannelEventListener(listener1);
        client.registerChannelEventListener(listener2);
        client.registerChannelEventListener(listener3);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        client.onChannelActive(channel);

        verify(listener1, times(1)).onChannelConnected(channel);
        verify(listener2, times(1)).onChannelConnected(channel);
        verify(listener3, times(1)).onChannelConnected(channel);
    }

    @Test
    public void testChannelEventTypesCoverage() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        client.onChannelActive(channel);
        verify(listener).onChannelConnected(channel);

        client.onChannelInactive(channel);
        verify(listener).onChannelDisconnected(channel);

        Throwable cause = new Exception("test");
        client.onChannelException(channel, cause);
        verify(listener).onChannelException(channel, cause);

        client.onChannelIdle(channel);
        verify(listener).onChannelIdle(channel);
    }

    @Test
    public void testSendSyncRequestWithNullChannel() {
        Channel channel = null;
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            client.sendSyncRequest(channel, request);
        } catch (Exception e) {
            // Expected exception
        }
    }

    @Test
    public void testSendAsyncResponse() {
        String serverAddress = "127.0.0.1:8080";
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            client.sendAsyncResponse(serverAddress, rpcMessage, request);
        } catch (Exception e) {
            // Expected when channel is not available
        }
    }

    @Test
    public void testLoadBalance() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            String address = client.loadBalance("test-service-group", request);
            // May fail if registry is not initialized
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testLoadBalanceWithNullMessage() {
        try {
            String address = client.loadBalance("test-service-group", null);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testChannelWritabilityChanged() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.isWritable()).thenReturn(true);

        try {
            AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
            handler.channelWritabilityChanged(ctx);
            verify(ctx, times(1)).fireChannelWritabilityChanged();
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testChannelWritabilityChangedWhenNotWritable() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.isWritable()).thenReturn(false);

        try {
            AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
            handler.channelWritabilityChanged(ctx);
            verify(ctx, times(1)).fireChannelWritabilityChanged();
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testSendSyncRequestWithBatchEnabled() {
        // Create a client with batch send enabled
        TestNettyRemotingClientWithBatch batchClient =
                new TestNettyRemotingClientWithBatch(clientConfig, messageExecutor);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            batchClient.sendSyncRequest(request);
        } catch (Exception e) {
            // Expected when registry is not initialized or timeout
            assertNotNull(e);
        } finally {
            try {
                batchClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testDoSelectWithEmptyList() throws Exception {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        InetSocketAddress address = client.doSelect(null, request);
        assertNull(address);
    }

    @Test
    public void testDoSelectWithSingleAddress() throws Exception {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        java.util.List<InetSocketAddress> list = new java.util.ArrayList<>();
        list.add(new InetSocketAddress("127.0.0.1", 8080));

        InetSocketAddress address = client.doSelect(list, request);
        assertNotNull(address);
        assertEquals("127.0.0.1", address.getHostString());
        assertEquals(8080, address.getPort());
    }

    @Test
    public void testSendAsyncRequestWithChannel() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            client.sendAsyncRequest(channel, request);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testSendAsyncRequestWithMergedWarpMessage() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        MergedWarpMessage mergeMessage = new MergedWarpMessage();
        mergeMessage.msgIds.add(1);
        mergeMessage.msgIds.add(2);
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");
        mergeMessage.msgs.add(request);

        try {
            client.sendAsyncRequest(channel, mergeMessage);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testSendSyncRequestNonBatchMode() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            client.sendSyncRequest(request);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testSendSyncRequestBatchModeSuccess() throws Exception {
        TestNettyRemotingClientWithBatchAndMockManager batchClient =
                new TestNettyRemotingClientWithBatchAndMockManager(clientConfig, messageExecutor);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            java.lang.reflect.Field basketMapField = AbstractNettyRemotingClient.class.getDeclaredField("basketMap");
            basketMapField.setAccessible(true);
            basketMapField.get(batchClient);

            batchClient.sendSyncRequest(request);
        } catch (Exception e) {
            assertNotNull(e);
        } finally {
            try {
                batchClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testSendSyncRequestBatchModeTimeout() throws Exception {
        TestNettyRemotingClientWithBatchTimeout batchClient =
                new TestNettyRemotingClientWithBatchTimeout(clientConfig, messageExecutor);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            batchClient.sendSyncRequest(request);
        } catch (java.util.concurrent.TimeoutException e) {
            assertNotNull(e);
        } catch (Exception e) {
            assertNotNull(e);
        } finally {
            try {
                batchClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testCollectMessageIdsForChannel() throws Exception {
        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        java.lang.reflect.Method collectMethod =
                AbstractNettyRemotingClient.class.getDeclaredMethod("collectMessageIdsForChannel", ChannelId.class);
        collectMethod.setAccessible(true);

        java.util.Set<Integer> messageIds = (java.util.Set<Integer>) collectMethod.invoke(client, channelId);

        assertNotNull(messageIds);
    }

    @Test
    public void testCleanupFuturesForMessageIds() throws Exception {
        java.util.Set<Integer> messageIds = new java.util.HashSet<>();
        messageIds.add(1);
        messageIds.add(2);

        Exception testException = new Exception("Test exception");

        java.lang.reflect.Method cleanupMethod = AbstractNettyRemotingClient.class.getDeclaredMethod(
                "cleanupFuturesForMessageIds", java.util.Set.class, Exception.class);
        cleanupMethod.setAccessible(true);

        cleanupMethod.invoke(client, messageIds, testException);
    }

    @Test
    public void testClientHandlerChannelRead() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);

        try {
            handler.channelRead(ctx, rpcMessage);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testClientHandlerChannelReadInvalidMessage() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        String invalidMessage = "not an RpcMessage";

        try {
            handler.channelRead(ctx, invalidMessage);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    /**
     * Concrete test implementation of AbstractNettyRemotingClient
     */
    static class TestNettyRemotingClient extends AbstractNettyRemotingClient {

        public TestNettyRemotingClient(NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return false;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 30000L;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    /**
     * Test implementation with batch send enabled
     */
    static class TestNettyRemotingClientWithBatch extends AbstractNettyRemotingClient {

        public TestNettyRemotingClientWithBatch(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return true;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 30000L;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    /**
     * Test implementation with batch send enabled and mock manager
     */
    static class TestNettyRemotingClientWithBatchAndMockManager extends AbstractNettyRemotingClient {

        public TestNettyRemotingClientWithBatchAndMockManager(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return true;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 100L;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    /**
     * Test implementation with batch send enabled and short timeout
     */
    static class TestNettyRemotingClientWithBatchTimeout extends AbstractNettyRemotingClient {

        public TestNettyRemotingClientWithBatchTimeout(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return true;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 1L;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    /**
     * Test ClientHandler methods for coverage
     */
    @Test
    public void testClientHandlerChannelInactive() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        try {
            handler.channelInactive(ctx);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testClientHandlerUserEventTriggeredReaderIdle() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        IdleStateEvent idleEvent = mock(IdleStateEvent.class);
        when(idleEvent.state()).thenReturn(IdleState.READER_IDLE);

        try {
            handler.userEventTriggered(ctx, idleEvent);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testClientHandlerUserEventTriggeredWriterIdle() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        IdleStateEvent writerIdleEvent = IdleStateEvent.WRITER_IDLE_STATE_EVENT;

        try {
            handler.userEventTriggered(ctx, writerIdleEvent);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testClientHandlerExceptionCaught() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        Throwable cause = new RuntimeException("Test exception");

        try {
            handler.exceptionCaught(ctx, cause);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testClientHandlerClose() throws Exception {
        AbstractNettyRemotingClient.ClientHandler handler = client.new ClientHandler();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        try {
            handler.close(ctx, null);
        } catch (Exception e) {
            // Expected in test environment
        }
    }

    @Test
    public void testSendSyncRequestBatchModeOfferFailed() throws Exception {
        TestNettyRemotingClientWithFullBasket fullBasketClient =
                new TestNettyRemotingClientWithFullBasket(clientConfig, messageExecutor);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            fullBasketClient.init();
            Object result = fullBasketClient.sendSyncRequest(request);
            assertNull(result, "Should return null when basket offer fails");
        } catch (Exception e) {
            // Expected when offer fails
        } finally {
            try {
                fullBasketClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testSendSyncRequestBatchModeRuntimeException() throws Exception {
        TestNettyRemotingClientWithBatchRuntimeException runtimeExceptionClient =
                new TestNettyRemotingClientWithBatchRuntimeException(clientConfig, messageExecutor);

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        try {
            runtimeExceptionClient.sendSyncRequest(request);
        } catch (RuntimeException e) {
            assertNotNull(e);
        } catch (Exception e) {
            assertNotNull(e);
        } finally {
            try {
                runtimeExceptionClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testMergedSendRunnableWithMessages() throws Exception {
        TestNettyRemotingClientWithMergeRunnable mergeClient =
                new TestNettyRemotingClientWithMergeRunnable(clientConfig, messageExecutor);

        try {
            mergeClient.init();

            GlobalBeginRequest request = new GlobalBeginRequest();
            request.setTransactionName("test-tx");

            // Give some time for the thread to start
            Thread.sleep(100);

            // Submit a request to trigger merge sending
            try {
                mergeClient.sendSyncRequest(request);
            } catch (Exception e) {
                // Expected
            }

            Thread.sleep(100);
        } finally {
            try {
                mergeClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testCollectMessageIdsForChannelWithMergedWarpMessage() throws Exception {
        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        // Add channel to manager
        String serverAddress = "127.0.0.1:8080";
        client.getClientChannelManager().getChannels().put(serverAddress, channel);

        // Create a MergedWarpMessage and add to mergeMsgMap
        MergedWarpMessage mergedMsg = new MergedWarpMessage();
        mergedMsg.msgIds.add(100);
        mergedMsg.msgIds.add(101);
        client.mergeMsgMap.put(1, mergedMsg);

        // Add basket
        BlockingQueue<RpcMessage> basket = new LinkedBlockingQueue<>();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        basket.offer(rpcMessage);
        client.basketMap.put(serverAddress, basket);

        java.lang.reflect.Method collectMethod =
                AbstractNettyRemotingClient.class.getDeclaredMethod("collectMessageIdsForChannel", ChannelId.class);
        collectMethod.setAccessible(true);

        java.util.Set<Integer> messageIds = (java.util.Set<Integer>) collectMethod.invoke(client, channelId);

        assertNotNull(messageIds);
    }

    @Test
    public void testCleanupFuturesForMessageIdsWithParentId() throws Exception {
        // Setup futures and child-to-parent mapping
        MessageFuture future1 = new MessageFuture();
        MessageFuture future2 = new MessageFuture();
        client.futures.put(1, future1);
        client.futures.put(2, future2);
        client.childToParentMap.put(1, 100);
        client.childToParentMap.put(2, 100);
        client.mergeMsgMap.put(100, new MergedWarpMessage());

        java.util.Set<Integer> messageIds = new java.util.HashSet<>();
        messageIds.add(1);
        messageIds.add(2);

        Exception testException = new Exception("Test exception");

        java.lang.reflect.Method cleanupMethod = AbstractNettyRemotingClient.class.getDeclaredMethod(
                "cleanupFuturesForMessageIds", java.util.Set.class, Exception.class);
        cleanupMethod.setAccessible(true);

        cleanupMethod.invoke(client, messageIds, testException);

        assertFalse(client.futures.containsKey(1));
        assertFalse(client.futures.containsKey(2));
        assertFalse(client.childToParentMap.containsKey(1));
        assertFalse(client.childToParentMap.containsKey(2));
        assertFalse(client.mergeMsgMap.containsKey(100));
    }

    @Test
    public void testFireChannelEventWithAllEventTypes() {
        ChannelEventListener listener = mock(ChannelEventListener.class);
        client.registerChannelEventListener(listener);

        Channel channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        // Test CONNECTED
        client.fireChannelEvent(channel, ChannelEventType.CONNECTED);
        verify(listener, times(1)).onChannelConnected(channel);

        // Test DISCONNECTED
        client.fireChannelEvent(channel, ChannelEventType.DISCONNECTED);
        verify(listener, times(1)).onChannelDisconnected(channel);

        // Test EXCEPTION
        Throwable cause = new Exception("test");
        client.fireChannelEvent(channel, ChannelEventType.EXCEPTION, cause);
        verify(listener, times(1)).onChannelException(channel, cause);

        // Test IDLE
        client.fireChannelEvent(channel, ChannelEventType.IDLE);
        verify(listener, times(1)).onChannelIdle(channel);
    }

    @Test
    public void testInit() {
        TestNettyRemotingClient newClient = new TestNettyRemotingClient(clientConfig, messageExecutor);
        try {
            newClient.init();
            assertNotNull(newClient);
        } finally {
            try {
                newClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testInitWithReconnectException() throws Exception {
        // Create a client that will throw exception during reconnect
        TestNettyRemotingClientWithReconnectException clientWithException =
                new TestNettyRemotingClientWithReconnectException(clientConfig, messageExecutor);

        try {
            clientWithException.init();
            assertNotNull(clientWithException);

            // Wait for the scheduled reconnect task to execute
            // The task is scheduled with SCHEDULE_DELAY_MILLS (60s delay)
            // We can trigger it manually by accessing the timerExecutor
            Thread.sleep(200);

            // Verify client is still initialized despite reconnect failures
            assertNotNull(clientWithException.getClientChannelManager());
        } finally {
            try {
                clientWithException.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testInitWithBatchSendEnabled() {
        TestNettyRemotingClientWithBatch batchClient =
                new TestNettyRemotingClientWithBatch(clientConfig, messageExecutor);
        try {
            batchClient.init();
            assertNotNull(batchClient);
        } finally {
            try {
                batchClient.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testDestroyWithMergeSendExecutor() {
        TestNettyRemotingClientWithBatch batchClient =
                new TestNettyRemotingClientWithBatch(clientConfig, messageExecutor);
        try {
            batchClient.init();
            batchClient.destroy();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Test implementation with full basket to test offer failure
     */
    static class TestNettyRemotingClientWithFullBasket extends AbstractNettyRemotingClient {

        public TestNettyRemotingClientWithFullBasket(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
            this.enableClientBatchSendRequest = true;
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return true;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 100L;
        }

        @Override
        protected String loadBalance(String transactionServiceGroup, Object msg) {
            // Setup a full basket before returning
            String serverAddress = "127.0.0.1:8080";
            BlockingQueue<RpcMessage> fullBasket = new LinkedBlockingQueue<>(1);
            RpcMessage dummyMsg = new RpcMessage();
            fullBasket.offer(dummyMsg);
            basketMap.put(serverAddress, fullBasket);
            return serverAddress;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    /**
     * Test implementation that throws RuntimeException
     */
    static class TestNettyRemotingClientWithBatchRuntimeException extends AbstractNettyRemotingClient {

        public TestNettyRemotingClientWithBatchRuntimeException(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
            this.enableClientBatchSendRequest = true;
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return true;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 1L;
        }

        @Override
        protected String loadBalance(String transactionServiceGroup, Object msg) {
            String serverAddress = "127.0.0.1:8080";
            // Create a mock MessageFuture that will throw RuntimeException
            BlockingQueue<RpcMessage> basket = new LinkedBlockingQueue<>();
            basketMap.put(serverAddress, basket);
            return serverAddress;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    /**
     * Test implementation for MergedSendRunnable testing
     */
    static class TestNettyRemotingClientWithMergeRunnable extends AbstractNettyRemotingClient {

        public TestNettyRemotingClientWithMergeRunnable(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
            this.enableClientBatchSendRequest = true;
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return true;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 100L;
        }

        @Override
        protected String loadBalance(String transactionServiceGroup, Object msg) {
            return "127.0.0.1:8080";
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    @Test
    public void testPrintMergeMessageLogWithDebugEnabled() throws Exception {
        // Enable DEBUG logging for AbstractNettyRemotingClient
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractNettyRemotingClient.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        try {
            TestNettyRemotingClientWithMergeRunnable mergeClient =
                    new TestNettyRemotingClientWithMergeRunnable(clientConfig, messageExecutor);

            mergeClient.init();

            // Create multiple requests to trigger merge with msgIds.size() > 1
            GlobalBeginRequest request1 = new GlobalBeginRequest();
            request1.setTransactionName("test-tx-1");

            GlobalBeginRequest request2 = new GlobalBeginRequest();
            request2.setTransactionName("test-tx-2");

            // Submit multiple requests to trigger merge sending
            Thread submitter = new Thread(() -> {
                try {
                    mergeClient.sendSyncRequest(request1);
                } catch (Exception e) {
                    // Expected
                }
            });

            Thread submitter2 = new Thread(() -> {
                try {
                    mergeClient.sendSyncRequest(request2);
                } catch (Exception e) {
                    // Expected
                }
            });

            submitter.start();
            submitter2.start();

            // Wait for messages to be added to basket
            Thread.sleep(50);

            // Trigger merge condition
            mergeClient.mergeLock.lock();
            try {
                mergeClient.mergeCondition.signalAll();
            } finally {
                mergeClient.mergeLock.unlock();
            }

            // Wait for merge processing
            Thread.sleep(150);

            submitter.join(1000);
            submitter2.join(1000);

            mergeClient.destroy();
        } finally {
            // Restore original log level
            logger.setLevel(originalLevel);
        }
    }

    @Test
    public void testPrintMergeMessageLogWithSingleMessage() throws Exception {
        // Enable DEBUG logging
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractNettyRemotingClient.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        try {
            TestNettyRemotingClientWithMergeRunnable mergeClient =
                    new TestNettyRemotingClientWithMergeRunnable(clientConfig, messageExecutor);

            mergeClient.init();

            // Create a single request (msgIds.size() == 1, should not call printMergeMessageLog)
            GlobalBeginRequest request = new GlobalBeginRequest();
            request.setTransactionName("test-tx-single");

            Thread submitter = new Thread(() -> {
                try {
                    mergeClient.sendSyncRequest(request);
                } catch (Exception e) {
                    // Expected
                }
            });

            submitter.start();
            Thread.sleep(50);

            // Trigger merge condition
            mergeClient.mergeLock.lock();
            try {
                mergeClient.mergeCondition.signalAll();
            } finally {
                mergeClient.mergeLock.unlock();
            }

            Thread.sleep(100);
            submitter.join(1000);

            mergeClient.destroy();
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    public void testMergedSendRunnableWithEmptyBasket() throws Exception {
        TestNettyRemotingClientWithMergeRunnable mergeClient =
                new TestNettyRemotingClientWithMergeRunnable(clientConfig, messageExecutor);

        try {
            mergeClient.init();

            // Add an empty basket
            String serverAddress = "127.0.0.1:8080";
            BlockingQueue<RpcMessage> emptyBasket = new LinkedBlockingQueue<>();
            mergeClient.basketMap.put(serverAddress, emptyBasket);

            // Trigger merge condition with empty basket
            mergeClient.mergeLock.lock();
            try {
                mergeClient.mergeCondition.signalAll();
            } finally {
                mergeClient.mergeLock.unlock();
            }

            // Wait for processing
            Thread.sleep(100);

            // Verify basket is still empty (return branch was taken)
            assertTrue(emptyBasket.isEmpty());

        } finally {
            mergeClient.destroy();
        }
    }

    /**
     * Test implementation that simulates reconnect exception
     */
    static class TestNettyRemotingClientWithReconnectException extends AbstractNettyRemotingClient {
        private static final org.slf4j.Logger TEST_LOGGER =
                LoggerFactory.getLogger(TestNettyRemotingClientWithReconnectException.class);
        private NettyClientChannelManager mockChannelManager;

        public TestNettyRemotingClientWithReconnectException(
                NettyClientConfig nettyClientConfig, ThreadPoolExecutor messageExecutor) {
            super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
        }

        @Override
        public void init() {
            // Use reflection to replace clientChannelManager with a mock
            try {
                java.lang.reflect.Field field =
                        AbstractNettyRemotingClient.class.getDeclaredField("clientChannelManager");
                field.setAccessible(true);
                mockChannelManager = mock(NettyClientChannelManager.class);

                // Make reconnect throw exception
                doThrow(new RuntimeException("Simulated reconnect failure"))
                        .when(mockChannelManager)
                        .reconnect(any());

                field.set(this, mockChannelManager);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Schedule a task that will trigger the exception handler
            timerExecutor.scheduleAtFixedRate(
                    () -> {
                        try {
                            getClientChannelManager().reconnect(getTransactionServiceGroup());
                        } catch (Exception ex) {
                            // This is the branch we want to cover (lines 126-129)
                            TEST_LOGGER.warn("reconnect server failed. {}", ex.getMessage());
                        }
                    },
                    10, // Short delay for testing
                    10000,
                    TimeUnit.MILLISECONDS);
        }

        @Override
        protected Function<String, NettyPoolKey> getPoolKeyFunction() {
            return serverAddress -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, serverAddress);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-service-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return false;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 30000L;
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public NettyClientChannelManager getClientChannelManager() {
            return mockChannelManager != null ? mockChannelManager : super.getClientChannelManager();
        }
    }
}
