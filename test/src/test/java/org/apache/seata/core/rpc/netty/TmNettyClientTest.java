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
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.rpc.netty.mockserver.ProtocolTestConstants;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.saga.engine.db.AbstractServerTest;
import org.apache.seata.server.UUIDGenerator;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class TmNettyClientTest extends AbstractServerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmNettyClientTest.class);
    @BeforeAll
    public static void init() {
        MockServer.start(ProtocolTestConstants.MOCK_SERVER_PORT);
    }

    @AfterAll
    public static void after() {
        MockServer.close();
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testDoConnect() throws Exception {

        //then test client
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();
        String serverAddress = "127.0.0.1:8099";
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);
        tmNettyRemotingClient.destroy();
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();

        TmNettyRemotingClient.getInstance().getClientChannelManager().reconnect(transactionServiceGroup);
        tmNettyRemotingClient.destroy();
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        tmNettyRemotingClient.init();

        String serverAddress = "127.0.0.1:8099";
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);

        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid("127.0.0.1:8091:1249853");
        request.setLockKey("lock key testSendMsgWithResponse");
        request.setResourceId("resoutceId1");
        BranchRegisterResponse branchRegisterResponse = (BranchRegisterResponse) tmNettyRemotingClient.sendSyncRequest(request);
        Assertions.assertNotNull(branchRegisterResponse);
        Assertions.assertEquals(ResultCode.Failed, branchRegisterResponse.getResultCode());
        Assertions.assertEquals("TransactionException[Could not found global transaction xid = 127.0.0.1:8091:1249853, may be has finished.]",
                branchRegisterResponse.getMsg());
        tmNettyRemotingClient.destroy();
    }

}
