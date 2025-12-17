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
package org.seata.mcp.core.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.protocol.MissingRuntimeTransportSession;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.ProtocolErrorException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ProtocolDefinition and related classes
 */
class ProtocolDefinitionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testConstants() {
        assertEquals("2.0", ProtocolDefinition.JSONRPC_VERSION);
        assertNull(ProtocolDefinition.FIRST_PAGE);
        assertEquals("initialize", ProtocolDefinition.METHOD_INITIALIZE);
        assertEquals("ping", ProtocolDefinition.METHOD_PING);
        assertEquals("tools/list", ProtocolDefinition.METHOD_TOOLS_LIST);
        assertEquals("tools/call", ProtocolDefinition.METHOD_TOOLS_CALL);
        assertEquals(-32601, ProtocolDefinition.ErrorCodes.METHOD_NOT_FOUND);
        assertEquals(-32603, ProtocolDefinition.ErrorCodes.INTERNAL_ERROR);
    }

    @Test
    void testDeserializeJsonRpcRequest() throws IOException {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"test\",\"id\":1,\"params\":{}}";
        ProtocolDefinition.JSONRPCMessage message = ProtocolDefinition.deserializeJsonRpcMessage(mapper, json);
        assertInstanceOf(ProtocolDefinition.JSONRPCRequest.class, message);
        ProtocolDefinition.JSONRPCRequest request = (ProtocolDefinition.JSONRPCRequest) message;
        assertEquals("2.0", request.getJsonrpc());
        assertEquals("test", request.getMethod());
        assertEquals(1, request.getId());
    }

    @Test
    void testDeserializeJsonRpcNotification() throws IOException {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"test\",\"params\":{}}";
        ProtocolDefinition.JSONRPCMessage message = ProtocolDefinition.deserializeJsonRpcMessage(mapper, json);
        assertInstanceOf(ProtocolDefinition.JSONRPCNotification.class, message);
        ProtocolDefinition.JSONRPCNotification notification = (ProtocolDefinition.JSONRPCNotification) message;
        assertEquals("2.0", notification.getJsonrpc());
        assertEquals("test", notification.getMethod());
    }

    @Test
    void testDeserializeJsonRpcResponse() throws IOException {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}";
        ProtocolDefinition.JSONRPCMessage message = ProtocolDefinition.deserializeJsonRpcMessage(mapper, json);
        assertInstanceOf(ProtocolDefinition.JSONRPCResponse.class, message);
        ProtocolDefinition.JSONRPCResponse response = (ProtocolDefinition.JSONRPCResponse) message;
        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertNotNull(response.getResult());
    }

    @Test
    void testDeserializeInvalidMessage() {
        String json = "{\"invalid\":\"message\"}";
        assertThrows(IllegalArgumentException.class, () -> ProtocolDefinition.deserializeJsonRpcMessage(mapper, json));
    }

    @Test
    void testJSONRPCRequest() {
        ProtocolDefinition.JSONRPCRequest request = new ProtocolDefinition.JSONRPCRequest();
        request.setMethod("test");
        request.setId(1);
        request.setParams(Collections.emptyMap());
        assertEquals("test", request.getMethod());
        assertEquals(1, request.getId());
        assertNotNull(request.getParams());
    }

    @Test
    void testJSONRPCNotification() {
        ProtocolDefinition.JSONRPCNotification notification = new ProtocolDefinition.JSONRPCNotification();
        notification.setMethod("test");
        Map<String, Object> params = new HashMap<>();
        notification.setParams(params);
        assertEquals("test", notification.getMethod());
        assertEquals(params, notification.getParams());
    }

    @Test
    void testJSONRPCResponse() {
        ProtocolDefinition.JSONRPCResponse response = new ProtocolDefinition.JSONRPCResponse();
        response.setId(1);
        response.setResult("success");
        ProtocolDefinition.JSONRPCResponse.JSONRPCError error =
                new ProtocolDefinition.JSONRPCResponse.JSONRPCError(1, "error", null);
        response.setError(error);
        assertEquals(1, response.getId());
        assertEquals("success", response.getResult());
        assertEquals(error, response.getError());
    }

    @Test
    void testJSONRPCError() {
        ProtocolDefinition.JSONRPCResponse.JSONRPCError error =
                new ProtocolDefinition.JSONRPCResponse.JSONRPCError(1, "message", "data");
        assertEquals(1, error.getCode());
        assertEquals("message", error.getMessage());
        assertEquals("data", error.getData());
        error.setCode(2);
        error.setMessage("new message");
        error.setData("new data");
        assertEquals(2, error.getCode());
        assertEquals("new message", error.getMessage());
        assertEquals("new data", error.getData());
    }

    @Test
    void testBaseMeta() {
        ProtocolDefinition.BaseMeta meta = new ProtocolDefinition.BaseMeta() {};
        assertNull(meta.meta());
        Map<String, Object> metaMap = new HashMap<>();
        meta.setMeta(metaMap);
        assertEquals(metaMap, meta.getMeta());
    }

    @Test
    void testInitializeRequest() {
        ProtocolDefinition.ClientCapabilities capabilities = new ProtocolDefinition.ClientCapabilities();
        ProtocolDefinition.Implementation clientInfo = new ProtocolDefinition.Implementation("client", "1.0");
        ProtocolDefinition.InitializeRequest request =
                new ProtocolDefinition.InitializeRequest("2024-11-05", capabilities, clientInfo);
        assertEquals("2024-11-05", request.getProtocolVersion());
        assertEquals(capabilities, request.getCapabilities());
        assertEquals(clientInfo, request.getClientInfo());
    }

    @Test
    void testInitializeResult() {
        ProtocolDefinition.ServerCapabilities capabilities =
                ProtocolDefinition.ServerCapabilities.builder().build();
        ProtocolDefinition.Implementation serverInfo = new ProtocolDefinition.Implementation("server", "1.0");
        ProtocolDefinition.InitializeResult result =
                new ProtocolDefinition.InitializeResult("2024-11-05", capabilities, serverInfo, "instructions");
        assertEquals("2024-11-05", result.getProtocolVersion());
        assertEquals(capabilities, result.getCapabilities());
        assertEquals(serverInfo, result.getServerInfo());
        assertEquals("instructions", result.getInstructions());
    }

    @Test
    void testClientCapabilities() {
        Map<String, Object> experimental = new HashMap<>();
        ProtocolDefinition.ClientCapabilities.RootCapabilities roots =
                new ProtocolDefinition.ClientCapabilities.RootCapabilities(true);
        ProtocolDefinition.ClientCapabilities capabilities =
                new ProtocolDefinition.ClientCapabilities(experimental, roots, null);
        assertEquals(experimental, capabilities.getExperimental());
        assertEquals(roots, capabilities.getRoots());
        assertTrue(roots.getListChanged());
    }

    @Test
    void testServerCapabilitiesBuilder() {
        ProtocolDefinition.ServerCapabilities capabilities = ProtocolDefinition.ServerCapabilities.builder()
                .completions()
                .logging()
                .prompts(true)
                .resources(true, true)
                .tools(true)
                .build();
        assertNotNull(capabilities.completions());
        assertNotNull(capabilities.logging());
        assertNotNull(capabilities.prompts());
        assertTrue(capabilities.prompts().listChanged());
        assertNotNull(capabilities.resources());
        assertTrue(capabilities.resources().subscribe());
        assertTrue(capabilities.resources().listChanged());
        assertNotNull(capabilities.tools());
        assertTrue(capabilities.tools().listChanged());
    }

    @Test
    void testImplementation() {
        ProtocolDefinition.Implementation impl = new ProtocolDefinition.Implementation("name", "version");
        assertEquals("name", impl.getName());
        assertEquals("version", impl.getVersion());
        impl.setTitle("title");
        assertEquals("title", impl.getTitle());
    }

    @Test
    void testRole() {
        ProtocolDefinition.Role role = ProtocolDefinition.Role.USER;
        assertNotNull(role);
        ProtocolDefinition.Role assistant = ProtocolDefinition.Role.ASSISTANT;
        assertNotNull(assistant);
    }

    @Test
    void testListToolsResult() {
        List<ProtocolDefinition.Tool> tools = Collections.emptyList();
        ProtocolDefinition.ListToolsResult result = new ProtocolDefinition.ListToolsResult(tools, "cursor");
        assertEquals(tools, result.getTools());
        assertEquals("cursor", result.getNextCursor());
        result.setNextCursor("newCursor");
        assertEquals("newCursor", result.getNextCursor());
    }

    @Test
    void testJsonSchema() {
        ProtocolDefinition.JsonSchema schema = new ProtocolDefinition.JsonSchema();
        schema.setType("object");
        Map<String, Object> properties = new HashMap<>();
        schema.setProperties(properties);
        List<String> required = Collections.singletonList("field");
        schema.setRequired(required);
        schema.setAdditionalProperties(false);
        assertEquals("object", schema.getType());
        assertEquals(properties, schema.getProperties());
        assertEquals(required, schema.getRequired());
        assertFalse(schema.getAdditionalProperties());
    }

    @Test
    void testTool() {
        ProtocolDefinition.JsonSchema inputSchema = new ProtocolDefinition.JsonSchema();
        Map<String, Object> outputSchema = new HashMap<>();
        ProtocolDefinition.ToolAnnotations annotations = new ProtocolDefinition.ToolAnnotations();
        Map<String, Object> meta = new HashMap<>();
        ProtocolDefinition.Tool tool = new ProtocolDefinition.Tool(
                "name", "title", "description", inputSchema, outputSchema, annotations, meta);
        assertEquals("name", tool.getName());
        assertEquals("title", tool.getTitle());
        assertEquals("description", tool.getDescription());
        assertEquals(inputSchema, tool.getInputSchema());
        assertEquals(outputSchema, tool.getOutputSchema());
        assertEquals(annotations, tool.getAnnotations());
        assertEquals(meta, tool.getMeta());
    }

    @Test
    void testToolAnnotations() {
        ProtocolDefinition.ToolAnnotations annotations = new ProtocolDefinition.ToolAnnotations();
        annotations.setTitle("title");
        annotations.setReadOnlyHint(true);
        annotations.setDestructiveHint(true);
        annotations.setIdempotentHint(true);
        annotations.setOpenWorldHint(true);
        annotations.setReturnDirect(true);
        assertEquals("title", annotations.getTitle());
        assertTrue(annotations.getReadOnlyHint());
        assertTrue(annotations.getDestructiveHint());
        assertTrue(annotations.getIdempotentHint());
        assertTrue(annotations.getOpenWorldHint());
        assertTrue(annotations.getReturnDirect());
    }

    @Test
    void testCallToolRequest() {
        Map<String, Object> arguments = new HashMap<>();
        Map<String, Object> meta = new HashMap<>();
        ProtocolDefinition.CallToolRequest request = new ProtocolDefinition.CallToolRequest("tool", arguments, meta);
        assertEquals("tool", request.getName());
        assertEquals(arguments, request.getArguments());
        assertEquals(meta, request.meta());
    }

    @Test
    void testCallToolResult() {
        List<ProtocolDefinition.Content> content =
                Collections.singletonList(new ProtocolDefinition.TextContent("text"));
        ProtocolDefinition.CallToolResult result = new ProtocolDefinition.CallToolResult(content, true);
        assertEquals(content, result.getContent());
        assertTrue(result.getError());
        Map<String, Object> structured = new HashMap<>();
        ProtocolDefinition.CallToolResult result2 = new ProtocolDefinition.CallToolResult(content, false, structured);
        assertFalse(result2.getError());
        assertEquals(structured, result2.getStructuredContent());
    }

    @Test
    void testTextContent() {
        ProtocolDefinition.TextContent content = new ProtocolDefinition.TextContent("text");
        assertEquals("text", content.getText());
        List<ProtocolDefinition.Role> audience = Collections.singletonList(ProtocolDefinition.Role.USER);
        content.setAudience(audience);
        content.setPriority(1.0);
        assertEquals(audience, content.getAudience());
        assertEquals(1.0, content.getPriority());
    }

    @Test
    void testContentType() {
        ProtocolDefinition.TextContent content = new ProtocolDefinition.TextContent("text");
        assertEquals("text", content.type());
    }

    @Test
    void testPaginatedRequest() {
        Map<String, Object> meta = new HashMap<>();
        ProtocolDefinition.PaginatedRequest request = new ProtocolDefinition.PaginatedRequest("cursor", meta);
        assertEquals("cursor", request.getCursor());
        assertEquals(meta, request.meta());
    }

    @Test
    void testPaginatedResult() {
        ProtocolDefinition.PaginatedResult result = new ProtocolDefinition.PaginatedResult("cursor");
        assertEquals("cursor", result.getNextCursor());
        result.setNextCursor("newCursor");
        assertEquals("newCursor", result.getNextCursor());
    }

    @Test
    void testLoggingLevel() {
        ProtocolDefinition.LoggingLevel level = ProtocolDefinition.LoggingLevel.INFO;
        assertEquals(1, level.level());
        assertEquals(0, ProtocolDefinition.LoggingLevel.DEBUG.level());
        assertEquals(7, ProtocolDefinition.LoggingLevel.EMERGENCY.level());
    }

    @Test
    void testSetLevelRequest() {
        ProtocolDefinition.SetLevelRequest request =
                new ProtocolDefinition.SetLevelRequest(ProtocolDefinition.LoggingLevel.INFO);
        assertEquals(ProtocolDefinition.LoggingLevel.INFO, request.getLevel());
        request.setLevel(ProtocolDefinition.LoggingLevel.DEBUG);
        assertEquals(ProtocolDefinition.LoggingLevel.DEBUG, request.getLevel());
    }

    @Test
    void testCompleteResult() {
        ProtocolDefinition.CompleteResult.CompleteCompletion completion =
                new ProtocolDefinition.CompleteResult.CompleteCompletion(Collections.singletonList("value"), 1, true);
        Map<String, Object> meta = new HashMap<>();
        ProtocolDefinition.CompleteResult result = new ProtocolDefinition.CompleteResult(completion, meta);
        assertEquals(completion, result.completion());
        assertEquals(meta, result.meta());
        assertEquals(1, completion.values().size());
        assertEquals(1, completion.total());
        assertTrue(completion.hasMore());
    }

    @Test
    void testRoot() {
        ProtocolDefinition.Root root = new ProtocolDefinition.Root("uri", "name");
        assertEquals("uri", root.getUri());
        assertEquals("name", root.getName());
        root.setUri("newUri");
        root.setName("newName");
        assertEquals("newUri", root.getUri());
        assertEquals("newName", root.getName());
    }

    @Test
    void testListRootsResult() {
        List<ProtocolDefinition.Root> roots = Collections.singletonList(new ProtocolDefinition.Root("uri", "name"));
        ProtocolDefinition.ListRootsResult result = new ProtocolDefinition.ListRootsResult(roots);
        assertEquals(roots, result.getRoots());
        assertNull(result.getNextCursor());
        result.setNextCursor("cursor");
        assertEquals("cursor", result.getNextCursor());
    }

    @Test
    void testProtocolErrorException() {
        ProtocolDefinition.JSONRPCResponse.JSONRPCError error =
                new ProtocolDefinition.JSONRPCResponse.JSONRPCError(1, "message", null);
        ProtocolErrorException exception = new ProtocolErrorException(error);
        assertEquals(error, exception.getJsonRpcError());
        assertEquals("message", exception.getMessage());
    }

    @Test
    void testProtocolErrorExceptionWithObject() {
        Object error = "error";
        ProtocolErrorException exception = new ProtocolErrorException(error);
        assertEquals("error", exception.getMessage());
    }

    @Test
    void testMissingRuntimeTransportSession() {
        MissingRuntimeTransportSession session = new MissingRuntimeTransportSession("test-id");
        assertTrue(session.isHealthy());
        session.setHealthy(false);
        assertFalse(session.isHealthy());
        ProtocolDefinition.LoggingLevel level = ProtocolDefinition.LoggingLevel.DEBUG;
        session.setMinLoggingLevel(level);
        assertTrue(session.isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel.INFO));
    }

    @Test
    void testMissingRuntimeTransportSessionSendRequest() {
        MissingRuntimeTransportSession session = new MissingRuntimeTransportSession("test-id");
        TypeReference<String> typeRef = new TypeReference<String>() {};
        assertThrows(IllegalStateException.class, () -> session.sendRequest("method", null, typeRef)
                .block());
    }

    @Test
    void testMissingRuntimeTransportSessionSendNotification() {
        MissingRuntimeTransportSession session = new MissingRuntimeTransportSession("test-id");
        assertThrows(IllegalStateException.class, () -> session.sendNotification("method", null)
                .block());
    }

    @Test
    void testMissingRuntimeTransportSessionClose() {
        MissingRuntimeTransportSession session = new MissingRuntimeTransportSession("test-id");
        session.close();
        session.closeGracefully().block();
    }

    @Test
    void testMissingRuntimeTransportSessionSetMinLoggingLevelNull() {
        MissingRuntimeTransportSession session = new MissingRuntimeTransportSession("test-id");
        assertThrows(IllegalArgumentException.class, () -> session.setMinLoggingLevel(null));
    }
}
