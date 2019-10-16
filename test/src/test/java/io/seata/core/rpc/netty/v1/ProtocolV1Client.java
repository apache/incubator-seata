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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.PositiveAtomicCounter;
import io.seata.core.codec.CodecType;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Geng Zhang
 */
public class ProtocolV1Client {

    /**
     * Logger for ProtocolV1Client
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV1Client.class);

    Channel channel;

    PositiveAtomicCounter idGenerator = new PositiveAtomicCounter();

    Map<Integer, Future> futureMap = new ConcurrentHashMap<>();

    private EventLoopGroup eventLoopGroup = createWorkerGroup();

    private DefaultEventExecutor defaultEventExecutor = new DefaultEventExecutor(eventLoopGroup);

    public void connect(String host, int port, int connectTimeout) {

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        bootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new ProtocolV1Encoder());
                pipeline.addLast(new ProtocolV1Decoder(8 * 1024 * 1024));
                pipeline.addLast(new ClientChannelHandler(ProtocolV1Client.this));
            }
        });
        // Bind and start to accept incoming connections.
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.awaitUninterruptibly(connectTimeout, TimeUnit.MILLISECONDS);
        if (channelFuture.isSuccess()) {
            channel = channelFuture.channel();
        } else {
            Throwable cause = channelFuture.cause();
            throw new RuntimeException("Failed to connect " + host + ":" + port +
                    (cause != null ? ". Cause by: " + cause.getMessage() : "."));
        }
    }

    private EventLoopGroup createWorkerGroup() {
        NamedThreadFactory threadName =
                new NamedThreadFactory("CLI-WORKER", false);
        return new NioEventLoopGroup(10, threadName);
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        channel = null;
    }

    public Future sendRpc(Map<String, String> head, Object body) {
        int msgId = idGenerator.incrementAndGet();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(msgId);
        rpcMessage.setCodec(CodecType.SEATA.getCode());
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        rpcMessage.setHeadMap(head);
        rpcMessage.setBody(body);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST);

        if (channel != null) {
            DefaultPromise promise = new DefaultPromise(defaultEventExecutor);
            futureMap.put(msgId, promise);
            channel.writeAndFlush(rpcMessage);
            return promise;
        } else {
            LOGGER.warn("channel is null");
        }
        return null;
    }

    // can test tps
    public static void main(String[] args) {
        ProtocolV1Client client = new ProtocolV1Client();
        client.connect("127.0.0.1", 8811, 500);

        Map<String, String> head = new HashMap<>();
        head.put("tracerId", "xxadadadada");
        head.put("token", "adadadad");

        BranchCommitRequest body = new BranchCommitRequest();
        body.setBranchId(12345L);
        body.setApplicationData("application");
        body.setBranchType(BranchType.AT);
        body.setResourceId("resource-1234");
        body.setXid("xid-1234");

        final int threads = 50;
        final AtomicLong cnt = new AtomicLong(0);
        // no queue
        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("client-", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                while (true) {
                    try {
                        Future future = client.sendRpc(head, body);
                        RpcMessage resp = (RpcMessage) future.get(200, TimeUnit.MILLISECONDS);
                        if (resp != null) {
                            cnt.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            });
        }

        Thread thread = new Thread(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                while (true) {
                    long count = cnt.get();
                    long tps = count - last;
                    LOGGER.error("last 1s invoke: {}, queue: {}", tps, service1.getQueue().size());
                    last = count;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "Print-tps-THREAD");
        thread.start();
    }


}
