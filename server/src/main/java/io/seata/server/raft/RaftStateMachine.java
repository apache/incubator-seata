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
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Utils;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSyncMsg;
import io.seata.server.storage.raft.lock.RaftLockManager;
import io.seata.server.storage.raft.session.RaftSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.alipay.remoting.serialization.SerializerManager.Hessian2;
import static io.seata.server.session.SessionHolder.ASYNC_COMMITTING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.RETRY_COMMITTING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.RETRY_ROLLBACKING_SESSION_MANAGER_NAME;
import static io.seata.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ACQUIRE_LOCK;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.REMOVE_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.UPDATE_BRANCH_SESSION_STATUS;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 * @author funkye
 */
public class RaftStateMachine extends StateMachineAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(RaftStateMachine.class);

    /**
     * Leader term
     */
    private final AtomicLong leaderTerm = new AtomicLong(-1);
    /**
     * counter value
     */
    private AtomicLong value = new AtomicLong(0);

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    public RaftStateMachine() {
    }

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()) {
            Closure processor = null;
            if (iterator.done() != null) {
                processor = iterator.done();
            } else {
                try {
                    SessionManager sessionManager = null;
                    RaftSyncMsg msg = SerializerManager.getSerializer(Hessian2).deserialize(iterator.getData().array(),
                        RaftSyncMsg.class.getName());
                    RaftSyncMsg.MsgType msgType = msg.getMsgType();
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
                    RaftSessionManager raftSessionManager =
                        sessionManager != null ? (RaftSessionManager)sessionManager : null;
                    LOG.info("state machine synchronization,task:{},sessionManager:{}", msgType,
                        sessionName != null ? sessionName : ROOT_SESSION_MANAGER_NAME);
                    if (ADD_GLOBAL_SESSION.equals(msgType)) {
                        GlobalSession globalSession = SessionConverter.convertGlobalSession(msg.getGlobalSession());
                        raftSessionManager.getFileSessionManager().addGlobalSession(globalSession);
                    } else if (ACQUIRE_LOCK.equals(msgType)) {
                        GlobalSession globalSession = SessionHolder.getRootSessionManager().findGlobalSession(msg.getBranchSession().getXid());
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
                        GlobalStatus status = msg.getGlobalStatus();
                        globalSession.setStatus(status);
                        raftSessionManager.updateGlobalSessionStatus(globalSession, status);
                    } else if (REMOVE_BRANCH_SESSION.equals(msgType)) {
                        GlobalSession globalSession = raftSessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                        BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                        raftSessionManager.removeBranchSession(globalSession, branchSession);
                    } else if (RELEASE_GLOBAL_SESSION_LOCK.equals(msgType)) {
                        GlobalSession globalSession = SessionHolder.getRootSessionManager().findGlobalSession(msg.getGlobalSession().getXid());
                        RaftLockManager.LOCK_MANAGER.releaseGlobalSessionLock(globalSession);
                    } else if (REMOVE_GLOBAL_SESSION.equals(msgType)) {
                        GlobalSession globalSession = sessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                        raftSessionManager.getFileSessionManager().removeGlobalSession(globalSession);
                    } else if (UPDATE_BRANCH_SESSION_STATUS.equals(msgType)) {
                        GlobalSession globalSession = sessionManager.findGlobalSession(msg.getBranchSession().getXid());
                        BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                        BranchStatus status = msg.getBranchStatus();
                        branchSession.setStatus(status);
                        raftSessionManager.updateBranchSessionStatus(branchSession, status);
                    }
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
        final long currVal = this.value.get();
        Utils.runInThread(() -> {
            final RaftSnapshotFile snapshot = new RaftSnapshotFile(writer.getPath() + File.separator + "data");
            if (snapshot.save(currVal)) {
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
            this.value.set(snapshot.load());
            return true;
        } catch (final IOException e) {
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
}
