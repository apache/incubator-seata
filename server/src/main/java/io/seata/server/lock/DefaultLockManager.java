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

import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.StoreMode;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

/**
 * The type Default lock manager.
 *
 * @author zhangsen
 * @data 2019 -05-15
 */
public class DefaultLockManager extends AbstractLockManager {

    private static Locker locker = null;

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        if (branchSession == null) {
            throw new IllegalArgumentException("branchSession can't be null for memory/file locker.");
        }
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
        if (branchSession == null) {
            throw new IllegalArgumentException("branchSession can't be null for memory/file locker.");
        }
        List<RowLock> locks = collectRowLocks(branchSession);
        try {
            return this.doReleaseLock(locks, branchSession);
        } catch (Exception t) {
            LOGGER.error("unLock error, branchSession:" + branchSession, t);
            return false;
        }
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        ArrayList<BranchSession> branchSessions = globalSession.getBranchSessions();
        String storeMode = CONFIG.getConfig(ConfigurationKeys.STORE_MODE);
        if (StoreMode.DB.name().equalsIgnoreCase(storeMode)) {
            List<RowLock> locks = new ArrayList<>();
            for (BranchSession branchSession : branchSessions) {
                locks.addAll(collectRowLocks(branchSession));
            }
            try {
                return this.doReleaseLock(locks, null);
            } catch (Exception t) {
                LOGGER.error("unLock globalSession error, xid:{}", globalSession.getXid(), t);
                return false;
            }
        } else {
            boolean releaseLockResult = true;
            for (BranchSession branchSession : branchSessions) {
                if (!this.releaseLock(branchSession)) {
                    releaseLockResult = false;
                }
            }
            return releaseLockResult;
        }
    }

    private boolean doReleaseLock(List<RowLock> locks, BranchSession branchSession) {
        if (CollectionUtils.isEmpty(locks)) {
            //no lock
            return true;
        }
        return getLocker(branchSession).releaseLock(locks);
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
