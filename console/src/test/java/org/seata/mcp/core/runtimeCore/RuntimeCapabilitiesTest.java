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
package org.seata.mcp.core.runtimeCore;

import org.apache.seata.mcp.core.protocol.MissingRuntimeTransportSession;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.runtimeCore.RuntimeCapabilities;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeExchangeContext;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeCapabilitiesTest {

    @Test
    void testAsyncDefaultsToolCapabilities() {
        ProtocolDefinition.Tool tool = new ProtocolDefinition.Tool(
                "name", "title", "desc", new ProtocolDefinition.JsonSchema(), Collections.emptyMap(), null, null);
        RuntimeCapabilities.AsyncToolSpecification spec = new RuntimeCapabilities.AsyncToolSpecification(
                tool, (ctx, args) -> Mono.just(new ProtocolDefinition.CallToolResult("ok", false)));

        ProtocolDefinition.Implementation serverInfo = new ProtocolDefinition.Implementation("server", "1.0");
        RuntimeCapabilities.Async async =
                new RuntimeCapabilities.Async(serverInfo, null, Collections.singletonList(spec), null, null, "inst");

        assertSame(serverInfo, async.serverInfo());
        assertNotNull(async.serverCapabilities());
        assertNotNull(async.tools());
        assertEquals(1, async.tools().size());
        assertNotNull(async.serverCapabilities().tools());
        org.junit.jupiter.api.Assertions.assertFalse(
                async.serverCapabilities().tools().listChanged());
    }

    @Test
    void testAsyncToolSpecificationBuilderRequiresFields() {
        RuntimeCapabilities.AsyncToolSpecification.Builder builder =
                RuntimeCapabilities.AsyncToolSpecification.builder();

        assertThrows(IllegalArgumentException.class, builder::build);

        ProtocolDefinition.Tool tool = new ProtocolDefinition.Tool(
                "name", "title", "desc", new ProtocolDefinition.JsonSchema(), Collections.emptyMap(), null, null);

        assertThrows(IllegalArgumentException.class, () -> builder.tool(tool).build());

        RuntimeCapabilities.AsyncToolSpecification spec = RuntimeCapabilities.AsyncToolSpecification.builder()
                .tool(tool)
                .callHandler((ctx, req) -> Mono.just(new ProtocolDefinition.CallToolResult("ok", false)))
                .build();

        ProtocolDefinition.CallToolRequest request =
                new ProtocolDefinition.CallToolRequest("name", Collections.emptyMap());
        StepVerifier.create(spec.callHandler()
                        .apply(
                                new RuntimeExchangeContext(
                                        "id",
                                        new MissingRuntimeTransportSession("id"),
                                        new ProtocolDefinition.ClientCapabilities(),
                                        new ProtocolDefinition.Implementation("client", "1.0"),
                                        RuntimeContext.EMPTY),
                                request))
                .expectNextMatches(result -> !result.getError())
                .verifyComplete();
    }

    @Test
    void testAsyncCompletionSpecificationStoresHandlers() {
        ProtocolDefinition.CompleteReference ref = new ProtocolDefinition.CompleteReference() {
            @Override
            public String type() {
                return "type";
            }

            @Override
            public String identifier() {
                return "id";
            }
        };
        RuntimeCapabilities.AsyncCompletionSpecification spec = new RuntimeCapabilities.AsyncCompletionSpecification(
                ref,
                (exchange, completeRequest) -> Mono.just(new ProtocolDefinition.CompleteResult(
                        new ProtocolDefinition.CompleteResult.CompleteCompletion(Collections.emptyList(), 0, false),
                        Collections.emptyMap())));

        assertSame(ref, spec.referenceKey());
        assertNotNull(spec.completionHandler());
    }
}
