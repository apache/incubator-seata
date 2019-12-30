/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.core.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.PlatformDependent;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.rpc.RemotingClient;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import io.seata.core.rpc.netty.v1.ProtocolV1Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rpc client.
 *
 * @author slievrly
 * @author zhaojun
 */
public class RpcClientBootstrap implements RemotingClient {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRpcRemotingClient.class);
    private final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker;
    private EventExecutorGroup defaultEventExecutorGroup;
    private AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool> clientChannelPool;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    private final ChannelHandler channelHandler;
    private final NettyPoolKey.TransactionRole transactionRole;
    
    public RpcClientBootstrap(NettyClientConfig nettyClientConfig, final EventExecutorGroup eventExecutorGroup,
                              ChannelHandler channelHandler, NettyPoolKey.TransactionRole transactionRole) {
        if (null == nettyClientConfig) {
            nettyClientConfig = new NettyClientConfig();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("use default netty client config.");
            }
        }
        this.nettyClientConfig = nettyClientConfig;
        int selectorThreadSizeThreadSize = this.nettyClientConfig.getClientSelectorThreadSize();
        this.transactionRole = transactionRole;
        this.eventLoopGroupWorker = new NioEventLoopGroup(selectorThreadSizeThreadSize,
            new NamedThreadFactory(getThreadPrefix(this.nettyClientConfig.getClientSelectorThreadPrefix()),
                selectorThreadSizeThreadSize));
        this.defaultEventExecutorGroup = eventExecutorGroup;
        this.channelHandler = channelHandler;
    }
    
    @Override
    public void start() {
        if (this.defaultEventExecutorGroup == null) {
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                new NamedThreadFactory(getThreadPrefix(nettyClientConfig.getClientWorkerThreadPrefix()),
                    nettyClientConfig.getClientWorkerThreads()));
        }
        this.bootstrap.group(this.eventLoopGroupWorker).channel(
            nettyClientConfig.getClientChannelClazz()).option(
            ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).option(
            ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()).option(
            ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()).option(ChannelOption.SO_RCVBUF,
            nettyClientConfig.getClientSocketRcvBufSize());
    
        if (nettyClientConfig.enableNative()) {
            if (PlatformDependent.isOsx()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("client run on macOS");
                }
            } else {
                bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED)
                    .option(EpollChannelOption.TCP_QUICKACK, true);
            }
        }
        if (nettyClientConfig.isUseConnPool()) {
            clientChannelPool = new AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool>() {
                @Override
                protected FixedChannelPool newPool(InetSocketAddress key) {
                    return new FixedChannelPool(
                        bootstrap.remoteAddress(key),
                        new DefaultChannelPoolHandler() {
                            @Override
                            public void channelCreated(Channel ch) throws Exception {
                                super.channelCreated(ch);
                                final ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(defaultEventExecutorGroup,
                                    new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                        nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                        nettyClientConfig.getChannelMaxAllIdleSeconds()));
                                pipeline.addLast(defaultEventExecutorGroup, new RpcClientHandler());
                            }
                        },
                        ChannelHealthChecker.ACTIVE,
                        FixedChannelPool.AcquireTimeoutAction.FAIL,
                        nettyClientConfig.getMaxAcquireConnMills(),
                        nettyClientConfig.getPerHostMaxConn(),
                        nettyClientConfig.getPendingConnSize(),
                        false
                    );
                }
            };
        } else {
            bootstrap.handler(
                new ChannelInitializer<SocketChannel>() {
                
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                            new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                nettyClientConfig.getChannelMaxAllIdleSeconds()))
                                .addLast(new ProtocolV1Decoder())
                                .addLast(new ProtocolV1Encoder());
                        if (null != channelHandler) {
                            ch.pipeline().addLast(channelHandler);
                        }
                    }
                });
        }
        if (initialized.compareAndSet(false, true) && LOGGER.isInfoEnabled()) {
            LOGGER.info("RpcClientBootstrap has started");
        }
    }
    
    @Override
    public void shutdown() {
        try {
            if (null != clientChannelPool) {
                clientChannelPool.close();
            }
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception exx) {
            LOGGER.error("Failed to shutdown: {}", exx.getMessage());
        }
    }
    
    /**
     * Gets new channel.
     *
     * @param address the address
     * @return the new channel
     */
    public Channel getNewChannel(InetSocketAddress address) {
        Channel channel;
        ChannelFuture f = this.bootstrap.connect(address);
        try {
            f.await(this.nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (f.isCancelled()) {
                throw new FrameworkException(f.cause(), "connect cancelled, can not connect to services-server.");
            } else if (!f.isSuccess()) {
                throw new FrameworkException(f.cause(), "connect failed, can not connect to services-server.");
            } else {
                channel = f.channel();
            }
        } catch (Exception e) {
            throw new FrameworkException(e, "can not connect to services-server.");
        }
        return channel;
    }
    
    /**
     * Gets thread prefix.
     *
     * @param threadPrefix the thread prefix
     * @return the thread prefix
     */
    private String getThreadPrefix(String threadPrefix) {
        return threadPrefix + THREAD_PREFIX_SPLIT_CHAR + transactionRole.name();
    }
}
