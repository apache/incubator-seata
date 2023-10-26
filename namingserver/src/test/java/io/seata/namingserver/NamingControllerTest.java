package io.seata.namingserver;

import io.seata.common.metadata.Cluster;
import io.seata.common.metadata.MetaResponse;
import io.seata.common.metadata.Node;
import io.seata.common.metadata.Unit;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
class NamingControllerTest {

    @Value("${heartbeat.threshold}")
    private int threshold;

    @Value("${heartbeat.period}")
    private int period;
    @Autowired
    NamingController namingController;


    @Test
    void mockRegister() {
        String clusterName = "cluster1";
        String namespace = "public";
        String unitName = String.valueOf(UUID.randomUUID());
        Node node = new Node();
        node.setTransactionEndpoint(new Node.Endpoint("127.0.0.1", 8080, "netty"));
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, null, vGroup);
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        Node node1 = unit.getNamingInstanceList().get(0);
        assertEquals("127.0.0.1", node1.getTransactionEndpoint().getHost());
        assertEquals(8080, node1.getTransactionEndpoint().getPort());
        namingController.unregisterInstance(unitName, node);
    }

    @Test
    void mockUnregisterGracefully() {
        String clusterName = "cluster1";
        String namespace = "public";
        String unitName = String.valueOf(UUID.randomUUID());
        Node node = new Node();
        node.setTransactionEndpoint(new Node.Endpoint("127.0.0.1", 8080, "netty"));
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, null, vGroup);
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        Node node1 = unit.getNamingInstanceList().get(0);
        assertEquals("127.0.0.1", node1.getTransactionEndpoint().getHost());
        assertEquals(8080, node1.getTransactionEndpoint().getPort());
        namingController.unregisterInstance(unitName, node);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        cluster = metaResponse.getClusterList().get(0);
        assertEquals(0, cluster.getUnitData().size());
    }

    @Test
    void mockUnregisterUngracefully() throws InterruptedException {
        String clusterName = "cluster1";
        String namespace = "public";
        String unitName = String.valueOf(UUID.randomUUID());
        Node node = new Node();
        node.setTransactionEndpoint(new Node.Endpoint("127.0.0.1", 8080, "netty"));
        namingController.registerInstance(namespace, clusterName, unitName, node);
        String vGroup = "vgroup1";
        namingController.changeGroup(namespace, clusterName, null, vGroup);
        MetaResponse metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        Cluster cluster = metaResponse.getClusterList().get(0);
        assertNotNull(cluster.getUnitData());
        assertEquals(1, cluster.getUnitData().size());
        Unit unit = cluster.getUnitData().get(0);
        assertNotNull(unit.getNamingInstanceList());
        assertEquals(1, unit.getNamingInstanceList().size());
        Node node1 = unit.getNamingInstanceList().get(0);
        assertEquals("127.0.0.1", node1.getTransactionEndpoint().getHost());
        assertEquals(8080, node1.getTransactionEndpoint().getPort());
        int timeGap = threshold + period;
        Thread.sleep(timeGap);
        metaResponse = namingController.discovery(vGroup, namespace);
        assertNotNull(metaResponse);
        assertNotNull(metaResponse.getClusterList());
        assertEquals(1, metaResponse.getClusterList().size());
        cluster = metaResponse.getClusterList().get(0);
        assertEquals(0, cluster.getUnitData().size());
    }
}