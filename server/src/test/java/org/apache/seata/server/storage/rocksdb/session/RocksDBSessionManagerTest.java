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
package org.apache.seata.server.storage.rocksdb.session;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionScanStats;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.lock.RocksDBLockManager;
import org.apache.seata.server.store.TransactionStoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.rocksdb.WriteBatch;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class RocksDBSessionManagerTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;
    private LockManager originalLockManager;
    private MockEnvironment environment;

    @BeforeEach
    void beforeEach() throws Exception {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        originalLockManager = (LockManager) lockerManagerField().get(null);
        environment = new MockEnvironment();
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        ConfigurationCache.clear();
    }

    @AfterEach
    void afterEach() throws Exception {
        lockerManagerField().set(null, originalLockManager);
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    @Test
    void testGlobalSessionLifecycle() throws Exception {
        try (RocksDBStoreEngine engine = open("global")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            GlobalSession globalSession = globalSession("tx-global", GlobalStatus.Begin);

            sessionManager.addGlobalSession(globalSession);

            GlobalSession found = sessionManager.findGlobalSession(globalSession.getXid(), false);
            Assertions.assertNotNull(found);
            Assertions.assertEquals(GlobalStatus.Begin, found.getStatus());

            sessionManager.updateGlobalSessionStatus(globalSession, GlobalStatus.Committing);
            Assertions.assertEquals(
                    GlobalStatus.Committing,
                    sessionManager
                            .findGlobalSession(globalSession.getXid(), false)
                            .getStatus());

            sessionManager.removeGlobalSession(globalSession);
            Assertions.assertNull(sessionManager.findGlobalSession(globalSession.getXid()));
        }
    }

    @Test
    void testBranchSessionLifecycle() throws Exception {
        try (RocksDBStoreEngine engine = open("branch")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            GlobalSession globalSession = globalSession("tx-branch", GlobalStatus.Begin);
            BranchSession branchSession = branchSession(globalSession, 1L);

            sessionManager.addGlobalSession(globalSession);
            sessionManager.addBranchSession(globalSession, branchSession);

            GlobalSession found = sessionManager.findGlobalSession(globalSession.getXid(), true);
            Assertions.assertEquals(1, found.getBranchSessions().size());
            Assertions.assertEquals(
                    branchSession.getBranchId(),
                    found.getBranchSessions().get(0).getBranchId());

            branchSession.setStatus(BranchStatus.PhaseOne_Done);
            sessionManager.updateBranchSessionStatus(branchSession, BranchStatus.PhaseOne_Done);
            found = sessionManager.findGlobalSession(globalSession.getXid(), true);
            Assertions.assertEquals(
                    BranchStatus.PhaseOne_Done, found.getBranchSessions().get(0).getStatus());

            sessionManager.removeBranchSession(globalSession, branchSession);
            Assertions.assertTrue(sessionManager
                    .findGlobalSession(globalSession.getXid(), true)
                    .getBranchSessions()
                    .isEmpty());
        }
    }

    @Test
    void testFindGlobalSessionsAndAllSessions() throws Exception {
        try (RocksDBStoreEngine engine = open("query")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            GlobalSession begin = globalSession("tx-begin", GlobalStatus.Begin);
            GlobalSession committed = globalSession("tx-committed", GlobalStatus.Committed);
            GlobalSession commitRetryTimeout =
                    globalSession("tx-commit-retry-timeout", GlobalStatus.CommitRetryTimeout);
            GlobalSession rollbacked = globalSession("tx-rollbacked", GlobalStatus.Rollbacked);
            GlobalSession rollbackRetryTimeout =
                    globalSession("tx-rollback-retry-timeout", GlobalStatus.RollbackRetryTimeout);
            GlobalSession timeoutRollbacked = globalSession("tx-timeout-rollbacked", GlobalStatus.TimeoutRollbacked);
            GlobalSession finished = globalSession("tx-finished", GlobalStatus.Finished);

            sessionManager.addGlobalSession(begin);
            sessionManager.addGlobalSession(committed);
            sessionManager.addGlobalSession(commitRetryTimeout);
            sessionManager.addGlobalSession(rollbacked);
            sessionManager.addGlobalSession(rollbackRetryTimeout);
            sessionManager.addGlobalSession(timeoutRollbacked);
            sessionManager.addGlobalSession(finished);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            List<GlobalSession> beginSessions = sessionManager.findGlobalSessions(condition);
            Assertions.assertEquals(1, beginSessions.size());
            Assertions.assertEquals(begin.getXid(), beginSessions.get(0).getXid());

            condition = new SessionCondition();
            condition.setTransactionId(committed.getTransactionId());
            List<GlobalSession> byTransactionId = sessionManager.findGlobalSessions(condition);
            Assertions.assertEquals(1, byTransactionId.size());
            Assertions.assertEquals(committed.getXid(), byTransactionId.get(0).getXid());

            Collection<GlobalSession> allSessions = sessionManager.allSessions();
            Assertions.assertEquals(7, allSessions.size());
            Assertions.assertTrue(contains(allSessions, begin));
            Assertions.assertTrue(contains(allSessions, committed));
            Assertions.assertTrue(contains(allSessions, commitRetryTimeout));
            Assertions.assertTrue(contains(allSessions, rollbacked));
            Assertions.assertTrue(contains(allSessions, rollbackRetryTimeout));
            Assertions.assertTrue(contains(allSessions, timeoutRollbacked));
            Assertions.assertTrue(contains(allSessions, finished));
        }
    }

    @Test
    void testCoordinatorStatusQueries() throws Exception {
        try (RocksDBStoreEngine engine = open("coordinator-query")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            GlobalSession begin = globalSession("tx-timeout-begin", GlobalStatus.Begin);
            GlobalSession timeoutRollbacking = globalSession("tx-timeout-rollbacking", GlobalStatus.TimeoutRollbacking);
            GlobalSession timeoutRollbackRetrying =
                    globalSession("tx-timeout-rollback-retrying", GlobalStatus.TimeoutRollbackRetrying);
            GlobalSession rollbackRetrying = globalSession("tx-rollback-retrying", GlobalStatus.RollbackRetrying);
            GlobalSession commitRetrying = globalSession("tx-commit-retrying", GlobalStatus.CommitRetrying);
            GlobalSession rollbacking = globalSession("tx-rollbacking", GlobalStatus.Rollbacking);
            GlobalSession committing = globalSession("tx-committing", GlobalStatus.Committing);
            GlobalSession asyncCommitting = globalSession("tx-async-committing", GlobalStatus.AsyncCommitting);
            GlobalSession rollbacked = globalSession("tx-rollbacked-end", GlobalStatus.Rollbacked);
            GlobalSession timeoutRollbacked =
                    globalSession("tx-timeout-rollbacked-end", GlobalStatus.TimeoutRollbacked);
            GlobalSession committed = globalSession("tx-committed-end", GlobalStatus.Committed);
            GlobalSession finished = globalSession("tx-finished-end", GlobalStatus.Finished);

            for (GlobalSession globalSession : Arrays.asList(
                    begin,
                    timeoutRollbacking,
                    timeoutRollbackRetrying,
                    rollbackRetrying,
                    commitRetrying,
                    rollbacking,
                    committing,
                    asyncCommitting,
                    rollbacked,
                    timeoutRollbacked,
                    committed,
                    finished)) {
                sessionManager.addGlobalSession(globalSession);
            }
            sessionManager.addBranchSession(commitRetrying, branchSession(commitRetrying, 1001L));

            SessionCondition timeoutCondition = new SessionCondition(GlobalStatus.Begin);
            timeoutCondition.setLazyLoadBranch(true);
            List<GlobalSession> timeoutSessions = sessionManager.findGlobalSessions(timeoutCondition);
            assertXids(timeoutSessions, begin.getXid());
            Assertions.assertTrue(timeoutSessions.get(0).isLazyLoadBranch());

            SessionCondition retryRollbackingCondition = new SessionCondition(
                    GlobalStatus.TimeoutRollbacking,
                    GlobalStatus.TimeoutRollbackRetrying,
                    GlobalStatus.RollbackRetrying);
            retryRollbackingCondition.setLazyLoadBranch(true);
            assertXids(
                    sessionManager.findGlobalSessions(retryRollbackingCondition),
                    timeoutRollbacking.getXid(),
                    timeoutRollbackRetrying.getXid(),
                    rollbackRetrying.getXid());

            SessionCondition retryCommittingCondition = new SessionCondition(GlobalStatus.CommitRetrying);
            retryCommittingCondition.setLazyLoadBranch(true);
            List<GlobalSession> retryCommittingSessions = sessionManager.findGlobalSessions(retryCommittingCondition);
            assertXids(retryCommittingSessions, commitRetrying.getXid());
            Assertions.assertTrue(retryCommittingSessions.get(0).isLazyLoadBranch());

            retryCommittingCondition.setLazyLoadBranch(false);
            retryCommittingSessions = sessionManager.findGlobalSessions(retryCommittingCondition);
            assertXids(retryCommittingSessions, commitRetrying.getXid());
            Assertions.assertFalse(retryCommittingSessions.get(0).isLazyLoadBranch());
            Assertions.assertEquals(
                    1, retryCommittingSessions.get(0).getBranchSessions().size());

            assertXids(
                    sessionManager.findGlobalSessions(new SessionCondition(GlobalStatus.Rollbacking)),
                    rollbacking.getXid());
            assertXids(
                    sessionManager.findGlobalSessions(new SessionCondition(GlobalStatus.Committing)),
                    committing.getXid());
            assertXids(
                    sessionManager.findGlobalSessions(new SessionCondition(GlobalStatus.AsyncCommitting)),
                    asyncCommitting.getXid());

            SessionCondition endStateCondition = new SessionCondition(
                    GlobalStatus.Rollbacked,
                    GlobalStatus.TimeoutRollbacked,
                    GlobalStatus.Committed,
                    GlobalStatus.Finished);
            endStateCondition.setLazyLoadBranch(true);
            assertXids(
                    sessionManager.findGlobalSessions(endStateCondition),
                    rollbacked.getXid(),
                    timeoutRollbacked.getXid(),
                    committed.getXid(),
                    finished.getXid());
        }
    }

    @Test
    void testStartupRecoveryPagesRetainContinuationUntilEveryXidIsExhausted() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("startup-recovery-pages")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            GlobalSession begin = globalSession("tx-recovery-begin", GlobalStatus.Begin);
            GlobalSession committing = globalSession("tx-recovery-committing", GlobalStatus.Committing);
            GlobalSession rollbacking = globalSession("tx-recovery-rollbacking", GlobalStatus.Rollbacking);
            begin.setBeginTime(100L);
            committing.setBeginTime(200L);
            rollbacking.setBeginTime(300L);
            sessionManager.addGlobalSession(begin);
            sessionManager.addGlobalSession(committing);
            sessionManager.addGlobalSession(rollbacking);

            RocksDBSessionManager.RecoveryCursor cursor = RocksDBSessionManager.RecoveryCursor.initial();
            Map<String, Integer> recoveryCounts = new HashMap<>();
            int pageCount = 0;
            while (true) {
                RocksDBSessionManager.RecoveryPage page = sessionManager.readStartupRecoveryPage(cursor);
                pageCount++;
                Assertions.assertTrue(page.getSessions().size() <= 1);
                page.getSessions().forEach(session -> recoveryCounts.merge(session.getXid(), 1, Integer::sum));
                if (page.isExhausted()) {
                    Assertions.assertNull(page.getContinuation());
                    break;
                }
                Assertions.assertNotNull(page.getContinuation());
                cursor = page.getContinuation();
                Assertions.assertTrue(pageCount < 20, "recovery pages must make forward progress");
            }

            Assertions.assertTrue(pageCount > 1);
            Assertions.assertEquals(
                    new HashSet<>(Arrays.asList(begin.getXid(), committing.getXid(), rollbacking.getXid())),
                    recoveryCounts.keySet());
            Assertions.assertTrue(recoveryCounts.values().stream().allMatch(count -> count == 1));
        }
    }

    @Test
    void testUnboundedQueriesConsumeContinuationsAndBoundedQueriesExposeThem() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("query-continuations")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            Set<String> beginXids = new HashSet<>();
            Set<String> allXids = new HashSet<>();
            for (int i = 0; i < 300; i++) {
                GlobalSession begin = globalSession("tx-continuation-begin-" + i, GlobalStatus.Begin);
                begin.setBeginTime(i + 1L);
                begin.setTimeout(1);
                sessionManager.addGlobalSession(begin);
                beginXids.add(begin.getXid());
                allXids.add(begin.getXid());

                GlobalSession committing = globalSession("tx-continuation-committing-" + i, GlobalStatus.Committing);
                committing.setBeginTime(1_000L + i);
                sessionManager.addGlobalSession(committing);
                allXids.add(committing.getXid());
            }

            assertEveryXidExactlyOnce(
                    sessionManager.findGlobalSessions(new SessionCondition(GlobalStatus.Begin)), beginXids);
            assertEveryXidExactlyOnce(
                    sessionManager.findGlobalSessions(
                            new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing)),
                    allXids);
            assertEveryXidExactlyOnce(sessionManager.allSessions(), allXids);

            SessionCondition timeoutCondition = new SessionCondition(GlobalStatus.Begin);
            timeoutCondition.setMaxTimeoutDeadlineMillis(System.currentTimeMillis());
            assertEveryXidExactlyOnce(sessionManager.findGlobalSessions(timeoutCondition), beginXids);

            SessionCondition boundedStatus = new SessionCondition(GlobalStatus.Begin);
            boundedStatus.setLimit(1);
            Assertions.assertEquals(
                    1, sessionManager.findGlobalSessions(boundedStatus).size());
            Assertions.assertEquals(
                    SessionCondition.ScanContinuation.RESUMABLE, boundedStatus.getStatusScanContinuation());
            Assertions.assertNotNull(boundedStatus.getNextStatusScanCursor());

            SessionCondition boundedMultiStatus = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            boundedMultiStatus.setScanLimit(1);
            Assertions.assertEquals(
                    1, sessionManager.findGlobalSessions(boundedMultiStatus).size());
            Assertions.assertFalse(boundedMultiStatus.getNextStatusScanCursors().isEmpty());

            SessionCondition boundedTimeout = new SessionCondition(GlobalStatus.Begin);
            boundedTimeout.setLimit(1);
            boundedTimeout.setMaxTimeoutDeadlineMillis(System.currentTimeMillis());
            Assertions.assertEquals(
                    1, sessionManager.findGlobalSessions(boundedTimeout).size());
            Assertions.assertEquals(
                    SessionCondition.ScanContinuation.RESUMABLE, boundedTimeout.getTimeoutScanContinuation());
            Assertions.assertNotNull(boundedTimeout.getNextTimeoutScanCursor());
        }
    }

    @Test
    void testResumableMultiStatusPageWithEmptyCursorsIsProtocolViolation() {
        try (RocksDBStoreEngine engine = open("scripted-empty-multi-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                publishMultiStatusContinuation(
                        condition, Collections.emptyMap(), SessionCondition.ScanContinuation.RESUMABLE);
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("multi-status scan continuation did not advance", exception.getMessage());
            Mockito.verify(storeManager).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testResumableMultiStatusPageCannotClearExistingCursors() {
        try (RocksDBStoreEngine engine = open("scripted-cleared-multi-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            Map<GlobalStatus, byte[]> callerCursors = new EnumMap<>(GlobalStatus.class);
            callerCursors.put(GlobalStatus.Begin, new byte[] {1});
            condition.setStatusScanCursors(callerCursors);
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                publishMultiStatusContinuation(
                        condition, Collections.emptyMap(), SessionCondition.ScanContinuation.RESUMABLE);
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("multi-status scan continuation did not advance", exception.getMessage());
            assertStatusCursorsEqual(callerCursors, condition.getStatusScanCursors());
            Mockito.verify(storeManager).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testResumableMultiStatusPageCannotRemoveExistingStatusCursor() {
        try (RocksDBStoreEngine engine = open("scripted-removed-status-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            Map<GlobalStatus, byte[]> callerCursors = new EnumMap<>(GlobalStatus.class);
            callerCursors.put(GlobalStatus.Begin, new byte[] {2});
            callerCursors.put(GlobalStatus.Committing, new byte[] {3});
            condition.setStatusScanCursors(callerCursors);
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                Map<GlobalStatus, byte[]> nextCursors = new EnumMap<>(GlobalStatus.class);
                nextCursors.put(GlobalStatus.Begin, new byte[] {4});
                publishMultiStatusContinuation(condition, nextCursors, SessionCondition.ScanContinuation.RESUMABLE);
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("multi-status scan continuation did not advance", exception.getMessage());
            assertStatusCursorsEqual(callerCursors, condition.getStatusScanCursors());
            Mockito.verify(storeManager).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testResumableMultiStatusCursorCannotRegressAfterUnsignedProgress() {
        try (RocksDBStoreEngine engine = open("scripted-regressed-multi-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            Map<GlobalStatus, byte[]> callerCursors = new EnumMap<>(GlobalStatus.class);
            callerCursors.put(GlobalStatus.Begin, new byte[] {0x7f});
            condition.setStatusScanCursors(callerCursors);
            GlobalSession duplicate = globalSession("tx-scripted-regressed-cursor", GlobalStatus.Begin);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                Map<GlobalStatus, byte[]> nextCursors = new EnumMap<>(GlobalStatus.class);
                int page = pages.getAndIncrement();
                if (page == 0) {
                    nextCursors.put(GlobalStatus.Begin, new byte[] {(byte) 0x80});
                    publishMultiStatusContinuation(condition, nextCursors, SessionCondition.ScanContinuation.RESUMABLE);
                } else if (page == 1) {
                    nextCursors.put(GlobalStatus.Begin, new byte[] {0x7f});
                    publishMultiStatusContinuation(condition, nextCursors, SessionCondition.ScanContinuation.RESUMABLE);
                } else {
                    publishMultiStatusContinuation(
                            condition, Collections.emptyMap(), SessionCondition.ScanContinuation.EXHAUSTED);
                }
                return Collections.singletonList(duplicate);
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("multi-status scan continuation did not advance", exception.getMessage());
            assertStatusCursorsEqual(callerCursors, condition.getStatusScanCursors());
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testClonedEqualStatusCursorFailsImmediatelyAndRestoresCallerCursor() {
        try (RocksDBStoreEngine engine = open("scripted-stalled-status-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            byte[] callerCursor = new byte[] {1};
            condition.setStatusScanCursor(callerCursor);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                if (pages.getAndIncrement() == 0) {
                    condition.setNextStatusScanCursor(new byte[] {2});
                } else {
                    condition.setNextStatusScanCursor(
                            Arrays.copyOf(condition.getStatusScanCursor(), condition.getStatusScanCursor().length));
                }
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("status scan continuation did not advance", exception.getMessage());
            Assertions.assertArrayEquals(callerCursor, condition.getStatusScanCursor());
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testStatusCursorCannotRegressAfterUnsignedProgress() {
        try (RocksDBStoreEngine engine = open("scripted-regressed-status-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            byte[] callerCursor = new byte[] {0x7f};
            condition.setStatusScanCursor(callerCursor);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                int page = pages.getAndIncrement();
                if (page == 0) {
                    condition.setNextStatusScanCursor(new byte[] {(byte) 0x80});
                } else if (page == 1) {
                    condition.setNextStatusScanCursor(new byte[] {0x7f});
                } else {
                    throw new AssertionError("regressing status cursor was accepted");
                }
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("status scan continuation did not advance", exception.getMessage());
            Assertions.assertArrayEquals(callerCursor, condition.getStatusScanCursor());
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testClonedEqualMultiStatusCursorMapFailsImmediately() {
        try (RocksDBStoreEngine engine = open("scripted-stalled-multi-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            Map<GlobalStatus, byte[]> callerCursors = new EnumMap<>(GlobalStatus.class);
            callerCursors.put(GlobalStatus.Begin, new byte[] {3});
            callerCursors.put(GlobalStatus.Committing, new byte[] {4});
            condition.setStatusScanCursors(callerCursors);
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                Map<GlobalStatus, byte[]> clonedCursors = new EnumMap<>(GlobalStatus.class);
                clonedCursors.put(GlobalStatus.Begin, new byte[] {3});
                clonedCursors.put(GlobalStatus.Committing, new byte[] {4});
                publishMultiStatusContinuation(condition, clonedCursors, SessionCondition.ScanContinuation.RESUMABLE);
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("multi-status scan continuation did not advance", exception.getMessage());
            assertStatusCursorsEqual(callerCursors, condition.getStatusScanCursors());
            Mockito.verify(storeManager).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testClonedEqualTimeoutCursorFailsImmediately() {
        try (RocksDBStoreEngine engine = open("scripted-stalled-timeout-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setMaxTimeoutDeadlineMillis(1_000L);
            byte[] callerCursor = new byte[] {5};
            condition.setTimeoutScanCursor(callerCursor);
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                condition.setNextTimeoutScanCursor(
                        Arrays.copyOf(condition.getTimeoutScanCursor(), condition.getTimeoutScanCursor().length));
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("timeout scan continuation did not advance", exception.getMessage());
            Assertions.assertArrayEquals(callerCursor, condition.getTimeoutScanCursor());
            Mockito.verify(storeManager).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testTimeoutCursorCannotRegressAfterUnsignedProgress() {
        try (RocksDBStoreEngine engine = open("scripted-regressed-timeout-continuation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setMaxTimeoutDeadlineMillis(1_000L);
            byte[] callerCursor = new byte[] {0x7f};
            condition.setTimeoutScanCursor(callerCursor);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                int page = pages.getAndIncrement();
                if (page == 0) {
                    condition.setNextTimeoutScanCursor(new byte[] {(byte) 0x80});
                } else if (page == 1) {
                    condition.setNextTimeoutScanCursor(new byte[] {0x7f});
                } else {
                    throw new AssertionError("regressing timeout cursor was accepted");
                }
                return Collections.emptyList();
            });

            IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> sessionManager.findGlobalSessions(condition));

            Assertions.assertEquals("timeout scan continuation did not advance", exception.getMessage());
            Assertions.assertArrayEquals(callerCursor, condition.getTimeoutScanCursor());
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testStatusPagesDeduplicateAndAggregateStatsAndRestoreCallerCursor() {
        try (RocksDBStoreEngine engine = open("scripted-status-page-aggregation")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            byte[] callerCursor = new byte[] {6};
            condition.setStatusScanCursor(callerCursor);
            GlobalSession first = globalSession("tx-scripted-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("tx-scripted-second", GlobalStatus.Begin);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                if (pages.getAndIncrement() == 0) {
                    condition.setNextStatusScanCursor(new byte[] {7});
                    condition.setScanStats(new SessionScanStats(1, 2, 3, 1, 4, false));
                    return Collections.singletonList(first);
                }
                condition.setNextStatusScanCursor(null);
                condition.setScanStats(new SessionScanStats(10, 20, 30, 2, 40, true));
                return Arrays.asList(first, second);
            });

            List<GlobalSession> actual = sessionManager.findGlobalSessions(condition);

            Assertions.assertEquals(
                    Arrays.asList(first.getXid(), second.getXid()),
                    actual.stream().map(GlobalSession::getXid).collect(Collectors.toList()));
            Assertions.assertArrayEquals(callerCursor, condition.getStatusScanCursor());
            Assertions.assertEquals(SessionCondition.ScanContinuation.EXHAUSTED, condition.getStatusScanContinuation());
            assertScanStats(condition.getScanStats(), 11, 22, 33, 2, 44, true);
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testMultiStatusPagesRestoreCallerCursorsAfterExhaustion() {
        try (RocksDBStoreEngine engine = open("scripted-multi-cursor-restoration")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            Map<GlobalStatus, byte[]> callerCursors = new EnumMap<>(GlobalStatus.class);
            callerCursors.put(GlobalStatus.Begin, new byte[] {8});
            callerCursors.put(GlobalStatus.Committing, new byte[] {9});
            condition.setStatusScanCursors(callerCursors);
            GlobalSession first = globalSession("tx-scripted-multi-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("tx-scripted-multi-second", GlobalStatus.Committing);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                if (pages.getAndIncrement() == 0) {
                    Map<GlobalStatus, byte[]> nextCursors = new EnumMap<>(GlobalStatus.class);
                    nextCursors.put(GlobalStatus.Begin, new byte[] {10});
                    nextCursors.put(GlobalStatus.Committing, new byte[] {11});
                    publishMultiStatusContinuation(condition, nextCursors, SessionCondition.ScanContinuation.RESUMABLE);
                    return Collections.singletonList(first);
                }
                publishMultiStatusContinuation(
                        condition, Collections.emptyMap(), SessionCondition.ScanContinuation.EXHAUSTED);
                return Collections.singletonList(second);
            });

            List<GlobalSession> actual = sessionManager.findGlobalSessions(condition);

            Assertions.assertEquals(Arrays.asList(first, second), actual);
            assertStatusCursorsEqual(callerCursors, condition.getStatusScanCursors());
            Assertions.assertEquals(SessionCondition.ScanContinuation.EXHAUSTED, condition.getStatusScanContinuation());
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testTimeoutPagesRestoreCallerCursorAfterExhaustion() {
        try (RocksDBStoreEngine engine = open("scripted-timeout-cursor-restoration")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setMaxTimeoutDeadlineMillis(1_000L);
            byte[] callerCursor = new byte[] {12};
            condition.setTimeoutScanCursor(callerCursor);
            GlobalSession first = globalSession("tx-scripted-timeout-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("tx-scripted-timeout-second", GlobalStatus.Begin);
            AtomicInteger pages = new AtomicInteger();
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                if (pages.getAndIncrement() == 0) {
                    condition.setNextTimeoutScanCursor(new byte[] {13});
                    return Collections.singletonList(first);
                }
                condition.setNextTimeoutScanCursor(null);
                return Collections.singletonList(second);
            });

            List<GlobalSession> actual = sessionManager.findGlobalSessions(condition);

            Assertions.assertEquals(Arrays.asList(first, second), actual);
            Assertions.assertArrayEquals(callerCursor, condition.getTimeoutScanCursor());
            Assertions.assertEquals(
                    SessionCondition.ScanContinuation.EXHAUSTED, condition.getTimeoutScanContinuation());
            Mockito.verify(storeManager, Mockito.times(2)).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testBoundedMultiStatusQueryCallsStoreOnceAndExposesContinuation() {
        try (RocksDBStoreEngine engine = open("scripted-bounded-multi-query")) {
            TransactionStoreManager storeManager = Mockito.mock(TransactionStoreManager.class);
            RocksDBSessionManager sessionManager = scriptedManager(engine, storeManager);
            SessionCondition condition = new SessionCondition(GlobalStatus.Begin, GlobalStatus.Committing);
            condition.setScanLimit(1);
            GlobalSession first = globalSession("tx-scripted-bounded", GlobalStatus.Begin);
            Map<GlobalStatus, byte[]> nextCursors = new EnumMap<>(GlobalStatus.class);
            nextCursors.put(GlobalStatus.Begin, new byte[] {14});
            Mockito.when(storeManager.readSession(Mockito.same(condition))).thenAnswer(invocation -> {
                publishMultiStatusContinuation(condition, nextCursors, SessionCondition.ScanContinuation.RESUMABLE);
                return Collections.singletonList(first);
            });

            Assertions.assertEquals(Collections.singletonList(first), sessionManager.findGlobalSessions(condition));
            Assertions.assertEquals(SessionCondition.ScanContinuation.RESUMABLE, condition.getStatusScanContinuation());
            assertStatusCursorsEqual(nextCursors, condition.getNextStatusScanCursors());
            Mockito.verify(storeManager).readSession(Mockito.same(condition));
        }
    }

    @Test
    void testLockAndExecute() throws Exception {
        try (RocksDBStoreEngine engine = open("lock")) {
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
            GlobalSession globalSession = globalSession("tx-lock", GlobalStatus.Begin);
            AtomicBoolean called = new AtomicBoolean(false);

            Boolean actual = sessionManager.lockAndExecute(globalSession, () -> {
                called.set(true);
                return true;
            });

            Assertions.assertTrue(actual);
            Assertions.assertTrue(called.get());
        }
    }

    @Test
    void testTerminalRemovalFailureIsRecoverableWithoutDeletingNewOwnerLock() throws Exception {
        String databaseName = "terminal-removal-recovery";
        GlobalSession terminalGlobal = globalSession("tx-terminal", GlobalStatus.Committed);
        BranchSession terminalBranch = branchSession(terminalGlobal, 1L);
        GlobalSession newGlobal = globalSession("tx-new-owner", GlobalStatus.Begin);
        BranchSession newBranch = branchSession(newGlobal, 2L);
        byte[] lockKey = RocksDBKeyCodec.encodeRowLock(newBranch.getResourceId(), "t_order", "1");
        byte[] terminalIndexKey =
                RocksDBKeyCodec.encodeLockBranchIndex(terminalGlobal.getXid(), terminalBranch.getBranchId(), lockKey);
        byte[] newOwnerIndexKey =
                RocksDBKeyCodec.encodeLockBranchIndex(newGlobal.getXid(), newBranch.getBranchId(), lockKey);

        try (RocksDBStoreEngine engine = open(databaseName)) {
            RocksDBSessionManager setupSessionManager = new RocksDBSessionManager("root.data", engine);
            RocksDBLockManager setupLockManager = new RocksDBLockManager(engine);
            installLockManager(setupLockManager);
            setupSessionManager.addGlobalSession(terminalGlobal);
            setupSessionManager.addBranchSession(terminalGlobal, terminalBranch);
            Assertions.assertTrue(setupLockManager.acquireLock(terminalBranch));

            RocksDBStoreEngine spyEngine = Mockito.spy(engine);
            AtomicInteger writes = new AtomicInteger();
            Mockito.doAnswer(invocation -> {
                        if (writes.incrementAndGet() == 2) {
                            throw new StoreException("injected GLOBAL_REMOVE failure");
                        }
                        return invocation.callRealMethod();
                    })
                    .when(spyEngine)
                    .write(Mockito.any(WriteBatch.class));

            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", spyEngine);
            RocksDBLockManager lockManager = new RocksDBLockManager(spyEngine);
            installLockManager(lockManager);

            Assertions.assertThrows(StoreException.class, () -> sessionManager.removeGlobalSession(terminalGlobal));
            Assertions.assertNotNull(sessionManager.findGlobalSession(terminalGlobal.getXid(), true));
            Assertions.assertNull(spyEngine.get(RocksDBColumnFamily.LOCK, lockKey));
            Assertions.assertNull(spyEngine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, terminalIndexKey));

            sessionManager.addGlobalSession(newGlobal);
            sessionManager.addBranchSession(newGlobal, newBranch);
            Assertions.assertTrue(lockManager.acquireLock(newBranch));
            Assertions.assertNotNull(spyEngine.get(RocksDBColumnFamily.LOCK, lockKey));
            Assertions.assertNotNull(spyEngine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, newOwnerIndexKey));
        }

        try (RocksDBStoreEngine reopenedEngine = open(databaseName)) {
            RocksDBSessionManager reopenedSessionManager = new RocksDBSessionManager("root.data", reopenedEngine);
            RocksDBLockManager reopenedLockManager = new RocksDBLockManager(reopenedEngine);
            installLockManager(reopenedLockManager);

            GlobalSession recoveredTerminal = reopenedSessionManager.findGlobalSession(terminalGlobal.getXid(), true);
            Assertions.assertNotNull(recoveredTerminal);
            Assertions.assertEquals(1, recoveredTerminal.getBranchSessions().size());
            Assertions.assertEquals(
                    terminalBranch.getBranchId(),
                    recoveredTerminal.getBranchSessions().get(0).getBranchId());
            reopenedSessionManager.removeGlobalSession(recoveredTerminal);

            Assertions.assertNull(reopenedSessionManager.findGlobalSession(terminalGlobal.getXid(), true));
            Assertions.assertTrue(reopenedEngine
                    .prefixScan(
                            RocksDBColumnFamily.BRANCH_SESSION,
                            RocksDBKeyCodec.encodeXidPrefix(terminalGlobal.getXid()))
                    .isEmpty());
            Assertions.assertNotNull(reopenedEngine.get(RocksDBColumnFamily.LOCK, lockKey));
            Assertions.assertNotNull(reopenedEngine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, newOwnerIndexKey));
        }
    }

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true));
    }

    private RocksDBSessionManager scriptedManager(RocksDBStoreEngine engine, TransactionStoreManager storeManager) {
        RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine);
        sessionManager.setTransactionStoreManager(storeManager);
        return sessionManager;
    }

    private void publishMultiStatusContinuation(
            SessionCondition condition,
            Map<GlobalStatus, byte[]> cursors,
            SessionCondition.ScanContinuation continuation) {
        condition.setNextStatusScanCursors(cursors, continuation);
    }

    private void assertStatusCursorsEqual(Map<GlobalStatus, byte[]> expected, Map<GlobalStatus, byte[]> actual) {
        Assertions.assertEquals(expected.keySet(), actual.keySet());
        expected.forEach((status, cursor) -> Assertions.assertArrayEquals(cursor, actual.get(status)));
    }

    private void assertScanStats(
            SessionScanStats stats,
            long rowsScanned,
            long rowsReturned,
            long pointReads,
            long sessionsReturned,
            long elapsedMillis,
            boolean limitReached) {
        Assertions.assertEquals(rowsScanned, stats.getRowsScanned());
        Assertions.assertEquals(rowsReturned, stats.getRowsReturned());
        Assertions.assertEquals(pointReads, stats.getPointReads());
        Assertions.assertEquals(sessionsReturned, stats.getSessionsReturned());
        Assertions.assertEquals(elapsedMillis, stats.getElapsedMillis());
        Assertions.assertEquals(limitReached, stats.isLimitReached());
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

    private boolean contains(Collection<GlobalSession> sessions, GlobalSession expected) {
        return sessions.stream().anyMatch(session -> expected.getXid().equals(session.getXid()));
    }

    private void assertXids(Collection<GlobalSession> sessions, String... xids) {
        Assertions.assertEquals(xids.length, sessions.size());
        Set<String> actual = sessions.stream().map(GlobalSession::getXid).collect(Collectors.toSet());
        Assertions.assertEquals(new HashSet<>(Arrays.asList(xids)), actual);
    }

    private void assertEveryXidExactlyOnce(Collection<GlobalSession> sessions, Set<String> expectedXids) {
        Map<String, Integer> xidCounts = new HashMap<>();
        sessions.forEach(session -> xidCounts.merge(session.getXid(), 1, Integer::sum));
        Assertions.assertEquals(expectedXids, xidCounts.keySet());
        Assertions.assertTrue(xidCounts.values().stream().allMatch(count -> count == 1));
    }

    private void installLockManager(LockManager lockManager) throws Exception {
        lockerManagerField().set(null, lockManager);
    }

    private Field lockerManagerField() throws Exception {
        Field field = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        return field;
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
