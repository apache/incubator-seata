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
package io.seata.common.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.store.StoreMode;
import io.seata.common.util.StringUtils;

/**
 * @author funkye
 */
public class Metadata {

    private final Map<String, Node> leaders = new ConcurrentHashMap<>();

    private final Map<String, Long> clusterTerm = new ConcurrentHashMap<>();

    private final Map<String, List<Node>> nodes = new ConcurrentHashMap<>();

    private StoreMode storeMode = StoreMode.FILE;

    public Node getLeader(String group) {
        return leaders.get(group);
    }

    public void setLeaderNode(Node node) {
        String group = node.getGroup();
        this.leaders.put(node.getGroup(), node);
    }

    public void setLeader(Node leader) {
        setLeader(leader.getGroup(), leader);
    }

    public void setLeader(String group, Node leader) {
        this.leaders.put(group, leader);
    }

    public List<Node> getNodes(String group) {
        return nodes.get(group);
    }

    public void setNodes(String group, List<Node> nodes) {
        this.nodes.put(group, nodes);
    }

    public boolean containsGroup(String group) {
        return nodes.containsKey(group);
    }

    public Set<String> groups() {
        return nodes.keySet();
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

    public Map<String, Long> getClusterTerm() {
        return clusterTerm;
    }

    public void refreshMetadata(String group, MetadataResponse metadataResponse) {
        List<Node> list = new ArrayList<>();
        for (Node node : metadataResponse.getNodes()) {
            if (node.getRole() == ClusterRole.LEADER) {
                this.setLeader(node);
            }
            list.add(node);
        }
        this.storeMode = StoreMode.get(metadataResponse.getStoreMode());
        this.nodes.put(group, list);
        this.clusterTerm.put(group, metadataResponse.getTerm());
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
