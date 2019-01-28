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

import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;
import com.alibaba.fescar.server.UUIDGenerator;
import com.alibaba.fescar.server.coordinator.DefaultCoordinator;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Author: xiajun.0706@163.com
 * @Project: fescar-all
 * @DateTime: 2019/01/25 08:32
 * @FileName: TmRpcClientTest
 * @Description:
 */
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
                System.out.println("doConnect - start server ing");
                RpcServer rpcServer = new RpcServer(workingThreads);
                rpcServer.setHandler(new DefaultCoordinator(rpcServer));
                UUIDGenerator.init(1);
                rpcServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);
        System.out.println("doConnect - start client ing");

        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        tmRpcClient.init();

        Method doConnectMethod = TmRpcClient.class.getDeclaredMethod("doConnect", String.class);
        doConnectMethod.setAccessible(true);
        String serverAddress = "0.0.0.0:8091";
        Channel channel = (Channel) doConnectMethod.invoke(tmRpcClient, serverAddress);
        System.out.print("channel = ");
        System.out.println(channel);
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
                System.out.println("testReconnect - start server ing");
                RpcServer rpcServer = new RpcServer(workingThreads);
                rpcServer.setHandler(new DefaultCoordinator(rpcServer));
                UUIDGenerator.init(1);
                rpcServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);
        System.out.println("testReconnect - start client ing");

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
                System.out.println("testSendMsgWithResponse - start server ing");
                RpcServer rpcServer = new RpcServer(workingThreads);
                rpcServer.setHandler(new DefaultCoordinator(rpcServer));
                UUIDGenerator.init(1);
                rpcServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);
        System.out.println("testSendMsgWithResponse - start client ing");

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        System.out.println("testSendMsgWithResponse - init tmRpcClient ing");
        tmRpcClient.init();

        Method doConnectMethod = TmRpcClient.class.getDeclaredMethod("doConnect", String.class);
        doConnectMethod.setAccessible(true);
        String serverAddress = "0.0.0.0:8091";
        System.out.println("testSendMsgWithResponse - do connect ing");
        Channel channel = (Channel) doConnectMethod.invoke(tmRpcClient, serverAddress);
        System.out.print("channel = " + channel);
        System.out.println(channel);
        Assert.assertNotNull(channel);

        System.out.println("testSendMsgWithResponse - sendMsgWithResponse ing");
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
        System.out.println(branchRegisterResponse);

    }
}
