package io.seata.server.storage.hbase.lock;

import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassName: HBaseLockManager
 * Description:
 *
 * @author haishin
 */
@LoadLevel(name = "hbase")
public class HBaseLockManager extends AbstractLockManager implements Initialize {

    /**
     * The locker.
     */
    private Locker locker;

    @Override
    public void init() {
        locker = new HBaseLocker();
    }

    @Override
    protected Locker getLocker(BranchSession branchSession) {
        return locker;
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        List<BranchSession> branchSessions = globalSession.getBranchSessions();
        if (CollectionUtils.isEmpty(branchSessions)) {
            return true;
        }
        List<Long> branchIds = branchSessions.stream().map(BranchSession::getBranchId).collect(Collectors.toList());
        try {
            return getLocker().releaseLock(globalSession.getXid(), branchIds);
        } catch (Exception t) {
            LOGGER.error("unLock globalSession error, xid:{} branchIds:{}", globalSession.getXid(),
                    CollectionUtils.toString(branchIds), t);
            return false;
        }
    }
}
