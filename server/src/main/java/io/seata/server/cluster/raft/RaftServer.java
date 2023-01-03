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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.codahale.metrics.Slf4jReporter;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.rpc.Disposable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_CLUSTER;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_REPORTER_ENABLED;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_REPORTER_INITIAL_DELAY;

/**
 * @author funkye
 */
public class RaftServer implements ConfigurationChangeListener, Disposable, Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RaftStateMachine raftStateMachine;
    private final RaftGroupService raftGroupService;
    private final Node node;
    private final CliService cliService;

    public RaftServer(final String dataPath, final String groupId, final PeerId serverId, final NodeOptions nodeOptions)
        throws IOException {
        String groupPath = dataPath + File.separator + groupId;
        // Initialization path
        FileUtils.forceMkdir(new File(groupPath));

        // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this separately
        final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        // Initialize the state machine
        this.raftStateMachine = new RaftStateMachine(groupId);
        // Set the state machine to startup parameters
        nodeOptions.setFsm(this.raftStateMachine);
        // Set the storage path
        // Log, must
        nodeOptions.setLogUri(groupPath + File.separator + "log");
        // Meta information, must
        nodeOptions.setRaftMetaUri(groupPath + File.separator + "raft_meta");
        // Snapshot, optional, is generally recommended
        nodeOptions.setSnapshotUri(groupPath + File.separator + "snapshot");
        boolean reporterEnabled = ConfigurationFactory.getInstance().getBoolean(SERVER_RAFT_REPORTER_ENABLED, false);
        nodeOptions.setEnableMetrics(reporterEnabled);
        // Initialize the raft Group service framework
        this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);
        this.cliService = RaftServerFactory.getCliServiceInstance();
        ConfigurationCache.addConfigListener(SERVER_RAFT_CLUSTER, this);
        this.node = this.raftGroupService.start();
        if (reporterEnabled) {
            final Slf4jReporter reporter = Slf4jReporter.forRegistry(node.getNodeMetrics().getMetricRegistry())
                .outputTo(logger).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
            reporter.start(ConfigurationFactory.getInstance().getInt(SERVER_RAFT_REPORTER_INITIAL_DELAY, 30),
                TimeUnit.MINUTES);
        }
    }

    public Node getNode() {
        return this.node;
    }

    public CliService getCliService() {
        return cliService;
    }

    public RaftStateMachine getRaftStateMachine() {
        return raftStateMachine;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (SERVER_RAFT_CLUSTER.equals(event.getDataId())) {
            final Configuration newConf = new Configuration();
            if (newConf.parse(event.getNewValue())) {
                Node node = getNode();
                if (node != null && node.isLeader()) {
                    CliService cliService = getCliService();
                    cliService.changePeers(DEFAULT_SEATA_GROUP, getNode().getOptions().getInitialConf(), newConf);
                }
            }
        }
    }

    @Override
    public void close() {
        destroy();
    }

    @Override
    public void destroy() {
        if (raftGroupService != null) {
            raftGroupService.shutdown();
        }
        if (cliService != null) {
            cliService.shutdown();
        }
    }

}
