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
package io.seata.server.cluster.raft.execute.lock;

import io.seata.server.cluster.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.cluster.raft.sync.msg.RaftSessionSyncMsg;

/**
 * @author jianbin.chen
 */
public class BranchReleaseLockExecute extends AbstractRaftMsgExecute {

    @Override
    public Boolean execute(RaftBaseMsg syncMsg) throws Throwable {
        RaftSessionSyncMsg  sessionSyncMsg = (RaftSessionSyncMsg)syncMsg;
        GlobalSession globalSession =
            SessionHolder.getRootSessionManager().findGlobalSession(sessionSyncMsg.getBranchSession().getXid());
        BranchSession branchSession = globalSession.getBranch(sessionSyncMsg.getBranchSession().getBranchId());
        if (branchSession != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("releaseBranchSessionLock xid: {}", globalSession.getXid());
            }
            return raftLockManager.localReleaseLock(branchSession);
        }
        return false;
    }

}
