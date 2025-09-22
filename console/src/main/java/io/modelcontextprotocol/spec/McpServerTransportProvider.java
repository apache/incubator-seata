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

import reactor.core.publisher.Mono;

/**
 * The core building block providing the server-side MCP transport. Implement this
 * interface to bridge between a particular server-side technology and the MCP server
 * transport layer.
 *
 * <p>
 * The lifecycle of the provider dictates that it be created first, upon application
 * startup, and then passed into either
 * a result of the MCP server creation, the provider will be notified of a
 * {@link McpServerSession.Factory} which will be used to handle a 1:1 communication
 * between a newly connected client and the server. The provider's responsibility is to
 * create instances of {@link McpServerTransport} that the session will utilise during the
 * session lifetime.
 *
 * <p>
 * Finally, the {@link McpServerTransport}s can be closed in bulk when {@link #close()} or
 * {@link #closeGracefully()} are called as part of the normal application shutdown event.
 * Individual {@link McpServerTransport}s can also be closed on a per-session basis, where
 * the {@link McpServerSession#close()} or {@link McpServerSession#closeGracefully()}
 * closes the provided transport.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface McpServerTransportProvider extends McpServerTransportProviderBase {

    /**
     * Sets the session factory that will be used to create sessions for new clients. An
     * implementation of the MCP server MUST call this method before any MCP interactions
     * take place.
     * @param sessionFactory the session factory to be used for initiating client sessions
     */
    void setSessionFactory(McpServerSession.Factory sessionFactory);

    /**
     * Sends a notification to all connected clients.
     * @param method the name of the notification method to be called on the clients
     * @param params a map of parameters to be sent with the notification
     * @return a Mono that completes when the notification has been broadcast
     */
    Mono<Void> notifyClients(String method, Object params);

    /**
     * Immediately closes all the transports with connected clients and releases any
     * associated resources.
     */
    default void close() {
        this.closeGracefully().subscribe();
    }

    /**
     * Gracefully closes all the transports with connected clients and releases any
     * associated resources asynchronously.
     * @return a {@link Mono<Void>} that completes when the connections have been closed.
     */
    Mono<Void> closeGracefully();
}
