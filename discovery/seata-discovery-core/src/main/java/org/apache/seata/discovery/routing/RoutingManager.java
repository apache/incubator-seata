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
package org.apache.seata.discovery.routing;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.chain.DefaultRouterChain;
import org.apache.seata.discovery.routing.chain.RouterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Responsible for routing filtering between service discovery and load balancing
 */
public class RoutingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingManager.class);
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private static volatile RoutingManager INSTANCE;

    private final RouterChain routerChain;

    /**
     * Constructor
     */
    private RoutingManager() {
        this.routerChain = new DefaultRouterChain();
    }

    /**
     * Get singleton instance
     * @return routing manager instance
     */
    public static RoutingManager getInstance() {
        if (INSTANCE == null) {
            synchronized (RoutingManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RoutingManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Execute routing filter
     * @param servers original service instances list
     * @return filtered service instances list
     */
    public List<ServiceInstance> filter(List<ServiceInstance> servers) {
        // Check if routing is enabled
        if (!fileConfig.getBoolean(ConfigurationKeys.CLIENT_ROUTING_ENABLED, false)) {
            return servers;
        }

        try {
            // Create routing context
            RoutingContext ctx = createRoutingContext();

            // Execute routing filter
            return routerChain.filterAll(servers, ctx);
        } catch (Exception e) {
            LOGGER.warn("Routing filter failed: {}, returning original servers", e.getMessage());
            return servers;
        }
    }

    /**
     * Create routing context
     * @return routing context
     */
    private RoutingContext createRoutingContext() {
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("timestamp", System.currentTimeMillis());

        return ctx;
    }
}
