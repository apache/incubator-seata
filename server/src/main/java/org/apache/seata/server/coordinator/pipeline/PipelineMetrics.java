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

import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.server.metrics.MeterIdConstants;
import org.apache.seata.server.metrics.MetricsManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntSupplier;

/**
 * Thread-safe observability metrics for the two-phase cleanup pipeline.
 *
 * <p>Maintains low-overhead local atomic counters/timers/gauges for direct inspection and
 * optionally forwards updates to a Seata metrics {@link Registry} (such as the one managed
 * by {@link MetricsManager}) when supplied.
 */
public class PipelineMetrics {

    private static final IntSupplier ZERO_SUPPLIER = () -> 0;

    private final Registry registry;

    private final LongAdder enqueued = new LongAdder();
    private final LongAdder dropped = new LongAdder();
    private final LongAdder dedupSkipped = new LongAdder();
    private final LongAdder rpcSuccess = new LongAdder();
    private final LongAdder rpcFailure = new LongAdder();
    private final LongAdder rpcLatencyTotalNanos = new LongAdder();
    private final LongAdder rpcLatencyCount = new LongAdder();
    private final AtomicLong rpcLatencyMaxNanos = new AtomicLong(0L);

    private final LongAdder cleanerBatches = new LongAdder();
    private final LongAdder cleanerDeletedSessions = new LongAdder();
    private final LongAdder cleanerBatchFailures = new LongAdder();
    private final LongAdder cleanerDropped = new LongAdder();
    private final LongAdder cleanerFlushTotalNanos = new LongAdder();
    private final AtomicLong cleanerFlushMaxNanos = new AtomicLong(0L);

    private final LongAdder dbGuardWaitTotalNanos = new LongAdder();
    private final LongAdder dbGuardWaitCount = new LongAdder();
    private final AtomicLong dbGuardWaitMaxNanos = new AtomicLong(0L);
    private final AtomicInteger dbGuardActive = new AtomicInteger(0);
    private final AtomicInteger dbGuardWaiting = new AtomicInteger(0);

    private volatile IntSupplier queueDepthSupplier = ZERO_SUPPLIER;
    private volatile IntSupplier cleanerBufferDepthSupplier = ZERO_SUPPLIER;
    private volatile IntSupplier inFlightCountSupplier = ZERO_SUPPLIER;

    /**
     * Creates a standalone {@link PipelineMetrics} instance with local counters only.
     */
    public PipelineMetrics() {
        this(null);
    }

    /**
     * Creates a {@link PipelineMetrics} instance backed by the given Seata {@link Registry}.
     *
     * @param registry optional metrics registry; may be {@code null} when metrics export is disabled
     */
    public PipelineMetrics(Registry registry) {
        this.registry = registry;
        if (this.registry != null) {
            this.registry.getGauge(MeterIdConstants.GAUGE_PIPELINE_QUEUE_DEPTH, this::getCurrentQueueDepth);
            this.registry.getGauge(
                    MeterIdConstants.GAUGE_PIPELINE_CLEANER_BUFFER_DEPTH, this::getCurrentCleanerBufferDepth);
            this.registry.getGauge(MeterIdConstants.GAUGE_PIPELINE_INFLIGHT_COUNT, this::getCurrentInFlightCount);
            this.registry.getGauge(MeterIdConstants.GAUGE_PIPELINE_DB_GUARD_ACTIVE, this::getDbGuardActiveCount);
            this.registry.getGauge(MeterIdConstants.GAUGE_PIPELINE_DB_GUARD_WAITING, this::getDbGuardWaitingCount);
        }
    }

    /**
     * Creates a {@link PipelineMetrics} instance attached to the global {@link MetricsManager} registry.
     *
     * @return metrics instance bound to {@link MetricsManager#get()}
     */
    public static PipelineMetrics withGlobalRegistry() {
        return new PipelineMetrics(MetricsManager.get().getRegistry());
    }

    public void bindQueueDepthSupplier(IntSupplier supplier) {
        this.queueDepthSupplier = supplier != null ? supplier : ZERO_SUPPLIER;
    }

    public void bindCleanerBufferDepthSupplier(IntSupplier supplier) {
        this.cleanerBufferDepthSupplier = supplier != null ? supplier : ZERO_SUPPLIER;
    }

    public void bindInFlightCountSupplier(IntSupplier supplier) {
        this.inFlightCountSupplier = supplier != null ? supplier : ZERO_SUPPLIER;
    }

    public void recordEnqueued() {
        enqueued.increment();
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_ENQUEUED).increase(1L);
        }
    }

    public void recordDropped() {
        dropped.increment();
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_DROPPED).increase(1L);
        }
    }

    public void recordDedupSkipped() {
        dedupSkipped.increment();
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_DEDUP_SKIPPED).increase(1L);
        }
    }

    public void recordRpcSuccess(long durationNanos) {
        rpcSuccess.increment();
        recordRpcLatency(durationNanos);
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_RPC_SUCCESS).increase(1L);
        }
    }

    public void recordRpcFailure(long durationNanos) {
        rpcFailure.increment();
        recordRpcLatency(durationNanos);
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_RPC_FAILURE).increase(1L);
        }
    }

    private void recordRpcLatency(long durationNanos) {
        long sanitized = Math.max(0L, durationNanos);
        rpcLatencyCount.increment();
        rpcLatencyTotalNanos.add(sanitized);
        rpcLatencyMaxNanos.accumulateAndGet(sanitized, Math::max);
        if (registry != null) {
            registry.getTimer(MeterIdConstants.TIMER_PIPELINE_RPC_LATENCY).record(sanitized, TimeUnit.NANOSECONDS);
        }
    }

    public void recordCleanerBatch(int deletedSessions, long durationNanos) {
        long sanitizedDuration = Math.max(0L, durationNanos);
        int sanitizedSessions = Math.max(0, deletedSessions);
        cleanerBatches.increment();
        cleanerDeletedSessions.add(sanitizedSessions);
        cleanerFlushTotalNanos.add(sanitizedDuration);
        cleanerFlushMaxNanos.accumulateAndGet(sanitizedDuration, Math::max);
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_CLEANER_BATCHES)
                    .increase(1L);
            if (sanitizedSessions > 0) {
                registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_CLEANER_DELETED_SESSIONS)
                        .increase(sanitizedSessions);
            }
            registry.getTimer(MeterIdConstants.TIMER_PIPELINE_CLEANER_FLUSH_LATENCY)
                    .record(sanitizedDuration, TimeUnit.NANOSECONDS);
        }
    }

    public void recordCleanerDeletedSessions(int deletedSessions) {
        int sanitized = Math.max(0, deletedSessions);
        if (sanitized == 0) {
            return;
        }
        cleanerDeletedSessions.add(sanitized);
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_CLEANER_DELETED_SESSIONS)
                    .increase(sanitized);
        }
    }

    public void recordCleanerBatchFailure() {
        cleanerBatchFailures.increment();
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_CLEANER_BATCH_FAILURES)
                    .increase(1L);
        }
    }

    public void recordCleanerDropped() {
        cleanerDropped.increment();
        if (registry != null) {
            registry.getCounter(MeterIdConstants.COUNTER_PIPELINE_CLEANER_DROPPED)
                    .increase(1L);
        }
    }

    public void recordDbGuardWait(long waitNanos) {
        long sanitized = Math.max(0L, waitNanos);
        dbGuardWaitCount.increment();
        dbGuardWaitTotalNanos.add(sanitized);
        dbGuardWaitMaxNanos.accumulateAndGet(sanitized, Math::max);
        if (registry != null) {
            registry.getTimer(MeterIdConstants.TIMER_PIPELINE_DB_GUARD_WAIT).record(sanitized, TimeUnit.NANOSECONDS);
        }
    }

    public void incrementDbGuardActive() {
        dbGuardActive.incrementAndGet();
    }

    public void decrementDbGuardActive() {
        dbGuardActive.decrementAndGet();
    }

    public void incrementDbGuardWaiting() {
        dbGuardWaiting.incrementAndGet();
    }

    public void decrementDbGuardWaiting() {
        dbGuardWaiting.decrementAndGet();
    }

    public long getEnqueuedCount() {
        return enqueued.sum();
    }

    public long getDroppedCount() {
        return dropped.sum();
    }

    public long getDedupSkippedCount() {
        return dedupSkipped.sum();
    }

    public long getRpcSuccessCount() {
        return rpcSuccess.sum();
    }

    public long getRpcFailureCount() {
        return rpcFailure.sum();
    }

    public long getRpcLatencyCount() {
        return rpcLatencyCount.sum();
    }

    public long getRpcLatencyTotalNanos() {
        return rpcLatencyTotalNanos.sum();
    }

    public long getRpcLatencyMaxNanos() {
        return rpcLatencyMaxNanos.get();
    }

    public long getCleanerBatchesCount() {
        return cleanerBatches.sum();
    }

    public long getCleanerDeletedSessionsCount() {
        return cleanerDeletedSessions.sum();
    }

    public long getCleanerBatchFailuresCount() {
        return cleanerBatchFailures.sum();
    }

    public long getCleanerDroppedCount() {
        return cleanerDropped.sum();
    }

    public long getCleanerFlushTotalNanos() {
        return cleanerFlushTotalNanos.sum();
    }

    public long getCleanerFlushMaxNanos() {
        return cleanerFlushMaxNanos.get();
    }

    public long getDbGuardWaitCount() {
        return dbGuardWaitCount.sum();
    }

    public long getDbGuardWaitTotalNanos() {
        return dbGuardWaitTotalNanos.sum();
    }

    public long getDbGuardWaitMaxNanos() {
        return dbGuardWaitMaxNanos.get();
    }

    public int getDbGuardActiveCount() {
        return dbGuardActive.get();
    }

    public int getDbGuardWaitingCount() {
        return dbGuardWaiting.get();
    }

    public int getCurrentQueueDepth() {
        return Math.max(0, queueDepthSupplier.getAsInt());
    }

    public int getCurrentCleanerBufferDepth() {
        return Math.max(0, cleanerBufferDepthSupplier.getAsInt());
    }

    public int getCurrentInFlightCount() {
        return Math.max(0, inFlightCountSupplier.getAsInt());
    }

    public Registry getRegistry() {
        return registry;
    }

    /**
     * Captures a point-in-time snapshot of all pipeline metrics.
     *
     * @return immutable snapshot
     */
    public Snapshot snapshot() {
        return new Snapshot(
                getEnqueuedCount(),
                getDroppedCount(),
                getDedupSkippedCount(),
                getRpcSuccessCount(),
                getRpcFailureCount(),
                getRpcLatencyTotalNanos(),
                getRpcLatencyMaxNanos(),
                getCleanerBatchesCount(),
                getCleanerDeletedSessionsCount(),
                getCleanerBatchFailuresCount(),
                getCleanerDroppedCount(),
                getCleanerFlushTotalNanos(),
                getCleanerFlushMaxNanos(),
                getDbGuardWaitCount(),
                getDbGuardWaitTotalNanos(),
                getDbGuardWaitMaxNanos(),
                getDbGuardActiveCount(),
                getDbGuardWaitingCount(),
                getCurrentQueueDepth(),
                getCurrentCleanerBufferDepth(),
                getCurrentInFlightCount());
    }

    /**
     * Immutable point-in-time view of {@link PipelineMetrics}.
     */
    public static final class Snapshot {
        private final long enqueued;
        private final long dropped;
        private final long dedupSkipped;
        private final long rpcSuccess;
        private final long rpcFailure;
        private final long rpcLatencyTotalNanos;
        private final long rpcLatencyMaxNanos;
        private final long cleanerBatches;
        private final long cleanerDeletedSessions;
        private final long cleanerBatchFailures;
        private final long cleanerDropped;
        private final long cleanerFlushTotalNanos;
        private final long cleanerFlushMaxNanos;
        private final long dbGuardWaitCount;
        private final long dbGuardWaitTotalNanos;
        private final long dbGuardWaitMaxNanos;
        private final int dbGuardActive;
        private final int dbGuardWaiting;
        private final int currentQueueDepth;
        private final int currentCleanerBufferDepth;
        private final int currentInFlightCount;

        private Snapshot(
                long enqueued,
                long dropped,
                long dedupSkipped,
                long rpcSuccess,
                long rpcFailure,
                long rpcLatencyTotalNanos,
                long rpcLatencyMaxNanos,
                long cleanerBatches,
                long cleanerDeletedSessions,
                long cleanerBatchFailures,
                long cleanerDropped,
                long cleanerFlushTotalNanos,
                long cleanerFlushMaxNanos,
                long dbGuardWaitCount,
                long dbGuardWaitTotalNanos,
                long dbGuardWaitMaxNanos,
                int dbGuardActive,
                int dbGuardWaiting,
                int currentQueueDepth,
                int currentCleanerBufferDepth,
                int currentInFlightCount) {
            this.enqueued = enqueued;
            this.dropped = dropped;
            this.dedupSkipped = dedupSkipped;
            this.rpcSuccess = rpcSuccess;
            this.rpcFailure = rpcFailure;
            this.rpcLatencyTotalNanos = rpcLatencyTotalNanos;
            this.rpcLatencyMaxNanos = rpcLatencyMaxNanos;
            this.cleanerBatches = cleanerBatches;
            this.cleanerDeletedSessions = cleanerDeletedSessions;
            this.cleanerBatchFailures = cleanerBatchFailures;
            this.cleanerDropped = cleanerDropped;
            this.cleanerFlushTotalNanos = cleanerFlushTotalNanos;
            this.cleanerFlushMaxNanos = cleanerFlushMaxNanos;
            this.dbGuardWaitCount = dbGuardWaitCount;
            this.dbGuardWaitTotalNanos = dbGuardWaitTotalNanos;
            this.dbGuardWaitMaxNanos = dbGuardWaitMaxNanos;
            this.dbGuardActive = dbGuardActive;
            this.dbGuardWaiting = dbGuardWaiting;
            this.currentQueueDepth = currentQueueDepth;
            this.currentCleanerBufferDepth = currentCleanerBufferDepth;
            this.currentInFlightCount = currentInFlightCount;
        }

        public long getEnqueued() {
            return enqueued;
        }

        public long getDropped() {
            return dropped;
        }

        public long getDedupSkipped() {
            return dedupSkipped;
        }

        public long getRpcSuccess() {
            return rpcSuccess;
        }

        public long getRpcFailure() {
            return rpcFailure;
        }

        public long getRpcLatencyTotalNanos() {
            return rpcLatencyTotalNanos;
        }

        public long getRpcLatencyMaxNanos() {
            return rpcLatencyMaxNanos;
        }

        public long getCleanerBatches() {
            return cleanerBatches;
        }

        public long getCleanerDeletedSessions() {
            return cleanerDeletedSessions;
        }

        public long getCleanerBatchFailures() {
            return cleanerBatchFailures;
        }

        public long getCleanerDropped() {
            return cleanerDropped;
        }

        public long getCleanerFlushTotalNanos() {
            return cleanerFlushTotalNanos;
        }

        public long getCleanerFlushMaxNanos() {
            return cleanerFlushMaxNanos;
        }

        public long getDbGuardWaitCount() {
            return dbGuardWaitCount;
        }

        public long getDbGuardWaitTotalNanos() {
            return dbGuardWaitTotalNanos;
        }

        public long getDbGuardWaitMaxNanos() {
            return dbGuardWaitMaxNanos;
        }

        public int getDbGuardActive() {
            return dbGuardActive;
        }

        public int getDbGuardWaiting() {
            return dbGuardWaiting;
        }

        public int getCurrentQueueDepth() {
            return currentQueueDepth;
        }

        public int getCurrentCleanerBufferDepth() {
            return currentCleanerBufferDepth;
        }

        public int getCurrentInFlightCount() {
            return currentInFlightCount;
        }

        @Override
        public String toString() {
            return "PipelineMetricsSnapshot{"
                    + "enqueued=" + enqueued
                    + ", dropped=" + dropped
                    + ", dedupSkipped=" + dedupSkipped
                    + ", rpcSuccess=" + rpcSuccess
                    + ", rpcFailure=" + rpcFailure
                    + ", cleanerBatches=" + cleanerBatches
                    + ", cleanerDeletedSessions=" + cleanerDeletedSessions
                    + ", cleanerBatchFailures=" + cleanerBatchFailures
                    + ", cleanerDropped=" + cleanerDropped
                    + ", dbGuardWaitCount=" + dbGuardWaitCount
                    + ", dbGuardActive=" + dbGuardActive
                    + ", dbGuardWaiting=" + dbGuardWaiting
                    + ", currentQueueDepth=" + currentQueueDepth
                    + ", currentCleanerBufferDepth=" + currentCleanerBufferDepth
                    + ", currentInFlightCount=" + currentInFlightCount
                    + '}';
        }
    }
}
