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

import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.namingserver.controller.NamingControllerV2;
import org.apache.seata.namingserver.entity.vo.v2.NamespaceVO;
import org.apache.seata.namingserver.manager.NamingManager;
import org.apache.seata.namingserver.metrics.NoOpNamingMetricsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.apache.seata.common.NamingServerConstants.CONSTANT_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NamingControllerV2Test {

    private NamingControllerV2 namingControllerV2;

    private NamingManager namingManager;

    @BeforeEach
    void setUp() {
        namingManager = new NamingManager();
        ReflectionTestUtils.setField(namingManager, "metricsManager", new NoOpNamingMetricsManager());
        ReflectionTestUtils.setField(namingManager, "heartbeatTimeThreshold", 500000);
        ReflectionTestUtils.setField(namingManager, "heartbeatCheckTimePeriod", 10000000);
        namingManager.init();
        namingControllerV2 = new NamingControllerV2();
        ReflectionTestUtils.setField(namingControllerV2, "namingManager", namingManager);
    }

    @Test
    void testNamespaces() {
        String clusterName = "test-cluster";
        String namespace = "test-namespace";
        String vGroup = "test-vGroup";
        String unitName = String.valueOf(UUID.randomUUID());

        // Register an instance to set up data
        NamingServerNode node = new NamingServerNode();
        node.setTransaction(new Node.Endpoint("127.0.0.1", 8091, "netty"));
        node.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        Map<String, Object> metadata = node.getMetadata();
        Map<String, Object> vGroups = new java.util.HashMap<>();
        vGroups.put(vGroup, unitName);
        metadata.put(CONSTANT_GROUP, vGroups);
        namingManager.registerInstance(node, namespace, clusterName, unitName);
        namingManager.addGroup(namespace, clusterName, unitName, vGroup);

        // Call the namespaces method
        SingleResult<Map<String, NamespaceVO>> result = namingControllerV2.namespaces();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("200", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().containsKey(namespace));

        NamespaceVO namespaceVO = result.getData().get(namespace);
        assertNotNull(namespaceVO);
        assertNotNull(namespaceVO.getClusters());
        assertTrue(namespaceVO.getClusters().containsKey(clusterName));
        org.apache.seata.namingserver.entity.vo.v2.ClusterVO clusterVO =
                namespaceVO.getClusters().get(clusterName);
        assertNotNull(clusterVO);
        assertNotNull(clusterVO.getVgroups());
        assertTrue(clusterVO.getVgroups().contains(vGroup));
        assertNotNull(clusterVO.getUnits());
        assertTrue(clusterVO.getUnits().contains(unitName));

        // Clean up
        namingManager.unregisterInstance(namespace, clusterName, unitName, node);
    }
}
