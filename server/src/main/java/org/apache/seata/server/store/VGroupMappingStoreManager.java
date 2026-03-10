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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;

import java.net.InetSocketAddress;
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
    Map<String, Object> loadVGroups();

    default Map<String, Object> readVGroups() {
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
            int registryPort = 0;
            String strPort = ConfigurationFactory.getInstance()
                    .getConfig(ConfigurationKeys.SERVER_REGISTRY_PORT_CAMEL);
            if (strPort != null) {
                try {
                    registryPort = Integer.parseInt(strPort);
                } catch (NumberFormatException ignored) {
                }
            }
            int port = registryPort > 0 ? registryPort : XID.getPort();
            InetSocketAddress address = new InetSocketAddress(XID.getIpAddress(), port);
            for (RegistryService<?> registryService : MultiRegistryFactory.getInstances()) {
                registryService.register(address);
            }
        } catch (Exception e) {
            throw new RuntimeException("vGroup mapping relationship notified failed! ", e);
        }
    }
}
