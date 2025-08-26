package org.apache.seata.mcp.register;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.annotation.Prompt;
import org.apache.seata.mcp.annotation.PromptParam;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.manager.MCPServerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MCPAutoRegister
 */
@ExtendWith(MockitoExtension.class)
class MCPAutoRegisterTest {

    @Mock
    private MCPServerManager mcpServerManager;

    @Mock
    private McpAsyncServer mcpAsyncServer;

    private ObjectMapper objectMapper;
    private MCPAutoRegister mcpAutoRegister;

    // Test beans for annotation scanning
    private TestToolBean testToolBean;
    private TestPromptBean testPromptBean;
    private TestComplexBean testComplexBean;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mcpAutoRegister = new MCPAutoRegister(mcpServerManager);

        // Use reflection to inject the ObjectMapper
        try {
            Field mapperField = MCPAutoRegister.class.getDeclaredField("mapper");
            mapperField.setAccessible(true);
            mapperField.set(mcpAutoRegister, objectMapper);
        } catch (Exception e) {
            fail("Failed to inject ObjectMapper: " + e.getMessage());
        }

        // Mock the server manager to return the async server (lenient to avoid unnecessary stubbing warnings)
        lenient().when(mcpServerManager.getServerInstance()).thenReturn(mcpAsyncServer);

        // Initialize test beans
        testToolBean = new TestToolBean();
        testPromptBean = new TestPromptBean();
        testComplexBean = new TestComplexBean();
    }

    @Test
    void testPostProcessAfterInitializationWithPromptAnnotation() {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addPrompt(any())).thenReturn(Mono.empty());

        Object result = mcpAutoRegister.postProcessAfterInitialization(testPromptBean, "testPromptBean");

        assertEquals(testPromptBean, result);
        verify(mcpAsyncServer, times(1)).addPrompt(any(McpServerFeatures.AsyncPromptSpecification.class));
    }

    @Test
    void testPostProcessAfterInitializationWithNoAnnotations() {
        Object plainBean = new Object();
        Object result = mcpAutoRegister.postProcessAfterInitialization(plainBean, "plainBean");

        assertEquals(plainBean, result);
        verify(mcpAsyncServer, never()).addTool(any());
        verify(mcpAsyncServer, never()).addPrompt(any());
    }

    @Test
    void testAutoRegisterToolWithSimpleParameters() throws Exception {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addTool(any())).thenReturn(Mono.empty());

        Method method = TestToolBean.class.getMethod("simpleTool", String.class, Integer.class);
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        mcpAutoRegister.autoRegisterTool(testToolBean, method, toolAnnotation);

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpAsyncServer).addTool(captor.capture());

        McpServerFeatures.AsyncToolSpecification spec = captor.getValue();
        assertNotNull(spec);
        assertEquals("simpleTool", spec.tool().getName());
        assertEquals("A simple tool for testing", spec.tool().getDescription());

        // Verify JSON schema
        McpSchema.JsonSchema inputSchema = spec.tool().getInputSchema();
        assertNotNull(inputSchema);

        // Convert to JSON to verify structure (since JsonSchema might not have direct getters)
        String schemaJson = objectMapper.writeValueAsString(inputSchema);
        JsonNode schemaNode = objectMapper.readTree(schemaJson);

        assertEquals("object", schemaNode.get("type").asText());
        assertTrue(schemaNode.has("properties"));
        assertTrue(schemaNode.get("properties").has("name"));
        assertTrue(schemaNode.get("properties").has("count"));
        assertTrue(schemaNode.has("required"));
    }

    @Test
    void testAutoRegisterToolWithComplexParameter() throws Exception {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addTool(any())).thenReturn(Mono.empty());

        Method method = TestToolBean.class.getMethod("complexTool", TestComplexParam.class);
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        mcpAutoRegister.autoRegisterTool(testToolBean, method, toolAnnotation);

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpAsyncServer).addTool(captor.capture());

        McpServerFeatures.AsyncToolSpecification spec = captor.getValue();
        McpSchema.JsonSchema inputSchema = spec.tool().getInputSchema();
        String schema = objectMapper.writeValueAsString(inputSchema);
        JsonNode schemaNode = objectMapper.readTree(schema);

        // Verify complex object schema
        JsonNode paramProperties = schemaNode.get("properties").get("param");
        assertEquals("object", paramProperties.get("type").asText());
        assertTrue(paramProperties.has("properties"));
        assertTrue(paramProperties.get("properties").has("name"));
        assertTrue(paramProperties.get("properties").has("value"));
    }

    @Test
    void testAutoRegisterPromptWithParameters() throws Exception {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addPrompt(any())).thenReturn(Mono.empty());

        Method method = TestPromptBean.class.getMethod("simplePrompt", String.class, Boolean.class);
        Prompt promptAnnotation = method.getAnnotation(Prompt.class);

        mcpAutoRegister.autoRegisterPrompt(testPromptBean, method, promptAnnotation);

        ArgumentCaptor<McpServerFeatures.AsyncPromptSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncPromptSpecification.class);
        verify(mcpAsyncServer).addPrompt(captor.capture());

        McpServerFeatures.AsyncPromptSpecification spec = captor.getValue();
        assertNotNull(spec);
        assertEquals("simplePrompt", spec.prompt().getName());
        assertEquals("A simple prompt for testing", spec.prompt().getDescription());
        assertEquals(2, spec.prompt().getArguments().size());
    }

    @Test
    void testToolCallHandlerExecution() throws Exception {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addTool(any())).thenReturn(Mono.empty());

        Method method = TestToolBean.class.getMethod("simpleTool", String.class, Integer.class);
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        mcpAutoRegister.autoRegisterTool(testToolBean, method, toolAnnotation);

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpAsyncServer).addTool(captor.capture());

        McpServerFeatures.AsyncToolSpecification spec = captor.getValue();

        // Create a mock call tool request
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("name", "test");
        arguments.put("count", 42);
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("simpleTool", arguments);

        // Execute the handler and block for result
        Mono<McpSchema.CallToolResult> result = spec.callHandler().apply(null, request);
        McpSchema.CallToolResult toolResult = result.block();

        assertNotNull(toolResult);
        assertFalse(toolResult.getError());
        assertEquals(1, toolResult.getContent().size());
        assertTrue(toolResult.getContent().get(0) instanceof McpSchema.TextContent);
        assertEquals("Hello test, count: 42",
                ((McpSchema.TextContent)toolResult.getContent().get(0)).getText());
    }

    @Test
    void testPromptCallHandlerExecution() throws Exception {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addPrompt(any())).thenReturn(Mono.empty());

        Method method = TestPromptBean.class.getMethod("simplePrompt", String.class, Boolean.class);
        Prompt promptAnnotation = method.getAnnotation(Prompt.class);

        mcpAutoRegister.autoRegisterPrompt(testPromptBean, method, promptAnnotation);

        ArgumentCaptor<McpServerFeatures.AsyncPromptSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncPromptSpecification.class);
        verify(mcpAsyncServer).addPrompt(captor.capture());

        McpServerFeatures.AsyncPromptSpecification spec = captor.getValue();

        // Create a mock get prompt request
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("topic", "AI");
        arguments.put("detailed", true);
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("simplePrompt", arguments);

        // Execute the handler and block for result
        Mono<McpSchema.GetPromptResult> result = spec.promptHandler().apply(null, request);
        McpSchema.GetPromptResult promptResult = result.block();

        assertNotNull(promptResult);
        assertEquals(1, promptResult.getMessages().size());
        McpSchema.PromptMessage message = promptResult.getMessages().get(0);
        assertEquals(McpSchema.Role.USER, message.getRole());
        assertTrue(message.getContent() instanceof McpSchema.TextContent);
        assertEquals("Prompt about AI (detailed: true)",
                ((McpSchema.TextContent)message.getContent()).getText());
    }

    @Test
    void testToolCallHandlerWithException() throws Exception {
        // Setup mocks for this specific test
        when(mcpAsyncServer.addTool(any())).thenReturn(Mono.empty());

        Method method = TestToolBean.class.getMethod("errorTool");
        Tool toolAnnotation = method.getAnnotation(Tool.class);

        mcpAutoRegister.autoRegisterTool(testToolBean, method, toolAnnotation);

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpAsyncServer).addTool(captor.capture());

        McpServerFeatures.AsyncToolSpecification spec = captor.getValue();

        // Create a mock call tool request
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("errorTool", Collections.emptyMap());

        // Execute the handler and block for result
        Mono<McpSchema.CallToolResult> result = spec.callHandler().apply(null, request);
        McpSchema.CallToolResult toolResult = result.block();

        assertNotNull(toolResult);
        assertTrue(toolResult.getError());
        assertEquals(1, toolResult.getContent().size());
        assertTrue(toolResult.getContent().get(0) instanceof McpSchema.TextContent);
        String errorMessage = ((McpSchema.TextContent)toolResult.getContent().get(0)).getText();
        assertTrue(errorMessage.contains("Test error"));
    }

    @Test
    void testGeneratePropertySchemaForPrimitiveTypes() throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod("generatePropertySchema", Class.class, java.lang.reflect.Parameter.class);
        method.setAccessible(true);

        Method testMethod = TestToolBean.class.getMethod("simpleTool", String.class, Integer.class);
        java.lang.reflect.Parameter stringParam = testMethod.getParameters()[0];
        java.lang.reflect.Parameter intParam = testMethod.getParameters()[1];

        JsonNode stringSchema = (JsonNode) method.invoke(mcpAutoRegister, String.class, stringParam);
        JsonNode intSchema = (JsonNode) method.invoke(mcpAutoRegister, Integer.class, intParam);

        assertEquals("string", stringSchema.get("type").asText());
        assertEquals("integer", intSchema.get("type").asText());
    }

    @Test
    void testGenerateTypeSchemaForCollections() throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod("generateTypeSchema",
                com.fasterxml.jackson.databind.node.ObjectNode.class,
                Class.class);
        method.setAccessible(true);

        com.fasterxml.jackson.databind.node.ObjectNode prop = objectMapper.createObjectNode();
        method.invoke(mcpAutoRegister, prop, List.class);

        assertEquals("array", prop.get("type").asText());
        assertTrue(prop.has("items"));
    }

    @Test
    void testConvertArgument() throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod("convertArgument", Object.class, Class.class);
        method.setAccessible(true);

        // Test string conversion
        Object result = method.invoke(mcpAutoRegister, "42", Integer.class);
        assertEquals(42, result);

        // Test null handling
        Object nullResult = method.invoke(mcpAutoRegister, null, String.class);
        assertNull(nullResult);

        // Test same type
        String stringResult = (String) method.invoke(mcpAutoRegister, "test", String.class);
        assertEquals("test", stringResult);
    }

    @Test
    void testGetClassInfoAsJson() throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod("getClassInfoAsJson", Class.class);
        method.setAccessible(true);

        String result = (String) method.invoke(mcpAutoRegister, TestEnum.class);
        assertNotNull(result);

        JsonNode resultNode = objectMapper.readTree(result);
        assertEquals("TestEnum", resultNode.get("className").asText());
        assertTrue(resultNode.has("enumValues"));
        assertEquals(2, resultNode.get("enumValues").size());
    }

    @Test
    void testGetAllFields() throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod("getAllFields", Class.class);
        method.setAccessible(true);

        Field[] fields = (Field[]) method.invoke(mcpAutoRegister, TestComplexParam.class);
        assertTrue(fields.length >= 2); // At least name and value fields
    }

    @Test
    void testIsCustomObject() throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod("isCustomObject", Class.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(mcpAutoRegister, TestComplexParam.class));
        assertFalse((Boolean) method.invoke(mcpAutoRegister, String.class));
        assertFalse((Boolean) method.invoke(mcpAutoRegister, Integer.class));
        assertFalse((Boolean) method.invoke(mcpAutoRegister, TestEnum.class));
    }

    // Test bean classes and supporting types
    public static class TestToolBean {
        @Tool(description = "A simple tool for testing")
        public String simpleTool(@ToolParam(description = "The name",required = true) String name,
                                 @ToolParam(description = "The count",required = true) Integer count) {
            return "Hello " + name + ", count: " + count;
        }

        @Tool(description = "A complex tool for testing")
        public String complexTool(@ToolParam(description = "Complex parameter") TestComplexParam param) {
            return "Complex: " + param.getName() + " = " + param.getValue();
        }

        @Tool(description = "A tool that throws an error")
        public String errorTool() {
            throw new RuntimeException("Test error");
        }
    }

    public static class TestPromptBean {
        @Prompt(description = "A simple prompt for testing")
        public String simplePrompt(@PromptParam(description = "The topic") String topic,
                                   @PromptParam(description = "Detailed flag") Boolean detailed) {
            return "Prompt about " + topic + " (detailed: " + detailed + ")";
        }
    }

    public static class TestComplexBean {
        // Bean without any annotations
        public String normalMethod() {
            return "normal";
        }
    }

    public static class TestComplexParam {
        @ToolParam(description = "Parameter name", required = true)
        private String name;

        @ToolParam(description = "Parameter value", required = false, example = "42")
        private Integer value;

        public TestComplexParam() {}

        public TestComplexParam(String name, Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }

    public enum TestEnum {
        VALUE1(1), VALUE2(2);

        private final int code;

        TestEnum(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}