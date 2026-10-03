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

import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

class RocksDBSessionManagerTest {

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

    private boolean contains(Collection<GlobalSession> sessions, GlobalSession expected) {
        return sessions.stream().anyMatch(session -> expected.getXid().equals(session.getXid()));
    }

    private void assertXids(Collection<GlobalSession> sessions, String... xids) {
        Assertions.assertEquals(xids.length, sessions.size());
        Set<String> actual = sessions.stream().map(GlobalSession::getXid).collect(Collectors.toSet());
        Assertions.assertEquals(new HashSet<>(Arrays.asList(xids)), actual);
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
