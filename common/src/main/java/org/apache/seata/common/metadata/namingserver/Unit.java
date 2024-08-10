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

import org.apache.seata.common.metadata.Node;

import java.util.List;

public class Unit {

    private String unitName;

    private List<NamingServerNode> nodeList;

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public List<NamingServerNode> getNamingInstanceList() {
        return nodeList;
    }

    public void setNamingInstanceList(List<NamingServerNode> nodeList) {
        this.nodeList = nodeList;
    }

    public void removeInstance(Node node) {
        if (nodeList != null) {
            nodeList.remove(node);
        }
    }

    /**
     * @param node node
     */
    public void addInstance(NamingServerNode node) {
        if (nodeList.contains(node)) {
            Node node1 = nodeList.get(nodeList.indexOf(node));
            if (node.isTotalEqual(node1)) {
                return;
            } else {
                nodeList.remove(node1);
            }
        }
        nodeList.add(node);

    }


}
