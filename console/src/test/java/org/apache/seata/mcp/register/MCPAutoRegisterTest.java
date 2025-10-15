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
package org.apache.seata.mcp.register;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MCPAutoRegisterTest {

    @Mock
    private MCPServerManager mcpServerManager;

    @Mock
    private McpAsyncServer mcpAsyncServer;

    private ObjectMapper objectMapper;
    private MCPAutoRegister mcpAutoRegister;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        mcpAutoRegister = new MCPAutoRegister(mcpServerManager);
        Field mapperField = MCPAutoRegister.class.getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(mcpAutoRegister, objectMapper);
        lenient().when(mcpServerManager.getServerInstance()).thenReturn(mcpAsyncServer);
    }

    private McpServerFeatures.AsyncToolSpecification registerTool(String methodName, Class<?>... paramTypes)
            throws Exception {
        when(mcpAsyncServer.addTool(any())).thenReturn(Mono.empty());
        Method method = TestBean.class.getMethod(methodName, paramTypes);
        mcpAutoRegister.autoRegisterTool(new TestBean(), method, method.getAnnotation(Tool.class));
        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> captor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpAsyncServer).addTool(captor.capture());
        return captor.getValue();
    }

    private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = MCPAutoRegister.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(mcpAutoRegister, args);
    }

    @Test
    void testPostProcessAfterInitialization() {
        when(mcpAsyncServer.addTool(any())).thenReturn(Mono.empty());
        Object bean = new TestBean();
        assertEquals(bean, mcpAutoRegister.postProcessAfterInitialization(bean, "test"));
        verify(mcpAsyncServer, atLeastOnce()).addTool(any());

        Object plain = new Object();
        assertEquals(plain, mcpAutoRegister.postProcessAfterInitialization(plain, "plain"));
    }

    @Test
    void testAutoRegisterTool() throws Exception {
        McpServerFeatures.AsyncToolSpecification spec = registerTool("simpleTool", String.class, int.class);
        assertEquals("simpleTool", spec.tool().getName());
        JsonNode schema = objectMapper.readTree(
                objectMapper.writeValueAsString(spec.tool().getInputSchema()));
        assertTrue(schema.get("properties").has("name"));
        assertEquals(2, schema.get("required").size());
    }

    @Test
    void testAutoRegisterWithComplexParam() throws Exception {
        McpServerFeatures.AsyncToolSpecification spec = registerTool("complexTool", TestParam.class);
        JsonNode schema = objectMapper.readTree(
                objectMapper.writeValueAsString(spec.tool().getInputSchema()));
        JsonNode props = schema.get("properties").get("param").get("properties");
        assertTrue(props.has("name"));
        assertTrue(props.has("value"));
    }

    @Test
    void testToolCallHandler() throws Exception {
        McpServerFeatures.AsyncToolSpecification spec = registerTool("simpleTool", String.class, int.class);
        Map<String, Object> args = new HashMap<>();
        args.put("name", "test");
        args.put("count", 5);
        McpSchema.CallToolResult result = spec.callHandler()
                .apply(null, new McpSchema.CallToolRequest("simpleTool", args))
                .block();
        assertFalse(result.getError());
        assertTrue(
                ((McpSchema.TextContent) result.getContent().get(0)).getText().contains("test"));
    }

    @Test
    void testToolCallHandlerWithError() throws Exception {
        McpServerFeatures.AsyncToolSpecification spec = registerTool("errorTool");
        McpSchema.CallToolResult result = spec.callHandler()
                .apply(null, new McpSchema.CallToolRequest("errorTool", Collections.emptyMap()))
                .block();
        assertTrue(result.getError());
    }

    @Test
    void testToolCallHandlerReturnsObject() throws Exception {
        McpServerFeatures.AsyncToolSpecification spec = registerTool("objectTool");
        McpSchema.CallToolResult result = spec.callHandler()
                .apply(null, new McpSchema.CallToolRequest("objectTool", Collections.emptyMap()))
                .block();
        assertTrue(
                ((McpSchema.TextContent) result.getContent().get(0)).getText().contains("data"));
    }

    @Test
    void testToolCallHandlerReturnsCallToolResult() throws Exception {
        McpServerFeatures.AsyncToolSpecification spec = registerTool("resultTool");
        McpSchema.CallToolResult result = spec.callHandler()
                .apply(null, new McpSchema.CallToolRequest("resultTool", Collections.emptyMap()))
                .block();
        assertTrue(result.getError());
    }

    @Test
    void testGenerateTypeSchema() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();

        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                String.class);
        assertEquals("string", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                int.class);
        assertEquals("integer", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                double.class);
        assertEquals("number", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                boolean.class);
        assertEquals("boolean", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                String[].class);
        assertEquals("array", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                List.class);
        assertEquals("array", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                Map.class);
        assertEquals("object", node.get("type").asText());

        node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                TestParam.class);
        assertEquals("object", node.get("type").asText());
        assertTrue(node.has("properties"));
    }

    @Test
    void testGenerateTypeSchemaCircularReference() throws Exception {
        Field field = MCPAutoRegister.class.getDeclaredField("processingTypes");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Class<?>> types = (Set<Class<?>>) field.get(mcpAutoRegister);
        types.add(TestParam.class);

        com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
        invokePrivate(
                "generateTypeSchema",
                new Class<?>[] {com.fasterxml.jackson.databind.node.ObjectNode.class, Class.class},
                node,
                TestParam.class);
        assertTrue(node.get("description").asText().contains("Circular"));
        types.clear();
    }

    @Test
    void testGetClassInfoAsJson() throws Exception {
        String json = (String) invokePrivate("getClassInfoAsJson", new Class<?>[] {Class.class}, TestEnum.class);
        JsonNode node = objectMapper.readTree(json);
        assertEquals("TestEnum", node.get("className").asText());
        assertTrue(node.has("enumValues"));
        assertTrue(node.get("enumValues").get(0).has("code"));
    }

    @Test
    void testGetAllFields() throws Exception {
        Field[] fields = (Field[]) invokePrivate("getAllFields", new Class<?>[] {Class.class}, TestChild.class);
        List<String> names = new ArrayList<>();
        for (Field f : fields) names.add(f.getName());
        assertTrue(names.contains("parentField"));
        assertTrue(names.contains("childField"));
    }

    @Test
    void testIsCustomObject() throws Exception {
        assertTrue((Boolean) invokePrivate("isCustomObject", new Class<?>[] {Class.class}, TestParam.class));
        assertFalse((Boolean) invokePrivate("isCustomObject", new Class<?>[] {Class.class}, String.class));
        assertFalse((Boolean) invokePrivate("isCustomObject", new Class<?>[] {Class.class}, TestEnum.class));
        assertFalse((Boolean) invokePrivate("isCustomObject", new Class<?>[] {Class.class}, int.class));
    }

    @Test
    void testConvertArgument() throws Exception {
        assertNull(invokePrivate("convertArgument", new Class<?>[] {Object.class, Class.class}, null, String.class));
        assertEquals(
                "test",
                invokePrivate("convertArgument", new Class<?>[] {Object.class, Class.class}, "test", String.class));

        Map<String, Object> map = new HashMap<>();
        map.put("name", "test");
        map.put("value", 42);
        Object result =
                invokePrivate("convertArgument", new Class<?>[] {Object.class, Class.class}, map, TestParam.class);
        assertTrue(result instanceof TestParam);
        assertEquals("test", ((TestParam) result).getName());
    }

    @Test
    void testConvertArgumentFailure() {
        try {
            invokePrivate(
                    "convertArgument",
                    new Class<?>[] {Object.class, Class.class},
                    Collections.singletonMap("x", "y"),
                    Integer.class);
            fail("Should throw");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
        }
    }

    // Test Data Classes
    public static class TestBean {
        @Tool(description = "Simple tool")
        public String simpleTool(
                @ToolParam(description = "Name", required = true) String name,
                @ToolParam(description = "Count", required = true) int count) {
            return "Hello " + name + " " + count;
        }

        @Tool(description = "Complex tool")
        public String complexTool(TestParam param) {
            return param.getName();
        }

        @Tool(description = "Error tool")
        public String errorTool() {
            throw new RuntimeException("Error");
        }

        @Tool(description = "Object tool")
        public TestParam objectTool() {
            return new TestParam("data", 1);
        }

        @Tool(description = "Result tool")
        public McpSchema.CallToolResult resultTool() {
            return new McpSchema.CallToolResult(Collections.singletonList(new McpSchema.TextContent("err")), true);
        }
    }

    public static class TestParam {
        @ToolParam(description = "Name", required = true)
        private String name;

        @ToolParam(description = "Value", required = false)
        private Integer value;

        public TestParam() {}

        public TestParam(String name, Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }

    public static class TestParent {
        private String parentField;

        public String getParentField() {
            return parentField;
        }
    }

    public static class TestChild extends TestParent {
        private String childField;

        public String getChildField() {
            return childField;
        }
    }

    public enum TestEnum {
        V1(1),
        V2(2);
        private final int code;

        TestEnum(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
