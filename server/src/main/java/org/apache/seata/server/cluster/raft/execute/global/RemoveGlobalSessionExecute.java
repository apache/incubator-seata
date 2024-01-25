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
package org.apache.seata.server.cluster.raft.execute.global;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.cluster.raft.execute.AbstractRaftMsgExecute;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.raft.session.RaftSessionManager;

/**
 */
public class RemoveGlobalSessionExecute extends AbstractRaftMsgExecute {
    
    private static final ThreadPoolExecutor EXECUTOR =
        new ThreadPoolExecutor(1, 1, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2048),
            new NamedThreadFactory("RemoveGlobalSessionExecute", 1), new ThreadPoolExecutor.CallerRunsPolicy());

    @Override
    public Boolean execute(RaftBaseMsg syncMsg) throws Throwable {
        RaftGlobalSessionSyncMsg sessionSyncMsg = (RaftGlobalSessionSyncMsg)syncMsg;
        // when the global transaction needs to be deleted, it does not affect any consistency issues, and can be
        // deleted in an asynchronous thread to improve the throughput of the state machine
        RaftSessionManager raftSessionManager = (RaftSessionManager) SessionHolder.getRootSessionManager(sessionSyncMsg.getGroup());
        Optional.ofNullable(raftSessionManager.findGlobalSession(sessionSyncMsg.getGlobalSession().getXid()))
            .ifPresent(globalSession -> {
                try {
                    raftLockManager.localReleaseGlobalSessionLock(globalSession);
                    EXECUTOR.execute(() -> {
                        try {
                            raftSessionManager.removeGlobalSession(globalSession);
                            if (logger.isDebugEnabled()) {
                                logger.debug("remove session xid: {}", globalSession.getXid());
                            }
                        } catch (TransactionException e) {
                            logger.error("remove global fail error:{}", e.getMessage());
                        }
                    });
                } catch (TransactionException e) {
                    logger.error(e.getMessage(), e);
                }
            });
        return true;
    }

}
