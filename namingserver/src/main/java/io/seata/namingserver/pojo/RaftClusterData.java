package io.seata.namingserver.pojo;

import io.seata.common.util.StringUtils;
import io.seata.discovery.registry.namingserver.NamingInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
@Component
public class RaftClusterData extends AbstractClusterData{
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftClusterData.class);
    private String clusterName;
    private final HashMap<String/* raft-group */, List<NamingInstance>> UNIT_INSTANCES_MAP;
    private final HashMap<String/* raft-group */, NamingInstance> UNIT_LEADER_MAP;
    private final HashMap<String/* vgroup */,String /* raft-group*/> VGROUP_UNIT_MAP;


    public RaftClusterData() {
        UNIT_INSTANCES_MAP = new HashMap<>();
        VGROUP_UNIT_MAP = new HashMap<>();
        UNIT_LEADER_MAP = new HashMap<>();
    }

    public RaftClusterData(String clusterName) {
        this.clusterName = clusterName;
        UNIT_INSTANCES_MAP = new HashMap<>();
        VGROUP_UNIT_MAP = new HashMap<>();
        UNIT_LEADER_MAP = new HashMap<>();
    }


    public void setLeaderNode(NamingInstance instance) {
        UNIT_LEADER_MAP.put(instance.getUnit(), instance);
    }


    public List<NamingInstance> getInstanceListByVgroup(String vgroup) {
        String unitName=VGROUP_UNIT_MAP.getOrDefault(vgroup,"");
        List<NamingInstance> namingInstanceList=new ArrayList<>();
        // 1.return the leader node of raft-group
        if(UNIT_LEADER_MAP.containsKey(unitName)){
            namingInstanceList.add(UNIT_LEADER_MAP.get(unitName));
            return namingInstanceList;
        }
        // 2.traverse the Raft group, set the leader node and return it
        for(NamingInstance namingInstance:UNIT_INSTANCES_MAP.get(unitName)){
            if(StringUtils.equals(namingInstance.getRole(),"leader")){
                setLeaderNode(namingInstance);
                namingInstanceList.add(UNIT_LEADER_MAP.get(unitName));
                return namingInstanceList;
            }
        }
        // 3.return all nodes of raft-group
        return UNIT_INSTANCES_MAP.get(unitName);
    }

    public void registerInstance(NamingInstance instance){
        String unitName=instance.getUnit();
        UNIT_INSTANCES_MAP.computeIfAbsent(unitName,k->new ArrayList<>()).add(instance);
    }


    public List<NamingInstance> getInstanceList() {
        return UNIT_INSTANCES_MAP.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void removeInstance(NamingInstance instance) {
        String unit=instance.getUnit();
        List<NamingInstance> instances = UNIT_INSTANCES_MAP.get(unit);
        if (instances != null) {
            instances.remove(instance);
            if (instances.isEmpty()) {
                UNIT_INSTANCES_MAP.remove(unit);
            }
        }
        if(StringUtils.equals(instance.getRole(),"leader")){
            UNIT_LEADER_MAP.remove(instance.getUnit(),instance);
        }
    }

    public boolean hasVgroup(String vgroup) {
        return VGROUP_UNIT_MAP.containsKey(vgroup);
    }
}
