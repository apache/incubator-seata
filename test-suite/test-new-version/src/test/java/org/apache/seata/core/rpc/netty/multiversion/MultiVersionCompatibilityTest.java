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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.seata.common.XID;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.MultiProtocolDecoderTest;
import org.apache.seata.core.rpc.netty.NettyClientBootstrap;
import org.apache.seata.core.rpc.netty.NettyClientConfig;
import org.apache.seata.core.rpc.netty.NettyPoolKey;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.core.rpc.netty.TestClientHandler;
import org.apache.seata.core.rpc.netty.TestServerHandler;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
import org.apache.seata.mockserver.MockCoordinator;
import org.apache.seata.mockserver.MockNettyRemotingServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This provides common utilities for testing multi-version protocol compatibility.
 * Supports testing all 2x2 combinations:
 * - V1 Server + V1 Client (manual construction - simulates legacy)
 * - V1 Server + V2 Client (manual construction - simulates legacy)
 * - V2 Server + V1 Client (MockNettyRemotingServer + manual client)
 * - V2 Server + V2 Client (MockNettyRemotingServer + NettyClientBootstrap - production-like)
 */
public abstract class MultiVersionCompatibilityTest {

    // LOG instance
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiVersionCompatibilityTest.class);

    // JSON ObjectMapper for pretty printing
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    /**
     * Convert object to pretty JSON format for logging
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to convert object to JSON, using toString(): {}", e.getMessage());
            return obj.toString();
        }
    }

    // ========== V1 Server (manual construction for legacy simulation) ==========
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected Channel serverChannel;

    // ========== V2 Server (using production MockNettyRemotingServer) ==========
    protected MockNettyRemotingServer mockRemotingServer;
    protected ThreadPoolExecutor serverWorkingThreads;

    // ========== V1 Client (manual construction for legacy simulation) ==========
    protected EventLoopGroup clientGroup;
    protected Channel clientChannel;

    // ========== V2 Client (using production NettyClientBootstrap) ==========
    protected NettyClientBootstrap clientBootstrap;

    // ========== Common ==========
    protected TestClientHandler testClientHandler;
    protected final AtomicReference<Object> requestRef = new AtomicReference<>();
    protected final AtomicReference<Object> responseRef = new AtomicReference<>();
    protected CountDownLatch responseLatch;

    // Helper for creating MultiProtocolDecoder with specific version (for V1 tests)
    private final MultiProtocolDecoderTest decoderTestHelper = new MultiProtocolDecoderTest();

    @BeforeEach
    public void setUp() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        clientGroup = new NioEventLoopGroup();
        requestRef.set(null);
        responseRef.set(null);
        responseLatch = new CountDownLatch(1);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // Shutdown client
        if (clientChannel != null) {
            clientChannel.close().sync();
        }
        if (clientBootstrap != null) {
            clientBootstrap.shutdown();
        }

        // Shutdown V1 server
        if (serverChannel != null) {
            serverChannel.close().sync();
        }

        // Shutdown V2 server (MockNettyRemotingServer)
        if (mockRemotingServer != null) {
            mockRemotingServer.destroy();
        }
        if (serverWorkingThreads != null) {
            serverWorkingThreads.shutdown();
        }

        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
        clientGroup.shutdownGracefully().sync();
    }

    // ==================== V1 Server Methods (manual, for legacy simulation) ====================

    /**
     * Start a V1 protocol server (manual construction to simulate legacy server)
     */
    protected void startV1Server(int port) throws InterruptedException {
        startServerByVersion(ProtocolConstants.VERSION_1, port);
    }

    private void startServerByVersion(byte version, int port) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(0, 0, 30));
                        pipeline.addLast(
                                decoderTestHelper.createMultiProtocolDecoder(version, createTestServerHandler()));
                    }
                });

        ChannelFuture future = serverBootstrap.bind(port).sync();
        serverChannel = future.channel();
        LOGGER.info("V1 Server started on port {} (manual construction)", port);
    }

    // ==================== V2 Server Methods (using MockNettyRemotingServer) ====================

    /**
     * Start a V2 protocol server using production MockNettyRemotingServer.
     * This uses the real server bootstrap with all production handlers:
     * - ProtocolDetectHandler -> SeataDetector -> MultiProtocolDecoder
     * - MockRegisterProcessor (handles TM/RM registration)
     * - MockHeartbeatProcessor (handles heartbeat)
     */
    protected void startV2Server(int port) {
        serverWorkingThreads = new ThreadPoolExecutor(
                10,
                10,
                500,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new NamedThreadFactory("MockServerThread", 10),
                new ThreadPoolExecutor.CallerRunsPolicy());

        NettyServerConfig config = new NettyServerConfig();
        config.setServerListenPort(port);
        mockRemotingServer = new MockNettyRemotingServer(serverWorkingThreads, config);

        // Initialize XID for the server
        XID.setIpAddress(NetUtil.getLocalIp());
        XID.setPort(port);
        Instance.getInstance().setTransaction(new Node.Endpoint(XID.getIpAddress(), XID.getPort(), "netty"));
        UUIDGenerator.init(1L);

        MockCoordinator coordinator = MockCoordinator.getInstance();
        coordinator.setRemotingServer(mockRemotingServer);
        mockRemotingServer.setHandler(coordinator);
        mockRemotingServer.init();

        LOGGER.info("V2 Server started on port {} (using MockNettyRemotingServer)", port);
    }

    // ==================== V1 Client Methods (manual, for legacy simulation) ====================

    /**
     * Connect V1 client (manual construction to simulate legacy client)
     */
    protected void connectV1Client(String host, int port, int connectTimeout) {
        connectClientByVersion(new ProtocolEncoderV1(), ProtocolConstants.VERSION_1, host, port, connectTimeout);
    }

    private void connectClientByVersion(
            MessageToByteEncoder encoder, byte version, String host, int port, int connectTimeout) {
        testClientHandler = createTestClientHandler();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup).channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new IdleStateHandler(0, 0, 15));
                pipeline.addLast(encoder);
                pipeline.addLast(decoderTestHelper.createMultiProtocolDecoder(version, testClientHandler));
            }
        });

        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.awaitUninterruptibly(connectTimeout, TimeUnit.MILLISECONDS);
        if (channelFuture.isSuccess()) {
            clientChannel = channelFuture.channel();
            LOGGER.info("V1 Client connected to {}:{} (manual construction)", host, port);
        }
    }

    // ==================== V2 Client Methods (using NettyClientBootstrap) ====================

    /**
     * Connect V2 client using production NettyClientBootstrap.
     * This uses the real client bootstrap with:
     * - ProtocolEncoderV2
     * - MultiProtocolDecoder
     */
    protected void connectV2Client(String host, int port, int connectTimeout) {
        testClientHandler = createTestClientHandler();

        NettyClientConfig config = new NettyClientConfig();
        config.setConnectTimeoutMillis(connectTimeout);

        clientBootstrap = new NettyClientBootstrap(config, NettyPoolKey.TransactionRole.TMROLE);
        clientBootstrap.setChannelHandlers(testClientHandler);
        clientBootstrap.start();

        clientChannel = clientBootstrap.getNewChannel(new InetSocketAddress(host, port));
        LOGGER.info("V2 Client connected to {}:{} (using NettyClientBootstrap)", host, port);
    }

    // ==================== Request/Response Methods ====================

    /**
     * Send request through client channel
     */
    protected void sendRequest(Object request) {
        if (clientChannel != null && clientChannel.isActive()) {
            RpcMessage rpcMessage = buildRequestMessage(request);
            clientChannel.writeAndFlush(rpcMessage);
        }
    }

    /**
     * Send heartbeat PING through client channel
     */
    protected void sendHeartbeatPing() {
        if (clientChannel != null && clientChannel.isActive()) {
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setId(getNextMessageId());
            rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
            rpcMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
            rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
            rpcMessage.setBody(HeartbeatMessage.PING);
            clientChannel.writeAndFlush(rpcMessage);
        }
    }

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

    // ==================== Handler Factory Methods ====================

    /**
     * Create test server handler (for V1 server manual construction)
     */
    protected TestServerHandler createTestServerHandler() {
        return new TestServerHandler(requestRef, null);
    }

    /**
     * Create test client handler (needed to capture responses for verification)
     */
    protected TestClientHandler createTestClientHandler() {
        return new TestClientHandler(responseRef, responseLatch);
    }

    /**
     * Reset response latch for next request/response cycle
     */
    protected void resetResponseLatch() {
        responseLatch = new CountDownLatch(1);
        responseRef.set(null);
        if (testClientHandler != null) {
            testClientHandler.resetLatch(responseLatch);
        }
    }

    // ==================== Test Helper Methods ====================

    @NotNull
    protected RegisterTMResponse doSendRegister(String extraData) throws InterruptedException {
        RegisterTMRequest request = new RegisterTMRequest("testApp", "testGroup");
        if (StringUtils.isNotBlank(extraData)) {
            request.setExtraData(extraData);
        }
        LOGGER.info("Sending RegisterTMRequest:\n{}", toPrettyJson(request));
        sendRequest(request);

        boolean received = responseLatch.await(30, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "Should receive response within timeout");

        Object response = responseRef.get();
        Assertions.assertNotNull(response, "Should receive response from server");
        LOGGER.info("Received RegisterTMResponse:\n{}", toPrettyJson(response));

        RegisterTMResponse tmResponse = (RegisterTMResponse) response;
        return tmResponse;
    }

    /**
     * Send heartbeat PING and verify PONG response.
     */
    protected void doSendHeartbeatAndVerify() throws InterruptedException {
        resetResponseLatch();

        LOGGER.info("Sending HeartbeatMessage: PING");
        sendHeartbeatPing();

        boolean received = responseLatch.await(30, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "Should receive heartbeat response within timeout");

        Object response = responseRef.get();
        Assertions.assertNotNull(response, "Should receive heartbeat response from server");
        LOGGER.info("Received HeartbeatMessage: {}", response);

        Assertions.assertInstanceOf(HeartbeatMessage.class, response, "Response should be HeartbeatMessage");
        HeartbeatMessage heartbeatResponse = (HeartbeatMessage) response;
        Assertions.assertFalse(heartbeatResponse.isPing(), "Response should be PONG (isPing=false)");
    }
}
