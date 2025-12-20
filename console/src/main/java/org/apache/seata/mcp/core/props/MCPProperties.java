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

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MCPProperties {

    private final Environment env;

    public static final String SSE_TYPE = "sse";

    public static final String STREAMABLE_TYPE = "streamable";

    private boolean enableAuth = true;

    private Long queryDuration = TimeUnit.DAYS.toMillis(1);

    private String mcpType = SSE_TYPE;

    private StreamableProperties streamableProperties;

    private SseServerProperties sseServerProperties;

    public MCPProperties(Environment env) {
        this.env = env;
    }

    public boolean isSseType() {
        return mcpType.equals(SSE_TYPE);
    }

    public List<String> getEndpoints() {
        List<String> result = new ArrayList<>();
        if (isSseType()) {
            result.add(sseServerProperties.sseEndpoint);
            result.add(sseServerProperties.messageEndpoint);
        } else {
            result.add(streamableProperties.mcpEndpoint);
        }
        return result;
    }

    public Long getQueryDuration() {
        return queryDuration;
    }

    public static class StreamableProperties {
        private final String mcpEndpoint;

        public StreamableProperties(String mcpEndPoint) {
            this.mcpEndpoint = mcpEndPoint;
        }
    }

    public static class SseServerProperties {

        private final String sseEndpoint;

        private final String messageEndpoint;

        public SseServerProperties(String sseEndpoint, String messageEndpoint) {
            this.sseEndpoint = sseEndpoint;
            this.messageEndpoint = messageEndpoint;
        }
    }

    @PostConstruct
    public void init() {
        mcpType = env.getProperty("spring.ai.mcp.server.protocol", "sse");
        if (mcpType.equals(STREAMABLE_TYPE)) {
            String mcpEndPoint = env.getProperty("spring.ai.mcp.server.streamable-http.mcp-endpoint", "/mcp");
            streamableProperties = new StreamableProperties(mcpEndPoint);
        } else {
            mcpType = SSE_TYPE;
            String sseEndpoint = env.getProperty("spring.ai.mcp.server.sse-endpoint", "/sse");
            String messageEndpoint = env.getProperty("spring.ai.mcp.server.sse-message-endpoint", "/mcp/message");
            sseServerProperties = new SseServerProperties(sseEndpoint, messageEndpoint);
        }
        String maxQueryDurationStr = env.getProperty("seata.mcp.query.max-query-duration", "86400000");
        try {
            queryDuration = Long.parseLong(maxQueryDurationStr);
        } catch (NumberFormatException ex) {
            queryDuration = TimeUnit.DAYS.toMillis(1);
        }
        enableAuth = Boolean.parseBoolean(env.getProperty("seata.mcp.auth.enabled", "true"));
        checkAfterPropertiesSet();
    }

    private void checkAfterPropertiesSet() {
        if (isSseType() && sseServerProperties == null)
            throw new IllegalStateException("SSE properties not initialized");
        if (!isSseType() && streamableProperties == null)
            throw new IllegalStateException("Streamable properties not initialized");
    }

    public boolean isEnableAuth() {
        return enableAuth;
    }
}
