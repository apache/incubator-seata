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
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class ConsulConfigurationTest {

    private ConsulConfiguration consulConfig;
    private ConsulClient mockConsulClient;
    private Configuration mockFileConfig;
    private MockedStatic<NetUtil> mockedNetUtil;

    @BeforeEach
    void setUp() {
        System.setProperty("seataEnv", "test");
        // Mock dependencies
        mockFileConfig = mock(Configuration.class);
        mockConsulClient = mock(ConsulClient.class);
        mockedNetUtil = mockStatic(NetUtil.class);

        // Setup static mocks
        when(mockFileConfig.getConfig(anyString(), anyString())).thenReturn("seata.properties");
        when(mockFileConfig.getConfig(anyString())).thenReturn("localhost:8500");
        mockedNetUtil
                .when(() -> NetUtil.toInetSocketAddress("127.0.0.1:8500"))
                .thenReturn(new InetSocketAddress("localhost", 8500));

        GetValue mockValue = mock(GetValue.class);
        when(mockValue.getDecodedValue()).thenReturn("testValue");
        Response<GetValue> mockResponse = new Response<>(mockValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue("seata.properties", (String) null)).thenReturn(mockResponse);

        setField(null, "client", mockConsulClient);

        // Initialize singleton
        consulConfig = ConsulConfiguration.getInstance();
    }

    @AfterEach
    void tearDown() {
        mockedNetUtil.close();
        reset(mockConsulClient);
    }

    @Test
    void testSingletonInstance() {
        ConsulConfiguration anotherInstance = ConsulConfiguration.getInstance();
        assertSame(consulConfig, anotherInstance);
    }

    @Test
    void testGetLatestConfig() throws InterruptedException {
        // Mock Consul response
        GetValue mockValue = mock(GetValue.class);
        when(mockValue.getDecodedValue()).thenReturn("testValue");
        Response<GetValue> mockResponse = new Response<>(mockValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue("testKey", (String) null)).thenReturn(mockResponse);

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
    void testInitSeataConfig() throws Exception {
        // Mock initial config load
        GetValue initValue = mock(GetValue.class);
        when(initValue.getDecodedValue()).thenReturn("val1");
        Response<GetValue> initResponse = new Response<>(initValue, 1L, false, 1L);
        when(mockConsulClient.getKVValue(eq("key1"), (String) isNull())).thenReturn(initResponse);

        ConsulConfiguration newInstance = ConsulConfiguration.getInstance();

        assertEquals("val1", newInstance.getLatestConfig("key1", null, 1000));
    }

    // Utility method to set private fields via reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = ConsulConfiguration.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
