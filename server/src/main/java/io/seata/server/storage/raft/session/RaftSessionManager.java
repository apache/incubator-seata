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
import java.util.Map;
import java.util.Objects;
import com.alipay.sofa.jraft.Closure;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.raft.RaftServerFactory;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;


import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.REMOVE_BRANCH_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.UPDATE_BRANCH_SESSION_STATUS;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.UPDATE_GLOBAL_SESSION_STATUS;
import static io.seata.server.session.SessionHolder.ASYNC_COMMITTING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.RETRY_COMMITTING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.RETRY_ROLLBACKING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;

/**
 * @author funkye
 */
@LoadLevel(name = "raft", scope = Scope.PROTOTYPE)
public class RaftSessionManager extends AbstractSessionManager implements Reloadable {

    private FileSessionManager fileSessionManager;

    private String name;

    public RaftSessionManager(String name, FileSessionManager fileSessionManager) {
        this.fileSessionManager = fileSessionManager;
        this.name = name;
    }

    @Override
    public void reload() {
        fileSessionManager.reload();
    }

    public void addGlobalSession(GlobalSession globalSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        LOGGER.info("addGlobalSession,raftSessionManager:{}", name);
                        getSessionManager().getFileSessionManager().addGlobalSession(globalSession);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ADD_GLOBAL_SESSION, globalTransactionDO);
            raftSyncMsg.setSessionName(this.name);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.fileSessionManager.addGlobalSession(globalSession);
        }
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
        this.addGlobalSession(globalSession);
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus globalStatus) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onStatusChange,raftSessionManager:{}", name);
                        }
                        SessionHolder.getRootSessionManager().updateGlobalSessionStatus(globalSession, globalStatus);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO(globalSession.getXid());
            RaftSessionSyncMsg raftSyncMsg =
                new RaftSessionSyncMsg(UPDATE_GLOBAL_SESSION_STATUS, globalTransactionDO, globalStatus);
            raftSyncMsg.setSessionName(this.name);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.updateGlobalSessionStatus(globalSession, globalStatus);
        }
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession,
        BranchStatus branchStatus) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onBranchStatusChange,raftSessionManager:{}", name);
                        }
                        SessionHolder.getRootSessionManager().updateBranchSessionStatus(branchSession, branchStatus);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            BranchTransactionDO branchTransactionDO =
                new BranchTransactionDO(globalSession.getXid(), branchSession.getBranchId());
            RaftSessionSyncMsg raftSyncMsg =
                new RaftSessionSyncMsg(UPDATE_BRANCH_SESSION_STATUS, branchTransactionDO, branchStatus);
            raftSyncMsg.setSessionName(this.name);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.updateBranchSessionStatus(branchSession, branchStatus);
        }
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onAddBranch,raftSessionManager:{}", name);
                        }
                        SessionHolder.getRootSessionManager().addBranchSession(globalSession, branchSession);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ADD_BRANCH_SESSION, branchTransactionDO);
            raftSyncMsg.setSessionName(this.name);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else if (!globalSession.getBranchSessions().contains(branchSession)) {
            this.addBranchSession(globalSession, branchSession);
        }
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        fileSessionManager.removeBranchSession(globalSession, branchSession);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onRemoveBranch,raftSessionManager:{}", name);
                        }
                        SessionHolder.getRootSessionManager().removeBranchSession(globalSession, branchSession);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            BranchTransactionDO branchTransactionDO =
                new BranchTransactionDO(globalSession.getXid(), branchSession.getBranchId());
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(REMOVE_BRANCH_SESSION, branchTransactionDO);
            raftSyncMsg.setSessionName(this.name);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.removeBranchSession(globalSession, branchSession);
        }
    }

    @Override
    public void onClose(GlobalSession globalSession) throws TransactionException {
        fileSessionManager.onClose(globalSession);
    }

    @Override
    public void onEnd(GlobalSession globalSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onEnd,raftSessionManager:{}", name);
                        }
                        getSessionManager().removeGlobalSession(globalSession);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO(globalSession.getXid());
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(REMOVE_GLOBAL_SESSION, globalTransactionDO);
            raftSyncMsg.setSessionName(this.name);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.removeGlobalSession(globalSession);
        }
    }

    private RaftSessionManager getSessionManager() {
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
        return (RaftSessionManager)sessionManager;
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

    public Map<String, GlobalSession> getSessionMap() {
        return this.fileSessionManager.getSessionMap();
    }

    public void setSessionMap(Map<String, GlobalSession> sessionMap) {
        if (sessionMap == null || sessionMap.isEmpty()) {
            return;
        }
        this.fileSessionManager.setSessionMap(sessionMap);
    }

}
