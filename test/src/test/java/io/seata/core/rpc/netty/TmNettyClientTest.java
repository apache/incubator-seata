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
package io.seata.core.rpc.netty;

import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.Channel;
import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.saga.engine.db.AbstractServerTest;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author slievrly
 */
public class TmNettyClientTest extends AbstractServerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmNettyClientTest.class);


    public static ThreadPoolExecutor initMessageExecutor() {
        return new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
                new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testDoConnect() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        //start services server first
        AtomicBoolean serverStatus = new AtomicBoolean();
        Thread thread = new Thread(() -> {
            try {
                nettyRemotingServer.setHandler(DefaultCoordinator.getInstance(nettyRemotingServer));
                // set registry
                XID.setIpAddress(NetUtil.getLocalIp());
                XID.setPort(8091);
                // init snowflake for transactionId, branchId
                UUIDGenerator.init(1L);
                System.out.println("pid info: " + ManagementFactory.getRuntimeMXBean().getName());
                nettyRemotingServer.init();
                serverStatus.set(true);
            } catch (Throwable t) {
                serverStatus.set(false);
                LOGGER.error("The seata-server failed to start", t);
            }
        });
        thread.start();

        //Wait for the seata-server to start.
        long start = System.nanoTime();
        long maxWaitNanoTime = 10 * 1000 * 1000 * 1000L; // 10s
        while (System.nanoTime() - start < maxWaitNanoTime) {
            Thread.sleep(100);
            if (serverStatus.get()) {
                break;
            }
        }
        if (!serverStatus.get()) {
            throw new RuntimeException("Waiting for a while, but the seata-server did not start successfully.");
        }

        //then test client
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();
        String serverAddress = "0.0.0.0:8091";
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);
        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        //start services server first
        Thread thread = new Thread(() -> {
            nettyRemotingServer.setHandler(DefaultCoordinator.getInstance(nettyRemotingServer));
            // set registry
            XID.setIpAddress(NetUtil.getLocalIp());
            XID.setPort(8091);
            // init snowflake for transactionId, branchId
            UUIDGenerator.init(1L);
            nettyRemotingServer.init();
        });
        thread.start();

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();

        TmNettyRemotingClient.getInstance().getClientChannelManager().reconnect(transactionServiceGroup);
        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {
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
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        tmNettyRemotingClient.init();

        String serverAddress = "0.0.0.0:8091";
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
        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
    }
}
