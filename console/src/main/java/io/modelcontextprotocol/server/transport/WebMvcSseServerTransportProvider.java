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

/*
 * ------------------------------------------------------------------------
 * This file contains code originally from the [Model Context Protocol Java SDK],
 * which is licensed under the MIT License.
 *
 * The original MIT license text is reproduced below:
 * ------------------------------------------------------------------------
 */

/*
 * MIT License
 * Copyright (c) 2025 the original author or authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.ServerResponse.SseBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side implementation of the Model Context Protocol (MCP) transport layer using
 * HTTP with Server-Sent Events (SSE) through Spring WebMVC. This implementation provides
 * a bridge between synchronous WebMVC operations and reactive programming patterns to
 * maintain compatibility with the reactive transport interface.
 * @see McpServerTransportProvider
 * @see RouterFunction
 */
public class WebMvcSseServerTransportProvider implements McpServerTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcSseServerTransportProvider.class);

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * Event type for HeartBeat sent through the SSE connection.
     */
    public static final String HEARTBEAT_EVENT_TYPE = "heartbeat";

    /**
     * Event type for sending the message endpoint URI to clients.
     */
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    /**
     * Default SSE endpoint path as specified by the MCP transport specification.
     */
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";

    private final ObjectMapper objectMapper;

    private final String messageEndpoint;

    private final String sseEndpoint;

    private final String baseUrl;

    private final RouterFunction<ServerResponse> routerFunction;

    private McpServerSession.Factory sessionFactory;

    /**
     * Map of active client sessions, keyed by session ID.
     */
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    /**
     * Flag indicating if the transport is shutting down.
     */
    private volatile boolean isClosing = false;

    /**
     * Constructs a new WebMvcSseServerTransportProvider instance with the default SSE
     * endpoint.
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     * of messages.
     * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
     * messages via HTTP POST. This endpoint will be communicated to clients through the
     * SSE connection's initial endpoint event.
     * @throws IllegalArgumentException if either objectMapper or messageEndpoint is null
     */
    public WebMvcSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint) {
        this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT);
    }

    /**
     * Constructs a new WebMvcSseServerTransportProvider instance.
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     * of messages.
     * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
     * messages via HTTP POST. This endpoint will be communicated to clients through the
     * SSE connection's initial endpoint event.
     * @param sseEndpoint The endpoint URI where clients establish their SSE connections.
     * @throws IllegalArgumentException if any parameter is null
     */
    public WebMvcSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
        this(objectMapper, "", messageEndpoint, sseEndpoint);
    }

    /**
     * Constructs a new WebMvcSseServerTransportProvider instance.
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     * of messages.
     * @param baseUrl The base URL for the message endpoint, used to construct the full
     * endpoint URL for clients.
     * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
     * messages via HTTP POST. This endpoint will be communicated to clients through the
     * SSE connection's initial endpoint event.
     * @param sseEndpoint The endpoint URI where clients establish their SSE connections.
     * @throws IllegalArgumentException if any parameter is null
     */
    public WebMvcSseServerTransportProvider(
            ObjectMapper objectMapper, String baseUrl, String messageEndpoint, String sseEndpoint) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(baseUrl, "Message base URL must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");

        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.routerFunction = RouterFunctions.route()
                .GET(this.sseEndpoint, this::handleSseConnection)
                .POST(this.messageEndpoint, this::handleMessage)
                .build();
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Broadcasts a notification to all connected clients through their SSE connections.
     * The message is serialized to JSON and sent as an SSE event with type "message". If
     * any errors occur during sending to a particular client, they are logged but don't
     * prevent sending to other clients.
     * @param method The method name for the notification
     * @param params The parameters for the notification
     * @return A Mono that completes when the broadcast attempt is finished
     */
    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

        return Flux.fromStream(sessions.values().stream())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(e -> logger.error(
                                "Failed to " + "send message to session " + "{}: {}", session.getId(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    /**
     * Initiates a graceful shutdown of the transport. This method:
     * <ul>
     * <li>Sets the closing flag to prevent new connections</li>
     * <li>Closes all active SSE connections</li>
     * <li>Removes all session records</li>
     * </ul>
     * @return A Mono that completes when all cleanup operations are finished
     */
    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(sessions.values())
                .doFirst(() -> logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size()))
                .flatMap(McpServerSession::closeGracefully)
                .then();
    }

    /**
     * Returns the RouterFunction that defines the HTTP endpoints for this transport. The
     * router function handles two endpoints:
     * <ul>
     * <li>GET /sse - For establishing SSE connections</li>
     * <li>POST [messageEndpoint] - For receiving JSON-RPC messages from clients</li>
     * </ul>
     * @return The configured RouterFunction for handling HTTP requests
     */
    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    @Override
    public List<String> protocolVersions() {
        return Arrays.asList(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26);
    }

    /**
     * Handles new SSE connection requests from clients by creating a new session and
     * establishing an SSE connection. This method:
     * <ul>
     * <li>Generates a unique session ID</li>
     * <li>Creates a new session with a WebMvcMcpSessionTransport</li>
     * <li>Sends an initial endpoint event to inform the client where to send
     * messages</li>
     * <li>Maintains the session in the sessions map</li>
     * </ul>
     * @param request The incoming server request
     * @return A ServerResponse configured for SSE communication, or an error response if
     * the server is shutting down or the connection fails
     */
    private ServerResponse handleSseConnection(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        String sessionId = UUID.randomUUID().toString();
        logger.debug("Creating new SSE connection for session: {}", sessionId);

        // Send initial endpoint event
        try {
            return ServerResponse.sse(
                    sseBuilder -> {
                        sseBuilder.onComplete(() -> {
                            logger.debug("SSE connection completed for session: {}", sessionId);
                            sessions.remove(sessionId);
                        });
                        sseBuilder.onTimeout(() -> {
                            logger.debug("SSE connection timed out for session: {}", sessionId);
                            sessions.remove(sessionId);
                        });

                        WebMvcMcpSessionTransport sessionTransport =
                                new WebMvcMcpSessionTransport(sessionId, sseBuilder);
                        McpServerSession session = sessionFactory.create(sessionTransport);
                        this.sessions.put(sessionId, session);

                        try {
                            sseBuilder
                                    .id(sessionId)
                                    .event(ENDPOINT_EVENT_TYPE)
                                    .data(this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId);
                        } catch (Exception e) {
                            logger.error("Failed to send initial endpoint event: {}", e.getMessage());
                            sseBuilder.error(e);
                        }
                    },
                    Duration.ZERO);
        } catch (Exception e) {
            logger.error("Failed to send initial endpoint event to session {}: {}", sessionId, e.getMessage());
            sessions.remove(sessionId);
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles incoming JSON-RPC messages from clients. This method:
     * <ul>
     * <li>Deserializes the request body into a JSON-RPC message</li>
     * <li>Processes the message through the session's handle method</li>
     * <li>Returns appropriate HTTP responses based on the processing result</li>
     * </ul>
     * @param request The incoming server request containing the JSON-RPC message
     * @return A ServerResponse indicating success (200 OK) or appropriate error status
     * with error details in case of failures
     */
    private ServerResponse handleMessage(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        if (!request.param("sessionId").isPresent()) {
            return ServerResponse.badRequest().body(new McpError("Session ID missing in message endpoint"));
        }

        String sessionId = request.param("sessionId").get();
        McpServerSession session = sessions.get(sessionId);

        if (session == null) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).body(new McpError("Session not found: " + sessionId));
        }

        try {
            String body = request.body(String.class);
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            // Process the message through the session's handle method
            session.handle(message).block(); // Block for WebMVC compatibility

            return ServerResponse.ok().build();
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            return ServerResponse.badRequest().body(new McpError("Invalid message format"));
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
        }
    }

    /**
     * Implementation of McpServerTransport for WebMVC SSE sessions. This class handles
     * the transport-level communication for a specific client session.
     */
    private class WebMvcMcpSessionTransport implements McpServerTransport {

        private final String sessionId;

        private final SseBuilder sseBuilder;

        /**
         * Creates a new session transport with the specified ID and SSE builder.
         * @param sessionId The unique identifier for this session
         * @param sseBuilder The SSE builder for sending server events to the client
         */
        WebMvcMcpSessionTransport(String sessionId, SseBuilder sseBuilder) {
            this.sessionId = sessionId;
            this.sseBuilder = sseBuilder;
            logger.debug("Session transport {} initialized with SSE builder", sessionId);
        }

        /**
         * Sends a JSON-RPC message to the client through the SSE connection.
         * @param message The JSON-RPC message to send
         * @return A Mono that completes when the message has been sent
         */
        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromCallable(() -> {
                        String jsonText = objectMapper.writeValueAsString(message);
                        sseBuilder.id(sessionId).event(MESSAGE_EVENT_TYPE).data(jsonText);
                        logger.debug("Message sent to session {}", sessionId);
                        return true;
                    })
                    .onErrorResume(e -> {
                        logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                        sseBuilder.error(e);
                        // An exception occurs, and the connection is closed
                        McpSession session = sessions.get(sessionId);
                        if (session != null) {
                            session.close();
                        }
                        return Mono.empty();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(); // Avoid blocking and achieve asynchronous sending
        }

        /**
         * Send heartbeat messages
         * Use standard SSE event types and formats
         */
        public Mono<Void> sendHeartbeat() {
            return Mono.fromCallable(() -> {
                        sseBuilder.id(sessionId).event(HEARTBEAT_EVENT_TYPE).data("ping");
                        logger.debug("Message sent to session {}", sessionId);
                        return true;
                    })
                    .onErrorResume(e -> {
                        logger.debug("Failed to send heartbeat to session {}: {}", sessionId, e.getMessage());
                        // An exception occurs, and the connection is closed
                        McpSession session = sessions.get(sessionId);
                        if (session != null) {
                            session.close();
                        }
                        return Mono.empty();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(); // Avoid blocking and achieve asynchronous sending
        }

        /**
         * Converts data from one type to another using the configured ObjectMapper.
         * @param data The source data object to convert
         * @param typeRef The target type reference
         * @return The converted object of type T
         * @param <T> The target type
         */
        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        /**
         * Initiates a graceful shutdown of the transport.
         * @return A Mono that completes when the shutdown is complete
         */
        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                logger.debug("Closing session transport: {}", sessionId);
                try {
                    sseBuilder.complete();
                    sessions.remove(sessionId);
                    logger.debug("Successfully completed SSE builder for session {}", sessionId);
                } catch (Exception e) {
                    logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
                }
            });
        }

        /**
         * Closes the transport immediately.
         */
        @Override
        public void close() {
            try {
                sseBuilder.complete();
                sessions.remove(sessionId);
                logger.debug("Successfully completed SSE builder for session {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
            }
        }
    }
}
