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
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.apache.seata.server.storage.rocksdb.index.RocksDBIndexManager;
import org.rocksdb.Checkpoint;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private static final byte[] EMPTY_PREFIX = new byte[0];

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
        try (Checkpoint checkpoint = Checkpoint.create(storeEngine.getDB())) {
            checkpoint.createCheckpoint(checkpointPath.toString());
        } catch (RocksDBException e) {
            throw new StoreException(e, "create RocksDB checkpoint failed, path:" + checkpointPath);
        }
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
     * Verify the consistency of the current RocksDB state.
     *
     * <p>This method scans all column families and cross-checks references between
     * global sessions, branch sessions, locks, lock indexes and secondary indexes.
     * The returned report describes the counts of checked entities and any
     * inconsistencies found. This method does not modify any data.
     */
    public RocksDBVerifyReport verifyCurrentState() {
        RocksDBVerifyReport.Builder report = RocksDBVerifyReport.builder();

        // Step 1: Check format version
        byte[] versionBytes =
                storeEngine.get(RocksDBColumnFamily.METADATA, "format_version".getBytes(StandardCharsets.UTF_8));
        if (versionBytes == null) {
            report.addError("missing format_version in metadata");
        } else {
            String version = new String(versionBytes, StandardCharsets.UTF_8);
            if (!Integer.toString(RocksDBStoreEngine.FORMAT_VERSION).equals(version)) {
                report.addError("format_version mismatch, expected:" + RocksDBStoreEngine.FORMAT_VERSION + ", found:"
                        + version);
            }
        }

        // Step 2: Scan global sessions
        Map<String, GlobalVerifyEntry> globalSessions = new HashMap<>();
        storeEngine.scanByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, EMPTY_PREFIX, (key, value) -> {
            try {
                RocksDBValueCodec.DecodedValue decoded = RocksDBValueCodec.decode(value);
                GlobalSession session = new GlobalSession(null, null, null, 0, true);
                session.decode(decoded.getPayload());
                globalSessions.put(
                        session.getXid(),
                        new GlobalVerifyEntry(session.getTransactionId(), session.getStatus(), session.getBeginTime()));
            } catch (Exception e) {
                report.addError(
                        "failed to decode global session, key length:" + key.length + ", message:" + e.getMessage());
            }
        });
        report.checkedGlobalCount(globalSessions.size());

        // Step 3: Verify GLOBAL_STATUS_INDEX
        int staleStatusIndex = 0;
        for (Map.Entry<String, GlobalVerifyEntry> entry : globalSessions.entrySet()) {
            String xid = entry.getKey();
            GlobalVerifyEntry globalEntry = entry.getValue();
            byte[] expectedKey =
                    RocksDBKeyCodec.encodeGlobalStatusIndex(globalEntry.status, globalEntry.beginTime, xid);
            if (storeEngine.get(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, expectedKey) == null) {
                staleStatusIndex++;
                report.addError("missing global status index, xid:" + xid);
            }
        }
        for (RocksDBStoreEngine.RocksDBEntry entry :
                storeEngine.prefixScan(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, EMPTY_PREFIX)) {
            String xid = RocksDBKeyCodec.extractXidFromStatusIndexKey(entry.getKey());
            if (xid == null) {
                report.addError("invalid global status index key, length:" + entry.getKey().length);
                staleStatusIndex++;
                continue;
            }
            GlobalVerifyEntry globalEntry = globalSessions.get(xid);
            if (globalEntry == null) {
                staleStatusIndex++;
                continue;
            }
            int statusCode = RocksDBKeyCodec.extractStatusCodeFromStatusIndexKey(entry.getKey());
            long beginTime = RocksDBKeyCodec.extractBeginTimeFromStatusIndexKey(entry.getKey());
            if (statusCode != globalEntry.status.getCode() || beginTime != globalEntry.beginTime) {
                staleStatusIndex++;
            }
            // Verify value is xid
            String indexXid = new String(entry.getValue(), StandardCharsets.UTF_8);
            if (!xid.equals(indexXid)) {
                report.addError("global status index value mismatch, key xid:" + xid + ", value xid:" + indexXid);
                staleStatusIndex++;
            }
        }
        report.staleStatusIndexCount(staleStatusIndex);

        // Step 4: Verify TRANSACTION_ID_INDEX
        int staleTxnIdIndex = 0;
        for (Map.Entry<String, GlobalVerifyEntry> entry : globalSessions.entrySet()) {
            String xid = entry.getKey();
            byte[] expectedKey = RocksDBKeyCodec.encodeTransactionIdIndex(entry.getValue().transactionId);
            if (storeEngine.get(RocksDBColumnFamily.TRANSACTION_ID_INDEX, expectedKey) == null) {
                staleTxnIdIndex++;
                report.addError("missing transaction id index, xid:" + xid);
            }
        }
        for (RocksDBStoreEngine.RocksDBEntry entry :
                storeEngine.prefixScan(RocksDBColumnFamily.TRANSACTION_ID_INDEX, EMPTY_PREFIX)) {
            String xid = new String(entry.getValue(), StandardCharsets.UTF_8);
            GlobalVerifyEntry globalEntry = globalSessions.get(xid);
            if (globalEntry == null) {
                staleTxnIdIndex++;
                continue;
            }
            long transactionId = ByteBuffer.wrap(entry.getKey()).getLong();
            if (transactionId != globalEntry.transactionId) {
                staleTxnIdIndex++;
                report.addError("transaction id index mismatch, xid:" + xid
                        + ", indexTxnId:" + transactionId
                        + ", globalTxnId:" + globalEntry.transactionId);
            }
        }
        report.staleTransactionIdIndexCount(staleTxnIdIndex);

        // Step 5: Check orphan branches
        int orphanBranches = 0;
        int checkedBranches = 0;
        for (RocksDBStoreEngine.RocksDBEntry entry :
                storeEngine.prefixScan(RocksDBColumnFamily.BRANCH_SESSION, EMPTY_PREFIX)) {
            checkedBranches++;
            String xid = RocksDBKeyCodec.extractXidFromBranchKey(entry.getKey());
            if (xid == null || !globalSessions.containsKey(xid)) {
                orphanBranches++;
            }
        }
        report.checkedBranchCount(checkedBranches);
        report.orphanBranchCount(orphanBranches);

        // Step 6: Verify LOCK_BRANCH_INDEX and collect valid lock keys
        Set<String> validLockKeyHexSet = new HashSet<>();
        int staleLockIndex = 0;
        for (RocksDBStoreEngine.RocksDBEntry entry :
                storeEngine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, EMPTY_PREFIX)) {
            byte[] lockKey = entry.getValue();
            byte[] lockValue = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
            if (lockValue == null) {
                staleLockIndex++;
                continue;
            }
            // Cross-check: lock value xid should reference an existing global session
            String lockXid = tryDecodeLockXid(lockValue);
            if (lockXid != null && !globalSessions.containsKey(lockXid)) {
                staleLockIndex++;
                continue;
            }
            validLockKeyHexSet.add(bytesToHex(lockKey));
        }
        report.staleLockIndexCount(staleLockIndex);

        // Step 7: Check orphan locks (LOCK without matching LOCK_BRANCH_INDEX)
        int orphanLocks = 0;
        int checkedLocks = 0;
        for (RocksDBStoreEngine.RocksDBEntry entry : storeEngine.prefixScan(RocksDBColumnFamily.LOCK, EMPTY_PREFIX)) {
            checkedLocks++;
            if (!validLockKeyHexSet.contains(bytesToHex(entry.getKey()))) {
                orphanLocks++;
            }
        }
        report.checkedLockCount(checkedLocks);
        report.orphanLockCount(orphanLocks);

        RocksDBVerifyReport result = report.build();
        LOGGER.info("RocksDB verify completed, {}", result);
        return result;
    }

    /**
     * Rebuild secondary indexes from global session data.
     *
     * <p>This is an explicit repair action. It clears and rebuilds the
     * {@code GLOBAL_STATUS_INDEX} and {@code TRANSACTION_ID_INDEX} column families.
     */
    public void repairIndexes() {
        storeEngine.withMaintenanceLock(() -> {
            LOGGER.info("Rebuilding RocksDB secondary indexes");
            indexManager.rebuildFromGlobalSessions();
            LOGGER.info("RocksDB secondary indexes rebuilt");
            return null;
        });
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Try to extract xid from a lock holder value.
     * Lock value layout: RocksDBValueCodec header + xid(int length + bytes) + transactionId(long) + ...
     * Returns null if decoding fails.
     */
    private static String tryDecodeLockXid(byte[] lockValue) {
        try {
            RocksDBValueCodec.DecodedValue decoded = RocksDBValueCodec.decode(lockValue);
            if (decoded.getType() != RocksDBValueCodec.ValueType.LOCK_HOLDER) {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded.getPayload());
            int xidLength = buffer.getInt();
            if (xidLength < 0 || buffer.remaining() < xidLength) {
                return null;
            }
            if (xidLength == 0) {
                return null;
            }
            byte[] xidBytes = new byte[xidLength];
            buffer.get(xidBytes);
            return new String(xidBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lightweight holder for global session fields needed during verify.
     */
    private static class GlobalVerifyEntry {
        final long transactionId;
        final GlobalStatus status;
        final long beginTime;

        GlobalVerifyEntry(long transactionId, GlobalStatus status, long beginTime) {
            this.transactionId = transactionId;
            this.status = status;
            this.beginTime = beginTime;
        }
    }
}
