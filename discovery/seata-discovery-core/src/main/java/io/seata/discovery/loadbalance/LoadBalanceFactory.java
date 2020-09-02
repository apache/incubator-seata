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

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_REGISTRY;
import static io.seata.common.DefaultValues.DEFAULT_LOAD_BALANCE;

/**
 * The type Load balance factory.
 *
 * @author slievrly
 */
public class LoadBalanceFactory {

    /**
     * The constant LOAD_BALANCE.
     */
    private static final String LOAD_BALANCE = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + "loadBalance";

    /**
     * Get instance.
     *
     * @return the instance
     */
    public static LoadBalance getInstance() {
        return EnhancedServiceLoader.load(LoadBalance.class, ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(LOAD_BALANCE, DEFAULT_LOAD_BALANCE));
    }
}
