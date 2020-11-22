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
package io.seata.server.raft;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Utils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.raft.AbstractRaftStateMachine;
import io.seata.core.raft.msg.RaftOnRequestMsg;
import io.seata.core.raft.msg.RaftSyncMsg;
import io.seata.core.rpc.processor.server.ServerOnRequestProcessor;
import io.seata.core.store.StoreMode;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.coordinator.RaftCoreSyncMsg;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.lock.FileLocker;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.lock.RaftLockManager;
import io.seata.server.storage.raft.session.RaftSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.alipay.remoting.serialization.SerializerManager.Hessian2;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ACQUIRE_LOCK;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;
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
public class RaftStateMachine extends AbstractRaftStateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(RaftStateMachine.class);
    private DefaultCore core;
    public RaftStateMachine() {
        mode = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_MODE);
    }

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()) {
            Closure processor = null;
            if (iterator.done() != null) {
                processor = iterator.done();
            } else {
                try {
                    Object raftSyncMsg = SerializerManager.getSerializer(Hessian2).deserialize(iterator.getData().array(),
                        RaftSyncMsg.class.getName());
                    onExecuteRaft(raftSyncMsg);
                } catch (Exception e) {
                    LOG.error("Message synchronization failure", e);
                }
            }
            if (processor != null) {
                processor.run(Status.OK());
            }
            iterator.next();
        }
    }

    @Override
    public void onSnapshotSave(final SnapshotWriter writer, final Closure done) {
        if (!StringUtils.equals(StoreMode.RAFT.getName(), mode)) {
            return;
        }
        Map<String, Object> maps = new HashMap<>();
        FileSessionManager fileSessionManager = (FileSessionManager)SessionHolder.getRootSessionManager();
        Map<String, GlobalSession> sessionMap = fileSessionManager.getSessionMap();
        Map<String, byte[]> sessionByteMap = new HashMap<>();
        LOG.info("sessionmap size:{}",sessionMap.size());
        sessionMap.forEach((k, v) -> sessionByteMap.put(v.getXid(), v.encode()));
        maps.put(ROOT_SESSION_MANAGER_NAME, sessionByteMap);
        maps.put("LOCK_MAP", FileLocker.LOCK_MAP);
        if (maps.isEmpty()) {
            return;
        }
        Utils.runInThread(() -> {
            final RaftSnapshotFile snapshot = new RaftSnapshotFile(writer.getPath() + File.separator + "data");
            if (snapshot.save(maps)) {
                if (writer.addFile("data")) {
                    done.run(Status.OK());
                } else {
                    done.run(new Status(RaftError.EIO, "Fail to add file to writer"));
                }
            } else {
                done.run(new Status(RaftError.EIO, "Fail to save counter snapshot %s", snapshot.getPath()));
            }
        });
    }

    @Override
    public void onError(final RaftException e) {
        LOG.error("Raft error: {}", e, e);
    }

    @Override
    public boolean onSnapshotLoad(final SnapshotReader reader) {
        if (!StringUtils.equals(StoreMode.RAFT.getName(), mode)) {
            return false;
        }
        if (isLeader()) {
            LOG.warn("Leader is not supposed to load snapshot");
            return false;
        }
        if (reader.getFileMeta("data") == null) {
            LOG.error("Fail to find data file in {}", reader.getPath());
            return false;
        }
        final RaftSnapshotFile snapshot = new RaftSnapshotFile(reader.getPath() + File.separator + "data");
        try {
            Map<String, Object> maps = snapshot.load();
            FileSessionManager fileSessionManager = (FileSessionManager)SessionHolder.getRootSessionManager();
            FileLocker.LOCK_MAP.putAll((Map<? extends String,
                ? extends ConcurrentMap<String, ConcurrentMap<Integer, FileLocker.BucketLockMap>>>)maps
                    .get("LOCK_MAP"));
            Map<String, byte[]> sessionByteMap = (Map<String, byte[]>)maps.get(ROOT_SESSION_MANAGER_NAME);
            if (!sessionByteMap.isEmpty()) {
                Map<String, GlobalSession> sessionMap = new HashMap<>();
                sessionByteMap.forEach((k, v) -> {
                    GlobalSession session = new GlobalSession();
                    session.decode(v);
                    sessionMap.put(k, session);
                });
                fileSessionManager.putAll(sessionMap);
                sessionMap.forEach((k, v) -> {
                    GlobalStatus status = v.getStatus();
                    try {
                        if (status == GlobalStatus.AsyncCommitting) {
                            SessionHolder.getAsyncCommittingSessionManager().addGlobalSession(v);
                        } else if (status == GlobalStatus.CommitRetrying) {
                            SessionHolder.getRetryCommittingSessionManager().addGlobalSession(v);
                        } else if (status == GlobalStatus.RollbackRetrying) {
                            SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(v);
                        }
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                });
            }
            return true;
        } catch (final Exception e) {
            LOG.error("Fail to load snapshot from {}", snapshot.getPath());
            return false;
        }

    }

    @Override
    public void onLeaderStart(final long term) {
        this.leaderTerm.set(term);
        super.onLeaderStart(term);

    }

    @Override
    public void onLeaderStop(final Status status) {
        this.leaderTerm.set(-1);
        super.onLeaderStop(status);
    }

    private void onExecuteRaft(Object raftSyncMsg) throws TransactionException {
        if (raftSyncMsg instanceof RaftSessionSyncMsg) {
            RaftSessionSyncMsg msg = (RaftSessionSyncMsg)raftSyncMsg;
            RaftSessionSyncMsg.MsgType msgType = msg.getMsgType();
            SessionManager sessionManager = null;
            String sessionName = msg.getSessionName();
            if (Objects.equals(sessionName, ROOT_SESSION_MANAGER_NAME)) {
                sessionManager = SessionHolder.getRootSessionManager();
            } else if (Objects.equals(sessionName, ASYNC_COMMITTING_SESSION_MANAGER_NAME)) {
                sessionManager = SessionHolder.getAsyncCommittingSessionManager();
            } else if (Objects.equals(sessionName, RETRY_COMMITTING_SESSION_MANAGER_NAME)) {
                sessionManager = SessionHolder.getRetryCommittingSessionManager();
            } else if (Objects.equals(sessionName, RETRY_ROLLBACKING_SESSION_MANAGER_NAME)) {
                sessionManager = SessionHolder.getRetryRollbackingSessionManager();
            }
            RaftSessionManager raftSessionManager = sessionManager != null ? (RaftSessionManager)sessionManager : null;
            LOG.info("state machine synchronization,task:{},sessionManager:{}", msgType,
                sessionName != null ? sessionName : ROOT_SESSION_MANAGER_NAME);
            if (ADD_GLOBAL_SESSION.equals(msgType)) {
                GlobalSession globalSession = SessionConverter.convertGlobalSession(msg.getGlobalSession());
                raftSessionManager.getFileSessionManager().addGlobalSession(globalSession);
            } else if (ACQUIRE_LOCK.equals(msgType)) {
                GlobalSession globalSession =
                    SessionHolder.getRootSessionManager().findGlobalSession(msg.getBranchSession().getXid());
                BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                boolean include = false;
                if (branchSession != null) {
                    include = true;
                    branchSession.setLockKey(msg.getBranchSession().getLockKey());
                } else {
                    branchSession = SessionConverter.convertBranchSession(msg.getBranchSession());
                }
                Boolean owner = RaftLockManager.LOCK_MANAGER.acquireLock(branchSession);
                if (owner && !include) {
                    globalSession.add(branchSession);
                }
            } else if (ADD_BRANCH_SESSION.equals(msgType)) {
                GlobalSession globalSession = raftSessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                if (branchSession == null) {
                    branchSession = SessionConverter.convertBranchSession(msg.getBranchSession());
                    globalSession.addBranch(branchSession);
                }
                raftSessionManager.addBranchSession(globalSession, branchSession);
            } else if (UPDATE_GLOBAL_SESSION_STATUS.equals(msgType)) {
                GlobalSession globalSession = raftSessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                if (globalSession != null) {
                    GlobalStatus status = msg.getGlobalStatus();
                    globalSession.setStatus(status);
                    raftSessionManager.updateGlobalSessionStatus(globalSession, status);
                }
            } else if (REMOVE_BRANCH_SESSION.equals(msgType)) {
                GlobalSession globalSession = raftSessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                if (globalSession != null) {
                    BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                    if (branchSession != null) {
                        raftSessionManager.removeBranchSession(globalSession, branchSession);
                    }
                }
            } else if (RELEASE_GLOBAL_SESSION_LOCK.equals(msgType)) {
                GlobalSession globalSession =
                    SessionHolder.getRootSessionManager().findGlobalSession(msg.getGlobalSession().getXid());
                if (globalSession != null) {
                    RaftLockManager.LOCK_MANAGER.releaseGlobalSessionLock(globalSession);
                }
            } else if (REMOVE_GLOBAL_SESSION.equals(msgType)) {
                GlobalSession globalSession = sessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                raftSessionManager.getFileSessionManager().removeGlobalSession(globalSession);
                SessionHolder.getRootSessionManager().removeGlobalSession(globalSession);
            } else if (UPDATE_BRANCH_SESSION_STATUS.equals(msgType)) {
                GlobalSession globalSession = sessionManager.findGlobalSession(msg.getBranchSession().getXid());
                if (globalSession != null) {
                    BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                    if (branchSession != null) {
                        BranchStatus status = msg.getBranchStatus();
                        branchSession.setStatus(status);
                        raftSessionManager.updateBranchSessionStatus(branchSession, status);
                    }
                }
            }
        } else if (raftSyncMsg instanceof RaftOnRequestMsg) {
            RaftOnRequestMsg raftOnRequestMsg = (RaftOnRequestMsg)raftSyncMsg;
            onRequestProcessor.onRequestMessage(null, raftOnRequestMsg.getRpcMessage(), false,
                raftOnRequestMsg.getRpcContext());
        } else if (raftSyncMsg instanceof RaftCoreSyncMsg) {
            RaftCoreSyncMsg coreSyncMsg = (RaftCoreSyncMsg)raftSyncMsg;
            core.doCommit(coreSyncMsg.getBranchStatusMap(), coreSyncMsg.getXid(), false);
        }
    }

    @Override
    public void setOnRequestProcessor(ServerOnRequestProcessor onRequestProcessor) {
        this.onRequestProcessor = onRequestProcessor;
    }

    public DefaultCore getCore() {
        return core;
    }

    public void setCore(DefaultCore core) {
        this.core = core;
    }
}
