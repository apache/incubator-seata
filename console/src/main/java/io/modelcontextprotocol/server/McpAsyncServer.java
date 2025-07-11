/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.util.UriTemplate;
import io.modelcontextprotocol.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * The Model Context Protocol (MCP) server implementation that provides asynchronous
 */
public class McpAsyncServer {

	private static final Logger logger = LoggerFactory.getLogger(McpAsyncServer.class);

	private final McpAsyncServer delegate;

	McpAsyncServer() {
		this.delegate = null;
	}

	/**
	 * Create a new McpAsyncServer with the given transport provider and capabilities.
	 * @param mcpTransportProvider The transport layer implementation for MCP
	 * communication.
	 * @param features The MCP server supported features.
	 * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
	 */
	McpAsyncServer(McpServerTransportProvider mcpTransportProvider, ObjectMapper objectMapper,
			McpServerFeatures.Async features) {
		this.delegate = new AsyncServerImpl(mcpTransportProvider, objectMapper, features);
	}

	/**
	 * Get the server capabilities that define the supported features and functionality.
	 * @return The server capabilities
	 */
	public ServerCapabilities getServerCapabilities() {
		return this.delegate.getServerCapabilities();
	}

	/**
	 * Get the server implementation information.
	 * @return The server implementation details
	 */
	public Implementation getServerInfo() {
		return this.delegate.getServerInfo();
	}





	/**
	 * Gracefully closes the server, allowing any in-progress operations to complete.
	 * @return A Mono that completes when the server has been closed
	 */
	public Mono<Void> closeGracefully() {
		return this.delegate.closeGracefully();
	}

	/**
	 * Close the server immediately.
	 */
	public void close() {
		this.delegate.close();
	}







	/**
	 * Add a new tool specification at runtime.
	 * @param toolSpecification The tool specification to add
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> addTool(McpServerFeatures.AsyncToolSpecification toolSpecification) {
		return this.delegate.addTool(toolSpecification);
	}

	/**
	 * Remove a tool handler at runtime.
	 * @param toolName The name of the tool handler to remove
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> removeTool(String toolName) {
		return this.delegate.removeTool(toolName);
	}

	/**
	 * Notifies clients that the list of available tools has changed.
	 * @return A Mono that completes when all clients have been notified
	 */
	public Mono<Void> notifyToolsListChanged() {
		return this.delegate.notifyToolsListChanged();
	}

	// ---------------------------------------
	// Resource Management
	// ---------------------------------------



	/**
	 * Add a new resource handler at runtime.
	 * @param resourceHandler The resource handler to add
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> addResource(McpServerFeatures.AsyncResourceSpecification resourceHandler) {
		return this.delegate.addResource(resourceHandler);
	}

	/**
	 * Remove a resource handler at runtime.
	 * @param resourceUri The URI of the resource handler to remove
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> removeResource(String resourceUri) {
		return this.delegate.removeResource(resourceUri);
	}

	/**
	 * Notifies clients that the list of available resources has changed.
	 * @return A Mono that completes when all clients have been notified
	 */
	public Mono<Void> notifyResourcesListChanged() {
		return this.delegate.notifyResourcesListChanged();
	}


	// ---------------------------------------
	// Resource Template Management
	// ---------------------------------------
	/**
	 * Add a new resource template handler at runtime.
	 * @param resourceHandler The resource template handler to add
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> addResourceTemplate(McpServerFeatures.AsyncResourceTemplateSpecification resourceHandler) {
		return this.delegate.addResourceTemplate(resourceHandler);
	}

	/**
	 * Remove a resource template handler at runtime.
	 * @param resourceUri The URI of the resource template handler to remove
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> removeResourceTemplate(String resourceUri) {
		return this.delegate.removeResourceTemplate(resourceUri);
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
		return this.delegate.addPrompt(promptSpecification);
	}

	/**
	 * Remove a prompt handler at runtime.
	 * @param promptName The name of the prompt handler to remove
	 * @return Mono that completes when clients have been notified of the change
	 */
	public Mono<Void> removePrompt(String promptName) {
		return this.delegate.removePrompt(promptName);
	}

	/**
	 * Notifies clients that the list of available prompts has changed.
	 * @return A Mono that completes when all clients have been notified
	 */
	public Mono<Void> notifyPromptsListChanged() {
		return this.delegate.notifyPromptsListChanged();
	}

	// ---------------------------------------
	// Logging Management
	// ---------------------------------------

	/**
	 * Send a logging message notification to all connected clients. Messages below the
	 * current minimum logging level will be filtered out.
	 * @param loggingMessageNotification The logging message to send
	 * @return A Mono that completes when the notification has been sent
	 */
	public Mono<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {
		return this.delegate.loggingNotification(loggingMessageNotification);
	}

	// ---------------------------------------
	// Sampling
	// ---------------------------------------



	/**
	 * This method is package-private and used for test only. Should not be called by user
	 * code.
	 * @param protocolVersions the Client supported protocol versions.
	 */
	void setProtocolVersions(List<String> protocolVersions) {
		this.delegate.setProtocolVersions(protocolVersions);
	}

	private static class AsyncServerImpl extends McpAsyncServer {

		private final McpServerTransportProvider mcpTransportProvider;

		private final ObjectMapper objectMapper;

		private final ServerCapabilities serverCapabilities;

		private final Implementation serverInfo;

		private final CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> tools = new CopyOnWriteArrayList<>();

		private final ConcurrentHashMap<UriTemplate, McpServerFeatures.AsyncResourceTemplateSpecification> resourceTemplates = new ConcurrentHashMap<>();

		private final ConcurrentHashMap<String, McpServerFeatures.AsyncResourceSpecification> resources = new ConcurrentHashMap<>();

		private final ConcurrentHashMap<String, McpServerFeatures.AsyncPromptSpecification> prompts = new ConcurrentHashMap<>();

		private LoggingLevel minLoggingLevel = LoggingLevel.DEBUG;

		private List<String> protocolVersions = Collections.singletonList(McpSchema.LATEST_PROTOCOL_VERSION);

		AsyncServerImpl(McpServerTransportProvider mcpTransportProvider, ObjectMapper objectMapper,
				McpServerFeatures.Async features) {
			this.mcpTransportProvider = mcpTransportProvider;
			this.objectMapper = objectMapper;
			this.serverInfo = features.getServerInfo();
			this.serverCapabilities = features.getServerCapabilities();
			this.tools.addAll(features.getTools());
			this.resources.putAll(features.getResources());
			this.resourceTemplates.putAll(features.getResourceTemplates());
			this.prompts.putAll(features.getPrompts());

			Map<String, McpServerSession.RequestHandler<?>> requestHandlers = new HashMap<>();

			// Initialize request handlers for standard MCP methods

			// Ping MUST respond with an empty data, but not NULL response.
			requestHandlers.put(McpSchema.METHOD_PING, (exchange, params) -> Mono.just(Collections.emptyMap()));

			// Add tools API handlers if the tool capability is enabled
			if (this.serverCapabilities.getTools() != null) {
				requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
				requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
			}

			// Add resources API handlers if provided
			if (this.serverCapabilities.getResources() != null) {
				requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
				requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
				requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
			}

			// Add prompts API handlers if provider exists
			if (this.serverCapabilities.getPrompts() != null) {
				requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
				requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
			}

			// Add logging API handlers if the logging capability is enabled
			if (this.serverCapabilities.getLogging() != null) {
				requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
			}

			Map<String, McpServerSession.NotificationHandler> notificationHandlers = new HashMap<>();

			notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, (exchange, params) -> Mono.empty());

			List<BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>>> rootsChangeConsumers = features
				.getRootsChangeConsumers();

			if (Utils.isEmpty(rootsChangeConsumers)) {
				rootsChangeConsumers = Collections.singletonList((exchange,
                                                                  roots) -> Mono.fromRunnable(() -> logger.warn(
                        "Roots list changed notification, but no consumers provided. Roots list changed: {}",
                        roots)));
			}

			notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED,
					asyncRootsListChangedNotificationHandler(rootsChangeConsumers));

			mcpTransportProvider
				.setSessionFactory(transport -> new McpServerSession(UUID.randomUUID().toString(), transport,
						this::asyncInitializeRequestHandler, Mono::empty, requestHandlers, notificationHandlers));
		}

		// ---------------------------------------
		// Lifecycle Management
		// ---------------------------------------
		private Mono<InitializeResult> asyncInitializeRequestHandler(
				InitializeRequest initializeRequest) {
			return Mono.defer(() -> {
				logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
						initializeRequest.getProtocolVersion(), initializeRequest.getCapabilities(),
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
				}
				else {
					logger.warn(
							"Client requested unsupported protocol version: {}, so the server will sugggest the {} version instead",
							initializeRequest.getProtocolVersion(), serverProtocolVersion);
				}

				return Mono.just(new InitializeResult(serverProtocolVersion, this.serverCapabilities,
						this.serverInfo, null));
			});
		}

		public ServerCapabilities getServerCapabilities() {
			return this.serverCapabilities;
		}

		public Implementation getServerInfo() {
			return this.serverInfo;
		}





		@Override
		public Mono<Void> closeGracefully() {
			return this.mcpTransportProvider.closeGracefully();
		}

		@Override
		public void close() {
			this.mcpTransportProvider.close();
		}




		// ---------------------------------------
		// Tool Management
		// ---------------------------------------

		@Override
		public Mono<Void> addTool(McpServerFeatures.AsyncToolSpecification toolSpecification) {
			if (toolSpecification == null) {
				return Mono.error(new McpError("Tool specification must not be null"));
			}
			if (toolSpecification.getTool() == null) {
				return Mono.error(new McpError("Tool must not be null"));
			}
			if (toolSpecification.getCall() == null) {
				return Mono.error(new McpError("Tool call handler must not be null"));
			}
			if (this.serverCapabilities.getTools() == null) {
				return Mono.error(new McpError("Server must be configured with tool capabilities"));
			}

			return Mono.defer(() -> {
				// Check for duplicate tool names
				if (this.tools.stream().anyMatch(th -> th.getTool().getName().equals(toolSpecification.getTool().getName()))) {
					return Mono
						.error(new McpError("Tool with name '" + toolSpecification.getTool().getName() + "' already exists"));
				}

				this.tools.add(toolSpecification);
				logger.debug("Added tool handler: {}", toolSpecification.getTool().getName());

				if (this.serverCapabilities.getTools().getListChanged()) {
					return notifyToolsListChanged();
				}
				return Mono.empty();
			});
		}


		@Override
		public Mono<Void> removeTool(String toolName) {
			if (toolName == null) {
				return Mono.error(new McpError("Tool name must not be null"));
			}
			if (this.serverCapabilities.getTools() == null) {
				return Mono.error(new McpError("Server must be configured with tool capabilities"));
			}

			return Mono.defer(() -> {
				boolean removed = this.tools
					.removeIf(toolSpecification -> toolSpecification.getTool().getName().equals(toolName));
				if (removed) {
					logger.debug("Removed tool handler: {}", toolName);
					if (this.serverCapabilities.getTools().getListChanged()) {
						return notifyToolsListChanged();
					}
					return Mono.empty();
				}
				return Mono.error(new McpError("Tool with name '" + toolName + "' not found"));
			});
		}

		@Override
		public Mono<Void> notifyToolsListChanged() {
			return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
		}

		private McpServerSession.RequestHandler<ListToolsResult> toolsListRequestHandler() {
			return (exchange, params) -> {
				List<Tool> tools = this.tools.stream().map(McpServerFeatures.AsyncToolSpecification::getTool).collect(Collectors.toList());

				return Mono.just(new ListToolsResult(tools, null));
			};
		}

		private McpServerSession.RequestHandler<CallToolResult> toolsCallRequestHandler() {
			return (exchange, params) -> {
				CallToolRequest callToolRequest = objectMapper.convertValue(params,
						new TypeReference<CallToolRequest>() {
						});

				Optional<McpServerFeatures.AsyncToolSpecification> toolSpecification = this.tools.stream()
					.filter(tr -> callToolRequest.getName().equals(tr.getTool().getName()))
					.findAny();

				if (!toolSpecification.isPresent()) {
					return Mono.error(new McpError("Tool not found: " + callToolRequest.getName()));
				}

				return toolSpecification.map(tool -> tool.getCall().apply(exchange, callToolRequest.getArguments()))
					.orElse(Mono.error(new McpError("Tool not found: " + callToolRequest.getName())));
			};
		}


		// ---------------------------------------
		// Resource Template Management
		// ---------------------------------------

		@Override
		public Mono<Void> addResourceTemplate(
				McpServerFeatures.AsyncResourceTemplateSpecification resourceTemplateSpecification) {
			if (resourceTemplateSpecification == null || resourceTemplateSpecification.getResource() == null) {
				return Mono.error(new McpError("Resource template must not be null"));
			}

			if (this.serverCapabilities.getResources() == null) {
				return Mono.error(new McpError("Server must be configured with resource capabilities"));
			}

			return Mono.defer(() -> {
				if (this.resourceTemplates.putIfAbsent(
						new UriTemplate(resourceTemplateSpecification.getResource().getUriTemplate()),
						resourceTemplateSpecification) != null) {
					return Mono.error(new McpError("Resource template with URI Template'"
							+ resourceTemplateSpecification.getResource().getUriTemplate() + "' already exists"));
				}
				logger.debug("Added resource template handler: {}",
						resourceTemplateSpecification.getResource().getUriTemplate());
				if (this.serverCapabilities.getResources().getListChanged()) {
					return notifyResourcesListChanged();
				}
				return Mono.empty();
			});
		}

		@Override
		public Mono<Void> removeResourceTemplate(String resourceUriTemplate) {
			if (resourceUriTemplate == null) {
				return Mono.error(new McpError("Resource Template URI must not be null"));
			}
			if (this.serverCapabilities.getResources() == null) {
				return Mono.error(new McpError("Server must be configured with resource capabilities"));
			}

			return Mono.defer(() -> {
				// Lookup the key in the map using the UriTemplate
				McpServerFeatures.AsyncResourceTemplateSpecification removed = this.resourceTemplates
						.remove(new UriTemplate(resourceUriTemplate));
				if (removed != null) {
					logger.debug("Removed resource template handler: {}", resourceUriTemplate);
					if (this.serverCapabilities.getResources().getListChanged()) {
						return notifyResourcesListChanged();
					}
					return Mono.empty();
				}
				return Mono
						.error(new McpError("Resource template with URI template '" + resourceUriTemplate + "' not found"));
			});
		}

		private McpServerSession.RequestHandler<ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
			return (exchange, params) -> {
				List<ResourceTemplate> resourceList = this.resourceTemplates.values()
						.stream()
						.map(McpServerFeatures.AsyncResourceTemplateSpecification::getResource)
						.collect(Collectors.toList());
				return Mono.just(new ListResourceTemplatesResult(null,resourceList));
			};
		}

		// ---------------------------------------
		// Resource Management
		// ---------------------------------------

		@Override
		public Mono<Void> addResource(McpServerFeatures.AsyncResourceSpecification resourceSpecification) {
			if (resourceSpecification == null || resourceSpecification.getResource() == null) {
				return Mono.error(new McpError("Resource must not be null"));
			}

			if (this.serverCapabilities.getResources() == null) {
				return Mono.error(new McpError("Server must be configured with resource capabilities"));
			}

			return Mono.defer(() -> {
				if (this.resources.putIfAbsent(resourceSpecification.getResource().getUri(), resourceSpecification) != null) {
					return Mono.error(new McpError(
							"Resource with URI '" + resourceSpecification.getResource().getUri() + "' already exists"));
				}
				logger.debug("Added resource handler: {}", resourceSpecification.getResource().getUri());
				if (this.serverCapabilities.getResources().getListChanged()) {
					return notifyResourcesListChanged();
				}
				return Mono.empty();
			});
		}



		@Override
		public Mono<Void> removeResource(String resourceUri) {
			if (resourceUri == null) {
				return Mono.error(new McpError("Resource URI must not be null"));
			}
			if (this.serverCapabilities.getResources() == null) {
				return Mono.error(new McpError("Server must be configured with resource capabilities"));
			}

			return Mono.defer(() -> {
				McpServerFeatures.AsyncResourceSpecification removed = this.resources.remove(resourceUri);
				if (removed != null) {
					logger.debug("Removed resource handler: {}", resourceUri);
					if (this.serverCapabilities.getResources().getListChanged()) {
						return notifyResourcesListChanged();
					}
					return Mono.empty();
				}
				return Mono.error(new McpError("Resource with URI '" + resourceUri + "' not found"));
			});
		}

		@Override
		public Mono<Void> notifyResourcesListChanged() {
			return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED, null);
		}

		private McpServerSession.RequestHandler<ListResourcesResult> resourcesListRequestHandler() {
			return (exchange, params) -> {
				List<Resource> resourceList = this.resources.values()
					.stream()
					.map(McpServerFeatures.AsyncResourceSpecification::getResource)
					.collect(Collectors.toList());
				return Mono.just(new ListResourcesResult(resourceList, null));
			};
		}

		private McpServerSession.RequestHandler<ReadResourceResult> resourcesReadRequestHandler() {
			return (exchange, params) -> {
				ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
						new TypeReference<ReadResourceRequest>() {
						});
				String resourceUri = resourceRequest.getUri();
				McpServerFeatures.AsyncResourceSpecification specification = this.resources.get(resourceUri);
				if (specification != null) {
					return specification.getReadHandler().apply(exchange, resourceRequest);
				}

				// If the resource is not found, we can check if it is a template
				for (Map.Entry<UriTemplate, McpServerFeatures.AsyncResourceTemplateSpecification> entry : this.resourceTemplates.entrySet()) {
					UriTemplate resourceUriTemplate = entry.getKey();
					if (resourceUriTemplate.matchesTemplate(resourceUri)) {
						McpServerFeatures.AsyncResourceTemplateSpecification spec = entry.getValue();
						return spec.getReadHandler().apply(exchange, resourceRequest);
					}
				}
				return Mono.error(new McpError("Resource not found: " + resourceUri));
			};
		}

		// ---------------------------------------
		// Prompt Management
		// ---------------------------------------

		@Override
		public Mono<Void> addPrompt(McpServerFeatures.AsyncPromptSpecification promptSpecification) {
			if (promptSpecification == null) {
				return Mono.error(new McpError("Prompt specification must not be null"));
			}
			if (this.serverCapabilities.getPrompts() == null) {
				return Mono.error(new McpError("Server must be configured with prompt capabilities"));
			}

			return Mono.defer(() -> {
				McpServerFeatures.AsyncPromptSpecification specification = this.prompts
					.putIfAbsent(promptSpecification.getPrompt().getName(), promptSpecification);
				if (specification != null) {
					return Mono.error(new McpError(
							"Prompt with name '" + promptSpecification.getPrompt().getName() + "' already exists"));
				}

				logger.debug("Added prompt handler: {}", promptSpecification.getPrompt().getName());

				// Servers that declared the listChanged capability SHOULD send a
				// notification,
				// when the list of available prompts changes
				if (this.serverCapabilities.getPrompts().getListChanged()) {
					return notifyPromptsListChanged();
				}
				return Mono.empty();
			});
		}

		private McpServerSession.NotificationHandler asyncRootsListChangedNotificationHandler(
				List<BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>>> rootsChangeConsumers) {
			return (exchange, params) -> exchange.listRoots()
					.flatMap(listRootsResult -> Flux.fromIterable(rootsChangeConsumers)
							.flatMap(consumer -> consumer.apply(exchange, listRootsResult.getRoots()))
							.onErrorResume(error -> {
								logger.error("Error handling roots list change notification", error);
								return Mono.empty();
							})
							.then());
		}


		@Override
		public Mono<Void> removePrompt(String promptName) {
			if (promptName == null) {
				return Mono.error(new McpError("Prompt name must not be null"));
			}
			if (this.serverCapabilities.getPrompts() == null) {
				return Mono.error(new McpError("Server must be configured with prompt capabilities"));
			}

			return Mono.defer(() -> {
				McpServerFeatures.AsyncPromptSpecification removed = this.prompts.remove(promptName);

				if (removed != null) {
					logger.debug("Removed prompt handler: {}", promptName);
					// Servers that declared the listChanged capability SHOULD send a
					// notification, when the list of available prompts changes
					if (this.serverCapabilities.getPrompts().getListChanged()) {
						return this.notifyPromptsListChanged();
					}
					return Mono.empty();
				}
				return Mono.error(new McpError("Prompt with name '" + promptName + "' not found"));
			});
		}

		@Override
		public Mono<Void> notifyPromptsListChanged() {
			return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED, null);
		}

		private McpServerSession.RequestHandler<ListPromptsResult> promptsListRequestHandler() {
			return (exchange, params) -> {
				// TODO: Implement pagination
				// McpSchema.PaginatedRequest request = objectMapper.convertValue(params,
				// new TypeReference<McpSchema.PaginatedRequest>() {
				// });

				List<Prompt> promptList = this.prompts.values()
					.stream()
					.map(McpServerFeatures.AsyncPromptSpecification::getPrompt)
					.collect(Collectors.toList());

				return Mono.just(new ListPromptsResult(promptList, null));
			};
		}

		private McpServerSession.RequestHandler<GetPromptResult> promptsGetRequestHandler() {
			return (exchange, params) -> {
				GetPromptRequest promptRequest = objectMapper.convertValue(params,
						new TypeReference<GetPromptRequest>() {
						});

				// Implement prompt retrieval logic here
				McpServerFeatures.AsyncPromptSpecification specification = this.prompts.get(promptRequest.getName());
				if (specification == null) {
					return Mono.error(new McpError("Prompt not found: " + promptRequest.getName()));
				}

				return specification.getPromptHandler().apply(exchange, promptRequest);
			};
		}

		// ---------------------------------------
		// Logging Management
		// ---------------------------------------

		@Override
		public Mono<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {

			if (loggingMessageNotification == null) {
				return Mono.error(new McpError("Logging message must not be null"));
			}

			Map<String, Object> params = this.objectMapper.convertValue(loggingMessageNotification,
					new TypeReference<Map<String, Object>>() {
					});

			if (loggingMessageNotification.getLevel().level() < minLoggingLevel.level()) {
				return Mono.empty();
			}

			return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_MESSAGE, params);
		}

		private McpServerSession.RequestHandler<Void> setLoggerRequestHandler() {
			return (exchange, params) -> {
				this.minLoggingLevel = objectMapper.convertValue(params, new TypeReference<LoggingLevel>() {
				});

				return Mono.empty();
			};
		}

		// ---------------------------------------
		// Sampling
		// ---------------------------------------

		@Override
		void setProtocolVersions(List<String> protocolVersions) {
			this.protocolVersions = protocolVersions;
		}

	}
}
