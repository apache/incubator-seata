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

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * An implementation of {@link McpTransportStream} using Project Reactor types.
 *
 * @param <CONNECTION> the resource serving the stream
 * @author Dariusz Jędrzejczyk
 */
public class DefaultMcpTransportStream<CONNECTION> implements McpTransportStream<CONNECTION> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMcpTransportStream.class);

    private static final AtomicLong counter = new AtomicLong();

    private final AtomicReference<String> lastId = new AtomicReference<>();

    // Used only for internal accounting
    private final long streamId;

    private final boolean resumable;

    private final Function<McpTransportStream<CONNECTION>, Publisher<CONNECTION>> reconnect;

    /**
     * Constructs a new instance representing a particular stream that can resume using
     * the provided reconnect mechanism.
     * @param resumable whether the stream is resumable and should try to reconnect
     * @param reconnect the mechanism to use in case an error is observed on the current
     * event stream to asynchronously kick off a resumed stream consumption, potentially
     * using the stored {@link #lastId()}.
     */
    public DefaultMcpTransportStream(
            boolean resumable, Function<McpTransportStream<CONNECTION>, Publisher<CONNECTION>> reconnect) {
        this.reconnect = reconnect;
        this.streamId = counter.getAndIncrement();
        this.resumable = resumable;
    }

    @Override
    public Optional<String> lastId() {
        return Optional.ofNullable(this.lastId.get());
    }

    @Override
    public long streamId() {
        return this.streamId;
    }

    @Override
    public Publisher<McpSchema.JSONRPCMessage> consumeSseStream(
            Publisher<Tuple2<Optional<String>, Iterable<McpSchema.JSONRPCMessage>>> eventStream) {

        // @formatter:off
        return Flux.deferContextual(ctx -> Flux.from(eventStream)
                .doOnNext(idAndMessage -> idAndMessage.getT1().ifPresent(id -> {
                    String previousId = this.lastId.getAndSet(id);
                    logger.debug("Updating last id {} -> {} for stream {}", previousId, id, this.streamId);
                }))
                .doOnError(e -> {
                    if (resumable && !(e instanceof McpTransportSessionNotFoundException)) {
                        Mono.from(reconnect.apply(this)).contextWrite(ctx).subscribe();
                    }
                })
                .flatMapIterable(Tuple2::getT2)); // @formatter:on
    }
}
