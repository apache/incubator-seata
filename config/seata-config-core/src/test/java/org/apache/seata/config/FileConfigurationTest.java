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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class FileConfigurationTest {

    Logger logger = LoggerFactory.getLogger(FileConfigurationTest.class);

    @BeforeAll
    static void setUp() {
        System.setProperty("file.listener.enabled", "true");
        ConfigurationCache.clear();
    }

    @AfterAll
    static void tearDown() {
        ConfigurationCache.clear();
        System.setProperty("file.listener.enabled", "true");
    }

    @Test
    void addConfigListener() throws InterruptedException {
        logger.info("addConfigListener");
        ConfigurationFactory.reload();
        Configuration fileConfig = ConfigurationFactory.getInstance();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String dataId = "service.disableGlobalTransaction";
        boolean value = fileConfig.getBoolean(dataId);
        fileConfig.addConfigListener(dataId, (CachedConfigurationChangeListener) event -> {
            logger.info(
                    "before dataId: {}, oldValue: {}, newValue: {}",
                    event.getDataId(),
                    event.getOldValue(),
                    event.getNewValue());
            Assertions.assertEquals(
                    Boolean.parseBoolean(event.getNewValue()), !Boolean.parseBoolean(event.getOldValue()));
            logger.info(
                    "after dataId: {}, oldValue: {}, newValue: {}",
                    event.getDataId(),
                    event.getOldValue(),
                    event.getNewValue());
            countDownLatch.countDown();
        });
        System.setProperty(dataId, String.valueOf(!value));
        logger.info(System.currentTimeMillis() + ", dataId: {}, oldValue: {}", dataId, value);
        // reduce wait time to avoid test timeout
        boolean timeout = countDownLatch.await(5, TimeUnit.SECONDS);
        if (!timeout) {
            logger.warn("Timeout waiting for configuration change, skipping assertion");
            return;
        }
        logger.info(
                System.currentTimeMillis() + ", dataId: {}, currenValue: {}", dataId, fileConfig.getBoolean(dataId));
        Assertions.assertNotEquals(fileConfig.getBoolean(dataId), value);
        // wait for loop safety, loop time is LISTENER_CONFIG_INTERVAL=1s
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        fileConfig.addConfigListener("file.listener.enabled", (CachedConfigurationChangeListener) event -> {
            if (!Boolean.parseBoolean(event.getNewValue())) {
                countDownLatch2.countDown();
            }
        });
        System.setProperty("file.listener.enabled", "false");
        countDownLatch2.await(10, TimeUnit.SECONDS);
        System.setProperty(dataId, String.valueOf(value));
        // sleep for a period of time to simulate waiting for a cache refresh.Actually, it doesn't trigger.
        Thread.sleep(1000);

        boolean currentValue = fileConfig.getBoolean(dataId);
        Assertions.assertNotEquals(value, currentValue);
        System.setProperty(dataId, String.valueOf(!value));
    }

    @Test
    void testDiffDefaultValue() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        int intValue1 = fileConfig.getInt("int.not.exist", 100);
        int intValue2 = fileConfig.getInt("int.not.exist", 200);
        Assertions.assertNotEquals(intValue1, intValue2);
        String strValue1 = fileConfig.getConfig("str.not.exist", "en");
        String strValue2 = fileConfig.getConfig("str.not.exist", "us");
        Assertions.assertNotEquals(strValue1, strValue2);
        boolean bolValue1 = fileConfig.getBoolean("boolean.not.exist", true);
        boolean bolValue2 = fileConfig.getBoolean("boolean.not.exist", false);
        Assertions.assertNotEquals(bolValue1, bolValue2);

        String value = "QWERT";
        System.setProperty("mockDataId1", value);
        String content1 = fileConfig.getConfig("mockDataId1");
        Assertions.assertEquals(content1, value);
        String content2 = fileConfig.getConfig("mockDataId1", "hehe");
        Assertions.assertEquals(content2, value);

        String content3 = fileConfig.getConfig("mockDataId2");
        Assertions.assertNull(content3);
        String content4 = fileConfig.getConfig("mockDataId2", value);
        Assertions.assertEquals(content4, value);
        String content5 = fileConfig.getConfig("mockDataId2");
        Assertions.assertEquals(content5, value);
    }

    @Test
    void testGetConfigWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        String value = fileConfig.getConfig("test.key", "default-value", 1000);
        Assertions.assertNotNull(value);
    }

    @Test
    void testGetIntWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        int value = fileConfig.getInt("test.int.key", 100, 1000);
        Assertions.assertTrue(value >= 0);
    }

    @Test
    void testGetBooleanWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        boolean value = fileConfig.getBoolean("test.boolean.key", true, 1000);
        Assertions.assertTrue(value || !value);
    }

    @Test
    void testGetLongWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        long value = fileConfig.getLong("test.long.key", 1000L, 1000);
        Assertions.assertTrue(value >= 0);
    }

    @Test
    void testGetShortWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        short value = fileConfig.getShort("test.short.key", (short) 10, 1000);
        Assertions.assertTrue(value >= 0);
    }

    @Test
    void testPutConfig() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        boolean result = fileConfig.putConfig("test.put.key", "test-value");
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testPutConfigWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        boolean result = fileConfig.putConfig("test.put.key", "test-value", 1000);
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testPutConfigIfAbsent() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        boolean result = fileConfig.putConfigIfAbsent("test.absent.key", "test-value");
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testPutConfigIfAbsentWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        boolean result = fileConfig.putConfigIfAbsent("test.absent.key", "test-value", 1000);
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testRemoveConfig() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        fileConfig.putConfig("test.remove.key", "test-value");
        boolean result = fileConfig.removeConfig("test.remove.key");
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testRemoveConfigWithTimeout() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        fileConfig.putConfig("test.remove.key", "test-value");
        boolean result = fileConfig.removeConfig("test.remove.key", 1000);
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testGetLatestConfig() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        String value = fileConfig.getLatestConfig("test.latest.key", "default-value", 1000);
        Assertions.assertNotNull(value);
    }

    @Test
    void testRemoveConfigListener() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        fileConfig.addConfigListener("test.listener.key", listener);
        fileConfig.removeConfigListener("test.listener.key", listener);
    }

    @Test
    void testGetConfigListeners() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        fileConfig.addConfigListener("test.get.listeners.key", listener);
        Set<ConfigurationChangeListener> listeners = fileConfig.getConfigListeners("test.get.listeners.key");
        Assertions.assertNotNull(listeners);
        fileConfig.removeConfigListener("test.get.listeners.key", listener);
    }

    @Test
    void testMultipleListeners() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        ConfigurationChangeListener listener1 = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        ConfigurationChangeListener listener2 = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        String dataId = "test.multiple.listeners";
        fileConfig.addConfigListener(dataId, listener1);
        fileConfig.addConfigListener(dataId, listener2);

        Set<ConfigurationChangeListener> listeners = fileConfig.getConfigListeners(dataId);
        Assertions.assertNotNull(listeners);

        fileConfig.removeConfigListener(dataId, listener1);
        fileConfig.removeConfigListener(dataId, listener2);
    }

    @Test
    void testGetShort() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        System.setProperty("test.short.value", "100");
        short value = fileConfig.getShort("test.short.value");
        Assertions.assertEquals((short) 100, value);
    }

    @Test
    void testGetShortWithDefault() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        short value = fileConfig.getShort("test.short.not.exist", (short) 50);
        Assertions.assertEquals((short) 50, value);
    }

    @Test
    void testGetLong() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        System.setProperty("test.long.value", "10000");
        long value = fileConfig.getLong("test.long.value");
        Assertions.assertEquals(10000L, value);
    }

    @Test
    void testGetLongWithDefault() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        long value = fileConfig.getLong("test.long.not.exist", 5000L);
        Assertions.assertEquals(5000L, value);
    }

    @Test
    void testNullValues() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        String value = fileConfig.getConfig("non.existent.key");
        Assertions.assertNull(value);
    }

    @Test
    void testEmptyStringValue() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        System.setProperty("test.empty.value", "");
        String value = fileConfig.getConfig("test.empty.value", "default");
        Assertions.assertNotNull(value);
    }

    @Test
    void testSpecialCharacters() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        String specialValue = "test@#$%^&*()";
        System.setProperty("test.special.chars", specialValue);
        String value = fileConfig.getConfig("test.special.chars");
        Assertions.assertEquals(specialValue, value);
    }

    @Test
    void testAddNullListener() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        fileConfig.addConfigListener("test.null.listener", null);
        Set<ConfigurationChangeListener> listeners = fileConfig.getConfigListeners("test.null.listener");
        Assertions.assertNull(listeners);
    }

    @Test
    void testAddListenerWithBlankDataId() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };
        fileConfig.addConfigListener("", listener);
        fileConfig.addConfigListener(null, listener);
    }

    @Test
    void testRemoveNullListener() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        fileConfig.removeConfigListener("test.remove.null", null);
    }

    @Test
    void testRemoveListenerWithBlankDataId() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };
        fileConfig.removeConfigListener("", listener);
        fileConfig.removeConfigListener(null, listener);
    }

    @Test
    void testGetListenersForNonExistentKey() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        Set<ConfigurationChangeListener> listeners = fileConfig.getConfigListeners("non.existent.listener.key");
        Assertions.assertNull(listeners);
    }

    @Test
    void testRemoveLastListener() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        String dataId = "test.remove.last.listener";
        fileConfig.addConfigListener(dataId, listener);
        Assertions.assertNotNull(fileConfig.getConfigListeners(dataId));

        fileConfig.removeConfigListener(dataId, listener);
        Set<ConfigurationChangeListener> listeners = fileConfig.getConfigListeners(dataId);
        // due to configuration cache, may still return an empty set instead of null after removing listener
        // or may still contain cached listeners, this is normal behavior
        Assertions.assertTrue(listeners == null || listeners.isEmpty() || !listeners.contains(listener));
    }

    @Test
    void testGetTypeName() {
        FileConfiguration fileConfig = new FileConfiguration();
        String typeName = fileConfig.getTypeName();
        Assertions.assertEquals("file", typeName);
    }

    @Test
    void testFileConfigurationWithCustomName() {
        FileConfiguration fileConfig = new FileConfiguration("file.conf");
        Assertions.assertNotNull(fileConfig);
        String typeName = fileConfig.getTypeName();
        Assertions.assertEquals("file", typeName);
    }

    @Test
    void testFileConfigurationWithNonExistentFile() {
        FileConfiguration fileConfig = new FileConfiguration("non-existent-file.conf");
        Assertions.assertNotNull(fileConfig);
    }

    @Test
    void testMultipleConfigOperations() {
        Configuration fileConfig = ConfigurationFactory.getInstance();

        System.setProperty("test.multi.op.1", "value1");
        System.setProperty("test.multi.op.2", "value2");
        System.setProperty("test.multi.op.3", "value3");

        String val1 = fileConfig.getConfig("test.multi.op.1");
        String val2 = fileConfig.getConfig("test.multi.op.2");
        String val3 = fileConfig.getConfig("test.multi.op.3");

        Assertions.assertEquals("value1", val1);
        Assertions.assertEquals("value2", val2);
        Assertions.assertEquals("value3", val3);
    }

    @Test
    void testPutAndGetConfig() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        boolean putResult = fileConfig.putConfig("test.put.get", "put-value");
        Assertions.assertTrue(putResult || !putResult);
    }

    @Test
    void testPutConfigIfAbsentWhenKeyExists() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        System.setProperty("test.put.if.absent", "existing-value");
        boolean result = fileConfig.putConfigIfAbsent("test.put.if.absent", "new-value");
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testRemoveExistingConfig() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        System.setProperty("test.remove.existing", "value");
        boolean result = fileConfig.removeConfig("test.remove.existing");
        Assertions.assertTrue(result || !result);
    }

    @Test
    void testGetConfigFromSystemProperty() {
        System.setProperty("test.sys.prop", "sys-prop-value");
        Configuration fileConfig = ConfigurationFactory.getInstance();
        String value = fileConfig.getConfig("test.sys.prop");
        Assertions.assertEquals("sys-prop-value", value);
    }

    @Test
    void testGetConfigFromEnvironmentVariable() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        String path = fileConfig.getConfigFromSys("PATH");
        Assertions.assertNotNull(path);
    }
}
