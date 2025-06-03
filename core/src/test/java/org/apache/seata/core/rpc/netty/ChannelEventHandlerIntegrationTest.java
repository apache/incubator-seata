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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChannelEventHandlerIntegrationTest {

    private static final int SERVER_PORT = 8919;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int TIMEOUT_SECONDS = 5;

    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;
    private static Channel serverChannel;

    @Mock
    private AbstractNettyRemotingClient mockRemotingClient;

    @Captor
    private ArgumentCaptor<Channel> channelCaptor;

    @Captor
    private ArgumentCaptor<Throwable> throwableCaptor;

    private ChannelEventHandler channelEventHandler;
    private EventLoopGroup clientGroup;
    private Channel clientChannel;
    private CountDownLatch channelActiveLatch;
    private CountDownLatch channelInactiveLatch;
    private CountDownLatch exceptionCaughtLatch;
    private CountDownLatch idleEventLatch;

    @BeforeAll
    static void setupClass() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new IdleStateHandler(1, 0, 0, TimeUnit.SECONDS));
                }
            });

        serverChannel = serverBootstrap.bind(SERVER_PORT).sync().channel();
    }

    @AfterAll
    static void tearDownClass() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    @BeforeEach
    void setUp() {
        channelEventHandler = new ChannelEventHandler(mockRemotingClient);

        clientGroup = new NioEventLoopGroup();
        channelActiveLatch = new CountDownLatch(1);
        channelInactiveLatch = new CountDownLatch(1);
        exceptionCaughtLatch = new CountDownLatch(1);
        idleEventLatch = new CountDownLatch(1);

        lenient()
            .doAnswer(invocation -> {
                channelActiveLatch.countDown();
                return null;
            })
            .when(mockRemotingClient)
            .onChannelActive(any(Channel.class));

        lenient()
            .doAnswer(invocation -> {
                channelInactiveLatch.countDown();
                return null;
            })
            .when(mockRemotingClient)
            .onChannelInactive(any(Channel.class));

        lenient()
            .doAnswer(invocation -> {
                exceptionCaughtLatch.countDown();
                return null;
            })
            .when(mockRemotingClient)
            .onChannelException(any(Channel.class), any(Throwable.class));

        lenient()
            .doAnswer(invocation -> {
                idleEventLatch.countDown();
                return null;
            })
            .when(mockRemotingClient)
            .onChannelIdle(any(Channel.class));
    }

    @AfterEach
    void tearDown() {
        if (clientChannel != null) {
            clientChannel.close();
        }
        if (clientGroup != null) {
            clientGroup.shutdownGracefully();
        }
    }

    @Test
    void testChannelActive() throws Exception {
        connectClient();

        assertTrue(
            channelActiveLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "Channel activation event was not detected");

        verify(mockRemotingClient).onChannelActive(channelCaptor.capture());
        Channel capturedChannel = channelCaptor.getValue();
        assertNotNull(capturedChannel);

        SocketAddress remoteAddress = capturedChannel.remoteAddress();
        assertInstanceOf(InetSocketAddress.class, remoteAddress);

        InetSocketAddress inetAddress = (InetSocketAddress) remoteAddress;
        assertEquals(SERVER_HOST, inetAddress.getHostString());
        assertEquals(SERVER_PORT, inetAddress.getPort());
    }

    @Test
    void testChannelInactive() throws Exception {
        connectClient();

        clientChannel.close().sync();

        assertTrue(
            channelInactiveLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "Channel deactivation event was not detected");

        verify(mockRemotingClient).onChannelInactive(any(Channel.class));
    }

    @Test
    void testChannelInactiveByServer() throws Exception {
        connectClient();

        DefaultChannelGroup serverChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        serverChannels.addAll(collectServerChannels(workerGroup));
        Channel serverSideClientChannel = serverChannels.stream()
            .filter(ch -> ch.isActive() && ch.remoteAddress() != null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Failed to find client channel on server side"));

        serverSideClientChannel.close().sync();
        assertTrue(
            channelInactiveLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "Channel inactive event was not detected on client side when server closed the connection");
        verify(mockRemotingClient).onChannelInactive(any(Channel.class));
    }

    @Test
    void testExceptionCaught() throws Exception {
        connectClient();

        RuntimeException testException = new RuntimeException("Test exception");
        clientChannel.pipeline().fireExceptionCaught(testException);

        assertTrue(exceptionCaughtLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Exception event was not detected");

        verify(mockRemotingClient).onChannelException(any(Channel.class), throwableCaptor.capture());

        Throwable capturedException = throwableCaptor.getValue();
        assertNotNull(capturedException);
    }

    @Test
    void testChannelIdle() throws Exception {
        connectClient(500);

        assertTrue(idleEventLatch.await(3, TimeUnit.SECONDS), "Idle event was not detected");

        verify(mockRemotingClient).onChannelIdle(any(Channel.class));
    }

    private void connectClient() throws InterruptedException {
        connectClient(0);
    }

    private void connectClient(int idleTimeoutMillis) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                if (idleTimeoutMillis > 0) {
                    pipeline.addLast(new IdleStateHandler(0, idleTimeoutMillis, 0, TimeUnit.MILLISECONDS));
                }
                pipeline.addLast(channelEventHandler);
            }
        });

        ChannelFuture future = bootstrap.connect(SERVER_HOST, SERVER_PORT).sync();
        clientChannel = future.channel();
        assertTrue(clientChannel.isActive());
    }

    private DefaultChannelGroup collectServerChannels(EventLoopGroup workerGroup) throws InterruptedException {
        DefaultChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        for (EventExecutor executor : workerGroup) {
            if (executor instanceof SingleThreadEventLoop) {
                SingleThreadEventLoop eventLoop = (SingleThreadEventLoop) executor;

                executor.submit(() -> {
                        Iterator<Channel> it = eventLoop.registeredChannelsIterator();
                        while (it.hasNext()) {
                            Channel ch = it.next();
                            if (ch.isActive() && ch instanceof SocketChannel) {
                                channels.add(ch);
                            }
                        }
                        return null;
                    })
                    .sync();
            }
        }
        return channels;
    }
}
