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

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.HttpClientUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Test for NamingserverRegistryServiceImpl
 * The @Disable annotation method requires local startup of namingserver for testing
 */
class NamingserverRegistryServiceImplTest {
    private final NamingserverRegistryServiceImpl registryService = NamingserverRegistryServiceImpl.getInstance();

    @BeforeAll
    public static void beforeClass() {
        // set the global instance information for the register
        Instance instance = Instance.getInstance();
        instance.setClusterName("cluster1");
        instance.setUnit("unit1");
        instance.setNamespace("dev");
        instance.setTransaction(new Node.Endpoint("127.0.0.1", 8888));
        instance.setControl(new Node.Endpoint("127.0.0.1", 8888));

        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(
                "group1",
                "unit1"); // vGroup -> unitName, namingserver automatically adds transaction groups based on it
        instance.addMetadata("vGroup", vGroups);

        System.setProperty("registry.seata.namespace", "dev");
        System.setProperty("registry.seata.cluster", "cluster1");
        System.setProperty("registry.seata.server-addr", "127.0.0.1:8081");

        System.setProperty("registry.seata.username", "seata");
        System.setProperty("registry.seata.password", "seata");

        // Set a smaller metadataMaxAgeMs for testing
        System.setProperty("registry.seata.metadataMaxAgeMs", "1000");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // Get the application environment
        ConfigurableEnvironment environment = context.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        Properties customProperties = new Properties();
        customProperties.setProperty("seata.registry.namingserver.server-addr[0]", "127.0.0.1:8081");

        PropertiesPropertySource customPropertySource = new PropertiesPropertySource("customSource", customProperties);
        propertySources.addLast(customPropertySource);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
    }

    @AfterAll
    public static void afterClass() {
        System.clearProperty("registry.seata.namespace");
        System.clearProperty("registry.seata.cluster");
        System.clearProperty("registry.seata.server-addr");
        System.clearProperty("registry.seata.username");
        System.clearProperty("registry.seata.password");
        System.clearProperty("registry.seata.metadataMaxAgeMs");
    }

    @Test
    public void testWatchCoversRefreshToken() throws Exception {
        NamingserverRegistryServiceImpl spyService = Mockito.spy(NamingserverRegistryServiceImpl.getInstance());
        doReturn("127.0.0.1:8081").when(spyService).getNamingAddr();

        ResponseBody body = ResponseBody.create("", MediaType.get("application/json"));

        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        try (MockedStatic<HttpClientUtil> mockedStatic = mockStatic(HttpClientUtil.class)) {

            mockedStatic
                    .when(() -> HttpClientUtil.doPost(anyString(), anyString(), anyMap(), anyInt()))
                    .thenReturn(mockResponse);

            spyService.watch("testGroup");
        }
    }

    @Test
    public void testGetNamingAddrs() throws Exception {
        Method getNamingAddrsMethod = NamingserverRegistryServiceImpl.class.getDeclaredMethod("getNamingAddrs");
        getNamingAddrsMethod.setAccessible(true);

        List<String> list = (List<String>) getNamingAddrsMethod.invoke(registryService);
        assertEquals(list.size(), 1);
    }

    @Test
    @Disabled
    public void testGetNamingAddr() throws Exception {
        Method getNamingAddrMethod = NamingserverRegistryServiceImpl.class.getDeclaredMethod("getNamingAddr");
        getNamingAddrMethod.setAccessible(true);

        String addr = (String) getNamingAddrMethod.invoke(registryService);
        assertEquals(addr, "127.0.0.1:8081");
    }

    @Test
    @Disabled
    public void testRegisterAndUnregister() throws Exception {
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8888));

        // The ServiceInstance parameter here has no effect and is only used for assertion testing.
        // In fact, register is registered by calling Instance.getInstance() of the global singleton.
        registryService.register(serviceInstance);

        List<ServiceInstance> list = registryService.lookup("group1");

        assertEquals(1, list.size());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("vGroup", Instance.getInstance().getMetadata().get("vGroup"));
        serviceInstance.setMetadata(metadata);

        assertEquals(list.get(0), serviceInstance);

        registryService.unregister(serviceInstance);
    }

    @Test
    @Disabled
    public void testRegister_withMetadata() throws Exception {
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8888));
        Instance.getInstance().addMetadata("key1", "value1");
        Instance.getInstance().addMetadata("key2", Collections.singletonMap("subKey", "subValue"));

        registryService.register(serviceInstance);

        List<ServiceInstance> list = registryService.lookup("group1");

        assertEquals(1, list.size());
        Map<String, Object> metadata = Instance.getInstance().getMetadata();
        metadata.put("vGroup", Instance.getInstance().getMetadata().get("vGroup"));
        serviceInstance.setMetadata(metadata);

        assertEquals(list.get(0), serviceInstance);

        registryService.unregister(serviceInstance);
    }

    @Test
    @Disabled
    public void testWatch() throws Exception {
        // 1. registering an instance
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress("127.0.0.1", 8888));
        registryService.register(serviceInstance);
        Thread.sleep(1000);

        // 2. test for no changes: should return 304
        boolean result1 = registryService.watch("group1");
        assertFalse(result1);

        // 3. triggering data changes: Re-registering instances
        registryService.unregister(serviceInstance);
        Thread.sleep(1000);

        // set a new term value
        Instance instance = Instance.getInstance();
        instance.setTerm(System.currentTimeMillis());
        registryService.register(serviceInstance);
        Thread.sleep(1000);

        // 4. test for changes: simulate the client using the old term, and return 200
        Field termField = NamingserverRegistryServiceImpl.class.getDeclaredField("term");
        termField.setAccessible(true);
        termField.set(registryService, 0L);

        boolean result2 = registryService.watch("group1");
        assertTrue(result2);

        reflectUnsubscribe("group1");
    }

    @Test
    public void testUnsubscribe() throws Exception {
        NamingListenerimpl namingListenerimpl = new NamingListenerimpl();

        registryService.subscribe(namingListenerimpl, "group3");

        registryService.unsubscribe(namingListenerimpl, "group3");

        Thread.sleep(2000);

        assertEquals(namingListenerimpl.isNotified, false);
    }

    @Test
    public void testAliveLookup() {
        String transactionServiceGroup = "test-group";

        List<ServiceInstance> result = registryService.aliveLookup(transactionServiceGroup);
        assertEquals(0, result.size());
    }

    private void reflectUnsubscribe(String vGroup) throws Exception {
        Field listenerServiceMapField = NamingserverRegistryServiceImpl.class.getDeclaredField("LISTENER_SERVICE_MAP");
        listenerServiceMapField.setAccessible(true);
        ConcurrentMap<String, List<NamingListener>> listenerServiceMap =
                (ConcurrentMap<String, List<NamingListener>>) listenerServiceMapField.get(null);
        listenerServiceMap.remove(vGroup);
    }

    @Test
    void testDoHealthCheck_success() {
        ResponseBody body = ResponseBody.create("", MediaType.get("application/json"));

        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        try (MockedStatic<HttpClientUtil> mockedStatic = mockStatic(HttpClientUtil.class)) {

            mockedStatic
                    .when(() -> HttpClientUtil.doGet(any(), any(), any(), anyInt()))
                    .thenReturn(mockResponse);

            NamingserverRegistryServiceImpl service =
                    mock(NamingserverRegistryServiceImpl.class, Answers.CALLS_REAL_METHODS);

            boolean result = service.doHealthCheck("127.0.0.1:8080");

            assertTrue(result);
        }
    }

    @Test
    void testUnregister_success() {
        ResponseBody body = ResponseBody.create("", MediaType.get("application/json"));

        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        try (MockedStatic<HttpClientUtil> mockedStatic = mockStatic(HttpClientUtil.class)) {

            mockedStatic
                    .when(() -> HttpClientUtil.doPost(any(), anyString(), any(), anyInt()))
                    .thenReturn(mockResponse);

            NamingserverRegistryServiceImpl service =
                    mock(NamingserverRegistryServiceImpl.class, Answers.CALLS_REAL_METHODS);

            service.unregister(new ServiceInstance(Instance.getInstance()));
        }
    }

    @Test
    void testRefreshGroup_withResponse() throws RetryableException, IOException {
        NamingserverRegistryServiceImpl spyService = Mockito.spy(NamingserverRegistryServiceImpl.getInstance());
        doReturn("127.0.0.1:8081").when(spyService).getNamingAddr();

        ResponseBody body = ResponseBody.create("{\"clusterList\":[],\"term\":1}", MediaType.get("application/json"));

        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        try (MockedStatic<HttpClientUtil> mockedStatic = mockStatic(HttpClientUtil.class)) {

            mockedStatic
                    .when(() -> HttpClientUtil.doGet(any(), any(), any(), anyInt()))
                    .thenReturn(mockResponse);

            spyService.refreshGroup("testGroup");
        }
    }

    @Test
    void testRefreshGroup_withNoBody() throws RetryableException, IOException {
        NamingserverRegistryServiceImpl spyService = Mockito.spy(NamingserverRegistryServiceImpl.getInstance());
        doReturn("127.0.0.1:8081").when(spyService).getNamingAddr();

        ResponseBody body = ResponseBody.create("", MediaType.get("application/json"));

        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        try (MockedStatic<HttpClientUtil> mockedStatic = mockStatic(HttpClientUtil.class)) {

            mockedStatic
                    .when(() -> HttpClientUtil.doGet(any(), any(), any(), anyInt()))
                    .thenReturn(mockResponse);

            Assertions.assertThrows(IOException.class, () -> spyService.refreshGroup("testGroup"));
        }
    }

    @Test
    void testRefreshGroup_withNoResponse() throws RetryableException, IOException {
        NamingserverRegistryServiceImpl spyService = Mockito.spy(NamingserverRegistryServiceImpl.getInstance());
        doReturn("127.0.0.1:8081").when(spyService).getNamingAddr();

        ResponseBody body = ResponseBody.create("", MediaType.get("application/json"));

        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        try (MockedStatic<HttpClientUtil> mockedStatic = mockStatic(HttpClientUtil.class)) {
            mockedStatic
                    .when(() -> HttpClientUtil.doGet(any(), any(), any(), anyInt()))
                    .thenReturn(null);

            Assertions.assertThrows(NamingRegistryException.class, () -> spyService.refreshGroup("testGroup"));
        }
    }

    private static class NamingListenerimpl implements NamingListener {

        public boolean isNotified = false;

        public boolean isNotified() {
            return isNotified;
        }

        public void setNotified(boolean notified) {
            isNotified = notified;
        }

        @Override
        public void onEvent(String vGroup) {
            isNotified = true;
        }
    }
}
