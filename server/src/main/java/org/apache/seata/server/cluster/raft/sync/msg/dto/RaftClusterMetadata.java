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
package org.apache.seata.server.cluster.raft.sync.msg.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.Version;

/**
 */
public class RaftClusterMetadata implements Serializable {

    private static final long serialVersionUID = 6208583637662412658L;

    private Node leader;

    private List<Node> followers = new ArrayList<>();

    private List<Node> learner = new ArrayList<>();

    private long term;

    public RaftClusterMetadata() {
    }

    public RaftClusterMetadata(long term) {
        this.term = term;
    }

    public Node createNode(String host, int txPort, int internalPort, int controlPort, String group,
        Map<String, Object> metadata) {
        Node node = new Node();
        node.setTransaction(node.createEndpoint(host, txPort, "seata"));
        node.setControl(node.createEndpoint(host, controlPort, "http"));
        node.setGroup(group);
        node.setVersion(Version.getCurrent());
        node.setInternal(node.createEndpoint(host, internalPort, "raft"));
        Optional.ofNullable(metadata).ifPresent(node::setMetadata);
        return node;
    }

    public Node getLeader() {
        return leader;
    }

    public void setLeader(Node leader) {
        this.leader = leader;
    }

    public long getTerm() {
        return term;
    }

    public List<Node> getFollowers() {
        return followers;
    }

    public void setFollowers(List<Node> followers) {
        this.followers = followers;
    }

    public List<Node> getLearner() {
        return learner;
    }

    public void setLearner(List<Node> learner) {
        this.learner = learner;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
