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
package io.seata.server.controller;

import java.util.List;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.raft.RaftServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import static io.seata.common.DefaultValues.SEATA_RAFT_GROUP;

@RestController
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);


    @GetMapping("/cluster")
    public Object healthCheck() {
        String initConfStr = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SERVER_RAFT_CLUSTER);
        if (StringUtils.isBlank(initConfStr)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("initialize SofaJRaft fail , server.raft.cluster is null");
            }
            return null;
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
        }
        List<PeerId> currentPeers = RaftServerFactory.getCliServiceInstance().getAlivePeers(SEATA_RAFT_GROUP, initConf);
        return currentPeers;
    }

}
