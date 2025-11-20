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

package org.seata.mcp.core.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.ProtocolErrorException;
import org.apache.seata.mcp.core.protocol.RuntimeTransport;
import org.apache.seata.mcp.core.protocol.ServerRuntimeSession;
import org.apache.seata.mcp.core.runtimeCore.NotificationProcessor;
import org.apache.seata.mcp.core.runtimeCore.RequestProcessor;
import org.apache.seata.mcp.core.runtimeCore.RuntimeExchangeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerRuntimeSessionTest {

    private static final String SESSION_ID = "session-1";

    @Mock
    private RuntimeTransport mockTransport;

    @Mock
    private RequestProcessor.InitializationProcessor mockInitProcessor;

    @Mock
    private RequestProcessor<Object> mockRequestProcessor;

    @Mock
    private NotificationProcessor mockNotificationProcessor;

    private ServerRuntimeSession createSession(
            Map<String, RequestProcessor<?>> requestHandlers, Map<String, NotificationProcessor> notificationHandlers) {
        return new ServerRuntimeSession(
                SESSION_ID,
                Duration.ofSeconds(5),
                mockTransport,
                mockInitProcessor,
                requestHandlers,
                notificationHandlers);
    }

    @Test
    void testBasicGettersAndHealth() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        assertEquals(SESSION_ID, session.getId());
        assertTrue(session.isHealthy());

        session.setHealthy(false);
        assertFalse(session.isHealthy());
    }

    @Test
    void testLoggingLevelAndNotificationFilter() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        assertTrue(session.isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel.INFO));
        assertTrue(session.isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel.ERROR));
        assertFalse(session.isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel.DEBUG));

        session.setMinLoggingLevel(ProtocolDefinition.LoggingLevel.ERROR);
        assertFalse(session.isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel.INFO));
        assertTrue(session.isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel.ERROR));
    }

    @Test
    void testSendRequestSuccess() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        when(mockTransport.sendMessage(any())).thenReturn(Mono.empty());
        when(mockTransport.unmarshalFrom(any(), any(TypeReference.class))).thenReturn("ok");

        TypeReference<String> typeRef = new TypeReference<String>() {};
        Mono<String> mono = session.sendRequest("test/method", Collections.singletonMap("k", "v"), typeRef);

        ProtocolDefinition.JSONRPCResponse resp = new ProtocolDefinition.JSONRPCResponse(
                ProtocolDefinition.JSONRPC_VERSION, SESSION_ID + "-0", "raw-result", null);

        StepVerifier.create(mono)
                .then(() -> {
                    when(mockTransport.unmarshalFrom(resp.getResult(), typeRef)).thenReturn("ok");
                    session.handle(resp).block();
                })
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    void testSendRequestErrorResponse() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        when(mockTransport.sendMessage(any())).thenReturn(Mono.empty());

        TypeReference<String> typeRef = new TypeReference<String>() {};
        Mono<String> mono = session.sendRequest("test/method", Collections.emptyMap(), typeRef);

        ProtocolDefinition.JSONRPCResponse.JSONRPCError error = new ProtocolDefinition.JSONRPCResponse.JSONRPCError(
                ProtocolDefinition.ErrorCodes.INTERNAL_ERROR, "error", null);
        ProtocolDefinition.JSONRPCResponse resp = new ProtocolDefinition.JSONRPCResponse(
                ProtocolDefinition.JSONRPC_VERSION, SESSION_ID + "-0", null, error);

        StepVerifier.create(mono)
                .then(() -> session.handle(resp).block())
                .expectError(ProtocolErrorException.class)
                .verify();
    }

    @Test
    void testSendRequestVoidType() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        when(mockTransport.sendMessage(any())).thenReturn(Mono.empty());

        TypeReference<Void> typeRef = new TypeReference<Void>() {};
        Mono<Void> mono = session.sendRequest("test/method", Collections.emptyMap(), typeRef);

        ProtocolDefinition.JSONRPCResponse resp = new ProtocolDefinition.JSONRPCResponse(
                ProtocolDefinition.JSONRPC_VERSION, SESSION_ID + "-0", "ignored", null);

        StepVerifier.create(mono).then(() -> session.handle(resp).block()).verifyComplete();
    }

    @Test
    void testHandleResponseNoPending() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());
        ProtocolDefinition.JSONRPCResponse resp = new ProtocolDefinition.JSONRPCResponse(
                ProtocolDefinition.JSONRPC_VERSION, "unknown-id", "result", null);
        StepVerifier.create(session.handle(resp)).verifyComplete();
    }

    @Test
    void testSendNotificationDelegatesToTransport() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());
        when(mockTransport.sendMessage(any())).thenReturn(Mono.empty());

        StepVerifier.create(session.sendNotification("notify/method", Collections.singletonMap("k", "v")))
                .verifyComplete();

        verify(mockTransport).sendMessage(any(ProtocolDefinition.JSONRPCNotification.class));
    }

    @Test
    void testHandleInitializeRequest() {
        Map<String, RequestProcessor<?>> requestHandlers = new HashMap<>();
        ServerRuntimeSession session = createSession(requestHandlers, Collections.emptyMap());

        when(mockTransport.sendMessage(any(ProtocolDefinition.JSONRPCResponse.class)))
                .thenReturn(Mono.empty());

        ProtocolDefinition.InitializeRequest initReq = new ProtocolDefinition.InitializeRequest();

        when(mockTransport.unmarshalFrom(any(), any(TypeReference.class))).thenReturn(initReq);

        ProtocolDefinition.InitializeResult initResult =
                new ProtocolDefinition.InitializeResult("ver", null, null, "inst");
        when(mockInitProcessor.handle(initReq)).thenReturn(Mono.just(initResult));

        ProtocolDefinition.JSONRPCRequest req = new ProtocolDefinition.JSONRPCRequest(
                ProtocolDefinition.JSONRPC_VERSION, ProtocolDefinition.METHOD_INITIALIZE, "1", Collections.emptyMap());

        StepVerifier.create(session.handle(req)).verifyComplete();

        ArgumentCaptor<ProtocolDefinition.JSONRPCResponse> captor =
                ArgumentCaptor.forClass(ProtocolDefinition.JSONRPCResponse.class);
        verify(mockTransport).sendMessage(captor.capture());
        ProtocolDefinition.JSONRPCResponse resp = captor.getValue();
        assertEquals("1", resp.getId());
        assertNull(resp.getError());
        assertNotNull(resp.getResult());
    }

    @Test
    void testHandleRequestWithHandler() {
        Map<String, RequestProcessor<?>> requestHandlers = new HashMap<>();
        requestHandlers.put("test/method", mockRequestProcessor);

        Map<String, NotificationProcessor> notificationHandlers = new HashMap<>();
        ServerRuntimeSession session = createSession(requestHandlers, notificationHandlers);

        when(mockTransport.sendMessage(any(ProtocolDefinition.JSONRPCResponse.class)))
                .thenReturn(Mono.empty());
        when(mockRequestProcessor.handle(any(RuntimeExchangeContext.class), any()))
                .thenReturn(Mono.just("ok"));

        ProtocolDefinition.ClientCapabilities caps = new ProtocolDefinition.ClientCapabilities();
        ProtocolDefinition.Implementation clientInfo = new ProtocolDefinition.Implementation("client", "1.0");
        session.init(caps, clientInfo);

        ProtocolDefinition.JSONRPCNotification initNotif = new ProtocolDefinition.JSONRPCNotification(
                ProtocolDefinition.JSONRPC_VERSION, ProtocolDefinition.METHOD_NOTIFICATION_INITIALIZED, null);
        StepVerifier.create(session.handle(initNotif)).verifyComplete();

        ProtocolDefinition.JSONRPCRequest req = new ProtocolDefinition.JSONRPCRequest(
                ProtocolDefinition.JSONRPC_VERSION, "test/method", "2", Collections.singletonMap("k", "v"));

        StepVerifier.create(session.handle(req)).verifyComplete();

        verify(mockRequestProcessor).handle(any(RuntimeExchangeContext.class), any());
        verify(mockTransport).sendMessage(any(ProtocolDefinition.JSONRPCResponse.class));
    }

    @Test
    void testHandleRequestMethodNotFound() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        when(mockTransport.sendMessage(any(ProtocolDefinition.JSONRPCResponse.class)))
                .thenReturn(Mono.empty());

        ProtocolDefinition.JSONRPCRequest req = new ProtocolDefinition.JSONRPCRequest(
                ProtocolDefinition.JSONRPC_VERSION, "unknown/method", "3", Collections.emptyMap());

        StepVerifier.create(session.handle(req)).verifyComplete();

        ArgumentCaptor<ProtocolDefinition.JSONRPCResponse> captor =
                ArgumentCaptor.forClass(ProtocolDefinition.JSONRPCResponse.class);
        verify(mockTransport).sendMessage(captor.capture());
        ProtocolDefinition.JSONRPCResponse resp = captor.getValue();
        assertEquals("3", resp.getId());
        assertNotNull(resp.getError());
        assertEquals(
                ProtocolDefinition.ErrorCodes.METHOD_NOT_FOUND, resp.getError().getCode());
    }

    @Test
    void testHandleNotificationWithHandler() {
        Map<String, RequestProcessor<?>> requestHandlers = new HashMap<>();
        Map<String, NotificationProcessor> notificationHandlers = new HashMap<>();
        notificationHandlers.put("notify/method", mockNotificationProcessor);

        ServerRuntimeSession session = createSession(requestHandlers, notificationHandlers);

        when(mockNotificationProcessor.handle(any(RuntimeExchangeContext.class), any()))
                .thenReturn(Mono.empty());

        ProtocolDefinition.ClientCapabilities caps = new ProtocolDefinition.ClientCapabilities();
        ProtocolDefinition.Implementation clientInfo = new ProtocolDefinition.Implementation("client", "1.0");
        session.init(caps, clientInfo);

        ProtocolDefinition.JSONRPCNotification initNotif = new ProtocolDefinition.JSONRPCNotification(
                ProtocolDefinition.JSONRPC_VERSION, ProtocolDefinition.METHOD_NOTIFICATION_INITIALIZED, null);
        StepVerifier.create(session.handle(initNotif)).verifyComplete();

        ProtocolDefinition.JSONRPCNotification notif = new ProtocolDefinition.JSONRPCNotification(
                ProtocolDefinition.JSONRPC_VERSION, "notify/method", Collections.singletonMap("k", "v"));

        StepVerifier.create(session.handle(notif)).verifyComplete();

        verify(mockNotificationProcessor).handle(any(RuntimeExchangeContext.class), any());
    }

    @Test
    void testHandleNotificationWithoutHandler() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        ProtocolDefinition.JSONRPCNotification notif =
                new ProtocolDefinition.JSONRPCNotification(ProtocolDefinition.JSONRPC_VERSION, "no/handler", null);

        StepVerifier.create(session.handle(notif)).verifyComplete();
    }

    @Test
    void testCloseGracefullyDelegatesToTransport() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        when(mockTransport.closeGracefully()).thenReturn(Mono.empty());

        StepVerifier.create(session.closeGracefully()).verifyComplete();

        verify(mockTransport).closeGracefully();
    }

    @Test
    void testCloseDelegatesToTransport() {
        ServerRuntimeSession session = createSession(Collections.emptyMap(), Collections.emptyMap());

        session.close();

        verify(mockTransport).close();
    }
}
