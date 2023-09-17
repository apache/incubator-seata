package io.seata.core.store;

public class MappingDO {
    private String namespace;

    private String cluster;

    private String unit;

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getVGroup() {
        return vGroup;
    }

    public void setVGroup(String vGroup) {
        this.vGroup = vGroup;
    }

}
