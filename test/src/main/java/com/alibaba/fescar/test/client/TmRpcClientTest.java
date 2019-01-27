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

package com.alibaba.fescar.test.client;

import com.alibaba.fescar.core.rpc.netty.RpcServer;
import com.alibaba.fescar.core.rpc.netty.TmRpcClient;
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
    public void doConnect() throws Exception {

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
}
