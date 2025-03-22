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

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;

import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.nacos.NacosConfiguration;
import org.apache.seata.discovery.registry.RegistryService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


/**
 * The type Nacos registry serivce impl test
 *
 */
public class NacosRegistryServiceImplTest {

    @InjectMocks
    private NacosRegistryServiceImpl registryService;

    @Mock
    private NamingService namingService;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8091;
    private static final String SERVICE_NAME = "seata-server";
    private static final String GROUP_NAME = "DEFAULT_GROUP";

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
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
    public void testRegister() throws Exception {
        RegistryService registryService = mock(NacosRegistryServiceImpl.class);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);

        Instance instance = new Instance();
        instance.setIp(HOST);
        instance.setPort(PORT);
        List<Instance> instanceList = Collections.singletonList(instance);

        when(namingService.getAllInstances(SERVICE_NAME, GROUP_NAME)).thenReturn(instanceList);

        registryService.register(inetSocketAddress);

        verify(registryService).register(inetSocketAddress);

        List<Instance> instances = namingService.getAllInstances(SERVICE_NAME, GROUP_NAME);
        long count = instances.stream()
                .filter(i -> HOST.equals(i.getIp()) && PORT == i.getPort())
                .count();

        assertEquals(1, count);
    }

    @Test
    public void testUnregister() throws Exception {
        RegistryService registryService = mock(NacosRegistryServiceImpl.class);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);

        // mocks behavior of deregister
        when(namingService.getAllInstances(SERVICE_NAME, GROUP_NAME)).thenReturn(Collections.emptyList());

        registryService.unregister(inetSocketAddress);
        verify(registryService).unregister(inetSocketAddress);

        List<Instance> instances = namingService.getAllInstances(SERVICE_NAME, GROUP_NAME);
        long count = instances.stream()
                .filter(i -> HOST.equals(i.getIp()) && PORT == i.getPort())
                .count();

        assertEquals(0, count);
    }

    @Test
    public void testSubscribe() throws Exception {
        RegistryService registryService = mock(NacosRegistryServiceImpl.class);
        EventListener eventListener = mock(EventListener.class);
        registryService.subscribe("test", eventListener);
        verify(registryService).subscribe("test", eventListener);
    }

    @Test
    public void testLookup() throws Exception {
        RegistryService registryService = mock(NacosRegistryServiceImpl.class);
        registryService.lookup("test-key");
        verify(registryService).lookup("test-key");
    }
}
