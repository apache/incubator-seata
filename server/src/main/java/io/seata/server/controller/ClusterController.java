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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import io.seata.common.XID;
import io.seata.common.metadata.ClusterRole;
import io.seata.common.metadata.MetadataResponse;
import io.seata.common.metadata.Node;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.server.cluster.manager.ClusterWatcherManager;
import io.seata.server.cluster.raft.RaftServer;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.seata.common.ConfigurationKeys.SERVER_RAFT_CLUSTER;
import static io.seata.common.ConfigurationKeys.STORE_MODE;
import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static io.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;

/**
 * @author funkye
 */
@RestController
@RequestMapping("/metadata/v1")
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @Resource
    private ApplicationContext applicationContext;

    private ServerProperties serverProperties;

    @PostConstruct
    public void init() {
        serverProperties = applicationContext.getBean(ServerProperties.class);
    }

    @GetMapping("/cluster")
    public MetadataResponse cluster(@RequestParam(defaultValue = DEFAULT_SEATA_GROUP) String group) {
        MetadataResponse metadataResponse = new MetadataResponse();
        RaftServer raftServer = RaftServerFactory.getInstance().getRaftServer(group);
        if (raftServer != null) {
            String mode = ConfigurationFactory.getInstance().getConfig(STORE_MODE);
            metadataResponse.setMode(mode);
            String currentConf = ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_CLUSTER);
            if (!StringUtils.isBlank(currentConf)) {
                final Configuration currentClusters = new Configuration();
                if (!currentClusters.parse(currentConf)) {
                    throw new IllegalArgumentException("fail to parse initConf:" + currentConf);
                }
                RouteTable routeTable = RouteTable.getInstance();
                if (!Objects.equals(routeTable.getConfiguration(group), currentClusters)) {
                    routeTable.updateConfiguration(group, currentClusters);
                }
                try {
                    routeTable.refreshLeader(RaftServerFactory.getCliClientServiceInstance(), group, 1000);
                    PeerId leader = routeTable.selectLeader(group);
                    if (leader != null) {
                        Set<Node> nodes = new HashSet<>();
                        Node leaderNode = new Node();
                        leaderNode.setRole(ClusterRole.LEADER);
                        leaderNode.setGroup(group);
                        leaderNode.setHttpPort(leader.getIdx() - SERVICE_OFFSET_SPRING_BOOT);
                        leaderNode.setNettyPort(leader.getIdx());
                        leaderNode.setHostAddress(leader.getIp());
                        nodes.add(leaderNode);
                        Configuration configuration = routeTable.getConfiguration(group);
                        nodes.addAll(configuration.getLearners().parallelStream().map(learner -> {
                            Node node = new Node();
                            node.setGroup(group);
                            node.setRole(ClusterRole.LEARNER);
                            node.setHttpPort(serverProperties.getPort());
                            node.setNettyPort(XID.getPort());
                            node.setHostAddress(learner.getIp());
                            return node;
                        }).collect(Collectors.toList()));
                        nodes.addAll(configuration.getPeers().parallelStream().map(follower -> {
                            Node node = new Node();
                            node.setGroup(group);
                            node.setRole(ClusterRole.FOLLOWER);
                            node.setHttpPort(follower.getIdx() - SERVICE_OFFSET_SPRING_BOOT);
                            node.setNettyPort(follower.getIdx());
                            node.setHostAddress(follower.getIp());
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

    @GetMapping("/watch")
    public void watch(HttpServletRequest request, @RequestParam(defaultValue = DEFAULT_SEATA_GROUP) String groupIds,
        @RequestParam(defaultValue = "29000") int timeout, Long lastUpdateTime) {
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        for (String group : groupIds.split(",")) {
            Watcher<AsyncContext> watcher = new Watcher<>(group, context, timeout, lastUpdateTime);
            boolean success = clusterWatcherManager.registryWatcher(watcher);
            if (!success) {
                HttpServletResponse httpServletResponse = (HttpServletResponse)watcher.getAsyncContext().getResponse();
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                context.complete();
            }
        }
    }

}
