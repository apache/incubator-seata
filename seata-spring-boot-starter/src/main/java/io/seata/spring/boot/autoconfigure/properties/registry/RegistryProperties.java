/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure.properties.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_LOAD_BALANCE;
import static io.seata.common.DefaultValues.VIRTUAL_NODES_DEFAULT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_PREFIX)
public class RegistryProperties {
    /**
     * file, nacos, eureka, redis, zk, consul, etcd3, sofa
     */
    private String type = "file";
    /**
     * the load balance
     */
    private String loadBalance = DEFAULT_LOAD_BALANCE;
    /**
     * the load balance virtual nodes
     */
    private int loadBalanceVirtualNodes = VIRTUAL_NODES_DEFAULT;

    public String getType() {
        return type;
    }

    public RegistryProperties setType(String type) {
        this.type = type;
        return this;
    }

    public String getLoadBalance() {
        return loadBalance;
    }

    public RegistryProperties setLoadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
        return this;
    }

    public int getLoadBalanceVirtualNodes() {
        return loadBalanceVirtualNodes;
    }

    public RegistryProperties setLoadBalanceVirtualNodes(int loadBalanceVirtualNodes) {
        this.loadBalanceVirtualNodes = loadBalanceVirtualNodes;
        return this;
    }
}
