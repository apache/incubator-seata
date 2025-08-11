package org.apache.seata.mcp.entity.pojo;

import org.apache.seata.common.util.StringUtils;

public class NameSpaceDetail {
    private String namespace;
    private String cluster;
    private String vGroup;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getvGroup() {
        return vGroup;
    }

    public void setvGroup(String vGroup) {
        this.vGroup = vGroup;
    }

    public boolean isValid(){
        if(StringUtils.isBlank(namespace)){
            return false;
        }
        return !StringUtils.isBlank(vGroup) || !StringUtils.isBlank(cluster);
    }

    @Override
    public String toString() {
        return "NameSpaceDetail{" +
                "namespace='" + namespace + '\'' +
                ", cluster='" + cluster + '\'' +
                ", vGroup='" + vGroup + '\'' +
                '}';
    }
}
