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
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ClientHandlerShutdownTest {
    private AbstractNettyRemotingClient client;
    private NettyClientChannelManager manager;
    private Channel channel;
    private ChannelHandlerContext context;
    private static final String ADDRESS = "127.0.0.1:8091";

    @BeforeEach
    void setUp() throws Exception {
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        client = new NettyRemotingClientBehaviorTest.TestNettyRemotingClient(new NettyClientConfig(), executor);
        manager = mock(NettyClientChannelManager.class);
        Field field = AbstractNettyRemotingClient.class.getDeclaredField("clientChannelManager");
        field.setAccessible(true);
        field.set(client, manager);
        channel = mock(Channel.class);
        context = mock(ChannelHandlerContext.class);
        when(context.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));
    }

    @AfterEach
    void tearDown() {
        client.destroy();
    }

    @Test
    void inactiveChannelIsCleanedAfterTimerShutdown() {
        client.timerExecutor.shutdown();
        assertDoesNotThrow(() -> client.new ClientHandler().channelInactive(context));
        verify(manager).releaseChannel(channel, ADDRESS);
        verify(manager).cleanupDisconnectedChannelMetadata(ADDRESS);
        verify(context).fireChannelInactive();
    }

    @Test
    void inactiveChannelIsCleanedAfterBothExecutorsShutdown() {
        client.timerExecutor.shutdown();
        client.messageExecutor.shutdown();
        assertDoesNotThrow(() -> client.new ClientHandler().channelInactive(context));
        verify(manager).releaseChannel(channel, ADDRESS);
        verify(manager).cleanupDisconnectedChannelMetadata(ADDRESS);
        verify(context).fireChannelInactive();
    }

    @Test
    void exceptionCleanupDoesNotRejectAfterTimerShutdown() {
        client.timerExecutor.shutdown();
        IllegalStateException cause = new IllegalStateException("connection closed");
        assertDoesNotThrow(() -> client.new ClientHandler().exceptionCaught(context, cause));
        verify(manager).releaseChannel(channel, ADDRESS);
        verify(context).fireExceptionCaught(cause);
    }

    @Test
    void readerIdleStillReleasesChannelAfterTimerShutdown() throws Exception {
        client.timerExecutor.shutdown();
        client.new ClientHandler().userEventTriggered(context, IdleStateEvent.READER_IDLE_STATE_EVENT);
        verify(manager).invalidateObject(ADDRESS, channel);
        verify(manager).releaseChannel(channel, ADDRESS);
    }

    @Test
    void runningTimerExecutesCleanup() {
        assertDoesNotThrow(() -> client.new ClientHandler().channelInactive(context));
        verify(manager, timeout(1000)).releaseChannel(channel, ADDRESS);
        verify(manager, timeout(1000)).cleanupDisconnectedChannelMetadata(ADDRESS);
    }
}
