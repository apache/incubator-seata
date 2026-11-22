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
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;

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

    @Test
    void testShutdownClosesBoundServerChannel() throws IOException {
        String previousShutdownWait = System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT);
        System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, "0");
        ConfigurationCache.clear();
        try (MockedStatic<MultiRegistryFactory> registryFactoryMock = Mockito.mockStatic(MultiRegistryFactory.class)) {
            registryFactoryMock.when(MultiRegistryFactory::getInstances).thenReturn(Collections.emptyList());

            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }

            NettyServerConfig config = new NettyServerConfig();
            config.setServerListenPort(port);
            NettyServerBootstrap bootstrap = new NettyServerBootstrap(config);
            try {
                bootstrap.start();
                Assertions.assertThrows(IOException.class, () -> {
                    try (ServerSocket probe = new ServerSocket()) {
                        probe.setReuseAddress(false);
                        probe.bind(new InetSocketAddress(port), 1);
                    }
                });

                bootstrap.shutdown();
                bootstrap.shutdown();

                try (ServerSocket rebound = new ServerSocket()) {
                    rebound.setReuseAddress(false);
                    rebound.bind(new InetSocketAddress(port), 1);
                    Assertions.assertEquals(port, rebound.getLocalPort());
                }
            } finally {
                bootstrap.shutdown();
            }
        } finally {
            if (previousShutdownWait == null) {
                System.clearProperty(ConfigurationKeys.SHUTDOWN_WAIT);
            } else {
                System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, previousShutdownWait);
            }
            ConfigurationCache.clear();
        }
    }

    private EventLoopGroup getEventLoopGroup(NettyServerBootstrap bootstrap, String fieldName) {
        try {
            java.lang.reflect.Field field = NettyServerBootstrap.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (EventLoopGroup) field.get(bootstrap);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to access Netty server event loop group: " + fieldName, e);
        }
    }
}
