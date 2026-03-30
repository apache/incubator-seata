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
package org.apache.seata.discovery.registry;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.ConfigChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FileRegistryServiceImplTest {

    private static final String TEST_GROUP = "testGroup";
    private static final String TEST_CLUSTER = "default_tx_group";

    private final ServiceInstance serviceInstance1 = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8080));
    private final ServiceInstance serviceInstance2 = new ServiceInstance(new InetSocketAddress("127.0.0.2", 8080));

    private static FileRegistryServiceImpl fileRegistryService;

    @BeforeAll
    public static void setUp() {
        System.setProperty("service.vgroupMapping.testGroup", TEST_GROUP);
        fileRegistryService = FileRegistryServiceImpl.getInstance();
    }

    @Test
    public void testEmptyMethod() throws Exception {
        fileRegistryService.register(serviceInstance1);
        fileRegistryService.unregister(serviceInstance1);

        ConfigChangeListener configChangeListener = new ConfigChangeListener() {
            @Override
            public ExecutorService getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {}
        };

        fileRegistryService.subscribe(TEST_CLUSTER, configChangeListener);
        fileRegistryService.unsubscribe(TEST_CLUSTER, configChangeListener);

        fileRegistryService.close();
    }

    /**
     * Tests the getServiceGroup method to ensure it retrieves the correct service group name
     */
    @Test
    public void testGetServiceGroup() {
        String result = fileRegistryService.getServiceGroup(TEST_GROUP);
        assertEquals(TEST_GROUP, result);
    }

    /**
     * Tests the aliveLookup and refreshAliveLookup methods.
     */
    @Test
    public void testAliveLookupAndRefreshAliveLookup() {
        RegistryService.CURRENT_INSTANCE_MAP.clear();
        List<ServiceInstance> serviceInstances = Collections.singletonList(serviceInstance1);

        // Test empty list
        List<ServiceInstance> result = fileRegistryService.aliveLookup(TEST_GROUP);
        assertTrue(result.isEmpty());

        // Test data is available
        fileRegistryService.refreshAliveLookup(TEST_GROUP, serviceInstances);
        result = fileRegistryService.aliveLookup(TEST_GROUP);
        assertEquals(serviceInstances, result);
    }

    /**
     * Tests the removeOfflineAddressesIfNecessary method when there is no intersection
     */
    @Test
    public void testRemoveOfflineAddressesIfNecessaryWithNoIntersection() {
        List<ServiceInstance> currentInstances = Collections.singletonList(serviceInstance1);
        Collection<ServiceInstance> newInstances = Collections.singletonList(serviceInstance2);

        fileRegistryService.refreshAliveLookup(TEST_GROUP, currentInstances);
        fileRegistryService.removeOfflineAddressesIfNecessary(
                TEST_GROUP, fileRegistryService.getServiceGroup(TEST_GROUP), newInstances);

        List<ServiceInstance> result = fileRegistryService.aliveLookup(TEST_GROUP);
        assertFalse(result.isEmpty());
        assertEquals(currentInstances, result);
    }

    /**
     * Tests the removeOfflineAddressesIfNecessary method when there is an intersection
     */
    @Test
    public void testRemoveOfflineAddressesIfNecessaryWithIntersection() {
        List<ServiceInstance> currentInstances = Arrays.asList(serviceInstance1, serviceInstance2);
        Collection<ServiceInstance> newInstances = Collections.singletonList(serviceInstance1);

        fileRegistryService.refreshAliveLookup(TEST_GROUP, currentInstances);
        fileRegistryService.removeOfflineAddressesIfNecessary(
                TEST_GROUP, fileRegistryService.getServiceGroup(TEST_GROUP), newInstances);

        List<ServiceInstance> result = fileRegistryService.aliveLookup(TEST_GROUP);
        assertFalse(result.isEmpty());
        assertEquals(new HashSet<>(newInstances), new HashSet<>(result));
    }

    @Test
    public void testLookup() throws Exception {
        List<ServiceInstance> lookup = fileRegistryService.lookup(TEST_CLUSTER);
        assertEquals(serviceInstance1, lookup.get(0));

        // Set the getServiceGroup() to return null by reflection
        Configuration mockConfig = Mockito.mock(Configuration.class);
        when(mockConfig.getConfig("service.vgroupMapping.default_tx_group")).thenReturn(null);
        Field configField = ConfigurationFactory.class.getDeclaredField("instance");
        configField.setAccessible(true);
        Object originalConfigInstance = configField.get(null);
        configField.set(null, mockConfig);

        try {
            assertThrows(ConfigNotFoundException.class, () -> fileRegistryService.lookup(TEST_CLUSTER));
        } finally {
            // reset ConfigurationFactory.instance
            configField.set(null, originalConfigInstance);
        }
    }
}
