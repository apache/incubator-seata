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
package org.apache.seata.mcp.core.props;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MCPProperties {

    private final Environment env;

    private Long queryDuration = TimeUnit.DAYS.toMillis(1);

    private final McpServerProperties mcpServerProperties;

    private final McpServerSseProperties mcpServerSseProperties;

    private final McpServerStreamableHttpProperties mcpServerStreamableHttpProperties;

    private final List<String> endpoints = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(MCPProperties.class);

    @Autowired
    public MCPProperties(
            @Nullable McpServerProperties mcpServerProperties,
            Environment env,
            @Nullable McpServerSseProperties serverSseProperties,
            @Nullable McpServerStreamableHttpProperties serverStreamableHttpProperties) {
        this.mcpServerProperties = mcpServerProperties;
        this.env = env;
        this.mcpServerSseProperties = serverSseProperties;
        this.mcpServerStreamableHttpProperties = serverStreamableHttpProperties;
    }

    public List<String> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    public Long getQueryDuration() {
        return queryDuration;
    }

    @PostConstruct
    public void init() {
        String maxQueryDurationStr = env.getProperty("seata.mcp.query.max-query-duration", "86400000");
        try {
            queryDuration = Long.parseLong(maxQueryDurationStr);
        } catch (NumberFormatException ex) {
            queryDuration = TimeUnit.DAYS.toMillis(1);
        }

        if (mcpServerProperties != null) {
            McpServerProperties.ServerProtocol protocol = mcpServerProperties.getProtocol();
            if (protocol == McpServerProperties.ServerProtocol.SSE && mcpServerSseProperties != null) {
                endpoints.add(mcpServerSseProperties.getSseEndpoint());
                endpoints.add(mcpServerSseProperties.getSseMessageEndpoint());
            } else if (protocol == McpServerProperties.ServerProtocol.STREAMABLE
                    && mcpServerStreamableHttpProperties != null) {
                endpoints.add(mcpServerStreamableHttpProperties.getMcpEndpoint());
            } else {
                throw new IllegalStateException(
                        "MCP server properties not properly configured or unsupported protocol");
            }
        } else {
            logger.warn("MCP server properties not properly configured");
        }
    }
}
