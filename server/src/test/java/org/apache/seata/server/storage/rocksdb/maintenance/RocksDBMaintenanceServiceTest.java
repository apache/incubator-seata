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
import org.apache.seata.core.model.GlobalStatus;
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
    void testVerifyDetectsMissingGlobalIndexes() {
        try (RocksDBStoreEngine engine = open("verify-missing-global-indexes")) {
            GlobalSession global = globalSession("tx-missing-indexes", GlobalStatus.Begin);
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, global);
            engine.delete(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(
                            global.getStatus(), global.getBeginTime(), global.getXid()));
            engine.delete(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(global.getTransactionId()));

            RocksDBVerifyReport report = new RocksDBMaintenanceService(engine).verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getStaleStatusIndexCount());
            Assertions.assertEquals(1, report.getStaleTransactionIdIndexCount());
            Assertions.assertTrue(report.getErrorMessages().stream()
                    .anyMatch(message -> message.contains("missing global status index")));
            Assertions.assertTrue(report.getErrorMessages().stream()
                    .anyMatch(message -> message.contains("missing transaction id index")));
        }
    }

    @Test
    void testVerifyDetectsOrphanBranch() {
        try (RocksDBStoreEngine engine = open("verify-orphan-branch")) {
            // Write a branch session without a corresponding global session
            byte[] branchKey = RocksDBKeyCodec.encodeBranch("xid-orphan", 1L);
            byte[] branchValue = org.apache.seata.server.storage.rocksdb.RocksDBValueCodec.encode(
                    org.apache.seata.server.storage.rocksdb.RocksDBValueCodec.ValueType.BRANCH_SESSION,
                    "payload".getBytes(StandardCharsets.UTF_8));
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, branchKey, branchValue);

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertFalse(report.isClean());
            Assertions.assertEquals(1, report.getOrphanBranchCount());
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

            // Write consistent LOCK + LOCK_BRANCH_INDEX pair
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-1");
            byte[] lockValue =
                    encodeLockHolder(global.getXid(), global.getTransactionId(), 1L, "resource", "table", "pk-1");
            byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex(global.getXid(), 1L, lockKey);

            try (WriteBatch batch = new WriteBatch()) {
                batch.put(engine.handle(RocksDBColumnFamily.LOCK), lockKey, lockValue);
                batch.put(engine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX), indexKey, lockKey);
                engine.write(batch);
            } catch (Exception e) {
                Assertions.fail(e);
            }

            RocksDBMaintenanceService service = new RocksDBMaintenanceService(engine);
            RocksDBVerifyReport report = service.verifyCurrentState();

            Assertions.assertTrue(report.isClean(), "expected clean state, got: " + report);
            Assertions.assertEquals(1, report.getCheckedLockCount());
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
                batch.put(engine.handle(RocksDBColumnFamily.LOCK), lockKey, lockValue);
                batch.put(engine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX), indexKey, lockKey);
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
            service.repairIndexes();

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

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for repair release");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
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
