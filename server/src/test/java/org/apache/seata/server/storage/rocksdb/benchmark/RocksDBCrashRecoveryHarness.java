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
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBWalSyncMode;
import org.apache.seata.server.storage.rocksdb.RocksDBWalSyncStats;
import org.apache.seata.server.storage.rocksdb.maintenance.RocksDBMaintenanceService;
import org.apache.seata.server.storage.rocksdb.maintenance.RocksDBVerifyReport;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manual parent/child crash-recovery harness for WAL RPO experiments. */
public final class RocksDBCrashRecoveryHarness {

    private static final String WRITER = "writer";
    private static final String PARENT = "parent";
    private static final String CLEAN = "clean";
    private static final long DEFAULT_CHECKPOINT_TIMEOUT_MILLIS = 300_000L;
    private static final long DEFAULT_CHECKPOINT_SYNC_TIMEOUT_MILLIS = 30_000L;

    private RocksDBCrashRecoveryHarness() {}

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        String mode = options.getOrDefault("mode", PARENT);
        validateCheckpointOptions(options);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            if (WRITER.equals(mode)) {
                writeUntilKilled(options);
                return;
            }
            if (CLEAN.equals(mode)) {
                runCleanShutdown(options);
                return;
            }
            runParent(options);
        } finally {
            ConfigurationCache.clear();
        }
    }

    private static void runParent(Map<String, String> options) throws Exception {
        Path dbPath = requiredPath(options, "dbPath");
        Path checkpoint = requiredPath(options, "checkpoint");
        Files.deleteIfExists(checkpoint);
        Process child = new ProcessBuilder(writerArguments(dbPath, checkpoint, options))
                .inheritIO()
                .start();
        waitForCheckpoint(checkpoint, child, checkpointTimeoutMillis(options));
        child.destroyForcibly();
        child.waitFor();
        Map<String, String> checkpointValues = readCheckpoint(checkpoint);
        long expected =
                Long.parseLong(checkpointValues.getOrDefault("expectedSessions", checkpointValues.get("sequence")));
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(storeConfig(dbPath, options))) {
            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();
            int recovered = engine.prefixScan(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0])
                    .size();
            boolean lastShutdownClean = engine.wasLastShutdownClean();
            System.out.println("R8_RESULT expected=" + expected + " recovered=" + recovered + " lost="
                    + Math.max(0L, expected - recovered) + " checkpointPolicy="
                    + checkpointValues.get("policy") + " checkpointSyncCount="
                    + checkpointValues.get("syncCount") + " checkpointLastSyncedSequence="
                    + checkpointValues.get("lastSyncedSequence") + " checkpointLatestSequence="
                    + checkpointValues.get("latestSequence") + " checkpointUnsyncedWrites="
                    + checkpointValues.get("unsyncedWrites") + " lastShutdownClean=" + lastShutdownClean
                    + " clean=" + report.isClean());
            if (!report.isClean()
                    || recovered > expected
                    || (requiresExactRecovery(options) && recovered != expected)) {
                throw new IllegalStateException("crash recovery verification failed:" + report);
            }
        }
    }

    private static void writeUntilKilled(Map<String, String> options) throws Exception {
        Path dbPath = requiredPath(options, "dbPath");
        Path checkpoint = requiredPath(options, "checkpoint");
        int count = Integer.parseInt(options.getOrDefault("count", "10000"));
        int checkpointAfter = Integer.parseInt(options.getOrDefault("checkpointAfter", "1000"));
        int warmupWrites = warmupWrites(options);
        String checkpointPolicy = options.getOrDefault("checkpointPolicy", "afterWrite");
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(storeConfig(dbPath, options))) {
            RocksDBTransactionStoreManager store = new RocksDBTransactionStoreManager(engine);
            writeWarmupSessions(store, engine, warmupWrites, options);
            for (int i = 1; i <= count; i++) {
                writeGlobalSession(store, "r8-crash-" + i);
                if (i == checkpointAfter) {
                    if ("afterSync".equals(checkpointPolicy)) {
                        waitForCheckpointSync(engine, options);
                    }
                    writeCheckpoint(
                            checkpoint,
                            i,
                            warmupWrites + i,
                            checkpointPolicy,
                            engine.diagnostics().getWalSyncStats());
                    Thread.sleep(Long.MAX_VALUE);
                }
            }
        }
    }

    private static void runCleanShutdown(Map<String, String> options) throws Exception {
        Path dbPath = requiredPath(options, "dbPath");
        int count = Integer.parseInt(options.getOrDefault("count", "10000"));
        int warmupWrites = warmupWrites(options);
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(storeConfig(dbPath, options))) {
            RocksDBTransactionStoreManager store = new RocksDBTransactionStoreManager(engine);
            writeWarmupSessions(store, engine, warmupWrites, options);
            for (int i = 1; i <= count; i++) {
                writeGlobalSession(store, "r8-clean-" + i);
            }
        }
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(storeConfig(dbPath, options))) {
            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();
            int recovered = engine.prefixScan(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0])
                    .size();
            boolean lastShutdownClean = engine.wasLastShutdownClean();
            int expected = warmupWrites + count;
            System.out.println("R8_CLEAN_RESULT expected=" + expected + " recovered=" + recovered
                    + " lastShutdownClean=" + lastShutdownClean + " clean=" + report.isClean());
            if (!lastShutdownClean || !report.isClean() || recovered != expected) {
                throw new IllegalStateException("clean shutdown verification failed:" + report);
            }
        }
    }

    private static void waitForCheckpoint(Path checkpoint, Process child, long timeoutMillis) throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (!Files.exists(checkpoint)) {
            if (!child.isAlive()) {
                throw new IllegalStateException("writer exited before checkpoint, exit=" + child.exitValue());
            }
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("timed out waiting for checkpoint:" + checkpoint);
            }
            Thread.sleep(10L);
        }
    }

    private static Path requiredPath(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("missing --" + key);
        }
        return Paths.get(value);
    }

    static long checkpointTimeoutMillis(Map<String, String> options) {
        String defaultTimeout = Long.toString(DEFAULT_CHECKPOINT_TIMEOUT_MILLIS);
        return Long.parseLong(options.getOrDefault("checkpointTimeoutMillis", defaultTimeout));
    }

    static RocksDBStoreConfig storeConfig(Path dbPath, Map<String, String> options) {
        boolean syncWrite = Boolean.parseBoolean(options.getOrDefault("syncWrite", "true"));
        RocksDBWalSyncMode walSyncMode = RocksDBWalSyncMode.of(options.getOrDefault("walSyncMode", "none"));
        int walSyncIntervalMillis = Integer.parseInt(options.getOrDefault("walSyncIntervalMillis", "2000"));
        long walSyncWriteThreshold = Long.parseLong(options.getOrDefault("walSyncWriteThreshold", "10"));
        return new RocksDBStoreConfig(
                dbPath.toString(),
                syncWrite,
                0L,
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
                false,
                false,
                walSyncMode,
                walSyncIntervalMillis,
                walSyncWriteThreshold,
                true,
                1000);
    }

    static void validateCheckpointOptions(Map<String, String> options) {
        String checkpointPolicy = options.getOrDefault("checkpointPolicy", "afterWrite");
        boolean syncWrite = Boolean.parseBoolean(options.getOrDefault("syncWrite", "true"));
        RocksDBWalSyncMode walSyncMode = RocksDBWalSyncMode.of(options.getOrDefault("walSyncMode", "none"));
        int warmupWrites = warmupWrites(options);
        int count = Integer.parseInt(options.getOrDefault("count", "10000"));
        int checkpointAfter = Integer.parseInt(options.getOrDefault("checkpointAfter", "1000"));
        if (!"afterWrite".equals(checkpointPolicy) && !"afterSync".equals(checkpointPolicy)) {
            throw new IllegalArgumentException("unknown checkpoint policy:" + checkpointPolicy);
        }
        if (warmupWrites < 0) {
            throw new IllegalArgumentException("warmupWrites must not be negative");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (!CLEAN.equals(options.getOrDefault("mode", PARENT)) && (checkpointAfter <= 0 || checkpointAfter > count)) {
            throw new IllegalArgumentException("checkpointAfter must be within 1..count");
        }
        if ("afterSync".equals(checkpointPolicy) && !syncWrite && !walSyncMode.isPeriodic()) {
            throw new IllegalArgumentException("afterSync requires syncWrite=true or periodic WAL sync");
        }
        if (warmupWrites > 0 && !syncWrite && !walSyncMode.isPeriodic()) {
            throw new IllegalArgumentException("warmupWrites requires syncWrite=true or periodic WAL sync");
        }
    }

    static boolean requiresExactRecovery(Map<String, String> options) {
        return Boolean.parseBoolean(options.getOrDefault("syncWrite", "true"))
                || "afterSync".equals(options.getOrDefault("checkpointPolicy", "afterWrite"));
    }

    static List<String> writerArguments(Path dbPath, Path checkpoint, Map<String, String> options) {
        List<String> arguments = new ArrayList<>();
        arguments.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        arguments.add("-cp");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add(RocksDBCrashRecoveryHarness.class.getName());
        arguments.add("--mode=writer");
        arguments.add("--dbPath=" + dbPath);
        arguments.add("--checkpoint=" + checkpoint);
        arguments.add("--count=" + options.getOrDefault("count", "10000"));
        arguments.add("--checkpointAfter=" + options.getOrDefault("checkpointAfter", "1000"));
        arguments.add("--warmupWrites=" + options.getOrDefault("warmupWrites", "0"));
        arguments.add("--syncWrite=" + options.getOrDefault("syncWrite", "true"));
        arguments.add("--walSyncMode=" + options.getOrDefault("walSyncMode", "none"));
        arguments.add("--walSyncIntervalMillis=" + options.getOrDefault("walSyncIntervalMillis", "2000"));
        arguments.add("--walSyncWriteThreshold=" + options.getOrDefault("walSyncWriteThreshold", "10"));
        arguments.add("--checkpointPolicy=" + options.getOrDefault("checkpointPolicy", "afterWrite"));
        arguments.add("--checkpointSyncTimeoutMillis="
                + options.getOrDefault(
                        "checkpointSyncTimeoutMillis", Long.toString(DEFAULT_CHECKPOINT_SYNC_TIMEOUT_MILLIS)));
        return arguments;
    }

    private static long checkpointSyncTimeoutMillis(Map<String, String> options) {
        return Long.parseLong(options.getOrDefault(
                "checkpointSyncTimeoutMillis", Long.toString(DEFAULT_CHECKPOINT_SYNC_TIMEOUT_MILLIS)));
    }

    private static int warmupWrites(Map<String, String> options) {
        return Integer.parseInt(options.getOrDefault("warmupWrites", "0"));
    }

    private static long writeWarmupSessions(
            RocksDBTransactionStoreManager store, RocksDBStoreEngine engine, int count, Map<String, String> options)
            throws Exception {
        for (int i = 1; i <= count; i++) {
            writeGlobalSession(store, "r8-warmup-" + i);
        }
        RocksDBWalSyncStats stats = engine.diagnostics().getWalSyncStats();
        if (count > 0 && stats.getMode().isPeriodic() && stats.getUnsyncedWriteRequests() > 0L) {
            waitForWalSync(engine, stats.getSyncCount() + 1L, checkpointSyncTimeoutMillis(options));
        }
        return engine.diagnostics().getWalSyncStats().getSyncCount();
    }

    private static void writeGlobalSession(RocksDBTransactionStoreManager store, String xid) throws Exception {
        GlobalSession session = new GlobalSession("r8", "r8", xid, 60000);
        session.setStatus(GlobalStatus.Begin);
        session.setBeginTime(System.currentTimeMillis());
        store.writeSession(LogOperation.GLOBAL_ADD, session);
    }

    private static void waitForWalSync(RocksDBStoreEngine engine, long minimumSyncCount, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (true) {
            RocksDBWalSyncStats stats = engine.diagnostics().getWalSyncStats();
            if (stats.getSyncCount() >= minimumSyncCount && stats.getUnsyncedWriteRequests() == 0L) {
                return;
            }
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("timed out waiting for periodic WAL sync, required=" + minimumSyncCount
                        + ", actual=" + stats.getSyncCount() + ", unsynced=" + stats.getUnsyncedWriteRequests());
            }
            Thread.sleep(1L);
        }
    }

    private static void waitForCheckpointSync(RocksDBStoreEngine engine, Map<String, String> options)
            throws InterruptedException {
        if (Boolean.parseBoolean(options.getOrDefault("syncWrite", "true"))) {
            return;
        }
        RocksDBWalSyncStats stats = engine.diagnostics().getWalSyncStats();
        if (stats.getUnsyncedWriteRequests() > 0L) {
            waitForWalSync(engine, stats.getSyncCount() + 1L, checkpointSyncTimeoutMillis(options));
        }
    }

    private static void writeCheckpoint(
            Path checkpoint, int sequence, int expectedSessions, String policy, RocksDBWalSyncStats stats)
            throws Exception {
        String values = "sequence=" + sequence + '\n'
                + "expectedSessions=" + expectedSessions + '\n'
                + "policy=" + policy + '\n'
                + "syncCount=" + stats.getSyncCount() + '\n'
                + "lastSyncedSequence=" + stats.getLastSyncedSequenceNumber() + '\n'
                + "latestSequence=" + stats.getLatestSequenceNumber() + '\n'
                + "unsyncedWrites=" + stats.getUnsyncedWriteRequests() + '\n';
        Path temporary = checkpoint.resolveSibling(checkpoint.getFileName() + ".tmp");
        Files.write(temporary, values.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temporary, checkpoint, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, checkpoint, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<String, String> readCheckpoint(Path checkpoint) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : Files.readAllLines(checkpoint, StandardCharsets.UTF_8)) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                values.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        if (!values.containsKey("sequence")) {
            throw new IllegalStateException("checkpoint does not contain sequence:" + checkpoint);
        }
        return values;
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                continue;
            }
            int separator = arg.indexOf('=');
            if (separator > 2) {
                values.put(arg.substring(2, separator), arg.substring(separator + 1));
            }
        }
        return values;
    }
}
