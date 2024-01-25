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
package io.seata.mockserver;

import io.seata.common.XID;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.server.UUIDGenerator;
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

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MockServer.class, args);
        start();
    }

    public static void start() {
        workingThreads = new ThreadPoolExecutor(50,
                50, 500, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20000),
                new NamedThreadFactory("ServerHandlerThread", 500), new ThreadPoolExecutor.CallerRunsPolicy());
        nettyRemotingServer = new MockNettyRemotingServer(workingThreads);

        // set registry
        XID.setIpAddress(NetUtil.getLocalIp());
        XID.setPort(8092);
        // init snowflake for transactionId, branchId
        UUIDGenerator.init(1L);

        MockCoordinator coordinator = MockCoordinator.getInstance();
        coordinator.setRemotingServer(nettyRemotingServer);
        nettyRemotingServer.setHandler(coordinator);
        nettyRemotingServer.init();

        LOGGER.info("pid info: " + ManagementFactory.getRuntimeMXBean().getName());
    }

    public static void close() {
        workingThreads.shutdown();
        nettyRemotingServer.destroy();
    }
}
