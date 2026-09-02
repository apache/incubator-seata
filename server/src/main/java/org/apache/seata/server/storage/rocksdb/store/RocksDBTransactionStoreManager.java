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
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionScanStats;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBLocalLocks;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.apache.seata.server.storage.rocksdb.index.RocksDBIndexManager;
import org.apache.seata.server.store.AbstractTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * RocksDB transaction store manager for file store engine.
 */
public class RocksDBTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBTransactionStoreManager.class);
    private static final String MIGRATION_STATUS_KEY = "migration_status";
    private static final int STATUS_SCAN_PAGE_SIZE = 1024;
    private static final int DEFAULT_MULTI_STATUS_SCAN_PAGE_SIZE = 256;
    private static final int DEFAULT_FULL_SCAN_MAX_LIMIT = 10000;
    private static final long DEFAULT_FULL_SCAN_DEADLINE_MILLIS = 5000L;

    private final RocksDBStoreEngine storeEngine;
    private final RocksDBLocalLocks xidLocks;
    private final RocksDBIndexManager indexManager;
    private final boolean factoryManaged;
    private final int fullScanMaxLimit;
    private final long fullScanDeadlineMillis;
    private final int multiStatusScanPageSize;

    public RocksDBTransactionStoreManager() {
        this(RocksDBStoreEngineFactory.getInstance(), new RocksDBLocalLocks(), true);
    }

    public RocksDBTransactionStoreManager(RocksDBLocalLocks xidLocks) {
        this(RocksDBStoreEngineFactory.getInstance(), xidLocks, true);
    }

    public RocksDBTransactionStoreManager(RocksDBStoreEngine storeEngine) {
        this(storeEngine, new RocksDBLocalLocks(), false);
    }

    public RocksDBTransactionStoreManager(RocksDBStoreEngine storeEngine, RocksDBLocalLocks xidLocks) {
        this(storeEngine, xidLocks, false);
    }

    private RocksDBTransactionStoreManager(
            RocksDBStoreEngine storeEngine, RocksDBLocalLocks xidLocks, boolean factoryManaged) {
        this.storeEngine = storeEngine;
        this.xidLocks = xidLocks;
        this.indexManager = new RocksDBIndexManager(storeEngine);
        this.indexManager.ensureReady();
        this.factoryManaged = factoryManaged;
        Configuration config = ConfigurationFactory.getInstance();
        this.fullScanMaxLimit =
                config.getInt(ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_MAX_LIMIT, DEFAULT_FULL_SCAN_MAX_LIMIT);
        this.fullScanDeadlineMillis = config.getLong(
                ConfigurationKeys.STORE_FILE_ROCKSDB_FULL_SCAN_DEADLINE_MILLIS, DEFAULT_FULL_SCAN_DEADLINE_MILLIS);
        this.multiStatusScanPageSize = Math.max(
                1,
                config.getInt(
                        ConfigurationKeys.STORE_FILE_ROCKSDB_MULTI_STATUS_SCAN_PAGE_SIZE,
                        DEFAULT_MULTI_STATUS_SCAN_PAGE_SIZE));
        logStartupState();
    }

    private void logStartupState() {
        byte[] indexVersionBytes = storeEngine.get(
                RocksDBColumnFamily.METADATA, RocksDBIndexManager.INDEX_VERSION_KEY.getBytes(StandardCharsets.UTF_8));
        byte[] indexBuildStatusBytes = storeEngine.get(
                RocksDBColumnFamily.METADATA,
                RocksDBIndexManager.INDEX_BUILD_STATUS_KEY.getBytes(StandardCharsets.UTF_8));
        byte[] migrationStatusBytes =
                storeEngine.get(RocksDBColumnFamily.METADATA, MIGRATION_STATUS_KEY.getBytes(StandardCharsets.UTF_8));
        String indexVersion =
                indexVersionBytes != null ? new String(indexVersionBytes, StandardCharsets.UTF_8) : "none";
        String indexBuildStatus =
                indexBuildStatusBytes != null ? new String(indexBuildStatusBytes, StandardCharsets.UTF_8) : "none";
        String migrationStatus =
                migrationStatusBytes != null ? new String(migrationStatusBytes, StandardCharsets.UTF_8) : "none";
        LOGGER.info(
                "RocksDB transaction store ready, indexVersion:{}, indexBuildStatus:{}, migrationStatus:{}",
                indexVersion,
                indexBuildStatus,
                migrationStatus);
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (session == null) {
            return true;
        }
        String xid = getXid(session);
        try (RocksDBLocalLocks.LockScope ignored = xidLocks.lock(RocksDBKeyCodec.encodeXid(xid))) {
            switch (logOperation) {
                case GLOBAL_ADD:
                case GLOBAL_UPDATE:
                    writeGlobalSession((GlobalSession) session);
                    return true;
                case GLOBAL_REMOVE:
                    removeGlobalSession((GlobalSession) session);
                    return true;
                case BRANCH_ADD:
                case BRANCH_UPDATE:
                    writeBranchSession((BranchSession) session);
                    return true;
                case BRANCH_REMOVE:
                    removeBranchSession((BranchSession) session);
                    return true;
                default:
                    throw new StoreException("Unknown LogOperation:" + logOperation.name());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new StoreException(e, "write RocksDB session failed, logOperation:" + logOperation.name());
        }
    }

    @Override
    public GlobalSession readSession(String xid) {
        return readSession(xid, true);
    }

    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        byte[] value = storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid));
        if (value == null) {
            return null;
        }
        GlobalSession globalSession = decodeGlobalSession(value, !withBranchSessions);
        if (withBranchSessions) {
            readBranchSessions(xid).forEach(globalSession::add);
        }
        return globalSession;
    }

    @Override
    public List<GlobalSession> readSortByTimeoutBeginSessions(boolean withBranchSessions) {
        return readSession(new GlobalStatus[] {GlobalStatus.Begin}, withBranchSessions);
    }

    @Override
    public List<GlobalSession> readSession(GlobalStatus[] statuses, boolean withBranchSessions) {
        if (statuses == null || statuses.length == 0) {
            return Collections.emptyList();
        }
        SessionCondition sessionCondition = new SessionCondition(statuses);
        sessionCondition.setLazyLoadBranch(!withBranchSessions);
        return readByStatuses(sessionCondition);
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        if (sessionCondition == null) {
            return Collections.emptyList();
        }
        sessionCondition.clearNextStatusScanCursor();
        sessionCondition.clearNextStatusScanCursors();
        sessionCondition.clearNextTimeoutScanCursor();
        sessionCondition.clearScanStats();
        if (StringUtils.isNotBlank(sessionCondition.getXid())) {
            GlobalSession globalSession = readSession(sessionCondition.getXid(), !sessionCondition.isLazyLoadBranch());
            if (globalSession == null || !matches(globalSession, sessionCondition)) {
                return Collections.emptyList();
            }
            return Collections.singletonList(globalSession);
        }
        if (sessionCondition.getTransactionId() != null && sessionCondition.getTransactionId() > 0) {
            return readByTransactionId(sessionCondition);
        }
        if (shouldUseTimeoutDeadlineScan(sessionCondition)) {
            return readByTimeoutDeadline(sessionCondition);
        }
        if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readByStatuses(sessionCondition);
        }
        return scanGlobalSessions(sessionCondition);
    }

    public RecoveryScanPage readRecoveryPage(GlobalStatus[] statuses, RecoveryCursor cursor) {
        GlobalStatus[] requestedStatuses = copyAndValidateRecoveryStatuses(statuses);
        RecoveryCursor effectiveCursor;
        if (cursor == null) {
            effectiveCursor = new RecoveryCursor(requestedStatuses, Collections.emptyMap());
        } else {
            if (!sameRecoveryStatuses(requestedStatuses, cursor.statuses)) {
                throw new IllegalArgumentException("recovery cursor statuses do not match requested statuses");
            }
            effectiveCursor = cursor;
        }
        SessionCondition sessionCondition =
                new SessionCondition(Arrays.copyOf(effectiveCursor.statuses, effectiveCursor.statuses.length));
        sessionCondition.setLazyLoadBranch(false);
        sessionCondition.setStatusScanCursors(effectiveCursor.statusScanCursors);
        SessionScanStatsAccumulator scanStats = new SessionScanStatsAccumulator();
        long deadlineNanos = fullScanDeadlineMillis > 0
                ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(fullScanDeadlineMillis)
                : 0L;
        MultiStatusScanResult result = readByStatusesWithKWayMerge(
                sessionCondition, null, multiStatusScanPageSize, new ScanBudget(null), deadlineNanos, scanStats, true);
        if (result.isDeadlineReached()) {
            logStatusScanDeadline(sessionCondition, result.getSessions().size());
        }
        RecoveryCursor continuation = result.isFullyExhausted()
                ? null
                : new RecoveryCursor(effectiveCursor.statuses, result.getNextStatusScanCursors());
        return new RecoveryScanPage(
                result.getSessions(), continuation, result.isFullyExhausted(), result.isDeadlineReached());
    }

    private static GlobalStatus[] copyAndValidateRecoveryStatuses(GlobalStatus[] statuses) {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("recovery statuses must not be empty");
        }
        GlobalStatus[] copy = Arrays.copyOf(statuses, statuses.length);
        EnumSet<GlobalStatus> seen = EnumSet.noneOf(GlobalStatus.class);
        for (GlobalStatus status : copy) {
            if (status == null) {
                throw new IllegalArgumentException("recovery statuses must not contain null");
            }
            if (!seen.add(status)) {
                throw new IllegalArgumentException("recovery statuses must not contain duplicates");
            }
        }
        return copy;
    }

    private static boolean sameRecoveryStatuses(GlobalStatus[] requestedStatuses, GlobalStatus[] cursorStatuses) {
        if (requestedStatuses.length != cursorStatuses.length) {
            return false;
        }
        EnumSet<GlobalStatus> requested = EnumSet.noneOf(GlobalStatus.class);
        requested.addAll(Arrays.asList(requestedStatuses));
        EnumSet<GlobalStatus> owned = EnumSet.noneOf(GlobalStatus.class);
        owned.addAll(Arrays.asList(cursorStatuses));
        return requested.equals(owned);
    }

    private static Map<GlobalStatus, byte[]> copyAndValidateRecoveryCursors(
            GlobalStatus[] statuses, Map<GlobalStatus, byte[]> statusScanCursors) {
        if (statusScanCursors == null || statusScanCursors.isEmpty()) {
            return Collections.emptyMap();
        }
        EnumSet<GlobalStatus> ownedStatuses = EnumSet.noneOf(GlobalStatus.class);
        ownedStatuses.addAll(Arrays.asList(statuses));
        Map<GlobalStatus, byte[]> copies = new EnumMap<>(GlobalStatus.class);
        for (Map.Entry<GlobalStatus, byte[]> entry : statusScanCursors.entrySet()) {
            GlobalStatus status = entry.getKey();
            byte[] statusScanCursor = entry.getValue();
            if (status == null || !ownedStatuses.contains(status)) {
                throw new IllegalStateException("recovery cursor contains an unowned status");
            }
            if (statusScanCursor == null
                    || !RocksDBKeyCodec.startsWith(
                            statusScanCursor, RocksDBKeyCodec.encodeGlobalStatusPrefix(status))) {
                throw new IllegalStateException("recovery cursor status prefix does not match its status");
            }
            copies.put(status, Arrays.copyOf(statusScanCursor, statusScanCursor.length));
        }
        return Collections.unmodifiableMap(copies);
    }

    @Override
    public void shutdown() {
        if (factoryManaged) {
            RocksDBStoreEngineFactory.destroy();
        } else {
            storeEngine.close();
        }
    }

    private void writeGlobalSession(GlobalSession session) {
        byte[] key = RocksDBKeyCodec.encodeXid(session.getXid());
        try (WriteBatch batch = new WriteBatch()) {
            byte[] oldValue = storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, key);
            if (oldValue != null) {
                indexManager.deleteGlobalIndexes(batch, decodeGlobalSession(oldValue, true));
            }
            storeEngine.put(batch, RocksDBColumnFamily.GLOBAL_SESSION, key, encodeGlobalSession(session));
            indexManager.putGlobalIndexes(batch, session);
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write RocksDB global session failed, xid:" + session.getXid());
        }
    }

    private void removeGlobalSession(GlobalSession session) {
        try (WriteBatch batch = new WriteBatch()) {
            byte[] oldValue =
                    storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(session.getXid()));
            if (oldValue != null) {
                indexManager.deleteGlobalIndexes(batch, decodeGlobalSession(oldValue, true));
            }
            storeEngine.delete(batch, RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(session.getXid()));
            storeEngine.deleteByPrefix(
                    batch, RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(session.getXid()));
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "remove RocksDB global session failed, xid:" + session.getXid());
        }
    }

    private void writeBranchSession(BranchSession session) {
        storeEngine.put(
                RocksDBColumnFamily.BRANCH_SESSION,
                RocksDBKeyCodec.encodeBranch(session.getXid(), session.getBranchId()),
                encodeBranchSession(session));
    }

    private void removeBranchSession(BranchSession session) {
        storeEngine.delete(
                RocksDBColumnFamily.BRANCH_SESSION,
                RocksDBKeyCodec.encodeBranch(session.getXid(), session.getBranchId()));
    }

    private List<BranchSession> readBranchSessions(String xid) {
        List<BranchSession> branches = new ArrayList<>();
        for (RocksDBStoreEngine.RocksDBEntry entry :
                storeEngine.prefixScan(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(xid))) {
            branches.add(decodeBranchSession(entry.getValue()));
        }
        return branches;
    }

    private List<GlobalSession> scanGlobalSessions(SessionCondition sessionCondition) {
        List<GlobalSession> result = new ArrayList<>();
        long startedAtNanos = System.nanoTime();
        long deadlineNanos = fullScanDeadlineMillis > 0
                ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(fullScanDeadlineMillis)
                : 0L;
        RocksDBStoreEngine.ScanStats stats = storeEngine.scanByPrefix(
                RocksDBColumnFamily.GLOBAL_SESSION, new byte[0], fullScanMaxLimit, deadlineNanos, (key, value) -> {
                    GlobalSession globalSession = decodeGlobalSession(value, sessionCondition.isLazyLoadBranch());
                    if (matches(globalSession, sessionCondition)) {
                        if (!sessionCondition.isLazyLoadBranch()) {
                            readBranchSessions(globalSession.getXid()).forEach(globalSession::add);
                        }
                        result.add(globalSession);
                    }
                });
        sessionCondition.setScanStats(new SessionScanStats(
                stats.getRowsScanned(),
                stats.getRowsReturned(),
                0L,
                result.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos),
                stats.isTruncated()));
        if (stats.isTruncated()) {
            LOGGER.warn(
                    "scanGlobalSessions truncated: scanned={}, returned={}, limitReached={}, deadlineReached={}, "
                            + "fullScanMaxLimit={}, fullScanDeadlineMillis={}",
                    stats.getRowsScanned(),
                    stats.getRowsReturned(),
                    stats.isLimitReached(),
                    stats.isDeadlineReached(),
                    fullScanMaxLimit,
                    fullScanDeadlineMillis);
        }
        return result;
    }

    private List<GlobalSession> readByTransactionId(SessionCondition sessionCondition) {
        String xid = indexManager.findXidByTransactionId(sessionCondition.getTransactionId());
        if (StringUtils.isBlank(xid)) {
            return Collections.emptyList();
        }
        GlobalSession globalSession = readSession(xid, !sessionCondition.isLazyLoadBranch());
        if (globalSession == null || !matches(globalSession, sessionCondition)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(globalSession);
    }

    private List<GlobalSession> readByTimeoutDeadline(SessionCondition sessionCondition) {
        SessionScanStatsAccumulator scanStats = new SessionScanStatsAccumulator();
        ScanBudget scanBudget = new ScanBudget(sessionCondition.getScanLimit());
        Set<String> seenXids = new LinkedHashSet<>();
        List<GlobalSession> result = new ArrayList<>();
        Integer limit = sessionCondition.getLimit();
        byte[] cursor = sessionCondition.getTimeoutScanCursor();
        do {
            if (scanBudget.isExhausted()) {
                break;
            }
            RocksDBIndexManager.TimeoutScanResult scanResult = indexManager.scanXidsByTimeoutDeadline(
                    sessionCondition.getMaxTimeoutDeadlineMillis(),
                    cursor,
                    scanBudget.clamp(nextPageLimit(limit, result)));
            scanBudget.record(scanResult.getRowsScanned());
            scanStats.record(scanResult);
            for (RocksDBIndexManager.TimeoutIndexEntry entry : scanResult.getEntries()) {
                if (appendMatchingTimeoutSession(sessionCondition, seenXids, result, entry, scanStats)
                        && isLimitReached(limit, result)) {
                    break;
                }
            }
            cursor = scanResult.getNextCursor();
        } while (cursor != null && !isLimitReached(limit, result) && !scanBudget.isExhausted());
        sessionCondition.setNextTimeoutScanCursor(cursor);
        sessionCondition.setScanStats(scanStats.toStats(result.size()));
        return result;
    }

    private List<GlobalSession> readByStatuses(SessionCondition sessionCondition) {
        SessionScanStatsAccumulator scanStats = new SessionScanStatsAccumulator();
        ScanBudget scanBudget = new ScanBudget(sessionCondition.getScanLimit());
        Set<String> seenXids = new LinkedHashSet<>();
        List<GlobalSession> result = new ArrayList<>();
        Long overTimeAliveMills = sessionCondition.getOverTimeAliveMills();
        Integer limit = sessionCondition.getLimit();
        Long maxBeginTime = overTimeAliveMills != null && overTimeAliveMills > 0
                ? System.currentTimeMillis() - overTimeAliveMills
                : null;
        if (!shouldUseBoundedStatusScan(sessionCondition, maxBeginTime, limit, scanBudget)) {
            for (GlobalStatus status : sessionCondition.getStatuses()) {
                indexManager.scanXidsByStatus(
                        status,
                        xid -> appendMatchingSession(sessionCondition, seenXids, result, xid, status, null, scanStats));
            }
            if (sessionCondition.getStatuses().length > 1) {
                sessionCondition.setNextStatusScanCursors(
                        Collections.emptyMap(), SessionCondition.ScanContinuation.EXHAUSTED);
            } else {
                sessionCondition.setNextStatusScanCursor(null);
            }
            sessionCondition.setScanStats(scanStats.toStats(result.size()));
            return result;
        }
        long effectiveMaxBeginTime = maxBeginTime == null ? Long.MAX_VALUE : maxBeginTime;
        // Deadline protection: when no explicit limit is set, apply fullScanDeadlineMillis
        long deadlineNanos = (limit == null || limit <= 0) && fullScanDeadlineMillis > 0
                ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(fullScanDeadlineMillis)
                : 0L;
        boolean deadlineReached = false;
        if (sessionCondition.getStatuses().length > 1) {
            MultiStatusScanResult merged = readByStatusesWithKWayMerge(
                    sessionCondition,
                    maxBeginTime,
                    limit,
                    scanBudget,
                    deadlineNanos,
                    scanStats,
                    (limit == null || limit <= 0) && !scanBudget.isBounded());
            if (merged.isDeadlineReached()) {
                logStatusScanDeadline(sessionCondition, merged.getSessions().size());
            }
            sessionCondition.setNextStatusScanCursors(
                    merged.getNextStatusScanCursors(),
                    merged.isFullyExhausted()
                            ? SessionCondition.ScanContinuation.EXHAUSTED
                            : SessionCondition.ScanContinuation.RESUMABLE);
            sessionCondition.setScanStats(scanStats.toStats(merged.getSessions().size()));
            return merged.getSessions();
        }
        boolean scannedPage = false;
        for (GlobalStatus status : sessionCondition.getStatuses()) {
            byte[] cursor = sessionCondition.getStatusScanCursor();
            do {
                if (scanBudget.isExhausted()) {
                    break;
                }
                if (scannedPage && deadlineNanos > 0 && System.nanoTime() >= deadlineNanos) {
                    deadlineReached = true;
                    break;
                }
                RocksDBIndexManager.StatusScanResult scanResult = indexManager.scanXidsByStatus(
                        status, 0L, effectiveMaxBeginTime, cursor, scanBudget.clamp(nextPageLimit(limit, result)));
                scannedPage = true;
                scanBudget.record(scanResult.getRowsScanned());
                scanStats.record(scanResult);
                appendMatchingSessionsBatch(sessionCondition, seenXids, result, scanResult.getEntries(), scanStats);
                cursor = scanResult.getNextCursor();
            } while (cursor != null && !isLimitReached(limit, result) && !scanBudget.isExhausted());
            sessionCondition.setNextStatusScanCursor(cursor);
        }
        if (deadlineReached) {
            logStatusScanDeadline(sessionCondition, result.size());
        }
        sessionCondition.setScanStats(scanStats.toStats(result.size()));
        return result;
    }

    private MultiStatusScanResult readByStatusesWithKWayMerge(
            SessionCondition sessionCondition,
            Long maxBeginTime,
            Integer limit,
            ScanBudget scanBudget,
            long deadlineNanos,
            SessionScanStatsAccumulator scanStats) {
        return readByStatusesWithKWayMerge(
                sessionCondition, maxBeginTime, limit, scanBudget, deadlineNanos, scanStats, false);
    }

    private MultiStatusScanResult readByStatusesWithKWayMerge(
            SessionCondition sessionCondition,
            Long maxBeginTime,
            Integer limit,
            ScanBudget scanBudget,
            long deadlineNanos,
            SessionScanStatsAccumulator scanStats,
            boolean requireProgressAfterDeadline) {
        Set<String> seenXids = new LinkedHashSet<>();
        List<GlobalSession> result = new ArrayList<>();
        PriorityQueue<StatusScanCursor> queue =
                new PriorityQueue<>(Comparator.comparingLong(StatusScanCursor::beginTime)
                        .thenComparingInt(StatusScanCursor::statusCode)
                        .thenComparing(StatusScanCursor::xid));
        List<StatusScanCursor> cursors = new ArrayList<>();
        Map<GlobalStatus, byte[]> initialCursors = sessionCondition.getStatusScanCursors();
        for (GlobalStatus status : sessionCondition.getStatuses()) {
            if (!requireProgressAfterDeadline && isDeadlineReached(deadlineNanos)) {
                return multiStatusScanResult(result, cursors, initialCursors, true, false);
            }
            StatusScanCursor cursor = new StatusScanCursor(
                    status,
                    maxBeginTime,
                    initialCursors.get(status),
                    limit,
                    multiStatusScanPageSize,
                    scanBudget,
                    scanStats);
            cursors.add(cursor);
            if (cursor.hasCurrent()) {
                queue.offer(cursor);
            }
        }
        boolean madeProgress = false;
        boolean deadlineReachedWhileMakingProgress = false;
        while (!queue.isEmpty() && !isLimitReached(limit, result)) {
            if (isDeadlineReached(deadlineNanos)) {
                deadlineReachedWhileMakingProgress = true;
                if (!requireProgressAfterDeadline || madeProgress) {
                    return multiStatusScanResult(result, cursors, initialCursors, true, false);
                }
            }
            StatusScanCursor cursor = queue.poll();
            RocksDBIndexManager.StatusIndexEntry entry = cursor.current();
            appendMatchingSession(
                    sessionCondition,
                    seenXids,
                    result,
                    entry.getXid(),
                    entry.getStatus(),
                    entry.getBeginTime(),
                    scanStats);
            boolean limitReached = isLimitReached(limit, result);
            cursor.advance(limit, result, scanStats, !limitReached);
            madeProgress = true;
            if (requireProgressAfterDeadline && isDeadlineReached(deadlineNanos)) {
                deadlineReachedWhileMakingProgress = true;
            }
            if (limitReached) {
                break;
            }
            if (cursor.hasCurrent()) {
                queue.offer(cursor);
            }
        }
        return multiStatusScanResult(result, cursors, initialCursors, deadlineReachedWhileMakingProgress, true);
    }

    private MultiStatusScanResult multiStatusScanResult(
            List<GlobalSession> sessions,
            List<StatusScanCursor> cursors,
            Map<GlobalStatus, byte[]> initialCursors,
            boolean deadlineReached,
            boolean passMayBeComplete) {
        boolean fullyExhausted = passMayBeComplete
                && !cursors.isEmpty()
                && cursors.stream().allMatch(StatusScanCursor::isFullyExhausted);
        if (fullyExhausted) {
            return new MultiStatusScanResult(sessions, Collections.emptyMap(), deadlineReached, true);
        }
        Map<GlobalStatus, byte[]> nextCursors = new EnumMap<>(GlobalStatus.class);
        nextCursors.putAll(initialCursors);
        for (StatusScanCursor cursor : cursors) {
            byte[] resumeCursor = cursor.resumeCursor();
            if (resumeCursor != null) {
                nextCursors.put(cursor.status(), resumeCursor);
            }
        }
        return new MultiStatusScanResult(sessions, nextCursors, deadlineReached, fullyExhausted);
    }

    private boolean isDeadlineReached(long deadlineNanos) {
        return deadlineNanos > 0 && System.nanoTime() >= deadlineNanos;
    }

    private void logStatusScanDeadline(SessionCondition sessionCondition, int returned) {
        LOGGER.warn(
                "readByStatuses deadline reached: statuses={}, returned={}, deadlineMillis={}",
                sessionCondition.getStatuses(),
                returned,
                fullScanDeadlineMillis);
    }

    private boolean appendMatchingSession(
            SessionCondition sessionCondition,
            Set<String> seenXids,
            List<GlobalSession> result,
            String xid,
            GlobalStatus indexStatus,
            Long indexBeginTime,
            SessionScanStatsAccumulator scanStats) {
        if (seenXids.contains(xid)) {
            return false;
        }
        scanStats.recordPointRead();
        GlobalSession globalSession = readSession(xid, !sessionCondition.isLazyLoadBranch());
        if (globalSession != null
                && globalSession.getStatus() == indexStatus
                && (indexBeginTime == null || globalSession.getBeginTime() == indexBeginTime)
                && matches(globalSession, sessionCondition)) {
            seenXids.add(xid);
            result.add(globalSession);
            return true;
        }
        return false;
    }

    /**
     * Batch append matching sessions using multiGet for efficient bulk reads.
     * Returns the number of sessions added to result.
     */
    private int appendMatchingSessionsBatch(
            SessionCondition sessionCondition,
            Set<String> seenXids,
            List<GlobalSession> result,
            List<RocksDBIndexManager.StatusIndexEntry> entries,
            SessionScanStatsAccumulator scanStats) {
        if (entries.isEmpty()) {
            return 0;
        }
        // Collect unique xids that we haven't seen yet
        Set<String> newXids = new LinkedHashSet<>();
        List<RocksDBIndexManager.StatusIndexEntry> newEntries = new ArrayList<>();
        for (RocksDBIndexManager.StatusIndexEntry entry : entries) {
            if (!seenXids.contains(entry.getXid()) && newXids.add(entry.getXid())) {
                newEntries.add(entry);
            }
        }
        if (newXids.isEmpty()) {
            return 0;
        }
        // Batch read all sessions using multiGet
        List<byte[]> keys = new ArrayList<>(newXids.size());
        for (String xid : newXids) {
            keys.add(RocksDBKeyCodec.encodeXid(xid));
        }
        scanStats.recordPointReads(newXids.size());
        List<byte[]> values = storeEngine.multiGet(RocksDBColumnFamily.GLOBAL_SESSION, keys);
        // Process results
        int added = 0;
        boolean withBranches = !sessionCondition.isLazyLoadBranch();
        for (int i = 0; i < newEntries.size(); i++) {
            RocksDBIndexManager.StatusIndexEntry entry = newEntries.get(i);
            byte[] value = values.get(i);
            if (value == null) {
                continue;
            }
            // Use lightweight decode for verification (skips applicationId,
            // serviceGroup, transactionName, applicationData allocations)
            GlobalSession lightweightSession =
                    decodeGlobalSessionLightweight(value, sessionCondition.isLazyLoadBranch());
            if (lightweightSession.getStatus() == entry.getStatus()
                    && lightweightSession.getBeginTime() == entry.getBeginTime()
                    && matches(lightweightSession, sessionCondition)) {
                GlobalSession globalSession = decodeGlobalSession(value, sessionCondition.isLazyLoadBranch());
                if (seenXids.add(entry.getXid())) {
                    if (withBranches) {
                        readBranchSessions(globalSession.getXid()).forEach(globalSession::add);
                    }
                    result.add(globalSession);
                    added++;
                }
            }
        }
        return added;
    }

    private boolean appendMatchingTimeoutSession(
            SessionCondition sessionCondition,
            Set<String> seenXids,
            List<GlobalSession> result,
            RocksDBIndexManager.TimeoutIndexEntry entry,
            SessionScanStatsAccumulator scanStats) {
        if (seenXids.contains(entry.getXid())) {
            return false;
        }
        scanStats.recordPointRead();
        GlobalSession globalSession = readSession(entry.getXid(), !sessionCondition.isLazyLoadBranch());
        if (globalSession != null
                && globalSession.getStatus() == GlobalStatus.Begin
                && RocksDBIndexManager.timeoutDeadlineMillis(globalSession) == entry.getDeadlineMillis()
                && matches(globalSession, sessionCondition)) {
            seenXids.add(entry.getXid());
            result.add(globalSession);
            return true;
        }
        return false;
    }

    private boolean shouldUseBoundedStatusScan(
            SessionCondition sessionCondition, Long maxBeginTime, Integer limit, ScanBudget scanBudget) {
        return maxBeginTime != null
                || limit != null && limit > 0
                || scanBudget.isBounded()
                || fullScanDeadlineMillis > 0
                || sessionCondition.getStatusScanCursor() != null;
    }

    private boolean shouldUseTimeoutDeadlineScan(SessionCondition sessionCondition) {
        if (sessionCondition.getMaxTimeoutDeadlineMillis() == null
                || CollectionUtils.isEmpty(sessionCondition.getStatuses())
                || sessionCondition.getStatuses().length != 1) {
            return false;
        }
        return sessionCondition.getStatuses()[0] == GlobalStatus.Begin;
    }

    private int nextPageLimit(Integer limit, List<GlobalSession> result) {
        if (limit == null || limit <= 0) {
            return STATUS_SCAN_PAGE_SIZE;
        }
        return Math.max(1, Math.min(STATUS_SCAN_PAGE_SIZE, limit - result.size()));
    }

    private boolean isLimitReached(Integer limit, List<GlobalSession> result) {
        return limit != null && limit > 0 && result.size() >= limit;
    }

    private static class ScanBudget {
        private final int limit;
        private int examined;

        ScanBudget(Integer limit) {
            this.limit = limit == null || limit <= 0 ? 0 : limit;
        }

        boolean isBounded() {
            return limit > 0;
        }

        boolean isExhausted() {
            return isBounded() && examined >= limit;
        }

        int clamp(int requested) {
            return isBounded() ? Math.min(requested, limit - examined) : requested;
        }

        void record(int rowsScanned) {
            examined += rowsScanned;
        }
    }

    private class StatusScanCursor {
        private final GlobalStatus status;
        private final Long maxBeginTime;
        private final ScanBudget scanBudget;
        private final int pageSize;
        private List<RocksDBIndexManager.StatusIndexEntry> entries = Collections.emptyList();
        private byte[] nextCursor;
        private byte[] resumeCursor;
        private int index;
        private boolean exhausted;

        StatusScanCursor(
                GlobalStatus status,
                Long maxBeginTime,
                byte[] initialCursor,
                Integer limit,
                int pageSize,
                ScanBudget scanBudget,
                SessionScanStatsAccumulator scanStats) {
            this.status = status;
            this.maxBeginTime = maxBeginTime;
            this.pageSize = pageSize;
            this.scanBudget = scanBudget;
            this.nextCursor = initialCursor;
            this.resumeCursor = initialCursor;
            loadNextPage(limit, Collections.emptyList(), scanStats);
        }

        boolean hasCurrent() {
            return current() != null;
        }

        RocksDBIndexManager.StatusIndexEntry current() {
            return index < entries.size() ? entries.get(index) : null;
        }

        long beginTime() {
            return current().getBeginTime();
        }

        int statusCode() {
            return status.getCode();
        }

        String xid() {
            return current().getXid();
        }

        GlobalStatus status() {
            return status;
        }

        byte[] resumeCursor() {
            RocksDBIndexManager.StatusIndexEntry current = current();
            return current == null
                    ? (nextCursor == null ? resumeCursor : nextCursor)
                    : RocksDBKeyCodec.encodeGlobalStatusIndex(status, current.getBeginTime(), current.getXid());
        }

        boolean isFullyExhausted() {
            return exhausted && current() == null;
        }

        void advance(
                Integer limit,
                List<GlobalSession> result,
                SessionScanStatsAccumulator scanStats,
                boolean loadNextPage) {
            RocksDBIndexManager.StatusIndexEntry current = current();
            if (current != null) {
                resumeCursor = nextSeekKey(
                        RocksDBKeyCodec.encodeGlobalStatusIndex(status, current.getBeginTime(), current.getXid()));
            }
            index++;
            if (loadNextPage && index >= entries.size()) {
                loadNextPage(limit, result, scanStats);
            }
        }

        private byte[] nextSeekKey(byte[] key) {
            byte[] next = Arrays.copyOf(key, key.length + 1);
            next[key.length] = 0;
            return next;
        }

        private void loadNextPage(Integer limit, List<GlobalSession> result, SessionScanStatsAccumulator scanStats) {
            if (exhausted || scanBudget.isExhausted()) {
                entries = Collections.emptyList();
                index = 0;
                return;
            }
            int requested = scanBudget.isBounded() ? 1 : Math.min(pageSize, nextPageLimit(limit, result));
            RocksDBIndexManager.StatusScanResult scanResult = indexManager.scanXidsByStatus(
                    status,
                    0L,
                    maxBeginTime == null ? Long.MAX_VALUE : maxBeginTime,
                    nextCursor,
                    scanBudget.clamp(requested));
            scanBudget.record(scanResult.getRowsScanned());
            scanStats.record(scanResult);
            entries = scanResult.getEntries();
            index = 0;
            nextCursor = scanResult.getNextCursor();
            exhausted = nextCursor == null;
        }
    }

    private static class MultiStatusScanResult {
        private final List<GlobalSession> sessions;
        private final Map<GlobalStatus, byte[]> nextStatusScanCursors;
        private final boolean deadlineReached;
        private final boolean fullyExhausted;

        private MultiStatusScanResult(
                List<GlobalSession> sessions,
                Map<GlobalStatus, byte[]> nextStatusScanCursors,
                boolean deadlineReached,
                boolean fullyExhausted) {
            this.sessions = sessions;
            this.nextStatusScanCursors = nextStatusScanCursors;
            this.deadlineReached = deadlineReached;
            this.fullyExhausted = fullyExhausted;
        }

        private List<GlobalSession> getSessions() {
            return sessions;
        }

        private Map<GlobalStatus, byte[]> getNextStatusScanCursors() {
            return nextStatusScanCursors;
        }

        private boolean isDeadlineReached() {
            return deadlineReached;
        }

        private boolean isFullyExhausted() {
            return fullyExhausted;
        }
    }

    public static final class RecoveryCursor {
        private final GlobalStatus[] statuses;
        private final Map<GlobalStatus, byte[]> statusScanCursors;

        private RecoveryCursor(GlobalStatus[] statuses, Map<GlobalStatus, byte[]> statusScanCursors) {
            this.statuses = copyAndValidateRecoveryStatuses(statuses);
            this.statusScanCursors = copyAndValidateRecoveryCursors(this.statuses, statusScanCursors);
        }
    }

    public static final class RecoveryScanPage {
        private final List<GlobalSession> sessions;
        private final RecoveryCursor continuation;
        private final boolean exhausted;
        private final boolean deadlineReached;

        private RecoveryScanPage(
                List<GlobalSession> sessions, RecoveryCursor continuation, boolean exhausted, boolean deadlineReached) {
            this.sessions = sessions;
            this.continuation = continuation;
            this.exhausted = exhausted;
            this.deadlineReached = deadlineReached;
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

        public boolean isDeadlineReached() {
            return deadlineReached;
        }
    }

    private static class SessionScanStatsAccumulator {
        private final long startedAtNanos = System.nanoTime();
        private long rowsScanned;
        private long rowsReturned;
        private long pointReads;
        private boolean limitReached;

        private void record(RocksDBIndexManager.StatusScanResult scanResult) {
            rowsScanned += scanResult.getRowsScanned();
            rowsReturned += scanResult.getRowsReturned();
            limitReached |= scanResult.isLimitReached();
        }

        private void record(RocksDBIndexManager.TimeoutScanResult scanResult) {
            rowsScanned += scanResult.getRowsScanned();
            rowsReturned += scanResult.getRowsReturned();
            limitReached |= scanResult.isLimitReached();
        }

        private void recordPointRead() {
            pointReads++;
        }

        private void recordPointReads(int count) {
            pointReads += count;
        }

        private SessionScanStats toStats(int sessionsReturned) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
            return new SessionScanStats(
                    rowsScanned, rowsReturned, pointReads, sessionsReturned, elapsedMillis, limitReached);
        }
    }

    private boolean matches(GlobalSession globalSession, SessionCondition sessionCondition) {
        if (sessionCondition.getOverTimeAliveMills() != null
                && sessionCondition.getOverTimeAliveMills() > 0
                && System.currentTimeMillis() - globalSession.getBeginTime()
                        <= sessionCondition.getOverTimeAliveMills()) {
            return false;
        }
        if (sessionCondition.getTransactionId() != null
                && sessionCondition.getTransactionId() > 0
                && !sessionCondition.getTransactionId().equals(globalSession.getTransactionId())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            for (GlobalStatus status : sessionCondition.getStatuses()) {
                if (status == globalSession.getStatus()) {
                    return true;
                }
            }
            return false;
        }
        if (sessionCondition.getMaxTimeoutDeadlineMillis() != null
                && RocksDBIndexManager.timeoutDeadlineMillis(globalSession)
                        > sessionCondition.getMaxTimeoutDeadlineMillis()) {
            return false;
        }
        if (sessionCondition.getStatus() != null) {
            return sessionCondition.getStatus() == globalSession.getStatus();
        }
        return true;
    }

    private byte[] encodeGlobalSession(GlobalSession session) {
        return RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.GLOBAL_SESSION, session.encode());
    }

    private byte[] encodeBranchSession(BranchSession session) {
        return RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.BRANCH_SESSION, session.encode());
    }

    private GlobalSession decodeGlobalSession(byte[] value, boolean lazyLoadBranch) {
        RocksDBValueCodec.DecodedValue decodedValue = RocksDBValueCodec.decode(value);
        if (decodedValue.getType() != RocksDBValueCodec.ValueType.GLOBAL_SESSION) {
            throw new StoreException("unexpected RocksDB value type for global session:" + decodedValue.getType());
        }
        GlobalSession globalSession = new GlobalSession(null, null, null, 0, lazyLoadBranch);
        globalSession.decode(decodedValue.getPayload());
        return globalSession;
    }

    /**
     * Lightweight decode that only extracts fields needed for status-query verification:
     * transactionId, timeout, xid, beginTime, status.
     * <p>
     * Skips String allocation for applicationId, transactionServiceGroup,
     * transactionName, and applicationData — these fields are not accessed
     * during index verification or {@link #matches} filtering.
     */
    private GlobalSession decodeGlobalSessionLightweight(byte[] value, boolean lazyLoadBranch) {
        RocksDBValueCodec.DecodedValue decodedValue = RocksDBValueCodec.decode(value);
        if (decodedValue.getType() != RocksDBValueCodec.ValueType.GLOBAL_SESSION) {
            throw new StoreException("unexpected RocksDB value type for global session:" + decodedValue.getType());
        }
        GlobalSession globalSession = new GlobalSession(null, null, null, 0, lazyLoadBranch);
        globalSession.decodeLightweight(decodedValue.getPayload());
        return globalSession;
    }

    private BranchSession decodeBranchSession(byte[] value) {
        RocksDBValueCodec.DecodedValue decodedValue = RocksDBValueCodec.decode(value);
        if (decodedValue.getType() != RocksDBValueCodec.ValueType.BRANCH_SESSION) {
            throw new StoreException("unexpected RocksDB value type for branch session:" + decodedValue.getType());
        }
        BranchSession branchSession = new BranchSession();
        branchSession.decode(decodedValue.getPayload());
        return branchSession;
    }

    private String getXid(SessionStorable session) {
        if (session instanceof GlobalSession) {
            return ((GlobalSession) session).getXid();
        }
        if (session instanceof BranchSession) {
            return ((BranchSession) session).getXid();
        }
        throw new StoreException(
                "unsupported session type:" + session.getClass().getName());
    }
}
