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
package org.apache.seata.server.storage.raft.sore;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.alipay.sofa.jraft.Closure;
import org.apache.seata.common.XID;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.RaftVGroupSyncMsg;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.apache.seata.server.store.VGroupMappingStoreManager;

@LoadLevel(name = "raft")
public class RaftVGroupMappingStoreManager implements VGroupMappingStoreManager {

    private final static Map<String/*unit(raft group)*/, Map<String/*vgroup*/, MappingDO>> VGROUP_MAPPING =
        new HashMap<>();


    public boolean localAddVGroup(MappingDO mappingDO) {
        return VGROUP_MAPPING.computeIfAbsent(mappingDO.getUnit(), k -> new HashMap<>()).put(mappingDO.getVGroup(),
            mappingDO) != null;
    }

    public void localAddVGroups(Map<String/*vgroup*/, MappingDO> vGroups, String unit) {
        VGROUP_MAPPING.computeIfAbsent(unit, k -> new HashMap<>()).putAll(vGroups);
    }

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                completableFuture.complete(localAddVGroup(mappingDO));
            } else {
                completableFuture.complete(false);
            }
        };
        RaftVGroupSyncMsg raftVGroupSyncMsg = new RaftVGroupSyncMsg(mappingDO, RaftSyncMsgType.ADD_VGROUP_MAPPING);
        try {
            RaftTaskUtil.createTask(closure, raftVGroupSyncMsg, completableFuture);
            return completableFuture.get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                completableFuture.complete(localRemoveVGroup(vGroup));
            } else {
                completableFuture.complete(false);
            }
        };
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup(vGroup);
        RaftVGroupSyncMsg raftVGroupSyncMsg = new RaftVGroupSyncMsg(mappingDO, RaftSyncMsgType.REMOVE_VGROUP_MAPPING);
        try {
            RaftTaskUtil.createTask(closure, raftVGroupSyncMsg, completableFuture);
            return completableFuture.get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        }
    }

    public boolean localRemoveVGroup(String vGroup) {
        VGROUP_MAPPING.forEach((unit, vgroup) -> vgroup.remove(vGroup));
        return true;
    }

    @Override
    public Map<String, Object> loadVGroups() {
        Map<String, Object> result = new HashMap<>();
        VGROUP_MAPPING.forEach((unit, vgroup) -> {
            for (String group : vgroup.keySet()) {
                result.put(group, unit);
            }
        });
        return result;
    }

    public Map<String/*vgroup*/, MappingDO> loadVGroupsByUnit(String unit) {
        return VGROUP_MAPPING.getOrDefault(unit, Collections.emptyMap());
    }

    @Override
    public Map<String, Object> readVGroups() {
        return loadVGroups();
    }

    @Override
   public void notifyMapping() {
        Instance instance = Instance.getInstance();
        Map<String, Object> map = this.readVGroups();
        instance.addMetadata("vGroup", map);
        for (String group : RaftServerManager.groups()) {
            Instance node = instance.clone();
            node.setRole(RaftServerManager.isLeader(group) ? ClusterRole.LEADER : ClusterRole.FOLLOWER);
            Instance.getInstances().add(node);
        }
        try {
            InetSocketAddress address = new InetSocketAddress(XID.getIpAddress(), XID.getPort());
            for (RegistryService<?> registryService : MultiRegistryFactory.getInstances()) {
                registryService.register(address);
            }
        } catch (Exception e) {
            throw new RuntimeException("vGroup mapping relationship notified failed! ", e);
        } finally {
            Instance.getInstances().clear();
        }
    }

}
