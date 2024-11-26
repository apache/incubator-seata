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
