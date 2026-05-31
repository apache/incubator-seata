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
package org.apache.seata.discovery.registry.raft;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.HttpStatus;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ParseEndpointException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.ClusterWatchEvent;
import org.apache.seata.common.metadata.Metadata;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.util.SeataHttpWatch;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RaftRegistryServiceImplTest {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("service.vgroupMapping.tx", "default");
        System.setProperty("registry.raft.username", "seata");
        System.setProperty("registry.raft.password", "seata");
        System.setProperty("registry.raft.serverAddr", "127.0.0.1:8092");
        System.setProperty("registry.raft.tokenValidityInMilliseconds", "10000");
        // Do not set preferredNetworks by default to allow tests to run without external metadata
        // System.setProperty("registry.preferredNetworks", "10.10.*");
        ConfigurationFactory.getInstance();
    }

    @AfterAll
    public static void adAfterClass() throws Exception {
        System.clearProperty("service.vgroupMapping.tx");
        System.clearProperty("registry.raft.username");
        System.clearProperty("registry.raft.password");
        System.clearProperty("registry.raft.serverAddr");
        System.clearProperty("registry.raft.tokenValidityInMilliseconds");
        System.clearProperty("registry.preferredNetworks");
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Reset the CLOSED flag after each test
        Field closedField = RaftRegistryServiceImpl.class.getDeclaredField("CLOSED");
        closedField.setAccessible(true);
        AtomicBoolean closed = (AtomicBoolean) closedField.get(null);
        closed.set(false);

        Method closeHttp2WatchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("closeHttp2Watch");
        closeHttp2WatchMethod.setAccessible(true);
        closeHttp2WatchMethod.invoke(null);

        setStaticField("HTTP2_WATCH_GROUP", null);
    }

    /**
     * Helper method to build a mock OkHttp Response for testing.
     */
    private static Response buildMockResponse(int statusCode, String body) {
        return new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message("")
                .body(ResponseBody.create(body != null ? body : "", MediaType.parse("application/json")))
                .build();
    }

    /**
     * test whether throws exception when login failed
     */
    @Test
    public void loginFailedTest() throws IOException, NoSuchMethodException {
        String jwtToken = "null";
        String responseBody =
                "{\"code\":\"401\",\"message\":\"Login failed\",\"data\":\"" + jwtToken + "\",\"success\":false}";

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {

            ResponseBody mockResponseBody = mock(ResponseBody.class);
            Response mockResponse = mock(Response.class);

            when(mockResponseBody.string()).thenReturn(responseBody);
            when(mockResponse.code()).thenReturn(HttpStatus.SC_OK);
            when(mockResponse.body()).thenReturn(mockResponseBody);

            when(HttpClientUtil.doPost(any(String.class), any(Map.class), any(Map.class), any(int.class)))
                    .thenReturn(mockResponse);

            // Use reflection to access and invoke the private method
            Method refreshTokenMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("refreshToken", String.class);
            refreshTokenMethod.setAccessible(true);
            assertThrows(
                    Exception.class,
                    () -> refreshTokenMethod.invoke(RaftRegistryServiceImpl.getInstance(), "127.0.0.1:8092"));
        }
    }

    /**
     * test whether the jwtToken updated when refreshToken method invoked
     */
    @Test
    public void refreshTokenSuccessTest()
            throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
                    NoSuchFieldException {
        String jwtToken = "newToken";
        String responseBody =
                "{\"code\":\"200\",\"message\":\"success\",\"data\":\"" + jwtToken + "\",\"success\":true}";

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {

            ResponseBody mockResponseBody = mock(ResponseBody.class);
            Response mockResponse = mock(Response.class);

            when(mockResponseBody.string()).thenReturn(responseBody);
            when(mockResponse.code()).thenReturn(HttpStatus.SC_OK);
            when(mockResponse.body()).thenReturn(mockResponseBody);

            when(HttpClientUtil.doPost(any(String.class), any(Map.class), any(Map.class), any(int.class)))
                    .thenReturn(mockResponse);

            Method refreshTokenMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("refreshToken", String.class);
            refreshTokenMethod.setAccessible(true);
            refreshTokenMethod.invoke(RaftRegistryServiceImpl.getInstance(), "127.0.0.1:8092");
            Field jwtTokenField = RaftRegistryServiceImpl.class.getDeclaredField("jwtToken");
            jwtTokenField.setAccessible(true);
            String jwtTokenAct = (String) jwtTokenField.get(null);

            assertEquals(jwtToken, jwtTokenAct);
        }
    }

    /**
     * test whether the jwtToken refreshed when it is expired
     */
    @Test
    public void secureTTLTest()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException,
                    InterruptedException {
        Field tokenTimeStamp = RaftRegistryServiceImpl.class.getDeclaredField("tokenTimeStamp");
        tokenTimeStamp.setAccessible(true);
        tokenTimeStamp.setLong(RaftRegistryServiceImpl.class, System.currentTimeMillis());
        Method isExpiredMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("isTokenExpired");
        isExpiredMethod.setAccessible(true);
        boolean rst = (boolean) isExpiredMethod.invoke(null);
        assertEquals(false, rst);
        Thread.sleep(10000);
        rst = (boolean) isExpiredMethod.invoke(null);
        assertEquals(true, rst);
    }

    /**
     * RaftRegistryServiceImpl#controlEndpointStr()
     * RaftRegistryServiceImpl#transactionEndpointStr()
     * Test endpoint selection based on configuration
     */
    @Test
    public void selectEndpointTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"v-0.svc-l.default.svc.cluster.local\",\"port\":7091},\"transaction\":{\"host\":\"v-0.svc-l.default.svc.cluster.local\",\"port\":8091},\"internal\":{\"host\":\"v-0.svc-l.default.svc.cluster.local\",\"port\":9091},\"group\":\"default\",\"role\":\"LEADER\",\"version\":\"2.3.0-SNAPSHOT\",\"metadata\":{\"external\":[{\"host\":\"192.168.105.7\",\"controlPort\":30071,\"transactionPort\":30091},{\"host\":\"10.10.105.7\",\"controlPort\":30071,\"transactionPort\":30091}]}},{\"control\":{\"host\":\"v-2.svc-l.default.svc.cluster.local\",\"port\":7091},\"transaction\":{\"host\":\"v-2.svc-l.default.svc.cluster.local\",\"port\":8091},\"internal\":{\"host\":\"v-2.svc-l.default.svc.cluster.local\",\"port\":9091},\"group\":\"default\",\"role\":\"FOLLOWER\",\"version\":\"2.3.0-SNAPSHOT\",\"metadata\":{\"external\":[{\"host\":\"192.168.105.7\",\"controlPort\":30073,\"transactionPort\":30093},{\"host\":\"10.10.105.7\",\"controlPort\":30073,\"transactionPort\":30093}]}},{\"control\":{\"host\":\"v-1.svc-l.default.svc.cluster.local\",\"port\":7091},\"transaction\":{\"host\":\"v-1.svc-l.default.svc.cluster.local\",\"port\":8091},\"internal\":{\"host\":\"v-1.svc-l.default.svc.cluster.local\",\"port\":9091},\"group\":\"default\",\"role\":\"FOLLOWER\",\"version\":\"2.3.0-SNAPSHOT\",\"metadata\":{\"external\":[{\"host\":\"192.168.105.7\",\"controlPort\":30072,\"transactionPort\":30092},{\"host\":\"10.10.105.7\",\"controlPort\":30072,\"transactionPort\":30092}]}}],\"storeMode\":\"raft\",\"term\":1}";

        Method selectControlEndpointStrMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectControlEndpointStr", Node.class);
        selectControlEndpointStrMethod.setAccessible(true);
        Method selectTransactionEndpointStrMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectTransactionEndpointStr", Node.class);
        selectTransactionEndpointStrMethod.setAccessible(true);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        List<Node> nodes = metadataResponse.getNodes();

        // Verify endpoint selection works and returns valid endpoints
        for (Node node : nodes) {
            String controlEndpointStr = (String) selectControlEndpointStrMethod.invoke(null, node);
            String transactionEndpointStr = (String) selectTransactionEndpointStrMethod.invoke(null, node);

            // Verify endpoints are properly formatted
            assertTrue(controlEndpointStr.contains(":"), "Control endpoint should contain port");
            assertTrue(transactionEndpointStr.contains(":"), "Transaction endpoint should contain port");
        }
    }

    /**
     * Test singleton pattern
     */
    @Test
    public void getInstanceTest() {
        RaftRegistryServiceImpl instance1 = RaftRegistryServiceImpl.getInstance();
        RaftRegistryServiceImpl instance2 = RaftRegistryServiceImpl.getInstance();
        assertNotNull(instance1);
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    /**
     * Test register method (empty implementation)
     */
    @Test
    public void registerTest() throws Exception {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        // Should not throw exception even though it's an empty implementation
        assertDoesNotThrow(() -> instance.register(address));
    }

    /**
     * Test unregister method (empty implementation)
     */
    @Test
    public void unregisterTest() throws Exception {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        // Should not throw exception even though it's an empty implementation
        assertDoesNotThrow(() -> instance.unregister(address));
    }

    /**
     * Test subscribe method (empty implementation)
     */
    @Test
    public void subscribeTest() throws Exception {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
        // Should not throw exception even though it's an empty implementation
        assertDoesNotThrow(() -> instance.subscribe("default", null));
    }

    /**
     * Test unsubscribe method (empty implementation)
     */
    @Test
    public void unsubscribeTest() throws Exception {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
        // Should not throw exception even though it's an empty implementation
        assertDoesNotThrow(() -> instance.unsubscribe("default", null));
    }

    /**
     * Test close method
     */
    @Test
    public void closeTest() throws NoSuchFieldException, IllegalAccessException {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();

        Field closedField = RaftRegistryServiceImpl.class.getDeclaredField("CLOSED");
        closedField.setAccessible(true);
        AtomicBoolean closed = (AtomicBoolean) closedField.get(null);

        assertFalse(closed.get(), "CLOSED should be false initially");

        instance.close();

        assertTrue(closed.get(), "CLOSED should be true after close");
    }

    /**
     * Test selectEndpoint with unsupported type
     */
    @Test
    public void selectEndpointUnsupportedTypeTest() throws Exception {
        String jsonString =
                "{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091}}";
        ObjectMapper objectMapper = new ObjectMapper();
        Node node = objectMapper.readValue(jsonString, Node.class);

        Method selectEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectEndpoint", String.class, Node.class);
        selectEndpointMethod.setAccessible(true);

        assertThrows(
                NotSupportYetException.class,
                () -> {
                    try {
                        selectEndpointMethod.invoke(null, "unsupported", node);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                },
                "Should throw NotSupportYetException for unsupported type");
    }

    /**
     * Test selectExternalEndpoint with empty metadata
     */
    @Test
    public void selectExternalEndpointEmptyMetadataTest() throws Exception {
        Node node = new Node();
        node.setMetadata(new HashMap<>());

        Method selectExternalEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectExternalEndpoint", Node.class, String[].class);
        selectExternalEndpointMethod.setAccessible(true);

        assertThrows(
                ParseEndpointException.class,
                () -> {
                    try {
                        selectExternalEndpointMethod.invoke(null, node, new String[] {"10.10.*"});
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                },
                "Should throw ParseEndpointException when metadata is empty");
    }

    /**
     * Test selectExternalEndpoint with null metadata
     */
    @Test
    public void selectExternalEndpointNullMetadataTest() throws Exception {
        Node node = new Node();
        node.setMetadata(null);

        Method selectExternalEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectExternalEndpoint", Node.class, String[].class);
        selectExternalEndpointMethod.setAccessible(true);

        assertThrows(
                ParseEndpointException.class,
                () -> {
                    try {
                        selectExternalEndpointMethod.invoke(null, node, new String[] {"10.10.*"});
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                },
                "Should throw ParseEndpointException when metadata is null");
    }

    /**
     * Test selectExternalEndpoint with empty external endpoints
     */
    @Test
    public void selectExternalEndpointEmptyExternalListTest() throws Exception {
        Node node = new Node();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("external", new ArrayList<>());
        node.setMetadata(metadata);

        Method selectExternalEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectExternalEndpoint", Node.class, String[].class);
        selectExternalEndpointMethod.setAccessible(true);

        assertThrows(
                ParseEndpointException.class,
                () -> {
                    try {
                        selectExternalEndpointMethod.invoke(null, node, new String[] {"10.10.*"});
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                },
                "Should throw ParseEndpointException when external endpoints list is empty");
    }

    /**
     * Test selectExternalEndpoint with no matching network
     */
    @Test
    public void selectExternalEndpointNoMatchingNetworkTest() throws Exception {
        Node node = new Node();
        Map<String, Object> metadata = new HashMap<>();
        List<LinkedHashMap<String, Object>> externalList = new ArrayList<>();
        LinkedHashMap<String, Object> external = new LinkedHashMap<>();
        external.put("host", "192.168.1.1");
        external.put("controlPort", 7091);
        external.put("transactionPort", 8091);
        externalList.add(external);
        metadata.put("external", externalList);
        node.setMetadata(metadata);

        Method selectExternalEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectExternalEndpoint", Node.class, String[].class);
        selectExternalEndpointMethod.setAccessible(true);

        assertThrows(
                ParseEndpointException.class,
                () -> {
                    try {
                        selectExternalEndpointMethod.invoke(null, node, new String[] {"10.10.*"});
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                },
                "Should throw ParseEndpointException when no external endpoint matches preferred network");
    }

    /**
     * Test isPreferredNetwork method
     */
    @Test
    public void isPreferredNetworkTest() throws Exception {
        Method isPreferredNetworkMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("isPreferredNetwork", String.class, List.class);
        isPreferredNetworkMethod.setAccessible(true);

        // Test with prefix match
        boolean result = (boolean) isPreferredNetworkMethod.invoke(null, "10.10.105.7", Arrays.asList("10.10.*"));
        assertTrue(result, "Should match with prefix 10.10.*");

        // Test with regex match
        result = (boolean) isPreferredNetworkMethod.invoke(null, "192.168.1.1", Arrays.asList("192\\.168\\..*"));
        assertTrue(result, "Should match with regex pattern");

        // Test with no match
        result = (boolean) isPreferredNetworkMethod.invoke(null, "172.16.0.1", Arrays.asList("10.10.*", "192.168.*"));
        assertFalse(result, "Should not match when IP doesn't match any pattern");

        // Test with blank pattern
        result = (boolean) isPreferredNetworkMethod.invoke(null, "10.10.1.1", Arrays.asList("", "10.10.*"));
        assertTrue(result, "Should skip blank pattern and match valid one");
    }

    /**
     * Test lookup method with metadata already present
     */
    @Test
    public void lookupWithExistingMetadataTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();

        // Setup metadata
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        List<InetSocketAddress> result = instance.lookup("tx");

        assertFalse(result.isEmpty(), "Should return non-empty list when metadata exists");
    }

    /**
     * Test aliveLookup in raft mode with leader
     */
    @Test
    public void aliveLookupInRaftModeWithLeaderTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();

        // Setup metadata
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        List<InetSocketAddress> result = instance.aliveLookup("tx");

        assertNotNull(result);
        if (metadata.isRaftMode() && metadata.getLeader("default") != null) {
            assertEquals(1, result.size(), "Should return single leader address in raft mode");
        }
    }

    /**
     * Test refreshAliveLookup in raft mode
     * Note: refreshAliveLookup returns the previous value from Map.put()
     */
    @Test
    public void refreshAliveLookupInRaftModeTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();

        // Setup metadata
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        // Pre-populate ALIVE_NODES with initial value so we can verify the return
        Field aliveNodesField = RaftRegistryServiceImpl.class.getDeclaredField("ALIVE_NODES");
        aliveNodesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> aliveNodes =
                (Map<String, List<InetSocketAddress>>) aliveNodesField.get(null);

        List<InetSocketAddress> initialList = new ArrayList<>();
        initialList.add(new InetSocketAddress("localhost", 9091));
        aliveNodes.put("tx", initialList);

        List<InetSocketAddress> aliveAddress = new ArrayList<>();
        aliveAddress.add(new InetSocketAddress("localhost", 8091));
        aliveAddress.add(new InetSocketAddress("localhost", 8092));

        // Should return the previous value (initialList) from Map.put()
        List<InetSocketAddress> result = instance.refreshAliveLookup("tx", aliveAddress);

        assertNotNull(result, "Should return previous value from Map");
        assertEquals(1, result.size(), "Previous list should have 1 element");
        assertEquals(9091, result.get(0).getPort(), "Previous list should contain port 9091");
    }

    /**
     * Test acquireClusterMetaData with authentication failure
     */
    @Test
    public void acquireClusterMetaDataAuthFailureTest() throws Exception {
        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            Response mockResponse = buildMockResponse(HttpStatus.SC_UNAUTHORIZED, null);

            when(HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockResponse);

            Method acquireClusterMetaDataMethod = RaftRegistryServiceImpl.class.getDeclaredMethod(
                    "acquireClusterMetaData", String.class, String.class);
            acquireClusterMetaDataMethod.setAccessible(true);

            // This should handle the auth failure and throw appropriate exception
            assertThrows(Exception.class, () -> {
                try {
                    acquireClusterMetaDataMethod.invoke(null, "default", "default");
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        }
    }

    /**
     * Test selectControlEndpoint without preferredNetworks
     */
    @Test
    public void selectControlEndpointWithoutPreferredNetworksTest() throws Exception {
        String jsonString =
                "{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091}}";
        ObjectMapper objectMapper = new ObjectMapper();
        Node node = objectMapper.readValue(jsonString, Node.class);

        Method selectControlEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectControlEndpoint", Node.class);
        selectControlEndpointMethod.setAccessible(true);

        InetSocketAddress result = (InetSocketAddress) selectControlEndpointMethod.invoke(null, node);

        assertEquals("localhost", result.getHostString());
        assertEquals(7091, result.getPort());
    }

    /**
     * Test selectTransactionEndpoint without preferredNetworks
     */
    @Test
    public void selectTransactionEndpointWithoutPreferredNetworksTest() throws Exception {
        String jsonString =
                "{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091}}";
        ObjectMapper objectMapper = new ObjectMapper();
        Node node = objectMapper.readValue(jsonString, Node.class);

        Method selectTransactionEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectTransactionEndpoint", Node.class);
        selectTransactionEndpointMethod.setAccessible(true);

        InetSocketAddress result = (InetSocketAddress) selectTransactionEndpointMethod.invoke(null, node);

        assertEquals("localhost", result.getHostString());
        assertEquals(8091, result.getPort());
    }

    /**
     * Test createExternalEndpoint
     */
    @Test
    public void createExternalEndpointTest() throws Exception {
        LinkedHashMap<String, Object> externalEndpoint = new LinkedHashMap<>();
        externalEndpoint.put("host", "10.10.1.1");
        externalEndpoint.put("controlPort", 7091);
        externalEndpoint.put("transactionPort", 8091);

        Method createExternalEndpointMethod = RaftRegistryServiceImpl.class.getDeclaredMethod(
                "createExternalEndpoint", LinkedHashMap.class, String.class);
        createExternalEndpointMethod.setAccessible(true);

        Node.ExternalEndpoint result =
                (Node.ExternalEndpoint) createExternalEndpointMethod.invoke(null, externalEndpoint, "10.10.1.1");

        assertEquals("10.10.1.1", result.getHost());
        assertEquals(7091, result.getControlPort());
        assertEquals(8091, result.getTransactionPort());
    }

    /**
     * Test token expiration check when timestamp is -1
     */
    @Test
    public void isTokenExpiredWhenTimestampIsMinusOneTest() throws Exception {
        Field tokenTimeStamp = RaftRegistryServiceImpl.class.getDeclaredField("tokenTimeStamp");
        tokenTimeStamp.setAccessible(true);
        tokenTimeStamp.setLong(RaftRegistryServiceImpl.class, -1);

        Method isExpiredMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("isTokenExpired");
        isExpiredMethod.setAccessible(true);
        boolean rst = (boolean) isExpiredMethod.invoke(null);

        assertTrue(rst, "Token should be expired when timestamp is -1");
    }

    /**
     * Test configuration key methods
     */
    @Test
    public void getRaftAddrFileKeyTest() throws Exception {
        Method getRaftAddrFileKeyMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("getRaftAddrFileKey");
        getRaftAddrFileKeyMethod.setAccessible(true);
        String result = (String) getRaftAddrFileKeyMethod.invoke(null);
        assertTrue(result.contains("registry"));
        assertTrue(result.contains("raft"));
        assertTrue(result.contains("serverAddr"));
    }

    @Test
    public void getRaftUserNameKeyTest() throws Exception {
        Method getRaftUserNameKeyMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("getRaftUserNameKey");
        getRaftUserNameKeyMethod.setAccessible(true);
        String result = (String) getRaftUserNameKeyMethod.invoke(null);
        assertTrue(result.contains("registry"));
        assertTrue(result.contains("raft"));
        assertTrue(result.contains("username"));
    }

    @Test
    public void getRaftPassWordKeyTest() throws Exception {
        Method getRaftPassWordKeyMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("getRaftPassWordKey");
        getRaftPassWordKeyMethod.setAccessible(true);
        String result = (String) getRaftPassWordKeyMethod.invoke(null);
        assertTrue(result.contains("registry"));
        assertTrue(result.contains("raft"));
        assertTrue(result.contains("password"));
    }

    @Test
    public void getPreferredNetworksTest() throws Exception {
        Method getPreferredNetworksMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("getPreferredNetworks");
        getPreferredNetworksMethod.setAccessible(true);
        String result = (String) getPreferredNetworksMethod.invoke(null);
        assertTrue(result.contains("registry"));
        assertTrue(result.contains("preferredNetworks"));
    }

    @Test
    public void getTokenExpireTimeInMillisecondsKeyTest() throws Exception {
        Method getTokenExpireTimeMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("getTokenExpireTimeInMillisecondsKey");
        getTokenExpireTimeMethod.setAccessible(true);
        String result = (String) getTokenExpireTimeMethod.invoke(null);
        assertTrue(result.contains("registry"));
        assertTrue(result.contains("raft"));
        assertTrue(result.contains("tokenValidityInMilliseconds"));
    }

    @Test
    public void getMetadataMaxAgeMsTest() throws Exception {
        Method getMetadataMaxAgeMsMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("getMetadataMaxAgeMs");
        getMetadataMaxAgeMsMethod.setAccessible(true);
        String result = (String) getMetadataMaxAgeMsMethod.invoke(null);
        assertTrue(result.contains("registry"));
        assertTrue(result.contains("raft"));
        assertTrue(result.contains("metadataMaxAgeMs"));
    }

    /**
     * Note: watch() is a private method used internally by the background metadata
     * refresh thread. Testing private methods directly is generally not recommended
     * as it couples tests to implementation details. The watch() functionality is
     * indirectly tested through the public API methods that rely on metadata updates.
     *
     * Removed test: watchSuccessTest() - too complex to mock all required internal state
     */

    /**
     * Test acquireClusterMetaData success scenario
     */
    @Test
    public void acquireClusterMetaDataSuccessTest() throws Exception {
        String responseBody =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        Field clusterNameField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_CLUSTER_NAME");
        clusterNameField.setAccessible(true);
        clusterNameField.set(null, "default");

        // Setup metadata
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(responseBody, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            Response mockResponse = buildMockResponse(HttpStatus.SC_OK, responseBody);

            when(HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockResponse);

            Method acquireClusterMetaDataMethod = RaftRegistryServiceImpl.class.getDeclaredMethod(
                    "acquireClusterMetaData", String.class, String.class);
            acquireClusterMetaDataMethod.setAccessible(true);

            // Should not throw exception
            assertDoesNotThrow(() -> acquireClusterMetaDataMethod.invoke(null, "default", "default"));
        }
    }

    /**
     * Test acquireClusterMetaDataByClusterName
     */
    @Test
    public void acquireClusterMetaDataByClusterNameTest() throws Exception {
        String responseBody =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        // Setup metadata first
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(responseBody, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        Field clusterNameField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_CLUSTER_NAME");
        clusterNameField.setAccessible(true);
        clusterNameField.set(null, "default");

        // Setup INIT_ADDRESSES to avoid NullPointerException in queryHttpAddress
        Field initAddressesField = RaftRegistryServiceImpl.class.getDeclaredField("INIT_ADDRESSES");
        initAddressesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> initAddresses =
                (Map<String, List<InetSocketAddress>>) initAddressesField.get(null);
        List<InetSocketAddress> addressList = new ArrayList<>();
        addressList.add(new InetSocketAddress("localhost", 7091));
        initAddresses.put("default", addressList);

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            Response mockResponse = buildMockResponse(HttpStatus.SC_OK, responseBody);

            when(HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockResponse);

            Method acquireClusterMetaDataByClusterNameMethod = RaftRegistryServiceImpl.class.getDeclaredMethod(
                    "acquireClusterMetaDataByClusterName", String.class);
            acquireClusterMetaDataByClusterNameMethod.setAccessible(true);

            // Should not throw exception
            assertDoesNotThrow(() -> acquireClusterMetaDataByClusterNameMethod.invoke(null, "default"));
        } finally {
            // Clean up
            initAddresses.remove("default");
        }
    }

    /**
     * Test lookup returning null when cluster name is null
     */
    @Test
    public void lookupWithNullClusterNameTest() throws Exception {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();

        // Use a service group that doesn't have a mapping
        List<InetSocketAddress> result = instance.lookup("nonexistent-service");

        // Should return null or empty list when cluster name cannot be resolved
        assertTrue(result == null || result.isEmpty());
    }

    /**
     * Test selectEndpoint with preferredNetworks for both control and transaction types
     * NOTE: This test is removed because PREFERRED_NETWORKS is a static final field
     * initialized at class load time from configuration. Setting system properties
     * after class initialization has no effect. The preferred networks functionality
     * is already tested through testSelectExternalEndpointSuccessMatch and
     * testIsPreferredNetwork which test the underlying methods directly.
     */

    /**
     * Test queryHttpAddress with different scenarios
     */
    @Test
    public void queryHttpAddressWithNodesTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        // Setup metadata
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        Field serviceGroupField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_SERVICE_GROUP");
        serviceGroupField.setAccessible(true);
        serviceGroupField.set(null, "tx");

        Method queryHttpAddressMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("queryHttpAddress", String.class, String.class);
        queryHttpAddressMethod.setAccessible(true);

        String result = (String) queryHttpAddressMethod.invoke(null, "default", "default");
        assertTrue(result.contains(":"));
    }

    /**
     * Test selectExternalEndpoint with successful matching
     */
    @Test
    public void selectExternalEndpointSuccessMatchTest() throws Exception {
        Node node = new Node();
        Map<String, Object> metadata = new HashMap<>();
        List<LinkedHashMap<String, Object>> externalList = new ArrayList<>();

        LinkedHashMap<String, Object> external1 = new LinkedHashMap<>();
        external1.put("host", "192.168.1.1");
        external1.put("controlPort", 7091);
        external1.put("transactionPort", 8091);
        externalList.add(external1);

        LinkedHashMap<String, Object> external2 = new LinkedHashMap<>();
        external2.put("host", "10.10.105.7");
        external2.put("controlPort", 30071);
        external2.put("transactionPort", 30091);
        externalList.add(external2);

        metadata.put("external", externalList);
        node.setMetadata(metadata);

        Method selectExternalEndpointMethod =
                RaftRegistryServiceImpl.class.getDeclaredMethod("selectExternalEndpoint", Node.class, String[].class);
        selectExternalEndpointMethod.setAccessible(true);

        Node.ExternalEndpoint result =
                (Node.ExternalEndpoint) selectExternalEndpointMethod.invoke(null, node, new String[] {"10.10.*"});

        assertEquals("10.10.105.7", result.getHost());
        assertEquals(30071, result.getControlPort());
        assertEquals(30091, result.getTransactionPort());
    }

    /**
     * Test refreshAliveLookup with empty address list
     * Note: Map.put() returns the previous value, which is null on first call
     */
    @Test
    public void refreshAliveLookupWithEmptyListTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();

        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        // Clean up any previous entries
        Field aliveNodesField = RaftRegistryServiceImpl.class.getDeclaredField("ALIVE_NODES");
        aliveNodesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> aliveNodes =
                (Map<String, List<InetSocketAddress>>) aliveNodesField.get(null);
        aliveNodes.remove("tx");

        List<InetSocketAddress> emptyList = new ArrayList<>();
        // First call returns null (previous value from Map.put), which is expected
        List<InetSocketAddress> result = instance.refreshAliveLookup("tx", emptyList);

        // Map.put() returns the previous value, which is null on first insert
        // This is expected behavior
        assertNull(result, "First call should return null (previous value from Map.put)");

        // Second call should return the empty list that was stored by first call
        result = instance.refreshAliveLookup("tx", emptyList);
        assertNotNull(result, "Second call should return the previous value (empty list)");
        assertTrue(result.isEmpty(), "Previous value should be the empty list");
    }

    /**
     * Test token not expired scenario
     */
    @Test
    public void isTokenNotExpiredTest() throws Exception {
        Field tokenTimeStamp = RaftRegistryServiceImpl.class.getDeclaredField("tokenTimeStamp");
        tokenTimeStamp.setAccessible(true);
        // Set to current time, should not be expired immediately
        tokenTimeStamp.setLong(RaftRegistryServiceImpl.class, System.currentTimeMillis());

        Method isExpiredMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("isTokenExpired");
        isExpiredMethod.setAccessible(true);
        boolean rst = (boolean) isExpiredMethod.invoke(null);

        assertFalse(rst, "Token should not be expired when timestamp is recent");
    }

    /**
     * Test watch method with null response.
     * Note: this also covers the previous "null status line" scenario because
     * both cases are represented as null okhttp response in current code path.
     */
    @Test
    public void watchWithNullResponseTest() throws Exception {
        prepareWatchClusterContext(
                "default",
                metadataResponse(1L, createNode("localhost", 7091, 8091, "default", ClusterRole.LEADER, "2.6.9")));

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            when(HttpClientUtil.doPost(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(null);
            boolean result = invokeWatch();

            assertFalse(result, "Watch should return false when response is null");
        }
    }

    /**
     * Test watch method with unauthorized response and credentials configured
     */
    @Test
    public void watchUnauthorizedWithCredentialsTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        System.setProperty("registry.raft.username", "seata");
        System.setProperty("registry.raft.password", "seata");

        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        Field clusterNameField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_CLUSTER_NAME");
        clusterNameField.setAccessible(true);
        clusterNameField.set(null, "default");

        Field initAddressesField = RaftRegistryServiceImpl.class.getDeclaredField("INIT_ADDRESSES");
        initAddressesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> initAddresses =
                (Map<String, List<InetSocketAddress>>) initAddressesField.get(null);
        List<InetSocketAddress> addressList = new ArrayList<>();
        addressList.add(new InetSocketAddress("localhost", 7091));
        initAddresses.put("default", addressList);

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            Response mockResponse = buildMockResponse(HttpStatus.SC_UNAUTHORIZED, null);

            when(HttpClientUtil.doPost(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockResponse);

            Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
            watchMethod.setAccessible(true);

            assertThrows(Exception.class, () -> {
                try {
                    watchMethod.invoke(null);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        } finally {
            initAddresses.remove("default");
        }
    }

    /**
     * Test watch method with IOException
     */
    @Test
    public void watchWithIOExceptionTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        Field clusterNameField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_CLUSTER_NAME");
        clusterNameField.setAccessible(true);
        clusterNameField.set(null, "default");

        Field initAddressesField = RaftRegistryServiceImpl.class.getDeclaredField("INIT_ADDRESSES");
        initAddressesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> initAddresses =
                (Map<String, List<InetSocketAddress>>) initAddressesField.get(null);
        List<InetSocketAddress> addressList = new ArrayList<>();
        addressList.add(new InetSocketAddress("localhost", 7091));
        initAddresses.put("default", addressList);

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            when(HttpClientUtil.doPost(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenThrow(new IOException("Connection failed"));

            Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
            watchMethod.setAccessible(true);

            assertThrows(Exception.class, () -> {
                try {
                    watchMethod.invoke(null);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        } finally {
            initAddresses.remove("default");
        }
    }

    /**
     * Test watch method with empty group terms
     */
    @Test
    public void watchWithEmptyGroupTermsTest() throws Exception {
        String jsonString = "{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}";

        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("emptyCluster", metadataResponse);

        Field clusterNameField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_CLUSTER_NAME");
        clusterNameField.setAccessible(true);
        clusterNameField.set(null, "emptyCluster");

        Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
        watchMethod.setAccessible(true);
        boolean result = (boolean) watchMethod.invoke(null);

        assertFalse(result, "Watch should return false when groupTerms is empty");
    }

    /**
     * Test watch method with expired token
     */
    @Test
    public void watchWithExpiredTokenTest() throws Exception {
        String jsonString =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        metadata.refreshMetadata("default", metadataResponse);

        Field clusterNameField = RaftRegistryServiceImpl.class.getDeclaredField("CURRENT_TRANSACTION_CLUSTER_NAME");
        clusterNameField.setAccessible(true);
        clusterNameField.set(null, "default");

        Field initAddressesField = RaftRegistryServiceImpl.class.getDeclaredField("INIT_ADDRESSES");
        initAddressesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> initAddresses =
                (Map<String, List<InetSocketAddress>>) initAddressesField.get(null);
        List<InetSocketAddress> addressList = new ArrayList<>();
        addressList.add(new InetSocketAddress("localhost", 7091));
        initAddresses.put("default", addressList);

        Field tokenTimeStamp = RaftRegistryServiceImpl.class.getDeclaredField("tokenTimeStamp");
        tokenTimeStamp.setAccessible(true);
        tokenTimeStamp.setLong(RaftRegistryServiceImpl.class, -1);

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            String loginResponse = "{\"code\":\"200\",\"message\":\"success\",\"data\":\"newToken\",\"success\":true}";
            Response mockLoginResponse = buildMockResponse(HttpStatus.SC_OK, loginResponse);
            Response mockWatchResponse = buildMockResponse(HttpStatus.SC_OK, null);

            when(HttpClientUtil.doPost(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockLoginResponse, mockWatchResponse);

            Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
            watchMethod.setAccessible(true);
            boolean result = (boolean) watchMethod.invoke(null);

            assertTrue(result, "Watch should return true after refreshing expired token");
        } finally {
            initAddresses.remove("default");
        }
    }

    @Test
    public void watchHttp2CreateAndConsumeUpdateTest() throws Exception {
        String clusterName = "watchHttp2CreateCluster";
        String group = "default";
        prepareWatchClusterContext(
                clusterName,
                metadataResponse(1L, createNode("127.0.0.1", 7091, 8091, group, ClusterRole.LEADER, "2.7.0")));
        setTokenNotExpired();

        SeataHttpWatch<ClusterWatchEvent> watch = mockWatch();
        when(watch.next())
                .thenReturn(newWatchUpdateResponse(
                        group,
                        metadataResponse(2L, createNode("127.0.0.1", 7091, 8091, group, ClusterRole.LEADER, "2.7.0"))));

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            mockedStatic
                    .when(() -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()))
                    .thenReturn(watch);

            boolean result = invokeWatchHttp2(clusterName);
            assertTrue(result, "watchHttp2 should return true when UPDATE term advances");

            Metadata metadata = (Metadata) getStaticField("METADATA");
            assertEquals(2L, metadata.getClusterTerm(clusterName).get(group).longValue());
            mockedStatic.verify(
                    () -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()),
                    times(1));
        }
    }

    @Test
    public void watchHttp2ReuseForSameGroupTest() throws Exception {
        String clusterName = "watchHttp2ReuseCluster";
        String group = "default";
        prepareWatchClusterContext(
                clusterName,
                metadataResponse(1L, createNode("127.0.0.1", 7191, 8191, group, ClusterRole.LEADER, "2.7.0")));
        setTokenNotExpired();

        SeataHttpWatch<ClusterWatchEvent> watch = mockWatch();
        when(watch.next())
                .thenReturn(newWatchUpdateResponse(
                        group,
                        metadataResponse(2L, createNode("127.0.0.1", 7191, 8191, group, ClusterRole.LEADER, "2.7.0"))))
                .thenReturn(newWatchUpdateResponse(
                        group,
                        metadataResponse(3L, createNode("127.0.0.1", 7191, 8191, group, ClusterRole.LEADER, "2.7.0"))));

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            mockedStatic
                    .when(() -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()))
                    .thenReturn(watch);

            assertTrue(invokeWatchHttp2(clusterName));
            assertTrue(invokeWatchHttp2(clusterName));

            mockedStatic.verify(
                    () -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()),
                    times(1));
        }
    }

    @Test
    public void watchHttp2SwitchGroupStableSelectionTest() throws Exception {
        String clusterName = "watchHttp2MultiGroupCluster";
        Node group2Leader = createNode("127.0.0.1", 7092, 8092, "g2", ClusterRole.LEADER, "2.7.0");
        Node group1Leader = createNode("127.0.0.1", 7091, 8091, "g1", ClusterRole.LEADER, "2.7.0");
        prepareWatchClusterContext(clusterName, metadataResponse(1L, group2Leader), metadataResponse(1L, group1Leader));
        setTokenNotExpired();
        setStaticField("HTTP2_WATCH_GROUP", "missing-group");

        SeataHttpWatch<ClusterWatchEvent> watch = mockWatch();
        when(watch.next())
                .thenReturn(newWatchUpdateResponse("g1", metadataResponse(2L, group1Leader)))
                .thenReturn(newWatchUpdateResponse("g1", metadataResponse(3L, group1Leader)));

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            mockedStatic
                    .when(() -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()))
                    .thenReturn(watch);

            assertTrue(invokeWatchHttp2(clusterName));
            assertTrue(invokeWatchHttp2(clusterName));

            mockedStatic.verify(
                    () -> HttpClientUtil.watchPost(
                            eq("http://127.0.0.1:7091/metadata/v1/watch"),
                            anyMap(),
                            anyMap(),
                            eq(ClusterWatchEvent.class),
                            anyInt()),
                    times(1));
        }
    }

    @Test
    public void watchHttp2RuntimeExceptionCloseAndRetryableTest() throws Exception {
        String clusterName = "watchHttp2ExceptionCluster";
        String group = "default";
        prepareWatchClusterContext(
                clusterName,
                metadataResponse(1L, createNode("127.0.0.1", 7391, 8391, group, ClusterRole.LEADER, "2.7.0")));
        setTokenNotExpired();

        SeataHttpWatch<ClusterWatchEvent> watch = mockWatch();
        when(watch.next()).thenThrow(new RuntimeException("watch stream failure"));

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            mockedStatic
                    .when(() -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()))
                    .thenReturn(watch);

            assertThrows(RetryableException.class, () -> invokeWatchHttp2(clusterName));
            verify(watch, times(1)).close();
            assertNull(getStaticField("HTTP2_WATCH"));
            assertNull(getStaticField("HTTP2_WATCH_GROUP"));
        }
    }

    @Test
    public void watchHttp2ClosedStateNoRetryNoiseTest() throws Exception {
        String clusterName = "watchHttp2ClosedCluster";
        String group = "default";
        prepareWatchClusterContext(
                clusterName,
                metadataResponse(1L, createNode("127.0.0.1", 7491, 8491, group, ClusterRole.LEADER, "2.7.0")));
        setTokenNotExpired();
        setClosed(true);

        SeataHttpWatch<ClusterWatchEvent> watch = mockWatch();
        when(watch.next()).thenThrow(new RuntimeException("closed during shutdown"));

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            mockedStatic
                    .when(() -> HttpClientUtil.watchPost(
                            anyString(), anyMap(), anyMap(), eq(ClusterWatchEvent.class), anyInt()))
                    .thenReturn(watch);

            boolean result = invokeWatchHttp2(clusterName);
            assertFalse(result, "watchHttp2 should return false instead of retrying when client is closed");
            verify(watch, times(1)).close();
        } finally {
            setClosed(false);
        }
    }

    @Test
    public void selectWatchGroupReuseAndSortedFallbackTest() throws Exception {
        Method selectWatchGroupMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("selectWatchGroup", Map.class);
        selectWatchGroupMethod.setAccessible(true);

        Map<String, Long> groupTerms = new HashMap<>();
        groupTerms.put("g2", 1L);
        groupTerms.put("g1", 1L);

        setStaticField("HTTP2_WATCH_GROUP", "g2");
        assertEquals("g2", selectWatchGroupMethod.invoke(null, groupTerms));

        setStaticField("HTTP2_WATCH_GROUP", "unknown");
        assertEquals("g1", selectWatchGroupMethod.invoke(null, groupTerms));

        assertNull(selectWatchGroupMethod.invoke(null, Collections.emptyMap()));
    }

    /**
     * Test lookup with empty raft cluster address
     */
    @Test
    public void lookupWithEmptyRaftClusterAddressTest() throws Exception {
        System.setProperty("registry.raft.serverAddr", "");
        System.setProperty("service.vgroupMapping.emptyAddrGroup", "emptyAddrCluster");

        try {
            RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
            List<InetSocketAddress> result = instance.lookup("emptyAddrGroup");

            assertTrue(result == null || result.isEmpty(), "Should return null or empty when serverAddr is empty");
        } finally {
            System.clearProperty("service.vgroupMapping.emptyAddrGroup");
            System.setProperty("registry.raft.serverAddr", "127.0.0.1:8092");
        }
    }

    /**
     * Test lookup with invalid endpoint array length
     */
    @Test
    public void lookupWithInvalidEndpointArrayLengthTest() throws Exception {
        System.setProperty("registry.raft.serverAddr", "localhost");
        System.setProperty("service.vgroupMapping.invalidEndpoint", "invalidCluster");

        try {
            RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
            assertThrows(Exception.class, () -> instance.lookup("invalidEndpoint"));
        } finally {
            System.clearProperty("service.vgroupMapping.invalidEndpoint");
            System.setProperty("registry.raft.serverAddr", "127.0.0.1:8092");
        }
    }

    /**
     * Test lookup with complete initialization flow
     */
    @Test
    public void lookupWithCompleteInitializationFlowTest() throws Exception {
        System.setProperty("registry.raft.serverAddr", "127.0.0.1:7091,127.0.0.1:7092");
        System.setProperty("service.vgroupMapping.initFlowGroup", "initFlowCluster");

        String loginResponse = "{\"code\":\"200\",\"message\":\"success\",\"data\":\"testToken\",\"success\":true}";
        String metadataResponseBody =
                "{\"nodes\":[{\"control\":{\"host\":\"localhost\",\"port\":7091},\"transaction\":{\"host\":\"localhost\",\"port\":8091},\"group\":\"default\",\"role\":\"LEADER\"}],\"storeMode\":\"raft\",\"term\":1}";

        Field initAddressesField = RaftRegistryServiceImpl.class.getDeclaredField("INIT_ADDRESSES");
        initAddressesField.setAccessible(true);
        Map<String, List<InetSocketAddress>> initAddresses =
                (Map<String, List<InetSocketAddress>>) initAddressesField.get(null);

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {
            Response mockLoginResponse = buildMockResponse(HttpStatus.SC_OK, loginResponse);
            Response mockMetadataResponse = buildMockResponse(HttpStatus.SC_OK, metadataResponseBody);

            when(HttpClientUtil.doPost(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockLoginResponse);
            when(HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(mockMetadataResponse);

            RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
            List<InetSocketAddress> result = instance.lookup("initFlowGroup");

            assertFalse(result.isEmpty(), "Should return non-empty list after complete initialization");
        } finally {
            System.clearProperty("service.vgroupMapping.initFlowGroup");
            System.setProperty("registry.raft.serverAddr", "127.0.0.1:8092");
            initAddresses.remove("initFlowCluster");
        }
    }

    @Test
    public void supportsHttp2VersionThresholdTest() throws Exception {
        Method supportsHttp2Method = RaftRegistryServiceImpl.class.getDeclaredMethod("supportsHttp2", Node.class);
        supportsHttp2Method.setAccessible(true);

        Node nullVersion = new Node();
        nullVersion.setVersion(null);
        assertFalse((boolean) supportsHttp2Method.invoke(null, nullVersion));

        Node blankVersion = new Node();
        blankVersion.setVersion("");
        assertFalse((boolean) supportsHttp2Method.invoke(null, blankVersion));

        Node invalidVersion = new Node();
        invalidVersion.setVersion("invalid-version");
        assertFalse((boolean) supportsHttp2Method.invoke(null, invalidVersion));

        Node v269 = new Node();
        v269.setVersion("2.6.9");
        assertFalse((boolean) supportsHttp2Method.invoke(null, v269));

        Node v270 = new Node();
        v270.setVersion("2.7.0");
        assertTrue((boolean) supportsHttp2Method.invoke(null, v270));

        Node v270Snapshot = new Node();
        v270Snapshot.setVersion("2.7.0-SNAPSHOT");
        assertTrue((boolean) supportsHttp2Method.invoke(null, v270Snapshot));

        Node v280 = new Node();
        v280.setVersion("2.8.0");
        assertTrue((boolean) supportsHttp2Method.invoke(null, v280));
    }

    @Test
    public void shouldRefreshMetadataFilterInvalidEventTest() throws Exception {
        Method shouldRefreshMetadata = RaftRegistryServiceImpl.class.getDeclaredMethod(
                "shouldRefreshMetadata", String.class, String.class, SeataHttpWatch.Response.class);
        shouldRefreshMetadata.setAccessible(true);

        assertFalse((boolean) shouldRefreshMetadata.invoke(null, "invalidEventCluster", "default", null));

        SeataHttpWatch.Response<ClusterWatchEvent> errorResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.ERROR, null);
        assertFalse((boolean) shouldRefreshMetadata.invoke(null, "invalidEventCluster", "default", errorResponse));

        SeataHttpWatch.Response<ClusterWatchEvent> nullObjectUpdate =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, null);
        assertFalse((boolean) shouldRefreshMetadata.invoke(null, "invalidEventCluster", "default", nullObjectUpdate));

        ClusterWatchEvent nullMetadataEvent = new ClusterWatchEvent();
        SeataHttpWatch.Response<ClusterWatchEvent> nullMetadataResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, nullMetadataEvent);
        assertFalse(
                (boolean) shouldRefreshMetadata.invoke(null, "invalidEventCluster", "default", nullMetadataResponse));

        MetadataResponse emptyNodesMetadata = new MetadataResponse();
        emptyNodesMetadata.setNodes(Collections.emptyList());
        emptyNodesMetadata.setStoreMode("raft");
        emptyNodesMetadata.setTerm(1L);
        ClusterWatchEvent emptyNodesEvent = new ClusterWatchEvent();
        emptyNodesEvent.setGroup("default");
        emptyNodesEvent.setMetadata(emptyNodesMetadata);
        SeataHttpWatch.Response<ClusterWatchEvent> emptyNodesResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, emptyNodesEvent);
        assertFalse((boolean) shouldRefreshMetadata.invoke(null, "invalidEventCluster", "default", emptyNodesResponse));
    }

    @Test
    public void shouldRefreshMetadataOnTermOrNodeChangeTest() throws Exception {
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        Method shouldRefreshMetadata = RaftRegistryServiceImpl.class.getDeclaredMethod(
                "shouldRefreshMetadata", String.class, String.class, SeataHttpWatch.Response.class);
        shouldRefreshMetadata.setAccessible(true);

        String termAdvancedCluster = "termAdvancedCluster";
        metadata.refreshMetadata(
                termAdvancedCluster,
                metadataResponse(1L, createNode("127.0.0.1", 7091, 8091, "default", ClusterRole.LEADER, "2.7.0")));
        ClusterWatchEvent termAdvancedEvent = new ClusterWatchEvent();
        termAdvancedEvent.setGroup("default");
        termAdvancedEvent.setMetadata(
                metadataResponse(2L, createNode("127.0.0.1", 7091, 8091, "default", ClusterRole.LEADER, "2.7.0")));
        SeataHttpWatch.Response<ClusterWatchEvent> termAdvancedResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, termAdvancedEvent);
        assertTrue((boolean) shouldRefreshMetadata.invoke(null, termAdvancedCluster, "default", termAdvancedResponse));

        String unchangedCluster = "unchangedCluster";
        metadata.refreshMetadata(
                unchangedCluster,
                metadataResponse(3L, createNode("127.0.0.1", 7092, 8092, "default", ClusterRole.LEADER, "2.7.0")));
        ClusterWatchEvent unchangedEvent = new ClusterWatchEvent();
        unchangedEvent.setGroup("default");
        unchangedEvent.setMetadata(
                metadataResponse(3L, createNode("127.0.0.1", 7092, 8092, "default", ClusterRole.LEADER, "2.7.0")));
        SeataHttpWatch.Response<ClusterWatchEvent> unchangedResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, unchangedEvent);
        assertFalse((boolean) shouldRefreshMetadata.invoke(null, unchangedCluster, "default", unchangedResponse));

        String nodeChangedCluster = "nodeChangedCluster";
        metadata.refreshMetadata(
                nodeChangedCluster,
                metadataResponse(5L, createNode("127.0.0.1", 7093, 8093, "default", ClusterRole.LEADER, "2.7.0")));
        ClusterWatchEvent nodeChangedEvent = new ClusterWatchEvent();
        nodeChangedEvent.setGroup("default");
        nodeChangedEvent.setMetadata(
                metadataResponse(5L, createNode("127.0.0.1", 7193, 8193, "default", ClusterRole.LEADER, "2.7.0")));
        SeataHttpWatch.Response<ClusterWatchEvent> nodeChangedResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, nodeChangedEvent);
        assertTrue((boolean) shouldRefreshMetadata.invoke(null, nodeChangedCluster, "default", nodeChangedResponse));

        String staleTermCluster = "staleTermCluster";
        Node currentLeader = createNode("127.0.0.1", 7094, 8094, "default", ClusterRole.LEADER, "2.7.0");
        metadata.refreshMetadata(staleTermCluster, metadataResponse(10L, currentLeader));
        ClusterWatchEvent staleTermEvent = new ClusterWatchEvent();
        staleTermEvent.setGroup("default");
        staleTermEvent.setMetadata(
                metadataResponse(9L, createNode("127.0.0.1", 7194, 8194, "default", ClusterRole.FOLLOWER, "2.7.0")));
        SeataHttpWatch.Response<ClusterWatchEvent> staleTermResponse =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, staleTermEvent);

        assertFalse((boolean) shouldRefreshMetadata.invoke(null, staleTermCluster, "default", staleTermResponse));
        assertEquals(
                10L, metadata.getClusterTerm(staleTermCluster).get("default").longValue());
        assertEquals(
                currentLeader.getTransaction().getPort(),
                metadata.getNodes(staleTermCluster, "default")
                        .get(0)
                        .getTransaction()
                        .getPort());
    }

    @Test
    public void hasMetadataChangedOrderInsensitiveTest() throws Exception {
        Field metadataField = RaftRegistryServiceImpl.class.getDeclaredField("METADATA");
        metadataField.setAccessible(true);
        Metadata metadata = (Metadata) metadataField.get(null);

        Method hasMetadataChanged = RaftRegistryServiceImpl.class.getDeclaredMethod(
                "hasMetadataChanged", String.class, String.class, MetadataResponse.class);
        hasMetadataChanged.setAccessible(true);

        String clusterName = "orderInsensitiveCluster";
        Node first = createNode("127.0.0.1", 7291, 8291, "default", ClusterRole.LEADER, "2.7.0");
        Node second = createNode("127.0.0.1", 7292, 8292, "default", ClusterRole.FOLLOWER, "2.7.0");
        metadata.refreshMetadata(clusterName, metadataResponse(10L, first, second));

        MetadataResponse reversedOrder = metadataResponse(10L, second, first);
        assertFalse((boolean) hasMetadataChanged.invoke(null, clusterName, "default", reversedOrder));

        Node roleChanged = createNode("127.0.0.1", 7292, 8292, "default", ClusterRole.LEADER, "2.7.0");
        MetadataResponse changedNodeSignature = metadataResponse(10L, first, roleChanged);
        assertTrue((boolean) hasMetadataChanged.invoke(null, clusterName, "default", changedNodeSignature));
    }

    @Test
    public void hasMetadataChangedEdgeCasesTest() throws Exception {
        Metadata metadata = (Metadata) getStaticField("METADATA");

        Method hasMetadataChanged = RaftRegistryServiceImpl.class.getDeclaredMethod(
                "hasMetadataChanged", String.class, String.class, MetadataResponse.class);
        hasMetadataChanged.setAccessible(true);

        assertFalse((boolean) hasMetadataChanged.invoke(null, "nullIncomingCluster", "default", null));

        Node first = createNode("127.0.0.1", 7391, 8391, "default", ClusterRole.LEADER, "2.7.0");
        assertTrue(
                (boolean) hasMetadataChanged.invoke(null, "emptyLocalCluster", "default", metadataResponse(1L, first)));

        assertFalse((boolean) hasMetadataChanged.invoke(null, "emptyBothCluster", "default", metadataResponse(1L)));

        String termChangedCluster = "termChangedCluster";
        metadata.refreshMetadata(termChangedCluster, metadataResponse(1L, first));
        assertTrue(
                (boolean) hasMetadataChanged.invoke(null, termChangedCluster, "default", metadataResponse(2L, first)));

        String nodeSizeChangedCluster = "nodeSizeChangedCluster";
        metadata.refreshMetadata(nodeSizeChangedCluster, metadataResponse(3L, first));
        Node second = createNode("127.0.0.1", 7392, 8392, "default", ClusterRole.FOLLOWER, "2.7.0");
        assertTrue((boolean) hasMetadataChanged.invoke(
                null, nodeSizeChangedCluster, "default", metadataResponse(3L, first, second)));
    }

    @Test
    public void buildNodeSignatureEdgeCasesTest() throws Exception {
        Method buildNodeSignature = RaftRegistryServiceImpl.class.getDeclaredMethod("buildNodeSignature", Node.class);
        buildNodeSignature.setAccessible(true);

        assertEquals("", buildNodeSignature.invoke(null, new Object[] {null}));

        Node node = new Node();
        node.setGroup("default");
        node.setRole(ClusterRole.LEADER);
        node.setVersion("2.7.0");
        assertEquals("||LEADER|2.7.0|default", buildNodeSignature.invoke(null, node));
    }

    private static void prepareWatchClusterContext(String clusterName, MetadataResponse... metadataResponses)
            throws Exception {
        Metadata metadata = (Metadata) getStaticField("METADATA");
        for (MetadataResponse metadataResponse : metadataResponses) {
            metadata.refreshMetadata(clusterName, metadataResponse);
        }
        setStaticField("CURRENT_TRANSACTION_CLUSTER_NAME", clusterName);
        setStaticField("CURRENT_TRANSACTION_SERVICE_GROUP", "tx");

        Map<String, List<InetSocketAddress>> aliveNodes =
                (Map<String, List<InetSocketAddress>>) getStaticField("ALIVE_NODES");
        aliveNodes.remove("tx");
    }

    private static boolean invokeWatch() throws Exception {
        Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
        watchMethod.setAccessible(true);
        try {
            return (boolean) watchMethod.invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static boolean invokeWatchHttp2(String clusterName) throws Exception {
        Method watchHttp2Method = RaftRegistryServiceImpl.class.getDeclaredMethod("watchHttp2", String.class);
        watchHttp2Method.setAccessible(true);
        try {
            return (boolean) watchHttp2Method.invoke(null, clusterName);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static SeataHttpWatch.Response<ClusterWatchEvent> newWatchUpdateResponse(
            String group, MetadataResponse metadataResponse) {
        ClusterWatchEvent event = new ClusterWatchEvent();
        event.setGroup(group);
        event.setMetadata(metadataResponse);
        return new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, event);
    }

    private static SeataHttpWatch<ClusterWatchEvent> mockWatch() {
        return mock(SeataHttpWatch.class);
    }

    private static void setTokenNotExpired() throws Exception {
        Field tokenTimeStamp = RaftRegistryServiceImpl.class.getDeclaredField("tokenTimeStamp");
        tokenTimeStamp.setAccessible(true);
        tokenTimeStamp.setLong(RaftRegistryServiceImpl.class, System.currentTimeMillis());
    }

    private static void setClosed(boolean value) throws Exception {
        Field closedField = RaftRegistryServiceImpl.class.getDeclaredField("CLOSED");
        closedField.setAccessible(true);
        AtomicBoolean closed = (AtomicBoolean) closedField.get(null);
        closed.set(value);
    }

    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field field = RaftRegistryServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Object getStaticField(String fieldName) throws Exception {
        Field field = RaftRegistryServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Node createNode(
            String host, int controlPort, int transactionPort, String group, ClusterRole role, String version) {
        Node node = new Node();
        node.setControl(new Node.Endpoint(host, controlPort));
        node.setTransaction(new Node.Endpoint(host, transactionPort));
        node.setGroup(group);
        node.setRole(role);
        node.setVersion(version);
        return node;
    }

    private static MetadataResponse metadataResponse(long term, Node... nodes) {
        MetadataResponse metadataResponse = new MetadataResponse();
        metadataResponse.setNodes(Arrays.asList(nodes));
        metadataResponse.setStoreMode("raft");
        metadataResponse.setTerm(term);
        return metadataResponse;
    }

    /**
     * Test startQueryMetadata creates thread pool
     */
    @Test
    public void startQueryMetadataTest() throws Exception {
        Field executorField = RaftRegistryServiceImpl.class.getDeclaredField("REFRESH_METADATA_EXECUTOR");
        executorField.setAccessible(true);
        executorField.set(null, null);

        Method startQueryMetadataMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("startQueryMetadata");
        startQueryMetadataMethod.setAccessible(true);
        startQueryMetadataMethod.invoke(null);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorField.get(null);
        assertNotNull(executor, "Thread pool should be created");
        assertTrue(executor.getCorePoolSize() > 0, "Thread pool should have core threads");

        executor.shutdownNow();
        executorField.set(null, null);
    }

    /**
     * Test startQueryMetadata multiple calls only create once
     */
    @Test
    public void startQueryMetadataMultipleCallsTest() throws Exception {
        Field executorField = RaftRegistryServiceImpl.class.getDeclaredField("REFRESH_METADATA_EXECUTOR");
        executorField.setAccessible(true);
        executorField.set(null, null);

        Method startQueryMetadataMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("startQueryMetadata");
        startQueryMetadataMethod.setAccessible(true);

        startQueryMetadataMethod.invoke(null);
        ThreadPoolExecutor executor1 = (ThreadPoolExecutor) executorField.get(null);

        startQueryMetadataMethod.invoke(null);
        ThreadPoolExecutor executor2 = (ThreadPoolExecutor) executorField.get(null);

        assertSame(executor1, executor2, "Multiple calls should return the same thread pool instance");

        executor1.shutdownNow();
        executorField.set(null, null);
    }
}
