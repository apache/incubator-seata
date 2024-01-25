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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.metrics.Counter;
import org.apache.seata.metrics.Gauge;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.Meter;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.Summary;
import org.apache.seata.metrics.Timer;

/**
 * Compact Registry implement, this registry only compute all Measurements when call measure method and do not cache
 *
 */
@LoadLevel(name = "compact", order = 1)
public class CompactRegistry implements Registry {
    private static final Map<String, Meter> METERS = new ConcurrentHashMap<>();

    @Override
    public <T extends Number> Gauge<T> getGauge(Id id, Supplier<T> supplier) {
        return (Gauge<T>)CollectionUtils.computeIfAbsent(METERS, id.getMeterKey(), key -> new CompactGauge<>(
                new Id(id.getName()).withTag(id.getTags()), supplier));
    }

    @Override
    public Counter getCounter(Id id) {
        return (Counter)CollectionUtils.computeIfAbsent(METERS, id.getMeterKey(), key -> new CompactCounter(
                new Id(id.getName()).withTag(id.getTags())));
    }

    @Override
    public Summary getSummary(Id id) {
        return (Summary)CollectionUtils.computeIfAbsent(METERS, id.getMeterKey(), key -> new CompactSummary(
                new Id(id.getName()).withTag(id.getTags())));
    }

    @Override
    public Timer getTimer(Id id) {
        return (Timer)CollectionUtils.computeIfAbsent(METERS, id.getMeterKey(), key -> new CompactTimer(
                new Id(id.getName()).withTag(id.getTags())));
    }

    @Override
    public Iterable<Measurement> measure() {
        final List<Measurement> measurements = new ArrayList<>();
        if (METERS.isEmpty()) {
            return measurements;
        }
        METERS.values().iterator()
                .forEachRemaining(meter -> meter.measure().forEach(measurements::add));
        return measurements;
    }

    @Override
    public void clearUp() {
        METERS.clear();
    }
}
