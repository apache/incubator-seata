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
package org.apache.seata.config.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsulConfigurationTest {

    private static final String ACL_TOKEN = "test-token";
    private ConsulConfiguration consulConfig;
    private ConsulClient mockConsulClient;
    private String previousSeataEnv;
    private String previousAclToken;
    private String previousConsulKey;

    @BeforeEach
    void setUp() {
        previousSeataEnv = System.setProperty("seataEnv", "test");
        previousAclToken = System.setProperty("aclToken", ACL_TOKEN);
        previousConsulKey = System.setProperty("config.consul.key", "seata.properties");
        mockConsulClient = mock(ConsulClient.class);

        GetValue mockValue = mock(GetValue.class);
        when(mockValue.getDecodedValue()).thenReturn("key1=val1");
        Response<GetValue> mockResponse = new Response<>(mockValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue("seata.properties", ACL_TOKEN)).thenReturn(mockResponse);

        setField(null, "instance", null);
        setField(null, "client", mockConsulClient);

        // Keep the initialization watch from starting an unbounded background task.
        try (MockedConstruction<ConsulConfiguration.ConsulListener> ignored =
                mockConstruction(ConsulConfiguration.ConsulListener.class)) {
            consulConfig = ConsulConfiguration.getInstance();
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        try {
            if (consulConfig != null) {
                ExecutorService executor = (ExecutorService) getField(consulConfig, "consulNotifierExecutor");
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            }
        } finally {
            setField(null, "instance", null);
            setField(null, "client", null);
            setField(null, "seataConfig", new Properties());
            restoreProperty("seataEnv", previousSeataEnv);
            restoreProperty("aclToken", previousAclToken);
            restoreProperty("config.consul.key", previousConsulKey);
        }
    }

    @Test
    void testSingletonInstance() {
        ConsulConfiguration anotherInstance = ConsulConfiguration.getInstance();
        assertSame(consulConfig, anotherInstance);
    }

    @Test
    void testGetLatestConfig() {
        // Mock Consul response
        GetValue mockValue = mock(GetValue.class);
        when(mockValue.getDecodedValue()).thenReturn("testValue");
        Response<GetValue> mockResponse = new Response<>(mockValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue("testKey", ACL_TOKEN)).thenReturn(mockResponse);

        String result = consulConfig.getLatestConfig("testKey", "default", 3000);
        assertEquals("testValue", result);
    }

    @Test
    void testPutConfigIfAbsent() {
        // Mock atomic put response
        Response<Boolean> casResponse = new Response<>(true, 1L, false, 1L);
        when(mockConsulClient.setKVValue(anyString(), anyString(), any(), any(PutParams.class)))
                .thenReturn(casResponse);

        assertTrue(consulConfig.putConfigIfAbsent("atomicKey", "atomicValue", 3000));
    }

    @Test
    void testInitSeataConfig() {
        assertEquals("val1", consulConfig.getLatestConfig("key1", null, 0));
        verify(mockConsulClient).getKVValue("seata.properties", ACL_TOKEN);
        verify(mockConsulClient, never()).getKVValue("key1", ACL_TOKEN);
    }

    @Test
    void testOnChangeEvent_skipWhenValueIsBlank() {
        String dataId = "seata.properties";

        // Mock the initial call in ConsulListener constructor (2-arg version)
        GetValue initValue = mock(GetValue.class);
        when(initValue.getDecodedValue()).thenReturn("dummy");
        Response<GetValue> initResponse = new Response<>(initValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue(dataId, ACL_TOKEN)).thenReturn(initResponse);

        // Mock the watch call in onChangeEvent loop (3-arg version)
        GetValue blankValue = mock(GetValue.class);
        when(blankValue.getDecodedValue()).thenReturn("");
        Response<GetValue> blankResponse = new Response<>(blankValue, 2L, false, 2L);
        RuntimeException stopWatching = new RuntimeException("stop watching after the blank response");
        when(mockConsulClient.getKVValue(eq(dataId), eq(ACL_TOKEN), any(QueryParams.class)))
                .thenReturn(blankResponse)
                .thenThrow(stopWatching);

        ConsulConfiguration.ConsulListener listener = new ConsulConfiguration.ConsulListener(dataId, null);

        try {
            assertSame(
                    stopWatching,
                    assertThrows(RuntimeException.class, () -> listener.onChangeEvent(new ConfigurationChangeEvent())));
            assertEquals("val1", consulConfig.getLatestConfig("key1", null, 0));
            verify(mockConsulClient, times(2)).getKVValue(eq(dataId), eq(ACL_TOKEN), any(QueryParams.class));
        } finally {
            listener.onShutDown();
        }
    }

    // Utility method to set private fields via reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = ConsulConfiguration.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getField(Object target, String fieldName) {
        try {
            Field field = ConsulConfiguration.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
