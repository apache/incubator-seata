package io.seata.core.lock;

import io.seata.common.loader.LoadLevel;

/**
 * @author zhangsen
 * @data 2019-05-15
 */
@LoadLevel(name = "local_db")
public class LocalDBLocker extends AbstractLocker {

    @Override
    public boolean acquireLock(RowLock... rowLock) {
        return false;
    }

    @Override
    public boolean unLock(RowLock... rowLock) {
        return false;
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) {
        return false;
    }
}
