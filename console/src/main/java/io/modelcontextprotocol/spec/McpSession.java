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

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

/**
 * Represents a Model Control Protocol (MCP) session that handles communication between
 * clients and the server. This interface provides methods for sending requests and
 * notifications, as well as managing the session lifecycle.
 *
 * <p>
 * The session operates asynchronously using Project Reactor's {@link Mono} type for
 * non-blocking operations. It supports both request-response patterns and one-way
 * notifications.
 * </p>
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 */
public interface McpSession {

    boolean isHealthy();

    /**
     * Sends a request to the model counterparty and expects a response of type T.
     *
     * <p>
     * This method handles the request-response pattern where a response is expected from
     * the client or server. The response type is determined by the provided
     * TypeReference.
     * </p>
     * @param <T> the type of the expected response
     * @param method the name of the method to be called on the counterparty
     * @param requestParams the parameters to be sent with the request
     * @param typeRef the TypeReference describing the expected response type
     * @return a Mono that will emit the response when received
     */
    <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef);

    /**
     * Sends a notification to the model client or server without parameters.
     *
     * <p>
     * This method implements the notification pattern where no response is expected from
     * the counterparty. It's useful for fire-and-forget scenarios.
     * </p>
     * @param method the name of the notification method to be called on the server
     * @return a Mono that completes when the notification has been sent
     */
    default Mono<Void> sendNotification(String method) {
        return sendNotification(method, null);
    }

    /**
     * Sends a notification to the model client or server with parameters.
     *
     * <p>
     * Similar to {@link #sendNotification(String)} but allows sending additional
     * parameters with the notification.
     * </p>
     * @param method the name of the notification method to be sent to the counterparty
     * @param params a map of parameters to be sent with the notification
     * @return a Mono that completes when the notification has been sent
     */
    Mono<Void> sendNotification(String method, Object params);

    /**
     * Closes the session and releases any associated resources asynchronously.
     * @return a {@link Mono<Void>} that completes when the session has been closed.
     */
    Mono<Void> closeGracefully();

    /**
     * Closes the session and releases any associated resources.
     */
    void close();
}
