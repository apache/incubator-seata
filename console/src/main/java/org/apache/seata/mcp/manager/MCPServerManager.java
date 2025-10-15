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
package org.apache.seata.mcp.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MCP Service Manager (supports dynamic control)
 */
public class MCPServerManager implements SmartLifecycle {
    private final ReentrantLock stateLock = new ReentrantLock();
    private static final Logger logger = LoggerFactory.getLogger(MCPServerManager.class);
    private final ReentrantLock poolLock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public String toString() {
        return "McpServerManager{" + "stateLock="
                + stateLock + ", heartbeatScheduler="
                + poolLock + ", heartbeatTask="
                + running + ", serverInstance="
                + serverInstance + ", transportProvider="
                + transportProvider + ", config="
                + config + '}';
    }

    public ReentrantLock getStateLock() {
        return stateLock;
    }

    public ReentrantLock getPoolLock() {
        return poolLock;
    }

    public AtomicBoolean getRunning() {
        return running;
    }

    public McpAsyncServer getServerInstance() {
        return serverInstance;
    }

    public MCPProperties getConfig() {
        return config;
    }

    private volatile McpAsyncServer serverInstance;
    private final ControlledTransportProvider transportProvider;
    private final MCPProperties config;

    public MCPServerManager(MCPProperties config, ObjectMapper objectMapper) {
        this.config = config;
        if (config.getMcpType().equals(MCPProperties.SSE_TYPE)) {
            MCPProperties.SseServerProperties properties = config.getSseServerProperties();
            transportProvider = new ControlledSseTransportProvider(
                    objectMapper, properties.getMessageEndpoint(), properties.getSseEndpoint());
        } else {
            MCPProperties.StreamableProperties properties = config.getStreamableProperties();
            transportProvider = new ControlledStreamableTransportProvider(
                    objectMapper,
                    properties.getMcpEndPoint(),
                    new McpTransportContextExtractor(),
                    Duration.ofSeconds(properties.getHeartBeatSecondDuration()));
        }
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return transportProvider.getRouterFunction();
    }

    /**
     * Suspension of Service (Remain Registered)
     */
    public void pause() {
        stateLock.lock();
        try {
            if (running.compareAndSet(true, false)) {
                doPause();
            }
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Restore service
     */
    public void resume() {
        stateLock.lock();
        try {
            if (running.compareAndSet(false, true)) {
                doResume();
            }
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public void start() {
        if (!running.get()) {
            initializeServer();
        }
    }

    @Override
    public void stop() {
        if (running.get()) {
            shutdownServer();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void initializeServer() {
        stateLock.lock();
        try {
            this.serverInstance = buildServer();
            transportProvider.activate();
            running.set(true);
            logServerState("Service initialized and running");
        } finally {
            stateLock.unlock();
        }
    }

    private void doPause() {
        transportProvider.deactivate();
        logServerState("Service paused");
    }

    private void doResume() {
        transportProvider.activate();
        logServerState("Service resumed");
    }

    private void shutdownServer() {
        stateLock.lock();
        try {
            transportProvider.shutdown();
            serverInstance.close();
            running.set(false);
            logServerState("Service fully shutdown");
        } finally {
            stateLock.unlock();
        }
    }

    private McpAsyncServer buildServer() {
        if (transportProvider instanceof ControlledSseTransportProvider) {
            ControlledSseTransportProvider sseTransportProvider = (ControlledSseTransportProvider) transportProvider;
            return McpServer.async(sseTransportProvider)
                    .serverInfo(config.getServerName(), config.getServerVersion())
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .tools(true)
                            .resources(false, false)
                            .prompts(false)
                            .build())
                    .build();
        } else {
            ControlledStreamableTransportProvider streamableTransportProvider =
                    (ControlledStreamableTransportProvider) transportProvider;
            return McpServer.async(streamableTransportProvider)
                    .serverInfo(config.getServerName(), config.getServerVersion())
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .tools(true)
                            .resources(false, false)
                            .prompts(false)
                            .build())
                    .build();
        }
    }

    private void logServerState(String message) {
        //        System.out.printf("[MCP Manager] %s | Running: %b%n", message, running.get());
        logger.info("[MCP Manager] {} | Transport Type: {} | Running: {}", message, config.getMcpType(), running.get());
    }

    private interface ControlledTransportProvider {
        void activate();

        void deactivate();

        void shutdown();

        RouterFunction<ServerResponse> getRouterFunction();
    }

    private static class ControlledSseTransportProvider extends WebMvcSseServerTransportProvider
            implements ControlledTransportProvider {
        private final AtomicBoolean active = new AtomicBoolean(false);

        public ControlledSseTransportProvider(ObjectMapper mapper, String messageEndpoint, String sseEndpoint) {
            super(mapper, messageEndpoint, sseEndpoint);
        }

        @Override
        public RouterFunction<ServerResponse> getRouterFunction() {
            return request -> {
                if (!active.get()) {
                    HandlerFunction<ServerResponse> handler =
                            req -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                    .body("Service is currently unavailable");
                    return Optional.of(handler);
                }
                return super.getRouterFunction().route(request);
            };
        }

        public void activate() {
            active.set(true);
        }

        public void deactivate() {
            active.set(false);
        }

        public void shutdown() {
            super.close();
            active.set(false);
        }
    }

    private static class ControlledStreamableTransportProvider extends WebMvcStreamableServerTransportProvider
            implements ControlledTransportProvider {
        private final AtomicBoolean active = new AtomicBoolean(false);

        public ControlledStreamableTransportProvider(
                ObjectMapper mapper,
                String mcpEndPoint,
                McpTransportContextExtractor contextExtractor,
                Duration keepAliveInterval) {
            super(mapper, mcpEndPoint, true, contextExtractor, keepAliveInterval);
        }

        @Override
        public RouterFunction<ServerResponse> getRouterFunction() {
            return request -> {
                if (!active.get()) {
                    HandlerFunction<ServerResponse> handler =
                            req -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                    .body("Service is currently unavailable");
                    return Optional.of(handler);
                }
                return super.getRouterFunction().route(request);
            };
        }

        public void activate() {
            active.set(true);
        }

        public void deactivate() {
            active.set(false);
        }

        public void shutdown() {
            super.close();
            active.set(false);
        }
    }
}
