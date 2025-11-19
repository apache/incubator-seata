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

import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeExchangeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeExchangeContextTest {

    private RuntimeSession session;
    private RuntimeExchangeContext exchangeContext;

    @BeforeEach
    void setUp() {
        session = mock(RuntimeSession.class);
        ProtocolDefinition.ClientCapabilities clientCaps = new ProtocolDefinition.ClientCapabilities();
        ProtocolDefinition.Implementation clientInfo = new ProtocolDefinition.Implementation("client", "1.0");
        exchangeContext =
                new RuntimeExchangeContext("session-id", session, clientCaps, clientInfo, RuntimeContext.EMPTY);
    }

    @Test
    void testListRootsAggregatesPages() {
        ProtocolDefinition.ListRootsResult firstPage = new ProtocolDefinition.ListRootsResult(
                new ArrayList<>(Collections.singletonList(new ProtocolDefinition.Root("uri1", "name1"))), "cursor-1");
        ProtocolDefinition.ListRootsResult secondPage = new ProtocolDefinition.ListRootsResult(
                new ArrayList<>(Collections.singletonList(new ProtocolDefinition.Root("uri2", "name2"))), null);

        when(session.sendRequest(
                        eq(ProtocolDefinition.METHOD_ROOTS_LIST),
                        any(ProtocolDefinition.PaginatedRequest.class),
                        any()))
                .thenAnswer(invocation -> {
                    ProtocolDefinition.PaginatedRequest request = invocation.getArgument(1);
                    if (request.getCursor() == null) {
                        return Mono.just(firstPage);
                    }
                    return Mono.just(secondPage);
                });

        StepVerifier.create(exchangeContext.listRoots())
                .assertNext(result -> {
                    List<ProtocolDefinition.Root> roots = result.getRoots();
                    org.junit.jupiter.api.Assertions.assertEquals(2, roots.size());
                    org.junit.jupiter.api.Assertions.assertEquals(
                            "uri1", roots.get(0).getUri());
                    org.junit.jupiter.api.Assertions.assertEquals(
                            "uri2", roots.get(1).getUri());
                })
                .verifyComplete();
    }

    @Test
    void testListRootsWithExplicitCursor() {
        ProtocolDefinition.ListRootsResult singlePage = new ProtocolDefinition.ListRootsResult(
                new ArrayList<>(Collections.singletonList(new ProtocolDefinition.Root("uri3", "name3"))), null);
        when(session.sendRequest(
                        eq(ProtocolDefinition.METHOD_ROOTS_LIST),
                        any(ProtocolDefinition.PaginatedRequest.class),
                        any()))
                .thenReturn(Mono.just(singlePage));

        StepVerifier.create(exchangeContext.listRoots("custom"))
                .expectNext(singlePage)
                .verifyComplete();

        ArgumentCaptor<ProtocolDefinition.PaginatedRequest> requestCaptor =
                ArgumentCaptor.forClass(ProtocolDefinition.PaginatedRequest.class);
        verify(session).sendRequest(eq(ProtocolDefinition.METHOD_ROOTS_LIST), requestCaptor.capture(), any());
        org.junit.jupiter.api.Assertions.assertEquals(
                "custom", requestCaptor.getValue().getCursor());
    }
}
