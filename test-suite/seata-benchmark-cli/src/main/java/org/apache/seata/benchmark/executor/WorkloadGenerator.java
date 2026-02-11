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
package org.apache.seata.benchmark.executor;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.apache.seata.benchmark.model.BenchmarkMetrics;
import org.apache.seata.benchmark.model.TransactionRecord;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Workload generator with TPS rate limiting
 */
public class WorkloadGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadGenerator.class);

    private final BenchmarkConfig config;
    private final TransactionExecutor executor;
    private final BenchmarkMetrics metrics;
    private final RateLimiter rateLimiter;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final List<TransactionRecord> recentRecords = Collections.synchronizedList(new ArrayList<>());

    public WorkloadGenerator(BenchmarkConfig config, TransactionExecutor executor, BenchmarkMetrics metrics) {
        this.config = config;
        this.executor = executor;
        this.metrics = metrics;
        this.rateLimiter = RateLimiter.create(config.getTargetTps());
        this.executorService = new ThreadPoolExecutor(
                config.getThreads(),
                config.getThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("workload-generator", config.getThreads()));
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info(
                    "Starting workload generator with target TPS: {}, threads: {}",
                    config.getTargetTps(),
                    config.getThreads());

            long startTime = System.currentTimeMillis();
            // Total duration includes warmup + benchmark duration
            long totalDuration = (config.getWarmupDuration() + config.getDuration()) * 1000L;
            long endTime = startTime + totalDuration;

            for (int i = 0; i < config.getThreads(); i++) {
                executorService.submit(() -> {
                    while (running.get() && System.currentTimeMillis() < endTime) {
                        if (paused.get()) {
                            try {
                                Thread.sleep(BenchmarkConstants.PAUSE_CHECK_INTERVAL_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            continue;
                        }

                        rateLimiter.acquire();
                        executeTransaction();
                    }
                });
            }

            LOGGER.info("Workload generator started");
        }
    }

    private void executeTransaction() {
        try {
            TransactionRecord record = executor.execute();

            if (record.isSuccess()) {
                metrics.recordSuccess(record.getDuration());
            } else {
                metrics.recordFailure(record.getDuration());
            }

            addRecentRecord(record);

        } catch (Exception e) {
            LOGGER.error("Transaction execution error", e);
            metrics.recordFailure(0);
        }
    }

    private void addRecentRecord(TransactionRecord record) {
        synchronized (recentRecords) {
            recentRecords.add(0, record);
            if (recentRecords.size() > BenchmarkConstants.MAX_RECENT_RECORDS) {
                recentRecords.remove(recentRecords.size() - 1);
            }
        }
    }

    public void pause() {
        paused.set(true);
        LOGGER.info("Workload generator paused");
    }

    public void resume() {
        paused.set(false);
        LOGGER.info("Workload generator resumed");
    }

    public boolean isPaused() {
        return paused.get();
    }

    public void waitForCompletion() {
        LOGGER.info("Waiting for benchmark completion...");
        long startTime = System.currentTimeMillis();
        int warmupDuration = config.getWarmupDuration();
        long totalDuration = (warmupDuration + config.getDuration()) * 1000L;
        long endTime = startTime + totalDuration;
        boolean warmupCompleted = warmupDuration == 0;

        if (warmupDuration > 0) {
            LOGGER.info("Warmup phase: {} seconds", warmupDuration);
        }

        try {
            while (System.currentTimeMillis() < endTime) {
                Thread.sleep(1000);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                // Check if warmup phase just completed
                if (!warmupCompleted && elapsed >= warmupDuration) {
                    warmupCompleted = true;
                    metrics.reset();
                    synchronized (recentRecords) {
                        recentRecords.clear();
                    }
                    LOGGER.info(
                            "Warmup completed, metrics reset. Starting benchmark phase: {} seconds",
                            config.getDuration());
                }

                if (elapsed % BenchmarkConstants.PROGRESS_REPORT_INTERVAL_SECONDS == 0 && elapsed > 0) {
                    long hours = elapsed / 3600;
                    long minutes = (elapsed % 3600) / 60;
                    long seconds = elapsed % 60;
                    String phase = warmupCompleted ? "" : "[WARMUP] ";
                    System.out.printf(
                            "%s[%02d:%02d:%02d] %d txns, %.1f txns/sec, %.1f%% success%n",
                            phase,
                            hours,
                            minutes,
                            seconds,
                            metrics.getTotalCount(),
                            metrics.getCurrentTps(),
                            metrics.getSuccessRate());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        stop();
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("Stopping workload generator...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(BenchmarkConstants.SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Workload generator stopped");
        }
    }

    public List<TransactionRecord> getRecentRecords() {
        synchronized (recentRecords) {
            return new ArrayList<>(recentRecords);
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
