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

import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.registry.compact.CompactRegistry;
import org.apache.seata.server.metrics.MeterIdConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineMetricsTest {

    @AfterEach
    void tearDown() {
        new CompactRegistry().clearUp();
    }

    @Test
    void localCountersTimersAndGaugesAreReflectedInSnapshot() {
        PipelineMetrics metrics = new PipelineMetrics();
        metrics.bindQueueDepthSupplier(() -> 12);
        metrics.bindCleanerBufferDepthSupplier(() -> 7);
        metrics.bindInFlightCountSupplier(() -> 19);

        metrics.recordEnqueued();
        metrics.recordEnqueued();
        metrics.recordDropped();
        metrics.recordDedupSkipped();
        metrics.recordRpcCompleted(TimeUnit.MILLISECONDS.toNanos(4));
        metrics.recordRpcRetryNeeded(TimeUnit.MILLISECONDS.toNanos(3));
        metrics.recordRpcFailure(TimeUnit.MILLISECONDS.toNanos(9));
        metrics.recordRmThrottled();
        metrics.recordCleanerBatch(25, TimeUnit.MILLISECONDS.toNanos(6));
        metrics.recordCleanerDeletedSessions(3);
        metrics.recordCleanerBatchFailure();
        metrics.recordCleanerDropped();
        metrics.recordDbGuardWait(TimeUnit.MILLISECONDS.toNanos(2));
        metrics.incrementDbGuardActive();
        metrics.incrementDbGuardWaiting();

        PipelineMetrics.Snapshot snap = metrics.snapshot();
        assertEquals(2L, snap.getEnqueued());
        assertEquals(1L, snap.getDropped());
        assertEquals(1L, snap.getDedupSkipped());
        assertEquals(1L, snap.getRpcCompleted());
        assertEquals(1L, snap.getRpcRetryNeeded());
        assertEquals(2L, snap.getRpcSuccess());
        assertEquals(1L, snap.getRpcFailure());
        assertEquals(1L, snap.getRmThrottled());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(16), snap.getRpcLatencyTotalNanos());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(9), snap.getRpcLatencyMaxNanos());
        assertEquals(1L, snap.getCleanerBatches());
        assertEquals(28L, snap.getCleanerDeletedSessions());
        assertEquals(1L, snap.getCleanerBatchFailures());
        assertEquals(1L, snap.getCleanerDropped());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(6), snap.getCleanerFlushTotalNanos());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(6), snap.getCleanerFlushMaxNanos());
        assertEquals(1L, snap.getDbGuardWaitCount());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(2), snap.getDbGuardWaitTotalNanos());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(2), snap.getDbGuardWaitMaxNanos());
        assertEquals(1, snap.getDbGuardActive());
        assertEquals(1, snap.getDbGuardWaiting());
        assertEquals(12, snap.getCurrentQueueDepth());
        assertEquals(7, snap.getCurrentCleanerBufferDepth());
        assertEquals(19, snap.getCurrentInFlightCount());
        assertTrue(snap.toString().contains("enqueued=2"));
        assertTrue(snap.toString().contains("rmThrottled=1"));

        metrics.decrementDbGuardActive();
        metrics.decrementDbGuardWaiting();
        metrics.bindQueueDepthSupplier(null);
        assertEquals(0, metrics.getDbGuardActiveCount());
        assertEquals(0, metrics.getDbGuardWaitingCount());
        assertEquals(0, metrics.getCurrentQueueDepth());
    }

    @Test
    void registryBackedMetricsPublishDistinctMeters() {
        CompactRegistry registry = new CompactRegistry();
        registry.clearUp();
        PipelineMetrics metrics = new PipelineMetrics(registry);
        metrics.bindQueueDepthSupplier(() -> 5);
        metrics.bindCleanerBufferDepthSupplier(() -> 3);
        metrics.bindInFlightCountSupplier(() -> 8);

        metrics.recordEnqueued();
        metrics.recordDropped();
        metrics.recordDedupSkipped();
        metrics.recordRpcCompleted(TimeUnit.MILLISECONDS.toNanos(5));
        metrics.recordRpcRetryNeeded(TimeUnit.MILLISECONDS.toNanos(2));
        metrics.recordRpcFailure(TimeUnit.MILLISECONDS.toNanos(10));
        metrics.recordRmThrottled();
        metrics.recordCleanerBatch(10, TimeUnit.MILLISECONDS.toNanos(3));
        metrics.recordCleanerBatchFailure();
        metrics.recordCleanerDropped();
        metrics.recordDbGuardWait(TimeUnit.MILLISECONDS.toNanos(2));

        Map<String, Measurement> measurements = new HashMap<>();
        for (Measurement measurement : registry.measure()) {
            measurements.put(measurement.getId().toString(), measurement);
        }

        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_ENQUEUED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_DROPPED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_DEDUP_SKIPPED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_RPC_COMPLETED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_RPC_RETRY_NEEDED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_RPC_FAILURE.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_RM_THROTTLED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_CLEANER_BATCHES.toString())
                        .getValue(),
                0.001);
        assertEquals(
                10.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_CLEANER_DELETED_SESSIONS.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_CLEANER_BATCH_FAILURES.toString())
                        .getValue(),
                0.001);
        assertEquals(
                1.0,
                measurements
                        .get(MeterIdConstants.COUNTER_PIPELINE_CLEANER_DROPPED.toString())
                        .getValue(),
                0.001);
        assertEquals(
                5.0,
                measurements
                        .get(MeterIdConstants.GAUGE_PIPELINE_QUEUE_DEPTH.toString())
                        .getValue(),
                0.001);
        assertEquals(
                3.0,
                measurements
                        .get(MeterIdConstants.GAUGE_PIPELINE_CLEANER_BUFFER_DEPTH.toString())
                        .getValue(),
                0.001);
        assertEquals(
                8.0,
                measurements
                        .get(MeterIdConstants.GAUGE_PIPELINE_INFLIGHT_COUNT.toString())
                        .getValue(),
                0.001);
        assertNotNull(measurements.get(MeterIdConstants.GAUGE_PIPELINE_DB_GUARD_ACTIVE.toString()));
        assertNotNull(measurements.get(MeterIdConstants.GAUGE_PIPELINE_DB_GUARD_WAITING.toString()));
    }
}
