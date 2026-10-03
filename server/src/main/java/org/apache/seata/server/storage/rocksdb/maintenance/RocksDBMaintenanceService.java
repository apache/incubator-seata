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

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine.SnapshotReadView;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.apache.seata.server.storage.rocksdb.index.RocksDBIndexManager;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Maintenance service for RocksDB file store engine.
 *
 * <p>Provides checkpoint creation, consistency verification and explicit index repair.
 * All operations are intended to be triggered explicitly (e.g., via admin API or benchmark);
 * none of them run automatically on the startup path.
 */
public class RocksDBMaintenanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBMaintenanceService.class);
    private static final String CHECKPOINT_METADATA_FILE = "seata-checkpoint-metadata.txt";
    private static final byte[] CLEAN_SHUTDOWN_KEY = "clean_shutdown".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CLEAN_SHUTDOWN_TRUE = "true".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CLEAN_SHUTDOWN_FALSE = "false".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ORPHAN_LOCK_CLEAN_CURSOR_KEY =
            "orphan_lock_clean_cursor".getBytes(StandardCharsets.UTF_8);
    static final byte[] LOCK_INDEX_REPAIR_PROGRESS_KEY =
            "rocksdb.repair.lock_branch_index.v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EMPTY_PREFIX = new byte[0];
    private static final RocksDBColumnFamily[] VERIFY_COLUMN_FAMILIES = {
        RocksDBColumnFamily.GLOBAL_SESSION,
        RocksDBColumnFamily.BRANCH_SESSION,
        RocksDBColumnFamily.LOCK,
        RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
        RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
        RocksDBColumnFamily.TRANSACTION_ID_INDEX,
        RocksDBColumnFamily.LOCK_BRANCH_INDEX
    };

    private final RocksDBStoreEngine storeEngine;
    private final RocksDBIndexManager indexManager;

    public RocksDBMaintenanceService() {
        this(RocksDBStoreEngineFactory.getInstance());
    }

    public RocksDBMaintenanceService(RocksDBStoreEngine storeEngine) {
        this.storeEngine = storeEngine;
        this.indexManager = new RocksDBIndexManager(storeEngine);
    }

    /**
     * Create a RocksDB checkpoint at the given directory.
     *
     * <p>The target directory must not exist or must be empty. A metadata file is written
     * into the checkpoint directory after the checkpoint is created successfully.
     *
     * @param checkpointPath target directory for the checkpoint
     * @param flush          whether to flush memtables before creating the checkpoint
     */
    public void createCheckpoint(Path checkpointPath, boolean flush) {
        if (checkpointPath == null) {
            throw new StoreException("checkpoint path must not be null");
        }
        if (Files.exists(checkpointPath) && !isDirectoryEmpty(checkpointPath)) {
            throw new StoreException("checkpoint target directory already exists and is not empty:" + checkpointPath);
        }
        // Ensure parent directory exists; RocksDB createCheckpoint creates the target directory itself
        Path parent = checkpointPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new StoreException(e, "create checkpoint parent directory failed:" + parent);
            }
        }
        if (flush) {
            storeEngine.flush();
        }
        storeEngine.createCheckpoint(checkpointPath.toString());
        try {
            writeCheckpointMetadata(checkpointPath);
            LOGGER.info(
                    "RocksDB checkpoint created, path:{}, flush:{}, source:{}",
                    checkpointPath,
                    flush,
                    storeEngine.getConfig().getDbPath());
        } catch (IOException e) {
            throw new StoreException(e, "write checkpoint metadata failed, path:" + checkpointPath);
        }
    }

    /**
     * Run an explicit full consistency verification.
     */
    public RocksDBVerifyReport verifyCurrentState() {
        return verifyCurrentState(RocksDBVerifyOptions.full());
    }

    /**
     * Verify the current state without retaining full database-sized reference sets.
     */
    public RocksDBVerifyReport verifyCurrentState(RocksDBVerifyOptions options) {
        return verifyCurrentState(options, 0);
    }

    private RocksDBVerifyReport verifyCurrentState(RocksDBVerifyOptions options, int maxRepairEntries) {
        return storeEngine.withSnapshot(view -> {
            RocksDBVerifyReport.Builder report = RocksDBVerifyReport.builder(options);
            verifyMetadata(view, report);
            long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(options.getDeadlineMillis());
            switch (options.getMode()) {
                case SAMPLE:
                    verifySample(view, deadlineNanos, options, report);
                    break;
                case PAGE:
                    verifyPage(view, deadlineNanos, options, report);
                    break;
                case FULL:
                default:
                    verifyFull(view, deadlineNanos, report);
                    break;
            }
            if (maxRepairEntries > 0
                    && report.isComplete()
                    && report.requiresGlobalSecondaryIndexRebuild()
                    && !report.hasUnrepairableSourceViolation()) {
                verifyRepairSourceUniqueness(view, deadlineNanos, maxRepairEntries, report);
            }
            RocksDBVerifyReport result = report.build();
            LOGGER.info("RocksDB verify completed, {}", result);
            return result;
        });
    }

    private void verifyRepairSourceUniqueness(
            SnapshotReadView view, long deadlineNanos, int maxRepairEntries, RocksDBVerifyReport.Builder report) {
        Set<Long> transactionIds = new HashSet<>();
        long[] rows = new long[1];
        boolean[] limitExceeded = new boolean[1];
        RocksDBStoreEngine.ScanStats stats = view.scanByPrefix(
                RocksDBColumnFamily.GLOBAL_SESSION,
                EMPTY_PREFIX,
                EMPTY_PREFIX,
                0,
                deadlineNanos,
                (key, value) -> {
                    if (++rows[0] > maxRepairEntries) {
                        limitExceeded[0] = true;
                        return false;
                    }
                    return true;
                },
                (key, value) -> {
                    GlobalVerifyEntry global = decodeGlobal(value, null, "global session");
                    if (global != null && !transactionIds.add(global.transactionId)) {
                        report.invalidGlobal(
                                "duplicate global transaction id:" + global.transactionId + ", xid:" + global.xid);
                    }
                });
        if (stats.isDeadlineReached()) {
            report.truncated();
            report.complete(false);
            return;
        }
        if (limitExceeded[0]) {
            throw new StoreException("RocksDB repair exceeds maxRepairEntries:" + maxRepairEntries);
        }
    }

    private void verifyMetadata(SnapshotReadView view, RocksDBVerifyReport.Builder report) {
        byte[] versionBytes = view.get(RocksDBColumnFamily.METADATA, "format_version".getBytes(StandardCharsets.UTF_8));
        String expected = Integer.toString(RocksDBStoreEngine.FORMAT_VERSION);
        if (versionBytes == null) {
            report.error("missing format_version in metadata");
        } else if (!expected.equals(new String(versionBytes, StandardCharsets.UTF_8))) {
            report.error("format_version mismatch, expected:" + expected + ", found:"
                    + new String(versionBytes, StandardCharsets.UTF_8));
        }
        byte[] cleanShutdown = view.get(RocksDBColumnFamily.METADATA, CLEAN_SHUTDOWN_KEY);
        if (!Arrays.equals(cleanShutdown, CLEAN_SHUTDOWN_TRUE) && !Arrays.equals(cleanShutdown, CLEAN_SHUTDOWN_FALSE)) {
            report.invalidMetadata("invalid clean_shutdown metadata");
        }
        byte[] orphanCleanupCursor = view.get(RocksDBColumnFamily.METADATA, ORPHAN_LOCK_CLEAN_CURSOR_KEY);
        if (orphanCleanupCursor != null && !RocksDBKeyCodec.isValidLockBranchIndexSeekKey(orphanCleanupCursor)) {
            report.invalidMetadata("invalid orphan lock cleanup cursor metadata");
        }
    }

    private void verifyFull(SnapshotReadView view, long deadlineNanos, RocksDBVerifyReport.Builder report) {
        for (RocksDBColumnFamily columnFamily : VERIFY_COLUMN_FAMILIES) {
            VerifyScanResult result = scanFamily(view, columnFamily, EMPTY_PREFIX, 0, deadlineNanos, report);
            if (result.stats.isDeadlineReached()) {
                truncate(report, columnFamily, result.lastKey, EMPTY_PREFIX);
                return;
            }
        }
        report.complete(true);
    }

    private void verifySample(
            SnapshotReadView view,
            long deadlineNanos,
            RocksDBVerifyOptions options,
            RocksDBVerifyReport.Builder report) {
        for (RocksDBColumnFamily columnFamily : VERIFY_COLUMN_FAMILIES) {
            VerifyScanResult result =
                    scanFamily(view, columnFamily, EMPTY_PREFIX, options.getLimit(), deadlineNanos, report);
            if (result.stats.isDeadlineReached()) {
                truncate(report, columnFamily, result.lastKey, EMPTY_PREFIX);
                return;
            }
        }
        report.complete(false);
    }

    private void verifyPage(
            SnapshotReadView view,
            long deadlineNanos,
            RocksDBVerifyOptions options,
            RocksDBVerifyReport.Builder report) {
        RocksDBVerifyCursor cursor = options.getCursor();
        int familyIndex = cursor == null ? 0 : familyIndex(cursor.getColumnFamily());
        int remaining = options.getLimit();
        for (int i = familyIndex; i < VERIFY_COLUMN_FAMILIES.length; i++) {
            RocksDBColumnFamily columnFamily = VERIFY_COLUMN_FAMILIES[i];
            byte[] seekKey =
                    cursor != null && cursor.getColumnFamily() == columnFamily ? cursor.getSeekKey() : EMPTY_PREFIX;
            VerifyScanResult result = scanFamily(view, columnFamily, seekKey, remaining, deadlineNanos, report);
            remaining -= result.stats.getRowsReturned();
            if (result.stats.isDeadlineReached()) {
                truncate(report, columnFamily, result.lastKey, seekKey);
                return;
            }
            if (remaining == 0 && result.lastKey != null) {
                report.truncated();
                report.nextCursor(new RocksDBVerifyCursor(columnFamily, nextSeekKey(result.lastKey)));
                report.complete(false);
                return;
            }
            cursor = null;
        }
        report.complete(true);
    }

    private VerifyScanResult scanFamily(
            SnapshotReadView view,
            RocksDBColumnFamily columnFamily,
            byte[] seekKey,
            int limit,
            long deadlineNanos,
            RocksDBVerifyReport.Builder report) {
        byte[][] lastKey = new byte[1][];
        RocksDBStoreEngine.ScanStats stats =
                view.scanByPrefix(columnFamily, seekKey, EMPTY_PREFIX, limit, deadlineNanos, null, (key, value) -> {
                    lastKey[0] = key;
                    verifyEntry(view, columnFamily, key, value, report);
                });
        report.scannedRecords(stats.getRowsScanned());
        return new VerifyScanResult(stats, lastKey[0]);
    }

    private static void truncate(
            RocksDBVerifyReport.Builder report, RocksDBColumnFamily columnFamily, byte[] lastKey, byte[] seekKey) {
        report.truncated();
        report.nextCursor(new RocksDBVerifyCursor(columnFamily, lastKey == null ? seekKey : nextSeekKey(lastKey)));
        report.complete(false);
    }

    private void verifyEntry(
            SnapshotReadView view,
            RocksDBColumnFamily columnFamily,
            byte[] key,
            byte[] value,
            RocksDBVerifyReport.Builder report) {
        report.checkedRecord(isIndex(columnFamily));
        switch (columnFamily) {
            case GLOBAL_SESSION:
                verifyGlobal(view, key, value, report);
                break;
            case BRANCH_SESSION:
                verifyBranch(view, key, value, report);
                break;
            case LOCK:
                verifyLock(view, key, value, report);
                break;
            case GLOBAL_STATUS_INDEX:
                verifyStatusIndex(view, key, value, report);
                break;
            case GLOBAL_TIMEOUT_INDEX:
                verifyTimeoutIndex(view, key, value, report);
                break;
            case TRANSACTION_ID_INDEX:
                verifyTransactionIdIndex(view, key, value, report);
                break;
            case LOCK_BRANCH_INDEX:
                verifyLockIndex(view, key, value, report);
                break;
            default:
                break;
        }
    }

    private void verifyGlobal(SnapshotReadView view, byte[] key, byte[] value, RocksDBVerifyReport.Builder report) {
        report.checkedGlobal();
        GlobalVerifyEntry global = decodeGlobal(value, report, "global session");
        if (global == null) {
            return;
        }
        if (!Arrays.equals(key, RocksDBKeyCodec.encodeXid(global.xid))) {
            report.invalidGlobal("global session key mismatch, xid:" + global.xid);
        }
        byte[] xidBytes = global.xid.getBytes(StandardCharsets.UTF_8);
        byte[] statusValue = view.get(
                RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                RocksDBKeyCodec.encodeGlobalStatusIndex(global.status, global.beginTime, global.xid));
        if (!Arrays.equals(xidBytes, statusValue)) {
            report.missingStatusIndex("missing global status index, xid:" + global.xid);
        }
        byte[] transactionValue = view.get(
                RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                RocksDBKeyCodec.encodeTransactionIdIndex(global.transactionId));
        if (transactionValue == null) {
            report.missingTransactionIdIndex("missing transaction id index, xid:" + global.xid);
        } else if (!Arrays.equals(xidBytes, transactionValue)) {
            String indexedXid = new String(transactionValue, StandardCharsets.UTF_8);
            GlobalVerifyEntry indexedGlobal = readGlobal(view, indexedXid);
            if (indexedGlobal != null && indexedGlobal.transactionId == global.transactionId) {
                report.invalidGlobal("duplicate global transaction id:" + global.transactionId + ", xid:" + global.xid);
            } else {
                report.missingTransactionIdIndex("missing valid transaction id index, xid:" + global.xid);
            }
        }
        if (global.status == GlobalStatus.Begin) {
            byte[] timeoutValue = view.get(
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(global.deadlineMillis(), global.xid));
            if (!Arrays.equals(xidBytes, timeoutValue)) {
                report.missingTimeoutIndex("missing global timeout index, xid:" + global.xid);
            }
        }
    }

    private void verifyBranch(SnapshotReadView view, byte[] key, byte[] value, RocksDBVerifyReport.Builder report) {
        report.checkedBranch();
        String xid = RocksDBKeyCodec.extractXidFromBranchKey(key);
        long branchId = RocksDBKeyCodec.extractBranchIdFromBranchKey(key);
        if (xid == null || branchId < 0) {
            report.invalidBranch("invalid branch session key");
            return;
        }
        if (view.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid)) == null) {
            report.orphanBranch("orphan branch session, xid:" + xid);
        }
        BranchVerifyEntry branch = decodeBranch(value, report);
        if (branch == null) {
            return;
        }
        if (!xid.equals(branch.xid) || branchId != branch.branchId) {
            report.invalidBranch("branch session payload does not match key, xid:" + xid + ", branchId:" + branchId);
        }
    }

    private void verifyLock(SnapshotReadView view, byte[] key, byte[] value, RocksDBVerifyReport.Builder report) {
        report.checkedLock();
        LockVerifyEntry lock = decodeLock(value);
        if (lock == null || view.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(lock.xid)) == null) {
            report.orphanLock("orphan or invalid lock holder");
            return;
        }
        if (view.get(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch(lock.xid, lock.branchId))
                == null) {
            report.orphanLock("lock holder references missing branch session, xid:" + lock.xid);
        }
        byte[] indexValue = view.get(
                RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                RocksDBKeyCodec.encodeLockBranchIndex(lock.xid, lock.branchId, key));
        if (!Arrays.equals(key, indexValue)) {
            report.orphanLock("lock holder has no matching branch index, xid:" + lock.xid);
        }
    }

    private void verifyStatusIndex(
            SnapshotReadView view, byte[] key, byte[] value, RocksDBVerifyReport.Builder report) {
        try {
            String xid = RocksDBKeyCodec.extractXidFromStatusIndexKey(key);
            GlobalVerifyEntry global = readGlobal(view, xid);
            byte[] expectedKey = global == null
                    ? null
                    : RocksDBKeyCodec.encodeGlobalStatusIndex(global.status, global.beginTime, global.xid);
            if (global == null
                    || !xid.equals(new String(value, StandardCharsets.UTF_8))
                    || !Arrays.equals(key, expectedKey)) {
                report.staleStatusIndex("stale global status index, xid:" + xid);
            }
        } catch (Exception e) {
            report.staleStatusIndex("invalid global status index, message:" + e.getMessage());
        }
    }

    private void verifyTimeoutIndex(
            SnapshotReadView view, byte[] key, byte[] value, RocksDBVerifyReport.Builder report) {
        try {
            String xid = new String(value, StandardCharsets.UTF_8);
            GlobalVerifyEntry global = readGlobal(view, xid);
            byte[] expectedKey = global == null
                    ? null
                    : RocksDBKeyCodec.encodeGlobalTimeoutIndex(global.deadlineMillis(), global.xid);
            if (global == null || global.status != GlobalStatus.Begin || !Arrays.equals(key, expectedKey)) {
                report.staleTimeoutIndex("stale global timeout index, xid:" + xid);
            }
        } catch (Exception e) {
            report.staleTimeoutIndex("invalid global timeout index, message:" + e.getMessage());
        }
    }

    private void verifyTransactionIdIndex(
            SnapshotReadView view, byte[] key, byte[] value, RocksDBVerifyReport.Builder report) {
        try {
            String xid = new String(value, StandardCharsets.UTF_8);
            GlobalVerifyEntry global = readGlobal(view, xid);
            byte[] expectedKey = global == null ? null : RocksDBKeyCodec.encodeTransactionIdIndex(global.transactionId);
            if (global == null || !Arrays.equals(key, expectedKey)) {
                report.staleTransactionIdIndex("stale transaction id index, xid:" + xid);
            }
        } catch (Exception e) {
            report.staleTransactionIdIndex("invalid transaction id index, message:" + e.getMessage());
        }
    }

    private void verifyLockIndex(
            SnapshotReadView view, byte[] key, byte[] lockKey, RocksDBVerifyReport.Builder report) {
        byte[] lockValue = view.get(RocksDBColumnFamily.LOCK, lockKey);
        LockVerifyEntry lock = decodeLock(lockValue);
        byte[] expectedKey =
                lock == null ? null : RocksDBKeyCodec.encodeLockBranchIndex(lock.xid, lock.branchId, lockKey);
        if (lock == null
                || view.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(lock.xid)) == null
                || view.get(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch(lock.xid, lock.branchId))
                        == null
                || !Arrays.equals(key, expectedKey)) {
            report.staleLockIndex("stale lock branch index");
        }
    }

    private GlobalVerifyEntry readGlobal(SnapshotReadView view, String xid) {
        if (xid == null) {
            return null;
        }
        byte[] value = view.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid));
        return value == null ? null : decodeGlobal(value, null, "global session");
    }

    private static GlobalVerifyEntry decodeGlobal(
            byte[] value, RocksDBVerifyReport.Builder report, String description) {
        try {
            RocksDBValueCodec.DecodedValue decoded = RocksDBValueCodec.decode(value);
            if (decoded.getType() != RocksDBValueCodec.ValueType.GLOBAL_SESSION) {
                throw new StoreException("unexpected value type:" + decoded.getType());
            }
            GlobalSession session = new GlobalSession(null, null, null, 0, true);
            session.decode(decoded.getPayload());
            return new GlobalVerifyEntry(
                    session.getXid(),
                    session.getTransactionId(),
                    session.getStatus(),
                    session.getBeginTime(),
                    session.getTimeout());
        } catch (Exception e) {
            if (report != null) {
                report.invalidGlobal("failed to decode " + description + ", message:" + e.getMessage());
            }
            return null;
        }
    }

    private static LockVerifyEntry decodeLock(byte[] lockValue) {
        if (lockValue == null) {
            return null;
        }
        try {
            RocksDBValueCodec.DecodedValue decoded = RocksDBValueCodec.decode(lockValue);
            if (decoded.getType() != RocksDBValueCodec.ValueType.LOCK_HOLDER) {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded.getPayload());
            String xid = readString(buffer);
            buffer.getLong();
            long branchId = buffer.getLong();
            return xid == null ? null : new LockVerifyEntry(xid, branchId);
        } catch (Exception e) {
            return null;
        }
    }

    private static BranchVerifyEntry decodeBranch(byte[] value, RocksDBVerifyReport.Builder report) {
        try {
            RocksDBValueCodec.DecodedValue decoded = RocksDBValueCodec.decode(value);
            if (decoded.getType() != RocksDBValueCodec.ValueType.BRANCH_SESSION) {
                throw new StoreException("unexpected value type:" + decoded.getType());
            }
            BranchSession session = new BranchSession();
            session.decode(decoded.getPayload());
            return new BranchVerifyEntry(session.getXid(), session.getBranchId());
        } catch (Exception e) {
            if (report != null) {
                report.invalidBranch("failed to decode branch session, message:" + e.getMessage());
            }
            return null;
        }
    }

    private static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0 || buffer.remaining() < length) {
            return null;
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isIndex(RocksDBColumnFamily columnFamily) {
        return columnFamily == RocksDBColumnFamily.GLOBAL_STATUS_INDEX
                || columnFamily == RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX
                || columnFamily == RocksDBColumnFamily.TRANSACTION_ID_INDEX
                || columnFamily == RocksDBColumnFamily.LOCK_BRANCH_INDEX;
    }

    private static int familyIndex(RocksDBColumnFamily columnFamily) {
        for (int i = 0; i < VERIFY_COLUMN_FAMILIES.length; i++) {
            if (VERIFY_COLUMN_FAMILIES[i] == columnFamily) {
                return i;
            }
        }
        throw new IllegalArgumentException("unsupported verify cursor column family:" + columnFamily);
    }

    private static byte[] nextSeekKey(byte[] key) {
        byte[] next = Arrays.copyOf(key, key.length + 1);
        next[key.length] = 0;
        return next;
    }

    /**
     * Rebuild secondary indexes from global session data.
     */
    public void repairIndexes() {
        storeEngine.withMaintenanceLock(() -> {
            LOGGER.info("Rebuilding RocksDB secondary indexes");
            indexManager.rebuildFromGlobalSessions();
            LOGGER.info("RocksDB secondary indexes rebuilt");
            return null;
        });
    }

    /**
     * Build a read-only repair proposal from the current verification state.
     */
    public RocksDBRepairPlan planRepair(RocksDBRepairOptions options) {
        Objects.requireNonNull(options, "repair options must not be null");
        RocksDBVerifyReport beforeVerifyReport = verifyCurrentState(
                RocksDBVerifyOptions.full(100, options.getVerifyDeadlineMillis()), options.getMaxRepairEntries());
        Set<RocksDBRepairPlan.Action> actions = EnumSet.noneOf(RocksDBRepairPlan.Action.class);
        if (requiresGlobalSecondaryIndexRebuild(beforeVerifyReport)) {
            actions.add(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES);
        }
        if (beforeVerifyReport.getStaleLockIndexCount() > 0) {
            actions.add(RocksDBRepairPlan.Action.DELETE_STALE_LOCK_BRANCH_INDEXES);
        }
        return new RocksDBRepairPlan(
                options.isDryRun(),
                actions,
                beforeVerifyReport,
                beforeVerifyReport.isComplete(),
                hasUnrepairableSourceViolation(beforeVerifyReport));
    }

    /**
     * Execute a previously reviewed repair proposal after explicit safety confirmation.
     */
    public RocksDBRepairReport executeRepair(RocksDBRepairPlan plan, RocksDBRepairOptions options) {
        Objects.requireNonNull(plan, "repair plan must not be null");
        Objects.requireNonNull(options, "repair options must not be null");
        if (options.isDryRun()) {
            return new RocksDBRepairReport(true, 0, plan.getBeforeVerifyReport(), plan.getBeforeVerifyReport());
        }
        if (!options.isConfirm() || !options.isMaintenanceMode()) {
            throw new StoreException("RocksDB repair requires confirm=true and maintenanceMode=true");
        }
        if (!plan.isVerificationComplete()) {
            throw new StoreException("RocksDB repair requires a complete verification plan");
        }
        return storeEngine.withMaintenanceRunLock(() -> executeRepairInRunLock(plan, options));
    }

    /**
     * Clear a stopped lock-index repair cursor after revalidating its source data.
     */
    public void resetStoppedLockIndexProgress(String runId, RocksDBRepairOptions options) {
        Objects.requireNonNull(options, "repair options must not be null");
        if (!options.isConfirm() || !options.isMaintenanceMode()) {
            throw new StoreException("lock index repair reset requires confirm=true and maintenanceMode=true");
        }
        if (runId == null || runId.isEmpty()) {
            throw new StoreException("lock index repair reset requires the stopped runId");
        }
        storeEngine.withMaintenanceRunLock(() -> {
            storeEngine.withMaintenanceLock(() -> {
                RocksDBLockIndexRepairProgress progress = readLockIndexProgress();
                if (progress == null
                        || progress.state != RocksDBLockIndexRepairProgress.State.STOPPED
                        || !runId.equals(progress.runId)) {
                    throw new StoreException("lock index repair reset requires a matching stopped run");
                }
                RocksDBVerifyReport report =
                        verifyCurrentState(RocksDBVerifyOptions.full(100, options.getVerifyDeadlineMillis()));
                if (!report.isComplete()) {
                    throw new StoreException("lock index repair reset requires a complete verification");
                }
                if (hasUnrepairableSourceViolation(report)) {
                    throw new StoreException("lock index repair reset rejected by unrepairable source violations");
                }
                storeEngine.delete(RocksDBColumnFamily.METADATA, LOCK_INDEX_REPAIR_PROGRESS_KEY);
                return null;
            });
            return null;
        });
    }

    private RocksDBRepairReport executeRepairInRunLock(RocksDBRepairPlan plan, RocksDBRepairOptions options) {
        boolean hasLockIndexRepairProgress = hasLockIndexRepairProgress();
        if (plan.hasAction(RocksDBRepairPlan.Action.DELETE_STALE_LOCK_BRANCH_INDEXES) || hasLockIndexRepairProgress) {
            if (plan.hasAction(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES)) {
                throw new StoreException("lock index repair must run separately from global secondary index rebuild");
            }
            return executeLockIndexRepair(options);
        }
        return storeEngine.withMaintenanceLock(() -> executeRepairInMaintenanceWindow(plan, options));
    }

    private RocksDBRepairReport executeLockIndexRepair(RocksDBRepairOptions options) {
        if (options.getLockIndexRunId() == null || options.getLockIndexRunId().isEmpty()) {
            throw new StoreException("lock index repair requires lockIndexRunId");
        }
        RocksDBVerifyReport before =
                verifyCurrentState(RocksDBVerifyOptions.full(100, options.getVerifyDeadlineMillis()));
        if (!before.isComplete()) {
            throw new StoreException("lock index repair requires a complete pre-repair verification");
        }
        RocksDBLockIndexRepairProgress saved = loadLockIndexProgress(options.getLockIndexRunId());
        byte[] cursor = saved == null ? null : saved.cursor;
        int deleted = saved == null ? 0 : saved.deleted;
        if (hasUnrepairableSourceViolation(before)) {
            final byte[] stoppedCursor = cursor;
            final int stoppedDeleted = deleted;
            storeEngine.withMaintenanceLock(() -> {
                stopLockIndexRepair(
                        options.getLockIndexRunId(),
                        stoppedCursor == null ? EMPTY_PREFIX : stoppedCursor,
                        stoppedDeleted);
                return null;
            });
            return new RocksDBRepairReport(RocksDBRepairReport.State.STOPPED, 0, deleted, cursor, before, before);
        }
        for (int i = 0; i < options.getMaxLockIndexBatches(); i++) {
            final byte[] batchCursor = cursor;
            final int batchDeleted = deleted;
            LockIndexBatchResult result = storeEngine.withMaintenanceLock(() -> repairOneLockIndexBatch(
                    batchCursor,
                    options.getLockIndexBatchLimit(),
                    options.getLockIndexRunId(),
                    batchDeleted,
                    options.getVerifyDeadlineMillis()));
            cursor = result.cursor;
            deleted = result.deleted;
            if (result.stopped) {
                return finishLockIndexRepair(
                        RocksDBRepairReport.State.STOPPED, options.getLockIndexRunId(), deleted, cursor, before);
            }
            if (cursor == null) {
                return finishLockIndexRepair(
                        RocksDBRepairReport.State.COMPLETED, options.getLockIndexRunId(), deleted, null, before);
            }
            if (i + 1 < options.getMaxLockIndexBatches()) {
                sleepBetweenLockIndexBatches(options.getLockIndexRoundSleepMillis());
            }
        }
        return finishLockIndexRepair(
                RocksDBRepairReport.State.PAUSED, options.getLockIndexRunId(), deleted, cursor, before);
    }

    private RocksDBRepairReport finishLockIndexRepair(
            RocksDBRepairReport.State requestedState,
            String runId,
            int deleted,
            byte[] cursor,
            RocksDBVerifyReport before) {
        return storeEngine.withMaintenanceLock(() -> {
            RocksDBVerifyReport after = verifyCurrentState();
            RocksDBRepairReport.State state = requestedState;
            if (!after.isComplete() || after.getInconsistentCount() > before.getInconsistentCount()) {
                stopLockIndexRepair(runId, cursor, deleted);
                state = RocksDBRepairReport.State.STOPPED;
            }
            return new RocksDBRepairReport(state, 1, deleted, cursor, before, after);
        });
    }

    private RocksDBLockIndexRepairProgress loadLockIndexProgress(String runId) {
        RocksDBLockIndexRepairProgress progress = readLockIndexProgress();
        if (progress == null) {
            return null;
        }
        if (progress.state == RocksDBLockIndexRepairProgress.State.STOPPED || !runId.equals(progress.runId)) {
            throw new StoreException("lock index repair cannot resume persisted run");
        }
        return progress;
    }

    private RocksDBLockIndexRepairProgress readLockIndexProgress() {
        byte[] raw = storeEngine.get(RocksDBColumnFamily.METADATA, LOCK_INDEX_REPAIR_PROGRESS_KEY);
        if (raw == null) {
            return null;
        }
        try {
            return RocksDBLockIndexRepairProgress.decode(raw);
        } catch (IllegalArgumentException e) {
            throw new StoreException(e, "invalid persisted lock index repair progress");
        }
    }

    private boolean hasLockIndexRepairProgress() {
        return storeEngine.get(RocksDBColumnFamily.METADATA, LOCK_INDEX_REPAIR_PROGRESS_KEY) != null;
    }

    private LockIndexBatchResult repairOneLockIndexBatch(
            byte[] cursor, int limit, String runId, int deletedBefore, long verifyDeadlineMillis) {
        byte[] seek = cursor == null ? EMPTY_PREFIX : cursor;
        RocksDBVerifyReport pageBefore = verifyCurrentState(RocksDBVerifyOptions.page(
                limit,
                new RocksDBVerifyCursor(RocksDBColumnFamily.LOCK_BRANCH_INDEX, seek),
                100,
                verifyDeadlineMillis));
        ArrayList<RocksDBStoreEngine.RocksDBEntry> entries = new ArrayList<>();
        RocksDBStoreEngine.ScanStats stats = storeEngine.scanByPrefix(
                RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                seek,
                EMPTY_PREFIX,
                limit,
                null,
                (key, value) -> entries.add(new RocksDBStoreEngine.RocksDBEntry(key, value)));
        if (entries.isEmpty()) {
            storeEngine.delete(RocksDBColumnFamily.METADATA, LOCK_INDEX_REPAIR_PROGRESS_KEY);
            return new LockIndexBatchResult(null, deletedBefore, false);
        }
        byte[] next = stats.isLimitReached()
                ? nextSeekKey(entries.get(entries.size() - 1).getKey())
                : null;
        ArrayList<byte[]> deletions = new ArrayList<>();
        for (RocksDBStoreEngine.RocksDBEntry entry : entries) {
            Boolean deletable = isDeletableStaleLockIndex(entry.getKey(), entry.getValue());
            if (deletable == null) {
                stopLockIndexRepair(runId, seek, deletedBefore);
                return new LockIndexBatchResult(seek, deletedBefore, true);
            }
            if (deletable) {
                deletions.add(entry.getKey());
            }
        }
        int deleted = deletedBefore + deletions.size();
        try (WriteBatch batch = new WriteBatch()) {
            for (byte[] key : deletions) {
                storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, key);
            }
            if (next == null) {
                storeEngine.delete(batch, RocksDBColumnFamily.METADATA, LOCK_INDEX_REPAIR_PROGRESS_KEY);
            } else {
                storeEngine.put(
                        batch,
                        RocksDBColumnFamily.METADATA,
                        LOCK_INDEX_REPAIR_PROGRESS_KEY,
                        new RocksDBLockIndexRepairProgress(
                                        RocksDBLockIndexRepairProgress.State.PAUSED, runId, next, deleted)
                                .encode());
            }
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write lock index repair batch failed");
        }
        RocksDBVerifyReport pageAfter = verifyCurrentState(RocksDBVerifyOptions.page(
                limit,
                new RocksDBVerifyCursor(RocksDBColumnFamily.LOCK_BRANCH_INDEX, seek),
                100,
                verifyDeadlineMillis));
        if (pageAfter.getInconsistentCount() > pageBefore.getInconsistentCount()) {
            stopLockIndexRepair(runId, next, deleted);
            return new LockIndexBatchResult(next, deleted, true);
        }
        return new LockIndexBatchResult(next, deleted, false);
    }

    private Boolean isDeletableStaleLockIndex(byte[] indexKey, byte[] lockKey) {
        byte[] lockValue = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
        if (lockValue == null) {
            return Boolean.TRUE;
        }
        LockVerifyEntry lock = decodeLock(lockValue);
        if (lock == null || !hasValidLockSource(lock)) {
            return null;
        }
        byte[] expected = RocksDBKeyCodec.encodeLockBranchIndex(lock.xid, lock.branchId, lockKey);
        if (Arrays.equals(indexKey, expected)) {
            return Boolean.FALSE;
        }
        return Arrays.equals(lockKey, storeEngine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, expected))
                ? Boolean.TRUE
                : null;
    }

    private boolean hasValidLockSource(LockVerifyEntry lock) {
        byte[] globalValue = storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(lock.xid));
        GlobalVerifyEntry global = decodeGlobal(globalValue, null, "global session");
        if (global == null || !lock.xid.equals(global.xid)) {
            return false;
        }
        byte[] branchValue = storeEngine.get(
                RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch(lock.xid, lock.branchId));
        BranchVerifyEntry branch = decodeBranch(branchValue, null);
        return branch != null && lock.xid.equals(branch.xid) && lock.branchId == branch.branchId;
    }

    private void stopLockIndexRepair(String runId, byte[] cursor, int deleted) {
        storeEngine.put(
                RocksDBColumnFamily.METADATA,
                LOCK_INDEX_REPAIR_PROGRESS_KEY,
                new RocksDBLockIndexRepairProgress(
                                RocksDBLockIndexRepairProgress.State.STOPPED,
                                runId,
                                cursor == null || cursor.length == 0 ? null : cursor,
                                deleted)
                        .encode());
    }

    private static void sleepBetweenLockIndexBatches(long sleepMillis) {
        if (sleepMillis == 0) {
            return;
        }
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreException(e, "lock index repair interrupted between batches");
        }
    }

    private static final class LockIndexBatchResult {
        final byte[] cursor;
        final int deleted;
        final boolean stopped;

        private LockIndexBatchResult(byte[] cursor, int deleted, boolean stopped) {
            this.cursor = cursor;
            this.deleted = deleted;
            this.stopped = stopped;
        }
    }

    private RocksDBRepairReport executeRepairInMaintenanceWindow(RocksDBRepairPlan plan, RocksDBRepairOptions options) {
        RocksDBVerifyReport beforeVerifyReport = verifyCurrentState(
                RocksDBVerifyOptions.full(100, options.getVerifyDeadlineMillis()), options.getMaxRepairEntries());
        if (!beforeVerifyReport.isComplete()) {
            throw new StoreException("RocksDB repair requires a complete pre-repair verification");
        }
        if (hasUnrepairableSourceViolation(beforeVerifyReport)) {
            throw new StoreException("RocksDB repair rejected because source data has unrepairable violations");
        }
        int executedActionCount = 0;
        if (plan.hasAction(RocksDBRepairPlan.Action.REBUILD_GLOBAL_SECONDARY_INDEXES)) {
            indexManager.rebuildFromGlobalSessionsAtomically(options.getMaxRepairEntries());
            executedActionCount++;
        }
        RocksDBVerifyReport afterVerifyReport =
                verifyCurrentState(RocksDBVerifyOptions.full(100, options.getVerifyDeadlineMillis()));
        if (!afterVerifyReport.isComplete()) {
            throw new StoreException("RocksDB repair requires a complete post-repair verification");
        }
        if (afterVerifyReport.getInconsistentCount() > beforeVerifyReport.getInconsistentCount()) {
            throw new StoreException("RocksDB repair increased consistency violations");
        }
        return new RocksDBRepairReport(false, executedActionCount, beforeVerifyReport, afterVerifyReport);
    }

    private static boolean requiresGlobalSecondaryIndexRebuild(RocksDBVerifyReport report) {
        return report.getStaleStatusIndexCount() > 0
                || report.getStaleTimeoutIndexCount() > 0
                || report.getStaleTransactionIdIndexCount() > 0
                || report.getMissingStatusIndexCount() > 0
                || report.getMissingTimeoutIndexCount() > 0
                || report.getMissingTransactionIdIndexCount() > 0;
    }

    private static boolean hasUnrepairableSourceViolation(RocksDBVerifyReport report) {
        return report.getInvalidGlobalCount() > 0
                || report.getInvalidBranchCount() > 0
                || report.getOrphanBranchCount() > 0
                || report.getOrphanLockCount() > 0
                || report.getInvalidMetadataCount() > 0;
    }

    private void writeCheckpointMetadata(Path checkpointPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("sourceDbPath=").append(storeEngine.getConfig().getDbPath()).append('\n');
        sb.append("formatVersion=").append(RocksDBStoreEngine.FORMAT_VERSION).append('\n');
        sb.append("createdAt=").append(Instant.now().toString()).append('\n');
        sb.append("columnFamilies=");
        RocksDBColumnFamily[] families = RocksDBColumnFamily.values();
        for (int i = 0; i < families.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(families[i].getName());
        }
        sb.append('\n');
        RocksDB.Version version = RocksDB.rocksdbVersion();
        sb.append("rocksdbVersion=")
                .append(version != null ? version.toString() : "unknown")
                .append('\n');
        sb.append("syncWrite=").append(storeEngine.getConfig().isSyncWrite()).append('\n');
        String seataVersion = RocksDBStoreEngine.class.getPackage() != null
                ? RocksDBStoreEngine.class.getPackage().getImplementationVersion()
                : null;
        sb.append("seataVersion=")
                .append(seataVersion != null ? seataVersion : "unknown")
                .append('\n');
        Files.write(
                checkpointPath.resolve(CHECKPOINT_METADATA_FILE), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isDirectoryEmpty(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            throw new StoreException(e, "check directory failed:" + dir);
        }
    }

    private static final class GlobalVerifyEntry {
        final String xid;
        final long transactionId;
        final GlobalStatus status;
        final long beginTime;
        final long timeout;

        GlobalVerifyEntry(String xid, long transactionId, GlobalStatus status, long beginTime, long timeout) {
            this.xid = xid;
            this.transactionId = transactionId;
            this.status = status;
            this.beginTime = beginTime;
            this.timeout = timeout;
        }

        long deadlineMillis() {
            if (timeout > 0 && beginTime > Long.MAX_VALUE - timeout) {
                return Long.MAX_VALUE;
            }
            return beginTime + timeout;
        }
    }

    private static final class BranchVerifyEntry {
        final String xid;
        final long branchId;

        private BranchVerifyEntry(String xid, long branchId) {
            this.xid = xid;
            this.branchId = branchId;
        }
    }

    private static final class VerifyScanResult {
        final RocksDBStoreEngine.ScanStats stats;
        final byte[] lastKey;

        private VerifyScanResult(RocksDBStoreEngine.ScanStats stats, byte[] lastKey) {
            this.stats = stats;
            this.lastKey = lastKey;
        }
    }

    private static final class LockVerifyEntry {
        final String xid;
        final long branchId;

        LockVerifyEntry(String xid, long branchId) {
            this.xid = xid;
            this.branchId = branchId;
        }
    }
}
