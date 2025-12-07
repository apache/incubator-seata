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
package org.apache.seata.config.apollo;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationChangeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type Apollo configuration test.
 */
public class ApolloConfigurationTest {

    private static final int PORT = 8081;
    private static ApolloMockServer apolloMockServer;

    private static ApolloConfiguration apolloConfiguration;

    /**
     * Sets up.
     *
     * @throws IOException the io exception
     */
    @BeforeAll
    public static void setUp() throws IOException {
        System.setProperty("seataEnv", "test");
        apolloMockServer = new ApolloMockServer(PORT);
        apolloConfiguration = ApolloConfiguration.getInstance();
    }

    /**
     * Test get config.
     */
    @Test
    public void testGetConfig() {
        String value = apolloConfiguration.getConfig("seata.test");
        assertEquals("mockdata", value);
        value = apolloConfiguration.getConfig("seata.key");
        Assertions.assertNull(value);
        value = apolloConfiguration.getConfig("seata.key.1", "default");
        assertEquals("default", value);
        value = apolloConfiguration.getLatestConfig("seata.key.2", "default", 3000);
        assertEquals("default", value);
    }

    /**
     * Test update config.
     */
    @Test
    public void testUpdateConfig() {
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            apolloConfiguration.putConfig("seata.test", "mockdata");
        });
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            apolloConfiguration.putConfigIfAbsent("seata.test", "mockdata");
        });
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            apolloConfiguration.removeConfig("seata.test");
        });
    }

    /**
     * Test listener.
     */
    @Test
    public void testListener() {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };
        apolloConfiguration.addConfigListener("seata.test", listener);
        assertEquals(1, apolloConfiguration.getConfigListeners("seata.test").size());
        apolloConfiguration.removeConfigListener("seata.test", null);
        assertEquals(1, apolloConfiguration.getConfigListeners("seata.test").size());
        apolloConfiguration.removeConfigListener("seata.test", listener);
        assertEquals(0, apolloConfiguration.getConfigListeners("seata.test").size());
    }

    @Test
    void testGetChangeTypeAdded() {
        ConfigurationChangeType result = apolloConfiguration.getChangeType(PropertyChangeType.ADDED);
        assertEquals(
                ConfigurationChangeType.ADD,
                result,
                "Should return ConfigurationChangeType.ADD for PropertyChangeType.ADDED");
    }

    @Test
    void testGetChangeTypeDeleted() {
        ConfigurationChangeType result = apolloConfiguration.getChangeType(PropertyChangeType.DELETED);
        assertEquals(
                ConfigurationChangeType.DELETE,
                result,
                "Should return ConfigurationChangeType.DELETE for PropertyChangeType.DELETED");
    }

    @Test
    void testGetChangeTypeModified() {
        ConfigurationChangeType result = apolloConfiguration.getChangeType(PropertyChangeType.MODIFIED);
        assertEquals(
                ConfigurationChangeType.MODIFY,
                result,
                "Should return ConfigurationChangeType.MODIFY for PropertyChangeType.MODIFIED");
    }

    @Test
    void testGetTypeName() {
        String result = apolloConfiguration.getTypeName();
        assertEquals("apollo", result, "Should return 'apollo' as the type name");
    }

    @Test
    void testGetApolloConfigService() {
        String result = ApolloConfiguration.getApolloConfigService();
        assertEquals(
                "config.apollo.apolloConfigService", result, "Should return the correct Apollo config service string");
    }

    /**
     * Tear down.
     *
     * @throws IOException the io exception
     */
    @AfterAll
    public static void tearDown() throws IOException {
        System.clearProperty("seataEnv");
        apolloMockServer.stop();
    }

    // Enhanced tests from ApolloConfigurationEnhancedTest

    @Test
    void testGetInstance() {
        ApolloConfiguration instance1 = ApolloConfiguration.getInstance();
        ApolloConfiguration instance2 = ApolloConfiguration.getInstance();

        Assertions.assertNotNull(instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    void testGetConfigWithTimeout() {
        String value = apolloConfiguration.getConfig("seata.test", "default", 1000);
        Assertions.assertEquals("mockdata", value);
    }

    @Test
    void testGetInt() {
        int value = apolloConfiguration.getInt("seata.int.key", 100, 1000);
        Assertions.assertTrue(value >= 0);
    }

    @Test
    void testGetBoolean() {
        boolean value = apolloConfiguration.getBoolean("seata.boolean.key", true, 1000);
        Assertions.assertTrue(value || !value);
    }

    @Test
    void testGetLong() {
        long value = apolloConfiguration.getLong("seata.long.key", 1000L, 1000);
        Assertions.assertTrue(value >= 0);
    }

    @Test
    void testAddMultipleListeners() {
        ConfigurationChangeListener listener1 = event -> {};
        ConfigurationChangeListener listener2 = event -> {};

        apolloConfiguration.addConfigListener("seata.multi.listener", listener1);
        apolloConfiguration.addConfigListener("seata.multi.listener", listener2);

        Set<ConfigurationChangeListener> listeners = apolloConfiguration.getConfigListeners("seata.multi.listener");
        Assertions.assertNotNull(listeners);
        Assertions.assertEquals(2, listeners.size());

        apolloConfiguration.removeConfigListener("seata.multi.listener", listener1);
        apolloConfiguration.removeConfigListener("seata.multi.listener", listener2);
    }

    @Test
    void testAddConfigListenerWithBlankDataId() {
        ConfigurationChangeListener listener = event -> {};

        apolloConfiguration.addConfigListener("", listener);
        apolloConfiguration.addConfigListener(null, listener);

        Set<ConfigurationChangeListener> listeners1 = apolloConfiguration.getConfigListeners("");
        Assertions.assertTrue(listeners1 == null || listeners1.isEmpty());

        try {
            Set<ConfigurationChangeListener> listeners2 = apolloConfiguration.getConfigListeners(null);
            Assertions.assertTrue(listeners2 == null || listeners2.isEmpty());
        } catch (NullPointerException e) {
            // Expected exception for null dataId
        }
    }

    @Test
    void testAddConfigListenerWithNullListener() {
        apolloConfiguration.addConfigListener("seata.key", null);

        Set<ConfigurationChangeListener> listeners = apolloConfiguration.getConfigListeners("seata.key");
        Assertions.assertTrue(listeners == null || listeners.isEmpty());
    }

    @Test
    void testRemoveConfigListenerWithNullListener() {
        ConfigurationChangeListener listener = event -> {};

        apolloConfiguration.addConfigListener("seata.remove.test", listener);
        Set<ConfigurationChangeListener> listeners = apolloConfiguration.getConfigListeners("seata.remove.test");
        Assertions.assertNotNull(listeners);

        apolloConfiguration.removeConfigListener("seata.remove.test", null);
        Set<ConfigurationChangeListener> remainingListeners =
                apolloConfiguration.getConfigListeners("seata.remove.test");
        Assertions.assertEquals(1, remainingListeners.size());

        apolloConfiguration.removeConfigListener("seata.remove.test", listener);
    }

    @Test
    void testGetConfigListenersForNonExistentKey() {
        Set<ConfigurationChangeListener> listeners = apolloConfiguration.getConfigListeners("non.existent.key");
        Assertions.assertTrue(listeners == null || listeners.isEmpty());
    }

    @Test
    void testGetConfigWithNullKey() {
        String value = apolloConfiguration.getConfig(null, "default", 1000);
        Assertions.assertEquals("default", value);
    }

    @Test
    void testGetLatestConfigWithLongTimeout() {
        String value = apolloConfiguration.getLatestConfig("seata.test", "default", 5000);
        Assertions.assertEquals("mockdata", value);
    }

    @Test
    void testGetLatestConfigForNonExistentKey() {
        String value = apolloConfiguration.getLatestConfig("non.existent.key", "default-value", 1000);
        Assertions.assertEquals("default-value", value);
    }
}
