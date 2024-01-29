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
import java.util.function.Supplier;

import org.apache.seata.metrics.Clock;
import org.apache.seata.metrics.Gauge;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.SystemClock;

/**
 * Compact Gauge implement with Supplier
 *
 */
public class CompactGauge<T extends Number> implements Gauge<T> {
    private final Id id;

    private final Supplier<T> supplier;

    private final Clock clock;

    public CompactGauge(Id id, Supplier<T> supplier) {
        this(id, supplier, SystemClock.INSTANCE);
    }

    public CompactGauge(Id id, Supplier<T> supplier, Clock clock) {
        this.id = id;
        this.supplier = supplier;
        this.clock = clock;
    }

    @Override
    public T get() {
        return supplier.get();
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public Iterable<Measurement> measure() {
        return Collections.singletonList(new Measurement(id, clock.getCurrentMilliseconds(), get().doubleValue()));
    }
}
