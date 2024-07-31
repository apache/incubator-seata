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
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.server.ParameterParser;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.metrics.MetricsManager;
import org.apache.seata.server.session.SessionHolder;

/**
 * Abstract Server Test
 *
 */
public abstract class AbstractServerTest {


    private static NettyRemotingServer nettyServer;
    private static final ThreadPoolExecutor workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
            new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    protected static void startSeataServer() throws InterruptedException {
        (new Thread(new Runnable() {
            public void run() {
                File file = new File("sessionStore/root.data");
                if(file.exists()){
                    file.delete();
                }

                ParameterParser parameterParser = new ParameterParser();

                //initialize the metrics
                MetricsManager.get().init();

                NettyServerConfig nettyServerConfig = new NettyServerConfig();
                nettyServerConfig.setServerListenPort(8091);
                nettyServer = new NettyRemotingServer(workingThreads, nettyServerConfig);
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
            }
        })).start();
        Thread.sleep(5000);
    }

    protected static final void stopSeataServer() throws InterruptedException {
        if(nettyServer != null){
            nettyServer.destroy();
            Thread.sleep(5000);
        }
    }

}
