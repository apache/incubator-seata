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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Streamable server runtime session.
 */
public class StreamableServerRuntimeSession implements RuntimeSession {

    private static final Logger logger = LoggerFactory.getLogger(StreamableServerRuntimeSession.class);

    private volatile boolean healthy = true;
    private final String id;
    private final Duration timeout;
    private final Map<String, RequestProcessor<?>> requestHandlers;
    private final Map<String, NotificationProcessor> notificationHandlers;
    private final ConcurrentHashMap<Object, SessionStream> streamMap = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong(0);
    private final AtomicReference<ProtocolDefinition.ClientCapabilities> clientCaps = new AtomicReference<>();
    private final AtomicReference<ProtocolDefinition.Implementation> clientInfo = new AtomicReference<>();
    private final AtomicReference<RuntimeSession> activeStream = new AtomicReference<>();
    private final MissingRuntimeTransportSession fallbackStream;
    private volatile ProtocolDefinition.LoggingLevel logLevel = ProtocolDefinition.LoggingLevel.INFO;

    public StreamableServerRuntimeSession(
            String id,
            ProtocolDefinition.ClientCapabilities clientCaps,
            ProtocolDefinition.Implementation clientInfo,
            Duration timeout,
            Map<String, RequestProcessor<?>> requestHandlers,
            Map<String, NotificationProcessor> notificationHandlers) {
        this.id = id;
        this.timeout = timeout;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
        this.fallbackStream = new MissingRuntimeTransportSession(id);
        this.activeStream.set(fallbackStream);
        this.clientCaps.lazySet(clientCaps);
        this.clientInfo.lazySet(clientInfo);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
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

    public String getId() {
        return id;
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object params, TypeReference<T> typeRef) {
        return Mono.defer(() -> activeStream.get().sendRequest(method, params, typeRef));
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        return Mono.defer(() -> activeStream.get().sendNotification(method, params));
    }

    public Mono<Void> delete() {
        return closeGracefully();
    }

    public SessionStream listeningStream(StreamableServerRuntimeTransport transport) {
        SessionStream stream = new SessionStream(transport);
        activeStream.set(stream);
        return stream;
    }

    public Flux<ProtocolDefinition.JSONRPCMessage> replay(Object lastEventId) {
        return Flux.empty();
    }

    public Mono<Void> responseStream(
            ProtocolDefinition.JSONRPCRequest req, StreamableServerRuntimeTransport transport) {
        return Mono.deferContextual(ctx -> {
            RuntimeContext ctxData = ctx.getOrDefault(RuntimeContext.KEY, RuntimeContext.EMPTY);
            SessionStream stream = new SessionStream(transport);

            RequestProcessor<?> handler = requestHandlers.get(req.method);
            if (handler == null) {
                return transport.sendMessage(new ProtocolDefinition.JSONRPCResponse(
                        ProtocolDefinition.JSONRPC_VERSION,
                        req.id,
                        null,
                        new ProtocolDefinition.JSONRPCResponse.JSONRPCError(
                                ProtocolDefinition.ErrorCodes.METHOD_NOT_FOUND,
                                "Method not found: " + req.method,
                                null)));
            }

            return handler.handle(
                            new RuntimeExchangeContext(id, stream, clientCaps.get(), clientInfo.get(), ctxData),
                            req.params)
                    .map(r ->
                            new ProtocolDefinition.JSONRPCResponse(ProtocolDefinition.JSONRPC_VERSION, req.id, r, null))
                    .onErrorResume(e -> Mono.just(new ProtocolDefinition.JSONRPCResponse(
                            ProtocolDefinition.JSONRPC_VERSION,
                            req.id,
                            null,
                            new ProtocolDefinition.JSONRPCResponse.JSONRPCError(
                                    ProtocolDefinition.ErrorCodes.INTERNAL_ERROR, e.getMessage(), null))))
                    .flatMap(transport::sendMessage)
                    .then(transport.closeGracefully());
        });
    }

    public Mono<Void> accept(ProtocolDefinition.JSONRPCNotification notif) {
        return Mono.deferContextual(ctx -> {
            RuntimeContext ctxData = ctx.getOrDefault(RuntimeContext.KEY, RuntimeContext.EMPTY);
            NotificationProcessor handler = notificationHandlers.get(notif.method);
            if (handler == null) {
                logger.warn("No handler for notification: {}", notif.method);
                return Mono.empty();
            }
            RuntimeSession stream = activeStream.get();
            return handler.handle(
                    new RuntimeExchangeContext(id, stream, clientCaps.get(), clientInfo.get(), ctxData), notif.params);
        });
    }

    public Mono<Void> accept(ProtocolDefinition.JSONRPCResponse resp) {
        return Mono.defer(() -> {
            SessionStream stream = streamMap.get(resp.id);
            if (stream == null) {
                return Mono.error(new ProtocolErrorException("Unknown response id: " + resp.id));
            }
            MonoSink<ProtocolDefinition.JSONRPCResponse> sink = stream.pending.remove(resp.id);
            if (sink == null) {
                return Mono.error(new ProtocolErrorException("No pending request for: " + resp.id));
            }
            sink.success(resp);
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            RuntimeSession stream = activeStream.getAndSet(fallbackStream);
            return stream.closeGracefully();
        });
    }

    @Override
    public void close() {
        RuntimeSession stream = activeStream.getAndSet(fallbackStream);
        if (stream != null) {
            stream.close();
        }
    }

    private String nextRequestId() {
        return id + "-" + requestIdGen.getAndIncrement();
    }

    public interface InitRequestHandler {
        Mono<ProtocolDefinition.InitializeResult> handle(ProtocolDefinition.InitializeRequest req);
    }

    public interface Factory {
        StreamableServerRuntimeSessionInit startSession(ProtocolDefinition.InitializeRequest req);
    }

    public static final class StreamableServerRuntimeSessionInit {
        private final StreamableServerRuntimeSession session;
        private final Mono<ProtocolDefinition.InitializeResult> result;

        public StreamableServerRuntimeSessionInit(
                StreamableServerRuntimeSession session, Mono<ProtocolDefinition.InitializeResult> result) {
            this.session = session;
            this.result = result;
        }

        public StreamableServerRuntimeSession getSession() {
            return session;
        }

        public Mono<ProtocolDefinition.InitializeResult> getInitResult() {
            return result;
        }
    }

    public final class SessionStream implements RuntimeSession {
        private volatile boolean healthy = true;
        private final ConcurrentHashMap<Object, MonoSink<ProtocolDefinition.JSONRPCResponse>> pending =
                new ConcurrentHashMap<>();
        private final StreamableServerRuntimeTransport transport;
        private final String transportId;
        private final String msgIdPrefix;

        SessionStream(StreamableServerRuntimeTransport transport) {
            this.transport = transport;
            this.transportId = UUID.randomUUID().toString();
            this.msgIdPrefix = transportId + "_";
        }

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        @Override
        public void setMinLoggingLevel(ProtocolDefinition.LoggingLevel level) {
            RuntimeUtils.notNull(level, "Level required");
            StreamableServerRuntimeSession.this.setMinLoggingLevel(level);
        }

        @Override
        public boolean isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel level) {
            return StreamableServerRuntimeSession.this.isNotificationForLevelAllowed(level);
        }

        @Override
        public <T> Mono<T> sendRequest(String method, Object params, TypeReference<T> typeRef) {
            String reqId = nextRequestId();
            streamMap.put(reqId, this);

            return Mono.defer(() -> {
                        ProtocolDefinition.JSONRPCRequest req = new ProtocolDefinition.JSONRPCRequest(
                                ProtocolDefinition.JSONRPC_VERSION, method, reqId, params);
                        String msgId = msgIdPrefix + UUID.randomUUID();

                        return transport
                                .sendMessage(req, msgId)
                                .onErrorResume(e -> {
                                    if (isDisconnect(e)) {
                                        logger.debug("Client disconnected for {}", reqId);
                                        return Mono.empty();
                                    }
                                    logger.error("Send failed for {}: {}", reqId, e.getMessage());
                                    return Mono.error(e);
                                })
                                .then(Mono.<ProtocolDefinition.JSONRPCResponse>create(
                                        sink -> pending.put(reqId, sink)));
                    })
                    .timeout(timeout)
                    .doOnError(e -> {
                        pending.remove(reqId);
                        streamMap.remove(reqId);
                    })
                    .handle((resp, sink) -> {
                        if (resp.error != null) {
                            sink.error(new ProtocolErrorException(resp.error));
                        } else if (typeRef.getType().equals(Void.class)) {
                            sink.complete();
                        } else {
                            sink.next(transport.unmarshalFrom(resp.result, typeRef));
                        }
                    });
        }

        private boolean isDisconnect(Throwable e) {
            if (e == null) {
                return false;
            }
            String msg = e.getMessage();
            return msg != null
                    && (msg.contains("Connection reset") || msg.contains("Broken pipe") || msg.contains("中止了一个已建立的连接"));
        }

        @Override
        public Mono<Void> sendNotification(String method, Object params) {
            ProtocolDefinition.JSONRPCNotification notif =
                    new ProtocolDefinition.JSONRPCNotification(ProtocolDefinition.JSONRPC_VERSION, method, params);
            return transport.sendMessage(notif, msgIdPrefix + UUID.randomUUID());
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.defer(() -> {
                pending.values().forEach(s -> s.error(new RuntimeException("Stream closed")));
                pending.clear();
                activeStream.compareAndSet(this, fallbackStream);
                streamMap.values().removeIf(this::equals);
                return transport.closeGracefully();
            });
        }

        @Override
        public void close() {
            pending.values().forEach(s -> s.error(new RuntimeException("Stream closed")));
            pending.clear();
            activeStream.compareAndSet(this, fallbackStream);
            streamMap.values().removeIf(this::equals);
            transport.close();
        }
    }
}
