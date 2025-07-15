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
package org.apache.console.mcp.config;

import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.config.MCPConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MCPConfigurationTest {

    @Mock
    private Environment environment;

    @InjectMocks
    private MCPConfiguration mcpConfiguration;

    @BeforeEach
    public void setUp() {
        // Clears values that may have been injected previously
        ReflectionTestUtils.setField(mcpConfiguration, "serverName", null);
        ReflectionTestUtils.setField(mcpConfiguration, "serverVersion", "1.0.0");
        ReflectionTestUtils.setField(mcpConfiguration, "sseEndpoint", "/sse");
        ReflectionTestUtils.setField(mcpConfiguration, "messageEndpoint", "/message");
        ReflectionTestUtils.setField(mcpConfiguration, "resourceSupport", true);
        ReflectionTestUtils.setField(mcpConfiguration, "resourceTemplates", false);
        ReflectionTestUtils.setField(mcpConfiguration, "loggingLevel", McpSchema.LoggingLevel.INFO);
        ReflectionTestUtils.setField(mcpConfiguration, "heartbeat", false);
        ReflectionTestUtils.setField(mcpConfiguration, "queryDuration", 86400000L);
    }

    @Test
    public void testDefaultValues() {
        // Sets the simulation behavior for the default values
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server")).thenReturn("seata-mcp-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("1.0.0");
        when(environment.getProperty("seata.mcp.sseEndpoint", "/sse")).thenReturn("/sse");
        when(environment.getProperty("seata.mcp.messageEndpoint", "/message")).thenReturn("/message");
        when(environment.getProperty("seata.mcp.heartbeat", "false")).thenReturn("false");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "86400000")).thenReturn("86400000");

        mcpConfiguration.init();

        assertEquals("seata-mcp-server", mcpConfiguration.getServerName());
        assertEquals("1.0.0", mcpConfiguration.getServerVersion());
        assertEquals("/sse", mcpConfiguration.getSseEndpoint());
        assertEquals("/message", mcpConfiguration.getMessageEndpoint());
        assertTrue(mcpConfiguration.isResourceSupport());
        assertFalse(mcpConfiguration.isResourceTemplates());
        assertEquals(McpSchema.LoggingLevel.INFO, mcpConfiguration.getLoggingLevel());
        assertFalse(mcpConfiguration.isHeartbeat());
        assertEquals(86400000L, mcpConfiguration.getQueryDuration());
    }
    
    @Test
    public void testCustomValues() {
        // Set the simulation behavior for custom values
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server")).thenReturn("custom-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("2.0.0");
        when(environment.getProperty("seata.mcp.sseEndpoint", "/sse")).thenReturn("/custom-sse");
        when(environment.getProperty("seata.mcp.messageEndpoint", "/message")).thenReturn("/custom-message");
        when(environment.getProperty("seata.mcp.heartbeat", "false")).thenReturn("true");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "86400000")).thenReturn("43200000");

        mcpConfiguration.init();

        assertEquals("custom-server", mcpConfiguration.getServerName());
        assertEquals("2.0.0", mcpConfiguration.getServerVersion());
        assertEquals("/custom-sse", mcpConfiguration.getSseEndpoint());
        assertEquals("/custom-message", mcpConfiguration.getMessageEndpoint());
        assertTrue(mcpConfiguration.isHeartbeat());
        assertEquals(43200000L, mcpConfiguration.getQueryDuration());
    }
    
    @Test
    public void testSetterMethods() {
        // Test all setter methods
        mcpConfiguration.setServerName("test-server");
        mcpConfiguration.setServerVersion("3.0.0");
        mcpConfiguration.setSseEndpoint("/test-sse");
        mcpConfiguration.setMessageEndpoint("/test-message");
        mcpConfiguration.setResourceSupport(false);
        mcpConfiguration.setResourceTemplates(true);
        mcpConfiguration.setLoggingLevel(McpSchema.LoggingLevel.DEBUG);
        mcpConfiguration.setHeartbeat(true);
        mcpConfiguration.setQueryDuration(60000L);
        
        // Verify the value of the setting
        assertEquals("test-server", mcpConfiguration.getServerName());
        assertEquals("3.0.0", mcpConfiguration.getServerVersion());
        assertEquals("/test-sse", mcpConfiguration.getSseEndpoint());
        assertEquals("/test-message", mcpConfiguration.getMessageEndpoint());
        assertFalse(mcpConfiguration.isResourceSupport());
        assertTrue(mcpConfiguration.isResourceTemplates());
        assertEquals(McpSchema.LoggingLevel.DEBUG, mcpConfiguration.getLoggingLevel());
        assertTrue(mcpConfiguration.isHeartbeat());
        assertEquals(60000L, mcpConfiguration.getQueryDuration());
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // Create two instances of the same configuration
        MCPConfiguration config1 = new MCPConfiguration();
        MCPConfiguration config2 = new MCPConfiguration();
        
        config1.setServerName("test-server");
        config1.setServerVersion("1.0.0");
        
        config2.setServerName("test-server");
        config2.setServerVersion("1.0.0");
        
        // Verify the equals and hashCode methods
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        
        // After modifying a property, it should not be equal
        config2.setServerName("different-server");
        assertNotEquals(config1, config2);
    }
    
    @Test
    public void testToString() {

        mcpConfiguration.setServerName("test-server");
        mcpConfiguration.setServerVersion("1.0.0");
        
        // Verify that the toString method contains key information
        String toString = mcpConfiguration.toString();
        assertTrue(toString.contains("test-server"));
        assertTrue(toString.contains("1.0.0"));
    }
}
