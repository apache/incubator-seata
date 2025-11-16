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

import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RuntimeCapabilities {

    public static final class Async {
        private final ProtocolDefinition.Implementation serverInfo;
        private final ProtocolDefinition.ServerCapabilities capabilities;
        private final List<AsyncToolSpecification> tools;
        private final Map<ProtocolDefinition.CompleteReference, AsyncCompletionSpecification> completions;
        private final List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>> rootHandlers;
        private final String instructions;

        public Async(
                ProtocolDefinition.Implementation serverInfo,
                ProtocolDefinition.ServerCapabilities capabilities,
                List<AsyncToolSpecification> tools,
                Map<ProtocolDefinition.CompleteReference, AsyncCompletionSpecification> completions,
                List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>> rootHandlers,
                String instructions) {
            RuntimeUtils.notNull(serverInfo, "Server info required");
            this.serverInfo = serverInfo;
            this.capabilities = capabilities != null ? capabilities : createDefaultCapabilities(tools);
            this.tools = tools != null ? tools : Collections.emptyList();
            this.completions = completions != null ? completions : Collections.emptyMap();
            this.rootHandlers = rootHandlers != null ? rootHandlers : Collections.emptyList();
            this.instructions = instructions;
        }

        private ProtocolDefinition.ServerCapabilities createDefaultCapabilities(List<AsyncToolSpecification> tools) {
            return new ProtocolDefinition.ServerCapabilities(
                    null,
                    null,
                    new ProtocolDefinition.ServerCapabilities.LoggingCapabilities(),
                    null,
                    null,
                    (tools != null && !tools.isEmpty())
                            ? new ProtocolDefinition.ServerCapabilities.ToolCapabilities(false)
                            : null);
        }

        public ProtocolDefinition.Implementation serverInfo() {
            return serverInfo;
        }

        public ProtocolDefinition.ServerCapabilities serverCapabilities() {
            return capabilities;
        }

        public List<AsyncToolSpecification> tools() {
            return tools;
        }

        public Map<ProtocolDefinition.CompleteReference, AsyncCompletionSpecification> completions() {
            return completions;
        }

        public List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>>
                rootsChangeConsumers() {
            return rootHandlers;
        }

        public String instructions() {
            return instructions;
        }
    }

    public static final class AsyncToolSpecification {
        private final ProtocolDefinition.Tool tool;
        private final BiFunction<RuntimeExchangeContext, Map<String, Object>, Mono<ProtocolDefinition.CallToolResult>>
                call;
        private final BiFunction<
                        RuntimeExchangeContext,
                        ProtocolDefinition.CallToolRequest,
                        Mono<ProtocolDefinition.CallToolResult>>
                handler;

        public AsyncToolSpecification(
                ProtocolDefinition.Tool tool,
                BiFunction<RuntimeExchangeContext, Map<String, Object>, Mono<ProtocolDefinition.CallToolResult>> call) {
            this(tool, call, (ex, req) -> call.apply(ex, req.getArguments()));
        }

        public AsyncToolSpecification(
                ProtocolDefinition.Tool tool,
                BiFunction<RuntimeExchangeContext, Map<String, Object>, Mono<ProtocolDefinition.CallToolResult>> call,
                BiFunction<
                                RuntimeExchangeContext,
                                ProtocolDefinition.CallToolRequest,
                                Mono<ProtocolDefinition.CallToolResult>>
                        handler) {
            this.tool = tool;
            this.call = call;
            this.handler = handler;
        }

        public ProtocolDefinition.Tool tool() {
            return tool;
        }

        public BiFunction<RuntimeExchangeContext, Map<String, Object>, Mono<ProtocolDefinition.CallToolResult>> call() {
            return call;
        }

        public BiFunction<
                        RuntimeExchangeContext,
                        ProtocolDefinition.CallToolRequest,
                        Mono<ProtocolDefinition.CallToolResult>>
                callHandler() {
            return handler;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ProtocolDefinition.Tool tool;
            private BiFunction<
                            RuntimeExchangeContext,
                            ProtocolDefinition.CallToolRequest,
                            Mono<ProtocolDefinition.CallToolResult>>
                    handler;

            public Builder tool(ProtocolDefinition.Tool tool) {
                this.tool = tool;
                return this;
            }

            public Builder callHandler(
                    BiFunction<
                                    RuntimeExchangeContext,
                                    ProtocolDefinition.CallToolRequest,
                                    Mono<ProtocolDefinition.CallToolResult>>
                            handler) {
                this.handler = handler;
                return this;
            }

            public AsyncToolSpecification build() {
                RuntimeUtils.notNull(tool, "Tool required");
                RuntimeUtils.notNull(handler, "Handler required");
                return new AsyncToolSpecification(tool, null, handler);
            }
        }
    }

    public static final class AsyncCompletionSpecification {
        private final ProtocolDefinition.CompleteReference ref;
        private final BiFunction<
                        RuntimeExchangeContext,
                        ProtocolDefinition.CompleteRequest,
                        Mono<ProtocolDefinition.CompleteResult>>
                handler;

        public AsyncCompletionSpecification(
                ProtocolDefinition.CompleteReference ref,
                BiFunction<
                                RuntimeExchangeContext,
                                ProtocolDefinition.CompleteRequest,
                                Mono<ProtocolDefinition.CompleteResult>>
                        handler) {
            this.ref = ref;
            this.handler = handler;
        }

        public ProtocolDefinition.CompleteReference referenceKey() {
            return ref;
        }

        public BiFunction<
                        RuntimeExchangeContext,
                        ProtocolDefinition.CompleteRequest,
                        Mono<ProtocolDefinition.CompleteResult>>
                completionHandler() {
            return handler;
        }
    }
}
