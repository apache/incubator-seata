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
package org.apache.seata.namingserver;

import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.MetaResponse;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.namingserver.controller.NamingController;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import static org.apache.seata.common.NamingServerConstants.CONSTANT_GROUP;
import static org.junit.jupiter.api.Assertions.*;


@RunWith(SpringRunner.class)
@SpringBootTest
class NamingControllerTest {

    @Value("${heartbeat.threshold}")
    private int threshold;

    @Value("${heartbeat.period}")
    private int period;
    @Autowired
    NamingController namingController;


    @Test
    void mockRegister() {
        String clusterName = "cluster1";
        String namespace = "public1";
        String unitName = String.valueOf(UUID.randomUUID());
        NamingServerNode node = new NamingServerNode();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8091, "netty"));
        node.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        Map<String, Object> meatadata = node.getMetadata();
        Map<String,Object> vGroups = new HashMap<>();
        vGroups.put("vgroup1",unitName);
        meatadata.put(CONSTANT_GROUP, vGroups);
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, unitName, vGroup);
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        Node node1 = unit.getNamingInstanceList().get(0);
        assertEquals("127.0.0.1", node1.getTransaction().getHost());
        assertEquals(8091, node1.getTransaction().getPort());
        namingController.unregisterInstance(unitName, node);
    }

    @Test
    void mockUnregisterGracefully() {
        String clusterName = "cluster2";
        String namespace = "public2";
        String unitName = String.valueOf(UUID.randomUUID());
        NamingServerNode node = new NamingServerNode();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8091, "netty"));
        node.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        Map<String, Object> meatadata = node.getMetadata();
        Map<String,Object> vGroups = new HashMap<>();
        vGroups.put("vgroup1",unitName);
        meatadata.put(CONSTANT_GROUP, vGroups);
        namingController.registerInstance(namespace, clusterName, unitName, node);
        NamingServerNode node2 = new NamingServerNode();
        node2.setTransaction(new Node.Endpoint("127.0.0.1", 8091, "netty"));
        node2.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        Map<String, Object> meatadata2 = node2.getMetadata();
        Map<String,Object> vGroups2 = new HashMap<>();
        vGroups2.put("vgroup2",null);
        meatadata2.put(CONSTANT_GROUP, vGroups2);
        namingController.registerInstance(namespace, "cluster2", UUID.randomUUID().toString(), node2);
        String vGroup = "vgroup1";
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        Node node1 = unit.getNamingInstanceList().get(0);
        assertEquals("127.0.0.1", node1.getTransaction().getHost());
        assertEquals(8091, node1.getTransaction().getPort());
        namingController.unregisterInstance(unitName, node);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(0, metaResponse.getClusterList().get(0).getUnitData().size());
    }

    @Test
    void mockUnregisterUngracefully() throws InterruptedException {
        String clusterName = "cluster1";
        String namespace = "public3";
        String unitName = String.valueOf(UUID.randomUUID());
        NamingServerNode node = new NamingServerNode();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8091, "netty"));
        node.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        Map<String, Object> meatadata = node.getMetadata();
        Map<String,Object> vGroups = new HashMap<>();
        vGroups.put("vgroup1",unitName);
        meatadata.put(CONSTANT_GROUP, vGroups);
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        //namingController.changeGroup(namespace, clusterName, vGroup, vGroup);
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        Node node1 = unit.getNamingInstanceList().get(0);
        assertEquals("127.0.0.1", node1.getTransaction().getHost());
        assertEquals(8091, node1.getTransaction().getPort());
        int timeGap = threshold + period;
        Thread.sleep(timeGap);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(0, metaResponse.getClusterList().get(0).getUnitData().size());
    }

    @Test
    void mockDiscoveryMultiNode() throws InterruptedException {
        String clusterName = "cluster1";
        String namespace = "public4";
        String unitName = String.valueOf(UUID.randomUUID());
        NamingServerNode node = new NamingServerNode();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8091, "netty"));
        node.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        Map<String, Object> meatadata = node.getMetadata();
        Map<String,Object> vGroups = new HashMap<>();
        vGroups.put("vgroup1",unitName);
        meatadata.put(CONSTANT_GROUP, vGroups);
        NamingServerNode node2 = new NamingServerNode();
        String unitName2 = String.valueOf(UUID.randomUUID());
        node2.setTransaction(new Node.Endpoint("127.0.0.1", 8092, "netty"));
        node2.setControl(new Node.Endpoint("127.0.0.1", 7092, "http"));
        vGroups = new HashMap<>();
        vGroups.put("vgroup1",unitName2);
        node2.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingController.registerInstance(namespace, clusterName, unitName, node);
        namingController.registerInstance(namespace, clusterName, unitName2, node2);
        String vGroup = "vgroup1";
        //namingController.changeGroup(namespace, clusterName, vGroup, vGroup);
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(2, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        namingController.unregisterInstance(unitName, node);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
    }

}