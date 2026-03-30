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
package org.apache.seata.discovery.registry.eureka;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.registry.RegistryProvider;
import org.apache.seata.discovery.registry.RegistryService;
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
 * EurekaRegistryServiceImpl integration test
 */
@Disabled
public class EurekaRegistryServiceImplTest {

    private static RegistryService registryService;

    @BeforeAll
    public static void setUp() throws Exception {
        ServiceLoader<RegistryProvider> providers = ServiceLoader.load(RegistryProvider.class);
        RegistryProvider provider = providers.iterator().next();
        registryService = provider.provide();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (registryService != null) {
            registryService.close();
        }
    }

    @Test
    public void testMetadataRegistrationAndDiscovery() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("environment", "test");
        metadata.put("weight", 100);

        ServiceInstance instance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8094), metadata);
        registryService.register(instance);

        Thread.sleep(8000);

        List<ServiceInstance> instances = null;
        // Wait for the Eureka client's local cache to sync to avoid the first query being empty.
        for (int i = 0; i < 10; i++) {
            instances = registryService.lookup("default_tx_group");
            if (instances != null && !instances.isEmpty()) {
                break;
            }
            Thread.sleep(2000);
        }

        assertNotNull(instances);
        assertFalse(instances.isEmpty());

        ServiceInstance foundInstance = instances.stream()
                .filter(inst -> inst.getAddress().equals(instance.getAddress()))
                .findFirst()
                .orElse(null);

        assertNotNull(foundInstance);
        Map<String, Object> foundMetadata = foundInstance.getMetadata();
        assertNotNull(foundMetadata);
        assertEquals("1.0.0", String.valueOf(foundMetadata.get("version")));
        assertEquals("test", String.valueOf(foundMetadata.get("environment")));
        assertEquals("100", String.valueOf(foundMetadata.get("weight")));
    }
}
