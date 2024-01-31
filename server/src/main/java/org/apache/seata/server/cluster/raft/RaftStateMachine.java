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
package org.apache.seata.server.cluster.raft;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.cluster.raft.snapshot.metadata.LeaderMetadataSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.session.SessionSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.cluster.raft.execute.RaftMsgExecute;
import org.apache.seata.server.cluster.raft.execute.branch.AddBranchSessionExecute;
import org.apache.seata.server.cluster.raft.execute.branch.RemoveBranchSessionExecute;
import org.apache.seata.server.cluster.raft.execute.branch.UpdateBranchSessionExecute;
import org.apache.seata.server.cluster.raft.execute.global.AddGlobalSessionExecute;
import org.apache.seata.server.cluster.raft.execute.global.RemoveGlobalSessionExecute;
import org.apache.seata.server.cluster.raft.execute.global.UpdateGlobalSessionExecute;
import org.apache.seata.server.cluster.raft.execute.lock.BranchReleaseLockExecute;
import org.apache.seata.server.cluster.raft.execute.lock.GlobalReleaseLockExecute;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftClusterMetadataMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static org.apache.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.ADD_BRANCH_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.ADD_GLOBAL_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.REFRESH_CLUSTER_METADATA;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.RELEASE_BRANCH_SESSION_LOCK;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.RELEASE_GLOBAL_SESSION_LOCK;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.REMOVE_BRANCH_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.REMOVE_GLOBAL_SESSION;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.UPDATE_BRANCH_SESSION_STATUS;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.UPDATE_GLOBAL_SESSION_STATUS;

/**
 */
public class RaftStateMachine extends StateMachineAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftStateMachine.class);

    private final String mode;

    private final String group;

    private final List<StoreSnapshotFile> snapshotFiles = new ArrayList<>();

    private static final Map<RaftSyncMsgType, RaftMsgExecute<?>> EXECUTES = new HashMap<>();

    private volatile RaftClusterMetadata raftClusterMetadata;

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
        mode = StoreConfig.getSessionMode().getName();
        EXECUTES.put(REFRESH_CLUSTER_METADATA, syncMsg -> {
            refreshClusterMetadata(syncMsg);
            return null;
        });
        registryStoreSnapshotFile(new LeaderMetadataSnapshotFile(group));
        if (StoreConfig.StoreMode.RAFT.getName().equalsIgnoreCase(mode)) {
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
            Closure done = iterator.done();
            if (done != null) {
                // leader does not need to be serialized, just execute the task directly
                done.run(Status.OK());
            } else {
                ByteBuffer byteBuffer = iterator.getData();
                // if data is empty, it is only a heartbeat event and can be ignored
                if (byteBuffer != null && byteBuffer.hasRemaining()) {
                    RaftBaseMsg msg = (RaftBaseMsg)RaftSyncMessageSerializer.decode(byteBuffer.array()).getBody();
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
        long current = System.currentTimeMillis();
        for (StoreSnapshotFile snapshotFile : snapshotFiles) {
            Status status = snapshotFile.save(writer);
            if (!status.isOk()) {
                done.run(status);
                return;
            }
        }
        LOGGER.info("groupId: {}, onSnapshotSave cost: {} ms.", group, System.currentTimeMillis() - current);
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
        long current = System.currentTimeMillis();
        for (StoreSnapshotFile snapshotFile : snapshotFiles) {
            if (!snapshotFile.load(reader)) {
                return false;
            }
        }
        LOGGER.info("groupId: {}, onSnapshotLoad cost: {} ms.", group, System.currentTimeMillis() - current);
        return true;
    }

    @Override
    public void onLeaderStart(final long term) {
        boolean leader = isLeader();
        this.leaderTerm.set(term);
        LOGGER.info("groupId: {}, onLeaderStart: term={}.", group, term);
        this.currentTerm.set(term);
        SeataClusterContext.bindGroup(group);
        syncMetadata();
        if (!leader && RaftServerManager.isRaftMode()) {
            CompletableFuture.runAsync(() -> {
                LOGGER.info("reload session, groupId: {}, session map size: {} ", group,
                    SessionHolder.getRootSessionManager().allSessions().size());
                SeataClusterContext.bindGroup(group);
                try {
                    // become the leader again,reloading global session
                    SessionHolder.reload(SessionHolder.getRootSessionManager().allSessions(),
                        StoreConfig.SessionMode.RAFT, false);
                } finally {
                    SeataClusterContext.unbindGroup();
                }
            });
        }
    }

    @Override
    public void onLeaderStop(final Status status) {
        this.leaderTerm.set(-1);
        LOGGER.info("groupId: {}, onLeaderStop: status={}.", group, status);
    }

    @Override
    public void onStopFollowing(final LeaderChangeContext ctx) {
        LOGGER.info("groupId: {}, onStopFollowing: {}.", group, ctx);
    }

    @Override
    public void onStartFollowing(final LeaderChangeContext ctx) {
        LOGGER.info("groupId: {}, onStartFollowing: {}.", group, ctx);
        this.currentTerm.set(ctx.getTerm());
    }

    @Override
    public void onConfigurationCommitted(Configuration conf) {
        LOGGER.info("groupId: {}, onConfigurationCommitted: {}.", group, conf);
        syncMetadata();
        RouteTable.getInstance().updateConfiguration(group, conf);
    }
    
    private void syncMetadata() {
        if (isLeader()) {
            SeataClusterContext.bindGroup(group);
            try {
                RaftClusterMetadataMsg raftClusterMetadataMsg =
                    new RaftClusterMetadataMsg(createNewRaftClusterMetadata());
                RaftTaskUtil.createTask(status -> refreshClusterMetadata(raftClusterMetadataMsg),
                    raftClusterMetadataMsg, null);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                SeataClusterContext.unbindGroup();
            }
        }
    }

    private void onExecuteRaft(RaftBaseMsg msg) {
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

    public RaftClusterMetadata getRaftLeaderMetadata() {
        return raftClusterMetadata;
    }

    public void setRaftLeaderMetadata(RaftClusterMetadata raftClusterMetadata) {
        this.raftClusterMetadata = raftClusterMetadata;
    }

    public RaftClusterMetadata createNewRaftClusterMetadata() {
        RaftClusterMetadata metadata = new RaftClusterMetadata(this.currentTerm.get());
        Node leader = metadata.createNode(XID.getIpAddress(), XID.getPort(),
            Integer.parseInt(((Environment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT))
                .getProperty("server.port", String.valueOf(8088))),
            group, Collections.emptyMap());
        leader.setRole(ClusterRole.LEADER);
        metadata.setLeader(leader);
        Configuration configuration = RouteTable.getInstance().getConfiguration(this.group);
        List<Node> learners = configuration.getLearners().stream().map(learner -> {
            int nettyPort = learner.getPort() - SERVICE_OFFSET_SPRING_BOOT;
            Node learnerNode = metadata.createNode(learner.getIp(), nettyPort, nettyPort - SERVICE_OFFSET_SPRING_BOOT,
                this.group, Collections.emptyMap());
            learnerNode.setRole(ClusterRole.LEARNER);
            return learnerNode;
        }).collect(Collectors.toList());
        metadata.setLearner(learners);
        List<Node> followers = configuration.getPeers().stream().map(follower -> {
            int nettyPort = follower.getPort() - SERVICE_OFFSET_SPRING_BOOT;
            Node followerNode = metadata.createNode(follower.getIp(), nettyPort, nettyPort - SERVICE_OFFSET_SPRING_BOOT,
                this.group, Collections.emptyMap());
            followerNode.setRole(ClusterRole.FOLLOWER);
            return followerNode;
        }).collect(Collectors.toList());
        metadata.setFollowers(followers);
        return metadata;
    }

    public void refreshClusterMetadata(RaftBaseMsg syncMsg) {
        raftClusterMetadata = ((RaftClusterMetadataMsg)syncMsg).getRaftClusterMetadata();
        ((ApplicationEventPublisher)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
            .publishEvent(new ClusterChangeEvent(this, group, raftClusterMetadata.getTerm(), this.isLeader()));
        LOGGER.info("groupId: {}, refresh cluster metadata: {}", group, raftClusterMetadata);
    }

}
