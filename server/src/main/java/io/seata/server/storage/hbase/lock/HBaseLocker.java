package io.seata.server.storage.hbase.lock;

import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockStore;

import java.util.List;

/**
 * ClassName: HBaseLocker
 * Description:
 *
 * @author haishin
 */
public class HBaseLocker extends AbstractLocker {

    private LockStore lockStore;

    /**
     * Instantiates a new Data base locker.
     */
    public HBaseLocker() {
        lockStore = new LockStoreHBaseDao();
    }

    @Override
    public boolean acquireLock(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
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
            // no lock
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

    /**
     * Sets lock store.
     *
     * @param lockStore the lock store
     */
    public void setLockStore(LockStore lockStore) {
        this.lockStore = lockStore;
    }
}
