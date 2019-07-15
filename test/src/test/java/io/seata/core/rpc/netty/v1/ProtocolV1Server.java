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
package io.seata.core.rpc.netty.v1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.seata.common.thread.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Geng Zhang
 */
public class ProtocolV1Server {

    private int port = 8811;
    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public void start() {

        bossGroup = createBossGroup();
        workerGroup = createWorkerGroup();

        serverBootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_RCVBUF, 8192 * 128)
                .childOption(ChannelOption.SO_SNDBUF, 8192 * 128)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                        8192, 31768))
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new ProtocolV1Decoder(8 * 1024 * 1024));
                        pipeline.addLast(new ProtocolV1Encoder());
                        pipeline.addLast(new ServerChannelHandler());
                    }
                });

        String host = "0.0.0.0";

        ChannelFuture future = serverBootstrap.bind(new InetSocketAddress(host, port));
        ChannelFuture channelFuture = future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    throw new RuntimeException("Server start fail !", future.cause());
                }
            }
        });

        try {
            channelFuture.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        serverBootstrap = null;
    }

    private EventLoopGroup createBossGroup() {
        NamedThreadFactory threadName =
                new NamedThreadFactory("SEV-BOSS-" + port, false);
        return new NioEventLoopGroup(2, threadName);
    }

    private EventLoopGroup createWorkerGroup() {
        NamedThreadFactory threadName =
                new NamedThreadFactory("SEV-WORKER-" + port, false);
        return new NioEventLoopGroup(10, threadName);
    }

    public static void main(String[] args) {
        ProtocolV1Server server = new ProtocolV1Server();
        server.start();
    }
}
