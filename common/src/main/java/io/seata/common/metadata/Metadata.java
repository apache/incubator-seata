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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.seata.common.store.StoreMode;
import io.seata.common.util.StringUtils;


import static io.seata.common.DefaultValues.DEFAULT_RAFT_PORT_INTERVAL;
import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * @author funkye
 */
public class Metadata {

    private final Map<String, Node> leaders = new HashMap<>();
    private final Map<String, Long> leaderUpdateTimes = new HashMap<>();
    private final Map<String, List<Node>> nodes = new HashMap<>();

    private StoreMode storeMode = StoreMode.FILE;

    public Node getLeader() {
        return getLeader(DEFAULT_SEATA_GROUP);
    }

    public Node getLeader(String group) {
        return leaders.get(group);
    }

    public void setLeaderNode(Node node) {
        String group = node.getGroup();
        this.leaders.put(node.getGroup(), node);
        this.leaderUpdateTimes.put(group, System.currentTimeMillis());
    }

    public boolean isExpired() {
        return isExpired(DEFAULT_SEATA_GROUP);
    }

    public boolean isExpired(String group) {
        Long timestamp = leaderUpdateTimes.get(group);
        return timestamp == null || (System.currentTimeMillis() - timestamp) > DEFAULT_RAFT_PORT_INTERVAL;
    }

    public boolean isNotExpired() {
        return !isExpired();
    }

    public void setLeader(Node leader) {
        setLeader(DEFAULT_SEATA_GROUP, leader);
    }

    public void setLeader(String group, Node leader) {
        this.leaders.put(group, leader);
    }


    public List<Node> getNodes() {
        return getNodes(DEFAULT_SEATA_GROUP);
    }

    public List<Node> getNodes(String group) {
        return nodes.get(group);
    }

    public void setNodes(String group, List<Node> nodes) {
        this.nodes.put(group, nodes);
    }

    public void setNodes(List<Node> nodes) {
        setNodes(DEFAULT_SEATA_GROUP, nodes);
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

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
