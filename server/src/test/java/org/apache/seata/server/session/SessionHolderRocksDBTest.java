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
package org.apache.seata.server.session;

import org.apache.seata.common.Constants;
import org.apache.seata.common.XID;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.file.store.FileSessionLogReplayer;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.lock.RocksDBOrphanLockCleanupController;
import org.apache.seata.server.storage.rocksdb.migration.RocksDBMigrationService;
import org.apache.seata.server.storage.rocksdb.session.RocksDBSessionManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SessionHolderRocksDBTest {

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
        System.clearProperty(ConfigurationKeys.STORE_FILE_DIR);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ROCKSDB_DIR);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE);
        SessionHolder.destroy();
        LockerManagerFactory.destroy();
        RocksDBStoreEngineFactory.destroy();
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    @Test
    void testRocksDBFileEngineInitializesRocksDBSessionManager() {
        configureRocksDBFileMode();

        SessionHolder.init(SessionMode.FILE);

        Assertions.assertTrue(SessionHolder.getRootSessionManager() instanceof RocksDBSessionManager);
    }

    @Test
    void testRocksDBFileEngineReloadsBranchLocks() throws Exception {
        configureRocksDBFileMode();
        SessionHolder.init(SessionMode.FILE);

        BranchSession branchSession = branchSession(1001L, 1L, "t_order:1");
        GlobalSession globalSession = globalSession(branchSession);
        globalSession.add(branchSession);

        SessionHolder.reload(Collections.singletonList(globalSession), SessionMode.FILE);

        BranchSession conflict = branchSession(1002L, 2L, "t_order:1");
        Assertions.assertFalse(LockerManagerFactory.getLockManager().acquireLock(conflict));
    }

    @Test
    void testRocksDBFileEngineMigratesFileSessionLog() throws Exception {
        configureRocksDBFileMode();
        GlobalSession globalSession = globalSession(2001L);
        appendFileLog(globalSession, LogOperation.GLOBAL_ADD);

        SessionHolder.init(SessionMode.FILE);

        GlobalSession actual = SessionHolder.getRootSessionManager().findGlobalSession(globalSession.getXid(), true);
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(globalSession.getXid(), actual.getXid());
    }

    @Test
    void testLegacyFileEngineRejectsMigratedLogBeforeCreatingSessionManager() {
        configureRocksDBFileMode();
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "file");
        Path fileLog = tempDir.resolve("file")
                .resolve(String.valueOf(XID.getPort()))
                .resolve(SessionHolder.ROOT_SESSION_MANAGER_NAME);
        new FileSessionLogReplayer().markMigrated(fileLog);

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertTrue(exception.getMessage().contains("one-way"));
        Assertions.assertTrue(exception.getMessage().contains("checkpoint"));
        Assertions.assertTrue(exception.getMessage().contains("export"));
    }

    @Test
    void testRocksDBFileEngineRecoversEverySessionExactlyOnceAcrossDeadlinePages() throws Exception {
        configurePagedRocksDBFileMode(2, 1);
        LockManager lockManager = recordingLockManager();
        setLockManager(lockManager);
        RocksDBStoreEngine engine = RocksDBStoreEngineFactory.getInstance();
        RocksDBSessionManager writer = new RocksDBSessionManager("recovery-writer", engine);
        List<String> expectedXids = new ArrayList<>();
        int sessionCount = 12;
        for (int i = 0; i < sessionCount; i++) {
            long transactionId = 3000L + i;
            BranchSession branchSession = branchSession(transactionId, transactionId, "t_order:" + i);
            GlobalSession globalSession = globalSession(branchSession);
            globalSession.setBeginTime(100L + i);
            writer.addGlobalSession(globalSession);
            writer.addBranchSession(globalSession, branchSession);
            expectedXids.add(globalSession.getXid());
        }
        markMigrationCompleted(engine);

        SessionHolder.init(SessionMode.FILE);

        ArgumentCaptor<BranchSession> recoveredBranches = ArgumentCaptor.forClass(BranchSession.class);
        Mockito.verify(lockManager, Mockito.times(sessionCount))
                .acquireLock(recoveredBranches.capture(), Mockito.eq(true), Mockito.eq(false));
        Map<String, Long> recoveryCounts = recoveredBranches.getAllValues().stream()
                .collect(Collectors.groupingBy(BranchSession::getXid, Collectors.counting()));
        Assertions.assertEquals(new HashSet<>(expectedXids), recoveryCounts.keySet());
        Assertions.assertTrue(recoveryCounts.values().stream().allMatch(count -> count == 1L));
    }

    @Test
    void testRocksDBFileEngineFailsAfterReloadedPageWhenIntermediatePageReadFails() throws Exception {
        configurePagedRocksDBFileMode(1, 1);
        LockManager lockManager = recordingLockManager();
        setLockManager(lockManager);
        RocksDBStoreEngine engine = RocksDBStoreEngineFactory.getInstance();
        RocksDBSessionManager writer = new RocksDBSessionManager("failing-recovery-writer", engine);
        BranchSession firstBranch = branchSession(4001L, 4001L, "t_order:first");
        GlobalSession first = globalSession(firstBranch);
        first.setBeginTime(100L);
        GlobalSession corrupt = globalSession(4002L);
        corrupt.setBeginTime(200L);
        writer.addGlobalSession(first);
        writer.addBranchSession(first, firstBranch);
        writer.addGlobalSession(corrupt);
        engine.put(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(corrupt.getXid()), new byte[] {0});
        markMigrationCompleted(engine);

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));
        Assertions.assertEquals("invalid RocksDB value header", exception.getMessage());

        ArgumentCaptor<BranchSession> recoveredBranch = ArgumentCaptor.forClass(BranchSession.class);
        Mockito.verify(lockManager, Mockito.times(1))
                .acquireLock(recoveredBranch.capture(), Mockito.eq(true), Mockito.eq(false));
        Assertions.assertEquals(first.getXid(), recoveredBranch.getValue().getXid());
    }

    @Test
    void testRocksDBFileEngineReleasesResourcesWhenStartupRecoveryFails() throws Exception {
        configurePagedRocksDBFileMode(1, 1);
        RocksDBStoreEngine engine = RocksDBStoreEngineFactory.getInstance();
        RocksDBSessionManager writer = new RocksDBSessionManager("failed-startup-writer", engine);
        GlobalSession corrupt = globalSession(4101L);
        writer.addGlobalSession(corrupt);
        byte[] globalKey = RocksDBKeyCodec.encodeXid(corrupt.getXid());
        byte[] validGlobalValue = engine.get(RocksDBColumnFamily.GLOBAL_SESSION, globalKey);
        engine.put(RocksDBColumnFamily.GLOBAL_SESSION, globalKey, new byte[] {0});
        markMigrationCompleted(engine);

        Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertNull(SessionHolder.getRootSessionManager());
        Assertions.assertNull(getRawVGroupMappingManager());
        Assertions.assertNull(getRawLockManager());
        Assertions.assertNull(getOrphanLockCleanupController());
        try (RocksDBStoreEngine reopened = RocksDBStoreEngine.open(RocksDBStoreConfig.fromConfiguration())) {
            reopened.put(RocksDBColumnFamily.GLOBAL_SESSION, globalKey, validGlobalValue);
        }

        SessionHolder.init(SessionMode.FILE);

        Assertions.assertTrue(SessionHolder.getRootSessionManager() instanceof RocksDBSessionManager);
        Assertions.assertNotNull(SessionHolder.getRootSessionManager().findGlobalSession(corrupt.getXid(), false));
    }

    @Test
    void testRocksDBFileEngineFailsWhenLaterPageTerminalReloadFails() throws Exception {
        DefaultCoordinator originalCoordinator = getDefaultCoordinator();
        try {
            configurePagedRocksDBFileMode(1, 1);
            setDefaultCoordinator(Mockito.mock(DefaultCoordinator.class));
            LockManager lockManager = recordingLockManager();
            setLockManager(lockManager);
            RocksDBStoreEngine engine = RocksDBStoreEngineFactory.getInstance();
            RocksDBSessionManager writer = new RocksDBSessionManager("strict-reload-writer", engine);
            BranchSession firstBranch = branchSession(5001L, 5001L, "t_order:first-strict");
            GlobalSession first = globalSession(firstBranch);
            first.setBeginTime(100L);
            GlobalSession committed = globalSession(5002L);
            committed.setBeginTime(200L);
            committed.setStatus(GlobalStatus.Committed);
            writer.addGlobalSession(first);
            writer.addBranchSession(first, firstBranch);
            writer.addGlobalSession(committed);
            Mockito.when(lockManager.releaseGlobalSessionLock(
                            Mockito.argThat(session -> committed.getXid().equals(session.getXid()))))
                    .thenThrow(new TransactionException("terminal cleanup failure"));
            markMigrationCompleted(engine);

            StoreException exception =
                    Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));
            Assertions.assertTrue(exception.getMessage().contains("RocksDB startup reload failed"));

            Mockito.verify(lockManager, Mockito.times(1))
                    .acquireLock(
                            Mockito.argThat(branch -> first.getXid().equals(branch.getXid())),
                            Mockito.eq(true),
                            Mockito.eq(false));
            Mockito.verify(lockManager, Mockito.times(1))
                    .releaseGlobalSessionLock(
                            Mockito.argThat(session -> committed.getXid().equals(session.getXid())));
        } finally {
            setDefaultCoordinator(originalCoordinator);
        }
    }

    @Test
    void testRocksDBFileEngineFailsWhenLaterPageErrorStateRemovalFails() throws Exception {
        configurePagedRocksDBFileMode(1, 1);
        LockManager lockManager = recordingLockManager();
        setLockManager(lockManager);
        RocksDBStoreEngine engine = RocksDBStoreEngineFactory.getInstance();
        RocksDBSessionManager writer = new RocksDBSessionManager("strict-error-removal-writer", engine);
        BranchSession firstBranch = branchSession(5101L, 5101L, "t_order:first-error-removal");
        GlobalSession first = globalSession(firstBranch);
        first.setBeginTime(100L);
        GlobalSession finished = globalSession(5102L);
        finished.setBeginTime(200L);
        finished.setStatus(GlobalStatus.Finished);
        writer.addGlobalSession(first);
        writer.addBranchSession(first, firstBranch);
        writer.addGlobalSession(finished);
        Mockito.when(lockManager.releaseGlobalSessionLock(
                        Mockito.argThat(session -> finished.getXid().equals(session.getXid()))))
                .thenThrow(new TransactionException("error-state removal failure"));
        markMigrationCompleted(engine);

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));
        Assertions.assertTrue(exception.getMessage().contains("RocksDB startup reload failed"));

        Mockito.verify(lockManager, Mockito.times(1))
                .acquireLock(
                        Mockito.argThat(branch -> first.getXid().equals(branch.getXid())),
                        Mockito.eq(true),
                        Mockito.eq(false));
        Mockito.verify(lockManager, Mockito.times(1))
                .releaseGlobalSessionLock(
                        Mockito.argThat(session -> finished.getXid().equals(session.getXid())));
    }

    private void configureRocksDBFileMode() {
        System.setProperty(
                ConfigurationKeys.STORE_FILE_DIR, tempDir.resolve("file").toString());
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        System.setProperty(
                ConfigurationKeys.STORE_FILE_ROCKSDB_DIR,
                tempDir.resolve("rocksdb").toString());
    }

    private void configurePagedRocksDBFileMode(int pageSize, long deadlineMillis) {
        configureRocksDBFileMode();
        System.setProperty(ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, String.valueOf(pageSize));
        System.setProperty(
                ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, String.valueOf(deadlineMillis));
        ConfigurationCache.clear();
    }

    private LockManager recordingLockManager() throws Exception {
        LockManager lockManager = Mockito.mock(LockManager.class);
        Mockito.when(lockManager.acquireLock(Mockito.any(BranchSession.class), Mockito.eq(true), Mockito.eq(false)))
                .thenReturn(true);
        return lockManager;
    }

    private void setLockManager(LockManager lockManager) throws Exception {
        Field field = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        field.set(null, lockManager);
    }

    private void setDefaultCoordinator(DefaultCoordinator coordinator) throws Exception {
        Field field = DefaultCoordinator.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, coordinator);
    }

    private DefaultCoordinator getDefaultCoordinator() throws Exception {
        Field field = DefaultCoordinator.class.getDeclaredField("instance");
        field.setAccessible(true);
        return (DefaultCoordinator) field.get(null);
    }

    private RocksDBOrphanLockCleanupController getOrphanLockCleanupController() throws Exception {
        Field field = SessionHolder.class.getDeclaredField("ROCKSDB_ORPHAN_LOCK_CLEANUP_CONTROLLER");
        field.setAccessible(true);
        return (RocksDBOrphanLockCleanupController) field.get(null);
    }

    private Object getRawVGroupMappingManager() throws Exception {
        Field field = SessionHolder.class.getDeclaredField("ROOT_VGROUP_MAPPING_MANAGER");
        field.setAccessible(true);
        return field.get(null);
    }

    private LockManager getRawLockManager() throws Exception {
        Field field = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        return (LockManager) field.get(null);
    }

    private void markMigrationCompleted(RocksDBStoreEngine engine) {
        engine.put(
                RocksDBColumnFamily.METADATA,
                RocksDBMigrationService.MIGRATION_STATUS_KEY.getBytes(StandardCharsets.UTF_8),
                RocksDBMigrationService.MIGRATION_STATUS_COMPLETED.getBytes(StandardCharsets.UTF_8));
    }

    private GlobalSession globalSession(BranchSession branchSession) {
        GlobalSession globalSession = globalSession(branchSession.getTransactionId());
        globalSession.setXid(branchSession.getXid());
        return globalSession;
    }

    private GlobalSession globalSession(long transactionId) {
        GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
        globalSession.setXid("127.0.0.1:8091:" + transactionId);
        globalSession.setTransactionId(transactionId);
        globalSession.setStatus(GlobalStatus.Begin);
        return globalSession;
    }

    private BranchSession branchSession(long transactionId, long branchId, String lockKey) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid("127.0.0.1:8091:" + transactionId);
        branchSession.setTransactionId(transactionId);
        branchSession.setBranchId(branchId);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/db");
        branchSession.setLockKey(lockKey);
        return branchSession;
    }

    private void appendFileLog(SessionStorable session, LogOperation logOperation) throws IOException {
        byte[] data = new TransactionWriteStore(session, logOperation).encode();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        Path fileLog = tempDir.resolve("file")
                .resolve(String.valueOf(XID.getPort()))
                .resolve(SessionHolder.ROOT_SESSION_MANAGER_NAME);
        Files.createDirectories(fileLog.getParent());
        Files.write(fileLog, buffer.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
