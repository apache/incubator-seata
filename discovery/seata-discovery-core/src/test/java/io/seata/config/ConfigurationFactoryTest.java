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
package io.seata.config;

import io.seata.discovery.loadbalance.ConsistentHashLoadBalance;
import io.seata.discovery.loadbalance.LoadBalanceFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Geng Zhang
 */
class ConfigurationFactoryTest {

    @Test
    void getInstance() {
        Configuration configuration = ConfigurationFactory.getInstance();
        // check singleton
        Assertions.assertEquals(configuration.getClass().getName(), ConfigurationFactory.getInstance().getClass().getName());
    }


    @Test
    void getLoadBalance() {
        Configuration configuration = ConfigurationFactory.getInstance();
        String loadBalanceType = configuration.getConfig(LoadBalanceFactory.LOAD_BALANCE_TYPE);
        int visualNode = configuration.getInt(ConsistentHashLoadBalance.LOAD_BALANCE_CONSISTENT_HASH_VISUAL_NODES);
        Assertions.assertEquals("RandomLoadBalance", loadBalanceType);
        Assertions.assertEquals(10,visualNode);
    }

}