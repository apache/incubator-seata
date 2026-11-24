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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

                try (ServerSocket rebound = new ServerSocket()) {
                    rebound.setReuseAddress(false);
                    rebound.bind(new InetSocketAddress(port), 1);
                    Assertions.assertEquals(port, rebound.getLocalPort());
                }

                Assertions.assertDoesNotThrow(bootstrap::shutdown);
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

    @Test
    void testStartClosesBoundChannelWhenRegistryFails() throws Exception {
        String previousShutdownWait = System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT);
        System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, "0");
        ConfigurationCache.clear();
        try {
            Exception[] failures = new Exception[] {
                new SocketException("mock socket failure"), new IllegalStateException("mock registry failure")
            };
            for (Exception failure : failures) {
                RegistryService<?> registryService = Mockito.mock(RegistryService.class);

                try (MockedStatic<MultiRegistryFactory> registryFactoryMock =
                        Mockito.mockStatic(MultiRegistryFactory.class)) {
                    registryFactoryMock
                            .when(MultiRegistryFactory::getInstances)
                            .thenReturn(Collections.singletonList(registryService));

                    int port;
                    try (ServerSocket socket = new ServerSocket(0)) {
                        port = socket.getLocalPort();
                    }

                    NettyServerConfig config = new NettyServerConfig();
                    config.setServerListenPort(port);
                    NettyServerBootstrap bootstrap = new NettyServerBootstrap(config);
                    AtomicReference<Channel> capturedChannel = new AtomicReference<>();
                    Mockito.doAnswer(invocation -> {
                                @SuppressWarnings("unchecked")
                                AtomicReference<Channel> serverChannel =
                                        (AtomicReference<Channel>) getField(bootstrap, "serverChannel");
                                Channel boundChannel = serverChannel.get();
                                Assertions.assertNotNull(boundChannel);
                                Assertions.assertTrue(boundChannel.isOpen());
                                capturedChannel.set(boundChannel);
                                throw failure;
                            })
                            .when(registryService)
                            .register(Mockito.any(Instance.class));
                    try {
                        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, bootstrap::start);
                        Assertions.assertSame(failure, thrown.getCause());

                        Channel boundChannel = capturedChannel.get();
                        Assertions.assertNotNull(boundChannel);
                        Assertions.assertFalse(boundChannel.isOpen());
                        Assertions.assertTrue(boundChannel.closeFuture().isDone());

                        @SuppressWarnings("unchecked")
                        AtomicReference<Channel> serverChannel =
                                (AtomicReference<Channel>) getField(bootstrap, "serverChannel");
                        Assertions.assertNull(serverChannel.get());
                    } finally {
                        bootstrap.shutdown();
                    }

                    try (ServerSocket rebound = new ServerSocket()) {
                        rebound.setReuseAddress(false);
                        rebound.bind(new InetSocketAddress(port), 1);
                        Assertions.assertEquals(port, rebound.getLocalPort());
                    }
                }
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

    @Test
    void testShutdownLogsWarningWhenCloseOrBossShutdownTimesOut() {
        Logger logger = (Logger) LoggerFactory.getLogger(NettyServerBootstrap.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        try {
            NettyServerBootstrap bootstrap = Mockito.mock(NettyServerBootstrap.class, Mockito.CALLS_REAL_METHODS);
            Mockito.doReturn(8091).when(bootstrap).getListenPort();

            Channel mockChannel = Mockito.mock(Channel.class);
            ChannelFuture closeFuture = Mockito.mock(ChannelFuture.class);
            Mockito.when(mockChannel.close()).thenReturn(closeFuture);
            Mockito.when(closeFuture.awaitUninterruptibly(5L, TimeUnit.SECONDS)).thenReturn(false);

            EventExecutor bossExecutor = Mockito.mock(EventExecutor.class);
            Mockito.when(bossExecutor.inEventLoop()).thenReturn(false);

            EventLoopGroup mockBossGroup = Mockito.mock(EventLoopGroup.class);
            Mockito.when(mockBossGroup.iterator())
                    .thenReturn(Collections.singletonList(bossExecutor).iterator());
            Future<?> bossFuture = Mockito.mock(Future.class);
            Mockito.doReturn(bossFuture).when(mockBossGroup).shutdownGracefully(0L, 5L, TimeUnit.SECONDS);
            Mockito.when(bossFuture.awaitUninterruptibly(5L, TimeUnit.SECONDS)).thenReturn(false);

            EventLoopGroup mockWorkerGroup = Mockito.mock(EventLoopGroup.class);

            setField(bootstrap, "initialized", new AtomicBoolean(false));
            setField(bootstrap, "serverChannel", new AtomicReference<>(mockChannel));
            setField(bootstrap, "eventLoopGroupBoss", mockBossGroup);
            setField(bootstrap, "eventLoopGroupWorker", mockWorkerGroup);

            Assertions.assertDoesNotThrow(bootstrap::shutdown);
            Mockito.verify(closeFuture).awaitUninterruptibly(5L, TimeUnit.SECONDS);
            Mockito.verify(bossFuture).awaitUninterruptibly(5L, TimeUnit.SECONDS);
            Mockito.verify(mockWorkerGroup).shutdownGracefully();

            boolean hasChannelWarn = events.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains("server channel"));
            boolean hasBossWarn = events.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains("boss event loop group"));
            Assertions.assertTrue(hasChannelWarn);
            Assertions.assertTrue(hasBossWarn);
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    @Test
    void testShutdownHandlesNullFieldsGracefully() {
        NettyServerBootstrap bootstrap = Mockito.mock(NettyServerBootstrap.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(8091).when(bootstrap).getListenPort();
        setField(bootstrap, "initialized", new AtomicBoolean(false));

        Assertions.assertDoesNotThrow(bootstrap::shutdown);
    }

    @Test
    void testShutdownInsideBossEventLoopAndChannelCloseFailure() {
        NettyServerBootstrap bootstrap = Mockito.mock(NettyServerBootstrap.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(8091).when(bootstrap).getListenPort();

        Channel mockChannel = Mockito.mock(Channel.class);
        Mockito.when(mockChannel.close()).thenThrow(new RuntimeException("close failed"));

        EventExecutor otherExecutor = Mockito.mock(EventExecutor.class);
        Mockito.when(otherExecutor.inEventLoop()).thenReturn(false);
        EventExecutor bossExecutor = Mockito.mock(EventExecutor.class);
        Mockito.when(bossExecutor.inEventLoop()).thenReturn(true);

        EventLoopGroup mockBossGroup = Mockito.mock(EventLoopGroup.class);
        Mockito.when(mockBossGroup.iterator())
                .thenReturn(Arrays.asList(otherExecutor, bossExecutor).iterator());
        Future<?> bossFuture = Mockito.mock(Future.class);
        Mockito.doReturn(bossFuture).when(mockBossGroup).shutdownGracefully(0L, 5L, TimeUnit.SECONDS);

        EventLoopGroup mockWorkerGroup = Mockito.mock(EventLoopGroup.class);

        setField(bootstrap, "initialized", new AtomicBoolean(false));
        setField(bootstrap, "serverChannel", new AtomicReference<>(mockChannel));
        setField(bootstrap, "eventLoopGroupBoss", mockBossGroup);
        setField(bootstrap, "eventLoopGroupWorker", mockWorkerGroup);

        Assertions.assertDoesNotThrow(bootstrap::shutdown);
        Mockito.verify(mockBossGroup).shutdownGracefully(0L, 5L, TimeUnit.SECONDS);
        Mockito.verify(bossFuture, Mockito.never()).awaitUninterruptibly(5L, TimeUnit.SECONDS);
        Mockito.verify(mockWorkerGroup).shutdownGracefully();
    }

    @Test
    void testShutdownHandlesBossEventLoopShutdownFailure() {
        NettyServerBootstrap bootstrap = Mockito.mock(NettyServerBootstrap.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(8091).when(bootstrap).getListenPort();

        EventLoopGroup mockBossGroup = Mockito.mock(EventLoopGroup.class);
        Mockito.when(mockBossGroup.iterator()).thenThrow(new RuntimeException("boss shutdown failed"));
        EventLoopGroup mockWorkerGroup = Mockito.mock(EventLoopGroup.class);

        setField(bootstrap, "initialized", new AtomicBoolean(false));
        setField(bootstrap, "serverChannel", new AtomicReference<>());
        setField(bootstrap, "eventLoopGroupBoss", mockBossGroup);
        setField(bootstrap, "eventLoopGroupWorker", mockWorkerGroup);

        Assertions.assertDoesNotThrow(bootstrap::shutdown);
        Mockito.verify(mockWorkerGroup).shutdownGracefully();
    }

    private EventLoopGroup getEventLoopGroup(NettyServerBootstrap bootstrap, String fieldName) {
        return (EventLoopGroup) getField(bootstrap, fieldName);
    }

    private Object getField(NettyServerBootstrap bootstrap, String fieldName) {
        try {
            Field field = NettyServerBootstrap.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(bootstrap);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to access Netty server field: " + fieldName, e);
        }
    }

    private void setField(NettyServerBootstrap bootstrap, String fieldName, Object value) {
        try {
            Field field = NettyServerBootstrap.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(bootstrap, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set Netty server field: " + fieldName, e);
        }
    }
}
