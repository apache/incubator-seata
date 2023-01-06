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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.exception.TransactionException;
import io.seata.server.cluster.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.session.RaftSessionManager;

/**
 * @author jianbin.chen
 */
public class RemoveGlobalSessionExecute extends AbstractRaftMsgExecute {
    
    private static final ThreadPoolExecutor EXECUTOR =
        new ThreadPoolExecutor(1, 1, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2048),
            new NamedThreadFactory("RemoveGlobalSessionExecute", 1), new ThreadPoolExecutor.CallerRunsPolicy());

    @Override
    public Boolean execute(RaftSessionSyncMsg sessionSyncMsg) {
        // when the global transaction needs to be deleted, it does not affect any consistency issues, and can be
        // deleted in an asynchronous thread to improve the throughput of the state machine
        RaftSessionManager raftSessionManager = (RaftSessionManager) SessionHolder.getRootSessionManager(sessionSyncMsg.getGroup());
        GlobalSession globalSession =
                raftSessionManager.findGlobalSession(sessionSyncMsg.getGlobalSession().getXid());
        try {
            raftLockManager.localReleaseGlobalSessionLock(globalSession);
        } catch (TransactionException e) {
            logger.error(e.getMessage(), e);
        }
        EXECUTOR.execute(() -> {
            if (globalSession != null) {
                try {
                    raftSessionManager.removeGlobalSession(globalSession);
                    if (logger.isDebugEnabled()) {
                        logger.debug("end xid: {}", globalSession.getXid());
                    }
                } catch (TransactionException e) {
                    logger.error("remove global fail error:{}", e.getMessage());
                }
            }
        });
        return true;
    }

}
