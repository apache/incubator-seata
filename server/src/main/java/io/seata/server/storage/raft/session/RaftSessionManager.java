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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.raft.RaftServerFactory;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.storage.raft.RaftSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;

import static io.seata.server.session.SessionHolder.ASYNC_COMMITTING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.RETRY_COMMITTING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.RETRY_ROLLBACKING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.REMOVE_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.UPDATE_BRANCH_SESSION_STATUS;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 * @author funkye
 */
@LoadLevel(name = "raft", scope = Scope.PROTOTYPE)
public class RaftSessionManager extends AbstractSessionManager implements Reloadable, Closure, java.io.Serializable {

    private FileSessionManager fileSessionManager;

    private GlobalSession globalSession;

    private BranchSession branchSession;

    private Closure done;

    private String name;

    public RaftSessionManager(String name, FileSessionManager fileSessionManager) {
        this.fileSessionManager = fileSessionManager;
        this.name = name;
    }

    public RaftSessionManager(GlobalSession globalSession, String name, Closure done) {
        this.globalSession = globalSession;
        this.done = done;
        this.name = name;
    }

    public RaftSessionManager(GlobalSession globalSession, Closure done) {
        this.globalSession = globalSession;
        this.done = done;
    }

    public RaftSessionManager(BranchSession branchSession, Closure done) {
        this.branchSession = branchSession;
        this.done = done;
    }

    public RaftSessionManager(GlobalSession globalSession, BranchSession branchSession, Closure done) {
        this.globalSession = globalSession;
        this.branchSession = branchSession;
        this.done = done;
    }

    @Override
    public void reload() {
        fileSessionManager.reload();
    }

    public void addGlobalSession(GlobalSession globalSession) throws TransactionException {
        if (!RaftServerFactory.getInstance().isLeader()) {
            throw new TransactionException("this node is not a leader node, so requests are not allowed");
        }
        RaftSessionManager raftSessionManager = new RaftSessionManager(globalSession, name, status -> {
            if (status.isOk()) {
                try {
                    SessionManager sessionManager = null;
                    if (!Objects.equals(name, ROOT_SESSION_MANAGER_NAME)) {
                        if (Objects.equals(name, ASYNC_COMMITTING_SESSION_MANAGER_NAME)) {
                            sessionManager = SessionHolder.getAsyncCommittingSessionManager();
                        } else if (Objects.equals(name, RETRY_COMMITTING_SESSION_MANAGER_NAME)) {
                            sessionManager = SessionHolder.getRetryCommittingSessionManager();
                        } else if (Objects.equals(name, RETRY_ROLLBACKING_SESSION_MANAGER_NAME)) {
                            sessionManager = SessionHolder.getRetryRollbackingSessionManager();
                        }
                    } else {
                        sessionManager = SessionHolder.getRootSessionManager();
                    }
                    RaftSessionManager manager = sessionManager != null ? (RaftSessionManager)sessionManager : null;
                    manager.getFileSessionManager().addGlobalSession(globalSession);
                } catch (TransactionException e) {
                    e.printStackTrace();
                }
            }
        });
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(ADD_GLOBAL_SESSION, globalTransactionDO);
        raftSyncMsg.setSessionName(this.name);
        RaftTaskUtil.createTask(raftSessionManager, raftSyncMsg);
    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return fileSessionManager.findGlobalSession(xid);
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return fileSessionManager.findGlobalSession(xid, withBranchSessions);
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return fileSessionManager.allSessions();
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        return fileSessionManager.findGlobalSessions(condition);
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
        throws TransactionException {
        return fileSessionManager.lockAndExecute(globalSession, lockCallable);
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {
        fileSessionManager.updateGlobalSessionStatus(session, status);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        fileSessionManager.removeGlobalSession(session);
    }

    @Override
    public void addBranchSession(GlobalSession session, BranchSession branchSession) throws TransactionException {
        fileSessionManager.addBranchSession(session, branchSession);
    }

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {
        addGlobalSession(globalSession);
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus globalStatus) throws TransactionException {
        RaftSessionManager raftSessionManager = new RaftSessionManager(globalSession, status -> {
            if (status.isOk()) {
                try {
                    SessionHolder.getRootSessionManager().updateGlobalSessionStatus(globalSession, globalStatus);
                } catch (TransactionException e) {
                    e.printStackTrace();
                }
            }
        });
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(UPDATE_GLOBAL_SESSION_STATUS, globalTransactionDO, globalStatus);
        raftSyncMsg.setSessionName(this.name);
        RaftTaskUtil.createTask(raftSessionManager, raftSyncMsg);
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession, BranchStatus branchStatus)
        throws TransactionException {
        RaftSessionManager raftSessionManager = new RaftSessionManager(globalSession, status -> {
            if (status.isOk()) {
                try {
                    SessionHolder.getRootSessionManager().updateBranchSessionStatus(branchSession, branchStatus);
                } catch (TransactionException e) {
                    e.printStackTrace();
                }
            }
        });
        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(UPDATE_BRANCH_SESSION_STATUS, branchTransactionDO, branchStatus);
        raftSyncMsg.setSessionName(this.name);
        RaftTaskUtil.createTask(raftSessionManager, raftSyncMsg);
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) {
        RaftSessionManager raftSessionManager = new RaftSessionManager(globalSession, status -> {
            if (status.isOk()) {
                try {
                    SessionHolder.getRootSessionManager().addBranchSession(globalSession, branchSession);
                } catch (TransactionException e) {
                    e.printStackTrace();
                }
            }
        });
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(ADD_BRANCH_SESSION, globalTransactionDO, branchTransactionDO);
        raftSyncMsg.setSessionName(this.name);
        RaftTaskUtil.createTask(raftSessionManager, raftSyncMsg);
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        fileSessionManager.removeBranchSession(globalSession, branchSession);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) {
        RaftSessionManager raftSessionManager = new RaftSessionManager(globalSession, status -> {
            if (status.isOk()) {
                try {
                    SessionHolder.getRootSessionManager().removeBranchSession(globalSession, branchSession);
                } catch (TransactionException e) {
                    e.printStackTrace();
                }
            }
        });
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(REMOVE_BRANCH_SESSION, globalTransactionDO, branchTransactionDO);
        raftSyncMsg.setSessionName(this.name);
        RaftTaskUtil.createTask(raftSessionManager, raftSyncMsg);
    }

    @Override
    public void onClose(GlobalSession globalSession) throws TransactionException {
        fileSessionManager.onClose(globalSession);
    }

    @Override
    public void onEnd(GlobalSession globalSession) {
        RaftSessionManager raftSessionManager = new RaftSessionManager(globalSession, status -> {
            if (status.isOk()) {
                try {
                    SessionManager sessionManager = null;
                    if (!Objects.equals(name, ROOT_SESSION_MANAGER_NAME)) {
                        if (Objects.equals(name, ASYNC_COMMITTING_SESSION_MANAGER_NAME)) {
                            sessionManager = SessionHolder.getAsyncCommittingSessionManager();
                        } else if (Objects.equals(name, RETRY_COMMITTING_SESSION_MANAGER_NAME)) {
                            sessionManager = SessionHolder.getRetryCommittingSessionManager();
                        } else if (Objects.equals(name, RETRY_ROLLBACKING_SESSION_MANAGER_NAME)) {
                            sessionManager = SessionHolder.getRetryRollbackingSessionManager();
                        }
                    } else {
                        sessionManager = SessionHolder.getRootSessionManager();
                    }
                    RaftSessionManager manager = sessionManager != null ? (RaftSessionManager)sessionManager : null;
                    manager.removeGlobalSession(globalSession);
                } catch (TransactionException e) {
                    e.printStackTrace();
                }
            }
        });
        LOGGER.info("onEnd,raftSessionManager:{}", name);
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(REMOVE_GLOBAL_SESSION, globalTransactionDO);
        raftSyncMsg.setSessionName(this.name);
        RaftTaskUtil.createTask(raftSessionManager, raftSyncMsg);
    }

    @Override
    public void run(Status status) {
        if (this.done != null) {
            done.run(status);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileSessionManager getFileSessionManager() {
        return fileSessionManager;
    }

    public void setFileSessionManager(FileSessionManager fileSessionManager) {
        this.fileSessionManager = fileSessionManager;
    }
}
