package io.seata.server.transaction.at;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.coordinator.AbstractCore;

/**
 * Created by txg on 2019-12-24.
 */
public class ATCore extends AbstractCore {

    public ATCore(ServerMessageSender messageSender) {
        super(messageSender);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return lockManager.isLockable(xid, resourceId, lockKeys);
    }
}
