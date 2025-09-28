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

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpNotificationHandler;
import io.modelcontextprotocol.server.McpRequestHandler;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author Dariusz Jędrzejczyk
 */
public class McpStreamableServerSession implements McpLoggableSession {

    private boolean healthy = true;

    private static final Logger logger = LoggerFactory.getLogger(McpStreamableServerSession.class);

    private final ConcurrentHashMap<Object, McpStreamableServerSessionStream> requestIdToStream =
            new ConcurrentHashMap<>();

    private final String id;

    private final Duration requestTimeout;

    private final AtomicLong requestCounter = new AtomicLong(0);

    private final Map<String, McpRequestHandler<?>> requestHandlers;

    private final Map<String, McpNotificationHandler> notificationHandlers;

    private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();

    private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();

    private final AtomicReference<McpLoggableSession> listeningStreamRef;

    private final MissingMcpTransportSession missingMcpTransportSession;

    private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public McpStreamableServerSession(
            String id,
            McpSchema.ClientCapabilities clientCapabilities,
            McpSchema.Implementation clientInfo,
            Duration requestTimeout,
            Map<String, McpRequestHandler<?>> requestHandlers,
            Map<String, McpNotificationHandler> notificationHandlers) {
        this.id = id;
        this.missingMcpTransportSession = new MissingMcpTransportSession(id);
        this.listeningStreamRef = new AtomicReference<>(this.missingMcpTransportSession);
        this.clientCapabilities.lazySet(clientCapabilities);
        this.clientInfo.lazySet(clientInfo);
        this.requestTimeout = requestTimeout;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    @Override
    public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
        Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
        this.minLoggingLevel = minLoggingLevel;
    }

    @Override
    public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
        return loggingLevel.level() >= this.minLoggingLevel.level();
    }

    public String getId() {
        return this.id;
    }

    private String generateRequestId() {
        return this.id + "-" + this.requestCounter.getAndIncrement();
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
        return Mono.defer(() -> {
            McpLoggableSession listeningStream = this.listeningStreamRef.get();
            return listeningStream.sendRequest(method, requestParams, typeRef);
        });
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        return Mono.defer(() -> {
            McpLoggableSession listeningStream = this.listeningStreamRef.get();
            return listeningStream.sendNotification(method, params);
        });
    }

    public Mono<Void> delete() {
        return this.closeGracefully().then(Mono.fromRunnable(() -> {}));
    }

    public McpStreamableServerSessionStream listeningStream(McpStreamableServerTransport transport) {
        McpStreamableServerSessionStream listeningStream = new McpStreamableServerSessionStream(transport);
        this.listeningStreamRef.set(listeningStream);
        return listeningStream;
    }

    public Flux<McpSchema.JSONRPCMessage> replay(Object lastEventId) {
        return Flux.empty();
    }

    public Mono<Void> responseStream(McpSchema.JSONRPCRequest jsonrpcRequest, McpStreamableServerTransport transport) {
        return Mono.deferContextual(ctx -> {
            McpTransportContext transportContext = ctx.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY);

            McpStreamableServerSessionStream stream = new McpStreamableServerSessionStream(transport);
            McpRequestHandler<?> requestHandler =
                    McpStreamableServerSession.this.requestHandlers.get(jsonrpcRequest.method);
            if (requestHandler == null) {
                MethodNotFoundError error = getMethodNotFoundError(jsonrpcRequest.method);
                return transport.sendMessage(new McpSchema.JSONRPCResponse(
                        McpSchema.JSONRPC_VERSION,
                        jsonrpcRequest.id,
                        null,
                        new McpSchema.JSONRPCResponse.JSONRPCError(
                                McpSchema.ErrorCodes.METHOD_NOT_FOUND, error.message, error.data)));
            }
            return requestHandler
                    .handle(
                            new McpAsyncServerExchange(
                                    this.id, stream, clientCapabilities.get(), clientInfo.get(), transportContext),
                            jsonrpcRequest.params)
                    .map(result ->
                            new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, jsonrpcRequest.id, result, null))
                    .onErrorResume(e -> {
                        McpSchema.JSONRPCResponse errorResponse = new McpSchema.JSONRPCResponse(
                                McpSchema.JSONRPC_VERSION,
                                jsonrpcRequest.id,
                                null,
                                new McpSchema.JSONRPCResponse.JSONRPCError(
                                        McpSchema.ErrorCodes.INTERNAL_ERROR, e.getMessage(), null));
                        return Mono.just(errorResponse);
                    })
                    .flatMap(transport::sendMessage)
                    .then(transport.closeGracefully());
        });
    }

    public Mono<Void> accept(McpSchema.JSONRPCNotification notification) {
        return Mono.deferContextual(ctx -> {
            McpTransportContext transportContext = ctx.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY);
            McpNotificationHandler notificationHandler = this.notificationHandlers.get(notification.method);
            if (notificationHandler == null) {
                logger.warn("No handler registered for notification method: {}", notification);
                return Mono.empty();
            }
            McpLoggableSession listeningStream = this.listeningStreamRef.get();
            return notificationHandler.handle(
                    new McpAsyncServerExchange(
                            this.id,
                            listeningStream,
                            this.clientCapabilities.get(),
                            this.clientInfo.get(),
                            transportContext),
                    notification.params);
        });
    }

    public Mono<Void> accept(McpSchema.JSONRPCResponse response) {
        return Mono.defer(() -> {
            McpStreamableServerSessionStream stream = this.requestIdToStream.get(response.id);
            if (stream == null) {
                return Mono.error(new McpError("Unexpected response for unknown id " + response.id));
                // JSONize
            }
            MonoSink<McpSchema.JSONRPCResponse> sink = stream.pendingResponses.remove(response.id);
            if (sink == null) {
                return Mono.error(new McpError("Unexpected response for unknown id " + response.id));
                // JSONize
            } else {
                sink.success(response);
            }
            return Mono.empty();
        });
    }

    private static final class MethodNotFoundError {
        private String method;
        private String message;
        private Object data;

        public MethodNotFoundError(String method, String message, String data) {
            this.method = method;
            this.message = message;
            this.data = data;
        }
    }

    private MethodNotFoundError getMethodNotFoundError(String method) {
        return new MethodNotFoundError(method, "Method not found: " + method, null);
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            McpLoggableSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);
            return listeningStream.closeGracefully();
        });
    }

    @Override
    public void close() {
        McpLoggableSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);
        if (listeningStream != null) {
            listeningStream.close();
        }
    }

    public interface InitRequestHandler {

        Mono<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);
    }

    public interface Factory {

        McpStreamableServerSessionInit startSession(McpSchema.InitializeRequest initializeRequest);
    }

    public static final class McpStreamableServerSessionInit {
        private McpStreamableServerSession session;
        private Mono<McpSchema.InitializeResult> initResult;

        public McpStreamableServerSessionInit(
                McpStreamableServerSession mcpStreamableServerSession, Mono<McpSchema.InitializeResult> handle) {
            this.session = mcpStreamableServerSession;
            this.initResult = handle;
        }

        public McpStreamableServerSession getSession() {
            return session;
        }

        public void setSession(McpStreamableServerSession session) {
            this.session = session;
        }

        public Mono<McpSchema.InitializeResult> getInitResult() {
            return initResult;
        }

        public void setInitResult(Mono<McpSchema.InitializeResult> initResult) {
            this.initResult = initResult;
        }
    }

    public final class McpStreamableServerSessionStream implements McpLoggableSession {

        private boolean healthy = true;

        private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses =
                new ConcurrentHashMap<>();

        private final McpStreamableServerTransport transport;

        private final String transportId;

        private final Supplier<String> uuidGenerator;

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public McpStreamableServerSessionStream(McpStreamableServerTransport transport) {
            this.transport = transport;
            this.transportId = UUID.randomUUID().toString();
            this.uuidGenerator = () -> this.transportId + "_" + UUID.randomUUID();
        }

        @Override
        public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
            Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
            McpStreamableServerSession.this.setMinLoggingLevel(minLoggingLevel);
        }

        @Override
        public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
            return McpStreamableServerSession.this.isNotificationForLevelAllowed(loggingLevel);
        }

        @Override
        public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
            String requestId = McpStreamableServerSession.this.generateRequestId();
            McpStreamableServerSession.this.requestIdToStream.put(requestId, this);

            return Mono.defer(() -> {
                        McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(
                                McpSchema.JSONRPC_VERSION, method, requestId, requestParams);
                        String messageId = this.uuidGenerator.get();

                        return this.transport
                                .sendMessage(jsonrpcRequest, messageId)
                                .onErrorResume(throwable -> {
                                    if (isClientDisconnection(throwable)) {
                                        logger.debug("Client disconnected, ignoring error for request {}", requestId);
                                        return Mono.empty();
                                    } else {
                                        logger.error("Failed to send message for request {}", requestId, throwable);
                                        return Mono.error(throwable);
                                    }
                                })
                                .then(Mono.<McpSchema.JSONRPCResponse>create(sink -> {
                                    this.pendingResponses.put(requestId, sink);
                                }));
                    })
                    .timeout(requestTimeout)
                    .doOnError(e -> {
                        this.pendingResponses.remove(requestId);
                        McpStreamableServerSession.this.requestIdToStream.remove(requestId);
                    })
                    .handle((jsonRpcResponse, sink) -> {
                        if (jsonRpcResponse.error != null) {
                            sink.error(new McpError(jsonRpcResponse.error));
                        } else {
                            if (typeRef.getType().equals(Void.class)) {
                                sink.complete();
                            } else {
                                sink.next(this.transport.unmarshalFrom(jsonRpcResponse.result, typeRef));
                            }
                        }
                    });
        }

        private boolean isClientDisconnection(Throwable e) {
            String message = e.getMessage();
            return message != null
                    && (message.contains("你的主机中的软件中止了一个已建立的连接")
                            || message.contains("Connection reset by peer")
                            || message.contains("Broken pipe"));
        }

        @Override
        public Mono<Void> sendNotification(String method, Object params) {
            McpSchema.JSONRPCNotification jsonrpcNotification =
                    new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, method, params);
            String messageId = this.uuidGenerator.get();
            return this.transport.sendMessage(jsonrpcNotification, messageId);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.defer(() -> {
                this.pendingResponses.values().forEach(s -> s.error(new RuntimeException("Stream closed")));
                this.pendingResponses.clear();
                McpStreamableServerSession.this.listeningStreamRef.compareAndSet(
                        this, McpStreamableServerSession.this.missingMcpTransportSession);
                McpStreamableServerSession.this.requestIdToStream.values().removeIf(this::equals);
                return this.transport.closeGracefully();
            });
        }

        @Override
        public void close() {
            this.pendingResponses.values().forEach(s -> s.error(new RuntimeException("Stream closed")));
            this.pendingResponses.clear();
            McpStreamableServerSession.this.listeningStreamRef.compareAndSet(
                    this, McpStreamableServerSession.this.missingMcpTransportSession);
            McpStreamableServerSession.this.requestIdToStream.values().removeIf(this::equals);
            this.transport.close();
        }
    }
}
