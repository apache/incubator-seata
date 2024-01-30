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
package org.apache.seata.common.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.common.util.StringUtils;


public class Metadata {

    private final Map<String/*vgroup*/, Map<String/*raft-group*/, Node>> leaders = new ConcurrentHashMap<>();

    private final Map<String/*vgroup*/, Map<String/*raft-group*/, Long/*term*/>> clusterTerm = new ConcurrentHashMap<>();

    private final Map<String/*vgroup*/, Map<String/*raft-group*/, List<Node>>> clusterNodes =
        new ConcurrentHashMap<>();

    private StoreMode storeMode = StoreMode.FILE;

    public Node getLeader(String clusterName) {
        Map<String/*raft-group*/, Node> map = leaders.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>());
        List<Node> nodes = new ArrayList<>(map.values());
        return nodes.size() > 0 ? nodes.get(ThreadLocalRandom.current().nextInt(nodes.size())) : null;
    }

    public void setLeaderNode(String clusterName, Node node) {
        String group = node.getGroup();
        Map<String/*raft-group*/, Node> map = leaders.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>());
        map.put(group, node);
        this.leaders.put(clusterName, map);
    }

    public List<Node> getNodes(String clusterName, String group) {
        return clusterNodes.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>()).get(group);
    }

    public List<Node> getNodes(String clusterName) {
        return clusterNodes.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>()).values().stream()
            .flatMap(List::stream).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public void setNodes(String clusterName, String group, List<Node> nodes) {
        this.clusterNodes.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>()).put(group, nodes);
    }

    public boolean containsGroup(String group) {
        return clusterNodes.containsKey(group);
    }

    public Set<String> groups(String clusterName) {
        return clusterNodes.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>()).keySet();
    }

    public StoreMode getStoreMode() {
        return storeMode;
    }

    public boolean isRaftMode() {
        return Objects.equals(storeMode, StoreMode.RAFT);
    }

    public void setStoreMode(StoreMode storeMode) {
        this.storeMode = storeMode;
    }

    public Map<String, Long> getClusterTerm(String clusterName) {
        return clusterTerm.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>());
    }

    public void refreshMetadata(String clusterName, MetadataResponse metadataResponse) {
        List<Node> list = new ArrayList<>();
        for (Node node : metadataResponse.getNodes()) {
            if (node.getRole() == ClusterRole.LEADER) {
                this.setLeaderNode(clusterName, node);
            }
            list.add(node);
        }
        this.storeMode = StoreMode.get(metadataResponse.getStoreMode());
        if (!list.isEmpty()) {
            String group = list.get(0).getGroup();
            this.setNodes(clusterName, group, list);
            this.clusterTerm.computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>()).put(group,
                metadataResponse.getTerm());
        }
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
