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

package org.apache.seata.mcp.core.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.apache.seata.mcp.core.runtimeCore.NotificationProcessor;
import org.apache.seata.mcp.core.runtimeCore.RequestProcessor;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeExchangeContext;
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

public class ServerRuntimeSession implements RuntimeSession {

    private static final Logger logger = LoggerFactory.getLogger(ServerRuntimeSession.class);
    private static final int STATE_UNINIT = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_READY = 2;

    private volatile boolean healthy = true;
    private final String sessionId;
    private final Duration timeout;
    private final RuntimeTransport transport;
    private final RequestProcessor.InitializationProcessor initHandler;
    private final Map<String, RequestProcessor<?>> requestHandlers;
    private final Map<String, NotificationProcessor> notificationHandlers;
    private final ConcurrentHashMap<Object, MonoSink<ProtocolDefinition.JSONRPCResponse>> pending =
            new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong(0);
    private final AtomicInteger state = new AtomicInteger(STATE_UNINIT);
    private final Sinks.One<RuntimeExchangeContext> exchangeSink = Sinks.one();
    private final AtomicReference<ProtocolDefinition.ClientCapabilities> clientCaps = new AtomicReference<>();
    private final AtomicReference<ProtocolDefinition.Implementation> clientInfo = new AtomicReference<>();
    private volatile ProtocolDefinition.LoggingLevel logLevel = ProtocolDefinition.LoggingLevel.INFO;

    public ServerRuntimeSession(
            String id,
            Duration timeout,
            RuntimeTransport transport,
            RequestProcessor.InitializationProcessor initHandler,
            Map<String, RequestProcessor<?>> requestHandlers,
            Map<String, NotificationProcessor> notificationHandlers) {
        this.sessionId = id;
        this.timeout = timeout;
        this.transport = transport;
        this.initHandler = initHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    public String getId() {
        return sessionId;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public void init(ProtocolDefinition.ClientCapabilities caps, ProtocolDefinition.Implementation info) {
        clientCaps.lazySet(caps);
        clientInfo.lazySet(info);
    }

    @Override
    public void setMinLoggingLevel(ProtocolDefinition.LoggingLevel level) {
        RuntimeUtils.notNull(level, "Level required");
        this.logLevel = level;
    }

    @Override
    public boolean isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel level) {
        return level.level() >= logLevel.level();
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object params, TypeReference<T> typeRef) {
        String reqId = sessionId + "-" + requestIdGen.getAndIncrement();
        return Mono.<ProtocolDefinition.JSONRPCResponse>create(sink -> {
                    pending.put(reqId, sink);
                    ProtocolDefinition.JSONRPCRequest req = new ProtocolDefinition.JSONRPCRequest(
                            ProtocolDefinition.JSONRPC_VERSION, method, reqId, params);
                    transport.sendMessage(req).subscribe(v -> {}, err -> {
                        pending.remove(reqId);
                        sink.error(err);
                    });
                })
                .timeout(timeout)
                .handle((resp, sink) -> {
                    if (resp.getError() != null) {
                        sink.error(new ProtocolError(resp.getError()));
                    } else if (typeRef.getType().equals(Void.class)) {
                        sink.complete();
                    } else {
                        sink.next(transport.unmarshalFrom(resp.getResult(), typeRef));
                    }
                });
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        ProtocolDefinition.JSONRPCNotification notif =
                new ProtocolDefinition.JSONRPCNotification(ProtocolDefinition.JSONRPC_VERSION, method, params);
        return transport.sendMessage(notif);
    }

    public Mono<Void> handle(ProtocolDefinition.JSONRPCMessage msg) {
        return Mono.defer(() -> {
            if (msg instanceof ProtocolDefinition.JSONRPCResponse) {
                return handleResponse((ProtocolDefinition.JSONRPCResponse) msg);
            } else if (msg instanceof ProtocolDefinition.JSONRPCRequest) {
                return handleRequest((ProtocolDefinition.JSONRPCRequest) msg);
            } else if (msg instanceof ProtocolDefinition.JSONRPCNotification) {
                return handleNotification((ProtocolDefinition.JSONRPCNotification) msg);
            }
            logger.warn("Unknown message type: {}", msg);
            return Mono.empty();
        });
    }

    private Mono<Void> handleResponse(ProtocolDefinition.JSONRPCResponse resp) {
        logger.debug("Response: {}", resp);
        MonoSink<ProtocolDefinition.JSONRPCResponse> sink = pending.remove(resp.getId());
        if (sink != null) {
            sink.success(resp);
        } else {
            logger.warn("No pending request for response {}", resp.getId());
        }
        return Mono.empty();
    }

    private Mono<Void> handleRequest(ProtocolDefinition.JSONRPCRequest req) {
        logger.debug("Request: {}", req);
        return Mono.defer(() -> {
            Mono<?> result;
            if (ProtocolDefinition.METHOD_INITIALIZE.equals(req.getMethod())) {
                ProtocolDefinition.InitializeRequest initReq = transport.unmarshalFrom(
                        req.getParams(), new TypeReference<ProtocolDefinition.InitializeRequest>() {});
                state.set(STATE_INIT);
                init(initReq.getCapabilities(), initReq.getClientInfo());
                result = initHandler.handle(initReq);
            } else {
                RequestProcessor<?> handler = requestHandlers.get(req.getMethod());
                if (handler == null) {
                    return transport.sendMessage(new ProtocolDefinition.JSONRPCResponse(
                            ProtocolDefinition.JSONRPC_VERSION,
                            req.getId(),
                            null,
                            new ProtocolDefinition.JSONRPCResponse.JSONRPCError(
                                    ProtocolDefinition.ErrorCodes.METHOD_NOT_FOUND,
                                    "Method not found: " + req.getMethod(),
                                    null)));
                }
                result = exchangeSink.asMono().flatMap(ex -> handler.handle(ex, req.getParams()));
            }
            return result.map(r -> new ProtocolDefinition.JSONRPCResponse(
                            ProtocolDefinition.JSONRPC_VERSION, req.getId(), r, null))
                    .onErrorResume(err -> Mono.just(new ProtocolDefinition.JSONRPCResponse(
                            ProtocolDefinition.JSONRPC_VERSION,
                            req.getId(),
                            null,
                            new ProtocolDefinition.JSONRPCResponse.JSONRPCError(
                                    ProtocolDefinition.ErrorCodes.INTERNAL_ERROR, err.getMessage(), null))))
                    .flatMap(transport::sendMessage);
        });
    }

    private Mono<Void> handleNotification(ProtocolDefinition.JSONRPCNotification notif) {
        logger.debug("Notification: {}", notif);
        return Mono.defer(() -> {
            if (ProtocolDefinition.METHOD_NOTIFICATION_INITIALIZED.equals(notif.getMethod())) {
                state.set(STATE_READY);
                exchangeSink.tryEmitValue(new RuntimeExchangeContext(
                        sessionId, this, clientCaps.get(), clientInfo.get(), RuntimeContext.EMPTY));
            }
            NotificationProcessor handler = notificationHandlers.get(notif.getMethod());
            if (handler == null) {
                logger.warn("No handler for notification: {}", notif.getMethod());
                return Mono.empty();
            }
            return exchangeSink.asMono().flatMap(ex -> handler.handle(ex, notif.getParams()));
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return transport.closeGracefully();
    }

    @Override
    public void close() {
        transport.close();
    }

    @FunctionalInterface
    public interface Factory {
        ServerRuntimeSession create(RuntimeTransport transport);
    }
}
