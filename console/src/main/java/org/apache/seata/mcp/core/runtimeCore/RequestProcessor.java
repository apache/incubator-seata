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

import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import reactor.core.publisher.Mono;

public interface RequestProcessor<T> {
    Mono<T> handle(RuntimeExchangeContext exchange, Object params);

    interface InitializationProcessor extends RequestProcessor<ProtocolDefinition.InitializeResult> {
        Mono<ProtocolDefinition.InitializeResult> handle(ProtocolDefinition.InitializeRequest request);

        @Override
        default Mono<ProtocolDefinition.InitializeResult> handle(RuntimeExchangeContext exchange, Object params) {
            if (params instanceof ProtocolDefinition.InitializeRequest) {
                return handle((ProtocolDefinition.InitializeRequest) params);
            }
            return Mono.error(new IllegalArgumentException("Invalid initialization request"));
        }
    }
}
