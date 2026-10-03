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
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
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

    private final RocksDBStoreEngine storeEngine;
    private final RocksDBLocalLocks localLocks;

    public RocksDBLocker(RocksDBStoreEngine storeEngine, RocksDBLocalLocks localLocks) {
        this.storeEngine = storeEngine;
        this.localLocks = localLocks;
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
                LockDO existingLock = readLock(lockDO);
                if (existingLock == null
                        || !StringUtils.equals(existingLock.getXid(), lockDO.getXid())
                        || !Objects.equals(lockDO.getBranchId(), existingLock.getBranchId())) {
                    continue;
                }
                byte[] lockKey = encodeLockKey(lockDO);
                batch.delete(storeEngine.handle(RocksDBColumnFamily.LOCK), lockKey);
                batch.delete(
                        storeEngine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX),
                        RocksDBKeyCodec.encodeLockBranchIndex(lockDO.getXid(), lockDO.getBranchId(), lockKey));
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
        List<RocksDBStoreEngine.RocksDBEntry> indexEntries = storeEngine.prefixScan(
                RocksDBColumnFamily.LOCK_BRANCH_INDEX, RocksDBKeyCodec.encodeLockBranchIndexGlobalPrefix(xid));
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
                batch.put(storeEngine.handle(RocksDBColumnFamily.LOCK), lockKey, encodeLock(lockDO));
            }
            storeEngine.write(batch);
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
        List<RocksDBStoreEngine.RocksDBEntry> indexEntries =
                storeEngine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, EMPTY_VALUE);
        if (CollectionUtils.isEmpty(indexEntries)) {
            return 0;
        }
        int cleaned = 0;
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(indexValues(indexEntries));
                WriteBatch batch = new WriteBatch()) {
            for (RocksDBStoreEngine.RocksDBEntry indexEntry : indexEntries) {
                byte[] lockKey = indexEntry.getValue();
                byte[] lockValue = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
                if (lockValue == null) {
                    batch.delete(storeEngine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX), indexEntry.getKey());
                    cleaned++;
                    continue;
                }

                LockDO existingLock = decodeLock(lockValue);
                byte[] expectedIndexKey = RocksDBKeyCodec.encodeLockBranchIndex(
                        existingLock.getXid(), existingLock.getBranchId(), lockKey);
                if (!Arrays.equals(expectedIndexKey, indexEntry.getKey())) {
                    batch.delete(storeEngine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX), indexEntry.getKey());
                    cleaned++;
                    continue;
                }

                if (storeEngine.get(
                                RocksDBColumnFamily.BRANCH_SESSION,
                                RocksDBKeyCodec.encodeBranch(existingLock.getXid(), existingLock.getBranchId()))
                        == null) {
                    batch.delete(storeEngine.handle(RocksDBColumnFamily.LOCK), lockKey);
                    batch.delete(storeEngine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX), indexEntry.getKey());
                    cleaned++;
                }
            }
            storeEngine.write(batch);
            return cleaned;
        } catch (RocksDBException e) {
            throw new StoreException(e, "clean RocksDB orphan locks failed");
        }
    }

    private void writeLocks(List<LockDO> lockDOs) {
        try (WriteBatch batch = new WriteBatch()) {
            for (LockDO lockDO : lockDOs) {
                byte[] lockKey = encodeLockKey(lockDO);
                batch.put(storeEngine.handle(RocksDBColumnFamily.LOCK), lockKey, encodeLock(lockDO));
                batch.put(
                        storeEngine.handle(RocksDBColumnFamily.LOCK_BRANCH_INDEX),
                        RocksDBKeyCodec.encodeLockBranchIndex(lockDO.getXid(), lockDO.getBranchId(), lockKey),
                        lockKey);
            }
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write RocksDB locks failed");
        }
    }

    private boolean releaseByIndex(byte[] indexPrefix, String xid, Long branchId) {
        List<RocksDBStoreEngine.RocksDBEntry> indexEntries =
                storeEngine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexPrefix);
        if (CollectionUtils.isEmpty(indexEntries)) {
            return true;
        }
        try (RocksDBLocalLocks.LockScope ignored = localLocks.lockAll(indexValues(indexEntries));
                WriteBatch batch = new WriteBatch()) {
            for (RocksDBStoreEngine.RocksDBEntry indexEntry : indexEntries) {
                byte[] lockKey = indexEntry.getValue();
                byte[] lockValue = storeEngine.get(RocksDBColumnFamily.LOCK, lockKey);
                if (lockValue != null) {
                    LockDO existingLock = decodeLock(lockValue);
                    if (StringUtils.equals(existingLock.getXid(), xid)
                            && (branchId == null || branchId.equals(existingLock.getBranchId()))) {
                        batch.delete(storeEngine.handle(RocksDBColumnFamily.LOCK), lockKey);
                    }
                }
            }
            storeEngine.deleteByPrefix(batch, RocksDBColumnFamily.LOCK_BRANCH_INDEX, indexPrefix);
            storeEngine.write(batch);
            return true;
        } catch (RocksDBException e) {
            throw new StoreException(e, "release RocksDB locks failed");
        }
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
