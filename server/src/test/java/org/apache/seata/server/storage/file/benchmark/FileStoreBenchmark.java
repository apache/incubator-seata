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
package org.apache.seata.server.storage.file.benchmark;

import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.file.store.FileTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Benchmark for the original File-based transaction store (FileTransactionStoreManager).
 *
 * <p>This benchmark mirrors the structure of RocksDBFileModeBenchmark so that results
 * can be compared side-by-side for write and restart scenarios.
 *
 * <p>File mode does not support random reads or lock operations, so only write and
 * restart scenarios are included.
 *
 * <p>Usage:
 * <pre>
 *   --globalCount=1000            Number of global sessions per round
 *   --branchPerGlobal=2           Branch sessions per global session
 *   --lockPerBranch=2             Lock keys per branch session
 *   --warmupRounds=1              Warmup rounds (not measured)
 *   --measureRounds=3             Measured rounds
 *   --batchSize=100               Batch size per round
 *   --sampleEvery=1               Sample every N-th operation for percentile
 *   --seed=20260606               Random seed for reproducible data
 *   --benchmark=write,restart     Comma-separated list or "all"
 * </pre>
 */
public final class FileStoreBenchmark {

    private static final String CSV_HEADER =
            "scenario,globalCount,branchPerGlobal,lockPerBranch,syncWrite,warmupRounds,"
                    + "measureRounds,batchSize,ops,totalMs,opsPerSecond,p50Ms,p95Ms,p99Ms,"
                    + "fileSizeBytes,fileCount";

    private static final List<String> ALL_BENCHMARKS = Arrays.asList("write", "restart");
    private static final GlobalStatus[] STATUSES = {
        GlobalStatus.Begin,
        GlobalStatus.Committing,
        GlobalStatus.RollbackRetrying,
        GlobalStatus.AsyncCommitting,
        GlobalStatus.Committed
    };
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile int sinkCount;
    private static volatile String sinkXid;

    private FileStoreBenchmark() {}

    // ---- Logging helpers ----

    private static String ts() {
        return LocalTime.now().format(TIME_FMT);
    }

    private static void log(String format, Object... args) {
        System.out.printf(Locale.ROOT, "[%s] %s%n", ts(), String.format(Locale.ROOT, format, args));
    }

    private static void logScenarioStart(String scenario, BenchmarkOptions options) {
        log(
                "=== START %s (globalCount=%d, branchPerGlobal=%d, lockPerBranch=%d) ===",
                scenario, options.globalCount, options.branchPerGlobal, options.lockPerBranch);
    }

    private static void logRoundStart(String scenario, int round, int totalRounds, boolean warmup) {
        log("  [%s] round %d/%d%s", scenario, round + 1, totalRounds, warmup ? " (warmup)" : "");
    }

    private static void logScenarioEnd(String scenario, long elapsedNanos, SystemMetrics before, SystemMetrics after) {
        double elapsedSec = elapsedNanos / 1_000_000_000.0;
        log("=== END   %s (%.2fs) ===", scenario, elapsedSec);
        if (before != null && after != null) {
            long heapDelta = after.heapUsed - before.heapUsed;
            long gcCountDelta = after.gcCount - before.gcCount;
            long gcTimeDelta = after.gcTimeMs - before.gcTimeMs;
            log(
                    "  heap: %.1fMB -> %.1fMB (delta %+dMB), gc: +%d collections / +%dms",
                    before.heapUsed / 1048576.0,
                    after.heapUsed / 1048576.0,
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

    // ---- System metrics ----

    private static final class SystemMetrics {
        final long heapUsed, heapMax, nonHeapUsed, gcCount, gcTimeMs;

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
            long gcCount = 0, gcTime = 0;
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

    // ---- Main entry ----

    public static void main(String[] args) throws Exception {
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            new FileStoreBenchmark().run(BenchmarkOptions.parse(args));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
        }
    }

    private void run(BenchmarkOptions options) throws Exception {
        Path rootPath = options.rootPath();
        Files.createDirectories(rootPath);
        Path runPath = rootPath.resolve("file-store-" + System.currentTimeMillis());
        Files.createDirectories(runPath);

        printEnvironment(options, rootPath, runPath);
        System.out.println(CSV_HEADER);
        List<String> csvLines = new ArrayList<>();

        long runStartedAt = System.nanoTime();
        try {
            if (options.isEnabled("write")) {
                runWriteBenchmark(runPath, options, csvLines);
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
        Path lastFilePath = null;

        for (int round = 0; round < options.totalRounds(); round++) {
            boolean warmup = round < options.warmupRounds;
            logRoundStart(scenario, round, options.totalRounds(), warmup);
            BenchmarkDataSet dataSet = BenchmarkDataSet.create(options, round);
            Path filePath = scenarioPath(runPath, "write-round-" + round);
            lastFilePath = filePath;

            FileTransactionStoreManager storeManager =
                    new FileTransactionStoreManager(filePath.resolve("store.db").toString(), new NoopSessionManager());
            try {
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    measure(
                            globalAddStats,
                            round,
                            options,
                            () -> storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession));
                    for (BranchSession branchSession : dataSet.branchesOf(globalSession)) {
                        measure(
                                branchAddStats,
                                round,
                                options,
                                () -> storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession));
                    }
                }
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    globalSession.setStatus(nextStatus(globalSession.getStatus()));
                    measure(
                            globalUpdateStats,
                            round,
                            options,
                            () -> storeManager.writeSession(LogOperation.GLOBAL_UPDATE, globalSession));
                    for (BranchSession branchSession : dataSet.branchesOf(globalSession)) {
                        branchSession.setStatus(BranchStatus.PhaseOne_Done);
                        measure(
                                branchUpdateStats,
                                round,
                                options,
                                () -> storeManager.writeSession(LogOperation.BRANCH_UPDATE, branchSession));
                    }
                }
                if (options.branchPerGlobal > 0) {
                    for (GlobalSession globalSession : dataSet.globalSessions) {
                        BranchSession branchSession =
                                dataSet.branchesOf(globalSession).get(0);
                        measure(
                                branchRemoveStats,
                                round,
                                options,
                                () -> storeManager.writeSession(LogOperation.BRANCH_REMOVE, branchSession));
                    }
                }
                for (GlobalSession globalSession : dataSet.globalSessions) {
                    measure(
                            globalRemoveStats,
                            round,
                            options,
                            () -> storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession));
                }
                // Give the async writer time to flush
                Thread.sleep(500);
            } finally {
                storeManager.shutdown();
            }
        }

        FileFootprint footprint = FileFootprint.from(lastFilePath);
        emit("write.global_add", options, globalAddStats, footprint, csvLines);
        emit("write.global_update", options, globalUpdateStats, footprint, csvLines);
        emit("write.global_remove", options, globalRemoveStats, footprint, csvLines);
        emit("write.branch_add", options, branchAddStats, footprint, csvLines);
        emit("write.branch_update", options, branchUpdateStats, footprint, csvLines);
        emit("write.branch_remove", options, branchRemoveStats, footprint, csvLines);
        logScenarioEnd(scenario, System.nanoTime() - scenarioStart, metricsBefore, SystemMetrics.snapshot());
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
        Path lastFilePath = null;

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
            Path filePath = scenarioPath(runPath, scenarioName + "-round-" + round);
            lastFilePath = filePath;

            // Write phase (not measured)
            FileTransactionStoreManager writeManager =
                    new FileTransactionStoreManager(filePath.resolve("store.db").toString(), new NoopSessionManager());
            writeDataSet(writeManager, dataSet);
            Thread.sleep(500); // wait for async flush
            writeManager.shutdown();

            // Restart read phase (measured)
            measure(restartStats, round, options, () -> {
                FileTransactionStoreManager readManager = new FileTransactionStoreManager(
                        filePath.resolve("store.db").toString(), new NoopSessionManager());
                try {
                    int count = 0;
                    List<TransactionWriteStore> stores;
                    while ((stores = readManager.readWriteStore(1000, false)) != null) {
                        count += stores.size();
                        for (TransactionWriteStore store : stores) {
                            sinkCount = store.getOperate().getCode();
                        }
                        if (stores.size() < 1000) {
                            break;
                        }
                    }
                    // Also try history file
                    List<TransactionWriteStore> hisStores;
                    while ((hisStores = readManager.readWriteStore(1000, true)) != null) {
                        count += hisStores.size();
                        if (hisStores.size() < 1000) {
                            break;
                        }
                    }
                } finally {
                    readManager.shutdown();
                }
            });
        }

        FileFootprint footprint = FileFootprint.from(lastFilePath);
        emit(scenarioName, options, restartStats, footprint, csvLines);
    }

    // ---- Helpers ----

    private static void writeDataSet(FileTransactionStoreManager storeManager, BenchmarkDataSet dataSet) {
        for (GlobalSession globalSession : dataSet.globalSessions) {
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            for (BranchSession branchSession : dataSet.branchesOf(globalSession)) {
                storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession);
            }
        }
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
            FileFootprint footprint,
            List<String> csvLines) {
        String line = scenario
                + "," + options.globalCount
                + "," + options.branchPerGlobal
                + "," + options.lockPerBranch
                + "," + options.syncWrite
                + "," + options.warmupRounds
                + "," + options.measureRounds
                + "," + options.batchSize
                + "," + stats.ops()
                + "," + millis(stats.totalNanos())
                + "," + format(stats.opsPerSecond())
                + "," + millis(stats.percentile(50))
                + "," + millis(stats.percentile(95))
                + "," + millis(stats.percentile(99))
                + "," + footprint.sizeBytes
                + "," + footprint.fileCount;
        System.out.println(line);
        if (csvLines != null) {
            csvLines.add(line);
        }
        logEmit(scenario, stats);
    }

    private static void printEnvironment(BenchmarkOptions options, Path rootPath, Path runPath) {
        System.out.println("File store benchmark");
        System.out.println("rootPath=" + rootPath);
        System.out.println("runPath=" + runPath);
        System.out.println("benchmarks=" + options.benchmarks);
        System.out.println("availableProcessors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("jdk=" + System.getProperty("java.version") + " " + System.getProperty("java.vm.name"));
        System.out.println("os=" + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("globalCount=" + options.globalCount);
        System.out.println("branchPerGlobal=" + options.branchPerGlobal);
        System.out.println("lockPerBranch=" + options.lockPerBranch);
        System.out.println("syncWrite=" + options.syncWrite);
        System.out.println("warmupRounds=" + options.warmupRounds);
        System.out.println("measureRounds=" + options.measureRounds);
        System.out.println("batchSize=" + options.batchSize);
        System.out.println("sampleEvery=" + options.sampleEvery);
        System.out.println("cleanup=" + options.cleanup);
        System.out.println("seed=" + options.seed);
        SystemMetrics mem = SystemMetrics.snapshot();
        System.out.printf(
                Locale.ROOT,
                "jvmHeapUsed=%.1fMB, jvmHeapMax=%.1fMB, gcCollections=%d, gcTimeMs=%d%n",
                mem.heapUsed / 1048576.0,
                mem.heapMax / 1048576.0,
                mem.gcCount,
                mem.gcTimeMs);
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
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

    private interface BenchmarkTask {
        void run() throws Exception;
    }

    // ---- Noop SessionManager (same pattern as WriteStoreTest) ----

    private static final class NoopSessionManager implements SessionManager {
        @Override
        public void destroy() {}

        @Override
        public void addGlobalSession(GlobalSession session) {}

        @Override
        public GlobalSession findGlobalSession(String xid) {
            return null;
        }

        @Override
        public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
            return null;
        }

        @Override
        public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) {}

        @Override
        public void removeGlobalSession(GlobalSession session) {}

        @Override
        public void addBranchSession(GlobalSession globalSession, BranchSession session) {}

        @Override
        public void updateBranchSessionStatus(BranchSession session, BranchStatus status) {}

        @Override
        public void removeBranchSession(GlobalSession globalSession, BranchSession session) {}

        @Override
        public Collection<GlobalSession> allSessions() {
            return Collections.emptyList();
        }

        @Override
        public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
            return Collections.emptyList();
        }

        @Override
        public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable) {
            return null;
        }

        @Override
        public void onBegin(GlobalSession globalSession) {}

        @Override
        public void onStatusChange(GlobalSession globalSession, GlobalStatus status) {}

        @Override
        public void onBranchStatusChange(
                GlobalSession globalSession, BranchSession branchSession, BranchStatus status) {}

        @Override
        public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) {}

        @Override
        public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) {}

        @Override
        public void onClose(GlobalSession globalSession) {}

        @Override
        public void onSuccessEnd(GlobalSession globalSession) {}

        @Override
        public void onFailEnd(GlobalSession globalSession) {}
    }

    // ---- Data generation ----

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
            long baseBeginTime =
                    System.currentTimeMillis() - options.globalCount - round - Math.floorMod(options.seed, 1000L);
            for (int i = 0; i < options.globalCount; i++) {
                GlobalSession globalSession = globalSession(i, round, options.seed, baseBeginTime + i);
                globals.add(globalSession);
                List<BranchSession> globalBranches = new ArrayList<>(options.branchPerGlobal);
                for (int branchIndex = 0; branchIndex < options.branchPerGlobal; branchIndex++) {
                    globalBranches.add(branchSession(globalSession, i, branchIndex, options.lockPerBranch));
                }
                branches.put(globalSession.getXid(), globalBranches);
            }
            return new BenchmarkDataSet(globals, branches);
        }

        private List<BranchSession> branchesOf(GlobalSession globalSession) {
            Collection<BranchSession> branches = branchSessions.get(globalSession.getXid());
            if (branches == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(branches);
        }

        private static GlobalSession globalSession(int index, int round, long seed, long beginTime) {
            GlobalSession globalSession = new GlobalSession(
                    "benchmark-app", "benchmark-group", "phase4-tx-" + seed + "-" + round + "-" + index, 60000);
            globalSession.setStatus(STATUSES[index % STATUSES.length]);
            globalSession.setBeginTime(beginTime);
            return globalSession;
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

    // ---- Stats ----

    private static final class OperationStats {
        private final int sampleEvery;
        private long ops;
        private long totalNanos;
        private long[] samples = new long[128];
        private int sampleCount;

        private OperationStats(int sampleEvery) {
            this.sampleEvery = Math.max(1, sampleEvery);
        }

        private void record(long nanos) {
            ops++;
            totalNanos += nanos;
            if (ops % sampleEvery == 0) {
                addSample(nanos);
            }
        }

        private long ops() {
            return ops;
        }

        private long totalNanos() {
            return totalNanos;
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

    // ---- File footprint ----

    private static final class FileFootprint {
        private final long sizeBytes;
        private final long fileCount;

        private FileFootprint(long sizeBytes, long fileCount) {
            this.sizeBytes = sizeBytes;
            this.fileCount = fileCount;
        }

        private static FileFootprint from(Path path) throws IOException {
            if (path == null || !Files.exists(path)) {
                return new FileFootprint(0L, 0L);
            }
            final long[] values = new long[2];
            try (Stream<Path> stream = Files.walk(path)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        values[0] += Files.size(file);
                    } catch (IOException ignored) {
                    }
                    values[1]++;
                });
            }
            return new FileFootprint(values[0], values[1]);
        }
    }

    // ---- Options ----

    private static final class BenchmarkOptions {
        private final int globalCount;
        private final int branchPerGlobal;
        private final int lockPerBranch;
        private final boolean syncWrite;
        private final boolean cleanup;
        private final int warmupRounds;
        private final int measureRounds;
        private final int batchSize;
        private final int sampleEvery;
        private final long seed;
        private final String dbPath;
        private final Set<String> benchmarks;

        private BenchmarkOptions(
                int globalCount,
                int branchPerGlobal,
                int lockPerBranch,
                boolean syncWrite,
                boolean cleanup,
                int warmupRounds,
                int measureRounds,
                int batchSize,
                int sampleEvery,
                long seed,
                String dbPath,
                Set<String> benchmarks) {
            this.globalCount = positive(globalCount, "globalCount");
            this.branchPerGlobal = nonNegative(branchPerGlobal, "branchPerGlobal");
            this.lockPerBranch = nonNegative(lockPerBranch, "lockPerBranch");
            this.syncWrite = syncWrite;
            this.cleanup = cleanup;
            this.warmupRounds = nonNegative(warmupRounds, "warmupRounds");
            this.measureRounds = positive(measureRounds, "measureRounds");
            this.batchSize = positive(batchSize, "batchSize");
            this.sampleEvery = positive(sampleEvery, "sampleEvery");
            this.seed = seed;
            this.dbPath = dbPath;
            this.benchmarks = benchmarks;
        }

        private static BenchmarkOptions parse(String[] args) {
            Map<String, String> values = parseArgs(args);
            return new BenchmarkOptions(
                    intValue(values, "globalCount", 1000),
                    intValue(values, "branchPerGlobal", 2),
                    intValue(values, "lockPerBranch", 2),
                    booleanValue(values, "syncWrite", false),
                    booleanValue(values, "cleanup", false),
                    intValue(values, "warmupRounds", 1),
                    intValue(values, "measureRounds", 3),
                    intValue(values, "batchSize", 100),
                    intValue(values, "sampleEvery", 1),
                    longValue(values, "seed", 20260606L),
                    stringValue(values, "dbPath", null),
                    parseBenchmarks(stringValue(values, "benchmark", "all")));
        }

        private boolean isEnabled(String benchmark) {
            return benchmarks.contains(benchmark);
        }

        private int totalRounds() {
            return warmupRounds + measureRounds;
        }

        private Path rootPath() throws IOException {
            if (dbPath == null || dbPath.trim().isEmpty()) {
                return Files.createTempDirectory("seata-file-store-benchmark-");
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
                if (!ALL_BENCHMARKS.contains(benchmark)) {
                    throw new IllegalArgumentException("Unsupported benchmark: " + benchmark);
                }
                result.add(benchmark);
            }
            if (result.isEmpty()) {
                result.addAll(ALL_BENCHMARKS);
            }
            return result;
        }

        private static int intValue(Map<String, String> values, String key, int defaultValue) {
            return Integer.parseInt(stringValue(values, key, Integer.toString(defaultValue)));
        }

        private static long longValue(Map<String, String> values, String key, long defaultValue) {
            return Long.parseLong(stringValue(values, key, Long.toString(defaultValue)));
        }

        private static boolean booleanValue(Map<String, String> values, String key, boolean defaultValue) {
            return Boolean.parseBoolean(stringValue(values, key, Boolean.toString(defaultValue)));
        }

        private static String stringValue(Map<String, String> values, String key, String defaultValue) {
            String v = values.get(key);
            return v != null ? v : defaultValue;
        }

        private static int positive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0");
            }
            return value;
        }

        private static int nonNegative(int value, String name) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
            return value;
        }
    }
}
