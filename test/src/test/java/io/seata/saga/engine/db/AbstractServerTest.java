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
package io.seata.saga.engine.db;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.netty.NettyRemotingServer;
import io.seata.server.ParameterParser;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.metrics.MetricsManager;
import io.seata.server.session.SessionHolder;

/**
 * Abstract Server Test
 *
 * @author lorne.cl
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