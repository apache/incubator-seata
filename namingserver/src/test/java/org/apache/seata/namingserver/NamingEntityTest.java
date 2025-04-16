package org.apache.seata.namingserver;

import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.namingserver.entity.bo.ClusterBO;
import org.apache.seata.namingserver.entity.pojo.ClusterData;
import org.apache.seata.namingserver.entity.vo.NamespaceVO;
import org.apache.seata.namingserver.entity.vo.monitor.ClusterVO;
import org.apache.seata.namingserver.entity.vo.monitor.WatcherVO;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RunWith(SpringRunner.class)
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
        assertAll(
                () -> assertEquals(unitNames1.size(), actualUnitNames.size()),
                () -> unitNames1.forEach(unit ->
                        assertTrue(actualUnitNames.contains(unit))),
                () -> actualUnitNames.forEach(unit ->
                        assertTrue(unitNames1.contains(unit)))
        );

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
}
