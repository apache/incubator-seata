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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.RuntimeTransportProvider;
import org.apache.seata.mcp.core.protocol.SchemaValidator;
import org.apache.seata.mcp.core.protocol.StandardJsonSchemaValidator;
import org.apache.seata.mcp.core.protocol.StreamableRuntimeTransportProvider;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Runtime core interface.
 */
public interface RuntimeCoreInterface {

    ProtocolDefinition.Implementation DEFAULT_SERVER_INFO =
            new ProtocolDefinition.Implementation("runtime-server", "1.0.0");

    static BaseAsyncSpecification<?> async(RuntimeTransportProvider provider) {
        return new StandardSpecificationBase(provider);
    }

    static BaseAsyncSpecification<?> async(StreamableRuntimeTransportProvider provider) {
        return new StreamableSpecificationBase(provider);
    }

    class StandardSpecificationBase extends BaseAsyncSpecification<StandardSpecificationBase> {
        private final RuntimeTransportProvider provider;

        StandardSpecificationBase(RuntimeTransportProvider provider) {
            RuntimeUtils.notNull(provider, "Provider required");
            this.provider = provider;
        }

        @Override
        public AsyncRuntimeEngine build() {
            return new AsyncRuntimeEngine(provider, getMapper(), buildFeatures(), getTimeout(), getValidator());
        }
    }

    class StreamableSpecificationBase extends BaseAsyncSpecification<StreamableSpecificationBase> {
        private final StreamableRuntimeTransportProvider provider;

        StreamableSpecificationBase(StreamableRuntimeTransportProvider provider) {
            this.provider = provider;
        }

        @Override
        public AsyncRuntimeEngine build() {
            return new AsyncRuntimeEngine(provider, getMapper(), buildFeatures(), getTimeout(), getValidator());
        }
    }

    abstract class BaseAsyncSpecification<S extends BaseAsyncSpecification<S>> {
        private ObjectMapper mapper;
        private ProtocolDefinition.Implementation serverInfo = DEFAULT_SERVER_INFO;
        private ProtocolDefinition.ServerCapabilities capabilities;
        private SchemaValidator validator;
        private String instructions;
        private final List<RuntimeCapabilities.AsyncToolSpecification> tools = new ArrayList<>();
        private final Map<ProtocolDefinition.CompleteReference, RuntimeCapabilities.AsyncCompletionSpecification>
                completions = new HashMap<>();
        private final List<BiFunction<RuntimeExchangeContext, List<ProtocolDefinition.Root>, Mono<Void>>> rootHandlers =
                new ArrayList<>();
        private Duration timeout = Duration.ofHours(10);

        public abstract AsyncRuntimeEngine build();

        public S serverInfo(String name, String version) {
            RuntimeUtils.requireText(name, "Name required");
            RuntimeUtils.requireText(version, "Version required");
            this.serverInfo = new ProtocolDefinition.Implementation(name, version);
            return self();
        }

        public S capabilities(ProtocolDefinition.ServerCapabilities capabilities) {
            RuntimeUtils.notNull(capabilities, "Capabilities required");
            this.capabilities = capabilities;
            return self();
        }

        public S tool(
                ProtocolDefinition.Tool tool,
                BiFunction<RuntimeExchangeContext, Map<String, Object>, Mono<ProtocolDefinition.CallToolResult>>
                        handler) {
            RuntimeUtils.notNull(tool, "Tool required");
            RuntimeUtils.notNull(handler, "Handler required");
            checkDuplicate(tool.getName());
            tools.add(new RuntimeCapabilities.AsyncToolSpecification(tool, handler));
            return self();
        }

        public S tools(List<RuntimeCapabilities.AsyncToolSpecification> specs) {
            RuntimeUtils.notNull(specs, "Specs required");
            specs.forEach(s -> {
                checkDuplicate(s.tool().getName());
                tools.add(s);
            });
            return self();
        }

        public S tools(RuntimeCapabilities.AsyncToolSpecification... specs) {
            RuntimeUtils.notNull(specs, "Specs required");
            for (RuntimeCapabilities.AsyncToolSpecification s : specs) {
                checkDuplicate(s.tool().getName());
                tools.add(s);
            }
            return self();
        }

        public S objectMapper(ObjectMapper mapper) {
            RuntimeUtils.notNull(mapper, "Mapper required");
            this.mapper = mapper;
            return self();
        }

        @SuppressWarnings("unchecked")
        protected S self() {
            return (S) this;
        }

        private void checkDuplicate(String name) {
            if (tools.stream().anyMatch(t -> t.tool().getName().equals(name))) {
                throw new IllegalArgumentException("Duplicate tool: " + name);
            }
        }

        protected RuntimeCapabilities.Async buildFeatures() {
            return new RuntimeCapabilities.Async(
                    serverInfo, capabilities, tools, completions, rootHandlers, instructions);
        }

        protected ObjectMapper getMapper() {
            return mapper != null ? mapper : new ObjectMapper();
        }

        protected SchemaValidator getValidator() {
            return validator != null ? validator : new StandardJsonSchemaValidator(getMapper());
        }

        protected Duration getTimeout() {
            return timeout;
        }
    }
}
