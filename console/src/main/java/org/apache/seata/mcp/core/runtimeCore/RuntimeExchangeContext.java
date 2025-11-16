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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Runtime exchange context.
 */
public class RuntimeExchangeContext {

    private final String id;
    private final RuntimeSession session;
    private final ProtocolDefinition.ClientCapabilities clientCaps;
    private final ProtocolDefinition.Implementation clientInfo;
    private final RuntimeContext context;

    private static final TypeReference<ProtocolDefinition.ListRootsResult> ROOTS_TYPE =
            new TypeReference<ProtocolDefinition.ListRootsResult>() {};
    public static final TypeReference<Object> OBJECT_TYPE = new TypeReference<Object>() {};

    public RuntimeExchangeContext(
            String id,
            RuntimeSession session,
            ProtocolDefinition.ClientCapabilities clientCaps,
            ProtocolDefinition.Implementation clientInfo,
            RuntimeContext context) {
        this.id = id;
        this.session = session;
        this.clientCaps = clientCaps;
        this.clientInfo = clientInfo;
        this.context = context;
    }

    public Mono<ProtocolDefinition.ListRootsResult> listRoots() {
        return listRoots(ProtocolDefinition.FIRST_PAGE)
                .expand(r -> r.getNextCursor() != null ? listRoots(r.getNextCursor()) : Mono.empty())
                .reduce(new ProtocolDefinition.ListRootsResult(new ArrayList<>(), null), (all, r) -> {
                    all.getRoots().addAll(r.getRoots());
                    return all;
                })
                .map(r -> new ProtocolDefinition.ListRootsResult(
                        Collections.unmodifiableList(r.getRoots()), r.getNextCursor()));
    }

    public Mono<ProtocolDefinition.ListRootsResult> listRoots(String cursor) {
        return session.sendRequest(
                ProtocolDefinition.METHOD_ROOTS_LIST, new ProtocolDefinition.PaginatedRequest(cursor), ROOTS_TYPE);
    }

    void setMinLoggingLevel(ProtocolDefinition.LoggingLevel level) {
        RuntimeUtils.notNull(level, "Level required");
        session.setMinLoggingLevel(level);
    }
}
