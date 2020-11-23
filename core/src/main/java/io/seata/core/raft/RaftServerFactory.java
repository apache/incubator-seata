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
package io.seata.core.raft;

import java.io.IOException;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.StoreMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.common.DefaultValues.SEATA_RAFT_GROUP;
import static io.seata.core.raft.AbstractRaftServer.RAFT_TAG;

/**
 * @author funkye
 */
public class RaftServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftServerFactory.class);

    private AbstractRaftServer raftServer;

    private AbstractRaftStateMachine stateMachine;

    private Boolean raftMode = false;

    public static RaftServerFactory getInstance() {
        return SingletonHandler.instance;
    }

    public void init(String host, int port, String... defaultConf) throws IOException {
        String initConfStr = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SERVER_RAFT_CLUSTER);
        if (StringUtils.isBlank(initConfStr)) {
            if (defaultConf == null || defaultConf.length == 0) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("initialize SofaJraft fail cluster is null");
                }
                return;
            } else {
                initConfStr = defaultConf[0];
            }
        }
        String mode = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_MODE);
        StoreMode storeMode = StoreMode.get(mode);
        if (storeMode.equals(StoreMode.RAFT)) {
            raftMode = true;
        }
        String colon = ":";
        int constantInt = 100 * 10;
        String serverIdStr = host + colon + (port - constantInt);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("initialize SofaJraft");
        }
        final String dataPath =
            ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_FILE_DIR, "sessionStore") + "/"
                + serverIdStr.split(colon)[1];
        final NodeOptions nodeOptions = new NodeOptions();
        // Set the election timeout to 1 second
        nodeOptions.setElectionTimeoutMs(constantInt);
        // Close the CLI service.
        nodeOptions.setDisableCli(false);
        // Snapshot should be made every 30 seconds
        nodeOptions.setSnapshotIntervalSecs(30);
        // analytic parameter
        final PeerId serverId = new PeerId();
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("Fail to parse serverId:" + serverIdStr);
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("Fail to parse initConf:" + initConfStr);
        }
        // Set up the initial cluster configuration
        nodeOptions.setInitialConf(initConf);
        raftServer = EnhancedServiceLoader.load(AbstractRaftServer.class, RAFT_TAG,
            new Object[] {dataPath, SEATA_RAFT_GROUP, serverId, nodeOptions});
        stateMachine = raftServer.raftStateMachine;
        LOGGER.info("Started counter server at port:{}", raftServer.node.getNodeId().getPeerId().getPort());
    }

    public AbstractRaftServer getRaftServer() {
        return raftServer;
    }

    public void setRaftServer(AbstractRaftServer raftServer) {
        this.raftServer = raftServer;
    }

    public AbstractRaftStateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(AbstractRaftStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public Boolean isLeader() {
        if (stateMachine != null) {
            return stateMachine.isLeader();
        } else if (raftServer == null) {
            return true;
        }
        return false;
    }

    public Boolean isRaftMode() {
        return raftMode;
    }

    private static class SingletonHandler {
        private static RaftServerFactory instance = new RaftServerFactory();
    }
}
