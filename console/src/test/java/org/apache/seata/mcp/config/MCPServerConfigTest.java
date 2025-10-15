package org.apache.seata.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.manager.MCPServerManager;
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
