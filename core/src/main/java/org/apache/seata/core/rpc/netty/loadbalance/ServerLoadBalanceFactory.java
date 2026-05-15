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
package org.apache.seata.core.rpc.netty.loadbalance;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side load balance factory.
 * Only AT and TCC branch types support server-side load balancing.
 * XA and SAGA are explicitly excluded because:
 *  XA: second-phase operations are bound to the local database connection of the original RM
 *  SAGA: state machine execution context is held in memory with no distributed lock protection
 */
public final class ServerLoadBalanceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLoadBalanceFactory.class);

    public static final String SERVER_LB_PREFIX = "server.loadBalance.";

    /**
     * Configuration key for AT mode load balance type.
     */
    public static final String SERVER_LB_AT_TYPE = SERVER_LB_PREFIX + "at.type";

    /**
     * Configuration key for TCC mode load balance type.
     */
    public static final String SERVER_LB_TCC_TYPE = SERVER_LB_PREFIX + "tcc.type";

    private ServerLoadBalanceFactory() {}

    /**
     * Get load balance strategy for the given branch type.
     *
     * Only AT and TCC are supported. For XA/SAGA and other types, returns null
     * to indicate that the original channel selection logic should be used.
     *
     * If the type configuration is not set or is blank, returns null to indicate
     * that the original channel selection logic should be used.
     *
     * @param branchType the branch type
     * @return the load balance instance, or null if load balancing is not configured/applicable
     */
    public static ServerLoadBalance getInstance(BranchType branchType) {
        if (branchType != BranchType.AT && branchType != BranchType.TCC) {
            return null;
        }

        String typeKey = buildTypeKey(branchType);
        String type = ConfigurationFactory.getInstance().getConfig(typeKey);

        if (StringUtils.isBlank(type)) {
            return null;
        }

        try {
            return EnhancedServiceLoader.load(ServerLoadBalance.class, type);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to load server load balance [{}] for branch type [{}], fallback to original logic",
                    type,
                    branchType,
                    e);
            return null;
        }
    }

    private static String buildTypeKey(BranchType branchType) {
        return SERVER_LB_PREFIX + branchType.name().toLowerCase() + ".type";
    }
}
