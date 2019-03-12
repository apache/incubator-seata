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
package com.alibaba.fescar.core.rpc.netty;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;
import com.alibaba.fescar.server.UUIDGenerator;
import com.alibaba.fescar.server.coordinator.DefaultCoordinator;

import io.netty.channel.Channel;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author jimin.jm@alibaba-inc.com
 * @date 2019/01/25
 */
@Ignore
public class TmRpcClientTest {

    private static final ThreadPoolExecutor
        workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
                                                new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testDoConnect() throws Exception {

        //start fescar server first
        workingThreads.submit(new Runnable() {
            @Override
            public void run() {
                RpcServer rpcServer = new RpcServer(workingThreads);
                rpcServer.setHandler(new DefaultCoordinator(rpcServer));
                UUIDGenerator.init(1);
                rpcServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        tmRpcClient.init();

        Method doConnectMethod = TmRpcClient.class.getDeclaredMethod("doConnect", String.class);
        doConnectMethod.setAccessible(true);
        String serverAddress = "0.0.0.0:8091";
        Channel channel = (Channel) doConnectMethod.invoke(tmRpcClient, serverAddress);
        System.out.print("channel = ");
        Assert.assertNotNull(channel);
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {

        //start fescar server first
        workingThreads.submit(new Runnable() {
            @Override
            public void run() {
                RpcServer rpcServer = new RpcServer(workingThreads);
                rpcServer.setHandler(new DefaultCoordinator(rpcServer));
                UUIDGenerator.init(1);
                rpcServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        tmRpcClient.init();

        Method doConnectMethod = TmRpcClient.class.getDeclaredMethod("reconnect");
        doConnectMethod.setAccessible(true);
        doConnectMethod.invoke(tmRpcClient);
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {

        //start fescar server first
        workingThreads.submit(new Runnable() {
            @Override
            public void run() {
                RpcServer rpcServer = new RpcServer(workingThreads);
                rpcServer.setHandler(new DefaultCoordinator(rpcServer));
                UUIDGenerator.init(1);
                rpcServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        tmRpcClient.init();

        Method doConnectMethod = TmRpcClient.class.getDeclaredMethod("doConnect", String.class);
        doConnectMethod.setAccessible(true);
        String serverAddress = "0.0.0.0:8091";
        Channel channel = (Channel) doConnectMethod.invoke(tmRpcClient, serverAddress);
        System.out.print("channel = " + channel);
        Assert.assertNotNull(channel);

        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setTransactionId(123456L);
        request.setLockKey("lock key testSendMsgWithResponse");
        request.setResourceId("resoutceId1");
        BranchRegisterResponse branchRegisterResponse = (BranchRegisterResponse)tmRpcClient.sendMsgWithResponse(request);
        Assert.assertNotNull(branchRegisterResponse);
        //we have not init SessionManager
        Assert.assertEquals(ResultCode.Failed, branchRegisterResponse.getResultCode());
        Assert.assertEquals("RuntimeException[SessionManager is NOT init!]",
                            branchRegisterResponse.getMsg());
    }
}
