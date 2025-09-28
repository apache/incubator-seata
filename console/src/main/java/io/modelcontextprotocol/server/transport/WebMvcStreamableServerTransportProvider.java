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
 * Modifications made by [Seata]:
 *   - Adapted code from Java 17 features to Java 8 compatible syntax
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
import io.modelcontextprotocol.server.DefaultMcpTransportContext;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSession;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerTransport;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.ProtocolVersions;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server-side implementation of the Model Context Protocol (MCP) streamable transport
 * layer using HTTP with Server-Sent Events (SSE) through Spring WebMVC. This
 * implementation provides a bridge between synchronous WebMVC operations and reactive
 * programming patterns to maintain compatibility with the reactive transport interface.
 *
 * <p>
 * This is the non-reactive version of
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 * @see McpStreamableServerTransportProvider
 * @see RouterFunction
 */
public class WebMvcStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcStreamableServerTransportProvider.class);

    public static final String MESSAGE_EVENT_TYPE = "message";

    private final String mcpEndpoint;

    private final boolean disallowDelete;

    private final ObjectMapper objectMapper;

    private final RouterFunction<ServerResponse> routerFunction;

    private McpStreamableServerSession.Factory sessionFactory;

    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

    private McpTransportContextExtractor contextExtractor;

    private volatile boolean isClosing = false;

    private KeepAliveScheduler keepAliveScheduler;

    public WebMvcStreamableServerTransportProvider(
            ObjectMapper objectMapper,
            String mcpEndpoint,
            boolean disallowDelete,
            McpTransportContextExtractor contextExtractor,
            Duration keepAliveInterval) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "McpTransportContextExtractor must not be null");

        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.disallowDelete = disallowDelete;
        this.contextExtractor = contextExtractor;
        this.routerFunction = RouterFunctions.route()
                .GET(this.mcpEndpoint, this::handleGet)
                .POST(this.mcpEndpoint, this::handlePost)
                .DELETE(this.mcpEndpoint, this::handleDelete)
                .build();

        if (keepAliveInterval != null) {
            this.keepAliveScheduler = KeepAliveScheduler.builder(() -> {
                        if (isClosing) {
                            return Flux.empty();
                        }
                        return Flux.fromIterable(this.sessions.values())
                                .publishOn(Schedulers.boundedElastic())
                                .filter(session -> {
                                    // Check if the session is healthy
                                    if (!session.isHealthy()) {
                                        logger.warn("Removing unhealthy session: {}", session.getId());
                                        session.closeGracefully().subscribe();
                                        this.sessions.remove(session.getId());
                                        return false;
                                    }
                                    return true;
                                })
                                .cast(McpSession.class);
                    })
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();

            this.keepAliveScheduler.start();
        }
    }

    @Override
    public List<String> protocolVersions() {
        return Arrays.asList(ProtocolVersions.MCP_2025_06_18, ProtocolVersions.MCP_2025_03_26);
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());

        return Mono.fromRunnable(() -> this.sessions.values().parallelStream().forEach(session -> {
            try {
                session.sendNotification(method, params).block();
            } catch (Exception e) {
                logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
                    this.isClosing = true;
                    logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());

                    this.sessions.values().parallelStream().forEach(session -> {
                        try {
                            session.closeGracefully().block();
                        } catch (Exception e) {
                            logger.error("Failed to close session {}: {}", session.getId(), e.getMessage());
                        }
                    });

                    this.sessions.clear();
                    logger.debug("Graceful shutdown completed");
                })
                .then()
                .doOnSuccess(v -> {
                    if (this.keepAliveScheduler != null) {
                        this.keepAliveScheduler.shutdown();
                    }
                });
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    private ServerResponse handleGet(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
        if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)) {
            return ServerResponse.badRequest().body("Invalid Accept header. Expected TEXT_EVENT_STREAM");
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        if (!request.headers().asHttpHeaders().containsKey(HttpHeaders.MCP_SESSION_ID)) {
            return ServerResponse.badRequest().body("Session ID required in mcp-session-id header");
        }

        String sessionId = request.headers().asHttpHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            return ServerResponse.notFound().build();
        }

        logger.debug("Handling GET request for session: {}", sessionId);

        try {
            return ServerResponse.sse(
                    sseBuilder -> {
                        sseBuilder.onTimeout(() -> logger.debug("SSE connection timed out for session: {}", sessionId));

                        WebMvcStreamableMcpSessionTransport sessionTransport =
                                new WebMvcStreamableMcpSessionTransport(sessionId, sseBuilder);

                        // Check if this is a replay request
                        if (request.headers().asHttpHeaders().containsKey(HttpHeaders.LAST_EVENT_ID)) {
                            String lastId = request.headers().asHttpHeaders().getFirst(HttpHeaders.LAST_EVENT_ID);

                            try {
                                session.replay(lastId)
                                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                                        .toIterable()
                                        .forEach(message -> {
                                            try {
                                                sessionTransport
                                                        .sendMessage(message)
                                                        .contextWrite(ctx ->
                                                                ctx.put(McpTransportContext.KEY, transportContext))
                                                        .block();
                                            } catch (Exception e) {
                                                logger.error("Failed to replay message: {}", e.getMessage());
                                                sseBuilder.error(e);
                                            }
                                        });
                            } catch (Exception e) {
                                logger.error("Failed to replay messages: {}", e.getMessage());
                                sseBuilder.error(e);
                            }
                        } else {
                            // Establish new listening stream
                            McpStreamableServerSession.McpStreamableServerSessionStream listeningStream =
                                    session.listeningStream(sessionTransport);

                            sseBuilder.onComplete(() -> {
                                logger.debug("SSE connection completed for session: {}", sessionId);
                                listeningStream.close();
                            });
                        }
                    },
                    Duration.ZERO);
        } catch (Exception e) {
            logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ServerResponse handlePost(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
        if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)
                || !acceptHeaders.contains(MediaType.APPLICATION_JSON)) {
            return ServerResponse.badRequest()
                    .body(new McpError("Invalid Accept headers. Expected TEXT_EVENT_STREAM and APPLICATION_JSON"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        try {
            String body = request.body(String.class);
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                if (jsonrpcRequest.getMethod().equals(McpSchema.METHOD_INITIALIZE)) {
                    McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(
                            jsonrpcRequest.getParams(), new TypeReference<McpSchema.InitializeRequest>() {});
                    McpStreamableServerSession.McpStreamableServerSessionInit init =
                            this.sessionFactory.startSession(initializeRequest);
                    this.sessions.put(init.getSession().getId(), init.getSession());

                    try {
                        McpSchema.InitializeResult initResult =
                                init.getInitResult().block();

                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(
                                        HttpHeaders.MCP_SESSION_ID,
                                        init.getSession().getId())
                                .body(new McpSchema.JSONRPCResponse(
                                        McpSchema.JSONRPC_VERSION, jsonrpcRequest.getId(), initResult, null));
                    } catch (Exception e) {
                        logger.error("Failed to initialize session: {}", e.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new McpError(e.getMessage()));
                    }
                }
            }

            // Handle other messages that require a session
            if (!request.headers().asHttpHeaders().containsKey(HttpHeaders.MCP_SESSION_ID)) {
                return ServerResponse.badRequest().body(new McpError("Session ID missing"));
            }

            String sessionId = request.headers().asHttpHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);
            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                return ServerResponse.status(HttpStatus.NOT_FOUND)
                        .body(new McpError("Session not found: " + sessionId));
            }

            if (message instanceof McpSchema.JSONRPCResponse) {
                McpSchema.JSONRPCResponse jsonrpcResponse = (McpSchema.JSONRPCResponse) message;
                session.accept(jsonrpcResponse)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                return ServerResponse.accepted().build();
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                McpSchema.JSONRPCNotification jsonrpcNotification = (McpSchema.JSONRPCNotification) message;
                session.accept(jsonrpcNotification)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                return ServerResponse.accepted().build();
            } else if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest) message;
                // For streaming responses, we need to return SSE
                return ServerResponse.sse(
                        sseBuilder -> {
                            sseBuilder.onComplete(
                                    () -> logger.debug("Request response stream completed for session: {}", sessionId));
                            sseBuilder.onTimeout(
                                    () -> logger.debug("Request response stream timed out for session: {}", sessionId));

                            WebMvcStreamableMcpSessionTransport sessionTransport =
                                    new WebMvcStreamableMcpSessionTransport(sessionId, sseBuilder);

                            try {
                                session.responseStream(jsonrpcRequest, sessionTransport)
                                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                                        .block();
                            } catch (Exception e) {
                                logger.error("Failed to handle request stream: {}", e.getMessage());
                                sseBuilder.error(e);
                            }
                        },
                        Duration.ZERO);
            } else {
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new McpError("Unknown message type"));
            }
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            return ServerResponse.badRequest().body(new McpError("Invalid message format"));
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
        }
    }

    private ServerResponse handleDelete(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        if (this.disallowDelete) {
            return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());

        if (!request.headers().asHttpHeaders().containsKey(HttpHeaders.MCP_SESSION_ID)) {
            return ServerResponse.badRequest().body("Session ID required in mcp-session-id header");
        }

        String sessionId = request.headers().asHttpHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            return ServerResponse.notFound().build();
        }

        try {
            session.delete()
                    .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                    .block();
            if (sessionId != null) {
                this.sessions.remove(sessionId);
            }
            return ServerResponse.ok().build();
        } catch (Exception e) {
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
        }
    }

    private class WebMvcStreamableMcpSessionTransport implements McpStreamableServerTransport {

        private final String sessionId;

        private final SseBuilder sseBuilder;

        private final ReentrantLock lock = new ReentrantLock();

        private volatile boolean closed = false;

        WebMvcStreamableMcpSessionTransport(String sessionId, SseBuilder sseBuilder) {
            this.sessionId = sessionId;
            this.sseBuilder = sseBuilder;
            logger.debug("Streamable session transport {} initialized with SSE builder", sessionId);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (this.closed) {
                    logger.debug("Attempted to send message to closed session: {}", this.sessionId);
                    return;
                }

                this.lock.lock();
                try {
                    if (this.closed) {
                        logger.debug("Session {} was closed during message send attempt", this.sessionId);
                        return;
                    }

                    String jsonText = objectMapper.writeValueAsString(message);
                    this.sseBuilder
                            .id(messageId != null ? messageId : this.sessionId)
                            .event(MESSAGE_EVENT_TYPE);
                    this.sseBuilder.data(jsonText);
                    logger.debug("Data sent successfully to session {}", this.sessionId);
                } catch (Exception e) {
                    if (isClientDisconnection(e)) {
                        logger.debug("Client disconnected, session {}: {}", this.sessionId, e.getMessage());
                    } else {
                        logger.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
                    }
                    try {
                        this.sseBuilder.complete();
                    } catch (Exception errorException) {
                        logger.error(
                                "Failed to send error to SSE builder for session {}: {}",
                                this.sessionId,
                                errorException.getMessage());
                    }
                } finally {
                    this.lock.unlock();
                }
            });
        }

        private boolean isClientDisconnection(Throwable e) {
            if (e == null) {
                return false;
            }

            String message = e.getMessage();
            if (message == null) {
                return false;
            }
            return message.contains("你的主机中的软件中止了一个已建立的连接")
                    || message.contains("ServletOutputStream failed to flush")
                    || message.contains("Connection reset by peer")
                    || message.contains("Broken pipe")
                    || message.contains("远程主机强迫关闭了一个现有的连接")
                    || message.contains("An existing connection was forcibly closed by the remote host")
                    || message.contains("Connection aborted")
                    || message.contains("Socket closed");
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(WebMvcStreamableMcpSessionTransport.this::close);
        }

        @Override
        public void close() {
            this.lock.lock();
            try {
                if (this.closed) {
                    logger.debug("Session transport {} already closed", this.sessionId);
                    return;
                }

                this.closed = true;
                this.sseBuilder.complete();
                logger.debug("Successfully completed SSE builder for session {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
            } finally {
                this.lock.unlock();
            }
        }
    }
}
