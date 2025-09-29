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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base test class for Netty client tests to eliminate code duplication
 */
public abstract class BaseNettyClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseNettyClientTest.class);

    /**
     * Get a dynamic available port
     */
    protected static int getDynamicPort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    /**
     * Initialize message executor thread pool
     */
    protected static ThreadPoolExecutor initMessageExecutor() {
        return new ThreadPoolExecutor(
                5,
                5,
                500,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Start Seata server with dynamic port and intelligent waiting
     */
    protected ServerInstance startServer(int port) throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyServerConfig serverConfig = new NettyServerConfig();
        serverConfig.setServerListenPort(port);
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads, serverConfig);

        AtomicBoolean serverStatus = new AtomicBoolean();
        Thread thread = new Thread(() -> {
            try {
                SessionHolder.init(null);
                nettyRemotingServer.setHandler(DefaultCoordinator.getInstance(nettyRemotingServer));
                // set registry
                XID.setIpAddress(NetUtil.getLocalIp());
                XID.setPort(port);
                // init snowflake for transactionId, branchId
                UUIDGenerator.init(1L);
                System.out.println(
                        "pid info: " + ManagementFactory.getRuntimeMXBean().getName());
                nettyRemotingServer.init();
                serverStatus.set(true);
            } catch (Throwable t) {
                serverStatus.set(false);
                LOGGER.error("The seata-server failed to start", t);
            }
        });
        thread.start();

        // Wait for the seata-server to start with intelligent waiting
        long start = System.nanoTime();
        long maxWaitNanoTime = 10 * 1000 * 1000 * 1000L; // 10s
        while (System.nanoTime() - start < maxWaitNanoTime) {
            Thread.sleep(100);
            if (serverStatus.get()) {
                break;
            }
        }
        if (!serverStatus.get()) {
            throw new RuntimeException("Waiting for a while, but the seata-server did not start successfully.");
        }

        return new ServerInstance(nettyRemotingServer, port);
    }

    /**
     * Start server with simpler logic (for some tests that don't need intelligent waiting)
     */
    protected ServerInstance startServerSimple(int port) throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyServerConfig serverConfig = new NettyServerConfig();
        serverConfig.setServerListenPort(port);
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads, serverConfig);

        new Thread(() -> {
                    SessionHolder.init(null);
                    nettyRemotingServer.setHandler(DefaultCoordinator.getInstance(nettyRemotingServer));
                    // set registry
                    XID.setIpAddress(NetUtil.getLocalIp());
                    XID.setPort(port);
                    // init snowflake for transactionId, branchId
                    UUIDGenerator.init(1L);
                    nettyRemotingServer.init();
                })
                .start();

        Thread.sleep(3000); // Simple wait
        return new ServerInstance(nettyRemotingServer, port);
    }

    /**
     * Configure client to use the specified port
     */
    protected void configureClient(int port) {
        ConfigurationTestHelper.putConfig("service.default.grouplist", "127.0.0.1:" + port);
        ConfigurationTestHelper.putConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.valueOf(port));
    }

    /**
     * Clean up client configuration
     */
    protected void cleanupClientConfig() {
        ConfigurationTestHelper.removeConfig("service.default.grouplist");
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }

    /**
     * Server instance wrapper to hold server and port information
     */
    protected static class ServerInstance {
        private final NettyRemotingServer server;
        private final int port;

        public ServerInstance(NettyRemotingServer server, int port) {
            this.server = server;
            this.port = port;
        }

        public NettyRemotingServer getServer() {
            return server;
        }

        public int getPort() {
            return port;
        }

        public String getAddress() {
            return "127.0.0.1:" + port;
        }

        public void destroy() {
            if (server != null) {
                server.destroy();
            }
        }
    }

    /**
     * Clean up configuration after each test
     */
    @AfterEach
    public void cleanupAfterTest() {
        cleanupClientConfig();
    }
}
