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
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Migrates legacy file-mode session logs to RocksDB current-state storage.
 */
public class RocksDBMigrationService {

    public static final String MIGRATION_STATUS_KEY = "migration_status";
    public static final String MIGRATION_STATUS_IN_PROGRESS = "in_progress";
    public static final String MIGRATION_STATUS_COMPLETED = "completed";

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBMigrationService.class);

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
            putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_COMPLETED);
            return false;
        }

        putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_IN_PROGRESS);
        clearCurrentState(storeEngine);

        MigrationState migrationState = new MigrationState();
        int replayed =
                fileSessionLogReplayer.replay(fileSessionLogPath, writeStore -> apply(writeStore, migrationState));
        writeCurrentState(storeEngine, migrationState.globalSessions.values());
        putMetadata(storeEngine, MIGRATION_STATUS_KEY, MIGRATION_STATUS_COMPLETED);
        storeEngine.flush();
        fileSessionLogReplayer.markMigrated(fileSessionLogPath);

        LOGGER.info(
                "Migrated file session logs to RocksDB, file:{}, records:{}, globalSessions:{}",
                fileSessionLogPath,
                replayed,
                migrationState.globalSessions.size());
        return true;
    }

    private void apply(TransactionWriteStore writeStore, MigrationState migrationState) {
        LogOperation logOperation = writeStore.getOperate();
        SessionStorable sessionStorable = writeStore.getSessionRequest();
        switch (logOperation) {
            case GLOBAL_ADD:
            case GLOBAL_UPDATE:
                applyGlobalUpdate((GlobalSession) sessionStorable, migrationState);
                break;
            case GLOBAL_REMOVE:
                applyGlobalRemove((GlobalSession) sessionStorable, migrationState);
                break;
            case BRANCH_ADD:
            case BRANCH_UPDATE:
                applyBranchUpdate((BranchSession) sessionStorable, migrationState);
                break;
            case BRANCH_REMOVE:
                applyBranchRemove((BranchSession) sessionStorable, migrationState);
                break;
            default:
                throw new StoreException("Unknown LogOperation:" + logOperation);
        }
    }

    private void applyGlobalUpdate(GlobalSession globalSession, MigrationState migrationState) {
        if (!validTransactionId(globalSession.getTransactionId(), globalSession.getXid(), "globalSession")) {
            return;
        }
        String xid = globalSession.getXid();
        if (migrationState.removedGlobals.contains(xid)) {
            return;
        }
        GlobalSession existing = migrationState.globalSessions.get(xid);
        if (existing == null) {
            if (shouldKeep(globalSession)) {
                migrationState.globalSessions.put(xid, globalSession);
                attachUnhandledBranches(globalSession, migrationState);
            } else {
                removeGlobalState(xid, migrationState);
            }
            return;
        }
        if (shouldKeep(globalSession)) {
            existing.setStatus(globalSession.getStatus());
        } else {
            removeGlobalState(xid, migrationState);
        }
    }

    private void applyGlobalRemove(GlobalSession globalSession, MigrationState migrationState) {
        if (!validTransactionId(globalSession.getTransactionId(), globalSession.getXid(), "globalSession")) {
            return;
        }
        removeGlobalState(globalSession.getXid(), migrationState);
    }

    private void applyBranchUpdate(BranchSession branchSession, MigrationState migrationState) {
        if (!validTransactionId(branchSession.getTransactionId(), branchSession.getXid(), "branchSession")) {
            return;
        }
        String xid = branchSession.getXid();
        if (migrationState.removedGlobals.contains(xid)) {
            return;
        }
        GlobalSession globalSession = migrationState.globalSessions.get(xid);
        if (globalSession == null) {
            migrationState
                    .unhandledBranches
                    .computeIfAbsent(xid, key -> new HashMap<>())
                    .put(branchSession.getBranchId(), branchSession);
            return;
        }
        addOrUpdateBranch(globalSession, branchSession);
    }

    private void applyBranchRemove(BranchSession branchSession, MigrationState migrationState) {
        if (!validTransactionId(branchSession.getTransactionId(), branchSession.getXid(), "branchSession")) {
            return;
        }
        String xid = branchSession.getXid();
        if (migrationState.removedGlobals.contains(xid)) {
            return;
        }
        Map<Long, BranchSession> bufferedBranches = migrationState.unhandledBranches.get(xid);
        if (bufferedBranches != null) {
            bufferedBranches.remove(branchSession.getBranchId());
        }
        GlobalSession globalSession = migrationState.globalSessions.get(xid);
        if (globalSession == null) {
            return;
        }
        BranchSession existing = globalSession.getBranch(branchSession.getBranchId());
        if (existing != null) {
            globalSession.remove(existing);
        }
    }

    private void attachUnhandledBranches(GlobalSession globalSession, MigrationState migrationState) {
        Map<Long, BranchSession> branches = migrationState.unhandledBranches.remove(globalSession.getXid());
        if (branches == null) {
            return;
        }
        branches.values().forEach(branchSession -> addOrUpdateBranch(globalSession, branchSession));
    }

    private void addOrUpdateBranch(GlobalSession globalSession, BranchSession branchSession) {
        BranchSession existing = globalSession.getBranch(branchSession.getBranchId());
        if (existing == null) {
            globalSession.add(branchSession);
        } else {
            existing.setStatus(branchSession.getStatus());
        }
    }

    private void removeGlobalState(String xid, MigrationState migrationState) {
        migrationState.globalSessions.remove(xid);
        migrationState.unhandledBranches.remove(xid);
        migrationState.removedGlobals.add(xid);
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

    private void writeCurrentState(RocksDBStoreEngine storeEngine, Collection<GlobalSession> globalSessions) {
        RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(storeEngine);
        for (GlobalSession globalSession : globalSessions) {
            storeManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);
            globalSession
                    .getSortedBranches()
                    .forEach(branchSession -> storeManager.writeSession(LogOperation.BRANCH_ADD, branchSession));
        }
    }

    private boolean hasCurrentState(RocksDBStoreEngine storeEngine) {
        return storeEngine.prefixExists(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0])
                || storeEngine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, new byte[0])
                || storeEngine.prefixExists(RocksDBColumnFamily.LOCK, new byte[0])
                || storeEngine.prefixExists(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                || storeEngine.prefixExists(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, new byte[0])
                || storeEngine.prefixExists(RocksDBColumnFamily.TRANSACTION_ID_INDEX, new byte[0]);
    }

    private void clearCurrentState(RocksDBStoreEngine storeEngine) {
        storeEngine.deleteByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0]);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.BRANCH_SESSION, new byte[0]);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.LOCK, new byte[0]);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0]);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, new byte[0]);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.TRANSACTION_ID_INDEX, new byte[0]);
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

    private static class MigrationState {
        private final Map<String, GlobalSession> globalSessions = new HashMap<>();
        private final Map<String, Map<Long, BranchSession>> unhandledBranches = new HashMap<>();
        private final Set<String> removedGlobals = new HashSet<>();
    }
}
