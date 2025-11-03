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
package org.apache.seata.config.springcloud;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.ConfigurationChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

class SpringCloudConfigurationTest {

    private ApplicationContext mockApplicationContext;
    private Environment mockEnvironment;

    @BeforeEach
    void setUp() {
        mockApplicationContext = Mockito.mock(ApplicationContext.class);
        mockEnvironment = Mockito.mock(Environment.class);

        Mockito.when(mockApplicationContext.getEnvironment()).thenReturn(mockEnvironment);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT, mockApplicationContext);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clear the OBJECT_MAP in ObjectHolder using reflection
        Field objectMapField = ReflectionUtil.getField(ObjectHolder.class, "OBJECT_MAP");
        objectMapField.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) objectMapField.get(ObjectHolder.INSTANCE);
        objectMap.clear();

        Field instanceField = ReflectionUtil.getField(SpringCloudConfiguration.class, "instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testGetInstance() {
        SpringCloudConfiguration instance1 = SpringCloudConfiguration.getInstance();
        SpringCloudConfiguration instance2 = SpringCloudConfiguration.getInstance();

        Assertions.assertNotNull(instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    void testGetTypeName() {
        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        Assertions.assertEquals("SpringCloudConfig", config.getTypeName());
    }

    @Test
    void testGetLatestConfigFromEnvironment() {
        Mockito.when(mockEnvironment.getProperty("seata.test.key")).thenReturn("test-value");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        String value = config.getLatestConfig("test.key", "default-value", 1000);

        Assertions.assertEquals("test-value", value);
        Mockito.verify(mockEnvironment).getProperty("seata.test.key");
    }

    @Test
    void testGetLatestConfigWithDefaultValue() {
        Mockito.when(mockEnvironment.getProperty("seata.non.existent.key")).thenReturn(null);

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        String value = config.getLatestConfig("non.existent.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testGetLatestConfigWithBlankValue() {
        Mockito.when(mockEnvironment.getProperty("seata.blank.key")).thenReturn("");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        String value = config.getLatestConfig("blank.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testGetLatestConfigWhenEnvironmentIsNull() {
        Mockito.when(mockApplicationContext.getEnvironment()).thenReturn(null);

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        String value = config.getLatestConfig("test.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testPutConfig() {
        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        boolean result = config.putConfig("test.key", "test-value", 1000);

        Assertions.assertFalse(result);
    }

    @Test
    void testPutConfigIfAbsent() {
        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        boolean result = config.putConfigIfAbsent("test.key", "test-value", 1000);

        Assertions.assertFalse(result);
    }

    @Test
    void testRemoveConfig() {
        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        boolean result = config.removeConfig("test.key", 1000);

        Assertions.assertFalse(result);
    }

    @Test
    void testAddConfigListener() {
        ConfigurationChangeListener listener = Mockito.mock(ConfigurationChangeListener.class);

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        config.addConfigListener("test.key", listener);
    }

    @Test
    void testRemoveConfigListener() {
        ConfigurationChangeListener listener = Mockito.mock(ConfigurationChangeListener.class);

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        config.removeConfigListener("test.key", listener);
    }

    @Test
    void testGetConfigListeners() {
        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        Set<org.apache.seata.config.ConfigurationChangeListener> listeners = config.getConfigListeners("test.key");

        Assertions.assertNull(listeners);
    }

    @Test
    void testGetConfig() {
        Mockito.when(mockEnvironment.getProperty("seata.service.vgroupMapping")).thenReturn("default");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        String value = config.getConfig("service.vgroupMapping", "default-value", 1000);

        Assertions.assertEquals("default", value);
    }

    @Test
    void testGetInt() {
        Mockito.when(mockEnvironment.getProperty("seata.transport.threadFactory.bossThreadSize"))
                .thenReturn("8");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        int value = config.getInt("transport.threadFactory.bossThreadSize", 1, 1000);

        Assertions.assertEquals(8, value);
    }

    @Test
    void testGetBoolean() {
        Mockito.when(mockEnvironment.getProperty("seata.service.disableGlobalTransaction"))
                .thenReturn("true");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        boolean value = config.getBoolean("service.disableGlobalTransaction", false, 1000);

        Assertions.assertTrue(value);
    }

    @Test
    void testGetLong() {
        Mockito.when(mockEnvironment.getProperty("seata.client.rm.lock.retryInterval"))
                .thenReturn("10");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        long value = config.getLong("client.rm.lock.retryInterval", 5L, 1000);

        Assertions.assertEquals(10L, value);
    }

    @Test
    void testGetShort() {
        Mockito.when(mockEnvironment.getProperty("seata.test.short.key")).thenReturn("100");

        SpringCloudConfiguration config = SpringCloudConfiguration.getInstance();
        short value = config.getShort("test.short.key", (short) 50, 1000);

        Assertions.assertEquals((short) 100, value);
    }
}
