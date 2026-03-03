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

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The type Nacos registry serivce impl test
 */
public class NacosRegistryServiceImplTest {

    private NamingService mockedNamingService;
    private NamingMaintainService mockedNamingMaintainService;

    private MockedStatic<ConfigurationFactory> mockedConfigurationFactory;
    private Configuration mockedRegisterServiceConfiguration;

    private NacosRegistryServiceImpl nacosRegistryService;

    // Constants extracted
    private static final String CONTEXT_PATH_KEY = "registry.nacos.contextPath";
    private static final String SLB_PATTERN_KEY = "registry.nacos.slbPattern";
    private static final String SERVER_ADDR_KEY = "registry.nacos.serverAddr";
    private static final String APPLICATION_KEY = "registry.nacos.application";
    private static final String GROUP_KEY = "registry.nacos.group";
    private static final String CLUSTER_KEY = "registry.nacos.cluster";

    private static final String NACOS_MOCKED_APPLICATION = "MOCKED_APP";
    private static final String NACOS_MOCKED_GROUP = "MOCKED_GROUP";
    private static final String NACOS_MOCKED_CLUSTER = "MOCKED_CLUSTER";
    private static final String NACOS_TX_SERVICE_GROUP_KEY = "MOCKED_TX_SERVICE_GROUP_KEY";

    @BeforeEach
    public void beforeEach() throws Exception {
        mockedNamingService = mock(NamingService.class);
        mockedNamingMaintainService = mock(NamingMaintainService.class);

        mockedConfigurationFactory = mockStatic(ConfigurationFactory.class);
        mockedRegisterServiceConfiguration = mock(Configuration.class);
        mockedConfigurationFactory
                .when(ConfigurationFactory::getInstance)
                .thenReturn(mockedRegisterServiceConfiguration);

        Configuration mockedCurrentNacosConfiguration = mock(Configuration.class);
        ReflectionUtil.modifyStaticFinalField(
                ConfigurationFactory.class, "CURRENT_FILE_INSTANCE", mockedCurrentNacosConfiguration);

        // default config expectations
        when(mockedCurrentNacosConfiguration.getConfig(SLB_PATTERN_KEY)).thenReturn("");
        when(mockedCurrentNacosConfiguration.getConfig(SERVER_ADDR_KEY)).thenReturn("127.0.0.1");

        // Mock for getServiceName() -> registry.nacos.application
        when(mockedCurrentNacosConfiguration.getConfig(APPLICATION_KEY)).thenReturn(NACOS_MOCKED_APPLICATION);
        when(mockedCurrentNacosConfiguration.getConfig(APPLICATION_KEY, "seata-server"))
                .thenReturn(NACOS_MOCKED_APPLICATION);

        // Mock for getServiceGroup() -> registry.nacos.group
        when(mockedCurrentNacosConfiguration.getConfig(GROUP_KEY)).thenReturn(NACOS_MOCKED_GROUP);
        when(mockedCurrentNacosConfiguration.getConfig(GROUP_KEY, "DEFAULT_GROUP"))
                .thenReturn(NACOS_MOCKED_GROUP);

        // Mock for getClusterName() -> registry.nacos.cluster
        when(mockedCurrentNacosConfiguration.getConfig(CLUSTER_KEY)).thenReturn(NACOS_MOCKED_CLUSTER);
        when(mockedCurrentNacosConfiguration.getConfig(CLUSTER_KEY, "default")).thenReturn(NACOS_MOCKED_CLUSTER);

        when(mockedCurrentNacosConfiguration.getConfig(CONTEXT_PATH_KEY)).thenReturn("/foo");

        nacosRegistryService = NacosRegistryServiceImpl.getInstance();

        ReflectionUtil.setFieldValue(nacosRegistryService, "naming", mockedNamingService);
        ReflectionUtil.setFieldValue(nacosRegistryService, "namingMaintain", mockedNamingMaintainService);
        ReflectionUtil.setFieldValue(nacosRegistryService, "useSLBWay", false);
    }

    @AfterEach
    public void afterEach() {
        // Clear any system properties set by tests
        Arrays.asList("username", "password", "accessKey", "secretKey", "ramRoleName", "contextPath")
                .forEach(System::clearProperty);

        // reset static fields to avoid test interdependence
        try {
            ReflectionUtil.setFieldValue(NacosRegistryServiceImpl.class, "naming", null);
            ReflectionUtil.setFieldValue(NacosRegistryServiceImpl.class, "namingMaintain", null);
            ReflectionUtil.setFieldValue(NacosRegistryServiceImpl.class, "instance", null);
            ReflectionUtil.setFieldValue(NacosRegistryServiceImpl.class, "useSLBWay", false);
        } catch (Exception e) {
            // ignore cleanup errors in tearDown
        } finally {
            if (mockedConfigurationFactory != null) {
                mockedConfigurationFactory.close();
            }
        }
    }

    @Test
    public void testGetConfigProperties() throws Exception {
        Method method = ReflectionUtil.getMethod(NacosRegistryServiceImpl.class, "getNamingProperties");
        Properties properties = (Properties) ReflectionUtil.invokeMethod(null, method);
        Assertions.assertThat(properties.getProperty("contextPath")).isEqualTo("/foo");
        System.setProperty("contextPath", "/bar");
        properties = (Properties) ReflectionUtil.invokeMethod(null, method);
        Assertions.assertThat(properties.getProperty("contextPath")).isEqualTo("/bar");
    }

    @Test
    public void shouldInitializeAuthUsingSystemProperties() throws Exception {
        // confirm that getNamingProperties picks up username/password when set
        System.setProperty("username", "testUser");
        System.setProperty("password", "testPass");

        Method method = ReflectionUtil.getMethod(NacosRegistryServiceImpl.class, "getNamingProperties");
        Properties properties = (Properties) ReflectionUtil.invokeMethod(null, method);
        assertEquals("testUser", properties.getProperty("username"));
        assertEquals("testPass", properties.getProperty("password"));
    }

    @Test
    public void shouldInitAuthWithAccessKeyAndSecretKey() throws Exception {
        System.setProperty("accessKey", "ak");
        System.setProperty("secretKey", "sk");

        Method method = ReflectionUtil.getMethod(NacosRegistryServiceImpl.class, "getNamingProperties");
        Properties properties = (Properties) ReflectionUtil.invokeMethod(null, method);
        assertEquals("ak", properties.getProperty("accessKey"));
        assertEquals("sk", properties.getProperty("secretKey"));
    }

    @Test
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

    @Test
    public void shouldRegisterSuccessfully() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8091);

        nacosRegistryService.register(inetSocketAddress);

        verify(mockedNamingService)
                .registerInstance(
                        NACOS_MOCKED_APPLICATION,
                        NACOS_MOCKED_GROUP,
                        inetSocketAddress.getAddress().getHostAddress(),
                        inetSocketAddress.getPort(),
                        NACOS_MOCKED_CLUSTER);
    }

    @Test
    public void shouldRegisterFailedWithInvalidAddress() {
        InetSocketAddress invalidAddress = new InetSocketAddress("127.0.0.1", 0);

        assertThrows(IllegalArgumentException.class, () -> nacosRegistryService.register(invalidAddress));
    }

    @Test
    public void shouldUnregisterSuccessfully() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8091);

        nacosRegistryService.unregister(inetSocketAddress);

        verify(mockedNamingService)
                .deregisterInstance(
                        NACOS_MOCKED_APPLICATION,
                        NACOS_MOCKED_GROUP,
                        inetSocketAddress.getAddress().getHostAddress(),
                        inetSocketAddress.getPort(),
                        NACOS_MOCKED_CLUSTER);
    }

    @Test
    public void shouldUnRegisterFailedWithInvalidAddress() {
        InetSocketAddress invalidAddress = new InetSocketAddress("127.0.0.1", 0);

        assertThrows(IllegalArgumentException.class, () -> nacosRegistryService.unregister(invalidAddress));
    }

    @Test
    public void shouldSubscribeAndStoreListener() throws Exception {
        EventListener mockedEventListener = mock(EventListener.class);

        nacosRegistryService.subscribe("MOCKED_CLUSTER_A", mockedEventListener);

        verify(mockedNamingService)
                .subscribe(
                        NACOS_MOCKED_APPLICATION,
                        NACOS_MOCKED_GROUP,
                        Collections.singletonList("MOCKED_CLUSTER_A"),
                        mockedEventListener);
        ConcurrentMap<String, List<EventListener>> listenerMap =
                ReflectionUtil.getFieldValue(nacosRegistryService, "LISTENER_SERVICE_MAP");
        assertEquals(listenerMap.get("MOCKED_CLUSTER_A"), Collections.singletonList(mockedEventListener));
    }

    @Test
    public void shouldUnsubscribeSuccessfully() throws Exception {
        EventListener mockedEventListener = mock(EventListener.class);
        EventListener existedEventListener = mock(EventListener.class);
        ConcurrentMap<String, List<EventListener>> listenerMap =
                ReflectionUtil.getFieldValue(nacosRegistryService, "LISTENER_SERVICE_MAP");
        listenerMap.put("MOCKED_CLUSTER_B", Arrays.asList(mockedEventListener, existedEventListener));

        nacosRegistryService.unsubscribe("MOCKED_CLUSTER_B", mockedEventListener);

        verify(mockedNamingService)
                .unsubscribe(
                        NACOS_MOCKED_APPLICATION,
                        NACOS_MOCKED_GROUP,
                        Collections.singletonList("MOCKED_CLUSTER_B"),
                        mockedEventListener);
        assertEquals(listenerMap.get("MOCKED_CLUSTER_B"), Collections.singletonList(existedEventListener));
    }

    @Test
    public void shouldLookupFailedWithoutClusterName() throws NoSuchFieldException {
        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn(null);

        assertThrows(ConfigNotFoundException.class, () -> nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY));

        assertEquals(
                NACOS_TX_SERVICE_GROUP_KEY,
                ReflectionUtil.getFieldValue(nacosRegistryService, "transactionServiceGroup"));
    }

    @Test
    public void shouldLookupWhenUseSLBWayIsTrueAndNoValueMatchedInClusterAddressMap() throws Exception {
        ReflectionUtil.setFieldValue(nacosRegistryService, "useSLBWay", true);
        String mockedPublicIp = "127.0.0.1";
        String mockedPublicPort = "8091";

        Service mockedService = new Service();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("publicIp", mockedPublicIp);
        metadata.put("publicPort", mockedPublicPort);
        mockedService.setMetadata(metadata);

        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn("MOCKED_CLUSTER");
        when(mockedNamingMaintainService.queryService(anyString(), anyString())).thenReturn(mockedService);

        List<InetSocketAddress> actualResult = nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY);

        List<InetSocketAddress> availableServers = new ArrayList<>();
        availableServers.add(new InetSocketAddress(mockedPublicIp, Integer.parseInt(mockedPublicPort)));
        assertEquals(actualResult, availableServers);

        nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY);
        verify(mockedNamingMaintainService, times(1)).queryService(anyString(), anyString());
    }

    @Test
    public void shouldThrowsExceptionWhenLookupFailsDueToNacosException() throws Exception {
        ReflectionUtil.setFieldValue(nacosRegistryService, "useSLBWay", true);

        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn("MOCKED_CLUSTER_B");
        when(mockedNamingMaintainService.queryService(anyString(), anyString())).thenThrow(new NacosException());

        assertThrows(NacosException.class, () -> nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY));
    }

    @Test
    public void shouldThrowsExceptionWhenLookupFailsDueToInvalidPublicPort() throws Exception {
        ReflectionUtil.setFieldValue(nacosRegistryService, "useSLBWay", true);
        String mockedPublicIp = "127.0.0.1";
        String invalidPort = "";

        Service mockedService = new Service();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("publicIp", mockedPublicIp);
        metadata.put("publicPort", invalidPort);
        mockedService.setMetadata(metadata);

        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn("MOCKED_CLUSTER_C");
        when(mockedNamingMaintainService.queryService(anyString(), anyString())).thenReturn(mockedService);

        assertThrows(Exception.class, () -> nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY));
    }

    @Test
    public void shouldAddClusterWhenNotExistInListenerMap() throws Exception {
        List<Instance> mockedHealthyInstances = Collections.singletonList(instance("127.0.0.1", 8091, true, true));

        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn("MOCKED_CLUSTER_D");
        when(mockedNamingService.getAllInstances(anyString(), anyString(), anyList()))
                .thenReturn(mockedHealthyInstances);

        nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY);

        List<InetSocketAddress> expected = Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091));

        ConcurrentMap<String, List<InetSocketAddress>> actualClusterAddrMap =
                ReflectionUtil.getFieldValue(nacosRegistryService, "CLUSTER_ADDRESS_MAP");
        assertEquals(expected, actualClusterAddrMap.get("MOCKED_CLUSTER_D"));
    }

    @Test
    public void shouldSubscribeClusterWhenNotExistInListenerMap() throws Exception {
        List<Instance> mockedHealthyInstances = Collections.singletonList(instance("127.0.0.1", 8091, true, true));

        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn("MOCKED_CLUSTER_E");
        when(mockedNamingService.getAllInstances(anyString(), anyString(), anyList()))
                .thenReturn(mockedHealthyInstances);

        nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY);

        EventListener capturedEventListener = captureSubscribedListener("MOCKED_CLUSTER_E");

        NamingEvent namingEvent = mock(NamingEvent.class);
        List<Instance> newInstances =
                Arrays.asList(instance("127.0.0.1", 8092, true, true), instance("10.0.0.1", 8093, false, true));
        when(namingEvent.getInstances()).thenReturn(newInstances);
        capturedEventListener.onEvent(namingEvent);

        ConcurrentMap<String, List<InetSocketAddress>> actualClusterAddrMap =
                ReflectionUtil.getFieldValue(nacosRegistryService, "CLUSTER_ADDRESS_MAP");
        List<InetSocketAddress> expected = Collections.singletonList(new InetSocketAddress("127.0.0.1", 8092));
        assertEquals(expected, actualClusterAddrMap.get("MOCKED_CLUSTER_E"));
    }

    @Test
    public void shouldNotOverwriteClusterWhenEventHasEmptyInstanceList() throws Exception {
        // prepare existing entry
        String cluster = "MOCKED_CLUSTER_EMPTY";
        List<InetSocketAddress> existing = Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091));
        ConcurrentMap<String, List<InetSocketAddress>> clusterMap =
                ReflectionUtil.getFieldValue(nacosRegistryService, "CLUSTER_ADDRESS_MAP");
        clusterMap.put(cluster, existing);

        List<Instance> initial = Collections.singletonList(instance("127.0.0.1", 8091, true, true));

        when(mockedRegisterServiceConfiguration.getConfig("service.vgroupMapping." + NACOS_TX_SERVICE_GROUP_KEY))
                .thenReturn(cluster);
        when(mockedNamingService.getAllInstances(anyString(), anyString(), anyList()))
                .thenReturn(initial);

        nacosRegistryService.lookup(NACOS_TX_SERVICE_GROUP_KEY);

        EventListener listener = captureSubscribedListener(cluster);

        NamingEvent emptyEvent = mock(NamingEvent.class);
        when(emptyEvent.getInstances()).thenReturn(Collections.emptyList());

        listener.onEvent(emptyEvent);

        assertEquals(existing, clusterMap.get(cluster));
    }

    // Helper to create Instance with desired properties
    private Instance instance(String ip, int port, boolean enabled, boolean healthy) {
        Instance ins = new Instance();
        ins.setIp(ip);
        ins.setPort(port);
        ins.setEnabled(enabled);
        ins.setHealthy(healthy);
        return ins;
    }

    // Helper to capture subscribed EventListener for a cluster name
    private EventListener captureSubscribedListener(String expectedCluster) throws NacosException {
        ArgumentCaptor<EventListener> eventListenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(mockedNamingService)
                .subscribe(
                        eq(NACOS_MOCKED_APPLICATION),
                        eq(NACOS_MOCKED_GROUP),
                        eq(Collections.singletonList(expectedCluster)),
                        eventListenerCaptor.capture());
        return eventListenerCaptor.getValue();
    }
}
