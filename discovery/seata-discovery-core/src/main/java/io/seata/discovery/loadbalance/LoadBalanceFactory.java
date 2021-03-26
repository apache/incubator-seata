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
package io.seata.discovery.loadbalance;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;

import static io.seata.common.DefaultValues.DEFAULT_LOAD_BALANCE;

/**
 * The type Load balance factory.
 *
 * @author slievrly
 */
public class LoadBalanceFactory {

    private static final String CLIENT_PREFIX = "client.";
    /**
     * The constant LOAD_BALANCE_PREFIX.
     */
    public static final String LOAD_BALANCE_PREFIX = CLIENT_PREFIX + "loadBalance.";

    public static final String LOAD_BALANCE_TYPE = LOAD_BALANCE_PREFIX + "type";

    public static final String RANDOM_LOAD_BALANCE = DEFAULT_LOAD_BALANCE;

    public static final String ROUND_ROBIN_LOAD_BALANCE = "RoundRobinLoadBalance";

    public static final String CONSISTENT_HASH_LOAD_BALANCE = "ConsistentHashLoadBalance";

    public static final String LEAST_ACTIVE_LOAD_BALANCE = "LeastActiveLoadBalance";


    /**
     * Get instance.
     *
     * @return the instance
     */
    public static LoadBalance getInstance() {
        String config = ConfigurationFactory.getInstance().getConfig(LOAD_BALANCE_TYPE, DEFAULT_LOAD_BALANCE);
        return EnhancedServiceLoader.load(LoadBalance.class, config);
    }
}
