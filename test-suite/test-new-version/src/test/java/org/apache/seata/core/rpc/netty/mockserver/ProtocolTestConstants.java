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
package org.apache.seata.core.rpc.netty.mockserver;

import org.apache.seata.config.ConfigurationCache;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Mock Constants
 **/
public class ProtocolTestConstants {
    public static final String APPLICATION_ID = "mock_tx_app_id";
    public static final String SERVICE_GROUP = "mock_tx_group";
    public static final int MOCK_SERVER_PORT = findAvailablePort();
    public static final String MOCK_SERVER_ADDRESS = "0.0.0.0:" + MOCK_SERVER_PORT;

    static {
        System.setProperty("service.mock.grouplist", "127.0.0.1:" + MOCK_SERVER_PORT);
        ConfigurationCache.clear();
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to allocate an available port", e);
        }
    }
}
