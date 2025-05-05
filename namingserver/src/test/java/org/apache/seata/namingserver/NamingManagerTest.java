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

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.namingserver.entity.vo.monitor.ClusterVO;
import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.manager.NamingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Arrays;

import static org.apache.seata.common.NamingServerConstants.CONSTANT_GROUP;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyInt;

@SpringBootTest
class NamingManagerTest {

    private NamingManager namingManager;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    private MockedStatic<HttpClientUtil> mockedHttpClientUtil;

    @BeforeEach
    void setUp() {
        namingManager = new NamingManager();
        ReflectionTestUtils.setField(namingManager, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(namingManager, "heartbeatTimeThreshold", 500000);
        ReflectionTestUtils.setField(namingManager, "heartbeatCheckTimePeriod", 10000000);

        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        mockedHttpClientUtil = Mockito.mockStatic(HttpClientUtil.class);
        mockedHttpClientUtil.when(() -> HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt())).thenReturn(httpResponse);

        namingManager.init();
    }

    private NamingServerNode createTestNode(String host, int port, String unitName) {
        NamingServerNode node = new NamingServerNode();
        node.setTransaction(new Node.Endpoint(host, port, "netty"));
        node.setControl(new Node.Endpoint(host, port + 1000, "http"));
        node.setUnit(unitName);
        node.setRole(ClusterRole.LEADER);
        node.setTerm(1L);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cluster-type", "default");
        node.setMetadata(metadata);
        return node;
    }

    @AfterEach
    void tearDown() {
        if (mockedHttpClientUtil != null) {
            mockedHttpClientUtil.close();
        }
    }

    @Test
    void testRegisterInstance() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();

        NamingServerNode node = createTestNode("127.0.0.1", 8080, unitName);
        boolean result = namingManager.registerInstance(node, namespace, clusterName, unitName);

        assertTrue(result);

        List<Node> instances = namingManager.getInstances(namespace, clusterName);
        assertEquals(1, instances.size());
        assertEquals("127.0.0.1", instances.get(0).getTransaction().getHost());
        assertEquals(8080, instances.get(0).getTransaction().getPort());
    }

    @Test
    void testRegisterInstances() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();

        NamingServerNode node1 = createTestNode("127.0.0.1", 8001, unitName);
        NamingServerNode node2 = createTestNode("127.0.0.1", 8002, unitName);
        List<NamingServerNode> nodeList = Arrays.asList(node1, node2);

        boolean result = namingManager.registerInstances(nodeList, namespace, clusterName);

        assertTrue(result);
        assertEquals(2, namingManager.getInstances(namespace, clusterName).size());
    }

    @Test
    void testUnregisterInstance() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8080, unitName);
        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        List<Node> instances = namingManager.getInstances(namespace, clusterName);
        assertEquals(1, instances.size());

        boolean result = namingManager.unregisterInstance(namespace, clusterName, unitName, node);

        assertTrue(result);
        assertTrue(namingManager.getInstances(namespace, clusterName).isEmpty());
        Mockito.verify(applicationContext, Mockito.times(2)).publishEvent(any(ClusterChangeEvent.class));
    }

    @Test
    void testGetClusterListByVgroup() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8080, unitName);
        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        List<Cluster> clusterListByVgroup = namingManager.getClusterListByVgroup(vGroup, namespace);

        assertNotNull(clusterListByVgroup);
        assertEquals(1, clusterListByVgroup.size());
        assertEquals(clusterName, clusterListByVgroup.get(0).getClusterName());

        List<Cluster> notExist = namingManager.getClusterListByVgroup("NotExist-vGroup", namespace);
        assertEquals(0, notExist.size());
    }

    @Test
    void testInstanceHeartBeatCheck() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8080, unitName);
        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        List<Node> instances = namingManager.getInstances(namespace, clusterName);
        assertEquals(1, instances.size());

        ReflectionTestUtils.setField(namingManager, "heartbeatTimeThreshold", 10);
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        namingManager.instanceHeartBeatCheck();

        List<Node> afterHeartBeat = namingManager.getInstances(namespace, clusterName);
        assertEquals(0, afterHeartBeat.size());
        Mockito.verify(applicationContext, Mockito.times(2)).publishEvent(any(ClusterChangeEvent.class));
    }

    @Test
    void testCreateGroup() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8080, unitName);
        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        Mockito.when(statusLine.getStatusCode()).thenReturn(200);
        Result<String> result = namingManager.createGroup(namespace, vGroup, clusterName, unitName);
        assertTrue(result.isSuccess());
        assertEquals("200", result.getCode());
        assertEquals("add vGroup successfully!", result.getMessage());

        mockedHttpClientUtil.verify(() -> HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt()), Mockito.times(1));
    }

    @Test
    void testCreateGroupNoInstance() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        namingManager.registerInstance(null, namespace, clusterName, unitName);
        Result<String> result = namingManager.createGroup(namespace, vGroup, clusterName, unitName);

        assertFalse(result.isSuccess());
        assertEquals("301", result.getCode());
        assertEquals("no instance in cluster:" + clusterName, result.getMessage());
    }

    @Test
    void testAddGroup() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8080, unitName);
        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        boolean result = namingManager.addGroup(namespace, vGroup, clusterName, unitName);
        assertTrue(result);

        boolean result1 = namingManager.addGroup(namespace, vGroup, clusterName, unitName);
        assertFalse(result1);
    }

    @Test
    void testRemoveGroup() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8000, unitName);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        Unit unit = new Unit();
        unit.setUnitName(unitName);
        List<NamingServerNode> nodeList = new ArrayList<>();
        nodeList.add(node);
        unit.setNamingInstanceList(nodeList);

        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(200);


        mockedHttpClientUtil.when(() -> HttpClientUtil.doGet(anyString(), anyMap(), anyMap(), anyInt()))
                .thenReturn(httpResponse);

        Result<String> result = namingManager.removeGroup(unit, vGroup, clusterName, namespace, unitName);

        assertTrue(result.isSuccess());
        assertEquals("200", result.getCode());
        assertEquals("remove group in old cluster successfully!", result.getMessage());

        mockedHttpClientUtil.verify(() -> HttpClientUtil.doGet(
                anyString(), anyMap(), anyMap(), anyInt()), Mockito.times(1));

    }

    @Test
    void testMonitorCluster() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        List<ClusterVO> emptyResult = namingManager.monitorCluster("empty-namespace");
        assertTrue(emptyResult.isEmpty());

        NamingServerNode node = createTestNode("127.0.0.1", 8001, unitName);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        List<ClusterVO> resultWithoutMapping = namingManager.monitorCluster(namespace);
        assertFalse(resultWithoutMapping.isEmpty());
        assertEquals(1, resultWithoutMapping.size());
        ClusterVO clusterVO = resultWithoutMapping.get(0);
        assertEquals(clusterName, clusterVO.getClusterName());
        assertTrue(clusterVO.getvGroupMapping().isEmpty());

        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        List<ClusterVO> resultWithMapping = namingManager.monitorCluster(namespace);
        assertFalse(resultWithMapping.get(0).getvGroupMapping().isEmpty());
        assertTrue(resultWithMapping.get(0).getvGroupMapping().contains(vGroup));
    }

    @Test
    void testNotifyClusterChange() {
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = UUID.randomUUID().toString();
        String vGroup = "test-vGroup";

        NamingServerNode node = createTestNode("127.0.0.1", 8000, unitName);
        Map<String, String> vGroups = new HashMap<>();
        vGroups.put(vGroup, unitName);
        node.getMetadata().put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);

        namingManager.notifyClusterChange(vGroup, namespace, clusterName, unitName, 1000L);

        Mockito.verify(applicationContext, Mockito.times(2)).publishEvent(any(ClusterChangeEvent.class));
    }
}
