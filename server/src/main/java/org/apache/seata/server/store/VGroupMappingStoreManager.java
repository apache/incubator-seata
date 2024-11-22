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
package org.apache.seata.server.store;

import org.apache.seata.common.XID;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public interface VGroupMappingStoreManager {
    /**
     * add VGroup Mapping relationship in cluster
     *
     * @param mappingDO the relationship between vGroup and Cluster
     */
    boolean addVGroup(MappingDO mappingDO);

    /**
     * remove VGroup Mapping relationship in cluster
     *
     * @param vGroup
     */
    boolean removeVGroup(String vGroup);

    /**
     * get VGroup Mapping relationship in cluster
     *
     * @return Key:vGroup,Value:unit
     */
    HashMap<String, Object> loadVGroups();

    default HashMap<String, Object> readVGroups() {
        return loadVGroups();
    }

    /**
     * notify mapping relationship to all namingserver nodes
     */
    default void notifyMapping() {
        Instance instance = Instance.getInstance();
        Map<String, Object> map = this.readVGroups();
        instance.addMetadata("vGroup", map);
        try {
            InetSocketAddress address = new InetSocketAddress(XID.getIpAddress(), XID.getPort());
            for (RegistryService<?> registryService : MultiRegistryFactory.getInstances()) {
                registryService.register(address);
            }
        } catch (Exception e) {
            throw new RuntimeException("vGroup mapping relationship notified failed! ", e);
        }
    }

}
