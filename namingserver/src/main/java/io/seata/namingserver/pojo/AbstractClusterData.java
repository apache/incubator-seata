package io.seata.namingserver.pojo;


import io.seata.common.metadata.Node;

import java.util.Collections;
import java.util.List;

public abstract class AbstractClusterData {
    /**
     * register instance in cluster
     * @param node node msg
     * @param unitName unit Name
     */
    public void registerInstance(Node node, String unitName){}

    /**
     * get all nodes in cluster
     * @return all nodes in cluster
     */
    public List<Node> getInstanceList() {
        return Collections.EMPTY_LIST;
    }

    /**
     * remove instacne in cluster
     * @param node node msg
     * @param unitName unit Name
     */
    public void removeInstance(Node node, String unitName) {
    }
}
