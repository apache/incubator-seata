package io.seata.server.lock.redis;

import io.seata.core.exception.TransactionException;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.GlobalSession;

public class RedisLockManager  extends AbstractLockManager {

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        try {
            return getLocker().releaseLock(branchSession.getXid(), branchSession.getBranchId());
        } catch (Exception t) {
            LOGGER.error("unLock error, xid {}, branchId:{}", branchSession.getXid(), branchSession.getBranchId(), t);
            return false;
        }
    }
}
