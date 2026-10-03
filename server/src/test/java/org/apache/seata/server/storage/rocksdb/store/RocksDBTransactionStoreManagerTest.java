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
package org.apache.seata.server.storage.rocksdb.store;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.session.SessionScanStats;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.index.RocksDBIndexManager;
import org.apache.seata.server.storage.rocksdb.session.RocksDBSessionManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class RocksDBTransactionStoreManagerTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;
    private SessionManager originalRootSessionManager;
    private Map<String, SessionManager> originalSessionManagerMap;
    private MockEnvironment environment;

    @BeforeEach
    void beforeEach() throws Exception {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        environment = new MockEnvironment();
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        ConfigurationCache.clear();
        originalRootSessionManager = getRootSessionManager();
        originalSessionManagerMap = getSessionManagerMap();
    }

    @AfterEach
    void afterEach() throws Exception {
        ConfigurationCache.clear();
        restoreSessionHolder();
        restoreEnvironment();
    }

    @Test
    void testWriteAndReadGlobalSession() {
        try (RocksDBStoreEngine engine = open("global")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-global", GlobalStatus.Begin);

            Assertions.assertTrue(storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession));

            GlobalSession actual = storeManager.readSession(globalSession.getXid(), false);
            Assertions.assertNotNull(actual);
            Assertions.assertEquals(globalSession.getXid(), actual.getXid());
            Assertions.assertEquals(GlobalStatus.Begin, actual.getStatus());
            Assertions.assertTrue(actual.isLazyLoadBranch());
        }
    }

    @Test
    void testReadGlobalSessionWithBranches() {
        try (RocksDBStoreEngine engine = open("branches")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-branches", GlobalStatus.Begin);
            BranchSession branchSession = branchSession(globalSession, 1L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession);

            GlobalSession actual = storeManager.readSession(globalSession.getXid(), true);

            Assertions.assertNotNull(actual);
            Assertions.assertEquals(1, actual.getBranchSessions().size());
            Assertions.assertEquals(
                    branchSession.getBranchId(),
                    actual.getBranchSessions().get(0).getBranchId());
        }
    }

    @Test
    void testRemoveBranchAndGlobalSession() {
        try (RocksDBStoreEngine engine = open("remove")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-remove", GlobalStatus.Begin);
            BranchSession branch1 = branchSession(globalSession, 1L);
            BranchSession branch2 = branchSession(globalSession, 2L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branch1);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branch2);
            storeManager.writeSession(LogOperation.BRANCH_REMOVE, branch1);

            Assertions.assertEquals(
                    1,
                    storeManager
                            .readSession(globalSession.getXid(), true)
                            .getBranchSessions()
                            .size());

            storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession);

            Assertions.assertNull(storeManager.readSession(globalSession.getXid(), true));
            Assertions.assertTrue(engine.prefixScan(
                            RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(globalSession.getXid()))
                    .isEmpty());
        }
    }

    @Test
    void testRangeDeleteGlobalRemoveKeepsOtherGlobalBranches() {
        try (RocksDBStoreEngine engine = open("range-remove", true)) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession removed = globalSession("tx-range-remove", GlobalStatus.Begin);
            GlobalSession kept = globalSession("tx-range-keep", GlobalStatus.Begin);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, removed);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(removed, 1L));
            storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(removed, 2L));
            storeManager.writeSession(LogOperation.GLOBAL_ADD, kept);
            storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(kept, 1L));

            storeManager.writeSession(LogOperation.GLOBAL_REMOVE, removed);

            Assertions.assertNull(storeManager.readSession(removed.getXid(), true));
            Assertions.assertTrue(engine.prefixScan(
                            RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(removed.getXid()))
                    .isEmpty());
            Assertions.assertEquals(
                    1,
                    storeManager
                            .readSession(kept.getXid(), true)
                            .getBranchSessions()
                            .size());
        }
    }

    @Test
    void testGlobalRemoveIsIdempotentAndPersistsAcrossReopenForBothDeleteStrategies() {
        for (boolean enableRangeDelete : new boolean[] {false, true}) {
            String databaseName = "global-remove-reopen-" + enableRangeDelete;
            GlobalSession removed = globalSession("tx-remove-reopen-" + enableRangeDelete, GlobalStatus.Begin);
            GlobalSession kept = globalSession("tx-keep-reopen-" + enableRangeDelete, GlobalStatus.Begin);

            try (RocksDBStoreEngine engine = open(databaseName, enableRangeDelete)) {
                RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                storeManager.writeSession(LogOperation.GLOBAL_ADD, removed);
                storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(removed, 1L));
                storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(removed, 2L));
                storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(removed, 3L));
                storeManager.writeSession(LogOperation.GLOBAL_ADD, kept);
                storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession(kept, 1L));

                Assertions.assertTrue(storeManager.writeSession(LogOperation.GLOBAL_REMOVE, removed));
                Assertions.assertTrue(storeManager.writeSession(LogOperation.GLOBAL_REMOVE, removed));
            }

            try (RocksDBStoreEngine engine = open(databaseName, enableRangeDelete)) {
                RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
                Assertions.assertNull(storeManager.readSession(removed.getXid(), true));
                Assertions.assertTrue(engine.prefixScan(
                                RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(removed.getXid()))
                        .isEmpty());
                Assertions.assertTrue(storeManager.readSession(new GlobalStatus[] {GlobalStatus.Begin}, false).stream()
                        .noneMatch(session -> removed.getXid().equals(session.getXid())));
                SessionCondition transactionIdCondition = new SessionCondition();
                transactionIdCondition.setTransactionId(removed.getTransactionId());
                Assertions.assertTrue(
                        storeManager.readSession(transactionIdCondition).isEmpty());
                Assertions.assertNull(engine.get(
                        RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                        RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                                removed.getBeginTime() + removed.getTimeout(), removed.getXid())));

                GlobalSession actualKept = storeManager.readSession(kept.getXid(), true);
                Assertions.assertNotNull(actualKept);
                Assertions.assertEquals(1, actualKept.getBranchSessions().size());
            }
        }
    }

    @Test
    void testReadByStatus() {
        try (RocksDBStoreEngine engine = open("status")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession begin = globalSession("tx-begin", GlobalStatus.Begin);
            GlobalSession committed = globalSession("tx-committed", GlobalStatus.Committed);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);

            List<GlobalSession> actual = storeManager.readSession(new GlobalStatus[] {GlobalStatus.Begin}, false);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(begin.getXid(), actual.get(0).getXid());
        }
    }

    @Test
    void testGlobalUpdateMovesStatusIndex() {
        try (RocksDBStoreEngine engine = open("status-update")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-status-update", GlobalStatus.Begin);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            globalSession.setStatus(GlobalStatus.Committing);
            storeManager.writeSession(LogOperation.GLOBAL_UPDATE, globalSession);

            Assertions.assertTrue(storeManager
                    .readSession(new GlobalStatus[] {GlobalStatus.Begin}, false)
                    .isEmpty());
            List<GlobalSession> committingSessions =
                    storeManager.readSession(new GlobalStatus[] {GlobalStatus.Committing}, false);
            Assertions.assertEquals(1, committingSessions.size());
            Assertions.assertEquals(
                    globalSession.getXid(), committingSessions.get(0).getXid());
        }
    }

    @Test
    void testGlobalRemoveDeletesIndexes() {
        try (RocksDBStoreEngine engine = open("remove-indexes")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-remove-indexes", GlobalStatus.Begin);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession);

            SessionCondition condition = new SessionCondition();
            condition.setTransactionId(globalSession.getTransactionId());
            Assertions.assertTrue(storeManager.readSession(condition).isEmpty());
            Assertions.assertTrue(storeManager
                    .readSession(new GlobalStatus[] {GlobalStatus.Begin}, false)
                    .isEmpty());
        }
    }

    @Test
    void testStaleIndexesDoNotReturnMismatchedGlobalSession() {
        try (RocksDBStoreEngine engine = open("stale-indexes")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-stale-indexes", GlobalStatus.Begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);

            byte[] xidValue = globalSession.getXid().getBytes(StandardCharsets.UTF_8);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(
                            GlobalStatus.Committing, globalSession.getBeginTime(), globalSession.getXid()),
                    xidValue);
            long staleTransactionId = globalSession.getTransactionId() + 1;
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(staleTransactionId),
                    xidValue);

            Assertions.assertTrue(storeManager
                    .readSession(new GlobalStatus[] {GlobalStatus.Committing}, false)
                    .isEmpty());
            SessionCondition condition = new SessionCondition();
            condition.setTransactionId(staleTransactionId);
            Assertions.assertTrue(storeManager.readSession(condition).isEmpty());
        }
    }

    @Test
    void testReadSortByTimeoutBeginSessionsUsesBeginTimeOrder() {
        try (RocksDBStoreEngine engine = open("begin-order")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession late = globalSession("tx-late", GlobalStatus.Begin);
            late.setBeginTime(System.currentTimeMillis());
            GlobalSession early = globalSession("tx-early", GlobalStatus.Begin);
            early.setBeginTime(late.getBeginTime() - 1000);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, late);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, early);

            List<GlobalSession> actual = storeManager.readSortByTimeoutBeginSessions(false);

            Assertions.assertEquals(2, actual.size());
            Assertions.assertEquals(early.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(late.getXid(), actual.get(1).getXid());
        }
    }

    @Test
    void testReadByTimeoutDeadlineUsesDeadlineOrderAndCursor() {
        try (RocksDBStoreEngine engine = open("timeout-deadline-cursor")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            long deadline = 1_000_000L;
            GlobalSession activeOld = globalSession("tx-timeout-active-old", GlobalStatus.Begin, 2_000_000);
            activeOld.setBeginTime(100L);
            GlobalSession firstExpired = globalSession("tx-timeout-first-expired", GlobalStatus.Begin, 100);
            firstExpired.setBeginTime(deadline - 1_000L);
            GlobalSession secondExpired = globalSession("tx-timeout-second-expired", GlobalStatus.Begin, 100);
            secondExpired.setBeginTime(deadline - 500L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, activeOld);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, secondExpired);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, firstExpired);

            SessionCondition firstPage = new SessionCondition(GlobalStatus.Begin);
            firstPage.setLazyLoadBranch(true);
            firstPage.setMaxTimeoutDeadlineMillis(deadline);
            firstPage.setLimit(1);
            List<GlobalSession> firstActual = storeManager.readSession(firstPage);

            Assertions.assertEquals(1, firstActual.size());
            Assertions.assertEquals(firstExpired.getXid(), firstActual.get(0).getXid());
            Assertions.assertNotNull(firstPage.getNextTimeoutScanCursor());
            Assertions.assertEquals(1, firstPage.getScanStats().getPointReads());

            SessionCondition secondPage = new SessionCondition(GlobalStatus.Begin);
            secondPage.setLazyLoadBranch(true);
            secondPage.setMaxTimeoutDeadlineMillis(deadline);
            secondPage.setLimit(1);
            secondPage.setTimeoutScanCursor(firstPage.getNextTimeoutScanCursor());
            List<GlobalSession> secondActual = storeManager.readSession(secondPage);

            Assertions.assertEquals(1, secondActual.size());
            Assertions.assertEquals(secondExpired.getXid(), secondActual.get(0).getXid());

            SessionCondition thirdPage = new SessionCondition(GlobalStatus.Begin);
            thirdPage.setLazyLoadBranch(true);
            thirdPage.setMaxTimeoutDeadlineMillis(deadline);
            thirdPage.setLimit(1);
            thirdPage.setTimeoutScanCursor(secondPage.getNextTimeoutScanCursor());
            List<GlobalSession> thirdActual = storeManager.readSession(thirdPage);

            Assertions.assertTrue(thirdActual.isEmpty());
            Assertions.assertNull(thirdPage.getNextTimeoutScanCursor());
        }
    }

    @Test
    void testReadByTimeoutDeadlineScanLimitResumesAfterThreeStaleRounds() {
        int scanLimit = 2;
        int staleRounds = 3;
        try (RocksDBStoreEngine engine = open("timeout-deadline-stale-scan-limit")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            for (int i = 0; i < scanLimit * staleRounds; i++) {
                String staleXid = "tx-timeout-stale-scan-limit-" + i;
                engine.put(
                        RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                        RocksDBKeyCodec.encodeGlobalTimeoutIndex(100L + i, staleXid),
                        staleXid.getBytes(StandardCharsets.UTF_8));
            }
            GlobalSession valid = globalSession("tx-timeout-valid-after-stale-rounds", GlobalStatus.Begin, 100);
            valid.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, valid);

            byte[] cursor = null;
            for (int round = 0; round < staleRounds; round++) {
                SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
                condition.setLazyLoadBranch(true);
                condition.setMaxTimeoutDeadlineMillis(1_000L);
                condition.setLimit(1);
                condition.setScanLimit(scanLimit);
                condition.setTimeoutScanCursor(cursor);

                Assertions.assertTrue(storeManager.readSession(condition).isEmpty());
                Assertions.assertEquals(scanLimit, condition.getScanStats().getRowsScanned());
                Assertions.assertEquals(scanLimit, condition.getScanStats().getPointReads());
                Assertions.assertNotNull(condition.getNextTimeoutScanCursor());
                cursor = condition.getNextTimeoutScanCursor();
            }

            SessionCondition progress = new SessionCondition(GlobalStatus.Begin);
            progress.setLazyLoadBranch(true);
            progress.setMaxTimeoutDeadlineMillis(1_000L);
            progress.setLimit(1);
            progress.setScanLimit(scanLimit);
            progress.setTimeoutScanCursor(cursor);
            List<GlobalSession> actual = storeManager.readSession(progress);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(valid.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(1, progress.getScanStats().getRowsScanned());
            Assertions.assertEquals(1, progress.getScanStats().getPointReads());
        }
    }

    @Test
    void testReadSessionResetsContinuationOutputsWithoutClearingInputCursors() {
        try (RocksDBStoreEngine engine = open("reused-condition-continuations")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("tx-reused-condition-first", GlobalStatus.Begin, 100);
            first.setBeginTime(100L);
            GlobalSession second = globalSession("tx-reused-condition-second", GlobalStatus.Begin, 100);
            second.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            condition.setLimit(1);
            condition.setScanLimit(1);
            condition.setNextTimeoutScanCursor(new byte[] {9});

            Assertions.assertEquals(1, storeManager.readSession(condition).size());
            Assertions.assertEquals(SessionCondition.ScanContinuation.RESUMABLE, condition.getStatusScanContinuation());
            Assertions.assertEquals(SessionCondition.ScanContinuation.UNSET, condition.getTimeoutScanContinuation());
            byte[] statusInputCursor = condition.getNextStatusScanCursor();
            Assertions.assertNotNull(statusInputCursor);
            condition.setStatusScanCursor(statusInputCursor);

            condition.setMaxTimeoutDeadlineMillis(1_000L);
            Assertions.assertEquals(1, storeManager.readSession(condition).size());
            Assertions.assertEquals(SessionCondition.ScanContinuation.UNSET, condition.getStatusScanContinuation());
            Assertions.assertEquals(
                    SessionCondition.ScanContinuation.RESUMABLE, condition.getTimeoutScanContinuation());
            Assertions.assertArrayEquals(statusInputCursor, condition.getStatusScanCursor());
            byte[] timeoutInputCursor = condition.getNextTimeoutScanCursor();
            Assertions.assertNotNull(timeoutInputCursor);
            condition.setTimeoutScanCursor(timeoutInputCursor);

            condition.setXid(first.getXid());
            Assertions.assertEquals(1, storeManager.readSession(condition).size());
            assertContinuationOutputsUnset(condition);
            Assertions.assertArrayEquals(statusInputCursor, condition.getStatusScanCursor());
            Assertions.assertArrayEquals(timeoutInputCursor, condition.getTimeoutScanCursor());

            condition.setXid(null);
            condition.setTransactionId(first.getTransactionId());
            condition.setNextStatusScanCursor(new byte[] {7});
            condition.setNextTimeoutScanCursor(new byte[] {8});
            Assertions.assertEquals(1, storeManager.readSession(condition).size());
            assertContinuationOutputsUnset(condition);
            Assertions.assertArrayEquals(statusInputCursor, condition.getStatusScanCursor());
            Assertions.assertArrayEquals(timeoutInputCursor, condition.getTimeoutScanCursor());
        }
    }

    @Test
    void testGlobalUpdateRemovesTimeoutIndexForNonBeginStatus() {
        try (RocksDBStoreEngine engine = open("timeout-index-status-update")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-timeout-index-update", GlobalStatus.Begin, 100);
            globalSession.setBeginTime(100L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            globalSession.setStatus(GlobalStatus.Committing);
            storeManager.writeSession(LogOperation.GLOBAL_UPDATE, globalSession);

            Assertions.assertTrue(engine.prefixScan(RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX, new byte[0])
                    .isEmpty());
        }
    }

    @Test
    void testLazyReadLoadsRetryBranchesOnDemand() throws Exception {
        try (RocksDBStoreEngine engine = open("lazy-retry-branches")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            setRootSessionManager(new RocksDBSessionManager("root.data", engine));
            GlobalSession committing = globalSession("tx-commit-retry", GlobalStatus.CommitRetrying);
            GlobalSession rollbacking = globalSession("tx-rollbacking", GlobalStatus.Rollbacking);
            BranchSession committingBranch = branchSession(committing, 1L);
            BranchSession rollbackingBranch = branchSession(rollbacking, 2L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, committing);
            storeManager.writeSession(LogOperation.BRANCH_ADD, committingBranch);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbacking);
            storeManager.writeSession(LogOperation.BRANCH_ADD, rollbackingBranch);

            SessionCondition condition = new SessionCondition(GlobalStatus.CommitRetrying, GlobalStatus.Rollbacking);
            condition.setLazyLoadBranch(true);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            for (GlobalSession globalSession : actual) {
                Assertions.assertTrue(globalSession.isLazyLoadBranch());
                Assertions.assertEquals(1, globalSession.getBranchSessions().size());
            }
        }
    }

    @Test
    void testReadBySessionConditionXid() {
        try (RocksDBStoreEngine engine = open("condition")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession globalSession = globalSession("tx-condition", GlobalStatus.Begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);

            List<GlobalSession> actual = storeManager.readSession(new SessionCondition(globalSession.getXid()));

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(globalSession.getXid(), actual.get(0).getXid());
        }
    }

    @Test
    void testReadBySessionConditionTransactionIdAndOvertime() {
        try (RocksDBStoreEngine engine = open("condition-filter")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession oldSession = globalSession("tx-old", GlobalStatus.Begin);
            oldSession.setBeginTime(System.currentTimeMillis() - 60000);
            GlobalSession newSession = globalSession("tx-new", GlobalStatus.Begin);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldSession);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, newSession);

            SessionCondition condition = new SessionCondition();
            condition.setTransactionId(oldSession.getTransactionId());
            condition.setOverTimeAliveMills(1000L);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(oldSession.getXid(), actual.get(0).getXid());
        }
    }

    @Test
    void testReadByStatusAndOvertimeUsesBeginTimeBoundary() {
        try (RocksDBStoreEngine engine = open("condition-status-overtime")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            long now = System.currentTimeMillis();
            GlobalSession oldSession = globalSession("tx-old-status", GlobalStatus.Begin);
            oldSession.setBeginTime(now - 60000);
            GlobalSession newSession = globalSession("tx-new-status", GlobalStatus.Begin);
            newSession.setBeginTime(now);
            GlobalSession oldOtherStatus = globalSession("tx-old-other-status", GlobalStatus.Committing);
            oldOtherStatus.setBeginTime(now - 60000);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldSession);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, newSession);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldOtherStatus);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            condition.setOverTimeAliveMills(1000L);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(oldSession.getXid(), actual.get(0).getXid());
            Assertions.assertTrue(actual.get(0).isLazyLoadBranch());
        }
    }

    @Test
    void testReadByStatusAndOvertimeHonorsLimitInBeginTimeOrder() {
        try (RocksDBStoreEngine engine = open("condition-status-overtime-limit")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            long now = System.currentTimeMillis();
            GlobalSession oldest = globalSession("tx-oldest-status", GlobalStatus.Begin);
            oldest.setBeginTime(now - 60000);
            GlobalSession middle = globalSession("tx-middle-status", GlobalStatus.Begin);
            middle.setBeginTime(now - 50000);
            GlobalSession newestExpired = globalSession("tx-newest-expired-status", GlobalStatus.Begin);
            newestExpired.setBeginTime(now - 40000);
            GlobalSession active = globalSession("tx-active-status", GlobalStatus.Begin);
            active.setBeginTime(now);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, newestExpired);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, active);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, middle);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            condition.setOverTimeAliveMills(1000L);
            condition.setLimit(2);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            Assertions.assertEquals(oldest.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(middle.getXid(), actual.get(1).getXid());
        }
    }

    @Test
    void testReadByStatusAndLimitUsesPagedIndexScan() throws Exception {
        try (RocksDBStoreEngine engine = open("condition-status-limit-paged")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            CountingIndexManager indexManager = new CountingIndexManager(engine);
            replaceIndexManager(storeManager, indexManager);
            GlobalSession oldest = globalSession("tx-oldest-status-only", GlobalStatus.Begin);
            oldest.setBeginTime(100L);
            GlobalSession middle = globalSession("tx-middle-status-only", GlobalStatus.Begin);
            middle.setBeginTime(200L);
            GlobalSession newest = globalSession("tx-newest-status-only", GlobalStatus.Begin);
            newest.setBeginTime(300L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, newest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, middle);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            condition.setLimit(2);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            Assertions.assertEquals(oldest.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(middle.getXid(), actual.get(1).getXid());
            Assertions.assertEquals(0, indexManager.fullStatusScanCalls);
            Assertions.assertTrue(indexManager.pagedStatusScanCalls > 0);
        }
    }

    @Test
    void testReadByStatusReturnsCompleteGlobalSessionFields() {
        try (RocksDBStoreEngine engine = open("condition-status-complete-fields")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession expected =
                    new GlobalSession("status-application", "status-service-group", "status-transaction", 60_000);
            expected.setApplicationData("status-application-data");
            expected.setBeginTime(123_456L);
            expected.setStatus(GlobalStatus.Committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, expected);

            GlobalSession byXid = storeManager.readSession(expected.getXid(), false);
            SessionCondition condition = new SessionCondition(GlobalStatus.Committed);
            condition.setLazyLoadBranch(true);
            condition.setLimit(10);
            List<GlobalSession> byStatus = storeManager.readSession(condition);

            Assertions.assertEquals(1, byStatus.size());
            GlobalSession actual = byStatus.get(0);
            Assertions.assertEquals("status-application", actual.getApplicationId());
            Assertions.assertEquals("status-service-group", actual.getTransactionServiceGroup());
            Assertions.assertEquals("status-transaction", actual.getTransactionName());
            Assertions.assertEquals("status-application-data", actual.getApplicationData());
            Assertions.assertEquals(byXid.getApplicationId(), actual.getApplicationId());
            Assertions.assertEquals(byXid.getTransactionServiceGroup(), actual.getTransactionServiceGroup());
            Assertions.assertEquals(byXid.getTransactionName(), actual.getTransactionName());
            Assertions.assertEquals(byXid.getApplicationData(), actual.getApplicationData());
            Assertions.assertEquals(byXid.getXid(), actual.getXid());
            Assertions.assertEquals(byXid.getTransactionId(), actual.getTransactionId());
            Assertions.assertEquals(byXid.getBeginTime(), actual.getBeginTime());
            Assertions.assertEquals(byXid.getStatus(), actual.getStatus());
        }
    }

    @Test
    void testReadByStatusReturnsDuplicateIndexXidOnlyOnce() {
        try (RocksDBStoreEngine engine = open("condition-status-duplicate-xid")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession session = globalSession("tx-status-duplicate-xid", GlobalStatus.Committed);
            session.setBeginTime(321L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, session);
            byte[] canonicalKey = RocksDBKeyCodec.encodeGlobalStatusIndex(
                    session.getStatus(), session.getBeginTime(), session.getXid());
            byte[] duplicateKey = Arrays.copyOf(canonicalKey, canonicalKey.length + 1);
            duplicateKey[duplicateKey.length - 1] = 1;
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    duplicateKey,
                    session.getXid().getBytes(StandardCharsets.UTF_8));

            SessionCondition condition = new SessionCondition(GlobalStatus.Committed);
            condition.setLazyLoadBranch(true);
            condition.setLimit(10);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(session.getXid(), actual.get(0).getXid());
        }
    }

    @Test
    void testReadByStatusWithoutLimitUsesPagedScan() throws Exception {
        try (RocksDBStoreEngine engine = open("condition-status-unlimited-fast")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            CountingIndexManager indexManager = new CountingIndexManager(engine);
            replaceIndexManager(storeManager, indexManager);
            GlobalSession first = globalSession("tx-status-unlimited-first", GlobalStatus.Begin);
            GlobalSession second = globalSession("tx-status-unlimited-second", GlobalStatus.Begin);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            // After Direction A optimization: always use paged scan (no unbounded full scan)
            Assertions.assertEquals(0, indexManager.fullStatusScanCalls);
            Assertions.assertTrue(indexManager.pagedStatusScanCalls >= 1);
        }
    }

    @Test
    void testReadByStatusAndLimitRecordsScanStats() {
        try (RocksDBStoreEngine engine = open("condition-status-limit-stats")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession oldest = globalSession("tx-stats-oldest", GlobalStatus.Begin);
            oldest.setBeginTime(100L);
            GlobalSession middle = globalSession("tx-stats-middle", GlobalStatus.Begin);
            middle.setBeginTime(200L);
            GlobalSession newest = globalSession("tx-stats-newest", GlobalStatus.Begin);
            newest.setBeginTime(300L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, newest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, middle);

            SessionCondition condition = new SessionCondition(GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);
            condition.setLimit(2);
            List<GlobalSession> actual = storeManager.readSession(condition);
            SessionScanStats stats = condition.getScanStats();

            Assertions.assertEquals(2, actual.size());
            Assertions.assertEquals(2, stats.getRowsScanned());
            Assertions.assertEquals(2, stats.getRowsReturned());
            Assertions.assertEquals(2, stats.getPointReads());
            Assertions.assertEquals(2, stats.getSessionsReturned());
            Assertions.assertTrue(stats.isLimitReached());
            Assertions.assertTrue(stats.getElapsedMillis() >= 0);
        }
    }

    @Test
    void testUnfilteredSessionScanRecordsScanStats() {
        try (RocksDBStoreEngine engine = open("condition-unfiltered-scan-stats")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession("tx-unfiltered-1", GlobalStatus.Begin));
            storeManager.writeSession(
                    LogOperation.GLOBAL_ADD, globalSession("tx-unfiltered-2", GlobalStatus.Committing));
            storeManager.writeSession(
                    LogOperation.GLOBAL_ADD, globalSession("tx-unfiltered-3", GlobalStatus.Committed));

            SessionCondition condition = new SessionCondition();
            condition.setLazyLoadBranch(true);
            List<GlobalSession> actual = storeManager.readSession(condition);
            SessionScanStats stats = condition.getScanStats();

            Assertions.assertEquals(3, actual.size());
            Assertions.assertEquals(3, stats.getRowsScanned());
            Assertions.assertEquals(3, stats.getRowsReturned());
            Assertions.assertEquals(0, stats.getPointReads());
            Assertions.assertEquals(3, stats.getSessionsReturned());
            Assertions.assertFalse(stats.isLimitReached());
        }
    }

    @Test
    void testReadByStatusAndLimitHonorsScanCursor() {
        try (RocksDBStoreEngine engine = open("condition-status-limit-cursor")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession oldest = globalSession("tx-cursor-oldest", GlobalStatus.Begin);
            oldest.setBeginTime(100L);
            GlobalSession middle = globalSession("tx-cursor-middle", GlobalStatus.Begin);
            middle.setBeginTime(200L);
            GlobalSession newest = globalSession("tx-cursor-newest", GlobalStatus.Begin);
            newest.setBeginTime(300L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, newest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, oldest);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, middle);

            SessionCondition firstPage = new SessionCondition(GlobalStatus.Begin);
            firstPage.setLazyLoadBranch(true);
            firstPage.setLimit(2);
            List<GlobalSession> firstActual = storeManager.readSession(firstPage);

            Assertions.assertEquals(2, firstActual.size());
            Assertions.assertEquals(oldest.getXid(), firstActual.get(0).getXid());
            Assertions.assertEquals(middle.getXid(), firstActual.get(1).getXid());
            Assertions.assertNotNull(firstPage.getNextStatusScanCursor());

            SessionCondition secondPage = new SessionCondition(GlobalStatus.Begin);
            secondPage.setLazyLoadBranch(true);
            secondPage.setLimit(2);
            secondPage.setStatusScanCursor(firstPage.getNextStatusScanCursor());
            List<GlobalSession> secondActual = storeManager.readSession(secondPage);

            Assertions.assertEquals(1, secondActual.size());
            Assertions.assertEquals(newest.getXid(), secondActual.get(0).getXid());
            Assertions.assertNull(secondPage.getNextStatusScanCursor());
        }
    }

    @Test
    void testReadByMultipleStatusesWithoutLimitUsesPagedScan() throws Exception {
        try (RocksDBStoreEngine engine = open("condition-multi-status-unlimited-fast")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            CountingIndexManager indexManager = new CountingIndexManager(engine);
            replaceIndexManager(storeManager, indexManager);
            GlobalSession committed = globalSession("tx-multi-unlimited-committed", GlobalStatus.Committed);
            GlobalSession rollbacked = globalSession("tx-multi-unlimited-rollbacked", GlobalStatus.Rollbacked);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbacked);

            SessionCondition condition = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            condition.setLazyLoadBranch(true);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            // After Direction A optimization: always use paged scan (no unbounded full scan)
            Assertions.assertEquals(0, indexManager.fullStatusScanCalls);
            Assertions.assertTrue(indexManager.pagedStatusScanCalls >= 1);
        }
    }

    @Test
    void testReadByMultipleStatusesWithoutLimitHonorsFullScanDeadline() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("condition-multi-status-deadline")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            Assertions.assertEquals(1L, fullScanDeadlineMillis(storeManager));
            int sessionCount = 4096;
            for (int i = 0; i < sessionCount; i++) {
                GlobalStatus status = i % 2 == 0 ? GlobalStatus.Committed : GlobalStatus.Rollbacked;
                GlobalSession session = globalSession("tx-multi-deadline-" + i, status);
                session.setBeginTime(i);
                storeManager.writeSession(LogOperation.GLOBAL_ADD, session);
            }

            SessionCondition condition = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            condition.setLazyLoadBranch(true);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertTrue(
                    actual.size() < sessionCount,
                    "an unlimited multi-status scan must stop when fullScanDeadlineMillis is reached");
        }
    }

    @Test
    void testUnboundedSingleStatusDeadlineStillPerformsOneScanPass() {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("condition-single-status-expired-deadline-progress")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession session = globalSession("tx-single-expired-deadline", GlobalStatus.Begin);
            session.setBeginTime(100L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, session);
            SessionCondition condition = new DeadlineExpiringCondition(true, false, GlobalStatus.Begin);
            condition.setLazyLoadBranch(true);

            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(
                    Collections.singletonList(session.getXid()),
                    actual.stream().map(GlobalSession::getXid).collect(Collectors.toList()));
            Assertions.assertEquals(SessionCondition.ScanContinuation.EXHAUSTED, condition.getStatusScanContinuation());
            Assertions.assertTrue(condition.getScanStats().getRowsScanned() > 0);
        }
    }

    @Test
    void testUnboundedMultiStatusDeadlineStillAdvancesContinuation() {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("condition-multi-status-expired-deadline-progress")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession committed = globalSession("tx-multi-expired-committed", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            GlobalSession rollbacked = globalSession("tx-multi-expired-rollbacked", GlobalStatus.Rollbacked);
            rollbacked.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbacked);
            SessionCondition condition =
                    new DeadlineExpiringCondition(false, true, GlobalStatus.Committed, GlobalStatus.Rollbacked);
            condition.setLazyLoadBranch(true);

            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(committed.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(SessionCondition.ScanContinuation.RESUMABLE, condition.getStatusScanContinuation());
            Assertions.assertFalse(condition.getNextStatusScanCursors().isEmpty());
        }
    }

    @Test
    void testMultiStatusPublishesExplicitExhaustedAndResumableStates() {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("condition-multi-status-continuation-state")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            SessionCondition exhausted = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);

            Assertions.assertTrue(storeManager.readSession(exhausted).isEmpty());
            Assertions.assertEquals(SessionCondition.ScanContinuation.EXHAUSTED, exhausted.getStatusScanContinuation());

            GlobalSession committed = globalSession("tx-continuation-state-committed", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            GlobalSession rollbacked = globalSession("tx-continuation-state-rollbacked", GlobalStatus.Rollbacked);
            rollbacked.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbacked);
            SessionCondition resumable = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            resumable.setLimit(1);

            Assertions.assertEquals(1, storeManager.readSession(resumable).size());
            Assertions.assertEquals(SessionCondition.ScanContinuation.RESUMABLE, resumable.getStatusScanContinuation());
            Assertions.assertFalse(resumable.getNextStatusScanCursors().isEmpty());
        }
    }

    @Test
    void testDataBearingFinalMultiStatusPagePublishesEmptyExhaustedCursors() {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "60000");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("condition-multi-status-data-bearing-exhaustion")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession committed = globalSession("tx-data-bearing-exhausted", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            SessionCondition condition = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            condition.setLazyLoadBranch(true);

            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(
                    Collections.singletonList(committed.getXid()),
                    actual.stream().map(GlobalSession::getXid).collect(Collectors.toList()));
            Assertions.assertEquals(SessionCondition.ScanContinuation.EXHAUSTED, condition.getStatusScanContinuation());
            Assertions.assertTrue(condition.getNextStatusScanCursors().isEmpty());
        }
    }

    @Test
    void testRecoveryPagesContinueAfterDeadlineUntilExhaustedWithoutDuplicates() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "3");
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("recovery-deadline-pages")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            replaceIndexManager(storeManager, new SlowIndexManager(engine));
            GlobalSession committedFirst = globalSession("tx-recovery-committed-first", GlobalStatus.Committed);
            GlobalSession rollbackedFirst = globalSession("tx-recovery-rollbacked-first", GlobalStatus.Rollbacked);
            GlobalSession committedSecond = globalSession("tx-recovery-committed-second", GlobalStatus.Committed);
            GlobalSession rollbackedSecond = globalSession("tx-recovery-rollbacked-second", GlobalStatus.Rollbacked);
            committedFirst.setBeginTime(100L);
            rollbackedFirst.setBeginTime(200L);
            committedSecond.setBeginTime(300L);
            rollbackedSecond.setBeginTime(400L);
            for (GlobalSession session :
                    Arrays.asList(committedFirst, rollbackedFirst, committedSecond, rollbackedSecond)) {
                storeManager.writeSession(LogOperation.GLOBAL_ADD, session);
            }

            RocksDBTransactionStoreManager.RecoveryCursor cursor = null;
            List<String> recoveredXids = new ArrayList<>();
            int deadlinePages = 0;
            boolean exhausted = false;
            for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
                RocksDBTransactionStoreManager.RecoveryScanPage page = storeManager.readRecoveryPage(
                        new GlobalStatus[] {GlobalStatus.Committed, GlobalStatus.Rollbacked}, cursor);
                recoveredXids.addAll(
                        page.getSessions().stream().map(GlobalSession::getXid).collect(Collectors.toList()));
                if (page.isDeadlineReached()) {
                    deadlinePages++;
                }
                if (page.isExhausted()) {
                    exhausted = true;
                    Assertions.assertNull(page.getContinuation());
                    break;
                }
                Assertions.assertNotNull(page.getContinuation());
                cursor = page.getContinuation();
            }

            Assertions.assertTrue(exhausted);
            Assertions.assertTrue(deadlinePages > 1);
            Assertions.assertEquals(
                    Arrays.asList(
                            committedFirst.getXid(),
                            rollbackedFirst.getXid(),
                            committedSecond.getXid(),
                            rollbackedSecond.getXid()),
                    recoveredXids);
        }
    }

    @Test
    void testRecoveryCursorDefensivelyCopiesCallerStatuses() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("recovery-cursor-status-copy")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("tx-recovery-copy-first", GlobalStatus.Committed);
            GlobalSession second = globalSession("tx-recovery-copy-second", GlobalStatus.Committed);
            first.setBeginTime(100L);
            second.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);
            GlobalStatus[] statuses = {GlobalStatus.Committed};

            RocksDBTransactionStoreManager.RecoveryScanPage firstPage = storeManager.readRecoveryPage(statuses, null);
            statuses[0] = GlobalStatus.Rollbacked;
            RocksDBTransactionStoreManager.RecoveryScanPage secondPage = storeManager.readRecoveryPage(
                    new GlobalStatus[] {GlobalStatus.Committed}, firstPage.getContinuation());

            Assertions.assertEquals(
                    first.getXid(), firstPage.getSessions().get(0).getXid());
            Assertions.assertEquals(
                    second.getXid(), secondPage.getSessions().get(0).getXid());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRecoveryCursorDeepCopiesPrivateState() throws Exception {
        Constructor<RocksDBTransactionStoreManager.RecoveryCursor> constructor =
                RocksDBTransactionStoreManager.RecoveryCursor.class.getDeclaredConstructor(
                        GlobalStatus[].class, Map.class);
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        GlobalStatus[] statuses = {GlobalStatus.Committed};
        byte[] cursorValue = RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Committed, 100L, "tx-cursor-copy");
        byte[] expectedCursorValue = Arrays.copyOf(cursorValue, cursorValue.length);
        Map<GlobalStatus, byte[]> cursorValues = new EnumMap<>(GlobalStatus.class);
        cursorValues.put(GlobalStatus.Committed, cursorValue);

        RocksDBTransactionStoreManager.RecoveryCursor cursor = constructor.newInstance(statuses, cursorValues);
        statuses[0] = GlobalStatus.Rollbacked;
        cursorValue[cursorValue.length - 1] ^= 1;
        cursorValues.clear();

        Field statusesField = RocksDBTransactionStoreManager.RecoveryCursor.class.getDeclaredField("statuses");
        statusesField.setAccessible(true);
        Assertions.assertArrayEquals(
                new GlobalStatus[] {GlobalStatus.Committed}, (GlobalStatus[]) statusesField.get(cursor));
        Field cursorValuesField =
                RocksDBTransactionStoreManager.RecoveryCursor.class.getDeclaredField("statusScanCursors");
        cursorValuesField.setAccessible(true);
        Map<GlobalStatus, byte[]> copiedCursorValues = (Map<GlobalStatus, byte[]>) cursorValuesField.get(cursor);
        Assertions.assertArrayEquals(expectedCursorValue, copiedCursorValues.get(GlobalStatus.Committed));
        Assertions.assertNotSame(cursorValue, copiedCursorValues.get(GlobalStatus.Committed));
        Assertions.assertThrows(UnsupportedOperationException.class, copiedCursorValues::clear);
    }

    @Test
    void testRecoveryCursorRejectsStatusMismatch() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("recovery-cursor-status-mismatch")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession first = globalSession("tx-recovery-mismatch-first", GlobalStatus.Committed);
            GlobalSession second = globalSession("tx-recovery-mismatch-second", GlobalStatus.Committed);
            first.setBeginTime(100L);
            second.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, first);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, second);
            RocksDBTransactionStoreManager.RecoveryScanPage firstPage =
                    storeManager.readRecoveryPage(new GlobalStatus[] {GlobalStatus.Committed}, null);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> storeManager.readRecoveryPage(
                            new GlobalStatus[] {GlobalStatus.Rollbacked}, firstPage.getContinuation()));

            Assertions.assertEquals("recovery cursor statuses do not match requested statuses", exception.getMessage());
        }
    }

    @Test
    void testRecoveryPageRecordsExpiredDeadlineAfterMandatoryProgress() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "2");
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, "1");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("recovery-expired-deadline-progress")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            replaceIndexManager(storeManager, new SlowIndexManager(engine));
            GlobalSession committed = globalSession("tx-recovery-expired-deadline", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);

            RocksDBTransactionStoreManager.RecoveryScanPage page =
                    storeManager.readRecoveryPage(new GlobalStatus[] {GlobalStatus.Committed}, null);

            Assertions.assertEquals(1, page.getSessions().size());
            Assertions.assertTrue(page.isExhausted());
            Assertions.assertTrue(page.isDeadlineReached());
        }
    }

    @Test
    void testMultiStatusScanPageSizeUsesConfiguredValue() throws Exception {
        environment.withProperty("seata." + ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE, "64");
        ConfigurationCache.clear();
        try (RocksDBStoreEngine engine = open("condition-multi-status-page-size")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            Assertions.assertEquals(64, multiStatusScanPageSize(storeManager));
        }
    }

    @Test
    void testReadByMultipleStatusesAndLimitUsesGlobalBeginTimeOrder() {
        try (RocksDBStoreEngine engine = open("condition-multi-status-limit-order")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession firstStatusLate = globalSession("tx-first-status-late", GlobalStatus.Committed);
            firstStatusLate.setBeginTime(300L);
            GlobalSession secondStatusEarly = globalSession("tx-second-status-early", GlobalStatus.Rollbacked);
            secondStatusEarly.setBeginTime(100L);
            GlobalSession secondStatusMiddle = globalSession("tx-second-status-middle", GlobalStatus.Rollbacked);
            secondStatusMiddle.setBeginTime(200L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, firstStatusLate);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, secondStatusEarly);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, secondStatusMiddle);

            SessionCondition condition = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            condition.setLazyLoadBranch(true);
            condition.setLimit(2);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            Assertions.assertEquals(secondStatusEarly.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(secondStatusMiddle.getXid(), actual.get(1).getXid());
        }
    }

    @Test
    void testReadByMultipleStatusesAndLimitResumesWithPerStatusCursors() {
        try (RocksDBStoreEngine engine = open("condition-multi-status-limit-cursor")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession committedEarly = globalSession("tx-committed-early", GlobalStatus.Committed);
            committedEarly.setBeginTime(100L);
            GlobalSession rollbackedMiddle = globalSession("tx-rollbacked-middle", GlobalStatus.Rollbacked);
            rollbackedMiddle.setBeginTime(200L);
            GlobalSession committedLate = globalSession("tx-committed-late", GlobalStatus.Committed);
            committedLate.setBeginTime(300L);
            GlobalSession rollbackedLate = globalSession("tx-rollbacked-late", GlobalStatus.Rollbacked);
            rollbackedLate.setBeginTime(400L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, committedLate);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbackedLate);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committedEarly);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbackedMiddle);

            SessionCondition firstPage = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            firstPage.setLazyLoadBranch(true);
            firstPage.setLimit(2);
            List<GlobalSession> firstActual = storeManager.readSession(firstPage);

            Assertions.assertEquals(
                    List.of(committedEarly.getXid(), rollbackedMiddle.getXid()),
                    firstActual.stream().map(GlobalSession::getXid).collect(java.util.stream.Collectors.toList()));
            Map<GlobalStatus, byte[]> nextCursors = firstPage.getNextStatusScanCursors();
            Assertions.assertNotNull(nextCursors.get(GlobalStatus.Committed));
            Assertions.assertNotNull(nextCursors.get(GlobalStatus.Rollbacked));

            SessionCondition secondPage = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            secondPage.setLazyLoadBranch(true);
            secondPage.setLimit(2);
            secondPage.setStatusScanCursors(nextCursors);
            List<GlobalSession> secondActual = storeManager.readSession(secondPage);

            Assertions.assertEquals(
                    List.of(committedLate.getXid(), rollbackedLate.getXid()),
                    secondActual.stream().map(GlobalSession::getXid).collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    void testReadByMultipleStatusesKeepsExhaustedStatusCursorUntilOtherStatusesFinish() {
        try (RocksDBStoreEngine engine = open("condition-multi-status-exhausted-cursor")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession committed = globalSession("tx-committed-only", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            GlobalSession rollbackedFirst = globalSession("tx-rollbacked-first", GlobalStatus.Rollbacked);
            rollbackedFirst.setBeginTime(200L);
            GlobalSession rollbackedSecond = globalSession("tx-rollbacked-second", GlobalStatus.Rollbacked);
            rollbackedSecond.setBeginTime(300L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbackedFirst);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbackedSecond);

            SessionCondition firstPage = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            firstPage.setLazyLoadBranch(true);
            firstPage.setLimit(2);
            List<GlobalSession> firstActual = storeManager.readSession(firstPage);

            Assertions.assertEquals(
                    List.of(committed.getXid(), rollbackedFirst.getXid()),
                    firstActual.stream().map(GlobalSession::getXid).collect(java.util.stream.Collectors.toList()));
            Assertions.assertNotNull(firstPage.getNextStatusScanCursors().get(GlobalStatus.Committed));

            SessionCondition secondPage = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            secondPage.setLazyLoadBranch(true);
            secondPage.setLimit(2);
            secondPage.setStatusScanCursors(firstPage.getNextStatusScanCursors());
            List<GlobalSession> secondActual = storeManager.readSession(secondPage);

            Assertions.assertEquals(
                    List.of(rollbackedSecond.getXid()),
                    secondActual.stream().map(GlobalSession::getXid).collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    void testReadByMultipleStatusesClearsCursorsAfterCombinedPassExhausts() {
        try (RocksDBStoreEngine engine = open("condition-multi-status-combined-pass-exhausted")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession committed = globalSession("tx-pass-committed", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            GlobalSession rollbacked = globalSession("tx-pass-rollbacked", GlobalStatus.Rollbacked);
            rollbacked.setBeginTime(200L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbacked);

            Map<GlobalStatus, byte[]> cursors = Collections.emptyMap();
            List<String> seenXids = new ArrayList<>();
            boolean exhausted = false;
            for (int page = 0; page < 5; page++) {
                SessionCondition condition = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
                condition.setLazyLoadBranch(true);
                condition.setLimit(1);
                condition.setScanLimit(1);
                condition.setStatusScanCursors(cursors);
                List<GlobalSession> actual = storeManager.readSession(condition);
                cursors = condition.getNextStatusScanCursors();
                if (condition.getStatusScanContinuation() == SessionCondition.ScanContinuation.EXHAUSTED) {
                    exhausted = true;
                    actual.forEach(session -> seenXids.add(session.getXid()));
                    break;
                }
                Assertions.assertEquals(
                        SessionCondition.ScanContinuation.RESUMABLE, condition.getStatusScanContinuation());
                seenXids.add(actual.get(0).getXid());
            }

            Assertions.assertTrue(exhausted, "the combined status pass must terminate");
            Assertions.assertEquals(List.of(committed.getXid(), rollbacked.getXid()), seenXids);
            Assertions.assertTrue(cursors.isEmpty(), "an exhausted combined pass must clear every status cursor");

            GlobalSession earlier = globalSession("tx-pass-new-earlier", GlobalStatus.Committed);
            earlier.setBeginTime(50L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, earlier);
            SessionCondition restarted = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            restarted.setLazyLoadBranch(true);
            restarted.setLimit(1);
            restarted.setScanLimit(1);
            restarted.setStatusScanCursors(cursors);

            List<GlobalSession> restartedActual = storeManager.readSession(restarted);

            Assertions.assertEquals(1, restartedActual.size());
            Assertions.assertEquals(earlier.getXid(), restartedActual.get(0).getXid());
        }
    }

    @Test
    void testReadByMultipleStatusesIgnoresStaleStatusIndexForOrdering() {
        try (RocksDBStoreEngine engine = open("condition-multi-status-stale-index-order")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession committed = globalSession("tx-committed-valid-index", GlobalStatus.Committed);
            committed.setBeginTime(100L);
            GlobalSession rollbacked = globalSession("tx-rollbacked-valid-index", GlobalStatus.Rollbacked);
            rollbacked.setBeginTime(200L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, rollbacked);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Committed, 50L, rollbacked.getXid()),
                    rollbacked.getXid().getBytes(StandardCharsets.UTF_8));

            SessionCondition condition = new SessionCondition(GlobalStatus.Committed, GlobalStatus.Rollbacked);
            condition.setLazyLoadBranch(true);
            condition.setLimit(2);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(2, actual.size());
            Assertions.assertEquals(committed.getXid(), actual.get(0).getXid());
            Assertions.assertEquals(rollbacked.getXid(), actual.get(1).getXid());
        }
    }

    @Test
    void testReadByStatusAndLimitIgnoresStaleBeginTimeIndexForOrdering() {
        try (RocksDBStoreEngine engine = open("condition-status-stale-begin-time-index-order")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession early = globalSession("tx-committed-early-valid-index", GlobalStatus.Committed);
            early.setBeginTime(100L);
            GlobalSession late = globalSession("tx-committed-late-valid-index", GlobalStatus.Committed);
            late.setBeginTime(300L);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, early);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, late);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Committed, 50L, late.getXid()),
                    late.getXid().getBytes(StandardCharsets.UTF_8));

            SessionCondition condition = new SessionCondition(GlobalStatus.Committed);
            condition.setLazyLoadBranch(true);
            condition.setLimit(1);
            List<GlobalSession> actual = storeManager.readSession(condition);

            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(early.getXid(), actual.get(0).getXid());
        }
    }

    @Test
    void testReadByStatusScanLimitCountsStaleEntriesAndResumesExactly() {
        int scanLimit = 4;
        try (RocksDBStoreEngine engine = open("condition-status-stale-scan-limit")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            for (int i = 0; i <= scanLimit; i++) {
                String staleXid = "tx-stale-scan-limit-" + i;
                engine.put(
                        RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                        RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, 100L + i, staleXid),
                        staleXid.getBytes(StandardCharsets.UTF_8));
            }
            GlobalSession valid = globalSession("tx-valid-after-stale-scan-limit", GlobalStatus.Begin);
            valid.setBeginTime(100L + scanLimit + 1L);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, valid);

            SessionCondition firstPage = new SessionCondition(GlobalStatus.Begin);
            firstPage.setLazyLoadBranch(true);
            firstPage.setLimit(1);
            firstPage.setScanLimit(scanLimit);
            List<GlobalSession> firstActual = storeManager.readSession(firstPage);

            Assertions.assertTrue(firstActual.isEmpty());
            Assertions.assertNotNull(firstPage.getNextStatusScanCursor());

            SessionCondition secondPage = new SessionCondition(GlobalStatus.Begin);
            secondPage.setLazyLoadBranch(true);
            secondPage.setLimit(1);
            secondPage.setScanLimit(scanLimit);
            secondPage.setStatusScanCursor(firstPage.getNextStatusScanCursor());
            List<GlobalSession> secondActual = storeManager.readSession(secondPage);

            Assertions.assertEquals(1, secondActual.size());
            Assertions.assertEquals(valid.getXid(), secondActual.get(0).getXid());
            Assertions.assertNotNull(secondPage.getNextStatusScanCursor());

            SessionCondition finalPage = new SessionCondition(GlobalStatus.Begin);
            finalPage.setLazyLoadBranch(true);
            finalPage.setLimit(1);
            finalPage.setScanLimit(scanLimit);
            finalPage.setStatusScanCursor(secondPage.getNextStatusScanCursor());

            Assertions.assertTrue(storeManager.readSession(finalPage).isEmpty());
            Assertions.assertNull(finalPage.getNextStatusScanCursor());
        }
    }

    @Test
    void testReadByEmptySessionConditionScansAll() {
        try (RocksDBStoreEngine engine = open("condition-all")) {
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            GlobalSession begin = globalSession("tx-begin-all", GlobalStatus.Begin);
            GlobalSession committed = globalSession("tx-committed-all", GlobalStatus.Committed);

            storeManager.writeSession(LogOperation.GLOBAL_ADD, begin);
            storeManager.writeSession(LogOperation.GLOBAL_ADD, committed);

            List<GlobalSession> actual = storeManager.readSession(new SessionCondition());

            Assertions.assertEquals(2, actual.size());
        }
    }

    private RocksDBStoreEngine open(String name) {
        return open(name, false);
    }

    private RocksDBStoreEngine open(String name, boolean enableRangeDelete) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true, enableRangeDelete));
    }

    private void assertContinuationOutputsUnset(SessionCondition condition) {
        Assertions.assertNull(condition.getNextStatusScanCursor());
        Assertions.assertEquals(SessionCondition.ScanContinuation.UNSET, condition.getStatusScanContinuation());
        Assertions.assertNull(condition.getNextTimeoutScanCursor());
        Assertions.assertEquals(SessionCondition.ScanContinuation.UNSET, condition.getTimeoutScanContinuation());
    }

    private GlobalSession globalSession(String name, GlobalStatus status) {
        return globalSession(name, status, 60000);
    }

    private GlobalSession globalSession(String name, GlobalStatus status, int timeout) {
        GlobalSession globalSession = new GlobalSession("app", "group", name, timeout);
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

    @SuppressWarnings("unchecked")
    private Map<String, SessionManager> getSessionManagerMap() throws Exception {
        Field field = SessionHolder.class.getDeclaredField("SESSION_MANAGER_MAP");
        field.setAccessible(true);
        return (Map<String, SessionManager>) field.get(null);
    }

    private SessionManager getRootSessionManager() throws Exception {
        Field field = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
        field.setAccessible(true);
        return (SessionManager) field.get(null);
    }

    private void setRootSessionManager(SessionManager sessionManager) throws Exception {
        Field rootField = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
        rootField.setAccessible(true);
        rootField.set(null, sessionManager);
        Field mapField = SessionHolder.class.getDeclaredField("SESSION_MANAGER_MAP");
        mapField.setAccessible(true);
        mapField.set(null, null);
    }

    private void restoreSessionHolder() throws Exception {
        Field rootField = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
        rootField.setAccessible(true);
        rootField.set(null, originalRootSessionManager);
        Field mapField = SessionHolder.class.getDeclaredField("SESSION_MANAGER_MAP");
        mapField.setAccessible(true);
        mapField.set(null, originalSessionManagerMap);
    }

    private void replaceIndexManager(RocksDBTransactionStoreManager storeManager, RocksDBIndexManager indexManager)
            throws Exception {
        Field field = RocksDBTransactionStoreManager.class.getDeclaredField("indexManager");
        field.setAccessible(true);
        field.set(storeManager, indexManager);
    }

    private long fullScanDeadlineMillis(RocksDBTransactionStoreManager storeManager) throws Exception {
        Field field = RocksDBTransactionStoreManager.class.getDeclaredField("fullScanDeadlineMillis");
        field.setAccessible(true);
        return field.getLong(storeManager);
    }

    private int multiStatusScanPageSize(RocksDBTransactionStoreManager storeManager) throws Exception {
        Field field = RocksDBTransactionStoreManager.class.getDeclaredField("multiStatusScanPageSize");
        field.setAccessible(true);
        return field.getInt(storeManager);
    }

    private static final class CountingIndexManager extends RocksDBIndexManager {
        private int fullStatusScanCalls;
        private int pagedStatusScanCalls;

        private CountingIndexManager(RocksDBStoreEngine storeEngine) {
            super(storeEngine);
        }

        @Override
        public void scanXidsByStatus(GlobalStatus status, Consumer<String> consumer) {
            fullStatusScanCalls++;
            super.scanXidsByStatus(status, consumer);
        }

        @Override
        public List<String> scanXidsByStatus(GlobalStatus status) {
            fullStatusScanCalls++;
            return super.scanXidsByStatus(status);
        }

        @Override
        public StatusScanResult scanXidsByStatus(
                GlobalStatus status, long minBeginTimeInclusive, long maxBeginTimeInclusive, byte[] cursor, int limit) {
            pagedStatusScanCalls++;
            return super.scanXidsByStatus(status, minBeginTimeInclusive, maxBeginTimeInclusive, cursor, limit);
        }
    }

    private static final class DeadlineExpiringCondition extends SessionCondition {
        private boolean expireOnStatusCursorRead;
        private boolean expireOnStatusCursorMapRead;

        private DeadlineExpiringCondition(
                boolean expireOnStatusCursorRead, boolean expireOnStatusCursorMapRead, GlobalStatus... statuses) {
            super(statuses);
            this.expireOnStatusCursorRead = expireOnStatusCursorRead;
            this.expireOnStatusCursorMapRead = expireOnStatusCursorMapRead;
        }

        @Override
        public byte[] getStatusScanCursor() {
            if (expireOnStatusCursorRead) {
                expireOnStatusCursorRead = false;
                waitPastConfiguredDeadline();
            }
            return super.getStatusScanCursor();
        }

        @Override
        public Map<GlobalStatus, byte[]> getStatusScanCursors() {
            if (expireOnStatusCursorMapRead) {
                expireOnStatusCursorMapRead = false;
                waitPastConfiguredDeadline();
            }
            return super.getStatusScanCursors();
        }

        private void waitPastConfiguredDeadline() {
            long startedAt = System.nanoTime();
            long elapsed;
            do {
                elapsed = System.nanoTime() - startedAt;
            } while (elapsed < TimeUnit.MILLISECONDS.toNanos(5));
        }
    }

    private static final class SlowIndexManager extends RocksDBIndexManager {

        private SlowIndexManager(RocksDBStoreEngine storeEngine) {
            super(storeEngine);
        }

        @Override
        public StatusScanResult scanXidsByStatus(
                GlobalStatus status, long minBeginTimeInclusive, long maxBeginTimeInclusive, byte[] cursor, int limit) {
            try {
                TimeUnit.MILLISECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return super.scanXidsByStatus(status, minBeginTimeInclusive, maxBeginTimeInclusive, cursor, limit);
        }
    }
}
