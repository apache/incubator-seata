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

import io.seata.core.exception.TransactionException;
import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.raft.RaftSessionSyncMsg;

/**
 * @author jianbin.chen
 */
public class RemoveGlobalSessionExecute extends AbstractRaftMsgExecute {

    public RemoveGlobalSessionExecute(RaftSessionSyncMsg sessionSyncMsg) {
        super(sessionSyncMsg);
    }

    @Override
    public Boolean execute(Object... args) {
        logger.info("remove start session map size:{}", raftSessionManager.getSessionMap().size());
        GlobalSession globalSession = raftSessionManager.findGlobalSession(sessionSyncMsg.getGlobalSession().getXid());
        if (globalSession != null) {
            try {
                globalSession.clean();
                raftSessionManager.removeGlobalSession(globalSession);
                logger.info("end xid: {}", globalSession.getXid());
            } catch (TransactionException e) {
                logger.error("remove global fail error:{}", e.getMessage());
            }
        }
        logger.info("remove end session map size:{}", raftSessionManager.getSessionMap().size());
        return true;
    }

}
