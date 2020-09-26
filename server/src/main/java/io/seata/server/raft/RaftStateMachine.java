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
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSyncMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.alipay.remoting.serialization.SerializerManager.Hessian2;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.REMOVE_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 * @author funkye
 */
public class RaftStateMachine extends StateMachineAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(RaftStateMachine.class);

    SessionManager sessionManager = SessionHolder.getRootSessionManager();

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

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()) {
            Closure processor = null;
            if (iterator.done() != null) {
                processor = iterator.done();
            } else {
                try {
                    RaftSyncMsg msg = SerializerManager.getSerializer(Hessian2).deserialize(iterator.getData().array(),
                        RaftSyncMsg.class.getName());
                    RaftSyncMsg.MsgType msgType = msg.getMsgType();
                    LOG.info("state machine synchronization,task:{}", msgType);
                    if (msgType.equals(ADD_GLOBAL_SESSION)) {
                        GlobalSession globalSession = SessionConverter.convertGlobalSession(msg.getGlobalSession());
                        SessionHolder.getRootSessionManager().addGlobalSession(globalSession);
                    } else if (msgType.equals(ADD_BRANCH_SESSION)) {
                        GlobalSession globalSession = sessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                        BranchSession branchSession = SessionConverter.convertBranchSession(msg.getBranchSession());
                        globalSession.addBranch(branchSession);
                        SessionHolder.getRootSessionManager().addBranchSession(globalSession, branchSession);
                    } else if (msgType.equals(UPDATE_GLOBAL_SESSION_STATUS)) {
                        GlobalSession globalSession = sessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                        SessionHolder.getRootSessionManager().updateGlobalSessionStatus(globalSession, msg.getGlobalStatus());
                    } else if (msgType.equals(REMOVE_BRANCH_SESSION)) {
                        GlobalSession globalSession = sessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                        BranchSession branchSession = globalSession.getBranch(msg.getBranchSession().getBranchId());
                        globalSession.removeBranch(branchSession);
                        SessionHolder.getRootSessionManager().removeBranchSession(globalSession, branchSession);
                    } else if (msgType.equals(REMOVE_GLOBAL_SESSION)) {
                        GlobalSession globalSession = sessionManager.findGlobalSession(msg.getGlobalSession().getXid());
                        sessionManager.removeGlobalSession(globalSession);
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