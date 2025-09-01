/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.DeafaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * The Model Context Protocol (MCP) server implementation that provides asynchronous
 * communication using Project Reactor's Mono and Flux types.
 *
 * <p>
 * This server implements the MCP specification, enabling AI models to expose tools,
 * resources, and prompts through a standardized interface. Key features include:
 * <ul>
 * <li>Asynchronous communication using reactive programming patterns
 * <li>Dynamic tool registration and management
 * <li>Resource handling with URI-based addressing
 * <li>Prompt template management
 * <li>Real-time client notifications for state changes
 * <li>Structured logging with configurable severity levels
 * <li>Support for client-side AI model sampling
 * </ul>
 *
 * <p>
 * The server follows a lifecycle:
 * <ol>
 * <li>Initialization - Accepts client connections and negotiates capabilities
 * <li>Normal Operation - Handles client requests and sends notifications
 * <li>Graceful Shutdown - Ensures clean connection termination
 * </ol>
 *
 * <p>
 * This implementation uses Project Reactor for non-blocking operations, making it
 * suitable for high-throughput scenarios and reactive applications. All operations return
 * Mono or Flux types that can be composed into reactive pipelines.
 *
 * <p>
 * The server supports runtime modification of its capabilities through methods like
 * {@link #addTool}, {@link #addResource}, and {@link #addPrompt}, automatically notifying
 * connected clients of changes when configured to do so.
 *
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

    private final CopyOnWriteArrayList<ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap<String, McpServerFeatures.AsyncResourceSpecification> resources =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, McpServerFeatures.AsyncPromptSpecification> prompts =
            new ConcurrentHashMap<>();

    // FIXME: this field is deprecated and should be remvoed together with the
    // broadcasting loggingNotification.
    private LoggingLevel minLoggingLevel = LoggingLevel.DEBUG;

    private final ConcurrentHashMap<CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions =
            new ConcurrentHashMap<>();

    private List<String> protocolVersions;

    private McpUriTemplateManagerFactory uriTemplateManagerFactory = new DeafaultMcpUriTemplateManagerFactory();

    /**
     * Create a new McpAsyncServer with the given transport provider and capabilities.
     * @param mcpTransportProvider The transport layer implementation for MCP
     * communication.
     * @param features The MCP server supported features.
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
     */
    McpAsyncServer(
            McpServerTransportProvider mcpTransportProvider,
            ObjectMapper objectMapper,
            McpServerFeatures.Async features,
            Duration requestTimeout,
            McpUriTemplateManagerFactory uriTemplateManagerFactory,
            JsonSchemaValidator jsonSchemaValidator) {
        this.mcpTransportProvider = mcpTransportProvider;
        this.objectMapper = objectMapper;
        this.serverInfo = features.serverInfo();
        this.serverCapabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.tools.addAll(withStructuredOutputHandling(jsonSchemaValidator, features.tools()));
        this.resources.putAll(features.resources());
        this.resourceTemplates.addAll(features.resourceTemplates());
        this.prompts.putAll(features.prompts());
        this.completions.putAll(features.completions());
        this.uriTemplateManagerFactory = uriTemplateManagerFactory;
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
            McpUriTemplateManagerFactory uriTemplateManagerFactory,
            JsonSchemaValidator jsonSchemaValidator) {
        this.mcpTransportProvider = mcpTransportProvider;
        this.objectMapper = objectMapper;
        this.serverInfo = features.serverInfo();
        this.serverCapabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.tools.addAll(withStructuredOutputHandling(jsonSchemaValidator, features.tools()));
        this.resources.putAll(features.resources());
        this.resourceTemplates.addAll(features.resourceTemplates());
        this.prompts.putAll(features.prompts());
        this.completions.putAll(features.completions());
        this.uriTemplateManagerFactory = uriTemplateManagerFactory;
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

        // Initialize request handlers for standard MCP methods

        // Ping MUST respond with an empty data, but not NULL response.
        requestHandlers.put(McpSchema.METHOD_PING, (exchange, params) -> Mono.just(Collections.EMPTY_MAP));

        // Add tools API handlers if the tool capability is enabled
        if (this.serverCapabilities.tools() != null) {
            requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
        }

        // Add resources API handlers if provided
        if (this.serverCapabilities.resources() != null) {
            requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
        }

        // Add prompts API handlers if provider exists
        if (this.serverCapabilities.prompts() != null) {
            requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
        }

        // Add logging API handlers if the logging capability is enabled
        if (this.serverCapabilities.logging() != null) {
            requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
        }

        // Add completion API handlers if the completion capability is enabled
        if (this.serverCapabilities.completions() != null) {
            requestHandlers.put(McpSchema.METHOD_COMPLETION_COMPLETE, completionCompleteRequestHandler());
        }
        return requestHandlers;
    }

    // ---------------------------------------
    // Lifecycle Management
    // ---------------------------------------
    private Mono<InitializeResult> asyncInitializeRequestHandler(InitializeRequest initializeRequest) {
        return Mono.defer(() -> {
            logger.info(
                    "Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
                    initializeRequest.getProtocolVersion(),
                    initializeRequest.getCapabilities(),
                    initializeRequest.getClientInfo());

            // The server MUST respond with the highest protocol version it supports
            // if
            // it does not support the requested (e.g. Client) version.
            String serverProtocolVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

            if (this.protocolVersions.contains(initializeRequest.getProtocolVersion())) {
                // If the server supports the requested protocol version, it MUST
                // respond
                // with the same version.
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

    /**
     * Get the server capabilities that define the supported features and functionality.
     * @return The server capabilities
     */
    public ServerCapabilities getServerCapabilities() {
        return this.serverCapabilities;
    }

    /**
     * Get the server implementation information.
     * @return The server implementation details
     */
    public Implementation getServerInfo() {
        return this.serverInfo;
    }

    /**
     * Gracefully closes the server, allowing any in-progress operations to complete.
     * @return A Mono that completes when the server has been closed
     */
    public Mono<Void> closeGracefully() {
        return this.mcpTransportProvider.closeGracefully();
    }

    /**
     * Close the server immediately.
     */
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

    // ---------------------------------------
    // Tool Management
    // ---------------------------------------

    /**
     * Add a new tool call specification at runtime.
     * @param toolSpecification The tool specification to add
     * @return Mono that completes when clients have been notified of the change
     */
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
                    // Pass through. No validation is required if no output schema is
                    // provided.
                    return result;
                }

                // If an output schema is provided, servers MUST provide structured
                // results that conform to this schema.
                // https://modelcontextprotocol.io/specification/2025-06-18/server/tools#output-schema
                if (result.getStructuredContent() == null) {
                    logger.warn(
                            "Response missing structured content which is expected when calling tool with non-empty outputSchema");
                    return new CallToolResult(
                            "Response missing structured content which is expected when calling tool with non-empty outputSchema",
                            true);
                }

                // Validate the result against the output schema
                JsonSchemaValidator.ValidationResponse validation =
                        this.jsonSchemaValidator.validate(outputSchema, result.getStructuredContent());

                if (!validation.isValid()) {
                    logger.warn("Tool call result validation failed: {}", validation.getErrorMessage());
                    return new CallToolResult(validation.getErrorMessage(), true);
                }

                if (Utils.isEmpty(result.getContent())) {
                    // For backwards compatibility, a tool that returns structured
                    // content SHOULD also return functionally equivalent unstructured
                    // content. (For example, serialized JSON can be returned in a
                    // TextContent block.)
                    // https://modelcontextprotocol.io/specification/2025-06-18/server/tools#structured-content

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

    /**
     * Remove a tool handler at runtime.
     * @param toolName The name of the tool handler to remove
     * @return Mono that completes when clients have been notified of the change
     */
    public Mono<Void> removeTool(String toolName) {
        if (toolName == null) {
            return Mono.error(new McpError("Tool name must not be null"));
        }
        if (this.serverCapabilities.tools() == null) {
            return Mono.error(new McpError("Server must be configured with tool capabilities"));
        }

        return Mono.defer(() -> {
            boolean removed = this.tools.removeIf(
                    toolSpecification -> toolSpecification.tool().getName().equals(toolName));
            if (removed) {
                logger.debug("Removed tool handler: {}", toolName);
                if (this.serverCapabilities.tools().listChanged()) {
                    return notifyToolsListChanged();
                }
                return Mono.empty();
            }
            return Mono.error(new McpError("Tool with name '" + toolName + "' not found"));
        });
    }

    /**
     * Notifies clients that the list of available tools has changed.
     * @return A Mono that completes when all clients have been notified
     */
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

    // ---------------------------------------
    // Resource Management
    // ---------------------------------------

    /**
     * Add a new resource handler at runtime.
     * @param resourceSpecification The resource handler to add
     * @return Mono that completes when clients have been notified of the change
     */
    public Mono<Void> addResource(McpServerFeatures.AsyncResourceSpecification resourceSpecification) {
        if (resourceSpecification == null || resourceSpecification.resource() == null) {
            return Mono.error(new McpError("Resource must not be null"));
        }

        if (this.serverCapabilities.resources() == null) {
            return Mono.error(new McpError("Server must be configured with resource capabilities"));
        }

        return Mono.defer(() -> {
            if (this.resources.putIfAbsent(resourceSpecification.resource().getUri(), resourceSpecification) != null) {
                return Mono.error(new McpError(
                        "Resource with URI '" + resourceSpecification.resource().getUri() + "' already exists"));
            }
            logger.debug(
                    "Added resource handler: {}",
                    resourceSpecification.resource().getUri());
            if (this.serverCapabilities.resources().listChanged()) {
                return notifyResourcesListChanged();
            }
            return Mono.empty();
        });
    }

    /**
     * Remove a resource handler at runtime.
     * @param resourceUri The URI of the resource handler to remove
     * @return Mono that completes when clients have been notified of the change
     */
    public Mono<Void> removeResource(String resourceUri) {
        if (resourceUri == null) {
            return Mono.error(new McpError("Resource URI must not be null"));
        }
        if (this.serverCapabilities.resources() == null) {
            return Mono.error(new McpError("Server must be configured with resource capabilities"));
        }

        return Mono.defer(() -> {
            McpServerFeatures.AsyncResourceSpecification removed = this.resources.remove(resourceUri);
            if (removed != null) {
                logger.debug("Removed resource handler: {}", resourceUri);
                if (this.serverCapabilities.resources().listChanged()) {
                    return notifyResourcesListChanged();
                }
                return Mono.empty();
            }
            return Mono.error(new McpError("Resource with URI '" + resourceUri + "' not found"));
        });
    }

    /**
     * Notifies clients that the list of available resources has changed.
     * @return A Mono that completes when all clients have been notified
     */
    public Mono<Void> notifyResourcesListChanged() {
        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED, null);
    }

    /**
     * Notifies clients that the resources have updated.
     * @return A Mono that completes when all clients have been notified
     */
    public Mono<Void> notifyResourcesUpdated(ResourcesUpdatedNotification resourcesUpdatedNotification) {
        return this.mcpTransportProvider.notifyClients(
                McpSchema.METHOD_NOTIFICATION_RESOURCES_UPDATED, resourcesUpdatedNotification);
    }

    private McpRequestHandler<ListResourcesResult> resourcesListRequestHandler() {
        return (exchange, params) -> {
            List<Resource> resourceList = this.resources.values().stream()
                    .map(McpServerFeatures.AsyncResourceSpecification::resource)
                    .collect(Collectors.toList());
            return Mono.just(new ListResourcesResult(resourceList, null));
        };
    }

    private McpRequestHandler<ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
        return (exchange, params) -> Mono.just(new ListResourceTemplatesResult(null, this.getResourceTemplates()));
    }

    private List<ResourceTemplate> getResourceTemplates() {
        ArrayList<ResourceTemplate> list = new ArrayList<>(this.resourceTemplates);
        List<ResourceTemplate> resourceTemplates = this.resources.keySet().stream()
                .filter(uri -> uri.contains("{"))
                .map(uri -> {
                    Resource resource = this.resources.get(uri).resource();
                    ResourceTemplate template = new ResourceTemplate(
                            resource.getUri(),
                            resource.getName(),
                            resource.getDescription(),
                            resource.getMimeType(),
                            resource.getAnnotations());
                    return template;
                })
                .collect(Collectors.toList());

        list.addAll(resourceTemplates);

        return list;
    }

    private McpRequestHandler<ReadResourceResult> resourcesReadRequestHandler() {
        return (exchange, params) -> {
            ReadResourceRequest resourceRequest =
                    objectMapper.convertValue(params, new TypeReference<ReadResourceRequest>() {});
            String resourceUri = resourceRequest.getUri();

            McpServerFeatures.AsyncResourceSpecification specification = this.resources.values().stream()
                    .filter(resourceSpecification -> this.uriTemplateManagerFactory
                            .create(resourceSpecification.resource().getUri())
                            .matches(resourceUri))
                    .findFirst()
                    .orElseThrow(() -> new McpError("Resource not found: " + resourceUri));

            return Mono.defer(() -> specification.readHandler().apply(exchange, resourceRequest));
        };
    }

    // ---------------------------------------
    // Prompt Management
    // ---------------------------------------

    /**
     * Add a new prompt handler at runtime.
     * @param promptSpecification The prompt handler to add
     * @return Mono that completes when clients have been notified of the change
     */
    public Mono<Void> addPrompt(McpServerFeatures.AsyncPromptSpecification promptSpecification) {
        if (promptSpecification == null) {
            return Mono.error(new McpError("Prompt specification must not be null"));
        }
        if (this.serverCapabilities.prompts() == null) {
            return Mono.error(new McpError("Server must be configured with prompt capabilities"));
        }

        return Mono.defer(() -> {
            McpServerFeatures.AsyncPromptSpecification specification =
                    this.prompts.putIfAbsent(promptSpecification.prompt().getName(), promptSpecification);
            if (specification != null) {
                return Mono.error(new McpError(
                        "Prompt with name '" + promptSpecification.prompt().getName() + "' already exists"));
            }

            logger.debug(
                    "Added prompt handler: {}", promptSpecification.prompt().getName());

            // Servers that declared the listChanged capability SHOULD send a
            // notification,
            // when the list of available prompts changes
            if (this.serverCapabilities.prompts().listChanged()) {
                return notifyPromptsListChanged();
            }
            return Mono.empty();
        });
    }

    /**
     * Remove a prompt handler at runtime.
     * @param promptName The name of the prompt handler to remove
     * @return Mono that completes when clients have been notified of the change
     */
    public Mono<Void> removePrompt(String promptName) {
        if (promptName == null) {
            return Mono.error(new McpError("Prompt name must not be null"));
        }
        if (this.serverCapabilities.prompts() == null) {
            return Mono.error(new McpError("Server must be configured with prompt capabilities"));
        }

        return Mono.defer(() -> {
            McpServerFeatures.AsyncPromptSpecification removed = this.prompts.remove(promptName);

            if (removed != null) {
                logger.debug("Removed prompt handler: {}", promptName);
                // Servers that declared the listChanged capability SHOULD send a
                // notification, when the list of available prompts changes
                if (this.serverCapabilities.prompts().listChanged()) {
                    return this.notifyPromptsListChanged();
                }
                return Mono.empty();
            }
            return Mono.error(new McpError("Prompt with name '" + promptName + "' not found"));
        });
    }

    /**
     * Notifies clients that the list of available prompts has changed.
     * @return A Mono that completes when all clients have been notified
     */
    public Mono<Void> notifyPromptsListChanged() {
        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED, null);
    }

    private McpRequestHandler<ListPromptsResult> promptsListRequestHandler() {
        return (exchange, params) -> {
            // TODO: Implement pagination
            // McpSchema.PaginatedRequest request = objectMapper.convertValue(params,
            // new TypeReference<McpSchema.PaginatedRequest>() {
            // });

            List<Prompt> promptList = this.prompts.values().stream()
                    .map(McpServerFeatures.AsyncPromptSpecification::prompt)
                    .collect(Collectors.toList());

            return Mono.just(new ListPromptsResult(promptList, null));
        };
    }

    private McpRequestHandler<GetPromptResult> promptsGetRequestHandler() {
        return (exchange, params) -> {
            GetPromptRequest promptRequest =
                    objectMapper.convertValue(params, new TypeReference<GetPromptRequest>() {});

            // Implement prompt retrieval logic here
            McpServerFeatures.AsyncPromptSpecification specification = this.prompts.get(promptRequest.getName());
            if (specification == null) {
                return Mono.error(new McpError("Prompt not found: " + promptRequest.getName()));
            }

            return Mono.defer(() -> specification.promptHandler().apply(exchange, promptRequest));
        };
    }

    // ---------------------------------------
    // Logging Management
    // ---------------------------------------

    /**
     * This implementation would, incorrectly, broadcast the logging message to all
     * connected clients, using a single minLoggingLevel for all of them. Similar to the
     * sampling and roots, the logging level should be set per client session and use the
     * ServerExchange to send the logging message to the right client.
     * @param loggingMessageNotification The logging message to send
     * @return A Mono that completes when the notification has been sent
     * @deprecated Use
     * {@link McpAsyncServerExchange#loggingNotification(LoggingMessageNotification)}
     * instead.
     */
    @Deprecated
    public Mono<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {

        if (loggingMessageNotification == null) {
            return Mono.error(new McpError("Logging message must not be null"));
        }

        if (loggingMessageNotification.getLevel().level() < minLoggingLevel.level()) {
            return Mono.empty();
        }

        return this.mcpTransportProvider.notifyClients(
                McpSchema.METHOD_NOTIFICATION_MESSAGE, loggingMessageNotification);
    }

    private McpRequestHandler<Object> setLoggerRequestHandler() {
        return (exchange, params) -> {
            return Mono.defer(() -> {
                SetLevelRequest newMinLoggingLevel =
                        objectMapper.convertValue(params, new TypeReference<SetLevelRequest>() {});

                exchange.setMinLoggingLevel(newMinLoggingLevel.getLevel());

                // FIXME: this field is deprecated and should be removed together
                // with the broadcasting loggingNotification.
                this.minLoggingLevel = newMinLoggingLevel.getLevel();

                return Mono.just(Collections.EMPTY_MAP);
            });
        };
    }

    private McpRequestHandler<CompleteResult> completionCompleteRequestHandler() {
        return (exchange, params) -> {
            CompleteRequest request = parseCompletionParams(params);

            if (request.ref() == null) {
                return Mono.error(new McpError("ref must not be null"));
            }

            if (request.ref().type() == null) {
                return Mono.error(new McpError("type must not be null"));
            }

            String type = request.ref().type();

            String argumentName = request.argument().name();

            // check if the referenced resource exists
            if (type.equals("ref/prompt") && request.ref() instanceof PromptReference) {
                PromptReference promptReference = (PromptReference) request.ref();
                McpServerFeatures.AsyncPromptSpecification promptSpec = this.prompts.get(promptReference.getName());
                if (promptSpec == null) {
                    return Mono.error(new McpError("Prompt not found: " + promptReference.getName()));
                }
                if (promptSpec.prompt().getArguments().stream()
                        .noneMatch(arg -> arg.getName().equals(argumentName))) {

                    return Mono.error(new McpError("Argument not found: " + argumentName));
                }
            }

            if (type.equals("ref/resource") && request.ref() instanceof ResourceReference) {
                ResourceReference resourceReference = (ResourceReference) request.ref();
                McpServerFeatures.AsyncResourceSpecification resourceSpec =
                        this.resources.get(resourceReference.getUri());
                if (resourceSpec == null) {
                    return Mono.error(new McpError("Resource not found: " + resourceReference.getUri()));
                }
                if (!uriTemplateManagerFactory
                        .create(resourceSpec.resource().getUri())
                        .getVariableNames()
                        .contains(argumentName)) {
                    return Mono.error(new McpError("Argument not found: " + argumentName));
                }
            }

            McpServerFeatures.AsyncCompletionSpecification specification = this.completions.get(request.ref());

            if (specification == null) {
                return Mono.error(new McpError("AsyncCompletionSpecification not found: " + request.ref()));
            }

            return Mono.defer(() -> specification.completionHandler().apply(exchange, request));
        };
    }

    /**
     * Parses the raw JSON-RPC request parameters into a {@link CompleteRequest}
     * object.
     * <p>
     * This method manually extracts the `ref` and `argument` fields from the input map,
     * determines the correct reference type (either prompt or resource), and constructs a
     * fully-typed {@code CompleteRequest} instance.
     * @param object the raw request parameters, expected to be a Map containing "ref" and
     * "argument" entries.
     * @return a {@link CompleteRequest} representing the structured completion
     * request.
     * @throws IllegalArgumentException if the "ref" type is not recognized.
     */
    @SuppressWarnings("unchecked")
    private CompleteRequest parseCompletionParams(Object object) {
        Map<String, Object> params = (Map<String, Object>) object;
        Map<String, Object> refMap = (Map<String, Object>) params.get("ref");
        Map<String, Object> argMap = (Map<String, Object>) params.get("argument");
        Map<String, Object> contextMap = (Map<String, Object>) params.get("context");
        Map<String, Object> meta = (Map<String, Object>) params.get("_meta");

        String refType = (String) refMap.get("type");

        CompleteReference ref;
        switch (refType) {
            case "ref/prompt":
                String name = (String) refMap.get("name");
                String title = refMap.get("title") != null ? (String) refMap.get("title") : null;
                ref = new PromptReference(refType, name, title);
                break;
            case "ref/resource":
                String uri = (String) refMap.get("uri");
                ref = new ResourceReference(refType, uri);
                break;
            default:
                throw new IllegalArgumentException("Invalid ref type: " + refType);
        }

        String argName = (String) argMap.get("name");
        String argValue = (String) argMap.get("value");
        CompleteRequest.CompleteArgument argument = new CompleteRequest.CompleteArgument(argName, argValue);

        CompleteRequest.CompleteContext context = null;
        if (contextMap != null) {
            Map<String, String> arguments = (Map<String, String>) contextMap.get("arguments");
            context = new CompleteRequest.CompleteContext(arguments);
        }

        return new CompleteRequest(ref, argument, meta, context);
    }

    /**
     * This method is package-private and used for test only. Should not be called by user
     * code.
     * @param protocolVersions the Client supported protocol versions.
     */
    void setProtocolVersions(List<String> protocolVersions) {
        this.protocolVersions = protocolVersions;
    }
}
