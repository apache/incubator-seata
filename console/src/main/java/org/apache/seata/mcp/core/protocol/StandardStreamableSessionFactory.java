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

package org.apache.seata.mcp.core.protocol;

import org.apache.seata.mcp.core.runtimeCore.NotificationProcessor;
import org.apache.seata.mcp.core.runtimeCore.RequestProcessor;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class StandardStreamableSessionFactory implements StreamableServerRuntimeSession.Factory {

    private final Duration timeout;
    private final StreamableServerRuntimeSession.InitRequestHandler initHandler;
    private final Map<String, RequestProcessor<?>> requestHandlers;
    private final Map<String, NotificationProcessor> notificationHandlers;

    public StandardStreamableSessionFactory(
            Duration timeout,
            StreamableServerRuntimeSession.InitRequestHandler initHandler,
            Map<String, RequestProcessor<?>> requestHandlers,
            Map<String, NotificationProcessor> notificationHandlers) {
        this.timeout = timeout;
        this.initHandler = initHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    @Override
    public StreamableServerRuntimeSession.StreamableServerRuntimeSessionInit startSession(
            ProtocolDefinition.InitializeRequest req) {
        StreamableServerRuntimeSession session = new StreamableServerRuntimeSession(
                UUID.randomUUID().toString(),
                req.getCapabilities(),
                req.getClientInfo(),
                timeout,
                requestHandlers,
                notificationHandlers);
        Mono<ProtocolDefinition.InitializeResult> result = initHandler.handle(req);
        return new StreamableServerRuntimeSession.StreamableServerRuntimeSessionInit(session, result);
    }
}
