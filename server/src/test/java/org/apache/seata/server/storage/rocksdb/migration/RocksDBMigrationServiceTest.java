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
package org.apache.seata.server.storage.rocksdb.migration;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.file.store.FileSessionLogReplayer;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.index.RocksDBIndexManager;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class RocksDBMigrationServiceTest {

    private static final String REMOVED_XID_METADATA_PREFIX = "migration_removed_xid:";

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
        LockerManagerFactory.destroy();
        RocksDBStoreEngineFactory.destroy();
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    @Test
    void testMigratesFileSessionsToRocksDBCurrentState() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession active = globalSession("tx-active", GlobalStatus.Begin);
        BranchSession branch = branchSession(active, 1L);
        GlobalSession committed = globalSession("tx-committed", GlobalStatus.Committed);
        appendLog(fileLog, active, LogOperation.GLOBAL_ADD);
        appendLog(fileLog, branch, LogOperation.BRANCH_ADD);
        appendLog(fileLog, committed, LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = open("rocksdb-current-state")) {
            RocksDBMigrationService migrationService = new RocksDBMigrationService();

            Assertions.assertTrue(migrationService.migrate(fileLog, engine));
            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));

            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession actual = storeManager.readSession(active.getXid(), true);
            Assertions.assertNotNull(actual);
            Assertions.assertEquals(1, actual.getBranchSessions().size());
            Assertions.assertEquals(
                    branch.getBranchId(), actual.getBranchSessions().get(0).getBranchId());
            Assertions.assertNull(storeManager.readSession(committed.getXid(), true));
            SessionCondition transactionIdCondition = new SessionCondition();
            transactionIdCondition.setTransactionId(active.getTransactionId());
            Assertions.assertEquals(
                    active.getXid(),
                    storeManager.readSession(transactionIdCondition).get(0).getXid());
            Assertions.assertEquals(
                    active.getXid(),
                    storeManager
                            .readSession(new GlobalStatus[] {GlobalStatus.Begin}, false)
                            .get(0)
                            .getXid());
            Assertions.assertTrue(Files.isRegularFile(migrationMarker(fileLog)));

            Assertions.assertFalse(migrationService.migrate(fileLog, engine));
        }
    }

    @Test
    void testInterruptedMigrationWithoutSourceFailsFast() {
        try (RocksDBStoreEngine engine = open("rocksdb-missing-source")) {
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_IN_PROGRESS);

            StoreException exception = Assertions.assertThrows(StoreException.class, () -> new RocksDBMigrationService()
                    .migrate(tempDir.resolve("missing").resolve("root.data"), engine));
            Assertions.assertTrue(exception.getMessage().contains("source file logs are missing"));
        }
    }

    @Test
    void testCurrentStateWithoutMigrationStatusFailsFast() {
        try (RocksDBStoreEngine engine = open("rocksdb-missing-status")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("tx-existing", GlobalStatus.Begin));

            StoreException exception = Assertions.assertThrows(StoreException.class, () -> new RocksDBMigrationService()
                    .migrate(tempDir.resolve("missing").resolve("root.data"), engine));
            Assertions.assertTrue(exception.getMessage().contains("without migration status"));
        }
    }

    @Test
    void testEmptyLegacySourceCreatesOneWayGuardBeforeCompleting() {
        Path fileLog = tempDir.resolve("empty-file").resolve("root.data");
        FileSessionLogReplayer replayer = new FileSessionLogReplayer();

        try (RocksDBStoreEngine engine = open("rocksdb-empty-source-guard")) {
            Assertions.assertFalse(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));
            Assertions.assertTrue(Files.isRegularFile(migrationMarker(fileLog)));
            Assertions.assertThrows(StoreException.class, () -> replayer.ensureLegacyFileModeAllowed(fileLog));
        }
    }

    @Test
    void testEmptySourceGuardRecoversAfterMarkerBeforeCompletedStatus() {
        Path fileLog = tempDir.resolve("guarded-empty-file").resolve("root.data");
        FileSessionLogReplayer replayer = new FileSessionLogReplayer();

        try (RocksDBStoreEngine engine = open("rocksdb-empty-source-guard-recovery")) {
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_GUARDING_EMPTY);
            engine.flush();
            replayer.markMigrated(fileLog);

            Assertions.assertFalse(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));
            Assertions.assertThrows(StoreException.class, () -> replayer.ensureLegacyFileModeAllowed(fileLog));
        }
    }

    @Test
    void testEmptySourceGuardFlushFailureRemainsRetryableBeforeMarker() {
        Path fileLog = tempDir.resolve("guard-flush-failure").resolve("root.data");

        try (RocksDBStoreEngine engine = spy(open("rocksdb-empty-source-guard-flush-failure"))) {
            doThrow(new StoreException("flush failed"))
                    .doCallRealMethod()
                    .when(engine)
                    .flush();
            RocksDBMigrationService service = new RocksDBMigrationService();

            Assertions.assertThrows(StoreException.class, () -> service.migrate(fileLog, engine));

            Assertions.assertEquals(
                    RocksDBMigrationService.MIGRATION_STATUS_GUARDING_EMPTY, getMigrationStatus(engine));
            Assertions.assertFalse(Files.exists(migrationMarker(fileLog)));

            Assertions.assertFalse(service.migrate(fileLog, engine));
            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));
            Assertions.assertTrue(Files.isRegularFile(migrationMarker(fileLog)));
        }
    }

    @Test
    void testEmptySourceGuardRejectsUnexpectedLegacyLogs() throws Exception {
        Path fileLog = tempDir.resolve("guard-with-source").resolve("root.data");
        appendLog(fileLog, globalSession("tx-unexpected-source", GlobalStatus.Begin), LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = open("rocksdb-empty-source-guard-with-source")) {
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_GUARDING_EMPTY);

            StoreException exception = Assertions.assertThrows(
                    StoreException.class, () -> new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertTrue(exception.getMessage().contains("guarding empty legacy source"));
            Assertions.assertTrue(exception.getMessage().contains("source file logs exist"));
        }
    }

    @Test
    void testEmptySourceGuardRejectsUnexpectedCurrentState() {
        Path fileLog = tempDir.resolve("guard-with-current-state").resolve("root.data");

        try (RocksDBStoreEngine engine = open("rocksdb-empty-source-guard-with-current-state")) {
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_GUARDING_EMPTY);
            new RocksDBTransactionStoreManager(engine)
                    .writeSession(LogOperation.GLOBAL_ADD, globalSession("tx-unexpected-state", GlobalStatus.Begin));

            StoreException exception = Assertions.assertThrows(
                    StoreException.class, () -> new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertTrue(exception.getMessage().contains("guarding empty legacy source"));
            Assertions.assertTrue(exception.getMessage().contains("RocksDB current state exists"));
        }
    }

    @Test
    void testInterruptedMigrationClearsAndReplays() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession stale = globalSession("tx-stale", GlobalStatus.Begin);
        GlobalSession active = globalSession("tx-replayed", GlobalStatus.Begin);
        appendLog(fileLog, active, LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = open("rocksdb-replay")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, stale);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(stale.getBeginTime() + stale.getTimeout(), stale.getXid()),
                    stale.getXid().getBytes(StandardCharsets.UTF_8));
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_IN_PROGRESS);

            Assertions.assertTrue(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertNull(storeManager.readSession(stale.getXid(), true));
            Assertions.assertNull(engine.get(
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                            stale.getBeginTime() + stale.getTimeout(), stale.getXid())));
            Assertions.assertNotNull(storeManager.readSession(active.getXid(), true));
            Assertions.assertArrayEquals(
                    active.getXid().getBytes(StandardCharsets.UTF_8),
                    engine.get(
                            RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                            RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                                    RocksDBIndexManager.timeoutDeadlineMillis(active), active.getXid())));
            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));
        }
    }

    @Test
    void testMigratedFileLogsWithoutRocksDBMetadataFailFast() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        appendLog(fileLog, globalSession("tx-active", GlobalStatus.Begin), LogOperation.GLOBAL_ADD);
        new FileSessionLogReplayer().markMigrated(fileLog);

        try (RocksDBStoreEngine engine = open("rocksdb-migrated-marker")) {
            StoreException exception = Assertions.assertThrows(
                    StoreException.class, () -> new RocksDBMigrationService().migrate(fileLog, engine));
            Assertions.assertTrue(exception.getMessage().contains("already migrated"));
        }
    }

    @Test
    void testLegacyFileModeRejectsMigratedMarkerWithRecoveryGuidance() {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        FileSessionLogReplayer replayer = new FileSessionLogReplayer();
        replayer.markMigrated(fileLog);

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> replayer.ensureLegacyFileModeAllowed(fileLog));

        Assertions.assertTrue(exception.getMessage().contains("one-way"));
        Assertions.assertTrue(exception.getMessage().contains("checkpoint"));
        Assertions.assertTrue(exception.getMessage().contains("export"));
    }

    @Test
    void testMigrationReplaysCurrentStateWithoutRevivingRemovedTransactions() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession active = globalSession("tx-active-ordered", GlobalStatus.Begin);
        BranchSession activeBranch = branchSession(active, 1L);
        GlobalSession removed = globalSession("tx-removed", GlobalStatus.Begin);
        BranchSession removedBranch = branchSession(removed, 2L);
        GlobalSession orphan = globalSession("tx-orphan", GlobalStatus.Begin);
        BranchSession orphanBranch = branchSession(orphan, 3L);

        appendLog(fileLog, activeBranch, LogOperation.BRANCH_ADD);
        appendLog(fileLog, active, LogOperation.GLOBAL_ADD);
        appendLog(fileLog, removed, LogOperation.GLOBAL_ADD);
        appendLog(fileLog, removedBranch, LogOperation.BRANCH_ADD);
        appendLog(fileLog, removed, LogOperation.GLOBAL_REMOVE);
        appendLog(fileLog, removed, LogOperation.GLOBAL_UPDATE);
        appendLog(fileLog, removedBranch, LogOperation.BRANCH_UPDATE);
        appendLog(fileLog, orphanBranch, LogOperation.BRANCH_ADD);
        for (int i = 0; i < 2048; i++) {
            GlobalSession historical = globalSession("tx-history-" + i, GlobalStatus.Begin);
            appendLog(fileLog, historical, LogOperation.GLOBAL_ADD);
            appendLog(fileLog, historical, LogOperation.GLOBAL_REMOVE);
        }

        try (RocksDBStoreEngine engine = open("rocksdb-streaming-replay")) {
            Assertions.assertTrue(new RocksDBMigrationService().migrate(fileLog, engine));

            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession actual = storeManager.readSession(active.getXid(), true);
            Assertions.assertNotNull(actual);
            Assertions.assertEquals(
                    activeBranch.getBranchId(),
                    actual.getBranchSessions().get(0).getBranchId());
            Assertions.assertNull(storeManager.readSession(removed.getXid(), true));
            Assertions.assertNull(storeManager.readSession(orphan.getXid(), true));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(orphan.getXid())));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.METADATA, REMOVED_XID_METADATA_PREFIX.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void testMigrationGlobalRemoveDeletesHighFanOutBranchesInBoundedBatches() throws Exception {
        Path fileLog = tempDir.resolve("fan-out-file").resolve("root.data");
        GlobalSession removed = globalSession("tx-high-fan-out", GlobalStatus.Begin);
        byte[] branchPrefix = RocksDBKeyCodec.encodeXidPrefix(removed.getXid());
        appendLog(fileLog, removed, LogOperation.GLOBAL_ADD);
        for (int i = 0; i < 1025; i++) {
            appendLog(fileLog, branchSession(removed, i + 1L), LogOperation.BRANCH_ADD);
        }
        appendLog(fileLog, removed, LogOperation.GLOBAL_REMOVE);

        try (RocksDBStoreEngine engine = spy(open("rocksdb-high-fan-out-remove"))) {
            doAnswer(invocation -> {
                        byte[] prefix = invocation.getArgument(1);
                        if (engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, prefix)) {
                            throw new AssertionError("migration attempted an unbounded branch prefix scan");
                        }
                        return invocation.callRealMethod();
                    })
                    .when(engine)
                    .prefixScan(eq(RocksDBColumnFamily.BRANCH_SESSION), any(byte[].class));

            Assertions.assertTrue(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertNull(new RocksDBTransactionStoreManager(engine).readSession(removed.getXid(), true));
            Assertions.assertFalse(engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, branchPrefix));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.METADATA, REMOVED_XID_METADATA_PREFIX.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void testMigrationReplayDoesNotReintroduceDatabaseSizedStateHolder() {
        Assertions.assertFalse(Arrays.stream(RocksDBMigrationService.class.getDeclaredClasses())
                .anyMatch(type -> "MigrationState".equals(type.getSimpleName())));
    }

    @Test
    void testInterruptedMigrationClearsRemovedTransactionTombstonesBeforeReplay() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession active = globalSession("tx-replayed", GlobalStatus.Begin);
        appendLog(fileLog, active, LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = open("rocksdb-replay-tombstone-cleanup")) {
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    (REMOVED_XID_METADATA_PREFIX + "interrupted").getBytes(StandardCharsets.UTF_8),
                    new byte[] {1});
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_IN_PROGRESS);

            Assertions.assertTrue(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.METADATA, REMOVED_XID_METADATA_PREFIX.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void testFlushFailureKeepsMigrationRetryableAndEventuallyGuardsLegacyMode() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession active = globalSession("tx-active", GlobalStatus.Begin);
        appendLog(fileLog, active, LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = spy(open("rocksdb-flush-failure"))) {
            doThrow(new StoreException("flush failed"))
                    .doCallRealMethod()
                    .when(engine)
                    .flush();
            RocksDBMigrationService migrationService = new RocksDBMigrationService();

            Assertions.assertThrows(StoreException.class, () -> migrationService.migrate(fileLog, engine));

            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_IN_PROGRESS, getMigrationStatus(engine));
            Assertions.assertFalse(Files.exists(migrationMarker(fileLog)));

            Assertions.assertTrue(migrationService.migrate(fileLog, engine));
            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));
            Assertions.assertNotNull(new RocksDBTransactionStoreManager(engine).readSession(active.getXid(), true));
            Assertions.assertThrows(
                    StoreException.class, () -> new FileSessionLogReplayer().ensureLegacyFileModeAllowed(fileLog));
        }
    }

    @Test
    void testCompletedStatusWithoutMarkerRepairsOneWayGuard() {
        Path fileLog = tempDir.resolve("file").resolve("root.data");

        try (RocksDBStoreEngine engine = open("rocksdb-completed-without-marker")) {
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_COMPLETED);

            Assertions.assertFalse(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertTrue(Files.isRegularFile(migrationMarker(fileLog)));
            Assertions.assertThrows(
                    StoreException.class, () -> new FileSessionLogReplayer().ensureLegacyFileModeAllowed(fileLog));
        }
    }

    @Test
    void testMarkerWithInProgressStatusFinishesWithoutSourceWhileLegacyModeIsRejected() {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession active = globalSession("tx-marker-before-completed", GlobalStatus.Begin);
        FileSessionLogReplayer replayer = new FileSessionLogReplayer();

        try (RocksDBStoreEngine engine = open("rocksdb-marker-before-completed")) {
            new RocksDBTransactionStoreManager(engine).writeSession(LogOperation.GLOBAL_ADD, active);
            engine.flush();
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_IN_PROGRESS);
            replayer.markMigrated(fileLog);
            Assertions.assertThrows(StoreException.class, () -> replayer.ensureLegacyFileModeAllowed(fileLog));

            Assertions.assertDoesNotThrow(() -> new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertEquals(RocksDBMigrationService.MIGRATION_STATUS_COMPLETED, getMigrationStatus(engine));
            Assertions.assertNotNull(new RocksDBTransactionStoreManager(engine).readSession(active.getXid(), true));
            Assertions.assertThrows(StoreException.class, () -> replayer.ensureLegacyFileModeAllowed(fileLog));
        }
    }

    @Test
    void testTerminalGlobalUpdatePreventsStaleGlobalAndBranchUpdatesFromRevivingTransaction() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession globalSession = globalSession("tx-terminal-update", GlobalStatus.Begin);
        BranchSession staleBranch = branchSession(globalSession, 7L);
        appendLog(fileLog, globalSession, LogOperation.GLOBAL_ADD);
        globalSession.setStatus(GlobalStatus.Committed);
        appendLog(fileLog, globalSession, LogOperation.GLOBAL_UPDATE);
        globalSession.setStatus(GlobalStatus.Begin);
        appendLog(fileLog, globalSession, LogOperation.GLOBAL_UPDATE);
        appendLog(fileLog, staleBranch, LogOperation.BRANCH_UPDATE);

        try (RocksDBStoreEngine engine = open("rocksdb-terminal-update-tombstone")) {
            Assertions.assertTrue(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertNull(new RocksDBTransactionStoreManager(engine).readSession(globalSession.getXid(), true));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(globalSession.getXid())));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.METADATA, REMOVED_XID_METADATA_PREFIX.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void testReplayRejectsOversizedBody() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        appendFrameSize(fileLog, StoreConfig.getMaxBranchSessionSize() + 2);

        try (RocksDBStoreEngine engine = open("rocksdb-oversized-frame")) {
            StoreException exception = Assertions.assertThrows(
                    StoreException.class, () -> new RocksDBMigrationService().migrate(fileLog, engine));
            Assertions.assertTrue(exception.getMessage().contains("exceeds limit"));
        }
    }

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true));
    }

    private GlobalSession globalSession(String name, GlobalStatus status) {
        GlobalSession globalSession = new GlobalSession("app", "group", name, 60000);
        globalSession.setStatus(status);
        return globalSession;
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

    private void appendLog(Path fileLog, SessionStorable session, LogOperation logOperation) throws IOException {
        byte[] data = new TransactionWriteStore(session, logOperation).encode();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        Files.createDirectories(fileLog.getParent());
        Files.write(fileLog, buffer.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void appendFrameSize(Path fileLog, int bodySize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(bodySize);
        Files.createDirectories(fileLog.getParent());
        Files.write(fileLog, buffer.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private Path migrationMarker(Path fileLog) {
        return fileLog.resolveSibling(fileLog.getFileName() + ".rocksdb_migrated");
    }

    private String getMigrationStatus(RocksDBStoreEngine engine) {
        byte[] value = engine.get(
                RocksDBColumnFamily.METADATA,
                RocksDBMigrationService.MIGRATION_STATUS_KEY.getBytes(StandardCharsets.UTF_8));
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    private void putMigrationStatus(RocksDBStoreEngine engine, String migrationStatus) {
        engine.put(
                RocksDBColumnFamily.METADATA,
                RocksDBMigrationService.MIGRATION_STATUS_KEY.getBytes(StandardCharsets.UTF_8),
                migrationStatus.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment() throws Exception {
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
