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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.Protocol;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.detector.Http2Detector;
import org.apache.seata.core.rpc.netty.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NettyClientBootstrapTest {

    @Mock
    private NettyClientConfig nettyClientConfig;

    @Test
    void testSharedEventLoopGroupEnabled() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(true);
        NettyClientBootstrap tmNettyClientBootstrap =
                new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
        EventLoopGroup tmEventLoopGroupWorker = getEventLoopGroupWorker(tmNettyClientBootstrap);

        NettyClientBootstrap rmNettyClientBootstrap =
                new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.RMROLE);
        EventLoopGroup rmEventLoopGroupWorker = getEventLoopGroupWorker(rmNettyClientBootstrap);

        Assertions.assertEquals(tmEventLoopGroupWorker, rmEventLoopGroupWorker);
    }

    @Test
    void testSharedEventLoopGroupDisabled() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(false);
        NettyClientBootstrap tmNettyClientBootstrap =
                new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
        EventLoopGroup tmEventLoopGroupWorker = getEventLoopGroupWorker(tmNettyClientBootstrap);

        NettyClientBootstrap rmNettyClientBootstrap =
                new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.RMROLE);
        EventLoopGroup rmEventLoopGroupWorker = getEventLoopGroupWorker(rmNettyClientBootstrap);

        Assertions.assertNotEquals(tmEventLoopGroupWorker, rmEventLoopGroupWorker);
    }

    @Test
    void testStartWithSharedEventLoopAndChannelSelection() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(true);
        when(nettyClientConfig.getClientChannelClazz()).thenAnswer(invocation -> {
            if (Epoll.isAvailable()) {
                return EpollSocketChannel.class;
            }
            return NioSocketChannel.class;
        });

        NettyClientBootstrap tmNettyClientBootstrap =
                new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
        tmNettyClientBootstrap.start();

        Assertions.assertSame(
                PooledByteBufAllocator.DEFAULT,
                getBootstrap(tmNettyClientBootstrap).config().options().get(ChannelOption.ALLOCATOR));
    }

    @Test
    void testStartWithNioChannelDoesNotApplyEpollOptions() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(false);
        when(nettyClientConfig.getClientChannelClazz()).thenAnswer(invocation -> NioSocketChannel.class);

        NettyClientBootstrap bootstrap =
                new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
        try {
            bootstrap.start();
            Assertions.assertFalse(
                    getBootstrap(bootstrap).config().options().containsKey(EpollChannelOption.EPOLL_MODE));
            Assertions.assertFalse(
                    getBootstrap(bootstrap).config().options().containsKey(EpollChannelOption.TCP_QUICKACK));
        } finally {
            getEventLoopGroupWorker(bootstrap).shutdownGracefully().syncUninterruptibly();
        }
    }

    @Test
    void testNioEventLoopGroup() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(false);

        try (MockedStatic<NettyServerConfig> mockedConfig =
                Mockito.mockStatic(NettyServerConfig.class, Mockito.CALLS_REAL_METHODS)) {
            mockedConfig.when(NettyServerConfig::enableEpoll).thenReturn(false);

            NettyClientBootstrap bootstrap =
                    new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
            EventLoopGroup eventLoopGroup = getEventLoopGroupWorker(bootstrap);
            Assertions.assertInstanceOf(MultiThreadIoEventLoopGroup.class, eventLoopGroup);
            eventLoopGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Test
    void testGetNewChannelWithGrpcProtocol() throws Exception {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(false);
        when(nettyClientConfig.getClientSelectorThreadSize()).thenReturn(1);
        when(nettyClientConfig.getClientSelectorThreadPrefix()).thenReturn("NettyClientSelector");
        when(nettyClientConfig.getClientChannelClazz())
                .thenAnswer(invocation ->
                        NettyServerConfig.enableEpoll() ? EpollSocketChannel.class : NioSocketChannel.class);
        when(nettyClientConfig.getConnectTimeoutMillis()).thenReturn(5000);
        when(nettyClientConfig.getClientSocketSndBufSize()).thenReturn(153600);
        when(nettyClientConfig.getClientSocketRcvBufSize()).thenReturn(153600);
        when(nettyClientConfig.getProtocol()).thenReturn(Protocol.GRPC.value);
        when(nettyClientConfig.getChannelMaxReadIdleSeconds()).thenReturn(0);
        when(nettyClientConfig.getChannelMaxWriteIdleSeconds()).thenReturn(5);
        when(nettyClientConfig.getChannelMaxAllIdleSeconds()).thenReturn(0);

        EventLoopGroup serverGroup = new NioEventLoopGroup(1);
        Channel serverChannel = null;
        NettyClientBootstrap clientBootstrap = null;
        Channel streamChannel = null;
        Channel parentChannel = null;
        try {
            CompletableFuture<RpcMessage> responseFuture = new CompletableFuture<>();
            ChannelHandler serverHandler = new SimpleChannelInboundHandler<RpcMessage>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
                    RpcMessage response = new RpcMessage();
                    response.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE);
                    response.setBody(HeartbeatMessage.PONG);
                    response.setId(msg.getId());
                    response.setHeadMap(new HashMap<>());
                    ctx.writeAndFlush(response);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    responseFuture.completeExceptionally(cause);
                }
            };

            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(serverGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new Http2Detector(new ChannelHandler[] {serverHandler}).getHandlers());
                        }
                    });
            Channel serverBound = serverBootstrap.bind("127.0.0.1", 0).sync().channel();
            serverChannel = serverBound;
            int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

            ChannelHandler clientHandler = new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    if (msg instanceof RpcMessage) {
                        responseFuture.complete((RpcMessage) msg);
                    }
                }

                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                    if (evt instanceof Http2ResetFrame) {
                        responseFuture.completeExceptionally(new AssertionError(
                                "Unexpected RST_STREAM with errorCode=" + ((Http2ResetFrame) evt).errorCode()));
                    }
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    responseFuture.completeExceptionally(cause);
                }
            };

            clientBootstrap = new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
            clientBootstrap.setChannelHandlers(clientHandler);
            clientBootstrap.start();

            streamChannel = clientBootstrap.getNewChannel(new InetSocketAddress("127.0.0.1", port));
            Assertions.assertInstanceOf(Http2StreamChannel.class, streamChannel);
            parentChannel = streamChannel.parent();
            Assertions.assertNotNull(streamChannel.pipeline().get(GrpcDecoder.class));
            Assertions.assertNotNull(streamChannel.pipeline().get(GrpcEncoder.class));

            RpcMessage ping = new RpcMessage();
            ping.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
            ping.setBody(HeartbeatMessage.PING);
            ping.setId(1);
            ping.setHeadMap(new HashMap<>());
            Assertions.assertTrue(streamChannel.writeAndFlush(ping).await(5, TimeUnit.SECONDS));

            RpcMessage response = responseFuture.get(5, TimeUnit.SECONDS);
            Assertions.assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE, response.getMessageType());
            Assertions.assertEquals(HeartbeatMessage.PONG, response.getBody());
            Assertions.assertEquals(1, response.getId());
        } finally {
            if (streamChannel != null) {
                streamChannel.close().awaitUninterruptibly(2, TimeUnit.SECONDS);
            }
            if (parentChannel != null) {
                parentChannel.close().awaitUninterruptibly(2, TimeUnit.SECONDS);
            }
            if (clientBootstrap != null) {
                clientBootstrap.shutdown();
                getEventLoopGroupWorker(clientBootstrap)
                        .shutdownGracefully(0, 2, TimeUnit.SECONDS)
                        .awaitUninterruptibly(5, TimeUnit.SECONDS);
            }
            if (serverChannel != null) {
                serverChannel.close().awaitUninterruptibly(2, TimeUnit.SECONDS);
            }
            serverGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
        }
    }

    private EventLoopGroup getEventLoopGroupWorker(NettyClientBootstrap bootstrap) {
        try {
            java.lang.reflect.Field field = NettyClientBootstrap.class.getDeclaredField("eventLoopGroupWorker");
            field.setAccessible(true);
            return (EventLoopGroup) field.get(bootstrap);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to access Netty client event loop group", e);
        }
    }

    private Bootstrap getBootstrap(NettyClientBootstrap bootstrap) {
        try {
            java.lang.reflect.Field field = NettyClientBootstrap.class.getDeclaredField("bootstrap");
            field.setAccessible(true);
            return (Bootstrap) field.get(bootstrap);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to access Netty client bootstrap", e);
        }
    }
}
