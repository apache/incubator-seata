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

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RocksDB transaction store manager for file store engine.
 */
public class RocksDBTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBTransactionStoreManager.class);
    private static final String MIGRATION_STATUS_KEY = "migration_status";

    private final RocksDBStoreEngine storeEngine;
    private final RocksDBLocalLocks xidLocks;
    private final RocksDBIndexManager indexManager;
    private final boolean factoryManaged;

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
        if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readByStatuses(sessionCondition);
        }
        return scanGlobalSessions(sessionCondition);
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
            batch.put(storeEngine.handle(RocksDBColumnFamily.GLOBAL_SESSION), key, encodeGlobalSession(session));
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
            batch.delete(
                    storeEngine.handle(RocksDBColumnFamily.GLOBAL_SESSION),
                    RocksDBKeyCodec.encodeXid(session.getXid()));
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
        storeEngine.scanByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, new byte[0], (key, value) -> {
            GlobalSession globalSession = decodeGlobalSession(value, sessionCondition.isLazyLoadBranch());
            if (matches(globalSession, sessionCondition)) {
                if (!sessionCondition.isLazyLoadBranch()) {
                    readBranchSessions(globalSession.getXid()).forEach(globalSession::add);
                }
                result.add(globalSession);
            }
        });
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

    private List<GlobalSession> readByStatuses(SessionCondition sessionCondition) {
        Set<String> seenXids = new LinkedHashSet<>();
        List<GlobalSession> result = new ArrayList<>();
        for (GlobalStatus status : sessionCondition.getStatuses()) {
            indexManager.scanXidsByStatus(status, xid -> {
                if (!seenXids.add(xid)) {
                    return;
                }
                GlobalSession globalSession = readSession(xid, !sessionCondition.isLazyLoadBranch());
                if (globalSession != null && matches(globalSession, sessionCondition)) {
                    result.add(globalSession);
                }
            });
        }
        return result;
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
