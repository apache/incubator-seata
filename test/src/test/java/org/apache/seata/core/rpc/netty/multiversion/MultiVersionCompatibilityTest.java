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
package org.apache.seata.core.rpc.netty.multiversion;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.MultiProtocolDecoderTest;
import org.apache.seata.core.rpc.netty.TestClientHandler;
import org.apache.seata.core.rpc.netty.TestServerHandler;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
import org.apache.seata.core.rpc.netty.v2.ProtocolEncoderV2;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prerequisites for running these tests:
 * If maven environment has dependency issues, run these commands first:
 * 1. chmod -R u+rwx ./*
 * 2. mvn -Prelease-seata -Dmaven.test.skip=true clean install -U
 * 
 * This provides common utilities for testing multi-version protocol compatibility
 * and simulates realistic server and client construction flows.
 */
public abstract class MultiVersionCompatibilityTest {

    // LOG instance
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiVersionCompatibilityTest.class);

    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected EventLoopGroup clientGroup;
    protected Channel serverChannel;
    protected Channel clientChannel;
    protected final AtomicReference<Object> requestRef = new AtomicReference<>();
    protected final AtomicReference<Object> responseRef = new AtomicReference<>();
    protected final CountDownLatch responseLatch = new CountDownLatch(1);
    
    private final MultiProtocolDecoderTest decoderTestHelper = new MultiProtocolDecoderTest();

    @BeforeEach
    public void setUp() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        clientGroup = new NioEventLoopGroup();
        requestRef.set(null);
        responseRef.set(null);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (clientChannel != null) {
            clientChannel.close().sync();
        }
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
        clientGroup.shutdownGracefully().sync();
    }


    protected void startV1Server(int port) throws InterruptedException {
        startServerByVersion(ProtocolConstants.VERSION_1, port);
    }

    protected void startV2Server(int port) throws InterruptedException {
        startServerByVersion(ProtocolConstants.VERSION_2, port);
    }

    protected void connectV1Client(String host, int port, int connectTimeout) {
        connectClientByVersion(new ProtocolEncoderV1(), ProtocolConstants.VERSION_1, host, port, connectTimeout);
    }

    protected void connectV2Client(String host, int port, int connectTimeout) {
        connectClientByVersion(new ProtocolEncoderV2(), ProtocolConstants.VERSION_2, host, port, connectTimeout);
    }

    private void startServerByVersion(byte version1, int port) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // Simulate V1 server with forced V1 version in MultiProtocolDecoder
                        pipeline.addLast(new IdleStateHandler(0, 0, 30));
                        pipeline.addLast(decoderTestHelper.createMultiProtocolDecoder(version1, createTestServerHandler()));
                    }
                });

        ChannelFuture future = serverBootstrap.bind(port).sync();
        serverChannel = future.channel();
    }

    private void connectClientByVersion(MessageToByteEncoder encoder, byte version, String host, int port, int connectTimeout) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup).channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                // Simulate V1 client with forced V1 version in MultiProtocolDecoder
                pipeline.addLast(new IdleStateHandler(0, 0, 15));
                pipeline.addLast(encoder); // V1 client uses V1 encoder
                pipeline.addLast(decoderTestHelper.createMultiProtocolDecoder(version, createTestClientHandler()));
            }
        });

        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.awaitUninterruptibly(connectTimeout, TimeUnit.MILLISECONDS);
        if (channelFuture.isSuccess()) {
            clientChannel = channelFuture.channel();
        }
    }

    /**
     * Send request through client channel
     */
    protected void sendRequest(Object request) {
        if (clientChannel != null && clientChannel.isActive()) {
            // Wrap request in RpcMessage as real clients do
            RpcMessage rpcMessage = buildRequestMessage(request);
            clientChannel.writeAndFlush(rpcMessage);
        }
    }
    
    /**
     * Build RpcMessage as real clients do
     */
    private RpcMessage buildRequestMessage(Object msg) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(getNextMessageId());
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        rpcMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        rpcMessage.setBody(msg);
        return rpcMessage;
    }
    
    private static final AtomicInteger MESSAGE_ID_GENERATOR = new AtomicInteger(0);
    
    private int getNextMessageId() {
        return MESSAGE_ID_GENERATOR.incrementAndGet();
    }

    /**
     * Send V1 request through client channel (same as sendRequest since all clients use V2 encoders)
     */
    protected void sendV1Request(Object request) {
        // All clients use V2 encoders, so just use sendRequest
        sendRequest(request);
    }

    /**
     * Create test server handler with shared state
     */
    protected TestServerHandler createTestServerHandler() {
        return new TestServerHandler(requestRef, null);
    }

    /**
     * Create test client handler with shared state
     */
    protected TestClientHandler createTestClientHandler() {
        return new TestClientHandler(responseRef, responseLatch);
    }

    @NotNull
    protected RegisterTMResponse doSendRegister(String extraData) throws InterruptedException {
        RegisterTMRequest request = new RegisterTMRequest("testApp", "testGroup");
        if(StringUtils.isNotBlank(extraData)){
            request.setExtraData(extraData);
        }
        sendRequest(request);

        // Wait for response
        boolean received = responseLatch.await(30, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "Should receive response within timeout");

        Object response = responseRef.get();
        Assertions.assertNotNull(response, "Should receive response from server");
        LOGGER.info("Received response: {}", response);

        RegisterTMResponse tmResponse = (RegisterTMResponse) response;
        return tmResponse;
    }
}