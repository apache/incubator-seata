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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Assert;
import org.junit.Test;

/**
 * The type Tm rpc client test.
 *
 * @author jimin.jm @alibaba-inc.com xiajun.0706@163.com
 * @date 2019 /01/25
 */
public class TmRpcClientTest {

    private static final ThreadPoolExecutor
        workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
        new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * Test get instance.
     *
     * @throws Exception the exception
     */
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
        Assert.assertEquals(defaultNettyClientConfig.isPoolLifo(), config.lifo);
    }

    /**
     * Do connect.
     *
     * @throws Exception the exception
     */
    @Test
    public void testInit() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);

        tmRpcClient.init();

        //check if attr of tmRpcClient object has been set success
        Field bootstrapField = getDeclaredField(tmRpcClient, "bootstrap");
        bootstrapField.setAccessible(true);
        Bootstrap bootstrap = (Bootstrap)bootstrapField.get(tmRpcClient);

        Assert.assertNotNull(bootstrap);
        Field optionsField = getDeclaredField(bootstrap, "options");
        optionsField.setAccessible(true);
        Map<ChannelOption<?>, Object> options = (Map<ChannelOption<?>, Object>)optionsField.get(bootstrap);
        Assert.assertTrue(Boolean.TRUE.equals(options.get(ChannelOption.TCP_NODELAY)));
        Assert.assertTrue(Boolean.TRUE.equals(options.get(ChannelOption.SO_KEEPALIVE)));
        Assert.assertEquals(10000, options.get(ChannelOption.CONNECT_TIMEOUT_MILLIS));
        Assert.assertTrue(Boolean.TRUE.equals(options.get(ChannelOption.SO_KEEPALIVE)));
        Assert.assertEquals(153600, options.get(ChannelOption.SO_RCVBUF));

        Field channelFactoryField = getDeclaredField(bootstrap, "channelFactory");
        channelFactoryField.setAccessible(true);
        ChannelFactory<? extends Channel>
            channelFactory = (ChannelFactory<? extends Channel>)channelFactoryField.get(bootstrap);
        Assert.assertNotNull(channelFactory);
        Assert.assertTrue(channelFactory.newChannel() instanceof NioSocketChannel);

    }

    /**
     * Gets application id.
     *
     * @throws Exception the exception
     */
    @Test
    public void getApplicationId() throws Exception {

    }

    /**
     * Sets application id.
     *
     * @throws Exception the exception
     */
    @Test
    public void setApplicationId() throws Exception {

    }

    /**
     * get private field in parent class
     *
     * @param object    the object
     * @param fieldName the field name
     * @return declared field
     */
    public static Field getDeclaredField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (Exception e) {

            }
        }

        return null;
    }
}
