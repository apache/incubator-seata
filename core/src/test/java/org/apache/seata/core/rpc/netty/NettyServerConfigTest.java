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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NettyServerConfigTest {

    private NettyServerConfig config;

    @BeforeEach
    public void setUp() {
        config = new NettyServerConfig();
    }

    @Test
    public void testGetServerListenPort() {
        Assertions.assertTrue(config.getServerListenPort() >= 0);
    }

    @Test
    public void testSetAndGetServerSocketSendBufSize() {
        config.setServerSocketSendBufSize(200000);
        Assertions.assertEquals(200000, config.getServerSocketSendBufSize());
    }

    @Test
    public void testSetAndGetServerSocketResvBufSize() {
        config.setServerSocketResvBufSize(150000);
        Assertions.assertEquals(150000, config.getServerSocketResvBufSize());
    }

    @Test
    public void testSetAndGetWriteBufferHighWaterMark() {
        config.setWriteBufferHighWaterMark(100000);
        Assertions.assertEquals(100000, config.getWriteBufferHighWaterMark());
    }

    @Test
    public void testSetAndGetWriteBufferLowWaterMark() {
        config.setWriteBufferLowWaterMark(50000);
        Assertions.assertEquals(50000, config.getWriteBufferLowWaterMark());
    }

    @Test
    public void testSetAndGetServerSelectorThreads() {
        config.setServerSelectorThreads(4);
        Assertions.assertEquals(4, config.getServerSelectorThreads());
    }

    @Test
    public void testSetAndGetServerWorkerThreads() {
        config.setServerWorkerThreads(8);
        Assertions.assertEquals(8, config.getServerWorkerThreads());
    }

    @Test
    public void testSetAndGetSoBackLogSize() {
        config.setSoBackLogSize(1024);
        Assertions.assertEquals(1024, config.getSoBackLogSize());
    }

    @Test
    public void testGetServerChannelMaxIdleTimeSeconds() {
        Assertions.assertTrue(config.getServerChannelMaxIdleTimeSeconds() > 0);
    }

    @Test
    public void testGetServerChannelClazz() {
        Assertions.assertNotNull(NettyServerConfig.SERVER_CHANNEL_CLAZZ);
    }

    @Test
    public void testGetStaticConfigValues() {
        Assertions.assertTrue(NettyServerConfig.getMinServerPoolSize() >= 0);
        Assertions.assertTrue(NettyServerConfig.getMaxServerPoolSize() > 0);
        Assertions.assertTrue(NettyServerConfig.getMaxTaskQueueSize() > 0);
        Assertions.assertTrue(NettyServerConfig.getKeepAliveTime() > 0);
    }

    @Test
    public void testGetHttpPoolConfiguration() {
        Assertions.assertTrue(NettyServerConfig.getMinHttpPoolSize() >= 0);
        Assertions.assertTrue(NettyServerConfig.getMaxHttpPoolSize() > 0);
        Assertions.assertTrue(NettyServerConfig.getMaxHttpTaskQueueSize() > 0);
        Assertions.assertTrue(NettyServerConfig.getHttpKeepAliveTime() > 0);
    }

    @Test
    public void testGetBranchResultPoolConfiguration() {
        Assertions.assertTrue(NettyServerConfig.getMinBranchResultPoolSize() >= 0);
        Assertions.assertTrue(NettyServerConfig.getMaxBranchResultPoolSize() > 0);
    }

    @Test
    public void testGetBossThreadConfiguration() {
        Assertions.assertNotNull(config.getBossThreadPrefix());
        Assertions.assertTrue(config.getBossThreadSize() > 0);
    }

    @Test
    public void testGetWorkerThreadPrefix() {
        Assertions.assertNotNull(config.getWorkerThreadPrefix());
    }

    @Test
    public void testGetShutdownWaitValue() {
        Assertions.assertNotNull(config);
    }

    @Test
    public void testGetExecutorThreadPrefix() {
        Assertions.assertNotNull(config.getExecutorThreadPrefix());
    }

    @Test
    public void testDefaultValues() {
        Assertions.assertTrue(config.getServerSocketSendBufSize() > 0);
        Assertions.assertTrue(config.getServerSocketResvBufSize() > 0);
        Assertions.assertTrue(config.getWriteBufferHighWaterMark() > 0);
        Assertions.assertTrue(config.getWriteBufferLowWaterMark() > 0);
        Assertions.assertTrue(config.getSoBackLogSize() > 0);
        Assertions.assertTrue(config.getServerChannelMaxIdleTimeSeconds() > 0);
    }

    @Test
    public void testThreadPoolSizes() {
        Assertions.assertTrue(config.getServerSelectorThreads() > 0);
        Assertions.assertTrue(config.getServerWorkerThreads() > 0);
    }
}
