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
import org.apache.seata.common.XID;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;

import static org.apache.seata.common.ConfigurationKeys.SERVER_REGISTRY_PORT_CAMEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NettyServerBootstrapTest {

    private int listenPort;

    private int registryPort;

    @BeforeEach
    void setUp() throws Exception {
        listenPort = findFreePort();
        // the advertised (registry) port differs from the listen port, as with a docker port mapping
        registryPort = listenPort + 1000;
        XID.setIpAddress("127.0.0.1");
        XID.setPort(listenPort);
        Instance.getInstance().setTransaction(null);
        System.setProperty(SERVER_REGISTRY_PORT_CAMEL, String.valueOf(registryPort));
        System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, "1");
        ConfigurationCache.clear();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(SERVER_REGISTRY_PORT_CAMEL);
        System.clearProperty(ConfigurationKeys.SHUTDOWN_WAIT);
        Instance.getInstance().setTransaction(null);
        ConfigurationCache.clear();
    }

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
    void testRegistrationAndUnregistrationUseTheAdvertisedPort() throws Exception {
        RegistryService<?> registryService = mock(RegistryService.class);
        List<RegistryService<?>> registryServices = Collections.singletonList(registryService);
        try (MockedStatic<MultiRegistryFactory> registryFactoryMock = Mockito.mockStatic(MultiRegistryFactory.class)) {
            registryFactoryMock.when(MultiRegistryFactory::getInstances).thenReturn(registryServices);

            NettyServerBootstrap bootstrap = new NettyServerBootstrap(new NettyServerConfig());
            bootstrap.setListenPort(listenPort);
            bootstrap.start();

            // the instance registered to the registry center advertises the registry port, not the listen port
            Instance instance = Instance.getInstance();
            assertNotNull(instance.getTransaction());
            assertEquals(registryPort, instance.getTransaction().getPort());
            assertNotEquals(listenPort, instance.getTransaction().getPort());

            ArgumentCaptor<Instance> registered = ArgumentCaptor.forClass(Instance.class);
            verify(registryService).register(registered.capture());
            assertEquals(registryPort, registered.getValue().getTransaction().getPort());

            bootstrap.shutdown();

            // the shutdown unregisters the very same instance, so the advertised endpoint is cleaned up
            ArgumentCaptor<Instance> unregistered = ArgumentCaptor.forClass(Instance.class);
            verify(registryService).unregister(unregistered.capture());
            assertEquals(registryPort, unregistered.getValue().getTransaction().getPort());
            verify(registryService).close();
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

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
