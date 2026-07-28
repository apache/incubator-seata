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
package org.apache.seata.core.rpc.netty;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NettyClientCoverageTest extends BaseNettyClientTest {

    @BeforeEach
    public void saveProperties() {
        System.clearProperty("service.default.grouplist");
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        System.clearProperty(ConfigurationKeys.SHUTDOWN_WAIT);
        ConfigurationCache.clear();
    }

    @AfterEach
    public void restoreProperties() {
        System.clearProperty("service.default.grouplist");
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        System.clearProperty(ConfigurationKeys.SHUTDOWN_WAIT);
        ConfigurationCache.clear();
    }

    @Test
    public void testGetDynamicPort() throws Exception {
        int port = getDynamicPort();
        Assertions.assertTrue(port > 0 && port <= 65535, "Port should be in valid range");

        int port2 = getDynamicPort();
        Assertions.assertTrue(port2 > 0 && port2 <= 65535, "Second port should also be in valid range");
    }

    @Test
    public void testConfigureAndCleanup() {
        Assertions.assertNull(System.getProperty("service.default.grouplist"));
        Assertions.assertNull(System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        Assertions.assertNull(System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));

        configureClient(12345);

        Assertions.assertEquals("127.0.0.1:12345", System.getProperty("service.default.grouplist"));
        Assertions.assertEquals("12345", System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        Assertions.assertEquals("0", System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));

        cleanupClientConfig();

        Assertions.assertNull(System.getProperty("service.default.grouplist"));
        Assertions.assertNull(System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        Assertions.assertNull(System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));
    }

    @Test
    public void testConfigureClientPreservesOriginalValues() {
        System.setProperty("service.default.grouplist", "original-group");
        System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "9999");
        System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, "30");

        configureClient(12345);

        Assertions.assertEquals("127.0.0.1:12345", System.getProperty("service.default.grouplist"));
        Assertions.assertEquals("12345", System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        Assertions.assertEquals("0", System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));

        cleanupClientConfig();

        Assertions.assertEquals("original-group", System.getProperty("service.default.grouplist"));
        Assertions.assertEquals("9999", System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        Assertions.assertEquals("30", System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));
    }

    @Test
    public void testInitMessageExecutor() {
        java.util.concurrent.ThreadPoolExecutor executor = initMessageExecutor();
        Assertions.assertNotNull(executor);
        Assertions.assertEquals(5, executor.getCorePoolSize());
        Assertions.assertEquals(5, executor.getMaximumPoolSize());
        executor.shutdown();
    }

    @Test
    public void testServerInstanceWrapper() throws Exception {
        int port = getDynamicPort();
        ServerInstance instance = new ServerInstance(null, port);
        Assertions.assertEquals(port, instance.getPort());
        Assertions.assertNull(instance.getServer());
        Assertions.assertEquals("127.0.0.1:" + port, instance.getAddress());
        instance.destroy();
    }
}
