/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.metrics.registry.compact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.seata.common.loader.LoadLevel;
import io.seata.metrics.Counter;
import io.seata.metrics.Gauge;
import io.seata.metrics.Id;
import io.seata.metrics.Measurement;
import io.seata.metrics.Meter;
import io.seata.metrics.registry.Registry;
import io.seata.metrics.Summary;
import io.seata.metrics.Timer;

/**
 * Compact Registry implement, this registry only compute all Measurements when call measure method and do not cache
 *
 * @author zhengyangyong
 */
@LoadLevel(name = "Compact", order = 1)
public class CompactRegistry implements Registry {
    private static final Map<UUID, Meter> METERS = new ConcurrentHashMap<>();

    @Override
    public <T extends Number> Gauge<T> getGauge(Id id, Supplier<T> supplier) {
        return (Gauge<T>)CompactRegistry.METERS.computeIfAbsent(id.getId(), key -> new CompactGauge<>(id, supplier));
    }

    @Override
    public Counter getCounter(Id id) {
        return (Counter)CompactRegistry.METERS.computeIfAbsent(id.getId(), key -> new CompactCounter(id));
    }

    @Override
    public Summary getSummary(Id id) {
        return (Summary)CompactRegistry.METERS.computeIfAbsent(id.getId(), key -> new CompactSummary(id));
    }

    @Override
    public Timer getTimer(Id id) {
        return (Timer)CompactRegistry.METERS.computeIfAbsent(id.getId(), key -> new CompactTimer(id));
    }

    @Override
    public Iterable<Measurement> measure() {
        List<Measurement> measurements = new ArrayList<>();
        if (CompactRegistry.METERS.size() == 0) {
            return measurements;
        }
        CompactRegistry.METERS.values().iterator().forEachRemaining(
            meter -> meter.measure().forEach(measurements::add));
        return measurements;
    }
}
