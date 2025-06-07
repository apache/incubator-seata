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

import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NettyClientBootstrapTest {

    @Mock
    private NettyClientConfig nettyClientConfig;
    @Test
    void testSharedEventLoopGroupEnabled() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(true);
        NettyClientBootstrap tmNettyClientBootstrap = new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
        EventLoopGroup tmEventLoopGroupWorker = getEventLoopGroupWorker(tmNettyClientBootstrap);

        NettyClientBootstrap rmNettyClientBootstrap = new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.RMROLE);
        EventLoopGroup rmEventLoopGroupWorker = getEventLoopGroupWorker(rmNettyClientBootstrap);

        Assertions.assertEquals(tmEventLoopGroupWorker, rmEventLoopGroupWorker);
    }

    @Test
    void testSharedEventLoopGroupDisabled() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(false);
        NettyClientBootstrap tmNettyClientBootstrap = new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.TMROLE);
        EventLoopGroup tmEventLoopGroupWorker = getEventLoopGroupWorker(tmNettyClientBootstrap);

        NettyClientBootstrap rmNettyClientBootstrap = new NettyClientBootstrap(nettyClientConfig, NettyPoolKey.TransactionRole.RMROLE);
        EventLoopGroup rmEventLoopGroupWorker = getEventLoopGroupWorker(rmNettyClientBootstrap);

        Assertions.assertNotEquals(tmEventLoopGroupWorker, rmEventLoopGroupWorker);
    }

    private EventLoopGroup getEventLoopGroupWorker(NettyClientBootstrap bootstrap) {
        try {
            java.lang.reflect.Field field = NettyClientBootstrap.class.getDeclaredField("eventLoopGroupWorker");
            field.setAccessible(true);
            return (EventLoopGroup) field.get(bootstrap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}