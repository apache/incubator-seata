/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.server.session;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.server.lock.LockManagerFactory;
import com.alibaba.fescar.server.store.SessionStorable;

/**
 * The type Branch session.
 */
public class BranchSession implements Lockable, Comparable<BranchSession>, SessionStorable {

    private long transactionId;

    private long branchId;

    private String resourceGroupId;

    private String resourceId;

    private String lockKey;

    private BranchType branchType;

    private BranchStatus status = BranchStatus.Unknown;

    private String applicationId;

    private String txServiceGroup;

    private String clientId;

    private String applicationData;

    private ConcurrentHashMap<Map<String, Long>, Set<String>> lockHolder = new ConcurrentHashMap<Map<String, Long>, Set<String>>();

    /**
     * Gets application data.
     *
     * @return the application data
     */
    public String getApplicationData() {
        return applicationData;
    }

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    /**
     * Gets resource group id.
     *
     * @return the resource group id
     */
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Sets resource group id.
     *
     * @param resourceGroupId the resource group id
     */
    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets tx service group.
     *
     * @return the tx service group
     */
    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    /**
     * Sets tx service group.
     *
     * @param txServiceGroup the tx service group
     */
    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    /**
     * Gets client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets client id.
     *
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets resource id.
     *
     * @return the resource id
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets resource id.
     *
     * @param resourceId the resource id
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Gets lock key.
     *
     * @return the lock key
     */
    public String getLockKey() {
        return lockKey;
    }

    /**
     * Sets lock key.
     *
     * @param lockKey the lock key
     */
    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    /**
     * Gets branch type.
     *
     * @return the branch type
     */
    public BranchType getBranchType() {
        return branchType;
    }

    /**
     * Sets branch type.
     *
     * @param branchType the branch type
     */
    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public BranchStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    void setStatus(BranchStatus status) {
        this.status = status;
    }

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    public String toString() {
        return "BR:" + branchId + "/" + transactionId;
    }

    @Override
    public int compareTo(BranchSession o) {
        return this.branchId < o.branchId ? -1 : (this.branchId > o.branchId ? 1 : 0);
    }

    /**
     * Gets lock holder.
     *
     * @return the lock holder
     */
    public ConcurrentHashMap<Map<String, Long>, Set<String>> getLockHolder() {
        return lockHolder;
    }

    @Override
    public boolean lock() throws TransactionException {
        return LockManagerFactory.get().acquireLock(this);
    }

    @Override
    public boolean unlock() throws TransactionException {
        if (lockHolder.size() == 0) {
            return true;
        }
        Iterator<Map.Entry<Map<String, Long>, Set<String>>> it = lockHolder.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Map<String, Long>, Set<String>> entry = it.next();
            Map<String, Long> bucket = entry.getKey();
            Set<String> keys = entry.getValue();
            synchronized (bucket) {
                for (String key : keys) {
                    Long v = bucket.get(key);
                    if (v == null) {
                        continue;
                    }
                    if (v.longValue() == getTransactionId()) {
                        bucket.remove(key);
                    }
                }
            }
        }
        lockHolder.clear();
        return true;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
        byteBuffer.putLong(transactionId);
        byteBuffer.putLong(branchId);
        if (null != resourceId) {
            byte[] resourceIdBytes = resourceId.getBytes();
            byteBuffer.putInt(resourceIdBytes.length);
            byteBuffer.put(resourceIdBytes);
        } else {
            byteBuffer.putInt(0);
        }
        if (null != lockKey) {
            byte[] lockKeyBytes = lockKey.getBytes();
            byteBuffer.putInt(lockKeyBytes.length);
            byteBuffer.put(lockKeyBytes);
        } else {
            byteBuffer.putInt(0);
        }
        if (null != applicationId) {
            byte[] applicationIdBytes = applicationId.getBytes();
            byteBuffer.putShort((short)applicationIdBytes.length);
            byteBuffer.put(applicationIdBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != txServiceGroup) {
            byte[] txServiceGroupBytes = txServiceGroup.getBytes();
            byteBuffer.putShort((short)txServiceGroupBytes.length);
            byteBuffer.put(txServiceGroupBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != clientId) {
            byte[] clientIdBytes = clientId.getBytes();
            byteBuffer.putShort((short)clientIdBytes.length);
            byteBuffer.put(clientIdBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != applicationData) {
            byte[] applicationDataBytes = applicationData.getBytes();
            byteBuffer.putInt(applicationDataBytes.length);
            byteBuffer.put(applicationDataBytes);
        } else {
            byteBuffer.putInt(0);
        }
        byteBuffer.flip();
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    @Override
    public void decode(byte[] a) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(a);
        this.transactionId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
        int resourceLen = byteBuffer.getInt();
        if (resourceLen > 0) {
            byte[] byResource = new byte[resourceLen];
            byteBuffer.get(byResource);
            this.resourceId = new String(byResource);
        }
        int lockKeyLen = byteBuffer.getInt();
        if (lockKeyLen > 0) {
            byte[] byLockKey = new byte[lockKeyLen];
            byteBuffer.get(byLockKey);
            this.lockKey = new String(byLockKey);
        }
        short applicationIdLen = byteBuffer.getShort();
        if (applicationIdLen > 0) {
            byte[] byApplicationId = new byte[applicationIdLen];
            byteBuffer.get(byApplicationId);
            this.applicationId = new String(byApplicationId);
        }
        short txServiceGroupLen = byteBuffer.getShort();
        if (txServiceGroupLen > 0) {
            byte[] byServiceGroup = new byte[txServiceGroupLen];
            byteBuffer.get(byServiceGroup);
            this.txServiceGroup = new String(byServiceGroup);
        }
        short clientIdLen = byteBuffer.getShort();
        if (clientIdLen > 0) {
            byte[] byClientId = new byte[clientIdLen];
            byteBuffer.get(byClientId);
            this.clientId = new String(byClientId);
        }
        int applicationDataLen = byteBuffer.getInt();
        if (applicationDataLen > 0) {
            byte[] byApplicationData = new byte[applicationDataLen];
            byteBuffer.get(byApplicationData);
            this.applicationData = new String(byApplicationData);
        }

    }

}
