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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Server side load balance factory.
 * Only AT and TCC branch types support server-side load balancing.
 * XA and SAGA retain their existing original-instance preference and recovery behavior.
 * TCC load balancing must only be enabled when confirm/cancel can run on another instance.
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

    private static final ConcurrentMap<BranchType, ResolvedStrategy> STRATEGIES = new ConcurrentHashMap<>();

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

        String typeKey = branchType == BranchType.AT ? SERVER_LB_AT_TYPE : SERVER_LB_TCC_TYPE;
        // ConfigurationFactory already caches values and refreshes them through configuration listeners.
        String configuredType = ConfigurationFactory.getInstance().getConfig(typeKey);
        String type = StringUtils.isBlank(configuredType) ? null : configuredType.trim();
        ResolvedStrategy cached = STRATEGIES.get(branchType);
        if (cached != null && Objects.equals(cached.type, type)) {
            return cached.strategy;
        }
        return STRATEGIES.compute(branchType, (key, current) -> {
                    if (current != null && Objects.equals(current.type, type)) {
                        return current;
                    }
                    return new ResolvedStrategy(type, loadStrategy(type, branchType));
                })
                .strategy;
    }

    private static ServerLoadBalance loadStrategy(String type, BranchType branchType) {
        if (type == null) {
            return null;
        }
        try {
            // LoadLevel defaults to Scope.SINGLETON: switching back also preserves a strategy's state.
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

    private static final class ResolvedStrategy {
        private final String type;
        private final ServerLoadBalance strategy;

        private ResolvedStrategy(String type, ServerLoadBalance strategy) {
            this.type = type;
            this.strategy = strategy;
        }
    }
}
