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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server side load balance factory.
 */
public final class ServerLoadBalanceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLoadBalanceFactory.class);

    public static final String SERVER_LB_PREFIX = "server.loadBalance.";
    public static final String SERVER_LB_ENABLED = SERVER_LB_PREFIX + "%s.enabled";
    public static final String SERVER_LB_TYPE = SERVER_LB_PREFIX + "%s.type";
    public static final String DEFAULT_SERVER_AT_LB_TYPE = "RoundRobinLoadBalance";
    public static final String XID_SERVER_LB_TYPE = "XID";
    private static final AtomicBoolean AT_DEFAULT_ENABLE_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean AT_DEFAULT_TYPE_LOGGED = new AtomicBoolean(false);

    private ServerLoadBalanceFactory() {}

    /**
     * Whether load balance is enabled for given branch type.
     *
     * <p>Only AT/TCC are supported currently.</p>
     */
    public static boolean isEnabled(BranchType branchType) {
        if (branchType == BranchType.AT) {
            if (ConfigurationFactory.getInstance().getConfig(buildEnabledKey(branchType)) == null
                    && AT_DEFAULT_ENABLE_LOGGED.compareAndSet(false, true)) {
                LOGGER.info(
                        "Use default server AT load balance switch as enabled, set {} explicitly to avoid implicit behavior",
                        buildEnabledKey(branchType));
            }
            return ConfigurationFactory.getInstance().getBoolean(buildEnabledKey(branchType), true);
        }
        if (branchType == BranchType.TCC) {
            return ConfigurationFactory.getInstance().getBoolean(buildEnabledKey(branchType), false);
        }
        return false;
    }

    /**
     * Get load balance strategy for branch type.
     */
    public static ServerLoadBalance getInstance(BranchType branchType) {
        if (!isEnabled(branchType)) {
            return null;
        }
        String configType = ConfigurationFactory.getInstance().getConfig(buildTypeKey(branchType));
        String type = configType;
        if (branchType == BranchType.AT && StringUtils.isBlank(type)) {
            if (AT_DEFAULT_TYPE_LOGGED.compareAndSet(false, true)) {
                LOGGER.info(
                        "Use default server AT load balance type [{}], set {} explicitly for server side",
                        DEFAULT_SERVER_AT_LB_TYPE,
                        buildTypeKey(branchType));
            }
            type = DEFAULT_SERVER_AT_LB_TYPE;
        }
        if (StringUtils.equalsIgnoreCase(type, XID_SERVER_LB_TYPE)) {
            if (branchType == BranchType.AT) {
                LOGGER.warn(
                        "Server side XID load balance is removed for AT branch type, fallback to [{}]",
                        DEFAULT_SERVER_AT_LB_TYPE);
                type = DEFAULT_SERVER_AT_LB_TYPE;
            } else {
                LOGGER.warn("Server side XID load balance is removed for {} branch type", branchType);
                return null;
            }
        }
        if (branchType == BranchType.TCC && StringUtils.isBlank(type)) {
            return null;
        }
        return EnhancedServiceLoader.load(ServerLoadBalance.class, type);
    }

    private static String buildEnabledKey(BranchType branchType) {
        return String.format(SERVER_LB_ENABLED, branchType.name().toLowerCase());
    }

    private static String buildTypeKey(BranchType branchType) {
        return String.format(SERVER_LB_TYPE, branchType.name().toLowerCase());
    }
}
