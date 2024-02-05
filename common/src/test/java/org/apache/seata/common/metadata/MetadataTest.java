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

import org.apache.seata.common.store.StoreMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


public class MetadataTest {

    private static Metadata metadata;

    @BeforeAll
    public static void init() {
        metadata = new Metadata();
    }

    @Test
    public void testGetLeader() {
        Assertions.assertNull(metadata.getLeader("leader"));

        Node node = new Node();
        node.setGroup("group");
        metadata.setLeaderNode("leader", node);
        Assertions.assertNotNull(metadata.getLeader("leader"));
    }

    @Test
    public void testGetNodes() {
        Assertions.assertEquals(new ArrayList<>(), metadata.getNodes("cluster"));
        Assertions.assertNull(metadata.getNodes("cluster", "group"));
    }

    @Test
    public void testSetNodes() {
        Assertions.assertDoesNotThrow(() -> metadata.setNodes("cluster", "group", new ArrayList<>()));
    }

    @Test
    public void testContainsGroup() {
        Assertions.assertFalse(metadata.containsGroup("group"));
    }

    @Test
    public void testGroups() {
        Assertions.assertDoesNotThrow(() -> metadata.groups("cluster"));
    }

    @Test
    public void testGetStoreMode() {
        metadata.setStoreMode(StoreMode.RAFT);
        Assertions.assertEquals(StoreMode.RAFT, metadata.getStoreMode());
    }

    @Test void testIsRaftMode() {
        Assertions.assertTrue(metadata.isRaftMode());
    }

    @Test
    public void testGetClusterTerm() {
        Assertions.assertDoesNotThrow(() -> metadata.getClusterTerm("cluster"));
    }

    @Test
    public void testRefreshMetadata() {
        Node node = new Node();
        node.setGroup("group");
        node.setRole(ClusterRole.LEADER);
        Node node1 = new Node();
        node1.setGroup("group");
        node1.setRole(ClusterRole.FOLLOWER);

        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        nodes.add(node1);

        MetadataResponse metadataResponse = new MetadataResponse();
        metadataResponse.setNodes(nodes);
        metadataResponse.setStoreMode(StoreMode.RAFT.getName());
        Assertions.assertDoesNotThrow(() -> metadata.refreshMetadata("cluster", metadataResponse));
        metadataResponse.setNodes(new ArrayList<>());
        Assertions.assertDoesNotThrow(() -> metadata.refreshMetadata("cluster", metadataResponse));
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("Metadata(leaders={}, clusterTerm={}, clusterNodes={\"cluster\"->{}}, storeMode=StoreMode.RAFT)", metadata.toString());
    }

}
