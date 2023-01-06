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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.common.store.StoreMode;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.common.ConfigurationKeys.SERVER_RAFT_PORT_CAMEL;
import static io.seata.common.DefaultValues.DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static io.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_APPLY_BATCH;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_AUTO_JOIN;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_DISRUPTOR_BUFFER_SIZE;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_APPEND_BUFFER_SIZE;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_SNAPSHOT_INTERVAL;
import static java.io.File.separator;

/**
 * @author funkye
 */
public class RaftServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftServerFactory.class);

    private static final Map<String/*group*/, RaftServer/*raft-group-cluster*/> RAFT_SERVER_MAP = new HashMap<>();

    private Boolean raftMode = false;

    private static final io.seata.config.Configuration CONFIG = ConfigurationFactory.getInstance();

    public static RaftServerFactory getInstance() {
        return SingletonHandler.INSTANCE;
    }

    public static CliService getCliServiceInstance() {
        return SingletonHandler.CLI_SERVICE;
    }

    public static CliClientService getCliClientServiceInstance() {
        return SingletonHandler.CLI_CLIENT_SERVICE;
    }

    public void init() {
        String initConfStr = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_CLUSTER);
        if (StringUtils.isBlank(initConfStr)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("initialize SofaJRaft fail , server.raft.cluster is null");
            }
            return;
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
        }
        int port = Integer.parseInt(System.getProperty(SERVER_RAFT_PORT_CAMEL, "0"));
        String host = XID.getIpAddress();
        int nettyPort = XID.getPort();
        PeerId serverId = null;
        if (port <= 0) {
            // Highly available deployments require different nodes
            for (PeerId peer : initConf.getPeers()) {
                if (StringUtils.equals(peer.getIp(), host) && peer.getIdx() == nettyPort) {
                    serverId = peer;
                    break;
                }
            }
        } else {
            // Local debugging use
            serverId = new PeerId(host, port, nettyPort);
        }
        String mode = CONFIG.getConfig(ConfigurationKeys.STORE_MODE);
        StoreMode storeMode = StoreMode.get(mode);
        if (storeMode.equals(StoreMode.RAFT)) {
            raftMode = true;
        }
        final String dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
            + separator + serverId.getPort();
        final NodeOptions nodeOptions = initNodeOptions(initConf);
        try {
            RaftServer raftServer = new RaftServer(dataPath, DEFAULT_SEATA_GROUP, serverId, nodeOptions);
            // as the foundation for multi raft group in the future
            RAFT_SERVER_MAP.put(DEFAULT_SEATA_GROUP, raftServer);
            LOGGER.info("started counter server at port:{}", raftServer.getNode().getNodeId().getPeerId().getPort());
        } catch (IOException e) {
            throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage());
        }
        // whether to join an existing cluster
        if (CONFIG.getBoolean(SERVER_RAFT_AUTO_JOIN, false)) {
            List<PeerId> currentPeers = null;
            try {
                currentPeers = getCliServiceInstance().getPeers(DEFAULT_SEATA_GROUP, initConf);
            } catch (Exception e) {
                // In the first deployment, the leader cannot be found
            }
            if (CollectionUtils.isNotEmpty(currentPeers)) {
                if (!currentPeers.contains(serverId)) {
                    Status status = getCliServiceInstance().addPeer(DEFAULT_SEATA_GROUP, initConf, serverId);
                    if (!status.isOk()) {
                        LOGGER.error("failed to join the RAFT cluster: {}. Please check the status of the cluster",
                            initConfStr);
                    }
                }
            }
        }
    }

    public RaftServer getRaftServer() {
        return getRaftServer(DEFAULT_SEATA_GROUP);
    }

    public RaftServer getRaftServer(String group) {
        return RAFT_SERVER_MAP.get(group);
    }

    public RaftStateMachine getStateMachine() {
        return getStateMachine(DEFAULT_SEATA_GROUP);
    }

    public RaftStateMachine getStateMachine(String group) {
        return RAFT_SERVER_MAP.get(group).getRaftStateMachine();
    }


    public Boolean isLeader() {
        return isLeader(DEFAULT_SEATA_GROUP);
    }

    public Boolean isLeader(String group) {
        AtomicReference<RaftStateMachine> stateMachine = new AtomicReference<>();
        Optional.ofNullable(RAFT_SERVER_MAP.get(group)).ifPresent(raftServer -> {
            stateMachine.set(raftServer.getRaftStateMachine());
        });
        RaftStateMachine raftStateMachine = stateMachine.get();
        return !isRaftMode() && RAFT_SERVER_MAP.isEmpty() || (raftStateMachine != null && raftStateMachine.isLeader());
    }

    public Boolean isRaftMode() {
        return raftMode;
    }

    public Boolean isNotRaftModeLeader() {
        return isNotRaftModeLeader(DEFAULT_SEATA_GROUP);
    }

    public Boolean isNotRaftModeLeader(String group) {
        return !isLeader(group) && isRaftMode();
    }

    private RaftOptions initRaftOptions() {
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setApplyBatch(CONFIG.getInt(SERVER_RAFT_APPLY_BATCH, raftOptions.getApplyBatch()));
        raftOptions.setMaxAppendBufferSize(
            CONFIG.getInt(SERVER_RAFT_MAX_APPEND_BUFFER_SIZE, raftOptions.getMaxAppendBufferSize()));
        raftOptions.setDisruptorBufferSize(
            CONFIG.getInt(SERVER_RAFT_DISRUPTOR_BUFFER_SIZE, raftOptions.getDisruptorBufferSize()));
        raftOptions.setMaxReplicatorInflightMsgs(
            CONFIG.getInt(SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS, raftOptions.getMaxReplicatorInflightMsgs()));
        return raftOptions;
    }

    private NodeOptions initNodeOptions(Configuration initConf) {
        NodeOptions nodeOptions = new NodeOptions();
        // enable the CLI service.
        nodeOptions.setDisableCli(false);
        // snapshot should be made every 30 seconds
        Integer snapshotInterval = CONFIG.getInt(SERVER_RAFT_SNAPSHOT_INTERVAL, 60 * 10);
        nodeOptions.setSnapshotIntervalSecs(snapshotInterval);
        nodeOptions.setRaftOptions(initRaftOptions());
        // set the election timeout to 2 second
        nodeOptions
            .setElectionTimeoutMs(CONFIG.getInt(SERVER_RAFT_ELECTION_TIMEOUT_MS, DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS));
        // set up the initial cluster configuration
        nodeOptions.setInitialConf(initConf);
        return nodeOptions;
    }

    private static class SingletonHandler {
        private static final RaftServerFactory INSTANCE = new RaftServerFactory();
        private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
        private static final CliClientService CLI_CLIENT_SERVICE = new CliClientServiceImpl();
        static {
            CLI_CLIENT_SERVICE.init(new CliOptions());
        }
    }

}
