package io.seata.namingserver.pojo;


import io.seata.discovery.registry.namingserver.NamingInstance;
import io.seata.namingserver.listener.ClusterChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Component
public class ClusterData extends AbstractClusterData implements ApplicationContextAware {
    private String clusterName;
    private final List<NamingInstance> instanceSet;
    private final Set<String> vgroupSet;
    private volatile long term = 0L;
    private static ApplicationContext applicationContext;



    public List<NamingInstance> getInstanceSet() {
        return instanceSet;
    }

    public void removeInstance(NamingInstance namingInstance) {
        instanceSet.remove(namingInstance);
        term = System.currentTimeMillis();
        for (String vGroup : vgroupSet) {
            applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, term));
        }
    }


    public ClusterData() {
        instanceSet = new ArrayList<>(32) ;
        vgroupSet = new HashSet<>(32);
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterType() {
        return "default";
    }

    public List<NamingInstance> getInstanceList() {
        return instanceSet;
    }


    public Set<String> getVgroups(){
        return  vgroupSet;
    }

    public boolean hasVgroup(String vgroup) {
        return vgroupSet.contains(vgroup);
    }

    public void addvGroup(String vgroup) {
        vgroupSet.add(vgroup);
        term = System.currentTimeMillis();
        applicationContext.publishEvent(new ClusterChangeEvent(this, vgroup, term));
    }

    public void removeGroup(String vgroup) {
        vgroupSet.remove(vgroup);
        term = System.currentTimeMillis();
        applicationContext.publishEvent(new ClusterChangeEvent(this, vgroup, term));
    }

    public List<NamingInstance> getInstanceListByVgroup(String vgroup) {
        if (!hasVgroup(vgroup)) {
            return null;
        }
        return new ArrayList<>(instanceSet);
    }

    public void registerInstance(NamingInstance instance) {
        instanceSet.remove(instance);
        instanceSet.add(instance);
        term = System.currentTimeMillis();
        for (String vGroup : vgroupSet) {
            applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, term));
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ClusterData.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
