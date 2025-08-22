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
import io.modelcontextprotocol.server.McpServerFeatures;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.manager.McpServerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MCPAutoRegisterTest {

    @Mock
    private McpServerManager mcpServerManager;

    @Mock
    private io.modelcontextprotocol.server.McpAsyncServer mcpServer;

    @Mock
    private MCPAutoRegister toolRegister;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mcpServerManager.getServerInstance()).thenReturn(mcpServer);
        when(mcpServer.addTool(any())).thenReturn(Mono.empty());
        toolRegister = new MCPAutoRegister(mcpServerManager);

        try {
            Field mapperField = MCPAutoRegister.class.getDeclaredField("mapper");
            mapperField.setAccessible(true);
            mapperField.set(toolRegister, objectMapper);
        } catch (Exception e) {
            fail("Failed to inject ObjectMapper: " + e.getMessage());
        }
    }

    @Test
    public void testRegisterToolWithSimpleTypes() {
        // Create a test bean
        TestToolWithSimpleTypes testBean = new TestToolWithSimpleTypes();

        // Register the tool
        toolRegister.postProcessAfterInitialization(testBean, "testBean");

        // Verify tool registration
        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> toolCaptor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpServer).addTool(toolCaptor.capture());

        // Verify the captured tool specifications
        McpServerFeatures.AsyncToolSpecification capturedSpec = toolCaptor.getValue();
        assertEquals("simpleMethod", capturedSpec.tool().getName());
        assertEquals("A simple test tool", capturedSpec.tool().getDescription());

        // verify JSON Schema
        try {
            JsonNode schema = objectMapper.readTree(
                    objectMapper.writeValueAsString(capturedSpec.tool().getInputSchema()));
            assertEquals("object", schema.get("type").asText());

            JsonNode props = schema.get("properties");
            assertTrue(props.has("stringParam"));
            assertTrue(props.has("intParam"));
            assertTrue(props.has("boolParam"));

            assertEquals("string", props.get("stringParam").get("type").asText());
            assertEquals("integer", props.get("intParam").get("type").asText());
            assertEquals("boolean", props.get("boolParam").get("type").asText());

            JsonNode required = schema.get("required");
            assertTrue(required.isArray());
            List<String> requiredFields = new ArrayList<>();
            required.forEach(node -> requiredFields.add(node.asText()));
            assertFalse(requiredFields.contains("stringParam"));
            assertFalse(requiredFields.contains("intParam"));
            assertFalse(requiredFields.contains("boolParam"));

        } catch (Exception e) {
            fail("Failed to parse schema: " + e.getMessage());
        }
    }

    @Test
    public void testRegisterToolWithComplexTypes() {
        TestToolWithComplexTypes testBean = new TestToolWithComplexTypes();

        toolRegister.postProcessAfterInitialization(testBean, "testBean");

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> toolCaptor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpServer).addTool(toolCaptor.capture());

        McpServerFeatures.AsyncToolSpecification capturedSpec = toolCaptor.getValue();
        assertEquals("complexMethod", capturedSpec.tool().getName());

        // verify JSON Schema
        try {
            JsonNode schema = objectMapper.readTree(
                    objectMapper.writeValueAsString(capturedSpec.tool().getInputSchema()));

            JsonNode props = schema.get("properties");
            assertTrue(props.has("complexParam"));
            assertEquals("object", props.get("complexParam").get("type").asText());

            JsonNode complexProps = props.get("complexParam").get("properties");
            assertTrue(complexProps.has("name"));
            assertTrue(complexProps.has("age"));
            assertTrue(complexProps.has("tags"));

            assertEquals("array", complexProps.get("tags").get("type").asText());

        } catch (Exception e) {
            fail("Failed to parse schema: " + e.getMessage());
        }
    }

    @Test
    public void testHandleCircularReference() {

        TestToolWithCircularReference testBean = new TestToolWithCircularReference();

        toolRegister.postProcessAfterInitialization(testBean, "testBean");

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> toolCaptor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpServer).addTool(toolCaptor.capture());

        McpServerFeatures.AsyncToolSpecification capturedSpec = toolCaptor.getValue();

        try {
            JsonNode schema = objectMapper.readTree(
                    objectMapper.writeValueAsString(capturedSpec.tool().getInputSchema()));
            JsonNode props = schema.get("properties");
            assertTrue(props.has("node"));

            // Verify that the description contains a circular reference warning
            JsonNode nodeChildProps = props.get("node").get("properties").get("child");
            assertTrue(nodeChildProps.has("description"));
            assertTrue(nodeChildProps.get("description").asText().contains("Circular references"));

        } catch (Exception e) {
            fail("Failed to parse schema: " + e.getMessage());
        }
    }

    @Test
    public void testHandleCollectionsAndMaps() {

        TestToolWithCollections testBean = new TestToolWithCollections();

        toolRegister.postProcessAfterInitialization(testBean, "testBean");

        ArgumentCaptor<McpServerFeatures.AsyncToolSpecification> toolCaptor =
                ArgumentCaptor.forClass(McpServerFeatures.AsyncToolSpecification.class);
        verify(mcpServer).addTool(toolCaptor.capture());

        McpServerFeatures.AsyncToolSpecification capturedSpec = toolCaptor.getValue();

        try {
            JsonNode schema = objectMapper.readTree(
                    objectMapper.writeValueAsString(capturedSpec.tool().getInputSchema()));
            JsonNode props = schema.get("properties");

            assertTrue(props.has("list"));
            assertEquals("array", props.get("list").get("type").asText());

            assertTrue(props.has("map"));
            assertEquals("object", props.get("map").get("type").asText());
            assertTrue(props.get("map").has("description"));
            assertTrue(props.get("map").get("description").asText().contains("Key-value"));

            assertTrue(props.has("array"));
            assertEquals("array", props.get("array").get("type").asText());
            assertEquals("string", props.get("array").get("items").get("type").asText());

        } catch (Exception e) {
            fail("Failed to parse schema: " + e.getMessage());
        }
    }

    // Bean classes for testing
    public static class TestToolWithSimpleTypes {
        @Tool(description = "A simple test tool")
        public String simpleMethod(
                @ToolParam(description = "A string parameter") String stringParam,
                @ToolParam(description = "An integer parameter") int intParam,
                @ToolParam(required = false, description = "A boolean parameter") boolean boolParam) {
            return "Processed: " + stringParam + ", " + intParam + ", " + boolParam;
        }
    }

    public static class TestToolWithComplexTypes {
        @Tool(description = "A test tool with complex types")
        public String complexMethod(@ToolParam ComplexParam complexParam) {
            return "Processed complex param";
        }

        public static class ComplexParam {
            @ToolParam(description = "The name")
            private String name;

            @ToolParam(description = "The age")
            private int age;

            @ToolParam(description = "Tags list")
            private List<String> tags;

            // Getters and setters
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public int getAge() {
                return age;
            }

            public void setAge(int age) {
                this.age = age;
            }

            public List<String> getTags() {
                return tags;
            }

            public void setTags(List<String> tags) {
                this.tags = tags;
            }
        }
    }

    public static class TestToolWithCircularReference {
        @Tool(description = "A test tool with circular reference")
        public String circularMethod(@ToolParam Node node) {
            return "Processed node";
        }

        public static class Node {
            @ToolParam
            private String value;

            @ToolParam
            private Node child;

            // Getters and setters
            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public Node getChild() {
                return child;
            }

            public void setChild(Node child) {
                this.child = child;
            }
        }
    }

    public static class TestToolWithCollections {
        @Tool(description = "A test tool with collections")
        public String collectionMethod(
                @ToolParam List<String> list, @ToolParam Map<String, Object> map, @ToolParam String[] array) {
            return "Processed collections";
        }
    }
}
