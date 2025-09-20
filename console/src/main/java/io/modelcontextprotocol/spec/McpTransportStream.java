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

package io.modelcontextprotocol.spec;

import org.reactivestreams.Publisher;
import reactor.util.function.Tuple2;

import java.util.Optional;

/**
 * A representation of a stream at the transport layer of the MCP protocol. In particular,
 * it is currently used in the Streamable HTTP implementation to potentially be able to
 * resume a broken connection from where it left off by optionally keeping track of
 * attached SSE event ids.
 *
 * @param <CONNECTION> the resource on which the stream is being served and consumed via
 * this mechanism
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransportStream<CONNECTION> {

    /**
     * The last observed event identifier.
     * @return if not empty, contains the most recent event that was consumed
     */
    Optional<String> lastId();

    /**
     * An internal stream identifier used to distinguish streams while debugging.
     * @return a {@code long} stream identifier value
     */
    long streamId();

    /**
     * Allows keeping track of the transport stream of events (currently an SSE stream
     * from Streamable HTTP specification) and enable resumability and reconnects in case
     * of stream errors.
     * @param eventStream a {@link Publisher} of tuples (pairs) of an optional identifier
     * associated with a collection of messages
     * @return a flattened {@link Publisher} of
     * {@link McpSchema.JSONRPCMessage JSON-RPC messages}
     * with the identifier stripped away
     */
    Publisher<McpSchema.JSONRPCMessage> consumeSseStream(
            Publisher<Tuple2<Optional<String>, Iterable<McpSchema.JSONRPCMessage>>> eventStream);
}
