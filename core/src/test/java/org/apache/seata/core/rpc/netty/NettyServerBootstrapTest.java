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
import io.netty.channel.MultiThreadIoEventLoopGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class NettyServerBootstrapTest {

    @Test
    void testNioEventLoopGroups() {
        NettyServerBootstrap bootstrap;
        try (MockedStatic<NettyServerConfig> mockedConfig =
                Mockito.mockStatic(NettyServerConfig.class, Mockito.CALLS_REAL_METHODS)) {
            mockedConfig.when(NettyServerConfig::enableEpoll).thenReturn(false);
            bootstrap = new NettyServerBootstrap(new NettyServerConfig());
        }

        EventLoopGroup bossGroup = getEventLoopGroup(bootstrap, "eventLoopGroupBoss");
        EventLoopGroup workerGroup = getEventLoopGroup(bootstrap, "eventLoopGroupWorker");
        try {
            Assertions.assertInstanceOf(MultiThreadIoEventLoopGroup.class, bossGroup);
            Assertions.assertInstanceOf(MultiThreadIoEventLoopGroup.class, workerGroup);
        } finally {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    private EventLoopGroup getEventLoopGroup(NettyServerBootstrap bootstrap, String fieldName) {
        try {
            java.lang.reflect.Field field = NettyServerBootstrap.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (EventLoopGroup) field.get(bootstrap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
