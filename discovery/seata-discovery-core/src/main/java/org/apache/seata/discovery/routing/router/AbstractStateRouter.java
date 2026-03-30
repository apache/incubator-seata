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
package org.apache.seata.discovery.routing.router;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.RouterSnapshotNode;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.StateRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides basic implementation for routers
 *
 * @param <T> service instance type
 */
public abstract class AbstractStateRouter<T> implements StateRouter<T> {
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private final String routerName;
    private final boolean fallbackEnabled;

    /**
     * Constructor
     *
     * @param routerName router name
     */
    protected AbstractStateRouter(String routerName) {
        this.routerName = routerName;
        this.fallbackEnabled =
                fileConfig.getBoolean(ConfigurationKeys.CLIENT_ROUTING_PREFIX + routerName + ".fallback", true);
    }

    @Override
    public List<T> route(List<T> servers, RoutingContext ctx, List<RouterSnapshotNode<T>> snapshots) {
        if (servers == null) {
            return null;
        }

        long startTime = System.currentTimeMillis();

        // Execute specific routing logic
        List<T> result = doRoute(servers, ctx);

        // Record details
        int inputSize = servers.size();
        int outputSize = result != null ? result.size() : 0;
        long executionTime = System.currentTimeMillis() - startTime;

        // Record snapshot (debug mode)
        if (snapshots != null) {
            RouterSnapshotNode<T> snapshot =
                    new RouterSnapshotNode<>(routerName, inputSize, outputSize, result, executionTime);
            snapshots.add(snapshot);
        }

        if (result == null || result.isEmpty()) {
            if (fallbackEnabled) {
                // Fallback mode: return original list when result is empty
                return servers;
            } else {
                // Strict mode: return empty list
                return new ArrayList<>();
            }
        }

        // Return result directly, no longer chain to next router
        return result;
    }

    /**
     * Execute specific routing logic
     *
     * @param servers service instances list
     * @param ctx     routing context
     * @return routed service instances list
     */
    protected abstract List<T> doRoute(List<T> servers, RoutingContext ctx);
}
