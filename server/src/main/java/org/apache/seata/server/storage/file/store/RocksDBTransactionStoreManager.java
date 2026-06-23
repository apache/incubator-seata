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
package org.apache.seata.server.storage.file.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.file.FlushDiskMode;
import org.apache.seata.server.store.AbstractTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.TransactionStoreManager;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.seata.common.DefaultValues.DEFAULT_QUERY_LIMIT;

/**
 * RocksDB transaction store for local file mode.
 */
public class RocksDBTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBTransactionStoreManager.class);

    private static final byte[] EMPTY_VALUE = new byte[0];
    private static final String GLOBAL_PREFIX = "g|";
    private static final String TRANSACTION_PREFIX = "t|";
    private static final String BRANCH_PREFIX = "b|";
    private static final String STATUS_PREFIX = "s|";
    private static final int PADDED_LONG_LENGTH = 19;

    private final Options options;
    private final WriteOptions writeOptions;
    private final RocksDB db;
    private final int logQueryLimit;

    static {
        RocksDB.loadLibrary();
    }

    public RocksDBTransactionStoreManager(String dbPath) throws IOException {
        this(dbPath, StoreConfig.getFlushDiskMode());
    }

    public RocksDBTransactionStoreManager(String dbPath, FlushDiskMode flushDiskMode) throws IOException {
        try {
            File dbDir = new File(dbPath);
            File parent = dbDir.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            options = new Options().setCreateIfMissing(true);
            writeOptions = new WriteOptions().setSync(flushDiskMode == FlushDiskMode.SYNC_MODEL);
            db = RocksDB.open(options, dbPath);
            logQueryLimit = DEFAULT_QUERY_LIMIT;
        } catch (RocksDBException e) {
            throw new IOException("init rocksdb store error, path:" + dbPath, e);
        }
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        try {
            switch (logOperation) {
                case GLOBAL_ADD:
                    return insertGlobalSession((GlobalSession) session);
                case GLOBAL_UPDATE:
                    return updateGlobalSession((GlobalSession) session);
                case GLOBAL_REMOVE:
                    return deleteGlobalSession((GlobalSession) session);
                case BRANCH_ADD:
                    return insertBranchSession((BranchSession) session);
                case BRANCH_UPDATE:
                    return updateBranchSession((BranchSession) session);
                case BRANCH_REMOVE:
                    return deleteBranchSession((BranchSession) session);
                default:
                    throw new StoreException("Unknown LogOperation:" + logOperation.name());
            }
        } catch (RocksDBException e) {
            LOGGER.error("write rocksdb store failed, operation: {}", logOperation, e);
            return false;
        }
    }

    private boolean insertGlobalSession(GlobalSession globalSession) throws RocksDBException {
        byte[] globalKey = globalKey(globalSession.getXid());
        if (db.get(globalKey) != null) {
            return false;
        }
        try (WriteBatch batch = new WriteBatch()) {
            putGlobal(batch, globalSession);
            db.write(writeOptions, batch);
            return true;
        }
    }

    private boolean updateGlobalSession(GlobalSession globalSession) throws RocksDBException {
        GlobalSession previous = readGlobalSession(globalSession.getXid(), false);
        if (previous == null) {
            return false;
        }
        if (globalSession.getExpectedStatus() != null && globalSession.getExpectedStatus() != previous.getStatus()) {
            return false;
        }
        try (WriteBatch batch = new WriteBatch()) {
            deleteStatusIndex(batch, previous);
            putGlobal(batch, globalSession);
            db.write(writeOptions, batch);
            return true;
        }
    }

    private boolean deleteGlobalSession(GlobalSession globalSession) throws RocksDBException {
        GlobalSession previous = readGlobalSession(globalSession.getXid(), false);
        if (previous == null) {
            return true;
        }
        try (WriteBatch batch = new WriteBatch()) {
            batch.delete(globalKey(previous.getXid()));
            batch.delete(transactionKey(previous.getTransactionId()));
            deleteStatusIndex(batch, previous);
            deleteBranches(batch, previous.getXid());
            db.write(writeOptions, batch);
            return true;
        }
    }

    private boolean insertBranchSession(BranchSession branchSession) throws RocksDBException {
        byte[] branchKey = branchKey(branchSession.getXid(), branchSession.getBranchId());
        if (db.get(branchKey) != null) {
            return false;
        }
        db.put(writeOptions, branchKey, branchSession.encode());
        return true;
    }

    private boolean updateBranchSession(BranchSession branchSession) throws RocksDBException {
        byte[] branchKey = branchKey(branchSession.getXid(), branchSession.getBranchId());
        if (db.get(branchKey) == null) {
            return false;
        }
        db.put(writeOptions, branchKey, branchSession.encode());
        return true;
    }

    private boolean deleteBranchSession(BranchSession branchSession) throws RocksDBException {
        db.delete(writeOptions, branchKey(branchSession.getXid(), branchSession.getBranchId()));
        return true;
    }

    private void putGlobal(WriteBatch batch, GlobalSession globalSession) throws RocksDBException {
        batch.put(globalKey(globalSession.getXid()), globalSession.encode());
        batch.put(transactionKey(globalSession.getTransactionId()), bytes(globalSession.getXid()));
        batch.put(statusKey(globalSession), EMPTY_VALUE);
    }

    private void deleteBranches(WriteBatch batch, String xid) throws RocksDBException {
        byte[] prefix = branchPrefix(xid);
        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seek(prefix); iterator.isValid() && startsWith(iterator.key(), prefix); iterator.next()) {
                batch.delete(iterator.key());
            }
            iterator.status();
        }
    }

    private void deleteStatusIndex(WriteBatch batch, GlobalSession globalSession) throws RocksDBException {
        batch.delete(statusKey(globalSession));
    }

    @Override
    public GlobalSession readSession(String xid) {
        return readSession(xid, true);
    }

    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        try {
            return readGlobalSession(xid, withBranchSessions);
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    private GlobalSession readGlobalSession(String xid, boolean withBranchSessions) throws RocksDBException {
        byte[] globalBytes = db.get(globalKey(xid));
        if (globalBytes == null) {
            return null;
        }
        GlobalSession globalSession = decodeGlobalSession(globalBytes, !withBranchSessions);
        if (withBranchSessions) {
            for (BranchSession branchSession : readBranchSessions(xid)) {
                globalSession.add(branchSession);
            }
        }
        return globalSession;
    }

    @Override
    public List<GlobalSession> readSortByTimeoutBeginSessions(boolean withBranchSessions) {
        return readSession(new GlobalStatus[] {GlobalStatus.Begin}, withBranchSessions);
    }

    @Override
    public List<GlobalSession> readSession(GlobalStatus[] statuses, boolean withBranchSessions) {
        return readSession(statuses, withBranchSessions, null);
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        if (StringUtils.isNotBlank(sessionCondition.getXid())) {
            GlobalSession globalSession = readSession(sessionCondition.getXid(), !sessionCondition.isLazyLoadBranch());
            return globalSession == null ? Collections.emptyList() : Collections.singletonList(globalSession);
        }
        if (sessionCondition.getTransactionId() != null) {
            GlobalSession globalSession = readSessionByTransactionId(
                    sessionCondition.getTransactionId(), !sessionCondition.isLazyLoadBranch());
            return globalSession == null ? Collections.emptyList() : Collections.singletonList(globalSession);
        }
        if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readSession(
                    sessionCondition.getStatuses(),
                    !sessionCondition.isLazyLoadBranch(),
                    sessionCondition.getOverTimeAliveMills());
        }
        if (sessionCondition.getOverTimeAliveMills() != null) {
            return readAllSessions(!sessionCondition.isLazyLoadBranch(), sessionCondition.getOverTimeAliveMills());
        }
        return Collections.emptyList();
    }

    private GlobalSession readSessionByTransactionId(long transactionId, boolean withBranchSessions) {
        try {
            byte[] xidBytes = db.get(transactionKey(transactionId));
            if (xidBytes == null) {
                return null;
            }
            return readGlobalSession(new String(xidBytes, StandardCharsets.UTF_8), withBranchSessions);
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    private List<GlobalSession> readSession(
            GlobalStatus[] statuses, boolean withBranchSessions, Long overTimeAliveMills) {
        if (statuses == null || statuses.length == 0 || logQueryLimit <= 0) {
            return Collections.emptyList();
        }
        List<GlobalSession> globalSessions = new ArrayList<>();
        long now = System.currentTimeMillis();
        try (RocksIterator iterator = db.newIterator()) {
            for (GlobalStatus status : statuses) {
                byte[] prefix = statusPrefix(status);
                for (iterator.seek(prefix); iterator.isValid() && startsWith(iterator.key(), prefix); iterator.next()) {
                    GlobalSession globalSession =
                            readGlobalSession(xidFromStatusKey(iterator.key()), withBranchSessions);
                    if (globalSession == null) {
                        continue;
                    }
                    if (overTimeAliveMills != null && now - globalSession.getBeginTime() <= overTimeAliveMills) {
                        break;
                    }
                    globalSessions.add(globalSession);
                    if (globalSessions.size() >= logQueryLimit) {
                        return globalSessions;
                    }
                }
            }
            iterator.status();
            return globalSessions;
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    private List<GlobalSession> readAllSessions(boolean withBranchSessions, Long overTimeAliveMills) {
        List<GlobalSession> globalSessions = new ArrayList<>();
        long now = System.currentTimeMillis();
        byte[] prefix = bytes(GLOBAL_PREFIX);
        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seek(prefix); iterator.isValid() && startsWith(iterator.key(), prefix); iterator.next()) {
                GlobalSession globalSession = decodeGlobalSession(iterator.value(), !withBranchSessions);
                if (overTimeAliveMills != null && now - globalSession.getBeginTime() <= overTimeAliveMills) {
                    continue;
                }
                if (withBranchSessions) {
                    for (BranchSession branchSession : readBranchSessions(globalSession.getXid())) {
                        globalSession.add(branchSession);
                    }
                }
                globalSessions.add(globalSession);
                if (globalSessions.size() >= logQueryLimit) {
                    return globalSessions;
                }
            }
            iterator.status();
            return globalSessions;
        } catch (RocksDBException e) {
            throw new StoreException(e);
        }
    }

    private List<BranchSession> readBranchSessions(String xid) throws RocksDBException {
        List<BranchSession> branchSessions = new ArrayList<>();
        byte[] prefix = branchPrefix(xid);
        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seek(prefix); iterator.isValid() && startsWith(iterator.key(), prefix); iterator.next()) {
                BranchSession branchSession = new BranchSession();
                branchSession.decode(iterator.value());
                branchSessions.add(branchSession);
            }
            iterator.status();
        }
        return branchSessions;
    }

    private GlobalSession decodeGlobalSession(byte[] globalBytes, boolean lazyLoadBranch) {
        GlobalSession decoded = new GlobalSession();
        decoded.decode(globalBytes);
        if (!lazyLoadBranch) {
            return decoded;
        }
        GlobalSession lazySession = new GlobalSession(
                decoded.getApplicationId(),
                decoded.getTransactionServiceGroup(),
                decoded.getTransactionName(),
                decoded.getTimeout(),
                true);
        lazySession.setTransactionId(decoded.getTransactionId());
        lazySession.setXid(decoded.getXid());
        lazySession.setStatus(decoded.getStatus());
        lazySession.setBeginTime(decoded.getBeginTime());
        lazySession.setApplicationData(decoded.getApplicationData());
        return lazySession;
    }

    @Override
    public void shutdown() {
        db.close();
        writeOptions.close();
        options.close();
    }

    private byte[] globalKey(String xid) {
        return bytes(GLOBAL_PREFIX + xid);
    }

    private byte[] transactionKey(long transactionId) {
        return bytes(TRANSACTION_PREFIX + paddedLong(transactionId));
    }

    private byte[] branchKey(String xid, long branchId) {
        return bytes(BRANCH_PREFIX + xid + "|" + paddedLong(branchId));
    }

    private byte[] branchPrefix(String xid) {
        return bytes(BRANCH_PREFIX + xid + "|");
    }

    private byte[] statusKey(GlobalSession globalSession) {
        return bytes(statusPrefixString(globalSession.getStatus())
                + paddedLong(globalSession.getBeginTime())
                + "|"
                + globalSession.getXid());
    }

    private byte[] statusPrefix(GlobalStatus status) {
        return bytes(statusPrefixString(status));
    }

    private String statusPrefixString(GlobalStatus status) {
        return STATUS_PREFIX + status.getCode() + "|";
    }

    private String xidFromStatusKey(byte[] key) {
        String keyString = new String(key, StandardCharsets.UTF_8);
        return keyString.substring(keyString.lastIndexOf('|') + 1);
    }

    private String paddedLong(long value) {
        return String.format("%0" + PADDED_LONG_LENGTH + "d", value);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
