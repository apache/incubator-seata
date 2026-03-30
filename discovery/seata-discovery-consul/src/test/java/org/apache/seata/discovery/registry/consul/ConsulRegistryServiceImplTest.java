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
package org.apache.seata.discovery.registry.consul;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.registry.RegistryProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ConsulRegistryServiceImpl test
 * Need to configure the address of consul into the registry.conf file for test
 */
@Disabled
public class ConsulRegistryServiceImplTest {

    private static MockConsulRegistryServiceImpl registryService;

    @BeforeAll
    public static void setUp() throws Exception {
        ServiceLoader<RegistryProvider> providers = ServiceLoader.load(RegistryProvider.class);
        RegistryProvider provider = providers.iterator().next();
        registryService = (MockConsulRegistryServiceImpl) provider.provide();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (registryService != null) {
            registryService.close();
        }
    }

    @Test
    public void testMetadataRegistrationAndDiscovery() throws Exception {
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("version", "1.0.0");
        metadata1.put("environment", "test");
        metadata1.put("weight", 100);

        ServiceInstance instance1 = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8080), metadata1);
        registryService.register(instance1);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("version", "2.0.0");
        metadata2.put("zone", "bj");

        ServiceInstance instance2 = new ServiceInstance(new InetSocketAddress("127.0.0.1", 9090), metadata2);
        registryService.register(instance2);

        Thread.sleep(3000);

        List<ServiceInstance> instances = registryService.lookup("default_tx_group");

        assertNotNull(instances);
        assertFalse(instances.isEmpty());

        ServiceInstance foundInstance1 = instances.stream()
                .filter(inst -> inst.getAddress().equals(instance1.getAddress()))
                .findFirst()
                .orElse(null);

        assertNotNull(foundInstance1);
        Map<String, Object> foundMetadata1 = foundInstance1.getMetadata();
        assertNotNull(foundMetadata1);
        assertEquals("1.0.0", foundMetadata1.get("version"));
        assertEquals("test", foundMetadata1.get("environment"));
        assertEquals("100", foundMetadata1.get("weight"));

        ServiceInstance foundInstance2 = instances.stream()
                .filter(inst -> inst.getAddress().equals(instance2.getAddress()))
                .findFirst()
                .orElse(null);

        assertNotNull(foundInstance2);
        Map<String, Object> foundMetadata2 = foundInstance2.getMetadata();
        assertNotNull(foundMetadata2);
        assertEquals("2.0.0", foundMetadata2.get("version"));
        assertEquals("bj", foundMetadata2.get("zone"));

        registryService.unregister(instance1);
        registryService.unregister(instance2);
    }
}
