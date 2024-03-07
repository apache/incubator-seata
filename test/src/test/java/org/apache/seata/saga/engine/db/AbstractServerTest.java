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
package org.apache.seata.saga.engine.db;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.rpc.ShutdownHook;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.apache.seata.server.ParameterParser;
import org.apache.seata.server.UUIDGenerator;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.metrics.MetricsManager;
import org.apache.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Server Test
 *
 */
public abstract class AbstractServerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerTest.class);

    private static NettyRemotingServer nettyServer;
    private static final ThreadPoolExecutor workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
            new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    protected static void startSeataServer() throws InterruptedException {
        (new Thread(() -> {
            LOGGER.info("Starting Seata Server...");

            try {
                File file = new File("sessionStore/root.data");
                if (file.exists()) {
                    file.delete();
                }

                ParameterParser parameterParser = new ParameterParser();

                //initialize the metrics
                MetricsManager.get().init();

                nettyServer = new NettyRemotingServer(workingThreads);
                UUIDGenerator.init(parameterParser.getServerNode());
                //log store mode : file、db
                SessionHolder.init();

                DefaultCoordinator coordinator = DefaultCoordinator.getInstance(nettyServer);
                coordinator.init();
                nettyServer.setHandler(coordinator);
                // register ShutdownHook
                ShutdownHook.getInstance().addDisposable(coordinator);

                //127.0.0.1 and 0.0.0.0 are not valid here.
                if (NetUtil.isValidIp(parameterParser.getHost(), false)) {
                    XID.setIpAddress(parameterParser.getHost());
                } else {
                    XID.setIpAddress(NetUtil.getLocalIp());
                }
                XID.setPort(nettyServer.getListenPort());

                nettyServer.init();

                LOGGER.info("Seata Server started");
            } catch (Exception e) {
                LOGGER.error("Start Seata Server error: {}", e.getMessage(), e);
            }
        })).start();
        Thread.sleep(5000);
    }

    protected static void stopSeataServer() throws InterruptedException {
        if (nettyServer != null) {
			LOGGER.info("Stopping Seata Server...");

            try {
                nettyServer.destroy();
                LOGGER.info("Seata Server stopped");
            } catch (Exception e) {
                LOGGER.error("Stop Seata Server error: {}", e.getMessage(), e);
            }

			Thread.sleep(5000);
        }
    }

}
