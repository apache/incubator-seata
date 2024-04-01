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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rm RPC client test.
 */
@Order(2)
class RmNettyClientTest {

    @BeforeAll
    public static void beforeAll() {
        RmNettyRemotingClient.getInstance().destroy();
    }

    @AfterAll
    public static void afterAll() {
        RmNettyRemotingClient.getInstance().destroy();
    }

    @Test
    public void assertGetInstanceAfterDestroy() {
        RmNettyRemotingClient oldClient = RmNettyRemotingClient.getInstance("ap", "group");
        AtomicBoolean initialized = getInitializeStatus(oldClient);
        oldClient.init();
        assertTrue(initialized.get());
        oldClient.destroy();
        assertFalse(initialized.get());
        RmNettyRemotingClient newClient = RmNettyRemotingClient.getInstance("ap", "group");
        Assertions.assertNotEquals(oldClient, newClient);
        initialized = getInitializeStatus(newClient);
        assertFalse(initialized.get());
        newClient.init();
        assertTrue(initialized.get());
        newClient.destroy();
    }

    @Test
    public void testCheckFailFast() throws Exception {
        RmNettyRemotingClient newClient = RmNettyRemotingClient.getInstance("fail_fast", "default_tx_group");

        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Resource mockResource = Mockito.mock(Resource.class);
        Map<String, Resource> resourceMap = new HashMap<>();
        resourceMap.put("jdbc:xx://localhost/test", mockResource);
        Mockito.when(resourceManager.getManagedResources()).thenReturn(resourceMap);
        newClient.setResourceManager(resourceManager);
        System.setProperty(ConfigurationKeys.ENABLE_RM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "true");
        ConfigurationCache.clear();
        Assertions.assertThrows(FrameworkException.class, newClient::init);
        System.setProperty(ConfigurationKeys.ENABLE_RM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "false");
    }
    
    private AtomicBoolean getInitializeStatus(final RmNettyRemotingClient rmNettyRemotingClient) {
        try {
            Field field = rmNettyRemotingClient.getClass().getDeclaredField("initialized");
            field.setAccessible(true);
            return (AtomicBoolean) field.get(rmNettyRemotingClient);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
