/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.core.rpc.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import com.alibaba.fescar.common.thread.NamedThreadFactory;
import com.alibaba.fescar.core.rpc.RemotingServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rpc remoting server.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018 /9/12 11:25
 * @FileName: AbstractRpcRemotingServer
 * @Description:
 */
public abstract class AbstractRpcRemotingServer extends AbstractRpcRemoting implements RemotingServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRpcRemotingServer.class);
    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupWorker;
    private final EventLoopGroup eventLoopGroupBoss;
    private final NettyServerConfig nettyServerConfig;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private int listenPort;

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getListenPort() {
        return listenPort;
    }

    /**
     * Instantiates a new Rpc remoting server.
     *
     * @param nettyServerConfig the netty server config
     */
    public AbstractRpcRemotingServer(final NettyServerConfig nettyServerConfig) {
        this(nettyServerConfig, null);
    }

    /**
     * Instantiates a new Rpc remoting server.
     *
     * @param nettyServerConfig the netty server config
     * @param messageExecutor   the message executor
     * @param handlers          the handlers
     */
    public AbstractRpcRemotingServer(final NettyServerConfig nettyServerConfig,
                                     final ThreadPoolExecutor messageExecutor, final ChannelHandler... handlers) {
        super(messageExecutor);
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = nettyServerConfig;
        this.eventLoopGroupBoss = new NioEventLoopGroup(nettyServerConfig.getBossThreadSize(),
            new NamedThreadFactory(nettyServerConfig.getBossThreadPrefix(), nettyServerConfig.getBossThreadSize()));
        if (NettyServerConfig.enableEpool()) {
            this.eventLoopGroupWorker = new EpollEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
                new NamedThreadFactory(nettyServerConfig.getWorkerThreadPrefix(),
                    nettyServerConfig.getServerWorkerThreads()));
        } else {
            this.eventLoopGroupWorker = new NioEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
                new NamedThreadFactory(nettyServerConfig.getWorkerThreadPrefix(),
                    nettyServerConfig.getServerWorkerThreads()));
        }
        if (null != handlers) {
            channelHandlers = handlers;
        }

    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
            nettyServerConfig.getServerWorkerThreads(),
            new NamedThreadFactory(nettyServerConfig.getExecutorThreadPrefix(),
                nettyServerConfig.getServerWorkerThreads()));
        if (listenPort == 0) {
            listenPort = nettyServerConfig.getDefaultListenPort();
        }
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupWorker)
            .channel(nettyServerConfig.SERVER_CHANNEL_CLAZZ)
            .option(ChannelOption.SO_BACKLOG, nettyServerConfig.getSoBackLogSize())
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSendBufSize())
            .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketResvBufSize())
            .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,
                nettyServerConfig.getWriteBufferHighWaterMark())
            .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, nettyServerConfig.getWriteBufferLowWaterMark())
            .localAddress(new InetSocketAddress(listenPort))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new IdleStateHandler(nettyServerConfig.getChannelMaxReadIdleSeconds(), 0, 0))
                        .addLast(new MessageCodecHandler());
                    if (null != channelHandlers) {
                        addChannelPipelineLast(ch, channelHandlers);
                    }

                }
            });

        if (nettyServerConfig.isEnableServerPooledByteBufAllocator()) {
            this.serverBootstrap.childOption(ChannelOption.ALLOCATOR, NettyServerConfig.DIRECT_BYTE_BUF_ALLOCATOR);
        }

        try {
            LOGGER.info("Server starting ... ");
            ChannelFuture future = this.serverBootstrap.bind(listenPort).sync();
            LOGGER.info("Server started ... ");
            future.channel().closeFuture().sync();
        } catch (InterruptedException exx) {
            throw new RuntimeException(exx);
        }

    }

    @Override
    public void shutdown() {
        try {
            this.eventLoopGroupBoss.shutdownGracefully();
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
    }

    @Override
    public void destroyChannel(String serverAddress, Channel channel) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will destroy channel:" + channel + ",address:" + serverAddress);
        }
        channel.disconnect();
        channel.close();
    }

}
