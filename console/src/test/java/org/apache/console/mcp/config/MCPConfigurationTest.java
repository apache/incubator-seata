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
        // 清除先前可能注入的值
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
        // 设置默认值的模拟行为
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server")).thenReturn("seata-mcp-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("1.0.0");
        when(environment.getProperty("seata.mcp.sseEndpoint", "/sse")).thenReturn("/sse");
        when(environment.getProperty("seata.mcp.messageEndpoint", "/message")).thenReturn("/message");
        when(environment.getProperty("seata.mcp.heartbeat", "false")).thenReturn("false");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "86400000")).thenReturn("86400000");
        
        // 初始化配置
        mcpConfiguration.init();
        
        // 验证默认值
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
        // 设置自定义值的模拟行为
        when(environment.getProperty("seata.mcp.serverName", "seata-mcp-server")).thenReturn("custom-server");
        when(environment.getProperty("seata.mcp.serverVersion", "1.0.0")).thenReturn("2.0.0");
        when(environment.getProperty("seata.mcp.sseEndpoint", "/sse")).thenReturn("/custom-sse");
        when(environment.getProperty("seata.mcp.messageEndpoint", "/message")).thenReturn("/custom-message");
        when(environment.getProperty("seata.mcp.heartbeat", "false")).thenReturn("true");
        when(environment.getProperty("seata.mcp.query.max_query_duration", "86400000")).thenReturn("43200000");
        
        // 初始化配置
        mcpConfiguration.init();
        
        // 验证自定义值
        assertEquals("custom-server", mcpConfiguration.getServerName());
        assertEquals("2.0.0", mcpConfiguration.getServerVersion());
        assertEquals("/custom-sse", mcpConfiguration.getSseEndpoint());
        assertEquals("/custom-message", mcpConfiguration.getMessageEndpoint());
        assertTrue(mcpConfiguration.isHeartbeat());
        assertEquals(43200000L, mcpConfiguration.getQueryDuration());
    }
    
    @Test
    public void testSetterMethods() {
        // 测试所有的setter方法
        mcpConfiguration.setServerName("test-server");
        mcpConfiguration.setServerVersion("3.0.0");
        mcpConfiguration.setSseEndpoint("/test-sse");
        mcpConfiguration.setMessageEndpoint("/test-message");
        mcpConfiguration.setResourceSupport(false);
        mcpConfiguration.setResourceTemplates(true);
        mcpConfiguration.setLoggingLevel(McpSchema.LoggingLevel.DEBUG);
        mcpConfiguration.setHeartbeat(true);
        mcpConfiguration.setQueryDuration(60000L);
        
        // 验证设置的值
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
        // 创建相同配置的两个实例
        MCPConfiguration config1 = new MCPConfiguration();
        MCPConfiguration config2 = new MCPConfiguration();
        
        config1.setServerName("test-server");
        config1.setServerVersion("1.0.0");
        
        config2.setServerName("test-server");
        config2.setServerVersion("1.0.0");
        
        // 验证equals和hashCode方法
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        
        // 修改一个属性后应该不相等
        config2.setServerName("different-server");
        assertNotEquals(config1, config2);
    }
    
    @Test
    public void testToString() {
        // 设置一些值
        mcpConfiguration.setServerName("test-server");
        mcpConfiguration.setServerVersion("1.0.0");
        
        // 验证toString方法包含关键信息
        String toString = mcpConfiguration.toString();
        assertTrue(toString.contains("test-server"));
        assertTrue(toString.contains("1.0.0"));
    }
}
