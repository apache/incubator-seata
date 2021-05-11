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
package io.seata.server.raft.execute.lock;

import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;

/**
 * @author jianbin.chen
 */
public class AcquireLockExecute extends AbstractRaftMsgExecute {

    public AcquireLockExecute(RaftSessionSyncMsg sessionSyncMsg, RaftSessionManager raftSessionManager) {
        super(sessionSyncMsg, raftSessionManager);
    }

    @Override
    public Boolean execute(Object... args) throws Throwable {
        GlobalSession globalSession =
            SessionHolder.getRootSessionManager().findGlobalSession(sessionSyncMsg.getBranchSession().getXid());
        BranchSession branchSession = globalSession.getBranch(sessionSyncMsg.getBranchSession().getBranchId());
        boolean include = false;
        if (branchSession != null) {
            include = true;
            branchSession.setLockKey(sessionSyncMsg.getBranchSession().getLockKey());
        } else {
            branchSession = SessionConverter.convertBranchSession(sessionSyncMsg.getBranchSession());
        }
        Boolean owner = LockerManagerFactory.getLockManager().acquireLock(branchSession);
        if (owner && !include) {
            globalSession.add(branchSession);
        }
        return owner;
    }
}
