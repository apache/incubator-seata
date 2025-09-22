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
 *   - [Optional: Added a heartbeat retry limit mechanism]
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

package io.modelcontextprotocol.util;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSession;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A utility class for scheduling regular keep-alive calls to maintain connections.
 */
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);
    private static final TypeReference<Object> OBJECT_TYPE_REF = new TypeReference<Object>() {};

    private final Duration initialDelay;
    private final Duration interval;
    private final Scheduler scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Disposable currentSubscription;
    private final Supplier<Flux<McpSession>> mcpSessions;

    // Session failure processor
    private final Consumer<McpSession> sessionFailureHandler;
    private final ConcurrentMap<String, Integer> sessionFailureCounts = new ConcurrentHashMap<>();
    private final int maxFailureCount = 3;

    KeepAliveScheduler(
            Scheduler scheduler,
            Duration initialDelay,
            Duration interval,
            Supplier<Flux<McpSession>> mcpSessions,
            Consumer<McpSession> sessionFailureHandler) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.mcpSessions = mcpSessions;
        this.sessionFailureHandler = sessionFailureHandler;
    }

    public static Builder builder(Supplier<Flux<McpSession>> mcpSessions) {
        return new Builder(mcpSessions);
    }

    public Disposable start() {
        if (this.isRunning.compareAndSet(false, true)) {
            this.currentSubscription = Flux.interval(this.initialDelay, this.interval, this.scheduler)
                    .flatMap(tick -> processKeepAlive())
                    .doOnCancel(() -> this.isRunning.set(false))
                    .doOnComplete(() -> this.isRunning.set(false))
                    .onErrorResume(error -> {
                        logger.error("KeepAlive scheduler error", error);
                        this.isRunning.set(false);
                        return Mono.empty();
                    })
                    .subscribe();

            return this.currentSubscription;
        } else {
            throw new IllegalStateException("KeepAlive scheduler is already running. Stop it first.");
        }
    }

    private Mono<Void> processKeepAlive() {
        return this.mcpSessions
                .get()
                .flatMap(session -> {
                    String sessionId = session.toString();

                    return session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF)
                            .doOnSuccess(response -> {
                                // Heartbeat Success: Resets the failure count
                                sessionFailureCounts.remove(sessionId);
                                logger.debug("Keep-alive ping successful for session: {}", sessionId);
                            })
                            .doOnError(error -> {
                                // Heartbeat Failures: Increases the failure count
                                int failures = sessionFailureCounts.compute(
                                        sessionId, (id, count) -> count == null ? 1 : count + 1);

                                logger.warn(
                                        "Keep-alive failed for session {} (attempt {}/{}): {}",
                                        sessionId,
                                        failures,
                                        maxFailureCount,
                                        error.getMessage());

                                // Maximum failures exceeded: Processing sessions
                                if (failures >= maxFailureCount) {
                                    handleSessionFailure(session, sessionId);
                                }
                            })
                            .onErrorResume(error -> Mono.empty());
                })
                .then();
    }

    private void handleSessionFailure(McpSession session, String sessionId) {
        try {
            logger.info("Removing session due to repeated keep-alive failures: {}", sessionId);
            sessionFailureCounts.remove(sessionId);

            McpStreamableServerSession streamableServerSession = (McpStreamableServerSession) session;
            streamableServerSession.setHealthy(false);
        } catch (Exception e) {
            logger.error("Error handling session failure for {}", sessionId, e);
        }
    }

    public void stop() {
        if (this.currentSubscription != null && !this.currentSubscription.isDisposed()) {
            this.currentSubscription.dispose();
        }
        this.isRunning.set(false);
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void shutdown() {
        stop();
        if (this.scheduler != null) {
            ((Disposable) this.scheduler).dispose();
        }
    }

    public Consumer<McpSession> getSessionFailureHandler() {
        return sessionFailureHandler;
    }

    public static class Builder {
        private Scheduler scheduler = Schedulers.boundedElastic();
        private Duration initialDelay = Duration.ofSeconds(0);
        private Duration interval = Duration.ofSeconds(30);
        private final Supplier<Flux<McpSession>> mcpSessions;
        private Consumer<McpSession> sessionFailureHandler;

        Builder(Supplier<Flux<McpSession>> mcpSessions) {
            this.mcpSessions = mcpSessions;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder interval(Duration interval) {
            this.interval = interval;
            return this;
        }

        public Builder sessionFailureHandler(Consumer<McpSession> handler) {
            this.sessionFailureHandler = handler;
            return this;
        }

        public KeepAliveScheduler build() {
            return new KeepAliveScheduler(scheduler, initialDelay, interval, mcpSessions, sessionFailureHandler);
        }
    }
}
