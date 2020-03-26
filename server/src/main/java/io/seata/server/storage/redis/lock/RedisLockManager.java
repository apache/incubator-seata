package io.seata.server.storage.redis.lock;

import java.util.ArrayList;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

public class RedisLockManager extends AbstractLockManager {

    @Override public Locker getLocker(BranchSession branchSession) {
        return new RedisLocker(branchSession);
    }

    @Override public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        ArrayList<BranchSession> branchSessions = globalSession.getBranchSessions();
        boolean releaseLockResult = true;
        for (BranchSession branchSession : branchSessions) {
            if (!this.releaseLock(branchSession)) {
                releaseLockResult = false;
            }
        }
        return releaseLockResult;
    }
}