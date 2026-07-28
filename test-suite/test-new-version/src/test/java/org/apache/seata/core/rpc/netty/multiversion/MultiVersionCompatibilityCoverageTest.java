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
package org.apache.seata.core.rpc.netty.multiversion;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.protocol.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultiVersionCompatibilityCoverageTest extends AbstractMultiVersionCompatibilityTest {

    @AfterEach
    @Override
    public void tearDown() throws InterruptedException {
        super.tearDown();
    }

    @Test
    public void testSetUpSetsPropertiesAndClearsCache() {
        Assertions.assertEquals(Protocol.SEATA.value, System.getProperty(ConfigurationKeys.TRANSPORT_PROTOCOL));
        Assertions.assertEquals("0", System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));
    }

    @Test
    public void testTearDownRestoresProperties() throws InterruptedException {
        String beforeProtocol = System.getProperty(ConfigurationKeys.TRANSPORT_PROTOCOL);
        Assertions.assertNotNull(beforeProtocol, "setUp should have set TRANSPORT_PROTOCOL");

        tearDown();
        setUp();

        Assertions.assertEquals(Protocol.SEATA.value, System.getProperty(ConfigurationKeys.TRANSPORT_PROTOCOL));
    }

    @Test
    public void testSetUpPreservesOriginalTransportProtocol() throws InterruptedException {
        tearDown();

        System.setProperty(ConfigurationKeys.TRANSPORT_PROTOCOL, "test-original-value");
        ConfigurationCache.clear();

        setUp();

        Assertions.assertEquals(Protocol.SEATA.value, System.getProperty(ConfigurationKeys.TRANSPORT_PROTOCOL));

        tearDown();

        Assertions.assertEquals("test-original-value", System.getProperty(ConfigurationKeys.TRANSPORT_PROTOCOL));

        System.clearProperty(ConfigurationKeys.TRANSPORT_PROTOCOL);
        ConfigurationCache.clear();
        setUp();
    }

    @Test
    public void testSetUpPreservesOriginalShutdownWait() throws InterruptedException {
        tearDown();

        System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, "42");
        ConfigurationCache.clear();

        setUp();

        Assertions.assertEquals("0", System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));

        tearDown();

        Assertions.assertEquals("42", System.getProperty(ConfigurationKeys.SHUTDOWN_WAIT));

        System.clearProperty(ConfigurationKeys.SHUTDOWN_WAIT);
        ConfigurationCache.clear();
        setUp();
    }

    @Test
    public void testToPrettyJsonNull() {
        Assertions.assertEquals("null", AbstractMultiVersionCompatibilityTest.toPrettyJson(null));
    }

    @Test
    public void testToPrettyJsonSimpleObject() {
        String result = AbstractMultiVersionCompatibilityTest.toPrettyJson("hello");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("hello"));
    }

    @Test
    public void testToPrettyJsonMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("key", "value");
        String result = AbstractMultiVersionCompatibilityTest.toPrettyJson(map);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("value"));
    }
}
