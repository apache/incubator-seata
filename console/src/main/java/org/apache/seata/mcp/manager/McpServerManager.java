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
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.config.MCPConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MCP Service Manager (supports dynamic control)
 */
public class McpServerManager implements SmartLifecycle {
    private final ReentrantLock stateLock = new ReentrantLock();
    private ScheduledExecutorService heartbeatScheduler;
    private static final Logger logger = LoggerFactory.getLogger(McpServerManager.class); // 日志记录
    private final boolean heartbeat;
    private final ReentrantLock poolLock = new ReentrantLock(); // 线程池锁
    private Future<?> heartbeatTask;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public String toString() {
        return "McpServerManager{" + "stateLock="
                + stateLock + ", heartbeatScheduler="
                + heartbeatScheduler + ", heartbeat="
                + heartbeat + ", poolLock="
                + poolLock + ", heartbeatTask="
                + heartbeatTask + ", running="
                + running + ", serverInstance="
                + serverInstance + ", transportProvider="
                + transportProvider + ", config="
                + config + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        McpServerManager that = (McpServerManager) o;
        return heartbeat == that.heartbeat
                && Objects.equals(stateLock, that.stateLock)
                && Objects.equals(heartbeatScheduler, that.heartbeatScheduler)
                && Objects.equals(poolLock, that.poolLock)
                && Objects.equals(heartbeatTask, that.heartbeatTask)
                && Objects.equals(running, that.running)
                && Objects.equals(serverInstance, that.serverInstance)
                && Objects.equals(transportProvider, that.transportProvider)
                && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                stateLock,
                heartbeatScheduler,
                heartbeat,
                poolLock,
                heartbeatTask,
                running,
                serverInstance,
                transportProvider,
                config);
    }

    public ReentrantLock getStateLock() {
        return stateLock;
    }

    public ScheduledExecutorService getHeartbeatScheduler() {
        return heartbeatScheduler;
    }

    public void setHeartbeatScheduler(ScheduledExecutorService heartbeatScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public ReentrantLock getPoolLock() {
        return poolLock;
    }

    public Future<?> getHeartbeatTask() {
        return heartbeatTask;
    }

    public void setHeartbeatTask(Future<?> heartbeatTask) {
        this.heartbeatTask = heartbeatTask;
    }

    public AtomicBoolean getRunning() {
        return running;
    }

    public McpAsyncServer getServerInstance() {
        return serverInstance;
    }

    public void setServerInstance(McpAsyncServer serverInstance) {
        this.serverInstance = serverInstance;
    }

    public ControlledTransportProvider getTransportProvider() {
        return transportProvider;
    }

    public MCPConfiguration getConfig() {
        return config;
    }

    private volatile McpAsyncServer serverInstance;
    private final ControlledTransportProvider transportProvider;
    private final MCPConfiguration config;

    public McpServerManager(MCPConfiguration config, ObjectMapper objectMapper) {
        this.config = config;
        this.transportProvider =
                new ControlledTransportProvider(objectMapper, config.getMessageEndpoint(), config.getSseEndpoint());
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        this.heartbeat = config.isHeartbeat();
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return transportProvider.getRouterFunction();
    }


    private void initScheduler() {
        poolLock.lock();
        try {
            if (heartbeatScheduler == null || heartbeatScheduler.isTerminated()) {
                heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "mcp-heartbeat");
                    t.setDaemon(true);
                    return t;
                });
            }
        } finally {
            poolLock.unlock();
        }
    }

    private void startHeartbeat() {
        poolLock.lock();
        try {
            initScheduler();

            if (heartbeat) {
                heartbeatScheduler.scheduleAtFixedRate(
                        this::sendHeartbeat,
                        0,
                        15000, // 15 second intervals
                        TimeUnit.MILLISECONDS);
            }
        } finally {
            poolLock.unlock();
        }
    }

    private void stopHeartbeat() {
        poolLock.lock();
        try {
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdownNow();
                heartbeatScheduler = null;
            }
        } finally {
            poolLock.unlock();
        }
    }

    private void sendHeartbeat() {
        try {
            transportProvider.sendHeartbeat();
        } catch (Exception e) {
            logger.debug("HeartBeatError:{}", e.getCause().getMessage(), e);
        }
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
            startHeartbeat();
            logServerState("Service initialized and running");
        } finally {
            stateLock.unlock();
        }
    }


    private void doPause() {
        transportProvider.deactivate();
        stopHeartbeat();
        logServerState("Service paused");
    }


    private void doResume() {
        transportProvider.activate();
        startHeartbeat();
        logServerState("Service resumed");
    }


    private void shutdownServer() {
        stateLock.lock();
        try {
            transportProvider.shutdown();
            serverInstance.close();
            running.set(false);
            stopHeartbeat();
            logServerState("Service fully shutdown");
        } finally {
            stateLock.unlock();
        }
    }

    private McpAsyncServer buildServer() {
        return McpServer.async(transportProvider)
                .serverInfo(config.getServerName(), config.getServerVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(config.isResourceSupport(), config.isResourceTemplates())
                        .build())
                .build();
    }

    private void logServerState(String message) {
        //        System.out.printf("[MCP Manager] %s | Running: %b%n", message, running.get());
        logger.info("[MCP Manager] {} | Running: {}", message, running.get());
    }


    private static class ControlledTransportProvider extends WebMvcSseServerTransportProvider {
        private final AtomicBoolean active = new AtomicBoolean(false);

        public ControlledTransportProvider(ObjectMapper mapper, String messageEndpoint, String sseEndpoint) {
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
}
