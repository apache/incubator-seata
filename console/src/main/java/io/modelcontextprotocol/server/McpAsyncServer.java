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

/*
 * ------------------------------------------------------------------------
 * This file contains code originally from the [Model Context Protocol Java SDK],
 * which is licensed under the MIT License.
 *
 * Modifications made by [Seata]:
 *   - Adapted code from Java 17 features to Java 8 compatible syntax
 *
 * The original MIT license text is reproduced below:
 * ------------------------------------------------------------------------
 */

/*
 * MIT License
 * Copyright (c) 2025 the original author or authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.modelcontextprotocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.DefaultMcpStreamableServerSessionFactory;
import io.modelcontextprotocol.spec.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CompleteReference;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.InitializeRequest;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.Root;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.SetLevelRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProviderBase;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 * @author Jihoon Kim
 * @see McpServer
 * @see McpSchema
 */
public class McpAsyncServer {

    private static final Logger logger = LoggerFactory.getLogger(McpAsyncServer.class);

    private final McpServerTransportProviderBase mcpTransportProvider;

    private final ObjectMapper objectMapper;

    private final JsonSchemaValidator jsonSchemaValidator;

    private final ServerCapabilities serverCapabilities;

    private final Implementation serverInfo;

    private final String instructions;

    private final CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> tools = new CopyOnWriteArrayList<>();

    private LoggingLevel minLoggingLevel = LoggingLevel.DEBUG;

    private final ConcurrentHashMap<CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions =
            new ConcurrentHashMap<>();

    private List<String> protocolVersions;

    McpAsyncServer(
            McpServerTransportProvider mcpTransportProvider,
            ObjectMapper objectMapper,
            McpServerFeatures.Async features,
            Duration requestTimeout,
            JsonSchemaValidator jsonSchemaValidator) {
        this.mcpTransportProvider = mcpTransportProvider;
        this.objectMapper = objectMapper;
        this.serverInfo = features.serverInfo();
        this.serverCapabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.tools.addAll(withStructuredOutputHandling(jsonSchemaValidator, features.tools()));
        this.completions.putAll(features.completions());
        this.jsonSchemaValidator = jsonSchemaValidator;

        Map<String, McpRequestHandler<?>> requestHandlers = prepareRequestHandlers();
        Map<String, McpNotificationHandler> notificationHandlers = prepareNotificationHandlers(features);

        this.protocolVersions = mcpTransportProvider.protocolVersions();

        mcpTransportProvider.setSessionFactory(transport -> new McpServerSession(
                UUID.randomUUID().toString(),
                requestTimeout,
                transport,
                this::asyncInitializeRequestHandler,
                requestHandlers,
                notificationHandlers));
    }

    McpAsyncServer(
            McpStreamableServerTransportProvider mcpTransportProvider,
            ObjectMapper objectMapper,
            McpServerFeatures.Async features,
            Duration requestTimeout,
            JsonSchemaValidator jsonSchemaValidator) {
        this.mcpTransportProvider = mcpTransportProvider;
        this.objectMapper = objectMapper;
        this.serverInfo = features.serverInfo();
        this.serverCapabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.tools.addAll(withStructuredOutputHandling(jsonSchemaValidator, features.tools()));
        this.completions.putAll(features.completions());
        this.jsonSchemaValidator = jsonSchemaValidator;

        Map<String, McpRequestHandler<?>> requestHandlers = prepareRequestHandlers();
        Map<String, McpNotificationHandler> notificationHandlers = prepareNotificationHandlers(features);

        this.protocolVersions = mcpTransportProvider.protocolVersions();

        mcpTransportProvider.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(
                requestTimeout, this::asyncInitializeRequestHandler, requestHandlers, notificationHandlers));
    }

    private Map<String, McpNotificationHandler> prepareNotificationHandlers(McpServerFeatures.Async features) {
        Map<String, McpNotificationHandler> notificationHandlers = new HashMap<>();

        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, (exchange, params) -> Mono.empty());

        List<BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>>> rootsChangeConsumers =
                features.rootsChangeConsumers();

        if (Utils.isEmpty(rootsChangeConsumers)) {
            rootsChangeConsumers = Collections.singletonList((exchange, roots) -> Mono.fromRunnable(() -> logger.warn(
                    "Roots list changed notification, but no consumers provided. Roots list changed: {}", roots)));
        }

        notificationHandlers.put(
                McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED,
                asyncRootsListChangedNotificationHandler(rootsChangeConsumers));
        return notificationHandlers;
    }

    private Map<String, McpRequestHandler<?>> prepareRequestHandlers() {
        Map<String, McpRequestHandler<?>> requestHandlers = new HashMap<>();

        requestHandlers.put(McpSchema.METHOD_PING, (exchange, params) -> Mono.just(Collections.EMPTY_MAP));

        if (this.serverCapabilities.tools() != null) {
            requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
        }

        if (this.serverCapabilities.logging() != null) {
            requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
        }

        return requestHandlers;
    }

    private Mono<InitializeResult> asyncInitializeRequestHandler(InitializeRequest initializeRequest) {
        return Mono.defer(() -> {
            logger.info(
                    "Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
                    initializeRequest.getProtocolVersion(),
                    initializeRequest.getCapabilities(),
                    initializeRequest.getClientInfo());

            String serverProtocolVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

            if (this.protocolVersions.contains(initializeRequest.getProtocolVersion())) {
                serverProtocolVersion = initializeRequest.getProtocolVersion();
            } else {
                logger.warn(
                        "Client requested unsupported protocol version: {}, so the server will suggest the {} version instead",
                        initializeRequest.getProtocolVersion(),
                        serverProtocolVersion);
            }

            return Mono.just(new InitializeResult(
                    serverProtocolVersion, this.serverCapabilities, this.serverInfo, this.instructions));
        });
    }

    public Mono<Void> closeGracefully() {
        return this.mcpTransportProvider.closeGracefully();
    }

    public void close() {
        this.mcpTransportProvider.close();
    }

    private McpNotificationHandler asyncRootsListChangedNotificationHandler(
            List<BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>>> rootsChangeConsumers) {
        return (exchange, params) -> exchange.listRoots()
                .flatMap(listRootsResult -> Flux.fromIterable(rootsChangeConsumers)
                        .flatMap(consumer -> Mono.defer(() -> consumer.apply(exchange, listRootsResult.getRoots())))
                        .onErrorResume(error -> {
                            logger.error("Error handling roots list change notification", error);
                            return Mono.empty();
                        })
                        .then());
    }

    public Mono<Void> addTool(McpServerFeatures.AsyncToolSpecification toolSpecification) {
        if (toolSpecification == null) {
            return Mono.error(new McpError("Tool specification must not be null"));
        }
        if (toolSpecification.tool() == null) {
            return Mono.error(new McpError("Tool must not be null"));
        }
        if (toolSpecification.call() == null && toolSpecification.callHandler() == null) {
            return Mono.error(new McpError("Tool call handler must not be null"));
        }
        if (this.serverCapabilities.tools() == null) {
            return Mono.error(new McpError("Server must be configured with tool capabilities"));
        }

        McpServerFeatures.AsyncToolSpecification wrappedToolSpecification =
                withStructuredOutputHandling(this.jsonSchemaValidator, toolSpecification);

        return Mono.defer(() -> {
            // Check for duplicate tool names
            if (this.tools.stream().anyMatch(th -> th.tool()
                    .getName()
                    .equals(wrappedToolSpecification.tool().getName()))) {
                return Mono.error(new McpError(
                        "Tool with name '" + wrappedToolSpecification.tool().getName() + "' already exists"));
            }

            this.tools.add(wrappedToolSpecification);
            logger.debug(
                    "Added tool handler: {}", wrappedToolSpecification.tool().getName());

            if (this.serverCapabilities.tools().listChanged()) {
                return notifyToolsListChanged();
            }
            return Mono.empty();
        });
    }

    private static class StructuredOutputCallToolHandler
            implements BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> {

        private final BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> delegateCallToolResult;

        private final JsonSchemaValidator jsonSchemaValidator;

        private final Map<String, Object> outputSchema;

        public StructuredOutputCallToolHandler(
                JsonSchemaValidator jsonSchemaValidator,
                Map<String, Object> outputSchema,
                BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> delegateHandler) {

            Assert.notNull(jsonSchemaValidator, "JsonSchemaValidator must not be null");
            Assert.notNull(delegateHandler, "Delegate call tool result handler must not be null");

            this.delegateCallToolResult = delegateHandler;
            this.outputSchema = outputSchema;
            this.jsonSchemaValidator = jsonSchemaValidator;
        }

        @Override
        public Mono<CallToolResult> apply(McpAsyncServerExchange exchange, CallToolRequest request) {

            return this.delegateCallToolResult.apply(exchange, request).map(result -> {
                if (outputSchema == null) {
                    if (result.getStructuredContent() != null) {
                        logger.warn(
                                "Tool call with no outputSchema is not expected to have a result with structured content, but got: {}",
                                result.getStructuredContent());
                    }
                    return result;
                }

                if (result.getStructuredContent() == null) {
                    logger.warn(
                            "Response missing structured content which is expected when calling tool with non-empty outputSchema");
                    return new CallToolResult(
                            "Response missing structured content which is expected when calling tool with non-empty outputSchema",
                            true);
                }

                JsonSchemaValidator.ValidationResponse validation =
                        this.jsonSchemaValidator.validate(outputSchema, result.getStructuredContent());

                if (!validation.isValid()) {
                    logger.warn("Tool call result validation failed: {}", validation.getErrorMessage());
                    return new CallToolResult(validation.getErrorMessage(), true);
                }

                if (Utils.isEmpty(result.getContent())) {
                    return new CallToolResult(
                            Collections.singletonList(new TextContent(validation.getJsonStructuredOutput())),
                            result.getError(),
                            result.getStructuredContent());
                }

                return result;
            });
        }
    }

    private static List<McpServerFeatures.AsyncToolSpecification> withStructuredOutputHandling(
            JsonSchemaValidator jsonSchemaValidator, List<McpServerFeatures.AsyncToolSpecification> tools) {

        if (Utils.isEmpty(tools)) {
            return tools;
        }

        return tools.stream()
                .map(tool -> withStructuredOutputHandling(jsonSchemaValidator, tool))
                .collect(Collectors.toList());
    }

    private static McpServerFeatures.AsyncToolSpecification withStructuredOutputHandling(
            JsonSchemaValidator jsonSchemaValidator, McpServerFeatures.AsyncToolSpecification toolSpecification) {

        if (toolSpecification.callHandler() instanceof StructuredOutputCallToolHandler) {
            // If the tool is already wrapped, return it as is
            return toolSpecification;
        }

        if (toolSpecification.tool().getOutputSchema() == null) {
            // If the tool does not have an output schema, return it as is
            return toolSpecification;
        }

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSpecification.tool())
                .callHandler(new StructuredOutputCallToolHandler(
                        jsonSchemaValidator,
                        toolSpecification.tool().getOutputSchema(),
                        toolSpecification.callHandler()))
                .build();
    }

    public Mono<Void> notifyToolsListChanged() {
        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
    }

    private McpRequestHandler<ListToolsResult> toolsListRequestHandler() {
        return (exchange, params) -> {
            List<Tool> tools = this.tools.stream()
                    .map(McpServerFeatures.AsyncToolSpecification::tool)
                    .collect(Collectors.toList());

            return Mono.just(new ListToolsResult(tools, null));
        };
    }

    private McpRequestHandler<CallToolResult> toolsCallRequestHandler() {
        return (exchange, params) -> {
            CallToolRequest callToolRequest =
                    objectMapper.convertValue(params, new TypeReference<CallToolRequest>() {});

            Optional<McpServerFeatures.AsyncToolSpecification> toolSpecification = this.tools.stream()
                    .filter(tr -> callToolRequest.getName().equals(tr.tool().getName()))
                    .findAny();

            if (!toolSpecification.isPresent()) {
                return Mono.error(new McpError("Tool not found: " + callToolRequest.getName()));
            }

            return toolSpecification
                    .map(tool -> Mono.defer(() -> tool.callHandler().apply(exchange, callToolRequest)))
                    .orElse(Mono.error(new McpError("Tool not found: " + callToolRequest.getName())));
        };
    }

    private McpRequestHandler<Object> setLoggerRequestHandler() {
        return (exchange, params) -> Mono.defer(() -> {
            SetLevelRequest newMinLoggingLevel =
                    objectMapper.convertValue(params, new TypeReference<SetLevelRequest>() {});

            exchange.setMinLoggingLevel(newMinLoggingLevel.getLevel());
            this.minLoggingLevel = newMinLoggingLevel.getLevel();

            return Mono.just(Collections.EMPTY_MAP);
        });
    }
}
