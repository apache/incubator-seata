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
package org.apache.seata.server.storage.raft.lock;

import java.util.concurrent.CompletableFuture;
import com.alipay.sofa.jraft.Closure;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBranchSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.lock.FileLockManager;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;

import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.RELEASE_BRANCH_SESSION_LOCK;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.RELEASE_GLOBAL_SESSION_LOCK;
/**
 */
@LoadLevel(name = "raft")
public class RaftLockManager extends FileLockManager {

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        GlobalTransactionDTO globalTransactionDTO = new GlobalTransactionDTO();
        globalTransactionDTO.setXid(globalSession.getXid());
        RaftGlobalSessionSyncMsg raftSyncMsg = new RaftGlobalSessionSyncMsg(RELEASE_GLOBAL_SESSION_LOCK, globalTransactionDTO);
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    completableFuture.complete(this.localReleaseGlobalSessionLock(globalSession));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                completableFuture.completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                    "seata raft state machine exception: " + status.getErrorMsg()));
            }
        };
        return RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        BranchTransactionDTO branchTransactionDTO = new BranchTransactionDTO();
        branchTransactionDTO.setBranchId(branchSession.getBranchId());
        branchTransactionDTO.setXid(branchSession.getXid());
        RaftBranchSessionSyncMsg raftSyncMsg = new RaftBranchSessionSyncMsg(RELEASE_BRANCH_SESSION_LOCK, branchTransactionDTO);
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    // ensure consistency through state machine reading
                    completableFuture.complete(super.releaseLock(branchSession));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                completableFuture.completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                    "seata raft state machine exception: " + status.getErrorMsg()));
            }
        };
        return RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    public boolean localReleaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        return super.releaseGlobalSessionLock(globalSession);
    }

    public boolean localReleaseLock(BranchSession branchSession) throws TransactionException {
        return super.releaseLock(branchSession);
    }

}
