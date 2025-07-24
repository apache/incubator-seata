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
package org.apache.seata.mcp.entity.pojo;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * MCP Server configuration parameters
 */
@Component
public class MCPProperties {

    /**
     * The name of the server
     */
    private String serverName;
    /**
     * Server version
     */
    private String serverVersion = "1.0.0";
    /**
     * SSE endpoints
     */
    private String sseEndpoint = "/sse";
    /**
     * MESSAGE ENDPOINT
     */
    private String messageEndpoint = "/message";
    /**
     * Specifies whether to enable resource
     */
    private boolean resourceSupport = true;

    private boolean resourceTemplates = false;

    private boolean promptSupport = true;
    /**
     * Turn on logging
     */
    private McpSchema.LoggingLevel loggingLevel = McpSchema.LoggingLevel.INFO;
    /**
     * Whether to enable heartbeat monitoring, which is not enabled by default
     */
    private boolean heartbeat = false;
    /**
     * Maximum query interval
     */
    private Long queryDuration = 86400000L;

    /**
     * Read MCP-related configuration parameters from the environment
     */
    @Lazy
    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        serverName = env.getProperty("seata.mcp.serverName", "seata-mcp-server");
        serverVersion = env.getProperty("seata.mcp.serverVersion", "1.0.0");
        sseEndpoint = env.getProperty("seata.mcp.sseEndpoint", "/sse");
        messageEndpoint = env.getProperty("seata.mcp.messageEndpoint", "/message");
        resourceSupport = Boolean.parseBoolean(env.getProperty("seata.mcp.resourceSupport", "true"));
        promptSupport = Boolean.parseBoolean(env.getProperty("seata.mcp.promptSupport", "true"));
        heartbeat = Boolean.parseBoolean(env.getProperty("seata.mcp.heartbeat", "false"));
        queryDuration = Long.parseLong(env.getProperty("seata.mcp.query.max_query_duration", "86400000"));
    }

    @Override
    public String toString() {
        return "McpProperties{" +
                "serverName='" + serverName + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                ", sseEndpoint='" + sseEndpoint + '\'' +
                ", messageEndpoint='" + messageEndpoint + '\'' +
                ", resourceSupport=" + resourceSupport +
                ", resourceTemplates=" + resourceTemplates +
                ", promptSupport=" + promptSupport +
                ", loggingLevel=" + loggingLevel +
                ", heartbeat=" + heartbeat +
                ", queryDuration=" + queryDuration +
                ", env=" + env +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MCPProperties that = (MCPProperties) o;
        return resourceSupport == that.resourceSupport && resourceTemplates == that.resourceTemplates && heartbeat == that.heartbeat && Objects.equals(serverName, that.serverName) && Objects.equals(serverVersion, that.serverVersion) && Objects.equals(sseEndpoint, that.sseEndpoint) && Objects.equals(messageEndpoint, that.messageEndpoint) && loggingLevel == that.loggingLevel && Objects.equals(queryDuration, that.queryDuration) && Objects.equals(env, that.env);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                serverName,
                serverVersion,
                sseEndpoint,
                messageEndpoint,
                resourceSupport,
                resourceTemplates,
                loggingLevel,
                heartbeat);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
    }

    public String getMessageEndpoint() {
        return messageEndpoint;
    }

    public void setMessageEndpoint(String messageEndpoint) {
        this.messageEndpoint = messageEndpoint;
    }

    public boolean isResourceSupport() {
        return resourceSupport;
    }

    public void setResourceSupport(boolean resourceSupport) {
        this.resourceSupport = resourceSupport;
    }

    public boolean isResourceTemplates() {
        return resourceTemplates;
    }

    public void setResourceTemplates(boolean resourceTemplates) {
        this.resourceTemplates = resourceTemplates;
    }

    public McpSchema.LoggingLevel getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public Long getQueryDuration() {
        return queryDuration;
    }

    public void setQueryDuration(Long queryDuration) {
        this.queryDuration = queryDuration;
    }

    public boolean isPromptSupport() {
        return promptSupport;
    }

    public void setPromptSupport(boolean promptSupport) {
        this.promptSupport = promptSupport;
    }
}
