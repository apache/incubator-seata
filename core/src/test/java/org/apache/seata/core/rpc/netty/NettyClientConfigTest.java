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

public class NettyClientConfigTest {

    private NettyClientConfig config;

    @BeforeEach
    public void setUp() {
        config = new NettyClientConfig();
    }

    @Test
    public void testDefaultValues() {
        Assertions.assertEquals(10000, config.getConnectTimeoutMillis());
        Assertions.assertEquals(153600, config.getClientSocketSndBufSize());
        Assertions.assertEquals(153600, config.getClientSocketRcvBufSize());
        Assertions.assertNotNull(config.getClientChannelClazz());
    }

    @Test
    public void testSetAndGetConnectTimeoutMillis() {
        config.setConnectTimeoutMillis(5000);
        Assertions.assertEquals(5000, config.getConnectTimeoutMillis());
    }

    @Test
    public void testSetAndGetClientSocketSndBufSize() {
        config.setClientSocketSndBufSize(200000);
        Assertions.assertEquals(200000, config.getClientSocketSndBufSize());
    }

    @Test
    public void testSetAndGetClientSocketRcvBufSize() {
        config.setClientSocketRcvBufSize(300000);
        Assertions.assertEquals(300000, config.getClientSocketRcvBufSize());
    }

    @Test
    public void testSetAndGetClientWorkerThreads() {
        config.setClientWorkerThreads(8);
        Assertions.assertEquals(8, config.getClientWorkerThreads());
    }

    @Test
    public void testGetPerHostMaxConn() {
        Assertions.assertEquals(2, config.getPerHostMaxConn());
    }

    @Test
    public void testSetPerHostMaxConnValid() {
        config.setPerHostMaxConn(5);
        Assertions.assertEquals(5, config.getPerHostMaxConn());
    }

    @Test
    public void testSetPerHostMaxConnBelowMinimum() {
        config.setPerHostMaxConn(1);
        Assertions.assertEquals(2, config.getPerHostMaxConn());
    }

    @Test
    public void testSetPerHostMaxConnZero() {
        config.setPerHostMaxConn(0);
        Assertions.assertEquals(2, config.getPerHostMaxConn());
    }

    @Test
    public void testSetAndGetPendingConnSize() {
        config.setPendingConnSize(1000);
        Assertions.assertEquals(1000, config.getPendingConnSize());
    }

    @Test
    public void testStaticGetters() {
        Assertions.assertTrue(NettyClientConfig.getRpcRmRequestTimeout() > 0);
        Assertions.assertTrue(NettyClientConfig.getRpcTmRequestTimeout() > 0);
        Assertions.assertEquals(2000, NettyClientConfig.getMaxNotWriteableRetry());
        Assertions.assertEquals(2, NettyClientConfig.getPerHostMinConn());
        Assertions.assertEquals(300, NettyClientConfig.getMaxCheckAliveRetry());
        Assertions.assertEquals(10, NettyClientConfig.getCheckAliveInterval());
        Assertions.assertEquals("/", NettyClientConfig.getSocketAddressStartChar());
    }

    @Test
    public void testSetAndGetVgroup() {
        NettyClientConfig.setVgroup("test-vgroup");
        Assertions.assertEquals("test-vgroup", NettyClientConfig.getVgroup());
    }

    @Test
    public void testSetAndGetClientAppName() {
        NettyClientConfig.setClientAppName("test-app");
        Assertions.assertEquals("test-app", NettyClientConfig.getClientAppName());
    }

    @Test
    public void testSetAndGetClientType() {
        NettyClientConfig.setClientType(1);
        Assertions.assertEquals(1, NettyClientConfig.getClientType());
    }

    @Test
    public void testGetMaxInactiveChannelCheck() {
        Assertions.assertTrue(NettyClientConfig.getMaxInactiveChannelCheck() > 0);
    }

    @Test
    public void testGetClientSelectorThreadSize() {
        int threadSize = config.getClientSelectorThreadSize();
        Assertions.assertTrue(threadSize > 0);
    }

    @Test
    public void testGetMaxAcquireConnMills() {
        Assertions.assertEquals(10000L, config.getMaxAcquireConnMills());
    }

    @Test
    public void testGetThreadPrefixes() {
        Assertions.assertNotNull(config.getClientSelectorThreadPrefix());
        Assertions.assertNotNull(config.getClientWorkerThreadPrefix());
        Assertions.assertEquals("rpcDispatch", config.getRpcDispatchThreadPrefix());
        Assertions.assertTrue(config.getTmDispatchThreadPrefix().contains("TMROLE"));
        Assertions.assertTrue(config.getRmDispatchThreadPrefix().contains("RMROLE"));
    }

    @Test
    public void testPoolConfiguration() {
        Assertions.assertEquals(1, config.getMaxPoolActive());
        Assertions.assertEquals(0, config.getMinPoolIdle());
        Assertions.assertTrue(config.isPoolTestBorrow());
        Assertions.assertTrue(config.isPoolTestReturn());
        Assertions.assertTrue(config.isPoolLifo());
    }

    @Test
    public void testGetProtocol() {
        Assertions.assertNotNull(config.getProtocol());
    }

    @Test
    public void testIdleTimeouts() {
        Assertions.assertTrue(config.getChannelMaxWriteIdleSeconds() >= 0);
        Assertions.assertTrue(config.getChannelMaxReadIdleSeconds() >= 0);
        Assertions.assertTrue(config.getChannelMaxAllIdleSeconds() >= 0);
    }
}
