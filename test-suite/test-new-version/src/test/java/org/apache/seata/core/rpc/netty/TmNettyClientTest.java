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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

/**
 * TmNettyClient Test - Refactored to use BaseNettyClientTest
 */
public class TmNettyClientTest extends BaseNettyClientTest {

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testDoConnect() throws Exception {
        int dynamicPort = getDynamicPort();
        ServerInstance serverInstance = startServer(dynamicPort);

        try {
            configureClient(dynamicPort);

            // then test client
            String applicationId = "app 1";
            String transactionServiceGroup = "group A";
            TmNettyRemotingClient tmNettyRemotingClient =
                    TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

            tmNettyRemotingClient.init();
            Channel channel = TmNettyRemotingClient.getInstance()
                    .getClientChannelManager()
                    .acquireChannel(serverInstance.getAddress());
            Assertions.assertNotNull(channel);

            tmNettyRemotingClient.destroy();
        } finally {
            serverInstance.destroy();
        }
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {
        int dynamicPort = getDynamicPort();
        ServerInstance serverInstance = startServerSimple(dynamicPort);

        try {
            configureClient(dynamicPort);

            String applicationId = "app 1";
            String transactionServiceGroup = "default_tx_group";
            TmNettyRemotingClient tmNettyRemotingClient =
                    TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

            tmNettyRemotingClient.init();
            TmNettyRemotingClient.getInstance().getClientChannelManager().reconnect(transactionServiceGroup);

            tmNettyRemotingClient.destroy();
        } finally {
            serverInstance.destroy();
        }
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {
        int dynamicPort = getDynamicPort();
        ServerInstance serverInstance = startServerSimple(dynamicPort);

        try {
            configureClient(dynamicPort);

            String applicationId = "app 1";
            String transactionServiceGroup = "default_tx_group";
            TmNettyRemotingClient tmNettyRemotingClient =
                    TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
            tmNettyRemotingClient.init();

            Channel channel = TmNettyRemotingClient.getInstance()
                    .getClientChannelManager()
                    .acquireChannel(serverInstance.getAddress());
            Assertions.assertNotNull(channel);

            GlobalCommitRequest request = new GlobalCommitRequest();
            request.setXid("127.0.0.1:" + dynamicPort + ":1249853");
            GlobalCommitResponse globalCommitResponse = null;
            try {
                globalCommitResponse = (GlobalCommitResponse) tmNettyRemotingClient.sendSyncRequest(request);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
            Assertions.assertNotNull(globalCommitResponse);
            Assertions.assertEquals(GlobalStatus.Finished, globalCommitResponse.getGlobalStatus());

            tmNettyRemotingClient.destroy();
        } finally {
            serverInstance.destroy();
        }
    }
}
