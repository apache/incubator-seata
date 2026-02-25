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
package org.apache.seata.server.controller;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.server.cluster.manager.ClusterWatcherManager;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.watch.Watcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/metadata/v1")
public class ClusterController {

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @PostMapping("/changeCluster")
    public Result<?> changeCluster(@RequestParam String raftClusterStr) {
        Result<?> result = new Result<>();
        final Configuration newConf = new Configuration();
        if (!newConf.parse(raftClusterStr)) {
            result.setMessage("fail to parse initConf:" + raftClusterStr);
        } else {
            RaftServerManager.groups().forEach(group -> {
                RaftServerManager.getCliServiceInstance()
                        .changePeers(group, RouteTable.getInstance().getConfiguration(group), newConf);
                RouteTable.getInstance().updateConfiguration(group, newConf);
            });
        }
        return result;
    }

    @GetMapping("/cluster")
    public MetadataResponse cluster(String group) {
        return clusterWatcherManager.getMetadataResponse(group);
    }

    @PostMapping("/watch")
    public void watch(
            HttpContext context,
            @RequestBody Map<String, Object> groupTerms,
            @RequestParam(defaultValue = "28000") Integer timeout) {
        context.setAsync(true);
        groupTerms.forEach((group, term) -> {
            Watcher<HttpContext> watcher = new Watcher<>(group, context, timeout, Long.parseLong(String.valueOf(term)));
            clusterWatcherManager.registryWatcher(watcher);
        });
    }
}
