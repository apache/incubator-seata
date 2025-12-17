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
package org.seata.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.config.MCPServerConfig;
import org.apache.seata.mcp.core.manager.MCPServerManager;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MCPServerConfigTest {

    @Test
    void testMcpServerManagerBeanCreatesAndStartsManager() throws Exception {
        MCPServerConfig config = new MCPServerConfig();

        MCPProperties properties = new MCPProperties();
        properties.setMcpType(MCPProperties.STREAMABLE_TYPE);
        MCPProperties.StreamableProperties streamableProps = new MCPProperties.StreamableProperties();
        streamableProps.setMcpEndPoint("/mcp");
        streamableProps.setHeartBeatSecondDuration(5L);
        properties.setStreamableProperties(streamableProps);
        properties.setServerName("test-server");
        properties.setServerVersion("1.0.0");

        Field field = MCPServerConfig.class.getDeclaredField("serverConfig");
        field.setAccessible(true);
        field.set(config, properties);

        MCPServerManager manager = config.mcpServerManager(new ObjectMapper());

        assertNotNull(manager);
        assertEquals(properties, manager.getConfig());
        assertTrue(manager.isRunning());
        assertNotNull(manager.getServerInstance());

        manager.stop();
    }

    @Test
    void testMcpRouterReturnsRouterFunctionFromManager() {
        MCPServerConfig config = new MCPServerConfig();
        MCPServerManager manager = mock(MCPServerManager.class);
        @SuppressWarnings("unchecked")
        RouterFunction<ServerResponse> router = (RouterFunction<ServerResponse>) mock(RouterFunction.class);
        when(manager.getRouterFunction()).thenReturn(router);

        RouterFunction<ServerResponse> result = config.mcpRouter(manager);

        assertNotNull(result);
        assertEquals(router, result);
        verify(manager, times(1)).getRouterFunction();
    }
}
