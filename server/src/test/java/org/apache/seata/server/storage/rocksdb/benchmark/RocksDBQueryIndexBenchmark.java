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

import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.apache.seata.server.storage.rocksdb.index.RocksDBIndexManager;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.rocksdb.RocksDB;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Manual benchmark for RocksDB query indexes.
 *
 * <p>Run from an IDE or with a test classpath. This class is intentionally not named *Test, so it does not run in the
 * regular unit test suite.
 */
public final class RocksDBQueryIndexBenchmark {

    private static final GlobalStatus[] STATUSES = {
        GlobalStatus.Begin,
        GlobalStatus.Committing,
        GlobalStatus.RollbackRetrying,
        GlobalStatus.AsyncCommitting,
        GlobalStatus.Committed
    };
    private static final GlobalStatus TARGET_STATUS = GlobalStatus.RollbackRetrying;
    private static volatile int sinkCount;
    private static volatile String sinkXid;

    private RocksDBQueryIndexBenchmark() {}

    public static void main(String[] args) throws Exception {
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            new RocksDBQueryIndexBenchmark().run();
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
        }
    }

    private void run() throws Exception {
        BenchmarkOptions options = BenchmarkOptions.fromSystemProperties();
        Path rootPath = options.rootPath();
        Files.createDirectories(rootPath);

        List<GlobalSession> sessions = new ArrayList<>(options.sessionCount);
        long[] writeNanos = new long[options.sessionCount];
        Path queryDbPath = rootPath.resolve("query");
        try (RocksDBStoreEngine engine = open(queryDbPath, options.syncWrite)) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            long baseBeginTime = System.currentTimeMillis() - options.sessionCount;
            for (int i = 0; i < options.sessionCount; i++) {
                GlobalSession globalSession = globalSession(i, baseBeginTime + i);
                sessions.add(globalSession);
                long startedAt = System.nanoTime();
                storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
                for (int branchIndex = 0; branchIndex < options.branchFanOut; branchIndex++) {
                    storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(globalSession, branchIndex));
                }
                writeNanos[i] = System.nanoTime() - startedAt;
            }
            engine.flush();

            long[] transactionIdNanos = repeat(options.repetitions, i -> {
                GlobalSession globalSession = sessions.get((i * 9973) % sessions.size());
                SessionCondition condition = new SessionCondition();
                condition.setTransactionId(globalSession.getTransactionId());
                condition.setLazyLoadBranch(true);
                List<GlobalSession> result = storeManager.readSession(condition);
                sinkXid = result.isEmpty() ? null : result.get(0).getXid();
            });
            long[] statusIndexNanos = repeat(options.repetitions, i -> {
                SessionCondition condition = new SessionCondition(TARGET_STATUS);
                condition.setLazyLoadBranch(true);
                sinkCount = storeManager.readSession(condition).size();
            });
            long[] beginSortedNanos = repeat(options.repetitions, i -> {
                sinkCount = storeManager.readSortByTimeoutBeginSessions(false).size();
            });
            long[] fullScanNanos = repeat(options.repetitions, i -> {
                SessionCondition condition = new SessionCondition();
                condition.setLazyLoadBranch(true);
                int count = 0;
                for (GlobalSession globalSession : storeManager.readSession(condition)) {
                    if (globalSession.getStatus() == TARGET_STATUS) {
                        count++;
                    }
                }
                sinkCount = count;
            });

            long rebuildNanos = benchmarkIndexRebuild(rootPath.resolve("upgrade"), options);
            printReport(
                    options,
                    rootPath,
                    writeNanos,
                    transactionIdNanos,
                    statusIndexNanos,
                    beginSortedNanos,
                    fullScanNanos,
                    rebuildNanos);
        }
    }

    private long benchmarkIndexRebuild(Path dbPath, BenchmarkOptions options) {
        long baseBeginTime = System.currentTimeMillis() - options.sessionCount;
        try (RocksDBStoreEngine engine = open(dbPath, options.syncWrite)) {
            for (int i = 0; i < options.sessionCount; i++) {
                GlobalSession globalSession = globalSession(i, baseBeginTime + i);
                engine.put(
                        RocksDBColumnFamily.GLOBAL_SESSION,
                        RocksDBKeyCodec.encodeXid(globalSession.getXid()),
                        RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.GLOBAL_SESSION, globalSession.encode()));
            }
            engine.flush();
        }
        try (RocksDBStoreEngine engine = open(dbPath, options.syncWrite)) {
            RocksDBIndexManager indexManager = new RocksDBIndexManager(engine);
            long startedAt = System.nanoTime();
            indexManager.ensureReady();
            engine.flush();
            return System.nanoTime() - startedAt;
        }
    }

    private static RocksDBStoreEngine open(Path dbPath, boolean syncWrite) {
        return RocksDBStoreEngine.open(new RocksDBStoreConfig(dbPath.toString(), syncWrite));
    }

    private static GlobalSession globalSession(int index, long beginTime) {
        GlobalSession globalSession = new GlobalSession("benchmark-app", "benchmark-group", "tx-" + index, 60000);
        globalSession.setStatus(STATUSES[index % STATUSES.length]);
        globalSession.setBeginTime(beginTime);
        return globalSession;
    }

    private static BranchSession branchSession(GlobalSession globalSession, int branchIndex) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid(globalSession.getXid());
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(globalSession.getTransactionId() * 1000 + branchIndex);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/benchmark");
        branchSession.setLockKey("benchmark_table:" + globalSession.getTransactionId() + ":" + branchIndex);
        return branchSession;
    }

    private static long[] repeat(int repetitions, BenchmarkTask task) {
        long[] samples = new long[repetitions];
        for (int i = 0; i < repetitions; i++) {
            long startedAt = System.nanoTime();
            task.run(i);
            samples[i] = System.nanoTime() - startedAt;
        }
        return samples;
    }

    private static void printReport(
            BenchmarkOptions options,
            Path rootPath,
            long[] writeNanos,
            long[] transactionIdNanos,
            long[] statusIndexNanos,
            long[] beginSortedNanos,
            long[] fullScanNanos,
            long rebuildNanos) {
        System.out.println("RocksDB query index benchmark");
        System.out.println("rootPath=" + rootPath);
        System.out.println("availableProcessors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("jdk=" + System.getProperty("java.version") + " " + System.getProperty("java.vm.name"));
        System.out.println("rocksdbJniVersion=" + rocksDbJniVersion());
        System.out.println("syncWrite=" + options.syncWrite);
        System.out.println("sessionCount=" + options.sessionCount);
        System.out.println("branchFanOut=" + options.branchFanOut);
        System.out.println("repetitions=" + options.repetitions);
        System.out.println("targetStatus=" + TARGET_STATUS);
        System.out.println("indexRebuild=" + millis(rebuildNanos) + " ms");
        printStats("writeGlobalWithBranches", writeNanos);
        printStats("queryByTransactionIdIndex", transactionIdNanos);
        printStats("queryByStatusIndex", statusIndexNanos);
        printStats("queryBeginSortedIndex", beginSortedNanos);
        printStats("queryFullGlobalScanThenFilter", fullScanNanos);
        System.out.println("sinkCount=" + sinkCount);
        System.out.println("sinkXid=" + sinkXid);
    }

    private static void printStats(String name, long[] samples) {
        Arrays.sort(samples);
        System.out.println(name + ".p50=" + millis(percentile(samples, 50)) + " ms");
        System.out.println(name + ".p95=" + millis(percentile(samples, 95)) + " ms");
        System.out.println(name + ".p99=" + millis(percentile(samples, 99)) + " ms");
    }

    private static long percentile(long[] sortedSamples, int percentile) {
        int index = (int) Math.ceil(sortedSamples.length * percentile / 100.0D) - 1;
        return sortedSamples[Math.max(0, Math.min(index, sortedSamples.length - 1))];
    }

    private static String millis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0D);
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
        void run(int iteration);
    }

    private static final class BenchmarkOptions {
        private final int sessionCount;
        private final int repetitions;
        private final int branchFanOut;
        private final boolean syncWrite;
        private final String dbPath;

        private BenchmarkOptions(
                int sessionCount, int repetitions, int branchFanOut, boolean syncWrite, String dbPath) {
            this.sessionCount = sessionCount;
            this.repetitions = repetitions;
            this.branchFanOut = branchFanOut;
            this.syncWrite = syncWrite;
            this.dbPath = dbPath;
        }

        private static BenchmarkOptions fromSystemProperties() {
            return new BenchmarkOptions(
                    intProperty("rocksdb.benchmark.sessions", 10000),
                    intProperty("rocksdb.benchmark.repetitions", 100),
                    intProperty("rocksdb.benchmark.branchFanOut", 0),
                    booleanProperty("rocksdb.benchmark.syncWrite", false),
                    System.getProperty("rocksdb.benchmark.dbPath"));
        }

        private Path rootPath() throws Exception {
            if (dbPath == null || dbPath.trim().isEmpty()) {
                return Files.createTempDirectory("seata-rocksdb-query-index-benchmark-");
            }
            return Paths.get(dbPath);
        }

        private static int intProperty(String key, int defaultValue) {
            String value = System.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        }

        private static boolean booleanProperty(String key, boolean defaultValue) {
            String value = System.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value);
        }
    }
}
