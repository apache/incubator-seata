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
package io.seata.server.storage.raft.session;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import com.alipay.sofa.jraft.Closure;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.raft.RaftServerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;

import static io.seata.server.raft.execute.RaftSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.server.raft.execute.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.server.raft.execute.RaftSyncMsg.MsgType.REMOVE_BRANCH_SESSION;
import static io.seata.server.raft.execute.RaftSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;
import static io.seata.server.raft.execute.RaftSyncMsg.MsgType.UPDATE_BRANCH_SESSION_STATUS;
import static io.seata.server.raft.execute.RaftSyncMsg.MsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 * @author funkye
 */
@LoadLevel(name = "raft", scope = Scope.PROTOTYPE)
public class RaftSessionManager extends FileSessionManager {

    public RaftSessionManager(String name, String sessionStoreFilePath) throws IOException {
        super(name,sessionStoreFilePath);
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
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        super.addGlobalSession(globalSession);
                        completableFuture.complete(true);
                    } catch (TransactionException e) {
                        completableFuture.completeExceptionally(e);
                    }
                } else {
                    completableFuture
                        .completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                            " The current TC is not a leader node, interrupt processing !"));
                }
            };
            GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ADD_GLOBAL_SESSION, globalTransactionDO);
            RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
        }
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus globalStatus) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = closureStatus -> {
            if (closureStatus.isOk()) {
                completableFuture.complete(true);
            } else {
                completableFuture.completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                    " The current TC is not a leader node, interrupt processing !"));
            }
        };
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO(globalSession.getXid());
        RaftSessionSyncMsg raftSyncMsg =
            new RaftSessionSyncMsg(UPDATE_GLOBAL_SESSION_STATUS, globalTransactionDO, globalStatus);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession,
        BranchStatus branchStatus) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = closureStatus -> {
            if (closureStatus.isOk()) {
                completableFuture.complete(true);
            } else {
                completableFuture
                        .completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                                " The current TC is not a leader node, interrupt processing !"));
            }
        };
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO(globalSession.getXid(), branchSession.getBranchId());
        RaftSessionSyncMsg raftSyncMsg =
                new RaftSessionSyncMsg(UPDATE_BRANCH_SESSION_STATUS, branchTransactionDO, branchStatus);
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
                completableFuture
                        .completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                                " The current TC is not a leader node, interrupt processing !"));
            }
        };
        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ADD_BRANCH_SESSION, branchTransactionDO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = closureStatus -> {
            if (closureStatus.isOk()) {
                completableFuture.complete(globalSession.remove(branchSession));
            } else {
                completableFuture.completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                    " The current TC is not a leader node, interrupt processing !"));
            }
        };
        BranchTransactionDO branchTransactionDO =
            new BranchTransactionDO(globalSession.getXid(), branchSession.getBranchId());
        RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(REMOVE_BRANCH_SESSION, branchTransactionDO);
        RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
    }

    @Override
    public void onEnd(GlobalSession globalSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onEnd,raftSessionManager:{}", name);
                        }
                        super.removeGlobalSession(globalSession);
                        completableFuture.complete(true);
                    } catch (TransactionException e) {
                        completableFuture.completeExceptionally(e);
                    }
                } else {
                    completableFuture
                        .completeExceptionally(new TransactionException(TransactionExceptionCode.NotRaftLeader,
                            " The current TC is not a leader node, interrupt processing !"));
                }
            };
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO(globalSession.getXid());
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(REMOVE_GLOBAL_SESSION, globalTransactionDO);
            RaftTaskUtil.createTask(closure, raftSyncMsg, completableFuture);
        } else {
            super.removeGlobalSession(globalSession);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
