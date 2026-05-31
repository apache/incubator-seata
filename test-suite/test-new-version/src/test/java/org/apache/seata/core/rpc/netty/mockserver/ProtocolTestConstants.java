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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.mockserver.MockServer;

/**
 * Mock Constants
 **/
public class ProtocolTestConstants {
    public static final String APPLICATION_ID = "mock_tx_app_id";
    public static final String SERVICE_GROUP = "mock_tx_group";

    /**
     * Start an independent MockServer instance with a random port, configure
     * system properties and clients to connect to it.
     *
     * @return the started MockServer instance
     */
    public static MockServer initMockServer() {
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();

        MockServer server = new MockServer();
        server.start(0);
        int port = server.getPort();

        System.setProperty("service.mock.grouplist", "127.0.0.1:" + port);
        System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.valueOf(port));
        ConfigurationFactory.reload();
        ConfigurationCache.clear();
        return server;
    }

    /**
     * Cleanup after tests: close the server, clear properties.
     *
     * @param server the MockServer instance to close
     */
    public static void closeMockServer(MockServer server) {
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();
        if (server != null) {
            server.close();
        }
        System.clearProperty("service.mock.grouplist");
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        ConfigurationCache.clear();
    }
}
