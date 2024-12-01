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
import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NamingServerNodeTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        NamingServerNode node = new NamingServerNode();
        Map<String,Object> map = new HashMap<>();
        map.put("k","v");
        node.setMetadata(map);
        node.setGroup("group");
        node.setUnit("unit");
        node.setHealthy(true);
        node.setTerm(111L);
        node.setControl(new Node.Endpoint("1.1.1.1",888));
        node.setTransaction(new Node.Endpoint("2.2.2.2",999));
        assertEquals(node.toJsonString(objectMapper),objectMapper.writeValueAsString(node));
    }
    
    @Test
    public void testContains() {
        NamingServerNode node1 = new NamingServerNode();
        node1.setControl(new Node.Endpoint("111.11.11.1",123));
        node1.setTransaction(new Node.Endpoint("111.11.11.1",124));
        Node node2 = new Node();
        node2.setControl(new Node.Endpoint("111.11.11.1",123));
        node2.setTransaction(new Node.Endpoint("111.11.11.1",124));
        NamingServerNode node3 = new NamingServerNode();
        node3.setControl(new Node.Endpoint("111.11.11.1",123));
        node3.setTransaction(new Node.Endpoint("111.11.11.1",124));
        Assertions.assertFalse(node1.equals(node2));
        Assertions.assertTrue(node1.equals(node3));
    }
}