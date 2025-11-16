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
import org.apache.seata.mcp.core.protocol.ProtocolError;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import org.apache.seata.mcp.core.protocol.RuntimeTransport;
import org.apache.seata.mcp.core.protocol.RuntimeTransportProvider;
import org.apache.seata.mcp.core.protocol.ServerRuntimeSession;
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

public class WebMvcSseChannelProvider implements RuntimeTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcSseChannelProvider.class);
    private static final String EVENT_MESSAGE = "message";
    private static final String EVENT_ENDPOINT = "endpoint";

    private final ObjectMapper mapper;
    private final String msgEndpoint;
    private final String sseEndpoint;
    private final String baseUrl;
    private final RouterFunction<ServerResponse> router;
    private final ConcurrentHashMap<String, ServerRuntimeSession> activeSessions = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown = false;
    private ServerRuntimeSession.Factory factory;

    public WebMvcSseChannelProvider(ObjectMapper mapper, String msgEndpoint, String sseEndpoint) {
        this(mapper, "", msgEndpoint, sseEndpoint);
    }

    public WebMvcSseChannelProvider(ObjectMapper mapper, String baseUrl, String msgEndpoint, String sseEndpoint) {
        RuntimeUtils.notNull(mapper, "Mapper required");
        RuntimeUtils.notNull(baseUrl, "Base URL required");
        RuntimeUtils.notNull(msgEndpoint, "Message endpoint required");
        RuntimeUtils.notNull(sseEndpoint, "SSE endpoint required");

        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.msgEndpoint = msgEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.router = RouterFunctions.route()
                .GET(sseEndpoint, this::connectSse)
                .POST(msgEndpoint, this::processMessage)
                .build();
    }

    @Override
    public void setSessionFactory(ServerRuntimeSession.Factory factory) {
        this.factory = factory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (activeSessions.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(activeSessions.values())
                .flatMap(s -> s.sendNotification(method, params).onErrorResume(e -> {
                    logger.error("Notify failed for {}: {}", s.getId(), e.getMessage());
                    return Mono.empty();
                }))
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(activeSessions.values())
                .flatMap(ServerRuntimeSession::closeGracefully)
                .doOnComplete(activeSessions::clear)
                .then();
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return router;
    }

    @Override
    public List<String> protocolVersions() {
        return Arrays.asList(ProtocolDefinition.VERSION_2024_11_05, ProtocolDefinition.VERSION_2025_06_18);
    }

    private ServerResponse connectSse(ServerRequest req) {
        if (shuttingDown) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Shutting down");
        }

        String id = UUID.randomUUID().toString();
        logger.debug("SSE connect: {}", id);

        try {
            return ServerResponse.sse(
                    sse -> {
                        sse.onComplete(() -> removeSession(id));
                        sse.onTimeout(() -> removeSession(id));

                        SseTransport transport = new SseTransport(id, sse);
                        ServerRuntimeSession session = factory.create(transport);
                        activeSessions.put(id, session);

                        try {
                            sse.id(id).event(EVENT_ENDPOINT).data(baseUrl + msgEndpoint + "?sessionId=" + id);
                        } catch (Exception e) {
                            logger.error("Send endpoint failed: {}", e.getMessage());
                            sse.error(e);
                        }
                    },
                    Duration.ZERO);
        } catch (Exception e) {
            logger.error("SSE setup failed for {}: {}", id, e.getMessage());
            removeSession(id);
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ServerResponse processMessage(ServerRequest req) {
        if (shuttingDown) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Shutting down");
        }

        String sessionId = req.param("sessionId").orElse(null);
        if (sessionId == null) {
            return ServerResponse.badRequest().body(new ProtocolError("Missing session ID"));
        }

        ServerRuntimeSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                    .body(new ProtocolError("Session not found: " + sessionId));
        }

        try {
            String body = req.body(String.class);
            ProtocolDefinition.JSONRPCMessage msg = ProtocolDefinition.deserializeJsonRpcMessage(mapper, body);
            session.handle(msg).block();
            return ServerResponse.ok().build();
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Deserialize failed: {}", e.getMessage());
            return ServerResponse.badRequest().body(new ProtocolError("Invalid format"));
        } catch (Exception e) {
            logger.error("Process failed: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ProtocolError(e.getMessage()));
        }
    }

    private void removeSession(String id) {
        activeSessions.remove(id);
        logger.debug("Session removed: {}", id);
    }

    private class SseTransport implements RuntimeTransport {
        private final String id;
        private final SseBuilder sse;

        SseTransport(String id, SseBuilder sse) {
            this.id = id;
            this.sse = sse;
        }

        @Override
        public Mono<Void> sendMessage(ProtocolDefinition.JSONRPCMessage msg) {
            return Mono.fromCallable(() -> {
                        String json = mapper.writeValueAsString(msg);
                        sse.id(id).event(EVENT_MESSAGE).data(json);
                        return null;
                    })
                    .onErrorResume(e -> {
                        logger.error("Send failed for {}: {}", id, e.getMessage());
                        sse.error(e);
                        RuntimeSession s = activeSessions.get(id);
                        if (s != null) s.close();
                        return Mono.empty();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
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
            try {
                sse.complete();
                removeSession(id);
            } catch (Exception e) {
                logger.warn("Close failed for {}: {}", id, e.getMessage());
            }
        }
    }
}
