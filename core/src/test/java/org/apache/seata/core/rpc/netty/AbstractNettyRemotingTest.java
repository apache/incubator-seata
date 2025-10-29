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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.rpc.processor.Pair;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractNettyRemotingTest {

    private TestNettyRemoting nettyRemoting;
    private ThreadPoolExecutor messageExecutor;

    @BeforeEach
    public void setUp() {
        messageExecutor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100), new NamedThreadFactory("test", 1));
        nettyRemoting = new TestNettyRemoting(messageExecutor);
    }

    @AfterEach
    public void tearDown() {
        if (nettyRemoting != null) {
            nettyRemoting.destroy();
        }
    }

    @Test
    public void testInit() throws Exception {
        nettyRemoting.init();

        Thread.sleep(3500);

        assertTrue(nettyRemoting.nowMills > 0);
    }

    @Test
    public void testGetNextMessageId() {
        int id1 = nettyRemoting.getNextMessageId();
        int id2 = nettyRemoting.getNextMessageId();

        assertTrue(id2 > id1);
    }

    @Test
    public void testGetAndSetGroup() {
        assertEquals("DEFAULT", nettyRemoting.getGroup());

        nettyRemoting.setGroup("TEST_GROUP");
        assertEquals("TEST_GROUP", nettyRemoting.getGroup());
    }

    @Test
    public void testBuildRequestMessage() {
        HeartbeatMessage heartbeat = HeartbeatMessage.PING;
        RpcMessage rpcMessage = nettyRemoting.buildRequestMessage(heartbeat, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        assertNotNull(rpcMessage);
        assertEquals(MessageType.TYPE_HEARTBEAT_MSG, rpcMessage.getMessageType());
        assertEquals(heartbeat, rpcMessage.getBody());
        assertEquals(ProtocolConstants.CONFIGURED_CODEC, rpcMessage.getCodec());
        assertEquals(ProtocolConstants.CONFIGURED_COMPRESSOR, rpcMessage.getCompressor());
        assertTrue(rpcMessage.getId() > 0);
    }

    @Test
    public void testBuildResponseMessage() {
        RpcMessage requestMessage = new RpcMessage();
        requestMessage.setId(123);
        requestMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
        requestMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);

        BranchCommitResponse response = new BranchCommitResponse();
        RpcMessage responseMessage = nettyRemoting.buildResponseMessage(
                requestMessage, response, (byte) MessageType.TYPE_BRANCH_COMMIT_RESULT);

        assertNotNull(responseMessage);
        assertEquals(123, responseMessage.getId());
        assertEquals(MessageType.TYPE_BRANCH_COMMIT_RESULT, responseMessage.getMessageType());
        assertEquals(response, responseMessage.getBody());
        assertEquals(requestMessage.getCodec(), responseMessage.getCodec());
        assertEquals(requestMessage.getCompressor(), responseMessage.getCompressor());
    }

    @Test
    public void testSendSyncWithNullChannel() throws TimeoutException {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(HeartbeatMessage.PING);

        Object result = nettyRemoting.sendSync(null, rpcMessage, 1000);

        assertNull(result);
    }

    @Test
    public void testSendSyncWithInvalidTimeout() {
        Channel channel = mock(Channel.class);
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(HeartbeatMessage.PING);

        assertThrows(FrameworkException.class, () -> {
            nettyRemoting.sendSync(channel, rpcMessage, 0);
        });

        assertThrows(FrameworkException.class, () -> {
            nettyRemoting.sendSync(channel, rpcMessage, -1);
        });
    }

    @Test
    public void testSendSyncSuccess() throws Exception {
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(true);
        when(channel.writeAndFlush(any(RpcMessage.class))).thenReturn(channelFuture);

        doAnswer(invocation -> {
                    io.netty.util.concurrent.GenericFutureListener listener = invocation.getArgument(0);
                    listener.operationComplete(channelFuture);
                    return channelFuture;
                })
                .when(channelFuture)
                .addListener(any(io.netty.util.concurrent.GenericFutureListener.class));

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        MessageFuture future = nettyRemoting.getFutures().get(rpcMessage.getId());
                        if (future != null) {
                            future.setResultMessage(HeartbeatMessage.PONG);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .start();

        Object result = nettyRemoting.sendSync(channel, rpcMessage, 3000);

        assertNotNull(result);
        assertEquals(HeartbeatMessage.PONG, result);
    }

    @Test
    public void testSendSyncTimeout() throws Exception {
        nettyRemoting.init();

        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(true);
        when(channel.writeAndFlush(any(RpcMessage.class))).thenReturn(channelFuture);

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        assertThrows(TimeoutException.class, () -> {
            nettyRemoting.sendSync(channel, rpcMessage, 100);
        });

        Thread.sleep(3500);

        assertNull(nettyRemoting.getFutures().get(rpcMessage.getId()));
    }

    @Test
    public void testSendSyncWriteFailed() {
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(false);
        when(channelFuture.cause()).thenReturn(new RuntimeException("Write failed"));
        when(channelFuture.channel()).thenReturn(channel);
        when(channel.writeAndFlush(any(RpcMessage.class))).thenReturn(channelFuture);

        doAnswer(invocation -> {
                    io.netty.util.concurrent.GenericFutureListener listener = invocation.getArgument(0);
                    listener.operationComplete(channelFuture);
                    return channelFuture;
                })
                .when(channelFuture)
                .addListener(any(io.netty.util.concurrent.GenericFutureListener.class));

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        assertThrows(Exception.class, () -> {
            nettyRemoting.sendSync(channel, rpcMessage, 1000);
        });
    }

    @Test
    public void testSendAsyncSuccess() {
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(true);
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(true);
        when(channel.writeAndFlush(any(RpcMessage.class))).thenReturn(channelFuture);

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        nettyRemoting.sendAsync(channel, rpcMessage);

        verify(channel, times(1)).writeAndFlush(any(RpcMessage.class));
    }

    @Test
    public void testSendAsyncWriteFailed() {
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(true);
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(false);
        when(channelFuture.channel()).thenReturn(channel);
        when(channel.writeAndFlush(any(RpcMessage.class))).thenReturn(channelFuture);

        doAnswer(invocation -> {
                    io.netty.util.concurrent.GenericFutureListener listener = invocation.getArgument(0);
                    listener.operationComplete(channelFuture);
                    return channelFuture;
                })
                .when(channelFuture)
                .addListener(any(io.netty.util.concurrent.GenericFutureListener.class));

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        nettyRemoting.sendAsync(channel, rpcMessage);

        assertTrue(nettyRemoting.destroyChannelCalled);
    }

    @Test
    public void testProcessMessageWithProcessor() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RemotingProcessor processor = mock(RemotingProcessor.class);
        nettyRemoting.registerProcessor((int) MessageType.TYPE_BRANCH_COMMIT, processor, messageExecutor);

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);

        Thread.sleep(100);

        verify(processor, times(1)).process(any(ChannelHandlerContext.class), any(RpcMessage.class));
    }

    @Test
    public void testProcessMessageWithNullExecutor() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RemotingProcessor processor = mock(RemotingProcessor.class);
        nettyRemoting.registerProcessor((int) MessageType.TYPE_BRANCH_COMMIT, processor, null);

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);

        verify(processor, times(1)).process(any(ChannelHandlerContext.class), any(RpcMessage.class));
    }

    @Test
    public void testProcessMessageWithNoProcessor() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);
    }

    @Test
    public void testProcessMessageThrowsExceptionInExecutor() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RemotingProcessor processor = mock(RemotingProcessor.class);
        doThrow(new RuntimeException("Test exception"))
                .when(processor)
                .process(any(ChannelHandlerContext.class), any(RpcMessage.class));

        nettyRemoting.registerProcessor((int) MessageType.TYPE_BRANCH_COMMIT, processor, messageExecutor);

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);

        Thread.sleep(100);

        verify(processor, times(1)).process(any(ChannelHandlerContext.class), any(RpcMessage.class));
    }

    @Test
    public void testProcessMessageThrowsExceptionWithNullExecutor() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RemotingProcessor processor = mock(RemotingProcessor.class);
        doThrow(new RuntimeException("Test exception"))
                .when(processor)
                .process(any(ChannelHandlerContext.class), any(RpcMessage.class));

        nettyRemoting.registerProcessor((int) MessageType.TYPE_BRANCH_COMMIT, processor, null);

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);

        verify(processor, times(1)).process(any(ChannelHandlerContext.class), any(RpcMessage.class));
    }

    @Test
    public void testProcessMessageWithNonMessageTypeAwareBody() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody("Not a MessageTypeAware object");

        nettyRemoting.processMessage(ctx, rpcMessage);
    }

    @Test
    public void testProcessMessageRejectedExecutionWithDumpStack() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        ExecutorService rejectedExecutor = mock(ExecutorService.class);
        doThrow(new RejectedExecutionException("Thread pool is full"))
                .when(rejectedExecutor)
                .execute(any(Runnable.class));

        RemotingProcessor processor = mock(RemotingProcessor.class);
        nettyRemoting.registerProcessor((int) MessageType.TYPE_BRANCH_COMMIT, processor, rejectedExecutor);

        nettyRemoting.allowDumpStack = true;

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);

        verify(rejectedExecutor, times(1)).execute(any(Runnable.class));
        verify(processor, times(0)).process(any(ChannelHandlerContext.class), any(RpcMessage.class));
    }

    @Test
    public void testProcessMessageRejectedExecutionWithoutDumpStack() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        ExecutorService rejectedExecutor = mock(ExecutorService.class);
        doThrow(new RejectedExecutionException("Thread pool is full"))
                .when(rejectedExecutor)
                .execute(any(Runnable.class));

        RemotingProcessor processor = mock(RemotingProcessor.class);
        nettyRemoting.registerProcessor((int) MessageType.TYPE_BRANCH_COMMIT, processor, rejectedExecutor);

        nettyRemoting.allowDumpStack = false;

        BranchCommitRequest request = new BranchCommitRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        nettyRemoting.processMessage(ctx, rpcMessage);

        verify(rejectedExecutor, times(1)).execute(any(Runnable.class));
        verify(processor, times(0)).process(any(ChannelHandlerContext.class), any(RpcMessage.class));
    }

    @Test
    public void testGetAddressFromChannel() {
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        String address = nettyRemoting.getAddressFromChannel(channel);

        assertNotNull(address);
        assertTrue(address.contains("127.0.0.1"));
        assertTrue(address.contains("8091"));
    }

    @Test
    public void testDestroy() {
        nettyRemoting.init();

        nettyRemoting.destroy();

        assertTrue(nettyRemoting.timerExecutor.isShutdown());
        assertTrue(messageExecutor.isShutdown());
    }

    @Test
    public void testChannelNotWritableException() {
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(false);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            nettyRemoting.sendSync(channel, rpcMessage, 1000);
        });

        assertEquals(FrameworkErrorCode.ChannelIsNotWritable, exception.getErrcode());
        assertTrue(nettyRemoting.destroyChannelCalled);
    }

    @Test
    public void testTimeoutCheckCleansFutures() throws Exception {
        nettyRemoting.init();

        RpcMessage rpcMessage =
                nettyRemoting.buildRequestMessage(HeartbeatMessage.PING, (byte) MessageType.TYPE_HEARTBEAT_MSG);

        MessageFuture future = new MessageFuture();
        future.setRequestMessage(rpcMessage);
        future.setTimeout(100);
        nettyRemoting.getFutures().put(rpcMessage.getId(), future);

        assertEquals(1, nettyRemoting.getFutures().size());

        Thread.sleep(4000);

        assertEquals(0, nettyRemoting.getFutures().size());
    }

    static class TestNettyRemoting extends AbstractNettyRemoting {

        public boolean destroyChannelCalled = false;

        public TestNettyRemoting(ThreadPoolExecutor messageExecutor) {
            super(messageExecutor);
        }

        @Override
        public void destroyChannel(String serverAddress, Channel channel) {
            destroyChannelCalled = true;
        }

        public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {
            this.processorTable.put(messageType, new Pair<>(processor, executor));
        }
    }
}
