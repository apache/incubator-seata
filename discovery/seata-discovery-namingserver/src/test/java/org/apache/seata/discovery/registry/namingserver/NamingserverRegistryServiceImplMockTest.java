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
package org.apache.seata.discovery.registry.namingserver;

import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.metadata.namingserver.MetaResponse;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mock test for NamingserverRegistryServiceImpl
 */
public class NamingserverRegistryServiceImplMockTest {

    private NamingserverRegistryServiceImpl registryService;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        registryService = NamingserverRegistryServiceImpl.getInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up static maps to avoid test interference
        Field listenerMapField = NamingserverRegistryServiceImpl.class.getDeclaredField("LISTENER_SERVICE_MAP");
        listenerMapField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, List<NamingListener>> listenerMap = (Map<String, List<NamingListener>>) listenerMapField.get(null);
        listenerMap.clear();

        Field currentInstanceMapField = RegistryService.class.getDeclaredField("CURRENT_INSTANCE_MAP");
        currentInstanceMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, List<ServiceInstance>>> currentInstanceMap =
                (Map<String, Map<String, List<ServiceInstance>>>) currentInstanceMapField.get(null);
        currentInstanceMap.clear();

        // Reset isSubscribed field
        Field isSubscribedField = NamingserverRegistryServiceImpl.class.getDeclaredField("isSubscribed");
        isSubscribedField.setAccessible(true);
        isSubscribedField.set(registryService, false);
    }

    @Test
    public void testGetInstance() {
        NamingserverRegistryServiceImpl instance = NamingserverRegistryServiceImpl.getInstance();
        assertInstanceOf(NamingserverRegistryServiceImpl.class, instance);
    }

    @Test
    public void testRegister() throws Exception {
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8091));

        Instance mockInstance = mock(Instance.class);
        when(mockInstance.getNamespace()).thenReturn("test-namespace");
        when(mockInstance.getClusterName()).thenReturn("test-cluster");
        when(mockInstance.getUnit()).thenReturn("test-unit");
        when(mockInstance.getTransaction()).thenReturn(new Node.Endpoint("127.0.0.1", 8091));
        when(mockInstance.getMetadata()).thenReturn(new HashMap<>());
        when(mockInstance.toJsonString(any())).thenReturn("{\"test\":\"value\"}");

        try (MockedStatic<Instance> mockedInstance = Mockito.mockStatic(Instance.class)) {
            mockedInstance.when(Instance::getInstance).thenReturn(mockInstance);

            registryService.register(serviceInstance);

            verify(mockInstance).setTimestamp(any(Long.class));
        }
    }

    @Test
    public void testUnregister() {
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8091));

        Instance mockInstance = mock(Instance.class);
        when(mockInstance.getNamespace()).thenReturn("test-namespace");
        when(mockInstance.getClusterName()).thenReturn("test-cluster");
        when(mockInstance.getUnit()).thenReturn("test-unit");
        when(mockInstance.getTransaction()).thenReturn(new Node.Endpoint("127.0.0.1", 8091));
        when(mockInstance.getMetadata()).thenReturn(new HashMap<>());
        when(mockInstance.toJsonString(any())).thenReturn("{\"test\":\"value\"}");

        try (MockedStatic<Instance> mockedInstance = Mockito.mockStatic(Instance.class)) {
            mockedInstance.when(Instance::getInstance).thenReturn(mockInstance);

            registryService.unregister(serviceInstance);

            verify(mockInstance).getUnit();
            verify(mockInstance).toJsonString(any());
            verify(mockInstance).getClusterName();
            verify(mockInstance).getNamespace();
        }
    }

    @Test
    public void testHandleMetadata_withMockResponse() throws Exception {
        // Use reflection to set the isSubscribed field to true
        Field isSubscribedField = NamingserverRegistryServiceImpl.class.getDeclaredField("isSubscribed");
        isSubscribedField.setAccessible(true);
        isSubscribedField.set(registryService, true);

        // Create a mock MetaResponse
        MetaResponse metaResponse = new MetaResponse();
        metaResponse.setTerm(1);

        Cluster cluster = new Cluster();
        Unit unit = new Unit();
        List<NamingServerNode> namingInstanceList = new ArrayList<>();
        NamingServerNode node = new NamingServerNode();
        node.setRole(ClusterRole.LEADER);
        node.setTerm(1);
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8091));
        namingInstanceList.add(node);
        unit.setNamingInstanceList(namingInstanceList);
        List<Unit> unitData = new ArrayList<>();
        unitData.add(unit);
        cluster.setUnitData(unitData);
        List<Cluster> clusterList = new ArrayList<>();
        clusterList.add(cluster);
        metaResponse.setClusterList(clusterList);

        // Call the method to test
        Method handleMetadataMethod =
                registryService.getClass().getDeclaredMethod("handleMetadata", MetaResponse.class, String.class);
        handleMetadataMethod.setAccessible(true);
        List<ServiceInstance> result =
                (List<ServiceInstance>) handleMetadataMethod.invoke(registryService, metaResponse, "testGroup");

        registryService.lookup("testGroup");

        // Verify the result
        assertEquals(1, result.size());
        assertEquals("127.0.0.1", result.get(0).getAddress().getAddress().getHostAddress());
        assertEquals(8091, result.get(0).getAddress().getPort());
        isSubscribedField.set(registryService, false);
    }

    @Test
    public void testSubscribeAndUnsubscribe() throws Exception {
        NamingListener mockListener = mock(NamingListener.class);
        String vGroup = "test-vgroup";

        registryService.subscribe(mockListener, vGroup);

        // Verify that the listener was added to the map
        Field listenerMapField = NamingserverRegistryServiceImpl.class.getDeclaredField("LISTENER_SERVICE_MAP");
        listenerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<NamingListener>> listenerMap = (Map<String, List<NamingListener>>) listenerMapField.get(null);

        assertNotNull(listenerMap);
        assertTrue(listenerMap.containsKey(vGroup));
        List<NamingListener> listeners = listenerMap.get(vGroup);
        assertNotNull(listeners);
        assertTrue(listeners.contains(mockListener));

        registryService.unsubscribe(mockListener, vGroup);

        assertFalse(listenerMap.containsKey(vGroup));
    }

    @Test
    public void testRefreshAliveLookup() {
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8091));
        String transactionServiceGroup = "test-group";

        // Test refreshAliveLookup with a list of instances
        List<ServiceInstance> instances = Collections.singletonList(serviceInstance);
        registryService.refreshAliveLookup(transactionServiceGroup, instances);

        assertEquals(1, instances.size());
        assertEquals("127.0.0.1", instances.get(0).getAddress().getAddress().getHostAddress());
        assertEquals(8091, instances.get(0).getAddress().getPort());
    }

    @Test
    public void testEmptyMethod() throws Exception {
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8091));
        NamingListener mockListener = mock(NamingListener.class);

        registryService.register(serviceInstance);

        registryService.unregister(serviceInstance);

        registryService.subscribe("test-cluster", mockListener);

        registryService.unsubscribe("test-cluster", mockListener);

        registryService.close();
    }

    @Test
    public void testGetNamespace() throws Exception {
        System.setProperty("registry.seata.namespace", "dev");

        Method getNamespaceMethod = NamingserverRegistryServiceImpl.class.getDeclaredMethod("getNamespace");
        getNamespaceMethod.setAccessible(true);
        String result = (String) getNamespaceMethod.invoke(registryService);

        assertEquals("dev", result);

        System.clearProperty("registry.seata.namespace");
    }

    @Test
    public void testGetMetadataMaxAgeMs() throws Exception {
        Method getMetadataMaxAgeMsMethod =
                NamingserverRegistryServiceImpl.class.getDeclaredMethod("getMetadataMaxAgeMs");
        getMetadataMaxAgeMsMethod.setAccessible(true);
        String result = (String) getMetadataMaxAgeMsMethod.invoke(registryService);

        assertEquals("registry.seata.metadataMaxAgeMs", result);
    }

    @Test
    public void testGetServiceGroup() {
        Configuration mockConfig = mock(Configuration.class);
        when(mockConfig.getConfig("service.vgroupMapping.test-key")).thenReturn("test-cluster");

        try (MockedStatic<ConfigurationFactory> mockedFactory = mockStatic(ConfigurationFactory.class)) {
            mockedFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

            String result = registryService.getServiceGroup("test-key");
            assertEquals("test-cluster", result);
        }
    }
}
