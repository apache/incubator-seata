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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.ROUTING_PREFIX;

@Component
@ConfigurationProperties(prefix = ROUTING_PREFIX)
public class RoutingProperties {

    private boolean enabled = false;
    private boolean debug = false;
    private String routers = "";
    private MetadataRouterConfig metadataRouter = new MetadataRouterConfig();

    // Dynamic numbered metadata router configurations (supports any number)
    private Map<String, MetadataRouterConfig> numberedMetadataRouters = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public RoutingProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public RoutingProperties setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public String getRouters() {
        return routers;
    }

    public RoutingProperties setRouters(String routers) {
        this.routers = routers;
        return this;
    }

    public MetadataRouterConfig getMetadataRouter() {
        return metadataRouter;
    }

    public RoutingProperties setMetadataRouter(MetadataRouterConfig metadataRouter) {
        this.metadataRouter = metadataRouter;
        return this;
    }

    public MetadataRouterConfig getNumberedMetadataRouter(String routerName) {
        return numberedMetadataRouters.get(routerName);
    }

    public void setNumberedMetadataRouter(String routerName, MetadataRouterConfig config) {
        numberedMetadataRouters.put(routerName, config);
    }

    public Map<String, MetadataRouterConfig> getNumberedMetadataRouters() {
        return numberedMetadataRouters;
    }

    public boolean hasNumberedMetadataRouter(String routerName) {
        return numberedMetadataRouters.containsKey(routerName);
    }
}
