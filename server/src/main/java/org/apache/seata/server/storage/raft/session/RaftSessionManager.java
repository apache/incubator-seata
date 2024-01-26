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
package org.apache.seata.server.storage.raft.session;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import com.alipay.sofa.jraft.Closure;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBranchSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.SessionConverter;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;

import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.ADD_BRANCH_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.ADD_GLOBAL_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.REMOVE_BRANCH_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.REMOVE_GLOBAL_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.UPDATE_BRANCH_SESSION_STATUS;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 */
@LoadLevel(name = "raft", scope = Scope.PROTOTYPE)
public class RaftSessionManager extends FileSessionManager {

    public RaftSessionManager(String name) throws IOException {
        super(name);
    }

    @Override
    public void addGlobalSession(GlobalSession globalSession) throws TransactionException {
        super.addGlobalSession(globalSession);
    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return super.findGlobalSession(xid);
    }

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    super.addGlobalSession(globalSession);
                    completableFuture.complete(true);
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                try {
                    completableFuture.completeExceptionally(
                        new TransactionException(TransactionExceptionCode.NotRaftLeader,
                            "seata raft state machine exception: " + status.getErrorMsg()));
                } finally {
                    try {
                        super.removeGlobalSession(globalSession);
                    } catch (TransactionException e) {
                        completableFuture.completeExceptionally(e);
                    }
                }
            }
        };
        GlobalTransactionDTO globalTransactionDTO = new GlobalTransactionDTO();
        SessionConverter.convertGlobalTransactionDO(globalTransactionDTO, globalSession);
        RaftGlobalSessionSyncMsg raftSyncMsg = new RaftGlobalSessionSyncMsg(ADD_GLOBAL_SESSION, globalTransactionDTO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus globalStatus) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = closureStatus -> {
            if (closureStatus.isOk()) {
                globalSession.setStatus(globalStatus);
                if (GlobalStatus.RollbackRetrying.equals(globalSession.getStatus())
                    || GlobalStatus.Rollbacking.equals(globalSession.getStatus())
                    || GlobalStatus.TimeoutRollbacking.equals(globalSession.getStatus())) {
                    globalSession.getBranchSessions().parallelStream()
                        .forEach(branchSession -> branchSession.setLockStatus(LockStatus.Rollbacking));
                }
                completableFuture.complete(true);
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + closureStatus.getErrorMsg()));
            }
        };
        GlobalTransactionDTO globalTransactionDO = new GlobalTransactionDTO(globalSession.getXid());
        globalTransactionDO.setStatus(globalStatus.getCode());
        RaftGlobalSessionSyncMsg raftSyncMsg =
            new RaftGlobalSessionSyncMsg(UPDATE_GLOBAL_SESSION_STATUS, globalTransactionDO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession,
        BranchStatus branchStatus) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = closureStatus -> {
            if (closureStatus.isOk()) {
                branchSession.setStatus(branchStatus);
                completableFuture.complete(true);
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + closureStatus.getErrorMsg()));
            }
        };
        BranchTransactionDTO branchTransactionDO = new BranchTransactionDTO(globalSession.getXid(), branchSession.getBranchId());
        branchTransactionDO.setStatus(branchStatus.getCode());
        RaftBranchSessionSyncMsg raftSyncMsg =
                new RaftBranchSessionSyncMsg(UPDATE_BRANCH_SESSION_STATUS, branchTransactionDO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        branchSession.setStatus(BranchStatus.Registered);
        Closure closure = status -> {
            if (status.isOk()) {
                completableFuture.complete(globalSession.add(branchSession));
            } else {
                try {
                    completableFuture.completeExceptionally(
                        new TransactionException(TransactionExceptionCode.NotRaftLeader,
                            "seata raft state machine exception: " + status.getErrorMsg()));
                } finally {
                    try {
                        globalSession.removeBranch(branchSession);
                    } catch (TransactionException e) {
                        completableFuture.completeExceptionally(e);
                    }
                }
            }
        };
        BranchTransactionDTO branchTransactionDTO = new BranchTransactionDTO();
        SessionConverter.convertBranchTransaction(branchTransactionDTO, branchSession);
        RaftBranchSessionSyncMsg raftSyncMsg = new RaftBranchSessionSyncMsg(ADD_BRANCH_SESSION, branchTransactionDTO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = closureStatus -> {
            if (closureStatus.isOk()) {
                completableFuture.complete(globalSession.remove(branchSession));
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + closureStatus.getErrorMsg()));
            }
        };
        BranchTransactionDTO branchTransactionDO =
            new BranchTransactionDTO(globalSession.getXid(), branchSession.getBranchId());
        RaftBranchSessionSyncMsg raftSyncMsg = new RaftBranchSessionSyncMsg(REMOVE_BRANCH_SESSION, branchTransactionDO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onSuccessEnd(GlobalSession globalSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    super.removeGlobalSession(globalSession);
                    completableFuture.complete(true);
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            } else {
                completableFuture.completeExceptionally(
                    new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        "seata raft state machine exception: " + status.getErrorMsg()));
            }
        };
        GlobalTransactionDTO globalTransactionDO = new GlobalTransactionDTO(globalSession.getXid());
        RaftGlobalSessionSyncMsg raftSyncMsg = new RaftGlobalSessionSyncMsg(REMOVE_GLOBAL_SESSION, globalTransactionDO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onFailEnd(GlobalSession globalSession) throws TransactionException {
        super.onFailEnd(globalSession);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void destroy() {}

}
