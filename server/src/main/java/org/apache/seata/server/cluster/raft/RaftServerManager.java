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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.alipay.remoting.serialization.SerializerManager;
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
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.discovery.registry.FileRegistryServiceImpl;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.apache.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_PORT_CAMEL;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SYNC;
import static org.apache.seata.common.DefaultValues.DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_APPLY_BATCH;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_DISRUPTOR_BUFFER_SIZE;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_APPEND_BUFFER_SIZE;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SNAPSHOT_INTERVAL;
import static java.io.File.separator;

/**
 */
public class RaftServerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftServerManager.class);

    private static final Map<String/*group*/, RaftServer/*raft-group-cluster*/> RAFT_SERVER_MAP = new HashMap<>();
    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    private static final org.apache.seata.config.Configuration CONFIG = ConfigurationFactory.getInstance();
    private static volatile boolean RAFT_MODE;
    private static RpcServer rpcServer;
    
    public static CliService getCliServiceInstance() {
        return SingletonHandler.CLI_SERVICE;
    }

    public static CliClientService getCliClientServiceInstance() {
        return SingletonHandler.CLI_CLIENT_SERVICE;
    }

    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            String initConfStr = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
            RAFT_MODE = StoreConfig.getSessionMode().equals(StoreConfig.SessionMode.RAFT);
            if (StringUtils.isBlank(initConfStr)) {
                if (RAFT_MODE) {
                    throw new IllegalArgumentException(
                        "Raft store mode must config: " + ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
                }
                return;
            } else {
                if (RAFT_MODE) {
                    for (RegistryService<?> instance : MultiRegistryFactory.getInstances()) {
                        if (!(instance instanceof FileRegistryServiceImpl)) {
                            throw new IllegalArgumentException("Raft store mode not support other Registration Center");
                        }
                    }
                }
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
                // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this
                // separately
                rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
                RaftServer raftServer = new RaftServer(dataPath, group, serverId, initNodeOptions(initConf), rpcServer);
                // as the foundation for multi raft group in the future
                RAFT_SERVER_MAP.put(group, raftServer);
            } catch (IOException e) {
                throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
            }
        }
    }

    public static void start() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            try {
                raftServer.start();
            } catch (IOException e) {
                LOGGER.error("start seata server raft cluster error, group: {} ", group, e);
                throw new RuntimeException(e);
            }
            LOGGER.info("started seata server raft cluster, group: {} ", group);
        });
        if (rpcServer != null) {
            rpcServer.registerProcessor(new PutNodeInfoRequestProcessor());
            SerializerManager.addSerializer(SerializerType.JACKSON.getCode(), new JacksonBoltSerializer());
            if (!rpcServer.init(null)) {
                throw new RuntimeException("start raft node fail!");
            }
        }
    }

    public static void destroy() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            raftServer.close();
            LOGGER.info("closed seata server raft cluster, group: {} ", group);
        });
        Optional.ofNullable(rpcServer).ifPresent(RpcServer::shutdown);
        RAFT_SERVER_MAP.clear();
        rpcServer = null;
        RAFT_MODE = false;
        INIT.set(false);
    }

    public static RaftServer getRaftServer(String group) {
        return RAFT_SERVER_MAP.get(group);
    }

    public static Collection<RaftServer> getRaftServers() {
        return RAFT_SERVER_MAP.values();
    }

    public static boolean isLeader(String group) {
        AtomicReference<RaftStateMachine> stateMachine = new AtomicReference<>();
        Optional.ofNullable(RAFT_SERVER_MAP.get(group)).ifPresent(raftServer -> {
            stateMachine.set(raftServer.getRaftStateMachine());
        });
        RaftStateMachine raftStateMachine = stateMachine.get();
        return !isRaftMode() && RAFT_SERVER_MAP.isEmpty() || (raftStateMachine != null && raftStateMachine.isLeader());
    }

    public static boolean isRaftMode() {
        return RAFT_MODE;
    }

    private static RaftOptions initRaftOptions() {
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

    private static NodeOptions initNodeOptions(Configuration initConf) {
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
        private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
        private static final CliClientService CLI_CLIENT_SERVICE = new CliClientServiceImpl();

        static {
            CLI_CLIENT_SERVICE.init(new CliOptions());
        }

    }

}
