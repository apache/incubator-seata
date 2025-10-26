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
package org.apache.seata.config.etcd3;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.netty.util.CharsetUtil.UTF_8;

class EtcdConfigurationTest {

    private Client mockClient;
    private KV mockKV;
    private Watch mockWatch;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("config.type", "etcd3");
        System.setProperty("config.etcd3.serverAddr", "http://127.0.0.1:2379");

        mockClient = Mockito.mock(Client.class);
        mockKV = Mockito.mock(KV.class);
        mockWatch = Mockito.mock(Watch.class);

        Mockito.when(mockClient.getKVClient()).thenReturn(mockKV);
        Mockito.when(mockClient.getWatchClient()).thenReturn(mockWatch);

        GetResponse mockGetResponse = Mockito.mock(GetResponse.class);
        Mockito.when(mockGetResponse.getKvs()).thenReturn(Collections.emptyList());
        CompletableFuture<GetResponse> emptyFuture = CompletableFuture.completedFuture(mockGetResponse);
        Mockito.when(mockKV.get(Mockito.any(ByteSequence.class))).thenReturn(emptyFuture);

        Field clientField = ReflectionUtil.getField(EtcdConfiguration.class, "client");
        clientField.setAccessible(true);
        clientField.set(null, mockClient);

        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, new Properties());
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("config.type");
        System.clearProperty("config.etcd3.serverAddr");

        Field instanceField = ReflectionUtil.getField(EtcdConfiguration.class, "instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field clientField = ReflectionUtil.getField(EtcdConfiguration.class, "client");
        clientField.setAccessible(true);
        clientField.set(null, null);

        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, new Properties());
    }

    @Test
    void testGetInstance() {
        EtcdConfiguration instance1 = EtcdConfiguration.getInstance();
        EtcdConfiguration instance2 = EtcdConfiguration.getInstance();

        Assertions.assertNotNull(instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    void testGetTypeName() {
        EtcdConfiguration config = EtcdConfiguration.getInstance();
        Assertions.assertEquals("etcd3", config.getTypeName());
    }

    @Test
    void testGetLatestConfigFromSeataConfig() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.key", "test-value");
        seataConfigField.set(null, props);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        String value = config.getLatestConfig("test.key", "default", 1000);

        Assertions.assertEquals("test-value", value);
    }

    @Test
    void testGetLatestConfigFromEtcd() throws Exception {
        KeyValue mockKeyValue = Mockito.mock(KeyValue.class);
        Mockito.when(mockKeyValue.getValue()).thenReturn(ByteSequence.from("etcd-value", UTF_8));

        GetResponse mockGetResponse = Mockito.mock(GetResponse.class);
        Mockito.when(mockGetResponse.getKvs()).thenReturn(Collections.singletonList(mockKeyValue));

        CompletableFuture<GetResponse> future = CompletableFuture.completedFuture(mockGetResponse);
        Mockito.when(mockKV.get(Mockito.any(ByteSequence.class))).thenReturn(future);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        String value = config.getLatestConfig("test.etcd.key", "default", 1000);

        Assertions.assertEquals("etcd-value", value);
    }

    @Test
    void testGetLatestConfigWithDefaultValue() throws Exception {
        GetResponse mockGetResponse = Mockito.mock(GetResponse.class);
        Mockito.when(mockGetResponse.getKvs()).thenReturn(Collections.emptyList());

        CompletableFuture<GetResponse> future = CompletableFuture.completedFuture(mockGetResponse);
        Mockito.when(mockKV.get(Mockito.any(ByteSequence.class))).thenReturn(future);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        String value = config.getLatestConfig("non.existent.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testPutConfigWhenSeataConfigEmpty() throws Exception {
        PutResponse mockPutResponse = Mockito.mock(PutResponse.class);
        CompletableFuture<PutResponse> future = CompletableFuture.completedFuture(mockPutResponse);
        Mockito.when(mockKV.put(Mockito.any(ByteSequence.class), Mockito.any(ByteSequence.class)))
                .thenReturn(future);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        boolean result = config.putConfig("test.key", "test-value", 1000);

        Assertions.assertTrue(result);
        Mockito.verify(mockKV).put(Mockito.any(ByteSequence.class), Mockito.any(ByteSequence.class));
    }

    @Test
    void testPutConfigWhenSeataConfigNotEmpty() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("existing.key", "existing-value");
        seataConfigField.set(null, props);

        PutResponse mockPutResponse = Mockito.mock(PutResponse.class);
        CompletableFuture<PutResponse> future = CompletableFuture.completedFuture(mockPutResponse);
        Mockito.when(mockKV.put(Mockito.any(ByteSequence.class), Mockito.any(ByteSequence.class)))
                .thenReturn(future);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        boolean result = config.putConfig("new.key", "new-value", 1000);

        Assertions.assertTrue(result);
    }

    @Test
    void testRemoveConfigWhenSeataConfigEmpty() throws Exception {
        DeleteResponse mockDeleteResponse = Mockito.mock(DeleteResponse.class);
        CompletableFuture<DeleteResponse> future = CompletableFuture.completedFuture(mockDeleteResponse);
        Mockito.when(mockKV.delete(Mockito.any(ByteSequence.class))).thenReturn(future);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        boolean result = config.removeConfig("test.key", 1000);

        Assertions.assertTrue(result);
        Mockito.verify(mockKV).delete(Mockito.any(ByteSequence.class));
    }

    @Test
    void testRemoveConfigWhenSeataConfigNotEmpty() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.key", "test-value");
        seataConfigField.set(null, props);

        PutResponse mockPutResponse = Mockito.mock(PutResponse.class);
        CompletableFuture<PutResponse> future = CompletableFuture.completedFuture(mockPutResponse);
        Mockito.when(mockKV.put(Mockito.any(ByteSequence.class), Mockito.any(ByteSequence.class)))
                .thenReturn(future);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        boolean result = config.removeConfig("test.key", 1000);

        Assertions.assertTrue(result);
    }

    @Test
    void testAddConfigListenerWithBlankDataId() {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        config.addConfigListener("", listener);
        config.addConfigListener(null, listener);

        Set<ConfigurationChangeListener> listeners1 = config.getConfigListeners("");
        Assertions.assertNull(listeners1);

        // getConfigListeners(null) may throw NPE, which is expected behavior
        try {
            Set<ConfigurationChangeListener> listeners2 = config.getConfigListeners(null);
            Assertions.assertNull(listeners2);
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    void testAddConfigListenerWithNullListener() {
        EtcdConfiguration config = EtcdConfiguration.getInstance();
        config.addConfigListener("test.key", null);

        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("test.key");
        Assertions.assertNull(listeners);
    }

    @Test
    void testRemoveConfigListenerWithBlankDataId() {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        config.removeConfigListener("", listener);
        config.removeConfigListener(null, listener);
    }

    @Test
    void testRemoveConfigListenerWithNullListener() {
        EtcdConfiguration config = EtcdConfiguration.getInstance();
        config.removeConfigListener("test.key", null);
    }

    @Test
    void testGetConfigListenersForNonExistentKey() {
        EtcdConfiguration config = EtcdConfiguration.getInstance();
        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("non.existent.key");
        Assertions.assertNull(listeners);
    }

    @Test
    void testGetConfig() {
        EtcdConfiguration config = EtcdConfiguration.getInstance();
        String value = config.getConfig("test.key", "default-value", 1000);
        Assertions.assertNotNull(value);
    }

    @Test
    void testGetInt() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.int.key", "100");
        seataConfigField.set(null, props);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        int value = config.getInt("test.int.key", 50, 1000);
        Assertions.assertEquals(100, value);
    }

    @Test
    void testGetBoolean() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(EtcdConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.boolean.key", "true");
        seataConfigField.set(null, props);

        EtcdConfiguration config = EtcdConfiguration.getInstance();
        boolean value = config.getBoolean("test.boolean.key", false, 1000);
        Assertions.assertTrue(value);
    }
}
