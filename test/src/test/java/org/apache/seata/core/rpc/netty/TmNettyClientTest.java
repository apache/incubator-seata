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
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.rpc.netty.mockserver.ProtocolTestConstants;
import org.apache.seata.discovery.registry.RegistryFactory;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.saga.engine.db.AbstractServerTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import static org.apache.seata.discovery.registry.RegistryService.CONFIG_SPLIT_CHAR;
import static org.apache.seata.discovery.registry.RegistryService.PREFIX_SERVICE_MAPPING;
import static org.apache.seata.discovery.registry.RegistryService.PREFIX_SERVICE_ROOT;

/**
 */
public class TmNettyClientTest extends AbstractServerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmNettyClientTest.class);
    static TmNettyRemotingClient tmNettyRemotingClient;
    @BeforeAll
    public static void init() {
        TmNettyRemotingClient.getInstance().destroy();
        ConfigurationTestHelper.putConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.valueOf(ProtocolTestConstants.MOCK_SERVER_PORT));
        MockServer.start(ProtocolTestConstants.MOCK_SERVER_PORT);
        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group-test";
        System.setProperty(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + transactionServiceGroup, "test");
        System.setProperty(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + "test.grouplist" ,"127.0.0.1:8099");
        tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        tmNettyRemotingClient.init();
    }

    @AfterAll
    public static void after() {
       // MockServer.close();
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        tmNettyRemotingClient.destroy();
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
        String transactionServiceGroup = "groupA";
        tmNettyRemotingClient.destroy();
        System.setProperty(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + transactionServiceGroup, "test");
        System.setProperty(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + "test.grouplist" ,"127.0.0.1:8099");
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();
        String serverAddress = "127.0.0.1:8099";

        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);
        TmNettyRemotingClient.getInstance().getClientChannelManager().reconnect(transactionServiceGroup);
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {
        String transactionServiceGroup = "default_tx_group";
        TmNettyRemotingClient.getInstance().getClientChannelManager().reconnect(transactionServiceGroup);
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {
        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid("127.0.0.1:8099:1249853");
        request.setLockKey("lock key testSendMsgWithResponse");
        request.setResourceId("resoutceId1");
        String serverAddress = "127.0.0.1:8099";
        LOGGER.info("getTransactionServiceGroup: {}",TmNettyRemotingClient.getInstance().getTransactionServiceGroup());
        List<InetSocketAddress> inetSocketAddressList = RegistryFactory.getInstance().aliveLookup(TmNettyRemotingClient.getInstance().getTransactionServiceGroup());
        Assertions.assertTrue(inetSocketAddressList.size() > 0);
        try(Socket socket = new Socket()){
            socket.connect(new InetSocketAddress("127.0.0.1",8099),1000);
        }
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);
        BranchRegisterResponse branchRegisterResponse = (BranchRegisterResponse) tmNettyRemotingClient.sendSyncRequest(request);
        Assertions.assertNotNull(branchRegisterResponse);
        Assertions.assertEquals(ResultCode.Failed, branchRegisterResponse.getResultCode());
        Assertions.assertEquals("TransactionException[Could not found global transaction xid = 127.0.0.1:8091:1249853, may be has finished.]",
                branchRegisterResponse.getMsg());
    }

}
