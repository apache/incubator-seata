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
package org.apache.seata.mcp.core.runtimeCore;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.apache.seata.mcp.core.protocol.BaseTransportProvider;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.ProtocolError;
import org.apache.seata.mcp.core.protocol.RuntimeTransport;
import org.apache.seata.mcp.core.protocol.RuntimeTransportProvider;
import org.apache.seata.mcp.core.protocol.SchemaValidator;
import org.apache.seata.mcp.core.protocol.ServerRuntimeSession;
import org.apache.seata.mcp.core.protocol.StandardStreamableSessionFactory;
import org.apache.seata.mcp.core.protocol.StreamableRuntimeTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Async runtime engine.
 */
public class AsyncRuntimeEngine {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRuntimeEngine.class);

    private final BaseTransportProvider transport;
    private final ObjectMapper mapper;
    private final SchemaValidator validator;
    private final ProtocolDefinition.ServerCapabilities capabilities;
    private final ProtocolDefinition.Implementation info;
    private final String instructions;
    private final CopyOnWriteArrayList<RuntimeCapabilities.AsyncToolSpecification> toolRegistry =
            new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<
                    ProtocolDefinition.CompleteReference, RuntimeCapabilities.AsyncCompletionSpecification>
            completionRegistry = new ConcurrentHashMap<>();
    private final Map<String, RequestProcessor<?>> requestRegistry = new HashMap<>();
    private final Map<String, NotificationProcessor> notificationRegistry = new HashMap<>();
    private ProtocolDefinition.LoggingLevel loggingLevel = ProtocolDefinition.LoggingLevel.DEBUG;
    private List<String> versions;

    AsyncRuntimeEngine(
            RuntimeTransportProvider provider,
            ObjectMapper mapper,
            RuntimeCapabilities.Async features,
            Duration timeout,
            SchemaValidator validator) {
        this.transport = provider;
        this.mapper = mapper;
        this.validator = validator;
        this.info = features.serverInfo();
        this.capabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.versions = provider.protocolVersions();

        this.toolRegistry.addAll(wrapTools(validator, features.tools()));
        this.completionRegistry.putAll(features.completions());

        setupHandlers(features);
        provider.setSessionFactory(t -> createSession(t, timeout));
    }

    AsyncRuntimeEngine(
            StreamableRuntimeTransportProvider provider,
            ObjectMapper mapper,
            RuntimeCapabilities.Async features,
            Duration timeout,
            SchemaValidator validator) {
        this.transport = provider;
        this.mapper = mapper;
        this.validator = validator;
        this.info = features.serverInfo();
        this.capabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.versions = provider.protocolVersions();

        this.toolRegistry.addAll(wrapTools(validator, features.tools()));
        this.completionRegistry.putAll(features.completions());

        setupHandlers(features);
        provider.setSessionFactory(
                new StandardStreamableSessionFactory(timeout, this::handleInit, requestRegistry, notificationRegistry));
    }

    private void setupHandlers(RuntimeCapabilities.Async features) {
        requestRegistry.put(ProtocolDefinition.METHOD_PING, (e, p) -> Mono.just(Collections.emptyMap()));

        if (capabilities.tools() != null) {
            requestRegistry.put(ProtocolDefinition.METHOD_TOOLS_LIST, this.handleToolsList());
            requestRegistry.put(ProtocolDefinition.METHOD_TOOLS_CALL, this.handleToolCall());
        }

        if (capabilities.logging() != null) {
            requestRegistry.put(ProtocolDefinition.METHOD_LOGGING_SET_LEVEL, this.handleSetLogLevel());
        }

        notificationRegistry.put(ProtocolDefinition.METHOD_NOTIFICATION_INITIALIZED, (e, p) -> Mono.empty());

        List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>> rootHandlers =
                features.rootsChangeConsumers();
        if (RuntimeUtils.isEmpty(rootHandlers)) {
            rootHandlers = Collections.singletonList(
                    (e, r) -> Mono.fromRunnable(() -> logger.warn("Roots changed but no handler: {}", r)));
        }
        notificationRegistry.put(
                ProtocolDefinition.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED, createRootsHandler(rootHandlers));
    }

    private ServerRuntimeSession createSession(RuntimeTransport t, Duration timeout) {
        return new ServerRuntimeSession(
                UUID.randomUUID().toString(), timeout, t, this::handleInit, requestRegistry, notificationRegistry);
    }

    private Mono<ProtocolDefinition.InitializeResult> handleInit(ProtocolDefinition.InitializeRequest req) {
        logger.info(
                "Initialize: version={}, clientInfo={}",
                req.getProtocolVersion(),
                JSON.toJSONString(req.getClientInfo()));

        String version = versions.contains(req.getProtocolVersion())
                ? req.getProtocolVersion()
                : versions.get(versions.size() - 1);

        if (!versions.contains(req.getProtocolVersion())) {
            logger.warn("Unsupported version {}, using {}", req.getProtocolVersion(), version);
        }

        return Mono.just(new ProtocolDefinition.InitializeResult(version, capabilities, info, instructions));
    }

    private NotificationProcessor createRootsHandler(
            List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>> handlers) {
        return (exchange, params) -> exchange.listRoots().flatMap(result -> Flux.fromIterable(handlers)
                .flatMap(h -> Mono.defer(() -> h.apply(exchange, result.getRoots()))
                        .onErrorResume(err -> {
                            logger.error("Roots handler error", err);
                            return Mono.empty();
                        }))
                .then());
    }

    public Mono<Void> addTool(RuntimeCapabilities.AsyncToolSpecification spec) {
        if (spec == null || spec.tool() == null || (spec.call() == null && spec.callHandler() == null)) {
            return Mono.error(new ProtocolError("Invalid tool specification"));
        }
        if (capabilities.tools() == null) {
            return Mono.error(new ProtocolError("Tools not enabled"));
        }

        RuntimeCapabilities.AsyncToolSpecification wrapped = wrapTool(validator, spec);

        return Mono.defer(() -> {
            if (toolRegistry.stream()
                    .anyMatch(t -> t.tool().getName().equals(wrapped.tool().getName()))) {
                return Mono.error(new ProtocolError(
                        "Tool already exists: " + wrapped.tool().getName()));
            }

            toolRegistry.add(wrapped);
            logger.debug("Tool registered: {}", wrapped.tool().getName());

            return capabilities.tools().listChanged() ? notifyToolsListChanged() : Mono.empty();
        });
    }

    private static List<RuntimeCapabilities.AsyncToolSpecification> wrapTools(
            SchemaValidator validator, List<RuntimeCapabilities.AsyncToolSpecification> tools) {
        return RuntimeUtils.isEmpty(tools)
                ? tools
                : tools.stream().map(t -> wrapTool(validator, t)).collect(Collectors.toList());
    }

    private static RuntimeCapabilities.AsyncToolSpecification wrapTool(
            SchemaValidator validator, RuntimeCapabilities.AsyncToolSpecification spec) {
        if (spec.callHandler() instanceof ToolOutputValidator) {
            return spec;
        }
        if (spec.tool().getOutputSchema() == null) {
            return spec;
        }

        return RuntimeCapabilities.AsyncToolSpecification.builder()
                .tool(spec.tool())
                .callHandler(new ToolOutputValidator(validator, spec.tool().getOutputSchema(), spec.callHandler()))
                .build();
    }

    private static class ToolOutputValidator
            implements BiFunction<
                    RuntimeExchangeContext,
                    ProtocolDefinition.CallToolRequest,
                    Mono<ProtocolDefinition.CallToolResult>> {
        private final SchemaValidator validator;
        private final Map<String, Object> schema;
        private final BiFunction<
                        RuntimeExchangeContext,
                        ProtocolDefinition.CallToolRequest,
                        Mono<ProtocolDefinition.CallToolResult>>
                delegate;

        ToolOutputValidator(
                SchemaValidator validator,
                Map<String, Object> schema,
                BiFunction<
                                RuntimeExchangeContext,
                                ProtocolDefinition.CallToolRequest,
                                Mono<ProtocolDefinition.CallToolResult>>
                        delegate) {
            RuntimeUtils.notNull(validator, "Validator required");
            RuntimeUtils.notNull(delegate, "Handler required");
            this.validator = validator;
            this.schema = schema;
            this.delegate = delegate;
        }

        @Override
        public Mono<ProtocolDefinition.CallToolResult> apply(
                RuntimeExchangeContext exchange, ProtocolDefinition.CallToolRequest request) {
            return delegate.apply(exchange, request).map(result -> {
                if (schema == null) {
                    if (result.getStructuredContent() != null) {
                        logger.warn("Unexpected structured content without schema");
                    }
                    return result;
                }

                if (result.getStructuredContent() == null) {
                    logger.warn("Missing structured content");
                    return new ProtocolDefinition.CallToolResult("Missing structured content", true);
                }

                SchemaValidator.ValidationResponse validation =
                        validator.validate(schema, result.getStructuredContent());
                if (!validation.isValid()) {
                    logger.warn("Validation failed: {}", validation.getErrorMessage());
                    return new ProtocolDefinition.CallToolResult(validation.getErrorMessage(), true);
                }

                if (RuntimeUtils.isEmpty(result.getContent())) {
                    return new ProtocolDefinition.CallToolResult(
                            Collections.singletonList(
                                    new ProtocolDefinition.TextContent(validation.getJsonStructuredOutput())),
                            result.getError(),
                            result.getStructuredContent());
                }

                return result;
            });
        }
    }

    public Mono<Void> notifyToolsListChanged() {
        return transport.notifyClients(ProtocolDefinition.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
    }

    private RequestProcessor<ProtocolDefinition.ListToolsResult> handleToolsList() {
        return (exchange, params) -> {
            List<ProtocolDefinition.Tool> tools = toolRegistry.stream()
                    .map(RuntimeCapabilities.AsyncToolSpecification::tool)
                    .collect(Collectors.toList());
            return Mono.just(new ProtocolDefinition.ListToolsResult(tools, null));
        };
    }

    private RequestProcessor<ProtocolDefinition.CallToolResult> handleToolCall() {
        return (exchange, params) -> {
            ProtocolDefinition.CallToolRequest req =
                    mapper.convertValue(params, new TypeReference<ProtocolDefinition.CallToolRequest>() {});

            return toolRegistry.stream()
                    .filter(t -> t.tool().getName().equals(req.getName()))
                    .findFirst()
                    .map(t -> Mono.defer(() -> t.callHandler().apply(exchange, req)))
                    .orElse(Mono.error(new ProtocolError("Tool not found: " + req.getName())));
        };
    }

    private RequestProcessor<Object> handleSetLogLevel() {
        return (exchange, params) -> {
            ProtocolDefinition.SetLevelRequest req =
                    mapper.convertValue(params, new TypeReference<ProtocolDefinition.SetLevelRequest>() {});
            exchange.setMinLoggingLevel(req.getLevel());
            this.loggingLevel = req.getLevel();
            return Mono.just(Collections.emptyMap());
        };
    }

    public Mono<Void> closeGracefully() {
        return transport.closeGracefully();
    }

    public void close() {
        transport.close();
    }
}
