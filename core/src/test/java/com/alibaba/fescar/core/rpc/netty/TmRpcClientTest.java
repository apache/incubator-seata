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

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Author: jxiajun.0706@163.com
 * @Project: fescar-all
 * @DateTime: 2019/01/25 08:32
 * @FileName: TmRpcClientTest
 * @Description:
 */
public class TmRpcClientTest {

    @Test
    public void testGetInstance() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        NettyClientConfig defaultNettyClientConfig = new NettyClientConfig();
        GenericKeyedObjectPool.Config config = tmRpcClient.getNettyPoolConfig();
        Assert.assertEquals(defaultNettyClientConfig.getMaxPoolActive(), config.maxActive);
        Assert.assertEquals(defaultNettyClientConfig.getMinPoolIdle(), config.minIdle);
        Assert.assertEquals(defaultNettyClientConfig.getMaxAcquireConnMills(), config.maxWait);
        Assert.assertEquals(defaultNettyClientConfig.isPoolTestBorrow(), config.testOnBorrow);
        Assert.assertEquals(defaultNettyClientConfig.isPoolTestReturn(), config.testOnReturn);
        Assert.assertEquals(defaultNettyClientConfig.isPoolFifo(), config.lifo);
    }

    @Test
    public void testInit() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        tmRpcClient.init();

        //Assert.assertEquals(tmRpcClient.);
    }

    @Test
    public void doConnect() throws Exception {

    }

    @Test
    public void getApplicationId() throws Exception {

    }

    @Test
    public void setApplicationId() throws Exception {

    }

}
