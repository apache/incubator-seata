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

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.AbstractSessionManager;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionScanStats;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBLocalLocks;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RocksDB-backed session manager for file store engine.
 */
@LoadLevel(name = "rocksdb", scope = Scope.PROTOTYPE)
public class RocksDBSessionManager extends AbstractSessionManager {

    private static final GlobalStatus[] RECOVERY_STATUSES = {
        GlobalStatus.UnKnown,
        GlobalStatus.Begin,
        GlobalStatus.Committing,
        GlobalStatus.CommitRetrying,
        GlobalStatus.Rollbacking,
        GlobalStatus.RollbackRetrying,
        GlobalStatus.TimeoutRollbacking,
        GlobalStatus.TimeoutRollbackRetrying,
        GlobalStatus.AsyncCommitting,
        GlobalStatus.Committed,
        GlobalStatus.CommitFailed,
        GlobalStatus.CommitRetryTimeout,
        GlobalStatus.Rollbacked,
        GlobalStatus.RollbackFailed,
        GlobalStatus.RollbackRetryTimeout,
        GlobalStatus.TimeoutRollbacked,
        GlobalStatus.TimeoutRollbackFailed,
        GlobalStatus.Finished,
        GlobalStatus.StopRollbackOrRollbackRetry,
        GlobalStatus.StopCommitOrCommitRetry,
        GlobalStatus.Deleting
    };

    private final RocksDBLocalLocks xidLocks;

    public RocksDBSessionManager() {
        this(null);
    }

    public RocksDBSessionManager(String name) {
        super(name);
        this.xidLocks = new RocksDBLocalLocks();
        this.transactionStoreManager = new RocksDBTransactionStoreManager(xidLocks);
    }

    public RocksDBSessionManager(String name, RocksDBStoreEngine storeEngine) {
        super(name);
        this.xidLocks = new RocksDBLocalLocks();
        this.transactionStoreManager = new RocksDBTransactionStoreManager(storeEngine, xidLocks);
    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return findGlobalSession(xid, true);
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return transactionStoreManager.readSession(xid, withBranchSessions);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        if (!LockerManagerFactory.getLockManager().releaseGlobalSessionLock(session)) {
            throw new TransactionException("Release RocksDB global session lock failed, xid = " + session.getXid());
        }
        super.removeGlobalSession(session);
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return findGlobalSessions(new SessionCondition(RECOVERY_STATUSES));
    }

    public RecoveryPage readStartupRecoveryPage(RecoveryCursor cursor) {
        RocksDBTransactionStoreManager.RecoveryScanPage scanPage = ((RocksDBTransactionStoreManager)
                        transactionStoreManager)
                .readRecoveryPage(RECOVERY_STATUSES, cursor.storeCursor);
        RecoveryCursor continuation = scanPage.isExhausted() ? null : new RecoveryCursor(scanPage.getContinuation());
        return new RecoveryPage(scanPage.getSessions(), continuation, scanPage.isExhausted());
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        if (condition == null || hasPositiveBound(condition)) {
            return transactionStoreManager.readSession(condition);
        }
        if (isTimeoutDeadlineQuery(condition)) {
            return readAllTimeoutPages(condition);
        }
        GlobalStatus[] statuses = condition.getStatuses();
        if (statuses == null || statuses.length == 0) {
            return transactionStoreManager.readSession(condition);
        }
        if (statuses.length == 1) {
            return readAllSingleStatusPages(condition);
        }
        return readAllMultiStatusPages(condition);
    }

    private List<GlobalSession> readAllSingleStatusPages(SessionCondition condition) {
        List<GlobalSession> sessions = new ArrayList<>();
        Set<String> seenXids = new LinkedHashSet<>();
        byte[] callerCursor = condition.getStatusScanCursor();
        byte[] cursor = callerCursor;
        ScanStatsAccumulator scanStats = new ScanStatsAccumulator();
        try {
            while (true) {
                appendPage(sessions, seenXids, transactionStoreManager.readSession(condition));
                scanStats.add(condition.getScanStats());
                SessionCondition.ScanContinuation continuation = condition.getStatusScanContinuation();
                if (continuation == SessionCondition.ScanContinuation.EXHAUSTED) {
                    return sessions;
                }
                if (continuation != SessionCondition.ScanContinuation.RESUMABLE) {
                    throw new IllegalStateException("status scan continuation state is unset");
                }
                byte[] nextCursor = condition.getNextStatusScanCursor();
                if (!hasStrictCursorProgress(cursor, nextCursor)) {
                    throw new IllegalStateException("status scan continuation did not advance");
                }
                condition.setStatusScanCursor(nextCursor);
                cursor = nextCursor;
            }
        } finally {
            condition.setStatusScanCursor(callerCursor);
            condition.setScanStats(scanStats.toStats(sessions.size()));
        }
    }

    private List<GlobalSession> readAllMultiStatusPages(SessionCondition condition) {
        List<GlobalSession> sessions = new ArrayList<>();
        Set<String> seenXids = new LinkedHashSet<>();
        Map<GlobalStatus, byte[]> callerCursors = condition.getStatusScanCursors();
        Map<GlobalStatus, byte[]> cursors = callerCursors;
        ScanStatsAccumulator scanStats = new ScanStatsAccumulator();
        try {
            while (true) {
                appendPage(sessions, seenXids, transactionStoreManager.readSession(condition));
                scanStats.add(condition.getScanStats());
                SessionCondition.ScanContinuation continuation = condition.getStatusScanContinuation();
                if (continuation == SessionCondition.ScanContinuation.EXHAUSTED) {
                    return sessions;
                }
                if (continuation != SessionCondition.ScanContinuation.RESUMABLE) {
                    throw new IllegalStateException("multi-status scan continuation state is unset");
                }
                Map<GlobalStatus, byte[]> nextCursors = condition.getNextStatusScanCursors();
                if (!hasStrictMultiStatusProgress(cursors, nextCursors)) {
                    throw new IllegalStateException("multi-status scan continuation did not advance");
                }
                condition.setStatusScanCursors(nextCursors);
                cursors = nextCursors;
            }
        } finally {
            condition.setStatusScanCursors(callerCursors);
            condition.setScanStats(scanStats.toStats(sessions.size()));
        }
    }

    private List<GlobalSession> readAllTimeoutPages(SessionCondition condition) {
        List<GlobalSession> sessions = new ArrayList<>();
        Set<String> seenXids = new LinkedHashSet<>();
        byte[] callerCursor = condition.getTimeoutScanCursor();
        byte[] cursor = callerCursor;
        ScanStatsAccumulator scanStats = new ScanStatsAccumulator();
        try {
            while (true) {
                appendPage(sessions, seenXids, transactionStoreManager.readSession(condition));
                scanStats.add(condition.getScanStats());
                SessionCondition.ScanContinuation continuation = condition.getTimeoutScanContinuation();
                if (continuation == SessionCondition.ScanContinuation.EXHAUSTED) {
                    return sessions;
                }
                if (continuation != SessionCondition.ScanContinuation.RESUMABLE) {
                    throw new IllegalStateException("timeout scan continuation state is unset");
                }
                byte[] nextCursor = condition.getNextTimeoutScanCursor();
                if (!hasStrictCursorProgress(cursor, nextCursor)) {
                    throw new IllegalStateException("timeout scan continuation did not advance");
                }
                condition.setTimeoutScanCursor(nextCursor);
                cursor = nextCursor;
            }
        } finally {
            condition.setTimeoutScanCursor(callerCursor);
            condition.setScanStats(scanStats.toStats(sessions.size()));
        }
    }

    private boolean hasPositiveBound(SessionCondition condition) {
        return isPositive(condition.getLimit()) || isPositive(condition.getScanLimit());
    }

    private boolean isTimeoutDeadlineQuery(SessionCondition condition) {
        GlobalStatus[] statuses = condition.getStatuses();
        return condition.getMaxTimeoutDeadlineMillis() != null
                && statuses != null
                && statuses.length == 1
                && statuses[0] == GlobalStatus.Begin;
    }

    private boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    private void appendPage(List<GlobalSession> sessions, Set<String> seenXids, List<GlobalSession> page) {
        for (GlobalSession session : page) {
            if (seenXids.add(session.getXid())) {
                sessions.add(session);
            }
        }
    }

    private boolean hasStrictMultiStatusProgress(
            Map<GlobalStatus, byte[]> currentCursors, Map<GlobalStatus, byte[]> nextCursors) {
        if (nextCursors.isEmpty()) {
            return false;
        }
        boolean advanced = false;
        for (Map.Entry<GlobalStatus, byte[]> entry : currentCursors.entrySet()) {
            byte[] nextCursor = nextCursors.get(entry.getKey());
            if (nextCursor == null) {
                return false;
            }
            int comparison = compareUnsigned(nextCursor, entry.getValue());
            if (comparison < 0) {
                return false;
            }
            advanced |= comparison > 0;
        }
        for (Map.Entry<GlobalStatus, byte[]> entry : nextCursors.entrySet()) {
            if (!currentCursors.containsKey(entry.getKey())) {
                if (entry.getValue().length == 0) {
                    return false;
                }
                advanced = true;
            }
        }
        return advanced;
    }

    private boolean hasStrictCursorProgress(byte[] currentCursor, byte[] nextCursor) {
        return nextCursor != null
                && nextCursor.length > 0
                && (currentCursor == null || compareUnsigned(nextCursor, currentCursor) > 0);
    }

    private int compareUnsigned(byte[] left, byte[] right) {
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int comparison = (left[i] & 0xff) - (right[i] & 0xff);
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static final class ScanStatsAccumulator {
        private long rowsScanned;
        private long rowsReturned;
        private long pointReads;
        private long elapsedMillis;
        private boolean limitReached;

        private void add(SessionScanStats stats) {
            if (stats == null) {
                return;
            }
            rowsScanned += stats.getRowsScanned();
            rowsReturned += stats.getRowsReturned();
            pointReads += stats.getPointReads();
            elapsedMillis += stats.getElapsedMillis();
            limitReached |= stats.isLimitReached();
        }

        private SessionScanStats toStats(long sessionsReturned) {
            return new SessionScanStats(
                    rowsScanned, rowsReturned, pointReads, sessionsReturned, elapsedMillis, limitReached);
        }
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        try (RocksDBLocalLocks.LockScope ignored = xidLocks.lock(RocksDBKeyCodec.encodeXid(globalSession.getXid()))) {
            return lockCallable.call();
        }
    }

    @Override
    public void destroy() {
        transactionStoreManager.shutdown();
    }

    public static final class RecoveryCursor {
        private final RocksDBTransactionStoreManager.RecoveryCursor storeCursor;

        private RecoveryCursor(RocksDBTransactionStoreManager.RecoveryCursor storeCursor) {
            this.storeCursor = storeCursor;
        }

        public static RecoveryCursor initial() {
            return new RecoveryCursor(null);
        }
    }

    public static final class RecoveryPage {
        private final List<GlobalSession> sessions;
        private final RecoveryCursor continuation;
        private final boolean exhausted;

        private RecoveryPage(List<GlobalSession> sessions, RecoveryCursor continuation, boolean exhausted) {
            this.sessions = sessions;
            this.continuation = continuation;
            this.exhausted = exhausted;
        }

        public List<GlobalSession> getSessions() {
            return sessions;
        }

        public RecoveryCursor getContinuation() {
            return continuation;
        }

        public boolean isExhausted() {
            return exhausted;
        }
    }
}
