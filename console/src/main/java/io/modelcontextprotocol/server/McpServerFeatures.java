/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.UriTemplate;
import io.modelcontextprotocol.util.Utils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * MCP server features specification that a particular server can choose to support.
 */
public class McpServerFeatures {

    /**
     * Asynchronous server features specification.
     */
    public static class Async {
        McpSchema.Implementation serverInfo;
        McpSchema.ServerCapabilities serverCapabilities;
        List<AsyncToolSpecification> tools;
        Map<String, AsyncResourceSpecification> resources;
        Map<UriTemplate, AsyncResourceTemplateSpecification> resourceTemplates;
        Map<String, AsyncPromptSpecification> prompts;
        List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers;

        @Override
        public String toString() {
            return "Async{" + "serverInfo="
                    + serverInfo + ", serverCapabilities="
                    + serverCapabilities + ", tools="
                    + tools + ", resources="
                    + resources + ", resourceTemplates="
                    + resourceTemplates + ", prompts="
                    + prompts + ", rootsChangeConsumers="
                    + rootsChangeConsumers + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Async async = (Async) o;
            return Objects.equals(serverInfo, async.serverInfo)
                    && Objects.equals(serverCapabilities, async.serverCapabilities)
                    && Objects.equals(tools, async.tools)
                    && Objects.equals(resources, async.resources)
                    && Objects.equals(resourceTemplates, async.resourceTemplates)
                    && Objects.equals(prompts, async.prompts)
                    && Objects.equals(rootsChangeConsumers, async.rootsChangeConsumers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts, rootsChangeConsumers);
        }

        public McpSchema.Implementation getServerInfo() {
            return serverInfo;
        }

        public void setServerInfo(McpSchema.Implementation serverInfo) {
            this.serverInfo = serverInfo;
        }

        public McpSchema.ServerCapabilities getServerCapabilities() {
            return serverCapabilities;
        }

        public void setServerCapabilities(McpSchema.ServerCapabilities serverCapabilities) {
            this.serverCapabilities = serverCapabilities;
        }

        public List<AsyncToolSpecification> getTools() {
            return tools;
        }

        public void setTools(List<AsyncToolSpecification> tools) {
            this.tools = tools;
        }

        public Map<String, AsyncResourceSpecification> getResources() {
            return resources;
        }

        public void setResources(Map<String, AsyncResourceSpecification> resources) {
            this.resources = resources;
        }

        public Map<UriTemplate, AsyncResourceTemplateSpecification> getResourceTemplates() {
            return resourceTemplates;
        }

        public void setResourceTemplates(Map<UriTemplate, AsyncResourceTemplateSpecification> resourceTemplates) {
            this.resourceTemplates = resourceTemplates;
        }

        public Map<String, AsyncPromptSpecification> getPrompts() {
            return prompts;
        }

        public void setPrompts(Map<String, AsyncPromptSpecification> prompts) {
            this.prompts = prompts;
        }

        public List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> getRootsChangeConsumers() {
            return rootsChangeConsumers;
        }

        public void setRootsChangeConsumers(
                List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers) {
            this.rootsChangeConsumers = rootsChangeConsumers;
        }

        /**
         * Create an instance and validate the arguments.
         * @param serverInfo The server implementation details
         * @param serverCapabilities The server capabilities
         * @param tools The list of tool specifications
         * @param resources The map of resource specifications
         * @param resourceTemplates The list of resource templates
         * @param prompts The map of prompt specifications
         * @param rootsChangeConsumers The list of consumers that will be notified when
         * the roots list changes
         */
        Async(
                McpSchema.Implementation serverInfo,
                McpSchema.ServerCapabilities serverCapabilities,
                List<AsyncToolSpecification> tools,
                Map<String, AsyncResourceSpecification> resources,
                Map<UriTemplate, AsyncResourceTemplateSpecification> resourceTemplates,
                Map<String, AsyncPromptSpecification> prompts,
                List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers) {

            Assert.notNull(serverInfo, "Server info must not be null");

            this.serverInfo = serverInfo;
            this.serverCapabilities = (serverCapabilities != null)
                    ? serverCapabilities
                    : new McpSchema.ServerCapabilities(
                            null, // experimental
                            new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
                            // logging
                            // by
                            // default
                            !Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
                            !Utils.isEmpty(resources)
                                    ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false)
                                    : null,
                            !Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

            this.tools = (tools != null) ? tools : Collections.emptyList();
            this.resources = (resources != null) ? resources : Collections.emptyMap();
            this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : Collections.emptyMap();
            this.prompts = (prompts != null) ? prompts : Collections.emptyMap();
            this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : Collections.emptyList();
        }

        /**
         * Convert a synchronous specification into an asynchronous one and provide
         * blocking code offloading to prevent accidental blocking of the non-blocking
         * transport.
         * @param syncSpec a potentially blocking, synchronous specification.
         * @return a specification which is protected from blocking calls specified by the
         * user.
         */
        static Async fromSync(Sync syncSpec) {
            List<AsyncToolSpecification> tools = new ArrayList<>();
            for (SyncToolSpecification tool : syncSpec.getTools()) {
                tools.add(AsyncToolSpecification.fromSync(tool));
            }

            Map<String, AsyncResourceSpecification> resources = new HashMap<>();
            syncSpec.getResources().forEach((key, resource) -> {
                resources.put(key, AsyncResourceSpecification.fromSync(resource));
            });

            Map<UriTemplate, AsyncResourceTemplateSpecification> resourceTemplates = new HashMap<>();
            syncSpec.getResourceTemplates().forEach((key, resource) -> {
                resourceTemplates.put(new UriTemplate(key), AsyncResourceTemplateSpecification.fromSync(resource));
            });

            Map<String, AsyncPromptSpecification> prompts = new HashMap<>();
            syncSpec.getPrompts().forEach((key, prompt) -> {
                prompts.put(key, AsyncPromptSpecification.fromSync(prompt));
            });

            List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers =
                    new ArrayList<>();

            for (BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> rootChangeConsumer :
                    syncSpec.getRootsChangeConsumers()) {
                rootChangeConsumers.add((exchange, list) -> Mono.<Void>fromRunnable(
                                () -> rootChangeConsumer.accept(new McpSyncServerExchange(exchange), list))
                        .subscribeOn(Schedulers.boundedElastic()));
            }

            return new Async(
                    syncSpec.getServerInfo(),
                    syncSpec.getServerCapabilities(),
                    tools,
                    resources,
                    resourceTemplates,
                    prompts,
                    rootChangeConsumers);
        }
    }

    public static class Sync {
        McpSchema.Implementation serverInfo;
        McpSchema.ServerCapabilities serverCapabilities;
        List<SyncToolSpecification> tools;
        Map<String, SyncResourceSpecification> resources;
        Map<String, SyncResourceTemplateSpecification> resourceTemplates;
        Map<String, SyncPromptSpecification> prompts;
        List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers;

        @Override
        public String toString() {
            return "Sync{" + "serverInfo="
                    + serverInfo + ", serverCapabilities="
                    + serverCapabilities + ", tools="
                    + tools + ", resources="
                    + resources + ", resourceTemplates="
                    + resourceTemplates + ", prompts="
                    + prompts + ", rootsChangeConsumers="
                    + rootsChangeConsumers + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Sync sync = (Sync) o;
            return Objects.equals(serverInfo, sync.serverInfo)
                    && Objects.equals(serverCapabilities, sync.serverCapabilities)
                    && Objects.equals(tools, sync.tools)
                    && Objects.equals(resources, sync.resources)
                    && Objects.equals(resourceTemplates, sync.resourceTemplates)
                    && Objects.equals(prompts, sync.prompts)
                    && Objects.equals(rootsChangeConsumers, sync.rootsChangeConsumers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts, rootsChangeConsumers);
        }

        public McpSchema.Implementation getServerInfo() {
            return serverInfo;
        }

        public void setServerInfo(McpSchema.Implementation serverInfo) {
            this.serverInfo = serverInfo;
        }

        public McpSchema.ServerCapabilities getServerCapabilities() {
            return serverCapabilities;
        }

        public void setServerCapabilities(McpSchema.ServerCapabilities serverCapabilities) {
            this.serverCapabilities = serverCapabilities;
        }

        public List<SyncToolSpecification> getTools() {
            return tools;
        }

        public void setTools(List<SyncToolSpecification> tools) {
            this.tools = tools;
        }

        public Map<String, SyncResourceSpecification> getResources() {
            return resources;
        }

        public void setResources(Map<String, SyncResourceSpecification> resources) {
            this.resources = resources;
        }

        public Map<String, SyncResourceTemplateSpecification> getResourceTemplates() {
            return resourceTemplates;
        }

        public void setResourceTemplates(Map<String, SyncResourceTemplateSpecification> resourceTemplates) {
            this.resourceTemplates = resourceTemplates;
        }

        public Map<String, SyncPromptSpecification> getPrompts() {
            return prompts;
        }

        public void setPrompts(Map<String, SyncPromptSpecification> prompts) {
            this.prompts = prompts;
        }

        public List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> getRootsChangeConsumers() {
            return rootsChangeConsumers;
        }

        public void setRootsChangeConsumers(
                List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers) {
            this.rootsChangeConsumers = rootsChangeConsumers;
        }

        /**
         * Create an instance and validate the arguments.
         * @param serverInfo The server implementation details
         * @param serverCapabilities The server capabilities
         * @param tools The list of tool specifications
         * @param resources The map of resource specifications
         * @param resourceTemplates The list of resource templates
         * @param prompts The map of prompt specifications
         * @param rootsChangeConsumers The list of consumers that will be notified when
         * the roots list changes
         */
        Sync(
                McpSchema.Implementation serverInfo,
                McpSchema.ServerCapabilities serverCapabilities,
                List<SyncToolSpecification> tools,
                Map<String, SyncResourceSpecification> resources,
                Map<String, SyncResourceTemplateSpecification> resourceTemplates,
                Map<String, SyncPromptSpecification> prompts,
                List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers) {

            Assert.notNull(serverInfo, "Server info must not be null");

            this.serverInfo = serverInfo;
            this.serverCapabilities = (serverCapabilities != null)
                    ? serverCapabilities
                    : new McpSchema.ServerCapabilities(
                            null, // experimental
                            new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
                            // logging
                            // by
                            // default
                            !Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
                            !Utils.isEmpty(resources)
                                    ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false)
                                    : null,
                            !Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

            this.tools = (tools != null) ? tools : new ArrayList<>();
            this.resources = (resources != null) ? resources : new HashMap<>();
            this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : new HashMap<>();
            this.prompts = (prompts != null) ? prompts : new HashMap<>();
            this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : new ArrayList<>();
        }
    }

    /**
     * Specification of a tool with its asynchronous handler function. Tools are the
     * primary way for MCP servers to expose functionality to AI models. Each tool
     * represents a specific capability, such as:
     * <ul>
     * <li>Performing calculations
     * <li>Accessing external APIs
     * <li>Querying databases
     * <li>Manipulating files
     * <li>Executing system commands
     * </ul>
     *
     * <p>
     * Example tool specification: <pre>{@code
     * new McpServerFeatures.AsyncToolSpecification(
     *     new Tool(
     *         "calculator",
     *         "Performs mathematical calculations",
     *         new JsonSchemaObject()
     *             .required("expression")
     *             .property("expression", JsonSchemaType.STRING)
     *     ),
     *     (exchange, args) -> {
     *         String expr = (String) args.get("expression");
     *         return Mono.fromSupplier(() -> evaluate(expr))
     *             .map(result -> new CallToolResult("Result: " + result));
     *     }
     * )
     * }</pre>
     *
     * returning results. The function's first argument is an
     * {@link McpAsyncServerExchange} upon which the server can interact with the
     * connected client. The second arguments is a map of tool arguments.
     */
    public static class AsyncToolSpecification {
        McpSchema.Tool tool;
        BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call;

        @Override
        public String toString() {
            return "AsyncToolSpecification{" + "tool=" + tool + ", call=" + call + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AsyncToolSpecification that = (AsyncToolSpecification) o;
            return Objects.equals(tool, that.tool) && Objects.equals(call, that.call);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tool, call);
        }

        public McpSchema.Tool getTool() {
            return tool;
        }

        public void setTool(McpSchema.Tool tool) {
            this.tool = tool;
        }

        public BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> getCall() {
            return call;
        }

        public void setCall(
                BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
            this.call = call;
        }

        public AsyncToolSpecification() {}

        public AsyncToolSpecification(
                McpSchema.Tool tool,
                BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
            this.tool = tool;
            this.call = call;
        }

        public static AsyncToolSpecification fromSync(SyncToolSpecification tool) {
            // FIXME: This is temporary, proper validation should be implemented
            if (tool == null) {
                return null;
            }
            return new AsyncToolSpecification(tool.getTool(), (exchange, map) -> Mono.fromCallable(
                            () -> tool.getCall().apply(new McpSyncServerExchange(exchange), map))
                    .subscribeOn(Schedulers.boundedElastic()));
        }
    }

    /**
     * Specification of a resource with its asynchronous handler function. Resources
     * provide context to AI models by exposing data such as:
     * <ul>
     * <li>File contents
     * <li>Database records
     * <li>API responses
     * <li>System information
     * <li>Application state
     * </ul>
     *
     * <p>
     * Example resource specification: <pre>{@code
     * new McpServerFeatures.AsyncResourceSpecification(
     *     new Resource("docs", "Documentation files", "text/markdown"),
     *     (exchange, request) ->
     *         Mono.fromSupplier(() -> readFile(request.getPath()))
     *             .map(ReadResourceResult::new)
     * )
     * }</pre>
     *
     * first argument is an {@link McpAsyncServerExchange} upon which the server can
     * interact with the connected client. The second arguments is a
     * {@link McpSchema.ReadResourceRequest}.
     */
    public static class AsyncResourceSpecification {
        McpSchema.Resource resource;
        BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                readHandler;

        @Override
        public String toString() {
            return "AsyncResourceSpecification{" + "resource=" + resource + ", readHandler=" + readHandler + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AsyncResourceSpecification that = (AsyncResourceSpecification) o;
            return Objects.equals(resource, that.resource) && Objects.equals(readHandler, that.readHandler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, readHandler);
        }

        public McpSchema.Resource getResource() {
            return resource;
        }

        public void setResource(McpSchema.Resource resource) {
            this.resource = resource;
        }

        public BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                getReadHandler() {
            return readHandler;
        }

        public void setReadHandler(
                BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                        readHandler) {
            this.readHandler = readHandler;
        }

        public AsyncResourceSpecification() {}

        public AsyncResourceSpecification(
                McpSchema.Resource resource,
                BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                        readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }

        static AsyncResourceSpecification fromSync(SyncResourceSpecification resource) {
            // FIXME: This is temporary, proper validation should be implemented
            if (resource == null) {
                return null;
            }
            return new AsyncResourceSpecification(resource.getResource(), (exchange, req) -> Mono.fromCallable(
                            () -> resource.getReadHandler().apply(new McpSyncServerExchange(exchange), req))
                    .subscribeOn(Schedulers.boundedElastic()));
        }
    }

    public static class AsyncResourceTemplateSpecification {
        McpSchema.ResourceTemplate resource;
        BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                readHandler;

        @Override
        public String toString() {
            return "AsyncResourceTemplateSpecification{" + "resource="
                    + resource + ", readHandler="
                    + readHandler + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AsyncResourceTemplateSpecification that = (AsyncResourceTemplateSpecification) o;
            return Objects.equals(resource, that.resource) && Objects.equals(readHandler, that.readHandler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, readHandler);
        }

        public McpSchema.ResourceTemplate getResource() {
            return resource;
        }

        public void setResource(McpSchema.ResourceTemplate resource) {
            this.resource = resource;
        }

        public BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                getReadHandler() {
            return readHandler;
        }

        public void setReadHandler(
                BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                        readHandler) {
            this.readHandler = readHandler;
        }

        public AsyncResourceTemplateSpecification() {}

        public AsyncResourceTemplateSpecification(
                McpSchema.ResourceTemplate resource,
                BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                        readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }

        static AsyncResourceTemplateSpecification fromSync(SyncResourceTemplateSpecification resource) {
            // FIXME: This is temporary, proper validation should be implemented
            if (resource == null) {
                return null;
            }
            return new AsyncResourceTemplateSpecification(resource.getResource(), (exchange, req) -> Mono.fromCallable(
                            () -> resource.getReadHandler().apply(new McpSyncServerExchange(exchange), req))
                    .subscribeOn(Schedulers.boundedElastic()));
        }
    }

    /**
     * Specification of a prompt template with its asynchronous handler function. Prompts
     * provide structured templates for AI model interactions, supporting:
     * <ul>
     * <li>Consistent message formatting
     * <li>Parameter substitution
     * <li>Context injection
     * <li>Response formatting
     * <li>Instruction templating
     * </ul>
     *
     * <p>
     * Example prompt specification: <pre>{@code
     * new McpServerFeatures.AsyncPromptSpecification(
     *     new Prompt("analyze", "Code analysis template"),
     *     (exchange, request) -> {
     *         String code = request.getArguments().get("code");
     *         return Mono.just(new GetPromptResult(
     *             "Analyze this code:\n\n" + code + "\n\nProvide feedback on:"
     *         ));
     *     }
     * )
     * }</pre>
     *
     * formatted templates. The function's first argument is an
     * {@link McpAsyncServerExchange} upon which the server can interact with the
     * connected client. The second arguments is a
     * {@link McpSchema.GetPromptRequest}.
     */
    public static class AsyncPromptSpecification {
        McpSchema.Prompt prompt;
        BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler;

        @Override
        public String toString() {
            return "AsyncPromptSpecification{" + "prompt=" + prompt + ", promptHandler=" + promptHandler + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AsyncPromptSpecification that = (AsyncPromptSpecification) o;
            return Objects.equals(prompt, that.prompt) && Objects.equals(promptHandler, that.promptHandler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prompt, promptHandler);
        }

        public McpSchema.Prompt getPrompt() {
            return prompt;
        }

        public void setPrompt(McpSchema.Prompt prompt) {
            this.prompt = prompt;
        }

        public BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
                getPromptHandler() {
            return promptHandler;
        }

        public void setPromptHandler(
                BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
                        promptHandler) {
            this.promptHandler = promptHandler;
        }

        public AsyncPromptSpecification() {}

        public AsyncPromptSpecification(
                McpSchema.Prompt prompt,
                BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
                        promptHandler) {
            this.prompt = prompt;
            this.promptHandler = promptHandler;
        }

        static AsyncPromptSpecification fromSync(SyncPromptSpecification prompt) {
            // FIXME: This is temporary, proper validation should be implemented
            if (prompt == null) {
                return null;
            }
            return new AsyncPromptSpecification(prompt.getPrompt(), (exchange, req) -> Mono.fromCallable(
                            () -> prompt.getPromptHandler().apply(new McpSyncServerExchange(exchange), req))
                    .subscribeOn(Schedulers.boundedElastic()));
        }
    }

    /**
     * Specification of a tool with its synchronous handler function. Tools are the
     * primary way for MCP servers to expose functionality to AI models. Each tool
     * represents a specific capability, such as:
     * <ul>
     * <li>Performing calculations
     * <li>Accessing external APIs
     * <li>Querying databases
     * <li>Manipulating files
     * <li>Executing system commands
     * </ul>
     *
     * <p>
     * Example tool specification: <pre>{@code
     * new McpServerFeatures.SyncToolSpecification(
     *     new Tool(
     *         "calculator",
     *         "Performs mathematical calculations",
     *         new JsonSchemaObject()
     *             .required("expression")
     *             .property("expression", JsonSchemaType.STRING)
     *     ),
     *     (exchange, args) -> {
     *         String expr = (String) args.get("expression");
     *         return new CallToolResult("Result: " + evaluate(expr));
     *     }
     * )
     * }</pre>
     *
     * returning results. The function's first argument is an
     * {@link McpSyncServerExchange} upon which the server can interact with the connected
     * client. The second arguments is a map of arguments passed to the tool.
     */
    public static class SyncToolSpecification {
        McpSchema.Tool tool;
        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call;

        @Override
        public String toString() {
            return "SyncToolSpecification{" + "tool=" + tool + ", call=" + call + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SyncToolSpecification that = (SyncToolSpecification) o;
            return Objects.equals(tool, that.tool) && Objects.equals(call, that.call);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tool, call);
        }

        public McpSchema.Tool getTool() {
            return tool;
        }

        public void setTool(McpSchema.Tool tool) {
            this.tool = tool;
        }

        public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> getCall() {
            return call;
        }

        public void setCall(BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
            this.call = call;
        }

        public SyncToolSpecification() {}

        public SyncToolSpecification(
                McpSchema.Tool tool,
                BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
            this.tool = tool;
            this.call = call;
        }
    }

    /**
     * Specification of a resource with its synchronous handler function. Resources
     * provide context to AI models by exposing data such as:
     * <ul>
     * <li>File contents
     * <li>Database records
     * <li>API responses
     * <li>System information
     * <li>Application state
     * </ul>
     *
     * <p>
     * Example resource specification: <pre>{@code
     * new McpServerFeatures.SyncResourceSpecification(
     *     new Resource("docs", "Documentation files", "text/markdown"),
     *     (exchange, request) -> {
     *         String content = readFile(request.getPath());
     *         return new ReadResourceResult(content);
     *     }
     * )
     * }</pre>
     *
     * first argument is an {@link McpSyncServerExchange} upon which the server can
     * interact with the connected client. The second arguments is a
     * {@link McpSchema.ReadResourceRequest}.
     */
    public static class SyncResourceSpecification {
        McpSchema.Resource resource;
        BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler;

        @Override
        public String toString() {
            return "SyncResourceSpecification{" + "resource=" + resource + ", readHandler=" + readHandler + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SyncResourceSpecification that = (SyncResourceSpecification) o;
            return Objects.equals(resource, that.resource) && Objects.equals(readHandler, that.readHandler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, readHandler);
        }

        public McpSchema.Resource getResource() {
            return resource;
        }

        public void setResource(McpSchema.Resource resource) {
            this.resource = resource;
        }

        public BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
                getReadHandler() {
            return readHandler;
        }

        public void setReadHandler(
                BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
                        readHandler) {
            this.readHandler = readHandler;
        }

        public SyncResourceSpecification() {}

        public SyncResourceSpecification(
                McpSchema.Resource resource,
                BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
                        readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }
    }

    /**
     * Specification of a resource template with its synchronous handler function.
     * Resource templates provide context to AI models by exposing data such as:
     * <ul>
     * <li>File contents
     * <li>Database records
     * <li>API responses
     * <li>System information
     * <li>Application state
     * </ul>
     *
     * <p>
     * Example resource specification: <pre>{@code
     * new McpServerFeatures.SyncResourceTemplateSpecification(
     *     new ResourceTemplate("file:///{path}", "docs", "Documentation files", "text/markdown"),
     *     (exchange, request) -> {
     *         String content = readFile(request.getPath());
     *         return new ReadResourceResult(content);
     *     }
     * )
     * }</pre>
     */
    public static class SyncResourceTemplateSpecification {
        McpSchema.ResourceTemplate resource;
        BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler;

        @Override
        public String toString() {
            return "SyncResourceTemplateSpecification{" + "resource=" + resource + ", readHandler=" + readHandler + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SyncResourceTemplateSpecification that = (SyncResourceTemplateSpecification) o;
            return Objects.equals(resource, that.resource) && Objects.equals(readHandler, that.readHandler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, readHandler);
        }

        public McpSchema.ResourceTemplate getResource() {
            return resource;
        }

        public void setResource(McpSchema.ResourceTemplate resource) {
            this.resource = resource;
        }

        public BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
                getReadHandler() {
            return readHandler;
        }

        public void setReadHandler(
                BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
                        readHandler) {
            this.readHandler = readHandler;
        }

        public SyncResourceTemplateSpecification() {}

        public SyncResourceTemplateSpecification(
                McpSchema.ResourceTemplate resource,
                BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
                        readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }
    }

    /**
     * Specification of a prompt template with its synchronous handler function. Prompts
     * provide structured templates for AI model interactions, supporting:
     * <ul>
     * <li>Consistent message formatting
     * <li>Parameter substitution
     * <li>Context injection
     * <li>Response formatting
     * <li>Instruction templating
     * </ul>
     *
     * <p>
     * Example prompt specification: <pre>{@code
     * new McpServerFeatures.SyncPromptSpecification(
     *     new Prompt("analyze", "Code analysis template"),
     *     (exchange, request) -> {
     *         String code = request.getArguments().get("code");
     *         return new GetPromptResult(
     *             "Analyze this code:\n\n" + code + "\n\nProvide feedback on:"
     *         );
     *     }
     * )
     * }</pre>
     *
     * formatted templates. The function's first argument is an
     * {@link McpSyncServerExchange} upon which the server can interact with the connected
     * client. The second arguments is a
     * {@link McpSchema.GetPromptRequest}.
     */
    public static class SyncPromptSpecification {
        McpSchema.Prompt prompt;
        BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler;

        @Override
        public String toString() {
            return "SyncPromptSpecification{" + "prompt=" + prompt + ", promptHandler=" + promptHandler + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SyncPromptSpecification that = (SyncPromptSpecification) o;
            return Objects.equals(prompt, that.prompt) && Objects.equals(promptHandler, that.promptHandler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prompt, promptHandler);
        }

        public McpSchema.Prompt getPrompt() {
            return prompt;
        }

        public void setPrompt(McpSchema.Prompt prompt) {
            this.prompt = prompt;
        }

        public BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>
                getPromptHandler() {
            return promptHandler;
        }

        public void setPromptHandler(
                BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>
                        promptHandler) {
            this.promptHandler = promptHandler;
        }

        public SyncPromptSpecification() {}

        public SyncPromptSpecification(
                McpSchema.Prompt prompt,
                BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>
                        promptHandler) {
            this.prompt = prompt;
            this.promptHandler = promptHandler;
        }
    }
}
