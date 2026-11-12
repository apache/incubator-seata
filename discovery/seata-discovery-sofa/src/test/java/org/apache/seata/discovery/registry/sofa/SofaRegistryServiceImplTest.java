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
package org.apache.seata.discovery.registry.sofa;

import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.SubscriberDataObserver;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SofaRegistryServiceImplTest {
    private static final String GROUP = "sofa-unit-group";
    private final Map<String, String> savedProperties = new HashMap<>();
    private final List<RegistryClientConfig> configs = new ArrayList<>();
    private MockedConstruction<DefaultRegistryClient> clients;
    private MockedStatic<ConfigurationFactory> configurations;
    private Configuration configuration;

    private final Map<String, Object> savedState = new HashMap<>();
    private final Map<String, Map> savedMaps = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        savedProperties.put("config.type", System.getProperty("config.type"));
        System.setProperty("config.type", "file");
        savedProperties.put("config.file.name", System.getProperty("config.file.name"));
        System.setProperty("config.file.name", "file.conf");
        for (String key : Arrays.asList(
                "serverAddr", "region", "datacenter", "group", "application", "cluster", "addressWaitTime")) {
            String name = "registry.sofa." + key;
            savedProperties.put(name, System.getProperty(name));
            System.clearProperty(name);
        }
        System.setProperty("registry.sofa.serverAddr", "127.0.0.1:9603");
        System.setProperty("registry.sofa.addressWaitTime", "0");
        {
            Field field = SofaRegistryServiceImpl.class.getDeclaredField("instance");
            field.setAccessible(true);
            savedState.put("instance", field.get(null));
        }
        {
            Field field = SofaRegistryServiceImpl.class.getDeclaredField("registryClient");
            field.setAccessible(true);
            savedState.put("registryClient", field.get(null));
        }
        {
            Field field = SofaRegistryServiceImpl.class.getDeclaredField("registryProps");
            field.setAccessible(true);
            savedState.put("registryProps", field.get(null));
        }
        for (String key : Arrays.asList("LISTENER_SERVICE_MAP", "CLUSTER_ADDRESS_MAP")) {
            savedMaps.put(key, new HashMap(map(key)));
        }
        set("instance", null);
        set("registryClient", null);
        map("LISTENER_SERVICE_MAP").clear();
        map("CLUSTER_ADDRESS_MAP").clear();
        configuration = mock(Configuration.class);
        configurations = mockStatic(ConfigurationFactory.class);
        configurations.when(ConfigurationFactory::getInstance).thenReturn(configuration);
        when(configuration.getConfig("service.vgroupMapping." + GROUP)).thenReturn("default");
        clients = mockConstruction(
                DefaultRegistryClient.class,
                (client, context) ->
                        configs.add((RegistryClientConfig) context.arguments().get(0)));
    }

    @AfterEach
    void tearDown() throws Exception {
        set("instance", null);
        set("registryClient", null);
        set("registryProps", null);
        map("LISTENER_SERVICE_MAP").clear();
        map("CLUSTER_ADDRESS_MAP").clear();
        for (Map.Entry<String, Object> entry : savedState.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map> entry : savedMaps.entrySet()) {
            map(entry.getKey()).putAll(entry.getValue());
        }
        RegistryService.CURRENT_ADDRESS_MAP.remove(GROUP);
        RegistryService.SERVICE_GROUP_NAME.remove("service.vgroupMapping." + GROUP);
        if (clients != null) {
            clients.close();
        }
        if (configurations != null) {
            configurations.close();
        }
        savedProperties.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    @Test
    void registersAndUnregistersWithDefaultClientConfiguration() throws Exception {
        SofaRegistryServiceImpl service = SofaRegistryServiceImpl.getInstance();
        assertSame(service, SofaRegistryServiceImpl.getInstance());
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        service.register(address);
        DefaultRegistryClient client = clients.constructed().get(0);
        verify(client).init();
        ArgumentCaptor<PublisherRegistration> registration = ArgumentCaptor.forClass(PublisherRegistration.class);
        verify(client).register(registration.capture(), eq("127.0.0.1:8091"));
        assertEquals("default", registration.getValue().getDataId());
        assertEquals("SEATA_GROUP", registration.getValue().getGroup());
        assertEquals("127.0.0.1", configs.get(0).getRegistryEndpoint());
        assertEquals(9603, configs.get(0).getRegistryEndpointPort());
        assertEquals("default", configs.get(0).getAppName());
        service.unregister(address);
        verify(client).unregister("default", "SEATA_GROUP", RegistryType.PUBLISHER);
        assertEquals(1, clients.constructed().size());
        service.close();
    }

    @Test
    void honorsExplicitConfigurationAndSubscriberRegistration() throws Exception {
        System.setProperty("registry.sofa.region", "zone-a");
        System.setProperty("registry.sofa.datacenter", "dc-a");
        System.setProperty("registry.sofa.group", "custom-group");
        System.setProperty("registry.sofa.cluster", "custom-cluster");
        System.setProperty("registry.sofa.application", "custom-app");
        SofaRegistryServiceImpl service = SofaRegistryServiceImpl.getInstance();
        SubscriberDataObserver observer = mock(SubscriberDataObserver.class);
        service.subscribe("custom-cluster", observer);
        DefaultRegistryClient client = clients.constructed().get(0);
        ArgumentCaptor<SubscriberRegistration> registration = ArgumentCaptor.forClass(SubscriberRegistration.class);
        verify(client).register(registration.capture());
        assertSame(observer, registration.getValue().getSubscriberDataObserver());
        assertEquals(ScopeEnum.global, registration.getValue().getScopeEnum());
        assertEquals("custom-group", registration.getValue().getGroup());
        assertEquals("custom-cluster", registration.getValue().getDataId());
        assertEquals("zone-a", configs.get(0).getZone());
        assertEquals("dc-a", configs.get(0).getDataCenter());
        assertEquals("custom-app", configs.get(0).getAppName());
        service.unsubscribe("custom-cluster", observer);
        verify(client).unregister("custom-cluster", "custom-group", RegistryType.SUBSCRIBER);
    }

    @Test
    void lookupFlattensZonesUpdatesCacheAndRemovesMissingCluster() throws Exception {
        SofaRegistryServiceImpl service = SofaRegistryServiceImpl.getInstance();
        assertNull(service.lookup(GROUP));
        DefaultRegistryClient client = clients.constructed().get(0);
        ArgumentCaptor<SubscriberRegistration> registration = ArgumentCaptor.forClass(SubscriberRegistration.class);
        verify(client).register(registration.capture());
        SubscriberDataObserver observer = registration.getValue().getSubscriberDataObserver();
        UserData data = mock(UserData.class);
        Map<String, List<String>> zones = new LinkedHashMap<>();
        zones.put("zone-a", Arrays.asList("127.0.0.1:8091", "127.0.0.2:8092"));
        zones.put("zone-b", Collections.singletonList("127.0.0.3:8093"));
        when(data.getZoneData()).thenReturn(zones);
        observer.handleData("default", data);
        assertEquals(
                Arrays.asList(
                        new InetSocketAddress("127.0.0.1", 8091),
                        new InetSocketAddress("127.0.0.2", 8092),
                        new InetSocketAddress("127.0.0.3", 8093)),
                service.lookup(GROUP));
        verify(client, times(1)).register(any(SubscriberRegistration.class));
        when(data.getZoneData()).thenReturn(Collections.emptyMap());
        observer.handleData("default", data);
        assertTrue(service.lookup(GROUP).isEmpty());
        when(data.getZoneData()).thenReturn(null);
        observer.handleData("default", data);
        assertNull(service.lookup(GROUP));
    }

    @Test
    void rejectsMissingMappingAndInvalidAddressesWithoutStartingClient() throws Exception {
        SofaRegistryServiceImpl service = SofaRegistryServiceImpl.getInstance();
        when(configuration.getConfig("service.vgroupMapping." + GROUP)).thenReturn(null);
        assertThrows(ConfigNotFoundException.class, () -> service.lookup(GROUP));
        assertThrows(IllegalArgumentException.class, () -> service.register(new InetSocketAddress("127.0.0.1", 0)));
        assertThrows(IllegalArgumentException.class, () -> service.unregister(new InetSocketAddress("127.0.0.1", 0)));
        assertTrue(clients.constructed().isEmpty());
    }

    @Test
    void fallsBackToFileConfigurationForAddressAndWaitTime() throws Exception {
        System.clearProperty("registry.sofa.serverAddr");
        System.clearProperty("registry.sofa.addressWaitTime");
        assertNotNull(SofaRegistryServiceImpl.getInstance());
        Field field = SofaRegistryServiceImpl.class.getDeclaredField("registryProps");
        field.setAccessible(true);
        Properties properties = (Properties) field.get(null);
        String configuredWait = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig("registry.sofa.addressWaitTime");
        assertEquals(configuredWait == null ? "3000" : configuredWait, properties.getProperty("addressWaitTime"));
        assertEquals(
                ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig("registry.sofa.serverAddr"),
                properties.getProperty("serverAddr"));
        assertEquals("default", properties.getProperty("cluster"));
    }

    private static Map map(String name) throws Exception {
        Field field = SofaRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map) field.get(null);
    }

    private static void set(String name, Object value) throws Exception {
        Field field = SofaRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
