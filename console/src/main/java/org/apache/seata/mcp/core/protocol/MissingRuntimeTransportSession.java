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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import reactor.core.publisher.Mono;

/**
 * Missing runtime transport session.
 */
public class MissingRuntimeTransportSession implements RuntimeSession {

    private volatile boolean healthy = true;
    private final String id;
    private volatile ProtocolDefinition.LoggingLevel logLevel = ProtocolDefinition.LoggingLevel.INFO;

    public MissingRuntimeTransportSession(String id) {
        this.id = id;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object params, TypeReference<T> typeRef) {
        return Mono.error(new IllegalStateException("Stream unavailable: " + id));
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        return Mono.error(new IllegalStateException("Stream unavailable: " + id));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }

    @Override
    public void close() {}

    @Override
    public void setMinLoggingLevel(ProtocolDefinition.LoggingLevel level) {
        RuntimeUtils.notNull(level, "Level required");
        this.logLevel = level;
    }

    @Override
    public boolean isNotificationForLevelAllowed(ProtocolDefinition.LoggingLevel level) {
        return level.level() >= logLevel.level();
    }
}
