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
package org.apache.seata.benchmark.model;

import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BenchmarkMetricsTest {

    @Test
    void ignoresSamplesOutsideTheReservoirButTracksTheirMax() {
        List<Long> bounds = new ArrayList<>();
        BenchmarkMetrics metrics = new BenchmarkMetrics(2, bound -> {
            bounds.add(bound);
            return bound - 1;
        });
        metrics.recordSuccess(10);
        metrics.recordFailure(20);
        assertEquals(20, metrics.getLatencyStats().getMax());

        metrics.recordSuccess(80);
        metrics.recordSuccess(50);
        assertEquals(Arrays.asList(3L, 4L), bounds);
        assertEquals(10, metrics.getLatencyStats().getP50());
        assertEquals(20, metrics.getLatencyStats().getP999());
        assertEquals(80, metrics.getLatencyStats().getMax());
    }

    @Test
    void replacesAnAdmittedSlotWithoutLosingTheObservedMax() {
        BenchmarkMetrics metrics = new BenchmarkMetrics(2, bound -> 0);
        metrics.recordSuccess(99);
        metrics.recordSuccess(10);
        assertEquals(99, metrics.getLatencyStats().getP999());

        metrics.recordSuccess(20);
        assertEquals(10, metrics.getLatencyStats().getP50());
        assertEquals(20, metrics.getLatencyStats().getP999());
        assertEquals(99, metrics.getLatencyStats().getMax());
    }

    @Test
    void resetClearsTheSampleOrdinalAndPriorMaximum() {
        List<Long> bounds = new ArrayList<>();
        BenchmarkMetrics metrics = new BenchmarkMetrics(1, bound -> {
            bounds.add(bound);
            return bound - 1;
        });
        metrics.recordSuccess(90);
        metrics.recordSuccess(95);
        assertEquals(95, metrics.getLatencyStats().getMax());

        metrics.reset();
        assertEquals(0, metrics.getLatencyStats().getMax());
        metrics.recordSuccess(7);
        metrics.recordSuccess(10);
        assertEquals(Arrays.asList(2L, 2L), bounds);
        assertEquals(7, metrics.getLatencyStats().getP999());
        assertEquals(10, metrics.getLatencyStats().getMax());
    }

    @Test
    void refreshesLatencyStatsWhenTransactionsAreRecorded() {
        BenchmarkMetrics metrics = new BenchmarkMetrics();
        assertEquals(0, metrics.getLatencyStats().getP95());
        assertEquals(0, metrics.getLatencyStats().getP99());

        metrics.recordTransaction(BenchmarkConstants.STATUS_COMMITTED, 10);
        assertEquals(10, metrics.getLatencyStats().getP95());
        assertEquals(10, metrics.getLatencyStats().getP99());

        metrics.recordTransaction(BenchmarkConstants.STATUS_FAILED, 20);
        assertEquals(20, metrics.getLatencyStats().getP99());

        metrics.recordTransaction(BenchmarkConstants.STATUS_COMPENSATED, 30);
        assertEquals(30, metrics.getLatencyStats().getP95());
        assertEquals(30, metrics.getLatencyStats().getMax());
        assertEquals(3, metrics.getTotalCount());
        assertEquals(2, metrics.getSuccessCount());
        assertEquals(1, metrics.getFailedCount());
        assertEquals(1, metrics.getCompensatedCount());
    }

    @Test
    void assignsEachConcurrentSampleADistinctOrdinal() throws Exception {
        int workers = 4;
        int perWorker = 500;
        List<Long> bounds = new ArrayList<>();
        BenchmarkMetrics metrics = new BenchmarkMetrics(1, bound -> {
            bounds.add(bound);
            return bound - 1;
        });
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                int base = worker * perWorker;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 1; i <= perWorker; i++) {
                        metrics.recordSuccess(base + i);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
            assertEquals(workers * perWorker, metrics.getTotalCount());
            assertEquals(workers * perWorker - 1, bounds.size());
            for (int i = 0; i < bounds.size(); i++) {
                assertEquals(i + 2L, bounds.get(i).longValue());
            }
            assertEquals(workers * perWorker, metrics.getLatencyStats().getMax());
        } finally {
            executor.shutdownNow();
        }
    }
}
