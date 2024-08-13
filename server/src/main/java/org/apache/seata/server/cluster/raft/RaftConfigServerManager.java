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

import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.RouteTable;
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
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigType;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.store.ConfigStoreManagerFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.ServerRunner;
import org.apache.seata.server.cluster.raft.processor.ConfigOperationRequestProcessor;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.io.File.separator;
import static org.apache.seata.common.ConfigurationKeys.*;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.Constants.RAFT_CONFIG_GROUP;
import static org.apache.seata.common.DefaultValues.*;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGEX_SPLIT_CHAR;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_PREFERED_NETWORKS;

/**
 * The type to manager raft server of config center
 */
public class RaftConfigServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConfigServerManager.class);
    private static final AtomicBoolean INIT = new AtomicBoolean(false);
    private static final org.apache.seata.config.Configuration CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static RpcServer rpcServer;
    private static RaftConfigServer raftServer;
    private static volatile boolean RAFT_MODE;
    private static String group = RAFT_CONFIG_GROUP;

    public static CliService getCliServiceInstance() {
        return RaftConfigServerManager.SingletonHandler.CLI_SERVICE;
    }

    public static CliClientService getCliClientServiceInstance() {
        return RaftConfigServerManager.SingletonHandler.CLI_CLIENT_SERVICE;
    }

    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            String initConfStr = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
            String configTypeName = CONFIG.getConfig(org.apache.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG
                    + org.apache.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + org.apache.seata.config.ConfigurationKeys.FILE_ROOT_TYPE);
            RAFT_MODE = ConfigType.Raft.name().equalsIgnoreCase(configTypeName);
            if (!RAFT_MODE){
                return;
            }
            if (StringUtils.isBlank(initConfStr)) {
                if (RAFT_MODE) {
                    throw new IllegalArgumentException(
                            "Raft config mode must config: " + ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
                }
                return;
            }
            final Configuration initConf = new Configuration();
            if (!initConf.parse(initConfStr)) {
                throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
            }
            int port = Integer.parseInt(System.getProperty(SERVER_RAFT_PORT_CAMEL, "0"));
            PeerId serverId = null;
            // XID may be null when configuration center is not initialized.
            String host = null;
            if (XID.getIpAddress() == null){
                String preferredNetworks = CONFIG.getConfig(REGISTRY_PREFERED_NETWORKS);
                host = StringUtils.isNotBlank(preferredNetworks) ? NetUtil.getLocalIp(preferredNetworks.split(REGEX_SPLIT_CHAR)) : NetUtil.getLocalIp();
            }else{
                host = XID.getIpAddress();
            }
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
            final String dataPath = CONFIG.getConfig(CONFIG_STORE_DIR, DEFAULT_DB_STORE_FILE_DIR)
                    + separator + "raft" + separator + serverId.getPort();
            try {
                // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this
                // separately
                SerializerManager.addSerializer(SerializerType.JACKSON.getCode(), new JacksonBoltSerializer());
                rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
                raftServer = new RaftConfigServer(dataPath, group, serverId, initNodeOptions(initConf), rpcServer);
            } catch (IOException e) {
                throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
            }
        }
    }
    public static void start() {
        if (!RAFT_MODE){
            return;
        }
        try {
            if (raftServer != null) {
                raftServer.start();
            }
        } catch (IOException e) {
            LOGGER.error("start seata server raft cluster error, group: {} ", group, e);
            throw new RuntimeException(e);
        }
        LOGGER.info("started seata server raft cluster, group: {} ", group);

        if (rpcServer != null) {
            rpcServer.registerProcessor(new PutNodeInfoRequestProcessor());
            rpcServer.registerProcessor(new ConfigOperationRequestProcessor());
            if (!rpcServer.init(null)) {
                throw new RuntimeException("start raft node fail!");
            }
        }
        // Make sure to close it at the end, as other components may still use the configuration, such as ShutdownWaitTime.
        ServerRunner.addDisposable(() -> {
            RaftConfigServerManager.destroy();
            ConfigStoreManagerFactory.destroy();
        });
    }


    public static void destroy() {
        raftServer.close();
        LOGGER.info("closed seata server raft cluster, group: {} ", group);
        Optional.ofNullable(rpcServer).ifPresent(RpcServer::shutdown);
        raftServer = null;
        rpcServer = null;
        RAFT_MODE = false;
        INIT.set(false);
    }

    public static RaftConfigServer getRaftServer() {
        return raftServer;
    }

    public static RpcServer getRpcServer() {
        return rpcServer;
    }

    public static boolean isLeader() {
        AtomicReference<RaftConfigStateMachine> stateMachine = new AtomicReference<>();
        Optional.ofNullable(raftServer).ifPresent(raftConfigServer -> {
            stateMachine.set(raftConfigServer.getRaftStateMachine());
        });
        RaftConfigStateMachine raftStateMachine = stateMachine.get();
        return raftStateMachine != null && raftStateMachine.isLeader();
    }

    public static PeerId getLeader() {

        RouteTable routeTable = RouteTable.getInstance();
        try {
            routeTable.refreshLeader(getCliClientServiceInstance(), RAFT_CONFIG_GROUP , 1000);
            return routeTable.selectLeader(RAFT_CONFIG_GROUP);
        } catch (Exception e) {
            LOGGER.error("there is an exception to getting the leader address: {}", e.getMessage(), e);
        }
        return null;

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
    public static String getGroup() {
        return group;
    }
    private static class SingletonHandler {
        private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
        private static final CliClientService CLI_CLIENT_SERVICE = new CliClientServiceImpl();

        static {
            CLI_CLIENT_SERVICE.init(new CliOptions());
        }

    }

}
