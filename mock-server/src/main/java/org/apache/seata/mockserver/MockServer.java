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
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The type Mock Server.
 */
@SpringBootApplication
public class MockServer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockServer.class);

    private static ThreadPoolExecutor workingThreads;
    private static MockNettyRemotingServer nettyRemotingServer;

    private static volatile boolean inited = false;

    public static final int MOCK_DEFAULT_PORT = 10091;
    public static String MOCK_SEATA_PORT_KEY = "SEATA_MOCK_PORT";

    /**
     * The entry point of application.
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

        start(port);
    }

    public static void start(int port) {
        if (!inited) {
            synchronized (MockServer.class) {
                if (!inited) {
                    inited = true;
                    workingThreads = new ThreadPoolExecutor(
                            50,
                            50,
                            500,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(20000),
                            new NamedThreadFactory("ServerHandlerThread", 500),
                            new ThreadPoolExecutor.CallerRunsPolicy());
                    NettyServerConfig config = new NettyServerConfig();
                    config.setServerListenPort(port);
                    nettyRemotingServer = new MockNettyRemotingServer(workingThreads, config);

                    // set registry
                    XID.setIpAddress(NetUtil.getLocalIp());
                    XID.setPort(port);
                    // init snowflake for transactionId, branchId
                    Instance.getInstance()
                            .setTransaction(new Node.Endpoint(XID.getIpAddress(), XID.getPort(), "netty"));
                    UUIDGenerator.init(1L);

                    MockCoordinator coordinator = MockCoordinator.getInstance();
                    coordinator.setRemotingServer(nettyRemotingServer);
                    nettyRemotingServer.setHandler(coordinator);
                    nettyRemotingServer.init();
                    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LOGGER.info("system is closing , pid info: "
                                    + ManagementFactory.getRuntimeMXBean().getName());
                        }
                    }));
                    LOGGER.info(
                            "pid info: " + ManagementFactory.getRuntimeMXBean().getName());
                    LOGGER.info("MockServer started on port: {}", port);
                }
            }
        }
    }

    public static void close() {
        if (inited) {
            synchronized (MockServer.class) {
                if (inited) {
                    inited = false;
                    workingThreads.shutdown();
                    nettyRemotingServer.destroy();
                }
            }
        }
    }
}
