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
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Defines the asynchronous transport layer for the Model Context Protocol (MCP).
 *
 * <p>
 * The McpTransport interface provides the foundation for implementing custom transport
 * mechanisms in the Model Context Protocol. It handles the bidirectional communication
 * between the client and server components, supporting asynchronous message exchange
 * using JSON-RPC format.
 * </p>
 *
 * <p>
 * Implementations of this interface are responsible for:
 * </p>
 * <ul>
 * <li>Managing the lifecycle of the transport connection</li>
 * <li>Handling incoming messages and errors from the server</li>
 * <li>Sending outbound messages to the server</li>
 * </ul>
 *
 * <p>
 * The transport layer is designed to be protocol-agnostic, allowing for various
 * implementations such as WebSocket, HTTP, or custom protocols.
 * </p>
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransport {

    /**
     * Initializes and starts the transport connection.
     *
     * <p>
     * This method should be called before any message exchange can occur. It sets up the
     * necessary resources and establishes the connection to the server.
     * </p>
     */
    default Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        return Mono.empty();
    }

    /**
     * Closes the transport connection and releases any associated resources.
     *
     * <p>
     * This method ensures proper cleanup of resources when the transport is no longer
     * needed. It should handle the graceful shutdown of any active connections.
     * </p>
     */
    default void close() {
        this.closeGracefully().subscribe();
    }

    /**
     * Closes the transport connection and releases any associated resources
     * asynchronously.
     * @return a {@link Mono<Void>} that completes when the connection has been closed.
     */
    Mono<Void> closeGracefully();

    /**
     * Sends a message to the peer asynchronously.
     *
     * <p>
     * This method handles the transmission of messages to the server in an asynchronous
     * manner. Messages are sent in JSON-RPC format as specified by the MCP protocol.
     * </p>
     * @param message the {@link JSONRPCMessage} to be sent to the server
     * @return a {@link Mono<Void>} that completes when the message has been sent
     */
    Mono<Void> sendMessage(JSONRPCMessage message);

    /**
     * Unmarshals the given data into an object of the specified type.
     * @param <T> the type of the object to unmarshal
     * @param data the data to unmarshal
     * @param typeRef the type reference for the object to unmarshal
     * @return the unmarshalled object
     */
    <T> T unmarshalFrom(Object data, TypeReference<T> typeRef);
}
