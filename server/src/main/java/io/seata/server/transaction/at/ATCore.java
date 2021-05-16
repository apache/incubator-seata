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
package io.seata.server.transaction.at;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.RemotingServer;
import io.seata.server.coordinator.AbstractCore;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionHolder;


import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflict;
import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * The type at core.
 *
 * @author ph3636
 */
public class ATCore extends AbstractCore {

    public ATCore(RemotingServer remotingServer) {
        super(remotingServer);
    }

    @Override
    public BranchType getHandleBranchType() {
        return BranchType.AT;
    }

    @Override
    protected void branchSessionLock(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        if (!branchSession.lock()) {
            Set<String> xids = lockManager.getLockOwners(branchSession);
            if (CollectionUtils.isNotEmpty(xids)) {
                SessionCondition condition = new SessionCondition();
                List<String> list = new ArrayList<>();
                list.addAll(xids);
                condition.setStatuses(new GlobalStatus[] {GlobalStatus.RollbackRetrying, GlobalStatus.RollbackFailed,
                    GlobalStatus.Rollbacking, GlobalStatus.TimeoutRollbacking, GlobalStatus.TimeoutRollbackRetrying,
                    GlobalStatus.TimeoutRollbackFailed});
                condition.setXids(list);
                condition.setLimit(1);
              if(CollectionUtils.isNotEmpty(SessionHolder.getRootSessionManager().findGlobalSessions(condition))){
                  throw new BranchTransactionException(LockKeyConflictFailFast,
                      String.format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                          branchSession.getBranchId()));
              }
            }
            throw new BranchTransactionException(LockKeyConflict,
                String.format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                    branchSession.getBranchId()));
        }
    }

    @Override
    protected void branchSessionUnlock(BranchSession branchSession) throws TransactionException {
        branchSession.unlock();
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return lockManager.isLockable(xid, resourceId, lockKeys);
    }
}
