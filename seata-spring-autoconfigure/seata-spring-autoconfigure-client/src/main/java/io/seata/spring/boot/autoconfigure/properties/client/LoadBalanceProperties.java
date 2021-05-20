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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_LOAD_BALANCE;
import static io.seata.common.DefaultValues.VIRTUAL_NODES_DEFAULT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOAD_BALANCE_PREFIX_KEBAB_STYLE;

/**
 * @author ls9527
 */
@Component
@ConfigurationProperties(prefix = LOAD_BALANCE_PREFIX_KEBAB_STYLE)
public class LoadBalanceProperties {
    /**
     * the load balance
     */
    private String type = DEFAULT_LOAD_BALANCE;
    /**
     * the load balance virtual nodes
     */
    private int virtualNodes = VIRTUAL_NODES_DEFAULT;


    public String getType() {
        return type;
    }

    public LoadBalanceProperties setType(String type) {
        this.type = type;
        return this;
    }

    public int getVirtualNodes() {
        return virtualNodes;
    }

    public LoadBalanceProperties setVirtualNodes(int virtualNodes) {
        this.virtualNodes = virtualNodes;
        return this;
    }
}
