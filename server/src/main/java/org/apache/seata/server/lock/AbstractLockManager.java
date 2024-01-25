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
package org.apache.seata.server.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.session.BranchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract lock manager.
 *
 */
public abstract class AbstractLockManager implements LockManager {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockManager.class);

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        return acquireLock(branchSession, true, false);
    }

    @Override
    public boolean acquireLock(BranchSession branchSession, boolean autoCommit, boolean skipCheckLock) throws TransactionException {
        if (branchSession == null) {
            throw new IllegalArgumentException("branchSession can't be null for memory/file locker.");
        }
        String lockKey = branchSession.getLockKey();
        if (StringUtils.isNullOrEmpty(lockKey)) {
            // no lock
            return true;
        }
        // get locks of branch
        List<RowLock> locks = collectRowLocks(branchSession);
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        return getLocker(branchSession).acquireLock(locks, autoCommit, skipCheckLock);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        if (branchSession == null) {
            throw new IllegalArgumentException("branchSession can't be null for memory/file locker.");
        }
        List<RowLock> locks = collectRowLocks(branchSession);
        try {
            return getLocker(branchSession).releaseLock(locks);
        } catch (Exception t) {
            LOGGER.error("unLock error, branchSession:{}", branchSession, t);
            return false;
        }
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        if (StringUtils.isBlank(lockKey)) {
            // no lock
            return true;
        }
        List<RowLock> locks = collectRowLocks(lockKey, resourceId, xid);
        try {
            return getLocker().isLockable(locks);
        } catch (Exception t) {
            LOGGER.error("isLockable error, xid:{} resourceId:{}, lockKey:{}", xid, resourceId, lockKey, t);
            return false;
        }
    }


    @Override
    public void cleanAllLocks() throws TransactionException {
        getLocker().cleanAllLocks();
    }

    /**
     * Gets locker.
     *
     * @return the locker
     */
    protected Locker getLocker() {
        return getLocker(null);
    }

    /**
     * Gets locker.
     *
     * @param branchSession the branch session
     * @return the locker
     */
    protected abstract Locker getLocker(BranchSession branchSession);

    @Override
    public List<RowLock> collectRowLocks(BranchSession branchSession) {
        if (branchSession == null || StringUtils.isBlank(branchSession.getLockKey())) {
            return Collections.emptyList();
        }

        String lockKey = branchSession.getLockKey();
        String resourceId = branchSession.getResourceId();
        String xid = branchSession.getXid();
        long transactionId = branchSession.getTransactionId();
        long branchId = branchSession.getBranchId();

        return collectRowLocks(lockKey, resourceId, xid, transactionId, branchId);
    }

    /**
     * Collect row locks list.
     *
     * @param lockKey    the lock key
     * @param resourceId the resource id
     * @param xid        the xid
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid) {
        return collectRowLocks(lockKey, resourceId, xid, XID.getTransactionId(xid), null);
    }

    /**
     * Collect row locks list.
     *
     * @param lockKey       the lock key
     * @param resourceId    the resource id
     * @param xid           the xid
     * @param transactionId the transaction id
     * @param branchID      the branch id
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid, Long transactionId,
        Long branchID) {
        List<RowLock> locks = new ArrayList<>();

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return locks;
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if (StringUtils.isBlank(mergedPKs)) {
                return locks;
            }
            String[] pks = mergedPKs.split(",");
            if (pks == null || pks.length == 0) {
                return locks;
            }
            for (String pk : pks) {
                if (StringUtils.isNotBlank(pk)) {
                    RowLock rowLock = new RowLock();
                    rowLock.setXid(xid);
                    rowLock.setTransactionId(transactionId);
                    rowLock.setBranchId(branchID);
                    rowLock.setTableName(tableName);
                    rowLock.setPk(pk);
                    rowLock.setResourceId(resourceId);
                    locks.add(rowLock);
                }
            }
        }
        return locks;
    }
    
    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        this.getLocker().updateLockStatus(xid, lockStatus);
    }

}
