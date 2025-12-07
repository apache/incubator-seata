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

import io.netty.channel.Channel;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.discovery.registry.RegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Netty client channel manager test.
 *
 */
@ExtendWith(MockitoExtension.class)
class NettyClientChannelManagerTest {

    private NettyClientChannelManager channelManager;

    @Mock
    private NettyPoolableFactory poolableFactory;

    @Mock
    private Function<String, NettyPoolKey> poolKeyFunction;

    private NettyClientConfig nettyClientConfig = new NettyClientConfig();

    @Mock
    private NettyPoolKey nettyPoolKey;

    @Mock
    private Channel channel;

    @Mock
    private Channel newChannel;

    @Mock
    private GenericKeyedObjectPool keyedObjectPool;

    @Mock
    private RegistryService registryService;

    @BeforeEach
    void setUp() {
        channelManager = new NettyClientChannelManager(poolableFactory, poolKeyFunction, nettyClientConfig);
    }

    @AfterEach
    void tearDown() {}

    @Test
    void assertAcquireChannelFromPool() {
        setupPoolFactory(nettyPoolKey, channel);
        Channel actual = channelManager.acquireChannel("localhost");
        verify(poolableFactory).makeObject(nettyPoolKey);
        Assertions.assertEquals(actual, channel);
    }

    private void setupPoolFactory(final NettyPoolKey nettyPoolKey, final Channel channel) {
        when(poolKeyFunction.apply(anyString())).thenReturn(nettyPoolKey);
        when(poolableFactory.makeObject(nettyPoolKey)).thenReturn(channel);
        when(poolableFactory.validateObject(nettyPoolKey, channel)).thenReturn(true);
    }

    @Test
    void assertAcquireChannelFromCache() {
        channelManager.getChannels().putIfAbsent("localhost", channel);
        when(channel.isActive()).thenReturn(true);
        Channel actual = channelManager.acquireChannel("localhost");
        verify(poolableFactory, times(0)).makeObject(nettyPoolKey);
        Assertions.assertEquals(actual, channel);
    }

    @Test
    void assertAcquireChannelFromPoolContainsInactiveCache() {
        channelManager.getChannels().putIfAbsent("localhost", channel);
        when(channel.isActive()).thenReturn(false);
        setupPoolFactory(nettyPoolKey, newChannel);
        Channel actual = channelManager.acquireChannel("localhost");
        verify(poolableFactory).makeObject(nettyPoolKey);
        Assertions.assertEquals(actual, newChannel);
    }

    @Test
    void assertReconnect() {
        channelManager.getChannels().putIfAbsent("127.0.0.1:8091", channel);
        channelManager.reconnect(DEFAULT_TX_GROUP);
    }

    @Test
    @SuppressWarnings("unchecked")
    void assertReleaseChannelWhichCacheIsEmpty() throws Exception {
        setNettyClientKeyPool();
        setUpReleaseChannel();
        channelManager.releaseChannel(channel, "127.0.0.1:8091");
        verify(keyedObjectPool).returnObject(nettyPoolKey, channel);
    }

    @Test
    @SuppressWarnings("unchecked")
    void assertReleaseCachedChannel() throws Exception {
        setNettyClientKeyPool();
        setUpReleaseChannel();
        channelManager.getChannels().putIfAbsent("127.0.0.1:8091", channel);
        channelManager.releaseChannel(channel, "127.0.0.1:8091");
        assertTrue(channelManager.getChannels().isEmpty());
        verify(keyedObjectPool).returnObject(nettyPoolKey, channel);
    }

    @Test
    @SuppressWarnings("unchecked")
    void assertReleaseChannelNotEqualToCache() throws Exception {
        setNettyClientKeyPool();
        setUpReleaseChannel();
        channelManager.getChannels().putIfAbsent("127.0.0.1:8091", newChannel);
        channelManager.releaseChannel(channel, "127.0.0.1:8091");
        assertEquals(1, channelManager.getChannels().size());
        verify(keyedObjectPool).returnObject(nettyPoolKey, channel);
    }

    @SuppressWarnings("unchecked")
    private void setUpReleaseChannel() {
        ConcurrentMap<String, Object> channelLocks =
                (ConcurrentMap<String, Object>) getFieldValue("channelLocks", channelManager);
        channelLocks.putIfAbsent("127.0.0.1:8091", new Object());
        ConcurrentMap<String, NettyPoolKey> poolKeyMap =
                (ConcurrentMap<String, NettyPoolKey>) getFieldValue("poolKeyMap", channelManager);
        poolKeyMap.putIfAbsent("127.0.0.1:8091", nettyPoolKey);
    }

    private Object getFieldValue(final String fieldName, final Object targetObject) {
        try {
            Field field = targetObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(targetObject);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void setNettyClientKeyPool() {
        try {
            Field field = channelManager.getClass().getDeclaredField("nettyClientKeyPool");
            field.setAccessible(true);
            field.set(channelManager, keyedObjectPool);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void testThrowFailFastExceptionWhenFailFastIsTrue() throws Exception {
        Method method =
                channelManager.getClass().getDeclaredMethod("throwFailFastException", boolean.class, String.class);
        method.setAccessible(true);

        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            try {
                method.invoke(channelManager, true, "Test error message");
            } catch (Exception e) {
                if (e.getCause() instanceof FrameworkException) {
                    throw (FrameworkException) e.getCause();
                }
                throw e;
            }
        });

        assertEquals("Test error message", exception.getMessage());
    }

    @Test
    void testThrowFailFastExceptionWhenFailFastIsFalse() throws Exception {
        Method method =
                channelManager.getClass().getDeclaredMethod("throwFailFastException", boolean.class, String.class);
        method.setAccessible(true);

        // Should not throw any exception
        method.invoke(channelManager, false, "Test error message");
    }

    @Test
    void testRegisterChannelWhenNoExistingChannel() {
        String serverAddress = "127.0.0.1:8091";
        String version = "1.5.0";
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 8091);

        when(channel.remoteAddress()).thenReturn(remoteAddress);

        channelManager.registerChannel(serverAddress, channel, version);

        assertEquals(channel, channelManager.getChannels().get(serverAddress));
        assertEquals(version, Version.getChannelVersion(channel));
    }

    @Test
    void testRegisterChannelWhenExistingChannelIsActive() {
        String serverAddress = "127.0.0.1:8091";
        String version = "1.5.0";

        when(newChannel.isActive()).thenReturn(true);

        // Put an active channel first
        channelManager.getChannels().put(serverAddress, newChannel);

        channelManager.registerChannel(serverAddress, channel, version);

        // Should not replace the active channel
        assertEquals(newChannel, channelManager.getChannels().get(serverAddress));
    }

    @Test
    void testRegisterChannelWhenExistingChannelIsInactive() {
        String serverAddress = "127.0.0.1:8091";
        String version = "1.5.0";
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 8091);

        when(newChannel.isActive()).thenReturn(false);
        when(channel.remoteAddress()).thenReturn(remoteAddress);

        // Put an inactive channel first
        channelManager.getChannels().put(serverAddress, newChannel);

        channelManager.registerChannel(serverAddress, channel, version);

        // Should replace the inactive channel
        assertEquals(channel, channelManager.getChannels().get(serverAddress));
        assertEquals(version, Version.getChannelVersion(channel));
    }

    @Test
    void testDoReconnectWithAvailableServers() {
        String transactionServiceGroup = "default_tx_group";
        List<InetSocketAddress> availableServers = new ArrayList<>();
        availableServers.add(new InetSocketAddress("127.0.0.1", 8091));

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenReturn(availableServers);

            setupPoolFactory(nettyPoolKey, channel);

            // Should not throw exception
            channelManager.doReconnect(transactionServiceGroup, false);

            // Verify that acquireChannel was attempted
            assertNotNull(channelManager.getChannels().get("127.0.0.1:8091"));
        } catch (Exception e) {
            Assertions.fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWithNoAvailableServersAndFailFastTrue() {
        String transactionServiceGroup = "default_tx_group";

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenReturn(Collections.emptyList());
            when(registryService.getServiceGroup(transactionServiceGroup)).thenReturn("default");

            assertThrows(FrameworkException.class, () -> {
                channelManager.doReconnect(transactionServiceGroup, true);
            });
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWithNoAvailableServersAndFailFastFalse() {
        String transactionServiceGroup = "default_tx_group";

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenReturn(Collections.emptyList());
            when(registryService.getServiceGroup(transactionServiceGroup)).thenReturn("default");

            // Should not throw exception
            channelManager.doReconnect(transactionServiceGroup, false);
        } catch (Exception e) {
            Assertions.fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWithBlankClusterNameAndFailFastTrue() {
        String transactionServiceGroup = "default_tx_group";

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenReturn(Collections.emptyList());
            when(registryService.getServiceGroup(transactionServiceGroup)).thenReturn("");

            assertThrows(FrameworkException.class, () -> {
                channelManager.doReconnect(transactionServiceGroup, true);
            });
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWithBlankClusterNameAndFailFastFalse() {
        String transactionServiceGroup = "default_tx_group";

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenReturn(Collections.emptyList());
            when(registryService.getServiceGroup(transactionServiceGroup)).thenReturn("");

            // Should not throw exception
            channelManager.doReconnect(transactionServiceGroup, false);
        } catch (Exception e) {
            Assertions.fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWhenGetAvailServerListThrowsExceptionWithFailFastTrue() {
        String transactionServiceGroup = "default_tx_group";

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenThrow(new RuntimeException("Registry error"));

            assertThrows(FrameworkException.class, () -> {
                channelManager.doReconnect(transactionServiceGroup, true);
            });
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWhenGetAvailServerListThrowsExceptionWithFailFastFalse() {
        String transactionServiceGroup = "default_tx_group";

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenThrow(new RuntimeException("Registry error"));

            // Should not throw exception
            channelManager.doReconnect(transactionServiceGroup, false);
        } catch (Exception e) {
            Assertions.fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testDoReconnectWithPartialFailures() {
        String transactionServiceGroup = "default_tx_group";
        List<InetSocketAddress> availableServers = new ArrayList<>();
        availableServers.add(new InetSocketAddress("127.0.0.1", 8091));
        availableServers.add(new InetSocketAddress("127.0.0.1", 8092));

        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.lookup(transactionServiceGroup)).thenReturn(availableServers);

            // Setup first server to succeed, second to fail
            when(poolKeyFunction.apply("127.0.0.1:8091")).thenReturn(nettyPoolKey);
            when(poolableFactory.makeObject(nettyPoolKey)).thenReturn(channel);
            when(poolableFactory.validateObject(nettyPoolKey, channel)).thenReturn(true);

            when(poolKeyFunction.apply("127.0.0.1:8092")).thenThrow(new RuntimeException("Connection failed"));

            // Should not throw exception even with partial failures
            channelManager.doReconnect(transactionServiceGroup, false);

            // Verify at least one connection succeeded
            assertFalse(channelManager.getChannels().isEmpty());
        } catch (Exception e) {
            Assertions.fail("Should not throw exception: " + e.getMessage());
        }
    }
}
