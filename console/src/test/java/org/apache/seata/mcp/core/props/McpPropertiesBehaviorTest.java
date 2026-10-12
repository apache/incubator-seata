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

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpPropertiesBehaviorTest {
    @Test
    void queryDurationDefaultsAndInvalidConfiguration() {
        for (String duration : List.of("86400000", "invalid", "3600000")) {
            MCPProperties properties = new MCPProperties(
                    null,
                    new MockEnvironment().withProperty("seata.mcp.query.max-query-duration", duration),
                    null,
                    null);
            properties.init();
            assertEquals(Long.valueOf("3600000".equals(duration) ? 3600000 : 86400000), properties.getQueryDuration());
            assertTrue(properties.getEndpoints().isEmpty());
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> properties.getEndpoints().add("/invalid"));
        }
    }

    @Test
    void protocolsExposeTheirConfiguredEndpointsAndRejectMissingConfiguration() {
        McpServerProperties server = new McpServerProperties();
        server.setProtocol(McpServerProperties.ServerProtocol.SSE);
        McpServerSseProperties sse = new McpServerSseProperties();
        sse.setSseEndpoint("/events");
        sse.setSseMessageEndpoint("/messages");
        MCPProperties properties = new MCPProperties(server, new MockEnvironment(), sse, null);
        properties.init();
        assertEquals(List.of("/events", "/messages"), properties.getEndpoints());
        assertThrows(
                IllegalStateException.class, () -> new MCPProperties(server, new MockEnvironment(), null, null).init());
        server.setProtocol(McpServerProperties.ServerProtocol.STREAMABLE);
        McpServerStreamableHttpProperties stream = new McpServerStreamableHttpProperties();
        stream.setMcpEndpoint("/tools");
        properties = new MCPProperties(server, new MockEnvironment(), null, stream);
        properties.init();
        assertEquals(List.of("/tools"), properties.getEndpoints());
        assertThrows(
                IllegalStateException.class, () -> new MCPProperties(server, new MockEnvironment(), null, null).init());
    }

    @Test
    void namespaceRequiresNameAndEitherClusterOrVgroup() {
        NameSpaceDetail namespace = new NameSpaceDetail();
        assertFalse(namespace.isValid());
        namespace.setNamespace("public");
        assertFalse(namespace.isValid());
        namespace.setCluster("default");
        assertTrue(namespace.isValid());
        namespace.setCluster(null);
        namespace.setvGroup("group");
        assertTrue(namespace.isValid());
        namespace.setNamespace(" ");
        assertFalse(namespace.isValid());
    }

    @Test
    void namingServerValidatesAddressesAndUsesProtocol() {
        NamingServerProperties properties = new NamingServerProperties();
        properties.setAddr(null);
        assertThrows(IllegalStateException.class, properties::getNamingServerUrl);
        properties.setAddr(List.of());
        assertThrows(IllegalStateException.class, properties::getNamingServerUrl);
        properties.setAddr(List.of("localhost:8080"));
        properties.setProtocol("https");
        assertEquals("https://localhost:8080", properties.getNamingServerUrl());
    }
}
