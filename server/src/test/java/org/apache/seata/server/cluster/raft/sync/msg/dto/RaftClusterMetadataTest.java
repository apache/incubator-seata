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

import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RaftClusterMetadataTest {

    @Test
    public void testDefaultConstructor() {
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        assertNotNull(metadata);
        assertNull(metadata.getLeader());
        assertNotNull(metadata.getFollowers());
        assertNotNull(metadata.getLearner());
        assertEquals(0, metadata.getTerm());
    }

    @Test
    public void testConstructorWithTerm() {
        long term = 12345L;
        RaftClusterMetadata metadata = new RaftClusterMetadata(term);
        assertEquals(term, metadata.getTerm());
    }

    @Test
    public void testSetAndGetLeader() {
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        Node leader = new Node();
        metadata.setLeader(leader);
        assertEquals(leader, metadata.getLeader());
    }

    @Test
    public void testSetAndGetFollowers() {
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        Node follower1 = new Node();
        Node follower2 = new Node();
        metadata.setFollowers(Arrays.asList(follower1, follower2));
        assertEquals(2, metadata.getFollowers().size());
    }

    @Test
    public void testSetAndGetLearner() {
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        Node learner1 = new Node();
        metadata.setLearner(Arrays.asList(learner1));
        assertEquals(1, metadata.getLearner().size());
    }

    @Test
    public void testSetAndGetTerm() {
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        metadata.setTerm(999L);
        assertEquals(999L, metadata.getTerm());
    }

    @Test
    public void testToString() {
        RaftClusterMetadata metadata = new RaftClusterMetadata(123L);
        String str = metadata.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void testNodeListManagement() {
        RaftClusterMetadata metadata = new RaftClusterMetadata(100L);

        Node leader = new Node();
        metadata.setLeader(leader);
        assertNotNull(metadata.getLeader());

        Node follower1 = new Node();
        Node follower2 = new Node();
        metadata.setFollowers(Arrays.asList(follower1, follower2));
        assertEquals(2, metadata.getFollowers().size());

        Node learner = new Node();
        metadata.setLearner(Arrays.asList(learner));
        assertEquals(1, metadata.getLearner().size());

        assertEquals(100L, metadata.getTerm());
    }
}
