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
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.expression.ConditionMatcher;
import org.apache.seata.discovery.routing.expression.ExpressionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Metadata router
 *
 * Supports three modes:
 * 1. Single expression: version >= 2.0
 * 2. OR logic expression: (version >= 2.0) || (env == dev) || (region == cn-bj)
 * 3. AND logic expression: (version >= 2.0) && (env == prod) && (region == cn-bj)
 *
 * Note: Mixed AND/OR logic is not supported, use multiple MetadataRouters for complex logic
 */
public class MetadataRouter extends AbstractStateRouter<ServiceInstance> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataRouter.class);
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private volatile String expression;

    /**
     * Constructor with router name
     * @param routerName router name (e.g., metadata-router-1, metadata-router-2)
     */
    public MetadataRouter(String routerName) {
        super(routerName);
        this.expression = fileConfig.getConfig(ConfigurationKeys.CLIENT_ROUTING_PREFIX + routerName + ".expression");
    }

    @Override
    protected List<ServiceInstance> doRoute(List<ServiceInstance> servers, RoutingContext ctx) {
        // Create a local copy to ensure consistency during method execution
        String currentExpression = this.expression;

        // If expression is empty or contains only spaces, return original server list directly
        if (currentExpression == null || currentExpression.trim().isEmpty()) {
            LOGGER.info("The expression is empty, so return original server list directly.");
            return servers;
        }

        // Parse expression
        List<ConditionMatcher> matchers;
        try {
            matchers = ExpressionParser.parse(currentExpression);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to parse expression: {}, return original server list.", currentExpression);
            return servers;
        }

        // Check if it's an OR expression
        if (ExpressionParser.isOrExpression(currentExpression)) {
            // OR logic: any condition satisfied is sufficient
            return servers.stream()
                    .filter(server -> matchers.stream().anyMatch(m -> m.match(server, ctx)))
                    .collect(Collectors.toList());
        } else {
            // Single expression or AND logic: all conditions must be satisfied
            return servers.stream()
                    .filter(server -> matchers.stream().allMatch(m -> m.match(server, ctx)))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Set expression
     * @param expression expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Get expression
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    @Override
    public String buildSnapshot() {
        return String.format("MetadataRouter: expression=%s", expression);
    }
}
