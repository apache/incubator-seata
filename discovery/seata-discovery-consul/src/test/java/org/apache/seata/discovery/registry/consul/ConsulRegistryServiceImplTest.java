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

import com.ecwid.consul.transport.RawResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ConsulRegistryServiceImplTest {

    final String TEST_CLUSTER_NAME = "testCluster";

    ConsulClient client;
    Configuration configuration;
    ConsulRegistryServiceImpl service;

    @BeforeEach
    public void init() throws Exception {
        service = (ConsulRegistryServiceImpl) new ConsulRegistryProvider().provide();
        client = mock(ConsulClient.class);
        this.setClient(service, client);

        configuration = mock(Configuration.class);
    }

    @Test
    public void testGetInstance() {
        Assertions.assertEquals(ConsulRegistryServiceImpl.getInstance(), service);
    }

    @Test
    public void testRegister() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8080);

        when(client.agentServiceRegister(any())).thenReturn(null);
        service.register(inetSocketAddress);
        verify(client).agentServiceRegister(any(), any());

        when(client.agentServiceDeregister(any())).thenReturn(null);
        service.unregister(inetSocketAddress);
        verify(client).agentServiceDeregister(any(), any());
    }

    @Test
    public void testSubscribeAndLookup() throws Exception {
        ConsulListener consulListener = mock(ConsulListener.class);

        ExecutorService executorService = mock(ExecutorService.class);
        setExecutorService(executorService);

        Response<List<HealthService>> response = new Response<>(new ArrayList<>(), mock(RawResponse.class));
        when(client.getHealthServices(any(), any())).thenReturn(response);

        service.subscribe(TEST_CLUSTER_NAME, consulListener);

        Assertions.assertNotNull(getMap("listenerMap").get(TEST_CLUSTER_NAME));
        Assertions.assertNotNull(getMap("notifiers").get(TEST_CLUSTER_NAME));
        verify(executorService).submit(any(Runnable.class));

        try (MockedStatic<ConfigurationFactory> configurationFactoryMockedStatic = Mockito.mockStatic(ConfigurationFactory.class)) {
            configurationFactoryMockedStatic.when(ConfigurationFactory::getInstance).thenReturn(configuration);

            // normal condition
            when(configuration.getConfig(any())).thenReturn(TEST_CLUSTER_NAME);
            List<InetSocketAddress> addresses = new ArrayList<>();
            getMap("clusterAddressMap").put(TEST_CLUSTER_NAME, addresses);
            Assertions.assertEquals(addresses, service.lookup("testGroup"));

            // when config exc
            when(configuration.getConfig(any())).thenReturn(null);
            Assertions.assertThrows(ConfigNotFoundException.class, () -> {
                service.lookup("testGroup");
            });
        }

        service.unsubscribe(TEST_CLUSTER_NAME, consulListener);
        Assertions.assertNull(getMap("notifiers").get(TEST_CLUSTER_NAME));
    }


    private void setClient(ConsulRegistryServiceImpl service, ConsulClient client) throws Exception {
        Field clientField = ConsulRegistryServiceImpl.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(service, client);
    }

    private void setExecutorService(ExecutorService executorService) throws Exception {
        Field executorServiceField = ConsulRegistryServiceImpl.class.getDeclaredField("notifierExecutor");
        executorServiceField.setAccessible(true);
        executorServiceField.set(service, executorService);
    }

    private <K, V> ConcurrentMap<K, V> getMap(String name) throws Exception {
        Field notifiersField = ConsulRegistryServiceImpl.class.getDeclaredField(name);
        notifiersField.setAccessible(true);
        return (ConcurrentMap<K, V>) notifiersField.get(service);
    }
}
