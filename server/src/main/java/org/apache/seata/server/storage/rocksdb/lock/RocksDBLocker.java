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
package org.apache.seata.server.storage.rocksdb.lock;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBLocalLocks;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * RocksDB locker for file store engine.
 */
public class RocksDBLocker extends AbstractLocker {

    private static final byte[] EMPTY_VALUE = new byte[0];
    static final int DEFAULT_LOCK_INDEX_SCAN_BATCH_SIZE = 1024;

    private final RocksDBStoreEngine storeEngine;
    private final RocksDBLocalLocks localLocks;
    private final int lockIndexScanBatchSize;

    public RocksDBLocker(RocksDBStoreEngine storeEngine, RocksDBLocalLocks localLocks) {
        this(storeEngine, localLocks, DEFAULT_LOCK_INDEX_SCAN_BATCH_SIZE);
    }

    RocksDBLocker(RocksDBStoreEngine storeEngine, RocksDBLocalLocks localLocks, int lockIndexScanBatchSize) {
        this.storeEngine = storeEngine;
        this.localLocks = localLocks;
        this.lockIndexScanBatchSize =
                lockIndexScanBatchSize > 0 ? lockIndexScanBatchSize : DEFAULT_LOCK_INDEX_SCAN_BATCH_SIZE;
    }

    boolean wasLastShutdownClean() {
        return storeEngine.wasLastShutdownClean();
    }

    @Override
    public boolean acquireLock(List<RowLock> locks) {
        return acquireLock(locks, true, false);
    }

    @Override
    public boolean acquireLock(List<RowLock> locks, boolean autoCommit, boolean skipCheckLock) {
        if (CollectionUtils.isEmpty(locks)) {
            return true;
        }
        List<LockDO> lockDOs = distinctByRowKey(convertToLockDO(locks));
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(toLockKeys(lockDOs))) {
            List<LockDO> unrepeatedLockDOs = new ArrayList<>();
            boolean failFast = false;

            for (LockDO lockDO : lockDOs) {
                LockDO existingLock = readLock(lockDO);
                if (existingLock == null) {
                    unrepeatedLockDOs.add(lockDO);
                    continue;
                }
                if (StringUtils.equals(existingLock.getXid(), lockDO.getXid())) {
                    continue;
                }

                LOGGER.info(
                        "Global lock on [{}:{}] is holding by xid {} branchId {}",
                        existingLock.getTableName(),
                        existingLock.getPk(),
                        existingLock.getXid(),
                        existingLock.getBranchId());
                if (!autoCommit && existingLock.getStatus() == LockStatus.Rollbacking.getCode()) {
                    failFast = true;
                }
                if (failFast) {
                    throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
                }
                return false;
            }

            if (CollectionUtils.isEmpty(unrepeatedLockDOs)) {
                return true;
            }
            writeLocks(unrepeatedLockDOs);
            return true;
        } catch (StoreException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("AcquireLock error, locks:{}", CollectionUtils.toString(locks), e);
            return false;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            return true;
        }
        List<LockDO> lockDOs = distinctByRowKey(convertToLockDO(locks));
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(toLockKeys(lockDOs));
                WriteBatch batch = new WriteBatch()) {
            for (LockDO lockDO : lockDOs) {
                byte[] lockKey = encodeLockKey(lockDO);
                byte[] indexKey = RocksDBKeyCodec.encodeLockBranchIndex(lockDO.getXid(), lockDO.getBranchId(), lockKey);
                if (readOwnedLock(lockKey, indexKey, lockDO.getXid(), lockDO.getBranchId()) == null) {
                    continue;
                }
                storeEngine.delete(batch, RocksDBColumnFamily.LOCK, lockKey);
                storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexKey);
            }
            storeEngine.write(batch);
            return true;
        } catch (StoreException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("unLock error, locks:{}", CollectionUtils.toString(locks), e);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        if (StringUtils.isBlank(xid) || branchId == null) {
            return true;
        }
        return releaseByIndex(RocksDBKeyCodec.encodeLockBranchIndexBranchPrefix(xid, branchId), xid, branchId);
    }

    @Override
    public boolean releaseLock(String xid) {
        if (StringUtils.isBlank(xid)) {
            return true;
        }
        return releaseByIndex(RocksDBKeyCodec.encodeLockBranchIndexGlobalPrefix(xid), xid, null);
    }

    @Override
    public boolean isLockable(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            return true;
        }
        List<LockDO> lockDOs = distinctByRowKey(convertToLockDO(locks));
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(toLockKeys(lockDOs))) {
            for (LockDO lockDO : lockDOs) {
                LockDO existingLock = readLock(lockDO);
                if (existingLock != null && !StringUtils.equals(existingLock.getXid(), lockDO.getXid())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(locks), e);
            return false;
        }
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        if (StringUtils.isBlank(xid)) {
            return;
        }
        byte[] indexPrefix = RocksDBKeyCodec.encodeLockBranchIndexGlobalPrefix(xid);
        byte[] seekKey = indexPrefix;
        try {
            while (seekKey != null) {
                List<RocksDBStoreEngine.RocksDBEntry> indexEntries =
                        scanLockBranchIndex(seekKey, indexPrefix, lockIndexScanBatchSize);
                if (CollectionUtils.isEmpty(indexEntries)) {
                    break;
                }

                try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(indexValues(indexEntries));
                        WriteBatch batch = new WriteBatch()) {
                    for (RocksDBStoreEngine.RocksDBEntry indexEntry : indexEntries) {
                        byte[] lockKey = indexEntry.getValue();
                        byte[] value = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
                        if (value == null) {
                            continue;
                        }
                        LockDO lockDO = decodeLock(value);
                        if (!StringUtils.equals(lockDO.getXid(), xid)) {
                            continue;
                        }
                        lockDO.setStatus(lockStatus.getCode());
                        storeEngine.put(batch, RocksDBColumnFamily.LOCK, lockKey, encodeLock(lockDO));
                    }
                    storeEngine.write(batch);
                }
                seekKey = nextLockBranchIndexSeekKey(indexPrefix, indexEntries);
            }
        } catch (RocksDBException e) {
            throw new StoreException(e, "update RocksDB lock status failed, xid:" + xid);
        }
    }

    @Override
    public void cleanAllLocks() {
        try (WriteBatch batch = new WriteBatch()) {
            storeEngine.deleteByPrefix(batch, RocksDBColumnFamily.LOCK, EMPTY_VALUE);
            storeEngine.deleteByPrefix(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, EMPTY_VALUE);
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "clean RocksDB locks failed");
        }
    }

    public int cleanOrphanLocks() {
        return cleanOrphanLocks(0).getCleaned();
    }

    public RocksDBLockManager.CleanOrphanLocksResult cleanOrphanLocks(int limit) {
        return cleanOrphanLocks(EMPTY_VALUE, limit);
    }

    public RocksDBLockManager.CleanOrphanLocksResult cleanOrphanLocks(byte[] seekKey, int limit) {
        byte[] actualSeekKey = seekKey == null ? EMPTY_VALUE : Arrays.copyOf(seekKey, seekKey.length);
        LockBranchIndexScanResult scanResult = scanLockBranchIndexWithStats(actualSeekKey, EMPTY_VALUE, limit);
        List<RocksDBStoreEngine.RocksDBEntry> indexEntries = scanResult.getIndexEntries();
        boolean limitReached = scanResult.getStats().isLimitReached();
        byte[] nextSeekKey = limitReached ? nextLockBranchIndexSeekKey(EMPTY_VALUE, indexEntries, limit) : null;
        if (CollectionUtils.isEmpty(indexEntries)) {
            return new RocksDBLockManager.CleanOrphanLocksResult(
                    0, scanResult.getStats().getRowsScanned(), limitReached, nextSeekKey);
        }
        int cleaned = 0;
        Map<String, List<OrphanLockCandidate>> candidatesByXid = new LinkedHashMap<>();
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(indexValues(indexEntries));
                WriteBatch batch = new WriteBatch()) {
            for (RocksDBStoreEngine.RocksDBEntry indexEntry : indexEntries) {
                byte[] lockKey = indexEntry.getValue();
                byte[] lockValue = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
                if (lockValue == null) {
                    storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexEntry.getKey());
                    cleaned++;
                    continue;
                }

                LockDO existingLock = decodeLock(lockValue);
                byte[] expectedIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(
                        existingLock.getXid(), existingLock.getBranchId(), lockKey);
                if (!Arrays.equals(expectedIndexKey, indexEntry.getKey())) {
                    storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexEntry.getKey());
                    cleaned++;
                    continue;
                }

                if (storeEngine.get(
                                RocksDBColumnFamily.BRANCH_SESSION,
                                RocksDBKeyCodec.encodeBranch(existingLock.getXid(), existingLock.getBranchId()))
                        == null) {
                    candidatesByXid
                            .computeIfAbsent(existingLock.getXid(), ignoredXid -> new ArrayList<>())
                            .add(new OrphanLockCandidate(indexEntry.getKey(), lockKey));
                }
            }
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "clean RocksDB orphan locks failed");
        }
        SessionManager sessionManager = SessionHolder.getRootSessionManager();
        for (Map.Entry<String, List<OrphanLockCandidate>> entry : candidatesByXid.entrySet()) {
            cleaned += cleanOrphanCandidates(sessionManager, entry.getKey(), entry.getValue());
        }
        return new RocksDBLockManager.CleanOrphanLocksResult(
                cleaned, scanResult.getStats().getRowsScanned(), limitReached, nextSeekKey);
    }

    private int cleanOrphanCandidates(SessionManager sessionManager, String xid, List<OrphanLockCandidate> candidates) {
        if (!persistentGlobalSessionExists(xid)) {
            return deleteConfirmedOrphans(xid, candidates, true);
        }
        if (sessionManager == null) {
            return 0;
        }
        GlobalSession globalSession = new GlobalSession();
        globalSession.setXid(xid);
        try {
            return sessionManager.lockAndExecute(globalSession, () -> deleteConfirmedOrphans(xid, candidates, false));
        } catch (TransactionException e) {
            throw new StoreException(e, "coordinate RocksDB orphan lock cleanup failed, xid:" + xid);
        }
    }

    private boolean persistentGlobalSessionExists(String xid) {
        return storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid)) != null;
    }

    private int deleteConfirmedOrphans(
            String xid, List<OrphanLockCandidate> candidates, boolean requireMissingGlobalSession) {
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(candidateLockKeys(candidates));
                WriteBatch batch = new WriteBatch()) {
            if (requireMissingGlobalSession
                    && storeEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid)) != null) {
                return 0;
            }
            int cleaned = 0;
            for (OrphanLockCandidate candidate : candidates) {
                byte[] indexedLockKey = storeEngine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, candidate.indexKey);
                if (!Arrays.equals(indexedLockKey, candidate.lockKey)) {
                    continue;
                }
                LockDO currentLock = readOwnedLock(candidate.lockKey, candidate.indexKey, xid, null);
                if (currentLock == null) {
                    storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, candidate.indexKey);
                    cleaned++;
                    continue;
                }
                if (storeEngine.get(
                                RocksDBColumnFamily.BRANCH_SESSION,
                                RocksDBKeyCodec.encodeBranch(currentLock.getXid(), currentLock.getBranchId()))
                        != null) {
                    continue;
                }
                storeEngine.delete(batch, RocksDBColumnFamily.LOCK, candidate.lockKey);
                storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, candidate.indexKey);
                cleaned++;
            }
            storeEngine.write(batch);
            return cleaned;
        } catch (RocksDBException e) {
            throw new StoreException(e, "delete confirmed RocksDB orphan locks failed, xid:" + xid);
        }
    }

    private List<byte[]> candidateLockKeys(List<OrphanLockCandidate> candidates) {
        List<byte[]> lockKeys = new ArrayList<>(candidates.size());
        for (OrphanLockCandidate candidate : candidates) {
            lockKeys.add(candidate.lockKey);
        }
        return lockKeys;
    }

    private List<RocksDBStoreEngine.RocksDBEntry> scanLockBranchIndex(byte[] prefix, int limit) {
        return scanLockBranchIndexWithStats(prefix, prefix, limit).getIndexEntries();
    }

    private List<RocksDBStoreEngine.RocksDBEntry> scanLockBranchIndex(byte[] seekKey, byte[] prefix, int limit) {
        return scanLockBranchIndexWithStats(seekKey, prefix, limit).getIndexEntries();
    }

    private LockBranchIndexScanResult scanLockBranchIndexWithStats(byte[] prefix, int limit) {
        return scanLockBranchIndexWithStats(prefix, prefix, limit);
    }

    private LockBranchIndexScanResult scanLockBranchIndexWithStats(byte[] seekKey, byte[] prefix, int limit) {
        List<RocksDBStoreEngine.RocksDBEntry> indexEntries = new ArrayList<>();
        RocksDBStoreEngine.ScanStats stats = storeEngine.scanByPrefix(
                RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                seekKey,
                prefix,
                limit,
                null,
                (key, value) -> indexEntries.add(new RocksDBStoreEngine.RocksDBEntry(key, value)));
        return new LockBranchIndexScanResult(indexEntries, stats);
    }

    private byte[] nextLockBranchIndexSeekKey(byte[] prefix, List<RocksDBStoreEngine.RocksDBEntry> indexEntries) {
        return nextLockBranchIndexSeekKey(prefix, indexEntries, lockIndexScanBatchSize);
    }

    private byte[] nextLockBranchIndexSeekKey(
            byte[] prefix, List<RocksDBStoreEngine.RocksDBEntry> indexEntries, int batchSize) {
        if (batchSize <= 0 || indexEntries.size() < batchSize) {
            return null;
        }
        byte[] nextSeekKey = RocksDBKeyCodec.prefixEnd(
                indexEntries.get(indexEntries.size() - 1).getKey());
        if (nextSeekKey == null || !RocksDBKeyCodec.startsWith(nextSeekKey, prefix)) {
            return null;
        }
        return nextSeekKey;
    }

    private static final class OrphanLockCandidate {
        private final byte[] indexKey;
        private final byte[] lockKey;

        private OrphanLockCandidate(byte[] indexKey, byte[] lockKey) {
            this.indexKey = Arrays.copyOf(indexKey, indexKey.length);
            this.lockKey = Arrays.copyOf(lockKey, lockKey.length);
        }
    }

    private static final class LockBranchIndexScanResult {
        private final List<RocksDBStoreEngine.RocksDBEntry> indexEntries;
        private final RocksDBStoreEngine.ScanStats stats;

        private LockBranchIndexScanResult(
                List<RocksDBStoreEngine.RocksDBEntry> indexEntries, RocksDBStoreEngine.ScanStats stats) {
            this.indexEntries = indexEntries;
            this.stats = stats;
        }

        private List<RocksDBStoreEngine.RocksDBEntry> getIndexEntries() {
            return indexEntries;
        }

        private RocksDBStoreEngine.ScanStats getStats() {
            return stats;
        }
    }

    private void writeLocks(List<LockDO> lockDOs) {
        try (WriteBatch batch = new WriteBatch()) {
            for (LockDO lockDO : lockDOs) {
                byte[] lockKey = encodeLockKey(lockDO);
                storeEngine.put(batch, RocksDBColumnFamily.LOCK, lockKey, encodeLock(lockDO));
                storeEngine.put(
                        batch,
                        RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                        RocksDBKeyCodec.encodeLockBranchIndex(lockDO.getXid(), lockDO.getBranchId(), lockKey),
                        lockKey);
            }
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write RocksDB locks failed");
        }
    }

    private boolean releaseByIndex(byte[] indexPrefix, String xid, Long branchId) {
        byte[] seekKey = indexPrefix;
        try {
            while (seekKey != null) {
                List<RocksDBStoreEngine.RocksDBEntry> indexEntries =
                        scanLockBranchIndex(seekKey, indexPrefix, lockIndexScanBatchSize);
                if (CollectionUtils.isEmpty(indexEntries)) {
                    break;
                }

                try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(indexValues(indexEntries));
                        WriteBatch batch = new WriteBatch()) {
                    for (RocksDBStoreEngine.RocksDBEntry indexEntry : indexEntries) {
                        byte[] lockKey = indexEntry.getValue();
                        if (readOwnedLock(lockKey, indexEntry.getKey(), xid, branchId) != null) {
                            storeEngine.delete(batch, RocksDBColumnFamily.LOCK, lockKey);
                        }
                        storeEngine.delete(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexEntry.getKey());
                    }
                    storeEngine.write(batch);
                }
                seekKey = nextLockBranchIndexSeekKey(indexPrefix, indexEntries);
            }
            return true;
        } catch (RocksDBException e) {
            throw new StoreException(e, "release RocksDB locks failed");
        }
    }

    private LockDO readOwnedLock(byte[] lockKey, byte[] indexKey, String xid, Long branchId) {
        byte[] lockValue = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
        if (lockValue == null) {
            return null;
        }
        LockDO currentLock = decodeLock(lockValue);
        byte[] expectedIndexKey =
                RocksDBKeyCodec.encodeLockBranchIndex(currentLock.getXid(), currentLock.getBranchId(), lockKey);
        if (!StringUtils.equals(currentLock.getXid(), xid)
                || branchId != null && !Objects.equals(currentLock.getBranchId(), branchId)
                || !Arrays.equals(expectedIndexKey, indexKey)) {
            return null;
        }
        return currentLock;
    }

    private LockDO readLock(LockDO lockDO) {
        byte[] value = storeEngine.get(RocksDBColumnFamily.LOCK, encodeLockKey(lockDO));
        return value == null ? null : decodeLock(value);
    }

    private List<LockDO> distinctByRowKey(List<LockDO> lockDOs) {
        Map<String, LockDO> result = new LinkedHashMap<>();
        for (LockDO lockDO : lockDOs) {
            result.putIfAbsent(lockDO.getRowKey(), lockDO);
        }
        return new ArrayList<>(result.values());
    }

    private Collection<byte[]> toLockKeys(List<LockDO> lockDOs) {
        return lockDOs.stream().map(this::encodeLockKey).collect(Collectors.toList());
    }

    private Collection<byte[]> indexValues(List<RocksDBStoreEngine.RocksDBEntry> entries) {
        return entries.stream().map(RocksDBStoreEngine.RocksDBEntry::getValue).collect(Collectors.toList());
    }

    private byte[] encodeLockKey(LockDO lockDO) {
        return RocksDBKeyCodec.encodeRowLock(lockDO.getResourceId(), lockDO.getTableName(), lockDO.getPk());
    }

    private byte[] encodeLock(LockDO lockDO) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, lockDO.getXid());
        writeLong(out, lockDO.getTransactionId());
        writeLong(out, lockDO.getBranchId());
        writeString(out, lockDO.getResourceId());
        writeString(out, lockDO.getTableName());
        writeString(out, lockDO.getPk());
        writeString(out, lockDO.getRowKey());
        writeInt(out, lockDO.getStatus());
        return RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.LOCK_HOLDER, out.toByteArray());
    }

    private LockDO decodeLock(byte[] value) {
        RocksDBValueCodec.DecodedValue decodedValue = RocksDBValueCodec.decode(value);
        if (decodedValue.getType() != RocksDBValueCodec.ValueType.LOCK_HOLDER) {
            throw new StoreException("unexpected RocksDB value type for lock holder:" + decodedValue.getType());
        }
        ByteBuffer buffer = ByteBuffer.wrap(decodedValue.getPayload());
        LockDO lockDO = new LockDO();
        lockDO.setXid(readString(buffer));
        lockDO.setTransactionId(readLong(buffer));
        lockDO.setBranchId(readLong(buffer));
        lockDO.setResourceId(readString(buffer));
        lockDO.setTableName(readString(buffer));
        lockDO.setPk(readString(buffer));
        lockDO.setRowKey(readString(buffer));
        lockDO.setStatus(readInt(buffer));
        return lockDO;
    }

    private void writeString(ByteArrayOutputStream out, String value) {
        if (value == null) {
            writeInt(out, -1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeInt(out, bytes.length);
        out.write(bytes, 0, bytes.length);
    }

    private String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeLong(ByteArrayOutputStream out, Long value) {
        write(
                out,
                ByteBuffer.allocate(Long.BYTES)
                        .putLong(value == null ? 0L : value)
                        .array());
    }

    private long readLong(ByteBuffer buffer) {
        return buffer.getLong();
    }

    private void writeInt(ByteArrayOutputStream out, Integer value) {
        write(
                out,
                ByteBuffer.allocate(Integer.BYTES)
                        .putInt(value == null ? 0 : value)
                        .array());
    }

    private int readInt(ByteBuffer buffer) {
        return buffer.getInt();
    }

    private void write(ByteArrayOutputStream out, byte[] bytes) {
        out.write(bytes, 0, bytes.length);
    }
}
