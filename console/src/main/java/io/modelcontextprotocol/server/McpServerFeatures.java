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

    public static final class Async {
        private final McpSchema.Implementation serverInfo;
        private final McpSchema.ServerCapabilities serverCapabilities;
        private final List<AsyncToolSpecification> tools;
        private final Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions;
        private final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers;
        private final String instructions;

        public Async(
                McpSchema.Implementation serverInfo,
                McpSchema.ServerCapabilities serverCapabilities,
                List<AsyncToolSpecification> tools,
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
                            null,
                            null,
                            (tools != null && !tools.isEmpty())
                                    ? new McpSchema.ServerCapabilities.ToolCapabilities(false)
                                    : null);

            this.tools = tools != null ? tools : Collections.emptyList();
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
