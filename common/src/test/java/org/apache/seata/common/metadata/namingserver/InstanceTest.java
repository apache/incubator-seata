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
import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class InstanceTest {
    private ObjectMapper objectMapper;
    private Instance instance;
    private Instance instanceA;
    private Instance instanceB;
    private Instance instanceC;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        instance = Instance.getInstance();
        instanceA = Instance.getInstance();
        instanceB = Instance.getInstance();
        instanceC = Instance.getInstance();

        instanceA.getControl().setHost("127.0.0.1");
        instanceA.getControl().setPort(8080);

        instanceA.getTransaction().setHost("127.0.0.1");
        instanceA.getTransaction().setPort(9090);

        instanceB.getControl().setHost("127.0.0.1");
        instanceB.getControl().setPort(8080);
        instanceB.getTransaction().setHost("127.0.0.1");
        instanceB.getTransaction().setPort(9090);


        ;
        instanceC.getControl().setHost("127.0.0.1");
        instanceC.getControl().setPort(8081);

        instanceC.getTransaction().setHost("127.0.0.1");
        instanceC.getTransaction().setPort(9090);
    }

    @Test
    void toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Instance instance = Instance.getInstance();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> mmap = new HashMap<>();
        mmap.put("k", "v");
        map.put("k", mmap);
        instance.setMetadata(map);
        instance.setControl(new Node.Endpoint("1.1.1.1", 888));
        instance.setTransaction(new Node.Endpoint("2.2.2.2", 999));
        assertEquals(instance.toJsonString(objectMapper), objectMapper.writeValueAsString(instance));
    }

    @Test
    public void testGetInstance_ShouldReturnSingletonInstance() {
        Instance anotherInstance = Instance.getInstance();
        assertEquals(instance, anotherInstance);
    }

    @Test
    public void testJsonSerialization_ShouldSerializeAndDeserializeCorrectly() {
        // Setup
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

        // Act
        String jsonString = instance.toJsonString(objectMapper);
        Instance deserializedInstance = null;
        try {
            deserializedInstance = objectMapper.readValue(jsonString, Instance.class);
        } catch (Exception e) {
            fail("Exception during JSON deserialization: " + e.getMessage());
        }

        // Assert
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
    public void testToMap_ShouldReturnCorrectMap() {
        // Setup
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

        // Act
        Map<String, String> resultMap = instance.toMap();

        // Assert
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
    void equals_SameInstance_ReturnsTrue() {
        assertEquals(instanceA, instanceA);
    }

    @Test
    void equals_Null_ReturnsFalse() {
        assertNotEquals(instanceA, null);
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        assertNotEquals(instanceA, "NotAnInstance");
    }

    @Test
    void equals_SameFields_ReturnsTrue() {
        assertEquals(instanceA, instanceB);
    }

    @Test
    void equals_DifferentControlPort_ReturnsFalse() {
        instanceC.getControl().setPort(8080);
        assertTrue(instanceA.equals(instanceC));
    }

    @Test
    void equals_DifferentTransactionPort_ReturnsFalse() {
        assertTrue(instanceA.equals(instanceC));
    }

}