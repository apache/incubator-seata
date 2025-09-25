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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 * @author Jihoon Kim
 * @see McpAsyncServer
 * @see McpServerTransportProvider
 */
public interface McpServer {

    McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server", "1.0.0");

    static AsyncSpecification<?> async(McpServerTransportProvider transportProvider) {
        return new SingleSessionAsyncSpecification(transportProvider);
    }

    class SingleSessionAsyncSpecification extends AsyncSpecification<SingleSessionAsyncSpecification> {

        private final McpServerTransportProvider transportProvider;

        private SingleSessionAsyncSpecification(McpServerTransportProvider transportProvider) {
            Assert.notNull(transportProvider, "Transport provider must not be null");
            this.transportProvider = transportProvider;
        }

        @Override
        public McpAsyncServer build() {
            McpServerFeatures.Async features = new McpServerFeatures.Async(
                    this.serverInfo,
                    this.serverCapabilities,
                    this.tools,
                    this.completions,
                    this.rootsChangeHandlers,
                    this.instructions);
            ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
            JsonSchemaValidator jsonSchemaValidator = this.jsonSchemaValidator != null
                    ? this.jsonSchemaValidator
                    : new DefaultJsonSchemaValidator(mapper);
            return new McpAsyncServer(
                    this.transportProvider, mapper, features, this.requestTimeout, jsonSchemaValidator);
        }
    }

    static AsyncSpecification<?> async(McpStreamableServerTransportProvider transportProvider) {
        return new StreamableServerAsyncSpecification(transportProvider);
    }

    class StreamableServerAsyncSpecification extends AsyncSpecification<StreamableServerAsyncSpecification> {

        private final McpStreamableServerTransportProvider transportProvider;

        public StreamableServerAsyncSpecification(McpStreamableServerTransportProvider transportProvider) {
            this.transportProvider = transportProvider;
        }

        @Override
        public McpAsyncServer build() {
            McpServerFeatures.Async features = new McpServerFeatures.Async(
                    this.serverInfo,
                    this.serverCapabilities,
                    this.tools,
                    this.completions,
                    this.rootsChangeHandlers,
                    this.instructions);
            ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
            JsonSchemaValidator jsonSchemaValidator = this.jsonSchemaValidator != null
                    ? this.jsonSchemaValidator
                    : new DefaultJsonSchemaValidator(mapper);
            return new McpAsyncServer(
                    this.transportProvider, mapper, features, this.requestTimeout, jsonSchemaValidator);
        }
    }

    abstract class AsyncSpecification<S extends AsyncSpecification<S>> {

        ObjectMapper objectMapper;

        McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

        McpSchema.ServerCapabilities serverCapabilities;

        JsonSchemaValidator jsonSchemaValidator;

        String instructions;

        final List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

        final Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions =
                new HashMap<>();

        final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeHandlers =
                new ArrayList<>();

        Duration requestTimeout = Duration.ofHours(10); // Default timeout

        public abstract McpAsyncServer build();

        public AsyncSpecification<S> serverInfo(String name, String version) {
            Assert.hasText(name, "Name must not be null or empty");
            Assert.hasText(version, "Version must not be null or empty");
            this.serverInfo = new McpSchema.Implementation(name, version);
            return this;
        }

        public AsyncSpecification<S> capabilities(McpSchema.ServerCapabilities serverCapabilities) {
            Assert.notNull(serverCapabilities, "Server capabilities must not be null");
            this.serverCapabilities = serverCapabilities;
            return this;
        }

        public AsyncSpecification<S> tool(
                McpSchema.Tool tool,
                BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<CallToolResult>> handler) {
            Assert.notNull(tool, "Tool must not be null");
            Assert.notNull(handler, "Handler must not be null");
            assertNoDuplicateTool(tool.getName());

            this.tools.add(new McpServerFeatures.AsyncToolSpecification(tool, handler));

            return this;
        }

        public AsyncSpecification<S> tools(List<McpServerFeatures.AsyncToolSpecification> toolSpecifications) {
            Assert.notNull(toolSpecifications, "Tool handlers list must not be null");

            for (McpServerFeatures.AsyncToolSpecification tool : toolSpecifications) {
                assertNoDuplicateTool(tool.tool().getName());
                this.tools.add(tool);
            }

            return this;
        }

        public AsyncSpecification<S> tools(McpServerFeatures.AsyncToolSpecification... toolSpecifications) {
            Assert.notNull(toolSpecifications, "Tool handlers list must not be null");

            for (McpServerFeatures.AsyncToolSpecification tool : toolSpecifications) {
                assertNoDuplicateTool(tool.tool().getName());
                this.tools.add(tool);
            }
            return this;
        }

        private void assertNoDuplicateTool(String toolName) {
            if (this.tools.stream()
                    .anyMatch(toolSpec -> toolSpec.tool().getName().equals(toolName))) {
                throw new IllegalArgumentException("Tool with name '" + toolName + "' is already registered.");
            }
        }

        public AsyncSpecification<S> objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }
    }
}
