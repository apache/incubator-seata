package io.seata.server.lock.redis;

import java.util.List;

import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockStore;

public class RedisLocker extends AbstractLocker {

    @Override
    public boolean acquireLock(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            //no lock
            return true;
        }
        try {
            return lockStore.acquireLock(convertToLockDO(locks));
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
            //no lock
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
            return lockStore.unLock(xid, branchId);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchId error, xid {}, branchId:{}", xid, branchId, t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            //no lock
            return true;
        }
        try {
            return lockStore.unLock(xid, branchIds);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchIds error, xid {}, branchIds:{}", xid, CollectionUtils.toString(branchIds), t);
            return false;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> locks) {
        try {
            return lockStore.isLockable(convertToLockDO(locks));
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }
}
