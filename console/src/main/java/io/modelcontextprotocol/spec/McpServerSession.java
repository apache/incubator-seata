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
 * Copyright (c) [Year] the original author or authors.
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
import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a Model Context Protocol (MCP) session on the server side. It manages
 * bidirectional JSON-RPC communication with the client.
 */
public class McpServerSession implements McpLoggableSession {

    private static final Logger logger = LoggerFactory.getLogger(McpServerSession.class);

    private boolean healthy = true;

    private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses =
            new ConcurrentHashMap<>();

    private final String id;

    /** Duration to wait for request responses before timing out */
    private final Duration requestTimeout;

    private final AtomicLong requestCounter = new AtomicLong(0);

    private final McpInitRequestHandler initRequestHandler;

    private final Map<String, McpRequestHandler<?>> requestHandlers;

    private final Map<String, McpNotificationHandler> notificationHandlers;

    private final McpServerTransport transport;

    private final Sinks.One<McpAsyncServerExchange> exchangeSink = Sinks.one();

    private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();

    private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();

    private static final int STATE_UNINITIALIZED = 0;

    private static final int STATE_INITIALIZING = 1;

    private static final int STATE_INITIALIZED = 2;

    private final AtomicInteger state = new AtomicInteger(STATE_UNINITIALIZED);

    private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

    /**
     * Creates a new server session with the given parameters and the transport to use.
     * @param id session id
     * @param transport the transport to use
     * @param initHandler called when a
     * {@link McpSchema.InitializeRequest} is received by the
     * server
     * @param requestHandlers map of request handlers to use
     * @param notificationHandlers map of notification handlers to use
     */
    public McpServerSession(
            String id,
            Duration requestTimeout,
            McpServerTransport transport,
            McpInitRequestHandler initHandler,
            Map<String, McpRequestHandler<?>> requestHandlers,
            Map<String, McpNotificationHandler> notificationHandlers) {
        this.id = id;
        this.requestTimeout = requestTimeout;
        this.transport = transport;
        this.initRequestHandler = initHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    /**
     * Creates a new server session with the given parameters and the transport to use.
     * @param id session id
     * @param transport the transport to use
     * @param initHandler called when a
     * {@link McpSchema.InitializeRequest} is received by the
     * server
     * @param initNotificationHandler called when a
     * {@link McpSchema#METHOD_NOTIFICATION_INITIALIZED} is
     * received.
     * @param requestHandlers map of request handlers to use
     * @param notificationHandlers map of notification handlers to use
     */
    public McpServerSession(
            String id,
            Duration requestTimeout,
            McpServerTransport transport,
            McpInitRequestHandler initHandler,
            InitNotificationHandler initNotificationHandler,
            Map<String, McpRequestHandler<?>> requestHandlers,
            Map<String, McpNotificationHandler> notificationHandlers) {
        this.id = id;
        this.requestTimeout = requestTimeout;
        this.transport = transport;
        this.initRequestHandler = initHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    /**
     * Retrieve the session id.
     * @return session id
     */
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    /**
     * Called upon successful initialization sequence between the client and the server
     * with the client capabilities and information.
     * <a href=
     * "https://github.com/modelcontextprotocol/specification/blob/main/docs/specification/basic/lifecycle.md#initialization">Initialization
     * Spec</a>
     * @param clientCapabilities the capabilities the connected client provides
     * @param clientInfo the information about the connected client
     */
    public void init(McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo) {
        this.clientCapabilities.lazySet(clientCapabilities);
        this.clientInfo.lazySet(clientInfo);
    }

    private String generateRequestId() {
        return this.id + "-" + this.requestCounter.getAndIncrement();
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

    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
        String requestId = this.generateRequestId();

        return Mono.<McpSchema.JSONRPCResponse>create(sink -> {
                    this.pendingResponses.put(requestId, sink);
                    McpSchema.JSONRPCRequest jsonrpcRequest =
                            new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method, requestId, requestParams);
                    this.transport.sendMessage(jsonrpcRequest).subscribe(v -> {}, error -> {
                        this.pendingResponses.remove(requestId);
                        sink.error(error);
                    });
                })
                .timeout(requestTimeout)
                .handle((jsonRpcResponse, sink) -> {
                    if (jsonRpcResponse.getError() != null) {
                        sink.error(new McpError(jsonRpcResponse.getError()));
                    } else {
                        if (typeRef.getType().equals(Void.class)) {
                            sink.complete();
                        } else {
                            sink.next(this.transport.unmarshalFrom(jsonRpcResponse.getResult(), typeRef));
                        }
                    }
                });
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        McpSchema.JSONRPCNotification jsonrpcNotification =
                new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, method, params);
        return this.transport.sendMessage(jsonrpcNotification);
    }

    /**
     * Called by the {@link McpServerTransportProvider} once the session is determined.
     * The purpose of this method is to dispatch the message to an appropriate handler as
     * specified by the MCP server implementation
     * ({@link io.modelcontextprotocol.server.McpAsyncServer}
     * {@link Factory} that the server creates.
     * @param message the incoming JSON-RPC message
     * @return a Mono that completes when the message is processed
     */
    public Mono<Void> handle(McpSchema.JSONRPCMessage message) {
        return Mono.defer(() -> {
            // first
            if (message instanceof McpSchema.JSONRPCResponse) {
                McpSchema.JSONRPCResponse response = (McpSchema.JSONRPCResponse) message;
                logger.debug("Received Response: {}", response);
                MonoSink<McpSchema.JSONRPCResponse> sink = pendingResponses.remove(response.getId());
                if (sink == null) {
                    logger.warn("Unexpected response for unknown id {}", response.getId());
                } else {
                    sink.success(response);
                }
                return Mono.empty();
            } else if (message instanceof McpSchema.JSONRPCRequest) {
                McpSchema.JSONRPCRequest request = (McpSchema.JSONRPCRequest) message;
                logger.debug("Received request: {}", request);
                return handleIncomingRequest(request)
                        .onErrorResume(error -> {
                            McpSchema.JSONRPCResponse errorResponse = new McpSchema.JSONRPCResponse(
                                    McpSchema.JSONRPC_VERSION,
                                    request.getId(),
                                    null,
                                    new McpSchema.JSONRPCResponse.JSONRPCError(
                                            McpSchema.ErrorCodes.INTERNAL_ERROR, error.getMessage(), null));
                            return this.transport.sendMessage(errorResponse).then(Mono.empty());
                        })
                        .flatMap(this.transport::sendMessage);
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                McpSchema.JSONRPCNotification notification = (McpSchema.JSONRPCNotification) message;
                // happening first
                logger.debug("Received notification: {}", notification);
                return handleIncomingNotification(notification)
                        .doOnError(error -> logger.error("Error handling notification: {}", error.getMessage()));
            } else {
                logger.warn("Received unknown message type: {}", message);
                return Mono.empty();
            }
        });
    }

    /**
     * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
     * @param request The incoming JSON-RPC request
     * @return A Mono containing the JSON-RPC response
     */
    private Mono<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) {
        return Mono.defer(() -> {
            Mono<?> resultMono;
            if (McpSchema.METHOD_INITIALIZE.equals(request.getMethod())) {
                McpSchema.InitializeRequest initializeRequest = transport.unmarshalFrom(
                        request.getParams(), new TypeReference<McpSchema.InitializeRequest>() {});

                this.state.lazySet(STATE_INITIALIZING);
                this.init(initializeRequest.getCapabilities(), initializeRequest.getClientInfo());
                resultMono = this.initRequestHandler.handle(initializeRequest);
            } else {
                // initialization happening first
                McpRequestHandler<?> handler = this.requestHandlers.get(request.getMethod());
                if (handler == null) {
                    MethodNotFoundError error = getMethodNotFoundError(request.getMethod());
                    return Mono.just(new McpSchema.JSONRPCResponse(
                            McpSchema.JSONRPC_VERSION,
                            request.getId(),
                            null,
                            new McpSchema.JSONRPCResponse.JSONRPCError(
                                    McpSchema.ErrorCodes.METHOD_NOT_FOUND, error.getMessage(), error.getData())));
                }

                resultMono =
                        this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, request.getParams()));
            }
            return resultMono
                    .map(result ->
                            new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), result, null))
                    .onErrorResume(error -> Mono.just(new McpSchema.JSONRPCResponse(
                            McpSchema.JSONRPC_VERSION,
                            request.getId(),
                            null,
                            new McpSchema.JSONRPCResponse.JSONRPCError(
                                    McpSchema.ErrorCodes.INTERNAL_ERROR,
                                    error.getMessage(),
                                    null)))); // TODO: add error message
            // through the data field
        });
    }

    /**
     * Handles an incoming JSON-RPC notification by routing it to the appropriate handler.
     * @param notification The incoming JSON-RPC notification
     * @return A Mono that completes when the notification is processed
     */
    private Mono<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
        return Mono.defer(() -> {
            if (McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(notification.getMethod())) {
                this.state.lazySet(STATE_INITIALIZED);
                exchangeSink.tryEmitValue(new McpAsyncServerExchange(
                        this.id, this, clientCapabilities.get(), clientInfo.get(), McpTransportContext.EMPTY));
            }

            McpNotificationHandler handler = notificationHandlers.get(notification.getMethod());
            if (handler == null) {
                logger.warn("No handler registered for notification method: {}", notification);
                return Mono.empty();
            }
            return this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, notification.getParams()));
        });
    }

    public static final class MethodNotFoundError {
        private String method;
        private String message;
        private Object data;

        public MethodNotFoundError(String method, String message, Object data) {
            this.method = method;
            this.message = message;
            this.data = data;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "MethodNotFoundError{" + "method='"
                    + method + '\'' + ", message='"
                    + message + '\'' + ", data="
                    + data + '}';
        }
    }

    private MethodNotFoundError getMethodNotFoundError(String method) {
        return new MethodNotFoundError(method, "Method not found: " + method, null);
    }

    @Override
    public Mono<Void> closeGracefully() {
        return this.transport.closeGracefully();
    }

    @Override
    public void close() {
        this.transport.close();
    }

    /**
     * Request handler for the initialization request.
     */
    public interface InitRequestHandler {

        /**
         * Handles the initialization request.
         * @param initializeRequest the initialization request by the client
         * @return a Mono that will emit the result of the initialization
         */
        Mono<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);
    }

    /**
     * Notification handler for the initialization notification from the client.
     */
    public interface InitNotificationHandler {

        /**
         * Specifies an action to take upon successful initialization.
         * @return a Mono that will complete when the initialization is acted upon.
         */
        Mono<Void> handle();
    }

    /**
     * A handler for client-initiated notifications.
     */
    public interface NotificationHandler {

        /**
         * Handles a notification from the client.
         * @param exchange the exchange associated with the client that allows calling
         * back to the connected client or inspecting its capabilities.
         * @param params the parameters of the notification.
         * @return a Mono that completes once the notification is handled.
         */
        Mono<Void> handle(McpAsyncServerExchange exchange, Object params);
    }

    /**
     * A handler for client-initiated requests.
     *
     * @param <T> the type of the response that is expected as a result of handling the
     * request.
     */
    public interface RequestHandler<T> {

        /**
         * Handles a request from the client.
         * @param exchange the exchange associated with the client that allows calling
         * back to the connected client or inspecting its capabilities.
         * @param params the parameters of the request.
         * @return a Mono that will emit the response to the request.
         */
        Mono<T> handle(McpAsyncServerExchange exchange, Object params);
    }

    /**
     * Factory for creating server sessions which delegate to a provided 1:1 transport
     * with a connected client.
     */
    @FunctionalInterface
    public interface Factory {

        /**
         * Creates a new 1:1 representation of the client-server interaction.
         * @param sessionTransport the transport to use for communication with the client.
         * @return a new server session.
         */
        McpServerSession create(McpServerTransport sessionTransport);
    }
}
