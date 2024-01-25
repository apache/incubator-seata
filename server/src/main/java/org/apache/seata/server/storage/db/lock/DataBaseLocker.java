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
package org.apache.seata.server.storage.db.lock;

import java.util.List;
import javax.sql.DataSource;
import org.apache.seata.common.exception.DataAccessException;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockStore;

/**
 * The type Data base locker.
 *
 */
public class DataBaseLocker extends AbstractLocker {

    private LockStore lockStore;

    /**
     * Instantiates a new Data base locker.
     */
    public DataBaseLocker() {
    }

    /**
     * Instantiates a new Data base locker.
     *
     * @param logStoreDataSource the log store data source
     */
    public DataBaseLocker(DataSource logStoreDataSource) {
        lockStore = new LockStoreDataBaseDAO(logStoreDataSource);
    }

    @Override
    public boolean acquireLock(List<RowLock> locks) {
        return acquireLock(locks, true, false);
    }

    @Override
    public boolean acquireLock(List<RowLock> locks, boolean autoCommit, boolean skipCheckLock) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.acquireLock(convertToLockDO(locks), autoCommit, skipCheckLock);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("AcquireLock error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.unLock(convertToLockDO(locks));
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        try {
            return lockStore.unLock(branchId);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchId error, xid {}, branchId:{}", xid, branchId, t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid) {
        try {
            return lockStore.unLock(xid);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchIds error, xid {}", xid, t);
            return false;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.isLockable(convertToLockDO(locks));
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        lockStore.updateLockStatus(xid, lockStatus);
    }

    /**
     * Sets lock store.
     *
     * @param lockStore the lock store
     */
    public void setLockStore(LockStore lockStore) {
        this.lockStore = lockStore;
    }

}
