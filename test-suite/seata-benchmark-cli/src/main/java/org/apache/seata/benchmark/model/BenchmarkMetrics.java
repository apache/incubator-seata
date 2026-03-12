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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark performance metrics
 */
public class BenchmarkMetrics {

    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private final List<Long> latencies =
            Collections.synchronizedList(new ArrayList<>(BenchmarkConstants.MAX_LATENCY_SAMPLES));
    private final AtomicLong totalSamples = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    private volatile long lastCountSnapshot = 0;
    private volatile long lastSnapshotTime = System.currentTimeMillis();

    private volatile LatencyStats cachedStats = null;
    private volatile long lastStatsUpdateTime = 0;

    public void recordSuccess(long latencyMs) {
        totalCount.incrementAndGet();
        successCount.incrementAndGet();
        addLatencySample(latencyMs);
    }

    public void recordFailure(long latencyMs) {
        totalCount.incrementAndGet();
        failedCount.incrementAndGet();
        addLatencySample(latencyMs);
    }

    private void addLatencySample(long latencyMs) {
        totalSamples.incrementAndGet();
        synchronized (latencies) {
            if (latencies.size() < BenchmarkConstants.MAX_LATENCY_SAMPLES) {
                latencies.add(latencyMs);
            } else {
                // Random replacement strategy to maintain bounded samples
                int index = ThreadLocalRandom.current().nextInt(BenchmarkConstants.MAX_LATENCY_SAMPLES);
                latencies.set(index, latencyMs);
            }
        }
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    public double getSuccessRate() {
        long total = totalCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount.get() / total * 100;
    }

    public double getAverageTps() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed == 0) {
            return 0.0;
        }
        return (double) totalCount.get() / elapsed * 1000;
    }

    public synchronized double getCurrentTps() {
        long currentTime = System.currentTimeMillis();
        long currentCount = totalCount.get();
        long elapsed = currentTime - lastSnapshotTime;

        if (elapsed == 0) {
            return 0.0;
        }

        double tps = (double) (currentCount - lastCountSnapshot) / elapsed * 1000;
        lastSnapshotTime = currentTime;
        lastCountSnapshot = currentCount;
        return tps;
    }

    public long getElapsedTimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public LatencyStats getLatencyStats() {
        long now = System.currentTimeMillis();
        if (cachedStats != null && (now - lastStatsUpdateTime) < BenchmarkConstants.LATENCY_STATS_CACHE_MS) {
            return cachedStats;
        }

        synchronized (latencies) {
            // Double-check
            if (cachedStats != null && (now - lastStatsUpdateTime) < BenchmarkConstants.LATENCY_STATS_CACHE_MS) {
                return cachedStats;
            }

            if (latencies.isEmpty()) {
                cachedStats = new LatencyStats(0, 0, 0, 0, 0);
            } else {
                List<Long> sortedLatencies = new ArrayList<>(latencies);
                Collections.sort(sortedLatencies);

                int size = sortedLatencies.size();
                long p50 = sortedLatencies.get(Math.max(0, (int) Math.ceil(size * 0.5) - 1));
                long p95 = sortedLatencies.get(Math.max(0, (int) Math.ceil(size * 0.95) - 1));
                long p99 = sortedLatencies.get(Math.max(0, (int) Math.ceil(size * 0.99) - 1));
                long p999 = sortedLatencies.get(Math.max(0, (int) Math.ceil(size * 0.999) - 1));
                long max = sortedLatencies.get(size - 1);

                cachedStats = new LatencyStats(p50, p95, p99, p999, max);
            }

            lastStatsUpdateTime = now;
            return cachedStats;
        }
    }

    public void reset() {
        totalCount.set(0);
        successCount.set(0);
        failedCount.set(0);
        latencies.clear();
        lastCountSnapshot = 0;
        lastSnapshotTime = System.currentTimeMillis();
    }

    public static class LatencyStats {
        private final long p50;
        private final long p95;
        private final long p99;
        private final long p999;
        private final long max;

        public LatencyStats(long p50, long p95, long p99, long p999, long max) {
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.p999 = p999;
            this.max = max;
        }

        public long getP50() {
            return p50;
        }

        public long getP95() {
            return p95;
        }

        public long getP99() {
            return p99;
        }

        public long getP999() {
            return p999;
        }

        public long getMax() {
            return max;
        }
    }
}
