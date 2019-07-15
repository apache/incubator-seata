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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Geng Zhang
 */
public class ProtocolV1Client {

    Channel channel;

    PositiveAtomicCounter idGenerator = new PositiveAtomicCounter();

    Map<Integer, Future> futureMap = new HashMap<>();

    private EventLoopGroup eventLoopGroup = createWorkerGroup();

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
//            if (NetUtils.toAddressString((InetSocketAddress) channel.remoteAddress())
//                    .equals(NetUtils.toAddressString((InetSocketAddress) channel.localAddress()))) {
//                // 服务端不存活时，连接左右两侧地址一样的情况
//                channel.close(); // 关掉重连
//                throw new InitErrorException("Failed to connect " + host + ":" + port
//                        + ". Cause by: Remote and local address are the same");
//            }
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

    public static void main(String[] args) throws InterruptedException, TimeoutException, ExecutionException {
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

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(client.idGenerator.incrementAndGet());
        rpcMessage.setCodec(CodecType.SEATA.getCode());
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        rpcMessage.setHeadMap(head);
        rpcMessage.setBody(body);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST);

        Future future = client.send(rpcMessage.getId(), rpcMessage);
        RpcMessage resp = (RpcMessage) future.get(200, TimeUnit.SECONDS);

        System.out.println(resp.getId() + " " + resp.getBody());
    }


    private Future send(int msgId, Object msg) {
        if (channel != null) {
            DefaultPromise promise = new DefaultPromise(new DefaultEventExecutor(eventLoopGroup));
            futureMap.put(msgId, promise);
            channel.writeAndFlush(msg);
            return promise;
        }
        return null;
    }
}
