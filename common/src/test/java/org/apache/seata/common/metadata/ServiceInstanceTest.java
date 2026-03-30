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
package org.apache.seata.common.metadata;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceInstanceTest {

    private final InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
    private final InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 8092);

    @Test
    public void testConstructorAndGetters() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        ServiceInstance instance1 = new ServiceInstance(address1, metadata);

        assertEquals(address1, instance1.getAddress());
        assertEquals(metadata, instance1.getMetadata());

        Instance instance = Instance.getInstance();
        instance.setTransaction(new Node.Endpoint("127.0.0.1", 8093));

        ServiceInstance instance2 = new ServiceInstance(Instance.getInstance());

        assertEquals(
                Instance.getInstance().getTransaction().getHost(),
                instance2.getAddress().getAddress().getHostAddress());
        assertEquals(
                Instance.getInstance().getTransaction().getPort(),
                instance2.getAddress().getPort());

        instance.setTransaction(null); // clean up after test
    }

    @Test
    public void testConvertToServiceInstanceList() {
        List<InetSocketAddress> addresses = new ArrayList<>();
        addresses.add(address1);
        addresses.add(address2);

        List<ServiceInstance> serviceInstances = ServiceInstance.convertToServiceInstanceList(addresses);

        assertEquals(2, serviceInstances.size());
        assertEquals(address1, serviceInstances.get(0).getAddress());
        assertEquals(address2, serviceInstances.get(1).getAddress());
    }

    @Test
    public void testConvertToServiceInstanceSet() {
        Set<InetSocketAddress> addresses = new HashSet<>();
        addresses.add(address1);
        addresses.add(address2);

        Set<ServiceInstance> serviceInstances = ServiceInstance.convertToServiceInstanceSet(addresses);

        assertEquals(2, serviceInstances.size());
    }

    @Test
    public void testSetAddressAndSetMetadata() {
        ServiceInstance instance = new ServiceInstance(address1);
        instance.setAddress(address2);
        instance.setMetadata(new HashMap<>());

        assertEquals(address2, instance.getAddress());
        assertNotNull(instance.getMetadata());
    }

    @Test
    public void testFromStringMap() {
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("stringKey", "stringValue");

        ServiceInstance instance = ServiceInstance.fromStringMap(address1, stringMap);

        assertEquals(address1, instance.getAddress());
        assertNotNull(instance.getMetadata());
        assertEquals("stringValue", instance.getMetadata().get("stringKey"));
    }

    @Test
    public void testEqualsAndToString() {
        ServiceInstance instance1 = new ServiceInstance(address1);
        ServiceInstance instance2 = new ServiceInstance(address1);
        ServiceInstance instance3 = new ServiceInstance(address2);

        assertTrue(instance1.equals(instance2));
        assertTrue(instance1.equals(instance1));
        assertFalse(instance1.equals(instance3));
        assertFalse(instance1.equals("string"));

        assertTrue(instance1.toString().contains("8091"));
    }
}
