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
package org.apache.seata.discovery.registry.nacos;

import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.apache.seata.common.metadata.ServiceInstance;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "nacosCaseEnabled", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NacosRegistryServiceImplTest {

    private static final String SERVICE_NAME = "default_tx_group";
    private static final String CLUSTER_NAME = "default";

    private static final NacosRegistryServiceImpl service = NacosRegistryServiceImpl.getInstance();

    @Test
    public void testGetInstance() {
        NacosRegistryServiceImpl instance = NacosRegistryServiceImpl.getInstance();
        assertInstanceOf(NacosRegistryServiceImpl.class, instance);
    }

    @Test
    @Order(1)
    public void testRegister() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("weight", 1.0);
        metadata.put("healthy", true);

        ServiceInstance serviceInstance1 = new ServiceInstance(address, metadata);

        // Verify ServiceInstance metadata
        assertNotNull(serviceInstance1.getMetadata());
        assertEquals("1.0.0", serviceInstance1.getMetadata().get("version"));
        assertEquals(1.0, serviceInstance1.getMetadata().get("weight"));
        assertEquals(true, serviceInstance1.getMetadata().get("healthy"));

        service.register(serviceInstance1);

        // Verify registration success
        long startTime = System.currentTimeMillis();
        while (service.lookup(SERVICE_NAME).isEmpty() && System.currentTimeMillis() - startTime < 5000) {
            Thread.sleep(100);
        }

        List<ServiceInstance> instances = service.lookup(SERVICE_NAME);
        assertFalse(instances.isEmpty());

        // Cleanup
        service.unregister(serviceInstance1);
    }

    @Test
    public void testRegisterWithInvalidAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            InetSocketAddress invalidAddress = new InetSocketAddress("127.0.0.1", 0);
            ServiceInstance invalidInstance = new ServiceInstance(invalidAddress, new HashMap<>());
            service.register(invalidInstance);
        });
    }

    @Test
    @Order(2)
    public void testUnregister() throws Exception {
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 8092);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("weight", 1.0);
        ServiceInstance serviceInstance2 = new ServiceInstance(address2, metadata);

        InetSocketAddress address3 = new InetSocketAddress("127.0.0.1", 8093);
        ServiceInstance serviceInstance3 = new ServiceInstance(address3, metadata);

        // Verify ServiceInstance metadata
        assertNotNull(serviceInstance2.getMetadata());
        assertEquals("1.0.0", serviceInstance2.getMetadata().get("version"));
        assertEquals(1.0, serviceInstance2.getMetadata().get("weight"));

        service.register(serviceInstance2);
        service.register(serviceInstance3);

        long startTime = System.currentTimeMillis();
        while (service.lookup(SERVICE_NAME).isEmpty() && System.currentTimeMillis() - startTime < 5000) {
            Thread.sleep(100);
        }

        List<ServiceInstance> instancesBefore = service.lookup(SERVICE_NAME);
        assertFalse(instancesBefore.isEmpty());

        service.unregister(serviceInstance2);

        startTime = System.currentTimeMillis();
        while (!service.lookup(SERVICE_NAME).isEmpty() && System.currentTimeMillis() - startTime < 5000) {
            Thread.sleep(100);
        }

        // Verify unregistration success
        List<ServiceInstance> instancesAfter = service.lookup(SERVICE_NAME);
        assertEquals(1, instancesAfter.size());

        service.unregister(serviceInstance3);
    }

    @Test
    @Order(3)
    public void testSubscribe() throws Exception {
        // Clean up any existing instances to avoid test pollution
        List<ServiceInstance> existingInstances = service.lookup(SERVICE_NAME);
        for (ServiceInstance instance : existingInstances) {
            service.unregister(instance);
        }
        Thread.sleep(1000);

        CountDownLatch latch = new CountDownLatch(1);

        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8094);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("weight", 1.0);
        metadata.put("healthy", true);

        ServiceInstance serviceInstance = new ServiceInstance(address, metadata);
        final String expectedIp = address.getAddress().getHostAddress();
        final int expectedPort = address.getPort();

        // Create test listener
        EventListener listener = new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event instanceof NamingEvent) {
                    NamingEvent namingEvent = (NamingEvent) event;
                    List<Instance> instances = namingEvent.getInstances();

                    if (instances != null && !instances.isEmpty()) {
                        for (Instance instance : instances) {
                            if (expectedIp.equals(instance.getIp()) && expectedPort == instance.getPort()) {
                                Map<String, String> instanceMetadata = instance.getMetadata();
                                assertNotNull(instanceMetadata);
                                assertEquals("1.0.0", instanceMetadata.get("version"));
                                assertEquals("1.0", instanceMetadata.get("weight"));
                                assertEquals("true", instanceMetadata.get("healthy"));
                                latch.countDown();
                                return;
                            }
                        }
                    }
                }
            }
        };

        service.subscribe(CLUSTER_NAME, listener);
        Thread.sleep(1000);

        service.register(serviceInstance);
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Cleanup
        service.unregister(serviceInstance);
        service.unsubscribe(CLUSTER_NAME, listener);
    }

    @Test
    @Order(4)
    public void testUnsubscribe() throws Exception {
        EventListener listener = new EventListener() {
            @Override
            public void onEvent(Event event) {}
        };

        // Subscribe first
        service.subscribe(CLUSTER_NAME, listener);

        // Verify listener is added to LISTENER_SERVICE_MAP
        ConcurrentMap<String, List<EventListener>> listenersMap = getListenersMap();
        boolean found = false;
        for (List<EventListener> listeners : listenersMap.values()) {
            if (listeners.contains(listener)) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        // Unsubscribe
        service.unsubscribe(CLUSTER_NAME, listener);

        // Verify listener is removed from LISTENER_SERVICE_MAP
        found = false;
        for (List<EventListener> listeners : listenersMap.values()) {
            if (listeners.contains(listener)) {
                found = true;
                break;
            }
        }
        assertFalse(found);

        // Verify listener is not null
        assertNotNull(listener);
    }

    @Test
    @Order(5)
    public void testClose() throws Exception {
        NacosRegistryServiceImpl instance = NacosRegistryServiceImpl.getInstance();
        NacosRegistryServiceImpl.getNamingInstance();

        Field useSLBWayField = NacosRegistryServiceImpl.class.getDeclaredField("useSLBWay");
        useSLBWayField.setAccessible(true);
        useSLBWayField.set(instance, true);
        NacosRegistryServiceImpl.getNamingMaintainInstance();

        instance.close();

        Field namingField = NacosRegistryServiceImpl.class.getDeclaredField("naming");
        namingField.setAccessible(true);
        assertNull(namingField.get(null));

        Field namingMaintainField = NacosRegistryServiceImpl.class.getDeclaredField("namingMaintain");
        namingMaintainField.setAccessible(true);
        assertNull(namingMaintainField.get(null));
    }

    private ConcurrentMap<String, List<EventListener>> getListenersMap() throws Exception {
        Class<?> clazz = NacosRegistryServiceImpl.class;

        Field listenerServiceMapField = clazz.getDeclaredField("LISTENER_SERVICE_MAP");
        listenerServiceMapField.setAccessible(true);

        return (ConcurrentMap<String, List<EventListener>>) listenerServiceMapField.get(null);
    }
}
