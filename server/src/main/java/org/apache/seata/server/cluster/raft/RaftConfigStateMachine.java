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

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.InvokeContext;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.cluster.raft.execute.RaftMsgExecute;
import org.apache.seata.server.cluster.raft.execute.config.ConfigOperationExecute;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.ConfigOperationResponse;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.config.ConfigSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.metadata.ConfigLeaderMetadataSnapshotFile;
import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftClusterMetadataMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.closure.ConfigClosure;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.raft.util.RaftConfigTaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.apache.seata.common.Constants.*;
import static org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType.*;

/**
 * The type raft config state machine.
 */
public class RaftConfigStateMachine extends StateMachineAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConfigStateMachine.class);

    private final String group;

    private final List<StoreSnapshotFile> snapshotFiles = new ArrayList<>();

    private static final Map<RaftSyncMsgType, RaftMsgExecute<?>> EXECUTES = new HashMap<>();

    private volatile RaftClusterMetadata raftClusterMetadata = new RaftClusterMetadata();

    private final Lock lock = new ReentrantLock();

    private static final ScheduledThreadPoolExecutor RESYNC_METADATA_POOL = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("reSyncMetadataPool", 1, true));

    /**
     * Leader term
     */
    private final AtomicLong leaderTerm = new AtomicLong(-1);

    /**
     * current term
     */
    private final AtomicLong currentTerm = new AtomicLong(-1);

    private final AtomicBoolean initSync = new AtomicBoolean(false);

    private ScheduledFuture<?> scheduledFuture;

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    public RaftConfigStateMachine(String group) {
        this.group = group;

        EXECUTES.put(REFRESH_CLUSTER_METADATA, syncMsg -> {
            refreshClusterMetadata(syncMsg);
            return null;
        });
        registryStoreSnapshotFile(new ConfigLeaderMetadataSnapshotFile(group));
        registryStoreSnapshotFile(new ConfigSnapshotFile(group));
        EXECUTES.put(CONFIG_OPERATION, new ConfigOperationExecute());
        EXECUTES.put(REFRESH_CLUSTER_METADATA, syncMsg -> {
            refreshClusterMetadata(syncMsg);
            return null;
        });
        this.scheduledFuture =
                RESYNC_METADATA_POOL.scheduleAtFixedRate(() -> syncCurrentNodeInfo(group), 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()) {
            Closure done = iterator.done();
            if (done != null) {
                // leader does not need to be serialized, just execute the task directly
                if (done instanceof ConfigClosure) {
                    ConfigClosure configClosure = (ConfigClosure) done;
                    RaftBaseMsg msg = configClosure.getRaftBaseMsg();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("sync msg: {}", msg);
                    }
                    ConfigOperationResponse response = (ConfigOperationResponse) onExecuteRaft(msg);
                    configClosure.getResponse().setSuccess(response.isSuccess());
                    configClosure.getResponse().setResult(response.getResult());
                    configClosure.getResponse().setErrMsg(response.getErrMsg());
                    configClosure.run(Status.OK());
                } else {
                    // If it's not a ConfigClosure, just run it with OK status
                    done.run(Status.OK());
                }
            } else {
                ByteBuffer byteBuffer = iterator.getData();
                // if data is empty, it is only a heartbeat event and can be ignored
                if (byteBuffer != null && byteBuffer.hasRemaining()) {
                    RaftBaseMsg msg = (RaftBaseMsg) RaftSyncMessageSerializer.decode(byteBuffer.array()).getBody();
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
        this.leaderTerm.set(term);
        LOGGER.info("groupId: {}, onLeaderStart: term={}.", group, term);
        this.currentTerm.set(term);
        syncMetadata();
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
        CompletableFuture.runAsync(() -> syncCurrentNodeInfo(ctx.getLeaderId()), RESYNC_METADATA_POOL);
    }

    @Override
    public void onConfigurationCommitted(Configuration conf) {
        LOGGER.info("groupId: {}, onConfigurationCommitted: {}.", group, conf);
        RouteTable.getInstance().updateConfiguration(group, conf);
        if (isLeader()) {
            lock.lock();
            try {
                List<PeerId> newFollowers = conf.getPeers();
                Set<PeerId> newLearners = conf.getLearners();
                List<Node> currentFollowers = raftClusterMetadata.getFollowers();
                if (CollectionUtils.isNotEmpty(newFollowers)) {
                    raftClusterMetadata.setFollowers(currentFollowers.stream()
                            .filter(node -> contains(node, newFollowers)).collect(Collectors.toList()));
                }
                if (CollectionUtils.isNotEmpty(newLearners)) {
                    raftClusterMetadata.setLearner(raftClusterMetadata.getLearner().stream()
                            .filter(node -> contains(node, newLearners)).collect(Collectors.toList()));
                }
                syncMetadata();
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean contains(Node node, Collection<PeerId> list) {
        if (node.getInternal() == null) {
            return true;
        }
        PeerId nodePeer = new PeerId(node.getInternal().getHost(), node.getInternal().getPort());
        return list.contains(nodePeer);
    }

    public void syncMetadata() {
        if (isLeader()) {
            SeataClusterContext.bindGroup(group);
            try {
                RaftClusterMetadataMsg raftClusterMetadataMsg =
                        new RaftClusterMetadataMsg(changeOrInitRaftClusterMetadata());
                RaftConfigTaskUtil.createTask(status -> refreshClusterMetadata(raftClusterMetadataMsg),
                        raftClusterMetadataMsg, null);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                SeataClusterContext.unbindGroup();
            }
        }
    }

    private Object onExecuteRaft(RaftBaseMsg msg) {
        RaftMsgExecute<?> execute = EXECUTES.get(msg.getMsgType());
        if (execute == null) {
            throw new RuntimeException(
                    "the state machine does not allow events that cannot be executed, please feedback the information to the Seata community !!! msg: "
                            + msg);
        }
        try {
            return execute.execute(msg);
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

    public RaftClusterMetadata changeOrInitRaftClusterMetadata() {
        raftClusterMetadata.setTerm(this.currentTerm.get());
        Node leaderNode = raftClusterMetadata.getLeader();
        RaftConfigServer raftServer = RaftConfigServerManager.getRaftServer();
        PeerId cureentPeerId = raftServer.getServerId();
        // After the re-election, the leader information may be different from the latest leader, and you need to replace the leader information
        if (leaderNode == null || (leaderNode.getInternal() != null
                && !cureentPeerId.equals(new PeerId(leaderNode.getInternal().getHost(), leaderNode.getInternal().getPort())))) {
            Node leader =
                    raftClusterMetadata.createNode(XID.getIpAddress() == null ? NetUtil.getLocalIp() : XID.getIpAddress(), XID.getPort() <= 0? 8091 : XID.getPort(), raftServer.getServerId().getPort(),
                            Integer.parseInt(
                                    ((Environment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT))
                                            .getProperty("server.port", String.valueOf(7091))),
                            group, Collections.emptyMap());
            leader.setRole(ClusterRole.LEADER);
            raftClusterMetadata.setLeader(leader);
        }
        return raftClusterMetadata;
    }

    public void refreshClusterMetadata(RaftBaseMsg syncMsg) {
        // Directly receive messages from the leader and update the cluster metadata
        raftClusterMetadata = ((RaftClusterMetadataMsg)syncMsg).getRaftClusterMetadata();
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null) {
            ((ApplicationEventPublisher)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                    .publishEvent(new ClusterChangeEvent(this, group, raftClusterMetadata.getTerm(), this.isLeader()));
            LOGGER.info("groupId: {}, refresh cluster metadata: {}", group, raftClusterMetadata);
        }

    }

    private void syncCurrentNodeInfo(String group) {
        if (initSync.get()) {
            return;
        }
        try {
            RouteTable.getInstance().refreshLeader(RaftConfigServerManager.getCliClientServiceInstance(), group, 1000);
            PeerId peerId = RouteTable.getInstance().selectLeader(group);
            if (peerId != null) {
                syncCurrentNodeInfo(peerId);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void syncCurrentNodeInfo(PeerId leaderPeerId) {
        try {
            // Ensure that the current leader must be version 2.1 or later to synchronize the operation
            Node leader = raftClusterMetadata.getLeader();
            if (leader != null && StringUtils.isNotBlank(leader.getVersion()) && initSync.compareAndSet(false, true)) {
                RaftConfigServer raftServer = RaftConfigServerManager.getRaftServer();
                PeerId cureentPeerId = raftServer.getServerId();
                Node node = raftClusterMetadata.createNode(XID.getIpAddress() == null ? NetUtil.getLocalIp() : XID.getIpAddress(), XID.getPort() <= 0? 8091 : XID.getPort(), cureentPeerId.getPort(),
                        Integer.parseInt(
                                ((Environment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT))
                                        .getProperty("server.port", String.valueOf(7091))),
                        group, Collections.emptyMap());
                InvokeContext invokeContext = new InvokeContext();
                PutNodeMetadataRequest putNodeInfoRequest = new PutNodeMetadataRequest(node);
                Configuration configuration = RouteTable.getInstance().getConfiguration(group);
                node.setRole(
                        configuration.getPeers().contains(cureentPeerId) ? ClusterRole.FOLLOWER : ClusterRole.LEARNER);
                invokeContext.put(com.alipay.remoting.InvokeContext.BOLT_CUSTOM_SERIALIZER,
                        SerializerType.JACKSON.getCode());
                CliClientServiceImpl cliClientService =
                        (CliClientServiceImpl)RaftConfigServerManager.getCliClientServiceInstance();
                // The previous leader may be an old snapshot or log playback, which is not accurate, and you
                // need to get the leader again
                cliClientService.getRpcClient().invokeAsync(leaderPeerId.getEndpoint(), putNodeInfoRequest,
                        invokeContext, (result, err) -> {
                            if (err == null) {
                                PutNodeMetadataResponse putNodeMetadataResponse = (PutNodeMetadataResponse)result;
                                if (putNodeMetadataResponse.isSuccess()) {
                                    scheduledFuture.cancel(true);
                                    LOGGER.info("sync node info to leader: {}, result: {}", leaderPeerId, result);
                                } else {
                                    initSync.compareAndSet(true, false);
                                    LOGGER.info(
                                            "sync node info to leader: {}, result: {}, retry will be made at the time of the re-election or after 10 seconds",
                                            leaderPeerId, result);
                                }
                            } else {
                                initSync.compareAndSet(true, false);
                                LOGGER.error("sync node info to leader: {}, error: {}", leaderPeerId, err.getMessage(),
                                        err);
                            }
                        }, 30000);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void changeNodeMetadata(Node node) {
        lock.lock();
        try {
            List<Node> list = node.getRole() == ClusterRole.FOLLOWER ? raftClusterMetadata.getFollowers()
                    : raftClusterMetadata.getLearner();
            // If the node currently exists, modify it
            for (Node follower : list) {
                Node.Endpoint endpoint = follower.getInternal();
                if (endpoint != null) {
                    // change old follower node metadata
                    if (endpoint.getHost().equals(node.getInternal().getHost())
                            && endpoint.getPort() == node.getInternal().getPort()) {
                        follower.setTransaction(node.getTransaction());
                        follower.setControl(node.getControl());
                        follower.setGroup(group);
                        follower.setMetadata(node.getMetadata());
                        follower.setVersion(node.getVersion());
                        follower.setRole(node.getRole());
                        return;
                    }
                }
            }
            // add new node node metadata
            list.add(node);
            syncMetadata();
        } finally {
            lock.unlock();
        }
    }


}
