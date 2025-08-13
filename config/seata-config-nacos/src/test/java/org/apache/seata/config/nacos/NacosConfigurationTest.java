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

import com.alibaba.nacos.api.exception.NacosException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.Dispose;
import org.apache.seata.config.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * The type Nacos configuration test
 */
public class NacosConfigurationTest {

    private static Configuration configuration;

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
}
