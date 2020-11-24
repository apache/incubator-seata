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
package io.seata.server.storage.raft.lock;

import java.util.ArrayList;
import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.raft.RaftServerFactory;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.lock.FileLocker;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;


import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ACQUIRE_LOCK;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;

/**
 * @author funkye
 */
@LoadLevel(name = "raft")
public class RaftLockManager extends AbstractLockManager {

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return new FileLocker(branchSession);
    }

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ACQUIRE_LOCK, branchTransactionDO);
            RaftTaskUtil.createTask(raftSyncMsg);
        }
        return super.acquireLock(branchSession);
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(RELEASE_GLOBAL_SESSION_LOCK, globalTransactionDO);
            RaftTaskUtil.createTask(raftSyncMsg);
        }
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
