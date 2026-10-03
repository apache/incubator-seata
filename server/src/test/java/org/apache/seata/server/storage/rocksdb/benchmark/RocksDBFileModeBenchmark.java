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
package org.apache.seata.server.storage.rocksdb.benchmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.session.SessionScanStats;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreDiagnostics;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBWalSyncMode;
import org.apache.seata.server.storage.rocksdb.RocksDBWalSyncStats;
import org.apache.seata.server.storage.rocksdb.lock.RocksDBLockManager;
import org.apache.seata.server.storage.rocksdb.lock.RocksDBOrphanLockCleanupController;
import org.apache.seata.server.storage.rocksdb.session.RocksDBSessionManager;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.rocksdb.RocksDB;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

/**
 * Manual benchmark for the RocksDB file mode store.
 *
 * <p>Run from an IDE or with a test classpath. This class is intentionally not named *Test, so it does not run in the
 * regular unit test suite.
 */
public final class RocksDBFileModeBenchmark {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CSV_HEADER =
            "scenario,globalCount,branchPerGlobal,lockPerBranch,writeWorkload,dataPayloadProfile,queryWorkload,syncWrite,enableRangeDelete,warmupRounds,"
                    + "measureRounds,batchSize,lockIndexScanBatchSize,orphanCleanBatchLimit,orphanCleanMaxBatches,orphanCleanRoundSleepMillis,queryIterationsPerRound,queryLimit,repeatRun,"
                    + "compareOrder,ops,totalMs,"
                    + "opsPerSecond,p50Ms,p95Ms,p99Ms,dbSizeBytes,fileCount,sstFiles,walFiles,"
                    + "estimateLiveDataSizeBytes,totalSstFilesSizeBytes,pendingCompactionBytes,"
                    + "globalEstimateKeys,branchEstimateKeys,lockEstimateKeys,rowsScanned,rowsReturned,rowsUpdated,"
                    + "pointReads,iteratorNext,writeBatchBytes,innerOperations,rangeDeleteCount,pointDeleteCount,"
                    + "deleteBatchCount,branchFanout,lockFanout,rocksdbConfigDigest,"
                    + "walSyncMode,walSyncIntervalMillis,walSyncWriteThreshold,walSyncCount,walSyncFailureCount,"
                    + "walSyncAvgMs,walSyncMaxMs,walUnsyncedWrites,walMaxUnsyncedWrites,walUnsyncedMs,"
                    + "walMaxUnsyncedMs,walLatestSequenceNumber,walLastSyncedSequenceNumber";
    private static final String SUMMARY_CSV_HEADER =
            "scenario,runGroup,runCount,opsPerSecondMean,opsPerSecondMedian,opsPerSecondP95,opsPerSecondP99,"
                    + "opsPerSecondMin,opsPerSecondMax,opsPerSecondStddev,totalMsMean,p50MsMedian,p95MsMedian,"
                    + "p99MsMedian,rowsScannedMean,rowsReturnedMean,rowsUpdatedMean,pointReadsMean,iteratorNextMean,"
                    + "writeBatchBytesMean,innerOperationsMean,rangeDeleteCountMean,pointDeleteCountMean,"
                    + "deleteBatchCountMean,branchFanoutMean,lockFanoutMean";
    private static final List<String> ALL_BENCHMARKS =
            Arrays.asList("write", "query", "lock", "cleanup", "lifecycle", "restart");
    private static final List<String> SUPPORTED_BENCHMARKS =
            Arrays.asList("write", "query", "lock", "cleanup", "lifecycle", "background", "restart");
    private static final GlobalStatus[] STATUSES = {
        GlobalStatus.Begin,
        GlobalStatus.Committing,
        GlobalStatus.RollbackRetrying,
        GlobalStatus.AsyncCommitting,
        GlobalStatus.Committed
    };
    private static final GlobalStatus TARGET_STATUS = GlobalStatus.RollbackRetrying;
    private static final GlobalStatus SECONDARY_STATUS = GlobalStatus.TimeoutRollbacking;
    private static final int SESSION_TIMEOUT_MILLIS = 60000;
    private static final long ACTIVE_BENCHMARK_WINDOW_MULTIPLIER = 10L;
    private static final String LOCK_OP_ACQUIRE = "acquire";
    private static final String LOCK_OP_CONFLICT = "conflict";
    private static final String LOCK_OP_UPDATE_STATUS = "update_status";
    private static final String LOCK_OP_RELEASE_BRANCH = "release_branch";
    private static final String LOCK_OP_RELEASE_GLOBAL = "release_global";
    private static final String LOCK_OP_CLEAN_ORPHAN = "clean_orphan";
    private static final String LOCK_OP_CLEAN_ORPHAN_BATCHED = "clean_orphan_batched";
    private static final int CLEAN_ORPHAN_BATCHED_BATCH_LIMIT = 1000;
    private static final int CLEAN_ORPHAN_BATCHED_MAX_BATCHES = 10;
    private static final long CLEAN_ORPHAN_BATCHED_ROUND_SLEEP_MILLIS =
            RocksDBOrphanLockCleanupController.DEFAULT_ORPHAN_LOCK_CLEAN_ROUND_SLEEP_MILLIS;
    private static final int CLEAN_ORPHAN_FOREGROUND_PROBE_BRANCHES = 100;
    private static final int BACKGROUND_QUERY_DEFAULT_LIMIT = 1024;
    private static final long BACKGROUND_FOREGROUND_PROBE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);
    private static final String WRITE_WORKLOAD_FULL = "full";
    private static final String WRITE_WORKLOAD_APPEND = "append";
    private static final String DATA_PAYLOAD_PROFILE_PRODUCTION = "production";
    private static final String DATA_PAYLOAD_PROFILE_PHASE4 = "phase4";
    private static final String QUERY_WORKLOAD_ALL = "all";
    private static final String QUERY_WORKLOAD_NONE = "none";
    private static final List<String> ALL_LOCK_WORKLOADS = Arrays.asList(
            LOCK_OP_ACQUIRE,
            LOCK_OP_CONFLICT,
            LOCK_OP_UPDATE_STATUS,
            LOCK_OP_RELEASE_BRANCH,
            LOCK_OP_RELEASE_GLOBAL,
            LOCK_OP_CLEAN_ORPHAN);
    // Extra lock workload operations that are accepted explicitly but are not part of the "full" alias.
    private static final List<String> EXTRA_LOCK_WORKLOADS = Arrays.asList(LOCK_OP_CLEAN_ORPHAN_BATCHED);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile int sinkCount;
    private static volatile String sinkXid;

    private RocksDBFileModeBenchmark() {}

    // ---- Logging helpers ----

    private static String ts() {
        return LocalTime.now().format(TIME_FMT);
    }

    private static void log(String format, Object... args) {
        System.out.printf(Locale.ROOT, "[%s] %s%n", ts(), String.format(Locale.ROOT, format, args));
    }

    private static void logScenarioStart(String scenario, BenchmarkOptions options) {
        log(
                "=== START %s (globalCount=%d, branchPerGlobal=%d, lockPerBranch=%d, syncWrite=%s, rangeDelete=%s, blockCache=%s) ===",
                scenario,
                options.globalCount,
                options.branchPerGlobal,
                options.lockPerBranch,
                options.syncWrite,
                options.enableRangeDelete,
                BenchmarkOptions.humanReadableSize(options.blockCacheSize));
    }

    private static void logRoundStart(String scenario, int round, int totalRounds, boolean warmup) {
        log("  [%s] round %d/%d%s", scenario, round + 1, totalRounds, warmup ? " (warmup)" : "");
    }

    private static void logScenarioEnd(
            String scenario, long elapsedNanos, SystemMetrics metricsBefore, SystemMetrics metricsAfter) {
        double elapsedSec = elapsedNanos / 1_000_000_000.0;
        log("=== END   %s (%.2fs) ===", scenario, elapsedSec);
        if (metricsBefore != null && metricsAfter != null) {
            long heapDelta = metricsAfter.heapUsed - metricsBefore.heapUsed;
            long gcCountDelta = metricsAfter.gcCount - metricsBefore.gcCount;
            long gcTimeDelta = metricsAfter.gcTimeMs - metricsBefore.gcTimeMs;
            log(
                    "  heap: %.1fMB -> %.1fMB (delta %+dMB), gc: +%d collections / +%dms",
                    metricsBefore.heapUsed / 1048576.0,
                    metricsAfter.heapUsed / 1048576.0,
                    heapDelta / 1048576,
                    gcCountDelta,
                    gcTimeDelta);
        }
    }

    private static void logEmit(String scenario, OperationStats stats) {
        log(
                "  >> %s: ops=%d, totalMs=%.1f, ops/s=%.1f, p50=%.3fms, p95=%.3fms, p99=%.3fms",
                scenario,
                stats.ops(),
                stats.totalNanos() / 1_000_000.0,
                stats.opsPerSecond(),
                stats.percentile(50) / 1_000_000.0,
                stats.percentile(95) / 1_000_000.0,
                stats.percentile(99) / 1_000_000.0);
    }

    private static void logBlockCacheStats(RocksDBStoreEngine engine) {
        try {
            long usage = engine.getBlockCacheUsage();
            long pinned = engine.getBlockCachePinnedUsage();
            long capacity = engine.getBlockCacheCapacity();
            if (capacity > 0) {
                double usagePercent = capacity > 0 ? (usage * 100.0 / capacity) : 0;
                log(
                        "  block cache: usage=%s, pinned=%s, capacity=%s (%.1f%% utilized)",
                        BenchmarkOptions.humanReadableSize(usage),
                        BenchmarkOptions.humanReadableSize(pinned),
                        BenchmarkOptions.humanReadableSize(capacity),
                        usagePercent);
            } else {
                log("  block cache: disabled (no LRUCache configured)");
            }
        } catch (Exception e) {
            log("  block cache: stats unavailable (%s)", e.getMessage());
        }
    }

    // ---- System metrics ----

    private static final class SystemMetrics {
        final long heapUsed;
        final long heapMax;
        final long nonHeapUsed;
        final long gcCount;
        final long gcTimeMs;

        private SystemMetrics(long heapUsed, long heapMax, long nonHeapUsed, long gcCount, long gcTimeMs) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.gcCount = gcCount;
            this.gcTimeMs = gcTimeMs;
        }

        static SystemMetrics snapshot() {
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = mem.getHeapMemoryUsage();
            MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();
            long gcCount = 0;
            long gcTime = 0;
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                long count = gc.getCollectionCount();
                if (count > 0) {
                    gcCount += count;
                }
                long time = gc.getCollectionTime();
                if (time > 0) {
                    gcTime += time;
                }
            }
            return new SystemMetrics(heap.getUsed(), heap.getMax(), nonHeap.getUsed(), gcCount, gcTime);
        }
    }

    // ---- A/B comparison ----

    private static void emitComparison(
            List<String> csvLinesA, List<String> csvLinesB, BenchmarkOptions optionsA, BenchmarkOptions optionsB) {
        System.out.println();
        log("=== A/B COMPARISON: %s ===", optionsA.compare);
        System.out.printf(Locale.ROOT, "%-40s %15s %15s %10s%n", "scenario", "A (ops/s)", "B (ops/s)", "delta%");
        StringBuilder sep = new StringBuilder();
        for (int i = 0; i < 82; i++) {
            sep.append('-');
        }
        System.out.println(sep.toString());

        Map<String, Double> mapA = parseOpsPerSecond(csvLinesA);
        Map<String, Double> mapB = parseOpsPerSecond(csvLinesB);

        Set<String> allScenarios = new LinkedHashSet<>();
        allScenarios.addAll(mapA.keySet());
        allScenarios.addAll(mapB.keySet());

        for (String scenario : allScenarios) {
            Double a = mapA.get(scenario);
            Double b = mapB.get(scenario);
            if (a == null || b == null) {
                continue;
            }
            double delta = a > 0 ? (b - a) / a * 100.0 : 0;
            String marker = delta > 5 ? " [B better]" : delta < -5 ? " [A better]" : "";
            System.out.printf(Locale.ROOT, "%-40s %15.1f %15.1f %+9.1f%%%s%n", scenario, a, b, delta, marker);
        }

        System.out.println();
        System.out.printf(Locale.ROOT, "A: %s%n", describeCompareOption(optionsA));
        System.out.printf(Locale.ROOT, "B: %s%n", describeCompareOption(optionsB));
    }

    private static void emitSummary(List<String> csvLines) {
        List<BenchmarkSummary> summaries = collectSummaries(csvLines);
        if (summaries.isEmpty()) {
            return;
        }
        System.out.println();
        log("=== REPEAT SUMMARY ===");
        System.out.println(SUMMARY_CSV_HEADER);
        for (BenchmarkSummary summary : summaries) {
            System.out.println(summary.toCsvLine());
        }
        log("=== REPEAT SUMMARY JSON ===");
        System.out.println(summariesAsJson(summaries));
    }

    private static List<String> summarizeCsvLines(List<String> csvLines) {
        List<BenchmarkSummary> summaries = collectSummaries(csvLines);
        List<String> result = new ArrayList<>(summaries.size());
        for (BenchmarkSummary summary : summaries) {
            result.add(summary.toCsvLine());
        }
        return result;
    }

    private static String summarizeCsvLinesAsJson(List<String> csvLines) {
        return summariesAsJson(collectSummaries(csvLines));
    }

    private static String summariesAsJson(List<BenchmarkSummary> summaries) {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>(summaries.size());
        List<String> summaryKeys = new ArrayList<>(summaries.size());
        for (BenchmarkSummary summary : summaries) {
            items.add(summary.toMap());
            summaryKeys.add(summary.scenario + ":" + summary.runGroup);
        }
        root.put("summaryKeys", summaryKeys);
        root.put("summaries", items);
        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize benchmark summary JSON", e);
        }
    }

    private static List<BenchmarkSummary> collectSummaries(List<String> csvLines) {
        Map<String, BenchmarkSummary> summaries = new LinkedHashMap<>();
        int scenarioIndex = csvColumnIndex("scenario");
        int repeatRunIndex = csvColumnIndex("repeatRun");
        int opsPerSecondIndex = csvColumnIndex("opsPerSecond");
        int totalMsIndex = csvColumnIndex("totalMs");
        int p50MsIndex = csvColumnIndex("p50Ms");
        int p95MsIndex = csvColumnIndex("p95Ms");
        int p99MsIndex = csvColumnIndex("p99Ms");
        int rowsScannedIndex = csvColumnIndex("rowsScanned");
        int rowsReturnedIndex = csvColumnIndex("rowsReturned");
        int rowsUpdatedIndex = csvColumnIndex("rowsUpdated");
        int pointReadsIndex = csvColumnIndex("pointReads");
        int iteratorNextIndex = csvColumnIndex("iteratorNext");
        int writeBatchBytesIndex = csvColumnIndex("writeBatchBytes");
        int innerOperationsIndex = csvColumnIndex("innerOperations");
        int rangeDeleteCountIndex = csvColumnIndex("rangeDeleteCount");
        int pointDeleteCountIndex = csvColumnIndex("pointDeleteCount");
        int deleteBatchCountIndex = csvColumnIndex("deleteBatchCount");
        int branchFanoutIndex = csvColumnIndex("branchFanout");
        int lockFanoutIndex = csvColumnIndex("lockFanout");
        int minColumns = max(
                        scenarioIndex,
                        repeatRunIndex,
                        opsPerSecondIndex,
                        totalMsIndex,
                        p50MsIndex,
                        p95MsIndex,
                        p99MsIndex,
                        rowsScannedIndex,
                        rowsReturnedIndex,
                        rowsUpdatedIndex,
                        pointReadsIndex,
                        iteratorNextIndex,
                        writeBatchBytesIndex,
                        innerOperationsIndex,
                        rangeDeleteCountIndex,
                        pointDeleteCountIndex,
                        deleteBatchCountIndex,
                        branchFanoutIndex,
                        lockFanoutIndex)
                + 1;
        for (String line : csvLines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < minColumns || "scenario".equals(parts[scenarioIndex])) {
                continue;
            }
            String scenario = parts[scenarioIndex];
            String runGroup = runGroup(parts[repeatRunIndex]);
            String key = scenario + '\u0000' + runGroup;
            BenchmarkSummary summary =
                    summaries.computeIfAbsent(key, ignored -> new BenchmarkSummary(scenario, runGroup));
            summary.add(
                    doubleValue(parts[opsPerSecondIndex]),
                    doubleValue(parts[totalMsIndex]),
                    doubleValue(parts[p50MsIndex]),
                    doubleValue(parts[p95MsIndex]),
                    doubleValue(parts[p99MsIndex]),
                    doubleValue(parts[rowsScannedIndex]),
                    doubleValue(parts[rowsReturnedIndex]),
                    doubleValue(parts[rowsUpdatedIndex]),
                    doubleValue(parts[pointReadsIndex]),
                    doubleValue(parts[iteratorNextIndex]),
                    doubleValue(parts[writeBatchBytesIndex]),
                    doubleValue(parts[innerOperationsIndex]),
                    doubleValue(parts[rangeDeleteCountIndex]),
                    doubleValue(parts[pointDeleteCountIndex]),
                    doubleValue(parts[deleteBatchCountIndex]),
                    doubleValue(parts[branchFanoutIndex]),
                    doubleValue(parts[lockFanoutIndex]));
        }
        return new ArrayList<>(summaries.values());
    }

    private static Map<String, Double> parseOpsPerSecond(List<String> csvLines) {
        Map<String, NumericSeries> values = new LinkedHashMap<>();
        int scenarioIndex = csvColumnIndex("scenario");
        int opsPerSecondIndex = csvColumnIndex("opsPerSecond");
        int minColumns = Math.max(scenarioIndex, opsPerSecondIndex) + 1;
        for (String line : csvLines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < minColumns || "scenario".equals(parts[scenarioIndex])) {
                continue;
            }
            try {
                values.computeIfAbsent(parts[scenarioIndex], ignored -> new NumericSeries())
                        .add(Double.parseDouble(parts[opsPerSecondIndex]));
            } catch (NumberFormatException ignored) {
            }
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, NumericSeries> entry : values.entrySet()) {
            result.put(entry.getKey(), entry.getValue().mean());
        }
        return result;
    }

    private static int csvColumnIndex(String column) {
        String[] columns = CSV_HEADER.split(",", -1);
        for (int i = 0; i < columns.length; i++) {
            if (column.equals(columns[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("CSV column not found:" + column);
    }

    private static String describeCompareOption(BenchmarkOptions options) {
        if ("syncWrite".equals(options.compare)) {
            return "syncWrite=" + options.syncWrite;
        }
        if ("enableRangeDelete".equals(options.compare)) {
            return "enableRangeDelete=" + options.enableRangeDelete;
        }
        if ("blockCacheSize".equals(options.compare)) {
            return "blockCacheSize=" + BenchmarkOptions.humanReadableSize(options.blockCacheSize);
        }
        if ("tuningProfile".equals(options.compare)) {
            return "tuningProfile=" + options.tuningProfile + " (" + options.tuningSummary() + ")";
        }
        if ("walSyncMode".equals(options.compare)) {
            return "walSyncMode=" + options.walSyncMode.configValue()
                    + ", intervalMillis=" + options.walSyncIntervalMillis
                    + ", writeThreshold=" + options.walSyncWriteThreshold;
        }
        return options.compare + " (custom)";
    }

    // ---- Main entry ----

    public static void main(String[] args) throws Exception {
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Map<String, String> arguments = BenchmarkOptions.parseArgs(args);
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        "seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE,
                        arguments.getOrDefault("multiStatusScanPageSize", "256"));
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        ConfigurationCache.clear();
        try {
            new RocksDBFileModeBenchmark().run(BenchmarkOptions.parse(args));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
        }
    }

    private void run(BenchmarkOptions options) throws Exception {
        if (options.compare != null) {
            runWithComparison(options);
            return;
        }
        if (options.repeatRuns == 1) {
            runOnce(options, null);
            return;
        }
        List<String> allCsv = new ArrayList<>();
        for (int repeatRun = 1; repeatRun <= options.repeatRuns; repeatRun++) {
            String runLabel = "R" + repeatRun;
            allCsv.addAll(runOnce(options.withRunLabel(runLabel), runLabel));
        }
        emitSummary(allCsv);
    }

    private void runWithComparison(BenchmarkOptions baseOptions) throws Exception {
        BenchmarkOptions optionsA = baseOptions.comparisonBaseOptions();
        BenchmarkOptions optionsB = baseOptions.flipCompareOption();

        log("=== A/B comparison mode: %s ===", baseOptions.compare);
        log("A: %s", describeCompareOption(optionsA));
        log("B: %s", describeCompareOption(optionsB));
        System.out.println();

        List<String> csvA = new ArrayList<>();
        List<String> csvB = new ArrayList<>();
        List<String> allCsv = new ArrayList<>();
        for (String runLabel : baseOptions.comparisonRunLabels()) {
            BenchmarkOptions runOptions = runLabel.startsWith("A") ? optionsA : optionsB;
            List<String> csv = runOnce(runOptions.withRunLabel(runLabel), runLabel);
            allCsv.addAll(csv);
            if (runLabel.startsWith("A")) {
                csvA.addAll(csv);
            }
            if (runLabel.startsWith("B")) {
                csvB.addAll(csv);
            }
            System.out.println();
        }

        emitSummary(allCsv);
        emitComparison(csvA, csvB, optionsA, optionsB);
    }

    private List<String> runOnce(BenchmarkOptions options, String runLabel) throws Exception {
        Path rootPath = options.rootPath();
        Files.createDirectories(rootPath);
        String suffix = runLabel != null ? "-" + runLabel : "";
        Path runPath = rootPath.resolve("rocksdb-file-mode-" + System.currentTimeMillis() + suffix);
        Files.createDirectories(runPath);

        printEnvironment(options, rootPath, runPath, runLabel);
        System.out.println(CSV_HEADER);
        List<String> csvLines = new ArrayList<>();

        long runStartedAt = System.nanoTime();
        try {
            if (options.isEnabled("write")) {
                runWriteBenchmark(runPath, options, csvLines);
            }
            if (options.isEnabled("query")) {
                runQueryBenchmark(runPath, options, csvLines);
            }
            if (options.isEnabled("lock")) {
                runLockBenchmark(runPath, options, csvLines);
            }
            if (options.isEnabled("cleanup")) {
                runCleanupBenchmark(runPath, options, csvLines);
            }
            if (options.isEnabled("lifecycle")) {
                runLifecycleBenchmark(runPath, options, csvLines);
            }
            if (options.isEnabled("background")) {
                runBackgroundInterferenceBenchmark(runPath, options, csvLines);
            }
            if (options.isEnabled("restart")) {
                runRestartBenchmark(runPath, options, csvLines);
            }
        } finally {
            if (options.cleanup) {
                deleteRecursively(runPath);
            }
        }
        double totalSec = (System.nanoTime() - runStartedAt) / 1_000_000_000.0;
        log("=== ALL DONE (%.1fs, %d scenarios emitted) ===", totalSec, csvLines.size());
        System.out.println("sinkCount=" + sinkCount);
        System.out.println("sinkXid=" + sinkXid);
        return csvLines;
    }

    private void runWriteBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines) throws Exception {
        String scenario = "write";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();

        OperationStats globalAddStats = new OperationStats(options.sampleEvery);
        OperationStats globalUpdateStats = new OperationStats(options.sampleEvery);
        OperationStats globalRemoveStats = new OperationStats(options.sampleEvery);
        OperationStats branchAddStats = new OperationStats(options.sampleEvery);
        OperationStats branchUpdateStats = new OperationStats(options.sampleEvery);
        OperationStats branchRemoveStats = new OperationStats(options.sampleEvery);
        Path lastDbPath = null;
        RocksDBWalSyncStats lastWalSyncStats = RocksDBWalSyncStats.NONE;

        for (int round = 0; round < options.totalRounds(); round++) {
            boolean warmup = round < options.warmupRounds;
            logRoundStart(scenario, round, options.totalRounds(), warmup);
            BenchmarkDataSet dataSet = BenchmarkDataSet.create(options, round);
            Path dbPath = scenarioPath(runPath, "write-round-" + round);
            lastDbPath = dbPath;
            try (RocksDBStoreEngine engine = open(dbPath, options)) {
                RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    RowMetrics globalAddMetrics = RowMetrics.written(3, estimateGlobalPutBytes(globalSession));
                    measure(
                            globalAddStats,
                            round,
                            options,
                            globalAddMetrics,
                            () -> storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession));
                    for (BranchSession branchSession : dataSet.branchesOf(globalSession)) {
                        RowMetrics branchAddMetrics = RowMetrics.written(1, estimateBranchPutBytes(branchSession));
                        measure(
                                branchAddStats,
                                round,
                                options,
                                branchAddMetrics,
                                () -> storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession));
                    }
                }
                if (options.isFullWriteWorkload()) {
                    for (GlobalSession globalSession : dataSet.globalSessions) {
                        globalSession.setStatus(nextStatus(globalSession.getStatus()));
                        RowMetrics globalUpdateMetrics =
                                RowMetrics.written(5, estimateGlobalUpdateBytes(globalSession));
                        measure(
                                globalUpdateStats,
                                round,
                                options,
                                globalUpdateMetrics,
                                () -> storeManager.writeSession(LogOperation.GLOBAL_UPDATE, globalSession));
                        for (BranchSession branchSession : dataSet.branchesOf(globalSession)) {
                            branchSession.setStatus(BranchStatus.PhaseOne_Done);
                            RowMetrics branchUpdateMetrics =
                                    RowMetrics.written(1, estimateBranchPutBytes(branchSession));
                            measure(
                                    branchUpdateStats,
                                    round,
                                    options,
                                    branchUpdateMetrics,
                                    () -> storeManager.writeSession(LogOperation.BRANCH_UPDATE, branchSession));
                        }
                    }
                    if (dataSet.totalBranchCount() > 0) {
                        for (GlobalSession globalSession : dataSet.globalSessions) {
                            List<BranchSession> branches = dataSet.branchesOf(globalSession);
                            if (branches.isEmpty()) {
                                continue;
                            }
                            BranchSession branchSession = branches.get(0);
                            RowMetrics branchRemoveMetrics =
                                    RowMetrics.written(1, estimateBranchDeleteBytes(branchSession));
                            measure(
                                    branchRemoveStats,
                                    round,
                                    options,
                                    branchRemoveMetrics,
                                    () -> storeManager.writeSession(LogOperation.BRANCH_REMOVE, branchSession));
                        }
                    }
                    for (GlobalSession globalSession : dataSet.globalSessions) {
                        int branchCount = dataSet.branchesOf(globalSession).size();
                        int remainingBranchCount = branchCount == 0 ? 0 : branchCount - 1;
                        RowMetrics globalRemoveMetrics =
                                globalRemoveMetrics(options, globalSession, remainingBranchCount);
                        measure(
                                globalRemoveStats,
                                round,
                                options,
                                globalRemoveMetrics,
                                () -> storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession));
                    }
                    verifyStoreEmpty(engine);
                }
                engine.flush();
                lastWalSyncStats = engine.diagnostics().getWalSyncStats();
            }
        }

        DbFootprint footprint = DbFootprint.from(lastDbPath, lastWalSyncStats);
        emit("write.global_add", options, globalAddStats, footprint, csvLines);
        emit("write.branch_add", options, branchAddStats, footprint, csvLines);
        if (options.isFullWriteWorkload()) {
            emit("write.global_update", options, globalUpdateStats, footprint, csvLines);
            emit("write.global_remove", options, globalRemoveStats, footprint, csvLines);
            emit("write.branch_update", options, branchUpdateStats, footprint, csvLines);
            emit("write.branch_remove", options, branchRemoveStats, footprint, csvLines);
        }
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private void runQueryBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines) throws Exception {
        String scenario = "query";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();

        BenchmarkDataSet dataSet = BenchmarkDataSet.create(options, 0);
        Path dbPath = scenarioPath(runPath, "query");
        OperationStats xidStats = new OperationStats(options.sampleEvery);
        OperationStats transactionIdStats = new OperationStats(options.sampleEvery);
        OperationStats statusStats = new OperationStats(options.sampleEvery);
        OperationStats multiStatusStats = new OperationStats(options.sampleEvery);
        OperationStats beginSortedStats = new OperationStats(options.sampleEvery);
        OperationStats fullScanStats = new OperationStats(options.sampleEvery);
        RocksDBWalSyncStats walSyncStats = RocksDBWalSyncStats.NONE;

        try (RocksDBStoreEngine engine = open(dbPath, options)) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            writeDataSet(storeManager, dataSet);
            engine.flush();
            log("  query data loaded: %d globals, %d branches", options.globalCount, dataSet.totalBranchCount());

            int iterations = options.totalRounds() * options.queryIterationsPerRound;
            int expectedStatusCount = dataSet.countByStatus(TARGET_STATUS);
            int expectedMultiStatusCount = expectedStatusCount + dataSet.countByStatus(SECONDARY_STATUS);
            boolean useOvertimeStatusQuery = options.expiredRatio > 0D;
            int expectedStatusQueryCount = useOvertimeStatusQuery
                    ? dataSet.countByStatusAndOverTime(TARGET_STATUS, SESSION_TIMEOUT_MILLIS)
                    : expectedStatusCount;
            int expectedLimitedStatusCount = options.queryLimit > 0
                    ? Math.min(expectedStatusQueryCount, options.queryLimit)
                    : expectedStatusQueryCount;
            // Superset bound for the overtime query: over-time eligibility is evaluated against
            // wall-clock time at query execution; minutes may pass between dataset creation and
            // the query loop, so every session (not just the back-dated ones) can become eligible.
            int statusQueryMaxCount =
                    options.queryLimit > 0 ? Math.min(expectedStatusCount, options.queryLimit) : expectedStatusCount;
            int multiStatusQueryMaxCount = options.queryLimit > 0
                    ? Math.min(expectedMultiStatusCount, options.queryLimit)
                    : expectedMultiStatusCount;
            int expectedBeginCount = dataSet.countByStatus(GlobalStatus.Begin);
            Map<GlobalStatus, byte[]> multiStatusCursors = new EnumMap<>(GlobalStatus.class);
            for (int i = 0; i < iterations; i++) {
                final int iteration = i;
                int round = i / options.queryIterationsPerRound;
                if (i % options.queryIterationsPerRound == 0) {
                    boolean warmup = round < options.warmupRounds;
                    logRoundStart(scenario, round, options.totalRounds(), warmup);
                }
                if (options.queryWorkloadIncludes("xid")) {
                    measure(xidStats, round, options, () -> {
                        GlobalSession globalSession = dataSet.pick(iteration);
                        GlobalSession actual = storeManager.readSession(globalSession.getXid(), false);
                        assertTrue(actual != null, "xid query returned no session");
                        sinkXid = actual.getXid();
                        return RowMetrics.pointReadReturned(1, 1);
                    });
                }
                if (options.queryWorkloadIncludes("transaction_id")) {
                    measure(transactionIdStats, round, options, () -> {
                        GlobalSession globalSession = dataSet.pick(iteration * 9973);
                        SessionCondition condition = new SessionCondition();
                        condition.setTransactionId(globalSession.getTransactionId());
                        condition.setLazyLoadBranch(true);
                        List<GlobalSession> actual = storeManager.readSession(condition);
                        assertEquals(1, actual.size(), "transactionId query size");
                        sinkXid = actual.get(0).getXid();
                        return RowMetrics.pointReadReturned(2, actual.size());
                    });
                }
                if (options.queryWorkloadIncludes("status")) {
                    measure(statusStats, round, options, () -> {
                        SessionCondition condition = new SessionCondition(TARGET_STATUS);
                        condition.setLazyLoadBranch(true);
                        if (useOvertimeStatusQuery) {
                            condition.setOverTimeAliveMills((long) SESSION_TIMEOUT_MILLIS);
                        }
                        if (options.queryLimit > 0) {
                            condition.setLimit(options.queryLimit);
                        }
                        List<GlobalSession> actual = storeManager.readSession(condition);
                        // Since Direction A an unlimited status scan is bounded by
                        // fullScanDeadlineMillis and may return a truncated prefix.
                        assertTrue(actual.size() <= statusQueryMaxCount, "status query size must not exceed expected");
                        if (actual.size() < expectedLimitedStatusCount) {
                            log(
                                    "  [query.status] truncated by scan deadline: returned %d of %d expected",
                                    actual.size(), expectedLimitedStatusCount);
                        }
                        sinkCount = actual.size();
                        return statusQueryMetrics(condition, actual.size());
                    });
                }
                if (options.queryWorkloadIncludes("status_multi")) {
                    measure(multiStatusStats, round, options, () -> {
                        SessionCondition condition = new SessionCondition(TARGET_STATUS, SECONDARY_STATUS);
                        condition.setLazyLoadBranch(true);
                        condition.setStatusScanCursors(multiStatusCursors);
                        if (options.queryLimit > 0) {
                            condition.setLimit(options.queryLimit);
                        }
                        List<GlobalSession> actual = storeManager.readSession(condition);
                        assertTrue(
                                actual.size() <= multiStatusQueryMaxCount,
                                "multi-status query size must not exceed expected");
                        multiStatusCursors.clear();
                        multiStatusCursors.putAll(condition.getNextStatusScanCursors());
                        sinkCount = actual.size();
                        return RowMetrics.fromScanStats(condition.getScanStats(), actual.size());
                    });
                }
                if (options.queryWorkloadIncludes("begin_sorted")) {
                    measure(beginSortedStats, round, options, () -> {
                        SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
                        condition.setLazyLoadBranch(true);
                        List<GlobalSession> actual = storeManager.readSession(condition);
                        // Since Direction A an unlimited scan is bounded by fullScanDeadlineMillis
                        // and may return a truncated prefix.
                        assertTrue(
                                actual.size() <= expectedBeginCount,
                                "begin sorted query size must not exceed expected");
                        if (actual.size() < expectedBeginCount) {
                            log(
                                    "  [query.begin_sorted] truncated by scan deadline: returned %d of %d expected",
                                    actual.size(), expectedBeginCount);
                        }
                        sinkCount = actual.size();
                        return RowMetrics.fromScanStats(condition.getScanStats(), actual.size());
                    });
                }
                if (options.queryWorkloadIncludes("full_scan_filter")) {
                    measure(fullScanStats, round, options, () -> {
                        SessionCondition condition = new SessionCondition();
                        condition.setLazyLoadBranch(true);
                        int count = 0;
                        for (GlobalSession globalSession : storeManager.readSession(condition)) {
                            if (globalSession.getStatus() == TARGET_STATUS) {
                                count++;
                            }
                        }
                        // Since Direction A an unconditional full scan is deliberately bounded
                        // (store.file.rocksdb.fullScanMaxLimit / fullScanDeadlineMillis), so the
                        // result may be a truncated prefix instead of the full match set.
                        assertTrue(count <= expectedStatusCount, "full scan status count must not exceed expected");
                        if (count < expectedStatusCount) {
                            log(
                                    "  [query.full_scan_filter] truncated by scan protection: matched %d of %d expected",
                                    count, expectedStatusCount);
                        }
                        sinkCount = count;
                        return RowMetrics.fromScanStats(condition.getScanStats(), count);
                    });
                }
            }
            logBlockCacheStats(engine);
            walSyncStats = engine.diagnostics().getWalSyncStats();
        }

        DbFootprint footprint = DbFootprint.from(dbPath, walSyncStats);
        if (options.queryWorkloadIncludes("xid")) {
            emit("query.xid", options, xidStats, footprint, csvLines);
        }
        if (options.queryWorkloadIncludes("transaction_id")) {
            emit("query.transaction_id", options, transactionIdStats, footprint, csvLines);
        }
        if (options.queryWorkloadIncludes("status")) {
            emit("query.status", options, statusStats, footprint, csvLines);
        }
        if (options.queryWorkloadIncludes("status_multi")) {
            emit("query.status_multi", options, multiStatusStats, footprint, csvLines);
        }
        if (options.queryWorkloadIncludes("begin_sorted")) {
            emit("query.begin_sorted", options, beginSortedStats, footprint, csvLines);
        }
        if (options.queryWorkloadIncludes("full_scan_filter")) {
            emit("query.full_scan_filter", options, fullScanStats, footprint, csvLines);
        }
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private static RowMetrics statusQueryMetrics(SessionCondition condition, int returnedEstimate) {
        if (isBoundedStatusQuery(condition)) {
            return RowMetrics.fromScanStats(condition.getScanStats(), returnedEstimate);
        }
        return RowMetrics.scannedAndReturned(returnedEstimate, returnedEstimate);
    }

    private static boolean isBoundedStatusQuery(SessionCondition condition) {
        Long overTimeAliveMills = condition.getOverTimeAliveMills();
        Integer limit = condition.getLimit();
        return (overTimeAliveMills != null && overTimeAliveMills > 0) || (limit != null && limit > 0);
    }

    private void runLockBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines) throws Exception {
        String scenario = "lock";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();

        OperationStats acquireStats = new OperationStats(options.sampleEvery);
        OperationStats conflictAcquireStats = new OperationStats(options.sampleEvery);
        OperationStats conflictCheckStats = new OperationStats(options.sampleEvery);
        OperationStats updateStatusStats = new OperationStats(options.sampleEvery);
        OperationStats releaseBranchStats = new OperationStats(options.sampleEvery);
        OperationStats releaseGlobalStats = new OperationStats(options.sampleEvery);
        OperationStats cleanOrphanStats = new OperationStats(options.sampleEvery);
        OperationStats cleanOrphanBatchedStats = new OperationStats(options.sampleEvery);
        OperationStats cleanOrphanBatchedProbeStats = new OperationStats(options.sampleEvery);
        Path lastDbPath = null;
        RocksDBWalSyncStats lastWalSyncStats = RocksDBWalSyncStats.NONE;

        boolean runPrimaryLockWorkload = options.lockWorkloadIncludes(LOCK_OP_ACQUIRE)
                || options.lockWorkloadIncludes(LOCK_OP_CONFLICT)
                || options.lockWorkloadIncludes(LOCK_OP_UPDATE_STATUS)
                || options.lockWorkloadIncludes(LOCK_OP_RELEASE_BRANCH)
                || options.lockWorkloadIncludes(LOCK_OP_RELEASE_GLOBAL);
        if (runPrimaryLockWorkload) {
            for (int round = 0; round < options.totalRounds(); round++) {
                boolean warmup = round < options.warmupRounds;
                logRoundStart(scenario, round, options.totalRounds(), warmup);
                BenchmarkDataSet dataSet = BenchmarkDataSet.create(options.withAtLeastOneLock(), round);
                Path dbPath = scenarioPath(runPath, "lock-round-" + round);
                lastDbPath = dbPath;
                try (RocksDBStoreEngine engine = open(dbPath, options)) {
                    RocksDBLockManager lockManager = new RocksDBLockManager(engine, options.lockIndexScanBatchSize);
                    for (BranchSession branchSession : dataSet.allBranches()) {
                        if (options.lockWorkloadIncludes(LOCK_OP_ACQUIRE)) {
                            RowMetrics acquireMetrics = RowMetrics.written(
                                    lockRows(branchSession) * 2L, estimateLockAcquireBytes(branchSession));
                            measure(acquireStats, round, options, acquireMetrics, () -> {
                                boolean locked = lockManager.acquireLock(branchSession);
                                assertTrue(locked, "lock acquire failed");
                            });
                        } else {
                            assertTrue(lockManager.acquireLock(branchSession), "lock setup acquire failed");
                        }
                    }
                    for (int globalIndex = 0; globalIndex < dataSet.globalSessions.size(); globalIndex++) {
                        GlobalSession globalSession = dataSet.globalSessions.get(globalIndex);
                        List<BranchSession> branches = dataSet.branchesOf(globalSession);
                        if (branches.isEmpty()) {
                            continue;
                        }
                        BranchSession branchSession = branches.get(0);
                        int branchLockRows = lockRows(branchSession);
                        int globalLockRows = lockRows(branches);
                        if (options.lockWorkloadIncludes(LOCK_OP_CONFLICT)
                                && options.shouldRunLockConflict(globalIndex)) {
                            BranchSession conflict = dataSet.conflictBranch(branchSession);
                            measure(conflictCheckStats, round, options, () -> {
                                boolean lockable = lockManager.isLockable(
                                        conflict.getXid(), conflict.getResourceId(), conflict.getLockKey());
                                assertTrue(!lockable, "conflict check should be false");
                                return RowMetrics.pointReads(lockRows(conflict));
                            });
                            measure(conflictAcquireStats, round, options, () -> {
                                boolean locked = lockManager.acquireLock(conflict);
                                assertTrue(!locked, "conflict acquire should be false");
                                return RowMetrics.pointReads(lockRows(conflict));
                            });
                        }
                        if (options.lockWorkloadIncludes(LOCK_OP_UPDATE_STATUS)) {
                            RowMetrics updateStatusMetrics = RowMetrics.scannedPointReadAndUpdated(
                                    globalLockRows,
                                    globalLockRows,
                                    globalLockRows,
                                    estimateLockValueRewriteBytes(branches));
                            measure(updateStatusStats, round, options, updateStatusMetrics, () -> {
                                lockManager.updateLockStatus(globalSession.getXid(), LockStatus.Rollbacking);
                            });
                        }
                        if (options.lockWorkloadIncludes(LOCK_OP_RELEASE_BRANCH)) {
                            RowMetrics releaseBranchMetrics = RowMetrics.scannedPointReadAndUpdated(
                                    branchLockRows,
                                    branchLockRows,
                                    branchLockRows * 2L,
                                    estimateLockDeleteBytes(branchSession));
                            measure(releaseBranchStats, round, options, releaseBranchMetrics, () -> {
                                boolean released = lockManager.releaseLock(branchSession);
                                assertTrue(released, "branch release failed");
                            });
                        }
                        if (options.lockWorkloadIncludes(LOCK_OP_RELEASE_GLOBAL)) {
                            int remainingGlobalLockRows = globalLockRows;
                            if (options.lockWorkloadIncludes(LOCK_OP_RELEASE_BRANCH)) {
                                remainingGlobalLockRows -= branchLockRows;
                                if (branches.size() == 1) {
                                    assertTrue(
                                            lockManager.acquireLock(branchSession),
                                            "lock reacquire before global release failed");
                                    remainingGlobalLockRows += branchLockRows;
                                }
                            }
                            final int releaseGlobalRows = remainingGlobalLockRows;
                            RowMetrics releaseGlobalMetrics = RowMetrics.scannedPointReadAndUpdated(
                                    releaseGlobalRows,
                                    releaseGlobalRows,
                                    releaseGlobalRows * 2L,
                                    estimateLockDeleteBytes(branches));
                            measure(releaseGlobalStats, round, options, releaseGlobalMetrics, () -> {
                                boolean released = lockManager.releaseGlobalSessionLock(globalSession);
                                assertTrue(released, "global release failed");
                            });
                        }
                    }
                    if (options.lockWorkloadIncludes(LOCK_OP_RELEASE_GLOBAL)) {
                        assertTrue(
                                engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0])
                                        .isEmpty(),
                                "lock table should be empty after release");
                        assertTrue(
                                engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                                        .isEmpty(),
                                "lock branch index should be empty after release");
                    }
                    lastWalSyncStats = engine.diagnostics().getWalSyncStats();
                }
            }
        }

        Path lastOrphanDbPath = null;
        RocksDBWalSyncStats lastOrphanWalSyncStats = RocksDBWalSyncStats.NONE;
        if (options.lockWorkloadIncludes(LOCK_OP_CLEAN_ORPHAN)) {
            for (int round = 0; round < options.totalRounds(); round++) {
                log("  [lock.clean_orphan] round %d/%d", round + 1, options.totalRounds());
                Path orphanDbPath = scenarioPath(runPath, "lock-clean-orphan-round-" + round);
                lastOrphanDbPath = orphanDbPath;
                try (RocksDBStoreEngine engine = open(orphanDbPath, options)) {
                    RocksDBLockManager lockManager = new RocksDBLockManager(engine, options.lockIndexScanBatchSize);
                    BenchmarkDataSet dataSet = BenchmarkDataSet.create(options.withAtLeastOneLock(), round);
                    List<BranchSession> allBranches = dataSet.allBranches();
                    int orphanRows = lockRows(allBranches);
                    for (BranchSession branchSession : allBranches) {
                        assertTrue(lockManager.acquireLock(branchSession), "orphan lock prepare failed");
                    }
                    RowMetrics cleanOrphanMetrics = RowMetrics.scannedPointReadAndUpdated(
                            orphanRows, orphanRows, orphanRows * 2L, estimateLockDeleteBytes(allBranches));
                    measure(cleanOrphanStats, round, options, cleanOrphanMetrics, () -> {
                        int cleaned = lockManager.cleanOrphanLocks();
                        assertTrue(cleaned > 0 || orphanRows == 0, "cleanOrphanLocks should clean prepared locks");
                    });
                    assertTrue(
                            engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0])
                                    .isEmpty(),
                            "lock table should be empty after clean orphan");
                    lastOrphanWalSyncStats = engine.diagnostics().getWalSyncStats();
                }
            }
        }

        Path lastOrphanBatchedDbPath = null;
        RocksDBWalSyncStats lastOrphanBatchedWalSyncStats = RocksDBWalSyncStats.NONE;
        if (options.lockWorkloadIncludes(LOCK_OP_CLEAN_ORPHAN_BATCHED)) {
            for (int round = 0; round < options.totalRounds(); round++) {
                log("  [lock.clean_orphan_batched] round %d/%d", round + 1, options.totalRounds());
                Path orphanDbPath = scenarioPath(runPath, "lock-clean-orphan-batched-round-" + round);
                lastOrphanBatchedDbPath = orphanDbPath;
                try (RocksDBStoreEngine engine = open(orphanDbPath, options)) {
                    RocksDBLockManager lockManager = new RocksDBLockManager(engine, options.lockIndexScanBatchSize);
                    BenchmarkDataSet dataSet = BenchmarkDataSet.create(options.withAtLeastOneLock(), round);
                    List<BranchSession> allBranches = dataSet.allBranches();
                    int orphanRows = lockRows(allBranches);
                    for (BranchSession branchSession : allBranches) {
                        assertTrue(lockManager.acquireLock(branchSession), "orphan lock prepare failed");
                    }

                    // Foreground probe: a thread acquiring/releasing locks with persisted branch sessions, so
                    // the batched cleaner keeps them while we measure foreground latency during cleanup.
                    RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                    List<BranchSession> probeBranches = new ArrayList<>();
                    for (int i = 0; i < CLEAN_ORPHAN_FOREGROUND_PROBE_BRANCHES; i++) {
                        BranchSession probe = new BranchSession(BranchType.AT);
                        probe.setXid("foreground-probe:" + i);
                        probe.setTransactionId(900_000_000L + i);
                        probe.setBranchId(i + 1L);
                        probe.setStatus(BranchStatus.Registered);
                        probe.setResourceId("jdbc:mysql://127.0.0.1/probe");
                        probe.setLockKey("t_probe:" + i);
                        probeBranches.add(probe);
                        storeManager.writeSession(LogOperation.BRANCH_ADD, probe);
                    }
                    boolean measureProbe = round >= options.warmupRounds;
                    AtomicBoolean probeRunning = new AtomicBoolean(true);
                    CountDownLatch probeStarted = new CountDownLatch(1);
                    Thread probeThread = new Thread(
                            () -> {
                                int index = 0;
                                while (probeRunning.get()) {
                                    BranchSession branch = probeBranches.get(index % probeBranches.size());
                                    index++;
                                    long startedAt = System.nanoTime();
                                    try {
                                        if (!lockManager.acquireLock(branch)) {
                                            continue;
                                        }
                                        lockManager.releaseLock(branch);
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    if (measureProbe) {
                                        cleanOrphanBatchedProbeStats.record(System.nanoTime() - startedAt);
                                        probeStarted.countDown();
                                    }
                                }
                            },
                            "orphan-batched-foreground-probe");
                    probeThread.start();

                    byte[] cursor = null;
                    int totalCleaned = 0;
                    int batchRounds = 0;
                    long passStartNanos = System.nanoTime();
                    try {
                        if (measureProbe) {
                            assertTrue(
                                    probeStarted.await(5L, TimeUnit.SECONDS),
                                    "orphan cleanup foreground probe did not start");
                        }
                        while (true) {
                            byte[] seekKey = cursor;
                            RocksDBLockManager.CleanOrphanLocksResult[] resultHolder =
                                    new RocksDBLockManager.CleanOrphanLocksResult[1];
                            measure(cleanOrphanBatchedStats, round, options, () -> {
                                RocksDBLockManager.CleanOrphanLocksResult batchedResult =
                                        lockManager.cleanOrphanLocksBatches(
                                                seekKey, options.orphanCleanBatchLimit, options.orphanCleanMaxBatches);
                                resultHolder[0] = batchedResult;
                                return RowMetrics.scannedPointReadAndUpdated(
                                        batchedResult.getScanned(),
                                        batchedResult.getScanned(),
                                        batchedResult.getCleaned(),
                                        0L);
                            });
                            RocksDBLockManager.CleanOrphanLocksResult result = resultHolder[0];
                            totalCleaned += result.getCleaned();
                            batchRounds++;
                            cursor = result.getNextSeekKey();
                            if (!result.isLimitReached() || cursor == null) {
                                break;
                            }
                            Thread.sleep(options.orphanCleanRoundSleepMillis);
                        }
                    } finally {
                        probeRunning.set(false);
                        probeThread.join();
                    }
                    double passMillis = (System.nanoTime() - passStartNanos) / 1_000_000.0;
                    // The probe thread releases/reacquires its locks while the cleaner scans; when a probe
                    // release lands between the cleaner's index scan and its LOCK point-read, the cleaner counts
                    // one harmless phantom clean (an idempotent delete of an already-deleted index entry).
                    // Hence cleaned may exceed orphanRows by up to the number of probe operations.
                    assertTrue(
                            totalCleaned >= orphanRows
                                    && totalCleaned <= orphanRows + cleanOrphanBatchedProbeStats.ops(),
                            "batched clean orphan should clean all prepared locks, expected:" + orphanRows
                                    + ", cleaned:" + totalCleaned + ", probeOps:"
                                    + cleanOrphanBatchedProbeStats.ops());
                    assertTrue(
                            engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0])
                                    .isEmpty(),
                            "lock table should be empty after batched clean orphan");
                    assertTrue(
                            engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                                    .isEmpty(),
                            "lock branch index should be empty after batched clean orphan");
                    log(
                            "  [lock.clean_orphan_batched] round %d pass completed: batchRounds=%d, cleaned=%d "
                                    + "(phantom=%d), passMillis=%.1f (%.1f ms/round avg), roundSleepMillis=%d",
                            round + 1,
                            batchRounds,
                            totalCleaned,
                            totalCleaned - orphanRows,
                            passMillis,
                            batchRounds == 0 ? 0D : passMillis / batchRounds,
                            options.orphanCleanRoundSleepMillis);
                    if (cleanOrphanBatchedProbeStats.ops() > 0L) {
                        log(
                                "  [lock.clean_orphan_batched] foreground probe: ops=%d samples=%d "
                                        + "p50=%.3fms p95=%.3fms p99=%.3fms max=%.3fms",
                                cleanOrphanBatchedProbeStats.ops(),
                                cleanOrphanBatchedProbeStats.sampleCount(),
                                cleanOrphanBatchedProbeStats.percentile(50) / 1e6,
                                cleanOrphanBatchedProbeStats.percentile(95) / 1e6,
                                cleanOrphanBatchedProbeStats.percentile(99) / 1e6,
                                cleanOrphanBatchedProbeStats.percentile(100) / 1e6);
                    }
                    lastOrphanBatchedWalSyncStats = engine.diagnostics().getWalSyncStats();
                }
            }
        }

        DbFootprint footprint = DbFootprint.from(lastDbPath, lastWalSyncStats);
        if (options.lockWorkloadIncludes(LOCK_OP_ACQUIRE)) {
            emit("lock.acquire", options, acquireStats, footprint, csvLines);
        }
        if (options.lockWorkloadIncludes(LOCK_OP_CONFLICT)) {
            emit("lock.conflict_check", options, conflictCheckStats, footprint, csvLines);
            emit("lock.conflict_acquire", options, conflictAcquireStats, footprint, csvLines);
        }
        if (options.lockWorkloadIncludes(LOCK_OP_UPDATE_STATUS)) {
            emit("lock.update_status", options, updateStatusStats, footprint, csvLines);
        }
        if (options.lockWorkloadIncludes(LOCK_OP_RELEASE_BRANCH)) {
            emit("lock.release_branch", options, releaseBranchStats, footprint, csvLines);
        }
        if (options.lockWorkloadIncludes(LOCK_OP_RELEASE_GLOBAL)) {
            emit("lock.release_global", options, releaseGlobalStats, footprint, csvLines);
        }
        if (options.lockWorkloadIncludes(LOCK_OP_CLEAN_ORPHAN)) {
            emit(
                    "lock.clean_orphan",
                    options,
                    cleanOrphanStats,
                    DbFootprint.from(lastOrphanDbPath, lastOrphanWalSyncStats),
                    csvLines);
        }
        if (options.lockWorkloadIncludes(LOCK_OP_CLEAN_ORPHAN_BATCHED)) {
            emit(
                    "lock.clean_orphan_batched",
                    options,
                    cleanOrphanBatchedStats,
                    DbFootprint.from(lastOrphanBatchedDbPath, lastOrphanBatchedWalSyncStats),
                    csvLines);
            emit(
                    "lock.clean_orphan_batched_foreground_probe",
                    options,
                    cleanOrphanBatchedProbeStats,
                    DbFootprint.from(lastOrphanBatchedDbPath, lastOrphanBatchedWalSyncStats),
                    csvLines);
        }
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private void runCleanupBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines) throws Exception {
        String scenario = "cleanup";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();

        OperationStats globalRemoveStats = new OperationStats(options.sampleEvery);
        Path lastDbPath = null;
        RocksDBWalSyncStats lastWalSyncStats = RocksDBWalSyncStats.NONE;

        for (int round = 0; round < options.totalRounds(); round++) {
            boolean warmup = round < options.warmupRounds;
            logRoundStart(scenario, round, options.totalRounds(), warmup);
            BenchmarkDataSet dataSet = BenchmarkDataSet.create(options, round);
            Path dbPath = scenarioPath(runPath, "cleanup-round-" + round);
            lastDbPath = dbPath;
            try (RocksDBStoreEngine engine = open(dbPath, options)) {
                RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                writeDataSet(storeManager, dataSet);
                engine.flush();
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    int branchCount = dataSet.branchesOf(globalSession).size();
                    measure(
                            globalRemoveStats,
                            round,
                            options,
                            globalRemoveMetrics(options, globalSession, branchCount),
                            () -> storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession));
                }
                verifyStoreEmpty(engine);
                engine.flush();
                lastWalSyncStats = engine.diagnostics().getWalSyncStats();
            }
        }

        emit(
                "cleanup.global_remove_with_branches",
                options,
                globalRemoveStats,
                DbFootprint.from(lastDbPath, lastWalSyncStats),
                csvLines);
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private void runLifecycleBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines) throws Exception {
        String scenario = "lifecycle";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();

        OperationStats globalRemoveStats = new OperationStats(options.sampleEvery);
        Path lastDbPath = null;
        RocksDBWalSyncStats lastWalSyncStats = RocksDBWalSyncStats.NONE;

        for (int round = 0; round < options.totalRounds(); round++) {
            boolean warmup = round < options.warmupRounds;
            logRoundStart(scenario, round, options.totalRounds(), warmup);
            BenchmarkDataSet dataSet = BenchmarkDataSet.create(options.withAtLeastOneLock(), round);
            Path dbPath = scenarioPath(runPath, "lifecycle-round-" + round);
            lastDbPath = dbPath;
            try (RocksDBStoreEngine engine = open(dbPath, options)) {
                RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                RocksDBLockManager lockManager = new RocksDBLockManager(engine, options.lockIndexScanBatchSize);
                writeDataSet(storeManager, dataSet);
                for (BranchSession branchSession : dataSet.allBranches()) {
                    assertTrue(lockManager.acquireLock(branchSession), "lifecycle lock setup acquire failed");
                }
                engine.flush();
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    List<BranchSession> branches = dataSet.branchesOf(globalSession);
                    int lockCount = lockRows(branches);
                    measure(
                            globalRemoveStats,
                            round,
                            options,
                            lifecycleGlobalRemoveMetrics(options, globalSession, branches, lockCount),
                            () -> {
                                assertTrue(
                                        lockManager.releaseGlobalSessionLock(globalSession),
                                        "lifecycle global lock release failed");
                                storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession);
                            });
                }
                verifyLifecycleStoreEmpty(engine);
                engine.flush();
                lastWalSyncStats = engine.diagnostics().getWalSyncStats();
            }
        }

        emit(
                "lifecycle.global_remove_with_locks",
                options,
                globalRemoveStats,
                DbFootprint.from(lastDbPath, lastWalSyncStats),
                csvLines);
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private void runBackgroundInterferenceBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines)
            throws Exception {
        String scenario = "background";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();
        OperationStats scanStats = new OperationStats(options.sampleEvery);
        OperationStats foregroundStats = new OperationStats(options.sampleEvery);
        Path dbPath = scenarioPath(runPath, "background");
        RocksDBWalSyncStats walSyncStats = RocksDBWalSyncStats.NONE;

        try (RocksDBStoreEngine engine = open(dbPath, options)) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            BenchmarkDataSet dataSet = BenchmarkDataSet.create(options, 0);
            writeDataSet(storeManager, dataSet);
            engine.flush();

            AtomicLong probeFailures = new AtomicLong();
            int foregroundProbeOperations =
                    options.queryLimit > 0 ? options.queryLimit : BACKGROUND_QUERY_DEFAULT_LIMIT;
            Thread probeThread = new Thread(
                    () -> runBackgroundForegroundProbe(
                            storeManager, options, foregroundStats, foregroundProbeOperations, probeFailures),
                    "r2-background-foreground-probe");
            probeThread.start();
            if (!QUERY_WORKLOAD_NONE.equals(options.queryWorkload)) {
                SessionManager originalSessionManager =
                        replaceRootSessionManager(new RocksDBSessionManager("root.data", engine));
                Set<String> seenXids = new LinkedHashSet<>();
                long firstRoundLatestBeginTime = Long.MIN_VALUE;
                boolean advancedPastFirstPage = false;
                int iterations = Math.max(1, options.totalRounds() * options.queryIterationsPerRound);
                try {
                    DefaultCoordinator coordinator = new BenchmarkCoordinator();
                    for (int iteration = 0; iteration < iterations; iteration++) {
                        long scanStartedAt = System.nanoTime();
                        List<GlobalSession> sessions = findCoordinatorBackgroundSessions(coordinator);
                        if (sessions.isEmpty()) {
                            break;
                        }
                        for (GlobalSession session : sessions) {
                            assertTrue(
                                    seenXids.add(session.getXid()),
                                    "background cursor repeated before pass completion");
                            if (iteration == 0) {
                                firstRoundLatestBeginTime = Math.max(firstRoundLatestBeginTime, session.getBeginTime());
                            } else if (session.getBeginTime() > firstRoundLatestBeginTime) {
                                advancedPastFirstPage = true;
                            }
                        }
                        scanStats.record(System.nanoTime() - scanStartedAt, RowMetrics.returned(sessions.size()));
                        log(
                                "  [background.r2] round=%d returned=%d unique=%d",
                                iteration + 1, sessions.size(), seenXids.size());
                        if (sessions.size() < BACKGROUND_QUERY_DEFAULT_LIMIT) {
                            break;
                        }
                    }
                } finally {
                    replaceRootSessionManager(originalSessionManager);
                }
                assertTrue(!seenXids.isEmpty(), "background cursor scan returned no session");
                assertTrue(advancedPastFirstPage, "background cursor did not advance past persistent failures");
            }
            probeThread.join();
            assertEquals(0L, probeFailures.get(), "foreground probe failed");
            walSyncStats = engine.diagnostics().getWalSyncStats();
        }

        DbFootprint footprint = DbFootprint.from(dbPath, walSyncStats);
        if (!QUERY_WORKLOAD_NONE.equals(options.queryWorkload)) {
            emit("background.r2_coordinator_multi_status_cursor", options, scanStats, footprint, csvLines);
            emit("background.r2_foreground_probe", options, foregroundStats, footprint, csvLines);
        } else {
            emit("background.r2_foreground_baseline", options, foregroundStats, footprint, csvLines);
        }
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private static void runBackgroundForegroundProbe(
            RocksDBTransactionStoreManager storeManager,
            BenchmarkOptions options,
            OperationStats foregroundStats,
            int operations,
            AtomicLong probeFailures) {
        for (int index = 0; index < operations; index++) {
            GlobalSession probe = BenchmarkDataSet.globalSession(
                    index++, 99, options.seed, System.currentTimeMillis(), GlobalStatus.Begin);
            long startedAt = System.nanoTime();
            try {
                storeManager.writeSession(LogOperation.GLOBAL_ADD, probe);
                storeManager.writeSession(LogOperation.GLOBAL_REMOVE, probe);
                foregroundStats.record(
                        System.nanoTime() - startedAt,
                        RowMetrics.written(
                                7L, estimateGlobalPutBytes(probe) + estimateGlobalRemoveBytes(probe, 0, false)));
            } catch (RuntimeException e) {
                probeFailures.incrementAndGet();
            }
            LockSupport.parkNanos(BACKGROUND_FOREGROUND_PROBE_INTERVAL_NANOS);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<GlobalSession> findCoordinatorBackgroundSessions(DefaultCoordinator coordinator)
            throws Exception {
        Method method = DefaultCoordinator.class.getDeclaredMethod(
                "findBackgroundSessions", GlobalStatus[].class, boolean.class);
        method.setAccessible(true);
        return (List<GlobalSession>)
                method.invoke(coordinator, new GlobalStatus[] {TARGET_STATUS, SECONDARY_STATUS}, true);
    }

    private static SessionManager replaceRootSessionManager(SessionManager replacement) throws Exception {
        Field field = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
        field.setAccessible(true);
        SessionManager original = (SessionManager) field.get(null);
        field.set(null, replacement);
        return original;
    }

    private static final class BenchmarkCoordinator extends DefaultCoordinator {

        private BenchmarkCoordinator() {
            super((RemotingServer) Proxy.newProxyInstance(
                    RemotingServer.class.getClassLoader(),
                    new Class[] {RemotingServer.class},
                    (proxy, method, args) -> null));
        }
    }

    private void runRestartBenchmark(Path runPath, BenchmarkOptions options, List<String> csvLines) throws Exception {
        String scenario = "restart";
        logScenarioStart(scenario, options);
        SystemMetrics metricsBefore = SystemMetrics.snapshot();
        long scenarioStart = System.nanoTime();

        runRestartScenario(runPath, options, "restart.mixed", null, csvLines);
        runRestartScenario(runPath, options, "restart.active_only", GlobalStatus.Begin, csvLines);
        runRestartScenario(runPath, options, "restart.terminal_only", GlobalStatus.Committed, csvLines);

        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
    }

    private void runRestartScenario(
            Path runPath,
            BenchmarkOptions options,
            String scenarioName,
            GlobalStatus overrideStatus,
            List<String> csvLines)
            throws Exception {
        OperationStats restartStats = new OperationStats(options.sampleEvery);
        Path lastDbPath = null;
        RocksDBWalSyncStats lastWalSyncStats = RocksDBWalSyncStats.NONE;

        log("  [%s] override status: %s", scenarioName, overrideStatus != null ? overrideStatus : "mixed");

        for (int round = 0; round < options.totalRounds(); round++) {
            boolean warmup = round < options.warmupRounds;
            logRoundStart(scenarioName, round, options.totalRounds(), warmup);
            BenchmarkDataSet dataSet = BenchmarkDataSet.create(options, round);
            if (overrideStatus != null) {
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    globalSession.setStatus(overrideStatus);
                }
            }
            Path dbPath = scenarioPath(runPath, scenarioName.replace('.', '_') + "-round-" + round);
            lastDbPath = dbPath;
            try (RocksDBStoreEngine engine = open(dbPath, options)) {
                RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                writeDataSet(storeManager, dataSet);
                engine.flush();
                lastWalSyncStats = engine.diagnostics().getWalSyncStats();
            }
            final BenchmarkDataSet finalDataSet = dataSet;
            measure(restartStats, round, options, () -> {
                try (RocksDBStoreEngine engine = open(dbPath, options)) {
                    new RocksDBTransactionStoreManager(engine);
                }
            });
            try (RocksDBStoreEngine engine = open(dbPath, options)) {
                // Count directly on the GLOBAL_SESSION column family: since Direction A the
                // store-manager full scan is bounded (fullScanMaxLimit) and would truncate
                // this post-restart verification at large scale.
                int[] restoredCount = {0};
                engine.scanByPrefix(
                        RocksDBColumnFamily.GLOBAL_SESSION, new byte[0], 0, 0L, (key, value) -> restoredCount[0]++);
                assertEquals(options.globalCount, restoredCount[0], scenarioName + " restart global count");
            }
        }

        emit(scenarioName, options, restartStats, DbFootprint.from(lastDbPath, lastWalSyncStats), csvLines);
    }

    private static void writeDataSet(RocksDBTransactionStoreManager storeManager, BenchmarkDataSet dataSet) {
        for (GlobalSession globalSession : dataSet.globalSessions) {
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            for (BranchSession branchSession : dataSet.branchesOf(globalSession)) {
                storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession);
            }
        }
    }

    private static void verifyLifecycleStoreEmpty(RocksDBStoreEngine engine) {
        verifyStoreEmpty(engine);
        assertTrue(
                engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0]).isEmpty(),
                "lock table should be empty after lifecycle global remove");
        assertTrue(
                engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                        .isEmpty(),
                "lock branch index should be empty after lifecycle global remove");
    }

    private static int lockRows(Collection<BranchSession> branchSessions) {
        int rows = 0;
        for (BranchSession branchSession : branchSessions) {
            rows += lockRows(branchSession);
        }
        return rows;
    }

    private static int lockRows(BranchSession branchSession) {
        if (branchSession == null || StringUtils.isBlank(branchSession.getLockKey())) {
            return 0;
        }
        int rows = 0;
        for (String tableLocks : branchSession.getLockKey().split(";")) {
            int separator = tableLocks.indexOf(':');
            String lockKeys = separator >= 0 ? tableLocks.substring(separator + 1) : tableLocks;
            for (String lockKey : lockKeys.split(",")) {
                if (!lockKey.trim().isEmpty()) {
                    rows++;
                }
            }
        }
        return rows;
    }

    private static long estimateGlobalPutBytes(GlobalSession session) {
        byte[] xidValue = bytes(session.getXid());
        return byteSize(RocksDBKeyCodec.encodeXid(session.getXid()), encodeGlobalSessionValue(session))
                + byteSize(
                        RocksDBKeyCodec.encodeGlobalStatusIndex(
                                session.getStatus(), session.getBeginTime(), session.getXid()),
                        xidValue)
                + byteSize(RocksDBKeyCodec.encodeTransactionIdIndex(session.getTransactionId()), xidValue);
    }

    private static long estimateGlobalUpdateBytes(GlobalSession session) {
        return estimateGlobalPutBytes(session) + estimateGlobalIndexDeleteBytes(session);
    }

    private static RowMetrics globalRemoveMetrics(BenchmarkOptions options, GlobalSession session, int branchCount) {
        byte[] prefix = RocksDBKeyCodec.encodeXidPrefix(session.getXid());
        boolean rangeDelete = options.enableRangeDelete && RocksDBKeyCodec.prefixEnd(prefix) != null;
        long pointDeleteCount = globalRemovePointDeleteCount(session) + (rangeDelete ? 0L : branchCount);
        return RowMetrics.globalRemove(
                rangeDelete,
                branchCount,
                globalRemovePointDeleteCount(session) + branchCount,
                pointDeleteCount,
                estimateGlobalRemoveBytes(session, branchCount, rangeDelete));
    }

    private static RowMetrics lifecycleGlobalRemoveMetrics(
            BenchmarkOptions options, GlobalSession session, Collection<BranchSession> branches, int lockCount) {
        int branchCount = branches.size();
        byte[] prefix = RocksDBKeyCodec.encodeXidPrefix(session.getXid());
        boolean rangeDelete = options.enableRangeDelete && RocksDBKeyCodec.prefixEnd(prefix) != null;
        long sessionPointDeletes = globalRemovePointDeleteCount(session) + (rangeDelete ? 0L : branchCount);
        int lockDeleteBatches =
                lockCount == 0 ? 0 : (lockCount + options.lockIndexScanBatchSize - 1) / options.lockIndexScanBatchSize;
        return RowMetrics.lifecycleGlobalRemove(
                rangeDelete,
                branchCount,
                lockCount,
                lockCount + (rangeDelete ? 0L : branchCount),
                sessionPointDeletes + lockCount * 2L,
                1L + lockDeleteBatches,
                estimateGlobalRemoveBytes(session, branchCount, rangeDelete) + estimateLockDeleteBytes(branches));
    }

    private static long estimateGlobalRemoveBytes(GlobalSession session, int branchCount, boolean rangeDelete) {
        long bytes = byteSize(RocksDBKeyCodec.encodeXid(session.getXid())) + estimateGlobalIndexDeleteBytes(session);
        if (session.getStatus() == GlobalStatus.Begin) {
            bytes += byteSize(RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                    session.getBeginTime() + session.getTimeout(), session.getXid()));
        }
        byte[] prefix = RocksDBKeyCodec.encodeXidPrefix(session.getXid());
        if (rangeDelete) {
            bytes += byteSize(prefix, RocksDBKeyCodec.prefixEnd(prefix));
        } else {
            bytes += (long) branchCount * (prefix.length + Long.BYTES);
        }
        return bytes;
    }

    private static long globalRemovePointDeleteCount(GlobalSession session) {
        return session.getStatus() == GlobalStatus.Begin ? 4L : 3L;
    }

    private static long estimateGlobalIndexDeleteBytes(GlobalSession session) {
        return byteSize(RocksDBKeyCodec.encodeGlobalStatusIndex(
                        session.getStatus(), session.getBeginTime(), session.getXid()))
                + byteSize(RocksDBKeyCodec.encodeTransactionIdIndex(session.getTransactionId()));
    }

    private static long estimateBranchPutBytes(BranchSession session) {
        return byteSize(
                RocksDBKeyCodec.encodeBranch(session.getXid(), session.getBranchId()),
                encodeBranchSessionValue(session));
    }

    private static long estimateBranchDeleteBytes(BranchSession session) {
        return byteSize(RocksDBKeyCodec.encodeBranch(session.getXid(), session.getBranchId()));
    }

    private static long estimateLockAcquireBytes(BranchSession branchSession) {
        long bytes = 0L;
        for (LockKeyEstimate estimate : lockKeyEstimates(branchSession)) {
            bytes += byteSize(estimate.lockKey, estimate.lockValue);
            bytes += byteSize(estimate.indexKey, estimate.lockKey);
        }
        return bytes;
    }

    private static long estimateLockValueRewriteBytes(Collection<BranchSession> branchSessions) {
        long bytes = 0L;
        for (BranchSession branchSession : branchSessions) {
            for (LockKeyEstimate estimate : lockKeyEstimates(branchSession)) {
                bytes += byteSize(estimate.lockKey, estimate.lockValue);
            }
        }
        return bytes;
    }

    private static long estimateLockDeleteBytes(Collection<BranchSession> branchSessions) {
        long bytes = 0L;
        for (BranchSession branchSession : branchSessions) {
            bytes += estimateLockDeleteBytes(branchSession);
        }
        return bytes;
    }

    private static long estimateLockDeleteBytes(BranchSession branchSession) {
        long bytes = 0L;
        for (LockKeyEstimate estimate : lockKeyEstimates(branchSession)) {
            bytes += byteSize(estimate.lockKey);
            bytes += byteSize(estimate.indexKey);
        }
        return bytes;
    }

    private static List<LockKeyEstimate> lockKeyEstimates(BranchSession branchSession) {
        if (branchSession == null || StringUtils.isBlank(branchSession.getLockKey())) {
            return Collections.emptyList();
        }
        List<LockKeyEstimate> result = new ArrayList<>();
        for (String tableLocks : branchSession.getLockKey().split(";")) {
            int separator = tableLocks.indexOf(':');
            if (separator <= 0 || separator == tableLocks.length() - 1) {
                continue;
            }
            String tableName = tableLocks.substring(0, separator);
            for (String pk : tableLocks.substring(separator + 1).split(",")) {
                String trimmedPk = pk.trim();
                if (trimmedPk.isEmpty()) {
                    continue;
                }
                byte[] lockKey = RocksDBKeyCodec.encodeRowLock(branchSession.getResourceId(), tableName, trimmedPk);
                byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex(
                        branchSession.getXid(), branchSession.getBranchId(), lockKey);
                byte[] lockValue = estimateLockValue(branchSession, tableName, trimmedPk);
                result.add(new LockKeyEstimate(lockKey, indexKey, lockValue));
            }
        }
        return result;
    }

    private static byte[] estimateLockValue(BranchSession branchSession, String tableName, String pk) {
        String rowKey = branchSession.getResourceId() + ":" + tableName + ":" + pk;
        int payloadSize = stringFieldBytes(branchSession.getXid())
                + Long.BYTES
                + Long.BYTES
                + stringFieldBytes(branchSession.getResourceId())
                + stringFieldBytes(tableName)
                + stringFieldBytes(pk)
                + stringFieldBytes(rowKey)
                + Integer.BYTES;
        return RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.LOCK_HOLDER, new byte[payloadSize]);
    }

    private static byte[] encodeGlobalSessionValue(GlobalSession session) {
        return RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.GLOBAL_SESSION, session.encode());
    }

    private static byte[] encodeBranchSessionValue(BranchSession session) {
        return RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.BRANCH_SESSION, session.encode());
    }

    private static int stringFieldBytes(String value) {
        return Integer.BYTES + bytes(value).length;
    }

    private static byte[] bytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }

    private static long byteSize(byte[]... values) {
        long size = 0L;
        for (byte[] value : values) {
            if (value != null) {
                size += value.length;
            }
        }
        return size;
    }

    private static RocksDBStoreEngine open(Path dbPath, BenchmarkOptions options) {
        RocksDBStoreConfig config = new RocksDBStoreConfig(
                dbPath.toString(),
                options.syncWrite,
                options.blockCacheSize,
                options.writeBufferSize,
                options.maxWriteBufferNumber,
                options.minWriteBufferNumberToMerge,
                options.maxBackgroundJobs,
                options.maxOpenFiles,
                options.targetFileSizeBase,
                options.level0FileNumCompactionTrigger,
                options.level0SlowdownWritesTrigger,
                options.level0StopWritesTrigger,
                options.enableStatistics,
                options.optimizeFiltersForHits,
                options.compressionType,
                options.enableRangeDelete,
                false, // rangeDeleteCompactAfterDelete
                options.walSyncMode,
                options.walSyncIntervalMillis,
                options.walSyncWriteThreshold,
                options.walSyncOnShutdown,
                options.walSyncWarnThresholdMillis,
                options.dbWriteBufferSize,
                options.globalWriteBufferSize,
                options.branchWriteBufferSize,
                options.lockWriteBufferSize,
                options.indexWriteBufferSize,
                options.metadataWriteBufferSize,
                options.maxTotalWalSize);
        return RocksDBStoreEngine.open(config);
    }

    private static RocksDBStoreEngine open(Path dbPath, boolean syncWrite) {
        return open(dbPath, syncWrite, false, 0L);
    }

    private static RocksDBStoreEngine open(
            Path dbPath, boolean syncWrite, boolean enableRangeDelete, long blockCacheSize) {
        RocksDBStoreConfig config = new RocksDBStoreConfig(
                dbPath.toString(),
                syncWrite,
                blockCacheSize,
                0L, // writeBufferSize (default)
                0, // maxWriteBufferNumber (default)
                0, // minWriteBufferNumberToMerge (default)
                0, // maxBackgroundJobs (default)
                0, // maxOpenFiles (default)
                0L, // targetFileSizeBase (default)
                0, // level0FileNumCompactionTrigger (default)
                0, // level0SlowdownWritesTrigger (default)
                0, // level0StopWritesTrigger (default)
                false, // enableStatistics
                false, // optimizeFiltersForHits
                null, // compressionType
                enableRangeDelete,
                false); // rangeDeleteCompactAfterDelete
        return RocksDBStoreEngine.open(config);
    }

    private static Path scenarioPath(Path runPath, String scenario) throws IOException {
        Path path = runPath.resolve(scenario);
        deleteRecursively(path);
        Files.createDirectories(path);
        return path;
    }

    private static void measure(OperationStats stats, int round, BenchmarkOptions options, BenchmarkTask task)
            throws Exception {
        long startedAt = System.nanoTime();
        task.run();
        long elapsed = System.nanoTime() - startedAt;
        if (round >= options.warmupRounds) {
            stats.record(elapsed);
        }
    }

    private static void measure(
            OperationStats stats, int round, BenchmarkOptions options, RowMetrics rows, BenchmarkTask task)
            throws Exception {
        long startedAt = System.nanoTime();
        task.run();
        long elapsed = System.nanoTime() - startedAt;
        if (round >= options.warmupRounds) {
            stats.record(elapsed, rows);
        }
    }

    private static void measure(OperationStats stats, int round, BenchmarkOptions options, MeasuredBenchmarkTask task)
            throws Exception {
        long startedAt = System.nanoTime();
        RowMetrics rows = task.run();
        long elapsed = System.nanoTime() - startedAt;
        if (round >= options.warmupRounds) {
            stats.record(elapsed, rows);
        }
    }

    private static void verifyStoreEmpty(RocksDBStoreEngine engine) {
        assertTrue(
                engine.prefixScan(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0])
                        .isEmpty(),
                "global_session should be empty");
        assertTrue(
                engine.prefixScan(RocksDBColumnFamily.BRANCH_SESSION, new byte[0])
                        .isEmpty(),
                "branch_session should be empty");
        assertTrue(
                engine.prefixScan(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, new byte[0])
                        .isEmpty(),
                "global_status_index should be empty");
        assertTrue(
                engine.prefixScan(RocksDBColumnFamily.TRANSACTION_ID_INDEX, new byte[0])
                        .isEmpty(),
                "transaction_id_index should be empty");
    }

    private static GlobalStatus nextStatus(GlobalStatus status) {
        if (status == GlobalStatus.Begin) {
            return GlobalStatus.Committing;
        }
        if (status == GlobalStatus.Committing) {
            return GlobalStatus.CommitRetrying;
        }
        if (status == GlobalStatus.RollbackRetrying) {
            return GlobalStatus.Rollbacking;
        }
        return status;
    }

    private static void emit(
            String scenario,
            BenchmarkOptions options,
            OperationStats stats,
            DbFootprint footprint,
            List<String> csvLines) {
        String line = scenario
                + ","
                + options.globalCount
                + ","
                + options.branchPerGlobal
                + ","
                + options.lockPerBranch
                + ","
                + options.writeWorkload
                + ","
                + options.dataPayloadProfile
                + ","
                + options.queryWorkload
                + ","
                + options.syncWrite
                + ","
                + options.enableRangeDelete
                + ","
                + options.warmupRounds
                + ","
                + options.measureRounds
                + ","
                + options.batchSize
                + ","
                + options.lockIndexScanBatchSize
                + ","
                + options.orphanCleanBatchLimit
                + ","
                + options.orphanCleanMaxBatches
                + ","
                + options.orphanCleanRoundSleepMillis
                + ","
                + options.queryIterationsPerRound
                + ","
                + options.queryLimit
                + ","
                + (options.runLabel == null ? "" : options.runLabel)
                + ","
                + options.compareOrder
                + ","
                + stats.ops()
                + ","
                + millis(stats.totalNanos())
                + ","
                + format(stats.opsPerSecond())
                + ","
                + millis(stats.percentile(50))
                + ","
                + millis(stats.percentile(95))
                + ","
                + millis(stats.percentile(99))
                + ","
                + footprint.sizeBytes
                + ","
                + footprint.fileCount
                + ","
                + footprint.sstFiles
                + ","
                + footprint.walFiles
                + ","
                + footprint.estimateLiveDataSizeBytes
                + ","
                + footprint.totalSstFilesSizeBytes
                + ","
                + footprint.pendingCompactionBytes
                + ","
                + footprint.globalEstimateKeys
                + ","
                + footprint.branchEstimateKeys
                + ","
                + footprint.lockEstimateKeys
                + ","
                + stats.rowsScanned()
                + ","
                + stats.rowsReturned()
                + ","
                + stats.rowsUpdated()
                + ","
                + stats.pointReads()
                + ","
                + stats.iteratorNext()
                + ","
                + stats.writeBatchBytes()
                + ","
                + stats.innerOperations()
                + ","
                + stats.rangeDeleteCount()
                + ","
                + stats.pointDeleteCount()
                + ","
                + stats.deleteBatchCount()
                + ","
                + stats.branchFanout()
                + ","
                + stats.lockFanout()
                + ","
                + configDigest(options)
                + ","
                + footprint.walSyncStats.getMode().configValue()
                + ","
                + options.walSyncIntervalMillis
                + ","
                + options.walSyncWriteThreshold
                + ","
                + footprint.walSyncStats.getSyncCount()
                + ","
                + footprint.walSyncStats.getSyncFailureCount()
                + ","
                + footprint.walSyncStats.getAvgSyncCostMillis()
                + ","
                + footprint.walSyncStats.getMaxSyncCostMillis()
                + ","
                + footprint.walSyncStats.getUnsyncedWriteRequests()
                + ","
                + footprint.walSyncStats.getMaxUnsyncedWriteRequests()
                + ","
                + footprint.walSyncStats.getUnsyncedMillis()
                + ","
                + footprint.walSyncStats.getMaxUnsyncedMillis()
                + ","
                + footprint.walSyncStats.getLatestSequenceNumber()
                + ","
                + footprint.walSyncStats.getLastSyncedSequenceNumber();
        System.out.println(line);
        if (csvLines != null) {
            csvLines.add(line);
        }
        logEmit(scenario, stats);
    }

    private static void printEnvironment(BenchmarkOptions options, Path rootPath, Path runPath, String runLabel) {
        System.out.println("RocksDB file mode benchmark" + (runLabel != null ? " [" + runLabel + "]" : ""));
        System.out.println("rootPath=" + rootPath);
        System.out.println("runPath=" + runPath);
        System.out.println("benchmarks=" + options.benchmarks);
        System.out.println("availableProcessors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("jdk=" + System.getProperty("java.version") + " " + System.getProperty("java.vm.name"));
        System.out.println("os=" + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("rocksdbJniVersion=" + rocksDbJniVersion());
        System.out.println("syncWrite=" + options.syncWrite);
        System.out.println("enableRangeDelete=" + options.enableRangeDelete);
        System.out.println("blockCacheSize=" + BenchmarkOptions.humanReadableSize(options.blockCacheSize));
        System.out.println("tuningProfile=" + options.tuningProfile);
        System.out.println("writeBufferSize=" + BenchmarkOptions.humanReadableSize(options.writeBufferSize));
        System.out.println("dbWriteBufferSize=" + BenchmarkOptions.humanReadableSize(options.dbWriteBufferSize));
        System.out.println(
                "globalWriteBufferSize=" + BenchmarkOptions.humanReadableSize(options.globalWriteBufferSize));
        System.out.println(
                "branchWriteBufferSize=" + BenchmarkOptions.humanReadableSize(options.branchWriteBufferSize));
        System.out.println("lockWriteBufferSize=" + BenchmarkOptions.humanReadableSize(options.lockWriteBufferSize));
        System.out.println("indexWriteBufferSize=" + BenchmarkOptions.humanReadableSize(options.indexWriteBufferSize));
        System.out.println(
                "metadataWriteBufferSize=" + BenchmarkOptions.humanReadableSize(options.metadataWriteBufferSize));
        System.out.println("maxTotalWalSize=" + BenchmarkOptions.humanReadableSize(options.maxTotalWalSize));
        System.out.println("maxWriteBufferNumber=" + options.maxWriteBufferNumber);
        System.out.println("minWriteBufferNumberToMerge=" + options.minWriteBufferNumberToMerge);
        System.out.println("maxBackgroundJobs=" + options.maxBackgroundJobs);
        System.out.println("maxOpenFiles=" + options.maxOpenFiles);
        System.out.println("targetFileSizeBase=" + BenchmarkOptions.humanReadableSize(options.targetFileSizeBase));
        System.out.println("level0FileNumCompactionTrigger=" + options.level0FileNumCompactionTrigger);
        System.out.println("level0SlowdownWritesTrigger=" + options.level0SlowdownWritesTrigger);
        System.out.println("level0StopWritesTrigger=" + options.level0StopWritesTrigger);
        System.out.println("enableStatistics=" + options.enableStatistics);
        System.out.println("optimizeFiltersForHits=" + options.optimizeFiltersForHits);
        System.out.println("compressionType=" + options.compressionType);
        System.out.println("walSyncMode=" + options.walSyncMode.configValue());
        System.out.println("walSyncIntervalMillis=" + options.walSyncIntervalMillis);
        System.out.println("walSyncWriteThreshold=" + options.walSyncWriteThreshold);
        System.out.println("walSyncOnShutdown=" + options.walSyncOnShutdown);
        System.out.println("walSyncWarnThresholdMillis=" + options.walSyncWarnThresholdMillis);
        System.out.println("globalCount=" + options.globalCount);
        System.out.println("branchPerGlobal=" + options.branchPerGlobal);
        System.out.println("lockPerBranch=" + options.lockPerBranch);
        System.out.println("writeWorkload=" + options.writeWorkload);
        System.out.println("warmupRounds=" + options.warmupRounds);
        System.out.println("measureRounds=" + options.measureRounds);
        System.out.println("batchSize=" + options.batchSize);
        System.out.println("lockIndexScanBatchSize=" + options.lockIndexScanBatchSize);
        System.out.println("dataPayloadProfile=" + options.dataPayloadProfile);
        System.out.println("queryWorkload=" + options.queryWorkload);
        System.out.println("orphanCleanBatchLimit=" + options.orphanCleanBatchLimit);
        System.out.println("orphanCleanMaxBatches=" + options.orphanCleanMaxBatches);
        System.out.println("orphanCleanRoundSleepMillis=" + options.orphanCleanRoundSleepMillis);
        System.out.println("queryIterationsPerRound=" + options.queryIterationsPerRound);
        System.out.println("queryLimit=" + options.queryLimit);
        System.out.println("repeatRuns=" + options.repeatRuns);
        System.out.println("compareOrder=" + options.compareOrder);
        System.out.println("sampleEvery=" + options.sampleEvery);
        System.out.println("cleanup=" + options.cleanup);
        System.out.println("seed=" + options.seed);
        System.out.println(
                "statusDistribution=" + (options.statusDistribution == null ? "" : options.statusDistribution));
        System.out.println("expiredRatio=" + options.expiredRatio);
        System.out.println("lockWorkload=" + options.lockWorkload);
        System.out.println("lockConflictRatio=" + options.lockConflictRatio);
        System.out.println("xidFanoutDistribution="
                + (options.xidFanoutDistribution == null ? "" : options.xidFanoutDistribution));
        if (options.compare != null) {
            System.out.println("compare=" + options.compare);
        }
        SystemMetrics mem = SystemMetrics.snapshot();
        System.out.printf(
                Locale.ROOT,
                "jvmHeapUsed=%.1fMB, jvmHeapMax=%.1fMB, gcCollections=%d, gcTimeMs=%d%n",
                mem.heapUsed / 1048576.0,
                mem.heapMax / 1048576.0,
                mem.gcCount,
                mem.gcTimeMs);
    }

    private static String rocksDbJniVersion() {
        Package rocksDbPackage = RocksDB.class.getPackage();
        String version = rocksDbPackage == null ? null : rocksDbPackage.getImplementationVersion();
        if (version != null) {
            return version;
        }
        try {
            java.security.CodeSource codeSource =
                    RocksDB.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return "unknown";
            }
            String fileName =
                    Paths.get(codeSource.getLocation().toURI()).getFileName().toString();
            String prefix = "rocksdbjni-";
            String suffix = ".jar";
            if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
                return fileName.substring(prefix.length(), fileName.length() - suffix.length());
            }
            return fileName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String configDigest(BenchmarkOptions options) {
        String raw = "syncWrite=" + options.syncWrite
                + ",enableRangeDelete=" + options.enableRangeDelete
                + ",blockCacheSize=" + options.blockCacheSize
                + ",writeBufferSize=" + options.writeBufferSize
                + ",dbWriteBufferSize=" + options.dbWriteBufferSize
                + ",globalWriteBufferSize=" + options.globalWriteBufferSize
                + ",branchWriteBufferSize=" + options.branchWriteBufferSize
                + ",lockWriteBufferSize=" + options.lockWriteBufferSize
                + ",indexWriteBufferSize=" + options.indexWriteBufferSize
                + ",metadataWriteBufferSize=" + options.metadataWriteBufferSize
                + ",maxTotalWalSize=" + options.maxTotalWalSize
                + ",maxWriteBufferNumber=" + options.maxWriteBufferNumber
                + ",minWriteBufferNumberToMerge=" + options.minWriteBufferNumberToMerge
                + ",maxBackgroundJobs=" + options.maxBackgroundJobs
                + ",maxOpenFiles=" + options.maxOpenFiles
                + ",targetFileSizeBase=" + options.targetFileSizeBase
                + ",level0FileNumCompactionTrigger=" + options.level0FileNumCompactionTrigger
                + ",level0SlowdownWritesTrigger=" + options.level0SlowdownWritesTrigger
                + ",level0StopWritesTrigger=" + options.level0StopWritesTrigger
                + ",enableStatistics=" + options.enableStatistics
                + ",optimizeFiltersForHits=" + options.optimizeFiltersForHits
                + ",compressionType=" + options.compressionType
                + ",walSyncMode=" + options.walSyncMode.configValue()
                + ",walSyncIntervalMillis=" + options.walSyncIntervalMillis
                + ",walSyncWriteThreshold=" + options.walSyncWriteThreshold
                + ",walSyncOnShutdown=" + options.walSyncOnShutdown
                + ",walSyncWarnThresholdMillis=" + options.walSyncWarnThresholdMillis
                + ",globalCount=" + options.globalCount
                + ",branchPerGlobal=" + options.branchPerGlobal
                + ",lockPerBranch=" + options.lockPerBranch
                + ",statusDistribution=" + options.statusDistribution
                + ",expiredRatio=" + options.expiredRatio
                + ",lockWorkload=" + options.lockWorkload
                + ",dataPayloadProfile=" + options.dataPayloadProfile
                + ",queryWorkload=" + options.queryWorkload
                + ",orphanCleanBatchLimit=" + options.orphanCleanBatchLimit
                + ",orphanCleanMaxBatches=" + options.orphanCleanMaxBatches
                + ",orphanCleanRoundSleepMillis=" + options.orphanCleanRoundSleepMillis
                + ",lockConflictRatio=" + options.lockConflictRatio
                + ",xidFanoutDistribution=" + options.xidFanoutDistribution;
        int hash = raw.hashCode();
        return String.format(Locale.ROOT, "%08x", hash);
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static int max(int... values) {
        int result = Integer.MIN_VALUE;
        for (int value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static double doubleValue(String value) {
        if (StringUtils.isBlank(value)) {
            return 0D;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0D;
        }
    }

    private static String runGroup(String runLabel) {
        if (StringUtils.isBlank(runLabel)) {
            return "all";
        }
        char first = Character.toUpperCase(runLabel.trim().charAt(0));
        if (first == 'A' || first == 'B' || first == 'R') {
            return Character.toString(first);
        }
        return runLabel.trim();
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new IllegalStateException(message + ", expected:" + expected + ", actual:" + actual);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(path)) {
            stream.forEach(paths::add);
        }
        Collections.sort(paths, Comparator.reverseOrder());
        for (Path current : paths) {
            Files.deleteIfExists(current);
        }
    }

    @SuppressWarnings("unchecked")
    private static void restoreEnvironment(Object originalEnvironment) throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }

    private interface BenchmarkTask {
        void run() throws Exception;
    }

    private interface MeasuredBenchmarkTask {
        RowMetrics run() throws Exception;
    }

    private static final class RowMetrics {
        private static final RowMetrics NONE = new RowMetrics(0L, 0L, 0L, 0L, 0L, 0L, 1L);

        private final long rowsScanned;
        private final long rowsReturned;
        private final long rowsUpdated;
        private final long pointReads;
        private final long iteratorNext;
        private final long writeBatchBytes;
        private final long innerOperations;
        private final long rangeDeleteCount;
        private final long pointDeleteCount;
        private final long deleteBatchCount;
        private final long branchFanout;
        private final long lockFanout;

        private RowMetrics(
                long rowsScanned,
                long rowsReturned,
                long rowsUpdated,
                long pointReads,
                long iteratorNext,
                long writeBatchBytes,
                long innerOperations) {
            this(
                    rowsScanned,
                    rowsReturned,
                    rowsUpdated,
                    pointReads,
                    iteratorNext,
                    writeBatchBytes,
                    innerOperations,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L);
        }

        private RowMetrics(
                long rowsScanned,
                long rowsReturned,
                long rowsUpdated,
                long pointReads,
                long iteratorNext,
                long writeBatchBytes,
                long innerOperations,
                long rangeDeleteCount,
                long pointDeleteCount,
                long deleteBatchCount,
                long branchFanout,
                long lockFanout) {
            this.rowsScanned = rowsScanned;
            this.rowsReturned = rowsReturned;
            this.rowsUpdated = rowsUpdated;
            this.pointReads = pointReads;
            this.iteratorNext = iteratorNext;
            this.writeBatchBytes = writeBatchBytes;
            this.innerOperations = innerOperations;
            this.rangeDeleteCount = rangeDeleteCount;
            this.pointDeleteCount = pointDeleteCount;
            this.deleteBatchCount = deleteBatchCount;
            this.branchFanout = branchFanout;
            this.lockFanout = lockFanout;
        }

        private static RowMetrics returned(long rowsReturned) {
            return pointReadReturned(rowsReturned, rowsReturned);
        }

        private static RowMetrics pointReadReturned(long pointReads, long rowsReturned) {
            return new RowMetrics(0L, rowsReturned, 0L, pointReads, 0L, 0L, 1L);
        }

        private static RowMetrics pointReads(long pointReads) {
            return new RowMetrics(0L, 0L, 0L, pointReads, 0L, 0L, 1L);
        }

        private static RowMetrics written(long rowsUpdated, long writeBatchBytes) {
            return new RowMetrics(0L, 0L, rowsUpdated, 0L, 0L, writeBatchBytes, 1L);
        }

        private static RowMetrics scanned(long rowsScanned) {
            return new RowMetrics(rowsScanned, 0L, 0L, 0L, rowsScanned, 0L, 1L);
        }

        private static RowMetrics updated(long rowsUpdated) {
            return written(rowsUpdated, rowsUpdated);
        }

        private static RowMetrics scannedAndReturned(long rowsScanned, long rowsReturned) {
            return new RowMetrics(rowsScanned, rowsReturned, 0L, 0L, rowsScanned, 0L, 1L);
        }

        private static RowMetrics fromScanStats(SessionScanStats scanStats, long returnedEstimate) {
            if (scanStats == null) {
                return scannedAndReturned(returnedEstimate, returnedEstimate);
            }
            return new RowMetrics(
                    scanStats.getRowsScanned(),
                    scanStats.getRowsReturned(),
                    0L,
                    scanStats.getPointReads(),
                    scanStats.getRowsScanned(),
                    0L,
                    1L);
        }

        private static RowMetrics scannedAndUpdated(long rowsScanned, long rowsUpdated) {
            return scannedPointReadAndUpdated(rowsScanned, rowsScanned, rowsUpdated, rowsUpdated);
        }

        private static RowMetrics scannedPointReadAndUpdated(
                long rowsScanned, long pointReads, long rowsUpdated, long writeBatchBytes) {
            return new RowMetrics(rowsScanned, 0L, rowsUpdated, pointReads, rowsScanned, writeBatchBytes, 1L);
        }

        private static RowMetrics globalRemove(
                boolean rangeDelete, long branchFanout, long rowsUpdated, long pointDeleteCount, long writeBatchBytes) {
            long rowsScanned = rangeDelete ? 0L : branchFanout;
            return new RowMetrics(
                    rowsScanned,
                    0L,
                    rowsUpdated,
                    0L,
                    rowsScanned,
                    writeBatchBytes,
                    1L,
                    rangeDelete ? 1L : 0L,
                    pointDeleteCount,
                    1L,
                    branchFanout,
                    0L);
        }

        private static RowMetrics lifecycleGlobalRemove(
                boolean rangeDelete,
                long branchFanout,
                long lockFanout,
                long rowsScanned,
                long pointDeleteCount,
                long deleteBatchCount,
                long writeBatchBytes) {
            return new RowMetrics(
                    rowsScanned,
                    0L,
                    pointDeleteCount,
                    0L,
                    rowsScanned,
                    writeBatchBytes,
                    deleteBatchCount,
                    rangeDelete ? 1L : 0L,
                    pointDeleteCount,
                    deleteBatchCount,
                    branchFanout,
                    lockFanout);
        }
    }

    private static final class BenchmarkSummary {
        private final String scenario;
        private final String runGroup;
        private final NumericSeries opsPerSecond = new NumericSeries();
        private final NumericSeries totalMs = new NumericSeries();
        private final NumericSeries p50Ms = new NumericSeries();
        private final NumericSeries p95Ms = new NumericSeries();
        private final NumericSeries p99Ms = new NumericSeries();
        private final NumericSeries rowsScanned = new NumericSeries();
        private final NumericSeries rowsReturned = new NumericSeries();
        private final NumericSeries rowsUpdated = new NumericSeries();
        private final NumericSeries pointReads = new NumericSeries();
        private final NumericSeries iteratorNext = new NumericSeries();
        private final NumericSeries writeBatchBytes = new NumericSeries();
        private final NumericSeries innerOperations = new NumericSeries();
        private final NumericSeries rangeDeleteCount = new NumericSeries();
        private final NumericSeries pointDeleteCount = new NumericSeries();
        private final NumericSeries deleteBatchCount = new NumericSeries();
        private final NumericSeries branchFanout = new NumericSeries();
        private final NumericSeries lockFanout = new NumericSeries();

        private BenchmarkSummary(String scenario, String runGroup) {
            this.scenario = scenario;
            this.runGroup = runGroup;
        }

        private void add(
                double opsPerSecondValue,
                double totalMsValue,
                double p50MsValue,
                double p95MsValue,
                double p99MsValue,
                double rowsScannedValue,
                double rowsReturnedValue,
                double rowsUpdatedValue,
                double pointReadsValue,
                double iteratorNextValue,
                double writeBatchBytesValue,
                double innerOperationsValue,
                double rangeDeleteCountValue,
                double pointDeleteCountValue,
                double deleteBatchCountValue,
                double branchFanoutValue,
                double lockFanoutValue) {
            opsPerSecond.add(opsPerSecondValue);
            totalMs.add(totalMsValue);
            p50Ms.add(p50MsValue);
            p95Ms.add(p95MsValue);
            p99Ms.add(p99MsValue);
            rowsScanned.add(rowsScannedValue);
            rowsReturned.add(rowsReturnedValue);
            rowsUpdated.add(rowsUpdatedValue);
            pointReads.add(pointReadsValue);
            iteratorNext.add(iteratorNextValue);
            writeBatchBytes.add(writeBatchBytesValue);
            innerOperations.add(innerOperationsValue);
            rangeDeleteCount.add(rangeDeleteCountValue);
            pointDeleteCount.add(pointDeleteCountValue);
            deleteBatchCount.add(deleteBatchCountValue);
            branchFanout.add(branchFanoutValue);
            lockFanout.add(lockFanoutValue);
        }

        private String toCsvLine() {
            return scenario
                    + ","
                    + runGroup
                    + ","
                    + opsPerSecond.count()
                    + ","
                    + format(opsPerSecond.mean())
                    + ","
                    + format(opsPerSecond.median())
                    + ","
                    + format(opsPerSecond.percentile(95))
                    + ","
                    + format(opsPerSecond.percentile(99))
                    + ","
                    + format(opsPerSecond.min())
                    + ","
                    + format(opsPerSecond.max())
                    + ","
                    + format(opsPerSecond.stddev())
                    + ","
                    + format(totalMs.mean())
                    + ","
                    + format(p50Ms.median())
                    + ","
                    + format(p95Ms.median())
                    + ","
                    + format(p99Ms.median())
                    + ","
                    + format(rowsScanned.mean())
                    + ","
                    + format(rowsReturned.mean())
                    + ","
                    + format(rowsUpdated.mean())
                    + ","
                    + format(pointReads.mean())
                    + ","
                    + format(iteratorNext.mean())
                    + ","
                    + format(writeBatchBytes.mean())
                    + ","
                    + format(innerOperations.mean())
                    + ","
                    + format(rangeDeleteCount.mean())
                    + ","
                    + format(pointDeleteCount.mean())
                    + ","
                    + format(deleteBatchCount.mean())
                    + ","
                    + format(branchFanout.mean())
                    + ","
                    + format(lockFanout.mean());
        }

        private Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("scenario", scenario);
            result.put("runGroup", runGroup);
            result.put("runCount", opsPerSecond.count());
            result.put("opsPerSecond", opsPerSecond.toMap());
            Map<String, Object> latencyMs = new LinkedHashMap<>();
            latencyMs.put("p50Median", p50Ms.median());
            latencyMs.put("p95Median", p95Ms.median());
            latencyMs.put("p99Median", p99Ms.median());
            result.put("latencyMs", latencyMs);
            Map<String, Object> rows = new LinkedHashMap<>();
            rows.put("scannedMean", rowsScanned.mean());
            rows.put("returnedMean", rowsReturned.mean());
            rows.put("updatedMean", rowsUpdated.mean());
            result.put("rows", rows);
            Map<String, Object> operations = new LinkedHashMap<>();
            operations.put("pointReadsMean", pointReads.mean());
            operations.put("iteratorNextMean", iteratorNext.mean());
            operations.put("writeBatchBytesMean", writeBatchBytes.mean());
            operations.put("rangeDeleteCountMean", rangeDeleteCount.mean());
            operations.put("pointDeleteCountMean", pointDeleteCount.mean());
            operations.put("deleteBatchCountMean", deleteBatchCount.mean());
            operations.put("branchFanoutMean", branchFanout.mean());
            operations.put("lockFanoutMean", lockFanout.mean());
            result.put("operations", operations);
            result.put("totalMsMean", totalMs.mean());
            result.put("innerOperationsMean", innerOperations.mean());
            return result;
        }
    }

    private static final class NumericSeries {
        private final List<Double> values = new ArrayList<>();

        private void add(double value) {
            values.add(value);
        }

        private int count() {
            return values.size();
        }

        private double mean() {
            if (values.isEmpty()) {
                return 0D;
            }
            double sum = 0D;
            for (double value : values) {
                sum += value;
            }
            return sum / values.size();
        }

        private double median() {
            if (values.isEmpty()) {
                return 0D;
            }
            List<Double> sorted = sortedValues();
            int middle = sorted.size() / 2;
            if (sorted.size() % 2 == 0) {
                return (sorted.get(middle - 1) + sorted.get(middle)) / 2D;
            }
            return sorted.get(middle);
        }

        private double percentile(int percentile) {
            if (values.isEmpty()) {
                return 0D;
            }
            List<Double> sorted = sortedValues();
            int index = (int) Math.ceil(sorted.size() * percentile / 100.0D) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }

        private double min() {
            if (values.isEmpty()) {
                return 0D;
            }
            double result = Double.MAX_VALUE;
            for (double value : values) {
                result = Math.min(result, value);
            }
            return result;
        }

        private double max() {
            if (values.isEmpty()) {
                return 0D;
            }
            double result = -Double.MAX_VALUE;
            for (double value : values) {
                result = Math.max(result, value);
            }
            return result;
        }

        private double stddev() {
            if (values.isEmpty()) {
                return 0D;
            }
            double mean = mean();
            double sumSquares = 0D;
            for (double value : values) {
                double delta = value - mean;
                sumSquares += delta * delta;
            }
            return Math.sqrt(sumSquares / values.size());
        }

        private List<Double> sortedValues() {
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            return sorted;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mean", mean());
            result.put("median", median());
            result.put("p95", percentile(95));
            result.put("p99", percentile(99));
            result.put("min", min());
            result.put("max", max());
            result.put("stddev", stddev());
            return result;
        }
    }

    private static final class BenchmarkDataSet {
        private final List<GlobalSession> globalSessions;
        private final Map<String, List<BranchSession>> branchSessions;

        private BenchmarkDataSet(List<GlobalSession> globalSessions, Map<String, List<BranchSession>> branchSessions) {
            this.globalSessions = globalSessions;
            this.branchSessions = branchSessions;
        }

        private static BenchmarkDataSet create(BenchmarkOptions options, int round) {
            List<GlobalSession> globals = new ArrayList<>(options.globalCount);
            Map<String, List<BranchSession>> branches = new LinkedHashMap<>();
            long now = System.currentTimeMillis();
            long baseBeginTime = now - options.globalCount - round - Math.floorMod(options.seed, 1000L);
            long expiredBaseBeginTime = now
                    - SESSION_TIMEOUT_MILLIS
                    - options.globalCount
                    - round
                    - Math.floorMod(options.seed, 1000L)
                    - 1L;
            long activeBaseBeginTime = now + SESSION_TIMEOUT_MILLIS * ACTIVE_BENCHMARK_WINDOW_MULTIPLIER;
            for (int i = 0; i < options.globalCount; i++) {
                long beginTime;
                if (options.expiredRatio > 0D) {
                    long timeOffset = Math.floorMod(i, 1000);
                    beginTime = options.isExpiredIndex(i)
                            ? expiredBaseBeginTime - timeOffset
                            : activeBaseBeginTime + timeOffset;
                } else {
                    beginTime = baseBeginTime + i;
                }
                GlobalSession globalSession =
                        globalSession(i, round, options.seed, beginTime, options.statusForIndex(i));
                if (options.usesProductionPayload()) {
                    globalSession.setApplicationData(productionApplicationData(i, round, options.seed));
                }
                globals.add(globalSession);
                int branchCount = options.branchCountForIndex(i);
                List<BranchSession> globalBranches = new ArrayList<>(branchCount);
                for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
                    globalBranches.add(branchSession(globalSession, i, branchIndex, options.lockPerBranch));
                }
                branches.put(globalSession.getXid(), globalBranches);
            }
            return new BenchmarkDataSet(globals, branches);
        }

        private GlobalSession pick(int index) {
            return globalSessions.get(Math.floorMod(index, globalSessions.size()));
        }

        private List<BranchSession> branchesOf(GlobalSession globalSession) {
            Collection<BranchSession> branches = branchSessions.get(globalSession.getXid());
            if (branches == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(branches);
        }

        private List<BranchSession> allBranches() {
            List<BranchSession> branches = new ArrayList<>();
            for (List<BranchSession> globalBranches : branchSessions.values()) {
                branches.addAll(globalBranches);
            }
            return branches;
        }

        private int totalBranchCount() {
            int count = 0;
            for (List<BranchSession> globalBranches : branchSessions.values()) {
                count += globalBranches.size();
            }
            return count;
        }

        private int countByStatus(GlobalStatus status) {
            int count = 0;
            for (GlobalSession globalSession : globalSessions) {
                if (globalSession.getStatus() == status) {
                    count++;
                }
            }
            return count;
        }

        private int countByStatusAndOverTime(GlobalStatus status, long overTimeAliveMills) {
            int count = 0;
            long now = System.currentTimeMillis();
            for (GlobalSession globalSession : globalSessions) {
                if (globalSession.getStatus() == status && now - globalSession.getBeginTime() > overTimeAliveMills) {
                    count++;
                }
            }
            return count;
        }

        private BranchSession conflictBranch(BranchSession holder) {
            BranchSession branchSession = new BranchSession(BranchType.AT);
            long transactionId = holder.getTransactionId() + 10_000_000L;
            branchSession.setXid("127.0.0.1:8091:" + transactionId);
            branchSession.setTransactionId(transactionId);
            branchSession.setBranchId(holder.getBranchId() + 10_000_000L);
            branchSession.setStatus(BranchStatus.Registered);
            branchSession.setResourceId(holder.getResourceId());
            branchSession.setLockKey(holder.getLockKey());
            return branchSession;
        }

        private static GlobalSession globalSession(
                int index, int round, long seed, long beginTime, GlobalStatus status) {
            GlobalSession globalSession = new GlobalSession(
                    "benchmark-app", "benchmark-group", "phase4-tx-" + seed + "-" + round + "-" + index, 60000);
            globalSession.setStatus(status);
            globalSession.setBeginTime(beginTime);
            return globalSession;
        }

        private static String productionApplicationData(int index, int round, long seed) {
            return "{\"bizType\":\"ORDER_CREATE\",\"userId\":\"U" + (100000 + index % 50000)
                    + "\",\"merchantId\":\"M" + (200000 + index % 30000)
                    + "\",\"orderId\":\"ORD" + seed + round + index
                    + "\",\"amount\":" + (100 + index % 9900) + "." + (index % 100)
                    + ",\"currency\":\"CNY\",\"channel\":\"APP\",\"retryCount\":0"
                    + ",\"timeoutNotify\":\"http://callback.internal/notify\","
                    + "\"rollbackPolicy\":\"SAGA\",\"priority\":\"NORMAL\","
                    + "\"traceId\":\"trace-" + Long.toHexString(seed) + "-" + Integer.toHexString(index)
                    + "\",\"extra\":{\"source\":\"benchmark\",\"region\":\"cn-east-1\"}}";
        }

        private static BranchSession branchSession(
                GlobalSession globalSession, int globalIndex, int branchIndex, int lockPerBranch) {
            BranchSession branchSession = new BranchSession(BranchType.AT);
            branchSession.setXid(globalSession.getXid());
            branchSession.setTransactionId(globalSession.getTransactionId());
            branchSession.setBranchId(globalIndex * 1000L + branchIndex + 1L);
            branchSession.setStatus(BranchStatus.Registered);
            branchSession.setResourceId("jdbc:mysql://127.0.0.1/benchmark");
            branchSession.setLockKey(lockKey(globalIndex, branchIndex, lockPerBranch));
            return branchSession;
        }

        private static String lockKey(int globalIndex, int branchIndex, int lockPerBranch) {
            if (lockPerBranch <= 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder("benchmark_table:");
            for (int i = 0; i < lockPerBranch; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(globalIndex)
                        .append('_')
                        .append(branchIndex)
                        .append('_')
                        .append(i);
            }
            return builder.toString();
        }
    }

    private static final class OperationStats {
        private final int sampleEvery;
        private long ops;
        private long totalNanos;
        private long rowsScanned;
        private long rowsReturned;
        private long rowsUpdated;
        private long pointReads;
        private long iteratorNext;
        private long writeBatchBytes;
        private long innerOperations;
        private long rangeDeleteCount;
        private long pointDeleteCount;
        private long deleteBatchCount;
        private long branchFanout;
        private long lockFanout;
        private long[] samples = new long[128];
        private int sampleCount;

        private OperationStats(int sampleEvery) {
            this.sampleEvery = Math.max(1, sampleEvery);
        }

        private void record(long nanos) {
            record(nanos, RowMetrics.NONE);
        }

        private void record(long nanos, RowMetrics rows) {
            ops++;
            totalNanos += nanos;
            RowMetrics actualRows = rows == null ? RowMetrics.NONE : rows;
            rowsScanned += actualRows.rowsScanned;
            rowsReturned += actualRows.rowsReturned;
            rowsUpdated += actualRows.rowsUpdated;
            pointReads += actualRows.pointReads;
            iteratorNext += actualRows.iteratorNext;
            writeBatchBytes += actualRows.writeBatchBytes;
            innerOperations += actualRows.innerOperations;
            rangeDeleteCount += actualRows.rangeDeleteCount;
            pointDeleteCount += actualRows.pointDeleteCount;
            deleteBatchCount += actualRows.deleteBatchCount;
            branchFanout += actualRows.branchFanout;
            lockFanout += actualRows.lockFanout;
            if (ops % sampleEvery == 0) {
                addSample(nanos);
            }
        }

        private long ops() {
            return ops;
        }

        private int sampleCount() {
            return sampleCount;
        }

        private long totalNanos() {
            return totalNanos;
        }

        private long rowsScanned() {
            return rowsScanned;
        }

        private long rowsReturned() {
            return rowsReturned;
        }

        private long rowsUpdated() {
            return rowsUpdated;
        }

        private long innerOperations() {
            return innerOperations;
        }

        private long pointReads() {
            return pointReads;
        }

        private long iteratorNext() {
            return iteratorNext;
        }

        private long writeBatchBytes() {
            return writeBatchBytes;
        }

        private long rangeDeleteCount() {
            return rangeDeleteCount;
        }

        private long pointDeleteCount() {
            return pointDeleteCount;
        }

        private long deleteBatchCount() {
            return deleteBatchCount;
        }

        private long branchFanout() {
            return branchFanout;
        }

        private long lockFanout() {
            return lockFanout;
        }

        private double opsPerSecond() {
            if (totalNanos <= 0L) {
                return 0D;
            }
            return ops * 1_000_000_000.0D / totalNanos;
        }

        private long percentile(int percentile) {
            if (sampleCount == 0) {
                return ops == 0L ? 0L : totalNanos / ops;
            }
            long[] sortedSamples = Arrays.copyOf(samples, sampleCount);
            Arrays.sort(sortedSamples);
            int index = (int) Math.ceil(sortedSamples.length * percentile / 100.0D) - 1;
            return sortedSamples[Math.max(0, Math.min(index, sortedSamples.length - 1))];
        }

        private void addSample(long nanos) {
            if (sampleCount == samples.length) {
                samples = Arrays.copyOf(samples, samples.length * 2);
            }
            samples[sampleCount++] = nanos;
        }
    }

    private static final class DbFootprint {
        private final long sizeBytes;
        private final long fileCount;
        private final long sstFiles;
        private final long walFiles;
        private final long estimateLiveDataSizeBytes;
        private final long totalSstFilesSizeBytes;
        private final long pendingCompactionBytes;
        private final long globalEstimateKeys;
        private final long branchEstimateKeys;
        private final long lockEstimateKeys;
        private final RocksDBWalSyncStats walSyncStats;

        private DbFootprint(
                long sizeBytes,
                long fileCount,
                long sstFiles,
                long walFiles,
                long estimateLiveDataSizeBytes,
                long totalSstFilesSizeBytes,
                long pendingCompactionBytes,
                long globalEstimateKeys,
                long branchEstimateKeys,
                long lockEstimateKeys,
                RocksDBWalSyncStats walSyncStats) {
            this.sizeBytes = sizeBytes;
            this.fileCount = fileCount;
            this.sstFiles = sstFiles;
            this.walFiles = walFiles;
            this.estimateLiveDataSizeBytes = estimateLiveDataSizeBytes;
            this.totalSstFilesSizeBytes = totalSstFilesSizeBytes;
            this.pendingCompactionBytes = pendingCompactionBytes;
            this.globalEstimateKeys = globalEstimateKeys;
            this.branchEstimateKeys = branchEstimateKeys;
            this.lockEstimateKeys = lockEstimateKeys;
            this.walSyncStats = walSyncStats == null ? RocksDBWalSyncStats.NONE : walSyncStats;
        }

        private static DbFootprint from(Path path) throws IOException {
            return from(path, RocksDBWalSyncStats.NONE);
        }

        private static DbFootprint from(Path path, RocksDBWalSyncStats walSyncStats) throws IOException {
            if (path == null || !Files.exists(path)) {
                return empty();
            }
            final long[] values = new long[4];
            try (Stream<Path> stream = Files.walk(path)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    values[0] += size(file);
                    values[1]++;
                    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (name.endsWith(".sst")) {
                        values[2]++;
                    }
                    if (name.endsWith(".log")) {
                        values[3]++;
                    }
                });
            }
            RocksDBStoreDiagnostics diagnostics = diagnostics(path);
            return new DbFootprint(
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    diagnostics.getProperty(RocksDBStoreDiagnostics.ESTIMATE_LIVE_DATA_SIZE),
                    diagnostics.getProperty(RocksDBStoreDiagnostics.TOTAL_SST_FILES_SIZE),
                    diagnostics.getProperty(RocksDBStoreDiagnostics.ESTIMATE_PENDING_COMPACTION_BYTES),
                    diagnostics.getColumnFamilyProperty(
                            RocksDBColumnFamily.GLOBAL_SESSION, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS),
                    diagnostics.getColumnFamilyProperty(
                            RocksDBColumnFamily.BRANCH_SESSION, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS),
                    diagnostics.getColumnFamilyProperty(
                            RocksDBColumnFamily.LOCK, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS),
                    walSyncStats);
        }

        private static DbFootprint empty() {
            return new DbFootprint(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, RocksDBWalSyncStats.NONE);
        }

        private static RocksDBStoreDiagnostics diagnostics(Path path) {
            try (RocksDBStoreEngine engine = open(path, false)) {
                return engine.diagnostics();
            } catch (Exception e) {
                return RocksDBStoreEngine.closedDiagnostics();
            }
        }

        private static long size(Path file) {
            try {
                return Files.size(file);
            } catch (IOException e) {
                return 0L;
            }
        }
    }

    private static final class StatusWeight {
        private final GlobalStatus status;
        private final int weight;

        private StatusWeight(GlobalStatus status, int weight) {
            this.status = status;
            this.weight = weight;
        }
    }

    private static final class IntWeight {
        private final int value;
        private final int weight;

        private IntWeight(int value, int weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private static final class LockKeyEstimate {
        private final byte[] lockKey;
        private final byte[] indexKey;
        private final byte[] lockValue;

        private LockKeyEstimate(byte[] lockKey, byte[] indexKey, byte[] lockValue) {
            this.lockKey = lockKey;
            this.indexKey = indexKey;
            this.lockValue = lockValue;
        }
    }

    private static final class BenchmarkOptions {
        private final int globalCount;
        private final int branchPerGlobal;
        private final int lockPerBranch;
        private final String writeWorkload;
        private final boolean syncWrite;
        private final boolean enableRangeDelete;
        private final long blockCacheSize;
        private final long writeBufferSize;
        private final long dbWriteBufferSize;
        private final long globalWriteBufferSize;
        private final long branchWriteBufferSize;
        private final long lockWriteBufferSize;
        private final long indexWriteBufferSize;
        private final long metadataWriteBufferSize;
        private final long maxTotalWalSize;
        private final int maxWriteBufferNumber;
        private final int minWriteBufferNumberToMerge;
        private final int maxBackgroundJobs;
        private final int maxOpenFiles;
        private final long targetFileSizeBase;
        private final int level0FileNumCompactionTrigger;
        private final int level0SlowdownWritesTrigger;
        private final int level0StopWritesTrigger;
        private final boolean enableStatistics;
        private final boolean optimizeFiltersForHits;
        private final String compressionType;
        private final RocksDBWalSyncMode walSyncMode;
        private final int walSyncIntervalMillis;
        private final long walSyncWriteThreshold;
        private final boolean walSyncOnShutdown;
        private final int walSyncWarnThresholdMillis;
        private final RocksDBWalSyncMode walSyncCompareMode;
        private final boolean cleanup;
        private final int warmupRounds;
        private final int measureRounds;
        private final int batchSize;
        private final int lockIndexScanBatchSize;
        private final int queryIterationsPerRound;
        private final int queryLimit;
        private final int repeatRuns;
        private final String compareOrder;
        private final String runLabel;
        private final int sampleEvery;
        private final long seed;
        private final String dbPath;
        private final Set<String> benchmarks;
        private final String compare;
        private final String tuningProfile;
        private final String statusDistribution;
        private final List<StatusWeight> parsedStatusDistribution;
        private final double expiredRatio;
        private final String lockWorkload;
        private final Set<String> lockWorkloadOperations;
        private final double lockConflictRatio;
        private final String xidFanoutDistribution;
        private final List<IntWeight> parsedXidFanoutDistribution;
        private String dataPayloadProfile = DATA_PAYLOAD_PROFILE_PRODUCTION;
        private String queryWorkload = QUERY_WORKLOAD_ALL;
        private int orphanCleanBatchLimit = CLEAN_ORPHAN_BATCHED_BATCH_LIMIT;
        private int orphanCleanMaxBatches = CLEAN_ORPHAN_BATCHED_MAX_BATCHES;
        private long orphanCleanRoundSleepMillis = CLEAN_ORPHAN_BATCHED_ROUND_SLEEP_MILLIS;

        private BenchmarkOptions(
                int globalCount,
                int branchPerGlobal,
                int lockPerBranch,
                boolean syncWrite,
                boolean enableRangeDelete,
                long blockCacheSize,
                boolean cleanup,
                int warmupRounds,
                int measureRounds,
                int batchSize,
                int sampleEvery,
                long seed,
                String dbPath,
                Set<String> benchmarks,
                String compare) {
            this(
                    globalCount,
                    branchPerGlobal,
                    lockPerBranch,
                    WRITE_WORKLOAD_FULL,
                    syncWrite,
                    enableRangeDelete,
                    blockCacheSize,
                    0L,
                    0,
                    0,
                    0,
                    0,
                    0L,
                    0,
                    0,
                    0,
                    false,
                    false,
                    null,
                    RocksDBWalSyncMode.NONE,
                    2000,
                    10L,
                    true,
                    1000,
                    RocksDBWalSyncMode.NONE,
                    cleanup,
                    warmupRounds,
                    measureRounds,
                    batchSize,
                    1024,
                    batchSize,
                    0,
                    1,
                    "AB",
                    null,
                    sampleEvery,
                    seed,
                    dbPath,
                    benchmarks,
                    compare,
                    "baseline",
                    null,
                    0D,
                    "full",
                    1D,
                    null,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L);
        }

        private BenchmarkOptions(
                int globalCount,
                int branchPerGlobal,
                int lockPerBranch,
                String writeWorkload,
                boolean syncWrite,
                boolean enableRangeDelete,
                long blockCacheSize,
                long writeBufferSize,
                int maxWriteBufferNumber,
                int minWriteBufferNumberToMerge,
                int maxBackgroundJobs,
                int maxOpenFiles,
                long targetFileSizeBase,
                int level0FileNumCompactionTrigger,
                int level0SlowdownWritesTrigger,
                int level0StopWritesTrigger,
                boolean enableStatistics,
                boolean optimizeFiltersForHits,
                String compressionType,
                RocksDBWalSyncMode walSyncMode,
                int walSyncIntervalMillis,
                long walSyncWriteThreshold,
                boolean walSyncOnShutdown,
                int walSyncWarnThresholdMillis,
                RocksDBWalSyncMode walSyncCompareMode,
                boolean cleanup,
                int warmupRounds,
                int measureRounds,
                int batchSize,
                int lockIndexScanBatchSize,
                int queryIterationsPerRound,
                int queryLimit,
                int repeatRuns,
                String compareOrder,
                String runLabel,
                int sampleEvery,
                long seed,
                String dbPath,
                Set<String> benchmarks,
                String compare,
                String tuningProfile,
                String statusDistribution,
                double expiredRatio,
                String lockWorkload,
                double lockConflictRatio,
                String xidFanoutDistribution,
                long dbWriteBufferSize,
                long globalWriteBufferSize,
                long branchWriteBufferSize,
                long lockWriteBufferSize,
                long indexWriteBufferSize,
                long metadataWriteBufferSize,
                long maxTotalWalSize) {
            this.globalCount = positive(globalCount, "globalCount");
            this.branchPerGlobal = nonNegative(branchPerGlobal, "branchPerGlobal");
            this.lockPerBranch = nonNegative(lockPerBranch, "lockPerBranch");
            this.writeWorkload = normalizeWriteWorkload(writeWorkload);
            this.syncWrite = syncWrite;
            this.enableRangeDelete = enableRangeDelete;
            this.blockCacheSize = nonNegative(blockCacheSize, "blockCacheSize");
            this.writeBufferSize = nonNegative(writeBufferSize, "writeBufferSize");
            this.dbWriteBufferSize = nonNegative(dbWriteBufferSize, "dbWriteBufferSize");
            this.globalWriteBufferSize = nonNegative(globalWriteBufferSize, "globalWriteBufferSize");
            this.branchWriteBufferSize = nonNegative(branchWriteBufferSize, "branchWriteBufferSize");
            this.lockWriteBufferSize = nonNegative(lockWriteBufferSize, "lockWriteBufferSize");
            this.indexWriteBufferSize = nonNegative(indexWriteBufferSize, "indexWriteBufferSize");
            this.metadataWriteBufferSize = nonNegative(metadataWriteBufferSize, "metadataWriteBufferSize");
            this.maxTotalWalSize = nonNegative(maxTotalWalSize, "maxTotalWalSize");
            this.maxWriteBufferNumber = nonNegative(maxWriteBufferNumber, "maxWriteBufferNumber");
            this.minWriteBufferNumberToMerge = nonNegative(minWriteBufferNumberToMerge, "minWriteBufferNumberToMerge");
            this.maxBackgroundJobs = nonNegative(maxBackgroundJobs, "maxBackgroundJobs");
            this.maxOpenFiles = nonNegative(maxOpenFiles, "maxOpenFiles");
            this.targetFileSizeBase = nonNegative(targetFileSizeBase, "targetFileSizeBase");
            this.level0FileNumCompactionTrigger =
                    nonNegative(level0FileNumCompactionTrigger, "level0FileNumCompactionTrigger");
            this.level0SlowdownWritesTrigger = nonNegative(level0SlowdownWritesTrigger, "level0SlowdownWritesTrigger");
            this.level0StopWritesTrigger = nonNegative(level0StopWritesTrigger, "level0StopWritesTrigger");
            this.enableStatistics = enableStatistics;
            this.optimizeFiltersForHits = optimizeFiltersForHits;
            this.compressionType = normalizeNullable(compressionType);
            this.walSyncMode = walSyncMode == null ? RocksDBWalSyncMode.NONE : walSyncMode;
            this.walSyncIntervalMillis = positive(walSyncIntervalMillis, "walSyncIntervalMillis");
            this.walSyncWriteThreshold = positive(walSyncWriteThreshold, "walSyncWriteThreshold");
            this.walSyncOnShutdown = walSyncOnShutdown;
            this.walSyncWarnThresholdMillis = positive(walSyncWarnThresholdMillis, "walSyncWarnThresholdMillis");
            this.walSyncCompareMode = walSyncCompareMode == null ? this.walSyncMode : walSyncCompareMode;
            this.cleanup = cleanup;
            this.warmupRounds = nonNegative(warmupRounds, "warmupRounds");
            this.measureRounds = positive(measureRounds, "measureRounds");
            this.batchSize = positive(batchSize, "batchSize");
            this.lockIndexScanBatchSize = positive(lockIndexScanBatchSize, "lockIndexScanBatchSize");
            this.queryIterationsPerRound = positive(queryIterationsPerRound, "queryIterationsPerRound");
            this.queryLimit = nonNegative(queryLimit, "queryLimit");
            this.repeatRuns = positive(repeatRuns, "repeatRuns");
            this.compareOrder = normalizeCompareOrder(compareOrder);
            this.runLabel = normalizeNullable(runLabel);
            this.sampleEvery = positive(sampleEvery, "sampleEvery");
            this.seed = seed;
            this.dbPath = dbPath;
            this.benchmarks = benchmarks;
            this.compare = compare;
            this.tuningProfile = StringUtils.isBlank(tuningProfile) ? "baseline" : tuningProfile.trim();
            this.statusDistribution = normalizeNullable(statusDistribution);
            this.parsedStatusDistribution = parseStatusDistribution(this.statusDistribution);
            this.expiredRatio = ratio(expiredRatio, "expiredRatio");
            this.lockWorkload = normalizeLockWorkload(lockWorkload);
            this.lockWorkloadOperations = parseLockWorkload(this.lockWorkload);
            this.lockConflictRatio = ratio(lockConflictRatio, "lockConflictRatio");
            this.xidFanoutDistribution = normalizeNullable(xidFanoutDistribution);
            this.parsedXidFanoutDistribution =
                    parseIntDistribution(this.xidFanoutDistribution, "xidFanoutDistribution");
        }

        private static BenchmarkOptions parse(String[] args) {
            Map<String, String> values = parseArgs(args);
            String compare = stringValue(values, "compare", null);
            int batchSize = intValue(values, "batchSize", 100);
            String defaultWalSyncMode = "walSyncMode".equals(compare) ? "periodic" : "none";
            RocksDBWalSyncMode requestedWalSyncMode =
                    RocksDBWalSyncMode.of(stringValue(values, "walSyncMode", defaultWalSyncMode));
            RocksDBWalSyncMode effectiveWalSyncMode =
                    "walSyncMode".equals(compare) ? RocksDBWalSyncMode.NONE : requestedWalSyncMode;
            BenchmarkOptions options = new BenchmarkOptions(
                    intValue(values, "globalCount", 1000),
                    intValue(values, "branchPerGlobal", 2),
                    intValue(values, "lockPerBranch", 2),
                    stringValue(values, "writeWorkload", WRITE_WORKLOAD_FULL),
                    booleanValue(values, "syncWrite", false),
                    booleanValue(values, "enableRangeDelete", true),
                    parseSizeOption(values, "blockCacheSize", 0L),
                    parseSizeOption(values, "writeBufferSize", 0L),
                    intValue(values, "maxWriteBufferNumber", 0),
                    intValue(values, "minWriteBufferNumberToMerge", 0),
                    intValue(values, "maxBackgroundJobs", 0),
                    intValue(values, "maxOpenFiles", 0),
                    parseSizeOption(values, "targetFileSizeBase", 0L),
                    intValue(values, "level0FileNumCompactionTrigger", 0),
                    intValue(values, "level0SlowdownWritesTrigger", 0),
                    intValue(values, "level0StopWritesTrigger", 0),
                    booleanValue(values, "enableStatistics", false),
                    booleanValue(values, "optimizeFiltersForHits", false),
                    stringValue(values, "compressionType", null),
                    effectiveWalSyncMode,
                    intValue(values, "walSyncIntervalMillis", 2000),
                    longValue(values, "walSyncWriteThreshold", 10L),
                    booleanValue(values, "walSyncOnShutdown", true),
                    intValue(values, "walSyncWarnThresholdMillis", 1000),
                    requestedWalSyncMode,
                    booleanValue(values, "cleanup", false),
                    intValue(values, "warmupRounds", 1),
                    intValue(values, "measureRounds", 3),
                    batchSize,
                    intValue(values, "lockIndexScanBatchSize", 1024),
                    intValue(values, "queryIterationsPerRound", batchSize),
                    intValue(values, "queryLimit", 0),
                    intValue(values, "repeatRuns", 1),
                    stringValue(values, "compareOrder", "AB"),
                    null,
                    intValue(values, "sampleEvery", 1),
                    longValue(values, "seed", 20260606L),
                    stringValue(values, "dbPath", null),
                    parseBenchmarks(stringValue(values, "benchmark", "all")),
                    compare,
                    stringValue(values, "tuningProfile", "baseline"),
                    stringValue(values, "statusDistribution", null),
                    ratioValue(values, "expiredRatio", 0D),
                    stringValue(values, "lockWorkload", "full"),
                    ratioValue(values, "lockConflictRatio", 1D),
                    stringValue(values, "xidFanoutDistribution", null),
                    parseSizeOption(values, "dbWriteBufferSize", 0L),
                    parseSizeOption(values, "globalWriteBufferSize", 0L),
                    parseSizeOption(values, "branchWriteBufferSize", 0L),
                    parseSizeOption(values, "lockWriteBufferSize", 0L),
                    parseSizeOption(values, "indexWriteBufferSize", 0L),
                    parseSizeOption(values, "metadataWriteBufferSize", 0L),
                    parseSizeOption(values, "maxTotalWalSize", 0L));
            BenchmarkOptions resolved = options;
            if (options.compare == null || "explicitR4".equals(options.compare)) {
                resolved = options.withTuningProfile(options.tuningProfile).withExplicitR4Overrides(values);
            }
            return resolved.withExecutionProfiles(
                    stringValue(values, "dataPayloadProfile", DATA_PAYLOAD_PROFILE_PRODUCTION),
                    stringValue(values, "queryWorkload", QUERY_WORKLOAD_ALL),
                    intValue(values, "orphanCleanBatchLimit", CLEAN_ORPHAN_BATCHED_BATCH_LIMIT),
                    intValue(values, "orphanCleanMaxBatches", CLEAN_ORPHAN_BATCHED_MAX_BATCHES),
                    longValue(values, "orphanCleanRoundSleepMillis", CLEAN_ORPHAN_BATCHED_ROUND_SLEEP_MILLIS));
        }

        private BenchmarkOptions withExecutionProfiles(
                String dataPayloadProfile,
                String queryWorkload,
                int orphanCleanBatchLimit,
                int orphanCleanMaxBatches,
                long orphanCleanRoundSleepMillis) {
            this.dataPayloadProfile = normalizeDataPayloadProfile(dataPayloadProfile);
            this.queryWorkload = normalizeQueryWorkload(queryWorkload);
            this.orphanCleanBatchLimit = positive(orphanCleanBatchLimit, "orphanCleanBatchLimit");
            this.orphanCleanMaxBatches = positive(orphanCleanMaxBatches, "orphanCleanMaxBatches");
            this.orphanCleanRoundSleepMillis = nonNegative(orphanCleanRoundSleepMillis, "orphanCleanRoundSleepMillis");
            return this;
        }

        private BenchmarkOptions copyExecutionProfilesFrom(BenchmarkOptions source) {
            return withExecutionProfiles(
                    source.dataPayloadProfile,
                    source.queryWorkload,
                    source.orphanCleanBatchLimit,
                    source.orphanCleanMaxBatches,
                    source.orphanCleanRoundSleepMillis);
        }

        private GlobalStatus statusForIndex(int index) {
            if (parsedStatusDistribution.isEmpty()) {
                return STATUSES[Math.floorMod(index, STATUSES.length)];
            }
            int totalWeight = 0;
            for (StatusWeight statusWeight : parsedStatusDistribution) {
                totalWeight += statusWeight.weight;
            }
            int slot = Math.floorMod(index, totalWeight);
            int cumulative = 0;
            for (StatusWeight statusWeight : parsedStatusDistribution) {
                cumulative += statusWeight.weight;
                if (slot < cumulative) {
                    return statusWeight.status;
                }
            }
            return parsedStatusDistribution.get(parsedStatusDistribution.size() - 1).status;
        }

        private int branchCountForIndex(int index) {
            if (parsedXidFanoutDistribution.isEmpty()) {
                return branchPerGlobal;
            }
            return weightedInt(parsedXidFanoutDistribution, index);
        }

        private boolean isExpiredIndex(int index) {
            return includeByRatio(index, expiredRatio);
        }

        private boolean shouldRunLockConflict(int index) {
            return includeByRatio(index, lockConflictRatio);
        }

        private boolean lockWorkloadIncludes(String operation) {
            return lockWorkloadOperations.contains(normalizeLockOperation(operation));
        }

        private boolean isFullWriteWorkload() {
            return WRITE_WORKLOAD_FULL.equals(writeWorkload);
        }

        private boolean usesProductionPayload() {
            return DATA_PAYLOAD_PROFILE_PRODUCTION.equals(dataPayloadProfile);
        }

        private boolean queryWorkloadIncludes(String operation) {
            return QUERY_WORKLOAD_ALL.equals(queryWorkload) || queryWorkload.equals(operation);
        }

        private boolean includeByRatio(int index, double ratioValue) {
            if (ratioValue <= 0D) {
                return false;
            }
            if (ratioValue >= 1D) {
                return true;
            }
            int selected = (int) Math.round(globalCount * ratioValue);
            if (selected <= 0) {
                return false;
            }
            return Math.floorMod(index, globalCount) < selected;
        }

        private static int weightedInt(List<IntWeight> weights, int index) {
            int totalWeight = 0;
            for (IntWeight weight : weights) {
                totalWeight += weight.weight;
            }
            int slot = Math.floorMod(index, totalWeight);
            int cumulative = 0;
            for (IntWeight weight : weights) {
                cumulative += weight.weight;
                if (slot < cumulative) {
                    return weight.value;
                }
            }
            return weights.get(weights.size() - 1).value;
        }

        private BenchmarkOptions withAtLeastOneLock() {
            return new BenchmarkOptions(
                            globalCount,
                            Math.max(1, branchPerGlobal),
                            Math.max(1, lockPerBranch),
                            writeWorkload,
                            syncWrite,
                            enableRangeDelete,
                            blockCacheSize,
                            writeBufferSize,
                            maxWriteBufferNumber,
                            minWriteBufferNumberToMerge,
                            maxBackgroundJobs,
                            maxOpenFiles,
                            targetFileSizeBase,
                            level0FileNumCompactionTrigger,
                            level0SlowdownWritesTrigger,
                            level0StopWritesTrigger,
                            enableStatistics,
                            optimizeFiltersForHits,
                            compressionType,
                            walSyncMode,
                            walSyncIntervalMillis,
                            walSyncWriteThreshold,
                            walSyncOnShutdown,
                            walSyncWarnThresholdMillis,
                            walSyncCompareMode,
                            cleanup,
                            warmupRounds,
                            measureRounds,
                            batchSize,
                            lockIndexScanBatchSize,
                            queryIterationsPerRound,
                            queryLimit,
                            repeatRuns,
                            compareOrder,
                            runLabel,
                            sampleEvery,
                            seed,
                            dbPath,
                            benchmarks,
                            compare,
                            tuningProfile,
                            statusDistribution,
                            expiredRatio,
                            lockWorkload,
                            lockConflictRatio,
                            xidFanoutDistribution,
                            dbWriteBufferSize,
                            globalWriteBufferSize,
                            branchWriteBufferSize,
                            lockWriteBufferSize,
                            indexWriteBufferSize,
                            metadataWriteBufferSize,
                            maxTotalWalSize)
                    .copyExecutionProfilesFrom(this);
        }

        private BenchmarkOptions comparisonBaseOptions() {
            if ("explicitR4".equals(compare)) {
                BenchmarkOptions baseline = withR4Budgets(0L, 0L, 0L, 0L, 0L, 0L, 0L);
                if (baseline.hasSameR4Budgets(this)) {
                    throw new IllegalArgumentException("explicitR4 comparison requires different effective R4 budgets");
                }
                return baseline;
            }
            return this;
        }

        private boolean hasSameR4Budgets(BenchmarkOptions other) {
            return dbWriteBufferSize == other.dbWriteBufferSize
                    && globalWriteBufferSize == other.globalWriteBufferSize
                    && branchWriteBufferSize == other.branchWriteBufferSize
                    && lockWriteBufferSize == other.lockWriteBufferSize
                    && indexWriteBufferSize == other.indexWriteBufferSize
                    && metadataWriteBufferSize == other.metadataWriteBufferSize
                    && maxTotalWalSize == other.maxTotalWalSize;
        }

        private BenchmarkOptions flipCompareOption() {
            if (compare == null) {
                return this;
            }
            switch (compare) {
                case "syncWrite":
                    return new BenchmarkOptions(
                                    globalCount,
                                    branchPerGlobal,
                                    lockPerBranch,
                                    writeWorkload,
                                    !syncWrite,
                                    enableRangeDelete,
                                    blockCacheSize,
                                    writeBufferSize,
                                    maxWriteBufferNumber,
                                    minWriteBufferNumberToMerge,
                                    maxBackgroundJobs,
                                    maxOpenFiles,
                                    targetFileSizeBase,
                                    level0FileNumCompactionTrigger,
                                    level0SlowdownWritesTrigger,
                                    level0StopWritesTrigger,
                                    enableStatistics,
                                    optimizeFiltersForHits,
                                    compressionType,
                                    walSyncMode,
                                    walSyncIntervalMillis,
                                    walSyncWriteThreshold,
                                    walSyncOnShutdown,
                                    walSyncWarnThresholdMillis,
                                    walSyncCompareMode,
                                    cleanup,
                                    warmupRounds,
                                    measureRounds,
                                    batchSize,
                                    lockIndexScanBatchSize,
                                    queryIterationsPerRound,
                                    queryLimit,
                                    repeatRuns,
                                    compareOrder,
                                    runLabel,
                                    sampleEvery,
                                    seed,
                                    dbPath,
                                    benchmarks,
                                    compare,
                                    tuningProfile,
                                    statusDistribution,
                                    expiredRatio,
                                    lockWorkload,
                                    lockConflictRatio,
                                    xidFanoutDistribution,
                                    dbWriteBufferSize,
                                    globalWriteBufferSize,
                                    branchWriteBufferSize,
                                    lockWriteBufferSize,
                                    indexWriteBufferSize,
                                    metadataWriteBufferSize,
                                    maxTotalWalSize)
                            .copyExecutionProfilesFrom(this);
                case "enableRangeDelete":
                    return new BenchmarkOptions(
                                    globalCount,
                                    branchPerGlobal,
                                    lockPerBranch,
                                    writeWorkload,
                                    syncWrite,
                                    !enableRangeDelete,
                                    blockCacheSize,
                                    writeBufferSize,
                                    maxWriteBufferNumber,
                                    minWriteBufferNumberToMerge,
                                    maxBackgroundJobs,
                                    maxOpenFiles,
                                    targetFileSizeBase,
                                    level0FileNumCompactionTrigger,
                                    level0SlowdownWritesTrigger,
                                    level0StopWritesTrigger,
                                    enableStatistics,
                                    optimizeFiltersForHits,
                                    compressionType,
                                    walSyncMode,
                                    walSyncIntervalMillis,
                                    walSyncWriteThreshold,
                                    walSyncOnShutdown,
                                    walSyncWarnThresholdMillis,
                                    walSyncCompareMode,
                                    cleanup,
                                    warmupRounds,
                                    measureRounds,
                                    batchSize,
                                    lockIndexScanBatchSize,
                                    queryIterationsPerRound,
                                    queryLimit,
                                    repeatRuns,
                                    compareOrder,
                                    runLabel,
                                    sampleEvery,
                                    seed,
                                    dbPath,
                                    benchmarks,
                                    compare,
                                    tuningProfile,
                                    statusDistribution,
                                    expiredRatio,
                                    lockWorkload,
                                    lockConflictRatio,
                                    xidFanoutDistribution,
                                    dbWriteBufferSize,
                                    globalWriteBufferSize,
                                    branchWriteBufferSize,
                                    lockWriteBufferSize,
                                    indexWriteBufferSize,
                                    metadataWriteBufferSize,
                                    maxTotalWalSize)
                            .copyExecutionProfilesFrom(this);
                case "blockCacheSize":
                    long flipped = blockCacheSize > 0 ? 0L : 128L * 1024 * 1024;
                    return new BenchmarkOptions(
                                    globalCount,
                                    branchPerGlobal,
                                    lockPerBranch,
                                    writeWorkload,
                                    syncWrite,
                                    enableRangeDelete,
                                    flipped,
                                    writeBufferSize,
                                    maxWriteBufferNumber,
                                    minWriteBufferNumberToMerge,
                                    maxBackgroundJobs,
                                    maxOpenFiles,
                                    targetFileSizeBase,
                                    level0FileNumCompactionTrigger,
                                    level0SlowdownWritesTrigger,
                                    level0StopWritesTrigger,
                                    enableStatistics,
                                    optimizeFiltersForHits,
                                    compressionType,
                                    walSyncMode,
                                    walSyncIntervalMillis,
                                    walSyncWriteThreshold,
                                    walSyncOnShutdown,
                                    walSyncWarnThresholdMillis,
                                    walSyncCompareMode,
                                    cleanup,
                                    warmupRounds,
                                    measureRounds,
                                    batchSize,
                                    lockIndexScanBatchSize,
                                    queryIterationsPerRound,
                                    queryLimit,
                                    repeatRuns,
                                    compareOrder,
                                    runLabel,
                                    sampleEvery,
                                    seed,
                                    dbPath,
                                    benchmarks,
                                    compare,
                                    tuningProfile,
                                    statusDistribution,
                                    expiredRatio,
                                    lockWorkload,
                                    lockConflictRatio,
                                    xidFanoutDistribution,
                                    dbWriteBufferSize,
                                    globalWriteBufferSize,
                                    branchWriteBufferSize,
                                    lockWriteBufferSize,
                                    indexWriteBufferSize,
                                    metadataWriteBufferSize,
                                    maxTotalWalSize)
                            .copyExecutionProfilesFrom(this);
                case "tuningProfile":
                    return withTuningProfile(tuningProfile);
                case "walSyncMode":
                    return withWalSyncMode(walSyncCompareMode);
                case "explicitR4":
                    return this;
                default:
                    throw new IllegalArgumentException("Unsupported compare option: " + compare);
            }
        }

        private BenchmarkOptions withTuningProfile(String profile) {
            String normalized = profile == null ? "baseline" : profile.trim().toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "baseline":
                    return withTuning(normalized, 0L, 0, 0, 0, 0, 0L, 0, 0, 0, enableStatistics, false, null);
                case "write-heavy":
                    return withTuning(
                            normalized,
                            128L * 1024 * 1024,
                            4,
                            2,
                            4,
                            0,
                            64L * 1024 * 1024,
                            8,
                            20,
                            36,
                            true,
                            false,
                            null);
                case "no-compression":
                    return withTuning(
                            normalized,
                            128L * 1024 * 1024,
                            4,
                            2,
                            4,
                            0,
                            64L * 1024 * 1024,
                            8,
                            20,
                            36,
                            true,
                            false,
                            "no");
                case "compaction-relaxed":
                    return withTuning(
                            normalized,
                            64L * 1024 * 1024,
                            4,
                            1,
                            6,
                            0,
                            64L * 1024 * 1024,
                            12,
                            32,
                            64,
                            true,
                            false,
                            null);
                case "balanced":
                    return withTuning(
                            normalized, 64L * 1024 * 1024, 3, 1, 4, 0, 64L * 1024 * 1024, 8, 20, 36, true, false, null);
                case "balanced-no-r4":
                    return withTuning(
                            normalized, 64L * 1024 * 1024, 3, 1, 4, 0, 64L * 1024 * 1024, 8, 20, 36, true, false, null);
                case "wal-only":
                    return withR4Budgets(
                            dbWriteBufferSize,
                            globalWriteBufferSize,
                            branchWriteBufferSize,
                            lockWriteBufferSize,
                            indexWriteBufferSize,
                            metadataWriteBufferSize,
                            maxTotalWalSize > 0 ? maxTotalWalSize : 1024L * 1024 * 1024);
                case "r4-memory-only":
                    return withR4Budgets(
                            dbWriteBufferSize > 0 ? dbWriteBufferSize : 512L * 1024 * 1024,
                            globalWriteBufferSize > 0 ? globalWriteBufferSize : 64L * 1024 * 1024,
                            branchWriteBufferSize > 0 ? branchWriteBufferSize : 128L * 1024 * 1024,
                            lockWriteBufferSize > 0 ? lockWriteBufferSize : 128L * 1024 * 1024,
                            indexWriteBufferSize > 0 ? indexWriteBufferSize : 64L * 1024 * 1024,
                            metadataWriteBufferSize > 0 ? metadataWriteBufferSize : 16L * 1024 * 1024,
                            maxTotalWalSize);
                case "memory-balanced":
                    return withTuning(
                                    normalized,
                                    64L * 1024 * 1024,
                                    3,
                                    1,
                                    4,
                                    0,
                                    64L * 1024 * 1024,
                                    8,
                                    20,
                                    36,
                                    true,
                                    false,
                                    null)
                            .withR4Budgets(
                                    dbWriteBufferSize > 0 ? dbWriteBufferSize : 512L * 1024 * 1024,
                                    globalWriteBufferSize > 0 ? globalWriteBufferSize : 64L * 1024 * 1024,
                                    branchWriteBufferSize > 0 ? branchWriteBufferSize : 128L * 1024 * 1024,
                                    lockWriteBufferSize > 0 ? lockWriteBufferSize : 128L * 1024 * 1024,
                                    indexWriteBufferSize > 0 ? indexWriteBufferSize : 64L * 1024 * 1024,
                                    metadataWriteBufferSize > 0 ? metadataWriteBufferSize : 16L * 1024 * 1024,
                                    maxTotalWalSize > 0 ? maxTotalWalSize : 1024L * 1024 * 1024);
                default:
                    throw new IllegalArgumentException("Unsupported tuningProfile:" + profile);
            }
        }

        private BenchmarkOptions withTuning(
                String profile,
                long newWriteBufferSize,
                int newMaxWriteBufferNumber,
                int newMinWriteBufferNumberToMerge,
                int newMaxBackgroundJobs,
                int newMaxOpenFiles,
                long newTargetFileSizeBase,
                int newLevel0FileNumCompactionTrigger,
                int newLevel0SlowdownWritesTrigger,
                int newLevel0StopWritesTrigger,
                boolean newEnableStatistics,
                boolean newOptimizeFiltersForHits,
                String newCompressionType) {
            return new BenchmarkOptions(
                            globalCount,
                            branchPerGlobal,
                            lockPerBranch,
                            writeWorkload,
                            syncWrite,
                            enableRangeDelete,
                            blockCacheSize,
                            newWriteBufferSize,
                            newMaxWriteBufferNumber,
                            newMinWriteBufferNumberToMerge,
                            newMaxBackgroundJobs,
                            newMaxOpenFiles,
                            newTargetFileSizeBase,
                            newLevel0FileNumCompactionTrigger,
                            newLevel0SlowdownWritesTrigger,
                            newLevel0StopWritesTrigger,
                            newEnableStatistics,
                            newOptimizeFiltersForHits,
                            newCompressionType,
                            walSyncMode,
                            walSyncIntervalMillis,
                            walSyncWriteThreshold,
                            walSyncOnShutdown,
                            walSyncWarnThresholdMillis,
                            walSyncCompareMode,
                            cleanup,
                            warmupRounds,
                            measureRounds,
                            batchSize,
                            lockIndexScanBatchSize,
                            queryIterationsPerRound,
                            queryLimit,
                            repeatRuns,
                            compareOrder,
                            runLabel,
                            sampleEvery,
                            seed,
                            dbPath,
                            benchmarks,
                            compare,
                            profile,
                            statusDistribution,
                            expiredRatio,
                            lockWorkload,
                            lockConflictRatio,
                            xidFanoutDistribution,
                            dbWriteBufferSize,
                            globalWriteBufferSize,
                            branchWriteBufferSize,
                            lockWriteBufferSize,
                            indexWriteBufferSize,
                            metadataWriteBufferSize,
                            maxTotalWalSize)
                    .copyExecutionProfilesFrom(this);
        }

        private BenchmarkOptions withExplicitR4Overrides(Map<String, String> values) {
            return withR4Budgets(
                    values.containsKey("dbWriteBufferSize")
                            ? parseSizeOption(values, "dbWriteBufferSize", dbWriteBufferSize)
                            : dbWriteBufferSize,
                    values.containsKey("globalWriteBufferSize")
                            ? parseSizeOption(values, "globalWriteBufferSize", globalWriteBufferSize)
                            : globalWriteBufferSize,
                    values.containsKey("branchWriteBufferSize")
                            ? parseSizeOption(values, "branchWriteBufferSize", branchWriteBufferSize)
                            : branchWriteBufferSize,
                    values.containsKey("lockWriteBufferSize")
                            ? parseSizeOption(values, "lockWriteBufferSize", lockWriteBufferSize)
                            : lockWriteBufferSize,
                    values.containsKey("indexWriteBufferSize")
                            ? parseSizeOption(values, "indexWriteBufferSize", indexWriteBufferSize)
                            : indexWriteBufferSize,
                    values.containsKey("metadataWriteBufferSize")
                            ? parseSizeOption(values, "metadataWriteBufferSize", metadataWriteBufferSize)
                            : metadataWriteBufferSize,
                    values.containsKey("maxTotalWalSize")
                            ? parseSizeOption(values, "maxTotalWalSize", maxTotalWalSize)
                            : maxTotalWalSize);
        }

        private BenchmarkOptions withR4Budgets(
                long newDbWriteBufferSize,
                long newGlobalWriteBufferSize,
                long newBranchWriteBufferSize,
                long newLockWriteBufferSize,
                long newIndexWriteBufferSize,
                long newMetadataWriteBufferSize,
                long newMaxTotalWalSize) {
            return new BenchmarkOptions(
                            globalCount,
                            branchPerGlobal,
                            lockPerBranch,
                            writeWorkload,
                            syncWrite,
                            enableRangeDelete,
                            blockCacheSize,
                            writeBufferSize,
                            maxWriteBufferNumber,
                            minWriteBufferNumberToMerge,
                            maxBackgroundJobs,
                            maxOpenFiles,
                            targetFileSizeBase,
                            level0FileNumCompactionTrigger,
                            level0SlowdownWritesTrigger,
                            level0StopWritesTrigger,
                            enableStatistics,
                            optimizeFiltersForHits,
                            compressionType,
                            walSyncMode,
                            walSyncIntervalMillis,
                            walSyncWriteThreshold,
                            walSyncOnShutdown,
                            walSyncWarnThresholdMillis,
                            walSyncCompareMode,
                            cleanup,
                            warmupRounds,
                            measureRounds,
                            batchSize,
                            lockIndexScanBatchSize,
                            queryIterationsPerRound,
                            queryLimit,
                            repeatRuns,
                            compareOrder,
                            runLabel,
                            sampleEvery,
                            seed,
                            dbPath,
                            benchmarks,
                            compare,
                            tuningProfile,
                            statusDistribution,
                            expiredRatio,
                            lockWorkload,
                            lockConflictRatio,
                            xidFanoutDistribution,
                            newDbWriteBufferSize,
                            newGlobalWriteBufferSize,
                            newBranchWriteBufferSize,
                            newLockWriteBufferSize,
                            newIndexWriteBufferSize,
                            newMetadataWriteBufferSize,
                            newMaxTotalWalSize)
                    .copyExecutionProfilesFrom(this);
        }

        private BenchmarkOptions withWalSyncMode(RocksDBWalSyncMode newWalSyncMode) {
            return new BenchmarkOptions(
                            globalCount,
                            branchPerGlobal,
                            lockPerBranch,
                            writeWorkload,
                            syncWrite,
                            enableRangeDelete,
                            blockCacheSize,
                            writeBufferSize,
                            maxWriteBufferNumber,
                            minWriteBufferNumberToMerge,
                            maxBackgroundJobs,
                            maxOpenFiles,
                            targetFileSizeBase,
                            level0FileNumCompactionTrigger,
                            level0SlowdownWritesTrigger,
                            level0StopWritesTrigger,
                            enableStatistics,
                            optimizeFiltersForHits,
                            compressionType,
                            newWalSyncMode,
                            walSyncIntervalMillis,
                            walSyncWriteThreshold,
                            walSyncOnShutdown,
                            walSyncWarnThresholdMillis,
                            newWalSyncMode,
                            cleanup,
                            warmupRounds,
                            measureRounds,
                            batchSize,
                            lockIndexScanBatchSize,
                            queryIterationsPerRound,
                            queryLimit,
                            repeatRuns,
                            compareOrder,
                            runLabel,
                            sampleEvery,
                            seed,
                            dbPath,
                            benchmarks,
                            compare,
                            tuningProfile,
                            statusDistribution,
                            expiredRatio,
                            lockWorkload,
                            lockConflictRatio,
                            xidFanoutDistribution,
                            dbWriteBufferSize,
                            globalWriteBufferSize,
                            branchWriteBufferSize,
                            lockWriteBufferSize,
                            indexWriteBufferSize,
                            metadataWriteBufferSize,
                            maxTotalWalSize)
                    .copyExecutionProfilesFrom(this);
        }

        private BenchmarkOptions withRunLabel(String newRunLabel) {
            return new BenchmarkOptions(
                            globalCount,
                            branchPerGlobal,
                            lockPerBranch,
                            writeWorkload,
                            syncWrite,
                            enableRangeDelete,
                            blockCacheSize,
                            writeBufferSize,
                            maxWriteBufferNumber,
                            minWriteBufferNumberToMerge,
                            maxBackgroundJobs,
                            maxOpenFiles,
                            targetFileSizeBase,
                            level0FileNumCompactionTrigger,
                            level0SlowdownWritesTrigger,
                            level0StopWritesTrigger,
                            enableStatistics,
                            optimizeFiltersForHits,
                            compressionType,
                            walSyncMode,
                            walSyncIntervalMillis,
                            walSyncWriteThreshold,
                            walSyncOnShutdown,
                            walSyncWarnThresholdMillis,
                            walSyncCompareMode,
                            cleanup,
                            warmupRounds,
                            measureRounds,
                            batchSize,
                            lockIndexScanBatchSize,
                            queryIterationsPerRound,
                            queryLimit,
                            repeatRuns,
                            compareOrder,
                            newRunLabel,
                            sampleEvery,
                            seed,
                            dbPath,
                            benchmarks,
                            compare,
                            tuningProfile,
                            statusDistribution,
                            expiredRatio,
                            lockWorkload,
                            lockConflictRatio,
                            xidFanoutDistribution,
                            dbWriteBufferSize,
                            globalWriteBufferSize,
                            branchWriteBufferSize,
                            lockWriteBufferSize,
                            indexWriteBufferSize,
                            metadataWriteBufferSize,
                            maxTotalWalSize)
                    .copyExecutionProfilesFrom(this);
        }

        private List<String> comparisonRunLabels() {
            List<String> labels = new ArrayList<>(repeatRuns * compareOrder.length());
            for (int repeatRun = 1; repeatRun <= repeatRuns; repeatRun++) {
                for (int i = 0; i < compareOrder.length(); i++) {
                    labels.add(compareOrder.charAt(i) + Integer.toString(repeatRun));
                }
            }
            return labels;
        }

        private String tuningSummary() {
            return "writeBufferSize="
                    + humanReadableSize(writeBufferSize)
                    + ",dbWriteBufferSize="
                    + humanReadableSize(dbWriteBufferSize)
                    + ",globalWriteBufferSize="
                    + humanReadableSize(globalWriteBufferSize)
                    + ",branchWriteBufferSize="
                    + humanReadableSize(branchWriteBufferSize)
                    + ",lockWriteBufferSize="
                    + humanReadableSize(lockWriteBufferSize)
                    + ",indexWriteBufferSize="
                    + humanReadableSize(indexWriteBufferSize)
                    + ",metadataWriteBufferSize="
                    + humanReadableSize(metadataWriteBufferSize)
                    + ",maxTotalWalSize="
                    + humanReadableSize(maxTotalWalSize)
                    + ",lockIndexScanBatchSize="
                    + lockIndexScanBatchSize
                    + ",maxWriteBufferNumber="
                    + maxWriteBufferNumber
                    + ",minWriteBufferNumberToMerge="
                    + minWriteBufferNumberToMerge
                    + ",maxBackgroundJobs="
                    + maxBackgroundJobs
                    + ",maxOpenFiles="
                    + maxOpenFiles
                    + ",targetFileSizeBase="
                    + humanReadableSize(targetFileSizeBase)
                    + ",level0FileNumCompactionTrigger="
                    + level0FileNumCompactionTrigger
                    + ",level0SlowdownWritesTrigger="
                    + level0SlowdownWritesTrigger
                    + ",level0StopWritesTrigger="
                    + level0StopWritesTrigger
                    + ",enableStatistics="
                    + enableStatistics
                    + ",optimizeFiltersForHits="
                    + optimizeFiltersForHits
                    + ",compressionType="
                    + compressionType;
        }

        private boolean isEnabled(String benchmark) {
            return benchmarks.contains(benchmark);
        }

        private int totalRounds() {
            return warmupRounds + measureRounds;
        }

        private Path rootPath() throws IOException {
            if (dbPath == null || dbPath.trim().isEmpty()) {
                return Files.createTempDirectory("seata-rocksdb-file-mode-benchmark-");
            }
            return Paths.get(dbPath);
        }

        private static Map<String, String> parseArgs(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (!arg.startsWith("--")) {
                    continue;
                }
                String keyValue = arg.substring(2);
                int equalsIndex = keyValue.indexOf('=');
                if (equalsIndex >= 0) {
                    values.put(keyValue.substring(0, equalsIndex), keyValue.substring(equalsIndex + 1));
                    continue;
                }
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    values.put(keyValue, args[++i]);
                } else {
                    values.put(keyValue, "true");
                }
            }
            return values;
        }

        private static Set<String> parseBenchmarks(String value) {
            Set<String> result = new LinkedHashSet<>();
            if (value == null || value.trim().isEmpty() || "all".equalsIgnoreCase(value.trim())) {
                result.addAll(ALL_BENCHMARKS);
                return result;
            }
            for (String item : value.split(",")) {
                String benchmark = item.trim().toLowerCase(Locale.ROOT);
                if (benchmark.isEmpty()) {
                    continue;
                }
                if (!SUPPORTED_BENCHMARKS.contains(benchmark)) {
                    throw new IllegalArgumentException("Unsupported benchmark:" + benchmark);
                }
                result.add(benchmark);
            }
            if (result.isEmpty()) {
                result.addAll(ALL_BENCHMARKS);
            }
            return result;
        }

        private static List<StatusWeight> parseStatusDistribution(String value) {
            if (StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }
            List<StatusWeight> result = new ArrayList<>();
            for (String item : value.split(",")) {
                String trimmed = item.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int separator = trimmed.indexOf(':');
                if (separator < 0) {
                    separator = trimmed.indexOf('=');
                }
                if (separator <= 0 || separator == trimmed.length() - 1) {
                    throw new IllegalArgumentException("Invalid statusDistribution item:" + item);
                }
                GlobalStatus status =
                        parseGlobalStatus(trimmed.substring(0, separator).trim());
                int weight = positive(
                        Integer.parseInt(trimmed.substring(separator + 1).trim()), "statusWeight");
                result.add(new StatusWeight(status, weight));
            }
            if (result.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(result);
        }

        private static List<IntWeight> parseIntDistribution(String value, String optionName) {
            if (StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }
            List<IntWeight> result = new ArrayList<>();
            for (String item : value.split(",")) {
                String trimmed = item.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int separator = trimmed.indexOf(':');
                if (separator < 0) {
                    separator = trimmed.indexOf('=');
                }
                if (separator <= 0 || separator == trimmed.length() - 1) {
                    throw new IllegalArgumentException("Invalid " + optionName + " item:" + item);
                }
                int distributionValue = nonNegative(
                        Integer.parseInt(trimmed.substring(0, separator).trim()), optionName + "Value");
                int weight = positive(
                        Integer.parseInt(trimmed.substring(separator + 1).trim()), optionName + "Weight");
                result.add(new IntWeight(distributionValue, weight));
            }
            if (result.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(result);
        }

        private static String normalizeLockWorkload(String value) {
            if (StringUtils.isBlank(value)) {
                return "full";
            }
            return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        }

        private static String normalizeWriteWorkload(String value) {
            String normalized = StringUtils.isBlank(value)
                    ? WRITE_WORKLOAD_FULL
                    : value.trim().toLowerCase(Locale.ROOT);
            if (!WRITE_WORKLOAD_FULL.equals(normalized) && !WRITE_WORKLOAD_APPEND.equals(normalized)) {
                throw new IllegalArgumentException("Unsupported writeWorkload: " + value);
            }
            return normalized;
        }

        private static String normalizeDataPayloadProfile(String value) {
            String normalized = StringUtils.isBlank(value)
                    ? DATA_PAYLOAD_PROFILE_PRODUCTION
                    : value.trim().toLowerCase(Locale.ROOT);
            if (!DATA_PAYLOAD_PROFILE_PRODUCTION.equals(normalized)
                    && !DATA_PAYLOAD_PROFILE_PHASE4.equals(normalized)) {
                throw new IllegalArgumentException("Unsupported dataPayloadProfile: " + value);
            }
            return normalized;
        }

        private static String normalizeQueryWorkload(String value) {
            String normalized = StringUtils.isBlank(value)
                    ? QUERY_WORKLOAD_ALL
                    : value.trim().toLowerCase(Locale.ROOT);
            if (!QUERY_WORKLOAD_ALL.equals(normalized)
                    && !QUERY_WORKLOAD_NONE.equals(normalized)
                    && !"xid".equals(normalized)
                    && !"transaction_id".equals(normalized)
                    && !"status".equals(normalized)
                    && !"status_multi".equals(normalized)
                    && !"begin_sorted".equals(normalized)
                    && !"full_scan_filter".equals(normalized)) {
                throw new IllegalArgumentException("Unsupported queryWorkload: " + value);
            }
            return normalized;
        }

        private static Set<String> parseLockWorkload(String value) {
            String normalized = normalizeLockWorkload(value);
            Set<String> operations = new LinkedHashSet<>();
            for (String item : normalized.split(",")) {
                String operation = normalizeLockOperation(item);
                if (operation.isEmpty()) {
                    continue;
                }
                if ("full".equals(operation)) {
                    operations.addAll(ALL_LOCK_WORKLOADS);
                    continue;
                }
                if ("release".equals(operation)) {
                    operations.add(LOCK_OP_RELEASE_BRANCH);
                    operations.add(LOCK_OP_RELEASE_GLOBAL);
                    continue;
                }
                if ("update".equals(operation)) {
                    operations.add(LOCK_OP_UPDATE_STATUS);
                    continue;
                }
                if (!ALL_LOCK_WORKLOADS.contains(operation) && !EXTRA_LOCK_WORKLOADS.contains(operation)) {
                    throw new IllegalArgumentException("Unsupported lockWorkload operation:" + item);
                }
                operations.add(operation);
            }
            if (operations.isEmpty()) {
                operations.addAll(ALL_LOCK_WORKLOADS);
            }
            return Collections.unmodifiableSet(operations);
        }

        private static String normalizeLockOperation(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        }

        private static GlobalStatus parseGlobalStatus(String value) {
            for (GlobalStatus status : GlobalStatus.values()) {
                if (status.name().equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unsupported global status:" + value);
        }

        private static int intValue(Map<String, String> values, String key, int defaultValue) {
            return Integer.parseInt(stringValue(values, key, Integer.toString(defaultValue)));
        }

        private static long longValue(Map<String, String> values, String key, long defaultValue) {
            return Long.parseLong(stringValue(values, key, Long.toString(defaultValue)));
        }

        private static double ratioValue(Map<String, String> values, String key, double defaultValue) {
            return ratio(Double.parseDouble(stringValue(values, key, Double.toString(defaultValue))), key);
        }

        private static boolean booleanValue(Map<String, String> values, String key, boolean defaultValue) {
            return Boolean.parseBoolean(stringValue(values, key, Boolean.toString(defaultValue)));
        }

        private static String stringValue(Map<String, String> values, String key, String defaultValue) {
            String value = values.get(key);
            if (value == null) {
                value = System.getProperty("rocksdb.file.benchmark." + key);
            }
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return value.trim();
        }

        private static String normalizeNullable(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return value.trim();
        }

        private static String normalizeCompareOrder(String value) {
            String normalized = StringUtils.isBlank(value) ? "AB" : value.trim().toUpperCase(Locale.ROOT);
            for (int i = 0; i < normalized.length(); i++) {
                char label = normalized.charAt(i);
                if (label != 'A' && label != 'B') {
                    throw new IllegalArgumentException("compareOrder only supports A and B labels:" + value);
                }
            }
            return normalized;
        }

        private static int positive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }

        private static long positive(long value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }

        private static int nonNegative(int value, String name) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " must be non-negative");
            }
            return value;
        }

        private static long nonNegative(long value, String name) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " must be non-negative");
            }
            return value;
        }

        private static double ratio(double value, String name) {
            if (value < 0D || value > 1D) {
                throw new IllegalArgumentException(name + " must be between 0 and 1");
            }
            return value;
        }

        private static long parseSizeOption(Map<String, String> values, String key, long defaultValue) {
            String value = stringValue(values, key, null);
            if (value == null) {
                return defaultValue;
            }
            return parseSize(value);
        }

        private static long parseSize(String value) {
            if (value == null || value.trim().isEmpty()) {
                return 0L;
            }
            value = value.trim().toUpperCase(Locale.ROOT);
            if (value.endsWith("GB")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024 * 1024 * 1024);
            }
            if (value.endsWith("MB")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024 * 1024);
            }
            if (value.endsWith("KB")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024);
            }
            if (value.endsWith("B")) {
                return Long.parseLong(value.substring(0, value.length() - 1));
            }
            return Long.parseLong(value);
        }

        private static String humanReadableSize(long bytes) {
            if (bytes <= 0) {
                return "0 (disabled)";
            }
            if (bytes >= 1024L * 1024 * 1024 && bytes % (1024L * 1024 * 1024) == 0) {
                return (bytes / (1024L * 1024 * 1024)) + "GB";
            }
            if (bytes >= 1024L * 1024 && bytes % (1024L * 1024) == 0) {
                return (bytes / (1024L * 1024)) + "MB";
            }
            if (bytes >= 1024 && bytes % 1024 == 0) {
                return (bytes / 1024) + "KB";
            }
            return bytes + "B";
        }
    }
}
