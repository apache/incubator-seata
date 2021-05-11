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

import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;

/**
 * @author jianbin.chen
 */
public class AddGlobalSessionExecute extends AbstractRaftMsgExecute {

    private boolean root;

    public AddGlobalSessionExecute(RaftSessionSyncMsg sessionSyncMsg, RaftSessionManager raftSessionManager,
        boolean root) {
        super(sessionSyncMsg, raftSessionManager);
        this.root = root;
    }

    @Override
    public Boolean execute(Object... objects) throws Throwable {
        GlobalSession globalSession;
        if (!root) {
            globalSession =
                SessionHolder.getRootSessionManager().findGlobalSession(sessionSyncMsg.getGlobalSession().getXid());
        } else {
            globalSession = SessionConverter.convertGlobalSession(sessionSyncMsg.getGlobalSession());
        }
        switch (globalSession.getStatus()) {
            case AsyncCommitting:
                globalSession.asyncCommit();
                break;
            case CommitRetrying:
                globalSession.queueToRetryCommit();
                break;
            case RollbackRetrying:
            case TimeoutRollbackFailed:
                globalSession.queueToRetryRollback();
                break;
            default:
                raftSessionManager.getFileSessionManager().addGlobalSession(globalSession);
                break;
        }
        return true;
    }

}
