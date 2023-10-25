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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.FileRegistryServiceImpl;
import io.seata.discovery.registry.MultiRegistryFactory;
import io.seata.discovery.registry.RegistryService;
import io.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.ConfigurationKeys.SERVER_RAFT_PORT_CAMEL;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_SYNC;
import static io.seata.common.DefaultValues.DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static io.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static io.seata.common.ConfigurationKeys.SERVER_RAFT_APPLY_BATCH;
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

    private RpcServer rpcServer;

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
        String initConfStr = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
        StoreConfig.SessionMode storeMode = StoreConfig.getSessionMode();
        if (storeMode.equals(StoreConfig.SessionMode.RAFT)) {
            for (RegistryService<?> instance : MultiRegistryFactory.getInstances()) {
                if (!(instance instanceof FileRegistryServiceImpl)) {
                    throw new IllegalArgumentException("Raft store mode not support other Registration Center");
                }
            }
            raftMode = true;
        }
        if (StringUtils.isBlank(initConfStr)) {
            if (raftMode) {
                throw new IllegalArgumentException(
                    "Raft store mode must config: " + ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
            }
            return;
        } else {
            LOGGER.warn("raft mode and raft cluster is an experimental feature");
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
        }
        int port = Integer.parseInt(System.getProperty(SERVER_RAFT_PORT_CAMEL, "0"));
        PeerId serverId = null;
        String host = XID.getIpAddress();
        if (port <= 0) {
            // Highly available deployments require different nodes
            for (PeerId peer : initConf.getPeers()) {
                if (StringUtils.equals(peer.getIp(), host)) {
                    if (serverId != null) {
                        throw new IllegalArgumentException(
                            "server.raft.cluster has duplicate ip, For local debugging, use -Dserver.raftPort to specify the raft port");
                    }
                    serverId = peer;
                }
            }
        } else {
            // Local debugging use
            serverId = new PeerId(host, port);
        }
        final String dataPath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
            + separator + "raft" + separator + serverId.getPort();
        String group = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
        try {
            // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this separately
            this.rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
            RaftServer raftServer = new RaftServer(dataPath, group, serverId, initNodeOptions(initConf), this.rpcServer);
            // as the foundation for multi raft group in the future
            RAFT_SERVER_MAP.put(group, raftServer);
        } catch (IOException e) {
            throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
        }
    }

    public void start() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            try {
                raftServer.start();
            } catch (IOException e) {
                LOGGER.error("start seata server raft cluster error, group: {} ", group, e);
                throw new RuntimeException(e);
            }
            LOGGER.info("started seata server raft cluster, group: {} ", group);
        });
        if (!this.rpcServer.init(null)) {
            throw new RuntimeException("start raft node fail!");
        }
    }

    public RaftServer getRaftServer(String group) {
        return RAFT_SERVER_MAP.get(group);
    }

    public Collection<RaftServer> getRaftServers() {
        return RAFT_SERVER_MAP.values();
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

    private RaftOptions initRaftOptions() {
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setApplyBatch(CONFIG.getInt(SERVER_RAFT_APPLY_BATCH, raftOptions.getApplyBatch()));
        raftOptions.setMaxAppendBufferSize(
            CONFIG.getInt(SERVER_RAFT_MAX_APPEND_BUFFER_SIZE, raftOptions.getMaxAppendBufferSize()));
        raftOptions.setDisruptorBufferSize(
            CONFIG.getInt(SERVER_RAFT_DISRUPTOR_BUFFER_SIZE, raftOptions.getDisruptorBufferSize()));
        raftOptions.setMaxReplicatorInflightMsgs(
            CONFIG.getInt(SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS, raftOptions.getMaxReplicatorInflightMsgs()));
        raftOptions.setSync(CONFIG.getBoolean(SERVER_RAFT_SYNC, raftOptions.isSync()));
        return raftOptions;
    }

    private NodeOptions initNodeOptions(Configuration initConf) {
        NodeOptions nodeOptions = new NodeOptions();
        // enable the CLI service.
        nodeOptions.setDisableCli(false);
        // snapshot should be made every 600 seconds
        int snapshotInterval = CONFIG.getInt(SERVER_RAFT_SNAPSHOT_INTERVAL, 60 * 10);
        nodeOptions.setSnapshotIntervalSecs(snapshotInterval);
        nodeOptions.setRaftOptions(initRaftOptions());
        // set the election timeout to 1 second
        nodeOptions
            .setElectionTimeoutMs(CONFIG.getInt(SERVER_RAFT_ELECTION_TIMEOUT_MS, DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS));
        // set up the initial cluster configuration
        nodeOptions.setInitialConf(initConf);
        return nodeOptions;
    }

    public static Set<String> groups() {
        return RAFT_SERVER_MAP.keySet();
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
