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

import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.namingserver.entity.bo.ClusterBO;
import org.apache.seata.namingserver.entity.pojo.ClusterData;
import org.apache.seata.namingserver.entity.vo.NamespaceVO;
import org.apache.seata.namingserver.entity.vo.monitor.ClusterVO;
import org.apache.seata.namingserver.entity.vo.monitor.WatcherVO;
import org.apache.seata.namingserver.listener.Watcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@SpringBootTest
class NamingEntityTest {

    @Test
    void testClusterBO() {
        HashSet<String> unitNames1 = new HashSet<>();
        unitNames1.add("testClusterBO1");
        unitNames1.add("testClusterBO2");
        ClusterBO clusterBO = new ClusterBO(unitNames1);
        assertNotNull(clusterBO);
        Set<String> actualUnitNames = clusterBO.getUnitNames();
        assertEquals(unitNames1.size(), actualUnitNames.size());
        unitNames1.forEach(unit -> assertTrue(actualUnitNames.contains(unit)));
        actualUnitNames.forEach(unit -> assertTrue(unitNames1.contains(unit)));

        HashSet<String> unitNames2 = new HashSet<>();
        unitNames2.add("testClusterBO3");
        clusterBO.setUnitNames(unitNames2);
        Set<String> afterSetter = clusterBO.getUnitNames();
        assertEquals(unitNames2.size(), afterSetter.size());
        unitNames2.forEach(unit -> assertTrue(afterSetter.contains(unit)));
        afterSetter.forEach(unit -> assertTrue(unitNames2.contains(unit)));
    }

    @Test
    void testNamespaceVO() {
        NamespaceVO namespaceVO = new NamespaceVO();
        assertNotNull(namespaceVO);
        assertNotNull(namespaceVO.getClusters());
        assertNotNull(namespaceVO.getVgroups());

        ArrayList<String> vGroups = new ArrayList<>();
        ArrayList<String> clusters = new ArrayList<>();
        vGroups.add("testVGroup1");
        vGroups.add("testVGroup2");
        clusters.add("testCluster1");
        clusters.add("testCluster2");

        namespaceVO.setVgroups(vGroups);
        namespaceVO.setClusters(clusters);

        assertEquals(namespaceVO.getVgroups().size(), vGroups.size());
        assertEquals(namespaceVO.getClusters().size(), clusters.size());
        vGroups.forEach(unit -> assertTrue(namespaceVO.getVgroups().contains(unit)));
        clusters.forEach(unit -> assertTrue(namespaceVO.getClusters().contains(unit)));
    }

    @Test
    void testDefaultConstructorClusterVO() {
        ClusterVO clusterVO = new ClusterVO();
        assertNotNull(clusterVO);
        assertNotNull(clusterVO.getUnitData());
        assertNotNull(clusterVO.getvGroupMapping());
        assertEquals(0, clusterVO.getUnitData().size());
        assertEquals(0, clusterVO.getvGroupMapping().size());
    }

    @Test
    void testParamConstructorClusterVO() {
        String clusterName = "testCluster";
        String clusterType = "testClusterType";
        ArrayList<Unit> unitData = new ArrayList<>();
        ClusterVO clusterVO = new ClusterVO(clusterName, clusterType, unitData);

        assertNotNull(clusterVO);
        assertNotNull(clusterVO.getUnitData());
        assertNotNull(clusterVO.getvGroupMapping());
        assertEquals(0, clusterVO.getUnitData().size());
        assertEquals(0, clusterVO.getvGroupMapping().size());
        assertEquals(clusterName, clusterVO.getClusterName());
        assertEquals(clusterType, clusterVO.getClusterType());
    }

    @Test
    void testSetvGroupMappingClusterVO() {
        ClusterVO clusterVO = new ClusterVO();
        assertNotNull(clusterVO);
        assertNotNull(clusterVO.getvGroupMapping());
        assertEquals(0, clusterVO.getvGroupMapping().size());

        ArrayList<String> vGroupMapping = new ArrayList<>();
        vGroupMapping.add("testVGroupMapping1");
        clusterVO.setvGroupMapping(vGroupMapping);
        assertEquals(1, clusterVO.getvGroupMapping().size());
        vGroupMapping.forEach(unit -> assertTrue(clusterVO.getvGroupMapping().contains(unit)));
    }

    @Test
    void testConvertFromClusterData() {
        ClusterData clusterData = new ClusterData();
        clusterData.setClusterName("testCluster");
        clusterData.setClusterType("testClusterType");
        clusterData.getUnitData().put("test", new Unit());

        ClusterVO clusterVO = ClusterVO.convertFromClusterData(clusterData);

        assertNotNull(clusterVO);
        assertNotNull(clusterVO.getUnitData());
        assertNotNull(clusterVO.getvGroupMapping());
        assertEquals(1, clusterVO.getUnitData().size());
        assertEquals(0, clusterVO.getvGroupMapping().size());
        assertEquals("testCluster", clusterVO.getClusterName());
        assertEquals("testClusterType", clusterVO.getClusterType());
    }

    @Test
    void testAddMapping() {
        ClusterVO clusterVO = new ClusterVO();
        clusterVO.getvGroupMapping().add("testVGroupMapping1");

        clusterVO.addMapping("testVGroupMapping1");
        assertEquals(1, clusterVO.getvGroupMapping().size());

        clusterVO.addMapping("testVGroupMapping2");
        assertEquals(2, clusterVO.getvGroupMapping().size());
    }

    @Test
    void testWatchVO() {
        WatcherVO watcherVO1 = new WatcherVO();
        ArrayList<String> watcherIP1 = new ArrayList<>();
        watcherVO1.setvGroup("testWatcher1");
        watcherVO1.setWatcherIp(watcherIP1);
        assertNotNull(watcherVO1);
        assertNotNull(watcherVO1.getWatcherIp());
        assertEquals("testWatcher1", watcherVO1.getvGroup());
        assertEquals(0, watcherVO1.getWatcherIp().size());

        String vGroup = "testWatch";
        ArrayList<String> watcherIP2 = new ArrayList<>();
        watcherIP2.add("127.0.0.1");
        WatcherVO watcherVO2 = new WatcherVO(vGroup, watcherIP2);
        assertNotNull(watcherVO2);
        assertNotNull(watcherVO2.getWatcherIp());
        assertEquals("testWatch", watcherVO2.getvGroup());
        assertEquals(1, watcherVO2.getWatcherIp().size());
        watcherIP2.forEach(unit -> assertTrue(watcherVO2.getWatcherIp().contains(unit)));
    }

    @Test
    void testWatcher() {
        String group = "testWatcher";
        String asyncContext = "testAsyncContext";
        int timeout = 10;
        long term = new Random().nextLong();
        String clientEndpoint = "127.0.0.1";
        Watcher<String> watcher = new Watcher<>(group, asyncContext, timeout, term, clientEndpoint);

        assertNotNull(watcher);
        assertEquals(group, watcher.getGroup());
        assertEquals(asyncContext, watcher.getAsyncContext());
        assertEquals(term, watcher.getTerm());
        assertEquals(clientEndpoint, watcher.getClientEndpoint());
        assertEquals("http", watcher.getProtocol());
        assertFalse(watcher.isDone());

        watcher.setTerm(100);
        watcher.setAsyncContext("newAsyncContext");
        watcher.setClientEndpoint("127.0.0.2");
        watcher.setProtocol("gRPC");
        watcher.setDone(true);
        assertEquals(100, watcher.getTerm());
        assertEquals("newAsyncContext", watcher.getAsyncContext());
        assertEquals("127.0.0.2", watcher.getClientEndpoint());
        assertEquals("gRPC", watcher.getProtocol());
        assertTrue(watcher.isDone());
    }
}
