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
import org.apache.seata.server.cluster.manager.ClusterWatcherManager;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    public ResponseEntity<Void> watch(
            @RequestParam Map<String, String> groupTerms, @RequestParam(defaultValue = "28000") Integer timeout)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline) {
            for (Map.Entry<String, String> entry : groupTerms.entrySet()) {
                if ("timeout".equals(entry.getKey())) {
                    continue;
                }
                long clientTerm = Long.parseLong(entry.getValue());
                MetadataResponse metadataResponse = clusterWatcherManager.getMetadataResponse(entry.getKey());
                if (metadataResponse.getTerm() > clientTerm) {
                    return ResponseEntity.ok().build();
                }
            }
            TimeUnit.MILLISECONDS.sleep(200L);
        }
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
}
