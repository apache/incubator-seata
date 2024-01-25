/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.cluster.raft.execute.branch;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.server.cluster.raft.execute.AbstractRaftMsgExecute;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBranchSessionSyncMsg;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.raft.session.RaftSessionManager;

/**
 */
public class UpdateBranchSessionExecute extends AbstractRaftMsgExecute {

    @Override
    public Boolean execute(RaftBaseMsg syncMsg) throws Throwable {
        RaftBranchSessionSyncMsg sessionSyncMsg = (RaftBranchSessionSyncMsg)syncMsg;
        RaftSessionManager raftSessionManager = (RaftSessionManager) SessionHolder.getRootSessionManager(sessionSyncMsg.getGroup());
        GlobalSession globalSession = raftSessionManager.findGlobalSession(sessionSyncMsg.getBranchSession().getXid());
        BranchSession branchSession = globalSession.getBranch(sessionSyncMsg.getBranchSession().getBranchId());
        BranchStatus status = BranchStatus.get(sessionSyncMsg.getBranchSession().getStatus());
        branchSession.setStatus(status);
        if (logger.isDebugEnabled()) {
            logger.debug("update branch: {} , status: {}", branchSession.getBranchId(), branchSession.getStatus());
        }
        return true;
    }

}
