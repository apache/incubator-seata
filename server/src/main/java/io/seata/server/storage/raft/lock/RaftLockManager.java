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

import java.util.concurrent.CompletableFuture;
import com.alipay.sofa.jraft.Closure;
import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.lock.Locker;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.lock.FileLockManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;

import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.ACQUIRE_LOCK;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.RELEASE_BRANCH_SESSION_LOCK;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;

/**
 * @author funkye
 */
@LoadLevel(name = "raft")
public class RaftLockManager extends FileLockManager {

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        // perform competitive lock first to avoid meaningless state machine execution
        if (super.acquireLock(branchSession)) {
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ACQUIRE_LOCK, branchTransactionDO);
            Closure closure = status -> {
                if (status.isOk()) {
                    completableFuture.complete(true);
                } else {
                    try {
                        // avoid data inconsistency after a new election
                        localReleaseLock(branchSession);
                        completableFuture.completeExceptionally(
                            new TransactionException(TransactionExceptionCode.NotRaftLeader,
                                "seata raft state machine exception: " + status.getErrorMsg()));
                    } catch (TransactionException e) {
                        completableFuture.completeExceptionally(e);
                    }
                }
            };
            return RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
        }
        return false;
    }

    public boolean localAcquireLock(BranchSession branchSession) throws TransactionException {
        return super.acquireLock(branchSession);
    }

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return super.getLocker(branchSession);
    }

    public boolean localReleaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        return super.releaseGlobalSessionLock(globalSession);
    }
    
    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(globalSession.getXid());
        RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(RELEASE_GLOBAL_SESSION_LOCK, globalTransactionDO);
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    completableFuture.complete(super.releaseGlobalSessionLock(globalSession));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + status.getErrorMsg()));
            }
        };
        return RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    // ensure consistency through state machine reading
                    completableFuture.complete(super.isLockable(xid, resourceId, lockKey));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + status.getErrorMsg()));
            }
        };
        return RaftTaskUtil.createTask(closure, completableFuture);
    }

    public boolean localReleaseLock(BranchSession branchSession) throws TransactionException {
        return super.releaseLock(branchSession);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setXid(branchSession.getXid());
        RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(RELEASE_BRANCH_SESSION_LOCK, branchTransactionDO);
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    // ensure consistency through state machine reading
                    completableFuture.complete(super.releaseLock(branchSession));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + status.getErrorMsg()));
            }
        };
        return RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

}
