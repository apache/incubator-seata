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
 * Copyright (c) [Year] the original author or authors.
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

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP server features specification that a particular server can choose to support.
 *
 * @author Dariusz Jędrzejczyk
 * @author Jihoon Kim
 */
public class McpServerFeatures {

    /**
     * Asynchronous server features specification.
     */
    public static final class Async {
        private final McpSchema.Implementation serverInfo;
        private final McpSchema.ServerCapabilities serverCapabilities;
        private final List<AsyncToolSpecification> tools;
        private final Map<String, AsyncResourceSpecification> resources;
        private final List<McpSchema.ResourceTemplate> resourceTemplates;
        private final Map<String, AsyncPromptSpecification> prompts;
        private final Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions;
        private final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers;
        private final String instructions;

        public Async(
                McpSchema.Implementation serverInfo,
                McpSchema.ServerCapabilities serverCapabilities,
                List<AsyncToolSpecification> tools,
                Map<String, AsyncResourceSpecification> resources,
                List<McpSchema.ResourceTemplate> resourceTemplates,
                Map<String, AsyncPromptSpecification> prompts,
                Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions,
                List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
                String instructions) {
            Assert.notNull(serverInfo, "Server info must not be null");

            this.serverInfo = serverInfo;
            this.serverCapabilities = (serverCapabilities != null)
                    ? serverCapabilities
                    : new McpSchema.ServerCapabilities(
                            null,
                            null,
                            new McpSchema.ServerCapabilities.LoggingCapabilities(),
                            (prompts != null && !prompts.isEmpty())
                                    ? new McpSchema.ServerCapabilities.PromptCapabilities(false)
                                    : null,
                            (resources != null && !resources.isEmpty())
                                    ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false)
                                    : null,
                            (tools != null && !tools.isEmpty())
                                    ? new McpSchema.ServerCapabilities.ToolCapabilities(false)
                                    : null);

            this.tools = tools != null ? tools : Collections.emptyList();
            this.resources = resources != null ? resources : Collections.emptyMap();
            this.resourceTemplates = resourceTemplates != null ? resourceTemplates : Collections.emptyList();
            this.prompts = prompts != null ? prompts : Collections.emptyMap();
            this.completions = completions != null ? completions : Collections.emptyMap();
            this.rootsChangeConsumers = rootsChangeConsumers != null ? rootsChangeConsumers : Collections.emptyList();
            this.instructions = instructions;
        }

        public McpSchema.Implementation serverInfo() {
            return serverInfo;
        }

        public McpSchema.ServerCapabilities serverCapabilities() {
            return serverCapabilities;
        }

        public List<AsyncToolSpecification> tools() {
            return tools;
        }

        public Map<String, AsyncResourceSpecification> resources() {
            return resources;
        }

        public List<McpSchema.ResourceTemplate> resourceTemplates() {
            return resourceTemplates;
        }

        public Map<String, AsyncPromptSpecification> prompts() {
            return prompts;
        }

        public Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions() {
            return completions;
        }

        public List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers() {
            return rootsChangeConsumers;
        }

        public String instructions() {
            return instructions;
        }
    }

    /**
     * Specification of a tool with its asynchronous handler function. Tools are the
     * primary way for MCP servers to expose functionality to AI models. Each tool
     * represents a specific capability.
     * {@link McpAsyncServerExchange} and a
     * {@link CallToolRequest} and returning
     * results. The function's first argument is an {@link McpAsyncServerExchange} upon
     * which the server can interact with the connected client. The second arguments is a
     * map of tool arguments.
     */
    public static final class AsyncToolSpecification {
        private final McpSchema.Tool tool;

        private final BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call;

        private final BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler;

        public AsyncToolSpecification(
                McpSchema.Tool tool,
                BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
            this(tool, call, (exchange, toolReq) -> call.apply(exchange, toolReq.getArguments()));
        }

        public AsyncToolSpecification(
                McpSchema.Tool tool,
                BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call,
                BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler) {
            this.tool = tool;
            this.call = call;
            this.callHandler = callHandler;
        }

        public McpSchema.Tool tool() {
            return tool;
        }

        public BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call() {
            return call;
        }

        public BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler() {
            return callHandler;
        }

        public static class Builder {
            private McpSchema.Tool tool;
            private BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler;

            public Builder tool(McpSchema.Tool tool) {
                this.tool = tool;
                return this;
            }

            public Builder callHandler(
                    BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler) {
                this.callHandler = callHandler;
                return this;
            }

            public AsyncToolSpecification build() {
                Assert.notNull(tool, "Tool must not be null");
                Assert.notNull(callHandler, "Call handler function must not be null");
                return new AsyncToolSpecification(tool, null, callHandler);
            }
        }

        public static Builder builder() {
            return new Builder();
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
     * Example resource specification:
     *
     * <pre>{@code
     * new McpServerFeatures.AsyncResourceSpecification(
     * 		new Resource("docs", "Documentation files", "text/markdown"),
     * 		(exchange, request) -> Mono.fromSupplier(() -> readFile(request.getPath()))
     * 				.map(ReadResourceResult::new))
     * }</pre>
     *
     * first argument is an {@link McpAsyncServerExchange} upon which the server can
     * interact with the connected client. The second arguments is a
     * {@link McpSchema.ReadResourceRequest}.
     */
    public static final class AsyncResourceSpecification {
        private final McpSchema.Resource resource;
        private final BiFunction<
                        McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                readHandler;

        public AsyncResourceSpecification(
                McpSchema.Resource resource,
                BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                        readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }

        public McpSchema.Resource resource() {
            return resource;
        }

        public BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>>
                readHandler() {
            return readHandler;
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
     * Example prompt specification:
     *
     * <pre>{@code
     * new McpServerFeatures.AsyncPromptSpecification(
     * 		new Prompt("analyze", "Code analysis template"),
     * 		(exchange, request) -> {
     * 			String code = request.getArguments().get("code");
     * 			return Mono.just(new GetPromptResult(
     * 					"Analyze this code:\n\n" + code + "\n\nProvide feedback on:"));
     * 		})
     * }</pre>
     *
     * formatted templates. The function's first argument is an
     * {@link McpAsyncServerExchange} upon which the server can interact with the
     * connected client. The second arguments is a
     * {@link McpSchema.GetPromptRequest}.
     */
    public static final class AsyncPromptSpecification {
        private final McpSchema.Prompt prompt;
        private final BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
                promptHandler;

        public AsyncPromptSpecification(
                McpSchema.Prompt prompt,
                BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
                        promptHandler) {
            this.prompt = prompt;
            this.promptHandler = promptHandler;
        }

        public McpSchema.Prompt prompt() {
            return prompt;
        }

        public BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
                promptHandler() {
            return promptHandler;
        }
    }

    /**
     * Specification of a completion handler function with asynchronous execution support.
     * Completions generate AI model outputs based on prompt or resource references and
     * user-provided arguments. This abstraction enables:
     * <ul>
     * <li>Customizable response generation logic
     * <li>Parameter-driven template expansion
     * <li>Dynamic interaction with connected clients
     * </ul>
     *
     * requests and returns results. The first argument is an
     * {@link McpAsyncServerExchange} used to interact with the client. The second
     * argument is a {@link McpSchema.CompleteRequest}.
     */
    public static final class AsyncCompletionSpecification {
        private final McpSchema.CompleteReference referenceKey;
        private final BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>>
                completionHandler;

        public AsyncCompletionSpecification(
                McpSchema.CompleteReference referenceKey,
                BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>>
                        completionHandler) {
            this.referenceKey = referenceKey;
            this.completionHandler = completionHandler;
        }

        public McpSchema.CompleteReference referenceKey() {
            return referenceKey;
        }

        public BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>>
                completionHandler() {
            return completionHandler;
        }
    }
}
