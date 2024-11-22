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

import java.util.ArrayList;
import java.util.List;
import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
//TODO-PSX all unit not pass
public class UnitTest {
    private Unit unit;
    private NamingServerNode node1;
    private NamingServerNode node2;

    @BeforeEach
    public void setUp() {
        unit = new Unit();
        unit.setUnitName("unit1");

        Node control1 = new Node();
        control1.setControl(new Node.Endpoint("localhost", 8091));
        node1 = new NamingServerNode();
        node1.setControl(control1.getControl());

        Node control2 = new Node();
        control2.setControl(new Node.Endpoint("localhost", 8092));
        node2 = new NamingServerNode();
        node2.setControl(control2.getControl());
    }


    @Test
    public void testAddInstance_NewInstance_ShouldAddSuccessfully() {
        assertTrue(unit.addInstance(node1));
    }

    @Test
    public void testAddInstance_NodeAlreadyExists_ShouldNotAdd() {
        unit.addInstance(node1);
        assertFalse(unit.addInstance(node1));
    }

    @Test
    public void testAddInstance_NodeChanged_ShouldUpdate() {
        unit.addInstance(node1);
        node1.setTerm(2L); // Change the term to simulate a newer node
        assertTrue(unit.addInstance(node1));
    }

    @Test
    public void testAddInstance_NodeDifferentPort_ShouldAddSuccessfully() {
        unit.addInstance(node1);
        NamingServerNode nodeDifferentPort = new NamingServerNode();
        Node controlDifferentPort = new Node();
        controlDifferentPort.setControl(new Node.Endpoint("localhost", 8093));
        nodeDifferentPort.setControl(controlDifferentPort.getControl());
        assertTrue(unit.addInstance(nodeDifferentPort));
    }

    @Test
    public void testRemoveInstance_NodeExists_ShouldRemoveSuccessfully() {
        unit.addInstance(node1);
        unit.removeInstance(node1);
        assertFalse(unit.getNamingInstanceList().contains(node1));
    }

    @Test
    public void testRemoveInstance_NodeDoesNotExist_ShouldNotRemove() {
        unit.addInstance(node1);
        NamingServerNode nodeDifferent = new NamingServerNode();
        Node controlDifferent = new Node();
        controlDifferent.setControl(new Node.Endpoint("localhost", 8093));
        nodeDifferent.setControl(controlDifferent.getControl());
        unit.removeInstance(nodeDifferent);
        assertTrue(unit.getNamingInstanceList().contains(node1));
    }

    @Test
    public void testGetNamingInstanceList_ShouldReturnCorrectList() {
        List<NamingServerNode> expectedList = new ArrayList<>();
        expectedList.add(node1);
        unit.addInstance(node1);

        List<NamingServerNode> actualList = unit.getNamingInstanceList();

        assertEquals(expectedList, actualList);
    }
}
