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
package org.apache.seata.config.zk;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.test.TestingServer;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationChangeType;
import org.apache.seata.config.processor.ConfigProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The type zk configuration test
 */
public class ZkConfigurationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkConfigurationTest.class);

    protected static TestingServer server = null;

    @BeforeAll
    public static void adBeforeClass() throws Exception {
        System.setProperty("config.type", "zk");
        System.setProperty("config.zk.serverAddr", "127.0.0.1:2181");
        server = new TestingServer(2181);
        server.start();
    }

    @AfterAll
    public static void adAfterClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testCheckExist() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        boolean exist = zookeeperConfiguration.checkExists("/");
        Assertions.assertTrue(exist);
    }

    @Test
    public void testPutConfig() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final boolean[] listened = {false};
        String dataId = "putMockDataId";
        ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                LOGGER.info("onChangeEvent:{}", event);
                if (event.getChangeType() == ConfigurationChangeType.MODIFY) {
                    Assertions.assertEquals("value2", event.getNewValue());
                    listened[0] = true;
                    countDownLatch.countDown();
                }
            }
        };
        zookeeperConfiguration.createPersistent(zookeeperConfiguration.buildPath(dataId), "value");
        zookeeperConfiguration.addConfigListener(dataId, changeListener);
        zookeeperConfiguration.putConfig(dataId, "value2");
        try {
            countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertTrue(listened[0]);

        zookeeperConfiguration.removeConfig(dataId);

        zookeeperConfiguration.removeConfigListener(dataId, changeListener);
    }

    @Test
    public void testRemoveConfig() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final boolean[] listened = {false};
        String dataId = "removeMockDataId";
        zookeeperConfiguration.createPersistent(zookeeperConfiguration.buildPath(dataId), "value");
        ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                LOGGER.info("onChangeEvent:{}", event);
                if (event.getChangeType() == ConfigurationChangeType.DELETE) {
                    Assertions.assertNull(event.getNewValue());
                    listened[0] = true;
                    countDownLatch.countDown();
                }
            }
        };

        zookeeperConfiguration.addConfigListener(dataId, changeListener);
        zookeeperConfiguration.putConfig(dataId, "value2");
        boolean remove = zookeeperConfiguration.removeConfig(dataId);
        Assertions.assertTrue(remove);
        try {
            countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(listened[0]);
    }

    @Test
    public void testEvent_pathEqualsConfigPath_blankValue() throws Exception {
        Method getConfigPath = ZookeeperConfiguration.class.getDeclaredMethod("getConfigPath");
        getConfigPath.setAccessible(true);

        String configPath = getConfigPath.invoke(null).toString();

        ZookeeperConfiguration.NodeCacheListenerImpl listener =
                new ZookeeperConfiguration.NodeCacheListenerImpl(configPath, null);

        ChildData mockData = mock(ChildData.class);
        when(mockData.getData()).thenReturn(new byte[0]);

        listener.event(CuratorCacheListener.Type.NODE_CHANGED, null, mockData);

        // If it can run to this point, it indicates that the null value branch has been overwritten
    }

    @Test
    public void testEvent_pathEqualsConfigPath_throwException() throws Exception {
        Method getConfigPathMethod = ZookeeperConfiguration.class.getDeclaredMethod("getConfigPath");
        getConfigPathMethod.setAccessible(true);
        String configPath = getConfigPathMethod.invoke(null).toString();
        ZookeeperConfiguration.NodeCacheListenerImpl listener =
                new ZookeeperConfiguration.NodeCacheListenerImpl(configPath, null);
        String invalidYaml = "server:\n" + "  port: 8080\n" + "::host localhost";
        ChildData mockData = mock(ChildData.class);
        when(mockData.getData()).thenReturn(invalidYaml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<ConfigProcessor> processorMockedStatic = Mockito.mockStatic(ConfigProcessor.class)) {
            processorMockedStatic
                    .when(() -> ConfigProcessor.resolverConfigDataType(anyString()))
                    .thenReturn("yaml");
            processorMockedStatic
                    .when(() -> ConfigProcessor.processConfig(anyString(), anyString()))
                    .thenThrow(new IOException("mock io exception"));
            listener.event(CuratorCacheListener.Type.NODE_CHANGED, null, mockData);
        }
    }

    // Enhanced tests from ZookeeperConfigurationEnhancedTest

    @Test
    void testGetTypeName() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        Assertions.assertEquals("zk", config.getTypeName());
    }

    @Test
    void testGetLatestConfigWithDefaultValue() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String value = config.getLatestConfig("non.existent.key", "default-value", 1000);

        Assertions.assertEquals("default-value", value);
    }

    @Test
    void testGetLatestConfigFromZookeeper() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String dataId = "test.zk.key";
        config.putConfig(dataId, "zk-value", 1000);

        String value = config.getLatestConfig(dataId, "default", 1000);
        Assertions.assertEquals("zk-value", value);

        config.removeConfig(dataId, 1000);
    }

    @Test
    void testPutConfigIfAbsent() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();

        Assertions.assertThrows(
                NotSupportYetException.class, () -> config.putConfigIfAbsent("test.key", "test-value", 1000));
    }

    // Listener tests are already covered by testPutConfig and testRemoveConfig

    @Test
    void testGetConfig() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String dataId = "test.config.key";
        config.putConfig(dataId, "config-value", 1000);

        String value = config.getConfig(dataId, "default-value", 1000);
        Assertions.assertEquals("config-value", value);

        config.removeConfig(dataId, 1000);
    }

    @Test
    void testGetInt() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String dataId = "test.int.key";
        config.putConfig(dataId, "100", 1000);

        int value = config.getInt(dataId, 50, 1000);
        Assertions.assertEquals(100, value);

        config.removeConfig(dataId, 1000);
    }

    @Test
    void testGetBoolean() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String dataId = "test.boolean.key";
        config.putConfig(dataId, "true", 1000);

        boolean value = config.getBoolean(dataId, false, 1000);
        Assertions.assertTrue(value);

        config.removeConfig(dataId, 1000);
    }

    @Test
    void testCheckExistsPath() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        boolean exists = config.checkExists("/");
        Assertions.assertTrue(exists);
    }

    @Test
    void testCheckExistsForNonExistentPath() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        boolean exists = config.checkExists("/non/existent/path");
        Assertions.assertFalse(exists);
    }

    @Test
    void testCreatePersistent() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String path = "/test/persistent/node";

        if (!config.checkExists("/test")) {
            config.createPersistent("/test");
        }
        if (!config.checkExists("/test/persistent")) {
            config.createPersistent("/test/persistent");
        }
        config.createPersistent(path);

        boolean exists = config.checkExists(path);
        Assertions.assertTrue(exists);
    }

    @Test
    void testReadData() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String dataId = "test.read.key";
        String testValue = "read-value";

        config.putConfig(dataId, testValue, 1000);
        String path = config.buildPath(dataId);
        String value = config.readData(path);

        Assertions.assertEquals(testValue, value);

        config.removeConfig(dataId, 1000);
    }

    @Test
    void testReadDataFromNonExistentNode() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String value = config.readData("/non/existent/node");
        Assertions.assertNull(value);
    }

    @Test
    void testBuildPath() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        String path = config.buildPath("test.key");
        Assertions.assertTrue(path.startsWith("/seata"));
        Assertions.assertTrue(path.contains("test.key"));
    }
}
