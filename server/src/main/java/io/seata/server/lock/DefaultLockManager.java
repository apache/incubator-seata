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
package io.seata.server.lock;

import java.util.List;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.lock.RowLock;
import io.seata.server.session.BranchSession;

/**
 * The type Default lock manager.
 *
 * @author zhangsen
 * @data 2019 -05-15
 */
public class DefaultLockManager extends AbstractLockManager {

    private static Locker locker = null;

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        String lockKey = branchSession.getLockKey();
        if (StringUtils.isNullOrEmpty(lockKey)) {
            //no lock
            return true;
        }
        //get locks of branch
        List<RowLock> locks = collectRowLocks(branchSession);
        if (CollectionUtils.isEmpty(locks)) {
            //no lock
            return true;
        }
        return getLocker(branchSession).acquireLock(locks);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        List<RowLock> locks = collectRowLocks(branchSession);
        if (CollectionUtils.isEmpty(locks)) {
            //no lock
            return true;
        }
        try {
            return getLocker(branchSession).releaseLock(locks);
        } catch (Exception t) {
            LOGGER.error("unLock error, branchSession:" + branchSession, t);
            return false;
        }
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        List<RowLock> locks = collectRowLocks(lockKey, resourceId, xid);
        try {
            return getLocker().isLockable(locks);
        } catch (Exception t) {
            LOGGER.error("isLockable error, xid:" + xid + ", resourceId:" + resourceId + ", lockKey:" + lockKey, t);
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
    protected Locker getLocker(BranchSession branchSession) {
        return LockerFactory.get(branchSession);
    }

}
