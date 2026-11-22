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
package org.apache.seata.server.coordinator.pipeline;

import java.util.Objects;

/**
 * Cohesive assembly container wiring {@link PipelineConfig}, {@link PipelineMetrics},
 * {@link BackgroundDbResourceGuard}, {@link InFlightSessionRegistry},
 * {@link BatchGlobalSessionCleaner}, and {@link PhaseTwoPipelineExecutor} with guaranteed
 * shared registry, resource guard, and metrics instances.
 *
 * <p>Recommended entry point for Wave 2 (#8357 / #8358) coordinator integration.
 */
public final class PipelineComponents {

    private final PipelineConfig config;
    private final PipelineMetrics metrics;
    private final BackgroundDbResourceGuard dbResourceGuard;
    private final InFlightSessionRegistry inFlightRegistry;
    private final BatchGlobalSessionCleaner cleaner;
    private final PhaseTwoPipelineExecutor executor;

    private PipelineComponents(
            PipelineConfig config,
            PipelineMetrics metrics,
            BackgroundDbResourceGuard dbResourceGuard,
            InFlightSessionRegistry inFlightRegistry,
            BatchGlobalSessionCleaner cleaner,
            PhaseTwoPipelineExecutor executor) {
        this.config = config;
        this.metrics = metrics;
        this.dbResourceGuard = dbResourceGuard;
        this.inFlightRegistry = inFlightRegistry;
        this.cleaner = cleaner;
        this.executor = executor;
    }

    /**
     * Assembles a complete set of two-phase cleanup pipeline components sharing a single
     * {@link InFlightSessionRegistry}, {@link BackgroundDbResourceGuard}, and
     * {@link PipelineMetrics}. Call {@link #start()} before offering sessions.
     *
     * @param config        pipeline configuration
     * @param commitHandler phase-two commit handler (must enforce {@code retrying = true})
     * @param deleteHandler optional batch delete handler; when {@code null}, no batch cleaner is
     *                      created and completed sessions release leases directly in the executor
     * @param metrics       optional metrics collector; a standalone instance is created if {@code null}
     * @return wired {@link PipelineComponents}
     */
    public static PipelineComponents create(
            PipelineConfig config,
            PhaseTwoPipelineExecutor.PhaseTwoCommitHandler commitHandler,
            BatchGlobalSessionCleaner.BatchDeleteHandler deleteHandler,
            PipelineMetrics metrics) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(commitHandler, "commitHandler must not be null");
        PipelineMetrics effectiveMetrics = metrics != null ? metrics : new PipelineMetrics();
        BackgroundDbResourceGuard dbGuard =
                new BackgroundDbResourceGuard(config.getDbMaxConcurrency(), effectiveMetrics);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(config.getInFlightLeaseMs());
        BatchGlobalSessionCleaner cleaner = deleteHandler != null
                ? new BatchGlobalSessionCleaner(config, deleteHandler, dbGuard, registry, effectiveMetrics)
                : null;
        PhaseTwoPipelineExecutor executor =
                new PhaseTwoPipelineExecutor(config, commitHandler, cleaner, registry, dbGuard, effectiveMetrics);
        return new PipelineComponents(config, effectiveMetrics, dbGuard, registry, cleaner, executor);
    }

    /**
     * Explicitly starts the executor (and batch cleaner when configured).
     */
    public void start() {
        executor.start();
    }

    /**
     * Gracefully shuts down the executor and batch cleaner within {@code timeoutMs}.
     *
     * @param timeoutMs shutdown wait budget in milliseconds
     * @return {@code true} if all components terminated cleanly within the timeout
     */
    public boolean shutdown(long timeoutMs) {
        return executor.shutdown(timeoutMs);
    }

    public PipelineConfig getConfig() {
        return config;
    }

    public PipelineMetrics getMetrics() {
        return metrics;
    }

    public BackgroundDbResourceGuard getDbResourceGuard() {
        return dbResourceGuard;
    }

    public InFlightSessionRegistry getInFlightRegistry() {
        return inFlightRegistry;
    }

    public BatchGlobalSessionCleaner getCleaner() {
        return cleaner;
    }

    public PhaseTwoPipelineExecutor getExecutor() {
        return executor;
    }
}
