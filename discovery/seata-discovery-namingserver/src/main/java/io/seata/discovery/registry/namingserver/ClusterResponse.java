package io.seata.discovery.registry.namingserver;

import java.util.List;

public class ClusterResponse {
    private List<NamingInstance> namingInstanceList;
    private long term;

    public ClusterResponse(){
    }

    public ClusterResponse(List<NamingInstance> namingInstanceList, long term) {
        this.namingInstanceList = namingInstanceList;
        this.term = term;
    }

    public List<NamingInstance> getNamingInstanceList() {
        return namingInstanceList;
    }

    public void setNamingInstanceList(List<NamingInstance> namingInstanceList) {
        this.namingInstanceList = namingInstanceList;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }
}
