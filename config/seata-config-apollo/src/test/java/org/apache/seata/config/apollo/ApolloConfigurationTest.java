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

import java.io.IOException;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals("mockdata", value);
        value = apolloConfiguration.getConfig("seata.key");
        Assertions.assertNull(value);
        value = apolloConfiguration.getConfig("seata.key.1", "default");
        Assertions.assertEquals("default", value);
        value = apolloConfiguration.getLatestConfig("seata.key.2", "default", 3000);
        Assertions.assertEquals("default", value);
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
            public void onChangeEvent(ConfigurationChangeEvent event) {

            }
        };
        apolloConfiguration.addConfigListener("seata.test", listener);
        Assertions.assertEquals(1, apolloConfiguration.getConfigListeners("seata.test").size());
        apolloConfiguration.removeConfigListener("seata.test", null);
        Assertions.assertEquals(1, apolloConfiguration.getConfigListeners("seata.test").size());
        apolloConfiguration.removeConfigListener("seata.test", listener);
        Assertions.assertEquals(0, apolloConfiguration.getConfigListeners("seata.test").size());

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

}
