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

import okhttp3.*;
import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.metadata.*;
import org.apache.seata.common.metadata.namingserver.*;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NamingserverRegistryServiceImplUnitTest {
    private static final String GROUP = "naming-unit-group";
    private static final String HOST = "127.0.0.1:18848";
    private static final Map<String, String> PROPERTIES = new HashMap<>();
    private NamingserverRegistryServiceImpl service;
    private MockedStatic<HttpClientUtil> http;
    private Object previousToken;
    private Object previousTimestamp;
    private final Map<String, Map> previousMaps = new HashMap<>();

    @BeforeAll
    static void configure() {
        property("username", "seata");
        property("password", "seata");
        property("server-addr", HOST);
        property("namespace", "unit-ns");
        property("metadataMaxAgeMs", "30000");
    }

    @AfterAll
    static void restoreProperties() {
        PROPERTIES.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        property("server-addr", HOST);
        property("namespace", "unit-ns");
        property("metadataMaxAgeMs", "30000");
        service = mock(NamingserverRegistryServiceImpl.class, CALLS_REAL_METHODS);
        previousToken = get("jwtToken", null);
        previousTimestamp = get("tokenTimeStamp", null);
        set("jwtToken", null, "unit-token");
        set("tokenTimeStamp", null, System.currentTimeMillis());
        for (String key : Arrays.asList("AVAILABLE_NAMINGSERVER_MAP", "VGROUP_ADDRESS_MAP", "LISTENER_SERVICE_MAP")) {
            Map map = (Map) get(key, null);
            previousMaps.put(key, new HashMap(map));
            map.clear();
        }
        available().put(HOST, new AtomicInteger(0));
        http = mockStatic(HttpClientUtil.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        service.unsubscribe(GROUP);
        ((ExecutorService) get("NOTIFIER_EXECUTOR", null)).submit(() -> {}).get(5, TimeUnit.SECONDS);
        http.close();
        set("jwtToken", null, previousToken);
        set("tokenTimeStamp", null, previousTimestamp);
        for (Map.Entry<String, Map> entry : previousMaps.entrySet()) {
            Map map = (Map) get(entry.getKey(), null);
            map.clear();
            map.putAll(entry.getValue());
        }
        RegistryService.CURRENT_ADDRESS_MAP.remove(GROUP);
    }

    @Test
    void selectsOnlyHealthyNodesAndCachesSelection() throws Exception {
        available().put("unhealthy:80", new AtomicInteger(1));
        assertEquals(HOST, service.getNamingAddr());
        available().get(HOST).set(1);
        assertEquals(HOST, service.getNamingAddr());
        set("namingServerAddressCache", service, null);
        assertThrows(NamingRegistryException.class, service::getNamingAddr);
        available().get(HOST).set(0);
        assertEquals(HOST, service.getNamingAddr());
    }

    @Test
    void validatesConfiguredAddressesAndNamespace() {
        assertEquals(Collections.singletonList(HOST), service.getNamingAddrs());
        property("server-addr", HOST + ",127.0.0.2:18848");
        assertEquals(2, service.getNamingAddrs().size());
        property("server-addr", " ");
        assertThrows(NamingRegistryException.class, service::getNamingAddrs);
        assertEquals("unit-ns", service.getNamespace());
        property("namespace", " ");
        assertEquals("public", service.getNamespace());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    void registersAndUnregistersWithExpectedBodyAndHeaders(int status) throws Exception {
        Instance instance = newInstance();
        instance.setNamespace("unit-ns");
        instance.setClusterName("cluster-a");
        instance.setUnit("unit-a");
        http.when(() -> HttpClientUtil.doPost(anyString(), anyString(), anyMap(), eq(3000)))
                .thenAnswer(invocation -> response(status, "{}"));
        service.register(instance);
        assertTrue(instance.getTimestamp() > 0);
        http.verify(() -> HttpClientUtil.doPost(
                eq("http://" + HOST + "/naming/v1/register?namespace=unit-ns&clusterName=cluster-a&unit=unit-a"),
                argThat((String body) -> body.contains("cluster-a")),
                argThat((Map<String, String> headers) -> "unit-token".equals(headers.get("Authorization"))
                        && "application/json".equals(headers.get("Content-Type"))),
                eq(3000)));
        service.unregister(instance);
        http.verify(() -> HttpClientUtil.doPost(
                eq("http://" + HOST + "/naming/v1/unregister?unit=unit-a&clusterName=cluster-a&namespace=unit-ns"),
                anyString(),
                anyMap(),
                eq(3000)));
    }

    @Test
    void skipsUnhealthyRegistrationAndToleratesTransportFailure() throws Exception {
        Instance instance = newInstance();
        available().get(HOST).set(1);
        service.doRegister(instance, Collections.singletonList(HOST));
        http.verifyNoInteractions();
        available().get(HOST).set(0);
        set("jwtToken", null, "");
        http.when(() -> HttpClientUtil.doPost(anyString(), anyString(), anyMap(), eq(3000)))
                .thenThrow(new IOException("offline"));
        assertDoesNotThrow(() -> service.register(instance));
        assertDoesNotThrow(() -> service.unregister(instance));
        http.verify(() -> HttpClientUtil.doPost(
                contains("/register?"),
                anyString(),
                argThat((Map<String, String> headers) -> !headers.containsKey("Authorization")),
                eq(3000)));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    void healthCheckReportsHttpStatus(int status) {
        http.when(() -> HttpClientUtil.doGet(anyString(), isNull(), anyMap(), eq(3000)))
                .thenReturn(response(status, "{}"));
        assertEquals(status == 200, service.doHealthCheck(HOST));
    }

    @Test
    void healthCheckHandlesTransportFailureAndHealthTransitions() throws Exception {
        http.when(() -> HttpClientUtil.doGet(anyString(), isNull(), anyMap(), eq(3000)))
                .thenThrow(new IOException("offline"));
        assertFalse(service.doHealthCheck(HOST));
        java.lang.reflect.Method check =
                NamingserverRegistryServiceImpl.class.getDeclaredMethod("checkAvailableNamingAddr", List.class);
        check.setAccessible(true);
        check.invoke(service, Collections.singletonList(HOST));
        assertEquals(1, available().get(HOST).get());
        http.when(() -> HttpClientUtil.doGet(anyString(), isNull(), anyMap(), eq(3000)))
                .thenReturn(response(200, "{}"));
        check.invoke(service, Collections.singletonList(HOST));
        assertEquals(0, available().get(HOST).get());
    }

    @Test
    void refreshesExpiredTokenAndUsesItForWatch() throws Exception {
        set("tokenTimeStamp", null, -1L);
        http.when(() -> HttpClientUtil.doPost(
                        eq("http://" + HOST + "/api/v1/auth/login"), anyMap(), anyMap(), eq(1000)))
                .thenReturn(response(200, "{\"code\":200,\"data\":\"fresh-token\"}"));
        http.when(() -> HttpClientUtil.doPost(contains("/naming/v1/watch?"), isNull(String.class), anyMap(), eq(30000)))
                .thenReturn(response(200, "{}"));
        assertTrue(service.watch(GROUP));
        assertEquals("fresh-token", NamingserverRegistryServiceImpl.jwtToken);
        assertTrue((Long) get("tokenTimeStamp", null) > 0);
        http.verify(() -> HttpClientUtil.doPost(
                contains("vGroup=" + GROUP + "&clientTerm=0&timeout=28000"),
                isNull(String.class),
                argThat((Map<String, String> headers) -> "fresh-token".equals(headers.get("Authorization"))),
                eq(30000)));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 200})
    void rejectsAuthenticationFailures(int status) throws Exception {
        set("tokenTimeStamp", null, 0L);
        http.when(() -> HttpClientUtil.doPost(contains("/api/v1/auth/login"), anyMap(), anyMap(), eq(1000)))
                .thenReturn(response(status, "{\"code\":401,\"data\":\"denied\"}"));
        assertThrows(AuthenticationFailedException.class, () -> service.watch(GROUP));
    }

    @Test
    void handlesMissingLoginBodyAndRetryableIoFailure() throws Exception {
        set("tokenTimeStamp", null, -1L);
        Response noBody = mock(Response.class);
        when(noBody.code()).thenReturn(200);
        http.when(() -> HttpClientUtil.doPost(contains("/api/v1/auth/login"), anyMap(), anyMap(), eq(1000)))
                .thenReturn(noBody);
        assertThrows(AuthenticationFailedException.class, () -> service.watch(GROUP));
        verify(noBody).close();
        http.when(() -> HttpClientUtil.doPost(contains("/api/v1/auth/login"), anyMap(), anyMap(), eq(1000)))
                .thenThrow(new IOException("login offline"));
        assertThrows(RetryableException.class, () -> service.watch(GROUP));
    }

    @Test
    void watchHandlesNoChangeNullResponseAndIoFailure() throws Exception {
        set("jwtToken", null, "");
        http.when(() -> HttpClientUtil.doPost(anyString(), isNull(String.class), anyMap(), eq(30000)))
                .thenReturn(response(304, ""), null)
                .thenThrow(new IOException("watch offline"));
        assertFalse(service.watch(GROUP));
        assertFalse(service.watch(GROUP));
        Thread.currentThread().interrupt();
        try {
            assertFalse(service.watch(GROUP));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void refreshGroupValidatesResponseAndPropagatesTransportFailure() throws Exception {
        http.when(() -> HttpClientUtil.doGet(contains("/naming/v1/discovery"), anyMap(), anyMap(), eq(3000)))
                .thenReturn(null, response(500, "error"));
        assertThrows(NamingRegistryException.class, () -> service.refreshGroup(GROUP));
        assertThrows(NamingRegistryException.class, () -> service.refreshGroup(GROUP));
        Response noBody = mock(Response.class);
        when(noBody.code()).thenReturn(200);
        http.when(() -> HttpClientUtil.doGet(contains("/naming/v1/discovery"), anyMap(), anyMap(), eq(3000)))
                .thenReturn(noBody);
        assertThrows(NamingRegistryException.class, () -> service.refreshGroup(GROUP));
        verify(noBody).close();
        http.when(() -> HttpClientUtil.doGet(contains("/naming/v1/discovery"), anyMap(), anyMap(), eq(3000)))
                .thenThrow(new IOException("discovery offline"));
        assertThrows(RemoteException.class, () -> service.refreshGroup(GROUP));
    }

    @Test
    void refreshGroupParsesMetadataAndIncludesNamespaceAndToken() throws Exception {
        http.when(() -> HttpClientUtil.doGet(
                        eq("http://" + HOST + "/naming/v1/discovery"), anyMap(), anyMap(), eq(3000)))
                .thenReturn(response(200, "{\"term\":4,\"clusterList\":[]}"));
        assertTrue(service.refreshGroup(GROUP).isEmpty());
        assertEquals(4L, get("term", service));
        http.verify(() -> HttpClientUtil.doGet(
                anyString(),
                argThat((Map<String, String> params) ->
                        GROUP.equals(params.get("vGroup")) && "unit-ns".equals(params.get("namespace"))),
                argThat((Map<String, String> headers) -> "unit-token".equals(headers.get("Authorization"))),
                eq(3000)));
    }

    @Test
    void metadataFiltersStaleLeadersAndFollowersAndCachesMembers() throws Exception {
        NamingServerNode leader = node(ClusterRole.LEADER, 5, 8091);
        NamingServerNode stale = node(ClusterRole.LEADER, 4, 8092);
        NamingServerNode member = node(ClusterRole.MEMBER, 0, 8093);
        NamingServerNode follower = node(ClusterRole.FOLLOWER, 5, 8094);
        Unit unit = new Unit();
        unit.setNamingInstanceList(Arrays.asList(leader, stale, member, follower));
        Cluster cluster = new Cluster();
        cluster.setUnitData(Collections.singletonList(unit));
        MetaResponse metadata = new MetaResponse();
        metadata.setTerm(5);
        metadata.setClusterList(Collections.singletonList(cluster));
        List<InetSocketAddress> expected =
                Arrays.asList(new InetSocketAddress("127.0.0.1", 8091), new InetSocketAddress("127.0.0.1", 8093));
        assertEquals(expected, service.handleMetadata(metadata, GROUP));
        set("isSubscribed", service, true);
        assertEquals(expected, service.lookup(GROUP));
        assertTrue(service.lookup("missing").isEmpty());
        metadata.setTerm(0);
        assertEquals(expected, service.handleMetadata(metadata, GROUP));
        assertEquals(5L, get("term", service));
    }

    @Test
    void aliveLookupPrefersCurrentGroupThenFallsBackToAnotherCluster() {
        assertTrue(service.aliveLookup(GROUP).isEmpty());
        List<InetSocketAddress> fallback = Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091));
        RegistryService.CURRENT_ADDRESS_MAP.get(GROUP).put("other", fallback);
        assertEquals(fallback, service.aliveLookup(GROUP));
        List<InetSocketAddress> preferred = Collections.singletonList(new InetSocketAddress("127.0.0.2", 8091));
        assertNull(service.refreshAliveLookup(GROUP, preferred));
        assertEquals(preferred, service.aliveLookup(GROUP));
        assertEquals(preferred, service.refreshAliveLookup(GROUP, Collections.emptyList()));
        assertEquals(fallback, service.aliveLookup(GROUP));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void subscriptionNotifiesAndStopsForWatchOrMetadataExpiry(boolean expired) throws Exception {
        if (expired) {
            property("metadataMaxAgeMs", "-1");
        }
        doReturn(true).when(service).watch(GROUP);
        CountDownLatch notified = new CountDownLatch(1);
        set("namingServerAddressCache", service, HOST);
        NamingListener listener = group -> {
            assertEquals(GROUP, group);
            try {
                service.unsubscribe(group);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            notified.countDown();
        };
        service.subscribe(listener, GROUP);
        assertTrue(notified.await(5, TimeUnit.SECONDS));
        ((ExecutorService) get("NOTIFIER_EXECUTOR", null)).submit(() -> {}).get(5, TimeUnit.SECONDS);
        assertNull(get("namingServerAddressCache", service));
        verify(service, times(expired ? 0 : 1)).watch(GROUP);
    }

    @Test
    void unsubscribeRemovesOnlyRequestedListener() throws Exception {
        NamingListener first = mock(NamingListener.class);
        NamingListener second = mock(NamingListener.class);
        Map<String, List<NamingListener>> listeners = (Map) get("LISTENER_SERVICE_MAP", null);
        listeners.put(GROUP, new ArrayList<>(Arrays.asList(first, second)));
        service.unsubscribe(first, GROUP);
        assertEquals(Collections.singletonList(second), listeners.get(GROUP));
        service.unsubscribe(second, GROUP);
        assertFalse(listeners.containsKey(GROUP));
        service.unsubscribe(second, GROUP);
        service.close();
    }

    @Test
    void initialLookupRefreshesAndSubscribesOnlyOnce() throws Exception {
        doReturn(Collections.emptyList()).when(service).refreshGroup(GROUP);
        org.mockito.ArgumentCaptor<NamingListener> listener = org.mockito.ArgumentCaptor.forClass(NamingListener.class);
        doAnswer(invocation -> {
                    set("isSubscribed", service, true);
                    return null;
                })
                .when(service)
                .subscribe(any(NamingListener.class), eq(GROUP));
        assertTrue(service.lookup(GROUP).isEmpty());
        assertTrue(service.lookup(GROUP).isEmpty());
        verify(service, times(1)).refreshGroup(GROUP);
        verify(service, times(1)).subscribe(listener.capture(), eq(GROUP));
        listener.getValue().onEvent(GROUP);
        verify(service, times(2)).refreshGroup(GROUP);
        doThrow(new IOException("refresh failed")).when(service).refreshGroup(GROUP);
        RuntimeException error =
                assertThrows(RuntimeException.class, () -> listener.getValue().onEvent(GROUP));
        assertInstanceOf(IOException.class, error.getCause());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void subscriptionStopsCleanlyAfterWatchOrListenerFailure(boolean listenerFailure) throws Exception {
        CountDownLatch stopped = new CountDownLatch(1);
        doAnswer(invocation -> {
                    if (!listenerFailure) {
                        service.unsubscribe(GROUP);
                        Thread.currentThread().interrupt();
                        stopped.countDown();
                        throw new RetryableException("watch failed");
                    }
                    return true;
                })
                .when(service)
                .watch(GROUP);
        service.subscribe(
                group -> {
                    try {
                        service.unsubscribe(group);
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                    Thread.currentThread().interrupt();
                    stopped.countDown();
                    throw new IllegalStateException("listener failed");
                },
                GROUP);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        ((ExecutorService) get("NOTIFIER_EXECUTOR", null)).submit(() -> {}).get(5, TimeUnit.SECONDS);
        assertFalse((Boolean) get("isSubscribed", service));
    }

    private static Instance newInstance() throws Exception {
        java.lang.reflect.Constructor<Instance> constructor = Instance.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static NamingServerNode node(ClusterRole role, long term, int port) {
        NamingServerNode node = new NamingServerNode();
        node.setRole(role);
        node.setTerm(term);
        node.setTransaction(new Node.Endpoint("127.0.0.1", port));
        return node;
    }

    private static Response response(int status, String body) {
        return new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("test")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();
    }

    private Map<String, AtomicInteger> available() throws Exception {
        return (Map) get("AVAILABLE_NAMINGSERVER_MAP", null);
    }

    private static Object get(String name, Object target) throws Exception {
        Field field = NamingserverRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(String name, Object target, Object value) throws Exception {
        Field field = NamingserverRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void property(String key, String value) {
        key = "registry.seata." + key;
        if (!PROPERTIES.containsKey(key)) {
            PROPERTIES.put(key, System.getProperty(key));
        }
        System.setProperty(key, value);
    }
}
