package io.seata.common.metadata;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private String clusterName;
    private String clusterType;
    private List<Unit> unitData;

    public Cluster() {
        unitData = new ArrayList<>();
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public List<Unit> getUnitData() {
        return unitData;
    }

    public void setUnitData(List<Unit> unitData) {
        this.unitData = unitData;
    }


}


