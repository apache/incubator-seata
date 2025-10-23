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
package org.apache.seata.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.Dispose;
import org.apache.seata.config.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * The type Nacos configuration test
 */
public class NacosConfigurationTest {

    private static Configuration configuration;
    private ConfigService mockConfigService;

    @BeforeAll
    public static void setup() throws NacosException {
        System.clearProperty("seataEnv");
        configuration = NacosConfiguration.getInstance();
        if (configuration instanceof Dispose) {
            ((Dispose) configuration).dispose();
        }
        ConfigurationFactory.reload();
        configuration = NacosConfiguration.getInstance();
    }

    @BeforeEach
    void setUpMocks() throws Exception {
        mockConfigService = Mockito.mock(ConfigService.class);

        Field configServiceField = ReflectionUtil.getField(NacosConfiguration.class, "configService");
        configServiceField.setAccessible(true);
        configServiceField.set(null, mockConfigService);

        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, new Properties());
    }

    @AfterEach
    void tearDownMocks() throws Exception {
        Field instanceField = ReflectionUtil.getField(NacosConfiguration.class, "instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field configServiceField = ReflectionUtil.getField(NacosConfiguration.class, "configService");
        configServiceField.setAccessible(true);
        configServiceField.set(null, null);

        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, new Properties());
    }

    @Test
    public void testGetConfigProperties() throws Exception {
        Assertions.assertNotNull(configuration);
        Method method = ReflectionUtil.getMethod(NacosConfiguration.class, "getConfigProperties");
        // do not use `ConfigurationFactory.getInstance()`, it's a proxy object
        Properties properties = (Properties) method.invoke(configuration);
        Assertions.assertEquals("/bar", properties.getProperty("contextPath"));
        System.setProperty("contextPath", "/foo");
        properties = (Properties) method.invoke(configuration);
        Assertions.assertEquals("/foo", properties.getProperty("contextPath"));
        System.clearProperty("contextPath");
    }

    @Test
    public void testInnerReceiveEmptyPushShouldNotUpdateConfig() throws Exception {

        String dataId = "seata.properties";
        String group = "SEATA_GROUP";
        String configKey = "session.mode";

        Properties oldConfig = new Properties();
        oldConfig.setProperty(configKey, "db");

        Field seataConfigField = NacosConfiguration.class.getDeclaredField("seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, oldConfig);

        TestListener listener = new TestListener();
        NacosConfiguration.NacosListener nacosListener = getNacosListener(dataId, listener);

        ConcurrentMap<ConfigurationChangeListener, NacosConfiguration.NacosListener> innerMap =
                new ConcurrentHashMap<>();
        innerMap.put(listener, nacosListener);

        ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, NacosConfiguration.NacosListener>> outerMap =
                new ConcurrentHashMap<>();
        outerMap.put(dataId, innerMap);

        Field listenerMapField = NacosConfiguration.class.getDeclaredField("CONFIG_LISTENERS_MAP");
        listenerMapField.setAccessible(true);
        listenerMapField.set(null, outerMap);

        // execute
        nacosListener.innerReceive(dataId, group, "");

        Properties actualConfig = (Properties) seataConfigField.get(null);
        Assertions.assertEquals("db", actualConfig.getProperty(configKey));

        Assertions.assertFalse(listener.invoked);
    }

    @Test
    public void testInnerReceiveShouldReturn() throws Exception {

        String dataId = "seata.properties";
        String group = "SEATA_GROUP";
        String configKey = "session.mode";

        Properties oldConfig = new Properties();
        oldConfig.setProperty(configKey, "db");

        Field seataConfigField = NacosConfiguration.class.getDeclaredField("seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, oldConfig);

        TestListener listener = new TestListener();
        NacosConfiguration.NacosListener nacosListener = getNacosListener(dataId, listener);

        ConcurrentMap<ConfigurationChangeListener, NacosConfiguration.NacosListener> innerMap =
                new ConcurrentHashMap<>();
        innerMap.put(listener, nacosListener);

        ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, NacosConfiguration.NacosListener>> outerMap =
                new ConcurrentHashMap<>();
        outerMap.put(dataId, innerMap);

        Field listenerMapField = NacosConfiguration.class.getDeclaredField("CONFIG_LISTENERS_MAP");
        listenerMapField.setAccessible(true);
        listenerMapField.set(null, outerMap);

        // execute
        nacosListener.innerReceive(dataId, group, "session.mode=redis");

        Properties actualConfig = (Properties) seataConfigField.get(null);
        Assertions.assertEquals("redis", actualConfig.getProperty(configKey));

        Assertions.assertFalse(listener.invoked);
    }

    @Test
    public void testInnerReceiveThrowException() throws Exception {

        String dataId = "seata.properties";
        String group = "SEATA_GROUP";
        String configKey = "session.mode";

        Properties oldConfig = new Properties();
        oldConfig.setProperty(configKey, "db");

        Field seataConfigField = NacosConfiguration.class.getDeclaredField("seataConfig");
        seataConfigField.setAccessible(true);
        seataConfigField.set(null, oldConfig);

        TestListener listener = new TestListener();
        NacosConfiguration.NacosListener nacosListener = getNacosListener(dataId, listener);

        ConcurrentMap<ConfigurationChangeListener, NacosConfiguration.NacosListener> innerMap =
                new ConcurrentHashMap<>();
        innerMap.put(listener, nacosListener);

        ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, NacosConfiguration.NacosListener>> outerMap =
                new ConcurrentHashMap<>();
        outerMap.put(dataId, innerMap);

        Field listenerMapField = NacosConfiguration.class.getDeclaredField("CONFIG_LISTENERS_MAP");
        listenerMapField.setAccessible(true);
        listenerMapField.set(null, outerMap);

        try (MockedStatic<ConfigProcessor> processorMockedStatic = Mockito.mockStatic(ConfigProcessor.class)) {
            processorMockedStatic
                    .when(() -> ConfigProcessor.resolverConfigDataType(anyString()))
                    .thenReturn("yaml");
            processorMockedStatic
                    .when(() -> ConfigProcessor.processConfig(anyString(), anyString()))
                    .thenThrow(new IOException("mock io exception"));
            // execute
            nacosListener.innerReceive(dataId, group, "session.mode=redis");
        }

        Properties actualConfig = (Properties) seataConfigField.get(null);
        Assertions.assertEquals("db", actualConfig.getProperty(configKey));

        Assertions.assertFalse(listener.invoked);
    }

    @NotNull
    private static NacosConfiguration.NacosListener getNacosListener(String dataId, TestListener listener)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {
        Class<?> outerClass = Class.forName("org.apache.seata.config.nacos.NacosConfiguration");
        Constructor<?> constructor = outerClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object nacosConfigurationInstance = constructor.newInstance();
        Class<?> innerClass = Class.forName("org.apache.seata.config.nacos.NacosConfiguration$NacosListener");

        Constructor<?> innerConstructor =
                innerClass.getDeclaredConstructor(outerClass, String.class, ConfigurationChangeListener.class);
        innerConstructor.setAccessible(true);
        NacosConfiguration.NacosListener nacosListener = (NacosConfiguration.NacosListener)
                innerConstructor.newInstance(nacosConfigurationInstance, dataId, listener);
        return nacosListener;
    }

    private static class TestListener implements ConfigurationChangeListener {
        boolean invoked = false;

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            invoked = true;
        }
    }

    // Enhanced tests from NacosConfigurationEnhancedTest

    @Test
    void testGetInstance() {
        NacosConfiguration instance1 = NacosConfiguration.getInstance();
        NacosConfiguration instance2 = NacosConfiguration.getInstance();

        Assertions.assertNotNull(instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    void testGetTypeName() {
        NacosConfiguration config = NacosConfiguration.getInstance();
        Assertions.assertEquals("nacos", config.getTypeName());
    }

    @Test
    void testGetLatestConfigFromSeataConfig() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.key", "test-value");
        seataConfigField.set(null, props);

        NacosConfiguration config = NacosConfiguration.getInstance();
        String value = config.getLatestConfig("test.key", "default", 1000);

        Assertions.assertEquals("test-value", value);
    }

    @Test
    void testGetLatestConfigFromNacos() throws Exception {
        Mockito.when(mockConfigService.getConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
                .thenReturn("nacos-value");

        NacosConfiguration config = NacosConfiguration.getInstance();
        String value = config.getLatestConfig("test.nacos.key", "default", 1000);

        Assertions.assertEquals("nacos-value", value);
    }

    @Test
    void testGetLatestConfigWithDefaultValue() throws Exception {
        Mockito.when(mockConfigService.getConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(null);

        NacosConfiguration config = NacosConfiguration.getInstance();
        String value = config.getLatestConfig("non.existent.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testGetLatestConfigWithNacosException() throws Exception {
        Mockito.when(mockConfigService.getConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
                .thenThrow(new NacosException(500, "Server error"));

        NacosConfiguration config = NacosConfiguration.getInstance();
        String value = config.getLatestConfig("error.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testPutConfigWhenSeataConfigEmpty() throws Exception {
        Mockito.when(mockConfigService.publishConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean result = config.putConfig("test.key", "test-value", 1000);

        Assertions.assertTrue(result);
        Mockito.verify(mockConfigService)
                .publishConfig(Mockito.eq("test.key"), Mockito.anyString(), Mockito.eq("test-value"));
    }

    @Test
    void testPutConfigWhenSeataConfigNotEmpty() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("existing.key", "existing-value");
        seataConfigField.set(null, props);

        Mockito.when(mockConfigService.publishConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean result = config.putConfig("new.key", "new-value", 1000);

        Assertions.assertTrue(result);
    }

    @Test
    void testPutConfigWithNacosException() throws Exception {
        Mockito.when(mockConfigService.publishConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new NacosException(500, "Server error"));

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean result = config.putConfig("test.key", "test-value", 1000);

        Assertions.assertFalse(result);
    }

    @Test
    void testPutConfigIfAbsent() {
        NacosConfiguration config = NacosConfiguration.getInstance();

        Assertions.assertThrows(
                NotSupportYetException.class, () -> config.putConfigIfAbsent("test.key", "test-value", 1000));
    }

    @Test
    void testRemoveConfigWhenSeataConfigEmpty() throws Exception {
        Mockito.when(mockConfigService.removeConfig(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean result = config.removeConfig("test.key", 1000);

        Assertions.assertTrue(result);
        Mockito.verify(mockConfigService).removeConfig(Mockito.eq("test.key"), Mockito.anyString());
    }

    @Test
    void testRemoveConfigWhenSeataConfigNotEmpty() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.key", "test-value");
        seataConfigField.set(null, props);

        Mockito.when(mockConfigService.publishConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean result = config.removeConfig("test.key", 1000);

        Assertions.assertTrue(result);
    }

    @Test
    void testRemoveConfigWithNacosException() throws Exception {
        Mockito.when(mockConfigService.removeConfig(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new NacosException(500, "Server error"));

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean result = config.removeConfig("test.key", 1000);

        Assertions.assertFalse(result);
    }

    @Test
    void testAddConfigListener() throws Exception {
        ConfigurationChangeListener listener = event -> {};

        NacosConfiguration config = NacosConfiguration.getInstance();
        config.addConfigListener("test.listener.key", listener);

        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("test.listener.key");
        Assertions.assertNotNull(listeners);
        Assertions.assertEquals(1, listeners.size());

        Mockito.verify(mockConfigService).addListener(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    void testAddConfigListenerWithBlankDataId() {
        ConfigurationChangeListener listener = event -> {};

        NacosConfiguration config = NacosConfiguration.getInstance();
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
        NacosConfiguration config = NacosConfiguration.getInstance();
        config.addConfigListener("test.key", null);

        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("test.key");
        Assertions.assertNull(listeners);
    }

    @Test
    void testRemoveConfigListener() throws Exception {
        ConfigurationChangeListener listener = event -> {};

        NacosConfiguration config = NacosConfiguration.getInstance();
        config.addConfigListener("test.remove.key", listener);

        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("test.remove.key");
        Assertions.assertNotNull(listeners);

        config.removeConfigListener("test.remove.key", listener);

        Mockito.verify(mockConfigService).removeListener(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    void testRemoveConfigListenerWithBlankDataId() {
        ConfigurationChangeListener listener = event -> {};

        NacosConfiguration config = NacosConfiguration.getInstance();
        config.removeConfigListener("", listener);
        config.removeConfigListener(null, listener);
    }

    @Test
    void testRemoveConfigListenerWithNullListener() {
        NacosConfiguration config = NacosConfiguration.getInstance();
        config.removeConfigListener("test.key", null);
    }

    @Test
    void testGetConfigListeners() throws Exception {
        ConfigurationChangeListener listener1 = event -> {};
        ConfigurationChangeListener listener2 = event -> {};

        NacosConfiguration config = NacosConfiguration.getInstance();
        config.addConfigListener("test.multi.listeners", listener1);
        config.addConfigListener("test.multi.listeners", listener2);

        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("test.multi.listeners");
        Assertions.assertNotNull(listeners);
        Assertions.assertEquals(2, listeners.size());
    }

    @Test
    void testGetConfigListenersForNonExistentKey() {
        NacosConfiguration config = NacosConfiguration.getInstance();
        Set<ConfigurationChangeListener> listeners = config.getConfigListeners("non.existent.key");
        Assertions.assertNull(listeners);
    }

    @Test
    void testGetConfig() throws Exception {
        Mockito.when(mockConfigService.getConfig(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
                .thenReturn("config-value");

        NacosConfiguration config = NacosConfiguration.getInstance();
        String value = config.getConfig("test.key", "default-value", 1000);

        Assertions.assertEquals("config-value", value);
    }

    @Test
    void testGetInt() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.int.key", "100");
        seataConfigField.set(null, props);

        NacosConfiguration config = NacosConfiguration.getInstance();
        int value = config.getInt("test.int.key", 50, 1000);
        Assertions.assertEquals(100, value);
    }

    @Test
    void testGetBoolean() throws Exception {
        Field seataConfigField = ReflectionUtil.getField(NacosConfiguration.class, "seataConfig");
        seataConfigField.setAccessible(true);
        Properties props = new Properties();
        props.setProperty("test.boolean.key", "true");
        seataConfigField.set(null, props);

        NacosConfiguration config = NacosConfiguration.getInstance();
        boolean value = config.getBoolean("test.boolean.key", false, 1000);
        Assertions.assertTrue(value);
    }
}
