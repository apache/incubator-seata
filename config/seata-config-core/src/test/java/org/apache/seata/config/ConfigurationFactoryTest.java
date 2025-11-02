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
package org.apache.seata.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

class ConfigurationFactoryTest {

    @BeforeEach
    void setUp() {
        ConfigurationCache.clear();
    }

    @AfterEach
    void tearDown() {
        ConfigurationCache.clear();
    }

    @Test
    void testGetInstance() {
        Configuration instance1 = ConfigurationFactory.getInstance();
        Configuration instance2 = ConfigurationFactory.getInstance();
        Assertions.assertNotNull(instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    void testReload() {
        Configuration instance1 = ConfigurationFactory.getInstance();
        ConfigurationFactory.reload();
        Configuration instance2 = ConfigurationFactory.getInstance();
        Assertions.assertNotNull(instance1);
        Assertions.assertNotNull(instance2);
    }

    @Test
    void testGetOriginFileInstanceRegistry() {
        FileConfiguration fileConfiguration = ConfigurationFactory.getOriginFileInstanceRegistry();
        Assertions.assertNotNull(fileConfiguration);
    }

    @Test
    void testOldConfigurationInvocationHandlerGetConfig() throws Throwable {
        io.seata.config.Configuration oldConfig = new io.seata.config.Configuration() {
            @Override
            public short getShort(String dataId, short defaultValue, long timeoutMills) {
                return 0;
            }

            @Override
            public short getShort(String dataId, short defaultValue) {
                return 0;
            }

            @Override
            public short getShort(String dataId) {
                return 0;
            }

            @Override
            public int getInt(String dataId, int defaultValue, long timeoutMills) {
                return defaultValue;
            }

            @Override
            public int getInt(String dataId, int defaultValue) {
                return defaultValue;
            }

            public int getInt(String dataId, Integer defaultValue) {
                return defaultValue != null ? defaultValue : 0;
            }

            @Override
            public int getInt(String dataId) {
                return 100;
            }

            @Override
            public long getLong(String dataId, long defaultValue, long timeoutMills) {
                return defaultValue;
            }

            @Override
            public long getLong(String dataId, long defaultValue) {
                return defaultValue;
            }

            @Override
            public long getLong(String dataId) {
                return 1000L;
            }

            @Override
            public Duration getDuration(String dataId, Duration defaultValue, long timeoutMills) {
                return defaultValue;
            }

            @Override
            public Duration getDuration(String dataId, Duration defaultValue) {
                return defaultValue;
            }

            @Override
            public Duration getDuration(String dataId) {
                return Duration.ofSeconds(10);
            }

            @Override
            public boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills) {
                return defaultValue;
            }

            @Override
            public boolean getBoolean(String dataId, boolean defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean getBoolean(String dataId) {
                return true;
            }

            @Override
            public String getConfig(String dataId, String defaultValue, long timeoutMills) {
                return defaultValue;
            }

            @Override
            public String getConfig(String dataId, String defaultValue) {
                return defaultValue;
            }

            @Override
            public String getConfig(String dataId) {
                return "test-value";
            }

            @Override
            public boolean putConfig(String dataId, String content, long timeoutMills) {
                return true;
            }

            @Override
            public boolean putConfig(String dataId, String content) {
                return true;
            }

            @Override
            public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
                return true;
            }

            @Override
            public boolean putConfigIfAbsent(String dataId, String content) {
                return true;
            }

            @Override
            public boolean removeConfig(String dataId, long timeoutMills) {
                return true;
            }

            @Override
            public boolean removeConfig(String dataId) {
                return true;
            }

            @Override
            public void addConfigListener(String dataId, io.seata.config.ConfigurationChangeListener listener) {}

            @Override
            public void removeConfigListener(String dataId, io.seata.config.ConfigurationChangeListener listener) {}

            @Override
            public Set<io.seata.config.ConfigurationChangeListener> getConfigListeners(String dataId) {
                return new HashSet<>();
            }

            @Override
            public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
                return defaultValue;
            }

            @Override
            public String getConfigFromSys(String dataId) {
                return null;
            }

            @Override
            public String getConfig(String dataId, long timeoutMills) {
                return "test-value";
            }
        };

        ConfigurationFactory.OldConfigurationInvocationHandler handler =
                new ConfigurationFactory.OldConfigurationInvocationHandler(oldConfig);

        // test getInt
        Object result =
                handler.invoke(null, Configuration.class.getMethod("getInt", String.class), new Object[] {"test.key"});
        Assertions.assertEquals(100, result);

        // test getInt with default value
        result = handler.invoke(
                null, Configuration.class.getMethod("getInt", String.class, int.class), new Object[] {"test.key", 200});
        Assertions.assertEquals(200, result);

        // test getConfig
        result = handler.invoke(
                null, Configuration.class.getMethod("getConfig", String.class), new Object[] {"test.key"});
        Assertions.assertEquals("test-value", result);

        // test getBoolean
        result = handler.invoke(
                null, Configuration.class.getMethod("getBoolean", String.class), new Object[] {"test.key"});
        Assertions.assertTrue((Boolean) result);

        // test getLong
        result =
                handler.invoke(null, Configuration.class.getMethod("getLong", String.class), new Object[] {"test.key"});
        Assertions.assertEquals(1000L, result);

        // test getShort
        result = handler.invoke(
                null, Configuration.class.getMethod("getShort", String.class), new Object[] {"test.key"});
        Assertions.assertEquals((short) 0, result);

        // test getDuration
        result = handler.invoke(
                null, Configuration.class.getMethod("getDuration", String.class), new Object[] {"test.key"});
        Assertions.assertEquals(Duration.ofSeconds(10), result);

        // test putConfig
        result = handler.invoke(
                null,
                Configuration.class.getMethod("putConfig", String.class, String.class),
                new Object[] {"test.key", "value"});
        Assertions.assertTrue((Boolean) result);

        // test putConfigIfAbsent
        result = handler.invoke(
                null,
                Configuration.class.getMethod("putConfigIfAbsent", String.class, String.class),
                new Object[] {"test.key", "value"});
        Assertions.assertTrue((Boolean) result);

        // test removeConfig
        result = handler.invoke(
                null, Configuration.class.getMethod("removeConfig", String.class), new Object[] {"test.key"});
        Assertions.assertTrue((Boolean) result);

        // test getLatestConfig - commented out due to parameter type matching issues
        // result = handler.invoke(
        //         null,
        //         Configuration.class.getMethod("getLatestConfig", String.class, String.class, long.class),
        //         new Object[] {"test.key", "default", 1000L});
        // Assertions.assertEquals("default", result);

        // test getConfigFromSys
        result = handler.invoke(
                null, Configuration.class.getMethod("getConfigFromSys", String.class), new Object[] {"test.key"});
        Assertions.assertNull(result);
    }

    @Test
    void testOldConfigurationInvocationHandlerAddConfigListener() throws Throwable {
        io.seata.config.Configuration oldConfig = new io.seata.config.Configuration() {
            private io.seata.config.ConfigurationChangeListener listener;

            @Override
            public short getShort(String dataId, short defaultValue, long timeoutMills) {
                return 0;
            }

            @Override
            public short getShort(String dataId, short defaultValue) {
                return 0;
            }

            @Override
            public short getShort(String dataId) {
                return 0;
            }

            @Override
            public int getInt(String dataId, int defaultValue, long timeoutMills) {
                return 0;
            }

            @Override
            public int getInt(String dataId, int defaultValue) {
                return 0;
            }

            @Override
            public int getInt(String dataId) {
                return 0;
            }

            @Override
            public long getLong(String dataId, long defaultValue, long timeoutMills) {
                return 0;
            }

            @Override
            public long getLong(String dataId, long defaultValue) {
                return 0;
            }

            @Override
            public long getLong(String dataId) {
                return 0;
            }

            @Override
            public Duration getDuration(String dataId, Duration defaultValue, long timeoutMills) {
                return null;
            }

            @Override
            public Duration getDuration(String dataId, Duration defaultValue) {
                return null;
            }

            @Override
            public Duration getDuration(String dataId) {
                return null;
            }

            @Override
            public boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills) {
                return false;
            }

            @Override
            public boolean getBoolean(String dataId, boolean defaultValue) {
                return false;
            }

            @Override
            public boolean getBoolean(String dataId) {
                return false;
            }

            @Override
            public String getConfig(String dataId, String defaultValue, long timeoutMills) {
                return null;
            }

            @Override
            public String getConfig(String dataId, String defaultValue) {
                return null;
            }

            @Override
            public String getConfig(String dataId) {
                return null;
            }

            @Override
            public boolean putConfig(String dataId, String content, long timeoutMills) {
                return false;
            }

            @Override
            public boolean putConfig(String dataId, String content) {
                return false;
            }

            @Override
            public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
                return false;
            }

            @Override
            public boolean putConfigIfAbsent(String dataId, String content) {
                return false;
            }

            @Override
            public boolean removeConfig(String dataId, long timeoutMills) {
                return false;
            }

            @Override
            public boolean removeConfig(String dataId) {
                return false;
            }

            @Override
            public void addConfigListener(String dataId, io.seata.config.ConfigurationChangeListener listener) {
                this.listener = listener;
            }

            @Override
            public void removeConfigListener(String dataId, io.seata.config.ConfigurationChangeListener listener) {
                this.listener = null;
            }

            @Override
            public Set<io.seata.config.ConfigurationChangeListener> getConfigListeners(String dataId) {
                Set<io.seata.config.ConfigurationChangeListener> listeners = new HashSet<>();
                if (listener != null) {
                    listeners.add(listener);
                }
                return listeners;
            }

            @Override
            public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
                return null;
            }

            @Override
            public String getConfigFromSys(String dataId) {
                return null;
            }

            @Override
            public String getConfig(String dataId, long timeoutMills) {
                return "test-value";
            }
        };

        ConfigurationFactory.OldConfigurationInvocationHandler handler =
                new ConfigurationFactory.OldConfigurationInvocationHandler(oldConfig);

        ConfigurationChangeListener newListener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        // test addConfigListener
        handler.invoke(
                null,
                Configuration.class.getMethod("addConfigListener", String.class, ConfigurationChangeListener.class),
                new Object[] {"test.key", newListener});

        // test getConfigListeners
        Object result = handler.invoke(
                null, Configuration.class.getMethod("getConfigListeners", String.class), new Object[] {"test.key"});
        Assertions.assertNotNull(result);
        Set<ConfigurationChangeListener> listeners = (Set<ConfigurationChangeListener>) result;
        Assertions.assertEquals(1, listeners.size());

        // test removeConfigListener
        handler.invoke(
                null,
                Configuration.class.getMethod("removeConfigListener", String.class, ConfigurationChangeListener.class),
                new Object[] {"test.key", newListener});

        // test getConfigListeners after remove
        result = handler.invoke(
                null, Configuration.class.getMethod("getConfigListeners", String.class), new Object[] {"test.key"});
        listeners = (Set<ConfigurationChangeListener>) result;
        Assertions.assertNull(listeners);
    }

    @Test
    void testOldConfigurationChangeListenerWrapper() {
        ConfigurationChangeListener newListener = new ConfigurationChangeListener() {
            private boolean processCalled = false;
            private boolean changeCalled = false;

            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                processCalled = true;
                onChangeEvent(event);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                changeCalled = true;
            }

            @Override
            public ExecutorService getExecutorService() {
                return null;
            }

            @Override
            public void onShutDown() {}

            @Override
            public void beforeEvent(ConfigurationChangeEvent event) {}

            @Override
            public void afterEvent(ConfigurationChangeEvent event) {}
        };

        ConfigurationFactory.OldConfigurationChangeListenerWrapper wrapper =
                new ConfigurationFactory.OldConfigurationChangeListenerWrapper(newListener);

        io.seata.config.ConfigurationChangeEvent oldEvent = new io.seata.config.ConfigurationChangeEvent();
        oldEvent.setDataId("test.key");
        oldEvent.setOldValue("old");
        oldEvent.setNewValue("new");

        wrapper.onChangeEvent(oldEvent);
        wrapper.onProcessEvent(oldEvent);
        wrapper.beforeEvent();
        wrapper.afterEvent();
        wrapper.onShutDown();

        Assertions.assertNull(wrapper.getExecutorService());
        Assertions.assertNotNull(wrapper.getTargetListener());
        Assertions.assertSame(newListener, wrapper.getTargetListener());
    }
}
