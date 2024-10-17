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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.rm.tcc.TCCResourceManager;
import org.apache.seata.saga.engine.db.AbstractServerTest;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class RmNettyClientTest extends AbstractServerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmNettyClientTest.class);

    @BeforeAll
    public static void init(){
        ConfigurationTestHelper.putConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "8091");
    }
    @AfterAll
    public static void after() {
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }

    public static ThreadPoolExecutor initMessageExecutor() {
        return new ThreadPoolExecutor(5, 5, 500, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(20000), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Test
    public void testMergeMsg() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        new Thread(() -> {
            SessionHolder.init(null);
            nettyRemotingServer.setHandler(DefaultCoordinator.getInstance(nettyRemotingServer));
            // set registry
            XID.setIpAddress(NetUtil.getLocalIp());
            XID.setPort(8091);
            // init snowflake for transactionId, branchId
            UUIDGenerator.init(1L);
            nettyRemotingServer.init();
        }).start();
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group";
        RmNettyRemotingClient rmNettyRemotingClient = RmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        rmNettyRemotingClient.setResourceManager(new TCCResourceManager());
        rmNettyRemotingClient.init();
        rmNettyRemotingClient.getClientChannelManager().initReconnect(transactionServiceGroup, true);
        String serverAddress = "0.0.0.0:8091";
        Channel channel = RmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);

        CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            CompletableFuture.runAsync(()->{
                BranchRegisterRequest request = new BranchRegisterRequest();
                request.setXid("127.0.0.1:8091:1249853");
                request.setLockKey("lock key testSendMsgWithResponse");
                request.setResourceId("resoutceId1");
                BranchRegisterResponse branchRegisterResponse = null;
                try {
                    branchRegisterResponse = (BranchRegisterResponse) rmNettyRemotingClient.sendSyncRequest(request);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
                Assertions.assertNotNull(branchRegisterResponse);
                Assertions.assertEquals(ResultCode.Failed, branchRegisterResponse.getResultCode());
                Assertions.assertEquals("TransactionException[Could not found global transaction xid = 127.0.0.1:8091:1249853, may be has finished.]",
                        branchRegisterResponse.getMsg());
                latch.countDown();
            });
        }
        latch.await(10,TimeUnit.SECONDS);
        nettyRemotingServer.destroy();
        rmNettyRemotingClient.destroy();
    }

}
