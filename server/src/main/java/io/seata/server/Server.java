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
package io.seata.server;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.XID;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.core.rpc.netty.RpcServer;
import io.seata.core.rpc.netty.ShutdownHook;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.session.SessionHolder;

/**
 * The type Server.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class Server {

    private static final int MIN_SERVER_POOL_SIZE = 100;
    private static final int MAX_SERVER_POOL_SIZE = 500;
    private static final int MAX_TASK_QUEUE_SIZE = 20000;
    private static final int KEEP_ALIVE_TIME = 500;
    private static final int SERVER_DEFAULT_PORT = 8091;
    private static final ThreadPoolExecutor WORKING_THREADS = new ThreadPoolExecutor(MIN_SERVER_POOL_SIZE,
        MAX_SERVER_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(MAX_TASK_QUEUE_SIZE),
        new NamedThreadFactory("ServerHandlerThread", MAX_SERVER_POOL_SIZE), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException the io exception
     */
    public static void main(String[] args) throws IOException {
        RpcServer rpcServer = new RpcServer(WORKING_THREADS);

        int port = SERVER_DEFAULT_PORT;
        //server port
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: sh services-server.sh $LISTEN_PORT $PATH_FOR_PERSISTENT_DATA");
                System.exit(0);
            }
        }
        rpcServer.setListenPort(port);

        //log store mode : file、db
        String storeMode = null;
        if (args.length > 1) {
            storeMode = args[1];
        }

        UUIDGenerator.init(1);
        SessionHolder.init(storeMode);

        DefaultCoordinator coordinator = new DefaultCoordinator(rpcServer);
        coordinator.init();
        rpcServer.setHandler(coordinator);
        // register ShutdownHook
        ShutdownHook.getInstance().addDisposable(coordinator);

        if (args.length > 2) {
            XID.setIpAddress(args[2]);
        } else {
            XID.setIpAddress(NetUtil.getLocalIp());
        }
        XID.setPort(rpcServer.getListenPort());

        rpcServer.init();

        System.exit(0);
    }
}
