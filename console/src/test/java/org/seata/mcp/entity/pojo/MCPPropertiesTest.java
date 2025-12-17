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
package org.seata.mcp.entity.pojo;

import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MCPProperties
 */
@ExtendWith(MockitoExtension.class)
class MCPPropertiesTest {

    @Mock
    private Environment environment;

    private MCPProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        properties = new MCPProperties();
        Field envField = MCPProperties.class.getDeclaredField("env");
        envField.setAccessible(true);
        envField.set(properties, environment);
    }

    @Test
    void testDefaultConstructor() {
        MCPProperties props = new MCPProperties();
        assertNotNull(props);
        assertEquals("1.0.0", props.getServerVersion());
        assertTrue(props.isEnableAuth());
        assertEquals(ProtocolDefinition.LoggingLevel.INFO, props.getLoggingLevel());
        assertEquals(86400000L, props.getQueryDuration());
    }

    @Test
    void testGettersAndSetters() {
        properties.setServerName("test-server");
        properties.setServerVersion("2.0.0");
        properties.setEnableAuth(false);
        properties.setLoggingLevel(ProtocolDefinition.LoggingLevel.DEBUG);
        properties.setQueryDuration(100000L);
        properties.setMcpType(MCPProperties.STREAMABLE_TYPE);

        assertEquals("test-server", properties.getServerName());
        assertEquals("2.0.0", properties.getServerVersion());
        assertFalse(properties.isEnableAuth());
        assertEquals(ProtocolDefinition.LoggingLevel.DEBUG, properties.getLoggingLevel());
        assertEquals(100000L, properties.getQueryDuration());
        assertEquals(MCPProperties.STREAMABLE_TYPE, properties.getMcpType());
    }

    @Test
    void testIsSseType() {
        properties.setMcpType(MCPProperties.SSE_TYPE);
        assertTrue(properties.isSseType());
        properties.setMcpType(MCPProperties.STREAMABLE_TYPE);
        assertFalse(properties.isSseType());
    }

    @Test
    void testGetEndpointsForSse() {
        properties.setMcpType(MCPProperties.SSE_TYPE);
        MCPProperties.SseServerProperties sseProps = new MCPProperties.SseServerProperties("/sse", "/message");
        properties.setSseServerProperties(sseProps);

        List<String> endpoints = properties.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(2, endpoints.size());
        assertTrue(endpoints.contains("/sse"));
        assertTrue(endpoints.contains("/message"));
    }

    @Test
    void testGetEndpointsForStreamable() {
        properties.setMcpType(MCPProperties.STREAMABLE_TYPE);
        MCPProperties.StreamableProperties streamableProps = new MCPProperties.StreamableProperties("/mcp", 30L);
        properties.setStreamableProperties(streamableProps);

        List<String> endpoints = properties.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains("/mcp"));
    }

    @Test
    void testStreamableProperties() {
        MCPProperties.StreamableProperties props = new MCPProperties.StreamableProperties();
        props.setMcpEndPoint("/test");
        props.setHeartBeatSecondDuration(60L);

        assertEquals("/test", props.getMcpEndPoint());
        assertEquals(60L, props.getHeartBeatSecondDuration());
    }

    @Test
    void testStreamablePropertiesConstructor() {
        MCPProperties.StreamableProperties props = new MCPProperties.StreamableProperties("/mcp", 30L);
        assertEquals("/mcp", props.getMcpEndPoint());
        assertEquals(30L, props.getHeartBeatSecondDuration());
    }

    @Test
    void testSseServerProperties() {
        MCPProperties.SseServerProperties props = new MCPProperties.SseServerProperties();
        props.setSseEndpoint("/sse");
        props.setMessageEndpoint("/message");

        assertEquals("/sse", props.getSseEndpoint());
        assertEquals("/message", props.getMessageEndpoint());
    }

    @Test
    void testSseServerPropertiesConstructor() {
        MCPProperties.SseServerProperties props = new MCPProperties.SseServerProperties("/sse", "/message");
        assertEquals("/sse", props.getSseEndpoint());
        assertEquals("/message", props.getMessageEndpoint());
    }

    @Test
    void testInitWithStreamableType() throws Exception {
        when(environment.getProperty("seata.mcp.mcpType", "sse")).thenReturn("streamable");
        when(environment.getProperty("seata.mcp.streamable.mcpEndpoint", "/mcp"))
                .thenReturn("/stream");
        when(environment.getProperty("seata.mcp.streamable.heartBeatSecondDuration", "30"))
                .thenReturn("60");
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server"))
                .thenReturn("test-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("2.0.0");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "604800000"))
                .thenReturn("1000000");
        when(environment.getProperty("seata.mcp.auth.enabled", "true")).thenReturn("false");

        properties.init();

        assertEquals("streamable", properties.getMcpType());
        assertNotNull(properties.getStreamableProperties());
        assertEquals("/stream", properties.getStreamableProperties().getMcpEndPoint());
        assertEquals(60L, properties.getStreamableProperties().getHeartBeatSecondDuration());
        assertEquals("test-server", properties.getServerName());
        assertEquals("2.0.0", properties.getServerVersion());
        assertEquals(1000000L, properties.getQueryDuration());
        assertFalse(properties.isEnableAuth());
    }

    @Test
    void testInitWithSseType() throws Exception {
        when(environment.getProperty("seata.mcp.mcpType", "sse")).thenReturn("sse");
        when(environment.getProperty("seata.mcp.sse.sseEndpoint", "/sse")).thenReturn("/custom-sse");
        when(environment.getProperty("seata.mcp.sse.messageEndpoint", "/message"))
                .thenReturn("/custom-message");
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server"))
                .thenReturn("test-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("2.0.0");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "604800000"))
                .thenReturn("1000000");
        when(environment.getProperty("seata.mcp.auth.enabled", "true")).thenReturn("false");

        properties.init();

        assertEquals("sse", properties.getMcpType());
        assertNotNull(properties.getSseServerProperties());
        assertEquals("/custom-sse", properties.getSseServerProperties().getSseEndpoint());
        assertEquals("/custom-message", properties.getSseServerProperties().getMessageEndpoint());
        assertEquals("test-server", properties.getServerName());
        assertEquals("2.0.0", properties.getServerVersion());
        assertEquals(1000000L, properties.getQueryDuration());
        assertFalse(properties.isEnableAuth());
    }

    @Test
    void testInitWithInvalidType() throws Exception {
        when(environment.getProperty("seata.mcp.mcpType", "sse")).thenReturn("invalid");
        when(environment.getProperty("seata.mcp.sse.sseEndpoint", "/sse")).thenReturn("/sse");
        when(environment.getProperty("seata.mcp.sse.messageEndpoint", "/message"))
                .thenReturn("/message");
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server"))
                .thenReturn("test-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("1.0.0");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "604800000"))
                .thenReturn("604800000");
        when(environment.getProperty("seata.mcp.auth.enabled", "true")).thenReturn("true");

        properties.init();

        assertEquals("sse", properties.getMcpType());
        assertNotNull(properties.getSseServerProperties());
    }

    @Test
    void testGetEnv() {
        assertEquals(environment, properties.getEnv());
    }

    @Test
    void testSetStreamableProperties() {
        MCPProperties.StreamableProperties streamableProps = new MCPProperties.StreamableProperties("/mcp", 30L);
        properties.setStreamableProperties(streamableProps);
        assertEquals(streamableProps, properties.getStreamableProperties());
    }

    @Test
    void testSetSseServerProperties() {
        MCPProperties.SseServerProperties sseProps = new MCPProperties.SseServerProperties("/sse", "/message");
        properties.setSseServerProperties(sseProps);
        assertEquals(sseProps, properties.getSseServerProperties());
    }
}
