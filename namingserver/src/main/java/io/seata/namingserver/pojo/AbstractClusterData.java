package io.seata.namingserver.pojo;

import io.seata.discovery.registry.namingserver.NamingInstance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractClusterData {
    public List<NamingInstance> getInstanceListByVgroup(String vgroup) {
        return null;
    }

    public void registerInstance(NamingInstance instance){}

    public List<NamingInstance> getInstanceList() {
        return Collections.EMPTY_LIST;
    }

    public void removeInstance(NamingInstance instance) {
    }

    public void addvGroup(String vGroup){}

    public void removeGroup(String vGroup){}

    public boolean hasVgroup(String vgroup) {
        return false;
    }

    public Set<String> getVgroups(){
        return null;
    }
}
