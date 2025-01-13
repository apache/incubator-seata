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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class UnitTest {

    private Unit unit;
    private NamingServerNode node1;
    private NamingServerNode node2;

    @BeforeEach
    void setUp() {
        unit = new Unit();
        node1 = new NamingServerNode();
        node1.setTerm(1L);
        node2 = new NamingServerNode();
        node2.setTerm(2L);
        List<NamingServerNode> nodeList = new ArrayList<>();
        nodeList.add(node1);
        unit.setNamingInstanceList(nodeList);
    }

    @Test
    void testGettersAndSetters() {
        unit.setUnitName("TestUnit");
        Assertions.assertEquals("TestUnit", unit.getUnitName());

        List<NamingServerNode> newList = new ArrayList<>();
        unit.setNamingInstanceList(newList);
        Assertions.assertEquals(newList, unit.getNamingInstanceList());
    }

    @Test
    void testRemoveInstance() {
        unit.removeInstance(node1);
        Assertions.assertFalse(unit.getNamingInstanceList().contains(node1));
    }

    @Test
    void testAddInstance() {
        // Test adding a new node
        Assertions.assertTrue(unit.addInstance(node2));
        Assertions.assertTrue(unit.getNamingInstanceList().contains(node2));

        // Test adding an existing node with a different term
        node1.setTerm(3L);
        Assertions.assertTrue(unit.addInstance(node1));
        Assertions.assertEquals(1, unit.getNamingInstanceList().size());

        // Test adding an existing node without change
        NamingServerNode node3 = new NamingServerNode();
        node3.setTerm(3L);
        Assertions.assertFalse(unit.addInstance(node3));
        Assertions.assertEquals(1, unit.getNamingInstanceList().size());
    }
}
