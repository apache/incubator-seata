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
package io.seata.server.cluster.raft;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.store.StoreMode;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.server.AbstractTCInboundHandler;
import io.seata.server.cluster.raft.context.RaftClusterContext;
import io.seata.server.cluster.raft.snapshot.SessionSnapshotFile;
import io.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import io.seata.server.cluster.raft.execute.RaftMsgExecute;
import io.seata.server.cluster.raft.execute.branch.AddBranchSessionExecute;
import io.seata.server.cluster.raft.execute.branch.RemoveBranchSessionExecute;
import io.seata.server.cluster.raft.execute.branch.UpdateBranchSessionExecute;
import io.seata.server.cluster.raft.execute.global.AddGlobalSessionExecute;
import io.seata.server.cluster.raft.execute.global.RemoveGlobalSessionExecute;
import io.seata.server.cluster.raft.execute.global.UpdateGlobalSessionExecute;
import io.seata.server.cluster.raft.execute.lock.BranchReleaseLockExecute;
import io.seata.server.cluster.raft.execute.lock.GlobalReleaseLockExecute;
import io.seata.server.cluster.listener.ClusterChangeEvent;
import io.seata.server.cluster.raft.msg.RaftSyncMsgSerializer;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.ADD_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.RELEASE_BRANCH_SESSION_LOCK;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.REMOVE_BRANCH_SESSION;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.UPDATE_BRANCH_SESSION_STATUS;
import static io.seata.server.storage.raft.RaftSessionSyncMsg.MsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 * @author funkye
 */
public class RaftStateMachine extends StateMachineAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftStateMachine.class);

    private final String mode;
    
    private final String group;

    private final List<StoreSnapshotFile> snapshotFiles = new ArrayList<>();

    private static final Map<MsgType, RaftMsgExecute<?>> EXECUTES = new HashMap<>();

    /**
     * Leader term
     */
    private final AtomicLong leaderTerm = new AtomicLong(-1);

    /**
     * current term
     */
    private final AtomicLong currentTerm = new AtomicLong(-1);

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    public RaftStateMachine(String group) {
        this.group = group;
        mode = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_MODE);
        if (StoreMode.RAFT.getName().equalsIgnoreCase(mode)) {
            registryStoreSnapshotFile(new SessionSnapshotFile(group));
            EXECUTES.put(ADD_GLOBAL_SESSION, new AddGlobalSessionExecute());
            EXECUTES.put(ADD_BRANCH_SESSION, new AddBranchSessionExecute());
            EXECUTES.put(REMOVE_BRANCH_SESSION, new RemoveBranchSessionExecute());
            EXECUTES.put(UPDATE_GLOBAL_SESSION_STATUS, new UpdateGlobalSessionExecute());
            EXECUTES.put(RELEASE_GLOBAL_SESSION_LOCK, new GlobalReleaseLockExecute());
            EXECUTES.put(REMOVE_GLOBAL_SESSION, new RemoveGlobalSessionExecute());
            EXECUTES.put(UPDATE_BRANCH_SESSION_STATUS, new UpdateBranchSessionExecute());
            EXECUTES.put(RELEASE_BRANCH_SESSION_LOCK, new BranchReleaseLockExecute());
        }
    }

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()) {
            if (iterator.done() != null) {
                // leader does not need to be serialized, just execute the task directly
                Optional.ofNullable(iterator.done()).ifPresent(done -> done.run(Status.OK()));
            } else {
                ByteBuffer byteBuffer = iterator.getData();
                // if data is empty, it is only a heartbeat event and can be ignored
                if (byteBuffer != null && byteBuffer.hasRemaining()) {
                    RaftSessionSyncMsg msg =
                        (RaftSessionSyncMsg)RaftSyncMsgSerializer.decode(byteBuffer.array()).getBody();
                    // follower executes the corresponding task
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("sync msg: {}", msg);
                    }
                    onExecuteRaft(msg);
                }
            }
            iterator.next();
        }
    }

    @Override
    public void onSnapshotSave(final SnapshotWriter writer, final Closure done) {
        if (!StringUtils.equals(StoreConfig.SessionMode.RAFT.getName(), mode)) {
            done.run(Status.OK());
            return;
        }
        for (StoreSnapshotFile snapshotFile : snapshotFiles) {
            Status status = snapshotFile.save(writer);
            if (!status.isOk()) {
                done.run(status);
                return;
            }
        }
        done.run(Status.OK());
    }

    @Override
    public boolean onSnapshotLoad(final SnapshotReader reader) {
        if (!StringUtils.equals(StoreConfig.SessionMode.RAFT.getName(), mode)) {
            return true;
        }
        if (isLeader()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Leader is not supposed to load snapshot");
            }
            return false;
        }
        for (StoreSnapshotFile snapshotFile : snapshotFiles) {
            if (!snapshotFile.load(reader)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onLeaderStart(final long term) {
        boolean leader = isLeader();
        this.leaderTerm.set(term);
        LOGGER.info("groupId: {}, onLeaderStart: term={}.", group, term);
        this.currentTerm.set(term);
        if (!leader && RaftServerFactory.getInstance().isRaftMode()) {
            CompletableFuture.runAsync(() -> {
                LOGGER.info("groupId: {}, session map: {} ", group,
                    SessionHolder.getRootSessionManager().allSessions().size());
                RaftClusterContext.bindGroup(group);
                try {
                    SessionHolder.reload(SessionHolder.getRootSessionManager().allSessions(),
                        StoreConfig.SessionMode.RAFT, false);
                } finally {
                    RaftClusterContext.unbindGroup();
                }
            });
        }
        AbstractTCInboundHandler.setPrevent(group, false);
        // become the leader again,reloading global session
        ((ApplicationEventPublisher)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
            .publishEvent(new ClusterChangeEvent(this, group, term));
    }

    @Override
    public void onLeaderStop(final Status status) {
        this.leaderTerm.set(-1);
        LOGGER.info("groupId: {}, onLeaderStop: status={}.", group, status);
        AbstractTCInboundHandler.setPrevent(group, true);
    }

    @Override
    public void onStopFollowing(final LeaderChangeContext ctx) {
        LOGGER.info("groupId: {}, onStopFollowing: {}.", group, ctx);
    }

    @Override
    public void onStartFollowing(final LeaderChangeContext ctx) {
        LOGGER.info("groupId: {}, onStartFollowing: {}.", group, ctx);
        this.currentTerm.set(ctx.getTerm());
        AbstractTCInboundHandler.setPrevent(group, true);
        ((ApplicationEventPublisher)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
            .publishEvent(new ClusterChangeEvent(this, group, ctx.getTerm()));
    }

    @Override
    public void onConfigurationCommitted(Configuration conf) {
        LOGGER.info("groupId: {}, onConfigurationCommitted: {}.", group, conf);
        RouteTable.getInstance().updateConfiguration(group, conf);
        ((ApplicationEventPublisher)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
            .publishEvent(new ClusterChangeEvent(this, group));
    }

    private void onExecuteRaft(RaftSessionSyncMsg msg) {
        RaftMsgExecute<?> execute = EXECUTES.get(msg.getMsgType());
        if (execute == null) {
            throw new RuntimeException(
                "the state machine does not allow events that cannot be executed, please feedback the information to the Seata community !!! msg: "
                    + msg);
        }
        try {
            execute.execute(msg);
        } catch (Throwable e) {
            LOGGER.error("Message synchronization failure: {}, msgType: {}", e.getMessage(), msg.getMsgType(), e);
            throw new RuntimeException(e);
        }
    }

    public AtomicLong getCurrentTerm() {
        return currentTerm;
    }

    public void registryStoreSnapshotFile(StoreSnapshotFile storeSnapshotFile) {
        snapshotFiles.add(storeSnapshotFile);
    }

}
