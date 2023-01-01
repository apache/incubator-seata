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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import io.seata.common.metadata.ClusterRole;
import io.seata.common.metadata.MetadataResponse;
import io.seata.common.metadata.Node;
import io.seata.common.store.StoreMode;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.SingleResult;
import io.seata.server.raft.RaftServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.seata.common.ConfigurationKeys.SERVER_RAFT_CLUSTER;
import static io.seata.common.ConfigurationKeys.STORE_MODE;
import static io.seata.common.DefaultValues.DEFAULT_RAFT_PORT_INTERVAL;
import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

@RestController
@RequestMapping("/metadata/v1")
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);


    @GetMapping("/cluster")
    public MetadataResponse cluster(String group) {
        String mode = ConfigurationFactory.getInstance().getConfig(STORE_MODE);
        MetadataResponse metadataResponse=new MetadataResponse();
        metadataResponse.setMode(mode);
        if (StringUtils.equalsIgnoreCase(StoreMode.RAFT.getName(), mode)) {
            String currentConf = ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_CLUSTER);
            if (!StringUtils.isBlank(currentConf)) {
                String raftGroup = StringUtils.isNotBlank(group) ? group : DEFAULT_SEATA_GROUP;
                final Configuration currentClusters = new Configuration();
                if (!currentClusters.parse(currentConf)) {
                    throw new IllegalArgumentException("fail to parse initConf:" + currentConf);
                }
                RouteTable routeTable = RouteTable.getInstance();
                if (!Objects.equals(routeTable.getConfiguration(raftGroup), currentClusters)) {
                    routeTable.updateConfiguration(raftGroup, currentClusters);
                }
                try {
                    routeTable.refreshLeader(RaftServerFactory.getCliClientServiceInstance(), raftGroup, 1000);
                    PeerId leader = routeTable.selectLeader(raftGroup);
                    if (leader != null) {
                        Set<Node> nodes =new HashSet<>();
                        Node leaderNode = new Node();
                        leaderNode.setRole(ClusterRole.LEADER);
                        leaderNode.setGroup(raftGroup);
                        leaderNode.setAddress(leader.getIp() + ":" + (leader.getPort() - DEFAULT_RAFT_PORT_INTERVAL));
                        nodes.add(leaderNode);
                        Configuration configuration = routeTable.getConfiguration(raftGroup);
                        nodes.addAll(configuration.getLearners().parallelStream().map(learner -> {
                            Node node = new Node();
                            node.setGroup(raftGroup);
                            node.setRole(ClusterRole.LEARNER);
                            node.setAddress(learner.getIp() + ":" + (learner.getPort() - DEFAULT_RAFT_PORT_INTERVAL));
                            return node;
                        }).collect(Collectors.toList()));
                        nodes.addAll(configuration.getPeers().parallelStream().map(follower -> {
                            Node node = new Node();
                            node.setGroup(raftGroup);
                            node.setRole(ClusterRole.FOLLOWER);
                            node.setAddress(
                                    follower.getIp() + ":" + (follower.getPort() - DEFAULT_RAFT_PORT_INTERVAL));
                            return node;
                        }).collect(Collectors.toList()));
                        metadataResponse.setNodes(new ArrayList<>(nodes));
                    }
                } catch (Exception e) {
                    LOGGER.error("there is an exception to getting the leader address: {}", e.getMessage(), e);
                }
            }
        }
        return metadataResponse;
    }


}
