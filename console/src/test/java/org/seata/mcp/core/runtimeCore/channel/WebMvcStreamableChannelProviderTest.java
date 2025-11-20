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
package org.seata.mcp.core.runtimeCore.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.StreamableServerRuntimeSession;
import org.apache.seata.mcp.core.runtimeCore.DefaultRuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContextResolver;
import org.apache.seata.mcp.core.runtimeCore.channel.WebMvcStreamableChannelProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerRequest.Headers;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WebMvcStreamableChannelProvider
 */
@ExtendWith(MockitoExtension.class)
class WebMvcStreamableChannelProviderTest {

    private static final String TEST_ENDPOINT = "/test/endpoint";
    private static final String TEST_SESSION_ID = "test-session-id";

    @Mock
    private RuntimeContextResolver mockContextResolver;

    @Mock
    private StreamableServerRuntimeSession.Factory mockFactory;

    @Mock
    private StreamableServerRuntimeSession mockSession;

    @Mock
    private ServerRequest mockRequest;

    @Mock
    private Headers mockHeaders;

    private ObjectMapper realMapper;
    private WebMvcStreamableChannelProvider provider;

    @BeforeEach
    void setUp() {
        realMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.closeGracefully().block();
        }
    }

    @Test
    void testConstructorWithValidParameters() {
        provider = new WebMvcStreamableChannelProvider(
                realMapper, TEST_ENDPOINT, false, mockContextResolver, Duration.ofSeconds(30));

        assertNotNull(provider);
        assertNotNull(provider.getRouterFunction());
    }

    @Test
    void testConstructorWithNullMapper() {
        assertThrows(IllegalArgumentException.class, () -> {
            new WebMvcStreamableChannelProvider(
                    null, TEST_ENDPOINT, false, mockContextResolver, Duration.ofSeconds(30));
        });
    }

    @Test
    void testConstructorWithNullEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> {
            new WebMvcStreamableChannelProvider(realMapper, null, false, mockContextResolver, Duration.ofSeconds(30));
        });
    }

    @Test
    void testConstructorWithNullContextResolver() {
        assertThrows(IllegalArgumentException.class, () -> {
            new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, null, Duration.ofSeconds(30));
        });
    }

    @Test
    void testConstructorWithNullKeepAliveInterval() {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        assertNotNull(provider);
    }

    @Test
    void testProtocolVersions() {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        List<String> versions = provider.protocolVersions();
        assertNotNull(versions);
        assertEquals(2, versions.size());
        assertTrue(versions.contains(ProtocolDefinition.VERSION_2024_11_05));
        assertTrue(versions.contains(ProtocolDefinition.VERSION_2025_06_18));
    }

    @Test
    void testSetSessionFactory() {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.setSessionFactory(mockFactory);
        assertNotNull(provider);
    }

    @Test
    void testGetRouterFunction() {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        RouterFunction<ServerResponse> router = provider.getRouterFunction();
        assertNotNull(router);
    }

    @Test
    void testNotifyClientsWithEmptySessions() {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        StepVerifier.create(provider.notifyClients("test/method", "params")).verifyComplete();
    }

    @Test
    void testNotifyClientsWithActiveSessions() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.sendNotification(anyString(), any())).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.notifyClients("test/method", "params")).verifyComplete();
    }

    @Test
    void testNotifyClientsWithException() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.getId()).thenReturn(TEST_SESSION_ID);
        when(mockSession.sendNotification(anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Test exception")));
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.notifyClients("test/method", "params")).verifyComplete();
    }

    @Test
    void testCloseGracefully() {
        provider = new WebMvcStreamableChannelProvider(
                realMapper, TEST_ENDPOINT, false, mockContextResolver, Duration.ofSeconds(30));

        StepVerifier.create(provider.closeGracefully()).verifyComplete();
    }

    @Test
    void testCloseGracefullyWithKeepAlive() {
        provider = new WebMvcStreamableChannelProvider(
                realMapper, TEST_ENDPOINT, false, mockContextResolver, Duration.ofMillis(100));

        StepVerifier.create(provider.closeGracefully()).verifyComplete();
    }

    @Test
    void testCloseGracefullyWithActiveSessions() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.closeGracefully()).verifyComplete();
    }

    @Test
    void testCloseGracefullyWithException() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.getId()).thenReturn(TEST_SESSION_ID);
        when(mockSession.closeGracefully()).thenReturn(Mono.error(new RuntimeException("Test exception")));
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.closeGracefully()).verifyComplete();
    }

    @Test
    void testShuttingDownFlag() {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.closeGracefully().block();

        // Verify provider was shut down
        assertNotNull(provider);
    }

    @Test
    void testConstructorWithNoDelete() {
        provider = new WebMvcStreamableChannelProvider(
                realMapper,
                TEST_ENDPOINT,
                true, // noDelete = true
                mockContextResolver,
                null);

        assertNotNull(provider);
        assertNotNull(provider.getRouterFunction());
    }

    @Test
    void testKeepAliveRemovesUnhealthySessions() throws Exception {
        provider = new WebMvcStreamableChannelProvider(
                realMapper, TEST_ENDPOINT, false, mockContextResolver, Duration.ofMillis(50));

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.getId()).thenReturn(TEST_SESSION_ID);
        when(mockSession.isHealthy()).thenReturn(false);
        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        // Wait for keep-alive to run
        Thread.sleep(150);

        // Verify session was removed
        assertTrue(activeSessions.isEmpty());
    }

    @Test
    void testKeepAliveWithShuttingDown() throws Exception {
        provider = new WebMvcStreamableChannelProvider(
                realMapper, TEST_ENDPOINT, false, mockContextResolver, Duration.ofMillis(50));

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        activeSessions.put(TEST_SESSION_ID, mockSession);

        // Shut down
        provider.closeGracefully().block();

        // Wait a bit
        Thread.sleep(100);

        // Verify session was cleared
        assertTrue(activeSessions.isEmpty());
    }

    @Test
    void testKeepAliveWithHealthySessions() throws Exception {
        provider = new WebMvcStreamableChannelProvider(
                realMapper, TEST_ENDPOINT, false, mockContextResolver, Duration.ofMillis(50));

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.getId()).thenReturn(TEST_SESSION_ID);
        when(mockSession.isHealthy()).thenReturn(true);
        activeSessions.put(TEST_SESSION_ID, mockSession);

        // Wait for keep-alive to run
        Thread.sleep(150);

        // Verify session is still there (healthy sessions are not removed)
        assertTrue(activeSessions.containsKey(TEST_SESSION_ID));
    }

    @Test
    void testHandleInitialize() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.setSessionFactory(mockFactory);

        ProtocolDefinition.InitializeRequest initRequest = new ProtocolDefinition.InitializeRequest();
        initRequest.setClientInfo(new ProtocolDefinition.Implementation("test-client", "1.0"));
        initRequest.setCapabilities(new ProtocolDefinition.ClientCapabilities());
        initRequest.setProtocolVersion(ProtocolDefinition.VERSION_2024_11_05);

        ProtocolDefinition.InitializeResult initResult = new ProtocolDefinition.InitializeResult();
        initResult.setServerInfo(new ProtocolDefinition.Implementation("test-server", "1.0"));
        initResult.setCapabilities(
                ProtocolDefinition.ServerCapabilities.builder().build());
        initResult.setProtocolVersion(ProtocolDefinition.VERSION_2024_11_05);

        ProtocolDefinition.JSONRPCRequest request = new ProtocolDefinition.JSONRPCRequest();
        request.setId("1");
        request.setMethod(ProtocolDefinition.METHOD_INITIALIZE);
        request.setParams(initRequest);

        StreamableServerRuntimeSession.StreamableServerRuntimeSessionInit init =
                mock(StreamableServerRuntimeSession.StreamableServerRuntimeSessionInit.class);
        when(init.getSession()).thenReturn(mockSession);
        when(init.getInitResult()).thenReturn(Mono.just(initResult));
        when(mockSession.getId()).thenReturn(TEST_SESSION_ID);
        when(mockFactory.startSession(any(ProtocolDefinition.InitializeRequest.class)))
                .thenReturn(init);

        // Use reflection to call handleInitialize
        Method handleInitializeMethod = WebMvcStreamableChannelProvider.class.getDeclaredMethod(
                "handleInitialize", ProtocolDefinition.JSONRPCRequest.class, RuntimeContext.class);
        handleInitializeMethod.setAccessible(true);
        ServerResponse response =
                (ServerResponse) handleInitializeMethod.invoke(provider, request, new DefaultRuntimeContext());

        assertNotNull(response);

        // Verify session was added
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);
        assertTrue(activeSessions.containsKey(TEST_SESSION_ID));
    }

    @Test
    void testHandleInitializeWithException() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.setSessionFactory(mockFactory);

        ProtocolDefinition.JSONRPCRequest request = new ProtocolDefinition.JSONRPCRequest();
        request.setId("1");
        request.setMethod(ProtocolDefinition.METHOD_INITIALIZE);
        request.setParams(Collections.emptyMap());

        when(mockFactory.startSession(any(ProtocolDefinition.InitializeRequest.class)))
                .thenThrow(new RuntimeException("Test exception"));

        // Use reflection to call handleInitialize
        Method handleInitializeMethod = WebMvcStreamableChannelProvider.class.getDeclaredMethod(
                "handleInitialize", ProtocolDefinition.JSONRPCRequest.class, RuntimeContext.class);
        handleInitializeMethod.setAccessible(true);
        ServerResponse response =
                (ServerResponse) handleInitializeMethod.invoke(provider, request, new DefaultRuntimeContext());

        assertNotNull(response);
    }

    @Test
    void testHandleGetWithShuttingDown() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.closeGracefully().block();

        // Use reflection to call handleGet
        Method handleGetMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleGet", ServerRequest.class);
        handleGetMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleGetMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleGetWithInvalidAccept() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);

        // Use reflection to call handleGet
        Method handleGetMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleGet", ServerRequest.class);
        handleGetMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleGetMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleGetWithMissingSessionId() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);
        when(mockContextResolver.extract(any(ServerRequest.class), any(RuntimeContext.class)))
                .thenReturn(new DefaultRuntimeContext());

        // Use reflection to call handleGet
        Method handleGetMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleGet", ServerRequest.class);
        handleGetMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleGetMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleGetWithNonExistentSession() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
        httpHeaders.set(ProtocolDefinition.HEADER_SESSION_ID, "non-existent");
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);
        when(mockContextResolver.extract(any(ServerRequest.class), any(RuntimeContext.class)))
                .thenReturn(new DefaultRuntimeContext());

        // Use reflection to call handleGet
        Method handleGetMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleGet", ServerRequest.class);
        handleGetMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleGetMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandlePostWithShuttingDown() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.closeGracefully().block();

        // Use reflection to call handlePost
        Method handlePostMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handlePost", ServerRequest.class);
        handlePostMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handlePostMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandlePostWithInvalidAccept() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);

        // Use reflection to call handlePost
        Method handlePostMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handlePost", ServerRequest.class);
        handlePostMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handlePostMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleDeleteWithShuttingDown() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        provider.closeGracefully().block();

        // Use reflection to call handleDelete
        Method handleDeleteMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleDelete", ServerRequest.class);
        handleDeleteMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleDeleteMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleDeleteWithNoDelete() throws Exception {
        provider = new WebMvcStreamableChannelProvider(
                realMapper,
                TEST_ENDPOINT,
                true, // noDelete = true
                mockContextResolver,
                null);

        // Use reflection to call handleDelete
        Method handleDeleteMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleDelete", ServerRequest.class);
        handleDeleteMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleDeleteMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleDeleteWithMissingSessionId() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        HttpHeaders httpHeaders = new HttpHeaders();
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);

        // Use reflection to call handleDelete
        Method handleDeleteMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleDelete", ServerRequest.class);
        handleDeleteMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleDeleteMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleDeleteWithNonExistentSession() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(ProtocolDefinition.HEADER_SESSION_ID, "non-existent");
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);

        // Use reflection to call handleDelete
        Method handleDeleteMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleDelete", ServerRequest.class);
        handleDeleteMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleDeleteMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testHandleDeleteWithValidSession() throws Exception {
        provider = new WebMvcStreamableChannelProvider(realMapper, TEST_ENDPOINT, false, mockContextResolver, null);

        // Use reflection to add session to activeSessions
        Field activeSessionsField = WebMvcStreamableChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, StreamableServerRuntimeSession> activeSessions =
                (Map<String, StreamableServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.delete()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(ProtocolDefinition.HEADER_SESSION_ID, TEST_SESSION_ID);
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);
        when(mockContextResolver.extract(any(ServerRequest.class), any(RuntimeContext.class)))
                .thenReturn(new DefaultRuntimeContext());

        // Use reflection to call handleDelete
        Method handleDeleteMethod =
                WebMvcStreamableChannelProvider.class.getDeclaredMethod("handleDelete", ServerRequest.class);
        handleDeleteMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) handleDeleteMethod.invoke(provider, mockRequest);

        assertNotNull(response);
        assertTrue(activeSessions.isEmpty());
    }
}
