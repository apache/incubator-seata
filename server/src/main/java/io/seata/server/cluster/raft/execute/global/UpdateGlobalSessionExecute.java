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
package io.seata.server.cluster.raft.execute.global;

import io.seata.core.model.GlobalStatus;
import io.seata.core.model.LockStatus;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.cluster.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.cluster.raft.sync.msg.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;

/**
 * @author jianbin.chen
 */
public class UpdateGlobalSessionExecute extends AbstractRaftMsgExecute {

    @Override
    public Boolean execute(RaftSessionSyncMsg sessionSyncMsg) throws Throwable {
        RaftSessionManager raftSessionManager = (RaftSessionManager) SessionHolder.getRootSessionManager(sessionSyncMsg.getGroup());
        GlobalTransactionDO globalTransactionDO = sessionSyncMsg.getGlobalSession();
        GlobalSession globalSession = raftSessionManager.findGlobalSession(globalTransactionDO.getXid());
        if (globalSession != null) {
            globalSession.setStatus(GlobalStatus.get(globalTransactionDO.getStatus()));
            if (GlobalStatus.RollbackRetrying.equals(globalSession.getStatus())
                || GlobalStatus.Rollbacking.equals(globalSession.getStatus())
                || GlobalStatus.TimeoutRollbacking.equals(globalSession.getStatus())) {
                globalSession.getBranchSessions().parallelStream()
                    .forEach(branchSession -> branchSession.setLockStatus(LockStatus.Rollbacking));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("xid: {}, change status: {}", globalSession.getXid(), globalSession.getStatus());
            }
        }
        return true;
    }
}
