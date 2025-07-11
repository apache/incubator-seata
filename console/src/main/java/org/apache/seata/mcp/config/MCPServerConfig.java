package org.apache.seata.mcp.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.manager.McpServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class MCPServerConfig {
    @Autowired
    private MCPConfiguration serverConfig;

    @Bean
    public McpServerManager mcpServerManager(ObjectMapper objectMapper) {
        MCPConfiguration config = serverConfig;
        McpServerManager mcpServerManager = new McpServerManager(config, objectMapper);
        mcpServerManager.start();
        return mcpServerManager;
    }


    @Bean
    public RouterFunction<ServerResponse> mcpRouter(McpServerManager manager) {
        return manager.getRouterFunction();
    }
}
