/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.core.lock;

import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.CollectionUtils;
import io.seata.core.store.LockDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract locker.
 *
 * @author zhangsen
 */
public abstract class AbstractLocker implements Locker {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractLocker.class);

    /**
     * The constant LOCK_SPLIT.
     */
    protected static final String LOCK_SPLIT = "^^^";

    /**
     * Convert to lock do list.
     *
     * @param locks the locks
     * @return the list
     */
    protected List<LockDO> convertToLockDO(List<RowLock> locks) {
        List<LockDO> lockDOs = new ArrayList<>();
        if (CollectionUtils.isEmpty(locks)) {
            return lockDOs;
        }
        for (RowLock rowLock : locks) {
            LockDO lockDO = new LockDO();
            lockDO.setBranchId(rowLock.getBranchId());
            lockDO.setPk(rowLock.getPk());
            lockDO.setResourceId(rowLock.getResourceId());
            lockDO.setRowKey(getRowKey(rowLock.getResourceId(), rowLock.getTableName(), rowLock.getPk()));
            lockDO.setXid(rowLock.getXid());
            lockDO.setTransactionId(rowLock.getTransactionId());
            lockDO.setTableName(rowLock.getTableName());
            lockDOs.add(lockDO);
        }
        return lockDOs;
    }

    /**
     * Get row key string.
     *
     * @param resourceId the resource id
     * @param tableName  the table name
     * @param pk         the pk
     * @return the string
     */
    protected String getRowKey(String resourceId, String tableName, String pk) {
        return new StringBuilder().append(resourceId).append(LOCK_SPLIT).append(tableName).append(LOCK_SPLIT).append(pk)
            .toString();
    }

    @Override
    public void cleanAllLocks() {

    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        return false;
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        return false;
    }

}
