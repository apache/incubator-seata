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
package org.apache.seata.mcp.core.runtimeCore.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.ProtocolErrorException;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import org.apache.seata.mcp.core.protocol.StreamableRuntimeTransportProvider;
import org.apache.seata.mcp.core.protocol.StreamableServerRuntimeSession;
import org.apache.seata.mcp.core.protocol.StreamableServerRuntimeTransport;
import org.apache.seata.mcp.core.runtimeCore.DefaultRuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContextResolver;
import org.apache.seata.mcp.core.util.KeepAliveScheduler;
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

public class WebMvcStreamableChannelProvider implements StreamableRuntimeTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcStreamableChannelProvider.class);
    private static final String EVENT_MESSAGE = "message";

    private final String endpoint;
    private final boolean noDelete;
    private final ObjectMapper mapper;
    private final RouterFunction<ServerResponse> router;
    private final ConcurrentHashMap<String, StreamableServerRuntimeSession> activeSessions = new ConcurrentHashMap<>();
    private final RuntimeContextResolver contextResolver;
    private volatile boolean shuttingDown = false;
    private KeepAliveScheduler keepAlive;
    private StreamableServerRuntimeSession.Factory factory;

    public WebMvcStreamableChannelProvider(
            ObjectMapper mapper,
            String endpoint,
            boolean noDelete,
            RuntimeContextResolver contextResolver,
            Duration keepAliveInterval) {
        RuntimeUtils.notNull(mapper, "Mapper required");
        RuntimeUtils.notNull(endpoint, "Endpoint required");
        RuntimeUtils.notNull(contextResolver, "Context resolver required");

        this.mapper = mapper;
        this.endpoint = endpoint;
        this.noDelete = noDelete;
        this.contextResolver = contextResolver;
        this.router = RouterFunctions.route()
                .GET(endpoint, this::handleGet)
                .POST(endpoint, this::handlePost)
                .DELETE(endpoint, this::handleDelete)
                .build();

        if (keepAliveInterval != null) {
            this.keepAlive = KeepAliveScheduler.builder(() -> {
                        if (shuttingDown) {
                            return Flux.empty();
                        }
                        return Flux.fromIterable(activeSessions.values())
                                .publishOn(Schedulers.boundedElastic())
                                .filter(s -> {
                                    if (!s.isHealthy()) {
                                        logger.warn("Removing unhealthy session: {}", s.getId());
                                        s.closeGracefully().subscribe();
                                        activeSessions.remove(s.getId());
                                        return false;
                                    }
                                    return true;
                                })
                                .cast(RuntimeSession.class);
                    })
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();
            this.keepAlive.start();
        }
    }

    @Override
    public List<String> protocolVersions() {
        return Arrays.asList(ProtocolDefinition.VERSION_2024_11_05, ProtocolDefinition.VERSION_2025_06_18);
    }

    @Override
    public void setSessionFactory(StreamableServerRuntimeSession.Factory factory) {
        this.factory = factory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (activeSessions.isEmpty()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> activeSessions.values().parallelStream().forEach(s -> {
            try {
                s.sendNotification(method, params).block();
            } catch (Exception e) {
                logger.error("Notify failed for {}: {}", s.getId(), e.getMessage());
            }
        }));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
                    shuttingDown = true;
                    activeSessions.values().parallelStream().forEach(s -> {
                        try {
                            s.closeGracefully().block();
                        } catch (Exception e) {
                            logger.error("Close failed for {}: {}", s.getId(), e.getMessage());
                        }
                    });
                    activeSessions.clear();
                })
                .then()
                .doOnSuccess(v -> {
                    if (keepAlive != null) {
                        keepAlive.shutdown();
                    }
                });
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return router;
    }

    private ServerResponse handleGet(ServerRequest req) {
        if (shuttingDown) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Shutting down");
        }

        if (!req.headers().asHttpHeaders().getAccept().contains(MediaType.TEXT_EVENT_STREAM)) {
            return ServerResponse.badRequest().body("Invalid Accept header");
        }

        RuntimeContext ctx = contextResolver.extract(req, new DefaultRuntimeContext());
        String sessionId = req.headers().asHttpHeaders().getFirst(ProtocolDefinition.HEADER_SESSION_ID);
        if (sessionId == null) {
            return ServerResponse.badRequest().body("Session ID required");
        }

        StreamableServerRuntimeSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ServerResponse.notFound().build();
        }

        try {
            return ServerResponse.sse(
                    sse -> {
                        sse.onTimeout(() -> logger.debug("SSE timeout: {}", sessionId));

                        StreamableTransport transport = new StreamableTransport(sessionId, sse);

                        String lastEventId =
                                req.headers().asHttpHeaders().getFirst(ProtocolDefinition.HEADER_LAST_EVENT_ID);
                        if (lastEventId != null) {
                            session.replay(lastEventId)
                                    .contextWrite(c -> c.put(RuntimeContext.KEY, ctx))
                                    .toIterable()
                                    .forEach(msg -> {
                                        try {
                                            transport
                                                    .sendMessage(msg)
                                                    .contextWrite(c -> c.put(RuntimeContext.KEY, ctx))
                                                    .block();
                                        } catch (Exception e) {
                                            logger.error("Replay failed: {}", e.getMessage());
                                            sse.error(e);
                                        }
                                    });
                        } else {
                            StreamableServerRuntimeSession.SessionStream stream = session.listeningStream(transport);
                            sse.onComplete(() -> {
                                logger.debug("SSE complete: {}", sessionId);
                                stream.close();
                            });
                        }
                    },
                    Duration.ZERO);
        } catch (Exception e) {
            logger.error("GET failed for {}: {}", sessionId, e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ServerResponse handlePost(ServerRequest req) {
        if (shuttingDown) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Shutting down");
        }

        List<MediaType> accept = req.headers().asHttpHeaders().getAccept();
        if (!accept.contains(MediaType.TEXT_EVENT_STREAM) || !accept.contains(MediaType.APPLICATION_JSON)) {
            return ServerResponse.badRequest().body(new ProtocolErrorException("Invalid Accept headers"));
        }

        RuntimeContext ctx = contextResolver.extract(req, new DefaultRuntimeContext());

        try {
            String body = req.body(String.class);
            ProtocolDefinition.JSONRPCMessage msg = ProtocolDefinition.deserializeJsonRpcMessage(mapper, body);

            if (msg instanceof ProtocolDefinition.JSONRPCRequest) {
                ProtocolDefinition.JSONRPCRequest request = (ProtocolDefinition.JSONRPCRequest) msg;
                if (ProtocolDefinition.METHOD_INITIALIZE.equals(request.getMethod())) {
                    return handleInitialize(request, ctx);
                }
            }

            String sessionId = req.headers().asHttpHeaders().getFirst(ProtocolDefinition.HEADER_SESSION_ID);
            if (sessionId == null) {
                return ServerResponse.badRequest().body(new ProtocolErrorException("Session ID missing"));
            }

            StreamableServerRuntimeSession session = activeSessions.get(sessionId);
            if (session == null) {
                return ServerResponse.status(HttpStatus.NOT_FOUND)
                        .body(new ProtocolErrorException("Session not found: " + sessionId));
            }

            return handleMessage(msg, session, ctx, sessionId);
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Deserialize failed: {}", e.getMessage());
            return ServerResponse.badRequest().body(new ProtocolErrorException("Invalid format"));
        } catch (Exception e) {
            logger.error("Process failed: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ProtocolErrorException(e.getMessage()));
        }
    }

    private ServerResponse handleInitialize(ProtocolDefinition.JSONRPCRequest request, RuntimeContext ctx) {
        try {
            ProtocolDefinition.InitializeRequest initReq = mapper.convertValue(
                    request.getParams(), new TypeReference<ProtocolDefinition.InitializeRequest>() {});
            StreamableServerRuntimeSession.StreamableServerRuntimeSessionInit init = factory.startSession(initReq);
            activeSessions.put(init.getSession().getId(), init.getSession());

            ProtocolDefinition.InitializeResult result = init.getInitResult().block();
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            ProtocolDefinition.HEADER_SESSION_ID,
                            init.getSession().getId())
                    .body(new ProtocolDefinition.JSONRPCResponse(
                            ProtocolDefinition.JSONRPC_VERSION, request.getId(), result, null));
        } catch (Exception e) {
            logger.error("Init failed: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ProtocolErrorException(e.getMessage()));
        }
    }

    private ServerResponse handleMessage(
            ProtocolDefinition.JSONRPCMessage msg,
            StreamableServerRuntimeSession session,
            RuntimeContext ctx,
            String sessionId) {
        if (msg instanceof ProtocolDefinition.JSONRPCResponse) {
            session.accept((ProtocolDefinition.JSONRPCResponse) msg)
                    .contextWrite(c -> c.put(RuntimeContext.KEY, ctx))
                    .block();
            return ServerResponse.accepted().build();
        } else if (msg instanceof ProtocolDefinition.JSONRPCNotification) {
            session.accept((ProtocolDefinition.JSONRPCNotification) msg)
                    .contextWrite(c -> c.put(RuntimeContext.KEY, ctx))
                    .block();
            return ServerResponse.accepted().build();
        } else if (msg instanceof ProtocolDefinition.JSONRPCRequest) {
            return ServerResponse.sse(
                    sse -> {
                        sse.onComplete(() -> logger.debug("Stream complete: {}", sessionId));
                        sse.onTimeout(() -> logger.debug("Stream timeout: {}", sessionId));

                        StreamableTransport transport = new StreamableTransport(sessionId, sse);
                        try {
                            session.responseStream((ProtocolDefinition.JSONRPCRequest) msg, transport)
                                    .contextWrite(c -> c.put(RuntimeContext.KEY, ctx))
                                    .block();
                        } catch (Exception e) {
                            logger.error("Stream failed: {}", e.getMessage());
                            sse.error(e);
                        }
                    },
                    Duration.ZERO);
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ProtocolErrorException("Unknown message type"));
    }

    private ServerResponse handleDelete(ServerRequest req) {
        if (shuttingDown) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Shutting down");
        }
        if (noDelete) {
            return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        String sessionId = req.headers().asHttpHeaders().getFirst(ProtocolDefinition.HEADER_SESSION_ID);
        if (sessionId == null) {
            return ServerResponse.badRequest().body("Session ID required");
        }

        StreamableServerRuntimeSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ServerResponse.notFound().build();
        }

        try {
            RuntimeContext ctx = contextResolver.extract(req, new DefaultRuntimeContext());
            session.delete().contextWrite(c -> c.put(RuntimeContext.KEY, ctx)).block();
            activeSessions.remove(sessionId);
            return ServerResponse.ok().build();
        } catch (Exception e) {
            logger.error("Delete failed for {}: {}", sessionId, e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ProtocolErrorException(e.getMessage()));
        }
    }

    private class StreamableTransport implements StreamableServerRuntimeTransport {
        private final String id;
        private final SseBuilder sse;
        private final ReentrantLock lock = new ReentrantLock();
        private volatile boolean closed = false;

        StreamableTransport(String id, SseBuilder sse) {
            this.id = id;
            this.sse = sse;
        }

        @Override
        public Mono<Void> sendMessage(ProtocolDefinition.JSONRPCMessage msg) {
            return sendMessage(msg, null);
        }

        @Override
        public Mono<Void> sendMessage(ProtocolDefinition.JSONRPCMessage msg, String msgId) {
            return Mono.fromRunnable(() -> {
                if (closed) return;

                lock.lock();
                try {
                    if (closed) return;

                    String json = mapper.writeValueAsString(msg);
                    sse.id(msgId != null ? msgId : id).event(EVENT_MESSAGE).data(json);
                } catch (Exception e) {
                    if (isDisconnect(e)) {
                        logger.debug("Client disconnected: {}", id);
                    } else {
                        logger.error("Send failed for {}: {}", id, e.getMessage());
                    }
                    try {
                        sse.complete();
                    } catch (Exception ignored) {
                    }
                } finally {
                    lock.unlock();
                }
            });
        }

        private boolean isDisconnect(Throwable e) {
            if (e == null) return false;
            String msg = e.getMessage();
            if (msg == null) return false;
            return msg.contains("Connection reset")
                    || msg.contains("Broken pipe")
                    || msg.contains("Connection aborted")
                    || msg.contains("Socket closed")
                    || msg.contains("forcibly closed");
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return mapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(this::close);
        }

        @Override
        public void close() {
            lock.lock();
            try {
                if (closed) return;
                closed = true;
                sse.complete();
            } catch (Exception e) {
                logger.warn("Close failed for {}: {}", id, e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }
}
