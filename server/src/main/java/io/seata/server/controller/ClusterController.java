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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.common.metadata.ClusterRole;
import io.seata.common.metadata.MetadataResponse;
import io.seata.common.metadata.Node;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.Result;
import io.seata.server.cluster.manager.ClusterWatcherManager;
import io.seata.server.cluster.raft.RaftServer;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.seata.common.ConfigurationKeys.STORE_MODE;
import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * @author funkye
 */
@RestController
@RequestMapping("/metadata/v1")
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    private ServerProperties serverProperties;

    @Resource
    ApplicationContext applicationContext;

    @PostConstruct
    private void init(){
        this.serverProperties = applicationContext.getBean(ServerProperties.class);
    }

    @PostMapping("/changeCluster")
    public Result<?> changeCluster(@RequestParam String raftClusterStr) {
        Result<?> result = new Result<>();
        final Configuration newConf = new Configuration();
        if (!newConf.parse(raftClusterStr)) {
            result.setMessage("fail to parse initConf:" + raftClusterStr);
        } else {
            RaftServerFactory.groups().forEach(group -> {
                RaftServerFactory.getCliServiceInstance().changePeers(group,
                    RouteTable.getInstance().getConfiguration(group), newConf);
                RouteTable.getInstance().updateConfiguration(group, newConf);
            });
        }
        return result;
    }

    @GetMapping("/cluster")
    public MetadataResponse cluster(String group) {
        MetadataResponse metadataResponse = new MetadataResponse();
        if (StringUtils.isBlank(group)) {
            group =
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
        }
        RaftServer raftServer = RaftServerFactory.getInstance().getRaftServer(group);
        if (raftServer != null) {
            String mode = ConfigurationFactory.getInstance().getConfig(STORE_MODE);
            metadataResponse.setStoreMode(mode);
            RouteTable routeTable = RouteTable.getInstance();
            try {
                routeTable.refreshLeader(RaftServerFactory.getCliClientServiceInstance(), group, 1000);
                PeerId leader = routeTable.selectLeader(group);
                if (leader != null) {
                    Set<Node> nodes = new HashSet<>();
                    Node leaderNode = new Node(leader.getIdx(), leader.getPort());
                    leaderNode.setRole(ClusterRole.LEADER);
                    leaderNode.setGroup(group);
                    leaderNode.setHost(leader.getIp());
                    nodes.add(leaderNode);
                    Configuration configuration = routeTable.getConfiguration(group);
                    int nettyPort = XID.getPort();
                    int httpPort = serverProperties.getPort();
                    String finalGroup = group;
                    nodes.addAll(configuration.getLearners().stream().map(learner -> {
                        Node node = new Node(learner.getIdx(), learner.getPort());
                        node.setGroup(finalGroup);
                        node.setRole(ClusterRole.LEARNER);
                        node.setHost(learner.getIp());
                        node.setNettyPort(nettyPort);
                        node.setHttpPort(httpPort);
                        return node;
                    }).collect(Collectors.toList()));
                    nodes.addAll(configuration.getPeers().stream().map(follower -> {
                        Node node = new Node(follower.getIdx(), follower.getPort());
                        node.setGroup(finalGroup);
                        node.setRole(ClusterRole.FOLLOWER);
                        node.setHost(follower.getIp());
                        node.setNettyPort(nettyPort);
                        node.setHttpPort(httpPort);
                        return node;
                    }).collect(Collectors.toList()));
                    metadataResponse.setTerm(raftServer.getRaftStateMachine().getCurrentTerm().get());
                    metadataResponse.setNodes(new ArrayList<>(nodes));
                }
            } catch (Exception e) {
                LOGGER.error("there is an exception to getting the leader address: {}", e.getMessage(), e);
            }
        }
        return metadataResponse;
    }

    @PostMapping("/watch")
    public void watch(HttpServletRequest request, @RequestParam Map<String, Object> groupTerms,
        @RequestParam(defaultValue = "28000") int timeout) {
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        groupTerms.forEach((group, term) -> {
            Watcher<AsyncContext> watcher =
                new Watcher<>(group, context, timeout, Long.parseLong(String.valueOf(term)));
            clusterWatcherManager.registryWatcher(watcher);
        });
    }

}
