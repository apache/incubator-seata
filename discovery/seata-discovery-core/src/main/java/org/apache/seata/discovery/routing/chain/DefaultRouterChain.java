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
package org.apache.seata.discovery.routing.chain;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.RouterSnapshotNode;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.StateRouter;
import org.apache.seata.discovery.routing.router.MetadataRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses chain of responsibility pattern to encapsulate multiple routers
 */
public class DefaultRouterChain implements RouterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRouterChain.class);
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private final List<StateRouter<ServiceInstance>> routers = new ArrayList<>();
    private final boolean debugEnabled;

    /**
     * Default constructor
     */
    public DefaultRouterChain() {
        this.debugEnabled = fileConfig.getBoolean(ConfigurationKeys.CLIENT_ROUTING_DEBUG, false);
        loadRouters();
    }

    /**
     * Execute routing filter
     *
     * @param servers service instances list
     * @param ctx     routing context
     * @return filtered service instances list
     */
    @Override
    public List<ServiceInstance> filterAll(List<ServiceInstance> servers, RoutingContext ctx) {
        return route(servers, ctx);
    }

    /**
     * Execute routing filter (internal method)
     *
     * @param servers service instances list
     * @param ctx     routing context
     * @return filtered service instances list
     */
    private List<ServiceInstance> route(List<ServiceInstance> servers, RoutingContext ctx) {
        List<ServiceInstance> result = servers;
        List<RouterSnapshotNode<ServiceInstance>> snapshots = debugEnabled ? new ArrayList<>() : null;

        for (StateRouter<ServiceInstance> router : routers) {
            result = router.route(result, ctx, snapshots);
        }

        // If debug mode is enabled, print complete snapshot information
        if (debugEnabled && !snapshots.isEmpty()) {
            buildRouterChainSnapshot(snapshots, servers.size(), result.size());
        }

        return result;
    }

    /**
     * Print router chain debug information
     *
     * @param snapshots   snapshot list
     * @param initialSize initial server count
     * @param finalSize   final server count
     */
    private void buildRouterChainSnapshot(
            List<RouterSnapshotNode<ServiceInstance>> snapshots, int initialSize, int finalSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Router Chain Debug ===\n");
        sb.append(String.format("Initial servers: %d, Final servers: %d\n", initialSize, finalSize));
        sb.append("Router execution details:\n");

        for (int i = 0; i < snapshots.size(); i++) {
            RouterSnapshotNode<ServiceInstance> snapshot = snapshots.get(i);
            sb.append(String.format(
                    "  [%d] %s: %d -> %d servers\n",
                    i + 1, snapshot.getRouterName(), snapshot.getInputSize(), snapshot.getOutputSize()));

            // If output count is small, show selected servers
            if (snapshot.getOutputSize() <= 5 && snapshot.getOutputSize() > 0) {
                sb.append("    Selected: ")
                        .append(snapshot.getSelectedServers())
                        .append("\n");
            }
        }

        sb.append("=== End Debug ===\n");
        LOGGER.info(sb.toString());
    }

    private void loadRouters() {
        // Get router list from configuration 'client.routing.routers'
        String routersConfig = fileConfig.getConfig(ConfigurationKeys.CLIENT_ROUTING_ROUTERS);

        if (routersConfig == null || routersConfig.trim().isEmpty()) {
            LOGGER.warn("No routers configured in 'client.routing.routers'. Routing will be disabled.");
            return;
        }

        // Parse router names from configuration
        String[] routerNames = routersConfig.split(",");

        for (String routerName : routerNames) {
            routerName = routerName.trim();
            if (routerName.isEmpty()) {
                continue;
            }

            if (isRouterEnabled(routerName)) {
                StateRouter<ServiceInstance> router = createRouter(routerName);
                if (router != null) {
                    routers.add(router);
                    LOGGER.debug("Successfully loaded router: {}", routerName);
                } else {
                    LOGGER.warn("Failed to create router: {}", routerName);
                }
            } else {
                LOGGER.debug("Router {} not loaded: enabled={}", routerName, isRouterEnabled(routerName));
            }
        }
    }

    /**
     * Create router instance based on router name
     */
    private StateRouter<ServiceInstance> createRouter(String routerName) {
        try {
            if (routerName.startsWith("spi-")) {
                // SPI router (spi-custom, spi-region, etc.)
                StateRouter<ServiceInstance> spiRouter = EnhancedServiceLoader.load(StateRouter.class, routerName);
                if (spiRouter != null) {
                    LOGGER.info("Successfully loaded SPI router: {}", routerName);
                    return spiRouter;
                } else {
                    LOGGER.error("Failed to load SPI router: {}", routerName);
                    return null;
                }
            }

            // Check if metadata router has expression configuration
            if (!hasRouterConfiguration(routerName)) {
                LOGGER.warn("Router {} is configured but has no configuration, skip loading.", routerName);
                return null;
            }

            if (routerName.startsWith("metadata-router-")) {
                // Numbered metadata router (metadata-router-1, metadata-router-2, etc.)
                return new MetadataRouter(routerName);
            }

            LOGGER.warn("Unknown router type: {}", routerName);
            return null;
        } catch (Exception e) {
            LOGGER.error("Error creating router: {}", routerName, e);
            return null;
        }
    }

    /**
     * Check if router has any configuration
     */
    private boolean hasRouterConfiguration(String routerName) {
        String enabledKey = ConfigurationKeys.CLIENT_ROUTING_PREFIX + routerName + ".enabled";
        String expressionKey = ConfigurationKeys.CLIENT_ROUTING_PREFIX + routerName + ".expression";

        // Router is considered configured if either enabled or expression is set
        return fileConfig.getConfig(enabledKey) != null || fileConfig.getConfig(expressionKey) != null;
    }

    /**
     * Check if router is enabled
     */
    private boolean isRouterEnabled(String routerName) {
        String configKey = ConfigurationKeys.CLIENT_ROUTING_PREFIX + routerName + ".enabled";
        return fileConfig.getBoolean(configKey, true);
    }
}
