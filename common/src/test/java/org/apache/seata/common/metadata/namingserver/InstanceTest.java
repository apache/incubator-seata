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
package org.apache.seata.common.metadata.namingserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.Test;

import static org.apache.seata.common.util.CollectionUtils.mapToJsonString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class InstanceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Instance instance;
    private Instance instanceA;
    private Instance instanceB;
    private Instance instanceC;

    @Test
    void toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Instance instance = Instance.getInstance();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> mmap = new HashMap<>();
        mmap.put("k", "v");
        map.put("k", mmap);
        instance.setMetadata(map);
        instance.setNamespace("namespace");
        instance.setClusterName("clustername");
        instance.setRole(ClusterRole.LEADER);
        instance.setUnit("unit");
        instance.setWeight(100d);
        instance.setHealthy(true);
        instance.setTerm(100L);
        instance.setTimestamp(System.currentTimeMillis());
        instance.setControl(new Node.Endpoint("1.1.1.1", 888));
        instance.setTransaction(new Node.Endpoint("2.2.2.2", 999));
        assertEquals(instance.toJsonString(objectMapper), objectMapper.writeValueAsString(instance));
    }

    @Test
    public void testGetInstance() {
        Instance anotherInstance = Instance.getInstance();
        instance = Instance.getInstance();
        assertEquals(instance, anotherInstance);
    }

    @Test
    public void testJsonSerializationShouldSerialize() {
        instance = Instance.getInstance();
        instance.setNamespace("testNamespace");
        instance.setClusterName("testCluster");
        instance.setUnit("testUnit");
        instance.getControl().setPort(1234);
        instance.getTransaction().setPort(4321);
        instance.setWeight(0.5);
        instance.setHealthy(false);
        instance.setTerm(1);
        instance.setTimestamp(System.currentTimeMillis());
        instance.addMetadata("key1", "value1");

        String jsonString = instance.toJsonString(objectMapper);
        Instance deserializedInstance = null;
        try {
            deserializedInstance = objectMapper.readValue(jsonString, Instance.class);
        } catch (Exception e) {
            fail("Exception during JSON deserialization: " + e.getMessage());
        }

        assertNotNull(deserializedInstance);
        assertEquals(instance.getNamespace(), deserializedInstance.getNamespace());
        assertEquals(instance.getClusterName(), deserializedInstance.getClusterName());
        assertEquals(instance.getUnit(), deserializedInstance.getUnit());
        assertEquals(instance.getControl().getPort(), deserializedInstance.getControl().getPort());
        assertEquals(instance.getTransaction().getPort(), deserializedInstance.getTransaction().getPort());
        assertEquals(instance.getWeight(), deserializedInstance.getWeight(), 0.0);
        assertEquals(instance.isHealthy(), deserializedInstance.isHealthy());
        assertEquals(instance.getTerm(), deserializedInstance.getTerm());
        assertEquals(instance.getTimestamp(), deserializedInstance.getTimestamp());
        assertEquals(instance.getMetadata(), deserializedInstance.getMetadata());
    }

    @Test
    public void testToMapShouldReturnCorrectMap() {
        instance = Instance.getInstance();
        instance.setNamespace("testNamespace");
        instance.setClusterName("testCluster");
        instance.setUnit("testUnit");
        instance.setControl(new Node.Endpoint("127.0.0.1", 1234));
        instance.setTransaction(new Node.Endpoint("127.0.0.1", 4321));

        instance.setWeight(0.5);
        instance.setHealthy(false);
        instance.setTerm(1);
        instance.setTimestamp(System.currentTimeMillis());
        instance.addMetadata("key1", "value1");

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("namespace", instance.getNamespace());
        resultMap.put("clusterName", instance.getClusterName());
        resultMap.put("unit", instance.getUnit());
        resultMap.put("control", instance.getControl().toString());
        resultMap.put("transaction", instance.getTransaction().toString());
        resultMap.put("weight", String.valueOf(instance.getWeight()));
        resultMap.put("healthy", String.valueOf(instance.isHealthy()));
        resultMap.put("term", String.valueOf(instance.getTerm()));
        resultMap.put("timestamp", String.valueOf(instance.getTimestamp()));
        resultMap.put("metadata", mapToJsonString(instance.getMetadata()));

        assertEquals("testNamespace", resultMap.get("namespace"));
        assertEquals("testCluster", resultMap.get("clusterName"));
        assertEquals("testUnit", resultMap.get("unit"));
        assertTrue(resultMap.get("control").contains("1234"));
        assertTrue(resultMap.get("transaction").contains("4321"));
        assertEquals("0.5", resultMap.get("weight"));
        assertEquals("false", resultMap.get("healthy"));
        assertEquals("1", resultMap.get("term"));
        assertTrue(resultMap.get("timestamp").matches("\\d+"));
        assertTrue(resultMap.get("metadata").contains("key1"));
        assertTrue(resultMap.get("metadata").contains("value1"));
    }

    @Test
    public void testSameInstance() {
        instanceA = Instance.getInstance();
        instanceA.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceA.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        assertEquals(instanceA, instanceA);
    }

    @Test
    public void testNull() {
        instanceA = Instance.getInstance();
        instanceA.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceA.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        assertNotEquals(instanceA, null);
    }

    @Test
    public void testDifferentClass() {
        instanceA = Instance.getInstance();
        instanceA.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceA.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        assertNotEquals(instanceA, "NotAnInstance");
    }

    @Test
    public void testSameFields() {
        instanceA = Instance.getInstance();
        instanceA.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceA.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        instanceB = Instance.getInstance();
        instanceB.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceB.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        assertEquals(instanceA, instanceB);
    }

    @Test
    public void testDifferentControlPort() {
        instanceA = Instance.getInstance();
        instanceA.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceA.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        instanceC = Instance.getInstance();
        instanceC.setControl(new Node.Endpoint("127.0.0.1", 8081));
        instanceC.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        instanceC.getControl().setPort(8080);
        assertTrue(instanceA.equals(instanceC));
    }

    @Test
    public void testDifferentTransactionPort() {
        instanceA = Instance.getInstance();
        instanceA.setControl(new Node.Endpoint("127.0.0.1", 8080));
        instanceA.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        instanceC = Instance.getInstance();
        instanceC.setControl(new Node.Endpoint("127.0.0.1", 8081));
        instanceC.setTransaction(new Node.Endpoint("127.0.0.1", 9090));
        assertTrue(instanceA.equals(instanceC));
    }
}