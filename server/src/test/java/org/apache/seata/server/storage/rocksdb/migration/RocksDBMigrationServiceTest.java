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
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
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
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class RocksDBMigrationServiceTest {

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
    void testInterruptedMigrationClearsAndReplays() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        GlobalSession stale = globalSession("tx-stale", GlobalStatus.Begin);
        GlobalSession active = globalSession("tx-replayed", GlobalStatus.Begin);
        appendLog(fileLog, active, LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = open("rocksdb-replay")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, stale);
            putMigrationStatus(engine, RocksDBMigrationService.MIGRATION_STATUS_IN_PROGRESS);

            Assertions.assertTrue(new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertNull(storeManager.readSession(stale.getXid(), true));
            Assertions.assertNotNull(storeManager.readSession(active.getXid(), true));
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
    void testDoesNotMarkMigrationWhenFlushBarrierFails() throws Exception {
        Path fileLog = tempDir.resolve("file").resolve("root.data");
        appendLog(fileLog, globalSession("tx-active", GlobalStatus.Begin), LogOperation.GLOBAL_ADD);

        try (RocksDBStoreEngine engine = spy(open("rocksdb-flush-failure"))) {
            doThrow(new StoreException("flush failed")).when(engine).flush();

            Assertions.assertThrows(StoreException.class, () -> new RocksDBMigrationService().migrate(fileLog, engine));

            Assertions.assertFalse(Files.exists(migrationMarker(fileLog)));
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
