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

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.file.store.FileSessionLogReplayer;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Migrates legacy file-mode session logs to RocksDB current-state storage.
 */
public class RocksDBMigrationService {

    public static final String MIGRATION_STATUS_KEY = "migration_status";
    public static final String MIGRATION_STATUS_IN_PROGRESS = "in_progress";
    public static final String MIGRATION_STATUS_GUARDING_EMPTY = "guarding_empty";
    public static final String MIGRATION_STATUS_COMPLETED = "completed";

    private static final String REMOVED_XID_METADATA_PREFIX = "migration_removed_xid:";
    private static final int CLEANUP_BATCH_SIZE = 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBMigrationService.class);

    private static final EnumSet<RocksDBColumnFamily> CURRENT_STATE_COLUMN_FAMILIES = EnumSet.of(
            RocksDBColumnFamily.GLOBAL_SESSION,
            RocksDBColumnFamily.BRANCH_SESSION,
            RocksDBColumnFamily.LOCK,
            RocksDBColumnFamily.LOCK_BRANCH_INDEX,
            RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
            RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
            RocksDBColumnFamily.TRANSACTION_ID_INDEX);

    private final FileSessionLogReplayer fileSessionLogReplayer;

    public RocksDBMigrationService() {
        this(new FileSessionLogReplayer());
    }

    public RocksDBMigrationService(FileSessionLogReplayer fileSessionLogReplayer) {
        this.fileSessionLogReplayer = fileSessionLogReplayer;
    }

    public boolean migrate(Path fileSessionLogPath, RocksDBStoreEngine storeEngine) {
        String migrationStatus = getMetadata(storeEngine, MIGRATION_STATUS_KEY);
        if (MIGRATION_STATUS_COMPLETED.equals(migrationStatus)) {
            if (!fileSessionLogReplayer.hasMigrationMarker(fileSessionLogPath)) {
                storeEngine.flush();
                fileSessionLogReplayer.markMigrated(fileSessionLogPath);
                putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_COMPLETED);
            }
            return false;
        }
        if (MIGRATION_STATUS_IN_PROGRESS.equals(migrationStatus)
                && fileSessionLogReplayer.hasMigrationMarker(fileSessionLogPath)) {
            putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_COMPLETED);
            return false;
        }
        if (MIGRATION_STATUS_GUARDING_EMPTY.equals(migrationStatus)) {
            completeEmptySourceGuard(fileSessionLogPath, storeEngine);
            return false;
        }
        if (StringUtils.isBlank(migrationStatus) && fileSessionLogReplayer.hasMigrationMarker(fileSessionLogPath)) {
            throw new StoreException("file session logs were already migrated to RocksDB, but RocksDB migration "
                    + "metadata is missing. Restore the RocksDB directory or remove the migration marker explicitly, "
                    + "file:" + fileSessionLogPath);
        }
        if (StringUtils.isNotBlank(migrationStatus) && !MIGRATION_STATUS_IN_PROGRESS.equals(migrationStatus)) {
            throw new StoreException("unknown RocksDB migration status:" + migrationStatus);
        }

        boolean hasFileLogs = fileSessionLogReplayer.hasSessionLogs(fileSessionLogPath);
        if (MIGRATION_STATUS_IN_PROGRESS.equals(migrationStatus) && !hasFileLogs) {
            throw new StoreException("RocksDB migration was interrupted but source file logs are missing");
        }
        if (StringUtils.isBlank(migrationStatus) && hasCurrentState(storeEngine)) {
            throw new StoreException("RocksDB current state exists without migration status");
        }
        if (!hasFileLogs) {
            putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_GUARDING_EMPTY);
            completeEmptySourceGuard(fileSessionLogPath, storeEngine);
            return false;
        }

        putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_IN_PROGRESS);
        clearCurrentState(storeEngine);
        clearRemovedXidTombstones(storeEngine);

        RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(storeEngine);
        int replayed = fileSessionLogReplayer.replay(
                fileSessionLogPath, writeStore -> apply(writeStore, storeEngine, storeManager));
        cleanupOrphanBranches(storeEngine);
        clearRemovedXidTombstones(storeEngine);
        storeEngine.flush();
        fileSessionLogReplayer.markMigrated(fileSessionLogPath);
        putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_COMPLETED);

        LOGGER.info("Migrated file session logs to RocksDB, file:{}, records:{}", fileSessionLogPath, replayed);
        return true;
    }

    private void apply(
            TransactionWriteStore writeStore,
            RocksDBStoreEngine storeEngine,
            RocksDBTransactionStoreManager storeManager) {
        LogOperation logOperation = writeStore.getOperate();
        SessionStorable sessionStorable = writeStore.getSessionRequest();
        switch (logOperation) {
            case GLOBAL_ADD:
            case GLOBAL_UPDATE:
                applyGlobalUpdate((GlobalSession) sessionStorable, storeEngine, storeManager);
                break;
            case GLOBAL_REMOVE:
                applyGlobalRemove((GlobalSession) sessionStorable, storeEngine, storeManager);
                break;
            case BRANCH_ADD:
            case BRANCH_UPDATE:
                applyBranchUpdate((BranchSession) sessionStorable, storeEngine, storeManager, logOperation);
                break;
            case BRANCH_REMOVE:
                applyBranchRemove((BranchSession) sessionStorable, storeEngine, storeManager);
                break;
            default:
                throw new StoreException("Unknown LogOperation:" + logOperation);
        }
    }

    private void applyGlobalUpdate(
            GlobalSession globalSession, RocksDBStoreEngine storeEngine, RocksDBTransactionStoreManager storeManager) {
        if (!validTransactionId(globalSession.getTransactionId(), globalSession.getXid(), "globalSession")) {
            return;
        }
        if (isRemovedGlobal(storeEngine, globalSession.getXid())) {
            return;
        }
        if (shouldKeep(globalSession)) {
            storeManager.writeSession(LogOperation.GLOBAL_UPDATE, globalSession);
        } else {
            removeGlobalAndWriteTombstone(globalSession, storeEngine, storeManager);
        }
    }

    private void applyGlobalRemove(
            GlobalSession globalSession, RocksDBStoreEngine storeEngine, RocksDBTransactionStoreManager storeManager) {
        if (!validTransactionId(globalSession.getTransactionId(), globalSession.getXid(), "globalSession")) {
            return;
        }
        removeGlobalAndWriteTombstone(globalSession, storeEngine, storeManager);
    }

    private void removeGlobalAndWriteTombstone(
            GlobalSession globalSession, RocksDBStoreEngine storeEngine, RocksDBTransactionStoreManager storeManager) {
        storeEngine.put(RocksDBColumnFamily.METADATA, removedGlobalMetadataKey(globalSession.getXid()), new byte[] {1});
        storeEngine.deleteByPrefix(
                RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix(globalSession.getXid()));
        storeManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession);
    }

    private void completeEmptySourceGuard(Path fileSessionLogPath, RocksDBStoreEngine storeEngine) {
        if (fileSessionLogReplayer.hasSessionLogs(fileSessionLogPath)) {
            throw new StoreException("RocksDB migration was guarding empty legacy source, but source file logs exist");
        }
        if (hasCurrentState(storeEngine)) {
            throw new StoreException(
                    "RocksDB migration was guarding empty legacy source, but RocksDB current state exists");
        }
        storeEngine.flush();
        fileSessionLogReplayer.markMigrated(fileSessionLogPath);
        putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_COMPLETED);
    }

    private void applyBranchUpdate(
            BranchSession branchSession,
            RocksDBStoreEngine storeEngine,
            RocksDBTransactionStoreManager storeManager,
            LogOperation logOperation) {
        if (!validTransactionId(branchSession.getTransactionId(), branchSession.getXid(), "branchSession")) {
            return;
        }
        if (isRemovedGlobal(storeEngine, branchSession.getXid())) {
            return;
        }
        storeManager.writeSession(logOperation, branchSession);
    }

    private void applyBranchRemove(
            BranchSession branchSession, RocksDBStoreEngine storeEngine, RocksDBTransactionStoreManager storeManager) {
        if (!validTransactionId(branchSession.getTransactionId(), branchSession.getXid(), "branchSession")) {
            return;
        }
        if (isRemovedGlobal(storeEngine, branchSession.getXid())) {
            return;
        }
        storeManager.writeSession(LogOperation.BRANCH_REMOVE, branchSession);
    }

    private boolean isRemovedGlobal(RocksDBStoreEngine storeEngine, String xid) {
        return storeEngine.get(RocksDBColumnFamily.METADATA, removedGlobalMetadataKey(xid)) != null;
    }

    private boolean shouldKeep(GlobalSession globalSession) {
        GlobalStatus globalStatus = globalSession.getStatus();
        switch (globalStatus) {
            case UnKnown:
            case Committed:
            case CommitFailed:
            case Rollbacked:
            case RollbackFailed:
            case TimeoutRollbacked:
            case TimeoutRollbackFailed:
            case RollbackRetryTimeout:
            case Finished:
                return false;
            default:
                return true;
        }
    }

    private boolean validTransactionId(long transactionId, String xid, String type) {
        if (transactionId != 0) {
            return true;
        }
        LOGGER.error("Migrate {} from file failed, the transactionId is zero, xid:{}", type, xid);
        return false;
    }

    private boolean hasCurrentState(RocksDBStoreEngine storeEngine) {
        return CURRENT_STATE_COLUMN_FAMILIES.stream()
                .anyMatch(columnFamily -> storeEngine.prefixExists(columnFamily, new byte[0]));
    }

    private void clearCurrentState(RocksDBStoreEngine storeEngine) {
        CURRENT_STATE_COLUMN_FAMILIES.forEach(columnFamily -> storeEngine.deleteByPrefix(columnFamily, new byte[0]));
    }

    private void cleanupOrphanBranches(RocksDBStoreEngine storeEngine) {
        byte[] branchPrefix = new byte[0];
        byte[] seekKey = branchPrefix;
        while (true) {
            List<RocksDBStoreEngine.RocksDBEntry> branches = new ArrayList<>(CLEANUP_BATCH_SIZE);
            storeEngine.scanByPrefix(
                    RocksDBColumnFamily.BRANCH_SESSION,
                    seekKey,
                    branchPrefix,
                    CLEANUP_BATCH_SIZE,
                    null,
                    (key, value) -> branches.add(new RocksDBStoreEngine.RocksDBEntry(key, value)));
            if (branches.isEmpty()) {
                return;
            }
            deleteOrphanBranches(storeEngine, branches);
            if (branches.size() < CLEANUP_BATCH_SIZE) {
                return;
            }
            seekKey = nextKey(branches.get(branches.size() - 1).getKey());
        }
    }

    private void deleteOrphanBranches(RocksDBStoreEngine storeEngine, List<RocksDBStoreEngine.RocksDBEntry> branches) {
        try (WriteBatch batch = new WriteBatch()) {
            boolean hasOrphans = false;
            for (RocksDBStoreEngine.RocksDBEntry branch : branches) {
                String xid = RocksDBKeyCodec.extractXidFromBranchKey(branch.getKey());
                if (xid == null
                        || storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid))
                                == null) {
                    storeEngine.delete(batch, RocksDBColumnFamily.BRANCH_SESSION, branch.getKey());
                    hasOrphans = true;
                }
            }
            if (hasOrphans) {
                storeEngine.write(batch);
            }
        } catch (RocksDBException e) {
            throw new StoreException(e, "remove orphan branch sessions after migration failed");
        }
    }

    private void clearRemovedXidTombstones(RocksDBStoreEngine storeEngine) {
        deleteMetadataByPrefix(storeEngine, REMOVED_XID_METADATA_PREFIX.getBytes(StandardCharsets.UTF_8));
    }

    private void deleteMetadataByPrefix(RocksDBStoreEngine storeEngine, byte[] prefix) {
        byte[] seekKey = prefix;
        while (true) {
            List<byte[]> keys = new ArrayList<>(CLEANUP_BATCH_SIZE);
            storeEngine.scanByPrefix(
                    RocksDBColumnFamily.METADATA,
                    seekKey,
                    prefix,
                    CLEANUP_BATCH_SIZE,
                    null,
                    (key, value) -> keys.add(key));
            if (keys.isEmpty()) {
                return;
            }
            try (WriteBatch batch = new WriteBatch()) {
                for (byte[] key : keys) {
                    storeEngine.delete(batch, RocksDBColumnFamily.METADATA, key);
                }
                storeEngine.write(batch);
            } catch (RocksDBException e) {
                throw new StoreException(e, "remove migration tombstones failed");
            }
            if (keys.size() < CLEANUP_BATCH_SIZE) {
                return;
            }
            seekKey = nextKey(keys.get(keys.size() - 1));
        }
    }

    private byte[] removedGlobalMetadataKey(String xid) {
        return (REMOVED_XID_METADATA_PREFIX + xid).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] nextKey(byte[] key) {
        byte[] next = new byte[key.length + 1];
        System.arraycopy(key, 0, next, 0, key.length);
        return next;
    }

    private String getMetadata(RocksDBStoreEngine storeEngine, String key) {
        byte[] value = storeEngine.get(RocksDBColumnFamily.METADATA, key.getBytes(StandardCharsets.UTF_8));
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    private void putMetadata(RocksDBStoreEngine storeEngine, String key, String value) {
        storeEngine.put(
                RocksDBColumnFamily.METADATA,
                key.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8));
    }
}
