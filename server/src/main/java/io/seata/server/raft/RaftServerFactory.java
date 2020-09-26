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

import java.io.IOException;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.common.DefaultValues.SEATA_RAFT_GROUP;

/**
 * @author funkye
 */
public class RaftServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftServerFactory.class);

    private RaftServer raftServer;

    private RaftStateMachine stateMachine;

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

        raftServer = new RaftServer(dataPath, SEATA_RAFT_GROUP, serverId, nodeOptions);
        stateMachine = raftServer.getFsm();
        LOGGER.info("Started counter server at port:{}", raftServer.getNode().getNodeId().getPeerId().getPort());
    }

    public RaftServer getRaftServer() {
        return raftServer;
    }

    public Boolean isLeader() {
        if (stateMachine != null) {
            return stateMachine.isLeader();
        } else if (raftServer == null) {
            return true;
        }
        return false;
    }

    private static class SingletonHandler {
        private static RaftServerFactory instance = new RaftServerFactory();
    }
}
