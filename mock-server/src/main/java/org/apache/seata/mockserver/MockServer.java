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
package org.apache.seata.mockserver;

import org.apache.seata.common.XID;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The type Mock Server.
 */
@SpringBootApplication
public class MockServer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockServer.class);

    public static final int MOCK_DEFAULT_PORT = 10091;
    public static final String MOCK_SEATA_PORT_KEY = "SEATA_MOCK_PORT";

    private ThreadPoolExecutor workingThreads;
    private MockNettyRemotingServer nettyRemotingServer;
    private MockCoordinator coordinator;
    private int port;
    private volatile boolean started = false;

    public MockServer() {}

    /**
     * Start this mock server instance on the specified port.
     * If port is 0, a random available port will be assigned.
     *
     * @param port the port to listen on, 0 for random port
     */
    public synchronized void start(int port) {
        start(port, new MockCoordinator());
    }

    /**
     * Start this mock server instance on the specified port with the given coordinator.
     *
     * @param port the port to listen on, 0 for random port
     * @param coordinator the mock coordinator to use
     */
    public synchronized void start(int port, MockCoordinator coordinator) {
        if (started) {
            return;
        }
        if (port == 0) {
            port = findAvailablePort();
        }

        ConfigurationCache.clear();
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        System.clearProperty("server.port");

        workingThreads = new ThreadPoolExecutor(
                50,
                50,
                500,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20000),
                new ThreadPoolExecutor.CallerRunsPolicy());
        NettyServerConfig config = new NettyServerConfig();
        config.setServerListenPort(port);
        nettyRemotingServer = new MockNettyRemotingServer(workingThreads, config);

        XID.setIpAddress(NetUtil.getLocalIp());
        XID.setPort(port);
        Instance.getInstance().setTransaction(new Node.Endpoint(XID.getIpAddress(), XID.getPort(), "netty"));
        UUIDGenerator.init(1L);

        this.coordinator = coordinator;
        coordinator.setRemotingServer(nettyRemotingServer);
        nettyRemotingServer.setHandler(coordinator);
        nettyRemotingServer.init();

        this.port = port;
        this.started = true;
        LOGGER.info("MockServer started on port: {}", port);
    }

    /**
     * Get the actual port this server is listening on.
     *
     * @return the listening port
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the coordinator instance of this server.
     *
     * @return the mock coordinator
     */
    public MockCoordinator getCoordinator() {
        return coordinator;
    }

    /**
     * Get the netty remoting server instance.
     *
     * @return the mock netty remoting server
     */
    public MockNettyRemotingServer getNettyRemotingServer() {
        return nettyRemotingServer;
    }

    /**
     * Close this mock server instance.
     */
    public synchronized void close() {
        if (started) {
            started = false;
            if (workingThreads != null) {
                workingThreads.shutdown();
            }
            if (nettyRemotingServer != null) {
                nettyRemotingServer.destroy();
            }
        }
    }

    // ==================== Static convenience methods for backward compatibility ====================

    private static volatile MockServer defaultInstance;

    /**
     * Start the default (singleton) mock server on specified port.
     * Retained for backward compatibility with existing tests.
     *
     * @param port the port to listen on
     * @return the default MockServer instance
     */
    public static MockServer startDefault(int port) {
        if (defaultInstance == null) {
            synchronized (MockServer.class) {
                if (defaultInstance == null) {
                    defaultInstance = new MockServer();
                    defaultInstance.start(port, MockCoordinator.getInstance());
                }
            }
        }
        return defaultInstance;
    }

    /**
     * Get the default mock server instance.
     *
     * @return the default MockServer instance, or null if not started
     */
    public static MockServer getDefault() {
        return defaultInstance;
    }

    /**
     * Close the default (singleton) mock server.
     */
    public static void closeDefault() {
        if (defaultInstance != null) {
            synchronized (MockServer.class) {
                if (defaultInstance != null) {
                    defaultInstance.close();
                    defaultInstance = null;
                }
            }
        }
    }

    /**
     * Find a random available port.
     *
     * @return available port number
     */
    public static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find available port", e);
        }
    }

    /**
     * The entry point of application (standalone deployment).
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MockServer.class, args);
        int port = NumberUtils.toInt(System.getenv(MOCK_SEATA_PORT_KEY), MOCK_DEFAULT_PORT);

        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid port number provided, using default port: {}", port, e);
            }
        }

        startDefault(port);
    }
}
