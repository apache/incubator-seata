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
import org.apache.seata.mcp.core.protocol.ServerRuntimeSession;
import org.apache.seata.mcp.core.runtimeCore.channel.WebMvcSseChannelProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WebMvcSseChannelProvider
 */
@ExtendWith(MockitoExtension.class)
class WebMvcSseChannelProviderTest {

    private static final String TEST_MSG_ENDPOINT = "/test/message";
    private static final String TEST_SSE_ENDPOINT = "/test/sse";
    private static final String TEST_BASE_URL = "http://localhost:8080";
    private static final String TEST_SESSION_ID = "test-session-id";

    @Mock
    private ServerRuntimeSession.Factory mockFactory;

    @Mock
    private ServerRuntimeSession mockSession;

    @Mock
    private ServerRequest mockRequest;

    private ObjectMapper realMapper;
    private WebMvcSseChannelProvider provider;

    @BeforeEach
    void setUp() {
        realMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (provider != null) {
            try {
                provider.closeGracefully().block();
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }

    @Test
    void testConstructorWithThreeParameters() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);

        assertNotNull(provider);
        assertNotNull(provider.getRouterFunction());
    }

    @Test
    void testConstructorWithFourParameters() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_BASE_URL, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);

        assertNotNull(provider);
        assertNotNull(provider.getRouterFunction());
    }

    @Test
    void testConstructorWithNullMapper() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WebMvcSseChannelProvider(null, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT));
    }

    @Test
    void testConstructorWithNullBaseUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WebMvcSseChannelProvider(realMapper, null, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT));
    }

    @Test
    void testConstructorWithNullMsgEndpoint() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WebMvcSseChannelProvider(realMapper, TEST_BASE_URL, null, TEST_SSE_ENDPOINT));
    }

    @Test
    void testConstructorWithNullSseEndpoint() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WebMvcSseChannelProvider(realMapper, TEST_BASE_URL, TEST_MSG_ENDPOINT, null));
    }

    @Test
    void testProtocolVersions() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        List<String> versions = provider.protocolVersions();
        assertNotNull(versions);
        assertEquals(2, versions.size());
        assertTrue(versions.contains(ProtocolDefinition.VERSION_2024_11_05));
        assertTrue(versions.contains(ProtocolDefinition.VERSION_2025_06_18));
    }

    @Test
    void testSetSessionFactory() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        provider.setSessionFactory(mockFactory);
        assertNotNull(provider);
    }

    @Test
    void testGetRouterFunction() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        RouterFunction<ServerResponse> router = provider.getRouterFunction();
        assertNotNull(router);
    }

    @Test
    void testNotifyClientsWithEmptySessions() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);

        StepVerifier.create(provider.notifyClients("test/method", "params")).verifyComplete();
    }

    @Test
    void testNotifyClientsWithActiveSessions() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        Field activeSessionsField = WebMvcSseChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerRuntimeSession> activeSessions =
                (Map<String, ServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.sendNotification(anyString(), any())).thenReturn(Mono.empty());
        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.notifyClients("test/method", "params")).verifyComplete();
    }

    @Test
    void testNotifyClientsWithException() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        Field activeSessionsField = WebMvcSseChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerRuntimeSession> activeSessions =
                (Map<String, ServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.getId()).thenReturn(TEST_SESSION_ID);
        when(mockSession.sendNotification(anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Test exception")));
        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.notifyClients("test/method", "params")).verifyComplete();
    }

    @Test
    void testCloseGracefully() {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);

        StepVerifier.create(provider.closeGracefully()).verifyComplete();
    }

    @Test
    void testCloseGracefullyWithActiveSessions() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);

        Field activeSessionsField = WebMvcSseChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerRuntimeSession> activeSessions =
                (Map<String, ServerRuntimeSession>) activeSessionsField.get(provider);

        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        StepVerifier.create(provider.closeGracefully()).verifyComplete();
    }

    @Test
    void testConnectSseWithShuttingDown() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        provider.closeGracefully().block();
        Method connectSseMethod = WebMvcSseChannelProvider.class.getDeclaredMethod("connectSse", ServerRequest.class);
        connectSseMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) connectSseMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testProcessMessageWithShuttingDown() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        provider.closeGracefully().block();
        Method processMessageMethod =
                WebMvcSseChannelProvider.class.getDeclaredMethod("processMessage", ServerRequest.class);
        processMessageMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) processMessageMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testProcessMessageWithMissingSessionId() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        when(mockRequest.param("sessionId")).thenReturn(Optional.empty());
        Method processMessageMethod =
                WebMvcSseChannelProvider.class.getDeclaredMethod("processMessage", ServerRequest.class);
        processMessageMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) processMessageMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testProcessMessageWithNonExistentSession() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        when(mockRequest.param("sessionId")).thenReturn(Optional.of("non-existent"));
        Method processMessageMethod =
                WebMvcSseChannelProvider.class.getDeclaredMethod("processMessage", ServerRequest.class);
        processMessageMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) processMessageMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }

    @Test
    void testProcessMessageWithValidSession() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        Field activeSessionsField = WebMvcSseChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerRuntimeSession> activeSessions =
                (Map<String, ServerRuntimeSession>) activeSessionsField.get(provider);

        ProtocolDefinition.JSONRPCRequest request = new ProtocolDefinition.JSONRPCRequest();
        request.setId("1");
        request.setMethod("test/method");
        request.setParams("params");
        when(mockSession.handle(any(ProtocolDefinition.JSONRPCMessage.class))).thenReturn(Mono.empty());
        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);
        when(mockRequest.param("sessionId")).thenReturn(Optional.of(TEST_SESSION_ID));
        when(mockRequest.body(String.class)).thenReturn(realMapper.writeValueAsString(request));
        Method processMessageMethod =
                WebMvcSseChannelProvider.class.getDeclaredMethod("processMessage", ServerRequest.class);
        processMessageMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) processMessageMethod.invoke(provider, mockRequest);
        assertNotNull(response);
    }

    @Test
    void testProcessMessageWithInvalidBody() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        Field activeSessionsField = WebMvcSseChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerRuntimeSession> activeSessions =
                (Map<String, ServerRuntimeSession>) activeSessionsField.get(provider);
        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);
        when(mockRequest.param("sessionId")).thenReturn(Optional.of(TEST_SESSION_ID));
        when(mockRequest.body(String.class)).thenReturn("invalid json");
        Method processMessageMethod =
                WebMvcSseChannelProvider.class.getDeclaredMethod("processMessage", ServerRequest.class);
        processMessageMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) processMessageMethod.invoke(provider, mockRequest);
        assertNotNull(response);
    }

    @Test
    void testProcessMessageWithException() throws Exception {
        provider = new WebMvcSseChannelProvider(realMapper, TEST_MSG_ENDPOINT, TEST_SSE_ENDPOINT);
        Field activeSessionsField = WebMvcSseChannelProvider.class.getDeclaredField("activeSessions");
        activeSessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerRuntimeSession> activeSessions =
                (Map<String, ServerRuntimeSession>) activeSessionsField.get(provider);

        ProtocolDefinition.JSONRPCRequest request = new ProtocolDefinition.JSONRPCRequest();
        request.setId("1");
        request.setMethod("test/method");
        request.setParams("params");

        when(mockSession.handle(any(ProtocolDefinition.JSONRPCMessage.class)))
                .thenReturn(Mono.error(new RuntimeException("Test exception")));
        when(mockSession.closeGracefully()).thenReturn(Mono.empty());
        activeSessions.put(TEST_SESSION_ID, mockSession);

        when(mockRequest.param("sessionId")).thenReturn(Optional.of(TEST_SESSION_ID));
        when(mockRequest.body(String.class)).thenReturn(realMapper.writeValueAsString(request));
        Method processMessageMethod =
                WebMvcSseChannelProvider.class.getDeclaredMethod("processMessage", ServerRequest.class);
        processMessageMethod.setAccessible(true);
        ServerResponse response = (ServerResponse) processMessageMethod.invoke(provider, mockRequest);

        assertNotNull(response);
    }
}
