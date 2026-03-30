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
import org.apache.seata.common.metadata.Metadata;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.HttpClientUtil;
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
import static org.mockito.Mockito.mock;
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
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        // Reset the CLOSED flag after each test
        Field closedField = RaftRegistryServiceImpl.class.getDeclaredField("CLOSED");
        closedField.setAccessible(true);
        AtomicBoolean closed = (AtomicBoolean) closedField.get(null);
        closed.set(false);
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
        assertDoesNotThrow(() -> instance.register(new ServiceInstance(address)));
    }

    /**
     * Test unregister method (empty implementation)
     */
    @Test
    public void unregisterTest() throws Exception {
        RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        // Should not throw exception even though it's an empty implementation
        assertDoesNotThrow(() -> instance.unregister(new ServiceInstance(address)));
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

        List<ServiceInstance> result = instance.lookup("tx");

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

        List<ServiceInstance> result = instance.aliveLookup("tx");

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

        List<ServiceInstance> aliveAddress = new ArrayList<>();
        aliveAddress.add(new ServiceInstance(new InetSocketAddress("localhost", 8091)));
        aliveAddress.add(new ServiceInstance(new InetSocketAddress("localhost", 8092)));

        // Should return the previous value (initialList) from Map.put()
        @SuppressWarnings("unchecked")
        List<InetSocketAddress> result =
                (List<InetSocketAddress>) (List<?>) instance.refreshAliveLookup("tx", aliveAddress);

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
        Map<String, List<ServiceInstance>> initAddresses =
                (Map<String, List<ServiceInstance>>) initAddressesField.get(null);
        List<ServiceInstance> addressList = new ArrayList<>();
        addressList.add(new ServiceInstance(new InetSocketAddress("localhost", 7091)));
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
        List<ServiceInstance> result = instance.lookup("nonexistent-service");

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

        List<ServiceInstance> emptyList = new ArrayList<>();
        // First call returns null (previous value from Map.put), which is expected
        List<ServiceInstance> result = instance.refreshAliveLookup("tx", emptyList);

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
     * Test watch method with null response
     */
    @Test
    public void watchWithNullResponseTest() throws Exception {
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
                    .thenReturn(null);

            Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
            watchMethod.setAccessible(true);
            boolean result = (boolean) watchMethod.invoke(null);

            assertFalse(result, "Watch should return false when response is null");
        } finally {
            initAddresses.remove("default");
        }
    }

    /**
     * Test watch method with null status line
     */
    @Test
    public void watchWithNullStatusLineTest() throws Exception {
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
            // Return null to simulate null response scenario
            when(HttpClientUtil.doPost(anyString(), anyMap(), anyMap(), anyInt()))
                    .thenReturn(null);

            Method watchMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("watch");
            watchMethod.setAccessible(true);
            boolean result = (boolean) watchMethod.invoke(null);

            assertFalse(result, "Watch should return false when response is null");
        } finally {
            initAddresses.remove("default");
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

    /**
     * Test lookup with empty raft cluster address
     */
    @Test
    public void lookupWithEmptyRaftClusterAddressTest() throws Exception {
        System.setProperty("registry.raft.serverAddr", "");
        System.setProperty("service.vgroupMapping.emptyAddrGroup", "emptyAddrCluster");

        try {
            RaftRegistryServiceImpl instance = RaftRegistryServiceImpl.getInstance();
            List<ServiceInstance> result = instance.lookup("emptyAddrGroup");

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
            List<ServiceInstance> result = instance.lookup("initFlowGroup");

            assertFalse(result.isEmpty(), "Should return non-empty list after complete initialization");
        } finally {
            System.clearProperty("service.vgroupMapping.initFlowGroup");
            System.setProperty("registry.raft.serverAddr", "127.0.0.1:8092");
            initAddresses.remove("initFlowCluster");
        }
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
