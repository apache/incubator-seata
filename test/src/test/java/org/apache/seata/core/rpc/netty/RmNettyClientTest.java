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
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.rm.tcc.TCCResourceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RmNettyClient Test - Refactored to use BaseNettyClientTest
 */
public class RmNettyClientTest extends BaseNettyClientTest {

    @Test
    public void testMergeMsg() throws Exception {
        int dynamicPort = getDynamicPort();
        ServerInstance serverInstance = startServerSimple(dynamicPort);

        try {
            configureClient(dynamicPort);

            String applicationId = "app 1";
            String transactionServiceGroup = "default_tx_group";
            RmNettyRemotingClient rmNettyRemotingClient =
                    RmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
            rmNettyRemotingClient.setResourceManager(new TCCResourceManager());
            rmNettyRemotingClient.init();
            rmNettyRemotingClient.getClientChannelManager().initReconnect(transactionServiceGroup, true);

            Channel channel = RmNettyRemotingClient.getInstance()
                    .getClientChannelManager()
                    .acquireChannel(serverInstance.getAddress());
            Assertions.assertNotNull(channel);

            CountDownLatch latch = new CountDownLatch(3);
            for (int i = 0; i < 3; i++) {
                CompletableFuture.runAsync(() -> {
                    BranchRegisterRequest request = new BranchRegisterRequest();
                    request.setXid("127.0.0.1:" + dynamicPort + ":1249853");
                    request.setLockKey("lock key testSendMsgWithResponse");
                    request.setResourceId("resoutceId1");
                    BranchRegisterResponse branchRegisterResponse = null;
                    try {
                        branchRegisterResponse =
                                (BranchRegisterResponse) rmNettyRemotingClient.sendSyncRequest(request);
                    } catch (TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                    Assertions.assertNotNull(branchRegisterResponse);
                    Assertions.assertEquals(ResultCode.Failed, branchRegisterResponse.getResultCode());
                    Assertions.assertEquals(
                            "TransactionException[Could not found global transaction xid = 127.0.0.1:" + dynamicPort
                                    + ":1249853, may be has finished.]",
                            branchRegisterResponse.getMsg());
                    latch.countDown();
                });
            }
            latch.await(10, TimeUnit.SECONDS);

            rmNettyRemotingClient.destroy();
        } finally {
            serverInstance.destroy();
        }
    }
}
