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
package org.apache.seata.mcp.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.controller.ControlMcpController;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = McpServerManagerTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc(addFilters = false)
public class McpServerManagerTest {

    @SpringBootConfiguration
    static class TestApplication {

        @Bean
        public MCPProperties mcpConfiguration() {
            MCPProperties config = new MCPProperties();
            config.setServerName("test-server");
            config.setServerVersion("1.0.0");
            config.setSseEndpoint("/sse");
            config.setMessageEndpoint("/message");
            config.setLoggingLevel(McpSchema.LoggingLevel.INFO);
            return config;
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public McpServerManager mcpServerManager(MCPProperties config, ObjectMapper objectMapper) {
            McpServerManager manager = new McpServerManager(config, objectMapper);
            manager.start();
            return manager;
        }

        @Bean
        public ControlMcpController controlMcpController(McpServerManager mcpServerManager) {
            return new ControlMcpController();
        }

        @Bean
        public RouterFunction<ServerResponse> mcpRouter(McpServerManager manager) {
            return manager.getRouterFunction();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private McpServerManager serverManager;

    @Test
    public void testServerLifecycle() {
        assertTrue(serverManager.isRunning());

        serverManager.stop();
        assertFalse(serverManager.isRunning());

        serverManager.start();
        assertTrue(serverManager.isRunning());
    }

    @Test
    public void testMcpServerManagerDirectly() {
        assertTrue(serverManager.isRunning());

        serverManager.pause();
        assertFalse(serverManager.isRunning());

        serverManager.resume();
        assertTrue(serverManager.isRunning());
    }

    @Test
    public void testRouterFunction() throws Exception {

        System.out.println("Testing router function...");

        // Test whether the route is normal when startup
        assertTrue(serverManager.isRunning());
        mockMvc.perform(get("/sse").accept(MediaType.TEXT_EVENT_STREAM))
                .andDo(result -> System.out.println(
                        "SSE response status: " + result.getResponse().getStatus()))
                .andExpect(status().isOk());

        // Test whether the route is unreachable when paused
        serverManager.pause();
        assertFalse(serverManager.isRunning());
        mockMvc.perform(get("/sse").accept(MediaType.TEXT_EVENT_STREAM))
                .andDo(result -> System.out.println(
                        "SSE response status: " + result.getResponse().getStatus()))
                .andExpect(status().isServiceUnavailable());
    }
}
