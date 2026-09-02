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
package org.apache.seata.server.storage.rocksdb.maintenance;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.WriteBatch;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RocksDBMaintenanceServiceTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;

    @BeforeEach
    void beforeEach() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
    }

    @AfterEach
    void afterEach() throws Exception {
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    // ---- Checkpoint tests ----

    @Test
    void testCheckpointCreatesAndCanBeReopened() {
        try (RocksDBStoreEngine engine = open("checkpoint")) {
            GlobalSession global = globalSession("tx-ckpt", GlobalStatus.Begin);
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, global);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            Path checkpointDir = tempDir.resolve("checkpoint-out");
            service.createCheckpoint(checkpointDir, true);

            // Checkpoint metadata should exist
            Assertions.assertTrue(Files.exists(checkpointDir.resolve("seata-checkpoint-metadata.txt")));

            // Reopen from checkpoint and verify data
            try (RocksDBStoreEngine checkpointEngine =
                    RocksDBStoreEngine.open(new RocksDBStoreConfig(checkpointDir.toString(), true))) {
                byte[] value = checkpointEngine.get(
                        RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(global.getXid()));
                Assertions.assertNotNull(value);
            }
        }
    }

    @Test
    void testCheckpointFailsWhenDirectoryNotEmpty() throws IOException {
        try (RocksDBStoreEngine engine = open("checkpoint-nonempty")) {
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            Path checkpointDir = tempDir.resolve("checkpoint-nonempty");
            Files.createDirectories(checkpointDir);
            Files.write(checkpointDir.resolve("existing-file.txt"), "data".getBytes(StandardCharsets.UTF_8));

            Assertions.assertThrows(StoreException.class, () -> service.createCheckpoint(checkpointDir, false));
        }
    }

    @Test
    void testCheckpointRejectsNullPath() {
        try (RocksDBStoreEngine engine = open("checkpoint-null")) {
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            Assertions.assertThrows(StoreException.class, () -> service.createCheckpoint(null, false));
        }
    }

    @Test
    void testCheckpointMetadataContainsExpectedFields() throws IOException {
        try (RocksDBStoreEngine engine = open("checkpoint-meta")) {
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            Path checkpointDir = tempDir.resolve("checkpoint-meta-out");
            service.createCheckpoint(checkpointDir, false);

            String metadata = new String(
                    Files.readAllBytes(checkpointDir.resolve("seata-checkpoint-metadata.txt")), StandardCharsets.UTF_8);
            Assertions.assertTrue(metadata.contains("sourceDbPath="));
            Assertions.assertTrue(metadata.contains("formatVersion=" + RocksDBStoreEngine.FORMAT_VERSION));
            Assertions.assertTrue(metadata.contains("columnFamilies="));
            Assertions.assertTrue(metadata.contains("global_session"));
            Assertions.assertTrue(metadata.contains("branch_session"));
            Assertions.assertTrue(metadata.contains("rocksdbVersion="));
            Assertions.assertTrue(metadata.contains("seataVersion="));
        }
    }

    @Test
    void testCheckpointDoesNotFlushWhenDisabled() {
        try (RocksDBStoreEngine engine = spy(open("checkpoint-no-flush"))) {
            new RocksDBMaintenanceService(engine).createCheckpoint(tempDir.resolve("checkpoint-no-flush-out"), false);

            verify(engine, never()).flush();
        }
    }

    @Test
    void testCheckpointFlushesOnceWhenEnabled() {
        try (RocksDBStoreEngine engine = spy(open("checkpoint-flush"))) {
            new RocksDBMaintenanceService(engine).createCheckpoint(tempDir.resolve("checkpoint-flush-out"), true);

            verify(engine, times(1)).flush();
        }
    }

    // ---- Verify tests ----

    @Test
    void testVerifyCleanState() {
        try (RocksDBStoreEngine engine = open("verify-clean")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("tx-1", GlobalStatus.Begin));
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("tx-2", GlobalStatus.Committing));

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertTrue(report.isClean(), "expected clean state, got: " + report);
            Assertions.assertEquals(2, report.getCheckedGlobalCount());
            Assertions.assertEquals(0, report.getStaleStatusIndexCount());
            Assertions.assertEquals(0, report.getStaleTransactionIdIndexCount());
            Assertions.assertEquals(0, report.getOrphanBranchCount());
            Assertions.assertEquals(0, report.getOrphanLockCount());
            Assertions.assertEquals(0, report.getStaleLockIndexCount());
        }
    }

    @Test
    void testPagedVerifyResumesFromReturnedCursor() {
        try (RocksDBStoreEngine engine = open("verify-page-cursor")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("page-1", GlobalStatus.Begin));
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("page-2", GlobalStatus.Begin));
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("page-3", GlobalStatus.Begin));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            RocksDBVerifyReport first = service.verifyCurrentState(RocksDBVerifyOptions.page(2, null, 10));
            RocksDBVerifyReport second =
                    service.verifyCurrentState(RocksDBVerifyOptions.page(2, first.getNextCursor(), 10));
            int checkedRecords = first.getCheckedRecordCount() + second.getCheckedRecordCount();
            RocksDBVerifyReport current = second;
            int pages = 2;
            while (!current.isComplete()) {
                current = service.verifyCurrentState(RocksDBVerifyOptions.page(2, current.getNextCursor(), 10));
                checkedRecords += current.getCheckedRecordCount();
                Assertions.assertTrue(++pages <= 8, "paged verify did not reach completion");
            }

            Assertions.assertEquals(RocksDBVerifyMode.PAGE, first.getMode());
            Assertions.assertEquals(2, first.getCheckedRecordCount());
            Assertions.assertEquals(2, first.getCheckedGlobalCount());
            Assertions.assertFalse(first.isComplete());
            Assertions.assertNotNull(first.getNextCursor());
            Assertions.assertEquals(2, second.getCheckedRecordCount());
            Assertions.assertEquals(1, second.getCheckedGlobalCount());
            Assertions.assertEquals(12, checkedRecords);
            Assertions.assertNull(current.getNextCursor());
            Assertions.assertTrue(first.isClean());
            Assertions.assertTrue(second.isClean());
        }
    }

    @Test
    void testPagedVerifyDetectsDuplicateTransactionIdWithOneRecordPages() {
        try (RocksDBStoreEngine engine = open("verify-page-duplicate-transaction-id")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("page-duplicate-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("page-duplicate-second", GlobalStatus.Begin);
            second.setTransactionId(first.getTransactionId());
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyCursor cursor = null;
            boolean foundInvalidGlobal = false;
            int pages = 0;
            do {
                RocksDBVerifyReport page = service.verifyCurrentState(RocksDBVerifyOptions.page(1, cursor, 10));
                foundInvalidGlobal |= page.getInvalidGlobalCount() > 0;
                cursor = page.getNextCursor();
                Assertions.assertTrue(++pages <= 16, "paged verify did not reach completion");
            } while (cursor != null);

            Assertions.assertTrue(foundInvalidGlobal, "a duplicate global must be reported from its own page");
        }
    }

    @Test
    void testSampleVerifyBoundsEachColumnFamily() {
        try (RocksDBStoreEngine engine = open("verify-sample")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            for (int i = 0; i < 3; i++) {
                storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("sample-" + i, GlobalStatus.Begin));
            }

            RocksDBVerifyReport report =
                    new RocksDBMaintenanceService(engine).verifyCurrentState(RocksDBVerifyOptions.sample(1, 10));

            Assertions.assertEquals(RocksDBVerifyMode.SAMPLE, report.getMode());
            Assertions.assertEquals(1, report.getCheckedGlobalCount());
            Assertions.assertTrue(report.getCheckedIndexCount() <= 4);
            Assertions.assertTrue(report.getCheckedRecordCount() <= 7);
            Assertions.assertFalse(report.isComplete());
            Assertions.assertNull(report.getNextCursor());
            Assertions.assertTrue(report.isClean());
        }
    }

    @Test
    void testVerifyCapsErrorSamplesWithoutLosingIssueCount() {
        try (RocksDBStoreEngine engine = open("verify-error-cap")) {
            for (int i = 0; i < 3; i++) {
                String xid = "xid-stale-" + i;
                engine.put(
                        RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                        RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, i, xid),
                        xid.getBytes(StandardCharsets.UTF_8));
            }

            RocksDBVerifyReport report =
                    new RocksDBMaintenanceService(engine).verifyCurrentState(RocksDBVerifyOptions.full(2));

            Assertions.assertEquals(3, report.getStaleStatusIndexCount());
            Assertions.assertEquals(3, report.getInconsistentCount());
            Assertions.assertEquals(2, report.getErrorMessages().size());
            Assertions.assertEquals(3, report.getTotalErrorCount());
        }
    }

    @Test
    void testVerifyDetectsMissingGlobalIndexes() {
        try (RocksDBStoreEngine engine = open("verify-missing-global-indexes")) {
            GlobalSession global = globalSession("missing-indexes", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            engine.delete(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(
                            global.getStatus(), global.getBeginTime(), global.getXid()));
            engine.delete(
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                            global.getBeginTime() + global.getTimeout(), global.getXid()));
            engine.delete(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(global.getTransactionId()));

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();

            Assertions.assertEquals(1, report.getMissingStatusIndexCount());
            Assertions.assertEquals(1, report.getMissingTimeoutIndexCount());
            Assertions.assertEquals(1, report.getMissingTransactionIdIndexCount());
            Assertions.assertEquals(3, report.getInconsistentCount());
        }
    }

    @Test
    void testVerifyEmptyDatabase() {
        try (RocksDBStoreEngine engine = open("verify-empty")) {
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertTrue(report.isClean(), "expected clean state for empty db, got: " + report);
            Assertions.assertEquals(0, report.getCheckedGlobalCount());
        }
    }

    @Test
    void testVerifyDetectsStaleStatusIndex() {
        try (RocksDBStoreEngine engine = open("verify-stale-status")) {
            // Write a status index entry that does NOT correspond to any global session
            byte[] staleKey = RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, 1000L, "xid-stale");
            byte[] xidValue = "xid-stale".getBytes(StandardCharsets.UTF_8);
            engine.put(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, staleKey, xidValue);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getStaleStatusIndexCount());
        }
    }

    @Test
    void testVerifyDetectsStaleTransactionIdIndex() {
        try (RocksDBStoreEngine engine = open("verify-stale-txnid")) {
            // Write a transaction id index entry pointing to a non-existent global
            byte[] key = RocksDBKeyCodec.encodeTransactionIdIndex(99999L);
            byte[] value = "xid-nonexistent".getBytes(StandardCharsets.UTF_8);
            engine.put(RocksDBColumnFamily.TRANSACTION_ID_INDEX, key, value);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getStaleTransactionIdIndexCount());
        }
    }

    @Test
    void testVerifyDetectsOrphanBranch() {
        try (RocksDBStoreEngine engine = open("verify-orphan-branch")) {
            GlobalSession orphanGlobal = globalSession("xid-orphan", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine)
                    .writeSession(LogOperation.BRANCH_ADD, branchSession(orphanGlobal, 1L));

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport full = service.verifyCurrentState();
            RocksDBVerifyReport sample = service.verifyCurrentState(RocksDBVerifyOptions.sample(1, 10));

            Assertions.assertFalse(full.isClean());
            Assertions.assertFalse(sample.isClean());
            Assertions.assertEquals(1, full.getOrphanBranchCount());
            Assertions.assertEquals(1, sample.getOrphanBranchCount());
        }
    }

    @Test
    void testFullVerifyStopsAtDeadlineAndReturnsCursor() {
        try (RocksDBStoreEngine engine = open("verify-full-deadline")) {
            for (int i = 0; i < 300; i++) {
                String xid = "deadline-" + i;
                engine.put(
                        RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                        RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, i, xid),
                        xid.getBytes(StandardCharsets.UTF_8));
            }

            RocksDBVerifyReport report =
                    new RocksDBMaintenanceService(engine).verifyCurrentState(RocksDBVerifyOptions.full(10, 0));

            Assertions.assertFalse(report.isComplete());
            Assertions.assertTrue(report.isTruncated());
            Assertions.assertNotNull(report.getNextCursor());
            Assertions.assertTrue(report.getScannedRecordCount() >= 256);
            Assertions.assertTrue(report.getCheckedRecordCount() < 300);
        }
    }

    @Test
    void testPagedVerifyStopsAtDeadlineAndReturnsCursor() {
        try (RocksDBStoreEngine engine = open("verify-page-deadline")) {
            for (int i = 0; i < 300; i++) {
                String xid = "deadline-page-" + i;
                engine.put(
                        RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                        RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, i, xid),
                        xid.getBytes(StandardCharsets.UTF_8));
            }

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine)
                    .verifyCurrentState(RocksDBVerifyOptions.page(500, null, 10, 0));

            Assertions.assertFalse(report.isComplete());
            Assertions.assertTrue(report.isTruncated());
            Assertions.assertNotNull(report.getNextCursor());
            Assertions.assertTrue(report.getScannedRecordCount() >= 256);
            Assertions.assertTrue(report.getCheckedRecordCount() < 300);
        }
    }

    @Test
    void testVerifyDetectsBranchPayloadKeyMismatch() {
        try (RocksDBStoreEngine engine = open("verify-branch-key-mismatch")) {
            GlobalSession global = globalSession("branch-key-mismatch", GlobalStatus.Begin);
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, global);
            BranchSession branch = branchSession(global, 1L);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branch);

            byte[] validKey = RocksDBKeyCodec.encodeBranch(global.getXid(), branch.getBranchId());
            byte[] payload = engine.get(RocksDBColumnFamily.BRANCH_SESSION, validKey);
            engine.delete(RocksDBColumnFamily.BRANCH_SESSION, validKey);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch(global.getXid(), 2L), payload);

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getInvalidBranchCount());
        }
    }

    @Test
    void testVerifyDetectsStaleLockIndex() {
        try (RocksDBStoreEngine engine = open("verify-stale-lock-idx")) {
            // Write a LOCK_BRANCH_INDEX entry pointing to a non-existent LOCK
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-1");
            byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex("xid-1", 1L, lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexKey, lockKey);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getStaleLockIndexCount());
        }
    }

    @Test
    void testVerifyDetectsOrphanLock() {
        try (RocksDBStoreEngine engine = open("verify-orphan-lock")) {
            // Write a LOCK entry without a corresponding LOCK_BRANCH_INDEX
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-orphan");
            byte[] lockValue = encodeLockHolder("xid-1", 1001L, 1L, "resource", "table", "pk-orphan");
            engine.put(RocksDBColumnFamily.LOCK, lockKey, lockValue);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getOrphanLockCount());
        }
    }

    @Test
    void testVerifyConsistentLockAndIndex() {
        try (RocksDBStoreEngine engine = open("verify-lock-ok")) {
            GlobalSession global = globalSession("tx-lock", GlobalStatus.Begin);
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, global);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(global, 1L));

            // Write consistent LOCK + LOCK_BRANCH_INDEX pair
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-1");
            byte[] lockValue =
                    encodeLockHolder(global.getXid(), global.getTransactionId(), 1L, "resource", "table", "pk-1");
            byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex(global.getXid(), 1L, lockKey);
            byte[] cleanupCursor = new byte[indexKey.length + 1];
            System.arraycopy(indexKey, 0, cleanupCursor, 0, indexKey.length);

            try (WriteBatch batch = new WriteBatch()) {
                engine.put(batch, RocksDBColumnFamily.LOCK, lockKey, lockValue);
                engine.put(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexKey, lockKey);
                engine.write(batch);
            } catch (Exception e) {
                Assertions.fail(e);
            }
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "orphan_lock_clean_cursor".getBytes(StandardCharsets.UTF_8),
                    cleanupCursor);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertTrue(report.isClean(), "expected clean state, got: " + report);
            Assertions.assertEquals(1, report.getCheckedLockCount());
        }
    }

    @Test
    void testVerifyDetectsLockWithoutBranchSession() throws Exception {
        try (RocksDBStoreEngine engine = open("verify-lock-without-branch")) {
            GlobalSession global = globalSession("lock-without-branch", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-no-branch");
            byte[] lockValue = encodeLockHolder(
                    global.getXid(), global.getTransactionId(), 1L, "resource", "table", "pk-no-branch");
            byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex(global.getXid(), 1L, lockKey);
            try (WriteBatch batch = new WriteBatch()) {
                engine.put(batch, RocksDBColumnFamily.LOCK, lockKey, lockValue);
                engine.put(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexKey, lockKey);
                engine.write(batch);
            }

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getOrphanLockCount());
            Assertions.assertEquals(1, report.getStaleLockIndexCount());
        }
    }

    @Test
    void testVerifyDetectsInvalidMaintenanceMetadata() {
        try (RocksDBStoreEngine engine = open("verify-invalid-maintenance-metadata")) {
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "clean_shutdown".getBytes(StandardCharsets.UTF_8),
                    "unknown".getBytes(StandardCharsets.UTF_8));
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "orphan_lock_clean_cursor".getBytes(StandardCharsets.UTF_8),
                    new byte[] {1});

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(2, report.getInvalidMetadataCount());
        }
    }

    @Test
    void testVerifyFaultInjectionMatrixIsDetectedByAllReadOnlyModes() throws Exception {
        try (RocksDBStoreEngine engine = open("verify-fault-injection-matrix")) {
            GlobalSession global = globalSession("verify-fault-injection", GlobalStatus.Begin);
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, global);
            engine.delete(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(
                            global.getStatus(), global.getBeginTime(), global.getXid()));
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(global.getTransactionId()),
                    "wrong-xid".getBytes(StandardCharsets.UTF_8));

            BranchSession branch = branchSession(global, 1L);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branch);
            byte[] validBranchKey = RocksDBKeyCodec.encodeBranch(global.getXid(), branch.getBranchId());
            byte[] branchPayload = engine.get(RocksDBColumnFamily.BRANCH_SESSION, validBranchKey);
            engine.delete(RocksDBColumnFamily.BRANCH_SESSION, validBranchKey);
            engine.put(
                    RocksDBColumnFamily.BRANCH_SESSION,
                    RocksDBKeyCodec.encodeBranch(global.getXid(), 2L),
                    branchPayload);

            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-matrix");
            byte[] lockValue =
                    encodeLockHolder(global.getXid(), global.getTransactionId(), 3L, "resource", "table", "pk-matrix");
            byte[] lockIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(global.getXid(), 3L, lockKey);
            try (WriteBatch batch = new WriteBatch()) {
                engine.put(batch, RocksDBColumnFamily.LOCK, lockKey, lockValue);
                engine.put(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, lockIndexKey, lockKey);
                engine.write(batch);
            }
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "clean_shutdown".getBytes(StandardCharsets.UTF_8),
                    "unknown".getBytes(StandardCharsets.UTF_8));
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "orphan_lock_clean_cursor".getBytes(StandardCharsets.UTF_8),
                    new byte[] {1});

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport full = service.verifyCurrentState();
            RocksDBVerifyReport sample = service.verifyCurrentState(RocksDBVerifyOptions.sample(1, 20));
            RocksDBVerifyReport page = service.verifyCurrentState(RocksDBVerifyOptions.page(100, null, 20));

            Assertions.assertFalse(full.isClean());
            Assertions.assertFalse(sample.isClean());
            Assertions.assertFalse(page.isClean());
            Assertions.assertEquals(1, sample.getMissingStatusIndexCount());
            Assertions.assertEquals(1, sample.getInvalidBranchCount());
            Assertions.assertEquals(1, sample.getOrphanLockCount());
            Assertions.assertEquals(1, sample.getStaleLockIndexCount());
            Assertions.assertEquals(2, sample.getInvalidMetadataCount());
            Assertions.assertEquals(sample.getInconsistentCount(), page.getInconsistentCount());
            System.out.println("R6_VERIFY_RESULT fullViolations=" + full.getInconsistentCount()
                    + " sampleViolations=" + sample.getInconsistentCount() + " pageViolations="
                    + page.getInconsistentCount() + " missingStatus=" + sample.getMissingStatusIndexCount()
                    + " invalidBranch=" + sample.getInvalidBranchCount() + " orphanLock="
                    + sample.getOrphanLockCount() + " staleLockIndex=" + sample.getStaleLockIndexCount()
                    + " invalidMetadata=" + sample.getInvalidMetadataCount());
        }
    }

    @Test
    void testVerifyDetectsStaleLockIndexForMissingGlobal() {
        try (RocksDBStoreEngine engine = open("verify-lock-stale-global")) {
            // Write LOCK + LOCK_BRANCH_INDEX pair referencing a non-existent global
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-gone");
            byte[] lockValue = encodeLockHolder("xid-gone", 8888L, 1L, "resource", "table", "pk-gone");
            byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex("xid-gone", 1L, lockKey);

            try (WriteBatch batch = new WriteBatch()) {
                engine.put(batch, RocksDBColumnFamily.LOCK, lockKey, lockValue);
                engine.put(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexKey, lockKey);
                engine.write(batch);
            } catch (Exception e) {
                Assertions.fail(e);
            }

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            // The lock index is stale because the global session it references doesn't exist
            Assertions.assertEquals(1, report.getStaleLockIndexCount());
            // The LOCK entry has no matching LOCK_BRANCH_INDEX in the valid set (because the index was stale)
            Assertions.assertEquals(1, report.getOrphanLockCount());
        }
    }

    // ---- Repair tests ----

    @Test
    void testRepairIndexesRebuildsStaleIndexes() {
        try (RocksDBStoreEngine engine = open("repair-indexes")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession global = globalSession("tx-repair", GlobalStatus.Begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, global);

            // Corrupt the status index by deleting the valid entry and adding a stale one
            byte[] staleKey = RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Committed, 9999L, "xid-stale");
            engine.put(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, staleKey, "xid-stale".getBytes(StandardCharsets.UTF_8));

            // Verify should detect stale index before repair
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport beforeRepair = service.verifyCurrentState();
            Assertions.assertFalse(beforeRepair.isClean());

            // Repair
            service.executeRepair(
                    service.planRepair(RocksDBRepairOptions.defaults()),
                    RocksDBRepairOptions.builder()
                            .dryRun(false)
                            .confirm(true)
                            .maintenanceMode(true)
                            .build());

            // Verify should be clean after repair
            RocksDBVerifyReport afterRepair = service.verifyCurrentState();
            Assertions.assertTrue(afterRepair.isClean(), "expected clean state after repair, got: " + afterRepair);
        }
    }

    @Test
    void testRepairIndexesSerializesConcurrentGlobalUpdate() throws Exception {
        try (RocksDBStoreEngine engine = spy(open("repair-concurrent-update"))) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession global = globalSession("tx-repair-concurrent", GlobalStatus.Begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, global);

            CountDownLatch repairEntered = new CountDownLatch(1);
            CountDownLatch releaseRepair = new CountDownLatch(1);
            CountDownLatch writerStarted = new CountDownLatch(1);
            CountDownLatch writerCompleted = new CountDownLatch(1);
            doAnswer(invocation -> {
                        repairEntered.countDown();
                        await(releaseRepair);
                        invocation.callRealMethod();
                        return null;
                    })
                    .when(engine)
                    .deleteByPrefix(eq(RocksDBColumnFamily.GLOBAL_STATUS_INDEX), any(byte[].class));

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> repair = executor.submit(() -> new RocksDBMaintenanceService(engine).repairIndexes());
                Assertions.assertTrue(repairEntered.await(5, TimeUnit.SECONDS));

                global.setStatus(GlobalStatus.Committing);
                Future<?> writer = executor.submit(() -> {
                    writerStarted.countDown();
                    storeManager.writeSession(LogOperation.GLOBAL_UPDATE, global);
                    writerCompleted.countDown();
                });
                Assertions.assertTrue(writerStarted.await(5, TimeUnit.SECONDS));
                Assertions.assertFalse(writerCompleted.await(200, TimeUnit.MILLISECONDS));

                releaseRepair.countDown();
                repair.get(5, TimeUnit.SECONDS);
                writer.get(5, TimeUnit.SECONDS);
                Assertions.assertTrue(new RocksDBMaintenanceService(engine)
                        .verifyCurrentState()
                        .isClean());
            } finally {
                releaseRepair.countDown();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void testPlanRepairDryRunClassifiesGlobalIndexesWithoutWriting() {
        try (RocksDBStoreEngine engine = open("repair-plan-dry-run")) {
            GlobalSession global = globalSession("repair-plan-dry-run", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            byte[] statusKey =
                    RocksDBKeyCodec.encodeGlobalStatusIndex(global.getStatus(), global.getBeginTime(), global.getXid());
            byte[] transactionKey = RocksDBKeyCodec.encodeTransactionIdIndex(global.getTransactionId());
            byte[] wrongXid = "wrong-xid".getBytes(StandardCharsets.UTF_8);
            engine.delete(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey);
            engine.put(RocksDBColumnFamily.TRANSACTION_ID_INDEX, transactionKey, wrongXid);

            RocksDBRepairPlan plan = new RocksDBMaintenanceService(engine).planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.isDryRun());
            Assertions.assertTrue(plan.hasAction(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES));
            Assertions.assertFalse(plan.hasUnrepairableSourceViolation());
            Assertions.assertEquals(3, plan.getBeforeVerifyReport().getInconsistentCount());
            Assertions.assertNull(engine.get(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey));
            Assertions.assertArrayEquals(
                    wrongXid, engine.get(RocksDBColumnFamily.TRANSACTION_ID_INDEX, transactionKey));
        }
    }

    @Test
    void testPlanRepairMarksOrphanBranchAsUnrepairable() {
        try (RocksDBStoreEngine engine = open("repair-plan-unrepairable")) {
            GlobalSession orphanGlobal = globalSession("repair-plan-orphan", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine)
                    .writeSession(LogOperation.BRANCH_ADD, branchSession(orphanGlobal, 1L));

            RocksDBRepairPlan plan = new RocksDBMaintenanceService(engine).planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasUnrepairableSourceViolation());
            Assertions.assertTrue(plan.getActions().isEmpty());
            Assertions.assertEquals(1, plan.getBeforeVerifyReport().getOrphanBranchCount());
        }
    }

    @Test
    void testExecuteRepairRejectsMissingConfirmOrMaintenanceModeWithoutWriting() {
        try (RocksDBStoreEngine engine = open("repair-gate")) {
            GlobalSession global = globalSession("repair-gate", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            byte[] statusKey =
                    RocksDBKeyCodec.encodeGlobalStatusIndex(global.getStatus(), global.getBeginTime(), global.getXid());
            engine.delete(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(false)
                                    .maintenanceMode(true)
                                    .build()));
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(false)
                                    .build()));
            Assertions.assertNull(engine.get(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey));
        }
    }

    @Test
    void testExecuteRepairRebuildsGlobalIndexesAfterExplicitConfirmation() {
        try (RocksDBStoreEngine engine = open("repair-global-indexes")) {
            GlobalSession global = globalSession("repair-global-indexes", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            engine.delete(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(
                            global.getStatus(), global.getBeginTime(), global.getXid()));
            engine.delete(
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                            global.getBeginTime() + global.getTimeout(), global.getXid()));
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(global.getTransactionId()),
                    "wrong-xid".getBytes(StandardCharsets.UTF_8));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            RocksDBRepairReport report = service.executeRepair(
                    plan,
                    RocksDBRepairOptions.builder()
                            .dryRun(false)
                            .confirm(true)
                            .maintenanceMode(true)
                            .build());

            Assertions.assertEquals(4, report.getBeforeVerifyReport().getInconsistentCount());
            Assertions.assertTrue(report.getAfterVerifyReport().isClean());
            Assertions.assertEquals(1, report.getExecutedActionCount());
            Assertions.assertTrue(
                    new RocksDBMaintenanceService(engine).verifyCurrentState().isClean());
        }
    }

    @Test
    void testExecuteRepairTreatsTransactionIdIndexPointingToDifferentTransactionAsStale() {
        try (RocksDBStoreEngine engine = open("repair-stale-transaction-id-index")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession current = globalSession("repair-stale-transaction-current", GlobalStatus.Begin);
            GlobalSession other = globalSession("repair-stale-transaction-other", GlobalStatus.Begin);
            Assertions.assertNotEquals(current.getTransactionId(), other.getTransactionId());
            storeManager.writeSession(LogOperation.GLOBAL_ADD, current);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, other);
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(current.getTransactionId()),
                    other.getXid().getBytes(StandardCharsets.UTF_8));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasAction(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES));
            Assertions.assertFalse(plan.hasUnrepairableSourceViolation());
            Assertions.assertEquals(0, plan.getBeforeVerifyReport().getInvalidGlobalCount());
            Assertions.assertEquals(1, plan.getBeforeVerifyReport().getMissingTransactionIdIndexCount());
            Assertions.assertEquals(1, plan.getBeforeVerifyReport().getStaleTransactionIdIndexCount());

            RocksDBRepairReport report = service.executeRepair(
                    plan,
                    RocksDBRepairOptions.builder()
                            .dryRun(false)
                            .confirm(true)
                            .maintenanceMode(true)
                            .build());

            Assertions.assertTrue(report.getAfterVerifyReport().isClean());
            Assertions.assertTrue(service.verifyCurrentState().isClean());
        }
    }

    @Test
    void testExecuteRepairRejectsInvalidGlobalSessionSource() {
        try (RocksDBStoreEngine engine = open("repair-invalid-global-source")) {
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("repair-invalid-global-source"),
                    new byte[] {1, 2, 3});
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasUnrepairableSourceViolation());
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .build()));
        }
    }

    @Test
    void testExecuteRepairRejectsDuplicateTransactionIdSource() {
        try (RocksDBStoreEngine engine = open("repair-duplicate-transaction-id")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("repair-duplicate-transaction-id-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("repair-duplicate-transaction-id-second", GlobalStatus.Begin);
            second.setTransactionId(first.getTransactionId());
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);
            byte[] transactionKey = RocksDBKeyCodec.encodeTransactionIdIndex(first.getTransactionId());
            byte[] beforeTransactionIndex = engine.get(RocksDBColumnFamily.TRANSACTION_ID_INDEX, transactionKey);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasUnrepairableSourceViolation());
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .build()));
            Assertions.assertArrayEquals(
                    beforeTransactionIndex, engine.get(RocksDBColumnFamily.TRANSACTION_ID_INDEX, transactionKey));
        }
    }

    @Test
    void testPlanRepairRejectsDuplicateTransactionIdSourceWhenIndexIsMissing() {
        try (RocksDBStoreEngine engine = open("repair-duplicate-transaction-id-missing-index")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("repair-duplicate-missing-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("repair-duplicate-missing-second", GlobalStatus.Begin);
            second.setTransactionId(first.getTransactionId());
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);
            engine.delete(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(first.getTransactionId()));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasAction(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES));
            Assertions.assertTrue(plan.hasUnrepairableSourceViolation());
            Assertions.assertEquals(1, plan.getBeforeVerifyReport().getInvalidGlobalCount());
            StoreException exception = Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .build()));
            Assertions.assertTrue(exception.getMessage().contains("unrepairable violations"));
        }
    }

    @Test
    void testPlanRepairRejectsDuplicateTransactionIdSourceWhenIndexPointsToUnrelatedGlobal() {
        try (RocksDBStoreEngine engine = open("repair-duplicate-transaction-id-unrelated-index")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("repair-duplicate-unrelated-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("repair-duplicate-unrelated-second", GlobalStatus.Begin);
            GlobalSession unrelated = globalSession("repair-duplicate-unrelated-target", GlobalStatus.Begin);
            second.setTransactionId(first.getTransactionId());
            Assertions.assertNotEquals(first.getTransactionId(), unrelated.getTransactionId());
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, unrelated);
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(first.getTransactionId()),
                    unrelated.getXid().getBytes(StandardCharsets.UTF_8));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasAction(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES));
            Assertions.assertTrue(plan.hasUnrepairableSourceViolation());
            Assertions.assertEquals(1, plan.getBeforeVerifyReport().getInvalidGlobalCount());
            Assertions.assertEquals(2, plan.getBeforeVerifyReport().getMissingTransactionIdIndexCount());
            Assertions.assertEquals(1, plan.getBeforeVerifyReport().getStaleTransactionIdIndexCount());
        }
    }

    @Test
    void testRepairSourceUniquenessPreflightHonorsMaxRepairEntries() {
        try (RocksDBStoreEngine engine = open("repair-source-preflight-limit")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("repair-limit-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("repair-limit-second", GlobalStatus.Begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);
            engine.delete(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(first.getTransactionId()));

            StoreException exception =
                    Assertions.assertThrows(StoreException.class, () -> new RocksDBMaintenanceService(engine)
                            .planRepair(RocksDBRepairOptions.builder()
                                    .maxRepairEntries(1)
                                    .build()));

            Assertions.assertTrue(exception.getMessage().contains("maxRepairEntries:1"));
        }
    }

    @Test
    void testExecuteRepairRejectsGlobalSessionKeyPayloadMismatch() {
        try (RocksDBStoreEngine engine = open("repair-global-key-payload-mismatch")) {
            GlobalSession global = globalSession("repair-global-key-payload-mismatch", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            byte[] expectedKey = RocksDBKeyCodec.encodeXid(global.getXid());
            byte[] statusKey =
                    RocksDBKeyCodec.encodeGlobalStatusIndex(global.getStatus(), global.getBeginTime(), global.getXid());
            byte[] rawValue = engine.get(RocksDBColumnFamily.GLOBAL_SESSION, expectedKey);
            engine.delete(RocksDBColumnFamily.GLOBAL_SESSION, expectedKey);
            engine.put(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid("different-global-key"), rawValue);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasUnrepairableSourceViolation());
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .build()));
            Assertions.assertArrayEquals(
                    global.getXid().getBytes(StandardCharsets.UTF_8),
                    engine.get(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey));
        }
    }

    @Test
    void testExecuteRepairRejectsRebuildBeyondEntryLimitWithoutWriting() {
        try (RocksDBStoreEngine engine = open("repair-entry-limit")) {
            GlobalSession global = globalSession("repair-entry-limit", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            byte[] statusKey =
                    RocksDBKeyCodec.encodeGlobalStatusIndex(global.getStatus(), global.getBeginTime(), global.getXid());
            engine.delete(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .maxRepairEntries(1)
                                    .build()));
            Assertions.assertNull(engine.get(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey));
        }
    }

    @Test
    void testMaintenanceLockBlocksConcurrentWrite() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-maintenance-lock")) {
            CountDownLatch maintenanceEntered = new CountDownLatch(1);
            CountDownLatch releaseMaintenance = new CountDownLatch(1);
            CountDownLatch writerStarted = new CountDownLatch(1);
            CountDownLatch writerCompleted = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> maintenance = executor.submit(() -> engine.withMaintenanceLock(() -> {
                    maintenanceEntered.countDown();
                    await(releaseMaintenance);
                    return null;
                }));
                Assertions.assertTrue(maintenanceEntered.await(5, TimeUnit.SECONDS));
                Future<?> writer = executor.submit(() -> {
                    writerStarted.countDown();
                    engine.put(
                            RocksDBColumnFamily.METADATA,
                            "repair-maintenance-lock".getBytes(StandardCharsets.UTF_8),
                            "blocked".getBytes(StandardCharsets.UTF_8));
                    writerCompleted.countDown();
                });
                Assertions.assertTrue(writerStarted.await(5, TimeUnit.SECONDS));
                Assertions.assertFalse(writerCompleted.await(200, TimeUnit.MILLISECONDS));

                releaseMaintenance.countDown();
                maintenance.get(5, TimeUnit.SECONDS);
                writer.get(5, TimeUnit.SECONDS);
                Assertions.assertArrayEquals(
                        "blocked".getBytes(StandardCharsets.UTF_8),
                        engine.get(
                                RocksDBColumnFamily.METADATA,
                                "repair-maintenance-lock".getBytes(StandardCharsets.UTF_8)));
            } finally {
                releaseMaintenance.countDown();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void testLockIndexRepairDeletesOnlyCanonicalDuplicateAndPauses() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-paused")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-paused", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertTrue(plan.hasAction(RocksDBRepairPlan.Action.DELETE_STALE_LOCK_BRANCH_INDEXES));
            RocksDBRepairReport report = service.executeRepair(plan, lockIndexRepairOptions("lock-run-paused", 1, 1));

            Assertions.assertEquals(RocksDBRepairReport.State.PAUSED, report.getState());
            Assertions.assertEquals(1, report.getDeletedLockIndexCount());
            Assertions.assertNotNull(report.getNextSeekKey());
            Assertions.assertNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
            Assertions.assertArrayEquals(fixture.lockValue, engine.get(RocksDBColumnFamily.LOCK, fixture.lockKey));
            Assertions.assertArrayEquals(
                    fixture.lockKey, engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, fixture.canonicalIndexKey));
            Assertions.assertNotNull(
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
        }
    }

    @Test
    void testLockIndexRepairResumesSameRunIdWithoutRepeatingDeletedEntries() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-resume")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-resume", 1L);
            byte[] firstStaleKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            byte[] secondStaleKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 2L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, firstStaleKey, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, secondStaleKey, fixture.lockKey);
            RocksDBMaintenanceService firstService = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = firstService.planRepair(RocksDBRepairOptions.defaults());

            RocksDBRepairReport first =
                    firstService.executeRepair(plan, lockIndexRepairOptions("lock-run-resume", 2, 1));
            Assertions.assertEquals(RocksDBRepairReport.State.PAUSED, first.getState());
            Assertions.assertEquals(1, first.getDeletedLockIndexCount());
            Assertions.assertNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, firstStaleKey));

            RocksDBMaintenanceService resumedService = new RocksDBMaintenanceService(engine);
            RocksDBRepairReport second = resumedService.executeRepair(
                    resumedService.planRepair(RocksDBRepairOptions.defaults()),
                    lockIndexRepairOptions("lock-run-resume", 2, 2));

            Assertions.assertEquals(RocksDBRepairReport.State.COMPLETED, second.getState());
            Assertions.assertEquals(2, second.getDeletedLockIndexCount());
            Assertions.assertNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, secondStaleKey));
            Assertions.assertArrayEquals(fixture.lockValue, engine.get(RocksDBColumnFamily.LOCK, fixture.lockKey));
            Assertions.assertNull(
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
        }
    }

    @Test
    void testLockIndexRepairRejectsDifferentRunIdForPersistedCursor() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-run-id")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-run-id", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());
            service.executeRepair(plan, lockIndexRepairOptions("lock-run-first", 1, 1));
            byte[] beforeProgress =
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY);

            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            service.planRepair(RocksDBRepairOptions.defaults()),
                            lockIndexRepairOptions("lock-run-other", 1, 1)));
            Assertions.assertArrayEquals(
                    beforeProgress,
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
            Assertions.assertArrayEquals(
                    fixture.lockKey, engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, fixture.canonicalIndexKey));
        }
    }

    @Test
    void testLockIndexRepairStopsWhenCanonicalIndexIsMissing() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-stopped")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-stopped", 1L);
            engine.delete(RocksDBColumnFamily.LOCK_BRANCH_INDEX, fixture.canonicalIndexKey);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            RocksDBRepairReport stopped = service.executeRepair(
                    service.planRepair(RocksDBRepairOptions.defaults()),
                    lockIndexRepairOptions("lock-run-stopped", 1, 1));

            Assertions.assertEquals(RocksDBRepairReport.State.STOPPED, stopped.getState());
            Assertions.assertEquals(0, stopped.getExecutedActionCount());
            Assertions.assertArrayEquals(fixture.lockValue, engine.get(RocksDBColumnFamily.LOCK, fixture.lockKey));
            Assertions.assertArrayEquals(
                    fixture.lockKey, engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            service.planRepair(RocksDBRepairOptions.defaults()),
                            lockIndexRepairOptions("lock-run-stopped", 1, 1)));
        }
    }

    @Test
    void testLockIndexRepairStopsWhenFinalFullVerifyWorsens() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-final-verify")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-final-verify", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            RocksDBMaintenanceService service = new FinalVerifyWorseningMaintenanceService(engine);

            RocksDBRepairReport report = service.executeRepair(
                    service.planRepair(RocksDBRepairOptions.defaults()),
                    lockIndexRepairOptions("lock-run-final-verify", 10, 1));

            Assertions.assertEquals(RocksDBRepairReport.State.STOPPED, report.getState());
            Assertions.assertEquals(1, report.getDeletedLockIndexCount());
            Assertions.assertEquals(2, report.getAfterVerifyReport().getInconsistentCount());
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            service.planRepair(RocksDBRepairOptions.defaults()),
                            lockIndexRepairOptions("lock-run-final-verify", 10, 1)));
        }
    }

    @Test
    void testLockIndexRepairSerializesConcurrentRuns() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-concurrent")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-concurrent", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            CountDownLatch firstVerifyEntered = new CountDownLatch(1);
            CountDownLatch releaseFirstVerify = new CountDownLatch(1);
            CountDownLatch secondVerifyEntered = new CountDownLatch(1);
            RocksDBMaintenanceService first =
                    new BlockingVerifyMaintenanceService(engine, firstVerifyEntered, releaseFirstVerify);
            RocksDBMaintenanceService second = new ObservingVerifyMaintenanceService(engine, secondVerifyEntered);
            RocksDBRepairPlan plan = first.planRepair(RocksDBRepairOptions.defaults());
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<RocksDBRepairReport> firstRun = executor.submit(
                        () -> first.executeRepair(plan, lockIndexRepairOptions("lock-run-concurrent", 1, 1)));
                Assertions.assertTrue(firstVerifyEntered.await(5, TimeUnit.SECONDS));
                Future<RocksDBRepairReport> secondRun = executor.submit(() -> second.executeRepair(
                        second.planRepair(RocksDBRepairOptions.defaults()),
                        lockIndexRepairOptions("lock-run-concurrent", 1, 1)));

                boolean secondEnteredBeforeFirstReleased = secondVerifyEntered.await(200, TimeUnit.MILLISECONDS);
                releaseFirstVerify.countDown();
                Assertions.assertEquals(
                        RocksDBRepairReport.State.PAUSED,
                        firstRun.get(5, TimeUnit.SECONDS).getState());
                Assertions.assertTrue(secondVerifyEntered.await(5, TimeUnit.SECONDS));
                secondRun.get(5, TimeUnit.SECONDS);
                Assertions.assertFalse(secondEnteredBeforeFirstReleased);
            } finally {
                releaseFirstVerify.countDown();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void testLockIndexRepairStopsWhenBranchSourceCannotBeDecoded() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-invalid-branch")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-invalid-branch", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            engine.put(
                    RocksDBColumnFamily.BRANCH_SESSION,
                    RocksDBKeyCodec.encodeBranch(fixture.global.getXid(), 1L),
                    new byte[] {1, 2, 3});
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            RocksDBRepairReport stopped = service.executeRepair(
                    service.planRepair(RocksDBRepairOptions.defaults()),
                    lockIndexRepairOptions("lock-run-invalid-branch", 1, 1));

            Assertions.assertEquals(RocksDBRepairReport.State.STOPPED, stopped.getState());
            Assertions.assertArrayEquals(fixture.lockValue, engine.get(RocksDBColumnFamily.LOCK, fixture.lockKey));
            Assertions.assertArrayEquals(
                    fixture.lockKey, engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
        }
    }

    @Test
    void testResetStoppedLockIndexProgressRequiresSafetyGatesAndMatchingRunId() {
        try (RocksDBStoreEngine engine = open("reset-stopped-lock-index-gates")) {
            byte[] stoppedProgress = new RocksDBLockIndexRepairProgress(
                            RocksDBLockIndexRepairProgress.State.STOPPED, "old-run", null, 2)
                    .encode();
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY,
                    stoppedProgress);
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.resetStoppedLockIndexProgress("old-run", resetOptions(false, true)));
            Assertions.assertArrayEquals(
                    stoppedProgress,
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.resetStoppedLockIndexProgress("old-run", resetOptions(true, false)));
            Assertions.assertArrayEquals(
                    stoppedProgress,
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.resetStoppedLockIndexProgress("other-run", resetOptions(true, true)));
            Assertions.assertArrayEquals(
                    stoppedProgress,
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
        }
    }

    @Test
    void testResetStoppedLockIndexProgressRejectsUnrepairableSource() {
        try (RocksDBStoreEngine engine = open("reset-stopped-lock-index-unrepairable")) {
            byte[] stoppedProgress = new RocksDBLockIndexRepairProgress(
                            RocksDBLockIndexRepairProgress.State.STOPPED, "old-run", null, 2)
                    .encode();
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY,
                    stoppedProgress);
            GlobalSession orphanGlobal = globalSession("reset-stopped-orphan", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine)
                    .writeSession(LogOperation.BRANCH_ADD, branchSession(orphanGlobal, 1L));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.resetStoppedLockIndexProgress("old-run", resetOptions(true, true)));
            Assertions.assertArrayEquals(
                    stoppedProgress,
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
        }
    }

    @Test
    void testResetStoppedLockIndexProgressAllowsNewRunAfterVerifiedReset() throws Exception {
        try (RocksDBStoreEngine engine = open("reset-stopped-lock-index-new-run")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "reset-stopped-lock-index-new-run", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY,
                    new RocksDBLockIndexRepairProgress(RocksDBLockIndexRepairProgress.State.STOPPED, "old-run", null, 0)
                            .encode());
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);

            service.resetStoppedLockIndexProgress("old-run", resetOptions(true, true));

            Assertions.assertNull(
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
            RocksDBRepairReport report = service.executeRepair(
                    service.planRepair(RocksDBRepairOptions.defaults()), lockIndexRepairOptions("new-run", 10, 1));
            Assertions.assertEquals(RocksDBRepairReport.State.COMPLETED, report.getState());
            Assertions.assertNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
            Assertions.assertArrayEquals(
                    fixture.lockKey, engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, fixture.canonicalIndexKey));
            Assertions.assertNull(
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
        }
    }

    @Test
    void testExecuteLockIndexRepairRejectsIncompleteExecutionPreflightWithoutWriting() throws Exception {
        try (RocksDBStoreEngine engine = open("repair-lock-index-incomplete-preflight")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "repair-lock-index-incomplete-preflight", 1L);
            byte[] staleIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), 0L, fixture.lockKey);
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, fixture.lockKey);
            for (long branchId = 2L; branchId < 302L; branchId++) {
                engine.put(
                        RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                        RocksDBKeyCodec.encodeLockBranchIndex(fixture.global.getXid(), branchId, fixture.lockKey),
                        fixture.lockKey);
            }
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());
            int indexCountBefore = engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                    .size();
            byte[] progressBefore =
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY);

            Assertions.assertTrue(plan.isVerificationComplete());
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .verifyDeadlineMillis(0)
                                    .lockIndexRunId("lock-run-incomplete-preflight")
                                    .lockIndexBatchLimit(10)
                                    .maxLockIndexBatches(1)
                                    .build()));
            Assertions.assertEquals(
                    indexCountBefore,
                    engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                            .size());
            Assertions.assertArrayEquals(
                    fixture.lockKey, engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
            Assertions.assertArrayEquals(
                    progressBefore,
                    engine.get(RocksDBColumnFamily.METADATA, RocksDBMaintenanceService.LOCK_INDEX_REPAIR_PROGRESS_KEY));
        }
    }

    @Test
    void testVerifyRejectsTrailingBytesOnAllSecondaryIndexKeys() throws Exception {
        try (RocksDBStoreEngine engine = open("verify-non-canonical-index-keys")) {
            LockIndexFixture fixture = lockIndexFixture(engine, "verify-non-canonical-index-keys", 1L);
            GlobalSession global = fixture.global;
            byte[] xid = global.getXid().getBytes(StandardCharsets.UTF_8);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    withTrailingByte(RocksDBKeyCodec.encodeGlobalStatusIndex(
                            global.getStatus(), global.getBeginTime(), global.getXid())),
                    xid);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    withTrailingByte(RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                            global.getBeginTime() + global.getTimeout(), global.getXid())),
                    xid);
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    withTrailingByte(RocksDBKeyCodec.encodeTransactionIdIndex(global.getTransactionId())),
                    xid);
            engine.put(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    withTrailingByte(fixture.canonicalIndexKey),
                    fixture.lockKey);

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();

            Assertions.assertEquals(1, report.getStaleStatusIndexCount());
            Assertions.assertEquals(1, report.getStaleTimeoutIndexCount());
            Assertions.assertEquals(1, report.getStaleTransactionIdIndexCount());
            Assertions.assertEquals(1, report.getStaleLockIndexCount());
        }
    }

    @Test
    void testExecuteRepairRejectsIncompleteVerification() {
        try (RocksDBStoreEngine engine = open("repair-incomplete-verification")) {
            for (int i = 0; i < 300; i++) {
                String xid = "stale-status-" + i;
                engine.put(
                        RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                        RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, i, xid),
                        xid.getBytes(StandardCharsets.UTF_8));
            }
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairOptions options =
                    RocksDBRepairOptions.builder().verifyDeadlineMillis(0).build();
            RocksDBRepairPlan plan = service.planRepair(options);

            Assertions.assertFalse(plan.isVerificationComplete());
            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .verifyDeadlineMillis(0)
                                    .build()));
        }
    }

    @Test
    void testExecuteRepairRejectsUnrepairableSourceViolation() {
        try (RocksDBStoreEngine engine = open("repair-unrepairable-gate")) {
            GlobalSession orphanGlobal = globalSession("repair-execute-orphan", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine)
                    .writeSession(LogOperation.BRANCH_ADD, branchSession(orphanGlobal, 1L));
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBRepairPlan plan = service.planRepair(RocksDBRepairOptions.defaults());

            Assertions.assertThrows(
                    StoreException.class,
                    () -> service.executeRepair(
                            plan,
                            RocksDBRepairOptions.builder()
                                    .dryRun(false)
                                    .confirm(true)
                                    .maintenanceMode(true)
                                    .build()));
            Assertions.assertEquals(
                    1,
                    new RocksDBMaintenanceService(engine).verifyCurrentState().getOrphanBranchCount());
        }
    }

    // ---- Report toString test ----

    @Test
    void testVerifyReportToString() {
        try (RocksDBStoreEngine engine = open("report-tostring")) {
            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            String str = report.toString();
            Assertions.assertTrue(str.contains("RocksDBVerifyReport"));
            Assertions.assertTrue(str.contains("globals=0"));
            Assertions.assertTrue(str.contains("clean=true"));
        }
    }

    // ---- Helper methods ----

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true));
    }

    private GlobalSession globalSession(String name, GlobalStatus status) {
        GlobalSession globalSession = new GlobalSession("app", "group", name, 60000);
        globalSession.setStatus(status);
        return globalSession;
    }

    private RocksDBRepairOptions lockIndexRepairOptions(String runId, int batchLimit, int maxBatches) {
        return RocksDBRepairOptions.builder()
                .dryRun(false)
                .confirm(true)
                .maintenanceMode(true)
                .lockIndexRunId(runId)
                .lockIndexBatchLimit(batchLimit)
                .maxLockIndexBatches(maxBatches)
                .build();
    }

    private RocksDBRepairOptions resetOptions(boolean confirm, boolean maintenanceMode) {
        return RocksDBRepairOptions.builder()
                .dryRun(false)
                .confirm(confirm)
                .maintenanceMode(maintenanceMode)
                .build();
    }

    private byte[] withTrailingByte(byte[] key) {
        byte[] result = Arrays.copyOf(key, key.length + 1);
        result[key.length] = 1;
        return result;
    }

    private LockIndexFixture lockIndexFixture(RocksDBStoreEngine engine, String name, long branchId) throws Exception {
        GlobalSession global = globalSession(name, GlobalStatus.Begin);
        RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
        storeManager.writeSession(LogOperation.GLOBAL_ADD, global);
        BranchSession branch = branchSession(global, branchId);
        storeManager.writeSession(LogOperation.BRANCH_ADD, branch);
        byte[] lockKey = RocksDBKeyCodec.encodeRowLock("repair-resource", "repair-table", name);
        byte[] lockValue = encodeLockHolder(
                global.getXid(), global.getTransactionId(), branchId, "repair-resource", "repair-table", name);
        byte[] canonicalIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(global.getXid(), branchId, lockKey);
        try (WriteBatch batch = new WriteBatch()) {
            engine.put(batch, RocksDBColumnFamily.LOCK, lockKey, lockValue);
            engine.put(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, canonicalIndexKey, lockKey);
            engine.write(batch);
        }
        return new LockIndexFixture(global, lockKey, lockValue, canonicalIndexKey);
    }

    private BranchSession branchSession(GlobalSession globalSession, long branchId) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid(globalSession.getXid());
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(branchId);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/db");
        branchSession.setLockKey("t_order:1");
        return branchSession;
    }

    private byte[] encodeLockHolder(
            String xid, long transactionId, long branchId, String resourceId, String tableName, String pk) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeLockString(out, xid);
        writeLockLong(out, transactionId);
        writeLockLong(out, branchId);
        writeLockString(out, resourceId);
        writeLockString(out, tableName);
        writeLockString(out, pk);
        writeLockString(out, resourceId + ":" + tableName + ":" + pk);
        writeLockInt(out, 0);
        return org.apache.seata.server.storage.rocksdb.RocksDBValueCodec.encode(
                org.apache.seata.server.storage.rocksdb.RocksDBValueCodec.ValueType.LOCK_HOLDER, out.toByteArray());
    }

    private void writeLockString(ByteArrayOutputStream out, String value) {
        if (value == null) {
            writeLockInt(out, -1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeLockInt(out, bytes.length);
        out.write(bytes, 0, bytes.length);
    }

    private void writeLockLong(ByteArrayOutputStream out, long value) {
        byte[] bytes = ByteBuffer.allocate(Long.BYTES).putLong(value).array();
        out.write(bytes, 0, bytes.length);
    }

    private void writeLockInt(ByteArrayOutputStream out, int value) {
        byte[] bytes = ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
        out.write(bytes, 0, bytes.length);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for maintenance operation release");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static final class LockIndexFixture {
        private final GlobalSession global;
        private final byte[] lockKey;
        private final byte[] lockValue;
        private final byte[] canonicalIndexKey;

        private LockIndexFixture(GlobalSession global, byte[] lockKey, byte[] lockValue, byte[] canonicalIndexKey) {
            this.global = global;
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.canonicalIndexKey = canonicalIndexKey;
        }
    }

    private static final class FinalVerifyWorseningMaintenanceService extends RocksDBMaintenanceService {
        private FinalVerifyWorseningMaintenanceService(RocksDBStoreEngine storeEngine) {
            super(storeEngine);
        }

        @Override
        public RocksDBVerifyReport verifyCurrentState() {
            super.verifyCurrentState();
            RocksDBVerifyReport.Builder builder = RocksDBVerifyReport.builder(RocksDBVerifyOptions.full());
            builder.staleLockIndex("injected final verify violation 1");
            builder.staleLockIndex("injected final verify violation 2");
            builder.complete(true);
            return builder.build();
        }
    }

    private static final class BlockingVerifyMaintenanceService extends RocksDBMaintenanceService {
        private final CountDownLatch verifyEntered;
        private final CountDownLatch releaseVerify;

        private BlockingVerifyMaintenanceService(
                RocksDBStoreEngine storeEngine, CountDownLatch verifyEntered, CountDownLatch releaseVerify) {
            super(storeEngine);
            this.verifyEntered = verifyEntered;
            this.releaseVerify = releaseVerify;
        }

        @Override
        public RocksDBVerifyReport verifyCurrentState() {
            verifyEntered.countDown();
            await(releaseVerify);
            return super.verifyCurrentState();
        }
    }

    private static final class ObservingVerifyMaintenanceService extends RocksDBMaintenanceService {
        private final CountDownLatch verifyEntered;

        private ObservingVerifyMaintenanceService(RocksDBStoreEngine storeEngine, CountDownLatch verifyEntered) {
            super(storeEngine);
            this.verifyEntered = verifyEntered;
        }

        @Override
        public RocksDBVerifyReport verifyCurrentState() {
            verifyEntered.countDown();
            return super.verifyCurrentState();
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment() throws Exception {
        java.lang.reflect.Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        java.util.Map<String, Object> objectMap = (java.util.Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }
}
