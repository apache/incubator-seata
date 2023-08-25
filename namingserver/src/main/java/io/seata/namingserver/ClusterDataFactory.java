package io.seata.namingserver;

import io.seata.common.util.StringUtils;
import io.seata.namingserver.pojo.AbstractClusterData;
import io.seata.namingserver.pojo.ClusterData;
import io.seata.namingserver.pojo.RaftClusterData;

public class ClusterDataFactory {

    public static AbstractClusterData createClusterData(String type) {
        if (StringUtils.equals(type,"default")) {
            return new ClusterData();
        } else if (StringUtils.equals(type,"raft")) {
            return new RaftClusterData();
        } else {
            return new ClusterData();
        }
    }
}

