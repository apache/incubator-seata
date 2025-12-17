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

package org.seata.mcp.core.runtimeCore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.ProtocolErrorException;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import org.apache.seata.mcp.core.protocol.RuntimeTransportProvider;
import org.apache.seata.mcp.core.protocol.SchemaValidator;
import org.apache.seata.mcp.core.runtimeCore.AsyncRuntimeEngine;
import org.apache.seata.mcp.core.runtimeCore.NotificationProcessor;
import org.apache.seata.mcp.core.runtimeCore.RequestProcessor;
import org.apache.seata.mcp.core.runtimeCore.RuntimeCapabilities;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeCoreInterface;
import org.apache.seata.mcp.core.runtimeCore.RuntimeExchangeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncRuntimeEngineTest {

    private static final ProtocolDefinition.Implementation SERVER_INFO =
            new ProtocolDefinition.Implementation("test-server", "1.0.0");

    @Mock
    private RuntimeTransportProvider mockTransportProvider;

    @Mock
    private SchemaValidator mockValidator;

    private ObjectMapper mapper;
    private ProtocolDefinition.ServerCapabilities capabilities;

    @BeforeEach
    void setUp() {
        mapper = new TestObjectMapper();
        capabilities = ProtocolDefinition.ServerCapabilities.builder()
                .tools(true)
                .logging()
                .build();

        when(mockTransportProvider.protocolVersions()).thenReturn(Arrays.asList("2024-11-05", "2024-06-01"));
    }

    @SuppressWarnings("unchecked")
    private static class TestObjectMapper extends ObjectMapper {
        @Override
        public <T> T convertValue(Object fromValue, TypeReference<T> typeRef) {
            String typeName = typeRef.getType().getTypeName();
            if (typeName.contains("ProtocolDefinition$SetLevelRequest")) {
                ProtocolDefinition.LoggingLevel level;
                if (fromValue instanceof Map) {
                    Object raw = ((Map<?, ?>) fromValue).get("level");
                    if (raw instanceof ProtocolDefinition.LoggingLevel) {
                        level = (ProtocolDefinition.LoggingLevel) raw;
                    } else {
                        level = ProtocolDefinition.LoggingLevel.valueOf(raw.toString());
                    }
                } else if (fromValue instanceof ProtocolDefinition.SetLevelRequest) {
                    level = ((ProtocolDefinition.SetLevelRequest) fromValue).getLevel();
                } else {
                    level = ProtocolDefinition.LoggingLevel.INFO;
                }
                return (T) new ProtocolDefinition.SetLevelRequest(level);
            }
            return super.convertValue(fromValue, typeRef);
        }
    }

    private AsyncRuntimeEngine createEngine(RuntimeCapabilities.Async features) {
        try {
            Constructor<AsyncRuntimeEngine> ctor = AsyncRuntimeEngine.class.getDeclaredConstructor(
                    RuntimeTransportProvider.class,
                    ObjectMapper.class,
                    RuntimeCapabilities.Async.class,
                    Duration.class,
                    SchemaValidator.class);
            ctor.setAccessible(true);
            return ctor.newInstance(mockTransportProvider, mapper, features, Duration.ofSeconds(10), mockValidator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RuntimeCapabilities.Async baseFeatures(
            List<RuntimeCapabilities.AsyncToolSpecification> tools,
            List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>> roots) {
        return new RuntimeCapabilities.Async(
                SERVER_INFO, capabilities, tools, Collections.emptyMap(), roots, "instructions");
    }

    private RuntimeCapabilities.AsyncToolSpecification simpleTool(String name) {
        ProtocolDefinition.Tool tool = new ProtocolDefinition.Tool(
                name, "Title", "Desc", new ProtocolDefinition.JsonSchema(), Collections.emptyMap(), null, null);
        return new RuntimeCapabilities.AsyncToolSpecification(
                tool, (ctx, args) -> Mono.just(new ProtocolDefinition.CallToolResult("ok", false)));
    }

    @Test
    void testConstructedViaBuilderSetsSessionFactory() {
        RuntimeCoreInterface.async(mockTransportProvider).objectMapper(mapper).build();

        verify(mockTransportProvider).setSessionFactory(any());
    }

    @Test
    void testHandleInitSelectsClosestVersion() throws Exception {
        RuntimeCapabilities.Async features = baseFeatures(Collections.emptyList(), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        ProtocolDefinition.InitializeRequest request = new ProtocolDefinition.InitializeRequest();
        request.setProtocolVersion("2020-01-01");
        request.setClientInfo(new ProtocolDefinition.Implementation("client", "1.0"));

        Method handleInit =
                AsyncRuntimeEngine.class.getDeclaredMethod("handleInit", ProtocolDefinition.InitializeRequest.class);
        handleInit.setAccessible(true);

        StepVerifier.create((Mono<ProtocolDefinition.InitializeResult>) handleInit.invoke(engine, request))
                .expectNextMatches(result -> "2024-06-01".equals(result.getProtocolVersion()))
                .verifyComplete();
    }

    @Test
    void testAddToolSuccessNotifiesClients() {
        RuntimeCapabilities.Async features = baseFeatures(Collections.emptyList(), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        when(mockTransportProvider.notifyClients(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(engine.addTool(simpleTool("new-tool"))).verifyComplete();

        verify(mockTransportProvider)
                .notifyClients(eq(ProtocolDefinition.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED), isNull());
    }

    @Test
    void testAddToolDuplicateFails() {
        RuntimeCapabilities.Async features =
                baseFeatures(Collections.singletonList(simpleTool("dup-tool")), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        StepVerifier.create(engine.addTool(simpleTool("dup-tool")))
                .expectError(ProtocolErrorException.class)
                .verify();
    }

    @Test
    void testHandleToolsListReturnsRegisteredTools() throws Exception {
        RuntimeCapabilities.Async features =
                baseFeatures(Collections.singletonList(simpleTool("listed-tool")), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        Method handleToolsList = AsyncRuntimeEngine.class.getDeclaredMethod("handleToolsList");
        handleToolsList.setAccessible(true);

        @SuppressWarnings("unchecked")
        RequestProcessor<ProtocolDefinition.ListToolsResult> processor =
                (RequestProcessor<ProtocolDefinition.ListToolsResult>) handleToolsList.invoke(engine);

        RuntimeExchangeContext exchange = new RuntimeExchangeContext(
                "id",
                mock(RuntimeSession.class),
                new ProtocolDefinition.ClientCapabilities(),
                SERVER_INFO,
                RuntimeContext.EMPTY);

        StepVerifier.create(processor.handle(exchange, Collections.emptyMap()))
                .expectNextMatches(result -> result.getTools().size() == 1
                        && "listed-tool".equals(result.getTools().get(0).getName()))
                .verifyComplete();
    }

    @Test
    void testHandleToolCallValidatesOutput() throws Exception {
        Map<String, Object> outputSchema = Collections.singletonMap("type", "object");

        ProtocolDefinition.ToolAnnotations toolAnnotations = new ProtocolDefinition.ToolAnnotations();

        ProtocolDefinition.Tool tool = new ProtocolDefinition.Tool(
                "validated-tool",
                "title",
                "desc",
                new ProtocolDefinition.JsonSchema(),
                outputSchema,
                toolAnnotations,
                null);
        RuntimeCapabilities.AsyncToolSpecification spec = RuntimeCapabilities.AsyncToolSpecification.builder()
                .tool(tool)
                .callHandler((ctx, req) -> {
                    Map<String, Object> structured = Collections.singletonMap("key", "value");
                    ProtocolDefinition.CallToolResult result = new ProtocolDefinition.CallToolResult("ok", false);
                    result.setStructuredContent(structured);
                    return Mono.just(result);
                })
                .build();

        when(mockValidator.validate(eq(outputSchema), any()))
                .thenReturn(SchemaValidator.ValidationResponse.asValid("{\"key\":\"value\"}"));

        RuntimeCapabilities.Async features = baseFeatures(Collections.singletonList(spec), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        Method handleToolCall = AsyncRuntimeEngine.class.getDeclaredMethod("handleToolCall");
        handleToolCall.setAccessible(true);

        @SuppressWarnings("unchecked")
        RequestProcessor<ProtocolDefinition.CallToolResult> processor =
                (RequestProcessor<ProtocolDefinition.CallToolResult>) handleToolCall.invoke(engine);

        RuntimeExchangeContext exchange = new RuntimeExchangeContext(
                "id",
                mock(RuntimeSession.class),
                new ProtocolDefinition.ClientCapabilities(),
                SERVER_INFO,
                RuntimeContext.EMPTY);

        ProtocolDefinition.CallToolRequest request =
                new ProtocolDefinition.CallToolRequest("validated-tool", Collections.emptyMap());

        StepVerifier.create(processor.handle(exchange, request))
                .expectNextMatches(result -> !result.getError() && result.getContent() != null)
                .verifyComplete();
    }

    @Test
    void testHandleToolCallNotFound() throws Exception {
        RuntimeCapabilities.Async features = baseFeatures(Collections.emptyList(), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        Method handleToolCall = AsyncRuntimeEngine.class.getDeclaredMethod("handleToolCall");
        handleToolCall.setAccessible(true);

        @SuppressWarnings("unchecked")
        RequestProcessor<ProtocolDefinition.CallToolResult> processor =
                (RequestProcessor<ProtocolDefinition.CallToolResult>) handleToolCall.invoke(engine);

        RuntimeExchangeContext exchange = new RuntimeExchangeContext(
                "id",
                mock(RuntimeSession.class),
                new ProtocolDefinition.ClientCapabilities(),
                SERVER_INFO,
                RuntimeContext.EMPTY);

        ProtocolDefinition.CallToolRequest request =
                new ProtocolDefinition.CallToolRequest("missing", Collections.emptyMap());

        StepVerifier.create(processor.handle(exchange, request))
                .expectError(ProtocolErrorException.class)
                .verify();
    }

    @Test
    void testHandleSetLogLevelUpdatesExchange() throws Exception {
        RuntimeCapabilities.Async features = baseFeatures(Collections.emptyList(), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        Method handleSetLogLevel = AsyncRuntimeEngine.class.getDeclaredMethod("handleSetLogLevel");
        handleSetLogLevel.setAccessible(true);

        @SuppressWarnings("unchecked")
        RequestProcessor<Object> processor = (RequestProcessor<Object>) handleSetLogLevel.invoke(engine);

        RuntimeSession session = mock(RuntimeSession.class);
        RuntimeExchangeContext exchange = new RuntimeExchangeContext(
                "id", session, new ProtocolDefinition.ClientCapabilities(), SERVER_INFO, RuntimeContext.EMPTY);

        Map<String, Object> request = Collections.singletonMap("level", ProtocolDefinition.LoggingLevel.ERROR.name());

        StepVerifier.create(processor.handle(exchange, request))
                .expectNextMatches(map -> map instanceof Map)
                .verifyComplete();

        verify(session).setMinLoggingLevel(ProtocolDefinition.LoggingLevel.ERROR);
    }

    @Test
    void testNotifyToolsListChangedAndClose() {
        RuntimeCapabilities.Async features = baseFeatures(Collections.emptyList(), Collections.emptyList());
        AsyncRuntimeEngine engine = createEngine(features);

        when(mockTransportProvider.notifyClients(anyString(), any())).thenReturn(Mono.empty());
        when(mockTransportProvider.closeGracefully()).thenReturn(Mono.empty());

        StepVerifier.create(engine.notifyToolsListChanged()).verifyComplete();
        verify(mockTransportProvider)
                .notifyClients(eq(ProtocolDefinition.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED), isNull());

        StepVerifier.create(engine.closeGracefully()).verifyComplete();
        verify(mockTransportProvider).closeGracefully();

        engine.close();
        verify(mockTransportProvider).close();
    }

    @Test
    void testCreateRootsHandlerInvokesConsumers() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>> handler = (ex, roots) -> {
            invoked.set(true);
            assertEquals(1, roots.size());
            return Mono.empty();
        };

        RuntimeCapabilities.Async features =
                baseFeatures(Collections.singletonList(simpleTool("tool")), Collections.singletonList(handler));
        AsyncRuntimeEngine engine = createEngine(features);

        Field registryField = AsyncRuntimeEngine.class.getDeclaredField("notificationRegistry");
        registryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, NotificationProcessor> registry = (Map<String, NotificationProcessor>) registryField.get(engine);

        NotificationProcessor processor = registry.get(ProtocolDefinition.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED);
        assertNotNull(processor);

        RuntimeSession session = mock(RuntimeSession.class);
        ProtocolDefinition.ListRootsResult rootsResult = new ProtocolDefinition.ListRootsResult(
                new ArrayList<>(Collections.singletonList(new ProtocolDefinition.Root("uri", "name"))), null);
        when(session.sendRequest(eq(ProtocolDefinition.METHOD_ROOTS_LIST), any(), any()))
                .thenReturn(Mono.just(rootsResult));

        RuntimeExchangeContext exchange = new RuntimeExchangeContext(
                "id", session, new ProtocolDefinition.ClientCapabilities(), SERVER_INFO, RuntimeContext.EMPTY);

        StepVerifier.create(processor.handle(exchange, Collections.emptyMap())).verifyComplete();

        assertTrue(invoked.get());
    }
}
