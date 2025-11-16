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

package org.apache.seata.mcp.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import org.apache.seata.mcp.core.protocol.StreamableServerRuntimeSession;
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
    private static final TypeReference<Object> OBJECT_TYPE = new TypeReference<Object>() {};
    private static final int MAX_FAILURES = 3;

    private final Duration initialDelay;
    private final Duration interval;
    private final Scheduler scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Supplier<Flux<RuntimeSession>> sessionSupplier;
    private final Consumer<RuntimeSession> failureHandler;
    private final ConcurrentMap<String, Integer> failureCounts = new ConcurrentHashMap<>();
    private Disposable subscription;

    KeepAliveScheduler(
            Scheduler scheduler,
            Duration initialDelay,
            Duration interval,
            Supplier<Flux<RuntimeSession>> sessionSupplier,
            Consumer<RuntimeSession> failureHandler) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.sessionSupplier = sessionSupplier;
        this.failureHandler = failureHandler;
    }

    public static Builder builder(Supplier<Flux<RuntimeSession>> sessionSupplier) {
        return new Builder(sessionSupplier);
    }

    public Disposable start() {
        if (running.compareAndSet(false, true)) {
            subscription = Flux.interval(initialDelay, interval, scheduler)
                    .flatMap(tick -> processKeepAlive())
                    .doOnCancel(() -> running.set(false))
                    .doOnComplete(() -> running.set(false))
                    .onErrorResume(e -> {
                        logger.error("KeepAlive error", e);
                        running.set(false);
                        return Mono.empty();
                    })
                    .subscribe();
            return subscription;
        }
        throw new IllegalStateException("Already running");
    }

    private Mono<Void> processKeepAlive() {
        return sessionSupplier
                .get()
                .flatMap(session -> {
                    String id = session.toString();
                    return session.sendRequest(ProtocolDefinition.METHOD_PING, null, OBJECT_TYPE)
                            .doOnSuccess(r -> {
                                failureCounts.remove(id);
                                logger.debug("Ping success: {}", id);
                            })
                            .doOnError(e -> {
                                int count = failureCounts.compute(id, (k, v) -> v == null ? 1 : v + 1);
                                logger.warn("Ping failed for {} ({}/{}): {}", id, count, MAX_FAILURES, e.getMessage());
                                if (count >= MAX_FAILURES) {
                                    handleFailure(session, id);
                                }
                            })
                            .onErrorResume(e -> Mono.empty());
                })
                .then();
    }

    private void handleFailure(RuntimeSession session, String id) {
        try {
            logger.info("Removing failed session: {}", id);
            failureCounts.remove(id);
            if (session instanceof StreamableServerRuntimeSession) {
                ((StreamableServerRuntimeSession) session).setHealthy(false);
            }
            if (failureHandler != null) {
                failureHandler.accept(session);
            }
        } catch (Exception e) {
            logger.error("Failure handler error for {}", id, e);
        }
    }

    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        stop();
        if (scheduler != null) {
            scheduler.dispose();
        }
    }

    public Consumer<RuntimeSession> getSessionFailureHandler() {
        return failureHandler;
    }

    public static class Builder {
        private Scheduler scheduler = Schedulers.boundedElastic();
        private Duration initialDelay = Duration.ofSeconds(0);
        private Duration interval = Duration.ofSeconds(30);
        private final Supplier<Flux<RuntimeSession>> sessionSupplier;
        private Consumer<RuntimeSession> failureHandler;

        Builder(Supplier<Flux<RuntimeSession>> sessionSupplier) {
            this.sessionSupplier = sessionSupplier;
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

        public Builder sessionFailureHandler(Consumer<RuntimeSession> handler) {
            this.failureHandler = handler;
            return this;
        }

        public KeepAliveScheduler build() {
            return new KeepAliveScheduler(scheduler, initialDelay, interval, sessionSupplier, failureHandler);
        }
    }
}
