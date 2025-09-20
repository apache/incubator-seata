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
 * Copyright (c) [Year] the original author or authors.
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

package io.modelcontextprotocol.server;

import java.util.Collections;

/**
 * Context associated with the transport layer. It allows to add transport-level metadata
 * for use further down the line. Specifically, it can be beneficial to extract HTTP
 * request metadata for use in MCP feature implementations.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransportContext {

    /**
     * Key for use in Reactor Context to transport the context to user land.
     */
    String KEY = "MCP_TRANSPORT_CONTEXT";

    /**
     * An empty, unmodifiable context.
     */
    @SuppressWarnings("unchecked")
    McpTransportContext EMPTY = new DefaultMcpTransportContext(Collections.EMPTY_MAP);

    /**
     * Extract a value from the context.
     * @param key the key under the data is expected
     * @return the associated value or {@code null} if missing.
     */
    Object get(String key);

    /**
     * Inserts a value for a given key.
     * @param key a String representing the key
     * @param value the value to store
     */
    void put(String key, Object value);

    /**
     * Copies the contents of the context to allow further modifications without affecting
     * the initial object.
     * @return a new instance with the underlying storage copied.
     */
    McpTransportContext copy();
}
