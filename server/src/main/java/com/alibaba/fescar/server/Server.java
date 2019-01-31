/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.server;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.core.rpc.netty.RpcServer;
import com.alibaba.fescar.server.coordinator.DefaultCoordinator;
import com.alibaba.fescar.server.session.SessionHolder;

/**
 * The type Server.
 */
public class Server {
    // Default Values
    private static final int
            default_port = 8091;
    private static final String
            default_data_directory = null;

    // Thread Pool Executor
    private static final ThreadPoolExecutor WORKING_THREADS = new ThreadPoolExecutor(
            100, // Core Pool Size
            500, // Max Pool Size
            500, // Time to keep alive
            TimeUnit.SECONDS,
            new LinkedBlockingQueue(20000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException the io exception
     */
    public static void main(String[] args) throws IOException {
        // Init Values
        int port = default_port;
        String data_directory = default_data_directory;

        // Get port from argument 0
        if (args.length > 0)
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                exitWithMessage("Usage: sh fescar-server.sh $LISTEN_PORT $PATH_FOR_PERSISTENT_DATA");
            }
        // If port is invalid, exit
        if (port < 0 || port > 65535)
            exitWithMessage("Invalid port. Please use a port number between 0-65535.");

        // Get data dir from argument 1
        if (args.length > 1)
            data_directory = args[1];

        // Initialise RPC Server
        RpcServer rpcServer = new RpcServer(WORKING_THREADS);
        rpcServer.setListenPort(port);

        // Initialise Session Holder
        SessionHolder.init(data_directory);

        // Initialise Coordinator
        DefaultCoordinator coordinator = new DefaultCoordinator(rpcServer);
        rpcServer.setHandler(coordinator);
        coordinator.init();

        // Initialise Universally Unique Identifier Generator
        UUIDGenerator.init(1);

        // Initialise XID
        XID.setIpAddress(NetUtil.getLocalIp());
        XID.setPort(rpcServer.getListenPort());

        // Start RPC Server
        rpcServer.init();
        // RPC Server Closed

        // Exit Code
        exitWithMessage("RPC Server Closed");
    }

    private static void exitWithMessage(String message) {
        System.err.println(message);
        System.exit(0);
    }
}
