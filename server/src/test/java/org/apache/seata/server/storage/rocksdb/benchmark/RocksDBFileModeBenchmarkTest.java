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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBWalSyncMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class RocksDBFileModeBenchmarkTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testCrashRecoveryCheckpointTimeoutSupportsScaledRuns() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("checkpointTimeoutMillis", "300000");

        Assertions.assertEquals(300000L, RocksDBCrashRecoveryHarness.checkpointTimeoutMillis(options));
        Assertions.assertEquals(
                300000L, RocksDBCrashRecoveryHarness.checkpointTimeoutMillis(Collections.<String, String>emptyMap()));
    }

    @Test
    void testCrashHarnessBuildsPeriodicWalConfigFromOptions() throws Exception {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("syncWrite", "false");
        options.put("walSyncMode", "periodic");
        options.put("walSyncIntervalMillis", "100");
        options.put("walSyncWriteThreshold", "10");

        RocksDBStoreConfig config =
                RocksDBCrashRecoveryHarness.storeConfig(Files.createTempDirectory("r8-wal"), options);

        Assertions.assertFalse(config.isSyncWrite());
        Assertions.assertEquals(RocksDBWalSyncMode.PERIODIC, config.getWalSyncMode());
        Assertions.assertEquals(100, config.getWalSyncIntervalMillis());
        Assertions.assertEquals(10L, config.getWalSyncWriteThreshold());
    }

    @Test
    void testCrashHarnessForwardsWarmupOptionToWriter() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("warmupWrites", "2");
        options.put("syncWrite", "false");
        options.put("walSyncMode", "periodic");
        options.put("walSyncIntervalMillis", "100");
        options.put("walSyncWriteThreshold", "10");
        options.put("checkpointPolicy", "afterSync");
        options.put("checkpointSyncTimeoutMillis", "5000");

        List<String> arguments =
                RocksDBCrashRecoveryHarness.writerArguments(Paths.get("db"), Paths.get("checkpoint"), options);

        Assertions.assertTrue(arguments.contains("--warmupWrites=2"));
        Assertions.assertTrue(arguments.contains("--syncWrite=false"));
        Assertions.assertTrue(arguments.contains("--walSyncMode=periodic"));
        Assertions.assertTrue(arguments.contains("--walSyncIntervalMillis=100"));
        Assertions.assertTrue(arguments.contains("--walSyncWriteThreshold=10"));
        Assertions.assertTrue(arguments.contains("--checkpointPolicy=afterSync"));
        Assertions.assertTrue(arguments.contains("--checkpointSyncTimeoutMillis=5000"));
    }

    @Test
    void testCrashHarnessRejectsAfterSyncWithoutDurabilityMechanism() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("syncWrite", "false");
        options.put("walSyncMode", "none");
        options.put("checkpointPolicy", "afterSync");

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> RocksDBCrashRecoveryHarness.validateCheckpointOptions(options));

        options.remove("checkpointPolicy");
        options.put("warmupWrites", "1");
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> RocksDBCrashRecoveryHarness.validateCheckpointOptions(options));
    }

    @Test
    void testCrashHarnessRequiresExactRecoveryForDurableCheckpoint() {
        Map<String, String> syncWrite = new LinkedHashMap<>();
        syncWrite.put("syncWrite", "true");
        Map<String, String> afterSync = new LinkedHashMap<>();
        afterSync.put("syncWrite", "false");
        afterSync.put("walSyncMode", "periodic");
        afterSync.put("checkpointPolicy", "afterSync");
        Map<String, String> asyncBeforeSync = new LinkedHashMap<>();
        asyncBeforeSync.put("syncWrite", "false");
        asyncBeforeSync.put("walSyncMode", "periodic");

        Assertions.assertTrue(RocksDBCrashRecoveryHarness.requiresExactRecovery(syncWrite));
        Assertions.assertTrue(RocksDBCrashRecoveryHarness.requiresExactRecovery(afterSync));
        Assertions.assertFalse(RocksDBCrashRecoveryHarness.requiresExactRecovery(asyncBeforeSync));
    }

    @Test
    void testCrashHarnessRejectsInvalidCheckpointBounds() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("count", "5");
        options.put("checkpointAfter", "6");

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> RocksDBCrashRecoveryHarness.validateCheckpointOptions(options));

        options.put("checkpointAfter", "0");
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> RocksDBCrashRecoveryHarness.validateCheckpointOptions(options));
    }

    @Test
    void testOptionsParseQueryAndCompareControls() throws Exception {
        Object options = parseOptions(
                "--batchSize=5",
                "--lockIndexScanBatchSize=256",
                "--queryIterationsPerRound=7",
                "--queryLimit=3",
                "--repeatRuns=2",
                "--compare=syncWrite",
                "--compareOrder=BA");

        Assertions.assertEquals(5, intField(options, "batchSize"));
        Assertions.assertEquals(256, intField(options, "lockIndexScanBatchSize"));
        Assertions.assertEquals(7, intField(options, "queryIterationsPerRound"));
        Assertions.assertEquals(3, intField(options, "queryLimit"));
        Assertions.assertEquals(2, intField(options, "repeatRuns"));
        Assertions.assertEquals("BA", stringField(options, "compareOrder"));
    }

    @Test
    void testParsePhase4ComparisonOptions() throws Exception {
        Object options = parseOptions(
                "--dataPayloadProfile=phase4",
                "--queryWorkload=status",
                "--orphanCleanBatchLimit=500",
                "--orphanCleanMaxBatches=2",
                "--orphanCleanRoundSleepMillis=75");

        Assertions.assertEquals("phase4", stringField(options, "dataPayloadProfile"));
        Assertions.assertEquals("status", stringField(options, "queryWorkload"));
        Assertions.assertEquals(500, intField(options, "orphanCleanBatchLimit"));
        Assertions.assertEquals(2, intField(options, "orphanCleanMaxBatches"));
        Assertions.assertEquals(75L, longField(options, "orphanCleanRoundSleepMillis"));
    }

    @Test
    void testDerivedOptionsPreserveExecutionProfiles() throws Exception {
        Object options = parseOptions(
                "--dataPayloadProfile=phase4",
                "--queryWorkload=status",
                "--orphanCleanBatchLimit=500",
                "--orphanCleanMaxBatches=2",
                "--orphanCleanRoundSleepMillis=75");

        Method method = options.getClass().getDeclaredMethod("withRunLabel", String.class);
        method.setAccessible(true);
        Object derived = method.invoke(options, "R1");

        Assertions.assertEquals("phase4", stringField(derived, "dataPayloadProfile"));
        Assertions.assertEquals("status", stringField(derived, "queryWorkload"));
        Assertions.assertEquals(500, intField(derived, "orphanCleanBatchLimit"));
        Assertions.assertEquals(2, intField(derived, "orphanCleanMaxBatches"));
        Assertions.assertEquals(75L, longField(derived, "orphanCleanRoundSleepMillis"));
    }

    @Test
    void testPhase4PayloadProfileOmitsProductionApplicationData() throws Exception {
        Object phase4Options = parseOptions("--globalCount=2", "--dataPayloadProfile=phase4");
        Object productionOptions = parseOptions("--globalCount=2", "--dataPayloadProfile=production");

        List<GlobalSession> phase4Sessions = globalSessions(createDataSet(phase4Options, 0));
        List<GlobalSession> productionSessions = globalSessions(createDataSet(productionOptions, 0));

        Assertions.assertTrue(phase4Sessions.stream().allMatch(session -> session.getApplicationData() == null));
        Assertions.assertTrue(productionSessions.stream()
                .allMatch(session -> session.getApplicationData() != null
                        && !session.getApplicationData().isEmpty()));
    }

    @Test
    void testMemoryBalancedProfileAppliesWalAndWriteBufferBudgets() throws Exception {
        Object options = parseOptions("--tuningProfile=memory-balanced");

        Assertions.assertEquals(1024L * 1024L * 1024L, longField(options, "maxTotalWalSize"));
        Assertions.assertEquals(512L * 1024L * 1024L, longField(options, "dbWriteBufferSize"));
        Assertions.assertEquals(64L * 1024L * 1024L, longField(options, "globalWriteBufferSize"));
        Assertions.assertEquals(128L * 1024L * 1024L, longField(options, "branchWriteBufferSize"));
        Assertions.assertEquals(128L * 1024L * 1024L, longField(options, "lockWriteBufferSize"));
        Assertions.assertEquals(64L * 1024L * 1024L, longField(options, "indexWriteBufferSize"));
        Assertions.assertEquals(16L * 1024L * 1024L, longField(options, "metadataWriteBufferSize"));
    }

    @Test
    void testSplitTuningProfilesApplyOnlyTheirOwnedSettings() throws Exception {
        Object balancedNoR4 = parseOptions("--tuningProfile=balanced-no-r4");
        Object walOnly = parseOptions("--tuningProfile=wal-only");
        Object memoryOnly = parseOptions("--tuningProfile=r4-memory-only");

        Assertions.assertEquals(64L * 1024L * 1024L, longField(balancedNoR4, "writeBufferSize"));
        Assertions.assertEquals(0L, longField(balancedNoR4, "maxTotalWalSize"));
        Assertions.assertEquals(0L, longField(balancedNoR4, "dbWriteBufferSize"));

        Assertions.assertEquals(1024L * 1024L * 1024L, longField(walOnly, "maxTotalWalSize"));
        Assertions.assertEquals(0L, longField(walOnly, "writeBufferSize"));
        Assertions.assertEquals(0L, longField(walOnly, "dbWriteBufferSize"));

        Assertions.assertEquals(512L * 1024L * 1024L, longField(memoryOnly, "dbWriteBufferSize"));
        Assertions.assertEquals(128L * 1024L * 1024L, longField(memoryOnly, "lockWriteBufferSize"));
        Assertions.assertEquals(0L, longField(memoryOnly, "maxTotalWalSize"));
        Assertions.assertEquals(0L, longField(memoryOnly, "writeBufferSize"));
    }

    @Test
    void testWalOnlyProfileHonorsExplicitOverrideDuringComparison() throws Exception {
        Object options = parseOptions("--compare=tuningProfile", "--tuningProfile=wal-only", "--maxTotalWalSize=2GB");
        Method method = options.getClass().getDeclaredMethod("flipCompareOption");
        method.setAccessible(true);

        Object profiled = method.invoke(options);

        Assertions.assertEquals(2L * 1024L * 1024L * 1024L, longField(profiled, "maxTotalWalSize"));
        Assertions.assertEquals(0L, longField(profiled, "dbWriteBufferSize"));
    }

    @Test
    void testSplitProfileConfigDigestsDiffer() throws Exception {
        Object walOnly = parseOptions("--tuningProfile=wal-only");
        Object memoryOnly = parseOptions("--tuningProfile=r4-memory-only");
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("configDigest", walOnly.getClass());
        method.setAccessible(true);

        Assertions.assertNotEquals(method.invoke(null, walOnly), method.invoke(null, memoryOnly));
    }

    @Test
    void testExplicitR4OptionsOverrideMemoryBalancedProfile() throws Exception {
        Object options = parseOptions(
                "--tuningProfile=memory-balanced",
                "--maxTotalWalSize=2GB",
                "--dbWriteBufferSize=768MB",
                "--lockWriteBufferSize=256MB");

        Assertions.assertEquals(2L * 1024L * 1024L * 1024L, longField(options, "maxTotalWalSize"));
        Assertions.assertEquals(768L * 1024L * 1024L, longField(options, "dbWriteBufferSize"));
        Assertions.assertEquals(256L * 1024L * 1024L, longField(options, "lockWriteBufferSize"));
    }

    @Test
    void testExplicitR4OptionsOverrideProfileDuringProfileComparison() throws Exception {
        Object options =
                parseOptions("--compare=tuningProfile", "--tuningProfile=memory-balanced", "--maxTotalWalSize=2GB");
        Method method = options.getClass().getDeclaredMethod("flipCompareOption");
        method.setAccessible(true);

        Object profiled = method.invoke(options);

        Assertions.assertEquals(2L * 1024L * 1024L * 1024L, longField(profiled, "maxTotalWalSize"));
    }

    @Test
    void testExplicitR4ComparisonKeepsABaselineAndAppliesBudgetsToB() throws Exception {
        Object options = parseOptions(
                "--compare=explicitR4",
                "--dbWriteBufferSize=256MB",
                "--globalWriteBufferSize=32MB",
                "--branchWriteBufferSize=64MB",
                "--lockWriteBufferSize=64MB",
                "--indexWriteBufferSize=32MB",
                "--metadataWriteBufferSize=8MB");
        Method baseMethod = options.getClass().getDeclaredMethod("comparisonBaseOptions");
        Method candidateMethod = options.getClass().getDeclaredMethod("flipCompareOption");
        baseMethod.setAccessible(true);
        candidateMethod.setAccessible(true);

        Object baseline = baseMethod.invoke(options);
        Object candidate = candidateMethod.invoke(options);

        Assertions.assertEquals(0L, longField(baseline, "dbWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "globalWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "maxTotalWalSize"));
        Assertions.assertEquals(256L * 1024L * 1024L, longField(candidate, "dbWriteBufferSize"));
        Assertions.assertEquals(32L * 1024L * 1024L, longField(candidate, "globalWriteBufferSize"));
        Assertions.assertEquals(64L * 1024L * 1024L, longField(candidate, "branchWriteBufferSize"));
        Assertions.assertEquals(64L * 1024L * 1024L, longField(candidate, "lockWriteBufferSize"));
        Assertions.assertEquals(32L * 1024L * 1024L, longField(candidate, "indexWriteBufferSize"));
        Assertions.assertEquals(8L * 1024L * 1024L, longField(candidate, "metadataWriteBufferSize"));
    }

    @Test
    void testExplicitR4ComparisonInheritsBalancedNoR4TuningOnBothSides() throws Exception {
        Object options =
                parseOptions("--compare=explicitR4", "--tuningProfile=balanced-no-r4", "--dbWriteBufferSize=268435456");

        assertBalancedNoR4Tuning(comparisonBaseOptions(options));
        assertBalancedNoR4Tuning(comparisonCandidateOptions(options));
    }

    @Test
    void testExplicitR4ComparisonClearsOnlyR4BudgetsOnBase() throws Exception {
        Object options =
                parseOptions("--compare=explicitR4", "--tuningProfile=balanced-no-r4", "--dbWriteBufferSize=268435456");

        Object baseline = comparisonBaseOptions(options);

        Assertions.assertEquals(0L, longField(baseline, "dbWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "globalWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "branchWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "lockWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "indexWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "metadataWriteBufferSize"));
        Assertions.assertEquals(0L, longField(baseline, "maxTotalWalSize"));
        assertBalancedNoR4Tuning(baseline);
    }

    @Test
    void testExplicitR4ComparisonCandidateRetainsResolvedExplicitBudget() throws Exception {
        Object options =
                parseOptions("--compare=explicitR4", "--tuningProfile=balanced-no-r4", "--dbWriteBufferSize=268435456");

        Object candidate = comparisonCandidateOptions(options);

        Assertions.assertEquals(268435456L, longField(candidate, "dbWriteBufferSize"));
        assertBalancedNoR4Tuning(candidate);
    }

    @Test
    void testExplicitR4ComparisonRejectsEqualEffectiveBudgets() throws Exception {
        Object options = parseOptions("--compare=explicitR4", "--tuningProfile=balanced-no-r4");

        Assertions.assertThrows(IllegalArgumentException.class, () -> comparisonBaseOptions(options));
    }

    @Test
    void testConfigDigestIncludesWalAndR4Budgets() throws Exception {
        Object smallBudget = parseOptions("--maxTotalWalSize=1GB", "--dbWriteBufferSize=512MB");
        Object largeBudget = parseOptions("--maxTotalWalSize=2GB", "--dbWriteBufferSize=512MB");
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("configDigest", smallBudget.getClass());
        method.setAccessible(true);

        Assertions.assertNotEquals(method.invoke(null, smallBudget), method.invoke(null, largeBudget));
    }

    @Test
    void testComparePlanHonorsOrderAndRepeatRuns() throws Exception {
        Object options = parseOptions("--compare=syncWrite", "--compareOrder=BA", "--repeatRuns=2");
        Method method = options.getClass().getDeclaredMethod("comparisonRunLabels");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) method.invoke(options);

        Assertions.assertEquals(Arrays.asList("B1", "A1", "B2", "A2"), labels);
    }

    @Test
    void testStatusDistributionControlsGeneratedDataSetStatuses() throws Exception {
        Object options =
                parseOptions("--globalCount=10", "--branchPerGlobal=0", "--statusDistribution=Begin:2,Committed:1");

        Assertions.assertEquals(7, countStatusForFirstIndexes(options, GlobalStatus.Begin, 10));
        Assertions.assertEquals(3, countStatusForFirstIndexes(options, GlobalStatus.Committed, 10));
        Assertions.assertEquals(0, countStatusForFirstIndexes(options, GlobalStatus.RollbackRetrying, 10));
    }

    @Test
    void testWorkloadOptionsControlGeneratedShape() throws Exception {
        Object options = parseOptions(
                "--globalCount=100",
                "--expiredRatio=0.25",
                "--xidFanoutDistribution=0:1,3:2",
                "--lockWorkload=acquire,release",
                "--lockConflictRatio=0.5");

        Assertions.assertEquals(25, countMethodTrue(options, "isExpiredIndex", 100));
        Assertions.assertEquals(Arrays.asList(0, 3, 3, 0, 3, 3), branchCountsForFirstIndexes(options, 6));
        Assertions.assertEquals(50, countMethodTrue(options, "shouldRunLockConflict", 100));
        Assertions.assertTrue(lockWorkloadIncludes(options, "acquire"));
        Assertions.assertTrue(lockWorkloadIncludes(options, "release_branch"));
        Assertions.assertTrue(lockWorkloadIncludes(options, "release_global"));
        Assertions.assertFalse(lockWorkloadIncludes(options, "conflict"));
        Assertions.assertFalse(lockWorkloadIncludes(options, "clean_orphan"));
    }

    @Test
    void testWriteBenchmarkSkipsFanoutZeroBranchRemove() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-fanout-zero-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=write",
                    "--globalCount=4",
                    "--branchPerGlobal=1",
                    "--lockPerBranch=0",
                    "--xidFanoutDistribution=0:1,1:1",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--batchSize=1",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Assertions.assertTrue(lines.stream().anyMatch(line -> line.startsWith("write.branch_remove,")));
            Map<String, String> globalAdd = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("write.global_add,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("write.global_add row missing")));
            Assertions.assertTrue(
                    Long.parseLong(globalAdd.get("writeBatchBytes")) > 0L,
                    "moving metric preparation outside the timed operation must preserve write byte metrics");
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testGlobalRemoveBenchmarkReportsActualDeleteStrategyMetrics() throws Exception {
        Path scanDbPath = Files.createTempDirectory("rocksdb-benchmark-global-remove-scan-");
        Path rangeDbPath = Files.createTempDirectory("rocksdb-benchmark-global-remove-range-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Map<String, String> scan = runGlobalRemoveBenchmark(scanDbPath, false, "write", "write.global_remove");
            Map<String, String> range = runGlobalRemoveBenchmark(rangeDbPath, true, "write", "write.global_remove");

            Assertions.assertEquals("8", scan.get("branchFanout"));
            Assertions.assertEquals("8", scan.get("rowsScanned"));
            Assertions.assertEquals("8", scan.get("iteratorNext"));
            Assertions.assertEquals("20", scan.get("pointDeleteCount"));
            Assertions.assertEquals("0", scan.get("rangeDeleteCount"));
            Assertions.assertEquals("4", scan.get("deleteBatchCount"));

            Assertions.assertEquals("8", range.get("branchFanout"));
            Assertions.assertEquals("0", range.get("rowsScanned"));
            Assertions.assertEquals("0", range.get("iteratorNext"));
            Assertions.assertEquals("12", range.get("pointDeleteCount"));
            Assertions.assertEquals("4", range.get("rangeDeleteCount"));
            Assertions.assertEquals("4", range.get("deleteBatchCount"));

            Map<String, String> cleanupScan =
                    runGlobalRemoveBenchmark(scanDbPath, false, "cleanup", "cleanup.global_remove_with_branches");
            Map<String, String> cleanupRange =
                    runGlobalRemoveBenchmark(rangeDbPath, true, "cleanup", "cleanup.global_remove_with_branches");
            Assertions.assertEquals("12", cleanupScan.get("branchFanout"));
            Assertions.assertEquals("12", cleanupScan.get("rowsScanned"));
            Assertions.assertEquals("25", cleanupScan.get("pointDeleteCount"));
            Assertions.assertEquals("0", cleanupScan.get("rangeDeleteCount"));
            Assertions.assertEquals("12", cleanupRange.get("branchFanout"));
            Assertions.assertEquals("0", cleanupRange.get("rowsScanned"));
            Assertions.assertEquals("13", cleanupRange.get("pointDeleteCount"));
            Assertions.assertEquals("4", cleanupRange.get("rangeDeleteCount"));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(scanDbPath);
            deleteRecursively(rangeDbPath);
        }
    }

    @Test
    void testDefaultBenchmarkSetExcludesExplicitBackgroundInterference() throws Exception {
        Object options = parseOptions();
        Field field = options.getClass().getDeclaredField("benchmarks");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<String> benchmarks = (java.util.Set<String>) field.get(options);

        Assertions.assertFalse(benchmarks.contains("background"));
    }

    @Test
    void testLifecycleGlobalRemoveReleasesLocksBeforeDeletingSessions() throws Exception {
        Path scanDbPath = Files.createTempDirectory("rocksdb-benchmark-lifecycle-remove-scan-");
        Path rangeDbPath = Files.createTempDirectory("rocksdb-benchmark-lifecycle-remove-range-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Map<String, String> scan = runGlobalLifecycleBenchmark(scanDbPath, false);
            Map<String, String> range = runGlobalLifecycleBenchmark(rangeDbPath, true);

            Assertions.assertEquals("12", scan.get("branchFanout"));
            Assertions.assertEquals("24", scan.get("lockFanout"));
            Assertions.assertEquals("36", scan.get("rowsScanned"));
            Assertions.assertEquals("0", scan.get("pointReads"));
            Assertions.assertEquals("0", scan.get("rangeDeleteCount"));

            Assertions.assertEquals("12", range.get("branchFanout"));
            Assertions.assertEquals("24", range.get("lockFanout"));
            Assertions.assertEquals("24", range.get("rowsScanned"));
            Assertions.assertEquals("0", range.get("pointReads"));
            Assertions.assertEquals("4", range.get("rangeDeleteCount"));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(scanDbPath);
            deleteRecursively(rangeDbPath);
        }
    }

    @Test
    void testBackgroundInterferenceCursorAdvancesPastPersistentFailures() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-background-interference-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=background",
                    "--globalCount=3000",
                    "--branchPerGlobal=0",
                    "--statusDistribution=RollbackRetrying:1,TimeoutRollbacking:1",
                    "--queryLimit=128",
                    "--queryIterationsPerRound=30",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);
            Assertions.assertTrue(
                    lines.stream().anyMatch(line -> line.startsWith("background.r2_coordinator_multi_status_cursor,")));
            Assertions.assertTrue(lines.stream().anyMatch(line -> line.startsWith("background.r2_foreground_probe,")));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testAppendWriteBenchmarkOnlyEmitsAddScenarios() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-append-write-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=write",
                    "--writeWorkload=append",
                    "--globalCount=4",
                    "--branchPerGlobal=1",
                    "--lockPerBranch=0",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--batchSize=1",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Assertions.assertEquals("append", stringField(options, "writeWorkload"));
            Map<String, String> globalAdd = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("write.global_add,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("write.global_add row missing")));
            Map<String, String> branchAdd = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("write.branch_add,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("write.branch_add row missing")));
            Assertions.assertEquals("4", globalAdd.get("ops"));
            Assertions.assertEquals("4", branchAdd.get("ops"));
            Assertions.assertFalse(lines.stream().anyMatch(line -> line.startsWith("write.global_update,")));
            Assertions.assertFalse(lines.stream().anyMatch(line -> line.startsWith("write.branch_update,")));
            Assertions.assertFalse(lines.stream().anyMatch(line -> line.startsWith("write.branch_remove,")));
            Assertions.assertFalse(lines.stream().anyMatch(line -> line.startsWith("write.global_remove,")));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testQueryStatusBenchmarkUsesBoundedScanStats() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-query-stats-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=query",
                    "--globalCount=6",
                    "--branchPerGlobal=0",
                    "--statusDistribution=RollbackRetrying:1",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--queryIterationsPerRound=1",
                    "--queryLimit=2",
                    "--queryWorkload=status",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Map<String, String> statusLine = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("query.status,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("query.status row missing")));
            Assertions.assertEquals("2", statusLine.get("rowsScanned"));
            Assertions.assertEquals("2", statusLine.get("rowsReturned"));
            Assertions.assertEquals("2", statusLine.get("pointReads"));
            Assertions.assertEquals("2", statusLine.get("iteratorNext"));
            Assertions.assertEquals(1, lines.size());
            Assertions.assertFalse(lines.stream().anyMatch(line -> line.startsWith("query.xid,")));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testQueryMultiStatusBenchmarkCarriesCursorAndUsesBoundedScanStats() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-query-multi-status-stats-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=query",
                    "--globalCount=6",
                    "--branchPerGlobal=0",
                    "--statusDistribution=RollbackRetrying:1,TimeoutRollbacking:1",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--queryIterationsPerRound=2",
                    "--queryLimit=2",
                    "--queryWorkload=status_multi",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Map<String, String> statusLine = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("query.status_multi,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("query.status_multi row missing")));
            Assertions.assertEquals("8", statusLine.get("rowsScanned"));
            Assertions.assertEquals("8", statusLine.get("rowsReturned"));
            Assertions.assertEquals("4", statusLine.get("pointReads"));
            Assertions.assertEquals("8", statusLine.get("iteratorNext"));
            Assertions.assertEquals(1, lines.size());
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testFullScanFilterBenchmarkUsesActualScanStats() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-full-scan-stats-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_MAX_LIMIT, "2");
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=query",
                    "--globalCount=6",
                    "--branchPerGlobal=0",
                    "--statusDistribution=RollbackRetrying:1",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--queryIterationsPerRound=1",
                    "--queryWorkload=full_scan_filter",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Map<String, String> fullScanLine = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("query.full_scan_filter,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("query.full_scan_filter row missing")));
            Assertions.assertEquals("2", fullScanLine.get("rowsScanned"));
            Assertions.assertEquals("2", fullScanLine.get("rowsReturned"));
            Assertions.assertEquals("0", fullScanLine.get("pointReads"));
            Assertions.assertEquals("2", fullScanLine.get("iteratorNext"));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testBeginSortedBenchmarkUsesStatusScanStats() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-begin-scan-stats-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=query",
                    "--globalCount=6",
                    "--branchPerGlobal=0",
                    "--statusDistribution=Begin:1",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--queryIterationsPerRound=1",
                    "--queryWorkload=begin_sorted",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Map<String, String> beginLine = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("query.begin_sorted,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("query.begin_sorted row missing")));
            Assertions.assertEquals("6", beginLine.get("rowsScanned"));
            Assertions.assertEquals("6", beginLine.get("rowsReturned"));
            Assertions.assertEquals("6", beginLine.get("pointReads"));
            Assertions.assertEquals("6", beginLine.get("iteratorNext"));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testBatchedOrphanBenchmarkEmitsForegroundProbeMetrics() throws Exception {
        Path dbPath = Files.createTempDirectory("rocksdb-benchmark-orphan-probe-");
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions(
                    "--benchmark=lock",
                    "--lockWorkload=clean_orphan_batched",
                    "--globalCount=4",
                    "--branchPerGlobal=1",
                    "--lockPerBranch=1",
                    "--warmupRounds=0",
                    "--measureRounds=1",
                    "--sampleEvery=1",
                    "--orphanCleanBatchLimit=2",
                    "--orphanCleanMaxBatches=1",
                    "--orphanCleanRoundSleepMillis=7",
                    "--cleanup=true",
                    "--dbPath=" + dbPath);
            Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method method =
                    RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);

            Map<String, String> probe = parseCsvLine(lines.stream()
                    .filter(line -> line.startsWith("lock.clean_orphan_batched_foreground_probe,"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("foreground probe row missing")));
            Assertions.assertTrue(Long.parseLong(probe.get("ops")) > 0L);
            Assertions.assertEquals("2", probe.get("orphanCleanBatchLimit"));
            Assertions.assertEquals("1", probe.get("orphanCleanMaxBatches"));
            Assertions.assertEquals("7", probe.get("orphanCleanRoundSleepMillis"));
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
            deleteRecursively(dbPath);
        }
    }

    @Test
    void testExpiredRatioActiveSamplesStayOutsideLongBenchmarkWindow() throws Exception {
        Object originalEnvironment =
                ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        try {
            Object options = parseOptions("--globalCount=10", "--branchPerGlobal=0", "--expiredRatio=0.5");
            Object dataSet = createDataSet(options, 0);
            long now = System.currentTimeMillis();
            long sessionTimeoutMillis = staticLongField(RocksDBFileModeBenchmark.class, "SESSION_TIMEOUT_MILLIS");

            List<GlobalSession> globalSessions = globalSessions(dataSet);
            for (int i = 0; i < globalSessions.size(); i++) {
                if (!isExpiredIndex(options, i)) {
                    long activeWindowMillis = globalSessions.get(i).getBeginTime() - now;
                    Assertions.assertTrue(
                            activeWindowMillis > sessionTimeoutMillis * 5L,
                            "active sample should stay outside a long benchmark overtime window");
                }
            }
        } finally {
            ConfigurationCache.clear();
            restoreEnvironment(originalEnvironment);
        }
    }

    @Test
    void testCsvHeaderIncludesInterpretabilityColumns() throws Exception {
        Field field = RocksDBFileModeBenchmark.class.getDeclaredField("CSV_HEADER");
        field.setAccessible(true);
        String header = (String) field.get(null);

        Assertions.assertTrue(header.contains("queryIterationsPerRound"));
        Assertions.assertTrue(header.contains("queryLimit"));
        Assertions.assertTrue(header.contains("lockIndexScanBatchSize"));
        Assertions.assertTrue(header.contains("dataPayloadProfile"));
        Assertions.assertTrue(header.contains("queryWorkload"));
        Assertions.assertTrue(header.contains("orphanCleanBatchLimit"));
        Assertions.assertTrue(header.contains("orphanCleanMaxBatches"));
        Assertions.assertTrue(header.contains("repeatRun"));
        Assertions.assertTrue(header.contains("compareOrder"));
        Assertions.assertTrue(header.contains("rowsScanned"));
        Assertions.assertTrue(header.contains("rowsReturned"));
        Assertions.assertTrue(header.contains("rowsUpdated"));
        Assertions.assertTrue(header.contains("innerOperations"));
        Assertions.assertTrue(header.contains("pointReads"));
        Assertions.assertTrue(header.contains("iteratorNext"));
        Assertions.assertTrue(header.contains("orphanCleanRoundSleepMillis"));
        Assertions.assertTrue(header.contains("writeBatchBytes"));
    }

    @Test
    void testSummaryHeaderIncludesRepeatAggregationColumns() throws Exception {
        Field field = RocksDBFileModeBenchmark.class.getDeclaredField("SUMMARY_CSV_HEADER");
        field.setAccessible(true);
        String header = (String) field.get(null);

        Assertions.assertTrue(header.contains("runGroup"));
        Assertions.assertTrue(header.contains("runCount"));
        Assertions.assertTrue(header.contains("opsPerSecondMean"));
        Assertions.assertTrue(header.contains("opsPerSecondMedian"));
        Assertions.assertTrue(header.contains("opsPerSecondP95"));
        Assertions.assertTrue(header.contains("opsPerSecondP99"));
        Assertions.assertTrue(header.contains("opsPerSecondStddev"));
        Assertions.assertTrue(header.contains("pointReadsMean"));
        Assertions.assertTrue(header.contains("iteratorNextMean"));
        Assertions.assertTrue(header.contains("writeBatchBytesMean"));
    }

    @Test
    void testParseOpsPerSecondUsesHeaderColumnAfterInterpretabilityFields() throws Exception {
        String line = csvLine(Map.of("scenario", "query.status", "repeatRun", "A1", "opsPerSecond", "123.4"));
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("parseOpsPerSecond", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Double> actual = (Map<String, Double>) method.invoke(null, List.of(line));

        Assertions.assertEquals(123.4D, actual.get("query.status"));
    }

    @Test
    void testParseOpsPerSecondAggregatesAllRepeatRuns() throws Exception {
        List<String> lines = List.of(
                csvLine(Map.of("scenario", "query.status", "repeatRun", "A1", "opsPerSecond", "100.0")),
                csvLine(Map.of("scenario", "query.status", "repeatRun", "A2", "opsPerSecond", "300.0")));
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("parseOpsPerSecond", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Double> actual = (Map<String, Double>) method.invoke(null, lines);

        Assertions.assertEquals(200.0D, actual.get("query.status"));
    }

    @Test
    void testSummarizeCsvLinesAggregatesRepeatStatistics() throws Exception {
        List<String> lines = List.of(
                csvLine(map(
                        "scenario", "query.status",
                        "repeatRun", "A1",
                        "opsPerSecond", "100.0",
                        "totalMs", "30.0",
                        "p50Ms", "3.0",
                        "p95Ms", "9.0",
                        "p99Ms", "10.0",
                        "rowsScanned", "10",
                        "rowsReturned", "5",
                        "rowsUpdated", "0",
                        "pointReads", "2",
                        "iteratorNext", "10",
                        "writeBatchBytes", "100",
                        "innerOperations", "1",
                        "rangeDeleteCount", "0",
                        "pointDeleteCount", "10",
                        "deleteBatchCount", "1",
                        "branchFanout", "4",
                        "lockFanout", "0")),
                csvLine(map(
                        "scenario", "query.status",
                        "repeatRun", "A2",
                        "opsPerSecond", "300.0",
                        "totalMs", "10.0",
                        "p50Ms", "1.0",
                        "p95Ms", "4.0",
                        "p99Ms", "5.0",
                        "rowsScanned", "30",
                        "rowsReturned", "15",
                        "rowsUpdated", "2",
                        "pointReads", "6",
                        "iteratorNext", "30",
                        "writeBatchBytes", "300",
                        "innerOperations", "3",
                        "rangeDeleteCount", "2",
                        "pointDeleteCount", "30",
                        "deleteBatchCount", "3",
                        "branchFanout", "12",
                        "lockFanout", "4")));
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("summarizeCsvLines", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> actual = (List<String>) method.invoke(null, lines);

        Assertions.assertEquals(1, actual.size());
        Map<String, String> summary = parseSummaryLine(actual.get(0));
        Assertions.assertEquals("query.status", summary.get("scenario"));
        Assertions.assertEquals("A", summary.get("runGroup"));
        Assertions.assertEquals("2", summary.get("runCount"));
        Assertions.assertEquals("200.000", summary.get("opsPerSecondMean"));
        Assertions.assertEquals("200.000", summary.get("opsPerSecondMedian"));
        Assertions.assertEquals("300.000", summary.get("opsPerSecondP95"));
        Assertions.assertEquals("300.000", summary.get("opsPerSecondP99"));
        Assertions.assertEquals("100.000", summary.get("opsPerSecondStddev"));
        Assertions.assertEquals("20.000", summary.get("rowsScannedMean"));
        Assertions.assertEquals("10.000", summary.get("rowsReturnedMean"));
        Assertions.assertEquals("1.000", summary.get("rowsUpdatedMean"));
        Assertions.assertEquals("4.000", summary.get("pointReadsMean"));
        Assertions.assertEquals("20.000", summary.get("iteratorNextMean"));
        Assertions.assertEquals("200.000", summary.get("writeBatchBytesMean"));
        Assertions.assertEquals("2.000", summary.get("innerOperationsMean"));
        Assertions.assertEquals("1.000", summary.get("rangeDeleteCountMean"));
        Assertions.assertEquals("20.000", summary.get("pointDeleteCountMean"));
        Assertions.assertEquals("2.000", summary.get("deleteBatchCountMean"));
        Assertions.assertEquals("8.000", summary.get("branchFanoutMean"));
        Assertions.assertEquals("2.000", summary.get("lockFanoutMean"));
    }

    @Test
    void testSummarizeCsvLinesAsJsonAggregatesRepeatStatistics() throws Exception {
        List<String> lines = List.of(
                csvLine(map(
                        "scenario", "query.status",
                        "repeatRun", "B1",
                        "opsPerSecond", "100.0",
                        "totalMs", "30.0",
                        "p50Ms", "3.0",
                        "p95Ms", "9.0",
                        "p99Ms", "10.0",
                        "rowsScanned", "10",
                        "rowsReturned", "5",
                        "rowsUpdated", "0",
                        "pointReads", "2",
                        "iteratorNext", "10",
                        "writeBatchBytes", "100",
                        "innerOperations", "1",
                        "rangeDeleteCount", "0",
                        "pointDeleteCount", "10",
                        "deleteBatchCount", "1",
                        "branchFanout", "4",
                        "lockFanout", "0")),
                csvLine(map(
                        "scenario", "query.status",
                        "repeatRun", "B2",
                        "opsPerSecond", "300.0",
                        "totalMs", "10.0",
                        "p50Ms", "1.0",
                        "p95Ms", "4.0",
                        "p99Ms", "5.0",
                        "rowsScanned", "30",
                        "rowsReturned", "15",
                        "rowsUpdated", "2",
                        "pointReads", "6",
                        "iteratorNext", "30",
                        "writeBatchBytes", "300",
                        "innerOperations", "3",
                        "rangeDeleteCount", "2",
                        "pointDeleteCount", "30",
                        "deleteBatchCount", "3",
                        "branchFanout", "12",
                        "lockFanout", "4")));
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("summarizeCsvLinesAsJson", List.class);
        method.setAccessible(true);

        String json = (String) method.invoke(null, lines);

        Map<?, ?> root = OBJECT_MAPPER.readValue(json, Map.class);
        List<?> summaries = (List<?>) root.get("summaries");
        Assertions.assertEquals(1, summaries.size());
        Map<?, ?> summary = (Map<?, ?>) summaries.get(0);
        Assertions.assertEquals("query.status", summary.get("scenario"));
        Assertions.assertEquals("B", summary.get("runGroup"));
        Assertions.assertEquals(2, summary.get("runCount"));
        Map<?, ?> opsPerSecond = (Map<?, ?>) summary.get("opsPerSecond");
        Assertions.assertEquals(200.0D, ((Number) opsPerSecond.get("mean")).doubleValue());
        Assertions.assertEquals(300.0D, ((Number) opsPerSecond.get("p95")).doubleValue());
        Map<?, ?> rows = (Map<?, ?>) summary.get("rows");
        Assertions.assertEquals(20.0D, ((Number) rows.get("scannedMean")).doubleValue());
        Assertions.assertEquals(10.0D, ((Number) rows.get("returnedMean")).doubleValue());
        Map<?, ?> operations = (Map<?, ?>) summary.get("operations");
        Assertions.assertEquals(4.0D, ((Number) operations.get("pointReadsMean")).doubleValue());
        Assertions.assertEquals(20.0D, ((Number) operations.get("iteratorNextMean")).doubleValue());
        Assertions.assertEquals(200.0D, ((Number) operations.get("writeBatchBytesMean")).doubleValue());
        Assertions.assertEquals(1.0D, ((Number) operations.get("rangeDeleteCountMean")).doubleValue());
        Assertions.assertEquals(20.0D, ((Number) operations.get("pointDeleteCountMean")).doubleValue());
        Assertions.assertEquals(2.0D, ((Number) operations.get("deleteBatchCountMean")).doubleValue());
        Assertions.assertEquals(8.0D, ((Number) operations.get("branchFanoutMean")).doubleValue());
        Assertions.assertEquals(2.0D, ((Number) operations.get("lockFanoutMean")).doubleValue());
        Assertions.assertEquals(Collections.singletonList("query.status:B"), root.get("summaryKeys"));
    }

    private Object parseOptions(String... args) throws Exception {
        Class<?> optionsClass = Class.forName(RocksDBFileModeBenchmark.class.getName() + "$BenchmarkOptions");
        Method method = optionsClass.getDeclaredMethod("parse", String[].class);
        method.setAccessible(true);
        return method.invoke(null, (Object) args);
    }

    private Object comparisonBaseOptions(Object options) throws Exception {
        return invokeOptionsMethod(options, "comparisonBaseOptions");
    }

    private Object comparisonCandidateOptions(Object options) throws Exception {
        return invokeOptionsMethod(options, "flipCompareOption");
    }

    private Object invokeOptionsMethod(Object options, String name) throws Exception {
        Method method = options.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        try {
            return method.invoke(options);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            throw e;
        }
    }

    private void assertBalancedNoR4Tuning(Object options) throws Exception {
        Assertions.assertEquals(64L * 1024L * 1024L, longField(options, "writeBufferSize"));
        Assertions.assertEquals(3, intField(options, "maxWriteBufferNumber"));
        Assertions.assertEquals(1, intField(options, "minWriteBufferNumberToMerge"));
        Assertions.assertEquals(4, intField(options, "maxBackgroundJobs"));
        Assertions.assertEquals(64L * 1024L * 1024L, longField(options, "targetFileSizeBase"));
        Assertions.assertEquals(8, intField(options, "level0FileNumCompactionTrigger"));
        Assertions.assertEquals(20, intField(options, "level0SlowdownWritesTrigger"));
        Assertions.assertEquals(36, intField(options, "level0StopWritesTrigger"));
    }

    private int countStatusForFirstIndexes(Object options, GlobalStatus status, int count) throws Exception {
        Method method = options.getClass().getDeclaredMethod("statusForIndex", int.class);
        method.setAccessible(true);
        int result = 0;
        for (int i = 0; i < count; i++) {
            if (method.invoke(options, i) == status) {
                result++;
            }
        }
        return result;
    }

    private int countMethodTrue(Object options, String name, int count) throws Exception {
        Method method = options.getClass().getDeclaredMethod(name, int.class);
        method.setAccessible(true);
        int result = 0;
        for (int i = 0; i < count; i++) {
            if ((Boolean) method.invoke(options, i)) {
                result++;
            }
        }
        return result;
    }

    private boolean isExpiredIndex(Object options, int index) throws Exception {
        Method method = options.getClass().getDeclaredMethod("isExpiredIndex", int.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(options, index);
    }

    private Object createDataSet(Object options, int round) throws Exception {
        Class<?> dataSetClass = Class.forName(RocksDBFileModeBenchmark.class.getName() + "$BenchmarkDataSet");
        Method method = dataSetClass.getDeclaredMethod("create", options.getClass(), int.class);
        method.setAccessible(true);
        return method.invoke(null, options, round);
    }

    @SuppressWarnings("unchecked")
    private List<GlobalSession> globalSessions(Object dataSet) throws Exception {
        Field field = dataSet.getClass().getDeclaredField("globalSessions");
        field.setAccessible(true);
        return (List<GlobalSession>) field.get(dataSet);
    }

    private List<Integer> branchCountsForFirstIndexes(Object options, int count) throws Exception {
        Method method = options.getClass().getDeclaredMethod("branchCountForIndex", int.class);
        method.setAccessible(true);
        List<Integer> result = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add((Integer) method.invoke(options, i));
        }
        return result;
    }

    private boolean lockWorkloadIncludes(Object options, String operation) throws Exception {
        Method method = options.getClass().getDeclaredMethod("lockWorkloadIncludes", String.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(options, operation);
    }

    private int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private String stringField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(target);
    }

    private long longField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(target);
    }

    private long staticLongField(Class<?> target, String name) throws Exception {
        Field field = target.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(null)).longValue();
    }

    private Map<String, String> runGlobalRemoveBenchmark(
            Path dbPath, boolean enableRangeDelete, String benchmark, String scenario) throws Exception {
        Object options = parseOptions(
                "--benchmark=" + benchmark,
                "--globalCount=4",
                "--branchPerGlobal=3",
                "--lockPerBranch=0",
                "--warmupRounds=0",
                "--measureRounds=1",
                "--batchSize=1",
                "--enableRangeDelete=" + enableRangeDelete,
                "--cleanup=true",
                "--dbPath=" + dbPath);
        Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);
        return parseCsvLine(lines.stream()
                .filter(line -> line.startsWith(scenario + ","))
                .findFirst()
                .orElseThrow(() -> new AssertionError(scenario + " row missing")));
    }

    private Map<String, String> runGlobalLifecycleBenchmark(Path dbPath, boolean enableRangeDelete) throws Exception {
        Object options = parseOptions(
                "--benchmark=lifecycle",
                "--globalCount=4",
                "--branchPerGlobal=3",
                "--lockPerBranch=2",
                "--warmupRounds=0",
                "--measureRounds=1",
                "--batchSize=1",
                "--enableRangeDelete=" + enableRangeDelete,
                "--cleanup=true",
                "--dbPath=" + dbPath);
        Constructor<RocksDBFileModeBenchmark> constructor = RocksDBFileModeBenchmark.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Method method = RocksDBFileModeBenchmark.class.getDeclaredMethod("runOnce", options.getClass(), String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) method.invoke(constructor.newInstance(), options, null);
        return parseCsvLine(lines.stream()
                .filter(line -> line.startsWith("lifecycle.global_remove_with_locks,"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("lifecycle.global_remove_with_locks row missing")));
    }

    private String csvLine(Map<String, String> valuesByColumn) throws Exception {
        Field field = RocksDBFileModeBenchmark.class.getDeclaredField("CSV_HEADER");
        field.setAccessible(true);
        String[] columns = ((String) field.get(null)).split(",");
        Map<String, String> values = new LinkedHashMap<>(valuesByColumn);
        String[] parts = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            parts[i] = values.getOrDefault(columns[i], "");
        }
        return String.join(",", parts);
    }

    private Map<String, String> parseCsvLine(String line) throws Exception {
        Field field = RocksDBFileModeBenchmark.class.getDeclaredField("CSV_HEADER");
        field.setAccessible(true);
        String[] columns = ((String) field.get(null)).split(",");
        String[] values = line.split(",", -1);
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < columns.length; i++) {
            result.put(columns[i], values[i]);
        }
        return result;
    }

    private Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(values[i], values[i + 1]);
        }
        return result;
    }

    private Map<String, String> parseSummaryLine(String line) throws Exception {
        Field field = RocksDBFileModeBenchmark.class.getDeclaredField("SUMMARY_CSV_HEADER");
        field.setAccessible(true);
        String[] columns = ((String) field.get(null)).split(",");
        String[] values = line.split(",", -1);
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < columns.length; i++) {
            result.put(columns[i], values[i]);
        }
        return result;
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (Exception ignored) {
                    // best-effort cleanup for benchmark temp files
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment(Object originalEnvironment) throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }
}
