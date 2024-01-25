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
package org.apache.seata.metrics.registry.compact;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.seata.metrics.Clock;
import org.apache.seata.metrics.Counter;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.SystemClock;

/**
 * Compact Counter implement with AtomicLong
 *
 */
public class CompactCounter implements Counter {
    private final Id id;

    private final AtomicLong counter;

    private final Clock clock;

    public CompactCounter(Id id) {
        this(id, SystemClock.INSTANCE);
    }

    public CompactCounter(Id id, Clock clock) {
        this.id = id;
        this.counter = new AtomicLong(0);
        this.clock = clock;
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public long increase(long value) {
        return counter.addAndGet(value);
    }

    @Override
    public long decrease(long value) {
        return increase(-1 * value);
    }

    @Override
    public long get() {
        return counter.get();
    }

    @Override
    public Iterable<Measurement> measure() {
        return Collections.singletonList(new Measurement(id, clock.getCurrentMilliseconds(), counter.get()));
    }
}
