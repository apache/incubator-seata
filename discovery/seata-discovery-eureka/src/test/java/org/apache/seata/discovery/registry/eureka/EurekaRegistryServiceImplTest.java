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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.discovery.shared.Application;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mockStatic;

public class EurekaRegistryServiceImplTest {

    private EurekaClient mockEurekaClient;
    private ApplicationInfoManager mockAppInfoManager;
    private Application mockApplication;
    private InstanceInfo mockInstanceInfo;
    private EurekaRegistryServiceImpl registryService;
    EurekaEventListener mockEventListener;

    @BeforeEach
    public void setUp() throws Exception {

        mockEurekaClient = mock(EurekaClient.class);
        mockAppInfoManager = mock(ApplicationInfoManager.class);
        mockApplication = mock(Application.class);
        mockInstanceInfo = mock(InstanceInfo.class);
        mockEventListener  = mock(EurekaEventListener.class);

        resetSingleton();
        registryService = EurekaRegistryServiceImpl.getInstance();
        setStaticField(EurekaRegistryServiceImpl.class, "eurekaClient", mockEurekaClient);
        setStaticField(EurekaRegistryServiceImpl.class, "applicationInfoManager", mockAppInfoManager);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        resetSingleton();
    }

    private static void resetSingleton() throws Exception {
        // Reset singleton and static fields
        setStaticField(EurekaRegistryServiceImpl.class, "instance", null);
        setStaticField(EurekaRegistryServiceImpl.class, "applicationInfoManager", null);
        setStaticField(EurekaRegistryServiceImpl.class, "eurekaClient", null);
        setStaticField(EurekaRegistryServiceImpl.class, "instanceConfig", null);
        clearStaticMap(EurekaRegistryServiceImpl.class, "LISTENER_SERVICE_MAP");
        clearStaticMap(EurekaRegistryServiceImpl.class, "CLUSTER_ADDRESS_MAP");
        clearStaticMap(EurekaRegistryServiceImpl.class, "CLUSTER_LOCK");
    }

    @Test
    public void testGetInstance() {
        EurekaRegistryServiceImpl instance1 = EurekaRegistryServiceImpl.getInstance();
        EurekaRegistryServiceImpl instance2 = EurekaRegistryServiceImpl.getInstance();
        Assertions.assertEquals(instance1,instance2);
    }

    @Test
    public void testRegister() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        registryService.register(address);
        CustomEurekaInstanceConfig instanceConfig = getInstanceConfig();
        Assertions.assertEquals( "127.0.0.1", instanceConfig.getIpAddress());
        Assertions.assertEquals( "default", instanceConfig.getAppname());
        verify(mockAppInfoManager).setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    void testSubscribe() throws Exception {
        String testCluster = "TEST_CLUSTER";
        registryService.subscribe(testCluster, mockEventListener);

        // Verify that the listener is added to LISTENER_SERVICE_MAP
        ConcurrentMap<String, List<EurekaEventListener>> listenerMap = getStaticListenerMap();
        Assertions.assertTrue(listenerMap.containsKey(testCluster));
        Assertions.assertTrue(listenerMap.get(testCluster).contains(mockEventListener));

        // Verify that the EurekaClient has registered the listener
        verify(mockEurekaClient, times(1)).registerEventListener(mockEventListener);
    }

    @Test
    void testUnsubscribe() throws Exception {
        String testCluster = "TEST_CLUSTER";
        registryService.subscribe(testCluster, mockEventListener);
        registryService.unsubscribe(testCluster, mockEventListener);

        // Verify that the listener is removed from LISTENER_SERVICE_MAP
        ConcurrentMap<String, List<EurekaEventListener>> listenerMap = getStaticListenerMap();
        Assertions.assertFalse(listenerMap.getOrDefault(testCluster, Collections.emptyList()).contains(mockEventListener));

        // Verify that the EurekaClient has deregistered the listener
        verify(mockEurekaClient, times(1)).unregisterEventListener(mockEventListener);
    }

    @Test
    void testUnsubscribeWithNoExistingListeners() throws Exception {
        String testCluster = "NON_EXISTENT_CLUSTER";
        registryService.unsubscribe(testCluster, mockEventListener);
        verify(mockEurekaClient).unregisterEventListener(any());
    }

    @Test
    public void testUnregister() throws Exception {
        registryService.unregister(new InetSocketAddress("127.0.0.1", 8091));
        verify(mockAppInfoManager).setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
    }

@Test
    public void testLookup() throws Exception {
    Configuration mockConfig = mock(Configuration.class);
    when(mockConfig.getConfig("service.vgroupMapping.test-group")).thenReturn("TEST-CLUSTER");

    try (MockedStatic<ConfigurationFactory> mockedFactory = mockStatic(ConfigurationFactory.class)) {
        mockedFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

        // Mock Eureka to return the application instance
        when(mockEurekaClient.getApplication("TEST-CLUSTER")).thenReturn(mockApplication);
        when(mockApplication.getInstances()).thenReturn(Collections.singletonList(mockInstanceInfo));
        when(mockInstanceInfo.getStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);
        when(mockInstanceInfo.getIPAddr()).thenReturn("192.168.1.1");
        when(mockInstanceInfo.getPort()).thenReturn(8091);

        List<InetSocketAddress> addresses = registryService.lookup("test-group");

        // Verify whether the transactionServiceGroup is set correctly
        Field serviceGroupField = EurekaRegistryServiceImpl.class.getDeclaredField("transactionServiceGroup");
        serviceGroupField.setAccessible(true);
        String actualServiceGroup = (String) serviceGroupField.get(registryService);
        Assertions.assertEquals("test-group", actualServiceGroup);
        Assertions.assertNotNull(addresses);
        Assertions.assertEquals(1, addresses.size());
        Assertions.assertEquals(new InetSocketAddress("192.168.1.1", 8091), addresses.get(0));
    }
}

    @Test
    public void testClose() throws Exception {
        registryService.close();
        verify(mockEurekaClient).shutdown();
        Assertions.assertNull(getStaticField(EurekaRegistryServiceImpl.class, "eurekaClient"));
        Assertions.assertNull(getStaticField(EurekaRegistryServiceImpl.class, "applicationInfoManager"));
    }

    // Helper method: Set static fields via reflection
    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    // Helper method: Get the value of a static field
    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(null);
    }


    private static void clearStaticMap(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((ConcurrentMap<?, ?>) field.get(null)).clear();
    }


    private CustomEurekaInstanceConfig getInstanceConfig() throws Exception {
        return getStaticField(EurekaRegistryServiceImpl.class, "instanceConfig");
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<String, List<EurekaEventListener>> getStaticListenerMap() throws Exception {
        Field field = EurekaRegistryServiceImpl.class.getDeclaredField("LISTENER_SERVICE_MAP");
        field.setAccessible(true);
        return (ConcurrentMap<String, List<EurekaEventListener>>) field.get(null);
    }
}