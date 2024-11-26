package org.apache.seata.common.metadata.namingserver;

import org.apache.seata.common.metadata.Cluster;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class MetaResponseTest {

    @Test
    void testConstructor() {
        List<Cluster> clusterList = new ArrayList<>();
        long term = 12345L;
        MetaResponse metaResponse = new MetaResponse(clusterList, term);

        Assertions.assertEquals(clusterList, metaResponse.getClusterList());
        Assertions.assertEquals(term, metaResponse.getTerm());
    }

    @Test
    void testGettersAndSetters() {
        MetaResponse metaResponse = new MetaResponse();

        List<Cluster> clusterList = new ArrayList<>();
        metaResponse.setClusterList(clusterList);
        Assertions.assertEquals(clusterList, metaResponse.getClusterList());

        long term = 67890L;
        metaResponse.setTerm(term);
        Assertions.assertEquals(term, metaResponse.getTerm());
    }
}
