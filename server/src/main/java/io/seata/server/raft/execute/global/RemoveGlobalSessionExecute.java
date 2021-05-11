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
package io.seata.server.raft.execute.global;

import io.seata.core.model.GlobalStatus;
import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;

/**
 * @author jianbin.chen
 */
public class RemoveGlobalSessionExecute extends AbstractRaftMsgExecute {

    private boolean root;

    public RemoveGlobalSessionExecute(RaftSessionSyncMsg sessionSyncMsg, RaftSessionManager raftSessionManager,
        boolean root) {
        super(sessionSyncMsg, raftSessionManager);
        this.root = root;
    }

    @Override
    public Boolean execute(Object... args) throws Throwable {
        GlobalSession globalSession = raftSessionManager.findGlobalSession(sessionSyncMsg.getGlobalSession().getXid());
        if (globalSession != null) {
            if (globalSession != null) {
                if (root) {
                    globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                    GlobalStatus status = globalSession.getStatus();
                    switch (status) {
                        case Rollbacked:
                            SessionHelper.endRollbacked(globalSession);
                            break;
                        case Committed:
                            SessionHelper.endCommitted(globalSession);
                            break;
                        case CommitFailed:
                            SessionHelper.endCommitFailed(globalSession);
                            break;
                        case RollbackFailed:
                            SessionHelper.endRollbackFailed(globalSession);
                            break;
                        default:
                            break;
                    }
                } else {
                    raftSessionManager.getFileSessionManager().removeGlobalSession(globalSession);
                }
            }
        }
        return true;
    }
}
