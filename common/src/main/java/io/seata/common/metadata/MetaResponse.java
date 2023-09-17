package io.seata.common.metadata;

import java.util.List;

public class MetaResponse {
    private List<Cluster> clusterList;
    private long term;

    public MetaResponse() {
    }

    public MetaResponse(List<Cluster> clusterList, long term) {
        this.clusterList = clusterList;
        this.term = term;
    }

    public List<Cluster> getClusterList() {
        return clusterList;
    }

    public void setClusterList(List<Cluster> clusterList) {
        this.clusterList = clusterList;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }
}
