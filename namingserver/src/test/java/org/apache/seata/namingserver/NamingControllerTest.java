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
import org.apache.seata.common.metadata.MetaResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.Unit;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

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
        String namespace = "public";
        String unitName = String.valueOf(UUID.randomUUID());
        Node node = new Node();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8080, "netty"));
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, null, vGroup);
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
        assertEquals(8080, node1.getTransaction().getPort());
        namingController.unregisterInstance(unitName, node);
    }

    @Test
    void mockUnregisterGracefully() {
        String clusterName = "cluster1";
        String namespace = "public";
        String unitName = String.valueOf(UUID.randomUUID());
        Node node = new Node();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8080, "netty"));
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, null, vGroup);
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
        assertEquals(8080, node1.getTransaction().getPort());
        namingController.unregisterInstance(unitName, node);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        cluster = metaResponse.getClusterList().get(0);
        assertEquals(0, cluster.getUnitData().size());
    }

    @Test
    void mockUnregisterUngracefully() throws InterruptedException {
        String clusterName = "cluster1";
        String namespace = "public";
        String unitName = String.valueOf(UUID.randomUUID());
        Node node = new Node();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8080, "netty"));
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, null, vGroup);
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
        assertEquals(8080, node1.getTransaction().getPort());
        int timeGap = threshold + period;
        Thread.sleep(timeGap);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        cluster = metaResponse.getClusterList().get(0);
        assertEquals(0, cluster.getUnitData().size());
    }
}