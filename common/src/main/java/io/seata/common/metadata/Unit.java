package io.seata.common.metadata;

import java.util.List;

public class Unit {

    private String unitName;

    private List<Node> nodeList;

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public List<Node> getNamingInstanceList() {
        return nodeList;
    }

    public void setNamingInstanceList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public void removeInstance(Node node){
        if (nodeList != null) {
            nodeList.remove(node);
        }
    }


}
