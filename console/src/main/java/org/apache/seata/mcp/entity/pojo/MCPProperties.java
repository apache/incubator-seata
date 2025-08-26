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

/**
 * MCP Server configuration parameters
 */
@Component
public class MCPProperties {

    public static final String SSE_TYPE = "sse";
    public static final String STREAMABLE_TYPE = "streamable";

    /**
     * The name of the server
     */
    private String serverName;
    /**
     * Server version
     */
    private String serverVersion = "1.0.0";
    /**
     * Whether to enable OAuth connection authentication
     */
    private boolean enableAuth = true;
    /**
     * Specifies whether to enable resource/prompt
     */
    private boolean resourceSupport = true;

    private boolean resourceTemplates = false;

    private boolean promptSupport = true;
    /**
     * Turn on logging
     */
    private McpSchema.LoggingLevel loggingLevel = McpSchema.LoggingLevel.INFO;
    /**
     * Maximum query interval, Millisecond, default: One day: 86400000L
     */
    private Long queryDuration = 86400000L;

    /**
     * Transmission types, Streamable and SSE are available
     */
    private String mcpType = SSE_TYPE;

    private StreamableProperties streamableProperties;

    private SseServerProperties sseServerProperties;

    public MCPProperties() {
    }

    public boolean isSseType() {
        return mcpType.equals(SSE_TYPE);
    }

    public static class StreamableProperties {
        private String mcpEndPoint = "/mcp";

        private Long heartBeatSecondDuration = 5L;

        public String getMcpEndPoint() {
            return mcpEndPoint;
        }

        public void setMcpEndPoint(String mcpEndPoint) {
            this.mcpEndPoint = mcpEndPoint;
        }

        public StreamableProperties() {
        }

        public StreamableProperties(String mcpEndPoint, Long heartBeatSecondDuration) {
            this.mcpEndPoint = mcpEndPoint;
            this.heartBeatSecondDuration = heartBeatSecondDuration;
        }

        public Long getHeartBeatSecondDuration() {
            return heartBeatSecondDuration;
        }

        public void setHeartBeatSecondDuration(Long heartBeatDuration) {
            this.heartBeatSecondDuration = heartBeatDuration;
        }

        @Override
        public String toString() {
            return "StreamableProperties{" +
                    "mcpEndPoint='" + mcpEndPoint + '\'' +
                    ", heartBeatSecondDuration=" + heartBeatSecondDuration +
                    '}';
        }
    }

    public static class SseServerProperties {
        /**
         * SSE endpoints
         */
        private String sseEndpoint = "/sse";
        /**
         * MESSAGE ENDPOINT
         */
        private String messageEndpoint = "/message";

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

        public SseServerProperties() {
        }

        public SseServerProperties(String sseEndpoint, String messageEndpoint) {
            this.sseEndpoint = sseEndpoint;
            this.messageEndpoint = messageEndpoint;
        }

        @Override
        public String toString() {
            return "SseServerProperties{" +
                    "sseEndpoint='" + sseEndpoint + '\'' +
                    ", messageEndpoint='" + messageEndpoint + '\'' +
                    '}';
        }
    }

    /**
     * Read MCP-related configuration parameters from the environment
     */
    @Lazy
    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        mcpType = env.getProperty("seata.mcp.mcpType","sse");
        if (mcpType.equals(STREAMABLE_TYPE)){
            String mcpEndPoint = env.getProperty("seata.mcp.streamable.mcpEndpoint","/mcp");
            Long heartBeatSecondDuration = Long.parseLong(env.getProperty("seata.mcp.streamable.heartBeatSecondDuration","30"));
            streamableProperties = new StreamableProperties(mcpEndPoint,heartBeatSecondDuration);
        }else {
            mcpType = SSE_TYPE;
            String sseEndpoint = env.getProperty("seata.mcp.sse.sseEndpoint", "/sse");
            String messageEndpoint = env.getProperty("seata.mcp.sse.messageEndpoint", "/message");
            sseServerProperties = new SseServerProperties(sseEndpoint,messageEndpoint);
        }
        serverName = env.getProperty("seata.mcp.serverName", "seata-mcp-server");
        serverVersion = env.getProperty("seata.mcp.serverVersion", "1.0.0");
        resourceSupport = Boolean.parseBoolean(env.getProperty("seata.mcp.resourceSupport", "true"));
        promptSupport = Boolean.parseBoolean(env.getProperty("seata.mcp.promptSupport", "true"));
        queryDuration = Long.parseLong(env.getProperty("seata.mcp.query.max_query_duration", "86400000"));
        enableAuth = Boolean.parseBoolean(env.getProperty("seata.mcp.auth.enabled", "true"));
    }

    @Override
    public String toString() {
        return "MCPProperties{" +
                "serverName='" + serverName + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                ", enableAuth=" + enableAuth +
                ", resourceSupport=" + resourceSupport +
                ", resourceTemplates=" + resourceTemplates +
                ", promptSupport=" + promptSupport +
                ", loggingLevel=" + loggingLevel +
                ", queryDuration=" + queryDuration +
                ", mcpType='" + mcpType + '\'' +
                ", streamableProperties=" + streamableProperties +
                ", sseServerProperties=" + sseServerProperties +
                ", env=" + env +
                '}';
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

    public boolean isEnableAuth() {
        return enableAuth;
    }

    public void setEnableAuth(boolean enableAuth) {
        this.enableAuth = enableAuth;
    }

    public String getMcpType() {
        return mcpType;
    }

    public Environment getEnv() {
        return env;
    }

    public StreamableProperties getStreamableProperties() {
        return streamableProperties;
    }

    public SseServerProperties getSseServerProperties() {
        return sseServerProperties;
    }

    public void setMcpType(String mcpType) {
        this.mcpType = mcpType;
    }

    public void setStreamableProperties(StreamableProperties streamableProperties) {
        this.streamableProperties = streamableProperties;
    }

    public void setSseServerProperties(SseServerProperties sseServerProperties) {
        this.sseServerProperties = sseServerProperties;
    }
}
