/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.namingserver.pojo;


import io.seata.common.metadata.Node;

import java.util.Collections;
import java.util.List;

public abstract class AbstractClusterData {
    /**
     * register instance in cluster
     *
     * @param node     node msg
     * @param unitName unit Name
     * @return true if the node has changed, false if there is no change.
     */
    public boolean registerInstance(Node node, String unitName) {
        return false;
    }

    /**
     * get all nodes in cluster
     *
     * @return all nodes in cluster
     */
    public List<Node> getInstanceList() {
        return Collections.EMPTY_LIST;
    }

    /**
     * remove instacne in cluster
     *
     * @param node     node msg
     * @param unitName unit Name
     */
    public void removeInstance(Node node, String unitName) {
    }
}
